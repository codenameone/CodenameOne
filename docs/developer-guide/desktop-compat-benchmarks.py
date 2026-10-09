#!/usr/bin/env python3
"""Render the measured tables of the Swing and JavaFX chapter.

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
are published by running this again and committing the one file it writes.

Nothing is estimated. A cell with no measurement behind it is printed as "--",
and the cells that were attempted and failed are listed with the harness's
reason. A ratio is always the JVM figure divided by the native one: above 1 the
native build is smaller or quicker, below 1 it is larger or slower, and both
are counted the same way.

An application is published under the description in LABELS, never under the
directory name the harness gave it. A result with no entry there stops the run:
add the line, with a description that names no third party.

Python 3.9 compatible, standard library only. The arithmetic and the cell
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


class Table(object):
    def __init__(self, tag, title, header, widths=None):
        self.tag = tag
        self.title = title
        self.header = header
        self.widths = widths
        self.rows = []
        self.after = []

    def add(self, *cells):
        self.rows.append([str(cell) for cell in cells])

    def say(self, paragraph):
        if paragraph:
            self.after.append(paragraph)

    def render(self):
        widths = self.widths or ["<3"] + [">2"] * (len(self.header) - 1)
        lines = ["." + self.title, '[cols="%s",options="header"]' % ",".join(widths), "|==="]
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
        parts.append("within 5% of it in %s" % count(level, "application"))
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


def linux_tables(results):
    tables = []
    missing = unmeasured(results)

    size = Table("linux-size", "Linux: the distributed application (MB)",
                 ["Application", "Toolkit", "`jlink` image", "`jlink` image, zipped", "`jpackage` image, zipped",
                  "Native build", "Native build, zipped", "JVM zip / native zip"],
                 ["<3", "<1", ">1", ">1", ">1", ">1", ">1", ">1"])
    pairs = []
    for result in results:
        name = name_of(result)
        jlink = dig(result, "size", "baseline_jlink", "zip_bytes")
        native = dig(result, "size", "cn1_linux", "zip_bytes")
        ratio = value_ratio(jlink, native)
        pairs.append((LABEL[name], ratio, megabytes(jlink) + " MB", megabytes(native) + " MB"))
        size.add(LABEL[name], TOOLKIT[name],
                 megabytes(dig(result, "size", "baseline_jlink", "bytes")), megabytes(jlink),
                 megabytes(dig(result, "size", "baseline_jpackage", "zip_bytes")),
                 megabytes(dig(result, "size", "cn1_linux", "bytes")), megabytes(native), show_ratio(ratio))
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
                  ["Application", "`jlink` image, zipped", "Native `.app`", "arm64 slice", "x86_64 slice",
                   "Native `.app`, zipped", "JVM zip / native zip"])
    pairs = []
    for result in results:
        label = LABEL[name_of(result)]
        jlink = dig(result, "size", "baseline_jlink", "zip_bytes")
        native = dig(result, "size", "cn1_macos", "zip_bytes")
        slices = dig(result, "cn1_macos", "slice_bytes") or {}
        ratio = value_ratio(jlink, native)
        pairs.append((label, ratio, megabytes(jlink) + " MB", megabytes(native) + " MB"))
        table.add(label, megabytes(jlink), megabytes(dig(result, "size", "cn1_macos", "bytes")),
                  megabytes(slices.get("arm64")), megabytes(slices.get("x86_64")), megabytes(native),
                  show_ratio(ratio))
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
    with open(args.out, "w") as handle:
        handle.write("\n".join(out).rstrip("\n") + "\n")
    sys.stderr.write("wrote %s\n" % args.out)


if __name__ == "__main__":
    main()
