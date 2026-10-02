#!/usr/bin/env python3
"""Runs the Flutter-vs-Codename One benchmark for one platform.

    scripts/flutter-bench/run_bench.py --platform ios \
        --cn1-app  /path/to/Bench.app     --cn1-bundle  com.example.bench \
        --flutter-app /path/to/Runner.app --flutter-bundle com.example.gallery \
        --json out/ios.json --markdown out/ios.md

    scripts/flutter-bench/run_bench.py --list        # adapters and their state
    scripts/flutter-bench/run_bench.py --render out/*.json --markdown all.md

The runs are INTERLEAVED -- one Codename One run, one Flutter run, repeated --
so a machine that gets busier partway through penalises both sides equally
instead of whichever one happened to run second. That is not a theoretical
concern on this project: two walkthrough recordings desynchronised and looked
like a timing regression, and the cause was a stray simulator holding the
machine at load 8.

Exit status is 0 unless `--gate` was asked for and either Codename One is behind
Flutter on a metric, or a metric moved past its row in scripts/flutter-bench/baseline
(flutter_baseline.py) -- or has no row to be judged by.
"""

import argparse
import glob
import json
import os
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import benchlib          # noqa: E402
import flutter_baseline  # noqa: E402
import platforms         # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
BASELINE_ROOT = str(flutter_baseline.ROOT)


def build_adapter(args):
    """Constructs the adapter named by --platform from the supplied paths."""
    kind = args.platform
    if kind in ("macos", "catalyst"):
        return platforms.MacOSAdapter(args.cn1_app, args.flutter_app)
    if kind == "linux":
        return platforms.LinuxAdapter(args.cn1_app, args.flutter_app)
    if kind == "windows":
        return platforms.WindowsAdapter(args.cn1_app, args.flutter_app)
    if kind.startswith("ios"):
        renderer = kind.split("-", 1)[1] if "-" in kind else None
        return platforms.IOSAdapter(
            udid=args.ios_udid,
            bundles={"codenameone": args.cn1_bundle,
                     "flutter": args.flutter_bundle},
            apps={"codenameone": args.cn1_app, "flutter": args.flutter_app},
            renderer=renderer,
            device=bool(args.ios_udid))
    if kind == "android":
        return platforms.AndroidAdapter(
            serial=args.android_serial,
            packages={"codenameone": args.cn1_bundle,
                      "flutter": args.flutter_bundle},
            activities={"codenameone": args.cn1_activity,
                        "flutter": args.flutter_activity},
            apks={"codenameone": args.cn1_app, "flutter": args.flutter_app})
    if kind == "javascript":
        return platforms.JavaScriptAdapter(
            {"codenameone": args.cn1_app, "flutter": args.flutter_app})
    raise SystemExit("unknown platform: %s" % kind)


def _booted_simulator():
    out = benchlib.run(["xcrun", "simctl", "list", "devices", "booted"])
    import re
    match = re.search(r"([0-9A-F]{8}-[0-9A-F-]{27})", out.stdout or "")
    return match.group(1) if match else None


def measure(adapter, runs, workdir):
    """Sizes both sides, then times them in interleaved rounds."""
    sides = {}
    for side in benchlib.SIDES:
        entry = {"artifact": adapter.artifact(side)}
        entry.update(adapter.sizes(side, workdir))
        # Each sample list carries the round it came from, so benchlib pairs
        # the two sides' launches of the same round even when a launch failed
        # on one side and the lists drifted out of step (paired_rounds).
        for base in ("cold_start", "cold_start_lower", "idle_memory"):
            entry[base + "_runs"] = []
            entry[base + "_rounds"] = []
        sides[side] = entry

    notes = []
    for index in range(runs):
        for side in benchlib.SIDES:
            try:
                upper, lower, memory = adapter.launch_and_time(side)
            except platforms.Unavailable as err:
                if str(err) not in notes:
                    notes.append(str(err))
                continue
            for base, value in (("cold_start", upper), ("cold_start_lower", lower),
                                ("idle_memory", memory)):
                if value is not None:
                    sides[side][base + "_runs"].append(value)
                    sides[side][base + "_rounds"].append(index)
            print("  run %d/%d %-12s cold=%s memory=%s"
                  % (index + 1, runs, side,
                     "--" if upper is None else "%7.0fms" % upper,
                     "--" if memory is None else
                     "%6.1fMB" % (memory / 1024.0 / 1024.0)), flush=True)
    return sides, notes


