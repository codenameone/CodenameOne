#!/usr/bin/env python3
"""Compile and run the real Initializr generator with its shipped resource ZIPs."""
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

root = Path(__file__).resolve().parents[2]
output = Path(sys.argv[1]).resolve()
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
version = ET.parse(root / 'scripts/initializr/pom.xml').find('m:properties/m:cn1.version', ns).text
with tempfile.TemporaryDirectory(prefix='cn1-generator-') as directory:
    work = Path(directory)
    deps = []
    for name in ['codenameone-core', 'codenameone-javase']:
        relative = Path('com/codenameone') / name / version / (name + '-' + version + '.jar')
        cached = Path(os.environ.get('CN1_TEST_MAVEN_REPOSITORY', Path.home() / '.m2/repository')) / relative
        if cached.is_file():
            deps.append(cached)
        else:
            jar = work / (name + '.jar')
            urllib.request.urlretrieve('https://repo.codenameone.com/maven2/' + relative.as_posix(), jar)
            deps.append(jar)
    deps.append(root / 'scripts/initializr/cn1libs/ZipSupport/jars/main.zip')
    classes = work / 'classes'
    classes.mkdir()
    resources = root / 'scripts/initializr/common/src/main/resources'
    # Same input and layout as common/pom.xml's package-claude-skill execution.
    with zipfile.ZipFile(classes / 'skill.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in sorted((resources / 'skill').rglob('*')):
            if path.is_file():
                archive.write(path, path.relative_to(resources / 'skill').as_posix())
    sources = root / 'scripts/initializr/common/src/main/java'
    java = list((sources / 'com/codename1/initializr/model').glob('*.java'))
    java += [sources / 'com/codename1/initializr/WebsiteThemeNative.java', root / 'scripts/tests/GenerateInitializr.java']
    cp = os.pathsep.join(map(str, deps))
    subprocess.run(['javac', '--release', '8', '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes)] + list(map(str, java)), check=True)
    subprocess.run(['java', '-Djava.awt.headless=true', '-cp', cp + os.pathsep + str(classes) + os.pathsep + str(resources),
                    'com.codename1.initializr.model.GenerateInitializr', str(output)], check=True, timeout=60)
    for archive in sorted(output.glob('*.zip')):
        with zipfile.ZipFile(archive) as z:
            assert z.testzip() is None
            for entry in z.infolist():
                expected = 0o100755 if entry.filename in ('build.sh', 'run.sh', 'mvnw') else 0o100644
                assert entry.create_system == 3 and entry.external_attr >> 16 == expected, entry.filename
            readme = z.read('README.md').decode()
            assert 'JDK ' + ('17' if 'JAVA_17' in archive.name else '8') + ' or newer' in readme
            assert '.\\build.bat javascript_cloud' in readme and './build.sh javascript_cloud' in readme
            if archive.name.startswith('ECLIPSE'):
                assert 'Run/Debug > Launch Configurations' in readme
        subprocess.run([sys.executable, str(root / 'scripts/test-starter-launchers.py'), str(archive)], check=True)
    print('PASS: real Initializr ZIPs across all IDEs and Java 8/17 options')
