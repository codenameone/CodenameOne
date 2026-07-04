# Native theme coverage tracker

Tracks how completely the two shipped native themes -- **iOS Modern** (iOS 26
"Liquid Glass") and **Android Material** (Material 3) -- cover their native
platform's control set, and which components/features are still missing. Every
"covered" row is measured by the fidelity suite
(`scripts/fidelity-app/common/src/main/resources/fidelity-tests.yaml`): the CN1
widget under the native theme is diffed against the real native widget, per
state and per light/dark appearance, with a one-way ratchet gate
(`FidelityGate`) plus separate geometry metrics and, for the animated glass
effects, deterministic animation-frame validation (`MorphFrameValidator`).

Scores below are the min-max fidelity across the component's tested
states/appearances, taken from the committed ratchet baselines
(`scripts/fidelity-app/baseline/*.json`) -- the same numbers the CI gate
enforces. They are refreshed here whenever the baseline is deliberately
re-anchored, so this file and the gate can never disagree.

### What the scores do and do not claim

- The headline percentage is a TOLERANT overlay comparison: it absorbs small
  position/size drift and anti-aliasing differences. Geometry (bbox center
  offset, width/height ratios) is measured separately, gated by its own
  ratchet in `FidelityGate`, and flagged per-pair in the report's main table
  ("OFF" column) -- a high score with an OFF geometry flag means the pixels
  blend well but the widget is materially mis-sized/mis-placed. `TabOne` is
  the canonical example: 95.6-96.1%% overlay with width ratio 0.75 / height
  ratio 0.54 vs native (tracked, not yet fixed).
- Corner-radius agreement is reported but NOT gated: the estimator is stable
  to ~1px, the same range honest AA occupies.
- The `GlassPanel*` rows isolate the glass BLEND over four backdrops and are
  scored as `material: normal` by design (both sides render the same
  backdrop); they do not score masked glass optics on their own.
- Animation frames validate CN1 determinism and motion properties (travel,
  overshoot, lens size, tint timing) against COMMITTED CN1 frame goldens --
  they do NOT compare against native intermediate frames. Native motion is
  captured as video (`scripts/capture-native-*-video`) and parity is a manual
  review step; automated native-motion comparison is future work.

## iOS Modern (iOS 26 Liquid Glass)

### Covered components

| Native control | CN1 building block | Fidelity test | Score (min-max) | Notes |
|---|---|---|---:|---|
| UIButton .glass | `Button` | Button | 90.9-93.4 | frosted capsule, backdrop-filter glass |
| UIButton .prominentGlass | `RaisedButton` UIID | RaisedButton | 87.8-92.5 | geometry: ~10% wider than native (tracked) |
| UIButton .plain | `FlatButton` UIID | FlatButton | 86.7-88.2 | geometry: native pill radius 92px vs CN1 44px (tracked) |
| UITextField | `TextField` | TextField | 97.3-97.6 | |
| Check glyph (Reminders style) | `CheckBox` | CheckBox | 92.2-97.5 | SF Symbol glyphs (iosSFStateIconsBool); iOS has no native checkbox |
| Radio glyph | `RadioButton` | RadioButton | 92.2-95.5 | SF largecircle.fill.circle glyph |
| UISwitch | `Switch` | Switch | 92.1-96.8 | + liquid droplet thumb morph (frame-validated) |
| UISlider | `Slider` | Slider | 92.4-95.1 | |
| UIProgressView | `Slider` (ProgressBar UIID) | ProgressBar | 94.4-95.4 | |
| UITabBar (floating pill) | `Tabs` | Tabs | 84.8-86.4 | + selection-lens morph (frame-validated); residual = frost texture, worst iOS rows |
| UINavigationBar | Toolbar UIID bar | Toolbar | 87.6-87.7 | residual = frost texture; still bottom-quartile |
| UIAlertController (alert) | Dialog UIID card | Dialog | 97.0-97.1 | |
| UIPickerView | `GenericSpinner` | Spinner | 91.5-91.8 | whole-row perspective; CN1 wheel wraps short models (native does not); dark off-row contrast tracked |
| UIVisualEffectView / UIGlassEffect | GlassPanel UIID | GlassPanel{Grey,Red,Grad,Photo} | 96.1-98.6 | glass-blend isolation over 4 backdrops (see scope note above) |

Isolation/ladder cases (not user-facing components): TabOne 95.6-96.1
(geometry OFF: w 0.75 / h 0.54 -- see scope note), TabsGeom 93.0-93.5,
GlassText/GlassIcon 98.6-98.7.

Animated glass (validated per-frame at fixed progress, no native golden):
TabsMorph (selection lens: travel, overshoot, lens size, tint timing),
SwitchMorph (droplet stretch/squash).

### Missing components (to reach a complete theme)

