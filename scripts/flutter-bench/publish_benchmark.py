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
"""Publishes the Flutter benchmark to the port-status-data branch.

    scripts/flutter-bench/publish_benchmark.py <flutter_benchmark.json> <owner/repo>

The same branch, and the same compare-and-swap, that every port's status report
goes through: the website build fetches it before Hugo renders the Port Status
page (scripts/website/sync_port_status_reports.sh). Stored as
benchmarks/flutter.json, beside ports/, because it is not a port report and the
per-port acceptance check would rightly reject it.
"""

import json
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                "..", "hellocodenameone", "conformance"))

import publish_port_status  # noqa: E402

TARGET = "benchmarks/flutter.json"


def main(argv):
    if len(argv) != 3:
        print("usage: publish_benchmark.py <flutter_benchmark.json> <owner/repo>", file=sys.stderr)
        return 2
    with open(argv[1], encoding="utf-8") as handle:
        report = json.load(handle)
    # At least one platform must have been MEASURED. Unmeasured platforms are
    # recorded in the document too, so "has platforms" is not enough: a night
    # where every leg failed would otherwise publish "not measured" everywhere
    # over the last good numbers.
    measured = [p for p in (report.get("platforms") or {}).values()
                if isinstance(p, dict) and p.get("status") == "measured"]
    if report.get("schema_version") != 1 or not measured:
        print("Not publishing %s: no measured platforms." % argv[1], file=sys.stderr)
        return 1
    try:
        publish_port_status.publish(report, argv[2], target=TARGET,
                                    message="Update the Flutter benchmark")
    except (publish_port_status.ApiError, ValueError, KeyError, OSError) as error:
        print("Flutter benchmark publication failed: %s" % error, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
