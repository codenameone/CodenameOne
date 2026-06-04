# Mockup Comparison Reference

Building a screen to match a designer mockup is hard to do "by eye": you cannot tell *how close*
the render is to the target, so you cannot converge reliably. This skill ships two tools that turn
that into a measurable loop:

- `tools/CompareToMockup.java` - scores a rendered screenshot against a mockup image and prints a
  similarity percentage. Supports a **partial/region mode** so device chrome in the mockup (a
  status-bar mock, a home indicator) does not sabotage the score.
- `tools/DesignImport.java` - parses a Figma / Sketch / Adobe XD design into a **starter**
  `theme.css` + `tokens.json` + `layout.md`, so you begin near the target instead of from scratch.

Plus `templates/MockupComparisonTest.java`, a copy-paste test that renders a screen and saves a PNG
to a predictable path for the comparison tool to read.

> This is the *correctness* counterpart to `screenshotTest` (see
> `references/testing-and-screenshots.md`). `screenshotTest` compares a render to a baseline the
> system produced itself - that proves **consistency**, not correctness. A designer mockup is an
> **independent reference**, so comparing against it actually measures whether you built the right
> thing.

## The loop

1. **(Optional) Import the design** to seed your styling:
   ```bash
   # Figma (needs a personal access token + the file key from the file URL)
   java .claude/skills/codename-one/tools/DesignImport.java figma \
        --token "$FIGMA_TOKEN" --file ABC123 --out target/design-import
   # Sketch / Adobe XD (local files, no token, no network)
   java .claude/skills/codename-one/tools/DesignImport.java design.sketch --out target/design-import
   # HTML / React design (e.g. a Claude-generated mockup): point it at the
   # design's tokens.css / styles.css, or the directory that contains it.
   java .claude/skills/codename-one/tools/DesignImport.java path/to/tokens.css --out target/design-import
   java .claude/skills/codename-one/tools/DesignImport.java path/to/design-dir/ --out target/design-import
   ```
   Then validate the emitted CSS before using it:
   ```bash
   java .claude/skills/codename-one/tools/IsCssValid.java target/design-import/theme.css
   ```
   Fold the useful colors/sizes from `theme.css` into `common/src/main/css/theme.css`, and use
   `layout.md` as the structure to build (it suggests the CN1 layout per container).

2. **Build the screen** from the layout map.

3. **Capture the render.** Either drive the simulator and use *Simulator menu -> Save Screenshot*,
   or copy `templates/MockupComparisonTest.java` into `common/src/test/java/<pkg>/`, point it at
   your screen, and run it:
   ```bash
   mvn -pl common test -Dtest=MockupComparisonTest
   # writes target/mockup-compare/home.png
   ```

4. **Score against the mockup.** Put mockups in `common/src/test/resources/mockups/`:
   ```bash
   java .claude/skills/codename-one/tools/CompareToMockup.java \
        target/mockup-compare/home.png \
        common/src/test/resources/mockups/home.png \
        --ignore-top 8% \
        --diff target/mockup-compare/home-diff.png
   # STRUCTURAL 0.912  PIXEL 0.874   (compared ... , ignored top 96px)
   ```

5. **Read the numbers, inspect `home-diff.png`** (green = match, red = large difference), adjust the
   CSS/layout, and repeat until the **STRUCTURAL** score reaches your target.

## Reading the two scores

| Score | What it is | Use it for |
| --- | --- | --- |
| `STRUCTURAL` | SSIM-style structural similarity (the headline number). Robust to the small pixel noise that always exists between a live render and a vector mockup. | The convergence metric you optimise. |
| `PIXEL` | Fraction of pixels whose max ARGB channel delta is <= 3 - the framework's "same pixel" rule. | Spotting exact-match regions; will read low against a non-render mockup. |

The render is auto-resized to the mockup's pixel dimensions first (`--resize fit`, the default), so
you can capture at any simulator skin size.

## Partial / region mode (the important part)

Mockups routinely include things your app does not render: an iOS status-bar mock, a notch, a home
indicator, a watermark. Comparing those regions tanks the score for no reason. Mask them:

