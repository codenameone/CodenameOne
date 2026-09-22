# iOS 27 screenshot baselines

Reference images for the `build-ios-metal-27` job in
`.github/workflows/scripts-ios.yml`, which runs the same `scripts/hellocodenameone`
suite as `build-ios-metal` on the Xcode 27 / iOS 27 image.

Separate from `../screenshots-metal` on purpose: that set is the iOS 26 toolchain
we actually pin (`CN1_XCODE_MAJOR`), and it keeps gating what applications are
built with today. This set is what stops iOS 27 breaking us in the gap before
that pin moves.

## Provenance

Captured by `build-ios-metal-27` on the GitHub `xcode-27` image (macOS 27.0,
Xcode 27.0), on an **iPhone 16 running iOS 27.0** created by the job itself --
that image pre-creates iPhone 17 / 17e / 18 Pro / 18 Pro Max / Air and no
iPhone 16, so the leg makes one rather than accepting a fallback device. Same
model as the iOS 26 leg on purpose: the two sets then differ by runtime and
theme, not by hardware.

Reviewed before being accepted, against the iOS 26 set:

| check | result |
|---|---|
| completeness | 155 images, exact name parity with `../screenshots-metal` |
| divergence from iOS 26 | 154 of 155 differ (0.6% byte-identical) |
| the 40 modern-theme images | 40 of 40 differ |

The last row is the one that matters. Only the 21 `DualAppearanceBaseTest`
subclasses install the modern theme -- 40 of the 155 images -- and they are where
Liquid Glass lives. Had they matched iOS 26 byte for byte,
`ios.themeGeneration=27` would not have reached the app and this set would have
enshrined the wrong generation while looking complete.

The near-total divergence in the other 115 is expected and is not evidence of a
theme change: they render the legacy iOS 7 theme, and differ because the runtime
does (iOS 26.3 against 27.0 -- different SF font revision, different system
chrome). That is also why this is a full independent set rather than a sparse
overlay on `../screenshots-metal`; there is essentially nothing to share.

## The .tolerance sidecars

Copied from `../screenshots-metal`, one per test that is known not to render
byte-identically run to run -- vector and GPU-heavy screens with anti-aliasing
jitter, and the ones that draw live map or web content. They are the same 19
files and the same thresholds as the iOS 26 set, because the instability is a
property of what those screens draw, not of the OS version.

Omitting them is not a theoretical problem: this set's first gating run failed on
`SVGStatic` alone, at 0.108% of pixels and a maximum channel delta of 4 --
comfortably inside the 0.30% / 4 its sidecar already allows on the 26 leg. The
other 18 happened to match exactly that time and would have failed on some later
run instead.

Note this is the screenshot suite's mechanism, not the fidelity harness's:
`scripts/fidelity-app/goldens/README.md` rejects tolerance sidecars outright for
native references, and that difference is deliberate. A native reference defines
what a theme is aiming at and must be exact; these screens exercise a renderer
whose own output jitters.

## Updating afterwards

As for `../screenshots-metal`: run the job, pull the artifact, compare the
"different" tests side by side with the previous baseline, accept only what is
intentional, and commit.
