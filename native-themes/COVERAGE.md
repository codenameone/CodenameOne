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
| UISegmentedControl | ButtonGroup / Tabs pill variant | `ToggleButton` themed (capsule track); not in the fidelity suite |
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
| SegmentedButton (single/multi) | ButtonGroup | `ToggleButton` themed (outlined pill); not in the fidelity suite |
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

## Desktop: Windows Fluent, macOS Aqua, GNOME Adwaita

The three desktop themes share one matrix, so they share one table.

First measured scores, against golden sets captured on hosted runners:

| Theme | Golden set | Pairs | Mean | Gating |
|---|---|---:|---:|---|
| Windows Fluent | `windows-11-fluent` | 60 | 82.1% | yes, on master |
| GNOME Adwaita | `gnome-adwaita` | 60 | 85.9% | yes, on master |
| macOS Aqua | `macos-aqua` | 60 | 84.6% (local) | yes, on master |

These are starting points, not results. All three themes were written without a
reference to check them against, so this is the first time any of them has been
measured, and the ratchet moves them up from here.

### Every desktop port installs one

| Port | Installs | Where |
|---|---|---|
| Windows (native) | Windows Fluent | `<copy file=` in `maven/windows/pom.xml` |
| Linux (native) | GNOME Adwaita | `<copy file=` in `maven/linux/pom.xml` |
| macOS (native) | macOS Aqua | the unset branch of `MacOSBuildHints.getThemeMode()` |
| Java SE desktop | legacy, unless asked | `JavaSEPort.resolveDesktopNativeTheme` |

The Java SE default is deliberately still legacy. That one default reaches every desktop
application ever built with Codename One rather than only ours, and an application that
wants the platform look already asks with `desktop.themeMode` or the cross-platform
`nativeTheme=native`. The three native ports have no such history -- none has shipped.

Flipping the three restyles every screen and reseeds their committed screenshot baselines
(166 Windows, 166 + 166 Linux x64/arm64, 160 macOS), which is why it was deferred when the
themes landed. They are reseeded in the same change as the flip, from the CI runners that
capture them.

### What the themes now turn on

Each desktop theme declares these, so they are behaviours of the theme rather than hooks a
port has to be told about separately. The three files install only on a desktop, so an
application still on the legacy theme is untouched.

| Constant | Effect |
|---|---|
| `interactiveScrollBool` | a grab-able thumb, a track that pages on click, a reserved gutter, no fade |
| `scrollThumbMinSizeInt` | 24px, so the thumb stays grabbable on content far taller than the viewport |
| `defaultNativeWindowModeBool` | a `Dialog` opens as a real operating system window |
| `desktopTitleBarMode` | `native` on Windows and macOS, `custom` on GNOME, whose HeaderBar IS the title bar |
| `commandBehavior: Native` | commands go to the platform's menu where there is one |
| `separatorThicknessMM` | the `Separator` rule, 1px on all three |

`commandBehavior: Native` is safe on a port with no menu bar because
`CodenameOneImplementation.setCommandBehavior` normalises it away there, the same way it
normalises `BUTTON_BAR` to `SOFTKEY` on a non-touch device. Before that it was a silent way
to lose every command: `MenuBar.updateCommands` handed them to a no-op `setNativeCommands`
and returned without drawing anything.

What the first round of measurement actually found is worth recording, because only one of
the four was a CSS problem:

| Finding | Effect |
|---|---|
| Three copies of every theme in a built tree (`Themes/`, the staged copy, one inside the javase jar) and the class loader reaching a stale one | A theme edit scored identically to no edit. Two conclusions in this work were wrong because of it. |
| The two sides rendered different label text on all six rows that carry text | Text field 65% -> 92%. Now gated by `scripts/check-fidelity-spec.py` in both directions. |
| The two sides sat at different widget values (slider 0.5, progress 0.6) | The CN1 knob sat 24px right of the reference's. |
| Three theme constants never set: `progressTrackThicknessMM`, `sliderThumbWidth/HeightMM`, `sliderContinuousTrackBool` | Progress bar 60% -> 75%; slider gained a round knob on a continuous track. |

The macOS hover rows were the exception that proves the reference is worth having: they
were the six worst tiles in the set, and the manifest already said why -- AppKit restyles
none of those controls on hover, so eighteen `.hover` rules were removed rather than tuned.
That property is now asserted rather than remembered
(`DesktopNativeThemeContentTest.aquaAddsNoHoverRules`).

### Covered components

