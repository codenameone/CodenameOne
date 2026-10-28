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

Each row is one metric on the same axis: Compose Multiplatform is the dashed
100% line and the bar is Codename One's value as a share of it, so a bar half
as long means half the size or half the time. Both absolute values are
printed beside the bar.

    chart.py results.json out.svg
"""
import json
import sys

FONT = 'font-family="Arial, sans-serif"'
CN1 = "#b5651d"


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
    a, b = r["codename_one"], r["compose_multiplatform"]
    return [(label, g(a), g(b), f) for label, g, f in spec if g(a) and g(b)]


def svg(r):
    data = rows(r)
    left, axis_w, row_h, top = 250, 420, 30, 70
    scale_max = max(1.25, max(c / m for _, c, m, _ in data) * 1.1)
    px = lambda share: axis_w * share / scale_max
    h = top + row_h * len(data) + 50
    w = left + axis_w + 200
    out = ['<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d">' % (w, h, w, h),
           '  <rect x="0" y="0" width="%d" height="%d" fill="#ffffff"/>' % (w, h),
           '  <text x="%d" y="24" %s font-size="14" font-weight="bold" text-anchor="middle" fill="#222222">'
           'Keynote on an iPhone simulator: Codename One relative to Compose Multiplatform</text>' % (w / 2, FONT),
           '  <text x="%d" y="43" %s font-size="10.5" text-anchor="middle" fill="#333333">'
           'The dashed line is Compose Multiplatform (100%%); a bar is Codename One as a share of it. '
           'Shorter is better on every row.</text>' % (w / 2, FONT)]
    for i, (label, c, m, f) in enumerate(data):
        y = top + i * row_h
        share = c / m
        out.append('  <text x="30" y="%d" %s font-size="10.5" font-weight="bold" fill="#333333">%s</text>'
                   % (y + 13, FONT, label))
        out.append('  <rect x="%d" y="%d" width="%.1f" height="16" fill="%s"/>' % (left, y, px(share), CN1))
        out.append('  <text x="%.1f" y="%d" %s font-size="9.5" fill="#333333">%d%%   (%s vs %s)</text>'
                   % (left + max(px(share), px(1)) + 8, y + 12, FONT, round(share * 100), f(c), f(m)))
    x100 = left + px(1)
    out.append('  <line x1="%.1f" y1="%d" x2="%.1f" y2="%d" stroke="#4a6fa5" stroke-width="1.5" '
               'stroke-dasharray="4 3"/>' % (x100, top - 8, x100, top + row_h * len(data) - 8))
    out.append('  <text x="%.1f" y="%d" %s font-size="9.5" text-anchor="middle" fill="#4a6fa5">'
               'Compose Multiplatform = 100%%</text>' % (x100, top + row_h * len(data) + 8, FONT))
    out.append('</svg>')
    return "\n".join(out) + "\n"


if __name__ == "__main__":
    with open(sys.argv[1]) as f:
        results = json.load(f)
    with open(sys.argv[2], "w") as f:
        f.write(svg(results))
