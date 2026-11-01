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

    render_charts.py [common.json] [--compare compare.json] --out docs/developer-guide/img

common.json is what common_compare.py --json writes: python-perf-time.svg
(milliseconds, log scale, because the runtimes differ by two orders of
magnitude) and python-perf-rss.svg (peak RSS, linear). compare.json is what
compare.py --json writes: python-kernels-time.svg, python-kernels-rss.svg,
python-startup.svg, python-flask-rps.svg, python-flask-memory.svg and
python-flask-tradeoff.svg. The
SVGs are self-contained so the website and the PDF build render them alike.
"""

import argparse
import json
import math
import os

COLORS = {
    "CPython": "#9aa0a6",
    "PyPy": "#f9ab00",
    "GraalPy": "#a142f4",
    "Codename One Python": "#1a73e8",
    "Codename One Java": "#0b8043",
}
# The budgeted Codename One arm is named "<CN1>, <n> MB budget"; it takes a lighter
# shade of the same blue so the two runs of one binary read as one runtime.
BUDGETED_COLOR = "#8ab4f8"


def color(arm):
    if arm in COLORS:
        return COLORS[arm]
    if arm.startswith("Codename One Python,"):
        return BUDGETED_COLOR
    return MUTED


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
    arms = [a for a in arms if all(a in r for _, r in rows)]
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
        o.append('<rect x="%d" y="%d" width="12" height="12" fill="%s"/>' % (lx, ly, color(a)))
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
                     % (LEFT, y, w, BAR - 1, color(a)))
            o.append('<text x="%.1f" y="%d" font-size="9" fill="%s">%s</text>'
                     % (LEFT + w + 4, y + BAR - 2, MUTED, ("%.0f" % v) if v >= 10 else ("%.1f" % v)))
            y += BAR
        y += ROW_GAP
    o.append("</svg>")
    return "\n".join(o) + "\n"


def tradeoff(fl, title):
    """Requests per second against footprint after load, one point per runtime.

    Throughput is on a log axis because the runtimes differ by two orders of magnitude;
    the two Codename One points are joined, since they are one binary choosing two
    different places on the same trade."""
    pts = [(n, r["loaded_mb"], r["rps"]) for n, r in fl.items()]
    width, height, left, right, top, bottom = WIDTH, 420, 80, 40, 70, 50
    pw, ph = width - left - right, height - top - bottom
    mx = max(p[1] for p in pts)
    step = 10 ** math.floor(math.log10(mx))
    xtop = math.ceil(mx / step) * step
    lo_e = math.floor(math.log10(min(p[2] for p in pts)))
    hi_e = math.ceil(math.log10(max(p[2] for p in pts)))

    def X(v):
        return left + pw * v / xtop

    def Y(v):
        return top + ph - ph * (math.log10(v) - lo_e) / (hi_e - lo_e)
    o = ['<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d" '
         'font-family="Helvetica, Arial, sans-serif">' % (width, height, width, height),
         '<rect width="100%" height="100%" fill="#ffffff"/>',
         '<text x="%d" y="24" font-size="16" font-weight="bold" fill="%s">%s</text>' % (left, TEXT, esc(title)),
         '<text x="%d" y="44" font-size="12" fill="%s">%s</text>' % (
             left, MUTED, esc("Up is faster, left is smaller. Requests/s on a log scale."))]
    for i in range(int(xtop / step) + 1):
        tx = X(step * i)
        o.append('<line x1="%.1f" y1="%d" x2="%.1f" y2="%d" stroke="#e8eaed"/>' % (tx, top, tx, top + ph))
        o.append('<text x="%.1f" y="%d" font-size="11" fill="%s" text-anchor="middle">%g MB</text>'
                 % (tx, top + ph + 16, MUTED, step * i))
    for e in range(lo_e, hi_e + 1):
        ty = Y(10 ** e)
        o.append('<line x1="%d" y1="%.1f" x2="%d" y2="%.1f" stroke="#e8eaed"/>' % (left, ty, left + pw, ty))
        label = ("%g" % 10 ** e) if e < 4 else ("%gk" % (10 ** e / 1000))
        o.append('<text x="%d" y="%.1f" font-size="11" fill="%s" text-anchor="end">%s</text>'
                 % (left - 6, ty + 4, MUTED, label))
    o.append('<text x="%.1f" y="%d" font-size="12" fill="%s" text-anchor="middle">%s</text>'
             % (left + pw / 2, height - 10, TEXT, "physical footprint after load"))
    cn1 = [p for p in pts if p[0].startswith("Codename One Python")]
    if len(cn1) == 2:
        o.append('<line x1="%.1f" y1="%.1f" x2="%.1f" y2="%.1f" stroke="%s" stroke-dasharray="4 3"/>'
                 % (X(cn1[0][1]), Y(cn1[0][2]), X(cn1[1][1]), Y(cn1[1][2]), COLORS["Codename One Python"]))
    for n, m, r in pts:
        o.append('<circle cx="%.1f" cy="%.1f" r="6" fill="%s"/>' % (X(m), Y(r), color(n)))
        anchor = "end" if X(m) > left + pw * 0.7 else "start"
        dx = -10 if anchor == "end" else 10
        o.append('<text x="%.1f" y="%.1f" font-size="11" fill="%s" text-anchor="%s">%s (%.0f MB, %s/s)</text>'
                 % (X(m) + dx, Y(r) + 4, TEXT, anchor, esc(n), m,
                    ("%.0f" % r) if r < 10000 else ("%.1fk" % (r / 1000))))
    o.append("</svg>")
    return "\n".join(o) + "\n"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("json", nargs="?")
    ap.add_argument("--compare")
    ap.add_argument("--out", required=True)
    a = ap.parse_args()
    out = []
    if a.json:
        with open(a.json) as f:
            data = json.load(f)
        out.append(("python-perf-time.svg", chart(data, "ms", "Run time per workload (lower is better, log scale)", "ms", True)))
        out.append(("python-perf-rss.svg", chart(data, "rss_mb", "Peak memory per workload (lower is better)", "MB", False)))
    if a.compare:
        with open(a.compare) as f:
            c = json.load(f)
        kernels = {"arms": c["arms"], "results": c["kernels"]}
        out.append(("python-kernels-time.svg", chart(kernels, "ms", "Kernel run time (lower is better, log scale)", "ms", True)))
        out.append(("python-kernels-rss.svg", chart(kernels, "rss_mb", "Kernel peak physical footprint (lower is better)", "MB", False)))
        startup = {"arms": c["arms"], "results": {
            "wall time (ms)": {n: {"v": r["ms"]} for n, r in c["startup"].items()},
            "peak memory (MB)": {n: {"v": r["rss_mb"]} for n, r in c["startup"].items()}}}
        out.append(("python-startup.svg", chart(startup, "v", "Startup, a program that prints one line (lower is better)", "", False)))
        fl = c["flask"]
        out.append(("python-flask-rps.svg", chart(
            {"arms": c["arms"], "results": {"requests/s": {n: {"v": r["rps"]} for n, r in fl.items()}}},
            "v", "Flask JSON API throughput (higher is better)", "", False)))
        mem = {"idle": "idle_mb", "after load": "loaded_mb", "5s after load": "settled_mb",
               "peak": "peak_mb"}
        rows = {k: {n: {"v": r[f]} for n, r in fl.items()} for k, f in mem.items()}
        rows["deployed size"] = {n: {"v": v} for n, v in c["deploy"].items()}
        out.append(("python-flask-memory.svg", chart(
            {"arms": c["arms"], "results": rows}, "v",
            "Flask server physical footprint and deployed size, MB (lower is better)", "MB", False)))
        out.append(("python-flask-tradeoff.svg", tradeoff(
            fl, "Flask: throughput against memory, the same binary with and without a budget")))
    for name, svg in out:
        with open(os.path.join(a.out, name), "w") as f:
            f.write(svg)


if __name__ == "__main__":
    main()