| Fidelity test | WinUI 3 | AppKit | GTK4 / libadwaita |
|---|---|---|---|
| DesktopButton | Button | NSButton (rounded) | GtkButton |
| DesktopAccentButton | Button + AccentButtonStyle | NSButton (default) | GtkButton `.suggested-action` |
| DesktopTextField | TextBox | NSTextField | GtkEntry |
| DesktopCheckBox | CheckBox | NSButton (checkbox) | GtkCheckButton |
| DesktopRadioButton | RadioButton | NSButton (radio) | GtkCheckButton in a group |
| DesktopSwitch | ToggleSwitch | NSSwitch | GtkSwitch |
| DesktopSlider | Slider | NSSlider | GtkScale |
| DesktopProgressBar | ProgressBar | NSProgressIndicator | GtkProgressBar |
| DesktopComboBox | ComboBox | NSPopUpButton | GtkDropDown |
| DesktopSeparator | MenuFlyoutSeparator | NSBox (separator) | GtkSeparator |
| DesktopGroupBox | headered Border | NSBox (titled) | GtkFrame |
| DesktopStepper | NumberBox (inline spin) | NSTextField + NSStepper | GtkSpinButton |
| DesktopLinkButton | HyperlinkButton | NSButton (link) | GtkLinkButton |
| DesktopSearchField | AutoSuggestBox | NSSearchField | GtkSearchEntry |
| DesktopListRow | ListViewItem | NSTableRowView | GtkListBoxRow |
| DesktopTabs | TabView | NSTabView | GtkNotebook |
| DesktopToolbar | CommandBar | title-bar strip | AdwHeaderBar |
| DesktopDisclosure | Expander | disclosure triangle + label | GtkExpander |
| DesktopScrollBar | ScrollBar | -- | GtkScrollbar |
| DesktopScrollBarHighlight | -- | -- | GtkScrollbar (PRELIGHT / ACTIVE) |
| DesktopMenuBar | MenuBar | -- | GtkPopoverMenuBar |
| DesktopMenuItem | MenuFlyoutItem | -- | menu row (`.model` button) |
| DesktopTooltip | ToolTip | -- | -- |

States: normal, hover, pressed, selected and disabled as each control supports them, in
both appearances.

### Rows that are not scored on every platform

A reference has to be RENDERABLE into a view, and three of these are not everywhere. Saying
where the reference exists is the honest answer: a blank golden scores 0% forever and reads
as a theme bug.

| Row | Missing on | Why |
|---|---|---|
| DesktopScrollBar | macOS | Measured, not assumed. An `NSScroller` reports `usableParts=allScrollerParts`, `knobProportion` 0.4, `isHidden=false` and a 17x56 frame -- and renders nothing through `NSView.cacheDisplay`. Tried detached and inside a real `NSScrollView`, in both `.legacy` and `.overlay` styles, with `AppleShowScrollBars=Always` already set by the capture script. The tile comes back holding one colour, the backdrop, every time. Same class of limitation as Aqua vibrancy. |
| DesktopScrollBarHighlight | macOS, Windows | The scrollbar's hover and drag states. Measured on a capture run: none of `PointerOver`, `UncheckedPointerOver`, `CheckedPointerOver` or `MouseOver` is a visual state of a WinUI `ScrollBar`, and neither is `Pressed` or `Dragging`. GTK can state it -- `PRELIGHT` and `ACTIVE` are what the CSS pseudo-classes resolve from -- and its captured tiles genuinely differ from normal, so the row scores there and nowhere else rather than not existing. |
| DesktopListRow hover | all three | A WinUI `ListViewItem` draws through `ListViewItemPresenter`, which paints its own pointer-over chrome rather than exposing a state `GoToState` can reach. Dropped from the row rather than scored on two platforms and blocked on the third; `selected` is a real property everywhere and is scored. |
| DesktopMenuBar, DesktopMenuItem | macOS | An `NSMenu` belongs to the window server, not to a view. |
| DesktopTooltip | macOS, GNOME | Both platforms' tooltips are separate windows. A WinUI `ToolTip` is an ordinary `Control`, which is why the row exists at all. |

Three things the second wave found in the references themselves, each caught by the capture
apps' own blockers rather than by eye:

- An `NSTableRowView` has no intrinsic size in either axis and laid out to 240x0, producing
  no image. Given the standard 24pt row height a table would have given it.
- An `NSStackView`'s `fittingSize` came back with no width, so the stepper tile showed the
  chevrons and no field -- half a control.
- A `CGColor` read from a dynamic `NSColor` freezes at whatever appearance was in force, so
  the toolbar's light tile was painted with the dark window background. Drawn rather than
  layer-backed now, which is why `TileView` draws its own fill too.

### Known visual gaps (tracked, honest list)

| Gap | Why it is open |
|---|---|
| Fluent reveal highlight | The gradient that follows the cursor across a control. Needs per-pixel pointer position at paint time; no CN1 primitive expresses it. |
| Mica / Acrylic | A WINDOW attribute (`DwmSetWindowAttribute`), not a region operation, so it is not a theme rule at all. The right shape is a `desktopWindowBackdrop` theme constant read at window creation. |
| Aqua vibrancy | `NSVisualEffectView` is composited by the window server and is invisible to `NSView.cacheDisplay`, which is the capture path that needs no Screen Recording consent. A missing golden is honest; a blank one scores 0% forever and reads as a theme bug. |
| macOS hover | AppKit draws no rollover state for any control in this matrix. The Aqua theme leaves hover equal to normal, the captured reference says the same, and the gate holds it there. Not a gap in the theme -- a property of the platform. |
| Adwaita has no Mica analogue | By design. Recorded so nobody goes looking for one. |
| Window chrome | The tile contract is a widget in a tile. `DesktopToolbar` now scores the title-bar strip, but the rest -- borders, shadows, corner radii, the traffic lights -- is not scored. |
| Dialog | Not scored: an alert needs a bigger tile than 240x56, and the tile size is a constant in each of the three standalone capture apps rather than a per-row value. Teaching all three per-row tiles is the prerequisite. |
| Fluent `ScrollBar` visual-state names | The WinUI `ScrollBar` template predates the `PointerOver` vocabulary, so `MouseOver` and `Dragging` are tried after the modern names. A capture where none of them matched reports a blocker rather than writing a tile identical to normal. |

