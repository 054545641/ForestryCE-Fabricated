"""Run native BC/TR energy interoperability scenarios using verified release JARs."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MODULES = ("main", "butterflies", "farms", "mail")


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def evidence(path):
    path = Path(path).resolve()
    return {"path": str(path), "sha256": sha(path)}


def save(path, data):
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def nested_compile_dependencies(path, target):
    extracted = []

    def visit(data):
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            metadata = json.loads(archive.read("fabric.mod.json"))
            for declaration in metadata.get("jars", []):
                name = declaration["file"]
                content = archive.read(name)
                destination = target / (hashlib.sha256(content).hexdigest()[:16] + "-" + Path(name).name)
                destination.write_bytes(content)
                extracted.append(destination)
                visit(content)

    visit(path.read_bytes())
    return extracted


def compile_probe(args, runtime, core, fabric, run):
    dependencies = run / "compile-dependencies"
    classes = run / "probe-classes"
    dependencies.mkdir()
    classes.mkdir()
    classpath = [*runtime["client_classpath"], str(core), str(args.bc), str(args.tr)]
    for artifact in (fabric, core, args.tr, args.bc):
        classpath.extend(str(path) for path in nested_compile_dependencies(artifact, dependencies))
    sources = sorted((ROOT / "scripts/energy-interop").glob("*.java"))
    arguments = ["--release", "25", "-encoding", "UTF-8", "-classpath", os.pathsep.join(dict.fromkeys(classpath)), "-d", str(classes), *map(str, sources)]
    argument_file = run / "javac.args"
    argument_file.write_text("\n".join(json.dumps(value) for value in arguments), encoding="utf-8")
    javac = args.java.with_name("javac.exe" if os.name == "nt" else "javac")
    with (run / "compile.log").open("w", encoding="utf-8") as log:
        subprocess.run([str(javac), "@" + str(argument_file)], check=True, stdout=log, stderr=subprocess.STDOUT, timeout=120)
    probe = run / "forestry-energy-interop-probe.jar"
    metadata = {"schemaVersion": 1, "id": "forestry_energy_interop_probe", "version": "1.0.0", "environment": "server",
                "entrypoints": {"main": ["energyinterop.EnergyInteropProbe"]},
                "depends": {"forestry": "*", "fabric-api": "*", "team_reborn_energy": "^5.0.0"}}
    with zipfile.ZipFile(probe, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("fabric.mod.json", json.dumps(metadata))
        for path in classes.rglob("*.class"):
            archive.write(path, path.relative_to(classes).as_posix())
    return probe


def stop(process):
    if process.poll() is not None:
        return
    try:
        process.stdin.write("stop\n")
        process.stdin.flush()
        process.wait(timeout=60)
    except (OSError, subprocess.TimeoutExpired):
        process.kill()
        process.wait(timeout=10)


def run_case(args, runtime, run, name, artifacts, probe, index):
    game = run / name
    mods = game / "mods"
    mods.mkdir(parents=True)
    selected = [*artifacts, probe]
    if name in ("bc-only", "both"):
        selected.append(args.bc)
    if name in ("tr-only", "both"):
        selected.append(args.tr)
    for artifact in selected:
        shutil.copy2(artifact, mods / artifact.name)
    for path in mods.glob("*.jar"):
        with zipfile.ZipFile(path) as archive:
            if json.loads(archive.read("fabric.mod.json"))["id"] == "team_reborn_energy":
                raise ValueError("A standalone Energy JAR would hide missing nested dependencies")
    shutil.copy2(args.eula_from, game / "eula.txt")
    (game / "server.properties").write_text(
        f"server-ip=127.0.0.1\nserver-port={args.port + index}\nonline-mode=false\nenforce-secure-profile=false\n"
        "level-name=energy-interop\nlevel-type=minecraft:flat\ngenerate-structures=false\n"
        'generator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1}],"structure_overrides":[]}\n'
        "view-distance=3\nsimulation-distance=3\nmax-players=1\npause-when-empty-seconds=0\n", encoding="utf-8")
    arguments = [str(args.java), "-Xmx2G", "-Dfabric.development=false", "--enable-native-access=ALL-UNNAMED",
                 "--sun-misc-unsafe-memory-access=allow", f"-DenergyInterop.bc={str(name != 'tr-only').lower()}",
                 f"-DenergyInterop.tr={str(name != 'bc-only').lower()}", "-cp", os.pathsep.join(runtime["server_classpath"]),
                 "net.fabricmc.loader.impl.launch.knot.KnotServer", "--nogui"]
    save(game / "launch.json", arguments)
    result = {"case": name, "mods": [evidence(path) for path in sorted(mods.glob("*.jar"))], "game": str(game)}
    save(game / "manifest.json", result)
    report = game / "energy-interop-results.json"
    log_path = game / "console.log"
    with log_path.open("w", encoding="utf-8") as log:
        process = subprocess.Popen(arguments, cwd=game, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT,
                                   text=True, creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
        print(json.dumps({"event": "started", "case": name, "pid": process.pid, "log": str(log_path)}), flush=True)
        try:
            deadline = time.monotonic() + args.timeout
            while not report.exists():
                if process.poll() is not None:
                    raise RuntimeError(f"Server exited with {process.returncode} before probe completed")
                if time.monotonic() > deadline:
                    raise TimeoutError(f"No probe results after {args.timeout} seconds")
                time.sleep(0.5)
            measurements = json.loads(report.read_text(encoding="utf-8"))
            result["probe"] = measurements
            stop(process)
            if process.returncode != 0:
                raise RuntimeError(f"Server did not stop cleanly: {process.returncode}")
            text = log_path.read_text(encoding="utf-8", errors="replace")
            bad = ("NoSuchMethodError", "NoClassDefFoundError", "Incompatible mod set!", "Mixin apply for mod",
                   "Could not execute entrypoint stage", "Encountered an unexpected exception", "Exception in server tick loop")
            errors = [marker for marker in bad if marker in text]
            if errors:
                raise RuntimeError(f"Runtime errors: {errors}")
            if measurements["status"] != "passed":
                raise RuntimeError("Energy probe reported a failure")
            result["status"] = "passed"
        except Exception as error:
            result.update(status="failed", error=str(error))
        finally:
            stop(process)
    result["log"] = evidence(log_path)
    save(game / "result.json", result)
    print(json.dumps({"event": "finished", "case": name, "status": result["status"], "error": result.get("error")}), flush=True)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--runtime-manifest", type=Path, required=True, help="Prepared native Fabric runtime manifest from test-fabric-release.py")
    parser.add_argument("--bc", type=Path, required=True)
    parser.add_argument("--tr", type=Path, required=True)
    parser.add_argument("--eula-from", type=Path, required=True, help="Existing user-accepted EULA file")
    parser.add_argument("--java", type=Path, default=ROOT / ".local/jdk25/bin" / ("java.exe" if os.name == "nt" else "java"))
    parser.add_argument("--fabric-api", type=Path)
    parser.add_argument("--port", type=int, default=25591)
    parser.add_argument("--timeout", type=int, default=300)
    parser.add_argument("--cases", nargs="+", choices=("bc-only", "tr-only", "both"), default=["bc-only", "tr-only", "both"])
    args = parser.parse_args()
    for field in ("runtime_manifest", "bc", "tr", "eula_from", "java"):
        setattr(args, field, getattr(args, field).resolve(strict=True))
    if "eula=true" not in args.eula_from.read_text().splitlines():
        raise ValueError("An existing user-accepted EULA is required; this script never accepts it")
    runtime = json.loads(args.runtime_manifest.read_text())
    for entry in dict.fromkeys(runtime["server_classpath"] + runtime["client_classpath"]):
        if not Path(entry).is_file():
            raise FileNotFoundError(entry)
    for entry in runtime["server_classpath"]:
        with zipfile.ZipFile(entry) as archive:
            if "team/reborn/energy/api/EnergyStorage.class" in archive.namelist():
                raise ValueError("Energy leaked onto the native launch classpath")
    isolation = json.loads((ROOT / "build/fabric-26.2/reports/f3/isolation.json").read_text())
    artifacts = []
    for module in MODULES:
        expected = isolation["modules"][module]
        path = ROOT / "build/fabric-26.2/distribution" / expected["jar"]
        if sha(path) != expected["sha256"]:
            raise ValueError(f"Distribution differs from verified artifact: {path}")
        artifacts.append(path)
    if args.fabric_api:
        fabric = args.fabric_api.resolve(strict=True)
    else:
        candidates = list((ROOT / ".local/gradle-fabric-26.2/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api").glob("*/*/fabric-api-*.jar"))
        properties = dict(line.split("=", 1) for line in (ROOT / "gradle.properties").read_text().splitlines() if "=" in line and not line.startswith("#"))
        candidates = [path for path in candidates if path.name == f"fabric-api-{properties['fabricApiVersion']}.jar"]
        if len(candidates) != 1:
            raise ValueError("Provide --fabric-api with the correct Fabric API JAR")
        fabric = candidates[0]
    run = ROOT / ".local/energy-interop/runs" / str(time.time_ns())
    run.mkdir(parents=True)
    summary = {"status": "running", "runtimeManifest": evidence(args.runtime_manifest), "artifacts": [evidence(path) for path in artifacts],
               "runtimeClasspath": [evidence(path) for path in runtime["server_classpath"]], "cases": []}
    save(run / "results.json", summary)
    (ROOT / ".local/energy-interop/latest-run.txt").write_text(str(run), encoding="utf-8")
    probe = compile_probe(args, runtime, artifacts[0], fabric, run)
    summary["probeArtifact"] = evidence(probe)
    summary["probeSources"] = [evidence(path) for path in sorted((ROOT / "scripts/energy-interop").glob("*.java"))]
    for index, name in enumerate(args.cases):
        summary["cases"].append(run_case(args, runtime, run, name, [*artifacts, fabric], probe, index))
        save(run / "results.json", summary)
    summary["status"] = "passed" if all(case["status"] == "passed" for case in summary["cases"]) else "failed"
    save(run / "results.json", summary)
    print(json.dumps({"status": summary["status"], "results": str(run / "results.json")}), flush=True)
    return 0 if summary["status"] == "passed" else 1


if __name__ == "__main__":
    raise SystemExit(main())
