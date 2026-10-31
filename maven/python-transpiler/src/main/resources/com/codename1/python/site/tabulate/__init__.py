# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Pretty-print tabular data as plain text, Markdown, reStructuredText or HTML."""

import math as _math

__all__ = ["tabulate", "tabulate_formats", "simple_separated_format"]

MIN_PADDING = 2


class Line:
    def __init__(self, begin, fill, sep, end):
        self.begin = begin
        self.fill = fill
        self.sep = sep
        self.end = end


class DataRow:
    def __init__(self, begin, sep, end):
        self.begin = begin
        self.sep = sep
        self.end = end


class TableFormat:
    def __init__(self, lineabove, linebelowheader, linebetweenrows, linebelow,
                 headerrow, datarow, padding, with_header_hide):
        self.lineabove = lineabove
        self.linebelowheader = linebelowheader
        self.linebetweenrows = linebetweenrows
        self.linebelow = linebelow
        self.headerrow = headerrow
        self.datarow = datarow
        self.padding = padding
        self.with_header_hide = with_header_hide


def _pipe_segment(align, width):
    if align == "left":
        return ":" + "-" * (width - 1)
    if align == "right" or align == "decimal":
        return "-" * (width - 1) + ":"
    if align == "center":
        return ":" + "-" * (width - 2) + ":"
    return "-" * width


def _pipe_line(colwidths, colaligns):
    segs = []
    for i in range(len(colwidths)):
        segs.append(_pipe_segment(colaligns[i], colwidths[i]))
    return "|" + "|".join(segs) + "|"


