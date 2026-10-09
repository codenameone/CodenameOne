#!/usr/bin/env python3
"""Render desktop-compat benchmark results as tables.

  report.py [--format markdown|asciidoc] [--out FILE] RESULT.json [RESULT.json ...]

Each RESULT.json is what bench.sh wrote for one application. A cell with no
measurement behind it is printed as "--": nothing here is estimated or carried
over from another run. Ratios are baseline / Codename One, so a number above 1
means the Codename One build is that many times smaller or faster, and a number
below 1 means it lost.

Python 3.9 compatible, standard library only.
"""

import argparse
import json
import sys

EMPTY = "--"
IDLE_CPU_DEFECT_PERCENT = 1.0


def dig(node, *path):
    for key in path:
        if not isinstance(node, dict) or key not in node:
            return None
        node = node[key]
    return node


def megabytes(value):
    return EMPTY if value is None else "%.1f" % (value / 1048576.0)


def megabytes_kb(value):
    return EMPTY if value is None else "%.1f" % (value / 1024.0)


def ratio(baseline, candidate):
    if baseline is None or candidate is None or candidate == 0:
        return EMPTY
    return "%.1fx" % (float(baseline) / float(candidate))


def timing(node):
    if not node or node.get("median") is None:
        return EMPTY
    return "%.0f (%.0f-%.0f)" % (node["median"], node["min"], node["max"])


def percent(value):
    return EMPTY if value is None else "%.1f%%" % value


class Table(object):
    def __init__(self, title, header, note=None):
        self.title = title
        self.header = header
        self.rows = []
        self.note = note

    def add(self, *cells):
        self.rows.append([str(cell) for cell in cells])

    def markdown(self):
        lines = ["### " + self.title, ""]
        if self.note:
            lines += [self.note, ""]
        lines.append("| " + " | ".join(self.header) + " |")
        lines.append("|" + "|".join(["---"] + ["---:"] * (len(self.header) - 1)) + "|")
        for row in self.rows:
            lines.append("| " + " | ".join(row) + " |")
        lines.append("")
        return "\n".join(lines)

    def asciidoc(self):
        lines = ["." + self.title]
        lines.append('[cols="' + ",".join(["<2"] + [">1"] * (len(self.header) - 1)) + '",options="header"]')
        lines.append("|===")
        lines.append("".join("|" + cell + " " for cell in self.header).rstrip())
        for row in self.rows:
            lines.append("")
            lines.append("".join("|" + cell + " " for cell in row).rstrip())
        lines.append("|===")
        if self.note:
            lines += ["", self.note]
        lines.append("")
        return "\n".join(lines)


