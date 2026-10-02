#!/usr/bin/env python3
"""Build the submission archive using an allowlist; omit credentials and build output."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

root = Path(__file__).resolve().parent.parent
output = root / 'artifacts' / 'online-library-homework.zip'
output.parent.mkdir(exist_ok=True)
files = [root / name for name in (
    'pom.xml', 'Dockerfile', 'docker-compose.yml', 'docker-compose.test.yml',
    'docker-compose.scale.yml', '.env.example', '.gitignore', '.dockerignore',
    'README.md', 'Отчёт.md',
)]
for folder in ('src', 'docker', 'scripts'):
    files.extend(p for p in (root / folder).rglob('*')
                 if p.is_file() and '__pycache__' not in p.parts and p.name != '.DS_Store')
with ZipFile(output, 'w', compression=ZIP_DEFLATED) as archive:
    for file in sorted(files):
        archive.write(file, Path('online-library') / file.relative_to(root))
with ZipFile(output) as archive:
    assert archive.testzip() is None
    assert 'online-library/Отчёт.md' in archive.namelist()
print(f'{output} ({output.stat().st_size:,} bytes; {len(files)} files)')
