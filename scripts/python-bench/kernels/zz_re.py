# Scratch: markdownify's and bs4's regex calls, alone, in ns per call.
import re
import time

re_whitespace = re.compile(r'[\t ]+')
re_all_whitespace = re.compile(r'[\t \r\n]+')
re_newline_whitespace = re.compile(r'[\t \r\n]*[\r\n][\t \r\n]*')
re_make_convert_fn_name = re.compile(r'[\[\]:-]')
re_escape_misc_chars = re.compile(r'([]\\&<`[>~=+|])')
re_escape_misc_dash_sequences = re.compile(r'(\s|^)(-+(?:\s|$))')
re_line_with_content = re.compile(r'^(.*)', flags=re.MULTILINE)
re_extract_newlines = re.compile(r'^(\n*)((?:.*[^\n])?)(\n*)$', flags=re.DOTALL)
re_word = re.compile(r'\w+')

SHORT = "Hello world"
MID = "The quick brown fox jumps over the lazy dog, and then\n  runs   away again."
NL = "\n   \n"
TAG = "div"

N = 100000


def p_ws_short(n):
    t = 0
    for _ in range(n):
        t += len(re_whitespace.sub(' ', SHORT))
    return t


def p_ws_mid(n):
    t = 0
    for _ in range(n):
        t += len(re_whitespace.sub(' ', MID))
    return t


def p_allws_mid(n):
    t = 0
    for _ in range(n):
        t += len(re_all_whitespace.sub(' ', MID))
    return t


def p_nlws_mid(n):
    t = 0
    for _ in range(n):
        t += len(re_newline_whitespace.sub('\n', MID))
    return t


def p_nlws_nl(n):
    t = 0
    for _ in range(n):
        t += len(re_newline_whitespace.sub('\n', NL))
    return t


def p_fn_name(n):
    t = 0
    for _ in range(n):
        t += len(re_make_convert_fn_name.sub("_", TAG))
    return t


def p_escape_grp(n):
    t = 0
    for _ in range(n):
        t += len(re_escape_misc_chars.sub(r'\\\1', MID))
    return t


def p_dash(n):
    t = 0
    for _ in range(n):
        t += len(re_escape_misc_dash_sequences.sub(r'\1\\\2', MID))
    return t


def p_line_fn(n):
    t = 0
    for _ in range(n):
        t += len(re_line_with_content.sub(lambda m: '> ' + m.group(1), MID))
    return t


def p_extract_nl(n):
    t = 0
    for _ in range(n):
        t += len(re_extract_newlines.match(MID).groups())
    return t


def p_match_short(n):
    t = 0
    for _ in range(n):
        if re_word.match(SHORT):
            t += 1
    return t


def p_search_mid(n):
    t = 0
    for _ in range(n):
        if re_newline_whitespace.search(MID):
            t += 1
    return t


PARTS = [
    ("ws_short", p_ws_short), ("ws_mid", p_ws_mid), ("allws_mid", p_allws_mid), ("nlws_mid", p_nlws_mid),
    ("nlws_nl", p_nlws_nl), ("fn_name", p_fn_name), ("escape_grp", p_escape_grp), ("dash", p_dash),
    ("line_fn", p_line_fn), ("extract_nl", p_extract_nl), ("match_short", p_match_short),
    ("search_mid", p_search_mid),
]


def main():
    import os
    only = os.environ.get("CORE_PART")
    if only:
        f = dict(PARTS)[only]
        for _ in range(400):
            f(N)
        return
    for name, f in PARTS:
        best = None
        for _ in range(5):
            t0 = time.perf_counter()
            f(N)
            dt = time.perf_counter() - t0
            if best is None or dt < best:
                best = dt
        print("PART %-12s %7.1f" % (name, best * 1e9 / N))


main()
