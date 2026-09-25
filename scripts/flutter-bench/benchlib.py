#!/usr/bin/env python3
"""Shared machinery for the Flutter-vs-Codename One benchmark.

The benchmark measures ONE application built two ways: Flutter's own AOT
compile of the gallery, and the identical Dart source transpiled to Java and
run on Codename One. Same source, same screens, same device, so a difference
in size, in start-up time or in memory is a difference between the two runtimes
and nothing else.

This module holds everything that is not platform specific: the metric list,
the sizing helpers, the interleaving and best-of-N statistics, the rendering of
the report, and the regression gate. Platform specific work -- how an
application is launched, and how its memory is read -- lives behind the adapter
interface in `platforms.py`.

Rules the numbers have to obey, because it is easy to produce flattering ones:

  * Release/AOT on both sides. A debug build of either is meaningless.
  * The SAME SURFACE SIZE. Window or screen area drives the size of the GPU
    surfaces that dominate a UI application's resident memory, so two builds
    measured at different sizes cannot be compared on memory at all.
  * INTERLEAVED runs, best-of-N, and the machine's load average recorded. A
    ratio taken under different load twice is not a ratio -- a walkthrough
    recording on this project desynchronised twice and looked like a timing
    defect, and the cause was a stray simulator holding the machine at load 8.
  * The start-up clock runs OUTSIDE both processes: each application prints one
    marker on its first painted frame and the harness times from launch to that
    line, so neither runtime is trusted to time itself.
  * Every metric is reported, including the ones Codename One loses.
"""

import json
import os
import re
import subprocess
import time
import zipfile

# Lower is better for every metric here; that is what makes "wins" well
# defined and lets the regression gate use a single comparison.

# Every platform the benchmark measures, in the order the report lists them.
# The workflow's plan job carries its own copy (it runs before any checkout);
# test_benchlib.PlatformIdsMatchTheWorkflow holds the two together.
PLATFORM_IDS = ("macos", "ios", "android", "linux", "windows", "javascript")

METRICS = [
    ("install_bytes", "Installed size", "bytes"),
    ("code_bytes", "Executable code", "bytes"),
    ("wire_bytes", "Download size (zipped)", "bytes"),
    ("cold_start_ms", "Cold start to first frame on screen", "ms"),
    ("idle_memory_bytes", "Memory at rest", "bytes"),
]

# Start-up is reported as a BRACKET, not a point, because the two runtimes do
# not expose the same event. Flutter's `cold_start_ms` is RASTERDONE, after its
# first frame is rasterised, and `cold_start_lower_ms` is FIRSTCONTENT, a
# UI-thread callback before it; Codename One has one marker. The RATIO is taken
# from Flutter's LOWER end -- the one least favourable to Codename One -- because
# the bracket exists precisely because the events may not line up, and a ratio
# of Flutter's time over ours only grows, and flatters us, as Flutter's figure
# grows. Both ends are carried so a reader sees the width of the uncertainty.
# The gate is Codename One's alone, so it is unaffected.
BRACKET_METRIC = "cold_start_ms"
BRACKET_LOWER = "cold_start_lower_ms"

METRIC_UNITS = dict((key, unit) for key, _label, unit in METRICS)
METRIC_LABELS = dict((key, label) for key, label, _unit in METRICS)

SIDES = ("codenameone", "flutter")

# The band a freshly recorded baseline gets, per metric. Size is deterministic
# on a runner, so it gets a tight band; wall-clock start-up on a shared runner
# is not, and a band tight enough to catch a real regression there would fire on
# load alone; memory at rest sits between the two. Written INTO each baseline
# file, so a platform that needs a different band says so in the file itself.
# Sizes get NO tolerance: they are deterministic for a given source tree and
# toolchain, so any growth is a real change and fails the gate. Re-baseline
# deliberately (commit the run's candidate) when growth is intended.
DEFAULT_TOLERANCES = {
    "install_bytes": 0.0,
    "code_bytes": 0.0,
    "wire_bytes": 0.0,
    "cold_start_ms": 0.25,
    "idle_memory_bytes": 0.15,
}


# ----------------------------------------------------------------------
# Sizing. Portable: every platform ships either a directory tree or a
# single archive, and both reduce to "how many bytes does the user get".
# ----------------------------------------------------------------------

