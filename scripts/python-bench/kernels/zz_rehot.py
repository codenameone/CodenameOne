# Scratch: the regular expressions a document converter spends its time in, each timed alone.
# Patterns are copied from markdownify and CPython 3.14's html.parser.
import re
import time

re_whitespace = re.compile(r'[\t ]+')
re_all_whitespace = re.compile(r'[\t \r\n]+')
re_newline_whitespace = re.compile(r'[\t \r\n]*[\r\n][\t \r\n]*')
re_extract_newlines = re.compile(r'^(\n*)((?:.*[^\n])?)(\n*)$', flags=re.DOTALL)
re_escape_misc_chars = re.compile(r'([]\\&<`[>~=+|])')
re_escape_misc_dash_sequences = re.compile(r'(\s|^)(-+(?:\s|$))')
re_escape_misc_hashes = re.compile(r'(\s|^)(#{1,6}(?:\s|$))')
re_escape_misc_list_items = re.compile(r'((?:\s|^)[0-9]{1,9})([.)](?:\s|$))')
re_html_heading = re.compile(r'h(\d+)')

interesting_normal = re.compile('[&<]')
starttagopen = re.compile('<[a-zA-Z]')
tagfind_tolerant = re.compile(r'([a-zA-Z][^\t\n\r\f />]*)(?:[\t\n\r\f ]|/(?!>))*')
attrfind_tolerant = re.compile(r"""
  (
    (?<=['"\t\n\r\f /])[^\t\n\r\f />][^\t\n\r\f /=>]*  # attribute name
   )
  ([\t\n\r\f ]*=[\t\n\r\f ]*        # value indicator
    ('[^']*'                        # LITA-enclosed value
    |"[^"]*"                        # LIT-enclosed value
    |(?!['"])[^>\t\n\r\f ]*         # bare value
    )
   )?
  (?:[\t\n\r\f ]|/(?!>))*           # possibly followed by a space
""", re.VERBOSE)
locatetagend = re.compile(r"""
  [a-zA-Z][^\t\n\r\f />]*           # tag name
  [\t\n\r\f /]*                     # optional whitespace before attribute name
  (?:(?<=['"\t\n\r\f /])[^\t\n\r\f />][^\t\n\r\f /=>]*  # attribute name
    (?:[\t\n\r\f ]*=[\t\n\r\f ]*    # value indicator
      (?:'[^']*'                    # LITA-enclosed value
        |"[^"]*"                    # LIT-enclosed value
        |(?!['"])[^>\t\n\r\f ]*     # bare value
       )
     )?
    [\t\n\r\f /]*                   # possibly followed by a space
   )*
   >?
""", re.VERBOSE)

WORDS = ["the", "quick", "brown", "fox", "jumps", "over", "lazy", "dog", "Wikipedia",
         "article", "history", "1999", "-", "#", "3.", "a|b", "x=y", "[ref]", "free"]


def texts():
    out = []
    k = 0
    for i in range(400):
        n = 1 + (i * 7) % 23
        ws = []
        for j in range(n):
            ws.append(WORDS[k % len(WORDS)])
            k += 1
        s = " ".join(ws)
        if i % 5 == 0:
            s = "\n" + s
        if i % 7 == 0:
            s = s + "\n\n"
        if i % 3 == 0:
            s = s.replace(" ", "  \t", 2)
        out.append(s)
    return out


def html_doc():
    parts = []
    for i in range(300):
        parts.append('<div class="c%d" id="x%d"><a href="/wiki/Page_%d" title="Page %d">link %d</a>'
                     ' text &amp; more <span style=\'color:red\' data-x=%d>s</span></div>\n' % (i, i, i, i, i, i))
    return "".join(parts)


TEXTS = texts()
HTML = html_doc()


def p_extract(n):
    t = 0
    for _ in range(n):
        for s in TEXTS:
            a, b, c = re_extract_newlines.match(s).groups()
            t += len(a) + len(b) + len(c)
    return t


def p_ws(n):
    t = 0
    for _ in range(n):
        for s in TEXTS:
            x = re_all_whitespace.sub(' ', s)
            y = re_newline_whitespace.sub('\n', s)
            y = re_whitespace.sub(' ', y)
            t += len(x) + len(y)
    return t


def p_escape(n):
    t = 0
    for _ in range(n):
        for s in TEXTS:
            x = re_escape_misc_chars.sub(r'\\\1', s)
            x = re_escape_misc_dash_sequences.sub(r'\1\\\2', x)
            x = re_escape_misc_hashes.sub(r'\1\\\2', x)
            x = re_escape_misc_list_items.sub(r'\1\\\2', x)
            t += len(x)
    return t


def p_heading(n):
    t = 0
    names = ["div", "h1", "span", "a", "h3", "p", "li", "td"]
    for _ in range(n):
        for i in range(400):
            if re_html_heading.match(names[i & 7]) is not None:
                t += 1
    return t


def p_scan(n):
    # html.parser's goahead: find the next markup, then classify it.
    t = 0
    raw = HTML
    for _ in range(n):
        i = 0
        end = len(raw)
        while i < end:
            m = interesting_normal.search(raw, i)
            j = m.start() if m else end
            if i < j:
                t += j - i
            i = j
            if i == end:
                break
            if raw.startswith('<', i):
                if starttagopen.match(raw, i):
                    m = locatetagend.match(raw, i + 1)
                    k = m.end()
                    tm = tagfind_tolerant.match(raw, i + 1)
                    p = tm.end()
                    while p < k:
                        am = attrfind_tolerant.match(raw, p)
                        if not am:
                            break
                        t += len(am.group(1))
                        p = am.end()
                    i = k
                else:
                    i = raw.find('>', i) + 1
            else:
                i += 1
    return t


def part(name, f, n, repeat):
    best = None
    r = None
    for _ in range(repeat):
        t0 = time.perf_counter()
        r = f(n)
        dt = time.perf_counter() - t0
        if best is None or dt < best:
            best = dt
    print("PART", name, int(best * 1000000), r)


def main():
    import os
    only = os.environ.get("CORE_PART")
    if only:
        f = {"extract": p_extract, "ws": p_ws, "escape": p_escape, "heading": p_heading, "scan": p_scan}[only]
        for _ in range(100000):
            f(1)
        return
    part("extract_newlines", p_extract, 20, 7)
    part("whitespace_subs", p_ws, 20, 7)
    part("escape_subs", p_escape, 20, 7)
    part("heading_match", p_heading, 50, 7)
    part("html_scan", p_scan, 10, 7)


main()