| Native control | Suggested CN1 building block | Status |
|---|---|---|
| UISegmentedControl | ButtonGroup / Tabs pill variant | not started |
| UIStepper | Stepper composite (2 glass buttons) | not started |
| UISearchBar / searchable nav | Toolbar search mode | not started |
| UIActivityIndicatorView | InfiniteProgress | not started (UIID exists, untested) |
| UIPageControl | Tabs page indicator | not started |
| UIDatePicker (wheels) | Picker (date/time spinner) | partially themed (DateSpinner UIIDs), not in suite |
| UIDatePicker (calendar) | Calendar | not started |
| UIMenu / context menu | ActionSheet / Command menu | not started |
| Action sheet (bottom) | Sheet / ActionSheet | not started |
| Bottom sheet (detents) | Sheet | not started |
| UITableView cell chrome | MultiButton / list rows | not started |
| Toast / HUD | ToastBar | not started |
| Pull-to-refresh spinner | pull-to-refresh (themed) | not started |
| Large-title navigation bar | Toolbar large-title mode | not started |
| Tab bar badge | Tabs badge | not started |
| UISlider liquid thumb morph | Slider droplet (reuse SwitchThumbDroplet) | planned (task tracked) |

### Known visual gaps (tracked, honest list)

- iOS `Tabs`/`Toolbar` frost texture: the two worst iOS families; theme knobs
  are at measured optima and two material-level tweaks (saturation, edge
  feather) measured flat -- closing this requires a closer reproduction of the
  native Liquid Glass material in the Metal patch, not tuning.
- `TabOne` geometry (w 0.75 / h 0.54 vs native) despite its high overlay score.
- iOS `Spinner` dark: off-row text contrast is low (uniform ~0.32 fade matches
  the native tone but the dark-sheet contrast is tracked for another pass).
- Android `ProgressBar`: ~1.5x native track height (geometry-tracked).
- Android disabled dark `Button`: lower contrast than native.
- Android FAB/switch small geometry deltas (geometry-tracked).

### Feature-level gaps

- Live glass while scrolling: composed-patch cache recomposes per frame when
  the backdrop moves (policy documented in `Component.internalPaintImpl` and
  the METALView glass patch cache); a pure-GPU two-pass material (like the
  selection lens shader) is the tracked follow-up.
- Tab icons: CN1 renders Apple SF Symbols on iOS (`FontImage.createSFOrMaterial`);
  a handful of glyphs still differ from the exact native weights.
- RTL mirroring of the glass morphs is untested.

## Android Material (Material 3)

### Covered components

| Native control | Fidelity test | Score (min-max) |
|---|---|---:|
| MaterialButton (filled) | Button | 92.6-96.8 |
| MaterialButton (tonal) | RaisedButton | 95.2-97.3 |
| MaterialButton (outlined) | FlatButton | 91.2-93.8 |
| TextInputLayout | TextField | 96.2-97.6 |
| MaterialCheckBox | CheckBox | 94.7-95.4 |
| MaterialRadioButton | RadioButton | 94.5-95.5 |
| MaterialSwitch | Switch | 95.4-96.4 |
| Slider | Slider | 98.4-99.6 |
| LinearProgressIndicator | ProgressBar | 96.9-97.3 |
| TabLayout | Tabs | 92.3-95.2 |
| MaterialToolbar | Toolbar | 95.1-98.7 |
| MaterialAlertDialog | Dialog | 95.7-95.8 |
| FloatingActionButton | FloatingActionButton | 94.4-97.1 |

### Missing components

| Native control | Suggested CN1 building block | Status |
|---|---|---|
| SegmentedButton (single/multi) | ButtonGroup | not started |
| Chips (assist/filter/input) | Button chip UIIDs | not started |
| NavigationBar (bottom) | Tabs bottom mode | not started (suite tests TabLayout only) |
| NavigationDrawer | Toolbar side menu | not started |
| Top app bar variants (center/medium/large) | Toolbar variants | not started |
| Snackbar | ToastBar | not started |
| BottomSheet | Sheet | not started |
| DatePicker / TimePicker dialogs | Picker | not started |
| Badge | Tabs/Button badge | not started |
| SearchBar / SearchView | Toolbar search mode | not started |
| CircularProgressIndicator | InfiniteProgress | not started |
| Range slider | Slider (range mode) | not started |
| Menu / ExposedDropdown | ComboBox / Command menu | not started |
| Card / ElevatedCard | Container card UIIDs | not started |

## How to add a component

1. Add the YAML entry (`fidelity-tests.yaml`): id, `material:` intent, native
   widget key(s), states.
2. Implement the native reference in the standalone capture apps and run them
   LOCALLY (references are committed, never generated by CI): `NativeRef.swift`
   + `scripts/build-ios-native-ref.sh` on a simulator runtime matching the
   golden set; `android-native-ref/` (`RefWidgets.java`) +
   `scripts/build-android-native-ref.sh` on the CI emulator profile.
3. Add the CN1 build case in `Cn1WidgetRenderer`.
4. Tune the theme (`native-themes/*/theme.css`), regenerate the shipped `.res`
   with `scripts/build-native-themes.sh`, and let the ratchet record the new
   baseline.

When a new OS design generation arrives (iOS 27, the next Material), do NOT
overwrite the existing golden sets: capture a new set
(`CN1SS_FIDELITY_GOLDEN_SET=ios-27-metal`), add the theme variant and a CI
matrix row pinned to a runner with that runtime, and gate both looks side by
side until the old one is deliberately retired.
