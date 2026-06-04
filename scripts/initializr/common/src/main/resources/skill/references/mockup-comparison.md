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

Photoshop PSD is **not** supported (opaque layered binary). Flatten-export it to PNG and use the
`CompareToMockup` path instead.

## Common pitfalls

| Symptom | Cause / fix |
| --- | --- |
| Score is low but the screen looks right | The mockup includes chrome you do not render. Mask it with `--ignore-top` / `--ignore`. |
| `STRUCTURAL` never approaches 1.0 | Expected against a vector mockup. Calibrate a per-project target (~0.9), don't chase 1.0. |
| "The mask excludes every pixel" | Your `--region` is outside the image, or `--ignore` rects cover everything. Coords are in mockup px. |
| `Unsupported or corrupt image` | Mockup is not a raster PNG/JPG (e.g. an SVG). Export it to PNG first. |
| Imported CSS sizes are all tiny/huge | Wrong `--px-per-mm` for the export scale. A 1x export is ~3.9 px/mm; a 3x export ~11.8. |
| Figma call returns HTTP 403 | Bad/expired token, or the token lacks access to that file. |
