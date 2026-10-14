"""Validate generated Forestry resources against source assets and the previous data baseline.

Usage: python scripts/check-datagen-output.py OUTPUT [--baseline src/generated]
Only OUTPUT and hand-authored resource roots satisfy model/texture references; the old
generated tree cannot hide missing output. Requires no Minecraft or third-party packages.
"""
import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PARTS = {"resources": "main", "resources_farms": "farms", "resources_mail": "mail", "resources_butterflies": "butterflies"}


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def walk(value):
    if isinstance(value, dict):
        yield value
        for child in value.values():
            yield from walk(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk(child)


def stack_counts(value, path=""):
    if isinstance(value, dict):
        if "id" in value and isinstance(value["id"], str):
            yield path, (value["id"], value.get("count", 1))
        for key, child in value.items():
            yield from stack_counts(child, path + "/" + key)
    elif isinstance(value, list):
        for index, child in enumerate(value):
            yield from stack_counts(child, path + "/" + str(index))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    parser.add_argument("--baseline", type=Path)
    parser.add_argument("--item-model-report", type=Path, help="Runtime model ID report from runLifecycleTestServer")
    args = parser.parse_args()
    output = args.output.resolve()
    roots = [output / part for part in PARTS] + [ROOT / "src" / owner / "resources" for owner in PARTS.values()]
    spec = importlib.util.spec_from_file_location("recipes", ROOT / "scripts/check-26.2-recipes.py")
    recipes = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(recipes)
    errors = []
    referenced = set()
    assets = {str(path.relative_to(root)) for root in roots for path in (root / "assets").rglob("*") if path.is_file()}
    sprite_aliases = {}
    for root in roots:
        for atlas in (root / "assets").glob("*/atlases/*.json"):
            for source in load(atlas).get("sources", []):
                if source.get("type") == "minecraft:single" and "sprite" in source:
                    sprite_aliases[source["sprite"]] = source["resource"]

    def reference(identifier, folder, suffix, source):
        if not isinstance(identifier, str) or not identifier.startswith("forestry:"):
            return
        if folder == "textures":
            identifier = sprite_aliases.get(identifier, identifier)
        rel = Path("assets/forestry") / folder / (identifier.split(":", 1)[1] + suffix)
        referenced.add(str(rel))
        if str(rel) not in assets:
            errors.append(f"{source}: missing {rel}")

    # Equipment definitions prefix texture ids at runtime; these are not block-atlas references.
    # Include hand-authored definitions, which otherwise escaped the generated-model scan.
    for root in roots:
        for equipment in (root / "assets/forestry/equipment").glob("*.json"):
            for layer_type, layers in load(equipment).get("layers", {}).items():
                for layer in layers:
                    reference(layer.get("texture"), "textures/entity/equipment/" + layer_type, ".png", equipment)

    counts = {}
    for part in PARTS:
        files = sorted((output / part).rglob("*.json"))
        if not files:
            errors.append(f"No generated files for {part}")
        counts[part] = len(files)
        for file in files:
            relative = file.relative_to(output)
            value = load(file)
            if "assets" in relative.parts:
                for obj in walk(value):
                    for key in ("parent", "model", "base"):
                        # A fluid-container's base texture is checked via its textures map.
                        if key == "base" and obj.get("type") != "minecraft:special":
                            continue
                        reference(obj.get(key), "models", ".json", relative)
                    for texture in obj.get("textures", {}).values():
                        reference(texture, "textures", ".png", relative)
                    for child in obj.get("children", {}).values() if isinstance(obj.get("children"), dict) else []:
                        reference(child, "models", ".json", relative)
                if "models" in relative.parts and "overrides" in value:
                    errors.append(f"{relative}: obsolete item overrides")
            if "data" in relative.parts and "recipe" in relative.parts:
                try:
                    recipes.recipe(value, str(relative))
                except ValueError as error:
                    errors.append(str(error))
                if args.baseline and (args.baseline / relative).is_file():
                    previous = dict(stack_counts(load(args.baseline / relative)))
                    current = dict(stack_counts(value))
                    for path in previous.keys() & current.keys():
                        if previous[path] != current[path]:
                            errors.append(f"{relative}{path}: stack changed {previous[path]} -> {current[path]}")

    for part, folder, expected in [("resources", "bee_species", 69), ("resources", "tree_species", 50), ("resources_butterflies", "butterfly_species", 35)]:
        actual = len(list((output / part / "data/forestry" / folder).glob("*.json")))
        if actual != expected:
            errors.append(f"{folder}: expected {expected}, found {actual}")
    # Concrete yields guard against helpers silently defaulting a requested output count to one.
    for recipe, expected in {"larch_wood": 3, "larch_fireproof_wood": 3, "polished_waxstone": 4, "waxstone_bricks": 4, "bog_earth_bucket": 6, "bog_earth_can": 8}.items():
        actual = load(output / f"resources/data/forestry/recipe/{recipe}.json")["result"].get("count", 1)
        if actual != expected:
            errors.append(f"{recipe}: yield {actual}, expected {expected}")
    for container, fluid in [("can", "minecraft:water"), ("wax_capsule", "minecraft:water"), ("refractory", "minecraft:water")]:
        value = load(output / f"resources/data/forestry/recipe/bog_earth_{container}.json")["key"]["X"]
        if value.get("neoforge:ingredient_type") != "forestry:fluid_container" or value.get("fluid") != {"id": fluid, "amount": 1000}:
            errors.append(f"bog_earth_{container}: wrong filled-container ingredient")
    if args.item_model_report:
        for identifier in load(args.item_model_report):
            namespace, path = identifier.split(":", 1)
            if not any((output / part / "assets" / namespace / "items" / (path + ".json")).is_file() for part in PARTS):
                errors.append(f"Runtime item model has no generated definition: {identifier}")
    if errors:
        print("\n".join(errors[:70]))
        raise SystemExit(f"Datagen validation failed: {len(errors)} errors")
    print(f"Datagen validation passed: {sum(counts.values())} JSON files, {len(referenced)} Forestry asset references; {counts}")


if __name__ == "__main__":
    main()
