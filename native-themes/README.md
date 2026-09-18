# Native theme CSS sources

This directory holds the Codename One platform native themes authored in CSS.
They are compiled by `scripts/build-native-themes.sh` (which invokes the thin
`maven/css-compiler` jar with `strictNoCef=true`) into `.res` files under the
repo's `Themes/` directory, alongside the legacy hand-authored themes.

## Layout

```
native-themes/
  base/                  shared tokens, @constants, @font-face (future)
  ios-modern/theme.css   iOS liquid-glass theme
  android-material/theme.css   Android Material 3 theme
```

Each `theme.css` is fed directly to the compiler. Until `@import` support is
confirmed in Flute/SAC, `theme.css` is a single self-contained file (no
`@import`).

## Authoring rules

Because these themes ship inside the port jars, rasterized image fallbacks
are forbidden. The compiler runs in `strictNoCef` mode: any rule that would
require CEF rasterization fails the build and lists the offending UIID.

**Allowed:**

- Solid `color` / `background-color`.
- `cn1-round-border`, `cn1-pill-border`, simple matched-side `border`.
- `border-radius`, including **combined with a visible border** -- that pair
  compiles to a `RoundRectBorder`, which is a native primitive, not a 9-piece
  image.
- **Per-side border widths and colours**, including with a radius. These compile
  to a `CSSBorder`, which carries each side natively. This is what the Fluent
  text field's bottom accent stroke is made of.
- **`box-shadow`**, as long as it is a single, non-`inset`, **black** shadow
  with explicit zero blur, positive spread (at least `1px` when using pixels), and absolute
  offsets whose magnitude does not exceed that spread.
  It compiles to `RoundRectBorder`'s own shadow -- shadowX, shadowY, shadowBlur,
  shadowSpread and shadowOpacity all round-trip through the resource format
  (`Resources` cases 0xff13 and 0xff15), so elevation is drawn, not rasterized.
  An alpha on the colour is the point: it becomes the shadow opacity.
- `filter: blur(...)` and colour-matrix filters. These are native: they compile to
  `filterBlur` / `filterColorMatrix` theme properties (and the `backdrop-` variants),
  not to a rasterized image. The older version of this list called `filter` forbidden,
  which was never true of the blur and colour-matrix forms.
- `padding`, `margin`, typography (`font-family`/`font-size`/`font-weight`).
- `cn1-derive`, `cn1-image-id` (resource images shipped as PNG), `cn1-mutable-image`.
- `cn1-source-dpi` for multi-DPI image variants.
- `.pressed`, `.selected`, `.unselected`, `.disabled`, `.hover` state selectors
  (dot-class syntax — the CN1 CSS compiler translates these to the binding state
  of the UIID, not CSS classes in the HTML sense).
- `.hover` is the desktop state and is **opt-in per UIID**: hover keys are emitted
  only for a UIID that actually declares a `.hover` rule, and at runtime
  `Component.getHoverStyle()` returns null for any UIID the theme says nothing
  about. That is deliberate -- a hover style conjured for an undeclared UIID would
  be built from the blank default style and would repaint the component white the
  moment the pointer crossed it. Hover outranks focus and is outranked by pressed
  and disabled.
- `@media (prefers-color-scheme: dark)` for dark palette overrides.
- `var(--x)` and `@constants { ... }`.

**Forbidden (trigger CEF):**

- A blurred `box-shadow`, even when its blur is smaller than its spread. The
  software painter has no separate CSS blur halo allocation and its cached fast
  path skips Gaussian blur. These shadows need rasterization.
- A `box-shadow` with omitted, zero, negative, or subpixel pixel spread. CSS
  omitted spread is zero; the native software painter requires positive spread,
  and its constructor default is not equivalent to CSS zero.
- A shadow offset larger in magnitude than its spread, or a relative-unit offset.
  Native shadow positions are ratios in `[0,1]`; out-of-range positions clip.
- An **`inset`** `box-shadow`. `RoundRectBorder` only draws an outer drop shadow.
- A **tinted** `box-shadow`. The resource format stops at shadowY and never reads
  a shadow colour, so a coloured shadow would round-trip to black and lose its hue
  with nothing downstream able to notice. Refusing it is the honest outcome.
- A `box-shadow` **combined with per-side (unequal) borders**. `RoundRectBorder`
  draws the shadow but cannot do unequal sides; `CSSBorder` does unequal sides but
  not shadows. Pick one per UIID.
- `background-image: url(...)` and `cn1-9patch`.
- A gradient that is not a CN1 native gradient. A simple two-stop `linear-gradient`
  is native and compiles fine. **`radial-gradient` currently hangs the compiler**
  rather than failing cleanly -- a pre-existing defect, not a rule; avoid it and do
  not read a hung build as your rule being rejected.

Each line above is measured against the compiler rather than assumed -- the
earlier version of this list forbade rounded-plus-bordered boxes, per-side border
colours, all box shadows and all filters, and only the box shadows were ever
actually refused. `CSSBoxShadowNativeBorderTest` in `maven/css-compiler` pins the shadow
boundary in both directions.

If a visual effect isn't in the allowed list, extend the CSS compiler and/or
`.res` format with a new native primitive -- don't rasterize.

## Mandatory constants

Each theme must declare these in `#Constants`:

- `includeNativeBool: false` -- native themes are the base; user themes set
  this to `true` and inherit from us. If we set it to `true` ourselves we'd
  try to inherit from ourselves and recurse at load time.
- `darkModeBool: true` -- enables UIManager's `$Dark<UIID>` style resolution,
  which is populated from the theme's `@media (prefers-color-scheme: dark)`
  blocks.

## cn1-derive inheritance rule

`cn1-derive` only works reliably when the derived UIID is a straightforward
refinement of the base (child refines parent). Examples that are fine:

- `SecondaryLabel { cn1-derive: Label; ... }`
- `MainTitle { cn1-derive: Title; ... }`
- `RaisedButton { cn1-derive: Button; ... }`
- `SelectedTab { cn1-derive: Tab; ... }`

Examples that were problematic and are now inlined:

- `TitleArea -> Toolbar` hung the iOS UIManager style resolver after
  `setThemeProps()` swapped in the theme mid-flight. Both themes inline
  Toolbar's props directly into TitleArea.
- `DialogTitle -> Title`, `DialogBody -> Dialog`, `PopupContent -> Dialog`
  are cross-context (different UIIDs, not refinement). Inlined.
- `TextArea -> TextField`, `RadioButton -> CheckBox` are specializations
  rather than refinements. Inlined for simplicity.

Rule of thumb: if a reader would have to check the base UIID to understand
the derived one, inline instead.

## Future: real backdrop-filter glass

The iOS 26 tab bar (and equivalent Material 3 surfaces) use an OS-provided
backdrop blur (UIVisualEffectView on iOS, RenderEffect on Android). The
current CSS approximates it with a solid surface-container color on the
tabs group; a real glass effect will need a new CSS primitive
(`cn1-backdrop-filter: glass(<intensity>)`) and port-side code that maps
it to UIVisualEffectView / RenderEffect. That lands in a separate PR.

## Rebuilding

```
./scripts/build-native-themes.sh
```

Outputs:

- `Themes/iOSModernTheme.res`
- `Themes/AndroidMaterialTheme.res`
