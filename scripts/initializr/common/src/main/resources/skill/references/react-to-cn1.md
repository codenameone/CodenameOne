# React / JSX -> Codename One

Bringing a React (or any HTML/JSX + CSS) design into Codename One. Modern AI design
tools (Claude included) emit a React bundle: an `index.html`, some `*.jsx`
components, a `tokens.css` of design tokens and a `styles.css`. This guide maps
those concepts onto CN1 and gives the workflow that actually converges on the
design instead of eyeballing it.

> Read alongside `references/html-css-cheatsheet.md` (HTML/CSS idioms),
> `references/css.md` (CN1 CSS subset + **limitations**), and
> `references/mockup-comparison.md` (the render -> inspect -> compare loop and the
> `DumpForm` / `DescribeForm` / `AlignmentCheck` / `GuiLint` tools).

## The mental shift

React is **declarative + retained CSS**; CN1 is **imperative widgets + a baked
theme**. There is no virtual DOM diff, no re-render on state change, no fl/grid
layout, and no CSS cascade. You build a component tree once in Java, mutate it on
events, and style it via UIIDs in `theme.css`.

| React / web | Codename One |
| --- | --- |
| `ReactDOM.render(<App/>)` | `Lifecycle.runApp()` builds a `Form` and calls `form.show()` |
| JSX element tree | a `Container` tree assembled in Java |
| `className="card"` | `component.setUIID("Card")` + a `Card { ... }` rule in `theme.css` |
| `styles.css` / CSS-in-JS | `common/src/main/css/theme.css` (a CN1 CSS **subset**) |
| `tokens.css` custom properties | hardcoded hex/sizes per UIID (CN1 CSS has **no variables**) -> seed with `tools/DesignImport.java` |
| `useState` | a field (or a 1-element array in a closure) + a `refresh` runnable you call on change |
| `useEffect(fn, [])` | do it in `runApp()` / `init()`; for polling use `UITimer` |
| re-render on `setState` | mutate the component, then `form.revalidate()` (and `form.repaint()` if only paint changed) |
| `onClick` | `button.addActionListener(e -> ...)` |
| controlled `<input value=..>` | `TextField` + `addDataChangedListener` (it is already two-way) |
| `flex-direction: row` | `Container` with `BoxLayout.x()` (or `FlowLayout`) |
| `flex-direction: column` | `Container` with `BoxLayout.y()` |
| `display: grid` (N cols) | `Container` with `GridLayout(rows, N)` |
| header / body / footer | one `BorderLayout`: NORTH / CENTER / SOUTH |
| sticky bottom bar | `form.add(BorderLayout.SOUTH, bar)` |
| `position: sticky` top bar | the `Form` `Toolbar` (`form.getToolbar()`) |
| `:root` light + `[data-theme=dark]` | a `Foo` UIID and a parallel `FooDark` UIID, swapped at runtime (see Dark mode) |
| `<svg>` icon | a Material icon: `FontImage.setMaterialIcon(label, FontImage.MATERIAL_*, mm)` |

## Components

| JSX | CN1 |
| --- | --- |
| `<div>` (layout) | `Container` (+ a layout) - keep it transparent, no styling (see `css.md`) |
| `<div className="card">` | `Container` with its own UIID for the card chrome |
| `<span>` / `<p>` / `<h1>` | `Label` (one line) or `SpanLabel` (wraps) |
| `<button>` | `Button` |
| `<input type=text>` | `TextField` |
| `<textarea>` | `TextArea` |
| `<select>` | `Picker` (`Picker.setStrings(...)`) |
| `<input type=checkbox>` | `CheckBox` |
| segmented control / radio group | `RadioButton` with `setToggle(true)` in a `ButtonGroup` |
| a tooltip / popover | `InteractionDialog` shown with `showPopupDialog(sourceComponent)` (fluid, non-modal) - NOT `Dialog.show` |
| a chip / pill | a `Container` (FlowLayout) with the pill UIID + a dot `Label` + a text `Label` |
| toast / snackbar | `ToastBar.showMessage(...)` |

### SpanLabel is two components

A `SpanLabel` is a `Container` holding an (optional) icon `Label` plus the wrapping
text. `setUIID` styles the **container**; the text has its own UIID - set it with
`setTextUIID(...)`. If you only `setUIID`, the text keeps a default style (e.g. an
**opaque** background), which is the #1 cause of "white box behind white text" in
dark mode. Always set both, and make text UIIDs `background-color: transparent`.

## tokens.css -> theme.css

`tokens.css` is a palette + type scale + spacing scale as CSS custom properties
(`--color-*`, `--fs-*`, `--space-*`, `--radius-*`). CN1 CSS has **no variables**,
so every token becomes a literal per UIID. Seed it mechanically:

```bash
java tools/DesignImport.java path/to/tokens.css --out target/design-import   # defaults --px-per-mm 3.78
java tools/IsCssValid.java target/design-import/theme.css
# fold the palette + sizes from target/design-import/theme.css into common/src/main/css/theme.css
```

