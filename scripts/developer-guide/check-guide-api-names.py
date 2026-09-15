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
# Where a word can earn its place as a real name rather than a typo. Wider
# than the published sources on purpose: the guide names NSObject, UIView,
# HKSampleQuery and NSLocale, and what tells those from a misspelling is that
# the ports and the tooling really use them.
#
# The workflow does not trigger on all of these, and deliberately. This set
# only ever SUPPRESSES a finding -- a word is reported when it is NOT vouched
# for -- so a change here that runs without this job cannot let a dead link or
# a misspelling through. Removing the last use of a name can only make the
# next run report MORE, which is visible and cheap to answer. The half that
# could go stale silently is the link index, and that reads APP_ROOTS, both of
# which the workflow watches. Triggering on Ports/**, vm/** and maven/**
# instead would run this job -- maven install, the demo build and the
# screenshots -- for nearly every change in the repository.
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
    # Spelled like a class in prose, by internal capitals or a leading acronym.
    r'\b([A-Z][a-z]+[A-Z][A-Za-z0-9]{3,}|[A-Z]{2,}[a-z][A-Za-z0-9]{2,})\b'
    # As a link label that is exactly the name: Image.html[Iamge]. A label
    # with more words in it is read by JAVADOC_LABEL below.
    r'|\[([A-Z][A-Za-z0-9]{3,})\]'
    # Reading a member off it: Iamge.createImage().
    r'|\b([A-Z][a-z]{4,})\s*\.\s*[a-z][A-Za-z0-9]*\s*\(')
# A code span is read whole rather than from its first word, because the name
# is rarely the whole of it: `new Iamge()`, `com.codename1.ui.Iamge`.
# Not after '/' or '-': those are a URL path segment and a command-line flag,
# neither of which is a class. CocoaPods/Specs and -Dtarget are both in the
# guide and both read as near misses without this.
# The visible text of a javadoc link, which names an API however many words
# it takes: Image.html[Iamge class] and Image.html[an Iamge] both name one.
# Only javadoc labels, because an ordinary AsciiDoc bracket holds alt text and
# attributes -- prose, where a capitalised word is not a class.
JAVADOC_LABEL = re.compile(
    r'codenameone\.com/javadoc/[A-Za-z0-9/.]+\.html'
    # A link to a member carries a fragment, and its label is the signature --
    # createFromImage(Image, boolean) names a type in the guide today.
    r'(?:#[^\[\]\n]{0,200})?'
    r'\[([^\]\n]{1,200})\]')
CODE_SPAN = re.compile(r'`([^`\n]{1,400})`')
# Four characters, not five: a five-character class that loses one is four
# long, and `new Imag()` was never even extracted. The candidate still has to
# be five, so this only widens what is looked AT, not what it is compared to.
SPAN_NAME = re.compile(r'(?<![-/\w])([A-Z][A-Za-z0-9]{3,})\b')
# Used where only a class can go: after `new`, or with a member read off it.
# That is the difference between `new Buttons()`, which is a class that does
# not exist, and `Worlds` or `Get All Subscription Statuses`, which are a
# product and an Apple endpoint the guide names in code spans.
CLASS_POSITION = re.compile(
    # Constructed,
    r'\bnew\s+([A-Z][A-Za-z0-9]{4,})\b'
    # a member called on it,
    r'|\b([A-Z][A-Za-z0-9]{4,})\s*\.\s*[a-z][A-Za-z0-9]*\s*\('
    # a constant read off it, or its class literal. Both say the token is a
    # type as plainly as a constructor does: Images.DEFAULT and Images.class
    # were read as the plural of Image.
    r'|\b([A-Z][A-Za-z0-9]{4,})\s*\.\s*(?:[A-Z][A-Z0-9_]*\b|class\b)'
    # And the places a type is written without being used: a declaration, a
    # cast, a type argument, an array. "ARAnchors anchors" names a type that
    # does not exist and was exempted as the plural of ARAnchor.
    r'|\b([A-Z][A-Za-z0-9]{4,})\s+[a-z][A-Za-z0-9]*\b'
    r'|\(\s*([A-Z][A-Za-z0-9]{4,})\s*\)'
    # Any type argument, not only the first: Map<String, ARAnchors> names one
    # after a comma. The delimiters are matched by look-around rather than
    # consumed, or the comma that ends one argument is eaten by the match for
    # the argument before it and the last one is never seen.
    r'|(?<=[<,])\s*([A-Z][A-Za-z0-9]{4,})\s*(?=[,>])'
    r'|\b([A-Z][A-Za-z0-9]{4,})\s*\[\s*\]'
    # And an annotation use, which names a type as surely as a declaration
    # does: `@Routes` is not the plural of anything, it is a type that does
    # not exist.
    r'|@([A-Z][A-Za-z0-9]{4,})\b')
