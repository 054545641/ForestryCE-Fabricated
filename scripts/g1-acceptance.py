"""Isolated production-JAR acceptance runner; requires Python 3.11+ and local JDK 21."""
import argparse
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import shutil
import socket
import struct
import subprocess
import time
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / '.local/g1-production'
RUNTIME = BASE / 'runtime'
JAVA = ROOT / '.local/jdk21/bin/java.exe'
CACHE = ROOT / '.local/gradle/caches/modules-2/files-2.1'
SCENARIOS = ['core-without', 'all-without', 'core-with', 'all-with']
DEPENDENCIES = [
    'architectury-neoforge-13.0.8.jar', 'rhino-2101.2.7-build.81.jar',
    'kubejs-neoforge-2101.7.2-build.368.jar', 'curios-neoforge-9.5.1+1.21.1.jar',
    'ponder-neoforge-1.0.87+mc1.21.1.jar', 'jei-1.21.1-neoforge-19.27.0.340.jar',
    'flywheel-neoforge-1.21.1-1.0.4.jar',
]


def load(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def install():
    BASE.mkdir(parents=True, exist_ok=True)
    installer = BASE / 'installer.jar'
    if not installer.exists():
        urllib.request.urlretrieve('https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.230/neoforge-21.1.230-installer.jar', installer)
    expected = '88f4a6efd54049b7c14f4588e879a964d1868688fd52efb64b29dd03ad991ef1'
    if hashlib.sha256(installer.read_bytes()).hexdigest() != expected:
        raise RuntimeError('NeoForge installer SHA-256 mismatch')
    RUNTIME.mkdir(exist_ok=True)
    with open(BASE / 'install-server.log', 'w') as log:
        subprocess.run([str(JAVA), '-jar', str(installer), '--installServer', str(RUNTIME)], cwd=BASE, stdout=log, stderr=subprocess.STDOUT, check=True)
    version = RUNTIME / 'versions/1.21.1'
    version.mkdir(parents=True, exist_ok=True)
    cache = ROOT / '.local/gradle/caches/neoformruntime/artifacts'
    shutil.copy2(cache / 'minecraft_1.21.1_client.jar', version / '1.21.1.jar')
    shutil.copy2(cache / 'minecraft_1.21.1_version_manifest.json', version / '1.21.1.json')
    profiles = RUNTIME / 'launcher_profiles.json'
    if not profiles.exists():
        profiles.write_text('{}', encoding='utf-8')
    with open(BASE / 'install-client.log', 'w') as log:
        subprocess.run([str(JAVA), '-jar', str(installer), '--installClient', str(RUNTIME)], cwd=BASE, stdout=log, stderr=subprocess.STDOUT, check=True)
    print('NeoForge production runtime installed')


def allowed(item):
    result = not item.get('rules')
    for rule in item.get('rules', []):
        os_rule = rule.get('os', {})
        matches = os_rule.get('name', 'windows') == 'windows'
        matches &= os_rule.get('arch', 'amd64') in ('amd64', 'x86_64')
        matches &= not rule.get('features')
        if matches:
            result = rule['action'] == 'allow'
    return result


def prepare():
    vanilla = load(RUNTIME / 'versions/1.21.1/1.21.1.json')
    neo = load(RUNTIME / 'versions/neoforge-21.1.230/neoforge-21.1.230.json')
    cached = {}
    for path in CACHE.rglob('*.jar'):
        cached.setdefault(path.name, []).append(path)

    def fetch(lib):
        artifact = lib.get('downloads', {}).get('artifact')
        if not artifact or not allowed(lib):
            return
        target = RUNTIME / 'libraries' / artifact['path']
        expected = artifact.get('sha1')
        def valid(path):
            return path.exists() and (not expected or hashlib.sha1(path.read_bytes()).hexdigest() == expected)
        if valid(target):
            return
        target.parent.mkdir(parents=True, exist_ok=True)
        for candidate in cached.get(target.name, []):
            if valid(candidate):
                shutil.copy2(candidate, target)
                return
        urllib.request.urlretrieve(artifact['url'], target)
        if not valid(target):
            raise RuntimeError(f'Hash mismatch: {target}')

    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        list(pool.map(fetch, vanilla['libraries'] + neo['libraries']))
    snapshot = BASE / 'artifacts'
    snapshot.mkdir(exist_ok=True)
    hashes = {}
    for source in (ROOT / 'build/libs').glob('*.jar'):
        if any(source.name.endswith(f'-{suffix}.jar') for suffix in ('api', 'sources', 'javadoc')):
            continue
        target = snapshot / source.name
        if target.exists() and target.read_bytes() != source.read_bytes():
            raise RuntimeError('Frozen artifact differs; use a new acceptance root for a new build')
        shutil.copy2(source, target)
        hashes[source.name] = hashlib.sha256(source.read_bytes()).hexdigest()
    (BASE / 'artifacts.json').write_text(json.dumps(hashes, indent=2), encoding='utf-8')
    for scenario in SCENARIOS:
        for side in ('server', 'client'):
            game = BASE / f'{side}-{scenario}'
            mods = game / 'mods'
            mods.mkdir(parents=True, exist_ok=True)
            for source in snapshot.glob('*.jar'):
                if scenario.startswith('all') or source.name.startswith('forestry-'):
                    shutil.copy2(source, mods / source.name)
            names = DEPENDENCIES + (['Patchouli-1.21.1-93-NEOFORGE.jar'] if scenario.endswith('-with') else [])
            for name in names:
                candidates = cached.get(name, [])
                if len(candidates) != 1:
                    raise RuntimeError(f'Expected one cached artifact: {name}')
                shutil.copy2(candidates[0], mods / name)
            if side == 'client' and not (game / 'options.txt').exists():
                (game / 'options.txt').write_text('lang:en_us\nguiScale:2\nrenderDistance:4\nsimulationDistance:5\nmaxFps:30\nonboardAccessibility:false\npauseOnLostFocus:false\nautoJump:false\n', encoding='utf-8')
    print(json.dumps({'artifacts': hashes, 'scenarios': SCENARIOS}, indent=2))


def verify_mods(game, scenario):
    expected_mods = set(DEPENDENCIES)
    if scenario.endswith('-with'):
        expected_mods.add('Patchouli-1.21.1-93-NEOFORGE.jar')
    for name, expected in load(BASE / 'artifacts.json').items():
        if scenario.startswith('all') or name.startswith('forestry-'):
            expected_mods.add(name)
            if hashlib.sha256((game / 'mods' / name).read_bytes()).hexdigest() != expected:
                raise RuntimeError('Production artifact changed: ' + name)
    if {p.name for p in (game / 'mods').glob('*.jar')} != expected_mods:
        raise RuntimeError('Unexpected mod set: ' + str(game))


def audit():
    result = {'jars': [], 'recipeReferences': [], 'instances': []}
    recipes = set()
    references = []
    for name, expected in load(BASE / 'artifacts.json').items():
        path = BASE / 'artifacts' / name
        assert hashlib.sha256(path.read_bytes()).hexdigest() == expected, name
        with zipfile.ZipFile(path) as jar:
            entries = jar.namelist()
            leaks = [n for n in entries if n.startswith('vazkii/patchouli/') or ('patchouli' in n.lower() and n.endswith('.jar'))]
            assert not leaks, leaks
            metadata = jar.read('META-INF/neoforge.mods.toml').decode()
            if name.startswith('forestry-'):
                assert 'modId = "patchouli"\ntype = "OPTIONAL"' in metadata
            for entry in entries:
                if entry.startswith('data/forestry/recipe/') and entry.endswith('.json'):
                    recipes.add('forestry:' + entry.removeprefix('data/forestry/recipe/').removesuffix('.json'))
                if entry.startswith('assets/forestry/patchouli_books/') and entry.endswith('.json'):
                    data = json.loads(jar.read(entry))
                    for page in data.get('pages', []):
                        for key in ('recipe', 'recipe2'):
                            if isinstance(page.get(key), str) and page[key].startswith('forestry:'):
                                references.append((entry, page[key]))
            result['jars'].append({'name': name, 'sha256': expected, 'bundledPatchouli': leaks})
    for entry, recipe in references:
        result['recipeReferences'].append({'entry': entry, 'recipe': recipe, 'exists': recipe in recipes})
    assert all(r['exists'] for r in result['recipeReferences']), 'Unresolved book recipe'
    for scenario in SCENARIOS:
        categories = {}
        book_entries = {}
        prefix = 'assets/forestry/patchouli_books/foresters_manual/en_us/'
        for path in (BASE / f'client-{scenario}/mods').glob('forestry*.jar'):
            with zipfile.ZipFile(path) as jar:
                for name in jar.namelist():
                    if not name.startswith(prefix) or not name.endswith('.json'):
                        continue
                    relative = name.removeprefix(prefix).removesuffix('.json')
                    if relative.startswith('categories/'):
                        categories['forestry:' + relative.removeprefix('categories/')] = json.loads(jar.read(name))
                    elif relative.startswith('entries/'):
                        book_entries[name] = json.loads(jar.read(name))
        for name, data in book_entries.items():
            assert data.get('category') in categories, (scenario, name, 'Missing book category')
        for name, data in categories.items():
            assert not data.get('parent') or data['parent'] in categories, (scenario, name, 'Missing parent category')
        for side in ('server', 'client'):
            game = BASE / f'{side}-{scenario}'
            verify_mods(game, scenario)
            log = game / 'logs/latest.log'
            text = log.read_text(encoding='utf-8', errors='replace') if log.exists() else ''
            result['instances'].append({'scenario': scenario, 'side': side,
                'bookEntries': len(book_entries), 'bookCategories': len(categories),
                'mods': sorted(p.name for p in (game / 'mods').glob('*.jar')),
                'serverReady': 'Done (' in text if side == 'server' else None,
                'playerJoined': 'G1Tester joined the game' in text if side == 'server' else None,
                'log': str(log.relative_to(ROOT))})
    (BASE / 'audit.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
    print(json.dumps({'jars': len(result['jars']), 'validRecipeReferences': len(references), 'instances': result['instances']}, indent=2))


def launch(args):
    scenario = args.scenario
    side = args.action.removeprefix('start-')
    game = BASE / f'{side}-{scenario}'
    verify_mods(game, scenario)
    index = SCENARIOS.index(scenario)
    if side == 'server':
        if not args.accept_eula:
            raise RuntimeError('Pass --accept-eula only after the user accepts the Minecraft EULA')
        (game / 'eula.txt').write_text('eula=true\n', encoding='utf-8')
        (game / 'server.properties').write_text(
            f'server-ip=127.0.0.1\nserver-port={25581 + index}\nonline-mode=false\n'
            f'enable-rcon=true\nrcon.port={25591 + index}\nrcon.password=g1-local-acceptance\n'
            'gamemode=creative\nforce-gamemode=true\ndifficulty=peaceful\nlevel-type=minecraft:flat\n'
            'generate-structures=false\nspawn-protection=0\nview-distance=3\nsimulation-distance=3\n'
            'max-players=2\nenforce-secure-profile=false\n', encoding='utf-8')
        text = (RUNTIME / 'libraries/net/neoforged/neoforge/21.1.230/win_args.txt').read_text()
        text = text.replace('libraries/', str(RUNTIME / 'libraries').replace('\\', '/') + '/')
        argfile = game / 'launch.args'
        text = text.replace('-DlibraryDirectory=libraries', '-DlibraryDirectory=' + str(RUNTIME / 'libraries').replace('\\', '/'))
        argfile.write_text('-Xmx2G\n' + text + '\n--nogui\n', encoding='utf-8')
    else:
        vanilla = load(RUNTIME / 'versions/1.21.1/1.21.1.json')
        neo = load(RUNTIME / 'versions/neoforge-21.1.230/neoforge-21.1.230.json')
        libraries = {}
        for lib in vanilla['libraries'] + neo['libraries']:
            if allowed(lib) and lib.get('downloads', {}).get('artifact'):
                parts = lib['name'].split(':')
                key = ':'.join(parts[:2] + parts[3:])
                libraries[key] = str(RUNTIME / 'libraries' / lib['downloads']['artifact']['path'])
        libraries['minecraft'] = str(RUNTIME / 'versions/1.21.1/1.21.1.jar')
        values = {
            'library_directory': str(RUNTIME / 'libraries'), 'classpath_separator': ';',
            'classpath': ';'.join(libraries.values()), 'version_name': neo['id'],
            'natives_directory': str(game / 'natives'), 'launcher_name': 'G1Acceptance', 'launcher_version': '1',
        }
        def expand(value):
            for key, replacement in values.items():
                value = value.replace('${' + key + '}', replacement)
            if '${' in value:
                raise RuntimeError('Unresolved argument: ' + value)
            if value.startswith('-DignoreList='):
                value += ',1.21.1.jar'
            return value
        jvm = ['-Xmx2G']
        for value in vanilla['arguments']['jvm'] + neo['arguments']['jvm']:
            if isinstance(value, dict):
                if not allowed(value):
                    continue
                value = value['value']
            jvm.extend(expand(v) for v in (value if isinstance(value, list) else [value]))
        game_args = neo['arguments']['game'] + [
            '--username', 'G1Tester', '--uuid', '4789f6e020333a3695b70dd753ebcd65',
            '--accessToken', '0', '--userType', 'legacy', '--version', neo['id'],
            '--gameDir', str(game), '--assetsDir', str(ROOT / '.local/gradle/caches/neoformruntime/assets'),
            '--assetIndex', vanilla['assetIndex']['id'], '--width', '1100', '--height', '740',
        ]
        if args.connect:
            deadline = time.monotonic() + 120
            while True:
                try:
                    with socket.create_connection(('127.0.0.1', 25581 + SCENARIOS.index(args.connect)), timeout=1):
                        break
                except OSError:
                    if time.monotonic() >= deadline:
                        raise RuntimeError('Target acceptance server is not ready')
                    time.sleep(1)
            game_args += ['--quickPlayMultiplayer', f'127.0.0.1:{25581 + SCENARIOS.index(args.connect)}']
        command = jvm + [neo['mainClass']] + game_args
        argfile = game / 'launch.args'
        argfile.write_text('\n'.join('"' + value.replace('\\', '\\\\').replace('"', '\\"') + '"' for value in command), encoding='utf-8')
    if (game / 'console.log').exists():
        shutil.copy2(game / 'console.log', game / ('console-' + time.strftime('%Y%m%d-%H%M%S') + '.log'))
    output = open(game / 'console.log', 'w', encoding='utf-8')
    process = subprocess.Popen([str(JAVA), '@' + str(argfile)], cwd=game, stdout=output, stderr=subprocess.STDOUT,
                               creationflags=subprocess.CREATE_NO_WINDOW)
    (game / 'pid.txt').write_text(str(process.pid))
    print(json.dumps({'pid': process.pid, 'game': str(game), 'scenario': scenario, 'side': side}))


def rcon(scenario, command):
    with socket.create_connection(('127.0.0.1', 25591 + SCENARIOS.index(scenario)), timeout=10) as connection:
        def send(request_id, kind, text):
            data = struct.pack('<ii', request_id, kind) + text.encode() + b'\x00\x00'
            connection.sendall(struct.pack('<i', len(data)) + data)
        def receive():
            def exact(size):
                data = b''
                while len(data) < size:
                    chunk = connection.recv(size - len(data))
                    if not chunk:
                        raise ConnectionError('RCON closed')
                    data += chunk
                return data
            data = exact(struct.unpack('<i', exact(4))[0])
            return struct.unpack('<ii', data[:8]), data[8:-2].decode('utf-8', errors='replace')
        send(1, 3, 'g1-local-acceptance')
        header, _ = receive()
        if header[0] == -1:
            raise RuntimeError('RCON authentication failed')
        send(2, 2, command)
        print(receive()[1])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['install', 'prepare', 'audit', 'start-server', 'start-client', 'command'])
    parser.add_argument('--root', type=Path, default=BASE, help='Acceptance directory beneath the repository .local directory')
    parser.add_argument('--scenario', choices=SCENARIOS, default='all-with')
    parser.add_argument('--connect', choices=SCENARIOS)
    parser.add_argument('--accept-eula', action='store_true')
    parser.add_argument('--command')
    args = parser.parse_args()
    BASE = args.root.resolve()
    if not BASE.is_relative_to(ROOT / '.local'):
        raise RuntimeError('Acceptance root must be inside the repository .local directory')
    RUNTIME = BASE / 'runtime'
    if args.action == 'install':
        install()
    elif args.action == 'prepare':
        prepare()
    elif args.action == 'audit':
        audit()
    elif args.action == 'command':
        rcon(args.scenario, args.command)
    else:
        launch(args)
