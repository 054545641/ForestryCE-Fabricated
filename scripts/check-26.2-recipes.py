"""Check checked-in recipe ingredient shapes without requiring a Minecraft bootstrap."""

import json
from pathlib import Path


def ingredient(value, where):
    if isinstance(value, str) and value:
        if value in ("minecraft:air", "minecraft:chain", "#c:slimeballs"):
            raise ValueError(f"{where}: obsolete or empty ingredient: {value}")
        return
    if isinstance(value, list) and value and all(
        isinstance(item, str) and item and not item.startswith("#") for item in value
    ):
        return
    if isinstance(value, dict) and value.get("neoforge:ingredient_type"):
        if value.get("items") == "minecraft:air":
            raise ValueError(f"{where}: component ingredient must not match air")
        if value["neoforge:ingredient_type"] == "forestry:fluid_container":
            fluid = value.get("fluid", {})
            if not value.get("item") or value["item"] == "minecraft:air" or not fluid.get("id") or fluid.get("amount", 0) <= 0:
                raise ValueError(f"{where}: invalid filled container ingredient")
        if value["neoforge:ingredient_type"] == "neoforge:compound":
            children = value.get("children", [])
            if not children:
                raise ValueError(f"{where}: empty compound ingredient")
            for child in children:
                ingredient(child, where + "/children")
        return
    raise ValueError(f"{where}: unsupported 26.2 ingredient shape: {value!r}")


def recipe(data, where):
    for key, value in data.get("key", {}).items():
        ingredient(value, where + "/key/" + key)
    for field in ("ingredients", "resources"):
        for value in data.get(field, []):
            ingredient(value, where + "/" + field)
    for field in ("ingredient", "input", "resource", "box", "plan"):
        if field in data:
            if field == "input" and data.get("type") == "forestry:still":
                continue
            ingredient(data[field], where + "/" + field)
    if "recipe" in data:
        recipe(data["recipe"], where + "/recipe")
    for value in data.get("inputs", []):
        if isinstance(value, dict) and "ingredient" in value:
            ingredient(value["ingredient"], where + "/inputs/ingredient")
    output = data.get("output")
    if isinstance(output, dict) and "ingredient" in output:
        ingredient(output["ingredient"], where + "/output/ingredient")


if __name__ == "__main__":
    root = Path(__file__).resolve().parents[1]
    count = 0
    for path in sorted((root / "src").rglob("*.json")):
        if "data" in path.parts and "recipe" in path.parts:
            recipe(json.loads(path.read_text(encoding="utf-8")), str(path.relative_to(root)))
            count += 1
    print(f"26.2 ingredient shape checks passed for {count} recipe JSON files")
