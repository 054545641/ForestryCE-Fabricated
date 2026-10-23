"""Reconcile F3 acceptance evidence against the exact four release artifacts."""
import hashlib
import json
from pathlib import Path
import re
import subprocess
from datetime import datetime, timezone
import zipfile

ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "build/fabric-26.2/reports/f3/acceptance.json"


def read(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def evidence(path):
    return {"path": path.relative_to(ROOT).as_posix(), "sha256": sha256(path)}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def log(path, markers):
    text = path.read_text(encoding="utf-8-sig", errors="replace")
    for marker in markers:
        require(marker in text, f"Missing {marker} in {path}")
    return text


def main():
    if REPORT.exists():
        REPORT.unlink()
    isolation_path = ROOT / "build/fabric-26.2/reports/f3/isolation.json"
    isolation = read(isolation_path)
    require(isolation["status"] == "passed", "JAR isolation did not pass")
    production = ROOT / ".local/f3-production"
    manifest = read(production / "manifest.json")
    artifacts = []
    artifact_time = 0
    for owner, item in isolation["modules"].items():
        jar = ROOT / "build/fabric-26.2/libs" / item["jar"]
        artifact_time = max(artifact_time, jar.stat().st_mtime)
        frozen = manifest["artifacts"][item["modId"]]
        require(sha256(jar) == item["sha256"] == frozen["sha256"] == sha256(Path(frozen["path"])), f"Stale artifact: {owner}")
        with zipfile.ZipFile(jar) as archive:
            for name in archive.namelist():
                if name.endswith(".class"):
                    compiled = ROOT / "build/fabric-26.2/classes/java" / owner / name
                    require(compiled.is_file() and compiled.read_bytes() == archive.read(name), f"Stale compiled class: {name}")
        artifacts.append({"module": item["modId"], "active_sources": item["activeSources"], **evidence(jar)})
    datagen_path = ROOT / "build/fabric-26.2/reports/f3/datagen.json"
    datagen = read(datagen_path)
    require(datagen["status"] == "passed" and datagen["files"] > 15000, "Incomplete native datagen")
    for name, checksum in datagen["sha256"].items():
        for folder in ("generated/f3-resources", "generated/f3-repeated", "datagen"):
            require(sha256(ROOT / "build/fabric-26.2" / folder / name) == checksum, f"Changed datagen output: {folder}/{name}")
    resources = read(ROOT / "build/fabric-26.2/generated/f3-resources/resource-report.json")
    require(all(not item["deferredRecipes"] for item in resources.values()), "Recipes remain deferred")
    development = []
    validation = ROOT / ".local/f3-validation"
    for name, markers in {
        "build": ["FORESTRY_F3_FOUR_JARS_VERIFIED"],
        **{f"gametest-{mask}": ["required tests passed"] for mask in range(8)},
        "persistence-seed": ["FORESTRY_F2_PERSISTENCE_SEEDED"],
        "persistence-verify": ["FORESTRY_F2_PERSISTENCE_VERIFIED"],
        "server-0": ["FORESTRY_F2_SERVER_READY", "All dimensions are saved"],
        "server-7": ["FORESTRY_F2_SERVER_READY", "All dimensions are saved"],
        **{f"client-{mask}": ["FORESTRY_F3_CLIENT_UI_JEI_RENDER_PASSED", "FORESTRY_F2_ITEM_MODELS_VERIFIED",
                               "FORESTRY_F2_RELOAD_RECONNECT_PASSED", "Using graphics backend OpenGL"] for mask in (0, 7)},
        "client-7-vulkan": ["FORESTRY_F3_CLIENT_UI_JEI_RENDER_PASSED", "FORESTRY_F2_ITEM_MODELS_VERIFIED",
                            "FORESTRY_F2_RELOAD_RECONNECT_PASSED", "Using graphics backend Vulkan"],
    }.items():
        path = validation / f"{name}.log"
        require(path.stat().st_mtime >= artifact_time, f"Development log predates the final artifacts: {path}")
        text = log(path, ["BUILD SUCCESSFUL", *markers])
        count = re.search(r"All (\d+) required tests passed", text)
        development.append({"name": name, "tests": int(count[1]) if count else None, **evidence(path)})
    results = read(production / "results.json")
    cases = {(mask, False, False) for mask in range(8)} | {(0, False, True), (7, False, True), (7, True, False), (7, True, True)}
    require(len(results) == len(cases) * 2, "Incomplete independent production matrix")
    require({(item["mask"], item["server_optional"], item["client_optional"], item["phase"]) for item in results}
            == {(*case, phase) for case in cases for phase in ("initial", "restart")}, "Wrong production matrix")
    native = []
    for item in results:
        require(item["status"] == "passed", "Production case failed")
        require(item["client"] == (item["mask"] in (0, 7) and item["phase"] == "initial"), "Missing native client scenario")
        directory = Path(item["directory"])
        path = directory / "server" / f"console-{item['phase']}.log"
        require(sha256(path) == item["log_sha256"], "Production log changed")
        text = log(path, ["Done (", "All dimensions are saved", "Loaded 69 bee species", "Loaded 50 tree species"])
        require(not any(mod in text for mod in ("forestry_f2_tests", "forestry_datagen", "forestry_release_probe")), "Test mod leaked into production server")
        selected = [manifest["artifacts"]["forestry"], *manifest["required"]]
        for index, mod in enumerate(("forestrybutterflies", "forestryfarms", "forestrymail")):
            if item["mask"] & (1 << index):
                selected.append(manifest["artifacts"][mod])
        if item["server_optional"]:
            selected += manifest["optional"]
        mods = directory / "server/mods"
        require({jar.name for jar in mods.glob("*.jar")} == {Path(entry["path"]).name for entry in selected}, "Unexpected production server mod")
        for entry in selected:
            require(sha256(mods / Path(entry["path"]).name) == entry["sha256"], "Production copy differs from frozen artifact")
        arguments = read(directory / "server/launch.json")
        require("-Dfabric.development=false" in arguments and not any("devlaunch" in argument or "minecraft-merged" in argument for argument in arguments), "Development launcher used for release")
        result = {"modules": item["mask"], "server_integrations": item["server_optional"], "client_integrations": item["client_optional"],
                  "phase": item["phase"], "client": item["client"], **evidence(path)}
        if item["client"]:
            client_path = directory / "client/console.log"
            log(client_path, ["FORESTRY_F3_RELEASE_CLIENT_PASSED", "Using graphics backend OpenGL"])
            require((directory / "client/client.passed").exists() and not (directory / "client/client.failed").exists(), "Native client did not pass")
            client_mods = directory / "client/mods"
            client_entries = [entry for entry in selected if entry not in manifest["optional"]] + [manifest["probe"]]
            if item["client_optional"]:
                client_entries += manifest["optional"]
            require({jar.name for jar in client_mods.glob("*.jar")} == {Path(entry["path"]).name for entry in client_entries}, "Unexpected production client mod")
            for entry in client_entries:
                require(sha256(client_mods / Path(entry["path"]).name) == entry["sha256"], "Client copy differs from frozen artifact")
            result["client_log"] = evidence(client_path)
        native.append(result)
    upstream_path = ROOT / ".local/f3-upstream/audit/audit.json"
    upstream = read(upstream_path)
    versions = [item for item in upstream if item["query"] == "versions"]
    require({item["project"] for item in versions} == {"patchouli", "kubejs", "rhino", "curios"}, "Incomplete upstream audit")
    require(all(item["http_status"] == 200 and not item["declared_candidates"] for item in versions), "Revisit external integration availability")
    clean_path = ROOT / ".local/f3-clean-build-data.log"
    log(clean_path, ["> Task :clean", "BUILD SUCCESSFUL", "FORESTRY_F3_DATAGEN_VERIFIED"])
    report = {"phase": "F3 / G3", "status": "passed", "verified_at": datetime.now(timezone.utc).isoformat(), "version": manifest["version"],
              "branch": subprocess.check_output(["git", "branch", "--show-current"], cwd=ROOT, text=True).strip(),
              "base_commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
              "uncommitted_worktree": bool(subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT, text=True).strip()),
              "clean_build": evidence(clean_path),
              "artifacts": artifacts, "isolation": evidence(isolation_path), "datagen": {"files": datagen["files"], **evidence(datagen_path)},
              "content": resources, "development": development, "production": native, "upstream": evidence(upstream_path),
              "scope": {"worlds": "New Fabric 26.2 worlds; cross-JVM F2 persistence fixtures", "network": "Separate localhost JVMs; offline test identity, not authenticated online login",
                        "patchouli_kubejs_rhino": "Inherited temporary closure; installation path not verified", "curios": "No Fabric 26.2 target; native equipment supported, extra accessory slots not ported"}}
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"FORESTRY_F3_ACCEPTANCE_VERIFIED {REPORT}")


if __name__ == "__main__":
    main()
