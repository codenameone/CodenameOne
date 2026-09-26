# Tab-motion probe: measuring the native iOS tab bar

An instrumented native app that measures what a real `UITabBarController` does,
so Codename One can reproduce it from numbers instead of by eye. It is the source
of two things in the core:

- `com.codename1.ui.TabGlassMotion` -- the Liquid Glass selection motion (lens
  travel, lift, deformation, platter fade, whole-bar pulse, touch glow);
- `com.codename1.ui.TabGlassGesture` -- what depends on the gesture rather than on
  a tap: holding the press, scrubbing the lens with the finger, the release;
- `com.codename1.ui.plaf.VibrancyMatrix` -- the colour matrix of vibrant content
  (the tab icons and labels) as a function of the tint;
- `GlassRecipe.liquidPill27` -- the tab bar's glass material.

`TabGlassMotionTest`, `TabGlassGestureTest` and `VibrancyMatrixTest` hold the Java
to the capture, using the fixtures in
`maven/core-unittests/src/test/resources/tab-glass-motion/` (`native-ios27.csv`,
`native-gestures.csv`, `native-vibrancy.csv`).

## What it records

**Motion.** UIKit plays the full selection only for genuine touches on a
controller-managed bar, so the XCUITest bundle in `UITests/` taps the app. On every
display frame the app logs the presentation geometry of every layer under the tab
bar -- window frame, bounds, position, transform, opacity, filters -- plus touch
timestamps. UIKit attaches no `CAAnimation` for this motion (only `CAMatch*`
animations that make the lens copies follow each other); it writes the layer values
itself each frame, so the frame log is the motion.

**Material.** Lossless `simctl io screenshot`s of the resting bar over backdrops
whose every pixel is known: the shared `glass-backdrop.png`, sharp 23 pt colour
bands with 1 pt black rulers, and flat 50% grey. Do not fit the material from a
screen recording: its h264 smears exactly the detail the blur radius is read from.

