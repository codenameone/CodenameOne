#!/usr/bin/env python3
"""Render the measured tables and charts of the Swing and JavaFX chapter.

  docs/developer-guide/desktop-compat-benchmarks.py \\
      --linux DIR --macos DIR --mobile DIR \\
      [--out docs/developer-guide/desktop-compat-benchmarks.adoc]

DIR holds one directory for each application, as the harness in
scripts/desktop-compat-benchmarks leaves them:

  --linux, --macos   <application>/result.json            (bench.sh)
  --mobile           <application>/*ios-size.json
                     <application>/*android-size.json     (mobile-size.sh)

Any of the three may be left out; its tables are then left out too.

Everything the chapter says about a number is written here, from the results:
the tables, the sentence under each that counts where the native build won and
where it lost, and the description of the machine. Desktop-Interop.asciidoc
includes the file once, whole, and quotes no figure of its own, so new results
are published by running this again and committing what it writes: that file
and the charts beside it, img/desktop-compat-*.svg, drawn from the same figures.

Nothing is estimated. A cell with no measurement behind it is printed as "--",
and the cells that were attempted and failed are listed with the harness's
reason. A ratio is always the JVM figure divided by the native one: above 1 the
native build is smaller or quicker, below 1 it is larger or slower, and both
are counted the same way.

An application is published under the description in LABELS, never under the
directory name the harness gave it. A result with no entry there stops the run:
add the line, with a description that names no third party.

Python 3.9 compatible. The tables need the standard library only; the charts
need matplotlib, and the same figures draw the same bytes. The arithmetic and the cell
formats are scripts/desktop-compat-benchmarks/report.py's own.
"""

import argparse
import glob
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "scripts", "desktop-compat-benchmarks"))
from report import EMPTY, dig, megabytes, megabytes_kb, percent, timing  # noqa: E402

# Directory name -> (what the chapter calls it, toolkit). The order here is the
# order of the rows.
LABELS = [
    ("swing-gallery", "`swing-gallery` sample", "Swing"),
    ("swingset2", "SwingSet2, the JDK demo", "Swing"),
    ("notepad", "Notepad, the JDK demo", "Swing"),
    ("tetris", "Falling-blocks game", "Swing"),
    ("javafx-gallery", "`javafx-gallery` sample", "JavaFX"),
    ("fx2048", "Open-source 2048 game", "JavaFX"),
    ("address4", "Address book", "JavaFX"),
    ("mandelbrot", "Mandelbrot explorer", "JavaFX"),
]
ORDER = dict((name, index) for index, (name, _label, _kind) in enumerate(LABELS))
LABEL = dict((name, label) for name, label, _kind in LABELS)
TOOLKIT = dict((name, kind) for name, _label, kind in LABELS)

VARIANTS = (("jvm_default", "the default JVM"), ("jvm_tuned", "the tuned JVM"),
            ("cn1_native", "the native build"))

# The order the chapter discusses them in: what is distributed, start-up,
# memory, idle CPU.
ORDER_OF_TABLES = ("linux-size", "macos-size", "mobile-size", "linux-layers", "linux-startup-warm",
                   "linux-startup-cold", "linux-memory-idle", "linux-memory-used", "linux-memory-peak",
                   "linux-idle-cpu")

# A native build idling above this share of one core is repainting or polling
# with nothing to do; the harness draws the line at the same place.
IDLE_CPU_LIMIT = 1.0


def plain(label):
    """A label as running text: without the backticks of a table cell."""
    return label


def name_of(result):
    return dig(result, "app", "name")


def load_results(directory):
    results = []
    if not directory:
        return results
    for path in sorted(glob.glob(os.path.join(directory, "*", "result.json"))):
        with open(path) as handle:
            results.append(json.load(handle))
    check_names([name_of(r) for r in results], directory)
    results.sort(key=lambda r: ORDER[name_of(r)])
    return results


def load_mobile(directory):
    sizes = {}
    if not directory:
        return sizes
    for app in sorted(os.listdir(directory)):
        entry = {}
        for target in ("ios", "android"):
            found = sorted(glob.glob(os.path.join(directory, app, "*" + target + "-size.json")))
            if found:
                with open(found[-1]) as handle:
                    entry[target] = json.load(handle)
        if entry:
            sizes[app] = entry
    check_names(list(sizes), directory)
    return sizes


