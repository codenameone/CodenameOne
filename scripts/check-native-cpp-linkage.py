#!/usr/bin/env python3
"""Every ParparVM native defined in a C++ translation unit must have C linkage.

ParparVM generates C that calls a native by its exact symbol name. A C++ compiler
mangles a function's name unless it is declared `extern "C"`, so a native defined in a
.cpp or .mm file without that declaration compiles cleanly, exports a mangled symbol,
and leaves the name the generated code calls undefined -- a link error on the device,
in a file nobody touched, naming a symbol that is visibly right there in the source.

This is not hypothetical. PR #5845 shipped exactly that in cn1_windows_window.cpp and
only a real Windows build caught it, because the two checks that look at natives cannot
see it: check-native-signatures.sh verifies that the NAME matches the Java method, and
it does -- linkage is not part of a name. Nothing else reads these files at all.

The check is deliberately absolute, with no baseline: a native without C linkage is
never intentional, and the fix is always the same one line.
"""

import re
import subprocess
import sys

# The generated code calls natives by these prefixes. A function whose name begins with a
# Java package path is a native entry point; anything else in these files is port-internal
# C++ that is supposed to be mangled.
NATIVE_PREFIX = re.compile(r'\b((?:com|net|org|java)_[A-Za-z0-9_]*_[A-Za-z0-9_]+)\s*\(')

DEFINITION = re.compile(
    r'(?:^|\n)[ \t]*(?:[A-Za-z_][A-Za-z0-9_]*[ \t\r\n*&]+)+'
    r'((?:com|net|org|java)_[A-Za-z0-9_]+)[ \t\r\n]*\(')


def blank_comments_and_literals(text):
    """Replace comments and string/char literals with spaces, preserving line structure.

    Offsets and line numbers have to survive: findings are reported by line, and the
    brace scan below would otherwise count a brace inside a string literal.
    """
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        two = text[i:i + 2]
        if two == '//':
            while i < n and text[i] != '\n':
                out.append(' ')
                i += 1
        elif two == '/*':
            while i < n and text[i:i + 2] != '*/':
                out.append('\n' if text[i] == '\n' else ' ')
                i += 1
            out.append('  ')
            i += 2
        elif c in '"\'':
            quote = c
            out.append(' ')
            i += 1
            while i < n:
                if text[i] == '\\':
                    out.append('  ')
                    i += 2
                    continue
                if text[i] == quote:
                    out.append(' ')
                    i += 1
                    break
                out.append('\n' if text[i] == '\n' else ' ')
                i += 1
        else:
            out.append(c)
            i += 1
    return ''.join(out)


def extern_c_spans(raw, clean):
    """Offset ranges covered by an `extern "C" { ... }` block.

    The literal has to be found in the RAW text -- blanking removes the "C" -- while the
    braces are counted in the CLEANED text, where a brace inside a literal cannot lie.
    """
    spans = []
    for m in re.finditer(r'extern[ \t\r\n]*"C"[ \t\r\n]*\{', raw):
        depth = 0
        i = m.end() - 1
        while i < len(clean):
            if clean[i] == '{':
                depth += 1
            elif clean[i] == '}':
                depth -= 1
                if depth == 0:
                    spans.append((m.start(), i))
                    break
            i += 1
        else:
            spans.append((m.start(), len(clean)))
    return spans


def declared_extern_c(raw, decl_start, name_start):
    """True for `extern "C" JAVA_VOID foo(...)` -- the single-declaration form.

    Anchored to THIS declaration, between its first token and the symbol name, rather
    than to a window of preceding text. A loose lookback would accept

        extern "C" void somethingElse(void);
        JAVA_VOID com_codename1_...(...) { }

    where the second function has no C linkage at all and the `extern "C"` belongs to the
    line above it. Offsets line up because blanking comments and literals preserves length.
    """
    return re.match(
        r'[ \t\r\n]*extern[ \t\r\n]*"C"[ \t\r\n]*(?:[A-Za-z_][A-Za-z0-9_]*[ \t\r\n*&]+)*$',
        raw[decl_start:name_start]) is not None


def check(path):
    with open(path, 'r', encoding='utf-8', errors='replace') as fh:
        return check_text(fh.read())


def check_text(raw):
    clean = blank_comments_and_literals(raw)
    spans = extern_c_spans(raw, clean)
    findings = []
    for m in DEFINITION.finditer(clean):
        start = m.start(1)
        # A declaration (ends in ';') is not a definition and needs no linkage of its own,
        # but it is also harmless to require it -- what matters is that the DEFINITION has
        # it, so look ahead for the body.
        tail = clean[m.end():m.end() + 4000]
        closing = tail.find(')')
        if closing < 0:
            continue
        after = tail[closing + 1:closing + 40].lstrip()
        if not after.startswith('{'):
            continue
        if any(lo <= start <= hi for lo, hi in spans):
            continue
        if declared_extern_c(raw, m.start(), start):
            continue
        findings.append((clean.count('\n', 0, start) + 1, m.group(1)))
    return findings



