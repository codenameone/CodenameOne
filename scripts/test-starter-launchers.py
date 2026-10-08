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
import time
from urllib.parse import parse_qs

source = Path(sys.argv[1]).resolve()
windows = os.name == 'nt'
# (output, exit code, reason): what Maven or the build client prints for each
# way a first build ends, and the word the launcher must report for it.
REASON_CASES = [
    ("Your build size is: 6kb\nSending build request to the server, notice that the build might take a while to complete!\n"
     "Your build was submitted follow the status on: https://cloud.codenameone.com/secure/index.html\nBUILD SUCCESS\n", 0, 'ok'),
    ("Jar size limit reached for free accounts.\nYou can upgrade your account at https://www.codenameone.com/pricing.html\n"
     "BUILD SUCCESS\n", 0, 'size_limit'),
    ("Sending build request to the server, notice that the build might take a while to complete!\nBUILD SUCCESS\n", 0,
     'not_submitted'),
    ("[ERROR] No compiler is provided in this environment. Perhaps you are running on a JRE rather than a JDK?\n"
     "BUILD FAILURE\n", 1, 'no_jdk'),
    ("[ERROR] Failed to execute goal ... Fatal error compiling: error: invalid target release: 17\nBUILD FAILURE\n", 1,
     'java_too_old'),
    ("[ERROR] COMPILATION ERROR :\nBUILD FAILURE\n", 1, 'compile'),
    ("Could not transfer artifact com.codenameone:codenameone-core:jar:7.0.274 from/to codenameone: PKIX path building "
     "failed: unable to find valid certification path to requested target\nBUILD FAILURE\n", 1, 'tls'),
    ("[ERROR] Plugin com.codenameone:codenameone-maven-plugin:7.0.275 or one of its dependencies could not be resolved: "
     "Could not find artifact com.codenameone:codenameone-maven-plugin:jar:7.0.275\nBUILD FAILURE\n", 1, 'maven_download'),
    ("Cannot authenticate a non-interactive build: no saved token and no credentials.\nBUILD FAILURE\n", 1, 'login_failed'),
    ("A certificate from Apple with the appropriate password is required for building an iOS native app!\n"
     "BUILD FAILURE\n", 1, 'no_certificate'),
    ("The icon must be a 512x512 pixel PNG image. It will be scaled to the proper sizes for devices\nBUILD FAILURE\n", 1,
     'project_config'),
    ("Error connecting to s3: 403 please check your proxy settings\nBUILD FAILURE\n", 1, 'upload_failed'),
    ("Server Detailed Error Message: ERROR You don't have enough build credits\nBUILD FAILURE\n", 1, 'server_refused'),
    ("[ERROR] Something nobody anticipated\nBUILD FAILURE\n", 1, 'build_failed'),
]

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
        executable.write_bytes(b'@echo off\r\nif defined MVNW_USERNAME exit /b 91\r\nif defined MVNW_PASSWORD exit /b 92\r\necho %cd%>"%CN1_TEST_RECORD%"\r\necho %*>>"%CN1_TEST_RECORD%"\r\nif defined CN1_TEST_OUTPUT_FILE type "%CN1_TEST_OUTPUT_FILE%"\r\nexit /b %CN1_TEST_EXIT%\r\n')
    else:
        # CN1_TEST_OUTPUT_FILE: what this fake Maven prints, so a test can hand
        # the launcher the output of a real failure and check the reason it reports.
        executable.write_text('#!/bin/sh\npwd > "$CN1_TEST_RECORD"\nprintf "%s\\n" "$@" >> "$CN1_TEST_RECORD"\n'
                              '[ -n "$CN1_TEST_OUTPUT_FILE" ] && cat "$CN1_TEST_OUTPUT_FILE"\nexit "$CN1_TEST_EXIT"\n')
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
            # The one-word reason, from the output of real failures. Each case is
            # what Maven or the build client actually prints, and the reason the
            # funnel needs to tell it apart from the others.
            output_file = parent / 'maven-output.txt'
            for text, exit_code, expected_reason in REASON_CASES:
                output_file.write_text(text)
                events.clear()
                result = subprocess.run([str(project / 'build.sh'), 'javascript_cloud'], cwd=parent,
                                        env=dict(env, CN1_TEST_EXIT=str(exit_code), CN1_TEST_OUTPUT_FILE=str(output_file)),
                                        text=True, capture_output=True, timeout=30)
                assert result.returncode == exit_code, (text, result.returncode, result.stderr)
                reasons = [e.get('reason') for e in events if e.get('step') == 'exit']
                assert reasons == [expected_reason], (text, exit_code, expected_reason, events)
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
            if os.path.isdir('/proc'):
                # Cancelling must stop Maven even on a Linux without pgrep (a slim
                # container without procps): the launcher then finds Maven's pid
                # through /proc. PATH here has every tool except pgrep.
                nopgrep = parent / 'bin-without-pgrep'
                nopgrep.mkdir(exist_ok=True)
                for folder in os.environ.get('PATH', '').split(os.pathsep):
                    if not os.path.isdir(folder):
                        continue
                    for name in os.listdir(folder):
                        link = nopgrep / name
                        if name != 'pgrep' and not link.exists():
                            try:
                                link.symlink_to(Path(folder) / name)
                            except OSError:
                                pass
                pidfile = parent / 'hung-maven.pid'
                noisy.write_text('#!/bin/sh\necho $$ > "%s"\nexec sleep 60\n' % pidfile)
                proc = subprocess.Popen([str(project / 'build-probe.sh'), 'javascript_cloud'], cwd=parent,
                                        env=dict(env, PATH=str(nopgrep)), stdout=subprocess.DEVNULL,
                                        stderr=subprocess.DEVNULL)
                for _ in range(100):
                    if pidfile.exists() and pidfile.read_text().strip():
                        break
                    time.sleep(0.1)
                maven_pid = int(pidfile.read_text().strip())
                proc.terminate()
                assert proc.wait(timeout=30) == 143, 'SIGTERM must exit 143'
                time.sleep(0.5)
                assert not Path('/proc/%d' % maven_pid).exists(), 'Maven survived the cancel without pgrep'

    else:
        assert not events, ('the archetype launchers report nothing', events)
    recorder.shutdown()
    print('PASS: targets, local defaults, parent cwd, spaces/apostrophes, failure exit codes'
          + (', Windows credential isolation' if windows else '')
          + (', build progress reports' if reporting else ''))