def check_names(names, where):
    unknown = [n for n in names if n not in LABEL]
    if unknown:
        sys.exit("%s: no description for %s. Add a line to LABELS in %s; the description "
                 "must not name a third party." % (where, ", ".join(sorted(unknown)), __file__))


def value_ratio(baseline, candidate):
    if baseline is None or candidate is None or candidate == 0:
        return None
    return float(baseline) / float(candidate)


def show_ratio(value):
    return EMPTY if value is None else "%.1fx" % value


# The series of a chart, in the colours the comparison charts of the website
# use: the native build, the default JVM, the tuned JVM.
NATIVE_SERIES = ("Native build", "#55d4d0")
DEFAULT_SERIES = ("JVM, default", "#f4ad55")
TUNED_SERIES = ("JVM, tuned", "#9eafff")
CHART_DIRECTORY = "img"
CHARTS = []


def chart(tag, title, alt, unit, labels, series, note):
    """Queues a chart of horizontal bars, one group for each application, and
    answers the line that places it. `series` is a list of
    ((name, colour), values); a value that was not measured is None."""
    rows = [(label, [values[i] for _s, values in series]) for i, label in enumerate(labels)]
    rows = [row for row in rows if any(v is not None for v in row[1])]
    if not rows:
        return ""
    CHARTS.append((tag, title, unit, rows, [s for s, _v in series], note))
    return 'image::%s/desktop-compat-%s.svg["%s",scaledwidth=90%%]' % (CHART_DIRECTORY, tag, alt)


def draw_charts(directory):
    """Writes the queued charts. matplotlib is needed for this and for nothing
    else, so it is imported here: the tables are written without it."""
    if not CHARTS:
        return
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    ground, ink, faint, rule = "#0b132b", "#eef2ff", "#c3cbe6", "#2a3350"
    # The same bytes for the same figures: no date, and ids that do not
    # change from one run to the next.
    plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 11, "svg.fonttype": "path",
                         "svg.hashsalt": "desktop-compat-benchmarks"})
    if not os.path.isdir(directory):
        os.makedirs(directory)
    for tag, title, unit, rows, series, note in CHARTS:
        step = 0.8 / len(series)
        figure, axes = plt.subplots(figsize=(10, 1.5 + len(rows) * (0.34 * len(series) + 0.3)))
        figure.patch.set_facecolor(ground)
        axes.set_facecolor(ground)
        most = max(v for _l, values in rows for v in values if v is not None)
        for at, (_label, values) in enumerate(rows):
            for index, value in enumerate(values):
                y = at + (index - (len(series) - 1) / 2.0) * step
                if value is None:
                    axes.text(most * 0.012, y, "not measured", va="center", color=faint, fontsize=9)
                    continue
                axes.barh(y, value, height=step * 0.86, color=series[index][1],
                          label=series[index][0] if at == 0 else None)
                axes.text(value + most * 0.012, y, ("%.0f" if value >= 100 else "%.1f") % value,
                          va="center", color=ink, fontsize=9)
        axes.set_yticks(range(len(rows)))
        axes.set_yticklabels([label.replace("`", "") for label, _v in rows], color=ink)
        axes.invert_yaxis()
        axes.set_xlim(0, most * 1.12)
        axes.set_xlabel(unit, color=faint)
        axes.tick_params(colors=faint, length=0)
        axes.xaxis.grid(True, color=rule, linewidth=0.8)
        axes.set_axisbelow(True)
        for side in ("top", "right", "left"):
            axes.spines[side].set_visible(False)
        axes.spines["bottom"].set_color(rule)
        axes.set_title(title + "\nLower is better", loc="left", color=ink, fontsize=15, pad=30)
        # Above the bars, under the title: inside the plot it covers the longest bar.
        legend = axes.legend(loc="lower left", bbox_to_anchor=(-0.01, 1.0), frameon=False, fontsize=10,
                             ncol=len(series), columnspacing=1.4, handlelength=1.2)
        for text in legend.get_texts():
            text.set_color(ink)
        figure.text(0.99, 0.01, note, ha="right", va="bottom", color=faint, fontsize=8)
        figure.tight_layout(rect=(0, 0.03, 1, 1))
        figure.savefig(os.path.join(directory, "desktop-compat-%s.svg" % tag), facecolor=ground,
                       metadata={"Date": None})
        plt.close(figure)


