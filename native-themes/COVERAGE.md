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
states/appearances. iOS numbers are from the current branch artifact set;
Android numbers are the committed CI baseline. Both are refreshed by the
`scripts-fidelity` workflow on every run.

## iOS Modern (iOS 26 Liquid Glass)

### Covered components

| Native control | CN1 building block | Fidelity test | Score (min-max) | Notes |
|---|---|---|---:|---|
| UIButton .glass | `Button` | Button | 90.9-93.4 | frosted capsule, backdrop-filter glass |
| UIButton .prominentGlass | `RaisedButton` UIID | RaisedButton | 87.8-92.5 | geometry: ~10% wider than native (tracked) |
| UIButton .plain | `FlatButton` UIID | FlatButton | 86.2-86.5 | geometry: native pill radius 92px vs CN1 44px (tracked) |
| UITextField | `TextField` | TextField | 97.3-97.6 | |
| Check glyph (Reminders style) | `CheckBox` | CheckBox | 89.8-95.6 | iOS has no native checkbox; compared glyph-to-glyph |
| Radio glyph | `RadioButton` | RadioButton | 87.8-92.8 | |
| UISwitch | `Switch` | Switch | 85.4-96.2 | + liquid droplet thumb morph (frame-validated) |
| UISlider | `Slider` | Slider | 92.4-95.1 | |
| UIProgressView | `Slider` (ProgressBar UIID) | ProgressBar | 94.4-95.4 | |
| UITabBar (floating pill) | `Tabs` | Tabs | 84.7-85.7 | + selection-lens morph (frame-validated); SF-vs-Material glyph gap dominates |
| UINavigationBar | Toolbar UIID bar | Toolbar | 72.4-78.7 | worst pair; geometry: CN1 bar 9% wide, squarer corners (tracked) |
| UIAlertController (alert) | Dialog UIID card | Dialog | 97.0-97.1 | |
| UIPickerView | `GenericSpinner` | Spinner | 89.8-90.0 | wheel perspective fade approximated |
| UIVisualEffectView / UIGlassEffect | GlassPanel UIID | GlassPanel{Grey,Red,Grad,Photo} | 94.9-98.6 | glass-blend isolation over 4 backdrops |

Isolation/ladder cases (not user-facing components): TabOne 95.2-95.9,
TabsGeom 92.9-93.2, GlassText/GlassIcon 98.6-98.7.

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
| MaterialButton (filled) | Button | 93.8-98.5 |
| MaterialButton (tonal) | RaisedButton | 98.6-98.8 |
| MaterialButton (outlined) | FlatButton | 92.8-94.5 |
| TextInputLayout | TextField | 95.1-98.2 |
| MaterialCheckBox | CheckBox | 94.1-96.6 |
| MaterialRadioButton | RadioButton | 94.9-97.1 |
| MaterialSwitch | Switch | 91.1-97.5 |
| Slider | Slider | 97.1-99.8 |
| LinearProgressIndicator | ProgressBar | 100.0 |
| TabLayout | Tabs | 91.5-99.4 |
| MaterialToolbar | Toolbar | 92.2-96.5 |
| MaterialAlertDialog | Dialog | 91.0-93.9 |
| FloatingActionButton | FloatingActionButton | 99.0-99.1 |

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
2. Implement the native reference: `NativeRef.swift` (iOS, offline goldens via
   `scripts/build-ios-native-ref.sh`) and/or `NativeWidgetFactoryImpl.java`
   (Android, same-run reference).
3. Add the CN1 build case in `Cn1WidgetRenderer`.
4. Tune the theme (`native-themes/*/theme.css`), regenerate the shipped `.res`
   with `scripts/build-native-themes.sh`, and let the ratchet record the new
   baseline.
