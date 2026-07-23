# Evidence map

Source: `docs/website/content/blog/pixel-perfect-is-a-test.md`
Canonical: https://www.codenameone.com/blog/pixel-perfect-is-a-test/

## Thesis

How native-reference image tests make custom-rendered UI fidelity measurable

## Supported beats

- **The themes were modern, but they were not finished:** We introduced the iOS Modern and Android Material 3 themes in May. They gave new projects a current starting point, but the first iOS implementation used translucent colors where iOS 26 uses a live glass material. Several controls still carried general Codename One geometry.
- **A native app produces the answer sheet:** The fidelity suite contains two standalone reference applications. One is written with UIKit and Swift. The other uses Android's Material components. We run those apps locally on pinned native toolchains and capture real controls in light and dark appearances, including normal, pressed, selected, and disabled states.
- **What the percentage means, and what it does not:** The current Android baseline contains 54 pairs. Its median tolerant visual score is 95.5%. The worst pair is the dark outlined button in its pressed state at 91.25%. The iOS baseline contains 68 pairs with a 94.4% median.
- **The tab bar became a rendering project:** The iOS 26 tab selection is not a tinted pill sliding under icons. During a touch-driven transition, the selection becomes a magnifying lens. It travels across the bar, refracts the background and glyphs under it, stretches during flight, and settles with a small spring overshoot.
- **Glass is a typed material, not a pile of constants:** The first glass pass exposed saturation, blur, scale, offset, refraction, and specular values as unrelated theme constants. That did not look good. A toolbar, panel, button, and moving lens could each be tuned into a different material by accident.
- **CSS had to grow with the themes:** RoundBorder now supports a gradient stroke. Button gained an opt-in release overlay so iOS Modern can fade the held state over 180ms instead of snapping it away. Dialog and InteractionDialog gained opt-in centered-title layouts while keeping command rows flush with the card edges.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5274
- https://github.com/codenameone/CodenameOne/pull/5363
- https://github.com/codenameone/CodenameOne/pull/5388
- https://github.com/codenameone/CodenameOne/pull/5373
- https://github.com/codenameone/CodenameOne/pull/5379
- https://github.com/codenameone/CodenameOne/pull/5376
- https://github.com/codenameone/CodenameOne/pull/5387
- https://cloud.codenameone.com/account/security
- https://github.com/codenameone/CodenameOne/pull/5374
- https://github.com/codenameone/CodenameOne/pull/5383
- https://github.com/codenameone/CodenameOne/pull/5390

## Independent problem evidence

- Apple Liquid Glass adoption guidance: https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass
- Android Material 2 to Material 3 migration guidance: https://developer.android.com/develop/ui/compose/designsystems/material2-material3

## Product proof

- `scripts/fidelity-app/goldens/ios-26-metal/`
- `scripts/fidelity-app/goldens/android-m3/`
- `scripts/fidelity-app/goldens/ios-26-metal-frames/`
- `CodenameOne/src/com/codename1/ui/TabSelectionMorph.java`
- `maven/core-unittests/src/test/java/com/codename1/ui/TabSelectionMorphTest.java`
