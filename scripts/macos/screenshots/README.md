# macOS screenshot baselines

Reference images for the native macOS (AppKit) build, target `mac-os-x-native`.
The `build-macos` job in
[`.github/workflows/scripts-macos.yml`](../../../.github/workflows/scripts-macos.yml)
compares `scripts/hellocodenameone` output against these PNGs via
[`scripts/run-macos-ui-tests.sh`](../../run-macos-ui-tests.sh).

The legacy Mac Catalyst baseline is
[`../../mac-catalyst/screenshots`](../../mac-catalyst/screenshots).

## Scope

This is a different port from Catalyst, not a newer build of it, and the images
differ accordingly. Expect:

- A real `NSWindow` title bar, with real traffic lights, rather than the frame
  Catalyst puts around a `UIWindowScene`.
- AppKit text rasterization and sub-pixel positioning.
- A backing scale that comes from `NSScreen`, so frame dimensions can differ
  from the Catalyst set outright.
- Scrollbar chrome drawn by AppKit conventions.

The `Window-*` family differs in kind rather than degree. A Catalyst window
renders into an off-screen raster that is handed to the scene's view; here every
window owns its own `CAMetalLayer` and is a real second GPU surface. That is the
point of the port, and it is why the two baselines are separate rather than one
set with a loose tolerance.

## Initial state

**This directory ships with no goldens.** The first CI run establishes the
baseline, and `scripts-macos.yml` sets `CN1SS_SKIP_COUNT_CHECK=1` so that run
can be green while it does.

That bypass is required, not belt and braces. `scripts/lib/cn1ss.sh` has three
guards, and only two of them are inert without goldens: with zero references
every capture is reported as "new" rather than "differs" so the mismatch gate
cannot fire, and the count floor is this directory's size -- zero -- so the
regression gate cannot fire. The third one can: a capture with no committed
golden is `missing_expected` and exits 18, tolerating
`CN1SS_ALLOWED_MISSING_EXPECTED` (default 0). It exists so a new test's golden
cannot be left unintegrated, and while this directory is empty it matches
*every* capture.

`CN1SS_FAIL_ON_TEST_PROBLEMS` stays on from the first run regardless, because a
test that crashes or never runs has to fail even while the baseline is being
seeded.

*Delete this section, and the `CN1SS_SKIP_COUNT_CHECK` line in
`.github/workflows/scripts-macos.yml`, in the commit that adopts the baseline.*

## Adopting the first baseline

1. Run the `build-macos` job and download the `macos-ui-tests` artifact.
2. Review every capture against the rules below.
3. Commit the accepted PNGs in one commit that names the run that produced them.
4. Run the suite a second time. It must be strict-green with zero differences.
   Anything else means the port is nondeterministic, and the fix is the port or a
   pinned window geometry -- not a tolerance file.

## Review rules

Open each new golden beside its counterpart in `../../mac-catalyst/screenshots/`
as a **review aid, never as a source**. Then:

- **Layout differences are bugs.** A widget that is shifted, clipped, wrapped
  differently or missing is a defect in this port. Fix the port; do not adopt
  the frame.
- **Rendering differences are expected and adopted.** AppKit text rasterization,
  the real title bar, AppKit scrollbars, and backing-scale-driven size changes
  are what a native port looks like.
- **A `Window-*` capture the same size as the requested window is a blocker.**
  It means the window manager is not producing a real `NSWindow`, which is the
  whole claim of this port.

  Not "a capture with no chrome in it", which is how this read at first and is
  wrong: `Window.capture()` returns the CONTENT view, so no run will ever show a
  title bar inside the frame. The evidence for real chrome is arithmetic --
  requesting 400x300 and getting 400x272 back means 28 points went somewhere, and
  28 points is a standard macOS title bar. Content equal to the requested size is
  what says there is no chrome at all.

  `WindowHostTest` allows `CHROME_ALLOWANCE = 64` below the request, so an inset
  anywhere in that range is within tolerance and is not by itself a finding. What
  matters is that it is the SAME on every run, and step 3 below is what proves
  that.

## Tolerance sidecars

Start with none, and add one only when a test is shown to be nondeterministic
between two runs of the same binary. "AppKit rasterizes text differently from
Catalyst" is deterministic and permanent: the answer is this baseline, which is
what this directory is for.