class Table(object):
    def __init__(self, tag, title, header, widths=None):
        self.tag = tag
        self.title = title
        self.header = header
        self.widths = widths
        self.rows = []
        self.after = []

    pictures = ()

    def add(self, *cells):
        self.rows.append([str(cell) for cell in cells])

    def say(self, paragraph):
        if paragraph:
            self.after.append(paragraph)

    def show(self, line):
        """Places a chart above the table; `line` is what [chart] answered."""
        if line:
            self.pictures = list(self.pictures) + [line]

    def render(self):
        widths = self.widths or ["<3"] + [">2"] * (len(self.header) - 1)
        lines = []
        for picture in self.pictures:
            lines += [picture, ""]
        lines += ["." + self.title, '[cols="%s",options="header"]' % ",".join(widths), "|==="]
        lines.append(" ".join("|" + cell for cell in self.header))
        for row in self.rows:
            lines.append("")
            lines.append(" ".join("|" + cell for cell in row))
        lines.append("|===")
        for paragraph in self.after:
            lines += ["", paragraph]
        lines.append("")
        return "\n".join(lines)


def count(number, singular, plural=None):
    return "%d %s" % (number, singular if number == 1 else (plural or singular + "s"))


def tally(pairs, better, worse, baseline):
    """One sentence on (label, ratio, baseline text, native text) rows: how many
    the native build won, how many it lost, and the two ends of the range."""
    measured = [p for p in pairs if p[1] is not None]
    if not measured:
        return ""
    won = [p for p in measured if p[1] > 1.0499]
    lost = [p for p in measured if p[1] < 0.9501]
    level = len(measured) - len(won) - len(lost)
    parts = []
    if won:
        parts.append("%s in %s" % (better, count(len(won), "application")))
    if lost:
        parts.append("%s in %s" % (worse, count(len(lost), "application")))
    if level:
        parts.append("within 5%% of it in %s" % count(level, "application"))
    out = "Against %s the native build is %s, of the %d measured." % (
        baseline, ", ".join(parts[:-1]) + (" and " if len(parts) > 1 else "") + parts[-1], len(measured))
    top = max(measured, key=lambda p: p[1])
    bottom = min(measured, key=lambda p: p[1])
    if won:
        out += " Its best result is %s for %s (%s against %s)." % (
            show_ratio(top[1]), plain(top[0]), top[3], top[2])
    if lost:
        out += " Its worst is %s for %s (%s against %s)." % (
            show_ratio(bottom[1]), plain(bottom[0]), bottom[3], bottom[2])
    return out


def unmeasured(results):
    lines = []
    for result in results:
        for variant, words in VARIANTS:
            for error in dig(result, "run", variant, "errors") or []:
                reason = str(error).split(": ")[-1].strip().rstrip(".")
                lines.append("%s under %s (%s)" % (plain(LABEL[name_of(result)]), words, reason))
    if not lines:
        return ""
    return "Not measured, and shown as `--`: " + "; ".join(lines) + "."


def megabytes_of(value, unit=1048576.0):
    return None if value is None else value / unit


def where_measured(results):
    """The line under a chart: what the figures were measured on."""
    facts = (dig(results[0], "environment") or {}) if results else {}
    parts = [str(facts[k]) for k in ("os", "machine") if usable(facts.get(k))]
    if usable(facts.get("host_cpu")):
        parts.append("in a container on " + str(facts["host_cpu"]).split("(")[0].strip())
    elif usable(facts.get("cpu")) and not str(facts["cpu"]).startswith("0x"):
        parts.append(str(facts["cpu"]))
    return ", ".join(parts)


