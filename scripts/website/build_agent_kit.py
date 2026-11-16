#!/usr/bin/env python3
"""Build the files codenameone.com/agents/ hands an AI coding agent.

    python3 scripts/website/build_agent_kit.py <output-dir> [--self-test]

Writes into <output-dir> (the website build passes docs/website/static/agents):

  cn1-create.jar            The Initializr generator plus the CreateProject
                            command (scripts/initializr/agent-cli). Thin: the
                            Codename One core and JavaSE jars it runs against
                            are fetched by create-app.sh at the version below.
  cn1-create.properties     cn1.version=<the release those jars come from>,
                            kit.build and template.<name>=<hash> cache keys.
  templates/<name>.jar      One per complete-app template (SAVOR, WAYLINE, ...):
                            its resources, added to the class path by
                            create-app only when --template names it.
  codenameone-skill.zip     The project skill every generated app carries
                            (scripts/initializr/common/src/main/resources/skill).
  codenameone-bootstrap.zip The bootstrap skill (scripts/agent-kit/bootstrap):
                            JDK, project creation, spec, the build loop.
  create-app.sh / .ps1      Copied from the bootstrap skill, so a one-line
                            curl can run them without unpacking anything.

There is one source for each: the project skill is the one the Initializr and
the archetype ship, so an agent that installs it from the site reads the same
words as one that finds it inside a project.

--self-test runs the jar once (--list, then a BAREBONES project in a temporary
directory) and fails when either does not work. The website build runs it.
"""

from __future__ import annotations

import os
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REPO = 'https://repo.codenameone.com/maven2/'
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}
RESOURCES = ROOT / 'scripts/initializr/common/src/main/resources'
GRADLE_TEMPLATE = ROOT / 'maven/build-engine/src/main/resources/com/codename1/project/templates/gradle'
BOOTSTRAP = ROOT / 'scripts/agent-kit/bootstrap'
# The generator's resources for a complete-app template are named <name>-*.
# Each of those templates is its own download, templates/<name>.jar, so a
# BAREBONES project (the usual first run) fetches a ~1 MB generator rather than
# every template: they are most of the 24 MB, one game alone is 23. The preview
# images are for the web page's gallery and ship nowhere.
SPLIT_TEMPLATES = ('wayline', 'savor', 'skirmish', 'dash', 'splat', 'parley', 'bastion',
                   'reel', 'pulse', 'slate')


def template_of(name: str) -> str | None:
    for t in SPLIT_TEMPLATES:
        if name.startswith(t + '-'):
            return t
    return None


def zip_tree(source: Path, archive: Path, prefix: str = '') -> None:
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
        for path in sorted(source.rglob('*')):
            if path.is_file() and '__pycache__' not in path.parts:
                info = zipfile.ZipInfo(prefix + path.relative_to(source).as_posix())
                info.compress_type = zipfile.ZIP_DEFLATED
                mode = 0o755 if os.access(path, os.X_OK) or path.suffix == '.sh' else 0o644
                info.external_attr = (0o100000 | mode) << 16
                z.writestr(info, path.read_bytes())


