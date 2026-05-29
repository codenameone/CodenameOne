---
title: SVG And Lottie At Build Time (Metal Only On iOS)
slug: svg-lottie-build-time
url: /blog/svg-lottie-build-time/
date: '2026-06-07'
author: Shai Almog
description: Drop an SVG or a Bodymovin / Lottie JSON file into src/main/svg/ or src/main/lottie/ and the build emits a Codename One Image subclass that draws through the shape API. SMIL animations honoured for SVG, keyframe collapse for Lottie. Metal-only on iOS because the GL path does not have the shape coverage. Plus the iOS Metal stencil-clip and drawString fixes that retook the SVG goldens.
feed_html: '<img src="https://www.codenameone.com/blog/svg-lottie-build-time.jpg" alt="SVG And Lottie At Build Time (Metal Only On iOS)" /> SVG and Lottie JSON files become Codename One Image subclasses at build time. SMIL animations for SVG, keyframe collapse for Lottie. Metal-only on iOS because the GL path does not have the shape coverage.'
---

![SVG And Lottie At Build Time (Metal Only On iOS)](/blog/svg-lottie-build-time.jpg)

The last post in this batch covers the new build-time SVG and Lottie pipeline. Three PRs: [#5042](https://github.com/codenameone/CodenameOne/pull/5042) is the SVG transcoder; [#5049](https://github.com/codenameone/CodenameOne/pull/5049) is the small set of iOS Metal and Android rendering fixes the SVG screenshot tests exposed; [#5066](https://github.com/codenameone/CodenameOne/pull/5066) is the Lottie / Bodymovin transcoder that reuses the SVG pipeline. They share one chapter in the dev guide at [SVG-Transcoder.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/SVG-Transcoder.asciidoc).

The headline before we get into it: this is **Metal-only on iOS**. The GL ES 2 path that was the iOS default until [last week's flip](/blog/metal-default-new-build-cloud-and-a-new-format/#metal-is-the-default-on-ios) does not have the shape API coverage we need. The SVG pipeline emits `Graphics.fillShape` / `drawShape` calls; the Metal renderer implements those end-to-end; the GL pipeline does not. Apps that opted in to Metal pick up SVG and Lottie automatically; apps still on `ios.metal=false` will see the placeholders. Now that Metal is the default this stops being a thing most apps notice on their next build.

## The shape

The transcoder is a `maven/svg-transcoder/` module that parses SVG with `javax.xml` StAX (no Batik, no Flamingo, no external deps) and emits a Codename One `Image` subclass. The output renders through the `Graphics` shape API, which means it works on every platform CN1 supports (iOS Metal, Android, JavaSE, JavaScript port). You drop an `.svg` file into `src/main/svg/` of your app, the build emits a `MyIcon.java`, `Resources.getImage("my-icon.svg")` or `Resources.getImage("my-icon")` returns it, the same code paths that already handle `.res`-backed images handle the new ones.

The Lottie pipeline ([#5066](https://github.com/codenameone/CodenameOne/pull/5066)) is the part that I find most pleasing in this batch: Lottie / Bodymovin JSON files (`.json`, `.lottie`) flow through the *same* pipeline. Each Bodymovin file is parsed into the same `SVGDocument` model the SVG path uses; the same `JavaCodeGenerator` emits the same `GeneratedSVGImage` subclass; the same `SVGRegistry` registers it under its source filename. **No new `Image` base class, no new registry, no per-port wiring.** Drop a Bodymovin export into `src/main/lottie/`, build the app, `getImage("my-spinner")` returns it.

```
src/main/svg/
    home.svg
    settings.svg
    profile.svg
src/main/lottie/
    spinner.json
    pulse.json
```

After the next build:

```java
Image home    = Resources.getGlobalResources().getImage("home");
Image spinner = Resources.getGlobalResources().getImage("spinner");
form.add(home).add(spinner);
```

## SVG coverage

The SVG side covers what most real-world icon SVG you have downloaded actually uses: `rect` (with rounded corners), `circle`, `ellipse`, `line`, `polyline`, `polygon`, the full `path` grammar (M / L / H / V / C / S / Q / T / A / Z plus relative-coordinate and smooth-curve reflection), groups with affine transforms (`translate`, `scale`, `rotate`, `skew`, `matrix`), linear gradients via `LinearGradientPaint`, fill, stroke, stroke-width, linecap, linejoin, opacity.

SMIL animations are supported: `<animate>`, `<animateTransform>` (`translate`, `scale`, `rotate`), and `<set>`. The transcoded image interpolates against wall-clock time on every paint, with `from` / `to` / `values` / `begin` / `dur` / `repeatCount` / `fill="freeze"` honoured. So a `<circle>` with an `<animateTransform>` rotating it around its centre becomes a real animated `Image` you can drop straight into a `Form` and watch spin without writing any animation code yourself.

Explicit non-coverage for v1: text, masks and clip-paths, filters, radial-gradient paint (falls back to the first stop colour), CSS keyframe animations. The first two are the next things we will go after; SVG `text` in particular is one of those features that looks small but really wants a layout engine behind it.

## Lottie coverage

The Lottie side is intentionally narrower than the spec, because the spec is a moving target and a lot of Lottie features depend on After Effects-specific behaviours. v1 coverage:

- Shape layers (`rc` / `el` / `sh`) with solid fills and strokes.
- Layer transforms: anchor, position, scale, rotation, opacity.
- Animated rotation, position, and scale, collapsed to a two-keyframe loop. The full Bezier-eased keyframe interpolation is on the list; v1 is "animated, but linear-eased between the first and last keyframe".
- Solid-color layers (`ty:1`) lower to a filled rect.

Most icon-grade Lotties (loading spinners, success ticks, simple morphing icons) lower cleanly. Complex character animations from After Effects with image references, masks, and effects do not, and the transcoder logs which layers it dropped so you know.

Documented coverage matrix is in the [SVG-Transcoder.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/SVG-Transcoder.asciidoc) chapter; the troubleshooting section is the one to read if a Bodymovin export from your designer comes out empty.

## Why a Java subclass and not a runtime parser

A design question that came up: why generate Java at build time rather than parse the SVG at runtime? The answer in three parts.

First, the SVG parser is JVM-only by design. ParparVM and the JavaScript port do not include `javax.xml`; we did not want to drag a parser into both of those ports just to render a static asset on every paint.

Second, the build-time pipeline produces deterministic, allocation-light code. A path's commands become a sequence of `g.fillShape(new GeneralPath()...)` calls with the constants inlined; an animation becomes a `currentTransform.translate(...)` against a wall-clock variable. There is no parsing on the device, no caching layer to invalidate, no surprise allocation in the paint loop.

Third, R8 and ParparVM rename / dead-code eliminate the generated code as freely as any other class. SVGs you do not actually `getImage(...)` get dropped from the final binary; the parts of the renderer your specific assets do not use also get dropped. The generated artefact behaves like the rest of your app code, which is the right answer.

## The Metal rendering fixes (#5049)

Three rendering-layer bugs surfaced once the SVG screenshot tests started exercising the shape API harder than anything we had thrown at it before. The fixes are small and worth knowing about because they affect any code path that uses `setClip(GeneralPath)`, gradient paint, or text under a transform; not just the SVG pipeline:

1. **iOS Metal `setClip(GeneralPath)` triangle.** `gradient_circle.svg` and `clipped_badge.svg` rendered as triangles instead of clipped paths. The Metal stencil clip's triangle fan was treating every Bezier control point as a polygon vertex. Fix: any non-rect `ClipShape` is now midpoint-flattened into a polyline before reaching native, so the stencil writer sees real polygon vertices.
2. **iOS Metal `drawString` skips the affine scale.** Text under a viewBox scale was rasterised at `font.pointSize` and stretched on the GPU, smearing the glyphs. `CN1MetalDrawString` now reads the effective scale from `currentTransform`, picks an atlas font at `pointSize * scale`, and divides the glyph metrics back into caller-side coordinates. Pure rotation / translation stays on the fast path.
3. **Android and iOS Metal `gradient_circle.svg` double-circle.** `LinearGradientPaint.paint` was baking `getTranslateX/Y()` into a translate that sat *before* the SVG scale, so the cell offset went through the scale twice and the fill landed below the stroke. The "translate dance" is dropped; the existing `Graphics.setTransform` conjugation re-applies the screen-level offset.

If your app uses `setClip(GeneralPath)` or paints text under a non-uniform transform anywhere, you pick these fixes up on next rebuild.

## Wrapping up

The pipeline is a small module on disk and a big change in how you ship art: SVGs and Lotties become Java that compiles into your app, ParparVM and R8 dead-code-eliminate the parts you do not use, the JavaScript port treats them like any other `Image`. The chapter at [SVG-Transcoder.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/SVG-Transcoder.asciidoc) covers everything; the coverage matrices are the right place to look if you have a specific asset you want to know about.

That closes out the post series for this release cycle. The weekly index for May 29 is [here](/blog/metal-default-new-build-cloud-and-a-new-format/); the next weekly index is the one I will publish at the end of this coming week, in the same short format. Let me know how the new shape lands.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
