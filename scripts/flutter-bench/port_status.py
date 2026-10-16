#!/usr/bin/env python3
"""Folds benchmark results into one document for the website's data.

    scripts/flutter-bench/port_status.py --results 'out/result-*.json' \
        --out <dir>

Writes ONE file, `<dir>/flutter_benchmark.json`. The nightly workflow publishes
it to the port-status-data branch as benchmarks/flutter.json (publish_benchmark.py),
and the website build resolves it into data/port_status_flutter_benchmark.json
(scripts/website/sync_port_status_reports.sh), the same road every port's own
status report travels. It is a separate document rather than a key on each port
report because it is not a port report: the per-port acceptance check would
rightly reject it, and the Port Status page's contract check asserts that page's
exact shape.

The file records, per platform, both sides' figures and the ratio, plus the
provenance needed to know what produced them: the commit, the Flutter SDK
revision, and the host load at the time.
"""

import argparse
import glob
import json
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import benchlib  # noqa: E402


def collect(patterns):
    reports = []
    for pattern in patterns:
        for path in sorted(glob.glob(pattern)):
            with open(path) as handle:
                reports.append(json.load(handle))
    return reports


def to_port_status(reports, commit):
    platforms = {}
    for report in reports:
        platform = report.get("platform")
        if not platform:
            continue
        if report.get("status") != "measured":
            # Recorded, not dropped. A platform missing from the page reads as
            # "no difference found" when it means "never ran".
            platforms[platform] = {
                "status": "not measured",
                "reason": report.get("reason", "no reason given"),
            }
            continue
        metrics = {}
        for key, label, unit in benchlib.METRICS:
            entry = report["verdict"].get(key, {})
            if entry.get("status") != "measured":
                metrics[key] = {"status": "not measured"}
                continue
            metrics[key] = {
                "label": label,
                "unit": unit,
                "codenameone": entry["codenameone"],
                "flutter": entry["flutter"],
                "ratio": entry["ratio"],
                "winner": entry["winner"],
            }
        platforms[platform] = {
            "status": "measured",
            "runs": report.get("runs"),
            "load_average": report.get("load_average"),
            "generated_at": report.get("generated_at"),
            "metrics": metrics,
            "regressions": report.get("regressions", []),
        }

    wins, measured = benchlib.tally(
        [r for r in reports if r.get("status") == "measured"])
    return {
        "schema_version": 1,
        "commit": commit,
        "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "app": "flutter gallery (new_gallery), same Dart source on both sides",
        "summary": {"codenameone_wins": wins, "metrics_measured": measured},
        "platforms": platforms,
    }


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--results", nargs="+", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--commit", default=os.environ.get("GITHUB_SHA", "unknown"))
    args = parser.parse_args(argv)

    reports = collect(args.results)
    if not reports:
        print("no results matched %s; nothing written" % args.results)
        return 0

    payload = to_port_status(reports, args.commit)
    os.makedirs(args.out, exist_ok=True)
    path = os.path.join(args.out, "flutter_benchmark.json")
    with open(path, "w") as handle:
        json.dump(payload, handle, indent=2, sort_keys=True)
        handle.write("\n")
    print("wrote %s (%d platform(s))" % (path, len(payload["platforms"])))
    return 0


if __name__ == "__main__":
    sys.exit(main())