def tree_size(path):
    """Apparent size of `path`: what the artifact actually occupies.

    Deliberately not `du`: that reports ALLOCATED blocks, which rounds every
    file up to the filesystem's block size and counts hard-linked framework
    copies once. An application's size as the user experiences it is the sum
    of its file lengths, and that is what both stores quote.
    """
    if os.path.isfile(path):
        return os.path.getsize(path)
    total = 0
    for root, _dirs, files in os.walk(path):
        for name in files:
            full = os.path.join(root, name)
            if os.path.islink(full):
                continue
            try:
                total += os.path.getsize(full)
            except OSError:
                # Deliberately ignored. A build tree is live while this walks
                # it -- a compiler or packaging step can remove or replace a
                # file between the listing and the stat -- and a file that has
                # gone is a file the artifact does not ship. Failing here would
                # turn a harmless race into a lost measurement.
                continue
    return total


def wire_size(path, workdir):
    """What the artifact compresses to -- the number a download actually costs.

    Stores compress before shipping, so an uncompressed comparison flatters
    whichever side ships more compressible bytes. Deflate at a fixed level so
    the number is reproducible across runners rather than dependent on
    whichever zip binary is installed.
    """
    out = os.path.join(workdir, os.path.basename(str(path).rstrip("/")) + ".benchzip")
    if os.path.exists(out):
        os.remove(out)
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        if os.path.isfile(path):
            archive.write(path, os.path.basename(path))
        else:
            for root, _dirs, files in os.walk(path):
                for name in files:
                    full = os.path.join(root, name)
                    if os.path.islink(full):
                        continue
                    archive.write(full, os.path.relpath(full, path))
    size = os.path.getsize(out)
    os.remove(out)
    return size


# Mach-O magic numbers, both endiannesses plus the fat/universal wrapper.
_MACHO_MAGIC = (
    b"\xfe\xed\xfa\xce", b"\xce\xfa\xed\xfe",   # 32 bit
    b"\xfe\xed\xfa\xcf", b"\xcf\xfa\xed\xfe",   # 64 bit
    b"\xca\xfe\xba\xbe", b"\xbe\xba\xfe\xca",   # universal
)


def macho_code_size(bundle):
    """Every Mach-O binary in an Apple bundle, summed.

    NOT just the main executable, and the difference is not small. On iOS a
    Flutter application's own code is not in the executable at all: `Runner` is
    a thin launcher of about a tenth of a megabyte, and the Dart AOT image and
    the engine live in Frameworks/App.framework and Frameworks/Flutter.framework.
    Sizing only the executable therefore compared our entire runtime against
    Flutter's stub and reported a 700x loss that did not exist.

    Summing every Mach-O in the bundle needs no per-side special casing: it
    asks "how many bytes of compiled code does this application ship", which
    is the same question on both sides however each chooses to lay them out.
    """
    return _sum_by_magic(bundle, lambda head: head[:4] in _MACHO_MAGIC)


def elf_code_size(tree):
    """Machine code in every ELF under `tree`: the bytes of its EXECUTABLE sections.

    Not whole files, for two reasons that pull the same way. Flutter's bundle
    puts the Dart AOT image in lib/libapp.so beside the engine library, so the
    `gallery` launcher alone would repeat the iOS mistake; every ELF is walked.
    And Codename One's native Linux port links the application's resources INTO
    the executable (an .incbin'd blob in .rodata), so whole-file bytes counted
    the gallery's artwork as Codename One code -- 114 MB against Flutter's
    51 MB, a comparison of pictures. Executable sections (SHF_EXECINSTR) are
    compiled code on both sides and nothing else; read-only data is excluded
    symmetrically, Flutter's Dart snapshot data included.
    """
    return _sum_by_magic(tree, lambda head: head[:4] == b"\x7fELF", _elf_exec_bytes)


def pe_code_size(tree):
    """Machine code in every PE image under `tree`; see elf_code_size.

    The same argument: Flutter's code is flutter_windows.dll plus the Dart image
    beside gallery.exe, and Codename One's Windows build embeds its resources in
    the executable. Sections marked IMAGE_SCN_MEM_EXECUTE are counted.
    """
    return _sum_by_magic(tree, lambda head: head[:2] == b"MZ", _pe_exec_bytes)


