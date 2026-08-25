#!/usr/bin/env python3
"""Compile every cn1lib's Android sources against the SDK that cn1lib pins.

A cn1lib's Android implementation is shipped as source. Our build packages
src/main/java as *resources*, so no compiler of ours ever looks at it -- the
customer's Gradle build is the first one that does. That is how cn1-admob
reached users calling addNetworkExtrasBundle with a class that is not a
MediationExtrasReceiver, and a consent listener nested on the wrong interface
(PR #5570): a broken app build for everyone who included the library, with
green CI behind it.

So the artifacts named in each library's android.gradleDep are fetched and its
sources compiled against exactly those, purely as a check. Nothing here is
packaged.

The port classes these sources call are stubbed by
scripts/cn1lib-api-check/stubs rather than resolved from the Android port,
because the port is profile-gated and empty on a fresh checkout; the stubs are
guarded against the port's real declarations below, so they cannot drift.

  scripts/check-cn1lib-android-api.py [--require-all] [lib ...]

With no libraries named, checks every maven/cn1-* that ships Android sources. A
library whose inputs are missing (no android.jar, no compiled core) is skipped
with a note so a partial local tree still gives a useful answer; CI passes
--require-all, where a skip means the gate quietly stopped covering something.
"""

import os
import re
import shutil
import subprocess
import sys
import urllib.request
import zipfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STUBS = os.path.join(REPO, 'scripts', 'cn1lib-api-check', 'stubs')
PORT_SRC = os.path.join(REPO, 'Ports', 'Android', 'src', 'com', 'codename1',
                        'impl', 'android')

# The stubs above are hand-written stand-ins. Each entry is a declaration that
# has to still be present in the port, or the stub is lying about the API the
# library is being compiled against.
STUB_GUARDS = [
    ('AndroidNativeUtil.java', 'public static Activity getActivity()'),
    ('AndroidImplementation.java',
     'public static void runOnUiThreadAndBlock(final Runnable r)'),
]

MAVEN_REPOS = [
    'https://dl.google.com/dl/android/maven2',
    'https://repo1.maven.org/maven2',
]

# An "umbrella" artifact carries the version an app declares but none of the
# classes; the implementation lives in a sibling it pins to the same version.
ARTIFACT_SUBSTITUTIONS = {
    ('com.google.android.gms', 'play-services-ads'): 'play-services-ads-lite',
}

GRADLE_DEP = re.compile(r"'([\w.\-]+):([\w.\-]+):([\w.\-]+)'")


def log(message):
    sys.stdout.write(message + '\n')
    sys.stdout.flush()


def libraries():
    maven = os.path.join(REPO, 'maven')
    for name in sorted(os.listdir(maven)):
        if not name.startswith('cn1-'):
            continue
        src = os.path.join(maven, name, 'android', 'src', 'main', 'java')
        if not os.path.isdir(src):
            continue
        if any(f.endswith('.java')
               for _b, _d, files in os.walk(src) for f in files):
            yield name


def java_sources(root):
    for base, _dirs, files in os.walk(root):
        for name in sorted(files):
            if name.endswith('.java'):
                yield os.path.join(base, name)


def pinned_artifacts(lib):
    props = os.path.join(REPO, 'maven', lib, 'common',
                         'codenameone_library_required.properties')
    if not os.path.isfile(props):
        return []
    with open(props, encoding='utf-8') as f:
        text = f.read()
    line = ''
    for raw in text.splitlines():
        if raw.startswith('codename1.arg.android.gradleDep='):
            line = raw.split('=', 1)[1]
            break
    out = []
    for group, artifact, version in GRADLE_DEP.findall(line):
        artifact = ARTIFACT_SUBSTITUTIONS.get((group, artifact), artifact)
        out.append((group, artifact, version))
    return out


def fetch(group, artifact, version, cache):
    """Download one artifact and return the jar of classes inside it."""
    base = '%s/%s/%s/%s-%s' % (group.replace('.', '/'), artifact, version,
                               artifact, version)
    for ext in ('aar', 'jar'):
        local = os.path.join(cache, '%s-%s.%s' % (artifact, version, ext))
        if not os.path.isfile(local):
            for repo in MAVEN_REPOS:
                url = '%s/%s.%s' % (repo, base, ext)
                try:
                    with urllib.request.urlopen(url, timeout=120) as response:
                        data = response.read()
                except Exception:
                    continue
                with open(local, 'wb') as f:
                    f.write(data)
                break
        if not os.path.isfile(local):
            continue
        if ext == 'jar':
            return local
        # An .aar is a zip whose compiled code is classes.jar.
        extracted = os.path.join(cache, '%s-%s-classes.jar' % (artifact, version))
        if not os.path.isfile(extracted):
            with zipfile.ZipFile(local) as z:
                if 'classes.jar' not in z.namelist():
                    return None
                with z.open('classes.jar') as src, open(extracted, 'wb') as dst:
                    shutil.copyfileobj(src, dst)
        return extracted
    return None


def javac_version(javac):
    """The major Java version of a javac, or 0 if it will not run."""
    try:
        result = subprocess.run([javac, '-version'], stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT)
    except OSError:
        return 0
    text = result.stdout.decode('utf-8', 'replace').strip()
    match = re.search(r'javac (\d+)(?:\.(\d+))?', text)
    if not match:
        return 0
    major = int(match.group(1))
    if major == 1:
        return int(match.group(2) or 0)
    return major


