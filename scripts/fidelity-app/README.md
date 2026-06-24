# Native theme fidelity suite

Measures how close Codename One's **native themes** (`iOSModernTheme`,
`AndroidMaterialTheme`) render compared to the **real native OS widgets**, so the
themes can be driven toward 99-100% fidelity. It is a different kind of test from
the `hellocodenameone` CN1SS suite: that one asserts CN1 output is pixel-identical
to a *stored CN1 golden*; this one measures the visual *similarity* between CN1's
render of a component and the *real native widget* (UIKit / Material).

## How it works

For every component with a native equivalent, for every state
(normal/pressed/disabled/selected) and appearance (light/dark), the on-device
runner produces two identically-sized tiles. Both the native widget and the CN1
component are anchored **top-left at their natural (preferred) size** in the
tile -- laid out identically -- so the comparison is fair and a genuine
size/extent difference shows up as a real fidelity gap rather than a harness
artifact:

1. **CN1 tile** -- the CN1 component under the native theme, captured via
   `Display.screenshot()` and cropped to the tile.
2. **Native tile** -- the REAL native widget (`NativeWidgetFactory`), rasterized
   off-screen to PNG bytes (Android `View.draw`, iOS `CALayer renderInContext`).

The host then scores each pair with a **structure-aware perceptual metric**
(`ProcessScreenshots --mode fidelity`). A naive per-pixel colour delta is useless
here: tiles are mostly background, and CN1 fills are near-white, so two widgets
that look nothing alike used to score 95%. Instead the score is the geometric
mean of two factors, so BOTH must be high:

- **shape_sim** -- each widget is cropped to its content bounding box and
  normalized onto a common 64x64 canvas, then compared by colour. This asks "is
  it the same kind of widget, styled the same?" independent of size/position (so
  a few-pixel shift does not tank it).
- **size_agreement** -- the ratio of the two content bounding-box dimensions:
  "is it the same size?" (a CN1 radio rendered 1.5x larger than Material's is a
  real but partial gap).

`fidelity_percent = 100 * sqrt(shape_sim * size_agreement)`. A recognizably
similar widget at a different size scores in the middle (~60-80%); a genuinely
different one scores low; an identical one scores 100. (`ssim` and
`mean_channel_delta` are still emitted as diagnostics.) `RenderFidelityReport`
renders the side-by-side report; `FidelityGate` enforces a one-way ratchet
(fidelity may not drop below the committed baseline minus an epsilon);
`FidelityComposite` renders the **visual fidelity cards** -- one PNG per
component+state showing the native widget (left) next to the CN1 render (right)
for each appearance with the fidelity % beside each pair, plus a single
`fidelity-overview.png` contact sheet. Cards are generated automatically by every
run and land in `artifacts/<platform>-fidelity/cards/` (uploaded by CI). They are
the human-readable "where do the themes stand" guide, and they make degenerate raw
tiles legible -- e.g. a Material progress bar is a thin line on a near-black dark
tile, so its raw PNG reads as solid black, but the card frames and scores it.

### Why the comparison is same-run (environment robustness)

The CN1 tile and the native tile are produced **in the same environment** every
run. Native widgets render slightly differently across environments (emulator
GPU, OS version, font hinting), so a native golden captured on one machine would
not match a CN1 render on another. Comparing same-environment renders makes the
*score* portable. The committed `goldens/` are re-seedable drift artifacts for
human review (`FIDELITY_UPDATE_GOLDENS=1`); the committed `baseline/` holds the
expected scores the ratchet gates against (`FIDELITY_UPDATE_BASELINE=1` to
re-record, a deliberate, reviewed action).

Native renders are snapped to their final state before rasterizing
(`jumpDrawablesToCurrentState` on Android) so the off-screen capture is
deterministic run-to-run.

## Layout

```
common/   CN1 app: FidelityApp, FidelityDeviceRunner, Cn1WidgetRenderer,
          NativeWidgetFactory, spec parser, fidelity-tests.yaml
ios/      Objective-C NativeWidgetFactory impl (UIKit)
android/  Java NativeWidgetFactory impl (Material 3)
goldens/  committed per-env native reference PNGs (drift artifact)
baseline/ committed per-platform expected fidelity scores (gated)
tools/    fidelity-stats.py -- summarize a baseline into Markdown
```

