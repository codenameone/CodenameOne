# Flutter runtime: readiness

Where the transpiled gallery does **not** yet match the reference, and what closing
each gap requires. Every line here is measured, not estimated; each has a check that
says when it is done.

Kept in the module rather than under `docs/`, which is published.

**Baseline, 2026-09-19**

| Axis | State |
|---|---|
| Routes rendering | 48 / 48, **0 runtime errors** |
| Static parity | mean **2.56%** wrong pixels, worst **7.74%**, none above 8% |
| Motion steps | 8, seven at or below their ratchet |
| Tests | 472 green (flutter-runtime 392, dart-transpiler 60, dart-runtime 20) |
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
| `CupertinoDatePicker` | `onDateTimeChanged` | 3 |
| `KeyboardListener` | `onKeyEvent` | 3 |
| `CupertinoSegmentedControl` | `onValueChanged` | 2 |
| `CupertinoSlidingSegmentedControl` | `onValueChanged` | 2 |
| `NavigationRail` | `onDestinationSelected` | 2 |
| `GestureDetector` | `onTapUp`, `onVerticalDragUpdate` | 2 |
| `CupertinoPicker` | `onSelectedItemChanged` | 1 |
| `CupertinoTimerPicker` | `onTimerDurationChanged` | 1 |
| `Chip` | `onDeleted` | 1 |
| `InputChip` | `onDeleted` | 1 |
| `InteractiveViewer` | `onInteractionStart` | 1 |
| `WillPopScope` | `onWillPop` | 1 |
| `Listener` | `onPointerDown` | 1 |
| `Dismissible` | `onDismissed` | 1 |

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

### Open, and real: the Cupertino picker's popup renders wrong

The popup opens and presents, but the picker inside it draws as a narrow vertical white
pill rather than a wheel. The tap, the route to `showCupertinoModalPopup` and
`Dialogs.showDialog` are all sound; this is a layout defect in the picker's content.

## 3. Structural gaps

| Gap | Consequence |
|---|---|
| A button **consumes** its child into a label string or icon char rather than mounting it | anything that is not reducible to one `Text` or `Icon` cannot render inside a button; the walk now sees through layout wrappers, which covers the common case but not a genuine composite |
| `Notification.dispatch` is a no-op | needs the listener's type, and `NotificationListener<T>` erases `T`; delivering without a type token would hand every `ScrollNotification` to a listener waiting for something else. Wants a type token from the transpiler |
| `CommonTransitions` drives both pages from one `Motion` | the iOS push works around it with its own transition; the outgoing page should ride `linearToEaseOut` while the incoming rides the three-point curve |

## 4. Known per-screen differences

| Screen | Difference | Measured |
|---|---|---|
| `/demo/grid-lists` | worst route | 7.74% |
| `/crane` | front layer detail | 7.63% |
| `/demo/typography` | line box for styles that state no height | 7.51% |
| `/demo/text-field` | field decoration detail | 6.11% |
| `/shrine` | field corners are rounded where Shrine's are beveled; CANCEL/NEXT sit left where the reference right-aligns them (`OverflowBar` ignores `alignment`); CANCEL takes the wrong colour | 3.17% |
| `compose_back` | worst frame of the closing container transform | 13.13% |
| `push_shrine` | 0.13pp over its ratchet after the master update; error sits in the Shrine gaps above | 3.26% mean |

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
/demo/grid-lists                    7.74
/crane                              7.63
/demo/typography                    7.51
/demo/text-field                    6.11
/fortnightly                        4.58
/demo/bottom-navigation             4.56
/demo/colors                        4.24
/demo/cupertino-scrollbar           4.19
/demo/data-table                    3.99
/demo/motion                        3.90
/reply                              3.64
/demo/cupertino-buttons             3.60
/demo/bottom-app-bar                3.32
/demo/2d-transformations            3.20
/shrine                             3.16
/demo/cupertino-picker              3.00
/rally                              2.98
/demo/cupertino-text-field          2.95
/demo/banner                        2.93
/demo/sliders                       2.84
/demo/bottom-sheet                  2.36
/demo/cupertino-search-text-field   2.23
/demo/cupertino-switch              2.15
/demo/cupertino-navigation-bar      2.05
/demo/cupertino-slider              2.05
/demo/cupertino-context-menu        1.98
/demo/cupertino-tab-bar             1.96
/demo/cupertino-alerts              1.87
/demo/cupertino-segmented-control   1.81
/starter                            1.69
/                                   1.54
/demo/nav_rail                      1.43
/demo/card                          1.33
/demo/lists                         1.32
/demo/nav_drawer                    1.15
/demo/pickers                       1.15
/demo/snackbars                     1.13
/demo/dialog                        0.91
/demo/selection-controls            0.91
/demo/tabs                          0.79
/demo/button                        0.77
/demo/menu                          0.73
/demo/chip                          0.72
/demo/progress-indicator            0.69
/demo/tooltip                       0.68
/demo/cupertino-activity-indicator  0.54
/demo/app-bar                       0.35
/demo/divider                       0.35
```
