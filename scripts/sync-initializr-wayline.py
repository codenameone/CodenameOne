#!/usr/bin/env python3
"""Keep Initializr's Wayline template aligned with scripts/wayline (--write to update).

scripts/wayline is the project CI builds and tests. The Initializr offers the same
project as a template, from five resources this script derives from it:

  wayline-src.zip      the application's sources (common/src/main, less the css)
  wayline-css.zip      its stylesheet
  wayline-pom.xml      the common module's pom: the one every Initializr project
                       gets (barebones-pom.xml), plus the dependency on `shared`
  wayline-modules.zip  the two modules a plain project does not have: `shared`,
                       the contract, and `backend`, the server that implements it
  wayline-settings.properties
                       the build hints the application needs and a plain project
                       does not have, from common/codenameone_settings.properties

Nothing else is copied. The reactor pom, the platform modules, the launchers and
the Maven wrapper are the ones every Initializr project gets, from common.zip.

Three things are changed on the way, all of them so that the generator can then
make the project the developer's own:

  * `wayline` in lower case -- an artifact id, a configuration key -- is written
    `myappname`, which is the token the generator replaces with the project's
    name. The Java package is left alone: the generator replaces it as a package.
  * The licence header of this repository is taken off: a generated project is the
    developer's, and is not published under it.
  * Line endings are LF.

The tests of the application, their goldens and the CI harness stay behind. The
server's tests go along -- they are how the developer finds out what they broke.

Without --write the script only compares, and fails on a difference: a change to
scripts/wayline that the template did not get.
"""
from pathlib import Path
import io
import re
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
sample = root / 'scripts/wayline'
resources = root / 'scripts/initializr/common/src/main/resources'

# What the generator rewrites as text; the rest is copied as bytes.
# Exactly the files the generator rewrites too (GeneratorModel.isTextFile): a
# `myappname` written into any other would stay in the generated project.
TEXT = ('.java', '.xml', '.properties', '.css', '.sh', '.md', '.json')

HEADER = re.compile(r'\A/\*\n \* Copyright \(c\) .*?\*/\n', re.S)
HASH_HEADER = re.compile(r'\A((?:#!.*\n)?)(?:#\n)?# Copyright \(c\) .*?(?=^[^#]|\Z)', re.S | re.M)
XML_HEADER = re.compile(r'<!--\s*Copyright \(c\) .*?-->\n', re.S)


def text(path):
    """A text file as the template carries it."""
    data = path.read_bytes().replace(b'\r\n', b'\n').decode('utf-8')
    if path.suffix in ('.java', '.css'):
        data = HEADER.sub('', data, count=1)
    elif path.suffix in ('.properties', '.sh'):
        data = HASH_HEADER.sub(r'\1', data, count=1)
    elif path.suffix == '.xml':
        data = XML_HEADER.sub('', data, count=1)
    # The application's name wherever it is written in lower case -- an artifact
    # id, a configuration key, a demonstration account's address -- but not in the
    # Java package, which the generator replaces as a package.
    data = re.sub(r'(?<!examples\.)(?<!examples/)wayline', 'myappname', data)
    # And in capitals, which is how an environment variable spells a setting.
    data = data.replace('WAYLINE', 'MYAPPNAME')
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
        'wayline-src.zip': sources,
        'wayline-css.zip': css,
        'wayline-modules.zip': modules,
        'wayline-pom.xml': common_pom(),
        'wayline-settings.properties': settings(),
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
    if '<artifactId>wayline-shared</artifactId>' not in (sample / 'common/pom.xml').read_text('utf-8'):
        raise SystemExit('scripts/wayline/common/pom.xml no longer depends on wayline-shared; '
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
    poms = dict(wanted['wayline-modules.zip'])
    poms['wayline-pom.xml'] = wanted['wayline-pom.xml']
    bad = leftovers(poms)
    if bad:
        raise SystemExit('A Codename One snapshot version is pinned in ' + ', '.join(bad)
                         + '; module poms take ${cn1.version} from the reactor.')
    stale = [name for name, entries in wanted.items() if current(resources / name) != entries]
    if stale and '--write' in sys.argv:
        for name in stale:
            entries = wanted[name]
            (resources / name).write_bytes(entries if isinstance(entries, bytes) else archive(entries))
        print('Initializr Wayline template updated: ' + ', '.join(stale))
        return
    if stale:
        raise SystemExit('Initializr Wayline template drift: ' + ', '.join(stale)
                         + '. Run python scripts/sync-initializr-wayline.py --write')
    print('Initializr Wayline template parity OK')


if __name__ == '__main__':
    main()
