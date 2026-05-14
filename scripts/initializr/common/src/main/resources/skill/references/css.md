# Codename One CSS Reference

`common/src/main/css/theme.css` is the entry point. The CN1 plugin's CSS compiler (`compile-css` goal, runs in `process-resources`) parses it and bakes the result into `common/target/classes/theme.res`. The runtime then loads styles by UIID from that binary resource — no CSS exists at runtime.

This is **not** web CSS. It is a deliberate subset designed for native rendering on mobile. Treat any unfamiliar property as "probably unsupported" until you check.

## Selector model

The only selector form is:

```
<UIID>[.<state>] [, <UIID>[.<state>] ...] { ... }
```

- `UIID` matches `component.getUIID()` — e.g. `Button`, `Form`, `Label`, `Title`, `Toolbar`, `MyCustomCard`.
- `.<state>` matches one of the built-in style variants: `.pressed`, `.disabled`, `.selected`.
- Multiple UIIDs may be grouped with commas.
- **No descendant combinators**, **no attribute selectors**, **no `*`**, **no `:hover`** (mobile has no hover).
- **`@media` queries**: only `@media (prefers-color-scheme: dark)` is honored — see *Dark mode* below. No viewport-size media queries.

```css
/* Valid */
Button { ... }
Button, Label { ... }
Button.pressed { ... }
MyCard.selected { ... }
@media (prefers-color-scheme: dark) {     /* honored — rewrites selectors as Dark variants */
    Form { background-color: #0f172a; }
}

/* INVALID — these silently fail or break the compile */
Form Button { ... }              /* no descendant combinator */
Button:hover { ... }              /* no :hover */
@media (max-width: 600px) { ... } /* no size media queries */
.btn { ... }                      /* class selectors don't exist; use UIIDs */
```

## Special case: `Container` UIID

`Container` is a structural component, **not** a styled one. Layout containers wrap other components; they should not paint backgrounds, borders, or contribute spacing of their own. Treat the default `Container` UIID as load-bearing:

- **Never restyle `Container`.** Don't add background-color, border, padding, or margin to a rule targeting `Container { ... }`. Many built-in screens nest `Container`s several deep; styling the base UIID at theme level produces gnarly visual surprises (double padding, accent color bleeding into the wrong region, layouts that shift on certain platforms).
- The default style for `Container` should remain: **transparent background, no border, 0 padding, 0 margin**. If the initializr or a cn1lib has accidentally restyled it, restore those defaults before doing anything else.
- If you need a styled "box" (a card, a banner, a section), give the wrapper its **own** UIID (e.g. `Card`, `HeroSection`) and style that UIID instead. The Java side mirrors this: `Container c = new Container(BoxLayout.y()); c.setUIID("Card");`.

