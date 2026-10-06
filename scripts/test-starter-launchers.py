#!/usr/bin/env python3
"""Execute the shipped launchers and Windows PowerShell wrapper without a build account.
Only the downloaded Maven executable is replaced with a recorder; launcher and
wrapper files, including the PowerShell download/cache discovery, stay untouched.
"""
import hashlib
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import threading
from urllib.parse import parse_qs

source = Path(sys.argv[1]).resolve()
windows = os.name == 'nt'
with tempfile.TemporaryDirectory(prefix='cn1-launcher-') as directory:
    parent = Path(directory)
    project = parent / "Project O'Brien with spaces"
    if source.is_file():
        if windows:
            command = ["powershell", "-NoProfile", "-Command",
                       "Expand-Archive -LiteralPath $env:CN1_TEST_ZIP -DestinationPath $env:CN1_TEST_PROJECT"]
            subprocess.run(command, env=dict(os.environ, CN1_TEST_ZIP=str(source), CN1_TEST_PROJECT=str(project)), check=True)
        else:
            subprocess.run(['unzip', '-q', str(source), '-d', str(project)], check=True)
        for name in ['build.sh', 'run.sh', 'mvnw']:
            if not windows:
                assert os.access(project / name, os.X_OK), name + ' must extract executable without chmod'
    else:
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
        executable.write_bytes(b'@echo off\r\nif defined MVNW_USERNAME exit /b 91\r\nif defined MVNW_PASSWORD exit /b 92\r\necho %cd%>"%CN1_TEST_RECORD%"\r\necho %*>>"%CN1_TEST_RECORD%"\r\nexit /b %CN1_TEST_EXIT%\r\n')
    else:
        executable.write_text('#!/bin/sh\npwd > "$CN1_TEST_RECORD"\nprintf "%s\\n" "$@" >> "$CN1_TEST_RECORD"\nexit "$CN1_TEST_EXIT"\n')
        executable.chmod(0o755)
    record = parent / 'record.txt'
    # Initializr launchers report build progress (see launcher-telemetry-sh.txt in
    # scripts/initializr). Point them at a local recorder that answers 204 at
    # once, so no test run lands in the production funnel and the reports can
    # be checked. Not a closed port: on Windows a refused localhost connection
    # is retried for about two seconds, which with two reports per build pushed
    # the windows-latest fixture step past its ten-minute limit.
    events = []

    class Recorder(BaseHTTPRequestHandler):
        def do_POST(self):
            body = self.rfile.read(int(self.headers.get('Content-Length') or 0)).decode()
            events.append({k: v[0] for k, v in parse_qs(body, keep_blank_values=True).items()})
            self.send_response(204)
            self.end_headers()

        def log_message(self, *args):
            pass

    class QuietServer(ThreadingHTTPServer):
        # A launcher's curl gives up after 3 s by design; a connection it drops
        # mid-request is not a test failure, so do not print a traceback for it.
        def handle_error(self, request, client_address):
            pass

    recorder = QuietServer(('127.0.0.1', 0), Recorder)
    threading.Thread(target=recorder.serve_forever, daemon=True).start()
    env = dict(os.environ, MAVEN_USER_HOME=str(cache), CN1_TEST_RECORD=str(record), CN1_TEST_EXIT='0',
               CN1_EVENTS_URL='http://127.0.0.1:%d/cn1-launcher-test' % recorder.server_address[1])
    if windows:
        env.update(MVNW_USERNAME='wrapper-test-user', MVNW_PASSWORD='wrapper-test-password')
    env.pop('MVNW_REPOURL', None)
    env.pop('__MVNW_ARG0_NAME__', None)

    def run(launcher, target, expected, code=0):
        record.unlink(missing_ok=True)
        env['CN1_TEST_EXIT'] = str(code)
        script = project / (launcher + ('.bat' if windows else '.sh'))
        if not windows and source.is_dir():
            script.chmod(0o755)  # archive modes are separately tested by StarterProjectServiceTest
        command = [str(script)] + ([target] if target else [])
        if windows:
            # cmd.exe parses its own command string, not the C-runtime quoting
            # that subprocess applies to a list (which escapes quotes as backslashes).
            command = '"' + os.environ.get('COMSPEC', 'cmd.exe') + '" /d /s /c ""' + str(script) + '" ' + target + '"'
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
    reporting = 'CN1_PROJECT_ID' in (project / ('build.bat' if windows else 'build.sh')).read_text()
    if reporting:
        # An Initializr launcher: every build reports launch then exit, keyed by
        # the 64-hex package hash and nothing that names the package.
        steps = [e.get('step') for e in events]
        assert 'launch' in steps and 'exit' in steps, events
        assert all(len(e.get('pkg', '')) == 64 for e in events), events
        assert any(e.get('step') == 'exit' and e.get('exit') == '37' for e in events), events
        if not windows:
            # Reporting must never change the build itself. A TMPDIR that does
            # not exist leaves no room for the reason log: the build still
            # returns Maven's own status. And Maven's stderr stays on stderr.
            missing_tmp = dict(env, TMPDIR=str(parent / 'no-such-tmp'), CN1_TEST_EXIT='0')
            result = subprocess.run([str(project / 'build.sh'), 'javascript_cloud'], cwd=parent, env=missing_tmp,
                                    text=True, capture_output=True, timeout=30)
            assert result.returncode == 0, ('missing TMPDIR failed the build', result.returncode, result.stderr)
            noisy = project / 'mvnw-stderr-probe.sh'
            noisy.write_text('#!/bin/sh\necho to-stdout\necho to-stderr >&2\nexit 0\n')
            noisy.chmod(0o755)
            probe = (project / 'build.sh').read_text().replace('( ./mvnw "$@" 2>', '( ./mvnw-stderr-probe.sh "$@" 2>')
            assert probe != (project / 'build.sh').read_text(), 'probe hook not found in build.sh'
            (project / 'build-probe.sh').write_text(probe)
            (project / 'build-probe.sh').chmod(0o755)
            result = subprocess.run([str(project / 'build-probe.sh'), 'javascript_cloud'], cwd=parent, env=env,
                                    text=True, capture_output=True, timeout=30)
            assert result.returncode == 0, result
            assert 'to-stdout' in result.stdout and 'to-stderr' not in result.stdout, result.stdout
            assert 'to-stderr' in result.stderr, result.stderr
            # Maven (and its plugins) can prompt: the launcher's stdin must reach it,
            # not the /dev/null bash gives a background command by default.
            noisy.write_text('#!/bin/sh\nread line\necho "got:$line"\nexit 0\n')
            result = subprocess.run([str(project / 'build-probe.sh'), 'javascript_cloud'], cwd=parent, env=env,
                                    input='typed-answer\n', text=True, capture_output=True, timeout=30)
            assert result.returncode == 0 and 'got:typed-answer' in result.stdout, (result.stdout, result.stderr)
    else:
        assert not events, ('the archetype launchers report nothing', events)
    recorder.shutdown()
    print('PASS: targets, local defaults, parent cwd, spaces/apostrophes, failure exit codes'
          + (', Windows credential isolation' if windows else '')
          + (', build progress reports' if reporting else ''))
