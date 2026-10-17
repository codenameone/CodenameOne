#!/usr/bin/env python3
"""Keep an Initializr full-stack template aligned with its sample (--write to update).

    python3 scripts/sync-initializr-template.py wayline|morsel [--write]

scripts/<name> is the project CI builds and tests. The Initializr offers the same
project as a template, from five resources this script derives from it:

  <name>-src.zip       the application's sources (common/src/main, less the css)
  <name>-css.zip       its stylesheet
  <name>-pom.xml       the common module's pom: the one every Initializr project
                       gets (barebones-pom.xml), plus the dependency on `shared`
  <name>-modules.zip  the two modules a plain project does not have: `shared`,
                       the contract, and `backend`, the server that implements it
  <name>-settings.properties
                       the build hints the application needs and a plain project
                       does not have, from common/codenameone_settings.properties

Nothing else is copied. The reactor pom, the platform modules, the launchers and
the Maven wrapper are the ones every Initializr project gets, from common.zip.

Four things are changed on the way, all of them so that the generator can then
make the project the developer's own:

  * The Asciidoctor tag markers (`// tag::name[]`, `// end::name[]`) are taken
    out. The developer guide includes regions of this project through them, so
    they belong to the guide and not to a generated project.

  * `<name>` in lower case -- an artifact id, a configuration key -- is written
    `myappname`, which is the token the generator replaces with the project's
    name. The Java package is left alone: the generator replaces it as a package.
  * The licence header of this repository is taken off: a generated project is the
    developer's, and is not published under it.
  * Line endings are LF.

The tests of the application, their goldens and the CI harness stay behind. The
server's tests go along -- they are how the developer finds out what they broke.

Without --write the script only compares, and fails on a difference: a change to
scripts/<name> that the template did not get.
"""
from pathlib import Path
import io
import re
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
# The samples the Initializr offers as full-stack templates. Each is a project
# under scripts/ whose Java package is com.codenameone.examples.<name>.
SAMPLES = ('wayline', 'morsel')
args = [a for a in sys.argv[1:] if not a.startswith('--')]
if len(args) != 1 or args[0] not in SAMPLES:
    raise SystemExit('usage: sync-initializr-template.py ' + '|'.join(SAMPLES) + ' [--write]')
name = args[0]
sample = root / 'scripts' / name
resources = root / 'scripts/initializr/common/src/main/resources'

# What the generator rewrites as text; the rest is copied as bytes.
# Exactly the files the generator rewrites too (GeneratorModel.isTextFile): a
# `myappname` written into any other would stay in the generated project.
TEXT = ('.java', '.xml', '.properties', '.css', '.sh', '.md', '.json')

HEADER = re.compile(r'\A/\*\n \* Copyright \(c\) .*?\*/\n', re.S)
HASH_HEADER = re.compile(r'\A((?:#!.*\n)?)(?:#\n)?# Copyright \(c\) .*?(?=^[^#]|\Z)', re.S | re.M)
XML_HEADER = re.compile(r'<!--\s*Copyright \(c\) .*?-->\n', re.S)
# A line holding nothing but an Asciidoctor tag marker: `// tag::name[]`,
# `# end::name[]`, `/* tag::name[] */`.
TAG_MARKER = re.compile(r'^[ \t]*(?://|#|/\*)[ \t]*(?:tag|end)::[A-Za-z0-9_-]+\[\][ \t]*(?:\*/)?[ \t]*\n',
                        re.M)


def text(path):
    """A text file as the template carries it."""
    data = path.read_bytes().replace(b'\r\n', b'\n').decode('utf-8')
    if path.suffix in ('.java', '.css'):
        data = HEADER.sub('', data, count=1)
    elif path.suffix in ('.properties', '.sh'):
        data = HASH_HEADER.sub(r'\1', data, count=1)
    elif path.suffix == '.xml':
        data = XML_HEADER.sub('', data, count=1)
    # The developer guide includes regions of the sample by Asciidoctor tag. The
    # markers are the guide's plumbing and mean nothing in a generated project.
    data = TAG_MARKER.sub('', data)
    # The application's name wherever it is written in lower case -- an artifact
    # id, a configuration key, a demonstration account's address -- but not in the
    # Java package, which the generator replaces as a package.
    data = re.sub(r'(?<!examples\.)(?<!examples/)' + name, 'myappname', data)
    # And in capitals, which is how an environment variable spells a setting.
    data = data.replace(name.upper(), 'MYAPPNAME')
    return data.lstrip('\n').encode('utf-8')


def read(path):
    return text(path) if path.suffix in TEXT else path.read_bytes()


def tree(base, prefix, skip=()):
    """Every file under `base`, keyed by its path in the archive."""
    found = {}
    if not base.is_dir():
        return found
    for path in sorted(base.rglob('*')):
        relative = path.relative_to(base).as_posix()
        if not path.is_file() or path.name == '.DS_Store':
            continue
        if any(relative == s or relative.startswith(s + '/') for s in skip):
            continue
        found[prefix + relative] = read(path)
    return found


