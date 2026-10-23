"""Record official Fabric 26.2 integration discovery without approving compatibility."""
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / ".local/f3-upstream/audit"
PROJECTS = ("patchouli", "kubejs", "rhino", "curios")
SOURCES = {"patchouli": "VazkiiMods/Patchouli", "kubejs": "kube-mods/kubejs", "rhino": "kube-mods/rhino", "curios": "TheIllusiveC4/Curios"}


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    evidence = []
    for project in PROJECTS:
        parameters = urlencode({"game_versions": json.dumps(["26.2"]), "loaders": json.dumps(["fabric"])})
        queries = {
            "versions": f"https://api.modrinth.com/v2/project/{project}/version?{parameters}",
            "branches": f"https://api.github.com/repos/{SOURCES[project]}/branches?per_page=100",
        }
        for kind, url in queries.items():
            record = {"project": project, "query": kind, "url": url, "queried_at": datetime.now(timezone.utc).isoformat()}
            try:
                with urlopen(Request(url, headers={"User-Agent": "Forestry-F3-Integration-Audit"}), timeout=30) as response:
                    body = response.read()
                    record["http_status"] = response.status
                rows = json.loads(body)
                if not isinstance(rows, list):
                    raise ValueError("Expected version/branch array")
                if kind == "branches" and len(rows) == 100:
                    raise ValueError("Branch pagination required")
                if kind == "versions":
                    if any("26.2" not in row["game_versions"] or "fabric" not in row["loaders"] for row in rows):
                        raise ValueError("Incorrect target returned")
                    record["declared_candidates"] = [{key: row[key] for key in ("id", "version_number", "game_versions", "loaders")} for row in rows]
                else:
                    record["branches"] = {row["name"]: row["commit"]["sha"] for row in rows}
                destination = OUTPUT / f"{project}-{kind}.json"
                destination.write_bytes(body)
                record["sha256"] = hashlib.sha256(body).hexdigest()
            except Exception as exception:
                record["error"] = str(exception)
            evidence.append(record)
            print(json.dumps(record, ensure_ascii=False), flush=True)
    (OUTPUT / "audit.json").write_text(json.dumps(evidence, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    if any("error" in record for record in evidence):
        raise SystemExit("Upstream audit is incomplete; no absence claim is authorized")


if __name__ == "__main__":
    main()