# Each fixture is (source, expected symbols reported). They encode the cases this gate has
# to get right, and they exist because the way a gate like this fails is silent: a parser
# that stops recognising a definition reports zero findings, which is indistinguishable
# from a clean tree. --self-test is wired into the same CI step as the gate itself.
SELF_TEST_CASES = (
    (
        'native inside an extern "C" block is fine',
        'extern "C" {\n'
        'JAVA_VOID com_codename1_impl_windows_WindowsNative_wrapped___int(void *t, int a) {\n'
        '}\n'
        '}\n',
        [],
    ),
    (
        'native declared extern "C" on its own is fine',
        'extern "C" JAVA_VOID com_codename1_impl_windows_WindowsNative_single___int(void *t, int a) {\n'
        '}\n',
        [],
    ),
    (
        'a prototype is not a definition',
        'JAVA_VOID com_codename1_impl_windows_WindowsNative_proto___int(void *t, int a);\n',
        [],
    ),
    (
        'a port-internal C++ helper is supposed to be mangled',
        'static int helperNotANative(int x) { return x + 1; }\n',
        [],
    ),
    (
        'a native in a comment is not a definition',
        '/*\nJAVA_VOID com_codename1_impl_windows_WindowsNative_commented___int(void *t, int a) {\n}\n*/\n',
        [],
    ),
    (
        'a brace inside a string literal does not close the block',
        'extern "C" {\n'
        'JAVA_VOID com_codename1_impl_windows_WindowsNative_literal___int(void *t, int a) {\n'
        '    printf("}");\n'
        '}\n'
        'JAVA_VOID com_codename1_impl_windows_WindowsNative_after___int(void *t, int a) {\n'
        '}\n'
        '}\n',
        [],
    ),
    (
        'a native at file scope is reported',
        'JAVA_VOID com_codename1_impl_windows_WindowsNative_unwrapped___int(void *t, int a) {\n'
        '}\n',
        ['com_codename1_impl_windows_WindowsNative_unwrapped___int'],
    ),
    (
        'an extern "C" belonging to the line above does not cover the next definition',
        'extern "C" void somethingElse(void);\n'
        'JAVA_VOID com_codename1_impl_windows_WindowsNative_adjacent___int(void *t, int a) {\n'
        '}\n',
        ['com_codename1_impl_windows_WindowsNative_adjacent___int'],
    ),
    (
        'a native inside a namespace is reported -- a namespace mangles too',
        'namespace cn1 {\n'
        'JAVA_VOID com_codename1_impl_windows_WindowsNative_namespaced___int(void *t, int a) {\n'
        '}\n'
        '}\n',
        ['com_codename1_impl_windows_WindowsNative_namespaced___int'],
    ),
)


def self_test():
    failures = 0
    for name, source, expected in SELF_TEST_CASES:
        got = [sym for _, sym in check_text(source)]
        if got != expected:
            failures += 1
            print('SELF-TEST FAIL: %s' % name)
            print('  expected: %s' % (expected,))
            print('  got:      %s' % (got,))
    if failures:
        print('\ncheck-native-cpp-linkage: %d of %d self-test case(s) failed.'
              % (failures, len(SELF_TEST_CASES)), file=sys.stderr)
        return 1
    print('check-native-cpp-linkage: %d self-test case(s) passed.' % len(SELF_TEST_CASES))
    return 0


def main(argv):
    if '--self-test' in argv:
        return self_test()
    if argv:
        paths = argv
    else:
        out = subprocess.run(['git', 'ls-files', '*.cpp', '*.cc', '*.cxx', '*.mm'],
                             capture_output=True, text=True, check=True).stdout
        paths = [p for p in out.split() if p]
    total = 0
    bad = 0
    for path in paths:
        try:
            findings = check(path)
        except IOError:
            continue
        total += 1
        for line, name in findings:
            bad += 1
            print('%s:%d: ParparVM native %s is defined without C linkage.' % (path, line, name))
            print('    The C++ compiler will mangle it and the generated code will not link.')
            print('    Move it inside the file\'s extern "C" block, or mark it extern "C".')
    if total == 0:
        # A check that examined nothing must never report success: an empty file list means
        # the glob or the invocation is wrong, not that the tree is clean.
        print('check-native-cpp-linkage: FATAL: no C++ files examined.', file=sys.stderr)
        return 2
    if bad:
        print('\ncheck-native-cpp-linkage: %d native(s) without C linkage in %d file(s).'
              % (bad, total), file=sys.stderr)
        return 1
    print('check-native-cpp-linkage: %d file(s) clean.' % total)
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
