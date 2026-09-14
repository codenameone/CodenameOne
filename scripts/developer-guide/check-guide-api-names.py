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
    ``InifiniteContainer``, ``DataChangeListener``, ``MiGLayout`` and
    ``GZipInputStream`` all shipped in the manual, and each one sends a
    reader looking for a class that is not there.

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

# The last segment may name a nested class -- URLImage.ImageAdapter,
# ActionEvent.Type, LayeredLayout.LayeredLayoutConstraint.Inset -- so the dot
# belongs in the pattern. Excluding it skipped those links entirely.
JAVADOC = re.compile(
    r'codenameone\.com/javadoc/(com/codename1/[A-Za-z0-9/]+(?:\.[A-Z][A-Za-z0-9]*)*)'
    r'\.html')
# A backticked identifier, or a bare word in prose that is shaped like a class
# name. The bare form needs both shapes: a name can lead with an acronym --
# URLImage, GZIPInputStream, JSONParser -- and the CamelCase alternative alone
# skipped those, so "URLIamge" went unread. Backticks are code, so four
# characters is enough there and "Iamge" is caught.
WORD = re.compile(r'`([A-Z][A-Za-z0-9]{3,})`'
                  r'|\b([A-Z][a-z]+[A-Z][A-Za-z0-9]{3,}'
                  r'|[A-Z]{2,}[a-z][A-Za-z0-9]{2,})\b')
IDENTIFIER = re.compile(r'\b[A-Z][A-Za-z0-9]{2,}\b')
# Comments are prose, and prose carries the same typos the guide does.
BLOCK_COMMENT = re.compile(r'/\*.*?\*/', re.S)
LINE_COMMENT = re.compile(r'(?<![:"\'])//[^\n]*')
XML_COMMENT = re.compile(r'<!--.*?-->', re.S)


def strip_comments(text):
    """Removes comments, which are prose and carry the same typos.

    The whole point of reading the tree is to tell a real identifier from a
    misspelling, and a comment holds neither -- it holds English. Leaving
    them in let a typo vouch for itself: `ConectionRequest` was written into
    both the io chapter and the javadoc of com.codename1.io, and the second
    copy made this check pass over the first.
    """
    text = BLOCK_COMMENT.sub(' ', text)
    text = LINE_COMMENT.sub(' ', text)
    return XML_COMMENT.sub(' ', text)


# A type declared inside another one. Read from the source rather than
# guessed, so a javadoc link to Outer.Inner is checked as exactly as one to a
# top-level class.
NESTED = re.compile(r'\b(?:class|interface|enum)\s+([A-Z][A-Za-z0-9]*)')


def class_index():
    """Simple and fully qualified names of every class an app can reference.

    Nested types are included under their outer class -- URLImage.ImageAdapter
    -- because the guide links to them and a javadoc URL spells them that way.
    """
    simple, qualified = set(), set()
    for root in APP_ROOTS:
        for path, _, files in os.walk(root):
            for name in files:
                if not name.endswith('.java'):
                    continue
                outer = name[:-5]
                simple.add(outer)
                rel = os.path.join(path, name)[len(root) + 1:-5]
                dotted = rel.replace(os.sep, '.')
                qualified.add(dotted)
                try:
                    with open(os.path.join(path, name), encoding='utf-8',
                              errors='ignore') as handle:
                        # Comments hold sample code, and a "class MyListener"
                        # written in a javadoc example is not a type anyone
                        # can link to.
                        declared = set(NESTED.findall(
                            strip_comments(handle.read())))
                except OSError:
                    continue
                for inner in declared:
                    if inner == outer:
                        continue
                    # Flat rather than exact nesting: a javadoc URL spells a
                    # doubly nested type Outer.Middle.Inner, and every segment
                    # of it is declared in this one file.
                    simple.add(inner)
                    qualified.add(dotted + '.' + inner)
                    for other in declared:
                        if other != inner:
                            qualified.add(dotted + '.' + other + '.' + inner)
    return simple, qualified


def tree_identifiers():
    """Capitalised words this repository uses, in code and in all of it.

    Two sets, because comments cut both ways. They are the only place some
    genuinely foreign names appear -- our own notes discuss SFSpeechRecognizer,
    HKWorkoutSession and XPath -- so throwing them away would report every one
    of those. But a comment is prose and carries the same typos the guide
    does, so letting them vouch for a word let a typo vouch for itself:
    `ConectionRequest` was written into both the io chapter and the javadoc of
    com.codename1.io, and the second copy made this check pass over the first.

    So a word one character away from a Codename One class has to earn its
    place in real code; two characters away, anywhere in the tree will do.
    """
    code, anywhere = set(), set()
    for root in IDENTIFIER_ROOTS:
        for path, dirs, files in os.walk(root):
            dirs[:] = [d for d in dirs if d not in ('target', '.git', 'build')]
            for name in files:
                if not name.endswith(IDENTIFIER_SUFFIXES):
                    continue
                try:
                    with open(os.path.join(path, name), encoding='utf-8',
                              errors='ignore') as handle:
                        text = handle.read()
                except OSError:
                    continue
                anywhere.update(IDENTIFIER.findall(text))
                code.update(IDENTIFIER.findall(strip_comments(text)))
    return code, anywhere


# Two edits rather than one, because a transposition costs two: "BordreLayout"
# is that far from BorderLayout.  The repository's own identifiers are what
# keep the wider radius quiet.
EDIT_LIMIT = 2
# A class whose name is shorter than this is too generic to tell a typo from an
# ordinary word: XPath is one character from Path and is neither.
SHORTEST_DISTINGUISHING_NAME = 5


def near_miss_of(word, candidates):
    """The closest candidate within EDIT_LIMIT edits as (name, distance)."""
    best = None
    for candidate in candidates:
        if len(candidate) < SHORTEST_DISTINGUISHING_NAME:
            continue
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
    return best


def is_invented_name(word, simple, anywhere):
    """Whether this is a name the guide invents for the reader's own class.

    "MyForm", "MyConstraint", "MyCalendar": the guide writes these, and each
    is a near-miss for the class it is built from. Exempting every word that
    starts with "My" was too much -- "MygLayout" is one edit from MigLayout
    and would have been waved through on its first two letters -- so what
    follows the prefix has to be a real name in its own right.
    """
    if not word.startswith('My') or len(word) < 4 or not word[2].isupper():
        return False
    rest = word[2:]
    return rest in simple or rest in anywhere


def is_plural_of_a_class(word, simple):
    return (word.endswith('s') and word[:-1] in simple) or \
           (word.endswith('es') and word[:-2] in simple)


def main():
    simple, qualified = class_index()
    code, anywhere = tree_identifiers()
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
            if word in simple or word in seen:
                continue
            seen.add(word)
            if is_plural_of_a_class(word, simple) or \
                    is_invented_name(word, simple, anywhere):
                continue
            near = near_miss_of(word, simple)
            if near is None:
                continue
            candidate, distance = near
            vouched = code if distance == 1 else anywhere
            if word not in vouched:
                typos.append((name, word, candidate))

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
