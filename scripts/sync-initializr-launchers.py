#!/usr/bin/env python3
"""Keep Initializr's embedded launchers aligned with the archetype (--write to update)."""
from pathlib import Path
import io
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
archetype = root / 'maven/cn1app-archetype/src/main/resources/archetype-resources'
archive = root / 'scripts/initializr/common/src/main/resources/common.zip'
expected = {name: (archetype / name).read_bytes() for name in
            ['build.sh', 'build.bat', 'run.sh', 'run.bat', 'mvnw', 'mvnw.cmd']}
# Preserve the historical Initializr alias; the native Windows target is unchanged.
expected['build.sh'] = expected['build.sh'].replace(b'function linux_device {', b'function uwp {\n  windows_device\n}\nfunction linux_device {')
expected['build.bat'] = expected['build.bat'].replace(b':linux_device\n', b':uwp\ngoto :windows_device\n\n:linux_device\n')
with zipfile.ZipFile(archive) as source:
    stale = [name for name, data in expected.items() if source.read(name) != data]
    if stale and '--write' in sys.argv:
        result = io.BytesIO()
        with zipfile.ZipFile(result, 'w') as target:
            for entry in source.infolist():
                target.writestr(entry, expected.get(entry.filename, source.read(entry.filename)))
        archive.write_bytes(result.getvalue())
    elif stale:
        raise SystemExit('Initializr launcher drift: ' + ', '.join(stale)
                         + '. Run python scripts/sync-initializr-launchers.py --write')
print('Initializr launcher parity OK')
