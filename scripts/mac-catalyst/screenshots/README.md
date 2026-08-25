# Mac Catalyst screenshot baselines

Reference images for the **legacy** Mac Catalyst build
(`codename1.arg.macNative.enabled=true`, target `mac-catalyst`). The
`build-mac-catalyst` job in
[`.github/workflows/scripts-mac-catalyst.yml`](../../../.github/workflows/scripts-mac-catalyst.yml)
compares `scripts/hellocodenameone` output against these PNGs via
[`scripts/run-mac-catalyst-ui-tests.sh`](../../run-mac-catalyst-ui-tests.sh).

The supported macOS baseline is [`../../macos/screenshots`](../../macos/screenshots),
captured from the native AppKit build. This directory is the record of the port
that came before it, kept green so a project that still depends on Catalyst
behaviour has a target that is actually tested.

## Scope

Mac Catalyst is not a macOS port. It is a slice of the iOS Xcode project: the
same `IPhoneBuilder` pipeline, the same UIKit, one extra SDK. The runtime stack
is the Metal backend forced on for that slice, so this golden set tracks the iOS
Metal baselines closely -- but not bit-identically, because:

- Mac windows are not notched or safe-area padded the way iPhone screens are, so
  layout-sensitive screens shift a few pixels.
- Text rendering on Catalyst applies a different default sub-pixel positioning
  policy than iOS.
- Tests that probe iOS-only APIs (SMS compose, the legacy AddressBookUI flow)
  short-circuit here and may produce different placeholder UI.

The `Window-*` family is the sharpest difference from the AppKit baseline, and
it is a difference in kind rather than degree. A Catalyst window is a
`UIWindowScene` whose Codename One content is rendered into an off-screen raster
and handed to the scene's view; an AppKit window owns a real `CAMetalLayer`. The
two will never converge, which is why they have separate baselines rather than a
shared one with a loose tolerance.

Treat this as the **Catalyst slice's own baseline**, evolving independently from
`../../macos/screenshots/` (native AppKit), `../../ios/screenshots/` (iOS OpenGL
ES) and `../../ios/screenshots-metal/` (iOS Metal).

## Updating

When a change is expected to modify a screenshot:

1. Run the CI `build-mac-catalyst` job, or locally:
   ```sh
   ./scripts/build-mac-catalyst-app.sh -q -DskipTests
   ./scripts/run-mac-catalyst-ui-tests.sh \
       scripts/hellocodenameone/ios/target/hellocodenameone-ios-1.0-SNAPSHOT-mac-catalyst-source/HelloCodenameOne.xcodeproj
   ```
   Requires Xcode 26 and `gem install xcodeproj`: the Catalyst path injects its
   build settings into an already-generated iOS project with a Ruby script, so
   the gem is a hard requirement here. The AppKit build needs neither.
2. Download the `mac-catalyst-ui-tests` artifact (or look under `artifacts/`
   locally) and pull the `*.png` files for tests that are now "different" or
   "new".
3. Inspect them side by side with the previous baseline. Accept only what is
   intentional -- "differs" means a real difference, not noise.
4. Copy the accepted PNGs into this directory and commit them, named after the
   test IDs.

## Tolerance sidecars

A `<TestName>.tolerance` file next to a golden loosens the comparison for that
test only. It is legitimate only for something that is nondeterministic *between
two runs of the same binary* -- live map tiles, clock text, an animation phase.
A permanent, reproducible rendering difference is a reason for a different
golden, never for a looser comparison.
