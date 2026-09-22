# iOS 27 screenshot baselines

Reference images for the `build-ios-metal-27` job in
`.github/workflows/scripts-ios.yml`, which runs the same `scripts/hellocodenameone`
suite as `build-ios-metal` on the Xcode 27 / iOS 27 image.

Separate from `../screenshots-metal` on purpose: that set is the iOS 26 toolchain
we actually pin (`CN1_XCODE_MAJOR`), and it keeps gating what applications are
built with today. This set is what stops iOS 27 breaking us in the gap before
that pin moves.

## Currently empty, and what fills it

Nothing here yet. These images can only come from a run on the 27 runner --
capturing them on a maintainer's machine would record that machine's toolchain
rather than the one the gate uses, which is the failure
`scripts/lib/xcode.sh` exists to prevent.

So the first run is a SEEDING run, and its screenshot step is marked
`continue-on-error` for exactly that reason:

1. Let `build-ios-metal-27` run once.
2. Download its `ios-ui-tests-metal-27` artifact.
3. **Look at every frame**, not the count. helloworld installs the modern theme
   itself and this leg builds it with `ios.themeGeneration=27`, so these should
   show the iOS 27 Liquid Glass surfaces -- if they look like the iOS 26 set, the
   generation did not reach the app and the baseline would enshrine that.
4. Commit the accepted PNGs here, named after the test IDs.
5. Remove the `continue-on-error` from that step, so the leg starts gating.

Until step 5 the job reports and never fails, which means it is not yet a gate.

## Updating afterwards

As for `../screenshots-metal`: run the job, pull the artifact, compare the
"different" tests side by side with the previous baseline, accept only what is
intentional, and commit.