The same caveat applies to `ContentPane` and `CenterAlignedContentPane` (the Form's inner container) to a lesser extent — change them only when you specifically want to recolor the form's background and you understand the cascading impact.

## Modern theme accent colors

Initializr projects generated since the introduction of the modern theme inherit a base palette plus an **accent color**. The fastest way to recolor a new app is to override the accent. The relevant UIIDs are bound to it:

```css
/* In theme.css — set this near the top, before other overrides. */
Button {
    background-color: #1d4ed8;     /* your accent */
    color: #ffffff;
    border: 1px solid #1d4ed8;
    border-radius: 3mm;
}
Button.pressed {
    background-color: #1e3a8a;     /* ~22% darker */
    border: 1px solid #1e3a8a;
}
Title, TitleCommand { color: #1d4ed8; }
```

This is the right entry point for "make the app look like our brand". Sweeping changes can be applied this way with no need to subclass components.

## Supported properties

| Category | Properties |
| --- | --- |
| Background | `background-color`, `background-image`, `background-repeat`, `background-position` |
| Border | `border`, `border-color`, `border-style`, `border-width`, `border-radius`, `border-image` (9-piece) — see *Borders* below |
| Spacing | `margin`, `margin-top/right/bottom/left`, `padding`, `padding-top/right/bottom/left` |
| Text | `color`, `font-family`, `font-size`, `font-weight`, `font-style`, `text-decoration`, `text-align` (with `align` fallback) |
| Layout (per-component) | `cn1-text-align`, `cn1-include-native-bool` |
| Theme constants | Any key inside `#Constants { ... }` (custom theme constants) |

Anything not on this list — `display`, `position`, `float`, `flex`, `grid`, `transform`, `box-shadow`, `opacity`, `overflow`, `z-index` — is **not honored**. Use Java layouts for arrangement.

## No negative values

`margin: -2mm` is **not supported**. The CN1 layout system does not allow negative margins, padding, or border insets. If a CSS rule resolves to a negative value the compiler either ignores it or clamps to zero, depending on the property.

If you find yourself wanting `margin-top: -2mm` (e.g. to overlap a card on top of a hero image), do it the CN1 way: use `LayeredLayout` with percent/mm insets — it's purpose-built for overlap.

## The CN1 box model — why borders need matching padding

In standard CSS the **border draws between padding and margin**, and the border has zero effect on the inner content area beyond its stroke width.

In Codename One the **border is painted on top of the component's edge**, inside the padding region. If the underlying component has no padding, a thick or rounded border will visually clip the content. As a rule of thumb:

> Whenever you set a non-trivial `border` (especially `RoundBorder`, `RoundRectBorder`, or a 9-piece image border), set padding to **at least the visual thickness of that border**. Otherwise the label/text inside the bordered component gets eaten by the border curve.

Example — a rounded primary button:

```css
PrimaryCta {
    background-color: #1d4ed8;
    color: #ffffff;
    border: 1px solid #1d4ed8;
    border-radius: 3mm;
    padding: 2mm 4mm;     /* MUST be >= the corner radius so text isn't clipped */
}
```

Symptoms when padding is too small:
- Text appears off-center or clipped at the curved corners.
- Tap target is smaller than it looks.
- A focused/pressed state visibly resizes the component because pressed-state border is thicker.

This is also why setting `border-radius: 10mm` on a tiny `Label` with `padding: 0` produces a label that looks broken — the rounded border is drawn but eats the text.

## Borders — recommended types, and what to avoid

CN1 supports four practical border styles. Two of them are the right tools 99% of the time; two exist for compatibility and should be considered fallbacks.

| Style | When to use | Notes |
| --- | --- | --- |
| **`RoundBorder`** (a circle / pill shape) | Circular FAB, avatar, pill button | Created via CSS `border-radius: <bigger than half the height>;` or `Border.createRoundBorder(...)` in Java. Native-rendered, scales perfectly. |
| **`RoundRectBorder`** (rounded rectangle with optional shadow) | Cards, normal rounded buttons, dialogs | Auto-selected by the CSS compiler for moderate `border-radius` values. Supports shadow (`box-shadow`-like) via `RoundRectBorder.create().shadowOpacity(...)`. **Always pair with padding ≥ corner radius.** |
| Solid `border: <w> solid <color>` | Plain rectangles | Vector, scales well. Fine. |
| `border-image: url('...9.png') ...;` (**9-piece image border**) | Last resort: chat bubbles, drop shadows, complex shapes | See warning below. |

**Why the 9-piece image border is a fallback**, not a default:

- It is rasterized: a fixed-resolution PNG stretched at runtime, so it does not scale crisply across device densities. On a high-DPI screen it gets blurry; on a low-DPI screen it can alias badly.
- It costs memory: the image is decoded and held in the resource map.
- It is brittle: the 1-pixel control border that defines the stretchable region must be edited carefully (Android Studio's draw-9-patch tool is the canonical authoring path). If the wrong pixels mark "stretch" you get visible seams.
- **The screenshot tests in `references/testing-and-screenshots.md` are particularly sensitive to 9-piece borders** — even minor cross-device anti-aliasing differences accumulate around the stretched seams and push the comparison over the tolerance threshold.

Prefer `RoundBorder` / `RoundRectBorder` for any rounded UI. Reach for `border-image` only when you need a shape neither vector border can produce (asymmetric bubble tails, decorative frames, etc.). When you must, drop the image at `common/src/main/css/images/<name>.9.png` and reference it as `border-image: url('/<name>.9.png') <top> <right> <bottom> <left>;`.

## Units

`mm`, `px`, `%`, named integers. **Prefer `mm`** because pixel density varies wildly across devices:

```css
Button {
    padding: 2mm 4mm;         /* GOOD — same physical size on any screen */
    border-radius: 3mm;
    margin: 1mm;
}
Button {
    padding: 12px;            /* BAD — tiny on 3x density, huge on 1x */
}
```

CN1 internally converts `mm` to pixels via `Display.convertToPixels(float)`.

## Colors

```css
Button {
    background-color: #1d4ed8;     /* hex, the most reliable form */
    color: rgb(255, 255, 255);     /* rgb() works */
    border: 1px solid #475569;
}
```

**Named colors**: only a few are recognized by the initializr's pre-processor (`pink`, `orange`, `purple`, `yellow`, `gray`, `grey`). Beyond that, **use hex**. CSS like `color: red;` may not compile.

Alpha: use `rgba(r, g, b, a)` where `a` is 0–255 in some compiler versions and 0.0–1.0 in others — test it. The safe alternative is to leave the color opaque and animate transparency programmatically via `setAlpha(int 0..255)` on the component style.

## Dark mode

CN1 supports a single, specific media query: `@media (prefers-color-scheme: dark)`. The CSS compiler walks rules inside that block and rewrites every selector into a `$Dark<UIID>` variant baked into `theme.res`. At runtime CN1 picks the dark variant when the platform reports dark mode (see `Display.getInstance().isDarkMode()` — also overridable with `setDarkMode(Boolean)`).

```css
Form { background-color: #ffffff; color: #0f172a; }
Toolbar { background-color: #ffffff; }
Button { background-color: #f1f5f9; color: #0f172a; }

@media (prefers-color-scheme: dark) {
    Form { background-color: #0f172a; color: #e2e8f0; }
    Toolbar { background-color: #0f172a; }
    Button { background-color: #1e293b; color: #e2e8f0; }
}
```

Both the light and dark blocks must define the same UIIDs you want to recolor — the dark block does *not* inherit declarations from the light block; the compiler creates an entirely separate Dark variant per UIID.

Don't try to nest other `@media` queries inside — viewport-size queries, prefers-reduced-motion, etc. are not recognized.

## Fonts — `native:` prefix and the platform mapping

CN1 ships ten `native:` font aliases. They resolve at runtime to the platform's default UI typeface (Roboto on Android, San Francisco on iOS, system font on desktop), at the requested weight.

**Main family** (upright):

| `font-family` | Android (Roboto) | iOS (San Francisco) |
| --- | --- | --- |
| `native:MainThin` | Roboto Thin | SF Thin |
| `native:MainLight` | Roboto Light | SF Light |
| `native:MainRegular` | Roboto Regular | SF Regular |
| `native:MainBold` | Roboto Bold | SF Bold |
| `native:MainBlack` | Roboto Black | SF Black |

**Italic family** (slanted):

| `font-family` | Android (Roboto) | iOS (San Francisco) |
| --- | --- | --- |
| `native:ItalicThin` | Roboto Thin Italic | SF Thin Italic |
| `native:ItalicLight` | Roboto Light Italic | SF Light Italic |
| `native:ItalicRegular` | Roboto Italic | SF Regular Italic |
| `native:ItalicBold` | Roboto Bold Italic | SF Bold Italic |
| `native:ItalicBlack` | Roboto Black Italic | SF Black Italic |

(The simulator typically resolves these to bundled Roboto faces — what you see in the simulator should match Android.)

Aliases shorthand `native:Main`, `native:MainBold`, `native:Italic` map to `native:MainRegular`, `native:MainBold`, `native:ItalicRegular` respectively.

```css
Title { font-family: "native:MainBold"; font-size: 4mm; }
Body  { font-family: "native:MainRegular"; font-size: 3mm; }
Caption { font-family: "native:ItalicLight"; font-size: 2.5mm; }
```

### Custom TTF fonts

Drop a `.ttf` (or `.otf`) under `common/src/main/css/fonts/`, then reference its **font name (not file name)** in `font-family`:

```css
@font-face {
    font-family: "Inter";
    src: url("fonts/Inter-Regular.ttf");
}
@font-face {
    font-family: "Inter Bold";
    src: url("fonts/Inter-Bold.ttf");
}

Title { font-family: "Inter Bold"; font-size: 4mm; }
Body  { font-family: "Inter"; font-size: 3mm; }
```

The compiler embeds the TTF into `theme.res`. At runtime CN1 looks the font up by the family name you declared. To load a TTF programmatically:

```java
Font inter = Font.createTrueTypeFont("Inter", "Inter-Regular.ttf")
                 .derive(3f, Font.STYLE_PLAIN);
label.getAllStyles().setFont(inter);
```

The first argument is the family name (used to register it in the theme); the second is the TTF file name inside `theme.res`.

## Theme constants (`#Constants`)

A special "selector" used to set framework-wide booleans/numbers/strings that CN1 reads at runtime:

```css
#Constants {
    useLargerTextScaleBool: true;       /* honor system "Large Text" accessibility */
    rtlBool: false;                     /* right-to-left layout */
    drawMapPointerBool: true;
    pureTouchBool: true;
    centeredPopupBool: true;
    statusBarHidden: false;
}
```

The naming convention `xxxBool` / `xxxInt` / `xxxImage` is required — the CSS compiler reads the suffix as the value type.

`useLargerTextScaleBool: true` is added by default for barebones templates because mobile users with accessibility scaling enabled otherwise get unreadably small text.

## Live preview while editing

`mvn -pl common cn1:run` runs the simulator with a CSS file watcher: save `theme.css` and the simulator hot-reloads styles without restarting. This is the fastest iteration loop for visual work.

## Compiling CSS programmatically (for tools/tests)

```java
import com.codename1.ui.css.CSSThemeCompiler;
import com.codename1.ui.util.MutableResource;

MutableResource res = new MutableResource();
new CSSThemeCompiler().compile(cssString, res, "MyTheme");
boolean ok = res.getTheme("MyTheme") != null;
```

Use this to validate generated CSS in tests.

## Common pitfalls

| Symptom | Likely cause |
| --- | --- |
| Style doesn't apply | UIID typo. Default UIIDs are `Button`, `Label`, etc. — case-sensitive. |
| Container has unexpected background / padding | The base `Container` UIID was restyled. Restore it to transparent / 0 padding / 0 margin and apply styling on a child UIID instead. |
| Text clipped by rounded corners | Padding too small relative to `border-radius`. Increase padding so it's ≥ the radius. |
| Colors look wrong on Android only | Channel order: a few older APIs expect ARGB ints, not 0xRGB. Stick to hex strings in CSS. |
| Padding ignored on the Form | You're setting padding on the Form itself; set it on its ContentPane or wrap content in your own UIID. |
| Border radius animates jaggy | Border radius is rasterized at compile when using image fallbacks; switch to `RoundRectBorder` and animate via `Form.animateLayout(...)`. |
| `text-align` does nothing | Add the `align` fallback (the initializr appends one automatically for `text-align`). |
| New CSS only takes effect after restart | The build cache may be stale — `mvn -pl common clean compile`. |
| 9-piece border looks blurry on iPhone Pro | Expected — 9-piece images are rasterized at the bundled resolution. Use a vector border (`RoundBorder`/`RoundRectBorder`) instead. |

## Reaching beyond the compiler

If you need a CSS feature that doesn't exist, you have two escapes:

1. **Programmatic styling**: `comp.getAllStyles().setBgColor(0xff1d4ed8 & 0xffffff)` etc. Verbose but supports anything. Margin/padding units are explicit — see *Margin and padding from Java* in `references/ui-components.md`.
2. **Custom painters**: implement `Painter` and `comp.getAllStyles().setBgPainter(painter)`. Lets you draw arbitrary shapes/gradients.

Programmatic styling is the right hammer for animations, dynamic theming, and anything per-instance.
