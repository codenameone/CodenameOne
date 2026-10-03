# Flutter runtime: readiness

Where the transpiled gallery does **not** yet match the reference, and what closing
each gap requires. Every line here is measured, not estimated; each has a check that
says when it is done.

Kept in the module rather than under `docs/`, which is published.

**Baseline, 2026-09-20**

| Axis | State |
|---|---|
| Routes rendering | 48 / 48, **0 runtime errors** |
| Static parity | mean **2.46%** wrong pixels, worst **7.60%**, none above 8% |
| Motion steps | 8, **all eight** at or below their ratchet |
| Tests | 474 green (flutter-runtime 394, dart-transpiler 60, dart-runtime 20) |
| Static analysis | SpotBugs over core: **0 findings** (the gate's requirement) |
| Ports | JavaSE simulator and iOS both build, launch and measure |

## How to reproduce the numbers

```bash
cd benchcn1
tools/relaunch.sh --app                 # rebuild runtime + app, restart the simulator
PORT=8765 python3 tools/parity.py       # static: 48 routes
tools/relaunch.sh                       # ALWAYS relaunch between the two -- the sweep
PORT=8765 python3 tools/motion/capture.py   # leaves the app on the last route, and the
python3 tools/motion/compare.py             # push steps then start from the wrong screen
```

`JAVA17_HOME` must be set or 16 of the transpiler's 60 tests skip silently -- the ones
that compile and execute generated code.

---

## 1. Interaction: callbacks the gallery passes that do nothing

The largest gap, and the one the pixel metrics cannot see: a screen can render at 2%
wrong and do nothing when touched. These are stored by the widget and read by nothing
in the runtime. Counts are how many times the gallery's generated code passes them.

| Widget | Inert callbacks | Uses |
|---|---|---|
| `MaterialApp` | `onGenerateRoute` | 10 |
| `TextFormField` | `onSaved`, `onFieldSubmitted` | 6 |
| `Focus` | `onKeyEvent`, `onFocusChange` | 5 |
| `FormField` | `onSaved` | 4 |
| `KeyboardListener` | `onKeyEvent` | 3 |
| `CupertinoSegmentedControl` | `onValueChanged` | 2 |
| `CupertinoSlidingSegmentedControl` | `onValueChanged` | 2 |
| `NavigationRail` | `onDestinationSelected` | 2 |
| `GestureDetector` | `onTapUp`, `onVerticalDragUpdate` | 2 |
| `Chip` | `onDeleted` | 1 |
| `InputChip` | `onDeleted` | 1 |
| `InteractiveViewer` | `onInteractionStart` | 1 |
| `WillPopScope` | `onWillPop` | 1 |
| `Listener` | `onPointerDown` | 1 |
| `Dismissible` | `onDismissed` | 1 |

The three Cupertino picker callbacks left this table on 2026-09-20: the wheels are
real now and report as they turn.

### Verified by use, not by counting

The census counts call sites; it does not know whether anything compensates. Two
checked so far, by driving the running app:

- **`MaterialApp.onGenerateRoute` -- compensated, NOT a live defect.** It is the
  gallery's whole route factory (`main.dart:69`) and reads as the largest gap in the
  table, but `Navigator` resolves named routes through its own table. Tapping a
  category, then a demo, navigates and renders correctly with zero errors. Latent
  risk: a route only the factory can produce would not resolve.
- **`Dismissible.onDismissed` -- LIVE DEFECT, confirmed.** A full left swipe across a
  mail card in `/reply` changes **0.0%** of the list. Swipe-to-dismiss does nothing at
  all; the reference dismisses the mail.

A further **40** inert callbacks exist that the gallery never passes. They are latent
rather than broken and are not tracked here until something needs them.

## 2. Parameters captured and ignored

Widgets that render their child correctly and drop what was asked of them. Verified
per parameter -- a widget appears only where nothing outside its own file reads the
field. `Visibility.visible` and `ClipRRect.borderRadius`, for instance, ARE honoured
and are not listed.

| Widget | Ignored parameters the gallery sets |
|---|---|
| `MouseRegion` | `cursor` (x20) -- desktop only |
| `Semantics` | `sortKey` (x15), `hint` |
| `Focus` | `focusNode` (x5), `onKeyEvent`, `onFocusChange`, `canRequestFocus` |
| `KeyboardListener` | `focusNode` (x5), `onKeyEvent` |
| `Tooltip` | `excludeFromSemantics` (x5) -- and no tooltip is shown at all |
| `Visibility` | `maintainState`, `maintainAnimation` |
| `AspectRatio` | **`aspectRatio`** -- the ratio is ignored entirely |
| `ClipRect` / `ClipRRect` / `ClipOval` | `clipper` (custom clip shapes) |
| `Form` | `autovalidateMode` |
| `WillPopScope` | `onWillPop` |

## 2b. RESOLVED: a list mis-rendered when it lost an item

Deleting a mail left every row below it drawing its new content over its old, and a
provider lookup in a rebuilt row answered null.

**Cause: `DismissibleRenderElement` did not override `visitChildren`.**
`Element.visitChildren` is empty by default, so an element that owns children and does
not override it owns them invisibly -- and the walk that unmounts a subtree is the one
that suffers. Deleting a mail deactivated five Dismissible elements and reached none of
their subtrees: their elements stayed mounted, their components stayed in the scene, and
the lists inside those rows were never unmounted at all.

It was introduced with swipe-to-dismiss, which is why nothing had shown it before.

Worth keeping, because the symptom pointed everywhere but at the cause. The element tree
looked right at every level -- keys emitted, `ObjectKey` equality sound, the keyed
reconciler faithful, `attachOrder` matching the container's child count exactly, no
element mounted twice, no component attached while already parented, and
`RenderHost.detach` never once skipping its guard. That last fact was the tell rather
than an acquittal: detach was not skipping, it was never being CALLED. Logging every
deactivation showed five Dismissibles going while the lists inside them never unmounted,
which places the fault between deactivation and unmount -- and that is `visitChildren`.

The bug CLASS is closed, not just this instance: a census of every element that owns
children finds no other that fails to override `visitChildren`. Two tests pin it.

## 2c. RETRACTED: the nested GestureDetector was the instrument

Recorded here as blocking, and wrong. `/demo/cupertino-picker` is not inert: every row
opens its picker, and always did.

`bench_pointer`'s `steps` defaulted to **8**, so a tap injected without naming steps
carried eight `pointerDragged` events at the SAME coordinates. A zero-distance drag has
no dominant axis -- `draggedOnX` is `0 > 0`, false -- so `shouldGrabScrollEvents` reduces
to `isScrollableY()`, a scrollable ancestor claims the gesture, and `Form.pointerReleased`
then takes the `dragged != null` path and never delivers to what was pressed.

Proved by instrumenting `Form.pointerReleased` rather than by reading it:

```text
picker, steps default (8)   dragged=ScrollPane   origPressedCmp=OverlayComponent enabled=true
home,   steps default (8)   dragged=null         origPressedCmp=OverlayComponent enabled=true
picker, steps: 0            dragged=null   -> the popup opens, 99.5% of the screen changes
```

The floor had been fixed once so `steps: 0` could express a tap; the DEFAULT was left at
8. It now depends on the gesture: a tap defaults to zero steps, a drag to eight.

**Every "changed 0.0%" verdict taken with a defaulted tap is void.** Re-checked since:

| Route | With a real tap |
|---|---|
| `/demo/cupertino-picker` | opens its popup -- works |
| `/demo/chip` action chip | still 0.0%, and CORRECT: the gallery passes `onPressed: () {}`, an empty callback |

The Dismissible verdict stands -- it was taken with a genuine multi-step swipe.

### RESOLVED: the Cupertino pickers are real wheels

They were stubs -- `CupertinoDatePicker` built a bare `Container` so its sheet came up
empty, and the other two built a plain `Column` of their children.

All three are the same control, and Codename One has it five times over in
`com.codename1.ui.spinner`. The only thing wrong with those classes was that every one
was package private. They are public now (`Spinner3D`, `DateSpinner3D`, `TimeSpinner3D`,
`DateTimeSpinner3D`, `DurationSpinner3D`, and the `InternalPickerWidget` contract), and
nothing about them changed.

Driven in the simulator, all five pickers in `/demo/cupertino-picker` open a working
wheel and none errors. `CupertinoDatePickerMode.monthYear` has no spinner of its own and
borrows the date wheel, so it shows one column more than it was asked for; the gallery
does not use that mode.

**Still not visible to the sweep**, which photographs settled routes: the rows render
correctly either way and the wheel only ever appears inside the popup.

## 2d. OPEN: the text field is wrong in four separate ways

Measured on `/demo/text-field`, which is where all four show at once. None of them
is a layout problem; the field is in the right place and the wrong shape.

| What | Measured |
|---|---|
| The requested size never reaches the label | The field asks for **48px** (16sp at 3x, and 16 is right -- demo pages run the Material 2018 scale, not the gallery's Montserrat) and the label renders at **76px**. Glyph heights confirm it: our `Name*` run is 55px against the reference's 34 |
| `TextField.maxLines` is stored and never read | The demo's Life story field asks for 3 lines and renders as one; the reference's box is three lines tall |
| `InputDecoration.helperText` is stored and never read | The reference carries `0/14` under the phone number and "Keep it short, this is just a demo." under Life story. We show neither, and the counter is missing too |
| `suffixIcon` is dropped | The password field's visibility toggle is absent |

The size is the one to fix first, because it is not confined to this demo: every
field in the app is set in a face a half larger than it asked for, and the extra
height pushes everything below it down.

## 3. Structural gaps

| Gap | Consequence |
|---|---|
| ~~A button **consumes** its child into a label string or icon char~~ | **RESOLVED.** A button now mounts a child it cannot reduce to a `Text` or an `Icon`, and makes it transparent to touch so the press still lands on the button underneath. It was not an exotic case: the compose page's account row is a `PopupMenuButton` whose child is a Row, and the row was a blank band |
| `Notification.dispatch` is a no-op | needs the listener's type, and `NotificationListener<T>` erases `T`; delivering without a type token would hand every `ScrollNotification` to a listener waiting for something else. Wants a type token from the transpiler |
| `CommonTransitions` drives both pages from one `Motion` | the iOS push works around it with its own transition; the outgoing page should ride `linearToEaseOut` while the incoming rides the three-point curve |
| `Image.scaled` is a **point sampler** on two of three ports | the desktop port reads one source pixel per destination pixel and Android asks `createScaledBitmap` not to filter, so every downscaled picture aliases. `Image.scaledSmooth`/`fillSmooth` were added beside them and the runtime uses those; the ports themselves are untouched, because fixing them moves ~500 committed screenshot baselines and that is not a call to make silently |

## 4. Known per-screen differences

| Screen | Difference | Measured |
|---|---|---|
| `/crane` | front layer detail | 7.60% |
| `/demo/typography` | line box for styles that state no height | 7.51% |
| `/demo/grid-lists` | **photo resampling only.** The tile geometry is identical to within one pixel on both axes and the two screens are indistinguishable; mean error 6.8 per channel. Was 7.74% before the filtered downscale | 5.90% |
| `/demo/text-field` | field decoration detail | 5.12% |
| `/shrine` | field corners are rounded where Shrine's are beveled | 2.47% |
| `/demo/cupertino-picker` | the wheel sheet is not centred quite as Flutter centres it (popup only, invisible to the sweep) | 3.03% |

Motion is at 8/8 within ratchet. The two container transforms were the last failures:
`reply_compose` 12.48% -> 5.82% worst and `compose_back` 13.36% -> 5.68%, once the
painted surface followed the shape rather than the rectangle (a circle squashes the box
toward a square about its centre), the rounded-rect path stopped sweeping its corners the
long way round, the tapped thing's content was cut out of the page rather than painted
from its component, and the closed content stopped fading -- the fade variant holds it
opaque and simply covers it.

## 5. Harness blind spots

The suites measure settled screenshots and eight scripted transitions. They cannot see:

- **Reachability** -- a screen with no way out scores perfectly. The splash page was
  enter-only for as long as it existed, because `IgnorePointer` ignored nothing and
  `onVerticalDragEnd` was never invoked.
- **Interaction state** -- only one step (`reply_card_press`) touches anything.
- **Anything the gallery does not exercise** -- the 40 latent callbacks above.
- **The harness itself is not in this repository.** `benchcn1` and `new-gallery-ref`
  are working copies on one machine. Master now carries `scripts/fidelity-app` as an
  in-repo precedent.

## Full route table

```
/crane                               7.60
/demo/typography                     7.51
/demo/grid-lists                     5.90
/demo/text-field                     5.12
/fortnightly                         4.58
/demo/bottom-navigation              4.56
/demo/colors                         4.24
/demo/cupertino-scrollbar            4.19
/demo/data-table                     3.99
/demo/motion                         3.90
/reply                               3.64
/demo/cupertino-buttons              3.60
/demo/bottom-app-bar                 3.32
/demo/2d-transformations             3.20
/demo/cupertino-picker               3.03
/rally                               2.94
/demo/banner                         2.93
/demo/sliders                        2.84
/shrine                              2.47
/demo/bottom-sheet                   2.36
/demo/cupertino-text-field           2.29
/demo/cupertino-switch               2.15
/demo/cupertino-navigation-bar       2.05
/demo/cupertino-slider               2.05
/demo/cupertino-context-menu         1.98
/demo/cupertino-tab-bar              1.96
/demo/cupertino-search-text-field    1.96
/demo/cupertino-alerts               1.87
/demo/cupertino-segmented-control    1.81
/starter                             1.69
/                                    1.54
/demo/nav_rail                       1.43
/demo/card                           1.33
/demo/lists                          1.32
/demo/pickers                        1.16
/demo/nav_drawer                     1.15
/demo/snackbars                      1.13
/demo/dialog                         0.91
/demo/selection-controls             0.91
/demo/tabs                           0.79
/demo/button                         0.77
/demo/menu                           0.73
/demo/chip                           0.72
/demo/progress-indicator             0.69
/demo/tooltip                        0.68
/demo/cupertino-activity-indicator   0.54
/demo/app-bar                        0.35
/demo/divider                        0.34
```
