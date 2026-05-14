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

Need per-screen styling? Set a different UIID on the parent and on the children, then write one rule per UIID.

## Supported properties

| Category | Properties |
| --- | --- |
| Background | `background-color`, `background-image`, `background-repeat`, `background-position` |
| Border | `border`, `border-color`, `border-style`, `border-width`, `border-radius`, `border-image` (9-patch) |
| Spacing | `margin`, `margin-top/right/bottom/left`, `padding`, `padding-top/right/bottom/left` |
| Text | `color`, `font-family`, `font-size`, `font-weight`, `font-style`, `text-decoration`, `text-align` (with `align` fallback) |
| Layout (per-component) | `cn1-text-align`, `cn1-include-native-bool` |
| Theme constants | Any key inside `#Constants { ... }` (custom theme constants) |

Anything not on this list — `display`, `position`, `float`, `flex`, `grid`, `transform`, `box-shadow`, `opacity` (use `cn1-opacity` selectively), `overflow`, `z-index` — is **not honored**. Use Java layouts for arrangement.

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

## Borders and 9-patch

Standard CSS borders:

```css
Card {
    border: 1px solid #cbd5e1;
    border-radius: 3mm;
}
```

For pixel-perfect non-rectangular borders (think iOS bubble), use 9-patch via `border-image`:

```css
ChatBubble {
    border-image: url('/bubble.9.png') 12 12 12 12;
}
```

The image must follow the Android 9-patch convention (1-pixel border indicating stretchable regions). Drop it in `common/src/main/css/images/` and reference it relative to the CSS root.

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

## Font handling

```css
Title {
    font-family: "native:MainBold";    /* CN1 named font alias */
    font-size: 4mm;
}
Body {
    font-family: "Roboto.ttf";          /* TTF in common/src/main/css/fonts/ */
    font-size: 3mm;
}
```

`native:Main` / `native:MainBold` / `native:Italic` etc. map to the platform default font. Bundle TTFs under `common/src/main/css/fonts/` to use custom faces — the compiler embeds them in `theme.res`.

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
| Colors look wrong on Android only | Channel order: a few older APIs expect ARGB ints, not 0xRGB. Stick to hex strings in CSS. |
| Padding ignored | You're setting padding on the Form itself; set it on its ContentPane (UIID `Container`) or wrap content in your own UIID. |
| Border radius animates jaggy | Border radius is rasterized at compile. To animate, draw via `Graphics.fillRoundRect` programmatically. |
| `text-align` does nothing | Add the `align` fallback (the initializr appends one automatically for `text-align`). |
| New CSS only takes effect after restart | The build cache may be stale — `mvn -pl common clean compile`. |

## Initializr's CSS overlays

Projects generated with theme options selected (Light/Dark/Accent/Rounded) get an auto-appended block at the bottom of `theme.css` between markers:

```css
/* Initializr Theme Overrides */
Form { background-color: #0f172a; color: #e2e8f0; }
...

/* Initializr Appended Custom CSS */
<your custom CSS>
```

Edit the overrides freely; the markers are informational, not load-bearing.

## Reaching beyond the compiler

If you need a CSS feature that doesn't exist, you have two escapes:

1. **Programmatic styling**: `comp.getAllStyles().setBgColor(0xff1d4ed8 & 0xffffff)` etc. Verbose but supports anything.
2. **Custom painters**: implement `Painter` and `comp.getAllStyles().setBgPainter(painter)`. Lets you draw arbitrary shapes/gradients.

Programmatic styling is the right hammer for animations, dynamic theming, and anything per-instance.