### Fonts are the honest ceiling

Segoe UI Variable and SF Pro are system-only and not redistributable, so the Windows and
macOS sets can never be reproduced away from those platforms. Only GNOME can be made fully
honest, Cantarell being redistributable and pinnable. Where a face cannot be matched the
residual is named rather than dismissed as anti-aliasing.

### Golden sets

All three are captured, reviewed, committed and gating on master. The second-wave rows have
no goldens yet: they are captured by dispatching
`fidelity-desktop-native-ref.yml -f targets=all -f mode=capture`, reviewed frame by frame,
committed in one commit naming the run, then re-dispatched and required to come back
byte-identical. Until that happens the desktop fidelity legs report those pairs as
`missing_expected` and fail, which is the correct behaviour -- a new row is not silently
skipped.

A baseline is recorded from the runner that SCORES it, never locally. The CN1 side renders
on the leg's own OS, and a Mac-recorded baseline failed the gnome gate on eighteen pairs --
the slider comes out one pixel taller on Linux. That is the "measured on its own OS runner"
rule applying to the baseline as well as the reference, and it is easy to miss because a
locally recorded baseline passes locally forever.

The protocol, including the measured reproducibility residual on the Windows set, is in
`scripts/fidelity-app/goldens/README.md`.

## UIIDs the framework assigns

`ToggleButton` shipped unstyled for as long as both themes existed: the UIID is
written by framework code, nothing defined it, and the control fell through with
no shape at all. Nothing errors in that situation -- the control simply looks
wrong -- so the whole surface was enumerated rather than sampled.

Enumerating it takes two passes, and the second one matters. Every
`setUIID("...")` literal in `CodenameOne/src` gives 128 names, of which 73 are
absent from both theme files. But absent from the CSS is not the same as
unstyled: `UIManager.resetThemeProps()` seeds defaults for a long list of UIIDs,
each behind a guard of the form

```java
if (installedTheme == null || !installedTheme.containsKey("RightSideCommand.derive")) {
    themeProps.put("RightSideCommand.derive", "SideCommand");
    themeProps.put("RightSideCommand.align", rightAlign);
}
```

so a theme that defines one of those names **suppresses** the framework's own
setup rather than filling a hole. Twenty-one of the 73 are seeded that way. The
real gap is the remainder: names in neither the CSS nor `resetThemeProps`.

Both themes now define those, including `ToggleButton` and the `ToggleButton*`
edge names `ComponentGroup` renames its controls to, `FloatingHint`, `TreeNode`,
the `Calendar` grid, the media transport and the directional popup panes.

### Three deliberate overrides

| UIID | What the framework seeds | Why the theme overrides it |
|---|---|---|
| `TableCell` | `transparency` only, no padding | every table rendered with cells nearly touching their borders |
| `TableHeader` | `transparency` only | the header was no heavier than a row |
| `ErrorLabel` | `derive: FloatingHint` plus a generic red | the platform error colours differ: `#b3261e` on Material, `#ff3b30` on iOS |

`FloatingHint` is worth its own note: the framework never defines it, and yet
`ErrorLabel`, `InputComponentAction` and `DescriptionLabel` are all seeded to
derive **from** it. Three UIIDs inheriting from one nothing defined.

### Deliberately still undefined

**Retired components.** `RSSReader`, `HTMLComponent`, `HTMLTable`, the
feature-phone soft keys and `VKBButton`.

**Names where the unstyled default is the better answer**, measured by rendering
the guide's figure set before and after rather than assumed:

| UIID | What defining it did |
|---|---|
| `icon` | `SpanLabel` and `SpanButton` lost the icon spacing the default gives them |
| `Emblem` | same, for `MultiButton`'s trailing badge |
| `StatusBar` | collapsed the strip a `Form` reserves, so iOS content ran into the system bar |
| `Scroll`, `ScrollThumb`, `HorizontalScroll`, `HorizontalScrollThumb` | the framework seeds these with a measured padding; overriding it changes the content width of every scrollable form, which re-wrapped text in unrelated figures |

The scrollbar rules alone moved a `SpanLabel` figure by 21 pixels. Sizing a
scrollbar or a status bar is a layout decision that deserves its own change with
its own before-and-after, not a line in a gap-filling sweep.

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
