# Native reference goldens

Each directory here is one **golden set**: the captured appearance of a platform's
real widgets, which the Codename One render is scored against. A golden is not a
test output. It is the definition of what the theme is trying to look like, so
nothing in CI ever writes one.

| Set | Captured from |
|---|---|
| `ios-26-metal` | iOS simulator, `scripts/build-ios-native-ref.sh` |
| `ios-27-metal` | iOS simulator, same script with `CN1SS_FIDELITY_GOLDEN_SET=ios-27-metal` |
| `android-m3` | Android emulator, `scripts/build-android-native-ref.sh` |
| `windows-11-fluent` | Hosted Windows runner, WinUI 3 |
| `macos-aqua` | Hosted macOS runner, AppKit |
| `gnome-adwaita` | Hosted Linux runner, GTK4 + libadwaita under Xvfb |

## The two iOS generations

`ios-26-metal` and `ios-27-metal` are both live. They are separate sets rather
than a shared base plus overrides, and that is measured rather than assumed:
only **12 of the 68** tiles are byte-identical between them, 0.1 MB in total, so
a shared layer would buy nothing and add a resolution step to every comparison.
56 tiles genuinely differ, concentrated in the Liquid Glass surfaces and the bar
and button chrome built on them.

What is NOT duplicated is the authoring: one `fidelity-tests.yaml`, one
`NativeRef.swift`. The generation difference comes from the OS drawing its own
widgets on a matching runtime, not from per-generation reference code -- which
is why the runtime the capture runs on is the thing that has to be right.

`CN1SS_FIDELITY_GOLDEN_SET` drives all three steps so they cannot disagree:

```
CN1SS_FIDELITY_GOLDEN_SET=ios-27-metal scripts/build-ios-native-ref.sh   # capture
CN1SS_FIDELITY_GOLDEN_SET=ios-27-metal scripts/build-fidelity-app.sh ios # build
CN1SS_FIDELITY_GOLDEN_SET=ios-27-metal scripts/run-ios-fidelity-tests.sh <app> # score
```

The build step is the one that is easy to forget: it selects the app's theme
generation (`ios.themeGeneration=27`), and without it the run scores the iOS 26
theme against iOS 27's native widgets and reports a regression in all 56.

**iOS 27.1 cannot host this capture.** That runtime supports exactly one device
type, iPhone Duo -- 65 supported types on iOS 26.3, 62 on 27.0, **1** on 27.1 --
so `ios-27-metal` is captured on iOS 27.0, on the same iPhone 16 model as the 26
set. `scripts/lib/ios-sim.sh` picks the newest runtime that HAS the model, which
is what makes that fall out rather than fail.

## The `-frames` sets are NOT native captures

`<set>-frames` holds **CN1 renders**, not platform ones. `run-ios-fidelity-tests.sh`
calls them "self-goldens (CN1 vs committed CN1)": they exist to catch the CN1
morph drifting -- a stuck frame, non-monotonic travel, a broken overshoot -- which
a score against a native still cannot see. That makes them the one golden kind CI
is allowed to compare against itself, and the reason seeding them is a deliberate
local act:

```
CN1SS_FIDELITY_GOLDEN_SET=ios-27-metal FIDELITY_UPDATE_GOLDENS=1 \
  scripts/run-ios-fidelity-tests.sh <app> <udid>
```

In CI a missing frame golden is a failure and is never self-approved.

Worth stating plainly because the neighbouring `-anim` directory IS native:
`native-*.mov` comes from `record-ios-native-anim.sh` and must be tap-driven for
tabs. Reading `-frames` as native too leads to preparing an `xcodegen` + XCUITest
capture for something a single flag produces.

The two iOS generations do not capture the same tab frames. `ios-26-metal-frames`
holds `TabsMorph`, the progress-based iOS 26 morph. `ios-27-metal-frames` holds
`TabsGlassMotion`, the time-based iOS 27 motion measured from UIKit
(`com.codename1.ui.TabGlassMotion`), frozen at value x 10 ms. The rows say which
set they belong to with `golden_sets:`, and the device runner and
`MorphFrameValidator --golden-set` both honour it.

**Seeded goldens still have to be looked at**, because whatever CN1 rendered
becomes the reference. What that check looks like, from seeding `ios-27-metal-frames`:
the 24 names and sizes matched `ios-26-metal-frames` exactly, none were blank, the
ten `SwitchMorph` frames came out byte-identical to the iOS 26 set (Switch uses no
glass recipe, and iOS 27 did not change it), and the `TabsMorph` frames moved in
dark (~6.5 mean delta) and barely in light (~0.11) -- which is precisely where the
theme changed, since the `pill` recipe's light variant was measured unchanged. A
seed that moved something else would have been the finding.

