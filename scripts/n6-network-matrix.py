"""Exercise real release-jar clients against isolated localhost dedicated servers."""
import json
import os
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
CASES = [("none", "none", False), ("none", "jei", False), ("jei", "none", False),
         ("all", "all", False), ("none", "curios", True), ("curios", "none", True)]


def start(side, integration, reject, log):
    args = ["cmd.exe", "/d", "/c", str(ROOT / "gradlew-local.bat"), f":runN6Release{side.title()}",
            "-Pn6Network=true", "-Pn6Modules=7", f"-Pn6Integrations={integration}",
            f"-Pn6ExpectDisconnect={str(reject).lower()}", "--offline", "--console=plain", "--no-daemon"]
    with log.open("w", encoding="utf-8") as stream:
        return subprocess.Popen(args, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT,
                                creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)


def wait_ready(process, log):
    deadline = time.monotonic() + 180
    while time.monotonic() < deadline:
        if "FORESTRY_N6_NETWORK_LISTENING" in log.read_text(encoding="utf-8", errors="replace"):
            return
        if process.poll() is not None:
            raise RuntimeError(f"Server exited before ready; inspect {log}")
        time.sleep(1)
    raise TimeoutError(f"Server startup timeout: {log}")


def main():
    results = []
    for server_mods, client_mods, reject in CASES:
        name = f"server-{server_mods}-client-{client_mods}"
        server_log = ROOT / ".local" / f"n6-net-{name}-server.log"
        client_log = ROOT / ".local" / f"n6-net-{name}-client.log"
        stop = ROOT / "run/26.2" / f"n6-server-7-{server_mods}-network/n6.network.stop"
        server = start("server", server_mods, reject, server_log)
        client = None
        try:
            wait_ready(server, server_log)
            client = start("client", client_mods, reject, client_log)
            if client.wait(timeout=300) != 0:
                raise RuntimeError(f"Client failed; inspect {client_log}")
            result = (ROOT / "run/26.2" / f"n6-client-7-{client_mods}-network/n6.passed").read_text().strip()
            stop.write_text("Client assertions complete\n", encoding="utf-8")
            if server.wait(timeout=60) != 0:
                raise RuntimeError(f"Server failed; inspect {server_log}")
            results.append(dict(case=name, expected_disconnect=reject, result=result))
            print(json.dumps(results[-1]), flush=True)
            (ROOT / ".local/n6-network-results.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
        finally:
            if server.poll() is None and stop.parent.exists():
                stop.write_text("Stopping isolated probe after client run\n", encoding="utf-8")
            for child in (client, server):
                if child is not None and child.poll() is None:
                    try:
                        child.wait(timeout=30)
                    except subprocess.TimeoutExpired:
                        # --no-daemon gives each launch its own disposable process tree.
                        subprocess.run(["taskkill", "/PID", str(child.pid), "/T", "/F"], capture_output=True)


if __name__ == "__main__":
    main()