def _elf_exec_bytes(data):
    """Total size of SHF_EXECINSTR sections that occupy file bytes, or None."""
    import struct
    if len(data) < 64 or data[:4] != b"\x7fELF":
        return None
    wide = data[4] == 2
    order = "<" if data[5] == 1 else ">"
    try:
        if wide:
            shoff, = struct.unpack_from(order + "Q", data, 0x28)
            shentsize, shnum = struct.unpack_from(order + "HH", data, 0x3A)
        else:
            shoff, = struct.unpack_from(order + "I", data, 0x20)
            shentsize, shnum = struct.unpack_from(order + "HH", data, 0x2E)
        total = 0
        for index in range(shnum):
            base = shoff + index * shentsize
            if wide:
                sh_type, sh_flags = struct.unpack_from(order + "IQ", data, base + 4)
                sh_size, = struct.unpack_from(order + "Q", data, base + 0x20)
            else:
                sh_type, sh_flags = struct.unpack_from(order + "II", data, base + 4)
                sh_size, = struct.unpack_from(order + "I", data, base + 0x14)
            if sh_flags & 0x4 and sh_type != 8:  # SHF_EXECINSTR, not SHT_NOBITS
                total += sh_size
        return total
    except struct.error:
        return None


def _pe_exec_bytes(data):
    """Total raw size of IMAGE_SCN_MEM_EXECUTE sections, or None."""
    import struct
    try:
        pe, = struct.unpack_from("<I", data, 0x3C)
        if data[pe:pe + 4] != b"PE\0\0":
            return None
        sections, = struct.unpack_from("<H", data, pe + 6)
        optional, = struct.unpack_from("<H", data, pe + 20)
        table = pe + 24 + optional
        total = 0
        for index in range(sections):
            base = table + index * 40
            raw, = struct.unpack_from("<I", data, base + 16)
            flags, = struct.unpack_from("<I", data, base + 36)
            if flags & 0x20000000:
                total += raw
        return total
    except struct.error:
        return None


def _sum_by_magic(tree, matches, measure=None):
    """Bytes of every regular file under `tree` whose header `matches`, or None.

    Identified by magic bytes rather than extension, so an unconventionally
    named library is still counted and a data file named like one is not.
    `measure`, when given, reads the file and answers how many of its bytes
    count; None from it means the file could not be parsed, and it is skipped
    rather than counted whole.
    """
    if os.path.isfile(tree):
        paths = [tree]
    else:
        paths = []
        for root, _dirs, files in os.walk(tree):
            paths.extend(os.path.join(root, name) for name in files)
    total = 0
    found = False
    for full in paths:
        if os.path.islink(full):
            continue
        try:
            with open(full, "rb") as handle:
                if not matches(handle.read(4)):
                    continue
                if measure is None:
                    total += os.path.getsize(full)
                    found = True
                    continue
                handle.seek(0)
                counted = measure(handle.read())
        except OSError:
            continue
        if counted is not None:
            total += counted
            found = True
    return total if found else None


# ----------------------------------------------------------------------
# Statistics
# ----------------------------------------------------------------------

def best_of(values):
    """The fastest/smallest run: the one least disturbed by the machine.

    Not the mean. A shared CI runner produces a long right tail from other
    jobs, and averaging it in measures the runner rather than the runtime.
    The minimum is the closest thing to "what this build can do".
    """
    return min(values) if values else None


def percentile(values, pct):
    """The `pct` percentile by nearest rank, with no interpolation.

    Frame times are a sample of real frames, not a continuous distribution,
    so an interpolated p95 invents a frame that never rendered.
    """
    if not values:
        return None
    ordered = sorted(values)
    rank = max(1, int(round(pct / 100.0 * len(ordered))))
    return ordered[min(rank, len(ordered)) - 1]


def load_average():
    """The 1/5/15 minute load, or None where the platform has no such notion.

    Recorded with every report because a ratio measured under different load
    twice is not a ratio, and because a surprising result is usually the
    machine rather than the code.
    """
    try:
        one, five, fifteen = os.getloadavg()
        return [round(one, 2), round(five, 2), round(fifteen, 2)]
    except (OSError, AttributeError):
        return None


# ----------------------------------------------------------------------
# Report assembly
# ----------------------------------------------------------------------

def summarise(side):
    """Collapses a side's per-run samples into the reported figure."""
    out = dict(side)
    out["cold_start_ms"] = best_of(side.get("cold_start_runs") or [])
    out["cold_start_lower_ms"] = best_of(side.get("cold_start_lower_runs") or [])
    out["idle_memory_bytes"] = best_of(side.get("idle_memory_runs") or [])
    return out


