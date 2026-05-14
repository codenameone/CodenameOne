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
- **No descendant combinators**, **no attribute selectors**, **no `*`**, **no `:hover`** on touch components (desktop / JavaScript ports do expose pointer-hover listeners in Java — see `references/ui-components.md`).
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
| Background | `background-color`, `background-image`, `background-repeat`, `background-position`, gradient backgrounds (specific subset — see *Gradients* below) |
| Border | `border`, `border-color`, `border-style`, `border-width`, `border-radius`, `border-image` (9-piece) — see *Borders* below |
| Spacing | `margin`, `margin-top/right/bottom/left`, `padding`, `padding-top/right/bottom/left` |
| Text | `color`, `font-family`, `font-size`, `font-weight`, `font-style`, `text-decoration`, `text-align` (with `align` fallback) |
| Layout (per-component) | `cn1-text-align`, `cn1-include-native-bool` |
| Theme constants | Any key inside `#Constants { ... }` (see *Theme constants* below) |

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
| **`RoundBorder`** (a circle / pill shape) | Circular FAB, avatar, pill button | Use `border-radius: <bigger than half the height>;` or `Border.createRoundBorder(...)` in Java. Native-rendered, scales perfectly. |
| **`RoundRectBorder`** (rounded rectangle with optional shadow) | Cards, normal rounded buttons, dialogs | Auto-selected by the CSS compiler for moderate `border-radius` values. Supports shadow via `RoundRectBorder.create().shadowOpacity(...)`. **Always pair with padding ≥ corner radius.** |
| Solid `border: <w> solid <color>` | Plain rectangles | Vector, scales well. Fine. |
| `border-image: url('...9.png') ...;` (**9-piece image border**) | Last resort | See warning below. |

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

## Gradients

CN1 supports a small subset of gradient backgrounds. Linear and radial gradients can be configured per UIID, but the syntax is more restricted than full CSS. The canonical way to set a gradient is via the `Style` class (see `Style.setBackgroundType` constants — `BACKGROUND_GRADIENT_LINEAR_VERTICAL`, `BACKGROUND_GRADIENT_LINEAR_HORIZONTAL`, `BACKGROUND_GRADIENT_RADIAL`). The CSS compiler accepts an equivalent shorthand for these — start with a constant lookup in the `Style` JavaDoc and translate to CSS only after confirming the variant is supported.

For arbitrary CSS-style gradients (`linear-gradient(135deg, ...)` with multiple stops), the supported path is a programmatic `Painter` set via `comp.getAllStyles().setBgPainter(...)` — it's not a CSS feature.

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

Custom TTF/OTF files are **packaged with the app binary** (placed under the build output so the runtime can `Font.createTrueTypeFont(name, file)` them at startup) — they are **not** embedded inside `theme.res`. That means each font you add increases the deployed app size; choose lean subsets where possible.

To load a TTF programmatically:

```java
Font inter = Font.createTrueTypeFont("Inter", "Inter-Regular.ttf")
                 .derive(3f, Font.STYLE_PLAIN);
label.getAllStyles().setFont(inter);
```

The first argument is the family name (used to look the font up); the second is the TTF file name as packaged with the app.

## Theme constants

`#Constants { ... }` in `theme.css` exposes framework-wide booleans, numbers, strings, and image references that change CN1 component behavior. The suffix on the key name is significant: `xxxBool` → boolean, `xxxInt` → integer, `xxxImage` → image lookup, otherwise → string.

```css
#Constants {
    useLargerTextScaleBool: true;          /* honor system "Larger Text" accessibility */
    rtlBool: false;                        /* default right-to-left for the whole app */
    drawMapPointerBool: true;              /* show a center marker on MapComponent */
    centeredPopupBool: true;
    statusBarHidden: false;
}
```

Read a constant at runtime with `UIManager.getInstance().getThemeConstant(name, defaultValue)`:

```java
boolean larger = UIManager.getInstance().isThemeConstant("useLargerTextScaleBool", true);
String mapTileText = UIManager.getInstance().getThemeConstant("mapTileLoadingText", "Loading...");
```

### Commonly-needed constants

