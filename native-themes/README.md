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
- `padding`, `margin`, typography (`font-family`/`font-size`/`font-weight`).
- `cn1-derive`, `cn1-image-id` (resource images shipped as PNG), `cn1-mutable-image`.
- `cn1-source-dpi` for multi-DPI image variants.
- `.pressed`, `.selected`, `.unselected`, `.disabled` state selectors (dot-class
  syntax — the CN1 CSS compiler translates these to the binding state of the
  UIID, not CSS classes in the HTML sense).
- `@media (prefers-color-scheme: dark)` for dark palette overrides.
- `var(--x)` and `@constants { ... }`.

**Forbidden (trigger CEF):**

- `box-shadow`, `cn1-box-shadow-*` -> 9-piece fallback.
- `border-radius` combined with a visible border -> 9-piece fallback.
- Mixed border widths/styles/colors per side.
- `filter`.
- Complex `linear-gradient` / `radial-gradient` that can't be expressed as a
  native gradient.

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