# ----------------------------------------------------------------------
# Compute: the VM workloads, run inside each app
# ----------------------------------------------------------------------

# vm/benchmarks' CommonWorkloads, in its own order. The Java source runs in the
# Codename One app and vm/benchmarks/dart/common_workloads.dart -- a port that
# reproduces Java's 32-bit wrapping, unsigned shift and String.hashCode -- in the
# Flutter one, so the two do the same work and must produce the same checksum.
COMPUTE_WORKLOADS = (
    "intArithmetic", "longArithmetic", "mathTranscendental", "arraySequential",
    "arrayRandom", "objectAllocation", "valueEscape", "hashMapChurn",
    "stringBuilding", "recursion", "quicksortBench",
)

COMPUTE_METRIC = "compute_geomean"
COMPUTE_LABEL = "Compute (geomean of the VM workloads)"

_COMPUTE_LINE = re.compile(
    r"BENCH:COMPUTE name=(\w+) checksum=(-?\d+) ms=(\d+)")
COMPUTE_DONE = "BENCH:COMPUTE-DONE"


def parse_compute_line(line):
    """(name, checksum, ms) from one app's result line, or None."""
    match = _COMPUTE_LINE.search(line)
    if not match:
        return None
    return match.group(1), match.group(2), int(match.group(3))


def compute_verdict(ours, theirs):
    """Per-workload ratios and their geometric mean.

    `ours` and `theirs` map a workload to (checksum, ms). The ratio is
    flutter_ms / codenameone_ms, so above 1.00x Codename One is faster.

    A workload is only compared when both checksums are identical: a different
    checksum means the two sides did not do the same computation -- on the web,
    for one, JavaScript numbers cannot hold a 64-bit integer -- and a ratio
    between two different computations is not a measurement. It is reported as
    such and left out of the mean rather than dropped silently.
    """
    rows = []
    ratios = []
    for name in COMPUTE_WORKLOADS:
        a = (ours or {}).get(name)
        b = (theirs or {}).get(name)
        if a is None or b is None:
            rows.append({"name": name, "status": "not measured"})
            continue
        if a[0] != b[0]:
            rows.append({"name": name, "status": "checksum mismatch",
                         "codenameone": a[1], "flutter": b[1],
                         "checksums": [a[0], b[0]]})
            continue
        if a[1] <= 0 or b[1] <= 0:
            rows.append({"name": name, "status": "too fast to time",
                         "codenameone": a[1], "flutter": b[1]})
            continue
        ratio = float(b[1]) / float(a[1])
        ratios.append(ratio)
        rows.append({"name": name, "status": "measured", "codenameone": a[1],
                     "flutter": b[1], "ratio": round(ratio, 3)})
    out = {"workloads": rows, "compared": len(ratios)}
    if ratios:
        product = 1.0
        for r in ratios:
            product *= r
        out["geomean"] = round(product ** (1.0 / len(ratios)), 3)
    return out


def render_compute(report):
    """The compute table for one platform, or None when nothing ran."""
    compute = report.get("compute")
    if not compute:
        return None
    intro = ("**Compute** -- the VM workloads (`vm/benchmarks`), run inside each app; "
             + "time is the best of the app's own repetitions, higher ratio means "
             + "Codename One is faster.")
    lines = [intro, ""]
    if compute.get("status") != "measured":
        lines.append("_Not measured: %s._" % compute.get("reason", "no reason given"))
        return "\n".join(lines)
    verdict = compute["verdict"]
    lines.append("| Workload | Codename One | Flutter | Ratio |")
    lines.append("| --- | ---: | ---: | ---: |")
    for row in verdict["workloads"]:
        if row["status"] == "measured":
            lines.append("| %s | %d ms | %d ms | %.2fx |" % (
                row["name"], row["codenameone"], row["flutter"], row["ratio"]))
        elif row["status"] == "not measured":
            lines.append("| %s | -- | -- | not measured |" % row["name"])
        else:
            lines.append("| %s | %s | %s | %s |" % (
                row["name"],
                "%d ms" % row["codenameone"], "%d ms" % row["flutter"], row["status"]))
    if "geomean" in verdict:
        lines.append("| **Geometric mean** | | | **%.2fx** |" % verdict["geomean"])
    return "\n".join(lines)