**Frames** (optional, `PROBE_SNAP=1`). Lossless snapshots of the bar region on every
frame for 1.4 s after each touch, for side-by-side visual comparison with the CN1
frames. Snapshotting costs a frame's worth of time, and UIKit steps this motion per
frame, so a snapshot run's TIMING is distorted: compare snapshots by motion state
(travel and lift, read from the same run's log), never by clock time, and never fit
the model from a snapshot run.

## Running it

```bash
scripts/probe-ios-tab-motion.sh [out_dir] [simulator_udid]
```

It resolves an iPhone 16 simulator of the golden set's generation
(`CN1SS_FIDELITY_GOLDEN_SET`, default `ios-27-metal`), builds this project with
xcodegen, runs tap-driven captures in light and dark on a 3-tab bar and in light on
a 5-tab bar, then the hold and drag captures (`hold-light.log`, `drag-light.log`,
`dragA-light.log` .. `dragC-light.log`, `dragD-dark.log`), then takes the material
screenshots.

Every capture is one XCUITest run driven by `PROBE_SEQ` (`UITests/TapTests.swift`),
a comma separated list of gestures played two seconds apart:

| Token | Gesture |
|---|---|
| `N` | tap tab `N` |
| `hN:MS` | press and hold tab `N` for `MS` milliseconds |
| `dA-B:MS[:H]` | press tab `A` for 0.3 s, drag to tab `B` over `MS` ms, hold `H` ms, release |
| `dA-B.F:MS:H` | as above, but stop at fraction `F` of the way from `A` to `B` |

For example the hold capture is `h1:1500,h1:1500,1,h2:3000`: two 1.5 s holds of
tab 1, a quick tap on it once it is selected (nothing travels, so the release
wobble is seen alone), and a 3 s hold of tab 2.

**Vibrancy** is read, not rendered: with `PROBE_TINTSWEEP=<file>` (one `RRGGBB` per
line) the app sets each colour as the tab bar tint, 0.25 s apart, and logs the
`vibrantColorMatrix` filters UIKit installs on the item layers as `TINT` lines,
then `TINTDONE`. The first matrix of a line is the selected item's. The app reads
the variable from its own environment, so launch it directly (as the material
screenshots do, with `SIMCTL_CHILD_PROBE_TINTSWEEP`, `SIMCTL_CHILD_PROBE_APPEARANCE`
and `SIMCTL_CHILD_PROBE_LOG`) and collect `Documents/<PROBE_LOG>` from the app
container once it holds `TINTDONE`.

The tint lists the committed tables were measured with are in `tints/`, one file
per log named in the `vibrancy` command below (`sweep.txt` -> `sweep-light.log`
and `sweep-dark.log`, and so on). A simulator app can read a host path, so pass the
list's absolute path.

## Regenerating the model

`scripts/fidelity-app/tools/tab-motion/tabmotion.py` (needs numpy and scipy, plus
Pillow for `material`). Every command that writes Java rewrites only the generated
tables, so the hand-written code around them survives.

```bash
T=scripts/fidelity-app/tools/tab-motion/tabmotion.py
O=artifacts/tab-motion-probe
R=maven/core-unittests/src/test/resources/tab-glass-motion
V=artifacts/tint-sweeps                           # PROBE_TINTSWEEP logs, see above

# Tap-driven selection (TabGlassMotion)
python3 $T fit $O/light-grey-3tabs.log $O/dark-grey-3tabs.log $O/light-grey-5tabs.log -o $O/tpl.json
python3 $T check $O/tpl.json $O/*grey-*tabs.log   # worst per-tap error of the fitted model
python3 $T java $O/tpl.json CodenameOne/src/com/codename1/ui/TabGlassMotion.java
python3 $T fixture $O/light-grey-3tabs.log $O/dark-grey-3tabs.log $O/light-grey-5tabs.log -o $R/native-ios27.csv
python3 $T material $O                             # prints the GlassRecipe numbers

# Holds and scrubs (TabGlassGesture)
python3 $T springs --press $O/hold-light.log $O/drag-light.log $O/*grey-*tabs.log \
    --drag $O/drag-light.log                       # prints the spring constants
python3 $T gesture --hold $O/hold-light.log --templates $O/tpl.json \
    --drags $O/drag-light.log $O/dragA-light.log $O/dragB-light.log $O/dragC-light.log $O/dragD-dark.log \
    -o CodenameOne/src/com/codename1/ui/TabGlassGesture.java
python3 $T gesture-fixture $O/hold-light.log $O/dragA-light.log $O/dragC-light.log $O/dragD-dark.log \
    -o $R/native-gestures.csv

# Vibrancy (VibrancyMatrix), from PROBE_TINTSWEEP logs
python3 $T vibrancy --light $V/adapt-light.log $V/cvw-light.log $V/lowc-light.log $V/grid17-light.log \
    $V/slices-light.log $V/sweep3-light.log $V/sweep-light.log $V/sweep2-light.log \
    --grey $V/sweep-dark.log --dark-check $V/sweep2-dark.log \
    -o CodenameOne/src/com/codename1/ui/plaf/VibrancyMatrix.java
python3 $T vibrancy-fixture --dark $V/sweep2-dark.log --light $V/sweep2-light.log \
    --grey-dark $V/sweep-dark.log --grey-light $V/sweep-light.log -o $R/native-vibrancy.csv
```

`fit` uses only clean taps: not the first tap of a run (it lags a frame), a quick
~66 ms press (a longer press grows the bar pulse further), no frame hitch in the
first frames, and the fast lift.

`gesture` needs the `fit` output because the settle after a scrub is expressed on
the tap's position curve. It prints what it fitted along the way:

- the **finger offset** of each drag log -- the finger's window x minus the lens
  target in the lens parent's coordinates -- least-squares fitted with the follow
  spring over every press of the log and rounded to 0.01 pt (59.55 to 59.71 pt on
  the 3-tab bar, 21.12 pt on the 5-tab bar);
- the **release wobble**, read off the quickest press of the hold log over 45
  frames from 0.9 s after touch-up, and its difference from the wobble after each
  long hold;
- the **settle**, a least-squares fit over every release that travels more than
  3 pt, with its leave-one-out error;
- the **scrub deformation kernels** -- 60 frames of delay over the lens's own speed
  and acceleration, ridge 1e-2, the wobble subtracted first -- with their
  leave-one-log-out error. A press whose lens moves more than 3 pt in its first
  0.25 s started on another tab and plays a tap first; it is left out.

`vibrancy` fits, per light tint, the one `alpha` of the light structure below that
reproduces the logged matrix, then least-squares fits the trilinear
(chroma, value, hue) table to all of them with second-difference smoothing (1e-3).
The grey ramp is read directly from the grey tints of `--grey`. It prints the dark
formula's error over `--dark-check` and the table's error over every light tint.

## What the capture showed (iOS 27.0, iPhone 16)

Taps:

- The lens centre follows ONE normalized curve for every jump distance: at the
  target after ~0.33 s, a 0.6% overshoot at ~0.38 s.
- On touch the lens lifts: its bounds grow 16 pt both ways, the grey platter fades
  out (clear glass) and the accent copy of the tabs seen through it magnifies by
  16%. The lift is released once the lens is within 3.5 pt of its target, but not
  before it has snapped to full.
- Deformation is a distance-independent stretch pulse plus an arrival squash whose
  depth is linear in the distance, plus a small second wobble about a second in.
- The whole bar -- glass, content and lens -- scales about its centre, adding
  ~8.7 pt to its width at 0.13 s and undershooting slightly.
- The accent colour lives in a second copy of the tab content (`SelectedContentView`)
  revealed only through the lens; the normal content has a lens-shaped hole.
- A second lift regime: on a 5-tab bar UIKit plays a slower lift rise for some
  jumps -- the same six of the eight scripted taps in two independent captures, so
  it is deterministic, but what selects it is not identified. Travel and
  deformation are unchanged; the model follows the fast lift every 3-tab tap uses.

Holds and scrubs:

- A held press keeps the lift for as long as the finger is down.
- The whole-bar growth is a spring toward +14.58 pt of width while pressed and
  back to 0 on release: omega 17.63 rad/s, zeta 0.593, 20 ms behind the touch. A
  quick tap is the same spring released early.
- While dragging, the lens follows the finger on a stiffer spring: omega 32 rad/s,
  zeta 0.903, 20 ms behind, within the first and last tab.
- Releasing a scrub selects the tab under the FINGER, not the one nearest the
  lens, which can be far behind after a flick.
- The settle to that tab is the tap position curve from the release point, plus a
  term linear in the lens velocity at release and one linear in its follow lag
  (finger target - lens) at release.
- Every release -- tap, hold or scrub -- ends in the same small wobble starting
  ~0.95 s after touch-up, independent of the gesture.
- While scrubbing, the lens deforms with its own motion: scaleX/scaleY with its
  speed and acceleration, its centre offset with its velocity and acceleration.

Vibrancy and material:

- A vibrant glyph is a colour matrix applied to what lies behind it, chosen by the
  tint. In dark the structure is exact (`M = e I + (0.5 - e) 1 t^T / |t|^2`,
  `offset = t`, `e = 0.5 min(t) / max(t)`) and reproduces 1200 held-out tints to
  1e-5. In light the structure is exact up to one scalar `alpha` per tint, which
  has no closed form that survived testing and is tabulated. Grey tints use a
  diagonal gain and offset ramp, the same in both appearances.
- The selection platter is a colour matrix of the bar's glass, not a fill of its
  own.
