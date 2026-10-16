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
"""Renders the developer guide's Flutter benchmark charts from benchmark results.

    scripts/flutter-bench/render_doc_charts.py result-android.json result-linux.json ... \\
        --out docs/developer-guide/img

Each argument is a per-platform result-<platform>.json as the flutter-bench
workflow uploads it (artifact flutter-bench-<platform>). One SVG is written per
metric -- flutter-bench-<metric>.svg -- with a pair of bars per platform.

The figures are the report's own: they come from benchlib.verdict, the function
that writes the published markdown, so start-up and memory are each side's
median over the paired rounds and the ratio is the median of the per-round
ratios. A chart computed any other way could disagree with the report it
illustrates. A platform where a metric was not measured is left out of that
chart rather than drawn as zero.

The SVGs are self-contained (no fonts or scripts loaded) so the website and the
PDF build render them the same way.
"""

import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import benchlib  # noqa: E402

PLATFORM_NAMES = {
    "android": "Android",
    "ios": "iOS",
    "javascript": "Web",
    "linux": "Linux",
    "macos": "macOS",
    "windows": "Windows",
}
# Display order: the platforms with every metric first.
PLATFORM_ORDER = ("android", "linux", "windows", "macos", "ios", "javascript")

# (metric, title, unit, the word a ratio above 1 reads as)
CHARTS = (
    ("install_bytes", "Installed size", "MB", "smaller"),
    ("code_bytes", "Executable code", "MB", "smaller"),
    ("cold_start_ms", "Cold start to first frame", "ms", "faster"),
    ("idle_memory_bytes", "Memory at rest", "MB", "less"),
)

CN1_COLOR = "#1a73e8"
FLUTTER_COLOR = "#9aa0a6"
TEXT_COLOR = "#202124"
MUTED_COLOR = "#5f6368"

WIDTH = 720
LEFT = 96
RIGHT = 150
BAR = 16
PAIR_GAP = 4
ROW_GAP = 18
TOP = 64


def scaled(key, value):
    """The value in the chart's unit."""
    if benchlib.METRIC_UNITS[key] == "bytes":
        return value / (1024.0 * 1024.0)
    return float(value)


def label(unit, value):
    if unit == "ms":
        return "%d ms" % round(value)
    return "%.1f MB" % value


def esc(text):
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def render(key, title, unit, word, rows):
    """rows: [(platform name, cn1 value, flutter value, ratio)] in chart units."""
    height = TOP + len(rows) * (2 * BAR + PAIR_GAP + ROW_GAP) + 36
    peak = max(max(r[1], r[2]) for r in rows)
    span = WIDTH - LEFT - RIGHT
    out = [
        '<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" '
        'viewBox="0 0 %d %d" font-family="Helvetica, Arial, sans-serif">' % (WIDTH, height, WIDTH, height),
        '<rect width="100%" height="100%" fill="#ffffff"/>',
        '<text x="%d" y="26" font-size="17" font-weight="bold" fill="%s">%s</text>'
        % (LEFT - 80, TEXT_COLOR, esc(title + " (lower is better)")),
        '<rect x="%d" y="38" width="12" height="12" fill="%s"/>' % (LEFT - 80, CN1_COLOR),
        '<text x="%d" y="49" font-size="12" fill="%s">Codename One</text>' % (LEFT - 62, TEXT_COLOR),
        '<rect x="%d" y="38" width="12" height="12" fill="%s"/>' % (LEFT + 40, FLUTTER_COLOR),
        '<text x="%d" y="49" font-size="12" fill="%s">Flutter</text>' % (LEFT + 58, TEXT_COLOR),
    ]
    y = TOP
    for name, ours, theirs, ratio in rows:
        out.append('<text x="%d" y="%d" font-size="13" text-anchor="end" fill="%s">%s</text>'
                   % (LEFT - 10, y + BAR + PAIR_GAP / 2 + 4, TEXT_COLOR, esc(name)))
        for offset, value, color in ((0, ours, CN1_COLOR), (BAR + PAIR_GAP, theirs, FLUTTER_COLOR)):
            w = max(1.0, span * value / peak)
            out.append('<rect x="%d" y="%d" width="%.1f" height="%d" fill="%s"/>'
                       % (LEFT, y + offset, w, BAR, color))
            out.append('<text x="%.1f" y="%d" font-size="11" fill="%s">%s</text>'
                       % (LEFT + w + 6, y + offset + BAR - 4, MUTED_COLOR, esc(label(unit, value))))
        verdict = "%.2fx %s" % (ratio, word)
        out.append('<text x="%d" y="%d" font-size="13" font-weight="bold" text-anchor="end" fill="%s">%s</text>'
                   % (WIDTH - 8, y + BAR + PAIR_GAP / 2 + 4, CN1_COLOR if ratio > 1.0 else MUTED_COLOR,
                      esc(verdict)))
        y += 2 * BAR + PAIR_GAP + ROW_GAP
    out.append('<text x="%d" y="%d" font-size="11" fill="%s">%s</text>'
               % (LEFT - 80, height - 12, MUTED_COLOR,
                  esc("Same Flutter Gallery source on both sides; Codename One builds it from the Dart.")))
    out.append("</svg>")
    return "\n".join(out) + "\n"


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("results", nargs="+", help="result-<platform>.json files")
    parser.add_argument("--out", required=True, help="directory to write the SVGs into")
    args = parser.parse_args(argv)

    verdicts = {}
    for path in args.results:
        with open(path) as f:
            report = json.load(f)
        verdicts[report["platform"]] = benchlib.verdict(report)

    os.makedirs(args.out, exist_ok=True)
    for key, title, unit, word in CHARTS:
        rows = []
        for platform in PLATFORM_ORDER:
            entry = verdicts.get(platform, {}).get(key)
            if not entry or entry.get("status") != "measured":
                continue
            rows.append((PLATFORM_NAMES[platform], scaled(key, entry["codenameone"]),
                         scaled(key, entry["flutter"]), entry["ratio"]))
        if not rows:
            print("%s: no platform measured it, no chart" % key)
            continue
        name = "flutter-bench-%s.svg" % key.replace("_bytes", "").replace("_ms", "").replace("_", "-")
        with open(os.path.join(args.out, name), "w") as f:
            f.write(render(key, title, unit, word, rows))
        print("wrote %s (%d platforms)" % (name, len(rows)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
