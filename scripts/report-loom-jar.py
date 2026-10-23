import argparse
import hashlib
import json
from pathlib import Path
import zipfile


def describe(path):
    with zipfile.ZipFile(path) as archive:
        entries = {
            entry.filename: hashlib.sha256(archive.read(entry)).hexdigest()
            for entry in archive.infolist()
            if not entry.is_dir()
        }
        canonical = ''.join(f'{name}\0{digest}\n' for name, digest in sorted(entries.items()))
        return {
            'file': path.name,
            'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
            'contents_sha256': hashlib.sha256(canonical.encode('utf-8')).hexdigest(),
            'zip_platforms': sorted({entry.create_system for entry in archive.infolist()}),
            'zip_timestamps': sorted({entry.date_time for entry in archive.infolist()}),
            'entries': entries,
        }


def main():
    parser = argparse.ArgumentParser(description='Reports hashes only, without copying Minecraft binaries.')
    parser.add_argument('--minecraft-maven', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    jars = sorted(args.minecraft_maven.glob('net/minecraft/minecraft-merged-*/*/*.jar'))
    if not jars:
        raise SystemExit('No generated Minecraft JARs found in the specified Loom repository')
    reports = [describe(path) for path in jars]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(reports, indent=2) + '\n', encoding='utf-8')
    for report in reports:
        print(f"{report['file']}: sha256={report['sha256']} contents_sha256={report['contents_sha256']}")


if __name__ == '__main__':
    main()
