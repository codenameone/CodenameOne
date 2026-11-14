# Regular expressions over text: tokenizing, substitution and search, as the
# Markdown escaping and whitespace normalization in markdownify do.
import re
from _harness import measure

WS = re.compile(r"\s+")
ESC = re.compile(r"([*_`\[\]])")
NUM = re.compile(r"\d+(?:\.\d+)?")


def work(n):
    text = "Some *bold*  text_with [links]\tand 12.5 numbers, 42 more   words here. " * 4
    total = 0
    for i in range(n):
        t = WS.sub(" ", text)
        t = ESC.sub(r"\\\1", t)
        total += len(t) + len(NUM.findall(t))
        m = re.match(r"(\w+) (\*?\w+\*?)", t)
        if m:
            total += len(m.group(2))
    return str(total)


measure("regex_text", work, 5000, 5)
