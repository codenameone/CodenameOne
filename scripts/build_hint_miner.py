#!/usr/bin/env python3
"""Mines every build hint the Maven plugin and the builders read.

Three accessor shapes reach a hint; a getArg() grep alone misses the whole
@Desktop group, which is read only by a private arg() helper in
GenerateDesktopAppWrapperMojo.

Arguments are split with a paren/quote-balanced scan rather than a regex,
because calls nest: getArg("ios.urlSchemes", getArg("ios.urlScheme", "")).
A regex that stops at the first comma both mis-reads the outer default and
consumes the inner call, silently dropping a hint from the catalog.

Not every hint name is written as a literal at the point it is read. Two shapes
occur and both used to be invisible, which let the gate report "all described"
while real hints had no catalog row at all:

  getArg(HINT, null)                        NativeVerifyOption, HINT="nativeVerify"
  getArg(platform + ".maps.provider", ...)  MapsProviderInjector

The first is resolved: a `static final String` in the same file whose value is a
literal is substituted. The second cannot be -- the platform is only known at
run time -- so it is reported as a COMPUTED site instead of ignored, and the
checker holds those against the catalog rather than letting them pass in
silence.
"""
import re, os, sys, json, collections

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "maven/codenameone-maven-plugin/src/main/java/com/codename1")

OPENERS = [
    (re.compile(r'\bgetArg\(\s*"'), False),
    (re.compile(r'(?<![A-Za-z0-9_.])arg\(\s*"'), False),
    (re.compile(r'\bbooleanArg\(\s*"'), False),
    (re.compile(r'\bgetProperty\(\s*"codename1\.arg\.'), True),
]

_ESCAPES = {'n': '\n', 't': '\t', 'r': '\r', 'b': '\b', 'f': '\f',
            '"': '"', "'": "'", '\\': '\\'}

# getArg/arg/booleanArg whose first argument is not a string literal. \s covers the
# line-wrapped calls, which are plain literal reads once the newline is crossed.
COMPUTED_OPENER = re.compile(
    r'\b(?:getArg|booleanArg)\(\s*(?!")|(?<![A-Za-z0-9_.])arg\(\s*(?!")')

# static final String NAME = "literal";  -- the only constant shape worth resolving.
CONST_DECL = re.compile(
    r'\bstatic\s+final\s+String\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*"((?:[^"\\]|\\.)*)"\s*;')

# A method declaration, so a helper that forwards one of its own parameters to an
# accessor can be recognised and its CALLERS mined instead. MacNativeBuilder's
# parseEntitlementBool(request, hint, def) is the shape: every caller passes a
# literal, none of them is a getArg, and the hints were invisible to a literal
# search of accessor calls alone.
METHOD_DECL = re.compile(
    r'\b(?:public|private|protected|static|final|synchronized|\s)+'
    r'[A-Za-z_][A-Za-z0-9_<>\[\], .?]*\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(([^)]*)\)\s*'
    r'(?:throws [A-Za-z0-9_., ]+)?\{')

FORWARDS = re.compile(r'\b(?:getArg|booleanArg)\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*,'
                      r'|(?<![A-Za-z0-9_.])arg\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*,')


def accessor_wrappers(text):
    """{method name: index of the parameter it forwards to an accessor}.

    Same file only. A private helper's name is not unique across the tree, and
    mining calls to a same-named method of some unrelated class would invent
    hints rather than find them.
    """
    out = {}
    for m in METHOD_DECL.finditer(text):
        name, params = m.group(1), m.group(2)
        if name in ("getArg", "arg", "booleanArg") or not params.strip():
            continue
        names = []
        for part in params.split(','):
            part = part.strip()
            if part:
                names.append(part.split()[-1].strip())
        # Body: to the next method declaration, or 4k, whichever comes first.
        body = text[m.end():m.end() + 4000]
        nxt = METHOD_DECL.search(body)
        if nxt:
            body = body[:nxt.start()]
        for f in FORWARDS.finditer(body):
            forwarded = f.group(1) or f.group(2)
            if forwarded in names:
                out[name] = names.index(forwarded)
                break
    return out


def read_literal(text, i):
    """Read a Java string literal starting just after the opening quote.

    Escape sequences are decoded, not preserved. Keeping them verbatim recorded
    android.file_paths' default as `<files-path name=\\"app_files\\" .../>` --
    backslashes the build never sees -- which then reached the developer guide.
    """
    out = []
    while i < len(text):
        c = text[i]
        if c == '\\':
            nxt = text[i + 1] if i + 1 < len(text) else ''
            if nxt == 'u':
                try:
                    out.append(chr(int(text[i + 2:i + 6], 16))); i += 6; continue
                except ValueError:
                    # Not a well-formed \uXXXX after all. Fall through and treat
                    # the backslash as a plain escape rather than guessing at a
                    # code point; a malformed escape in a builder source is not
                    # this script's problem to diagnose.
                    pass
            out.append(_ESCAPES.get(nxt, nxt)); i += 2; continue
        if c == '"':
            return "".join(out), i + 1
        out.append(c); i += 1
    return None, i

