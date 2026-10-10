#!/usr/bin/env python3
"""Shared machinery for the Flutter-vs-Codename One benchmark.

The benchmark measures ONE application built two ways: Flutter's own AOT
compile of the gallery, and the identical Dart source transpiled to Java and
run on Codename One. Same source, same screens, same device, so a difference
in size, in start-up time or in memory is a difference between the two runtimes
and nothing else.

This module holds everything that is not platform specific: the metric list,
the sizing helpers, the per-round statistics, the rendering of the report, and
the regression gate. Platform specific work -- how an
application is launched, and how its memory is read -- lives behind the adapter
interface in `platforms.py`.

Rules the numbers have to obey, because it is easy to produce flattering ones:

  * Release/AOT on both sides. A debug build of either is meaningless.
  * The SAME SURFACE SIZE. Window or screen area drives the size of the GPU
    surfaces that dominate a UI application's resident memory, so two builds
    measured at different sizes cannot be compared on memory at all.
  * INTERLEAVED rounds, and the machine's load average recorded. A ratio
    taken under different load twice is not a ratio -- a walkthrough recording
    on this project desynchronised twice and looked like a timing defect, and
    the cause was a stray simulator holding the machine at load 8.
  * Head to head, start-up and memory at rest are judged PER ROUND: the ratio
    is the median of the per-round ratios, each from two launches made back to
    back, and the figures shown are each side's median. Not each side's best:
    one undisturbed outlier on either side decided the verdict on its own (see
    paired_rounds). The regression gate (flutter_baseline.py) reads the same
    statistic, Codename One over Flutter, so the pinned Flutter build is its
    unit of measure and a slow runner cancels out.
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

# Start-up is a point wherever Flutter can report the moment its content frame
# was on screen (see platforms.MARKERS), and then `cold_start_lower_ms` is None.
# Where it cannot -- the web -- it is reported as a BRACKET, because the two
# runtimes do not expose the same event. Flutter's `cold_start_ms` is RASTERDONE, after its
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


def median(values):
    """The middle value; the mean of the two middle ones for an even count.

    The round count defaults to an odd number so that the median is a real
    round's figure rather than an average of two.
    """
    if not values:
        return None
    ordered = sorted(values)
    mid = len(ordered) // 2
    if len(ordered) % 2:
        return ordered[mid]
    return (ordered[mid - 1] + ordered[mid]) / 2.0


# The metrics judged head to head round by round, and the per-side sample
# lists each one is read from: `<base>_runs` holds the values and, when the
# harness recorded it, `<base>_rounds` the round each value came from.
#
# Memory at rest is here too, not left at best-of. The case for the minimum is
# that it is the run least disturbed by the machine, which holds for a clock
# and not for resident memory: a low reading is more often an app that had not
# finished loading than an undisturbed one. Measured on the Android emulator:
# the first round read 33.0 MB for Codename One against 45.0-45.7 MB in every
# other round, and 60.7 MB for Flutter against 88.1-90.6 MB, so best-of compared
# two half-loaded apps. The median of the per-round ratios ignores such a round
# on either side, for the same reason it ignores a lucky start-up.
PAIRED_METRICS = {
    "cold_start_ms": "cold_start",
    "idle_memory_bytes": "idle_memory",
}
BRACKET_LOWER_BASE = "cold_start_lower"


def _samples(side, base):
    """(values, rounds) for one sample list; rounds is None when not recorded."""
    values = list(side.get(base + "_runs") or [])
    rounds = side.get(base + "_rounds")
    if rounds is not None and len(rounds) != len(values):
        rounds = None
    return values, (list(rounds) if rounds is not None else None)


def paired_rounds(ours_side, theirs_side, key):
    """[(ours, theirs)] for every round both sides measured, or (None, reason).

    Why per round. The runs are INTERLEAVED, so the two launches of one round
    are adjacent in time and share whatever the machine was doing; two
    different rounds do not. Best-of-N compared each side's luckiest round
    with the other's, so a single undisturbed launch on either side decided
    the verdict alone. Real numbers from the macOS runner (ms, five rounds):
    Codename One 1787, 707, 468, 584, 668 against Flutter 2512, 617, 522,
    356, 769. Codename One was faster in three of the five rounds, yet
    best-of reported a Flutter win (356 against 468, 0.76x), on the strength
    of one Flutter round; each side's median flips the same way (668 against
    617), because it too compares different rounds. The median of the five
    per-round ratios is 522/468 = 1.115, and no single round can move it past the
    middle one.

    Rounds are matched by the round number the harness recorded with each
    sample, so a launch that failed on one side drops that round only, rather
    than shifting every later pair. Reports without round numbers are paired
    by position, which is only sound when both lists are the same length;
    otherwise this declines and the caller falls back to best-of.

    For the bracketed start-up (BRACKET_METRIC), Flutter's figure in each
    round is the end of ITS bracket least favourable to Codename One --
    min(upper, lower) -- so the bracket rule holds round by round. A round
    with no lower end is dropped rather than judged by the upper end alone,
    which would flatter us.
    """
    base = PAIRED_METRICS[key]
    ours, ours_rounds = _samples(ours_side, base)
    theirs, theirs_rounds = _samples(theirs_side, base)
    lower, lower_rounds = ([], None)
    if key == BRACKET_METRIC:
        lower, lower_rounds = _samples(theirs_side, BRACKET_LOWER_BASE)
    if not ours or not theirs:
        return None, "no per-round samples on both sides"
    if ours_rounds is not None and theirs_rounds is not None \
            and (not lower or lower_rounds is not None):
        theirs_by_round = dict(zip(theirs_rounds, theirs))
        lower_by_round = dict(zip(lower_rounds or [], lower))
        pairs = []
        for number, value in zip(ours_rounds, ours):
            if number not in theirs_by_round:
                continue
            other = theirs_by_round[number]
            if lower:
                if number not in lower_by_round:
                    continue
                pairs.append((value, other, lower_by_round[number]))
            else:
                pairs.append((value, other, None))
    else:
        if len(ours) != len(theirs):
            return None, ("the sides measured %d and %d rounds and carry no round "
                          "numbers to match them by" % (len(ours), len(theirs)))
        if lower and len(lower) != len(theirs):
            return None, ("Flutter's bracket has %d lower ends for %d rounds and "
                          "no round numbers to match them by" % (len(lower), len(theirs)))
        pairs = [(a, b, lower[i] if lower else None)
                 for i, (a, b) in enumerate(zip(ours, theirs))]
    # A zero is a failed sample, not an infinitely fast one; verdict treats a
    # zero summary the same way.
    pairs = [p for p in pairs if p[0] and p[1] and (p[2] is None or p[2])]
    if not pairs:
        return None, "no round was measured by both sides"
    return pairs, None


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
    """Collapses a side's per-run samples into one figure per metric: the best run.

    Read only where rounds cannot be paired (verdict's fallback, which says so in
    the report). Neither the head-to-head verdict nor the regression gate uses it
    otherwise: both judge the median of the per-round ratios (paired_rounds).
    """
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


# What a side's result on one workload was, when it was not a comparable timing.
# "wrong result" and "did not run" name the side that failed; the geometric mean
# is still taken over the workloads both sides got right, so a failure is shown,
# never scored as a timing.
FAILED_WRONG = "wrong result"
FAILED_NOT_RUN = "did not run"


def compute_verdict(ours, theirs, reference=None):
    """Per-workload ratios and their geometric mean.

    `ours` and `theirs` map a workload to (checksum, ms). The ratio is
    flutter_ms / codenameone_ms, so above 1.00x Codename One is faster.

    A workload is only compared when both checksums are identical: a different
    checksum means the two sides did not do the same computation -- on the web,
    for one, JavaScript numbers cannot hold a 64-bit integer -- and a ratio
    between two different computations is not a measurement.

    `reference` maps a workload to the checksum a host JVM computes from the same
    source (reference_checksums). With it, a disagreement or a missing result is
    attributed: the row says WHICH side got a wrong result or did not run, and
    `failures` lists them, instead of both collapsing into "checksum mismatch" or
    "not measured". Before this, Flutter web returning a wrong intArithmetic and
    no longArithmetic at all simply removed those workloads from the web leg --
    including ones Codename One wins -- and a Codename One failure would have
    vanished the same way. The reference only arbitrates a DISAGREEMENT: two sides
    that agree are compared whatever the JVM says, because a transcendental
    workload may legitimately differ from a desktop JVM's libm in the last bit on
    both.
    """
    rows = []
    ratios = []
    failures = []
    reference = reference or {}
    for name in COMPUTE_WORKLOADS:
        a = (ours or {}).get(name)
        b = (theirs or {}).get(name)
        expected = reference.get(name)
        if a is None or b is None:
            row = {"name": name, "status": "not measured"}
            failed = []
            if a is None and b is not None:
                failed = [("codenameone", FAILED_NOT_RUN)]
            elif b is None and a is not None:
                failed = [("flutter", FAILED_NOT_RUN)]
            if expected is not None:
                # The side that did run can still be wrong.
                if a is not None and a[0] != expected:
                    failed.append(("codenameone", FAILED_WRONG))
                if b is not None and b[0] != expected:
                    failed.append(("flutter", FAILED_WRONG))
            if failed:
                row["failed"] = [{"side": side, "how": how} for side, how in failed]
                failures.extend({"name": name, "side": side, "how": how} for side, how in failed)
            if a is not None:
                row["codenameone"] = a[1]
            if b is not None:
                row["flutter"] = b[1]
            rows.append(row)
            continue
        if a[0] != b[0]:
            row = {"name": name, "status": "checksum mismatch",
                   "codenameone": a[1], "flutter": b[1],
                   "checksums": [a[0], b[0]]}
            if expected is not None:
                failed = []
                if a[0] != expected:
                    failed.append("codenameone")
                if b[0] != expected:
                    failed.append("flutter")
                if len(failed) == 1:
                    row["failed"] = [{"side": failed[0], "how": FAILED_WRONG}]
                    failures.append({"name": name, "side": failed[0], "how": FAILED_WRONG})
            rows.append(row)
            continue
        if a[1] <= 0 or b[1] <= 0:
            rows.append({"name": name, "status": "too fast to time",
                         "codenameone": a[1], "flutter": b[1]})
            continue
        ratio = float(b[1]) / float(a[1])
        ratios.append(ratio)
        rows.append({"name": name, "status": "measured", "codenameone": a[1],
                     "flutter": b[1], "ratio": round(ratio, 3)})
    out = {"workloads": rows, "compared": len(ratios), "failures": failures}
    if ratios:
        product = 1.0
        for r in ratios:
            product *= r
        out["geomean"] = round(product ** (1.0 / len(ratios)), 3)
    return out


_REFERENCE_DRIVER = """
public class RefDriver {
    public static void main(String[] a) {
        String[] names = {%s};
        for (String n : names) {
            long c;
            try {
                c = (Long) com.bench.CommonWorkloads.class.getMethod(n).invoke(null);
            } catch (Exception e) {
                throw new RuntimeException(n, e);
            }
            System.out.println("REF " + n + " " + c);
        }
    }
}
"""


def reference_checksums(repo_root, workdir, java_home=None):
    """The checksum each compute workload produces on the host JVM, or None.

    Compiled and run from vm/benchmarks' CommonWorkloads.java -- the very file
    prepare.sh copies into the Codename One app -- so it cannot drift from what
    the apps run. None (and the verdict falls back to unattributed mismatches)
    when no JDK is on hand; that is reported, never guessed.
    """
    source = os.path.join(repo_root, "vm", "benchmarks", "common", "src", "main",
                          "java", "com", "bench", "CommonWorkloads.java")
    if not os.path.isfile(source):
        return None
    java_home = java_home or os.environ.get("JAVA_HOME")
    def tool(name):
        if java_home:
            for candidate in (name, name + ".exe"):
                path = os.path.join(java_home, "bin", candidate)
                if os.path.isfile(path):
                    return path
        return name
    out_dir = os.path.join(workdir, "compute-reference")
    src_dir = os.path.join(out_dir, "src")
    os.makedirs(os.path.join(src_dir, "com", "bench"), exist_ok=True)
    with open(source, "rb") as fin, open(os.path.join(src_dir, "com", "bench",
                                                       "CommonWorkloads.java"), "wb") as fout:
        fout.write(fin.read())
    names = ", ".join('"%s"' % n for n in COMPUTE_WORKLOADS)
    with open(os.path.join(src_dir, "RefDriver.java"), "w") as f:
        f.write(_REFERENCE_DRIVER % names)
    classes = os.path.join(out_dir, "classes")
    os.makedirs(classes, exist_ok=True)
    try:
        subprocess.run([tool("javac"), "-d", classes,
                        os.path.join(src_dir, "com", "bench", "CommonWorkloads.java"),
                        os.path.join(src_dir, "RefDriver.java")],
                       check=True, capture_output=True, timeout=300)
        result = subprocess.run([tool("java"), "-cp", classes, "RefDriver"],
                                check=True, capture_output=True, text=True, timeout=900)
    except (OSError, subprocess.SubprocessError):
        return None
    refs = {}
    for line in result.stdout.splitlines():
        parts = line.split()
        if len(parts) == 3 and parts[0] == "REF":
            refs[parts[1]] = parts[2]
    return refs or None


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
        reason = compute.get("reason", "no reason given").rstrip(".")
        lines.append("_Not measured: %s._" % reason)
        return "\n".join(lines)
    verdict = compute["verdict"]
    lines.append("| Workload | Codename One | Flutter | Ratio |")
    lines.append("| --- | ---: | ---: | ---: |")
    for row in verdict["workloads"]:
        if row.get("failed"):
            # Attributed: say which side failed, and show whatever timing the
            # other side has. Not part of the mean.
            def cell(side):
                for f in row["failed"]:
                    if f["side"] == side:
                        return f["how"]
                return "%d ms" % row[side] if side in row else "--"
            lines.append("| %s | %s | %s | %s |" % (
                row["name"], cell("codenameone"), cell("flutter"),
                "; ".join("%s: %s" % ("Codename One" if f["side"] == "codenameone"
                                      else "Flutter", f["how"]) for f in row["failed"])))
        elif row["status"] == "measured":
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
        if key in PAIRED_METRICS:
            pairs, reason = paired_rounds(report["codenameone"], report["flutter"], key)
            if pairs:
                out[key] = _paired_entry(pairs)
                continue
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
        if key in PAIRED_METRICS:
            # Said in the report: a reader must not take this for the paired
            # statistic. Only reached with per-side figures but no pairable
            # rounds -- a report rendered from older JSON, or rounds lost.
            out[key]["statistic"] = "best_of"
            out[key]["fallback"] = reason
    return out


def _paired_entry(pairs):
    """The verdict entry for a metric judged round by round (paired_rounds)."""
    ratios = []
    theirs = []
    for ours, upper, lower in pairs:
        # The bracket's least favourable end, per round: see BRACKET_METRIC.
        other = min(upper, lower) if lower is not None else upper
        theirs.append(other)
        ratios.append(float(other) / float(ours))
    ratio = round(median(ratios), 3)
    entry = {
        "status": "measured",
        "statistic": "paired_median",
        # Each side's own median, for the magnitudes. The ratio is NOT their
        # quotient: it is the median of the per-round ratios, and the two can
        # disagree -- that disagreement is the whole reason for pairing.
        "codenameone": median([p[0] for p in pairs]),
        "flutter": median(theirs),
        "ratio": ratio,
        # By the ratio, so the winner, check_behind and the tally all read the
        # same statistic. A tie goes to Flutter, as it does for a summary.
        "winner": "codenameone" if ratio > 1.0 else "flutter",
        "rounds": len(pairs),
        "rounds_won": sum(1 for r in ratios if r > 1.0),
    }
    if any(p[2] is not None for p in pairs):
        entry["flutter_bracket"] = [median([p[2] for p in pairs]),
                                    median([p[1] for p in pairs])]
    return entry


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
        lines.append("_%d interleaved rounds%s_" % (
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
            if entry.get("flutter_bracket"):
                # Medians of each end over the paired rounds.
                flutter_cell = "%s (%s-%s)" % (
                    flutter_cell, format_value(key, entry["flutter_bracket"][0]),
                    format_value(key, entry["flutter_bracket"][1]))
            elif key == BRACKET_METRIC and entry.get("statistic") != "paired_median":
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
        for note in statistics_notes(report):
            lines.append("> %s" % note)
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


def statistics_notes(report):
    """What the start-up and memory rows' numbers are, stated under the table.

    Needed because the paired ratio is not the quotient of the two medians
    shown beside it, and a reader who checks the arithmetic deserves to be
    told why it does not divide out.
    """
    notes = []
    verdict_ = report.get("verdict") or {}
    paired = []
    for key, label, _unit in METRICS:
        entry = verdict_.get(key, {})
        if entry.get("status") != "measured":
            continue
        if entry.get("statistic") == "paired_median":
            paired.append("%d of %d rounds on %s" % (entry["rounds_won"], entry["rounds"], label.lower()))
        elif entry.get("statistic") == "best_of":
            notes.append("%s could not be paired round by round (%s), so its "
                         "figures and ratio are each side's best run, and one "
                         "undisturbed round can decide it." % (label, entry.get("fallback")))
    if paired:
        notes.append("Start-up and memory at rest are judged round by round: the "
                     "ratio is the median of the per-round ratios, each from two "
                     "launches made back to back, and the times and sizes shown "
                     "are each side's median, so the ratio need not equal their "
                     "quotient. Codename One was ahead in %s."
                     % "; ".join(paired))
    entry = verdict_.get(BRACKET_METRIC, {})
    if entry.get("status") == "measured" and (
            entry.get("flutter_bracket")
            or (entry.get("statistic") != "paired_median"
                and report["flutter"].get(BRACKET_LOWER) is not None)):
        notes.append("Start-up is bracketed: the two runtimes do not expose the "
                     "same event, so Flutter's figure is given as a range and "
                     "the ratio uses the end least favourable to Codename One%s."
                     % (", round by round" if entry.get("flutter_bracket") else ""))
    return notes


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

# Platforms where losing to Flutter is reported but does not fail the gate, each with
# the reason the comment prints. Only the head-to-head comparison is relaxed: a
# workload Codename One gets wrong or does not run still fails, and so does a move
# outside this platform's regression baselines.
#
# JavaScript is the one platform where a loss to Flutter is currently accepted: the
# browser runs Flutter's AOT-compiled Dart through dart2js/Wasm against our
# bytecode-to-JavaScript translation, and closing the compute gap there is not this
# benchmark's priority. It stays measured and shown so the gap remains visible.
HEAD_TO_HEAD_REPORT_ONLY = {
    "javascript": "a loss to Flutter on the web is currently accepted",
}


def gating_behind(platform_id, behind):
    """The part of `behind` that fails the gate on `platform_id`.

    Everywhere but a report-only platform that is all of it. On a report-only
    platform it is only Codename One's own compute failures (a wrong or missing
    result), which are a correctness defect rather than a loss on speed or size.
    """
    if platform_id not in HEAD_TO_HEAD_REPORT_ONLY:
        return list(behind)
    return [item for item in behind if item.get("failed")]


def check_behind(report):
    """Every measured metric on which Codename One is BEHIND Flutter.

    The benchmark is a gate, not a scoreboard: a ratio under 1.00 on any metric
    fails the job, exactly as a regression against the baseline does. It needs
    no baseline and no tolerance -- both sides are built from the same Dart
    source and measured interleaved on the same runner, so a slow runner slows
    both and cannot turn a win into a loss or a loss into a win.

    A metric that was not measured is not judged here; the regression gate fails
    the ones a baseline gates, and the report says which were not measured.
    """
    findings = []
    compute = report.get("compute") or {}
    # A workload Codename One got wrong or did not run, where the reference says
    # which side failed, fails the gate on its own: attributing Flutter's failures
    # is only honest if ours are held to the same standard.
    for failure in (compute.get("verdict") or {}).get("failures", []):
        if failure["side"] == "codenameone":
            findings.append({
                "metric": COMPUTE_METRIC,
                "label": "%s: %s (%s)" % (COMPUTE_LABEL, failure["name"], failure["how"]),
                "codenameone": None,
                "flutter": None,
                "ratio": 0.0,
                "behind": True,
                "failed": failure,
            })
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


def render_gate(report):
    """One line saying whether this platform's numbers were actually gated."""
    gate = report.get("gate")
    if not gate:
        return None
    behind = report.get("behind") or []
    lost = ("; **BEHIND FLUTTER** on %s" % ", ".join(item["label"] for item in behind)
            if behind else "")
    accepted = report.get("behind_accepted")
    if behind and accepted:
        lost += " (reported, not gated: %s)" % accepted
    if gate.get("status") == "armed":
        findings = report.get("regressions") or []
        line = ("**Gate:** %s against `scripts/flutter-bench/baseline`%s."
                % ("OUTSIDE THE BASELINE" if findings else "within tolerance", lost))
        if gate.get("stale_overlay"):
            line += (" This pull request's own overlay is stale (%s); the run was judged "
                     "without it, and fails until it is re-measured." % gate["stale_overlay"])
        advisories = report.get("advisories") or []
        if advisories:
            line += (" Smaller than its baseline, which does not fail the gate: %s -- "
                     "rebaseline to tighten." % "; ".join(
                         "%s %+.2f%%" % (a["label"], a["moved_by"]) for a in advisories))
        if findings or gate.get("stale_overlay") or advisories:
            line += (" To accept the move, download this platform's `baseline-<platform>.json` "
                     "artifact and run `%s`, then commit the overlay it writes."
                     % gate.get("fix", "flutter_baseline.py calibrate"))
        return line
    return ("**Gate: NOT JUDGED** -- %s%s."
            % (gate.get("reason", "no baseline could be read"), lost))


def render_regressions(platform_id, findings):
    lines = []
    for item in findings:
        if item.get("failed"):
            lines.append("%s: Codename One %s on compute workload %s (checked against the host JVM)"
                         % (platform_id,
                            "gave a wrong result" if item["failed"]["how"] == FAILED_WRONG
                            else "did not run",
                            item["failed"]["name"]))
            continue
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
        if item.get("verdict"):
            # A regression-gate finding (flutter_baseline.judge). Imported here, not at
            # the top: flutter_baseline imports this module.
            import flutter_baseline
            lines.append("%s: %s" % (platform_id, flutter_baseline.describe(item)))
            continue
        lines.append("%s: %s" % (platform_id, item.get("label", item.get("metric"))))
    return lines


def run(cmd, **kwargs):
    """subprocess.run with the arguments this harness always wants."""
    kwargs.setdefault("capture_output", True)
    kwargs.setdefault("text", True)
    return subprocess.run(cmd, **kwargs)