def verdict(report):
    """Who wins each metric, and by how much.

    `ratio` is flutter/codenameone with lower-is-better throughout, so a ratio
    above 1 means Codename One is ahead by that factor. A metric either side
    failed to produce is reported as not measured rather than as a win: a
    missing number is not a result.
    """
    out = {}
    for key, _label, _unit in METRICS:
        ours = report["codenameone"].get(key)
        theirs = report["flutter"].get(key)
        if key == BRACKET_METRIC:
            lower = report["flutter"].get(BRACKET_LOWER)
            if lower and theirs:
                # The least favourable end: see BRACKET_METRIC. This used the
                # upper end, which the report's own note said it did not.
                theirs = min(theirs, lower)
        if ours is None or theirs is None or not ours or not theirs:
            out[key] = {"status": "not measured"}
            continue
        out[key] = {
            "status": "measured",
            "codenameone": ours,
            "flutter": theirs,
            "ratio": round(float(theirs) / float(ours), 3),
            "winner": "codenameone" if ours < theirs else "flutter",
        }
    return out


def build_report(platform_id, sides, runs, notes=None):
    report = {
        "schema_version": 1,
        "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "platform": platform_id,
        "load_average": load_average(),
        "runs": runs,
        "app": "flutter gallery (new_gallery), same Dart source on both sides",
        "codenameone": summarise(sides["codenameone"]),
        "flutter": summarise(sides["flutter"]),
    }
    if notes:
        report["notes"] = notes
    report["verdict"] = verdict(report)
    return report


def format_value(key, value):
    if value is None:
        return "--"
    unit = METRIC_UNITS.get(key)
    if unit == "bytes":
        return "%.1f MB" % (value / 1024.0 / 1024.0)
    if unit == "ms":
        return "%.0f ms" % value
    return str(value)


def render_markdown(reports, title="Flutter vs Codename One"):
    """The PR comment body: one table per platform, plus a roll-up.

    Written for a reader who will not open the artifact, so every row states
    both absolute numbers rather than only the ratio -- a 2x on a metric
    nobody cares about reads the same as a 2x on one that matters, unless the
    magnitudes are visible.
    """
    lines = ["## %s" % title, ""]
    if not reports:
        lines.append("_No benchmark results were produced._")
        return "\n".join(lines) + "\n"

    lines.append("Same application, built both ways: Flutter's AOT compile of "
                 "the gallery, and the identical Dart source transpiled to Java "
                 "on Codename One. Lower is better for every metric, so a ratio "
                 "above 1.00x means Codename One is ahead by that factor.")
    lines.append("")

    for report in reports:
        lines.append("### %s" % report["platform"])
        lines.append("")
        load = report.get("load_average")
        lines.append("_best of %d interleaved runs%s_" % (
            report.get("runs", 0),
            ("; host load %s" % ", ".join(str(x) for x in load)) if load else ""))
        lines.append("")
        lines.append("| Metric | Codename One | Flutter | Ratio | Winner |")
        lines.append("| --- | ---: | ---: | ---: | --- |")
        for key, label, _unit in METRICS:
            entry = report["verdict"].get(key, {})
            if entry.get("status") != "measured":
                lines.append("| %s | -- | -- | -- | not measured |" % label)
                continue
            flutter_cell = format_value(key, entry["flutter"])
            if key == BRACKET_METRIC:
                lower = report["flutter"].get(BRACKET_LOWER)
                if lower is not None:
                    # Both ends of the bracket, so the reader can see how much
                    # of the gap is measurement uncertainty rather than runtime.
                    flutter_cell = "%s (%s-%s)" % (
                        flutter_cell, format_value(key, lower),
                        format_value(key, report["flutter"].get(BRACKET_METRIC)))
            lines.append("| %s | %s | %s | %.2fx | %s |" % (
                label,
                format_value(key, entry["codenameone"]),
                flutter_cell,
                entry["ratio"],
                "Codename One" if entry["winner"] == "codenameone" else "Flutter"))
        lines.append("")
        if report["verdict"].get(BRACKET_METRIC, {}).get("status") == "measured" \
                and report["flutter"].get(BRACKET_LOWER) is not None:
            lines.append("> Start-up is bracketed: the two runtimes do not "
                         "expose the same event, so Flutter's figure is given "
                         "as a range and the ratio uses the end least "
                         "favourable to Codename One.")
            lines.append("")
        compute_block = render_compute(report)
        if compute_block:
            lines.append(compute_block)
            lines.append("")
        gate_line = render_gate(report)
        if gate_line:
            lines.append(gate_line)
            lines.append("")
        for note in report.get("notes", []) or []:
            lines.append("> %s" % note)
        if report.get("notes"):
            lines.append("")

    wins, measured = tally(reports)
    lines.append("**Codename One wins %d of %d measured metrics across %d platform(s).**"
                 % (wins, measured, len(reports)))
    lines.append("")
    return "\n".join(lines) + "\n"