Then translate sizes. A web design is in **px**; CN1 sizes in **mm** (density
independent). At the desktop/browser scale CN1 effectively renders ~3.78 px/mm, so
`16px ~= 4.2mm`, `24px ~= 6.3mm`. Borders/radii in `px` stay crisp; size text and
spacing in `mm`. **Bundle the real font** if you want to match the typeface: drop
the `.ttf` files under `common/src/main/css/fonts/` and reference them with
`@font-face { font-family: "Inter"; src: url("fonts/Inter-Regular.ttf"); }` (one
`@font-face` per weight, distinct family names like `"Inter SemiBold"`).

## State and re-render

There is no automatic re-render. The idiom that scales:

```java
// one place that re-derives everything from the current state:
Runnable refresh = () -> {
    preview.setOptions(currentOptions());   // rebuild the bits that depend on state
    summary.setText(buildSummary());
    form.revalidate();
};
field.addDataChangedListener((t, i) -> refresh.run());
toggle.addActionListener(e -> { state = ...; refresh.run(); });
refresh.run(); // initial paint
```

Hold that `refresh` in a field if anything outside `runApp` (e.g. a dark-mode
toggle) must trigger it.

## Dark mode

The web flips a `data-theme` attribute and the cascade does the rest. CN1 bakes one
theme; there is no cascade. The robust pattern: author every themed UIID twice -
`Foo` (light) and `FooDark` (dark) - and **re-skin the live tree** by walking it and
swapping each component's UIID to/from its `Dark` variant, then `form.refreshTheme()`.
When you re-skin, also rebuild anything whose internals depend on the mode (e.g. a
live preview) BEFORE re-skinning, so the freshly built sub-tree gets themed too.

## Render it the way it ships, then inspect it

This is the lesson that saves the most time: **the Initializr-style UI is a
desktop/web app, so render it on the DESKTOP simulator, not a phone skin.** A phone
skin is high-DPI - `mm`-sized chrome blows up and the responsive layout never
reaches its wide form, so it looks nothing like the design. Boot it like the
generated desktop stub (`Display.init(JFrame contentPane)`, no skin, desktop
px/mm). `tools/DumpForm.java` does exactly that and emits a model you can analyse
without screenshots:

```bash
CP="common/target/classes:$(mvn -q -pl common dependency:build-classpath -Dmdep.outputFile=/dev/stdout | tail -1)"
java -cp "$CP" tools/DumpForm.java com.example.MyApp --out target/form-model.tsv
java tools/DescribeForm.java   target/form-model.tsv   # a concise, vision-free outline of the screen
java tools/AlignmentCheck.java target/form-model.tsv   # designer "guides": elements nudged off the grid
java tools/GuiLint.java        target/form-model.tsv   # nested scroll, opaque text labels, image borders, ...
```

For pixel fidelity, capture a PNG (desktop size) and score it against a PNG of the
design (render the design's own HTML headlessly, e.g. Playwright) with
`tools/CompareToMockup.java`. See `references/mockup-comparison.md`.

## Gotchas learned the hard way

- **Phone skin != desktop.** Verify desktop UIs in desktop mode (above). The phone
  skin's high DPI is why `mm` chrome looks huge.
- **Opaque text labels.** Label/SpanLabel default to an opaque background; on a
  matching surface it is invisible, on a different one (dark mode) it is a coloured
  box. Set text UIIDs `background-color: transparent`. `GuiLint` flags these.
- **Opaque structural containers.** A plain layout `Container` with no
  `background-color` paints opaque white. Give structural containers
  `background-color: transparent` so the parent shows through. `GuiLint` flags these.
- **`Display.isDarkMode()` returns a nullable `Boolean`** - never auto-unbox it.
- **`getCurrent()` in tests.** The `cn1:test` runner keeps an earlier form
  "current", and a second `show()` cannot transition while a test holds the EDT.
  Capture the form you built directly (a static handle), not `Display.getCurrent()`.
- **`fadeScrollBarBool: false`** is a global constant: it makes EVERY scrollable show
  a persistent scrollbar, including a phone-preview sub-form. Turn scrolling off on
  the bits that should not show one.
- **Borders + radius eat padding.** A rounded/`RoundRectBorder` needs padding >= the
  corner radius or it clips the text (see `css.md`). A too-large `border-radius` on a
  short label becomes an ellipse, not a pill - keep it ~= half the height.
- **CSS can silently emit a 9-piece IMAGE border** for combinations it cannot draw as
  a vector (blurry when scaled, bloats the theme). `GuiLint` flags `IMAGE-BORDER`.
- **A tiny empty `Label` with a background + radius renders a broken placeholder.**
  For a dot, use a `FontImage` circle glyph (`MATERIAL_FIBER_MANUAL_RECORD`) with a
  transparent `Style`, not a sized empty label.
