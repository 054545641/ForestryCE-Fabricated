"""Prepare and test frozen Forestry jars using the official, non-development NeoForge server.

The run action requires explicit user acceptance of Minecraft's EULA. Preparation does not accept it.
"""
import argparse
from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / ".local/n6-production"
RUNTIME = BASE / "runtime"
JAVA = ROOT / ".local/jdk25/bin/java.exe"
MODULES = ["forestry", "forestrybutterflies", "forestryfarms", "forestrymail"]
SCENARIOS = [(mask, "none") for mask in range(8)] + [(0, "all"), (7, "all")]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def prepare():
    artifacts = BASE / "artifacts"
    artifacts.mkdir(parents=True, exist_ok=True)
    properties = dict(line.split("=", 1) for line in (ROOT / "gradle.properties").read_text().splitlines()
                      if "=" in line and not line.startswith("#"))
    version = properties["forestryVersion"]
    names = [f"{mod}-26.2-{version}.jar" for mod in MODULES]
    hashes = {}
    for name in names:
        source = ROOT / "build/26.2/libs" / name
        with zipfile.ZipFile(source) as jar:
            forbidden = [entry for entry in jar.namelist() if entry.startswith(("forestry/n6/", "forestry/lifecycle/", "forestry/gametest/", "thedarkcolour/modkit/", "mezz/jei/", "top/theillusivec4/"))]
            if forbidden:
                raise RuntimeError(f"Non-production contents: {forbidden[:4]}")
        shutil.copy2(source, artifacts / name)
        hashes[name] = digest(source)
    optional = []
    for group, artifact, dep_version in [("mezz.jei", "jei-26.2-neoforge", properties["jeiVersion"]),
                                          ("top.theillusivec4.curios", "curios-neoforge", properties["curiosVersion"])]:
        candidates = list((ROOT / ".local/gradle-26.2/caches/modules-2/files-2.1" / group / artifact / dep_version).rglob(f"{artifact}-{dep_version}.jar"))
        if len(candidates) != 1:
            raise RuntimeError(f"Expected exactly one locked dependency: {artifact}")
        optional.append(candidates[0])
    args = (RUNTIME / "libraries/net/neoforged/neoforge/26.2.0.88/win_args.txt").read_text()
    args = args.replace("libraries/", (RUNTIME / "libraries").as_posix() + "/")
    args = args.replace("-DlibraryDirectory=libraries", "-DlibraryDirectory=" + (RUNTIME / "libraries").as_posix())
    for mask, integration in SCENARIOS:
        game = BASE / "instances" / f"{mask}-{integration}"
        mods = game / "mods"
        mods.mkdir(parents=True, exist_ok=True)
        chosen = [artifacts / names[0]] + [artifacts / names[i + 1] for i in range(3) if mask & (1 << i)]
        if integration == "all":
            chosen += optional
        expected = {path.name for path in chosen}
        unexpected = {path.name for path in mods.glob("*.jar")} - expected
        if unexpected:
            raise RuntimeError(f"Unexpected mods in managed instance {game}: {unexpected}")
        for source in chosen:
            shutil.copy2(source, mods / source.name)
        (game / "launch.args").write_text("-Xmx2G\n" + args + "\n--nogui\n", encoding="utf-8")
        (game / "server.properties").write_text(
            "server-ip=127.0.0.1\nserver-port=25578\nonline-mode=false\nenforce-secure-profile=false\n"
            "level-name=n6-acceptance\nlevel-type=minecraft:flat\ngenerate-structures=false\n"
            'generator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"structure_overrides":[]}\n'
            "view-distance=2\nsimulation-distance=2\nmax-players=2\npause-when-empty-seconds=0\n", encoding="utf-8")
    manifest = dict(minecraft="26.2", neoforge="26.2.0.88", version=version, jars=hashes,
                    optional={path.name: digest(path) for path in optional})
    (BASE / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print(json.dumps(dict(prepared=len(SCENARIOS), artifacts=hashes), indent=2))


def wait_for(process, log, predicate, timeout=180):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        text = log.read_text(encoding="utf-8", errors="replace")
        if predicate(text):
            return text
        if process.poll() is not None:
            raise RuntimeError(f"Server stopped before assertions completed: {log}")
        time.sleep(1)
    raise TimeoutError(f"Server stage timed out: {log}")


def run(accept_eula):
    if not accept_eula:
        raise RuntimeError("User must accept https://www.minecraft.net/eula before --accept-eula is supplied")
    manifest = json.loads((BASE / "manifest.json").read_text())
    results = []
    (BASE / "results.json").write_text("[]\n", encoding="utf-8")
    for mask, integration in SCENARIOS:
        game = BASE / "instances" / f"{mask}-{integration}"
        selected = [MODULES[0]] + [MODULES[i + 1] for i in range(3) if mask & (1 << i)]
        expected = {f"{mod}-26.2-{manifest['version']}.jar": manifest["jars"][f"{mod}-26.2-{manifest['version']}.jar"] for mod in selected}
        if integration == "all":
            expected.update(manifest["optional"])
        actual = {mod.name for mod in (game / "mods").glob("*.jar")}
        if actual != expected.keys():
            raise RuntimeError(f"Wrong module set in {game}: missing={expected.keys() - actual}, unexpected={actual - expected.keys()}")
        for mod in (game / "mods").glob("*.jar"):
            if digest(mod) != expected.get(mod.name):
                raise RuntimeError(f"Snapshot differs from audited artifact: {mod}")
        (game / "eula.txt").write_text("eula=true\n", encoding="utf-8")
        for phase in ("initial", "restart"):
            log = game / f"console-{phase}.log"
            with log.open("w", encoding="utf-8") as output:
                process = subprocess.Popen([str(JAVA), "@" + str(game / "launch.args")], cwd=game,
                    stdin=subprocess.PIPE, stdout=output, stderr=subprocess.STDOUT, text=True,
                    creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
            try:
                text = wait_for(process, log, lambda value: 'Done (' in value)
                if "(DEDICATED_SERVER in DEV)" in text or "forestryn6test" in text or "forestrylifecycletest" in text:
                    raise RuntimeError(f"Development runtime leaked: {log}")
                before = text.count("Loaded 50 tree species")
                process.stdin.write("reload\n"); process.stdin.flush()
                # The production species loader runs once at boot and again for the real /reload.
                wait_for(process, log, lambda value: value.count("Loaded 50 tree species") > before)
                process.stdin.write("save-all flush\nstop\n"); process.stdin.flush()
                if process.wait(timeout=60) != 0:
                    raise RuntimeError(f"Nonzero dedicated server exit: {log}")
                text = log.read_text(encoding="utf-8", errors="replace")
                if "All dimensions are saved" not in text or not (game / "n6-acceptance/level.dat").exists():
                    raise RuntimeError(f"World save not confirmed: {log}")
                results.append(dict(mask=mask, integrations=integration, phase=phase, result="passed"))
                (BASE / "results.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
                print(json.dumps(results[-1]), flush=True)
            finally:
                if process.poll() is None:
                    try:
                        process.stdin.write("stop\n"); process.stdin.flush(); process.wait(timeout=20)
                    except (OSError, subprocess.TimeoutExpired):
                        process.kill(); process.wait()


def audit():
    """Independently check completed logs, loaded modules, reload data and frozen artifacts."""
    manifest = json.loads((BASE / "manifest.json").read_text())
    results = json.loads((BASE / "results.json").read_text())
    expected_runs = {(mask, integration, phase) for mask, integration in SCENARIOS for phase in ("initial", "restart")}
    passed_runs = {(row["mask"], row["integrations"], row["phase"]) for row in results if row["result"] == "passed"}
    if len(results) != len(expected_runs) or passed_runs != expected_runs:
        raise RuntimeError("The native server matrix is incomplete")
    for name, sha256 in manifest["jars"].items():
        for path in (BASE / "artifacts" / name, ROOT / "build/26.2/libs" / name):
            if digest(path) != sha256:
                raise RuntimeError(f"Audited artifact changed: {path}")
    rows = []
    for mask, integration in SCENARIOS:
        game = BASE / "instances" / f"{mask}-{integration}"
        selected = {MODULES[0]} | {MODULES[i + 1] for i in range(3) if mask & (1 << i)}
        expected = {f"{mod}-26.2-{manifest['version']}.jar": manifest["jars"][f"{mod}-26.2-{manifest['version']}.jar"] for mod in selected}
        if integration == "all":
            expected.update(manifest["optional"])
        actual = {path.name: digest(path) for path in (game / "mods").glob("*.jar")}
        if actual != expected:
            raise RuntimeError(f"Frozen module set changed: {game}")
        if not (game / "n6-acceptance/level.dat").is_file() or list((game / "crash-reports").glob("*.txt")):
            raise RuntimeError(f"Missing saved world or unexpected crash report: {game}")
        for phase in ("initial", "restart"):
            log = game / f"console-{phase}.log"
            text = log.read_text(encoding="utf-8", errors="replace")
            if "(DEDICATED_SERVER in PROD)" not in text or "in DEV)" in text:
                raise RuntimeError(f"Not an official production launch: {log}")
            mod_section = text.split("Mod List:\n", 1)[1].split("\n[", 1)[0]
            loaded = set(re.findall(r"\(([\w.-]+)\)\s*$", mod_section, re.MULTILINE))
            if {mod for mod in loaded if mod.startswith("forestry")} != selected:
                raise RuntimeError(f"Wrong loaded Forestry modules: {loaded}")
            if any((mod in loaded) != (integration == "all") for mod in ("jei", "curios")) or "modkit" in loaded:
                raise RuntimeError(f"Wrong loaded optional/development modules: {loaded}")
            if {"patchouli", "kubejs", "rhino"} & loaded:
                raise RuntimeError(f"Unavailable G2 integration present in the production matrix: {loaded}")
            for marker in ("Done (", "Reloading!", "Saved the game", "All dimensions are saved", "Closing FML Loader"):
                if marker not in text:
                    raise RuntimeError(f"Missing {marker!r}: {log}")
            species = {kind: [int(value) for value in re.findall(rf"Loaded (\d+) {kind} species", text)] for kind in ("bee", "tree", "butterfly")}
            if species != {"bee": [69, 69], "tree": [50, 50], "butterfly": [35, 35] if mask & 1 else []}:
                raise RuntimeError(f"Wrong boot/reload species data: {log}: {species}")
            recipes = [int(value) for value in re.findall(r"Loaded (\d+) recipes", text)]
            if len(recipes) != 2 or recipes[0] == 0 or recipes[0] != recipes[1]:
                raise RuntimeError(f"Recipe count changed across reload: {log}: {recipes}")
            observations = []
            lines = text.splitlines()
            for index, line in enumerate(lines):
                if "/ERROR]" not in line and " ERROR " not in line:
                    continue
                if "[os.dr.wi.re.HkeyPerformanceDataUtil/]" in line and "Perflib 009" in line:
                    observations.append("Windows Perflib English performance counters unavailable")
                elif "An exception occurred processing Appender DebugFile" in line and index + 1 < len(lines) and re.fullmatch(
                    r"org\.apache\.logging\.log4j\.core\.appender\.AppenderLoggingException: java\.lang\.NoClassDefFoundError: Could not initialize class io\.netty\.channel\.(kqueue|epoll)\.Native", lines[index + 1]
                ):
                    observations.append("Upstream debug appender failed while formatting an unsupported native transport probe")
                else:
                    raise RuntimeError(f"Unreviewed error in {log}: {line}")
            rows.append(dict(mask=mask, integrations=integration, phase=phase, mode="PROD", loaded_mods=sorted(loaded),
                species=species, recipes=recipes, log_sha256=digest(log), observations=sorted(set(observations))))
    report = dict(audited_at=datetime.now().astimezone().isoformat(), result="passed", run_count=len(rows), artifacts=manifest, runs=rows)
    (BASE / "audit.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps(dict(result="passed", native_runs=len(rows), artifact_hashes_verified=True,
                         unavailable_integrations_absent=True,
                         observations=sorted({value for row in rows for value in row["observations"]})), indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["prepare", "run", "audit"])
    parser.add_argument("--accept-eula", action="store_true")
    args = parser.parse_args()
    if args.action == "prepare":
        prepare()
    elif args.action == "run":
        run(args.accept_eula)
    else:
        audit()
