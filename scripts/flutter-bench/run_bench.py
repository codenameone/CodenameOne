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

Exit status is 0 unless `--gate` was asked for and a metric regressed past its
baseline tolerance.
"""

import argparse
import glob
import json
import os
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import benchlib          # noqa: E402
import platforms         # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
BASELINES = os.path.join(HERE, "baselines")


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
        entry["cold_start_runs"] = []
        entry["cold_start_lower_runs"] = []
        entry["idle_memory_runs"] = []
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
            if upper is not None:
                sides[side]["cold_start_runs"].append(upper)
            if lower is not None:
                sides[side]["cold_start_lower_runs"].append(lower)
            if memory is not None:
                sides[side]["idle_memory_runs"].append(memory)
            print("  run %d/%d %-12s cold=%s memory=%s"
                  % (index + 1, runs, side,
                     "--" if upper is None else "%7.0fms" % upper,
                     "--" if memory is None else
                     "%6.1fMB" % (memory / 1024.0 / 1024.0)), flush=True)
    return sides, notes


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--platform")
    parser.add_argument("--runs", type=int,
                        default=int(os.environ.get("BENCH_RUNS", "5")))
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

    print("%s: best of %d interleaved runs" % (adapter.label, args.runs))
    sides, notes = measure(adapter, args.runs, args.workdir)
    notes.extend(adapter.notes())
    if not adapter.exercised:
        notes.append(
            "This platform's adapter has not been exercised end to end before; "
            "treat the first published run as the thing under review.")
    report = benchlib.build_report(adapter.id, sides, args.runs, notes)
    report["status"] = "measured"

    if args.baseline_out:
        _ensure_dir(args.baseline_out)
        with open(args.baseline_out, "w") as handle:
            json.dump(benchlib.baseline_candidate(report), handle, indent=2, sort_keys=True)
        print("wrote baseline candidate %s" % args.baseline_out)

    findings = []
    unarmed = False
    if args.gate:
        relative = "scripts/flutter-bench/baselines/%s.json" % adapter.id
        baseline = benchlib.load_baseline(os.path.join(BASELINES, "%s.json" % adapter.id))
        if baseline is None:
            # A FAILURE, not a note. This used to print "recording this run as
            # the first" and succeed -- while recording nothing -- so every run
            # took this branch and the gate that was advertised never compared
            # anything. An unarmed gate has to be visible where a gate's result
            # is read, which is the job's status.
            unarmed = True
            report["gate"] = {"status": "unarmed", "baseline": relative,
                              "reason": "no committed baseline for %s" % adapter.id}
        else:
            findings = benchlib.check_regressions(report, baseline)
            report["regressions"] = findings
            report["gate"] = {"status": "armed", "baseline": relative}
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

    if findings:
        print("\nGATE FAILED")
        for line in benchlib.render_regressions(adapter.id, findings):
            print("  " + line)
        return 1
    if unarmed:
        print("\nGATE NOT ARMED: %s has no committed baseline. Commit the candidate "
              "this run wrote (--baseline-out) as %s." % (adapter.id, report["gate"]["baseline"]))
        return 1
    return 0


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