def measure_compute(adapter, workdir=None):
    """One compute-mode launch per side, scored by benchlib.compute_verdict.

    Each app repeats every workload itself (warm-ups, then the best of five),
    so a single launch per side is a complete result. A side that cannot run
    makes the whole section "not measured" with its reason: half a comparison
    is not a comparison.
    """
    results = {}
    for side in benchlib.SIDES:
        try:
            results[side] = adapter.run_compute(side)
        except platforms.Unavailable as err:
            return {"status": "not measured", "reason": str(err)}
        print("  compute %-12s %d workloads" % (side, len(results[side])), flush=True)
    # Which side is right when they disagree, or when one produced nothing:
    # the same source on the host JVM. See benchlib.compute_verdict.
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    reference = benchlib.reference_checksums(
        repo_root, workdir or os.path.join(repo_root, "scripts", "flutter-bench", "out"))
    print("  compute reference: %s" % (
        "%d workloads from the host JVM" % len(reference) if reference
        else "unavailable (no JDK); disagreements stay unattributed"), flush=True)
    return {
        "status": "measured",
        "codenameone": dict((k, list(v)) for k, v in results["codenameone"].items()),
        "flutter": dict((k, list(v)) for k, v in results["flutter"].items()),
        "reference": reference,
        "verdict": benchlib.compute_verdict(results["codenameone"], results["flutter"],
                                            reference),
    }


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--platform")
    # Interleaved rounds. Odd, so the per-round median benchlib judges start-up
    # and memory by is one real round rather than the mean of two; nine rather
    # than five so that two disturbed rounds -- a CI runner's first launch is
    # routinely 2-3x the rest -- still leave the median among clean ones. A
    # round cost 25-30 s on the hosted runners (two launches, each held
    # SETTLE_S for its memory reading), so this adds about two minutes to legs
    # that finish in 15-25 of their 120.
    parser.add_argument("--runs", type=int,
                        default=int(os.environ.get("BENCH_RUNS", "9")))
    parser.add_argument("--cn1-app")
    parser.add_argument("--flutter-app")
    parser.add_argument("--cn1-bundle")
    parser.add_argument("--flutter-bundle")
    parser.add_argument("--cn1-activity", default=".MainActivity")
    parser.add_argument("--flutter-activity", default=".MainActivity")
    parser.add_argument("--ios-udid")
    parser.add_argument("--android-serial")
    parser.add_argument("--json")
    parser.add_argument("--markdown")
    # The platform's temp directory, not "/tmp": native Windows Python has no
    # /tmp, and the zip wire_size writes there failed the Windows leg after both
    # applications had built.
    parser.add_argument("--workdir", default=tempfile.gettempdir())
    parser.add_argument("--gate", action="store_true",
                        help="fail when a metric regresses past its baseline, "
                             "or when there is no baseline to compare against")
    parser.add_argument("--baseline-out",
                        help="write a baseline recorded from this run here")
    parser.add_argument("--list", action="store_true",
                        help="list adapters and whether each has been exercised")
    parser.add_argument("--render", nargs="*",
                        help="render markdown from existing result json files")
    args = parser.parse_args(argv)

    if args.list:
        return _list_adapters()

    if args.render is not None:
        return _render(args)

    if not args.platform:
        parser.error("--platform is required unless --list or --render is used")

    adapter = build_adapter(args)
    ok, reason = adapter.available()
    if not ok:
        # A platform that cannot be measured is recorded as such rather than
        # dropped. A benchmark that silently omits a platform reads exactly
        # like one that measured it and found nothing to report.
        report = {
            "schema_version": 1,
            "platform": adapter.id,
            "status": "unavailable",
            "reason": reason,
        }
        _write(args, report, [report])
        print("%s: not measured -- %s" % (adapter.id, reason))
        # Under --gate an unmeasured platform FAILS. Returning success let a
        # platform escape its gate just by not being measured -- the emulator
        # dropping off adb, Chrome missing -- with the leg green and a regression
        # riding through unseen. The result is still written above, so the
        # comment says why.
        return 1 if args.gate else 0

    print("%s: %d interleaved rounds" % (adapter.label, args.runs))
    sides, notes = measure(adapter, args.runs, args.workdir)
    notes.extend(adapter.notes())
    if not adapter.exercised:
        notes.append(
            "This platform's adapter has not been exercised end to end before; "
            "treat the first published run as the thing under review.")
    report = benchlib.build_report(adapter.id, sides, args.runs, notes)
    report["status"] = "measured"
    # The runner's CPU model, recorded with every result: a ratio row can be keyed by it
    # (flutter_baseline.row_key), as the ParparVM gate's are. On Android this is the host
    # the emulator runs on, which is the CPU both apps actually execute on.
    report["cpu"] = flutter_baseline.perf_gate.cpu_model()
    print("%s: compute workloads" % adapter.label)
    report["compute"] = measure_compute(adapter, args.workdir)

    if args.baseline_out:
        _ensure_dir(args.baseline_out)
        with open(args.baseline_out, "w") as handle:
            json.dump(flutter_baseline.candidate(report), handle, indent=2, sort_keys=True)
        print("wrote baseline candidate %s" % args.baseline_out)

    findings = []
    stale = False
    if args.gate:
        findings, stale = _judge(report, adapter.id)
        # Losing to Flutter fails the gate on its own, baseline or not: the
        # benchmark exists to show Codename One ahead on every metric, and a
        # loss that matched the baseline would otherwise pass as "no change".
        behind = benchlib.check_behind(report)
        report["behind"] = behind
        findings = findings + behind

    _write(args, report, [report])
    # Printed AFTER the gate is decided, so the job log carries the gate line
    # ("within tolerance", "REGRESSED", "NOT ARMED"); printed before, a green
    # job could not show whether it had compared against anything.
    print(benchlib.render_markdown([report]))

    if findings or stale:
        print("\nGATE FAILED")
        for line in benchlib.render_regressions(adapter.id, findings):
            print("  " + line)
        if stale:
            print("  this pull request's own overlay is stale: %s" % report["gate"]["stale_overlay"])
        if report.get("regressions") or stale:
            print("  to accept the move: %s" % report["gate"]["fix"])
        return 1
    return 0