| Constant | Purpose |
| --- | --- |
| `useLargerTextScaleBool` | Honor system "Larger Text" accessibility setting via `Display.getLargerTextScale()`. |
| `rtlBool` | Set the whole app to right-to-left layout. |
| `globalToobarBool` | Whether `Toolbar` is enabled by default on every new `Form`. |
| `hideBackCommandBool` | Hide the back command from the side menu when possible. |
| `hideLeftSideMenuBool` / `hideRightSideMenuBool` | Suppress the side-menu hamburger icon on one side. |
| `iosScrollMotionBool` | Use iOS-style rubber-band scroll physics (default `true` on iOS). |
| `iosStyleBackArrowBool` | Use the iOS chevron back arrow icon. |
| `paintsTitleBarBool` | Whether the title bar contributes a background fill. |
| `pureTouchBool` | Disable focus visuals (mouse/keyboard cues) — use on pure-touch builds. |
| `dlgCommandGridBool` | Lay dialog buttons out in a grid for uniform sizing. |
| `dlgInvisibleButtons` | Hex color for the separator between dialog buttons. |
| `dialogTransitionInImage` / `dialogTransitionOutImage` | Custom `Timeline` transition images for dialogs. |
| `formTransitionIn` / `formTransitionOut` | Default form-transition names. |
| `formTransitionInImage` / `formTransitionOutImage` | Custom `Timeline` transition images for forms. |
| `fadeScrollBarBool` / `fadeScrollEdgeBool` / `fadeScrollEdgeInt` | Scrollbar/edge fade behavior. |
| `centeredPopupBool` | Center popups instead of anchoring near the source component. |
| `menuImage` / `menuImageSize` | Hamburger menu icon override and size. |
| `infiniteImage` / `infiniteMaterialDesignSize` / `infiniteMaterialImageSize` / `infiniteDefaultColor` | `InfiniteProgress` appearance. |
| `mapTileLoadingImage` / `mapTileLoadingText` | `MapComponent` tile-loading placeholders. |
| `mapZoomButtonsBool` / `drawMapPointerBool` | `MapComponent` toggles. |
| `imageviewerNavigationArrowsBool` / `imageviewerThumbnailsBool` / `imageviewerThumbnailBarHeightMM` | `ImageViewer` defaults. |
| `mediaPlayImage` / `mediaPauseImage` / `mediaFwdImage` / `mediaBackImage` | Media-player icon overrides. |
| `labelGap` / `listItemGapInt` | Default gaps inside compound components. |
| `dlgButtonCommandUIID` / `dlgCommandButtonSizeInt` | Dialog button styling. |
| `comboImage` | Dropdown arrow image used by Picker/legacy ComboBox. |
| `checkBoxCheckedImage` / `checkBoxUncheckedImage` / `checkBoxCheckDisImage` / `checkBoxUncheckDisImage` / `checkBoxOppositeSideBool` | Custom checkbox iconography. |
| `defaultCommandImage` / `defaultEmblemImage` | Fallback icons for command/list rendering. |
| `iconUiid` / `textUiid` | UIID overrides for icon/text inside `SpanLabel` / `SpanButton` / `MultiButton` / `SpanMultiButton`. |
| `interactionDialogSpeedInt` | `InteractionDialog` slide duration in ms (defaults to 400). |
| `DecayMotionScaleFactorInt` | Velocity-to-distance multiplier for exponential-decay scroll motion (default 950). |
| `disabledColor` | Default color used when disabling components. |