def _html_escape(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def _html_row(tag, cells, colaligns):
    out = []
    for i in range(len(cells)):
        a = colaligns[i]
        if a == "right" or a == "decimal":
            attr = ' style="text-align: right;"'
        elif a == "center":
            attr = ' style="text-align: center;"'
        else:
            attr = ""
        out.append("<" + tag + attr + ">" + _html_escape(cells[i]) + "</" + tag + ">")
    return "<tr>" + "".join(out) + "</tr>"


_FORMATS = {
    "plain": TableFormat(None, None, None, None, DataRow("", "  ", ""), DataRow("", "  ", ""), 0, []),
    "simple": TableFormat(Line("", "-", "  ", ""), Line("", "-", "  ", ""), None,
                          Line("", "-", "  ", ""), DataRow("", "  ", ""), DataRow("", "  ", ""),
                          0, ["lineabove", "linebelow"]),
    "grid": TableFormat(Line("+", "-", "+", "+"), Line("+", "=", "+", "+"), Line("+", "-", "+", "+"),
                        Line("+", "-", "+", "+"), DataRow("|", "|", "|"), DataRow("|", "|", "|"), 1, []),
    "github": TableFormat(Line("|", "-", "|", "|"), Line("|", "-", "|", "|"), None, None,
                          DataRow("|", "|", "|"), DataRow("|", "|", "|"), 1, ["lineabove"]),
    "pipe": TableFormat("pipe", "pipe", None, None, DataRow("|", "|", "|"), DataRow("|", "|", "|"),
                        1, ["lineabove"]),
    "orgtbl": TableFormat(None, Line("|", "-", "+", "|"), None, None, DataRow("|", "|", "|"),
                          DataRow("|", "|", "|"), 1, []),
    "psql": TableFormat(Line("+", "-", "+", "+"), Line("|", "-", "+", "|"), None,
                        Line("+", "-", "+", "+"), DataRow("|", "|", "|"), DataRow("|", "|", "|"), 1, []),
    "rst": TableFormat(Line("", "=", "  ", ""), Line("", "=", "  ", ""), None,
                       Line("", "=", "  ", ""), DataRow("", "  ", ""), DataRow("", "  ", ""), 0, []),
    "tsv": TableFormat(None, None, None, None, DataRow("", "\t", ""), DataRow("", "\t", ""), 0, []),
    "html": TableFormat(None, None, None, None, None, None, 0, []),
}

tabulate_formats = sorted(_FORMATS.keys())


def simple_separated_format(separator):
    return TableFormat(None, None, None, None, DataRow("", separator, ""),
                       DataRow("", separator, ""), 0, [])


def _isconvertible(kind, s):
    try:
        kind(s)
        return True
    except (ValueError, TypeError):
        return False


def _isnumber(s):
    if not _isconvertible(float, s):
        return False
    if isinstance(s, (str, bytes)):
        f = float(s)
        if _math.isinf(f) or _math.isnan(f):
            return s.lower() in ("inf", "-inf", "nan")
    return True


def _isint(s):
    if type(s) is int:
        return True
    if isinstance(s, (str, bytes)):
        return _isconvertible(int, s)
    return False


def _isbool(s):
    if type(s) is bool:
        return True
    return isinstance(s, str) and s in ("True", "False")


def _type(s, numparse):
    if s is None:
        return type(None)
    if hasattr(s, "isoformat"):
        return str
    if _isbool(s):
        return bool
    if numparse and _isint(s):
        return int
    if numparse and _isnumber(s):
        return float
    return str


_RANK = {type(None): 0, bool: 1, int: 2, float: 3, str: 5}


def _more_generic(t1, t2):
    r1 = _RANK.get(t1, 5)
    r2 = _RANK.get(t2, 5)
    r = r1 if r1 > r2 else r2
    for k in _RANK:
        if _RANK[k] == r:
            return k
    return str


def _column_type(values, numparse):
    t = bool
    for v in values:
        t = _more_generic(t, _type(v, numparse))
    return t


def _format(val, valtype, floatfmt, intfmt, missingval):
    if val is None:
        return missingval
    if valtype is str:
        return str(val)
    if valtype is int:
        if intfmt == "":
            return str(val)
        return format(int(val), intfmt)
    if valtype is bool:
        return str(val)
    if valtype is float:
        return format(float(val), floatfmt)
    return str(val)


def _padleft(width, s):
    return " " * (width - len(s)) + s if width > len(s) else s


def _padright(width, s):
    return s + " " * (width - len(s)) if width > len(s) else s


def _padboth(width, s):
    if width <= len(s):
        return s
    pad = width - len(s)
    left = pad // 2
    return " " * left + s + " " * (pad - left)


def _afterpoint(s):
    if _isnumber(s):
        if _isint(s):
            return -1
        pos = s.rfind(".")
        if pos < 0:
            pos = s.lower().rfind("e")
        if pos >= 0:
            return len(s) - pos - 1
        return -1
    return -1


def _align_column(strings, alignment, minwidth):
    if alignment == "left":
        strings = [s.rstrip() for s in strings]
        padfn = _padright
    elif alignment == "center":
        strings = [s.strip() for s in strings]
        padfn = _padboth
    elif alignment == "decimal":
        decs = [_afterpoint(s.strip()) for s in strings]
        maxdec = max(decs) if decs else 0
        strings = [s.strip() + " " * (maxdec - d) for s, d in zip(strings, decs)]
        padfn = _padleft
    elif alignment == "right":
        strings = [s.strip() for s in strings]
        padfn = _padleft
    else:
        return strings
    maxw = max([len(s) for s in strings]) if strings else 0
    width = maxw if maxw > minwidth else minwidth
    return [padfn(width, s) for s in strings]


def _normalize(data, headers, showindex):
    index = None
    if hasattr(data, "keys") and hasattr(data, "values"):
        keys = list(data.keys())
        cols = [list(data[k]) for k in keys]
        if isinstance(headers, str) and headers == "keys":
            headers = [str(k) for k in keys]
        n = max([len(c) for c in cols]) if cols else 0
        rows = []
        for i in range(n):
            rows.append([c[i] if i < len(c) else None for c in cols])
        if headers is not None and not isinstance(headers, str) and hasattr(headers, "keys"):
            headers = [str(headers[k]) for k in keys]
    else:
        items = list(data)
        rows = []
        if items and hasattr(items[0], "keys"):
            keys = list(items[0].keys())
            if isinstance(headers, str) and headers == "keys":
                headers = [str(k) for k in keys]
            elif hasattr(headers, "keys"):
                keys = list(headers.keys())
                headers = [str(headers[k]) for k in keys]
            elif headers:
                pass
            for r in items:
                rows.append([r.get(k) for k in keys])
        else:
            for r in items:
                rows.append(list(r))
            if isinstance(headers, str) and headers == "keys":
                headers = list(range(len(rows[0]))) if rows else []
    if isinstance(headers, str) and headers == "firstrow":
        if rows:
            headers = rows[0]
            rows = rows[1:]
        else:
            headers = []
    headers = [str(h) for h in headers] if headers is not None else []
    if showindex is True or showindex == "always":
        index = list(range(len(rows)))
    elif showindex is not False and showindex != "default" and showindex != "never" and showindex is not None:
        if isinstance(showindex, str):
            index = None
        else:
            index = list(showindex)
    if index is not None:
        rows = [[index[i] if i < len(index) else ""] + rows[i] for i in range(len(rows))]
        headers = [""] + headers if headers else headers
    ncols = max([len(r) for r in rows]) if rows else len(headers)
    if len(headers) > ncols:
        ncols = len(headers)
    if headers and len(headers) < ncols:
        headers = [""] * (ncols - len(headers)) + headers
    rows = [r + [None] * (ncols - len(r)) for r in rows]
    return rows, headers


def tabulate(tabular_data, headers=(), tablefmt="simple", floatfmt="g", intfmt="",
             numalign="default", stralign="default", missingval="", showindex="default",
             disable_numparse=False, colalign=None):
    rows, headers = _normalize(tabular_data, headers, showindex)
    if isinstance(tablefmt, str):
        if tablefmt not in _FORMATS:
            fmt = _FORMATS["simple"]
            tablefmt = "simple"
        else:
            fmt = _FORMATS[tablefmt]
    else:
        fmt = tablefmt
        tablefmt = ""
    numparse = not disable_numparse
    ncols = len(rows[0]) if rows else len(headers)
    cols = [[r[i] for r in rows] for i in range(ncols)]
    coltypes = [_column_type(c, numparse) for c in cols]
    scols = []
    for i in range(ncols):
        ff = floatfmt[i] if isinstance(floatfmt, (list, tuple)) else floatfmt
        mv = missingval[i] if isinstance(missingval, (list, tuple)) else missingval
        scols.append([_format(v, coltypes[i], ff, intfmt, mv) for v in cols[i]])
    cols = scols
    if numalign == "default":
        numalign = "decimal"
    if stralign == "default":
        stralign = "left"
    aligns = []
    for t in coltypes:
        aligns.append(numalign if t is int or t is float else stralign)
    if colalign is not None:
        for i in range(len(colalign)):
            if i < len(aligns):
                aligns[i] = colalign[i]
    min_padding = MIN_PADDING
    if headers:
        minwidths = [len(h) + min_padding for h in headers]
    else:
        minwidths = [0] * ncols
    cols = [_align_column(cols[i], aligns[i], minwidths[i]) for i in range(ncols)]
    if headers:
        hdrs = []
        for i in range(ncols):
            w = len(cols[i][0]) if cols[i] else 0
            if w < minwidths[i]:
                w = minwidths[i]
            a = aligns[i]
            if a == "left":
                hdrs.append(_padright(w, headers[i]))
            elif a == "center":
                hdrs.append(_padboth(w, headers[i]))
            else:
                hdrs.append(_padleft(w, headers[i]))
        headers = hdrs
        widths = [len(h) for h in headers]
        # a column wider than its header (after alignment) keeps the cell width
        for i in range(ncols):
            if cols[i] and len(cols[i][0]) > widths[i]:
                widths[i] = len(cols[i][0])
                headers[i] = (_padright if aligns[i] == "left" else _padboth if aligns[i] == "center" else _padleft)(widths[i], headers[i])
    else:
        widths = [len(c[0]) if c else 0 for c in cols]
    out_rows = [[cols[i][j] for i in range(ncols)] for j in range(len(rows))]
    return _format_table(fmt, tablefmt, headers, out_rows, widths, aligns)


def _build_row(cells, row):
    return (row.begin + row.sep.join(cells) + row.end).rstrip()


def _build_line(widths, aligns, line):
    if line == "pipe":
        return _pipe_line(widths, aligns)
    cells = [line.fill * w for w in widths]
    return (line.begin + line.sep.join(cells) + line.end).rstrip()


def _format_table(fmt, tablefmt, headers, rows, widths, aligns):
    if tablefmt == "html":
        lines = ["<table>"]
        if headers:
            lines.append("<thead>")
            lines.append(_html_row("th", headers, aligns))
            lines.append("</thead>")
        lines.append("<tbody>")
        for r in rows:
            lines.append(_html_row("td", r, aligns))
        lines.append("</tbody>")
        lines.append("</table>")
        return "\n".join(lines)
    lines = []
    hidden = fmt.with_header_hide if (headers and fmt.with_header_hide) else []
    pad = fmt.padding
    pw = [w + 2 * pad for w in widths]
    padding = " " * pad
    ph = [padding + c + padding for c in headers]
    pr = [[padding + c + padding for c in r] for r in rows]
    if fmt.lineabove and "lineabove" not in hidden:
        lines.append(_build_line(pw, aligns, fmt.lineabove))
    if ph:
        lines.append(_build_row(ph, fmt.headerrow))
        if fmt.linebelowheader and "linebelowheader" not in hidden:
            lines.append(_build_line(pw, aligns, fmt.linebelowheader))
    if pr and fmt.linebetweenrows and "linebetweenrows" not in hidden:
        for r in pr[:-1]:
            lines.append(_build_row(r, fmt.datarow))
            lines.append(_build_line(pw, aligns, fmt.linebetweenrows))
        lines.append(_build_row(pr[-1], fmt.datarow))
    else:
        for r in pr:
            lines.append(_build_row(r, fmt.datarow))
    if fmt.linebelow and "linebelow" not in hidden:
        lines.append(_build_line(pw, aligns, fmt.linebelow))
    return "\n".join(lines)