```bash
# Ignore a status bar at the top (pixels or percent of height):
... --ignore-top 96            # drop the top 96 px
... --ignore-top 8%            # drop the top 8% of the height

# Ignore a bottom tab-bar / home indicator:
... --ignore-bottom 5%

# Exclude an arbitrary rectangle (repeatable) - e.g. a logo the mockup mocks but you load remotely:
... --ignore 24,700,300,80 --ignore 0,0,375,44

# Compare ONLY one region - e.g. evaluate just the product card:
... --region 24,120,327,180
```

All rectangle coordinates are in the **mockup's** pixel space (`X,Y,W,H`).

## Calibrate the target - don't chase 1.000

A live render will **never** hit `STRUCTURAL 1.000` against a *vector* mockup: fonts render with
different hinting, anti-aliasing differs, sub-pixel positions shift. A good match against a real
design typically lands in the **0.88-0.96** range once chrome is masked. Establish what "good"
looks like for your project on a screen you have visually confirmed, then use that as the bar for
the rest. If your "mockup" is itself a screenshot from the same renderer, you can expect higher.

Use `--min` to turn the score into a gate once you have a target:
```bash
java .claude/skills/codename-one/tools/CompareToMockup.java render.png mockup.png \
     --ignore-top 8% --min 0.9      # exit code 1 if structural < 0.9
```

## Design import notes

`DesignImport` output is a **starting point**, extracted mechanically:

- `tokens.json` - the palette (hex + usage count), the type scale, and spacing values found.
- `theme.css` - `Form`/`Title`/`Label`/`Button` seeded from the dominant colors/sizes, plus a UIID
  block per named layer. Sizes are converted to `mm` at `--px-per-mm` (default `11.8`, about a 3x
  retina export); each value carries the source px in a comment. **Always run it through
  `IsCssValid.java`** and tune `--px-per-mm` if sizes look off.
- `layout.md` - the component tree with frames and text, plus a design-layout -> CN1-layout table.

Format support:

| Format | How it is read | Needs |
| --- | --- | --- |
| Figma | `api.figma.com` REST API | a personal access token (`--token`) + file key (`--file`); network |
| Sketch (`.sketch`) | unzipped JSON (`pages/*.json`) | nothing - fully local |
| Adobe XD (`.xd`) | unzipped JSON (`graphicContent.agc`) | nothing - fully local |
| CSS tokens (`tokens.css` / `styles.css`, or a dir containing one) | CSS custom properties (`--name: value`) | nothing - fully local |

Photoshop PSD is **not** supported (opaque layered binary). Flatten-export it to PNG and use the
`CompareToMockup` path instead.

### HTML / React designs (e.g. Claude-generated mockups)

Modern AI-generated designs ship as an HTML/React bundle: an `Initializr.html`, some `*.jsx`
components, and CSS broken into a **`tokens.css`** (design tokens as CSS custom properties:
`--color-*`, `--fs-*`, `--space-*`, `--radius-*`) plus a `styles.css`. There is no layer tree, so
DesignImport's **CSS-token mode** extracts the palette/type-scale/spacing from the custom properties
(first value wins, so the light theme under `:root` beats a later `[data-theme="dark"]` override) and
seeds `Form`/`Title`/`Label`/`Button`. It defaults to `--px-per-mm 3.78` (1x CSS px) instead of the
`11.8` used for retina raster exports.

Then build the **mockup PNG** by rendering the design's own HTML headlessly and screenshot it - that
raster becomes the independent reference for `CompareToMockup`:

```bash
# Render the design to a PNG at the width you are targeting (light + dark via colorScheme).
# Any headless browser works; Playwright is convenient:
npx playwright screenshot --viewport-size=1440,2000 path/to/Initializr.html design-light.png
# (serve the dir over http if the page fetches sibling .jsx/.css with CORS)
```

Map the design's web layout to CN1 by hand - CN1 CSS has **no variables, flex, or grid**, so each
`--color-*` becomes a hardcoded hex per UIID, a `flex-direction:row` becomes `BoxLayout.x()`, a
`display:grid` becomes `GridLayout`, and every light token gets a parallel `FooDark` UIID for dark
mode (toggled by re-skinning the tree, since CN1 bakes one theme at a time).

