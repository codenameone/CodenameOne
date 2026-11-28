#!/usr/bin/env python3
"""Decide which member workflows of ci.yml a pull request or push runs.

The member workflows (scripts-ios.yml, linux-build-run.yml, pr.yml, ...) used to
trigger on their own, each with its own `paths` filter, and so each started on
every push without waiting for anything. They are now `workflow_call` workflows
that ci.yml calls after its precheck. Their old filters live in
.github/ci/ci-members.json, unchanged, and this answers them the way GitHub did:

  * patterns are evaluated in order and the last one matching a file decides
    (a leading `!` excludes);
  * `*` matches within one path segment, `**` across segments, `?` one character;
  * a change to ci.yml, ci-members.json or this script runs everything;
  * an event a member does not list never runs it; an event with no `paths`
    always runs it (blog-prose.yml, the branch-only push filters).

Writes `<member>=true|false` lines to $GITHUB_OUTPUT (or stdout) plus `any`.

  scripts/ci/select-ci-members.py --event pull_request --base <sha> [--head HEAD]
  scripts/ci/select-ci-members.py --event workflow_dispatch      # everything
  scripts/ci/select-ci-members.py --event push --files a b c     # testing
"""
import argparse
import json
import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
MEMBERS = ROOT / ".github" / "ci" / "ci-members.json"
ZERO = "0" * 40


def glob_regex(pattern):
    out, i = [], 0
    while i < len(pattern):
        c = pattern[i]
        if pattern.startswith("**/", i):
            out.append("(?:.*/)?")
            i += 3
            continue
        if pattern.startswith("**", i):
            out.append(".*")
            i += 2
            continue
        if c == "*":
            out.append("[^/]*")
        elif c == "?":
            out.append("[^/]")
        elif c == "[":
            j = pattern.find("]", i)
            if j < 0:
                out.append(re.escape(c))
            else:
                out.append(pattern[i:j + 1])
                i = j
        else:
            out.append(re.escape(c))
        i += 1
    return re.compile("^" + "".join(out) + "$")


def matches(patterns, files):
    compiled = [(p.startswith("!"), glob_regex(p.lstrip("!"))) for p in patterns]
    for f in files:
        included = False
        for negated, rx in compiled:
            if rx.match(f):
                included = not negated
        if included:
            return True
    return False


# A change to the selection itself runs every member: a broken filter must not
# be able to skip the very workflows that would show it.
SELF = (".github/workflows/ci.yml", ".github/ci/ci-members.json", "scripts/ci/select-ci-members.py")


def select(members, event, files):
    """files None means 'unknown' (a dispatch, a new branch): run everything."""
    if files is not None and any(f in SELF for f in files):
        files = None
    trigger = "pull_request" if event == "pull_request" else "push"
    result = {}
    for name, triggers in members.items():
        if event == "workflow_dispatch" or files is None:
            run = True
        elif trigger not in triggers:
            run = False
        else:
            paths = triggers[trigger].get("paths")
            run = True if not paths else matches(paths, files)
        result[name] = run
    return result


def changed_files(base, head):
    if not base or base == ZERO:
        return None
    try:
        out = subprocess.run(["git", "diff", "--name-only", base + "..." + head],
                             cwd=ROOT, check=True, capture_output=True, text=True).stdout
    except subprocess.CalledProcessError as e:
        print("::warning::cannot diff %s...%s (%s); running every member"
              % (base, head, e.stderr.strip()), file=sys.stderr)
        return None
    return [l for l in out.splitlines() if l]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--event", required=True)
    ap.add_argument("--base")
    ap.add_argument("--head", default="HEAD")
    ap.add_argument("--files", nargs="*")
    args = ap.parse_args()
    members = json.loads(MEMBERS.read_text())
    files = args.files if args.files is not None else (
        None if args.event == "workflow_dispatch" else changed_files(args.base, args.head))
    result = select(members, args.event, files)
    lines = ["%s=%s" % (k, str(v).lower()) for k, v in sorted(result.items())]
    lines.append("any=%s" % str(any(result.values())).lower())
    target = os.environ.get("GITHUB_OUTPUT")
    if target:
        with open(target, "a") as fh:
            fh.write("\n".join(lines) + "\n")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
