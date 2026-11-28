# The string operations markdownify, bs4 and the converters run per node:
# slicing, find, replace, strip, startswith, join and %-formatting.
from _harness import measure


def work(n):
    line = "   <td class=\"cell\">Revenue &amp; costs, 2024</td>   "
    out = []
    acc = 0
    for i in range(n):
        s = line.strip()
        a = s.find(">") + 1
        b = s.rfind("<")
        cell = s[a:b].replace("&amp;", "&")
        if cell.startswith("Rev"):
            acc += len(cell)
        out.append("| %s | %d |" % (cell.lower(), i % 7))
        if len(out) == 64:
            acc += len("\n".join(out))
            out = []
    return str(acc)


measure("str_ops", work, 300000, 5)
