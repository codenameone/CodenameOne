#!/usr/bin/env python3
#
# Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
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
#
"""Draws results.json as the guide's chart, every metric relative to CMP.

Each metric is one group on the same axis: Compose Multiplatform is the dashed
100% line, and the two bars are Codename One running the same Compose source
and Codename One running the same screens written in Java, each as a share of
it. A bar half as long means half the size or half the time. The absolute
values are printed beside each bar.

    chart.py results.json out.svg
"""
import json
import sys

FONT = 'font-family="Arial, sans-serif"'
SERIES = [("codename_one", "Codename One, same Compose source", "#b5651d"),
          ("codename_one_java", "Codename One, the same screens in Java", "#2e7d5b")]


def rows(r):
    """(label, cn1 value, cmp value, formatter) for every metric present."""
    mb = lambda b: "%.1f MB" % (b / 1048576.0)
    ms = lambda v: "%.1f ms" % v if v < 10 else "%d ms" % round(v)
    us = lambda k: (lambda x: x["perf_median_of_runs_us"].get(k, 0) / 1000.0)
    key = lambda k: (lambda x: x.get(k))
    spec = [
        ("Executable, stripped", key("executable_stripped_bytes"), mb),
        ("App bundle", key("bundle_bytes"), mb),
        ("Physical footprint", key("footprint_bytes"), mb),
        ("Cold start", key("cold_start_ms"), ms),
        ("Warm start, median", key("warm_start_median_ms"), ms),
        ("Recomposition, median", us("recompose-median-us"), ms),
        ("Recomposition, p95", us("recompose-p95-us"), ms),
        ("Scroll work per frame, median", us("scroll-work-median-us"), ms),
        ("Scroll work per frame, p95", us("scroll-work-p95-us"), ms),
        ("Scroll frame interval, median", us("scroll-frame-median-us"), ms),
    ]
    base = r["compose_multiplatform"]
    out = []
    for label, g, f in spec:
        vals = [g(r[k]) if k in r else None for k, _, _ in SERIES]
        if g(base) and all(vals):
            out.append((label, vals, g(base), f))
    return out


def svg(r):
    data = rows(r)
    left, axis_w, bar_h, group_h, top = 250, 400, 13, 36, 92
    scale_max = max(1.25, max(c / m for _, vals, m, _ in data for c in vals) * 1.1)
    px = lambda share: axis_w * share / scale_max
    h = top + group_h * len(data) + 40
    w = left + axis_w + 220
    out = ['<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d">' % (w, h, w, h),
           '  <rect x="0" y="0" width="%d" height="%d" fill="#ffffff"/>' % (w, h),
           '  <text x="%d" y="24" %s font-size="14" font-weight="bold" text-anchor="middle" fill="#222222">'
           'Keynote on an iPhone simulator, relative to Compose Multiplatform</text>' % (w / 2, FONT),
           '  <text x="%d" y="43" %s font-size="10.5" text-anchor="middle" fill="#333333">'
           'The dashed line is Compose Multiplatform (100%%); each bar is a share of it. '
           'Shorter is better on every row.</text>' % (w / 2, FONT)]
    lx = 30
    for _, name, color in SERIES:
        out.append('  <rect x="%d" y="56" width="12" height="12" fill="%s"/>' % (lx, color))
        out.append('  <text x="%d" y="66" %s font-size="10.5" fill="#333333">%s</text>' % (lx + 18, FONT, name))
        lx += 18 + 7 * len(name) + 30
    for i, (label, vals, m, f) in enumerate(data):
        y = top + i * group_h
        out.append('  <text x="30" y="%d" %s font-size="10.5" font-weight="bold" fill="#333333">%s</text>'
                   % (y + 17, FONT, label))
        for j, ((_, _, color), c) in enumerate(zip(SERIES, vals)):
            by = y + j * (bar_h + 2)
            share = c / m
            out.append('  <rect x="%d" y="%d" width="%.1f" height="%d" fill="%s"/>' % (left, by, px(share), bar_h, color))
            out.append('  <text x="%.1f" y="%d" %s font-size="9.5" fill="#333333">%d%%  (%s vs %s)</text>'
                       % (left + max(px(share), px(1)) + 8, by + 10, FONT, round(share * 100), f(c), f(m)))
    x100 = left + px(1)
    out.append('  <line x1="%.1f" y1="%d" x2="%.1f" y2="%d" stroke="#4a6fa5" stroke-width="1.5" '
               'stroke-dasharray="4 3"/>' % (x100, top - 6, x100, top + group_h * len(data) - 6))
    out.append('  <text x="%.1f" y="%d" %s font-size="9.5" text-anchor="middle" fill="#4a6fa5">'
               'Compose Multiplatform = 100%%</text>' % (x100, top + group_h * len(data) + 10, FONT))
    out.append('</svg>')
    return "\n".join(out) + "\n"


if __name__ == "__main__":
    with open(sys.argv[1]) as f:
        results = json.load(f)
    with open(sys.argv[2], "w") as f:
        f.write(svg(results))
