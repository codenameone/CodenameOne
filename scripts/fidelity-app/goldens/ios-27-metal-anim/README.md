# Animation references — how they are captured and what they answer

The iOS 27 counterpart of `../ios-26-metal-anim`. Everything here was recorded
on an iPhone 16 simulator running the iOS 27.0 runtime, with Xcode 27
(`CN1_XCODE_MAJOR=27`), the same device and toolchain the `ios-27-metal` and
`ios-27-metal-frames` goldens come from.

- **`native-tabs-*.mov`** — the REAL `UITabBar` Liquid Glass selection morph,
  recorded from the NativeRef app with
  `CN1SS_FIDELITY_GOLDEN_SET=ios-27-metal scripts/record-ios-native-anim.sh tabs <light|dark>`.

  **The recording MUST be tap-driven.** UIKit plays the full Liquid Glass
  morph only for genuine touch-driven selection on a `UITabBarController`;
  programmatic `selectedIndex` changes play a flat simplified platter slide.
  The recording script drives real taps through the XCUITest bundle in
  `../../ios-native-ref/tap-driver/` (requires `xcodegen`).

  The script's raw output starts on the home screen, several seconds before the
  app finishes launching. Those seconds were cut, so each video opens in the app,
  as the iOS 26 references do. Each contains six tap-driven selections.

- **`cn1-tabs-*.mov`** — the Codename One morph in motion, 1088x290 at 60 fps,
  the same frame the `../ios-27-metal-frames/TabsMorph_*` goldens use. The
  sequence is a half-second hold on the first tab, the first-to-last-tab
  selection morph, then a one-second hold on the last tab.

  Unlike the iOS 26 video, which was rendered in the JavaSE simulator, these
  frames come from the real iOS renderer: the fidelity app on the same iOS 27
  simulator, with the iOS 27 theme installed. Each frame is the morph frozen
  through `Tabs.setMorphTestState` at the linear timeline value a 60 fps
  display would sample during the theme's 480 ms
  `tabsAnimatedIndicatorDurationInt`, so it is exactly the motion the animation
  plays, with no timing jitter. The morph is shown in one direction only.

- **`native-switch-*.mov`** — the real `UISwitch` toggle. The switch morph is
  not interaction-gated, so the self-animating recording path is fine there.

The four lens/glass implementations (Metal shader, iOS CPU reference, JavaSE,
JavaScript) are held together by `scripts/verify-javascript-lens-parity.mjs`.
