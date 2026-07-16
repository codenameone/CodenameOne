# Animation references — which video is authoritative for what

Two families of motion references live here, and they intentionally disagree
about the tab morph's material:

- **`native-tabs-*.mov`** — the REAL `UITabBar` transition recorded from the
  NativeRef app on the iOS 26 simulator runtime named by this golden set
  (re-record with `scripts/record-ios-native-anim.sh tabs <light|dark>`). On
  the original iOS 26 runtime the drop was a pronounced frosted, refracting
  bubble (see the developer-guide fidelity image); Apple toned the effect down
  in later 26.x updates, so a fresh recording shows a flat, compact drop.
  These recordings answer "what does current UIKit do" — they are the honest
  native baseline for TIMING and TRAVEL, **not** the material target.

- **`cn1-tabs-*.mov`** — the Codename One morph in motion, rendered
  deterministically from the JavaSE simulator at the reference density
  (60 fps, the theme's 480 ms `tabsAnimatedIndicatorDurationInt` timeline,
  1088x290 tile). This is the SHIPPED look — the frosted-bubble material
  pinned by `../ios-26-metal-frames/TabsMorph_*` and the developer-guide
  fidelity image — kept deliberately at the original iOS 26 design.
  **Tune the morph model, lens constants, and theme against THIS and the
  frame goldens, never against the flat `native-tabs-*.mov` material.**

- **`native-switch-*.mov`** — the real `UISwitch` toggle; unaffected by the
  above (the switch motion did not change between 26.x updates).

The four lens/glass implementations (Metal shader, iOS CPU reference, JavaSE,
JavaScript) are held together by `scripts/verify-javascript-lens-parity.mjs`.
