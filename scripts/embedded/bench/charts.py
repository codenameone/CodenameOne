#!/usr/bin/env python3
"""Draws the developer guide's embedded benchmark charts from the raw results.

    scripts/embedded/bench/charts.py [output-dir [server-results]]

Reads scripts/embedded/bench/results.txt (the GUI comparison, written by run.sh)
and scripts/embedded/bench/headless/results.txt (the server comparison, written by
headless/run.sh), takes the same medians as the tables in
docs/developer-guide/embedded-benchmarks.adoc and writes one SVG per chart into
docs/developer-guide/img (or output-dir). CI's performance gate passes the
server results its own run produced, so its charts show that run. Standard library only: the charts are
plain horizontal bars, and a plotting package is one more thing a contributor
would have to install to regenerate them.
"""

import os
import statistics
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", "..", ".."))

BACKGROUND = "#0b132b"
GRID = "#2a3350"
TEXT = "#e0e6f0"
MUTED = "#8a96b0"
HIGHLIGHT = "#3fb68b"
OTHER = "#5b7bd5"
SERIES = ["#3fb68b", "#5b7bd5", "#e0a458"]

GUI_NAMES = [
    ("codenameone", "Codename One"),
    ("slint", "Slint"),
    ("qt", "Qt Widgets"),
    ("javafx", "JavaFX"),
    ("chromium", "Chromium kiosk"),
]

SERVER_NAMES = [
    ("codenameone", "Codename One"),
    ("rust", "Rust, axum"),
    ("go", "Go, net/http"),
    ("node", "Node.js"),
    ("python", "Python, FastAPI"),
    ("spring", "Spring Boot"),
]


def number(token):
    """A measurement, with run.sh's "50+" (a lower bound) read as its value and
    its "?" (not measured) as None."""
    if token == "?":
        return None
    return float(token.rstrip("+"))


def median(values):
    measured = [v for v in values if v is not None]
    return statistics.median(measured) if measured else 0.0


def read_rows(path, width):
    rows = []
    with open(path) as source:
        for line in source:
            fields = line.split()
            if len(fields) == width and not line.startswith("#"):
                rows.append(fields)
    return rows


def gui_medians(path):
    """framework -> column -> median, over every run of that framework and mode.

    Columns: first_frame_ms fps cpu_pct pss_kb private_kb rss_kb mapped_kb files.
    """
    runs = {}
    for fields in read_rows(path, 10):
        key = (fields[0], fields[1])
        runs.setdefault(key, []).append([number(v) for v in fields[2:]])
    medians = {}
    for (name, mode), values in runs.items():
        medians.setdefault(name, {})[mode] = [median(c) for c in zip(*values)]
    # Everything but the first frame is measured once the application runs, where
    # a cold and a warm start are the same thing, so the table pools both.
    for (name, mode), values in runs.items():
        pooled = medians[name].setdefault("all", [])
        pooled.extend(values)
    for name in medians:
        medians[name]["all"] = [median(c) for c in zip(*medians[name]["all"])]
    return medians


def server_medians(path):
    """runtime -> columns, the median of every run after the first round.

    Columns: start_ms idle_pss_kb idle_private_kb mapped_kb mapped_files
    steady_cpu_pct steady_pss_kb peak_rps peak_p99_ms peak_pss_kb. The first round
    (a cold and a warm run per runtime) pages the container image in, so it is left
    out, as the table says.
    """
    seen = {}
    runs = {}
    for fields in read_rows(path, 12):
        name = fields[0]
        seen[name] = seen.get(name, 0) + 1
        if seen[name] <= 2:
            continue
        runs.setdefault(name, []).append([number(v) for v in fields[2:]])
    return {name: [median(c) for c in zip(*values)]
            for name, values in runs.items()}


def escape(text):
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def label(value, unit):
    if value >= 1000:
        text = "{:,.0f}".format(value)
    elif value >= 100:
        text = "{:.0f}".format(value)
    else:
        text = "{:.1f}".format(value).rstrip("0").rstrip(".")
    return text + unit


def nice_max(value):
    """The axis end: the first round step of a power of ten at or above value, so
    the longest bar fills most of the plot."""
    step = 1.0
    while step * 10 <= value:
        step *= 10
    for factor in (1, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10):
        if step * factor >= value:
            return step * factor
    return step * 10