The component matrix is data-driven in `common/src/main/resources/fidelity-tests.yaml`.

## Running locally

```bash
# Android (emulator must be booted)
./scripts/build-fidelity-app.sh android        # or drive android-source + gradle directly
./scripts/run-android-fidelity-tests.sh <gradle_project_dir>

# iOS (Metal pipeline; simulator must be booted)
./scripts/build-fidelity-app.sh ios
xcodebuild -workspace <FidelityApp.xcworkspace> -scheme FidelityApp \
  -sdk iphonesimulator -destination "id=<UDID>" ARCHS=arm64 ONLY_ACTIVE_ARCH=YES build
./scripts/run-ios-fidelity-tests.sh <FidelityApp.app> <UDID>
```

To re-seed in a fresh environment:
`FIDELITY_UPDATE_GOLDENS=1 FIDELITY_UPDATE_BASELINE=1 ./scripts/run-...`.

CI runs both platforms in `.github/workflows/scripts-fidelity.yml`, gating each
PR that touches the native themes, the app, or the renderers.

## Current standing

See `tools/fidelity-stats.py` for the live summary
(`python3 scripts/fidelity-app/tools/fidelity-stats.py`).

### Android (Material 3) -- complete, verified

44 component/state/appearance pairs, run on an API-34 emulator, with the
top-left-anchored harness and the structure-aware metric:

- **Overall mean ~48%** -- an honest figure. (An earlier color-delta metric
  reported a meaningless 82.6%: it was mostly measuring "both tiles share a
  background colour", and scored a small outlined native field vs a full-width
  CN1 field at 94.6%. The structure-aware metric scores that pair at ~40%.)
- **Per component (mean):** Switch ~81%, RaisedButton ~70%, Button ~67%,
  RadioButton ~61%, CheckBox ~60%, FlatButton ~57%, TextField ~39%, Slider ~13%,
  ProgressBar ~0%.
- **Real findings it surfaces:** CN1's radio/checkbox render ~1.5x larger than
  Material; CN1 TextField defaults to a full-width filled field vs Material's
  narrow outlined one; CN1 Slider/ProgressBar differ markedly from Material; the
  native Material progress indicator barely renders at this tile size (a
  per-component capture issue to tune). Switch is the closest match.
- The off-screen native raster is deterministic run-to-run after
  `jumpDrawablesToCurrentState` (a real bug the gate caught and we fixed).

### iOS (Modern theme, Metal) -- CN1 side working; native reference blocked

- The **CN1 render pipeline works end-to-end on the Metal simulator**: all 44
  CN1 tiles render under `iOSModernTheme` and ship over the WebSocket; the suite
  completes cleanly (`CN1SS:SUITE:FINISHED`). iOS CN1 tiles are captured at
  Retina resolution (e.g. 1087x254 px for a 60x14mm tile).
- The **native UIKit reference capture is blocked** by a ParparVM bridge issue:
  the native `renderWidgetPng` (returning `byte[]`) executes its Objective-C
  body (confirmed via NSLog) but the call surfaces a `NullPointerException` for a
  deterministic subset of widget kinds (`ios_uiswitch`, `ios_uislider`,
  `ios_uiprogress`, the SF-symbol check/radio) while `ios_uibutton_*` /
  `ios_uitextfield` return empty. `bool`-returning methods (`isSupported`) work;
  only the array-returning path fails. UIKit must run on the iOS main thread,
  but CN1's EDT is a separate thread, and neither calling from the EDT nor a
  `dispatch_sync(main)` hop from it has produced delivered bytes. ParparVM does
  not populate `getStackTrace()`, so the exact failing line is not yet pinned.
- **Recommended next approach for iOS:** instead of a `byte[]`-returning native
  interface, wrap the native `UIView` in a CN1 `PeerComponent` and capture it via
  `Display.screenshot()` cropped to the peer bounds. On iOS,
  `cn1_captureView` (`Ports/iOSPort/nativeSources/IOSNative.m`) already composites
  peer `UIView`s into the screenshot via `drawViewHierarchyInRect:`, so this runs
  entirely on CN1's own main-thread capture path and sidesteps the array-return
  bridge. (Android stays on the off-screen `renderViewOnBitmap` path, which is
  reliable there.)

These numbers are the baseline the theme work drives upward.
