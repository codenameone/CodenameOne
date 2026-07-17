# Animation references — how they are captured and what they answer

- **`native-tabs-*.mov`** — the REAL `UITabBar` Liquid Glass selection morph,
  recorded from the NativeRef app on the iOS 26 simulator runtime named by
  this golden set (`scripts/record-ios-native-anim.sh tabs <light|dark>`).

  **The recording MUST be tap-driven.** UIKit plays the full Liquid Glass
  morph — the frosted refracting drop that bulges past the bar with chromatic
  rims — only for genuine touch-driven selection on a `UITabBarController`;
  programmatic `selectedIndex`/`selectedItem` changes play a flat simplified
  platter slide, and a bare `UITabBar` never shows the full effect at all. An
  earlier reference was mis-recorded through the programmatic path, and its
  flat drop briefly led the CN1 tuning astray. The recording script now drives
  real taps through the XCUITest bundle in
  `../../ios-native-ref/tap-driver/` (requires `xcodegen`, like the
  input-validation iOS driver).

- **`cn1-tabs-*.mov`** — the Codename One morph in motion, rendered
  deterministically from the JavaSE simulator at the reference density
  (60 fps, the theme's 480 ms `tabsAnimatedIndicatorDurationInt` timeline,
  1088x290 tile). The CN1 side of the same motion, at the same tile the
  `../ios-26-metal-frames/TabsMorph_*` goldens use.

- **`native-switch-*.mov`** — the real `UISwitch` toggle; the switch morph is
  not interaction-gated, so the self-animating recording path is fine there.

The four lens/glass implementations (Metal shader, iOS CPU reference, JavaSE,
JavaScript) are held together by `scripts/verify-javascript-lens-parity.mjs`.