## Why the desktop sets come from CI

The other sets are captured on a maintainer's machine. The desktop ones cannot
be: a working developer's Mac has a chosen accent colour, a chosen appearance
and custom fonts, and a reference captured there would encode all three. A
hosted runner is the closest available thing to a default-configured machine.

This is measurable rather than theoretical. The same capture app run on a
maintainer's macOS 26 machine reports a window background of `#FFFFFF` and
`#171717`; on the macos-15 runner it reports `#E7E7E7` and `#262626`. Both are
correct for their machine and only one of them is a reference.

## Capturing a set

```
gh workflow run fidelity-desktop-native-ref.yml -f targets=windows -f mode=capture
```

`targets` is `all`, `windows`, `macos` or `gnome`. `mode=probe` answers only the
environment questions and writes one tile; `mode=capture` writes the full matrix.

The workflow is **dispatch only**, with no schedule and no path trigger, and that
is deliberate: a native reference defines the design generation a theme is written
against, so a job that re-captured it on its own would turn a real OS design
change into a green build.

## Promoting a run to a golden set

1. **Download the artifact and look at every frame.** Not the tile count -- the
   frames. A capture that is wrong in one direction reports a full count and no
   blockers, which is exactly how a light-mode backdrop ended up behind the dark
   half of a Windows set.
2. **Read `capture-manifest.json`** and check it describes the environment you
   intended: OS build, scale factor, accent colour, fonts, transparency and
   contrast settings, toolkit versions. It also carries two fields worth reading
   every time:
   - `backdrop_by_appearance` -- the surface each half was captured on. The apps
     refuse a set where the two are equal, but the values themselves are what you
     compare against the theme's own `--window-bg-color`.
   - `states_identical_to_normal` -- states the platform does not restyle. These
     are real findings, not gaps: AppKit draws no rollover state at all, so every
     macOS hover tile is listed, and Adwaita restyles a `GtkEntry` on focus rather
     than on hover. **A theme must leave those states equal too**, or it diverges
     from a reference that cannot move.
3. **Commit the set in one commit that names the run**, so the provenance of every
   byte is recoverable.
4. **Dispatch the same capture again and require byte-identical output.**
   Nondeterminism is fixed in the reference app or by pinning an environment knob,
   never with a tolerance file. There are no tolerance sidecars here and there
   will not be.

   What that took on Windows, since the same trail is likely to be walked again:
   a screen `BitBlt` reads whatever is in front of those coordinates, and the app
   cannot take the foreground on a hosted runner, so two runs differed on *every*
   tile and one came back `#E0E0E0` in both appearances -- not the window at all.
   `PrintWindow` with `PW_RENDERFULLCONTENT` renders the window's own content
   instead and took it to 3 tiles. Control-template storyboards are not covered by
   `SPI_SETCLIENTAREAANIMATION`, so the app grabs repeatedly and writes only when
   two consecutive grabs agree.

   **The measured residual, recorded rather than tolerated:** the Windows set
   reproduces byte-for-byte except for 2-3 pixels on an anti-aliased EDGE, which
   differ by +/-1 in a channel between runs. That is GPU rasterizer rounding;
   nothing in the app or the environment pins it. It is far below the comparator's
   content threshold and does not move a score. It is written down here so the next
   person does not spend a run discovering it, and it is NOT a licence to accept a
   larger one.

   Measured again when the second wave of rows landed, and the shape held: two runs
   of identical code differed on `DesktopSlider_normal_dark` (2px),
   `DesktopSlider_hover_dark` (3px) and `DesktopTooltip_normal_light` (2px), every
   one of them +/-1 in a channel on a rounded border or a thumb edge. So it is a
   property of anti-aliased edges on this runner rather than of the slider, which is
   the only control the first measurement happened to have.

   GNOME, by contrast, reproduced **byte-for-byte across two runs, all 104 tiles**,
   including the manifest -- so the residual is not a property of the suite.

   The Windows set also carries one PNG that is NOT a tile: `Button_normal_light.png`,
   the self-check the app BitBlts to prove the Mica backdrop reached its window. It is
   excluded from `tiles_written` and must be excluded from the committed set too --
   it has no CN1 counterpart, so leaving it in makes the golden count disagree with
   the number of pairs the gate can score.
5. **Record the first baseline separately**, with `FIDELITY_UPDATE_BASELINE=1`, so
   the commit that defines the goldens and the commit that defines the ratchet are
   two reviewable changes rather than one.

## What CI may and may not do

CI scores against these files and never writes them. `FIDELITY_UPDATE_GOLDENS`
must not be set in any desktop workflow -- the desktop suite follows the iOS
model, where committed goldens are the contract, not the Android one.