def bar_chart(path, title, subtitle, rows, unit, series=None):
    """Horizontal bars, one group per row.

    rows is a list of (label, [values]); series names the values when there is
    more than one per row. The first row is Codename One and is drawn highlighted
    when there is a single series.
    """
    count = len(rows[0][1])
    left, right, top = 150, 90, 78 + (26 if series else 0)
    bar, gap, group_gap = 18, 4, 16
    group = count * bar + (count - 1) * gap
    width = 720
    plot = width - left - right
    height = top + len(rows) * (group + group_gap) + 40
    peak = nice_max(max(v for _, values in rows for v in values))

    out = []
    out.append('<?xml version="1.0" encoding="utf-8"?>')
    out.append('<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" '
               'viewBox="0 0 {w} {h}" font-family="Helvetica, Arial, sans-serif">'
               .format(w=width, h=height))
    out.append('<rect width="100%" height="100%" fill="{}"/>'.format(BACKGROUND))
    out.append('<text x="20" y="32" font-size="18" font-weight="bold" fill="{}">{}</text>'
               .format(TEXT, escape(title)))
    out.append('<text x="20" y="54" font-size="13" fill="{}">{}</text>'
               .format(MUTED, escape(subtitle)))
    if series:
        x = left
        for index, name in enumerate(series):
            out.append('<rect x="{}" y="68" width="12" height="12" fill="{}"/>'
                       .format(x, SERIES[index]))
            out.append('<text x="{}" y="79" font-size="12" fill="{}">{}</text>'
                       .format(x + 18, TEXT, escape(name)))
            x += 34 + int(6.6 * len(name))
    bottom = height - 30
    for tick in range(0, 6):
        value = peak * tick / 5
        x = left + plot * tick / 5
        out.append('<line x1="{x:.1f}" y1="{t}" x2="{x:.1f}" y2="{b}" stroke="{c}" '
                   'stroke-width="0.8"/>'.format(x=x, t=top - 8, b=bottom, c=GRID))
        out.append('<text x="{:.1f}" y="{}" font-size="11" fill="{}" text-anchor="middle">'
                   '{}</text>'.format(x, bottom + 16, MUTED, label(value, "")))
    y = top
    for row_index, (name, values) in enumerate(rows):
        out.append('<text x="{}" y="{:.1f}" font-size="13" fill="{}" text-anchor="end" '
                   'font-weight="{}">{}</text>'
                   .format(left - 10, y + group / 2 + 4.5, TEXT,
                           "bold" if row_index == 0 else "normal", escape(name)))
        for index, value in enumerate(values):
            if series:
                colour = SERIES[index]
            else:
                colour = HIGHLIGHT if row_index == 0 else OTHER
            length = max(plot * value / peak, 1.5)
            by = y + index * (bar + gap)
            out.append('<rect x="{}" y="{}" width="{:.1f}" height="{}" rx="2" fill="{}"/>'
                       .format(left, by, length, bar, colour))
            out.append('<text x="{:.1f}" y="{:.1f}" font-size="12" fill="{}">{}</text>'
                       .format(left + length + 6, by + bar / 2 + 4, TEXT, label(value, unit)))
        y += group + group_gap
    out.append('</svg>')
    with open(path, "w") as target:
        target.write("\n".join(out) + "\n")


def main():
    target = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        ROOT, "docs", "developer-guide", "img")
    gui = gui_medians(os.path.join(HERE, "results.txt"))
    server = server_medians(sys.argv[2] if len(sys.argv) > 2 else
                            os.path.join(HERE, "headless", "results.txt"))
    names = dict(GUI_NAMES)

    def gui_rows(column, mode, scale=1.0):
        return [(names[k], [gui[k][mode][column] / scale]) for k, _ in GUI_NAMES if k in gui]

    bar_chart(os.path.join(target, "embedded-gui-fps.svg"),
              "Frames per second", "The same animated screen, software rendering, higher is better",
              gui_rows(1, "all"), "")
    bar_chart(os.path.join(target, "embedded-gui-first-frame.svg"),
              "First frame, cold start", "Milliseconds from launch to the first frame, lower is better",
              gui_rows(0, "cold"), " ms")
    bar_chart(os.path.join(target, "embedded-gui-memory.svg"),
              "Memory while animating", "Proportional set size in MB, lower is better",
              gui_rows(3, "all", 1000.0), " MB")

    labels = dict(SERVER_NAMES)
    order = [k for k, _ in SERVER_NAMES if k in server]
    bar_chart(os.path.join(target, "embedded-server-throughput.svg"),
              "Peak throughput", "JSON requests per second on two cores, 32 connections, higher is better",
              [(labels[k], [server[k][7]]) for k in order], "")
    bar_chart(os.path.join(target, "embedded-server-start.svg"),
              "Start", "Milliseconds from launch to the first answered request, lower is better",
              [(labels[k], [server[k][0]]) for k in order], " ms")
    bar_chart(os.path.join(target, "embedded-server-memory.svg"),
              "Memory", "Proportional set size in MB, lower is better",
              [(labels[k], [server[k][1] / 1000.0, server[k][6] / 1000.0, server[k][9] / 1000.0])
               for k in order], " MB",
              series=["Idle", "Steady, 200 requests/s", "Peak load"])


if __name__ == "__main__":
    main()

