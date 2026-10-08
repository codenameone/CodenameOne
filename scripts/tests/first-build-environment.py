#!/usr/bin/env python3
"""Run a real Initializr download's first cloud build in one deliberately awkward
environment, and check that it ends the way a developer can act on.

    first-build-environment.py <initializr-zip> <case>

Real Maven, real Codename One plugin and build client, real downloads. CI has
no Codename One account, so the build client runs headless and stops at sign-in
("Cannot authenticate a non-interactive build"): a build that gets THAT far has
made it through everything a developer's machine can break before sign-in, and
reports login_failed. A broken environment must stop earlier, with its own word.
The launcher's report goes to a local recorder, never the production funnel.

Cases (the workflow provides the JDK each one needs through JAVA_HOME):
  baseline   JDK 17                                    -> login_failed
  non_ascii  project path and user home with non-ASCII -> login_failed
  jre        a JRE: java but no javac                  -> no_jdk
  old_jdk    JDK 11 for a Java 17 project              -> java_too_old
  mirror     settings.xml mirroring everything to an
             unreachable repository (a corporate Nexus
             that does not proxy repo.codenameone.com) -> maven_download
"""
import os
import shutil
import subprocess
import sys
import tempfile
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs

EXPECTED = {
    'baseline': 'login_failed',
    'non_ascii': 'login_failed',
    'jre': 'no_jdk',
    'old_jdk': 'java_too_old',
    'mirror': 'maven_download',
}

if not os.environ.get('CI'):
    # The build client signs in with the token this machine saved for
    # Codename One (Java Preferences: the registry on Windows, a plist on
    # macOS -- neither follows user.home), so on a developer's own machine the
    # "headless" first build is a REAL build, submitted to their account. CI
    # runners have no saved sign-in.
    sys.exit('first-build-environment.py runs in CI only: on this machine the build client would use your '
             'saved Codename One sign-in and submit a real build')

source, case = Path(sys.argv[1]).resolve(), sys.argv[2]
expected = EXPECTED[case]
windows = os.name == 'nt'
events = []


class Recorder(BaseHTTPRequestHandler):
    def do_POST(self):
        body = self.rfile.read(int(self.headers.get('Content-Length') or 0)).decode()
        events.append({k: v[0] for k, v in parse_qs(body, keep_blank_values=True).items()})
        self.send_response(204)
        self.end_headers()

    def log_message(self, *args):
        pass


recorder = ThreadingHTTPServer(('127.0.0.1', 0), Recorder)
threading.Thread(target=recorder.serve_forever, daemon=True).start()

with tempfile.TemporaryDirectory(prefix='cn1-first-build-') as directory:
    parent = Path(directory)
    name = 'Prøject Jösé 测试' if case == 'non_ascii' else 'MyFirstApp'
    project = parent / name
    if windows:
        subprocess.run(['powershell', '-NoProfile', '-Command',
                        'Expand-Archive -LiteralPath $env:CN1_ZIP -DestinationPath $env:CN1_DEST'],
                       env=dict(os.environ, CN1_ZIP=str(source), CN1_DEST=str(project)), check=True)
    else:
        subprocess.run(['unzip', '-q', str(source), '-d', str(project)], check=True)
    opts = ['-Djava.awt.headless=true']
    if case in ('non_ascii', 'mirror'):
        # A user home Maven has never seen: its own settings.xml and an empty
        # local repository, so nothing is served from the runner's cache.
        # No space: MAVEN_OPTS is split on spaces by mvn. A real user's home
        # never travels through MAVEN_OPTS, so this only shapes the test.
        home = parent / ('Jösé_Ñ_用户' if case == 'non_ascii' else 'home')
        (home / '.m2').mkdir(parents=True)
        opts.append('-Duser.home=' + str(home))
        if case == 'mirror':
            (home / '.m2/settings.xml').write_text(
                '<settings><mirrors><mirror><id>corporate</id><mirrorOf>*</mirrorOf>'
                '<url>http://127.0.0.1:9/nexus/</url></mirror></mirrors></settings>')
    env = dict(os.environ, MAVEN_OPTS=' '.join(opts),
               CN1_EVENTS_URL='http://127.0.0.1:%d/first-build' % recorder.server_address[1])
    script = project / ('build.bat' if windows else 'build.sh')
    command = [str(script), 'javascript_cloud']
    if windows:
        command = '"' + os.environ.get('COMSPEC', 'cmd.exe') + '" /d /s /c ""' + str(script) + '" javascript_cloud"'
    result = subprocess.run(command, cwd=project, env=env, text=True, encoding='utf-8', errors='replace',
                            capture_output=True, timeout=1500)
    lines = (result.stdout + result.stderr).splitlines()
    # Enough to see what failed: every error line, then the last stretch.
    errors = [l for l in lines if any(k in l for k in ('ERROR', 'Exception', 'Caused by', 'rror:', 'FATAL'))]
    tail = '\n'.join(errors[:60] + ['----- last lines -----'] + lines[-120:])
    exits = [e for e in events if e.get('step') == 'exit']
    reason = exits[-1].get('reason') if exits else None
    print(f'{case}: exit {result.returncode}, reason {reason!r} (expected {expected!r})')
    if reason != expected or result.returncode == 0:
        print(tail)
    assert result.returncode != 0, ('the headless first build must stop at sign-in or before', tail)
    assert reason == expected, (case, reason, expected, tail)
    print(f'PASS: {case}')
recorder.shutdown()
