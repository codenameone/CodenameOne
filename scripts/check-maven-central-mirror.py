#!/usr/bin/env python3
"""Every CI job that checks out this repository installs the Maven Central mirror.

Maven Central throttles GitHub's runners, and under load it answers "not found" for
artifacts that exist -- which the resolver treats as final, so retry settings cannot
help. `.github/actions/maven-central-mirror` puts Google's copy of Central ahead of
Central in ~/.m2/settings.xml. It only works in a job that runs it, so this fails any
job that checks out the repository and runs a step before installing it -- including
after an actions/setup-java, which writes a settings.xml of its own, replacing ours.

Two more things it holds:

- A job's workspace checkout is a full checkout of the commit under test (no
  `repository:`, `path:`, `ref:` or `sparse-checkout:`). A job with none is skipped:
  the local action may not be at ./.github there.
- A Maven wrapper in a CI-only project must download its distribution from
  repo.codenameone.com, not Central (the files are seeded there by
  maven/scripts/r2/seed-maven-distributions.sh). The archetype's wrapper is exempt: it
  is copied into every customer project, which must not depend on our hosting.

Usage:
  scripts/check-maven-central-mirror.py          # report, exit 1 on a finding
  scripts/check-maven-central-mirror.py --fix    # insert the missing steps
"""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ACTION = "./.github/actions/maven-central-mirror"
STEP = "- name: Use Google's copy of Maven Central first\n{pad}  uses: " + ACTION + "\n"
# release.yml writes publishing credentials with setup-java and can only be exercised by a
# tag, so it is not changed blind; it resolves from Central as it always has.
WORKFLOW_EXEMPT = {"release.yml"}
WRAPPER_EXEMPT = {
    "maven/cn1app-archetype/src/main/resources/archetype-resources/.mvn/wrapper/maven-wrapper.properties",
}
CENTRAL_HOSTS = ("repo.maven.apache.org", "repo1.maven.org")


def steps_of(lines, start, end):
    """(steps_indent, [(first, last_exclusive)]) for the steps: block in lines[start:end]."""
    for i in range(start, end):
        m = re.match(r"^(\s*)steps:\s*$", lines[i])
        if not m:
            continue
        base = len(m.group(1))
        items = []
        item_indent = None
        j = i + 1
        while j < end:
            line = lines[j]
            if line.strip() == "" or line.lstrip().startswith("#"):
                j += 1
                continue
            indent = len(line) - len(line.lstrip())
            if indent <= base and not line.lstrip().startswith("- "):
                break
            if indent < base:
                break
            if line.lstrip().startswith("- ") and (item_indent is None or indent == item_indent):
                if item_indent is None:
                    item_indent = indent
                if items:
                    items[-1][1] = j
                items.append([j, end])
            j += 1
        if items:
            items[-1][1] = j
            # Trailing blank and comment lines belong to the next step (a comment above
            # a step describes that step), so a step ends at its last content line.
            for item in items:
                while item[1] > item[0] + 1 and (lines[item[1] - 1].strip() == ""
                                                 or lines[item[1] - 1].lstrip().startswith("#")):
                    item[1] -= 1
        return item_indent, [tuple(x) for x in items]
    return None, []


def jobs_of(lines):
    """[(name, first, last_exclusive)] for each job under the top-level jobs: key."""
    try:
        top = next(i for i, l in enumerate(lines) if re.match(r"^jobs:\s*$", l))
    except StopIteration:
        return []
    jobs = []
    for i in range(top + 1, len(lines)):
        if re.match(r"^\S", lines[i]):
            break
        m = re.match(r"^  ([A-Za-z0-9_-]+):\s*(#.*)?$", lines[i])
        if m:
            if jobs:
                jobs[-1][2] = i
            jobs.append([m.group(1), i, len(lines)])
    if jobs:
        end = next((i for i in range(jobs[-1][1] + 1, len(lines)) if re.match(r"^\S", lines[i])), len(lines))
        jobs[-1][2] = end
    return [tuple(j) for j in jobs]