def build_tables(results):
    tables = []

    size = Table(
        "Distributable size (MB; zip = one deflate -9 archive)",
        ["App", "Jar + deps", "jlink image + app", "jlink zip", "jpackage image", "jpackage zip",
         "CN1 native", "CN1 zip", "jlink zip / CN1 zip"],
        "The jlink image holds only the modules the application needs "
        "(`jdeps --print-module-deps`), is stripped of debug information and compressed "
        "(`--compress zip-6`), and has no CDS archive in it.")
    for result in results:
        name = dig(result, "app", "name")
        target = dig(result, "app", "target") or "linux"
        cn1 = dig(result, "size", "cn1_" + target)
        slices = dig(result, "cn1_" + target, "slice_bytes")
        if slices:
            name = "%s (CN1 bundle is universal: %s)" % (name, ", ".join(
                "%s %.1f MB" % (arch, slices[arch] / 1048576.0) for arch in sorted(slices)))
        size.add(name,
                 megabytes(dig(result, "size", "baseline_jars", "bytes")),
                 megabytes(dig(result, "size", "baseline_jlink", "bytes")),
                 megabytes(dig(result, "size", "baseline_jlink", "zip_bytes")),
                 megabytes(dig(result, "size", "baseline_jpackage", "bytes")),
                 megabytes(dig(result, "size", "baseline_jpackage", "zip_bytes")),
                 megabytes(dig(cn1, "bytes")),
                 megabytes(dig(cn1, "zip_bytes")),
                 ratio(dig(result, "size", "baseline_jlink", "zip_bytes"), dig(cn1, "zip_bytes")))
    tables.append(size)

    measured = [r for r in results if dig(r, "run")]
    if measured:
        for section, title in (("idle", "Memory after start-up and %s s idle (MB)"),
                               ("after_script", "Memory after the scripted interaction (MB)")):
            header = ["App"]
            for variant in ("JVM default", "JVM tuned", "CN1 native"):
                header += [variant + " RSS", "PSS", "USS"]
            header += ["RSS default / CN1", "RSS tuned / CN1"]
            idle_seconds = dig(measured[0], "run", "jvm_default", "idle_seconds") or \
                dig(measured[0], "run", "cn1_native", "idle_seconds") or 10
            table = Table(title % ("%g" % idle_seconds) if "%s" in title else title, header,
                          "Summed over the application's whole process tree from "
                          "`/proc/<pid>/smaps_rollup`; median of the sessions.")
            for result in measured:
                row = [dig(result, "app", "name")]
                for variant in ("jvm_default", "jvm_tuned", "cn1_native"):
                    for field in ("rss_kb", "pss_kb", "uss_kb"):
                        row.append(megabytes_kb(dig(result, "run", variant, section, field)))
                cn1 = dig(result, "run", "cn1_native", section, "rss_kb")
                row.append(ratio(dig(result, "run", "jvm_default", section, "rss_kb"), cn1))
                row.append(ratio(dig(result, "run", "jvm_tuned", section, "rss_kb"), cn1))
                table.add(*row)
            tables.append(table)

        peak = Table("Peak RSS over a whole session (MB, `/usr/bin/time -v`)",
                     ["App", "JVM default", "JVM tuned", "CN1 native", "default / CN1", "tuned / CN1",
                      "Tuned -Xmx (MB)"])
        for result in measured:
            cn1 = dig(result, "run", "cn1_native", "peak_rss_kb")
            peak.add(dig(result, "app", "name"),
                     megabytes_kb(dig(result, "run", "jvm_default", "peak_rss_kb")),
                     megabytes_kb(dig(result, "run", "jvm_tuned", "peak_rss_kb")),
                     megabytes_kb(cn1),
                     ratio(dig(result, "run", "jvm_default", "peak_rss_kb"), cn1),
                     ratio(dig(result, "run", "jvm_tuned", "peak_rss_kb"), cn1),
                     dig(result, "tuned", "xmx_mb") or EMPTY)
        tables.append(peak)

        for mode in ("cold", "warm"):
            startup = Table(
                "Start-up, %s: process start to first painted frame (ms, median (min-max))" % mode,
                ["App", "JVM default", "JVM tuned", "CN1 native", "default / CN1", "tuned / CN1"],
                "Linux: read off the X server, the first poll at which the application's window "
                "shows more than a flat colour. macOS and Windows: the window appearing, which is "
                "earlier than its first frame.")
            for result in measured:
                key = "startup_" + mode
                # Linux reports the first painted frame. macOS and Windows can
                # only see the window appear, and report that instead.
                field = "first_paint_ms"
                if dig(result, "run", "cn1_native", key, field) is None and \
                        dig(result, "run", "jvm_default", key, field) is None:
                    field = "window_ms"
                cn1 = dig(result, "run", "cn1_native", key, field)
                default = dig(result, "run", "jvm_default", key, field)
                tuned = dig(result, "run", "jvm_tuned", key, field)
                startup.add(dig(result, "app", "name"), timing(default), timing(tuned), timing(cn1),
                            ratio(dig(default, "median"), dig(cn1, "median")),
                            ratio(dig(tuned, "median"), dig(cn1, "median")))
            # macOS and Windows take no cold figures; a table of dashes says nothing.
            if any(cell != EMPTY for row in startup.rows for cell in row[1:]):
                tables.append(startup)

        cpu = Table("Idle CPU (% of one core, second half of the idle window)",
                    ["App", "JVM default", "JVM tuned", "CN1 native", "CN1 visible changes", "Verdict"])
        for result in measured:
            value = dig(result, "run", "cn1_native", "idle", "cpu_percent")
            verdict = EMPTY
            if value is not None:
                verdict = "DEFECT: repaints while idle" if value > IDLE_CPU_DEFECT_PERCENT else "ok"
            cpu.add(dig(result, "app", "name"),
                    percent(dig(result, "run", "jvm_default", "idle", "cpu_percent")),
                    percent(dig(result, "run", "jvm_tuned", "idle", "cpu_percent")),
                    percent(value),
                    dig(result, "run", "cn1_native", "idle", "visible_changes")
                    if value is not None else EMPTY,
                    verdict)
        tables.append(cpu)

    layers = Table(
        "What the compatibility runtime costs, and what dead-code elimination removed",
        ["App", "Layer", "Classes in staged jar", "Class bytes (MB)", "Classes translated to C", "Kept"],
        "Staged jar: what the application hands to any builder. Translated: the classes "
        "ParparVM still emitted C for after its dead-code pass (the application row also "
        "counts the classes the translator makes out of lambdas).")
    for result in results:
        staged = dig(result, "layers", "staged_jar", "classes") or {}
        translated = None
        for key in sorted(dig(result, "layers") or {}):
            if key.startswith("translated_"):
                translated = dig(result, "layers", key)
        for layer in ("swing-compat", "javafx-compat", "compat-jdk", "application", "codenameone-core"):
            if layer not in staged:
                continue
            kept = dig(translated, layer, "classes")
            share = EMPTY
            # Only for the runtime layers: the translator adds classes of its
            # own to the application's (one per lambda), so a share there would
            # compare two different sets.
            if kept is not None and staged[layer]["classes"] and layer != "application":
                share = "%.0f%%" % (100.0 * kept / staged[layer]["classes"])
            layers.add(dig(result, "app", "name"), layer, staged[layer]["classes"],
                       megabytes(staged[layer]["bytes"]), EMPTY if kept is None else kept, share)
    if layers.rows:
        tables.append(layers)
    return tables