def tally(reports):
    wins = 0
    measured = 0
    for report in reports:
        for key, _label, _unit in METRICS:
            entry = report["verdict"].get(key, {})
            if entry.get("status") != "measured":
                continue
            measured += 1
            if entry["winner"] == "codenameone":
                wins += 1
    return wins, measured


# ----------------------------------------------------------------------
# The regression gate
# ----------------------------------------------------------------------

def load_baseline(path):
    if not os.path.exists(path):
        return None
    with open(path) as handle:
        return json.load(handle)


def check_regressions(report, baseline):
    """Compares this run against the committed baseline for its platform.

    Only Codename One's own numbers are gated. Flutter's are recorded for the
    ratio and are outside our control, so a Flutter SDK upgrade that makes
    their build bigger must not turn our build red.

    Each metric carries its own tolerance because they are not equally noisy.
    Size is deterministic on a runner and gets a tight band; wall-clock
    measurements on a shared runner do not, and a band tight enough to catch a
    real regression there would fire constantly on load alone.
    """
    if not baseline:
        return []
    findings = []
    tolerances = baseline.get("tolerances", {})
    values = baseline.get("codenameone", {})
    for key, label, _unit in METRICS:
        expected = values.get(key)
        tolerance = tolerances.get(key)
        if expected is None or tolerance is None:
            continue
        actual = report["codenameone"].get(key)
        if actual is None:
            # A gated metric that this run did not produce is a FAILURE, never
            # a skip. The adapter being available is what made this report
            # "measured", and sizes are read from the build output whether or
            # not the app ever ran -- so a desktop build that launches and
            # never prints its first-frame marker loses every start-up and
            # memory sample while the size rows still fill in. Skipping the
            # absent rows then printed "within tolerance" for a run whose
            # start-up was completely broken.
            findings.append({
                "metric": key,
                "label": label,
                "baseline": expected,
                "actual": None,
                "tolerance": tolerance,
                "missing": True,
            })
            continue
        judged = actual * runner_slowdown_discount(report, baseline, key)
        limit = expected * (1.0 + tolerance)
        if judged > limit:
            finding = {
                "metric": key,
                "label": label,
                "baseline": expected,
                "actual": actual,
                "tolerance": tolerance,
                "over_by": round((judged / float(expected) - 1.0) * 100.0, 1),
            }
            if judged != actual:
                finding["judged"] = round(judged, 1)
            findings.append(finding)
    return findings


def check_behind(report):
    """Every measured metric on which Codename One is BEHIND Flutter.

    The benchmark is a gate, not a scoreboard: a ratio under 1.00 on any metric
    fails the job, exactly as a regression against the baseline does. It needs
    no baseline and no tolerance -- both sides are built from the same Dart
    source and measured interleaved on the same runner, so a slow runner slows
    both and cannot turn a win into a loss or a loss into a win.

    A metric that was not measured is not judged here; check_regressions fails
    the ones a baseline gates, and the report says which were not measured.
    """
    findings = []
    compute = report.get("compute") or {}
    geomean = (compute.get("verdict") or {}).get("geomean")
    if compute.get("status") == "measured" and geomean is not None and geomean < 1.0:
        findings.append({
            "metric": COMPUTE_METRIC,
            "label": COMPUTE_LABEL,
            "codenameone": None,
            "flutter": None,
            "ratio": geomean,
            "behind": True,
        })
    for key, label, _unit in METRICS:
        entry = (report.get("verdict") or {}).get(key, {})
        if entry.get("status") != "measured":
            continue
        if entry["ratio"] < 1.0:
            findings.append({
                "metric": key,
                "label": label,
                "codenameone": entry["codenameone"],
                "flutter": entry["flutter"],
                "ratio": entry["ratio"],
                "behind": True,
            })
    return findings


# Wall-clock metrics whose runner speed Flutter's own number measures.
LOAD_NORMALISED = ("cold_start_ms",)


