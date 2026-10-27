#!/usr/bin/env python3
#
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Codename One designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Codename One through http://www.codenameone.com/ if you
# need additional information or have any questions.
"""Renders the developer guide's Python performance charts.

    render_charts.py common.json --out docs/developer-guide/img

The input is what common_compare.py --json writes. Two SVGs come out:
python-perf-time.svg (milliseconds, log scale, because the runtimes differ by
two orders of magnitude) and python-perf-rss.svg (peak RSS, linear). The SVGs
are self-contained so the website and the PDF build render them alike.
"""

import argparse
import json
import math
import os

COLORS = {
    "CPython": "#9aa0a6",
    "PyPy": "#f9ab00",
    "GraalPy": "#a142f4",
    "Codename One": "#1a73e8",
    "Codename One, hot spot in Java": "#0b8043",
}
TEXT = "#202124"
MUTED = "#5f6368"
WIDTH = 760
LEFT = 150
RIGHT = 80
BAR = 9
ROW_GAP = 14


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def chart(data, key, title, unit, log):
    arms = data["arms"]
    rows = list(data["results"].items())
    plot = WIDTH - LEFT - RIGHT
    values = [r[a][key] for _, r in rows for a in arms]
    hi = max(values)
    lo = min(v for v in values if v > 0)
    if log:
        lo_e = math.floor(math.log10(lo))
        hi_e = math.ceil(math.log10(hi))

        def x(v):
            return plot * (math.log10(max(v, 10 ** lo_e)) - lo_e) / (hi_e - lo_e)
        ticks = [10 ** e for e in range(lo_e, hi_e + 1)]
    else:
        step = 10 ** math.floor(math.log10(hi))
        top = math.ceil(hi / step) * step

        def x(v):
            return plot * v / top
        ticks = [step * i for i in range(int(top / step) + 1)]
    legend_h = 22 * ((len(arms) + 2) // 3)
    top_y = 40 + legend_h
    group = len(arms) * BAR + ROW_GAP
    height = top_y + group * len(rows) + 30
    o = ['<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d" '
         'font-family="Helvetica, Arial, sans-serif">' % (WIDTH, height, WIDTH, height),
         '<rect width="100%" height="100%" fill="#ffffff"/>',
         '<text x="%d" y="24" font-size="16" font-weight="bold" fill="%s">%s</text>'
         % (LEFT, TEXT, esc(title))]
    for i, a in enumerate(arms):
        lx = LEFT + (i % 3) * 200
        ly = 36 + (i // 3) * 22
        o.append('<rect x="%d" y="%d" width="12" height="12" fill="%s"/>' % (lx, ly, COLORS.get(a, MUTED)))
        o.append('<text x="%d" y="%d" font-size="12" fill="%s">%s</text>' % (lx + 18, ly + 11, TEXT, esc(a)))
    bottom = top_y + group * len(rows)
    for t in ticks:
        tx = LEFT + x(t)
        o.append('<line x1="%.1f" y1="%d" x2="%.1f" y2="%d" stroke="#e8eaed"/>' % (tx, top_y - 4, tx, bottom))
        label = ("%g" % t) if t < 10000 else ("%gk" % (t / 1000))
        o.append('<text x="%.1f" y="%d" font-size="11" fill="%s" text-anchor="middle">%s %s</text>'
                 % (tx, bottom + 16, MUTED, label, unit))
    y = top_y
    for name, r in rows:
        o.append('<text x="%d" y="%d" font-size="12" fill="%s" text-anchor="end">%s</text>'
                 % (LEFT - 8, y + len(arms) * BAR / 2 + 4, TEXT, esc(name)))
        for a in arms:
            v = r[a][key]
            w = max(x(v), 1)
            o.append('<rect x="%d" y="%d" width="%.1f" height="%d" fill="%s"/>'
                     % (LEFT, y, w, BAR - 1, COLORS.get(a, MUTED)))
            o.append('<text x="%.1f" y="%d" font-size="9" fill="%s">%s</text>'
                     % (LEFT + w + 4, y + BAR - 2, MUTED, ("%.0f" % v) if v >= 10 else ("%.1f" % v)))
            y += BAR
        y += ROW_GAP
    o.append("</svg>")
    return "\n".join(o) + "\n"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("json")
    ap.add_argument("--out", required=True)
    a = ap.parse_args()
    with open(a.json) as f:
        data = json.load(f)
    with open(os.path.join(a.out, "python-perf-time.svg"), "w") as f:
        f.write(chart(data, "ms", "Run time per workload (lower is better, log scale)", "ms", True))
    with open(os.path.join(a.out, "python-perf-rss.svg"), "w") as f:
        f.write(chart(data, "rss_mb", "Peak memory per workload (lower is better)", "MB", False))


if __name__ == "__main__":
    main()