def failures(results):
    lines = []
    for result in results:
        name = dig(result, "app", "name")
        for part in ("baseline", "cn1"):
            node = dig(result, part)
            if isinstance(node, dict) and node.get("ok") is False:
                lines.append("%s: %s build failed (%s): %s" % (
                    name, part, node.get("classification"), node.get("first_error") or "see log"))
        for variant in sorted(dig(result, "run") or {}):
            for error in dig(result, "run", variant, "errors") or []:
                lines.append("%s: %s: %s" % (name, variant, error))
    return lines


def environment_lines(results):
    facts = dig(results[0], "environment") or {}
    order = ["commit", "os", "kernel", "machine", "cpu", "host_cpu", "cpus", "ram_kb", "in_container",
             "container_image", "screen", "baseline_jdk", "openjfx", "cn1_build_jdk", "cn1_c_compiler",
             "cn1_c_compiler_version", "gtk", "xcode"]
    lines = []
    for key in order:
        value = facts.get(key)
        if value is None or value == "":
            continue
        if key == "ram_kb":
            value = "%.1f GiB" % (value / 1048576.0)
        lines.append((key.replace("_kb", "").replace("_", " "), value))
    tuned = sorted(set(dig(r, "tuned", "flags") for r in results if dig(r, "tuned", "flags")))
    for flags in tuned:
        lines.append(("tuned JVM flags", flags))
    cold = sorted(set(dig(r, "run", v, "cold_method") or "" for r in results
                      for v in ("jvm_default", "jvm_tuned", "cn1_native")) - {""})
    if cold:
        lines.append(("cold start made cold by", ", ".join(cold)))
    return lines


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--format", choices=("markdown", "asciidoc"), default="markdown")
    parser.add_argument("--out", default=None)
    parser.add_argument("results", nargs="+")
    args = parser.parse_args()

    results = []
    for path in args.results:
        with open(path) as handle:
            results.append(json.load(handle))
    results.sort(key=lambda r: (dig(r, "app", "kind") or "", dig(r, "app", "name") or ""))

    markdown = args.format == "markdown"
    out = []
    out.append("## Desktop compatibility benchmarks" if markdown else "== Desktop compatibility benchmarks")
    out.append("")
    for table in build_tables(results):
        out.append(table.markdown() if markdown else table.asciidoc())
    problems = failures(results)
    if problems:
        out.append("### Failures" if markdown else "=== Failures")
        out.append("")
        out.extend(("- " if markdown else "* ") + line for line in problems)
        out.append("")
    out.append("### Environment" if markdown else "=== Environment")
    out.append("")
    for key, value in environment_lines(results):
        out.append(("- **%s**: %s" if markdown else "* *%s*: %s") % (key, value))
    out.append("")
    text = "\n".join(out)
    if args.out:
        with open(args.out, "w") as handle:
            handle.write(text)
    else:
        sys.stdout.write(text)


if __name__ == "__main__":
    main()
