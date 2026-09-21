#!/usr/bin/env python3
"""Hold build-ios-metal and build-ios-metal-27 to being the same job.

The iOS 27 leg is a COPY of the iOS 26 leg -- 320 lines of it -- because the two
must exercise identical steps on different toolchains. A copy that nobody checks
drifts: a step added to one, a timeout raised on the other, and the "identical"
claim in the workflow comment quietly stops being true while both jobs stay
green. Nothing else in the repository would notice, because each job passes on
its own terms.

This normalises away the differences that are SUPPOSED to exist -- the runner,
the toolchain pin, the theme generation, the baseline directory, the artifact and
comment names, the concurrency group, the header comment, and the port-status
upload the 27 leg deliberately omits -- and requires everything else to match
exactly.

Adding a real difference means adding it to SUBSTITUTIONS below with a reason,
which is a reviewable act rather than an invisible one.
"""
import re
import sys
from pathlib import Path

WORKFLOW = Path(__file__).resolve().parent.parent / ".github/workflows/scripts-ios.yml"
JOB_26 = "build-ios-metal"
JOB_27 = "build-ios-metal-27"

# Applied to the 27 leg to bring it back to the 26 leg. Each entry is a
# difference that is intended; anything left over is drift.
SUBSTITUTIONS = [
    ("runs-on: xcode-27", "runs-on: macos-15"),
    ("-metal27-${{ github.ref_name }}", "-metal-${{ github.ref_name }}"),
    ("ios-ui-tests-metal-27", "ios-ui-tests-metal"),
    ("screenshots-metal-27", "screenshots-metal"),
    ("CN1SS_IOS_METAL27_COMMENT", "CN1SS_IOS_METAL_COMMENT"),
    ("CN1SS_PREVIEW_SUBDIR: ios-metal-27", "CN1SS_PREVIEW_SUBDIR: ios-metal"),
]

# Blocks present in exactly one leg on purpose. Matched as a run of consecutive
# lines and removed before comparison.
ONLY_IN_27 = [
    "CN1_XCODE_MAJOR: '27'",
    "IOS_DEPENDENCY_ARGS: '-Dcodename1.arg.ios.themeGeneration=27'",
]
# Whole STEPS present in one leg only, named by their `- name:`. Dropped as a
# block rather than line by line: a step's generic lines (`if: always()`,
# `uses: actions/upload-artifact@v7`, `retention-days: 14`) also appear in steps
# that DO exist in both, so excluding them by text would blind the comparison to
# real drift in those.
ONLY_IN_26_STEPS = ["Upload iOS Metal port status"]
ONLY_IN_27_STEPS = []


def job_lines(text, job):
    """The body of one job, without its leading comment block."""
    lines = text.split("\n")
    start = None
    for i, line in enumerate(lines):
        if line == f"  {job}:":
            start = i
            break
    if start is None:
        sys.exit(f"check-ios-metal-legs-match: job '{job}' not found in {WORKFLOW}")
    body = []
    for line in lines[start + 1:]:
        # A new job starts at exactly two spaces of indent.
        if line and not line.startswith("    ") and not line.startswith("#") and line.strip():
            break
        body.append(line)
    return body


def normalise(body, drop_exact, drop_steps):
    out = []
    skipping = False
    for line in body:
        stripped = line.strip()
        if stripped.startswith("- name:"):
            name = stripped[len("- name:"):].strip()
            skipping = name in drop_steps
        if skipping:
            # A dropped step ends where the next step begins.
            if stripped.startswith("- name:") and stripped[len("- name:"):].strip() not in drop_steps:
                skipping = False
            else:
                continue
        if not stripped or stripped.startswith("#"):
            continue          # comments and blank lines are prose, not behaviour
        if stripped in drop_exact:
            continue
        out.append(re.sub(r"\s+", " ", stripped))
    return out


def main():
    text = WORKFLOW.read_text(encoding="utf-8")
    b26 = normalise(job_lines(text, JOB_26), set(), set(ONLY_IN_26_STEPS))
    b27 = normalise(job_lines(text, JOB_27), set(ONLY_IN_27), set(ONLY_IN_27_STEPS))
    if not b26 or not b27:
        sys.exit("check-ios-metal-legs-match: one of the jobs is empty; the parser is wrong")
    rewritten = []
    for line in b27:
        for old, new in SUBSTITUTIONS:
            line = line.replace(old, new)
        rewritten.append(line)
    if rewritten == b26:
        print(f"check-ios-metal-legs-match: {JOB_26} and {JOB_27} match "
              f"({len(b26)} significant lines).")
        return 0
    import difflib
    print(f"check-ios-metal-legs-match: FAILED -- {JOB_27} has drifted from {JOB_26}.")
    print("Lines shown as '-' are in the 26 leg only, '+' in the 27 leg only.")
    print("If the difference is intended, add it to SUBSTITUTIONS/ONLY_IN_* with a reason.")
    for line in difflib.unified_diff(b26, rewritten, lineterm="",
                                     fromfile=JOB_26, tofile=JOB_27):
        print(line)
    return 1


if __name__ == "__main__":
    sys.exit(main())