def fetch(version: str, name: str, work: Path) -> Path:
    relative = f'com/codenameone/{name}/{version}/{name}-{version}.jar'
    local = os.environ.get('CN1_TEST_MAVEN_REPOSITORY')
    if local and (Path(local) / relative).is_file():
        return Path(local) / relative
    jar = work / f'{name}.jar'
    urllib.request.urlretrieve(REPO + relative, jar)
    return jar


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2
    out = Path(sys.argv[1]).resolve()
    self_test = '--self-test' in sys.argv[2:]
    out.mkdir(parents=True, exist_ok=True)
    version = ET.parse(ROOT / 'scripts/initializr/pom.xml').find('m:properties/m:cn1.version', NS).text

    with tempfile.TemporaryDirectory(prefix='cn1-agent-kit-') as directory:
        work = Path(directory)
        deps = [fetch(version, 'codenameone-core', work), fetch(version, 'codenameone-javase', work)]
        zipsupport = ROOT / 'scripts/initializr/cn1libs/ZipSupport/jars/main.zip'
        classes = work / 'classes'
        classes.mkdir()
        # The two archives the generator reads as resources; the same input and
        # layout as common/pom.xml's package-claude-skill and package-gradle-template.
        zip_tree(RESOURCES / 'skill', classes / 'skill.zip')
        zip_tree(GRADLE_TEMPLATE, classes / 'gradle.zip')
        sources = ROOT / 'scripts/initializr/common/src/main/java'
        java = sorted((sources / 'com/codename1/initializr/model').glob('*.java'))
        java += [sources / 'com/codename1/initializr/WebsiteThemeNative.java']
        java += sorted((ROOT / 'scripts/initializr/agent-cli/src').rglob('*.java'))
        cp = os.pathsep.join(map(str, deps + [zipsupport]))
        subprocess.run(['javac', '--release', '8', '-encoding', 'UTF-8', '-nowarn', '-cp', cp,
                        '-d', str(classes)] + list(map(str, java)), check=True)
        jar = out / 'cn1-create.jar'
        with zipfile.ZipFile(jar, 'w', zipfile.ZIP_DEFLATED) as z:
            z.writestr('META-INF/MANIFEST.MF',
                       'Manifest-Version: 1.0\nMain-Class: com.codename1.initializr.model.CreateProject\n'
                       f'Codename-One-Version: {version}\n')
            for path in sorted(classes.rglob('*')):
                if path.is_file():
                    z.write(path, path.relative_to(classes).as_posix())
            for path in sorted(RESOURCES.rglob('*')):
                rel = path.relative_to(RESOURCES).as_posix()
                if path.is_file() and template_of(rel) is None and not rel.endswith('-preview.jpg'):
                    z.write(path, rel)
            with zipfile.ZipFile(zipsupport) as zs:
                for entry in zs.infolist():
                    if not entry.is_dir() and not entry.filename.startswith('META-INF/'):
                        z.writestr(entry.filename, zs.read(entry))
        # kit.build changes whenever the generator does, so create-app.sh's cache
        # (keyed on both) never keeps an old jar under an unchanged release.
        import hashlib
        kit = hashlib.sha256(jar.read_bytes()).hexdigest()[:12]
        props = [f'cn1.version={version}', f'kit.build={kit}']
        (out / 'templates').mkdir(exist_ok=True)
        for t in SPLIT_TEMPLATES:
            tjar = out / 'templates' / f'{t}.jar'
            with zipfile.ZipFile(tjar, 'w', zipfile.ZIP_DEFLATED) as z:
                for path in sorted(RESOURCES.glob(t + '-*')):
                    if path.is_file() and not path.name.endswith('-preview.jpg'):
                        z.write(path, path.name)
            # The hash keys create-app's cache, as kit.build does for the generator.
            props.append(f'template.{t}=' + hashlib.sha256(tjar.read_bytes()).hexdigest()[:12])
        (out / 'cn1-create.properties').write_text('\n'.join(props) + '\n')

        zip_tree(RESOURCES / 'skill', out / 'codenameone-skill.zip', 'codename-one/')
        zip_tree(BOOTSTRAP, out / 'codenameone-bootstrap.zip', 'codename-one-bootstrap/')
        for script in ('create-app.sh', 'create-app.ps1'):
            shutil.copy2(BOOTSTRAP / script, out / script)

        if self_test:
            run_cp = os.pathsep.join(map(str, [jar] + deps))
            base = ['java', '-Djava.awt.headless=true', '-cp', run_cp,
                    'com.codename1.initializr.model.CreateProject']
            listing = subprocess.run(base + ['--list'], check=True, capture_output=True, text=True,
                                     timeout=120).stdout
            assert 'BAREBONES' in listing, listing
            project = work / 'probe'
            subprocess.run(base + ['--name', 'KitProbe', '--package', 'com.example.kitprobe',
                                   '--dir', str(project)], check=True, timeout=120)
            for name in ('AGENTS.md', 'mvnw', 'mvnw.cmd', 'pom.xml', 'common/codenameone_settings.properties'):
                assert (project / name).is_file(), name
            assert os.access(project / 'mvnw', os.X_OK), 'mvnw must be executable'
            # A split template: refused clearly without its jar, generated with it.
            missing = subprocess.run(base + ['--name', 'KitSavor', '--package', 'com.example.kitsavor',
                                             '--template', 'SAVOR', '--dir', str(work / 'nosavor')],
                                     capture_output=True, text=True, timeout=120)
            assert missing.returncode == 1 and 'templates/savor.jar' in missing.stderr, missing.stderr
            with_jar = ['java', '-Djava.awt.headless=true', '-cp',
                        os.pathsep.join(map(str, [jar, out / 'templates/savor.jar'] + deps)),
                        'com.codename1.initializr.model.CreateProject']
            subprocess.run(with_jar + ['--name', 'KitSavor', '--package', 'com.example.kitsavor',
                                       '--template', 'SAVOR', '--dir', str(work / 'savor')],
                           check=True, timeout=120)
            assert (work / 'savor/pom.xml').is_file(), 'SAVOR project'
            assert jar.stat().st_size < 5_000_000, f'cn1-create.jar grew to {jar.stat().st_size} bytes'
            print('agent kit self-test passed')
    print('Wrote the agent kit to', out)
    return 0


if __name__ == '__main__':
    sys.exit(main())