def split_args(text, i):
    """Split the argument list; i points just after the opening paren."""
    args, depth, cur = [], 0, []
    while i < len(text):
        c = text[i]
        if c == '"':
            lit, j = read_literal(text, i + 1)
            # Re-quote with proper escaping so a decoded inner quote does not
            # break the literal-default check below.
            cur.append(json.dumps(lit or "", ensure_ascii=False)); i = j; continue
        if c == "'":
            j = i + 1
            while j < len(text) and text[j] != "'":
                j += 2 if text[j] == '\\' else 1
            cur.append(text[i:j+1]); i = j + 1; continue
        if c in "([":
            depth += 1
        elif c in ")]":
            if depth == 0:
                args.append("".join(cur).strip()); return args
            depth -= 1
        elif c == ',' and depth == 0:
            args.append("".join(cur).strip()); cur = []; i += 1; continue
        cur.append(c); i += 1
    return args

LITERAL_DEFAULT = re.compile(r'null|true|false|-?\d+|"(?:[^"\\]|\\.)*"')

hits = collections.defaultdict(list)
computed = []


def first_argument(text, open_paren):
    """The first argument's source text, or None when the call is malformed."""
    args = split_args(text, open_paren + 1)
    return args[0] if args else None


def resolve_constants(expr, constants):
    """Substitute same-file string constants into a first-argument expression."""
    return re.sub(r'\b([A-Za-z_][A-Za-z0-9_]*)\b',
                  lambda m: json.dumps(constants[m.group(1)]) if m.group(1) in constants
                  else m.group(0),
                  expr)


def concat_of_literals(expr):
    """The value of a `"a" + "b"` expression, or None when a piece is not a literal."""
    parts = [p.strip() for p in expr.split('+')]
    out = []
    for part in parts:
        if len(part) >= 2 and part.startswith('"') and part.endswith('"'):
            lit, _ = read_literal(part, 1)
            if lit is None:
                return None
            out.append(lit)
        else:
            return None
    return "".join(out)

for dirpath, _, files in os.walk(SRC):
    for fn in sorted(files):
        if not fn.endswith(".java"):
            continue
        path = os.path.join(dirpath, fn)
        with open(path, encoding="utf-8", errors="replace") as fh:
            text = fh.read()
        rel = os.path.relpath(path, ROOT)
        constants = {m.group(1): (read_literal(m.group(0), m.group(0).index('"') + 1)[0] or "")
                     for m in CONST_DECL.finditer(text)}
        for m in COMPUTED_OPENER.finditer(text):
            open_paren = text.rindex('(', m.start(), m.end())
            expr = first_argument(text, open_paren)
            if not expr:
                continue
            line = text.count("\n", 0, m.start()) + 1
            resolved = concat_of_literals(resolve_constants(expr, constants))
            if resolved is not None:
                # Fully resolved -- an ordinary hint read that merely spelled its
                # name with a constant.
                hits[resolved].append(("<expr>", rel, line))
            elif '"' in expr or '[' in expr:
                # The name is BUILT here, or read out of a table -- either way no
                # literal at a getArg call names it, so the literal pass cannot
                # see it. IPhoneBuilder's WALLET_INJECTION_HINTS is the second
                # kind: ten real hints living in a String[][] and reaching getArg
                # as hintAndMarker[0], invisible to every literal search until
                # subscripts were reported too.
                computed.append({"expr": " ".join(expr.split()),
                                 "file": rel, "line": line})
            # Anything else is a forwarding helper -- getArg(key, ...) inside a
            # wrapper, or the declaration of getArg itself -- whose caller passes a
            # literal that the literal pass already mines. Reporting those would bury
            # the handful of sites that genuinely compute a name.
        # Literals that only ever reach an accessor through a helper.
        for wrapper, index in sorted(accessor_wrappers(text).items()):
            for m in re.finditer(r'(?<![A-Za-z0-9_.])' + re.escape(wrapper) + r'\s*\(', text):
                args = split_args(text, m.end())
                if len(args) <= index:
                    continue
                arg = args[index].strip()
                if not (len(arg) >= 2 and arg.startswith('"') and arg.endswith('"')):
                    continue
                key, _ = read_literal(arg, 1)
                if key is None or not re.fullmatch(r'[A-Za-z][A-Za-z0-9_.!]*', key):
                    continue
                line = text.count("\n", 0, m.start()) + 1
                hits[key].append(("<expr>", rel, line))
        for pat, prefixed in OPENERS:
            for m in pat.finditer(text):
                # position of the char just after the opening quote of arg 1
                key, after = read_literal(text, m.end())
                if key is None:
                    continue
                if prefixed:
                    if not key:
                        continue
                else:
                    if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_.!]*', key):
                        continue
                # find the call's opening paren to split the whole arg list
                open_paren = text.rindex('(', m.start(), m.end())
                args = split_args(text, open_paren + 1)
                default = args[1].strip() if len(args) > 1 else ""
                if not LITERAL_DEFAULT.fullmatch(default or "null"):
                    default = "<expr>"
                line = text.count("\n", 0, m.start()) + 1
                hits[key].append((default or "null", rel, line))

def hits_computed():
    """(expression, file, line) for every site that builds a hint name."""
    return sorted((c["expr"], c["file"], c["line"]) for c in computed)


if __name__ == "__main__":
    print(f"distinct keys mined: {len(hits)}, computed sites: {len(computed)}",
          file=sys.stderr)
    out = sys.argv[1] if len(sys.argv) > 1 else "-"
    payload = {k: v for k, v in sorted(hits.items())}
    # Under a key no hint name can take, so a consumer reading this as a plain
    # name->sites map cannot mistake it for a hint.
    payload["#computed"] = sorted(
        (c["expr"], c["file"], c["line"]) for c in computed)
    if out == "-":
        json.dump(payload, sys.stdout, indent=1)
    else:
        with open(out, "w", encoding="utf-8") as fh:
            json.dump(payload, fh, indent=1)
