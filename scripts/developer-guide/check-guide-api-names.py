#!/usr/bin/env python3
"""Check that the developer guide spells Codename One class names correctly.

Two questions, both answered from the tree rather than from a list of
approved words.

1.  Every ``codenameone.com/javadoc/com/codename1/.../Foo.html`` link must
    name a class that exists.  The URL carries the full package, so this is
    exact, and a dead one is a 404 for the reader.

2.  A word that is one edit away from a Codename One class, and that appears
    nowhere in this repository's own sources, is a misspelling of that class.
    ``EnclodedImage``, ``ConectionRequest``, ``GenenricListCellRenderer``,
    ``BubbleTransiton``, ``SwipableContainer``, ``GridbagLayout``,
    ``InifiniteContainer``, ``DataChangeListener`` and ``MiGLayout`` all
    shipped in the manual, and each one sends a reader looking for a class
    that is not there.

The second question needs no allow-list because the repository answers it.
The guide legitimately names classes from other worlds -- ``NSObject``,
``UIView``, ``HKSampleQuery``, ``JMenuBar``, ``VStack`` -- and some of those
are also one edit from a Codename One class; every one of them is also a real
identifier in the ports, the native sources or the build tooling, which is
what tells them apart from a typo.  Plurals in prose (``Receipts``,
``Worlds``) are recognised as plurals rather than listed.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
GUIDE = os.path.join(ROOT, 'docs', 'developer-guide')

# Everything an application's own code can reference.
APP_ROOTS = [
    os.path.join(ROOT, 'CodenameOne', 'src'),
    os.path.join(ROOT, 'Ports', 'CLDC11', 'src'),
    os.path.join(ROOT, 'vm', 'JavaAPI', 'src'),
]
# Every source the repository holds, which is where the foreign names live.
IDENTIFIER_ROOTS = [os.path.join(ROOT, d)
                    for d in ('CodenameOne', 'Ports', 'vm', 'maven')]
IDENTIFIER_SUFFIXES = ('.java', '.m', '.h', '.mm', '.kt', '.js', '.c', '.cpp',
                       '.xml', '.gradle', '.swift', '.cs')

JAVADOC = re.compile(r'codenameone\.com/javadoc/(com/codename1/[A-Za-z0-9/]+)\.html')
# A backticked identifier, or a bare CamelCase word in prose.
WORD = re.compile(r'`([A-Z][A-Za-z0-9]{5,})`|\b([A-Z][a-z]+[A-Z][A-Za-z0-9]{3,})\b')
IDENTIFIER = re.compile(r'\b[A-Z][A-Za-z0-9]{3,}\b')


def class_index():
    """Simple and fully qualified names of every class an app can reference."""
    simple, qualified = set(), set()
    for root in APP_ROOTS:
        for path, _, files in os.walk(root):
            for name in files:
                if not name.endswith('.java'):
                    continue
                simple.add(name[:-5])
                rel = os.path.join(path, name)[len(root) + 1:-5]
                qualified.add(rel.replace(os.sep, '.'))
    return simple, qualified


def tree_identifiers():
    """Every capitalised identifier this repository's own sources use."""
    seen = set()
    for root in IDENTIFIER_ROOTS:
        for path, dirs, files in os.walk(root):
            dirs[:] = [d for d in dirs if d not in ('target', '.git', 'build')]
            for name in files:
                if not name.endswith(IDENTIFIER_SUFFIXES):
                    continue
                try:
                    with open(os.path.join(path, name), encoding='utf-8',
                              errors='ignore') as handle:
                        seen.update(IDENTIFIER.findall(handle.read()))
                except OSError:
                    continue
    return seen


# Two edits rather than one, because a transposition costs two: "BordreLayout"
# is that far from BorderLayout.  The repository's own identifiers are what
# keep the wider radius quiet.
EDIT_LIMIT = 2


def near_miss_of(word, candidates):
    """The closest candidate within EDIT_LIMIT edits, or None."""
    best = None
    for candidate in candidates:
        if abs(len(word) - len(candidate)) > EDIT_LIMIT:
            continue
        previous = list(range(len(candidate) + 1))
        for i, a in enumerate(word, 1):
            current = [i]
            for j, b in enumerate(candidate, 1):
                current.append(min(previous[j] + 1,
                                   current[j - 1] + 1,
                                   previous[j - 1] + (a != b)))
            previous = current
            if min(previous) > EDIT_LIMIT:
                break
        else:
            distance = previous[-1]
            if 0 < distance <= EDIT_LIMIT and (best is None or distance < best[1]):
                best = (candidate, distance)
    return best[0] if best else None


def is_plural_of_a_class(word, simple):
    return (word.endswith('s') and word[:-1] in simple) or \
           (word.endswith('es') and word[:-2] in simple)


def main():
    simple, qualified = class_index()
    known = tree_identifiers()
    dead_links, typos, seen = [], [], set()

    for name in sorted(os.listdir(GUIDE)):
        if not name.endswith(('.asciidoc', '.adoc')):
            continue
        with open(os.path.join(GUIDE, name), encoding='utf-8') as handle:
            text = handle.read()
        for match in JAVADOC.finditer(text):
            target = match.group(1).replace('/', '.')
            if target not in qualified:
                dead_links.append('%s: %s' % (name, target))
        for match in WORD.finditer(text):
            word = match.group(1) or match.group(2)
            if word in simple or word in known or word in seen:
                continue
            seen.add(word)
            if is_plural_of_a_class(word, simple) or word.startswith('My'):
                # "MyForm", "MyConstraint": names the guide invents for the
                # reader's own class, which are near-misses by construction.
                continue
            near = near_miss_of(word, simple)
            if near:
                typos.append((name, word, near))

    for line in dead_links:
        print('javadoc link names a class that does not exist: %s' % line,
              file=sys.stderr)
    for name, word, near in typos:
        print('%s: %s is a near-miss for %s and appears nowhere in this '
              'repository -- a misspelling' % (name, word, near),
              file=sys.stderr)

    if dead_links or typos:
        print('check-guide-api-names: %d dead javadoc link(s), %d misspelled '
              'class name(s).' % (len(dead_links), len(typos)), file=sys.stderr)
        return 1
    print('check-guide-api-names: every javadoc link resolves, and no class '
          'name is misspelled.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