(The full list is in the Codename One Developer Guide under *Advanced Theming → Theme constants*: <https://www.codenameone.com/developer-guide/>.)

## Java side of styling: `setUIID`, `getStyle`, `getAllStyles`

```java
Component cmp = ...;

cmp.setUIID("PrimaryCta");                    // (1) pick a CSS rule
cmp.getAllStyles().setBgColor(0x1d4ed8);      // (2) per-instance override, write-only
int currentBg = cmp.getStyle().getBgColor();  // (3) read effective style for the current state
```

- **`setUIID(String)`** is the **only** way to apply a theme rule. Whenever you create a component and want it styled, give it a UIID, then write the rule in `theme.css`. Don't reach for the style API for things CSS can express — themed UIIDs are reusable and theme-overridable; programmatic styling is one-off.

- **`getStyle()`** returns the style for the **currently selected state** (one of `getUnselectedStyle()`, `getSelectedStyle()`, `getPressedStyle()`, `getDisabledStyle()`). It is **read-only** in spirit — mutating it only modifies one state and tends to produce confusing results during interactions. Use `getStyle()` to **read** the effective values for the current state (e.g. in a test that verifies a screen rendered correctly).

- **`getAllStyles()`** returns a fan-out style object that writes to **all four** state styles at once. It is the right hammer for "set this color on the component" overrides. Treat it as **write-only** — don't read values from it; the values you read may not match the state in effect, and the cross-state aggregation is for setters only.

```java
// Test pattern — verify CSS applied:
Button save = new Button("Save");
save.setUIID("PrimaryCta");
form.add(save);
form.show();
assertEqual(0x1d4ed8, save.getUnselectedStyle().getBgColor(),
        "PrimaryCta should map to accent in the theme");

// One-off programmatic override — animation, dynamic theming:
errorBanner.getAllStyles().setBgColor(0xb91c1c);
```

## Validating styles from a test

You generally don't validate CSS at the CSS level. Instead build the component, attach it to a Form, then read its effective `Style` from a unit test:

```java
public class PrimaryCtaStyleTest extends AbstractTest {
    @Override public boolean shouldExecuteOnEDT() { return true; }
    @Override public boolean runTest() {
        Button btn = new Button("Submit");
        btn.setUIID("PrimaryCta");
        Form f = new Form("Test", BoxLayout.y());
        f.add(btn);
        f.show();

        Style s = btn.getUnselectedStyle();
        assertEqual(0x1d4ed8, s.getBgColor(), "Accent applied to background");
        assertEqual(0xffffff, s.getFgColor(), "Foreground forced to white");
        assertNotNull(s.getBorder(),           "Border attached");
        return true;
    }
}
```

This is more useful than checking raw CSS strings: it confirms the rule made it into `theme.res`, was matched at runtime, and resolved to the values you expect. Plus it catches regressions like UIID typos, accidental cascading, and resource cache staleness — symptoms that pure CSS validation would miss.

If the test fails and you want to inspect every UIID/key the theme actually contains:

```java
java.util.Hashtable<String, Object> map = UIManager.getInstance().getThemeProps();
for (Object k : map.keySet()) System.out.println(k);
```

That's the canonical "what's actually in the theme?" introspection.

## Animations — use transitions and `animate()`, not CSS

CSS does not animate anything in CN1. The two right tools, in priority order:

1. **`Form.animateLayout(durationMs)`** — for layout-driven animations. Mutate `setHidden`, change a child's UIID/visibility, swap LayoutConstraints, then call `animateLayout(250)`. CN1 tweens children from their old positions/sizes to the new ones. Use this for hide/show, panel push-in, expanding cards.

2. **CN1 transitions (`CommonTransitions`, `MorphTransition`, `Form#setTransitionInAnimator`/`setTransitionOutAnimator`)** — for whole-screen transitions (slide, fade) and component-to-component morphs across Forms. Set the desired transition on a Form before calling `show()`.

   ```java
   nextForm.setTransitionInAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 250));
   nextForm.show();
   ```

3. **Per-component animation via `Component.animate()` + `setAnimation(true)`** — for "I want a value to drift over N ms". Override `animate()` on a Component, return `true` to keep the animation alive, and CN1 calls it once per frame. Register the animation with `getComponentForm().registerAnimated(this)`.

   ```java
   class Pulse extends Container {
       private Motion m = Motion.createEaseInOutMotion(0, 255, 600);
       @Override public boolean animate() {
           int alpha = m.getValue();
           getAllStyles().setBgTransparency(alpha);
           if (m.isFinished()) m.start();   // loop
           return true;                     // keep ticking
       }
       @Override protected void initComponent() {
           super.initComponent();
           m.start();
           getComponentForm().registerAnimated(this);
       }
   }
   ```

Painters are for **drawing** (custom backgrounds, decorations), not for animating. A `Painter` that mutates state to look animated won't actually be re-painted unless something else triggers a repaint — and that something else is the `animate()` mechanism above. Use `animate()` to drive state changes; use `Painter` only for one-off drawing.

## Common pitfalls

| Symptom | Cause / fix |
| --- | --- |
| Style doesn't apply | Write a `Style` test (see *Validating styles from a test* above). Check the UIID is set on the component; check the colors via `getUnselectedStyle().getBgColor()`; iterate `UIManager.getInstance().getThemeProps().keySet()` to confirm the theme actually contains the entry. |
| Container has unexpected background / padding | The base `Container` UIID was restyled. Restore it to transparent / 0 padding / 0 margin and apply styling on a child UIID instead. |
| Text clipped by rounded corners | Padding too small relative to `border-radius`. Increase padding so it's ≥ the radius. |
| Padding ignored on the Form | You're setting padding on the Form itself; set it on its ContentPane or wrap content in your own UIID. |
| Border radius animates jaggy | Border radius is rasterized at compile when using image fallbacks; switch to `RoundRectBorder` and animate via `Form.animateLayout(...)`. |
| `text-align` does nothing | Add the `align` fallback (the initializr appends one automatically for `text-align`). |
| New CSS only takes effect after restart | The build cache may be stale — `mvn -pl common clean compile`. |
| 9-piece border looks blurry on iPhone Pro | Expected — 9-piece images are rasterized at the bundled resolution. Use a vector border (`RoundBorder`/`RoundRectBorder`) instead. |
| Custom TTF doesn't render on device but works in simulator | The `@font-face` `src:` filename and the JS-side `Font.createTrueTypeFont(name, file)` filename must match exactly, and the file must end up packaged with the app. Re-check spelling and confirm the file is under `common/src/main/css/fonts/`. |

## Reaching beyond the compiler

If you need a CSS feature that doesn't exist, you have two escapes:

1. **Programmatic styling** via `comp.getAllStyles().setXxx(...)`. Verbose, write-only, but supports anything. See *Margin and padding from Java* in `references/ui-components.md` for the unit pitfall.
2. **Custom painters** — implement `Painter` and `comp.getAllStyles().setBgPainter(painter)`. Lets you draw arbitrary shapes/gradients. Pair with `animate()` if the painted state needs to change over time.

Programmatic styling is the right hammer for animations, dynamic theming, and anything per-instance.