def linux_tables(results):
    tables = []
    note = where_measured(results)
    names = [LABEL[name_of(r)] for r in results]
    missing = unmeasured(results)

    size = Table("linux-size", "Linux: the distributed application (MB)",
                 ["Application", "Toolkit", "`jlink` image", "`jlink` image, zipped", "`jpackage` image, zipped",
                  "Native build", "Native build, zipped", "JVM image / native build", "JVM zip / native zip"],
                 ["<3", "<1", ">1", ">1", ">1", ">1", ">1", ">1", ">1"])
    pairs = []
    installed = []
    for result in results:
        name = name_of(result)
        jlink = dig(result, "size", "baseline_jlink", "zip_bytes")
        native = dig(result, "size", "cn1_linux", "zip_bytes")
        ratio = value_ratio(jlink, native)
        pairs.append((LABEL[name], ratio, megabytes(jlink) + " MB", megabytes(native) + " MB"))
        jlink_disk = dig(result, "size", "baseline_jlink", "bytes")
        native_disk = dig(result, "size", "cn1_linux", "bytes")
        disk_ratio = value_ratio(jlink_disk, native_disk)
        installed.append((LABEL[name], disk_ratio, megabytes(jlink_disk) + " MB", megabytes(native_disk) + " MB"))
        size.add(LABEL[name], TOOLKIT[name],
                 megabytes(jlink_disk), megabytes(jlink),
                 megabytes(dig(result, "size", "baseline_jpackage", "zip_bytes")),
                 megabytes(native_disk), megabytes(native), show_ratio(disk_ratio), show_ratio(ratio))
    for tag, key, title, alt in (
            ("linux-size", "bytes", "Linux: the application as installed",
             "Bar chart of the installed size of each application on Linux: the native build beside the jlink image"),
            ("linux-size-zipped", "zip_bytes", "Linux: the application as downloaded, zipped",
             "Bar chart of the zipped size of each application on Linux: the native build beside the jlink image")):
        size.show(chart(tag, title, alt, "MB", names, [
            (NATIVE_SERIES, [megabytes_of(dig(r, "size", "cn1_linux", key)) for r in results]),
            (("JVM image, jlink", DEFAULT_SERIES[1]),
             [megabytes_of(dig(r, "size", "baseline_jlink", key)) for r in results])], note))
    size.say("The `jlink` image is the application with the smallest Java runtime that runs it: the "
             "application's jars and the modules `jlink` found it to need. The native build is one "
             "executable file with nothing beside it. Both rely on the desktop libraries of the system, "
             "X11 and, for the native build and for JavaFX, GTK; neither column counts them.")
    size.say(tally(installed, "smaller", "larger", "the `jlink` image as installed"))
    size.say(tally(pairs, "smaller", "larger", "the zipped `jlink` image"))
    tables.append(size)

    measured = [r for r in results if dig(r, "run")]
    if not measured:
        return tables

    idle_seconds = dig(measured[0], "run", "cn1_native", "idle_seconds") or 10
    for section, tag, title in (
            ("idle", "linux-memory-idle", "Linux: memory %g seconds after the first frame (MB, RSS / PSS)"
             % idle_seconds),
            ("after_script", "linux-memory-used",
             "Linux: memory after the scripted interaction (MB, RSS / PSS)")):
        table = Table(tag, title, ["Application", "JVM, default", "JVM, tuned", "Native build",
                                   "Default RSS / native", "Tuned RSS / native"])
        against_default, against_tuned = [], []
        for result in measured:
            label = LABEL[name_of(result)]
            cells, rss = [], {}
            for variant, _words in VARIANTS:
                rss[variant] = dig(result, "run", variant, section, "rss_kb")
                pss = dig(result, "run", variant, section, "pss_kb")
                cells.append(EMPTY if rss[variant] is None
                             else "%s / %s" % (megabytes_kb(rss[variant]), megabytes_kb(pss)))
            native = megabytes_kb(rss["cn1_native"]) + " MB"
            first = value_ratio(rss["jvm_default"], rss["cn1_native"])
            second = value_ratio(rss["jvm_tuned"], rss["cn1_native"])
            against_default.append((label, first, megabytes_kb(rss["jvm_default"]) + " MB", native))
            against_tuned.append((label, second, megabytes_kb(rss["jvm_tuned"]) + " MB", native))
            table.add(label, cells[0], cells[1], cells[2], show_ratio(first), show_ratio(second))
        if section == "idle":
            table.show(chart(tag, "Linux: memory %g seconds after the first frame" % idle_seconds,
                             "Bar chart of the resident memory of each application on Linux once it is idle: "
                             "the native build beside the default and the tuned JVM", "MB of RSS",
                             [LABEL[name_of(r)] for r in measured],
                             [(series, [megabytes_of(dig(r, "run", variant, section, "rss_kb"), 1024.0)
                                        for r in measured])
                              for series, variant in ((NATIVE_SERIES, "cn1_native"),
                                                      (DEFAULT_SERIES, "jvm_default"),
                                                      (TUNED_SERIES, "jvm_tuned"))], note))
        table.say(tally(against_default, "smaller", "larger", "the default JVM"))
        table.say(tally(against_tuned, "smaller", "larger", "the tuned JVM"))
        table.say(missing)
        tables.append(table)

    peak = Table("linux-memory-peak", "Linux: peak RSS over a whole session (MB)",
                 ["Application", "JVM, default", "JVM, tuned", "Native build", "Default / native",
                  "Tuned / native", "Tuned `-Xmx` (MB)"])
    for result in measured:
        native = dig(result, "run", "cn1_native", "peak_rss_kb")
        default = dig(result, "run", "jvm_default", "peak_rss_kb")
        tuned = dig(result, "run", "jvm_tuned", "peak_rss_kb")
        peak.add(LABEL[name_of(result)], megabytes_kb(default), megabytes_kb(tuned), megabytes_kb(native),
                 show_ratio(value_ratio(default, native)), show_ratio(value_ratio(tuned, native)),
                 dig(result, "tuned", "xmx_mb") or EMPTY)
    tables.append(peak)

    for mode in ("warm", "cold"):
        table = Table("linux-startup-" + mode,
                      "Linux: %s start, from process start to the first painted frame "
                      "(ms, median with the range)" % mode,
                      ["Application", "JVM, default", "JVM, tuned", "Native build", "Default / native",
                       "Tuned / native"])
        against_default, against_tuned = [], []
        for result in measured:
            label = LABEL[name_of(result)]
            times = dict((variant, dig(result, "run", variant, "startup_" + mode, "first_paint_ms"))
                         for variant, _words in VARIANTS)
            medians = dict((variant, dig(times[variant], "median")) for variant in times)

            def text(variant):
                return EMPTY if medians[variant] is None else "%.0f ms" % medians[variant]
            first = value_ratio(medians["jvm_default"], medians["cn1_native"])
            second = value_ratio(medians["jvm_tuned"], medians["cn1_native"])
            against_default.append((label, first, text("jvm_default"), text("cn1_native")))
            against_tuned.append((label, second, text("jvm_tuned"), text("cn1_native")))
            table.add(label, timing(times["jvm_default"]), timing(times["jvm_tuned"]),
                      timing(times["cn1_native"]), show_ratio(first), show_ratio(second))
        if mode == "cold":
            table.show(chart("linux-startup-cold", "Linux: cold start, to the first painted frame",
                             "Bar chart of the cold start of each application on Linux: the native build "
                             "beside the default and the tuned JVM", "ms, median",
                             [LABEL[name_of(r)] for r in measured],
                             [(series, [dig(r, "run", variant, "startup_cold", "first_paint_ms", "median")
                                        for r in measured])
                              for series, variant in ((NATIVE_SERIES, "cn1_native"),
                                                      (DEFAULT_SERIES, "jvm_default"),
                                                      (TUNED_SERIES, "jvm_tuned"))], note))
        table.say(tally(against_default, "quicker", "slower", "the default JVM"))
        table.say(tally(against_tuned, "quicker", "slower", "the tuned JVM"))
        tables.append(table)

    cpu = Table("linux-idle-cpu", "Linux: CPU while idle (share of one core)",
                ["Application", "JVM, default", "JVM, tuned", "Native build"])
    busy = []
    for result in measured:
        value = dig(result, "run", "cn1_native", "idle", "cpu_percent")
        if value is not None and value > IDLE_CPU_LIMIT:
            busy.append("%s (%s)" % (plain(LABEL[name_of(result)]), percent(value)))
        cpu.add(LABEL[name_of(result)], percent(dig(result, "run", "jvm_default", "idle", "cpu_percent")),
                percent(dig(result, "run", "jvm_tuned", "idle", "cpu_percent")), percent(value))
    known = [r for r in measured if dig(r, "run", "cn1_native", "idle", "cpu_percent") is not None]
    sentence = "Of the %d native builds, %d idle at or under %g%% of a core." % (
        len(known), len(known) - len(busy), IDLE_CPU_LIMIT)
    if busy:
        sentence += " Above it: " + ", ".join(busy) + "."
    cpu.say(sentence)
    tables.append(cpu)

    layers = Table("linux-layers", "How much of the compatibility runtime an application keeps",
                   ["Application", "Layer", "Classes handed to the builder", "Classes translated", "Kept"],
                   ["<3", "<2", ">1", ">1", ">1"])
    for result in results:
        staged = dig(result, "layers", "staged_jar", "classes") or {}
        translated = None
        for key in sorted(dig(result, "layers") or {}):
            if key.startswith("translated_"):
                translated = dig(result, "layers", key)
        for layer in ("swing-compat", "javafx-compat", "compat-jdk"):
            if layer not in staged:
                continue
            kept = dig(translated, layer, "classes")
            total = staged[layer]["classes"]
            layers.add(LABEL[name_of(result)], "`%s`" % layer, total, EMPTY if kept is None else kept,
                       EMPTY if kept is None or not total else "%.0f%%" % (100.0 * kept / total))
    if layers.rows:
        tables.append(layers)
    return tables