def resolve_javac():
    """A javac new enough to read the SDKs these libraries pin.

    Android SDKs ship Java 11 class files (LevelPlay 9.6 is one), which javac 8
    refuses to read at all, and the Android build itself runs on JDK 17. So the
    check follows the Android toolchain rather than whichever JDK the
    surrounding job happens to be on.
    """
    candidates = []
    for var in ('JAVA17_HOME', 'JAVA_HOME_17', 'JAVA_HOME_21', 'JAVA_HOME_11',
                'JAVA_HOME'):
        home = os.environ.get(var)
        if home:
            candidates.append(os.path.join(home, 'bin', 'javac'))
    found = shutil.which('javac')
    if found:
        candidates.append(found)
    for javac in candidates:
        if javac_version(javac) >= 11:
            return javac
    return None


def core_classpath():
    """Where codenameone-core's classes are, compiled or installed."""
    classes = os.path.join(REPO, 'maven', 'core', 'target', 'classes')
    if os.path.isdir(classes):
        return classes
    home = os.path.expanduser('~/.m2/repository/com/codenameone/codenameone-core')
    if os.path.isdir(home):
        for version in sorted(os.listdir(home), reverse=True):
            jar = os.path.join(home, version,
                               'codenameone-core-%s.jar' % version)
            if os.path.isfile(jar):
                return jar
    return None


def android_jar():
    for candidate in (os.environ.get('CN1_BINARIES'),
                      os.path.join(REPO, 'maven', 'target', 'cn1-binaries'),
                      os.path.join(os.path.dirname(REPO), 'cn1-binaries')):
        if not candidate:
            continue
        jar = os.path.join(candidate, 'android', 'android.jar')
        if os.path.isfile(jar):
            return jar
    return None


def check_stub_guards():
    problems = []
    for name, declaration in STUB_GUARDS:
        port_file = os.path.join(PORT_SRC, name)
        if not os.path.isfile(port_file):
            continue
        with open(port_file, encoding='utf-8', errors='replace') as f:
            if declaration not in f.read():
                problems.append(
                    'scripts/cn1lib-api-check/stubs no longer matches the '
                    'Android port: %s does not declare "%s". Update the stub to '
                    'the port\'s current signature.' % (name, declaration))
    return problems


def check_library(lib, cache, classpath_base, javac):
    src = os.path.join(REPO, 'maven', lib, 'android', 'src', 'main', 'java')
    sources = list(java_sources(src)) + list(java_sources(STUBS))
    classpath = list(classpath_base)
    for group, artifact, version in pinned_artifacts(lib):
        jar = fetch(group, artifact, version, cache)
        if jar is None:
            return ['%s: could not resolve the pinned artifact %s:%s:%s named '
                    'in android.gradleDep.' % (lib, group, artifact, version)]
        classpath.append(jar)
    out = os.path.join(cache, lib + '-classes')
    os.makedirs(out, exist_ok=True)
    # The library's own portable half (the callback fan-in, the constants) is
    # on the sourcepath rather than the classpath, so the check needs nothing
    # built first.
    common = os.path.join(REPO, 'maven', lib, 'common', 'src', 'main', 'java')
    command = [javac, '-nowarn', '-Xlint:-options',
               '-source', '1.8', '-target', '1.8',
               '-encoding', 'UTF-8', '-d', out,
               '-sourcepath', common,
               '-classpath', os.pathsep.join(classpath)] + sources
    result = subprocess.run(command, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT)
    if result.returncode != 0:
        text = result.stdout.decode('utf-8', 'replace').strip()
        return ['%s: does not compile against the SDK it pins:\n%s' % (lib, text)]
    log('  %s: compiles against %s' % (
        lib, ', '.join('%s:%s' % (a, v) for _g, a, v in pinned_artifacts(lib))
        or 'core and the Android SDK alone'))
    return []


def main(argv):
    require_all = '--require-all' in argv
    wanted = [a for a in argv if not a.startswith('-')]

    problems = check_stub_guards()

    javac = resolve_javac()
    jar = android_jar()
    core = core_classpath()
    missing = []
    if javac is None:
        missing.append('a JDK 11 or newer javac (set JAVA17_HOME); the pinned '
                       'SDKs ship class files javac 8 cannot read')
    if jar is None:
        missing.append('android.jar (stage cn1-binaries, or set CN1_BINARIES)')
    if core is None:
        missing.append('codenameone-core classes (build maven/core)')
    if missing:
        message = 'check-cn1lib-android-api: missing ' + '; '.join(missing)
        if require_all:
            sys.stderr.write(message + '\n')
            return 1
        log(message + ' -- skipping')
        return 1 if problems else 0

    libs = [lib for lib in libraries() if not wanted or lib in wanted]
    if wanted:
        for name in wanted:
            if name not in libs:
                sys.stderr.write('%s ships no Android sources\n' % name)
                return 1
    log('check-cn1lib-android-api: %d librar%s'
        % (len(libs), 'y' if len(libs) == 1 else 'ies'))

    cache = os.path.join(REPO, 'maven', 'target', 'cn1lib-api-check')
    os.makedirs(cache, exist_ok=True)
    classpath_base = [jar, core]
    for lib in libs:
        problems.extend(check_library(lib, cache, classpath_base, javac))

    if problems:
        sys.stderr.write('\n')
        for problem in problems:
            sys.stderr.write(problem + '\n\n')
        return 1
    log('check-cn1lib-android-api: no findings')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
