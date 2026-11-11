#!/usr/bin/env python3
"""Keep Initializr's embedded launchers aligned with the archetype (--write to update)."""
from pathlib import Path
import io
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
archetype = root / 'maven/cn1app-archetype/src/main/resources/archetype-resources'
archive = root / 'scripts/initializr/common/src/main/resources/common.zip'
# Git may check text files out as CRLF on Windows. ZIP payloads use canonical
# LF for Unix launchers and CRLF for every Windows one. A .bat with LF endings
# breaks cmd.exe's goto: it finds labels by re-reading the file in 512-byte
# blocks and, without CR, can miss one that straddles a block boundary -- adding
# the device targets moved a label onto one and `build.bat javascript` ran the
# android target instead.
expected = {name: (archetype / name).read_bytes().replace(b'\r\n', b'\n') for name in
            ['build.sh', 'build.bat', 'run.sh', 'run.bat', 'mvnw', 'mvnw.cmd']}
expected['mvnw.cmd'] = expected['mvnw.cmd'].replace(b'\n', b'\r\n')
for name in ('build.bat', 'run.bat'):
    expected[name] = expected[name].replace(b'\n', b'\r\n')
# Preserve the historical Initializr alias; the native Windows target is unchanged.
expected['build.sh'] = expected['build.sh'].replace(b'function linux_device {', b'function uwp {\n  windows_device\n}\nfunction linux_device {')
expected['build.bat'] = expected['build.bat'].replace(b':linux_device\r\n', b':uwp\r\ngoto :windows_device\r\n\r\n:linux_device\r\n')
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
# The desktop jar runs on machines other than the one that built it, so every
# command that builds one asks for every desktop platform's media natives. The
# IDE launchers are separate copies of the archetype's and nothing regenerates
# them, so a copy that builds the jar without the flag is reported here.
FLAG = b'cn1.ffmpeg.allPlatforms'
resources = archive.parent
host_only = []


def builds_without_flag(data):
    """True when a command in this launcher builds the desktop jar without the flag."""
    if b'<action>' in data:
        # NetBeans: one file, several actions, each a command of its own.
        # An action that only cleans resolves no media and needs no flag.
        return any(b'executable-jar' in action and FLAG not in action
                   and action.count(b'<goal>') != action.count(b'<goal>clean</goal>')
                   for action in data.split(b'<action>'))
    lines = [line for line in data.splitlines() if b'-Pexecutable-jar' in line]
    if lines:
        return any(FLAG not in line for line in lines)
    return b'executable-jar' in data and FLAG not in data


for ide in ('eclipse.zip', 'idea.zip', 'netbeans.zip', 'vscode.zip', 'common.zip'):
    with zipfile.ZipFile(resources / ide) as source:
        for entry in source.infolist():
            if entry.is_dir() or entry.filename.endswith('pom.xml'):
                continue
            data = source.read(entry.filename)
            if builds_without_flag(data):
                host_only.append(ide + '!' + entry.filename)
# The archetype's own copies, which no archive above is built from.
for name in ['.vscode/settings.json', 'mvnconfig.toml', 'tools/eclipse', 'tools/netbeans']:
    path = archetype / name
    for file in ([path] if path.is_file() else sorted(p for p in path.rglob('*') if p.is_file())):
        if builds_without_flag(file.read_bytes()):
            host_only.append(str(file.relative_to(root)))
if builds_without_flag((root / 'maven/cli/src/main/batsh/build.batsh').read_bytes()):
    host_only.append('maven/cli/src/main/batsh/build.batsh')
run_profiles = root / 'maven/codenameone-maven-plugin/src/main/resources/com/codename1/maven/intellij/workspace.xml'
if FLAG not in run_profiles.read_bytes():
    host_only.append(str(run_profiles.relative_to(root)))
if host_only:
    raise SystemExit('These build the desktop jar without -D' + FLAG.decode()
                     + '=true, so it would carry media natives for the build machine only: '
                     + ', '.join(host_only))
print('Initializr launcher parity OK')