def _judge(report, platform_id):
    """The regression gate: (findings, stale). Reads scripts/flutter-bench/baseline the
    way the ParparVM gate reads its own (vm/selfhost/perf-gate.py), including what it does
    when this pull request's own overlay went stale: judge without it and fail, rather than
    refuse to judge and leave nothing to re-measure from."""
    number = flutter_baseline.perf_baseline.pr_number()
    artifact = "baseline-%s.json" % platform_id
    stale = None
    try:
        data = flutter_baseline.load(BASELINE_ROOT)
    except flutter_baseline.BaselineError as error:
        data = None
        own = number and os.path.isfile(os.path.join(BASELINE_ROOT, "pr", "%d.json" % number))
        if own:
            try:
                data = flutter_baseline.load(BASELINE_ROOT, exclude=number)
                stale = str(error)
            except flutter_baseline.BaselineError:
                data = None
        if data is None:
            # Overlays that contradict each other leave nothing to judge against: a
            # failure, never a skip, and the comment says which.
            report["gate"] = {"status": "refused", "reason": "flutter-baseline: %s" % error}
            finding = {"metric": "baseline", "label": "the baselines could not be read: %s"
                       % error}
            report["regressions"] = [finding]
            return [finding], False
    findings = flutter_baseline.judge(report, data)
    for note in data.get("notes", []):
        print("flutter-baseline note: %s" % note)
    report["regressions"] = findings
    report["gate"] = {"status": "armed", "baseline": "scripts/flutter-bench/baseline",
                      "pr": number,
                      "fix": flutter_baseline.fix_command(number, findings, artifact)}
    if stale:
        report["gate"]["stale_overlay"] = stale
    return findings, bool(stale)


def _write(args, report, reports):
    if args.json:
        _ensure_dir(args.json)
        with open(args.json, "w") as handle:
            json.dump(report, handle, indent=2, sort_keys=True)
        print("wrote %s" % args.json)
    if args.markdown:
        _ensure_dir(args.markdown)
        with open(args.markdown, "w") as handle:
            handle.write(benchlib.render_markdown(
                [r for r in reports if r.get("status") == "measured"]))
        print("wrote %s" % args.markdown)


def _ensure_dir(path):
    parent = os.path.dirname(os.path.abspath(path))
    if parent and not os.path.isdir(parent):
        os.makedirs(parent)


def _render(args):
    """Merges per-platform result files into one comment body."""
    paths = []
    for pattern in (args.render or []):
        paths.extend(sorted(glob.glob(pattern)))
    reports = []
    skipped = []
    for path in paths:
        with open(path) as handle:
            report = json.load(handle)
        if report.get("status") == "measured":
            reports.append(report)
        else:
            skipped.append(report)
    body = benchlib.render_markdown(reports)
    if skipped:
        body += "\n_Not measured: %s._\n" % ", ".join(
            "%s (%s)" % (r.get("platform"), r.get("reason", "no reason given"))
            for r in skipped)
    if args.markdown:
        _ensure_dir(args.markdown)
        with open(args.markdown, "w") as handle:
            handle.write(body)
        print("wrote %s" % args.markdown)
    else:
        print(body)
    return 0


def _list_adapters():
    rows = [
        ("macos", "macOS (Catalyst)", platforms.MacOSAdapter.exercised),
        ("ios", "iOS (release bundles)", platforms.IOSAdapter.exercised),
        ("android", "Android", platforms.AndroidAdapter.exercised),
        ("linux", "Linux", platforms.LinuxAdapter.exercised),
        ("windows", "Windows", platforms.WindowsAdapter.exercised),
        ("javascript", "JavaScript", platforms.JavaScriptAdapter.exercised),
    ]
    print("%-12s %-22s %s" % ("id", "platform", "exercised end to end"))
    for ident, label, exercised in rows:
        print("%-12s %-22s %s" % (ident, label, "yes" if exercised else "NO"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