> Capturing the **CN1 render** for the comparison is the same `templates/MockupComparisonTest.java`
> flow, with two gotchas on the high-DPI phone skin. (1) Grab the built `Form` directly (not
> `Display.getCurrent()`, which the test harness keeps on an earlier form because a second
> `show()` cannot transition while a test holds the EDT), set its size, `layoutContainer()`, and
> `paintComponent()` it onto an `Image`. (2) To see a tall scrolling screen in one shot, paint at a
> height larger than the skin (the scrollable column lays out to whatever height you give the form).

## Render it as a DESKTOP app, not a phone skin

The single biggest time-sink: a desktop/web-style UI (a wizard, a form, anything
that is wide on a browser) **must be rendered on the desktop simulator**, not a
phone skin. Phone skins are high-DPI, so `mm`-sized chrome blows up and the
responsive layout never reaches its wide form - it will look nothing like the
design and you will chase phantom problems. Boot it like the generated desktop
stub: `Display.init(JFrame contentPane)` with **no skin** and
`JavaSEPort.setDefaultPixelMilliRatio(screenDPI/25.4 * retina)`. `tools/DumpForm.java`
does exactly this (and `DesktopRenderMain`-style harnesses can capture PNGs the same
way).

## Inspecting a screen without vision

Once it renders, you do not need screenshots to reason about layout. `tools/DumpForm.java`
boots the app in desktop mode and writes a flat model of the current `Form` (every
component: UIID, absolute bounds, scrolling, opacity, background, border type,
layout, text). Three self-contained tools read that model:

```bash
CP="common/target/classes:$(mvn -q -pl common dependency:build-classpath -Dmdep.outputFile=/dev/stdout | tail -1)"
java -cp "$CP" tools/DumpForm.java com.example.MyApp --out target/form-model.tsv   # capture (desktop mode)
java tools/DescribeForm.java   target/form-model.tsv   # concise designer-language outline of the screen
java tools/AlignmentCheck.java target/form-model.tsv   # the alignment grid + elements nudged just off it
java tools/GuiLint.java        target/form-model.tsv   # static bug check (see below)
```

- **DescribeForm** turns the model into an indented outline (`role "uiid" 'text'
  @x,y WxH bg/border/scroll`) so an LLM can "see" the structure and iterate without
  a screenshot.
- **AlignmentCheck** reports the shared left/right edge "guides" (the grid the design
  settled on) and flags elements whose edge sits a few px off a guide - the
  designer-guide check for "this one is nudged 10px right". Tune `--tol`,
  `--min-shared`, `--min-off`.
- **GuiLint** flags runtime/cross-theme bugs that are invisible in a screenshot:
  nested scrolling, **opaque text labels** and **opaque structural containers** (the
  dark-mode white-box trap), CSS-generated **image borders**, a restyled base
  `Container` UIID, and zero-size components that still carry text.

These complement `CompareToMockup` (pixel/structural fidelity): use the inspectors to
get the structure and styling right, and `CompareToMockup` to converge on the pixels.

## Common pitfalls

| Symptom | Cause / fix |
| --- | --- |
| Score is low but the screen looks right | The mockup includes chrome you do not render. Mask it with `--ignore-top` / `--ignore`. |
| `STRUCTURAL` never approaches 1.0 | Expected against a vector mockup. Calibrate a per-project target (~0.9), don't chase 1.0. |
| "The mask excludes every pixel" | Your `--region` is outside the image, or `--ignore` rects cover everything. Coords are in mockup px. |
| `Unsupported or corrupt image` | Mockup is not a raster PNG/JPG (e.g. an SVG). Export it to PNG first. |
| Imported CSS sizes are all tiny/huge | Wrong `--px-per-mm` for the export scale. A 1x export is ~3.9 px/mm; a 3x export ~11.8. |
| Figma call returns HTTP 403 | Bad/expired token, or the token lacks access to that file. |