def check_file(path, fix):
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines(keepends=True)
    findings = []
    inserts = []
    for name, first, last in jobs_of(lines):
        indent, items = steps_of(lines, first, last)
        if not items:
            continue
        # A segment opens at the workspace checkout and at every step after it that
        # replaces ~/.m2/settings.xml (see is_setup_java below). It must be
        # closed by the mirror step before the first `run:` step, or by the end of the
        # job. Setup-only steps in between (more setup-java, other actions) are fine, so
        # three setup-java steps in a row need one mirror step after the last.
        opener = None
        seen_checkout = False
        where = "%s: job '%s'" % (path.relative_to(ROOT), name)
        for k, (a, b) in enumerate(items):
            body = "".join(lines[a:b])
            # Only a full checkout of the commit under test is sure to contain the local
            # action: another repository, a subdirectory, another ref (master, a PR head)
            # or a sparse checkout may not. Those jobs are housekeeping, not Maven builds.
            is_checkout = bool(re.search(r"uses:\s*actions/checkout@", body)) and not re.search(
                r"^\s*(repository|path|ref|sparse-checkout):", body, re.M)
            # setup-java writes ~/.m2/settings.xml; a cache restore of the whole ~/.m2
            # (not just ~/.m2/repository) can bring back an older run's settings.xml.
            # Either replaces ours, so the mirror is installed again after them.
            is_setup_java = bool(re.search(r"uses:\s*actions/setup-java@", body)) or (
                bool(re.search(r"uses:\s*actions/cache(/restore)?@", body))
                and bool(re.search(r"\.m2/?\s*$", body, re.M)))
            if ACTION in body:
                opener = None
                continue
            if (is_checkout and not seen_checkout) or (is_setup_java and seen_checkout):
                seen_checkout = True
                opener = k
                continue
            if opener is not None and re.search(r"^\s*(- )?run:", body, re.M):
                findings.append(where + " runs '" + body.strip().splitlines()[0].strip()
                                + "' before installing " + ACTION
                                + " after its checkout/setup-java")
                inserts.append((items[opener][1], indent))
                opener = None
        if opener is not None:
            findings.append(where + " ends without installing " + ACTION
                            + " after its last checkout/setup-java")
            inserts.append((items[opener][1], indent))
    if fix and inserts:
        for at, indent in sorted(inserts, reverse=True):
            pad = " " * indent
            step = pad + STEP.format(pad=pad)
            # Keep the file's spacing: a blank line before the step when steps are
            # separated by one.
            if at < len(lines) and lines[at].strip() == "":
                step = "\n" + step
            lines.insert(at, step)
        path.write_text("".join(lines), encoding="utf-8")
    return findings


def check_wrappers():
    out = subprocess.run(["git", "ls-files", "*maven-wrapper.properties"], cwd=ROOT,
                         capture_output=True, text=True, check=True).stdout.split()
    findings = []
    for rel in out:
        if rel in WRAPPER_EXEMPT:
            continue
        for line in (ROOT / rel).read_text(encoding="utf-8").splitlines():
            if line.startswith(("distributionUrl=", "wrapperUrl=")) and any(h in line for h in CENTRAL_HOSTS):
                findings.append(rel + ": " + line.split("=", 1)[0] + " downloads from Maven Central;"
                                " use https://repo.codenameone.com/maven2/ (same path)")
    return findings


def main():
    fix = "--fix" in sys.argv[1:]
    findings = []
    for path in sorted((ROOT / ".github" / "workflows").glob("*.y*ml")):
        if path.name in WORKFLOW_EXEMPT:
            continue
        findings += check_file(path, fix)
    findings += check_wrappers()
    if fix:
        print("inserted the mirror step where it was missing; re-run without --fix to verify")
        return 0
    for f in findings:
        print("ERROR: " + f)
    if findings:
        print("%d finding(s). scripts/check-maven-central-mirror.py --fix inserts the missing steps." % len(findings))
        return 1
    print("every workspace-checkout job installs the Maven Central mirror")
    return 0


if __name__ == "__main__":
    sys.exit(main())
