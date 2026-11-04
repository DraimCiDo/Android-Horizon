#!/usr/bin/env python3
"""Build-time inputs and resource archive for the self-contained Android APK.

Generated game files stay in ignored .local/ and target/ folders, never in Git.
"""
import argparse
from concurrent.futures import ThreadPoolExecutor
import json
from pathlib import Path
import time
import zipfile

from tools import lab


ROOT = Path(__file__).resolve().parents[1]


def download(job):
    for attempt in range(4):
        try:
            return lab.download(*job)
        except Exception as error:
            if attempt == 3:
                raise RuntimeError(f'{job[0]}: {error}') from error
            time.sleep(2 ** attempt)


def inputs():
    game = json.loads((ROOT / 'research-lock.json').read_text())['minecraft']
    work = ROOT / '.local/android-bundle-inputs'
    manifest = work / 'version.json'
    download((f"https://piston-meta.mojang.com/v1/packages/{game['manifest_sha1']}/{game['version']}.json",
              manifest, game['manifest_sha1']))
    version = json.loads(manifest.read_text())
    jobs = []
    for kind in ('client', 'server'):
        item = version['downloads'][kind]
        if item['sha1'] != game[f'{kind}_sha1']:
            raise ValueError(f'{kind} JAR differs from research-lock.json')
        jobs.append((item['url'], work / f'{kind}.jar', item['sha1']))
    for library in version['libraries']:
        artifact = library.get('downloads', {}).get('artifact')
        if artifact:
            relative = Path(artifact['path'])
            if relative.is_absolute() or '..' in relative.parts:
                raise ValueError('Unsafe library path')
            jobs.append((artifact['url'], work / 'libraries' / relative, artifact['sha1']))
    index = version['assetIndex']
    index_path = work / 'assets/indexes' / f"{index['id']}.json"
    download((index['url'], index_path, index['sha1']))
    # Deduplicate by content hash: several asset names can refer to one object.
    hashes = {item['hash'] for item in json.loads(index_path.read_text())['objects'].values()}
    for digest in sorted(hashes):
        if len(digest) != 40 or any(c not in '0123456789abcdef' for c in digest):
            raise ValueError('Invalid asset hash')
        jobs.append((f'https://resources.download.minecraft.net/{digest[:2]}/{digest}',
                     work / 'assets/objects' / digest[:2] / digest, digest))
    print(f'Downloading/verifying {len(jobs)} pinned game inputs', flush=True)
    with ThreadPoolExecutor(max_workers=8) as pool:
        list(pool.map(download, jobs))
    print(f'Inputs ready: {work}', flush=True)


def pack(source, output):
    for required in ('horizon-resources.json', 'facts.json'):
        if not (source / required).is_file():
            raise ValueError(f'Prepared resources lack {required}')
    if (source / '.horizon-setup-incomplete').exists():
        raise ValueError('Resources are incomplete')
    output.mkdir(parents=True, exist_ok=True)
    archive = output / 'horizon-resources.zip'
    temporary = archive.with_suffix('.part')
    count = total = 0
    with zipfile.ZipFile(temporary, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for path in sorted(source.rglob('*')):
            if path.is_symlink():
                raise ValueError(f'Resource symlink is not allowed: {path}')
            if not path.is_file():
                continue
            relative = path.relative_to(source)
            if any(p in ('classes', 'client-classes') for p in relative.parts) or path.suffix == '.log':
                continue
            z.write(path, relative.as_posix())
            count += 1
            total += path.stat().st_size
    temporary.replace(archive)
    game = json.loads((ROOT / 'research-lock.json').read_text())['minecraft']['version']
    metadata = {'schema': 1, 'minecraft': game, 'files': count,
                'archive_bytes': archive.stat().st_size, 'unpacked_bytes': total,
                'sha256': lab.digest(archive, 'sha256')}
    (output / 'horizon-bundle.json').write_text(json.dumps(metadata, indent=2) + '\n')
    print(json.dumps(metadata), flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    sub.add_parser('inputs')
    bundle = sub.add_parser('pack')
    bundle.add_argument('source', type=Path)
    bundle.add_argument('output', type=Path)
    args = parser.parse_args()
    if args.command == 'inputs':
        inputs()
    else:
        pack(args.source, args.output)
