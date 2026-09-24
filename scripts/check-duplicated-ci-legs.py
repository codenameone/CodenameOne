#!/usr/bin/env python3
"""Hold each duplicated CI leg to being the same job as the one it copies.

Two jobs here are COPIES of another job -- build-ios-metal-27 of build-ios-metal,
and fidelity-ios-metal-27 of fidelity-ios-metal -- because each pair must run
identical steps on different toolchains. A copy that nobody checks drifts: a step
added to one, a timeout raised on the other, and the "identical" claim in the
workflow comment quietly stops being true while both jobs stay green. Nothing
else in the repository would notice, because each job passes on its own terms.

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

ROOT = Path(__file__).resolve().parent.parent

# One entry per duplicated pair. `subs` is applied to the COPY to bring it back
# to the original; each entry is a difference that is intended, and anything left
# over is drift. `only_in_*_steps` names whole steps that exist in one leg on
# purpose, matched by their `- name:`.
PAIRS = [
    {
        "workflow": ".github/workflows/scripts-ios.yml",
        "original": "build-ios-metal",
        "copy": "build-ios-metal-27",
        "subs": [
            ("runs-on: xcode-27", "runs-on: macos-15"),
            ("-metal27-${{ github.ref_name }}", "-metal-${{ github.ref_name }}"),
            ("ios-ui-tests-metal-27", "ios-ui-tests-metal"),
            ("screenshots-metal-27", "screenshots-metal"),
            ("CN1SS_IOS_METAL27_COMMENT", "CN1SS_IOS_METAL_COMMENT"),
            ("CN1SS_PREVIEW_SUBDIR: ios-metal-27", "CN1SS_PREVIEW_SUBDIR: ios-metal"),
            # A SUBSTITUTION, not an only_in_copy entry. Declared as "extra" it
            # matched a line the copy also still carried at ios-sim-debug, so the
            # guard saw no drift while the leg silently used the iOS 26 ratchet.
            ("CN1_WARNING_LEG: ios-sim-debug-xcode27", "CN1_WARNING_LEG: ios-sim-debug"),
        ],
        # Lines the COPY must contain, checked before any substitution. The
        # subs list cannot do this job: it describes differences that are
        # ALLOWED, so a copy that quietly reverts to the original's value shows
        # up as no difference at all. That is not hypothetical -- the iOS 27 leg
        # carried the iOS 26 warning ratchet for two full runs, compared a 27
        # build against a 26 baseline, failed the census before taking a single
        # screenshot, and this guard reported the legs identical throughout.
        "required_in_copy": [
            "CN1_WARNING_LEG: ios-sim-debug-xcode27",
            "SCREENSHOT_REF_DIR: ${{ github.workspace }}/scripts/ios/screenshots-metal-27",
            # The simulator this leg captures on. Both lines, because each alone
            # is a silent fallback: no boot step and the destination names a
            # device that does not exist; no destination and run-ios-ui-tests.sh
            # asks for "iPhone 16" by name, finds none on the Xcode 27 image, and
            # captures on whatever it falls back to -- an iPad, the run that
            # exposed it. Declaring them as allowed extras only permitted them;
            # deleting both still read as "the legs match".
            "boot-ios-simulator.sh 'iOS-27[0-9-]*' 'iPhone 16'",
            "IOS_SIM_DESTINATION: 'platform=iOS Simulator,id=${{ steps.sim27.outputs.udid }}'",
        ],
        "only_in_copy": [
            "CN1_XCODE_MAJOR: '27'",
            "IOS_DEPENDENCY_ARGS: '-Dcodename1.arg.ios.themeGeneration=27'",
            "IOS_SIM_DESTINATION: 'platform=iOS Simulator,id=${{ steps.sim27.outputs.udid }}'",
        ],
        # The published port report is keyed on the port id `ios-metal` and there
        # is one iOS port; a second upload under that id would publish whichever
        # leg finished last.
        "only_in_original_steps": ["Upload iOS Metal port status"],
        # The 26 leg finds its iPhone 16 already on the image; the 27 image has
        # none, so this leg creates one rather than accepting a fallback device.
        "only_in_copy_steps": ["Boot an iPhone 16 on iOS 27"],
    },
    {
        "workflow": ".github/workflows/scripts-fidelity.yml",
        "original": "fidelity-ios-metal",
        "copy": "fidelity-ios-metal-27",
        "subs": [
            ("runs-on: xcode-27", "runs-on: macos-15"),
            ("name: Fidelity (iOS Modern 27, Metal)", "name: Fidelity (iOS Modern, Metal)"),
            ("'iOS-27[0-9-]*' 'iPhone 16' iPhone16-fidelity27",
             "'iOS-26[0-9-]*' 'iPhone 16' iPhone16-fidelity"),
            ("name: ios-fidelity-27", "name: ios-fidelity"),
            ("ios-27-metal-fidelity-baseline.json", "ios-26-metal-fidelity-baseline.json"),
        ],
        "required_in_copy": [
            "CN1SS_FIDELITY_GOLDEN_SET: 'ios-27-metal'",
        ],
        "only_in_copy": [
            "CN1SS_FIDELITY_GOLDEN_SET: 'ios-27-metal'",
            "CN1_XCODE_MAJOR: '27'",
        ],
        "only_in_original_steps": [],
        "only_in_copy_steps": [],
    },
]


def job_lines(text, job, path):
    """The body of one job, without its leading comment block."""
    lines = text.split("\n")
    start = None
    for i, line in enumerate(lines):
        if line == f"  {job}:":
            start = i
            break
    if start is None:
        sys.exit(f"check-duplicated-ci-legs: job '{job}' not found in {path}")
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
    failures = 0
    for pair in PAIRS:
        path = ROOT / pair["workflow"]
        text = path.read_text(encoding="utf-8")
        orig = normalise(job_lines(text, pair["original"], path), set(),
                         set(pair["only_in_original_steps"]))
        copy = normalise(job_lines(text, pair["copy"], path), set(pair["only_in_copy"]),
                         set(pair["only_in_copy_steps"]))
        # Requirements are checked against the UNFILTERED copy: only_in_copy
        # removes exactly the generation-specific lines a requirement is most
        # likely to name, so checking the filtered list would always report them
        # missing.
        #
        # Declared-extra STEPS are kept here too. A required line may live inside
        # one -- the iOS 27 leg's simulator boot is exactly that -- and checking
        # a list those steps were already removed from would report it missing
        # even when present, or, worse, let a requirement on it be dropped as
        # "unfindable".
        copy_raw = normalise(job_lines(text, pair["copy"], path), set(), set())
        if not orig or not copy:
            sys.exit(f"check-duplicated-ci-legs: {pair['copy']} or {pair['original']} "
                     f"parsed empty; the parser is wrong")
        missing = [r for r in pair.get("required_in_copy", [])
                   if not any(r in line for line in copy_raw)]
        if missing:
            failures += 1
            print(f"check-duplicated-ci-legs: FAILED -- {pair['copy']} is missing "
                  f"line(s) that make it a SEPARATE leg rather than a duplicate of "
                  f"{pair['original']}:")
            for m in missing:
                print(f"    {m}")
            print("  Without these the copy runs against the original's own "
                  "baselines/identity and silently validates nothing.")
            continue
        rewritten = []
        for line in copy:
            for old, new in pair["subs"]:
                line = line.replace(old, new)
            rewritten.append(line)
        if rewritten == orig:
            print(f"check-duplicated-ci-legs: {pair['original']} and {pair['copy']} "
                  f"match ({len(orig)} significant lines).")
            continue
        failures += 1
        import difflib
        print(f"check-duplicated-ci-legs: FAILED -- {pair['copy']} has drifted from "
              f"{pair['original']} in {pair['workflow']}.")
        print("If the difference is intended, add it to that pair's subs/only_in_* "
              "entry with a reason.")
        for line in difflib.unified_diff(orig, rewritten, lineterm="",
                                         fromfile=pair["original"], tofile=pair["copy"]):
            print(line)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
