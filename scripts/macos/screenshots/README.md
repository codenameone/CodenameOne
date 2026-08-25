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
baseline, and it is green rather than red while it does: with zero references
every capture is reported as "new" and never as "differs", so the mismatch gate
cannot fire, and the count floor is the size of this directory -- zero -- so the
regression gate cannot fire either. `CN1SS_FAIL_ON_TEST_PROBLEMS` is on from the
first run regardless, because a test that crashes or never runs has to fail even
while the baseline is being seeded.

*Delete this section in the commit that adopts the baseline.*

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
- **A `Window-*` capture with no window chrome is a blocker.** It means the
  window manager is not producing a real `NSWindow`, which is the whole claim of
  this port.

## Tolerance sidecars

Start with none, and add one only when a test is shown to be nondeterministic
between two runs of the same binary. "AppKit rasterizes text differently from
Catalyst" is deterministic and permanent: the answer is this baseline, which is
what this directory is for.
