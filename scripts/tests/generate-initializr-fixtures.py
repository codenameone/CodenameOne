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


def check_gradle_fixture(archive, z, maven_settings):
    """A Gradle download: one project at the root, the shared template's build files,
    nothing of Maven's, and an extractable, executable wrapper."""
    names = set(z.namelist())
    backend_only = '-BACKEND_ONLY-' in archive.name
    for name in names:
        assert not name.startswith('common/') and not name.endswith('pom.xml'), (archive.name, name)
        assert not name.startswith(('mvnw', '.mvn/')) and name not in ('build.sh', 'run.sh', 'build.bat', 'run.bat'), \
            (archive.name, name)
    for name in ['settings.gradle.kts', 'build.gradle.kts', 'gradle.properties', '.gitignore', 'gradlew',
                 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar', 'gradle/wrapper/gradle-wrapper.properties']:
        assert name in names, (archive.name, name)
    settings = z.read('settings.gradle.kts').decode()
    assert 'id("com.codenameone") version "' in settings and 'rootProject.name = "launcherprobe"' in settings, settings
    readme = z.read('README.md').decode()
    assert 'JDK 17 or newer' in readme and './gradlew' in readme and 'mvn' not in readme, readme
    if backend_only:
        assert 'codenameone_settings.properties' not in names and 'application.properties' in names, archive.name
    else:
        # The app's settings are common.zip's, moved to the root: byte-identical to
        # what a Maven download ships in common/, which scaffolding-parity.yml holds
        # to the archetype.
        assert z.read('codenameone_settings.properties') == maven_settings, archive.name
        assert 'mvn ' not in z.read('AGENTS.md').decode(), archive.name
    if os.name != 'nt':
        with tempfile.TemporaryDirectory(prefix='cn1-gradle-fixture-') as extract:
            subprocess.run(['unzip', '-q', str(archive), '-d', extract], check=True)
            assert os.access(Path(extract) / 'gradlew', os.X_OK), 'gradlew must extract executable without chmod'


PLATFORM_DIRS = ('android/', 'ios/', 'javase/', 'javascript/', 'linux/', 'win/')


def check_maven_layout_fixture(archive, z):
    """A Maven download with the layout choices: MAVEN-<layout>-<IDE>.zip."""
    names = set(z.namelist())
    layout = archive.name.split('-')[1]
    root_pom = z.read('pom.xml').decode()
    if layout == 'BACKEND_ONLY':
        for name in names:
            assert not name.startswith(('common/', 'backend/') + PLATFORM_DIRS), (archive.name, name)
            assert name not in ('build.sh', 'run.sh', 'build.bat', 'run.bat', 'AGENTS.md'), (archive.name, name)
            assert not (name.endswith('/pom.xml')), (archive.name, name)
            if name.endswith(('.java', '.properties', '.md', '.xml', '.json')):
                assert 'gradle' not in z.read(name).decode().lower(), (archive.name, name)
        assert '<parent>' not in root_pom and '<artifactId>codenameone-backend</artifactId>' in root_pom, archive.name
        for name in ['mvnw', 'mvnw.cmd', 'application.properties', 'application-dev.properties',
                     'src/main/java/com/example/probe/Api.java', 'README.md']:
            assert name in names, (archive.name, name)
        assert './mvnw cn1:backend' in z.read('src/main/java/com/example/probe/Api.java').decode(), archive.name
        return False
    full = layout == 'FULL'
    for name in names:
        if not full:
            assert not name.startswith(PLATFORM_DIRS), (archive.name, name)
        if layout == 'APP':
            assert not name.startswith('backend/'), (archive.name, name)
    assert ('backend/pom.xml' in names) == (layout in ('APP_WITH_BACKEND', 'FULL')), archive.name
    assert ('<activeByDefault>' in root_pom) == full, archive.name
    assert '<exists>${basedir}/android/pom.xml</exists>' in root_pom, archive.name
    common_pom = z.read('common/pom.xml').decode()
    assert '<goal>compile-javase-natives</goal>' in common_pom, archive.name
    assert common_pom.count('<id>simulator</id>') == 1, archive.name
    return True


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
    # Same input and layout as common/pom.xml's package-claude-skill and
    # package-gradle-template executions.
    gradle_template = root / 'maven/build-engine/src/main/resources/com/codename1/project/templates/gradle'
    for name, source in [('skill.zip', resources / 'skill'), ('gradle.zip', gradle_template)]:
        with zipfile.ZipFile(classes / name, 'w', zipfile.ZIP_DEFLATED) as archive:
            for path in sorted(source.rglob('*')):
                if path.is_file():
                    archive.write(path, path.relative_to(source).as_posix())
    sources = root / 'scripts/initializr/common/src/main/java'
    java = list((sources / 'com/codename1/initializr/model').glob('*.java'))
    java += [sources / 'com/codename1/initializr/WebsiteThemeNative.java', root / 'scripts/tests/GenerateInitializr.java']
    cp = os.pathsep.join(map(str, deps))
    subprocess.run(['javac', '--release', '8', '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes)] + list(map(str, java)), check=True)
    subprocess.run(['java', '-Djava.awt.headless=true', '-cp', cp + os.pathsep + str(classes) + os.pathsep + str(resources),
                    'com.codename1.initializr.model.GenerateInitializr', str(output)], check=True, timeout=60)
    maven_settings = zipfile.ZipFile(output / 'INTELLIJ-JAVA_17.zip').read('common/codenameone_settings.properties')
    for archive in sorted(output.glob('*.zip')):
        with zipfile.ZipFile(archive) as z:
            assert z.testzip() is None
            for entry in z.infolist():
                expected = 0o100755 if entry.filename in ('build.sh', 'run.sh', 'mvnw', 'gradlew') else 0o100644
                assert entry.create_system == 3 and entry.external_attr >> 16 == expected, entry.filename
            if archive.name.startswith('GRADLE-'):
                check_gradle_fixture(archive, z, maven_settings)
                continue
            if archive.name.startswith('MAVEN-') and not check_maven_layout_fixture(archive, z):
                continue
            readme = z.read('README.md').decode()
            assert 'JDK ' + ('8' if 'JAVA_8' in archive.name else '17') + ' or newer' in readme
            assert '.\\build.bat javascript_cloud' in readme and './build.sh javascript_cloud' in readme
            if archive.name.startswith('ECLIPSE'):
                assert 'Run/Debug > Launch Configurations' in readme
        subprocess.run([sys.executable, str(root / 'scripts/test-starter-launchers.py'), str(archive)], check=True)
    print('PASS: real Initializr ZIPs across all IDEs, Java 8/17 Maven, every Maven layout and every Gradle project type')
