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

- **`cn1-tabs-*.mov`** — the Codename One tab bar in motion, 1178x448 at 60 fps:
  the Codename One half of `docs/videos/tabs-side-by-side-*-bar.mp4`. Both come
  from `scripts/record-ios-tabs-side-by-side.sh`, which records the fidelity
  app's live tab showcase (the same `Tabs`, theme and backdrop as the goldens)
  and the native motion probe with the simulator's own recorder while one
  XCUITest driver gives both the same real touches: six taps, a held press, a
  tap on the selected tab, a slow drag and a flick. Each gesture gets a 4.2 s
  slot, aligned on its touch, so the two sides line up gesture by gesture.

  To compare motion frame by frame with UIKit, use the native snapshots of
  `scripts/probe-ios-tab-motion.sh` (see
  `../../ios-native-ref/motion-probe/README.md`) rather than these recordings:
  a screen recording is lossy and variable-rate.

- **`native-switch-*.mov`** — the real `UISwitch` toggle. The switch morph is
  not interaction-gated, so the self-animating recording path is fine there.

The four lens/glass implementations (Metal shader, iOS CPU reference, JavaSE,
JavaScript) are held together by `scripts/verify-javascript-lens-parity.mjs`.