def macos_tables(results):
    table = Table("macos-size", "macOS: the distributed application (MB)",
                  ["Application", "`jlink` image", "`jlink` image, zipped", "Native `.app`", "arm64 slice",
                   "x86_64 slice", "Native `.app`, zipped", "JVM image / native `.app`", "JVM zip / native zip"],
                  ["<3", ">1", ">1", ">1", ">1", ">1", ">1", ">1", ">1"])
    pairs = []
    installed = []
    for result in results:
        label = LABEL[name_of(result)]
        jlink = dig(result, "size", "baseline_jlink", "zip_bytes")
        native = dig(result, "size", "cn1_macos", "zip_bytes")
        slices = dig(result, "cn1_macos", "slice_bytes") or {}
        ratio = value_ratio(jlink, native)
        pairs.append((label, ratio, megabytes(jlink) + " MB", megabytes(native) + " MB"))
        jlink_disk = dig(result, "size", "baseline_jlink", "bytes")
        native_disk = dig(result, "size", "cn1_macos", "bytes")
        disk_ratio = value_ratio(jlink_disk, native_disk)
        installed.append((label, disk_ratio, megabytes(jlink_disk) + " MB", megabytes(native_disk) + " MB"))
        table.add(label, megabytes(jlink_disk), megabytes(jlink), megabytes(native_disk),
                  megabytes(slices.get("arm64")), megabytes(slices.get("x86_64")), megabytes(native),
                  show_ratio(disk_ratio), show_ratio(ratio))
    table.show(chart("macos-size", "macOS: the application as installed",
                     "Bar chart of the installed size of each application on macOS: the universal native "
                     "application beside the jlink image", "MB", [LABEL[name_of(r)] for r in results], [
                         (("Native .app, universal", NATIVE_SERIES[1]),
                          [megabytes_of(dig(r, "size", "cn1_macos", "bytes")) for r in results]),
                         (("arm64 slice of the native .app", TUNED_SERIES[1]),
                          [megabytes_of((dig(r, "cn1_macos", "slice_bytes") or {}).get("arm64"))
                           for r in results]),
                         (("JVM image, one architecture", DEFAULT_SERIES[1]),
                          [megabytes_of(dig(r, "size", "baseline_jlink", "bytes")) for r in results])],
                     where_measured(results)))
    table.say("The native `.app` is universal: it holds an arm64 and an x86_64 slice and runs on both. "
              "The `jlink` image holds the runtime of one architecture, so a JVM application that runs "
              "on both ships two of them.")
    table.say(tally(installed, "smaller", "larger", "the `jlink` image as installed"))
    table.say(tally(pairs, "smaller", "larger", "the zipped `jlink` image"))
    return [table] if results else []


