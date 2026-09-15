#!/usr/bin/env python3
"""Check that the developer guide spells Codename One class names correctly.

Two questions, both answered from the tree rather than from a list of
approved words.

1.  Every ``codenameone.com/javadoc/com/codename1/.../Foo.html`` link must
    name a class the published javadoc carries.  The URL spells the full
    package, so this is exact, and a dead one is a 404 for the reader.
    ``com.codename1.impl`` and package-private types are not published --
    see ``.github/scripts/build_javadocs.sh`` -- so a link to one is dead
    even though the source is right here.

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
# Exactly what the published javadoc is built from, which
# .github/scripts/build_javadocs.sh spells out: CodenameOne/src and the
# java.* compatibility classes in Ports/CLDC11/src. The guide links to
# java/util/List.html, so a typo there has to be caught the same way one in
# com.codename1 is -- and vm/JavaAPI/src is deliberately absent, because
# javadoc never sees it and a link to a class only IT has is dead.
APP_ROOTS = [
    os.path.join(ROOT, 'CodenameOne', 'src'),
    os.path.join(ROOT, 'Ports', 'CLDC11', 'src'),
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
    r'codenameone\.com/javadoc/([a-z][A-Za-z0-9]*(?:/[A-Za-z0-9]+)*'
    r'(?:\.[A-Z][A-Za-z0-9]*)*)\.html')
# A backticked identifier, or a bare word in prose that is shaped like a class
# name. The bare form needs both shapes: a name can lead with an acronym --
# URLImage, GZIPInputStream, JSONParser -- and the CamelCase alternative alone
# skipped those, so "URLIamge" went unread. Backticks are code, so four
# characters is enough there and "Iamge" is caught.
WORD = re.compile(
    # In a code span, whether or not the name is the whole of it:
    # `Iamge`, `Iamge.createImage()`.
    r'`([A-Z][A-Za-z0-9]{4,})[`.(]'
    # Spelled like a class in prose, by internal capitals or a leading acronym.
    r'|\b([A-Z][a-z]+[A-Z][A-Za-z0-9]{3,}|[A-Z]{2,}[a-z][A-Za-z0-9]{2,})\b'
    # As a link label: Image.html[Iamge].
    r'|\[([A-Z][A-Za-z0-9]{4,})\]'
    # Reading a member off it: Iamge.createImage().
    r'|\b([A-Z][a-z]{4,})\s*\.\s*[a-z][A-Za-z0-9]*\s*\(')
# Every one of those carries a signal that the token is code -- backticks, a
# link target, a member call. A bare capitalised word in prose carries none,
# and asking for it is not a near miss away from a class name, it IS one:
# measured over this guide, that alternative reported Threat for Thread,
# Imagine for Image, Managing for Ranging and 41 more, all ordinary English.
# A one-word class named in running prose is out of reach here, and a wrong
# name in a code span, a link or a call is not.
IDENTIFIER = re.compile(r'\b[A-Z][A-Za-z0-9]{2,}\b')
# Comments are prose, and prose carries the same typos the guide does.
def strip_comments(text, mask_literals=False):
    """Removes comments, which are prose and carry the same typos.

    The whole point of reading the tree is to tell a real identifier from a
    misspelling, and a comment holds neither -- it holds English. Leaving
    them in let a typo vouch for itself: `ConectionRequest` was written into
    both the io chapter and the javadoc of com.codename1.io, and the second
    copy made this check pass over the first.

    String literals are respected rather than pattern-matched around. A
    lookbehind for ':' was not enough -- "file:///" carries a second slash
    pair no lookbehind sees, and cutting the line there removed an opening
    brace, unbalancing the nesting scan and losing every type declared after
    it in that file.

    `mask_literals` blanks their contents as well, which the declaration scan
    needs and the identifier scan must not have; see the note below.
    """
    out = []
    at = 0
    length = len(text)
    while at < length:
        ch = text[at]
        if ch == '"' or ch == "'":
            end = at + 1
            while end < length:
                if text[end] == '\\':
                    end += 2
                    continue
                if text[end] == ch or text[end] == '\n':
                    end += 1
                    break
                end += 1
            # Blanked for the declaration scan, where a literal's contents are
            # data: a brace inside one -- String x = "}" -- was counted as
            # structure, which closed a type early and made everything nested
            # after it look top-level. JSONSanitizer and RECompiler both hold
            # such literals.
            #
            # Kept when the caller is collecting identifiers, because a name
            # written in a literal is still a name this code uses: OAuth2 and
            # Slide appear that way and are not misspellings of anything.
            out.append(' ' * (end - at) if mask_literals else text[at:end])
            at = end
        elif text.startswith('//', at):
            newline = text.find('\n', at)
            at = length if newline < 0 else newline
        elif text.startswith('/*', at):
            close = text.find('*/', at + 2)
            out.append(' ')
            at = length if close < 0 else close + 2
        elif text.startswith('<!--', at):
            close = text.find('-->', at + 4)
            out.append(' ')
            at = length if close < 0 else close + 3
        else:
            out.append(ch)
            at += 1
    return ''.join(out)



# A type declaration, with the modifiers that decide whether javadoc publishes
# it. Nested types are read from the source rather than guessed, so a link to
# Outer.Inner is checked as exactly as one to a top-level class.
DECLARATION = re.compile(
    # Annotations may sit between the modifiers and the keyword, and Java
    # allows them in any order: "public @Documented @Retention(...) @interface
    # Inherited" is how the CLDC annotations are written. Stopping the group at
    # the first '@' left those looking package-private, so their pages were
    # called dead.
    r'(?P<modifiers>(?:\b(?:public|protected|private|static|final|abstract'
    r'|strictfp|sealed|non-sealed)\s+'
    r'|@(?!\s*interface\b)[A-Za-z][A-Za-z0-9.]*(?:\s*\([^)]*\))?\s*)*)'
    # The annotation form has to come first, and the plain "interface" must
    # refuse to match inside it: \b cannot match before '@', so
    # "public @interface Route" was matched from its inner token and lost the
    # modifier, leaving every annotation out of the index.
    r'(?P<kind>@\s*interface|\bclass|(?<!@)(?<!@ )\binterface|\benum)'
    r'\s+(?P<name>[A-Z][A-Za-z0-9]*)')
# Declarations and braces in one pass, so a file is read once.
TOKEN = re.compile(DECLARATION.pattern + r'|[{}]')
# The published javadoc drops this package and everything under it; see
# .github/scripts/build_javadocs.sh, which greps those sources out of its
# argfile and passes -exclude on top.
UNPUBLISHED_PACKAGE = 'com.codename1.impl'


HIDDEN_DOC = re.compile(
    r'(?:/\*\*(?:[^*]|\*(?!/))*?@hidden(?:[^*]|\*(?!/))*?\*/'
    r'|(?:^[ \t]*///[^\n]*\n)*^[ \t]*///[^\n]*@hidden[^\n]*\n(?:[ \t]*///[^\n]*\n)*)'
    r'[\s]*(?:@[A-Za-z][A-Za-z0-9.]*(?:\([^)]*\))?[\s]*)*'
    r'(?:(?:public|protected|private|static|final|abstract|strictfp|sealed'
    r'|non-sealed)\s+)*'
    r'(?:@\s*interface|class|interface|enum)\s+(?P<name>[A-Z][A-Za-z0-9]*)',
    re.M)


def hidden_types(raw):
    """Names javadoc will not publish because the doc says @hidden.

    Visibility is not the whole answer: com.codename1.vpn.tunnel.TunnelHost and
    TunnelBuffers are public and have no page, because the tag tells javadoc to
    leave them out. Read from the raw source, since comments are what carries
    the tag and the scan below runs on a stripped copy.
    """
    return {match.group('name') for match in HIDDEN_DOC.finditer(raw)}


def declared_types(source):
    """Every type in one compilation unit, with its real nesting.

    Yields ``(trail, published)``, where trail runs from the outermost type
    inwards -- so URLImage.ImageAdapter comes out as a pair and two sibling
    interfaces never join. Taking every ordered pair of declarations instead
    invented URLImage.RequestDecorator.ErrorCallback, which has no page, and
    accepted a guide link to it.

    Publication follows javadoc's ``-protected``: a type has a page when it
    and every type enclosing it are public or protected.
    """
    out = []
    stack = []
    depth = 0
    pending = None
    # One pass. Searching for the next declaration from each position instead
    # re-scanned the rest of the file every time, which is quadratic and took
    # a minute over the tree.
    for token in TOKEN.finditer(source):
        text = token.group(0)
        if text == '{':
            if pending is not None:
                name, visible, kind = pending
                # A member of an interface is implicitly public, so
                # Mapper.Direct has a page although it carries no modifier.
                if not visible and stack and stack[-1][1][2] == 'interface':
                    visible = True
                stack.append((depth, (name, visible, kind)))
                trail = [n for _, (n, _, _) in stack]
                published = all(flag for _, (_, flag, _) in stack)
                out.append((trail, published))
                pending = None
            depth += 1
        elif text == '}':
            depth -= 1
            while stack and stack[-1][0] >= depth:
                stack.pop()
            pending = None
        else:
            modifiers = token.group('modifiers').split()
            kind = 'interface' if 'interface' in token.group('kind') else 'class'
            pending = (token.group('name'),
                       'public' in modifiers or 'protected' in modifiers,
                       kind)
    return out


def class_index():
    """Simple and fully qualified names of every class the guide may link to.

    Nested types are included under their outer class -- URLImage.ImageAdapter
    -- because the guide links to them and a javadoc URL spells them that way.
    A type the published javadoc does not carry is left out, since the point
    of the link check is that the reader reaches a page.
    """
    simple, qualified = set(), set()
    for root in APP_ROOTS:
        for path, _, files in os.walk(root):
            for name in files:
                if not name.endswith('.java'):
                    continue
                outer = name[:-5]
                rel = os.path.join(path, name)[len(root) + 1:-5]
                package = rel.replace(os.sep, '.').rsplit('.', 1)[0]
                if package == UNPUBLISHED_PACKAGE or \
                        package.startswith(UNPUBLISHED_PACKAGE + '.'):
                    continue
                try:
                    with open(os.path.join(path, name), encoding='utf-8',
                              errors='ignore') as handle:
                        raw = handle.read()
                except OSError:
                    continue
                # Comments hold sample code, and a "class MyListener" written
                # in a javadoc example is not a type anyone can link to -- but
                # they also carry @hidden, which has to be read first.
                hidden = hidden_types(raw)
                source = strip_comments(raw, mask_literals=True)
                for trail, published in declared_types(source):
                    if any(step in hidden for step in trail):
                        continue
                    if not published or trail[0] != outer:
                        # javadoc runs with -protected, so a package-private
                        # type has no page even though the source is here.
                        continue
                    simple.add(trail[-1])
                    qualified.add('.'.join([package] + trail))
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
            word = next(group for group in match.groups() if group)
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