def expected():
    main = sample / 'common/src/main'
    sources = tree(main, '', skip=('css',))
    css = tree(main / 'css', '')
    modules = {}
    modules.update(tree(sample / 'shared', 'shared/', skip=('target',)))
    modules.update(tree(sample / 'backend', 'backend/', skip=('target',)))
    return {
        name + '-src.zip': sources,
        name + '-css.zip': css,
        name + '-modules.zip': modules,
        name + '-pom.xml': common_pom(),
        name + '-settings.properties': settings(),
    }


SHARED_DEPENDENCY = """        <!-- The contract shared with the server; see shared/pom.xml. -->
        <dependency>
            <groupId>${project.groupId}</groupId>
            <artifactId>myappname-shared</artifactId>
            <version>${project.version}</version>
        </dependency>
"""


def common_pom():
    """The common module's pom for the template.

    Not the sample's own: that one belongs to the reactor the sample has, built
    from the archetype in this tree, while a generated project gets the reactor in
    common.zip, and a module pom is only right for the reactor it was written for.
    The one thing the application adds to a plain project's pom is `shared`.
    """
    if '<artifactId>' + name + '-shared</artifactId>' not in (sample / 'common/pom.xml').read_text('utf-8'):
        raise SystemExit('scripts/' + name + '/common/pom.xml no longer depends on ' + name + '-shared; '
                         'the template pom is built on the assumption that it does.')
    pom = (resources / 'barebones-pom.xml').read_bytes().replace(b'\r\n', b'\n').decode('utf-8')
    marker = '    <dependencies>\n'
    if pom.count(marker) < 1:
        raise SystemExit('barebones-pom.xml has no <dependencies> to add the shared module to')
    return pom.replace(marker, marker + SHARED_DEPENDENCY, 1).encode('utf-8')


def keys(properties):
    return [line.split('=', 1)[0] for line in properties.splitlines()
            if '=' in line and not line.lstrip().startswith('#')]


def settings():
    """The build hints of the application that a plain project lacks.

    Only the keys common.zip's settings do not have: the rest of that file is the
    project's identity and the defaults every project gets, which the generator
    writes itself.
    """
    with zipfile.ZipFile(resources / 'common.zip') as common:
        plain = set(keys(common.read('common/codenameone_settings.properties').decode('utf-8')))
    lines = (sample / 'common/codenameone_settings.properties').read_text('utf-8').splitlines()
    kept = []
    for index, line in enumerate(lines):
        if '=' not in line or line.lstrip().startswith('#') or line.split('=', 1)[0] in plain:
            continue
        # The comment written above a hint is about it, and goes with it.
        start = index
        while start > 0 and lines[start - 1].startswith('# '):
            start -= 1
        kept.extend(lines[start:index + 1])
    return ('\n'.join(kept) + '\n').encode('utf-8')


def archive(entries):
    """A zip whose bytes depend on its entries and on nothing else."""
    result = io.BytesIO()
    with zipfile.ZipFile(result, 'w', zipfile.ZIP_STORED) as target:
        for name in sorted(entries):
            info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            info.external_attr = (0o755 if name.endswith('.sh') else 0o644) << 16
            target.writestr(info, entries[name])
    return result.getvalue()


def current(path):
    if not path.is_file():
        return None
    if path.suffix != '.zip':
        return path.read_bytes()
    with zipfile.ZipFile(path) as source:
        return {name: source.read(name) for name in source.namelist() if not name.endswith('/')}


def leftovers(entries):
    """A SNAPSHOT version has no business in a template: the generator stamps the
    version the Initializr was built for, and only into the reactor pom."""
    return sorted(name for name, data in entries.items()
                  if name.endswith('pom.xml') and b'-SNAPSHOT</cn1' in data)


def main():
    wanted = expected()
    poms = dict(wanted[name + '-modules.zip'])
    poms[name + '-pom.xml'] = wanted[name + '-pom.xml']
    bad = leftovers(poms)
    if bad:
        raise SystemExit('A Codename One snapshot version is pinned in ' + ', '.join(bad)
                         + '; module poms take ${cn1.version} from the reactor.')
    stale = [resource for resource, entries in wanted.items()
             if current(resources / resource) != entries]
    if stale and '--write' in sys.argv:
        for resource in stale:
            entries = wanted[resource]
            (resources / resource).write_bytes(entries if isinstance(entries, bytes)
                                               else archive(entries))
        print('Initializr ' + name + ' template updated: ' + ', '.join(stale))
        return
    if stale:
        raise SystemExit('Initializr ' + name + ' template drift: ' + ', '.join(stale)
                         + '. Run python3 scripts/sync-initializr-template.py ' + name + ' --write')
    print('Initializr ' + name + ' template parity OK')


if __name__ == '__main__':
    main()
