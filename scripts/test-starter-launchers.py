#!/usr/bin/env python3
"""Execute the shipped launchers and Windows PowerShell wrapper without a build account.
Only the downloaded Maven executable is replaced with a recorder; launcher and
wrapper files, including the PowerShell download/cache discovery, stay untouched.
"""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

source = Path(sys.argv[1]).resolve()
windows = os.name == 'nt'
with tempfile.TemporaryDirectory(prefix='cn1-launcher-') as directory:
    parent = Path(directory)
    project = parent / "Project O'Brien with spaces"
    shutil.copytree(source, project)
    properties = (project / '.mvn/wrapper/maven-wrapper.properties').read_text()
    url = next(line.split('=', 1)[1].strip() for line in properties.splitlines() if line.startswith('distributionUrl='))
    distro = url.rsplit('/', 1)[1].removesuffix('-bin.zip')
    h = 0
    for char in url:
        h = (h * 31 + ord(char)) & 0xffffffff
    cache_key = hashlib.md5(url.encode()).hexdigest() if windows else format(h, 'x')
    cache = parent / "Cache O'Brien with spaces"
    executable = cache / 'wrapper/dists' / distro / cache_key / 'bin' / ('mvn.cmd' if windows else 'mvn')
    executable.parent.mkdir(parents=True)
    if windows:
        executable.write_bytes(b'@echo off\r\necho %cd%>"%CN1_TEST_RECORD%"\r\necho %*>>"%CN1_TEST_RECORD%"\r\nexit /b %CN1_TEST_EXIT%\r\n')
    else:
        executable.write_text('#!/bin/sh\npwd > "$CN1_TEST_RECORD"\nprintf "%s\\n" "$@" >> "$CN1_TEST_RECORD"\nexit "$CN1_TEST_EXIT"\n')
        executable.chmod(0o755)
    record = parent / 'record.txt'
    env = dict(os.environ, MAVEN_USER_HOME=str(cache), CN1_TEST_RECORD=str(record), CN1_TEST_EXIT='0')
    env.pop('MVNW_REPOURL', None)
    env.pop('__MVNW_ARG0_NAME__', None)

    def run(launcher, target, expected, code=0):
        record.unlink(missing_ok=True)
        env['CN1_TEST_EXIT'] = str(code)
        script = project / (launcher + ('.bat' if windows else '.sh'))
        if not windows:
            script.chmod(0o755)  # archive modes are separately tested by StarterProjectServiceTest
        command = [str(script)] + ([target] if target else [])
        if windows:
            command = [os.environ.get('COMSPEC', 'cmd.exe'), '/d', '/c', 'call "' + str(script) + '" ' + target]
        result = subprocess.run(command, cwd=parent, env=env, text=True, capture_output=True, timeout=30)
        assert result.returncode == code, (command, result.returncode, result.stdout, result.stderr)
        output = record.read_text()
        assert Path(output.splitlines()[0]).resolve() == project.resolve(), output
        assert expected in output, output
        if not target:
            assert 'LOCAL' in result.stdout and 'javascript' in result.stdout, result.stdout

    for target, argument in [('javascript', '-Dcodename1.buildTarget=local-javascript'),
                             ('javascript_cloud', '-Dcodename1.buildTarget=javascript'),
                             ('windows_device', '-Dcodename1.buildTarget=windows-device'),
                             ('linux_device', '-Dcodename1.buildTarget=linux-device'),
                             ('ios_source', '-Dcodename1.buildTarget=ios-source')]:
        run('build', target, argument)
    run('build', '', '-Pexecutable-jar')
    run('run', '', '-Psimulator')
    run('build', 'javascript_cloud', '-Dcodename1.buildTarget=javascript', 37)
    run('run', 'simulator', '-Psimulator', 37)
    print('PASS: targets, local defaults, parent cwd, spaces/apostrophes, failure exit codes')
