#!/usr/bin/env python3
"""Emits the upcall header the clang syntax check compiles against.

Generated from IOSCallCallbacks.java rather than written by hand. The list
used to be spelled out in the workflow, with a comment saying a generator
would be one more thing to get wrong -- and then a callback was added, the
list was not, and the syntax check failed on a symbol that exists. A curated
copy of everything in one file is exactly what a machine should keep.

The mangling is ParparVM's, and only the part these callbacks use: the
argument list opens with `__`, every argument contributes a leading `_` and
its type name with dots as underscores, and a void return appends nothing.

Two rules keep this honest. A method whose parameters this cannot mangle is
SKIPPED rather than guessed at -- the class holds internal helpers as well as
upcalls, and `install(IOSNative)` is one. And every
`IOSCallCallbacks` symbol the native sources actually call must appear in the
output, which is checked here: that is the direction drift hurts, and it is
what the hand-written list got wrong.
"""
import re
import sys

C_TYPE = {
    'int': 'JAVA_INT',
    'long': 'JAVA_LONG',
    'boolean': 'JAVA_BOOLEAN',
    'float': 'JAVA_FLOAT',
    'double': 'JAVA_DOUBLE',
    'String': 'JAVA_OBJECT',
}
MANGLED = {
    'int': 'int',
    'long': 'long',
    'boolean': 'boolean',
    'float': 'float',
    'double': 'double',
    'String': 'java_lang_String',
}


def main(source, out):
    text = open(source, encoding='utf-8').read()
    # Only the static methods the natives call up through; an instance
    # method or a non-void return would need mangling rules this does not
    # implement, so it is refused rather than approximated.
    pattern = re.compile(
        r'^\s*(?:public\s+|private\s+|protected\s+)?static\s+void\s+'
        r'(\w+)\s*\(([^)]*)\)', re.M)
    lines = ['#import "xmlvm.h"']
    seen = set()
    for name, args in pattern.findall(text):
        if name in seen:
            continue
        seen.add(name)
        types = []
        for arg in [a.strip() for a in args.split(',') if a.strip()]:
            java = arg.split()[0]
            if java not in MANGLED:
                types = None
                break
            types.append(java)
        if types is None:
            # Not an upcall: the natives only ever pass primitives and
            # strings. Skipped quietly here and caught below if some native
            # really does call it.
            continue
        symbol = 'com_codename1_impl_ios_IOSCallCallbacks_' + name + '__'
        symbol += ''.join('_' + MANGLED[t] for t in types)
        params = ''.join(', ' + C_TYPE[t] for t in types)
        lines.append('extern void %s(CODENAME_ONE_THREAD_STATE%s);'
                     % (symbol, params))
    open(out, 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
    # The exact SET, not the header text. A substring test passed a
    # truncated name -- `..._Strin` is inside `..._String` -- so a call site
    # with the name wrong by a character was reported as declared, which is
    # the single failure mode this check exists for.
    declared = set(
        re.findall(r'com_codename1_impl_ios_IOSCallCallbacks_[A-Za-z0-9_]+',
                   '\n'.join(lines)))
    missing = set()
    for native in sys.argv[3:]:
        called = re.findall(
            r'com_codename1_impl_ios_IOSCallCallbacks_[A-Za-z0-9_]+',
            open(native, encoding='utf-8', errors='replace').read())
        for symbol in called:
            if symbol not in declared:
                missing.add(symbol + '  (called by ' + native + ')')
    if missing:
        sys.exit('gen-ios-callback-stub: the native sources call symbols this '
                 'header does not declare:\n  '
                 + '\n  '.join(sorted(missing))
                 + '\nEither the Java method is not a static void with '
                   'mangleable parameters, or the call site has the name '
                   'wrong.')
    print('gen-ios-callback-stub: %d callbacks, all call sites declared'
          % (len(lines) - 1))


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
