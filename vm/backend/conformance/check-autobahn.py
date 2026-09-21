#!/usr/bin/env python3
"""Turn an Autobahn|Testsuite report into a pass or a failure.

Three rules, and the second is the load-bearing one.

1. Every case must be STRICTLY green. `NON-STRICT` is a failure here, not a pass
   with a note: it means "RFC-legal but lenient", and leniency in a frame parser
   is the whole bug class this server's tests exist to close. A server that only
   validates a fragmented text message at FIN is non-strict, and it has already
   accepted however many megabytes the peer chose to send after the byte that
   made the message invalid.

2. The set of cases that RAN must equal the committed manifest. Without this,
   a spec-file edit, an image bump, or a server that dies on case 3.2 silently
   shrinks the suite to the cases it happens to pass -- and the job still reports
   green. The manifest is a record of COVERAGE, not a list of permitted
   failures; there is no per-case tolerance anywhere in this script. Growing it
   is a one-line commit that shows up as a diff.

3. A floor on the count, so a truncated or unparseable report fails loudly
   rather than reporting zero cases and zero failures.

Usage: check-autobahn.py <reports-dir> <manifest> [--write-manifest]
"""
import json
import os
import sys

# Autobahn's own vocabulary. INFORMATIONAL is what the 9.* throughput cases
# report; they measure rather than judge, so they carry no pass or fail.
ACCEPTABLE = {"OK", "INFORMATIONAL"}
MINIMUM_CASES = 200


def load(reports_dir):
    index = os.path.join(reports_dir, "index.json")
    if not os.path.isfile(index):
        sys.exit("check-autobahn: no index.json in %s -- did wstest run?" % reports_dir)
    with open(index) as handle:
        data = json.load(handle)
    if not data:
        sys.exit("check-autobahn: index.json names no agents")
    # One agent, whatever the spec called it.
    agent = sorted(data.keys())[0]
    return agent, data[agent]


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    write_manifest = "--write-manifest" in sys.argv
    if len(args) != 2:
        sys.exit(__doc__)
    reports_dir, manifest_path = args

    agent, cases = load(reports_dir)
    ran = sorted(cases.keys())

    if write_manifest:
        with open(manifest_path, "w") as handle:
            handle.write("# Every Autobahn case this server is expected to run.\n")
            handle.write("# A record of coverage, NOT a list of permitted failures:\n")
            handle.write("# check-autobahn.py requires every one of them to be strictly green.\n")
            for case in ran:
                handle.write(case + "\n")
        print("check-autobahn: wrote %d case ids to %s" % (len(ran), manifest_path))
        return 0

    failures = []
    for case in ran:
        result = cases[case]
        behavior = result.get("behavior")
        close = result.get("behaviorClose")
        if behavior not in ACCEPTABLE or close not in ACCEPTABLE:
            failures.append("%s: behavior=%s behaviorClose=%s" % (case, behavior, close))

    problems = []
    if failures:
        problems.append("%d case(s) did not pass strictly:" % len(failures))
        problems.extend("  " + line for line in failures)

    if not os.path.isfile(manifest_path):
        problems.append("no manifest at %s; run with --write-manifest to seed it"
                        % manifest_path)
    else:
        with open(manifest_path) as handle:
            expected = sorted(line.strip() for line in handle
                              if line.strip() and not line.startswith("#"))
        missing = [c for c in expected if c not in cases]
        extra = [c for c in ran if c not in expected]
        if missing:
            problems.append("%d case(s) in the manifest did not run -- the suite "
                            "shrank:" % len(missing))
            problems.extend("  " + c for c in missing)
        if extra:
            problems.append("%d case(s) ran that the manifest does not list -- add "
                            "them in a commit:" % len(extra))
            problems.extend("  " + c for c in extra)

    if len(ran) < MINIMUM_CASES:
        problems.append("only %d case(s) ran, which is below the floor of %d: the "
                        "report is probably truncated" % (len(ran), MINIMUM_CASES))

    if problems:
        print("check-autobahn: FAILED for agent %s" % agent)
        for line in problems:
            print("  " + line)
        return 1

    print("check-autobahn: %d case(s), all strictly green, for agent %s"
          % (len(ran), agent))
    return 0


if __name__ == "__main__":
    sys.exit(main())
