#!/usr/bin/env python3
"""Compare the build-hint key/value sets of the two CN1 scaffolding settings files.

Both the Maven archetype template (Velocity placeholders: ${mainName}, ${package},
${javaVersion}) and the initializr template (literal placeholders: MyAppName,
com.example.myapp, a fixed java version) describe the SAME generated
codenameone_settings.properties. We canonicalise those per-project placeholders and
then require the set of property entries -- including commented-out hint lines such as
``#codename1.arg.ios.onDeviceDebug=true`` -- to be identical. Comment prose and line
ordering are ignored; anything else is real drift between the two scaffolds.

Usage:
    normalize_cn1_settings.py --archetype <file> --initializr <file>

Exits 0 when the two are in sync, 1 when they have drifted.
"""

import argparse
import re
import sys

# A "setting" line is an optionally-commented codename1.* assignment. Prose comments
# (e.g. "# Modern native themes ...") and Velocity #set( ... ) directives are skipped
# because they do not match this shape.
SETTING_RE = re.compile(r'^(?P<hash>#?)\s*(?P<key>codename1\.[^=\s]+)\s*=(?P<value>.*)$')


def canonicalize_value(key, value):
    """Replace per-project placeholders so the two scaffolds line up."""
    # Application/display name placeholders.
    value = value.replace('${mainName}', '<APP>').replace('MyAppName', '<APP>')
    # Package placeholders.
    value = value.replace('${package}', '<PKG>').replace('com.example.myapp', '<PKG>')
    # Java language version: archetype leaves a ${javaVersion} token resolved at
    # generation time; initializr bakes a concrete default. Either way it is a
    # per-project choice, not a scaffold difference.
    if key == 'codename1.arg.java.version':
        return '<JAVAVER>'
    return value


def parse(path):
    """Return {entry_string} where entry_string is e.g. '#codename1.arg.ios.onDeviceDebug=true'."""
    entries = set()
    with open(path, 'r', encoding='utf-8') as handle:
        for raw in handle:
            line = raw.rstrip('\n').rstrip('\r')
            stripped = line.strip()
            if not stripped:
                continue
            if stripped.startswith('#set('):  # Velocity directive
                continue
            match = SETTING_RE.match(stripped)
            if not match:
                continue  # prose comment or non-codename1 line
            commented = bool(match.group('hash'))
            key = match.group('key')
            value = canonicalize_value(key, match.group('value').strip())
            entries.add(('#' if commented else '') + key + '=' + value)
    return entries


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--archetype', required=True)
    parser.add_argument('--initializr', required=True)
    args = parser.parse_args()

    archetype = parse(args.archetype)
    initializr = parse(args.initializr)

    if not archetype:
        print('ERROR: no codename1.* settings parsed from archetype template', file=sys.stderr)
        return 2
    if not initializr:
        print('ERROR: no codename1.* settings parsed from initializr template', file=sys.stderr)
        return 2

    missing = sorted(archetype - initializr)   # in archetype, absent from initializr
    extra = sorted(initializr - archetype)      # in initializr, absent from archetype

    if not missing and not extra:
        print('OK: archetype and initializr codenameone_settings.properties are in sync '
              '(%d hint entries).' % len(archetype))
        return 0

    print('SCAFFOLDING DRIFT: the Maven archetype and the initializr scaffolding produce '
          'different codenameone_settings.properties.\n')
    if missing:
        print('  Present in the Maven archetype but MISSING from the initializr common.zip:')
        for entry in missing:
            print('    - %s' % entry)
        print('  -> add these to common/codenameone_settings.properties inside')
        print('     scripts/initializr/common/src/main/resources/common.zip')
        print('')
    if extra:
        print('  Present in the initializr common.zip but MISSING from the Maven archetype:')
        for entry in extra:
            print('    + %s' % entry)
        print('  -> add these to')
        print('     maven/cn1app-archetype/src/main/resources/archetype-resources/common/codenameone_settings.properties')
        print('')
    print('Default initializr and default Maven scaffolding must produce identical results.')
    return 1


if __name__ == '__main__':
    sys.exit(main())