# What a class extends or implements. Read as a whole clause rather than as an
# alternative above, because an implements list holds several names and a
# pattern that matched the first consumed it, leaving the rest unseen.
INHERITS = re.compile(r'\b(?:extends|implements)\s+([^{;`\n]{1,200})')
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
    # The argument list balances one level of nesting, because an annotation
    # can take another as an argument: @Foo(value=@Bar(type=Baz.class)).
    # Stopping at the first ')' left the match restarting at `class` without
    # the `public` in front of it.
    r'|@(?!\s*interface\b)[A-Za-z][A-Za-z0-9.]*'
    r'(?:\s*\((?:[^()]|\([^()]*\))*\))?\s*)*)'
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

    ``@hidden`` is matched by simple name rather than by nesting path, so two
    nested types in one file that share a name share the marker. No such pair
    exists in the published sources -- checked over all of them -- and if one
    were written the effect is a valid link reported as dead, which fails
    loudly and in the file that caused it rather than letting anything
    through.
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
                #
                # Directly in its body, not anywhere inside it: a type declared
                # in a default or static method is local to that method, and
                # javadoc publishes no page for one. The enclosing interface is
                # still on the stack there, so without the depth test a local
                # class was read as a published member.
                if not visible and stack \
                        and stack[-1][1][2] == 'interface' \
                        and depth == stack[-1][0] + 1:
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
    """The closest classes within EDIT_LIMIT edits, as (names, distance).

    Every candidate at the minimum distance, sorted, rather than whichever the
    set happened to yield first. A word can sit the same distance from several
    classes -- RAModel is two edits from both ARModel and Model -- and the set
    iterates in hash order, which changes with the interpreter's seed. That
    was cosmetic while the answer only named a class in the message; it stopped
    being cosmetic when the rule about rearrangements started reading it, since
    RAModel rearranges ARModel and does not rearrange Model, so the same input
    passed or failed depending on the seed.
    """
    best = None
    names = []
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
            if not 0 < distance <= EDIT_LIMIT:
                continue
            if best is None or distance < best:
                best, names = distance, [candidate]
            elif distance == best:
                names.append(candidate)
    if best is None:
        return None
    return sorted(names), best


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
    """Whether this is a class name with a plural ending, as prose writes it.

    Not asked where only a class can go -- after `new`, or with a member read
    off it -- because there the same shape is a name that does not exist:
    `new Buttons()` and `Images.createImage()` were waved through as plurals
    of Button and Image. Everywhere else it holds: the guide names a product
    called `Worlds` and an Apple endpoint called `Get All Subscription
    Statuses`, both in code spans.
    """
    return (word.endswith('s') and word[:-1] in simple) or \
           (word.endswith('es') and word[:-2] in simple)


