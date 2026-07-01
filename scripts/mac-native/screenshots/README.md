# Mac native screenshot baselines

Reference images for the Mac native build (`codename1.arg.macNative.enabled=true`). The `build-mac-native` job in [`.github/workflows/scripts-ios.yml`](../../../.github/workflows/scripts-ios.yml) compares `scripts/hellocodenameone` Mac Catalyst output against these PNGs via [`scripts/run-mac-native-ui-tests.sh`](../../run-mac-native-ui-tests.sh).

## Scope

The Mac native build is produced by the same `IPhoneBuilder` pipeline as iOS, via Mac Catalyst under the hood (implementation detail; the user-facing build hint is `macNative.*`). The runtime stack is the Metal rendering backend forced on for the Mac slice, so this golden set is expected to track the iOS Metal baselines closely — but not bit-identically, because:

- Mac windows aren't notched / safe-area-padded the way iPhone screens are, so layout-sensitive screens shift a few pixels.
- AppKit-backed font rendering on Catalyst applies a different default sub-pixel positioning policy than iOS.
- Some tests that probe iOS-only APIs (e.g., SMS compose, the legacy AddressBookUI flow) short-circuit on Mac and may produce different placeholder UI.

Treat this as the **Mac slice's own baseline**, evolving independently from `../ios/screenshots/` (the OpenGL ES iOS reference set) and `../ios/screenshots-metal/` (the iOS Metal reference set).

## Updating

When a Mac-side change is expected to modify a screenshot:

1. Run the CI `build-mac-native` job, or locally:
   ```sh
   ./scripts/build-mac-native-app.sh -q -DskipTests
   ./scripts/run-mac-native-ui-tests.sh \
       scripts/hellocodenameone/ios/target/hellocodenameone-ios-1.0-SNAPSHOT-mac-source/HelloCodenameOne.xcodeproj
   ```
2. Download the `mac-native-ui-tests` artifact (or look under `artifacts/` locally) and pull the `*.png` files for tests that are now "different" or "new".
3. Inspect them side-by-side with any previous baseline. Accept only what's intentional.
4. Copy the accepted PNGs into this directory and commit them, naming them after the test IDs (same names as in `../ios/screenshots/`).

## Initial state

This directory ships empty so the first CI run can establish the baseline. Until goldens are populated, all tests will report as "new" in the PR comment but the run won't fail (the `CN1SS_MIN_SCREENSHOTS` guard defaults to 0 here, vs the iOS pipeline's ~30). Bump that env var in the workflow once a stable baseline is in place.
