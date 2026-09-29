#!/usr/bin/env python3
"""Download a JDK into a private directory and print its home, touching nothing else.

    python3 vm/selfhost/fetch-jdk.py 25 <directory>

The performance gate runs inside each platform's own build, and setup-java would change
JAVA_HOME and PATH for every later step of a job that is not about JDK 25 at all -- the
macOS job's screenshot run, a Windows job's JDK 21 suite. This leaves the environment
alone: the caller exports the printed home as JDK_25_HOME and nothing else changes.

Temurin from Adoptium first; Microsoft's build of OpenJDK when Adoptium has no build for
the platform or cannot be reached.
"""
import io
import os
from pathlib import Path
import platform
import shutil
import sys
import tarfile
import time
import urllib.request
import zipfile


def target():
    system = {'Linux': 'linux', 'Darwin': 'mac', 'Windows': 'windows'}[platform.system()]
    machine = platform.machine().lower()
    arch = {'x86_64': 'x64', 'amd64': 'x64', 'aarch64': 'aarch64', 'arm64': 'aarch64'}[machine]
    return system, arch


def urls(version, system, arch):
    yield ('https://api.adoptium.net/v3/binary/latest/%s/ga/%s/%s/jdk/hotspot/normal/eclipse'
           % (version, system, arch))
    ms_os = {'linux': 'linux', 'mac': 'macOS', 'windows': 'windows'}[system]
    ext = 'zip' if system == 'windows' else 'tar.gz'
    yield 'https://aka.ms/download-jdk/microsoft-jdk-%s-%s-%s.%s' % (version, ms_os, arch, ext)


def download(url):
    last = None
    for attempt in range(3):
        try:
            with urllib.request.urlopen(url, timeout=300) as response:
                return response.read()
        except Exception as error:   # network errors of every kind get the same retry
            last = error
            time.sleep(10 * (attempt + 1))
    raise RuntimeError('%s: %s' % (url, last))


def extract(data, destination):
    if data[:2] == b'PK':
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            archive.extractall(destination)
    else:
        with tarfile.open(fileobj=io.BytesIO(data), mode='r:gz') as archive:
            archive.extractall(destination)


def find_home(root, version):
    exe = 'java.exe' if platform.system() == 'Windows' else 'java'
    for java in sorted(root.rglob(exe)):
        if java.parent.name == 'bin' and java.is_file():
            home = java.parent.parent
            if platform.system() != 'Windows':
                os.chmod(java, 0o755)
            return home
    raise RuntimeError('no bin/%s under %s' % (exe, root))


def main(argv):
    if len(argv) != 2:
        sys.exit('usage: fetch-jdk.py <version> <directory>')
    version, directory = argv[0], Path(argv[1]).resolve()
    system, arch = target()
    if directory.exists():
        shutil.rmtree(directory)
    directory.mkdir(parents=True)
    errors = []
    for url in urls(version, system, arch):
        try:
            extract(download(url), directory)
            home = find_home(directory, version)
            # Unpacked archives lose the executable bit on some extractors; restore it on
            # everything in bin/ so the JDK's own launchers run.
            if platform.system() != 'Windows':
                for tool in (home / 'bin').iterdir():
                    os.chmod(tool, 0o755)
                for lib in home.rglob('jspawnhelper'):
                    os.chmod(lib, 0o755)
            print(home)
            return 0
        except Exception as error:
            errors.append(str(error))
            shutil.rmtree(directory)
            directory.mkdir(parents=True)
    sys.exit('fetch-jdk: no JDK %s for %s-%s: %s' % (version, system, arch, '; '.join(errors)))


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
