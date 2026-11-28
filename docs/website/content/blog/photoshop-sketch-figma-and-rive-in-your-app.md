---
title: "Open Photoshop, Sketch and Figma Files in Your App, and Play Rive Animations"
slug: photoshop-sketch-figma-and-rive-in-your-app
url: /blog/photoshop-sketch-figma-and-rive-in-your-app/
date: '2026-11-29'
author: Shai Almog
description: "com.codename1.art reads PSD and PSB with layers, masks and layer styles, Sketch files, Figma files through the REST API, and SVG at runtime, renders them identically on every platform, and includes a drawing editor that exports SVG, PSD and PNG. Rive files now play as ordinary images, translated to Java at build time."
feed_html: "<img src=\"https://www.codenameone.com/blog/photoshop-sketch-figma-and-rive-in-your-app.jpg\" alt=\"Photoshop, Sketch, Figma and Rive in your app\" /> Read layered PSD, Sketch and Figma files, render live SVG with filters, edit artwork in a built-in editor, and play interactive Rive animations, from one Java codebase."
series: ["release-2026-11-27"]
---

![Photoshop, Sketch, Figma and Rive in your app](/blog/photoshop-sketch-figma-and-rive-in-your-app.jpg)

Designers hand developers files. Photoshop files, Sketch files, Figma links, SVGs, Rive animations. Until now the developer's job was to flatten all of that into PNGs and hope nobody asked for a change. If your app itself works with design files, a print shop, a sticker maker, a template editor, a brand portal, you needed a native SDK per platform and a server to fill the gaps.

This week those files open inside your app, keep their layers, and render the same pixels on iOS, Android, the desktop and the web.

## One document tree for every format

`com.codename1.art` reads every format into the same `ArtDocument` tree of groups, shapes, text, raster layers and adjustments.

| Format | Read | Write | What's kept |
| --- | --- | --- | --- |
| SVG | yes | yes | Shapes, paths, `use`, gradients, patterns, clip paths, masks, text, images, CSS, filters |
| PSD and PSB | yes | yes | Layers, groups, masks, vector masks, clipping, layer styles, adjustment layers |
| Sketch 43 and later | yes | | Pages, artboards, symbols with overrides, shared styles |
| Figma | yes | | Files fetched through the Figma REST API |
| `.cn1gfx` | yes | yes | Everything, losslessly |

`ArtFormats.read` detects the format from the bytes, not the file name:

```java {class="cn1-no-try"}
ArtDocument doc = ArtFormats.read(in);      // SVG, PSD, Sketch, Figma or .cn1gfx
Image image = ArtRenderer.toImage(doc, 2.0);  // two pixels per document unit
```

![A PSD, a Sketch file and an SVG rendered side by side](/developer-guide/img/generated/art-load.png)

{{< mermaid >}}
flowchart LR
    PSD[PSD / PSB] --> T
    SK[Sketch] --> T
    FG[Figma REST] --> T
    SVG[SVG] --> T
    T[ArtDocument tree] --> R[ArtRenderer]
    T --> E[ArtEditor]
    R --> PX[Same pixels on<br/>iOS, Android, desktop, web]
    E --> OUT[SVG, PSD, PNG, .cn1gfx]
{{< /mermaid >}}

## Live SVG, not transcoded SVG

We've had an SVG transcoder for a long time. It turns SVG into Java at build time, which is ideal for icons you ship. It's useless for an SVG your user uploads at runtime. Now `Image.createSVG` falls back to `ArtRenderer` on any platform with no native SVG support, so a runtime SVG with filters, masks and CSS draws the same everywhere.

## Every Photoshop effect

`ArtRenderer` is a software renderer, which is why the pixels match across platforms. `com.codename1.art.effects` implements drop and inner shadows, outer and inner glows, Gaussian, motion, zoom and background blur, strokes, color, gradient and pattern overlays, bevel and emboss, satin, Photoshop adjustment layers like levels, curves and hue and saturation, and the whole SVG filter primitive set.

![Layer styles and blend modes rendered by ArtRenderer](/developer-guide/img/generated/art-effects.png)

## Figma, straight from the API

```java {class="cn1-no-try"}
FigmaClient figma = new FigmaClient(token);   // the token is never stored
ArtDocument doc = figma.fetch("FILE_KEY");    // blocks: call it off the EDT
form.add(BorderLayout.CENTER, new ArtView(new ArtModel(doc)));
```

The client fetches images over HTTPS only and never sends your token along with them. If you already downloaded the JSON yourself, `FigmaReader.read` takes it as is.

## A drawing editor you can drop in

`ArtEditor` is a ready-made editor with a toolbar, a layers panel and an inspector. It selects, moves and resizes, pans, draws rectangles, ellipses, lines, Bezier curves and freehand strokes, paints and erases pixels, sets text, picks colors and fills. `ArtModel` records an undo history, and `beginGroup()` and `endGroup()` turn a series of changes into one undo step. When the user is done, `exportSvg`, `exportPsd`, `exportPng` or `exportNative` hands you the result.

![The drawing editor with toolbar, layers and inspector](/developer-guide/img/generated/art-editor.png)

Files from strangers are a classic attack surface, so every reader caps dimensions, layer counts, decompressed size, ZIP entries and nesting depth. An oversized file throws an `IOException`; it never takes your app's memory down with it. Anything a reader can't represent is listed in `ArtDocument.getNotes()`.

## Rive, as an image

[Rive](https://rive.app/) makes interactive vector animations driven by state machines: a button that squishes, a mascot that reacts to the user, a loader with personality. Now a `.riv` file is just an image, like an SVG or a Lottie file. Drop it next to your `theme.css` and use it:

```css
JuiceStyle {
    background: url(juice.riv);
    cn1-background-type: cn1-image-scaled-fit;
}
```

Or from code, with the state machine inputs Rive designers wire up:

```java {class="cn1-no-try"}
Image lightSwitch = Resources.getGlobalResources().getImage("switch.riv");
form.add(BorderLayout.CENTER, new ScaleImageLabel(lightSwitch));
lightSwitch.setAnimationInput("isOn", true);
lightSwitch.fireAnimationTrigger("bump");
```

The runtime is pure Java, translated from the open source Rive runtime. The build turns each `.riv` into a Java class that builds the objects directly, so nothing is parsed on the device, there's no reflection, and an app with no Rive files carries none of the runtime. It plays shapes, gradients, clipping, timeline animations, state machines with layers, blends and listeners, bones, skins and IK, meshes, nested artboards, trim paths, embedded fonts, flex layouts and data binding with view models. Audio events play through the media API. Show it in a `RiveView` and touch goes to the animation too.

## What you can build with it

- A print or merchandise app that opens the customer's PSD and shows the real layers.
- A brand portal that pulls the latest screens straight from Figma.
- A template editor where users change text and colors in a designer's file and export it.
- A UI with the animations your designer actually made, instead of a GIF of them.

The [Graphics documents chapter](/developer-guide/#graphics-documents) has the full API. Tomorrow: {{< post-link path="/blog/sixteen-components-and-charts-you-can-touch" text="sixteen new components and charts you can touch" >}}.