def runner_slowdown_discount(report, baseline, key):
    """How much of this run's time is the runner being slow, as a factor <= 1.

    The emulator's speed swings by up to 2x between runs: in one run the Android
    emulator booted in 75s instead of ~57s and Flutter's own cold start came in at
    2244 ms against 1031-1505 ms in the runs before, and Codename One's rose with it
    -- the gate failed a change that Linux and Windows, running the same code, showed
    had not moved at all. Flutter is PINNED (FLUTTER_REF) and measured interleaved on
    the same emulator, so when it is slower than its recorded reference the runner is
    slower, by that much.

    One-sided on purpose: a runner no slower than the reference is judged exactly as
    before, so this only ever discounts load; it never makes the gate stricter, and a
    slow Flutter cannot hide a regression on a normal runner. Metrics outside
    LOAD_NORMALISED, and baselines without a flutter_reference, are not adjusted.
    """
    if key not in LOAD_NORMALISED:
        return 1.0
    reference = (baseline.get("flutter_reference") or {}).get(key)
    theirs = (report.get("flutter") or {}).get(key)
    if not reference or not theirs or theirs <= reference:
        return 1.0
    return reference / float(theirs)


def baseline_candidate(report):
    """A baseline recorded from this run, ready to commit as-is.

    Only Codename One's figures are gated, and are the values here; Flutter's
    appear only as flutter_reference, the yardstick for runner speed that
    runner_slowdown_discount reads.
    Written by every gated run, so re-baselining after a deliberate change --
    a Flutter SDK bump moves every number -- is copying one file, not
    re-deriving it by hand.
    """
    values = {}
    for key, _label, _unit in METRICS:
        value = report.get("codenameone", {}).get(key)
        if value is not None:
            values[key] = value
    reference = {}
    for key in LOAD_NORMALISED:
        value = report.get("flutter", {}).get(key)
        if value is not None:
            reference[key] = value
    candidate = {
        "schema_version": 1,
        "platform": report.get("platform"),
        "generated_at": report.get("generated_at"),
        "tolerances": dict((k, DEFAULT_TOLERANCES[k]) for k in values if k in DEFAULT_TOLERANCES),
        "codenameone": values,
    }
    if reference:
        candidate["flutter_reference"] = reference
    return candidate


def render_gate(report):
    """One line saying whether this platform's numbers were actually gated."""
    gate = report.get("gate")
    if not gate:
        return None
    behind = report.get("behind") or []
    lost = ("; **BEHIND FLUTTER** on %s" % ", ".join(item["label"] for item in behind)
            if behind else "")
    if gate.get("status") == "armed":
        findings = report.get("regressions") or []
        return ("**Gate:** %s against the committed baseline%s."
                % ("REGRESSED" if findings else "within tolerance", lost))
    return ("**Gate: NOT ARMED** -- %s. This run's candidate baseline is attached "
            "to the workflow as an artifact; committing it as `%s` arms the gate%s."
            % (gate.get("reason", "no baseline"), gate.get("baseline", "baselines/<platform>.json"),
               lost))


def render_regressions(platform_id, findings):
    lines = []
    for item in findings:
        if item.get("behind") and item["metric"] == COMPUTE_METRIC:
            lines.append("%s: %s is %.2fx (the gate requires 1.00x or better)"
                         % (platform_id, item["label"], item["ratio"]))
            continue
        if item.get("behind"):
            lines.append("%s: %s is %s against Flutter's %s (ratio %.2fx; the gate requires 1.00x or better)"
                         % (platform_id, item["label"],
                            format_value(item["metric"], item["codenameone"]),
                            format_value(item["metric"], item["flutter"]),
                            item["ratio"]))
            continue
        if item.get("missing"):
            lines.append("%s: %s was not measured, but the baseline gates it at %s"
                         % (platform_id, item["label"],
                            format_value(item["metric"], item["baseline"])))
            continue
        lines.append(
            "%s: %s is %s%s against a baseline of %s (+%.1f%%, tolerance +%.0f%%)"
            % (platform_id, item["label"],
               format_value(item["metric"], item["actual"]),
               (" (%s after discounting a slow runner)" % format_value(item["metric"], item["judged"])
                if "judged" in item else ""),
               format_value(item["metric"], item["baseline"]),
               item["over_by"], item["tolerance"] * 100.0))
    return lines


def run(cmd, **kwargs):
    """subprocess.run with the arguments this harness always wants."""
    kwargs.setdefault("capture_output", True)
    kwargs.setdefault("text", True)
    return subprocess.run(cmd, **kwargs)