def mobile_tables(sizes):
    if not sizes:
        return []
    table = Table("mobile-size", "iOS and Android: the distributed application (MB)",
                  ["Application", "iOS `.app`", "iOS `.app`, zipped", "iOS executable, stripped", "Android APK",
                   "APK, unpacked", "Of which dex"])
    for name in sorted(sizes, key=lambda n: ORDER[n]):
        ios = sizes[name].get("ios") or {}
        android = sizes[name].get("android") or {}
        table.add(LABEL[name], megabytes(ios.get("bytes")), megabytes(ios.get("zip_bytes")),
                  megabytes(ios.get("executable_stripped_bytes")), megabytes(android.get("apk_bytes")),
                  megabytes(android.get("unpacked_bytes")), megabytes(android.get("dex_bytes")))
    return [table]


def usable(value):
    """Whether an environment fact reads as a fact: a tool that printed a
    warning or an error where its version should be is left out."""
    text = str(value).lower()
    return value not in (None, "") and "warning" not in text and "error" not in text


def environment(linux, macos):
    lines = [".Where the figures were measured"]
    if linux:
        facts = dig(linux[0], "environment") or {}
        parts = []
        if usable(facts.get("os")):
            parts.append(str(facts["os"]))
        if usable(facts.get("machine")):
            parts.append(str(facts["machine"]))
        if facts.get("cpus"):
            parts.append("%s CPUs" % facts["cpus"])
        if facts.get("ram_kb"):
            parts.append("%.1f GB of memory" % (facts["ram_kb"] / 1048576.0))
        where = "in a container" if facts.get("in_container") else "on the machine itself"
        line = "* *Linux*: " + ", ".join(parts) + ", " + where
        if usable(facts.get("host_cpu")):
            line += ", hosted on " + str(facts["host_cpu"]).split("(")[0].strip()
        line += "."
        if usable(facts.get("screen")):
            line += " The display is a virtual framebuffer, Xvfb, of %s pixels." % str(facts["screen"]).replace("x", " by ")
        if usable(facts.get("gtk")):
            line += " GTK %s." % facts["gtk"]
        if usable(facts.get("openjfx")):
            line += " OpenJFX %s for the JavaFX baselines." % facts["openjfx"]
        lines.append(line)
        flags = sorted(set(dig(r, "tuned", "flags") for r in linux if dig(r, "tuned", "flags")))
        if flags:
            common = [f for f in flags[0].split() if not f.startswith("-Xmx")]
            lines.append("* *Tuned JVM flags*: `%s`, and the `-Xmx` of the table above." % " ".join(common))
    if macos:
        facts = dig(macos[0], "environment") or {}
        parts = [str(facts[k]) for k in ("os", "cpu") if usable(facts.get(k))]
        if usable(facts.get("xcode")):
            parts.append(str(facts["xcode"]).split(",")[0])
        if parts:
            lines.append("* *macOS, iOS and Android sizes*: " + ", ".join(parts) + ".")
    lines.append("")
    return "\n".join(lines) if len(lines) > 2 else ""


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--linux")
    parser.add_argument("--macos")
    parser.add_argument("--mobile")
    parser.add_argument("--out", default=os.path.join(HERE, "desktop-compat-benchmarks.adoc"))
    args = parser.parse_args()
    if not (args.linux or args.macos or args.mobile):
        parser.error("give at least one of --linux, --macos and --mobile")

    linux = load_results(args.linux)
    macos = load_results(args.macos)
    mobile = load_mobile(args.mobile)

    out = ["// Written by desktop-compat-benchmarks.py from measured results. Change the results and run it again;",
           "// an edit made here is lost. Desktop-Interop.asciidoc includes this file once, whole.", ""]
    tables = dict((t.tag, t) for t in linux_tables(linux) + macos_tables(macos) + mobile_tables(mobile))
    for tag in ORDER_OF_TABLES:
        if tag in tables:
            out.append(tables[tag].render())
    out.append(environment(linux, macos))
    draw_charts(os.path.join(os.path.dirname(os.path.abspath(args.out)), CHART_DIRECTORY))
    with open(args.out, "w") as handle:
        handle.write("\n".join(out).rstrip("\n") + "\n")
    sys.stderr.write("wrote %s\n" % args.out)


if __name__ == "__main__":
    main()