def class_positions():
    """Every word the guide uses where only a type can go.

    Read over the whole guide first, so a word judged in one chapter is
    judged the same way in every other.
    """
    found = set()
    for name in sorted(os.listdir(GUIDE)):
        if not name.endswith(('.asciidoc', '.adoc')):
            continue
        with open(os.path.join(GUIDE, name), encoding='utf-8') as handle:
            text = handle.read()
        for span in CODE_SPAN.finditer(text):
            body = span.group(1)
            for match in CLASS_POSITION.finditer(body):
                found.add(next(g for g in match.groups() if g))
            for clause in INHERITS.finditer(body):
                for match in SPAN_NAME.finditer(clause.group(1)):
                    found.add(match.group(1))
        for match in WORD.finditer(text):
            if match.lastindex == 3:
                found.add(match.group(3))
    return found


def main():
    simple, qualified = class_index()
    code, anywhere = tree_identifiers()
    dead_links, typos, seen = [], [], set()
    # Every place in the guide where a token stands as a type, gathered before
    # anything is judged. Collecting it per file made the answer depend on
    # which file came first: a chapter writing "Buttons" as a plural put the
    # word in `seen`, and a later chapter's `new Buttons()` was skipped as
    # already-considered before its position was read.
    in_class_position = class_positions()

    for name in sorted(os.listdir(GUIDE)):
        if not name.endswith(('.asciidoc', '.adoc')):
            continue
        with open(os.path.join(GUIDE, name), encoding='utf-8') as handle:
            text = handle.read()
        for match in JAVADOC.finditer(text):
            target = match.group(1).replace('/', '.')
            if target not in qualified:
                dead_links.append('%s: %s' % (name, target))
        # Each word with whether it was written as code, because the plural
        # rule depends on it: "Buttons" in prose is the plural of a class, and
        # `new Buttons()` is a class that does not exist.
        words = []
        for span in CODE_SPAN.finditer(text):
            for match in SPAN_NAME.finditer(span.group(1)):
                words.append(match.group(1))
        for label in JAVADOC_LABEL.finditer(text):
            for match in SPAN_NAME.finditer(label.group(1)):
                words.append(match.group(1))
        for match in WORD.finditer(text):
            words.append(next(group for group in match.groups() if group))
        for word in words:
            if word in simple or word in seen:
                continue
            seen.add(word)
            if is_invented_name(word, simple, anywhere):
                continue
            if word not in in_class_position \
                    and is_plural_of_a_class(word, simple):
                continue
            near = near_miss_of(word, simple)
            if near is None:
                continue
            tied, distance = near
            if len(word) < SHORTEST_DISTINGUISHING_NAME and distance > 1:
                # A short word is read so that a five-character class losing a
                # character -- Imag for Image -- is looked at, but two edits
                # from something that short is no longer a resemblance: Matt,
                # a name in the credits, is two from the class Mat22.
                continue
            # The first alphabetically is what the message names, and every
            # tied class decides the rule: if any of them is a rearrangement
            # the word is treated as one, so the answer cannot depend on which
            # candidate was picked.
            candidate = tied[0]
            # Vouching still applies in a class position. Requiring a real
            # Codename One class there sounds stricter and is wrong: the guide
            # calls members off platform classes it does not ship --
            # `Context.openFileInput()` is Android's, two edits from Contact --
            # and each would be reported. So a name that is a real identifier
            # in this repository is accepted wherever it appears; what the
            # position changes is the plural rule above.
            # One edit away, or the same letters in a different order, and a
            # comment cannot vouch for it: those are what a typo looks like,
            # and a typo copied into a comment was vouching for itself --
            # which is how `ConectionRequest` survived in both the io chapter
            # and the javadoc of com.codename1.io.
            #
            # Two edits that are NOT a rearrangement are how a foreign name
            # looks: SFSpeechRecognizer beside SpeechRecognizer, IPsec beside
            # Inset, Overridden beside Override. Requiring code for those
            # reports all seven of them, so there a comment is evidence
            # enough.
            rearranged = any(sorted(word.lower()) == sorted(other.lower())
                             for other in tied)
            vouched = code if distance == 1 or rearranged else anywhere
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
