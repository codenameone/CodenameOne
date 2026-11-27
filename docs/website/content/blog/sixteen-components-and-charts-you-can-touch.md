---
title: "Sixteen New Components, and Charts You Can Touch"
slug: sixteen-components-and-charts-you-can-touch
url: /blog/sixteen-components-and-charts-you-can-touch/
date: '2026-11-30'
author: Shai Almog
description: "Skeleton loaders, carousels, coach marks, empty states, range sliders, chips, a color picker, circular progress, audio waveforms, expandable text, timelines, an emoji picker, a mention field and a masonry layout are now in core. The new com.codename1.ui.chart package draws 19 series types that animate, pan, zoom, show tooltips and talk to screen readers."
feed_html: "<img src=\"https://www.codenameone.com/blog/sixteen-components-and-charts-you-can-touch.jpg\" alt=\"Sixteen new components and charts you can touch\" /> Sixteen components every modern app needs, now in core, and a rewritten chart package with 19 series types, animation, gestures and accessibility."
series: ["release-2026-11-27"]
---

![Sixteen new components and charts you can touch](/blog/sixteen-components-and-charts-you-can-touch.jpg)

Open any app on your phone and count the pieces nobody thinks about. The gray shimmer while a list loads. The onboarding pager with dots. The friendly "nothing here yet" screen with a retry button. The price filter with two thumbs. The tag field that turns words into chips. Every team builds these, every team builds them a little wrong, and an agent will happily build them a little wrong again in every project you start.

So they're in core now. Sixteen of them, plus a chart package rewritten from scratch.

## Loading and onboarding

`Skeleton` replaces the spinner with placeholders shaped like the content that's coming, which makes an app feel faster even when it isn't.

```java {class="cn1-no-try"}
form.add(Skeleton.card());              // image, title and two lines
form.add(Skeleton.rows(3));             // three shimmering rows
Skeleton.show(bio, Skeleton.text(3));   // mask a real component until data arrives
```

`Carousel` is the swipeable pager everyone calls something different (image slider, view pager, banner rotator), with `PageIndicator` dots and optional autoplay. `CoachMarks` runs a feature tour: the screen dims, the component you're explaining shows through a cut-out, and a bubble says what it does.

```java {class="cn1-no-try"}
new CoachMarks(form)
        .add(search, "Search", "Find any order by number or name.")
        .add(filter, "Filters", "Narrow the list to what is still open.")
        .show();
```

`EmptyState` is the screen you show when there's nothing to show, with an icon, a title, a message and an action:

```java {class="cn1-no-try"}
new EmptyState(FontImage.MATERIAL_CLOUD_OFF, "Can't load orders",
        "Check your connection and try again.")
        .setAction("Retry", e -> reload());
```

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,180px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img src="/developer-guide/img/generated/components-skeleton.png" alt="Skeleton placeholders while a list loads" loading="lazy" style="width:100%;height:auto" /><figcaption>Skeleton.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/components-carousel.png" alt="A carousel with page dots" loading="lazy" style="width:100%;height:auto" /><figcaption>Carousel.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/components-coach-marks.png" alt="A coach mark tour highlighting a button" loading="lazy" style="width:100%;height:auto" /><figcaption>Coach marks.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/components-empty-state.png" alt="An empty state with a retry button" loading="lazy" style="width:100%;height:auto" /><figcaption>Empty state.</figcaption></figure>
</div>
{{< /guide-block >}}

## Input

`RangeSlider` has two thumbs that can't cross, with snapping:

```java {class="cn1-no-try"}
RangeSlider price = new RangeSlider(0, 500, 100, 350);
price.setStep(10);
price.addActionListener(e -> summary.setText(
        "$" + price.getLowerValue() + " to $" + price.getUpperValue()));
```

`Chip` is a filter or choice token, and `ChipInput` turns typed words into chips as you hit a separator. `ColorPicker` gives you presets, a full picker and a one-line dialog. `EmojiPicker` is the grid you expect. `MentionField` suggests names after an `@`, or tags after a `#` if you call `setTrigger('#')`, and you can override `suggest` to search a directory of a million users.

```java {class="cn1-no-try"}
MentionField comment = new MentionField();
comment.setCandidates("Ada Lovelace", "Alan Turing", "Grace Hopper");
comment.addActionListener(e -> notify(comment.getMentions()));
```

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,180px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img src="/developer-guide/img/generated/components-range-slider.png" alt="A range slider for a price filter" loading="lazy" style="width:100%;height:auto" /><figcaption>RangeSlider.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/components-chip-input.png" alt="A tag field with chips" loading="lazy" style="width:100%;height:auto" /><figcaption>ChipInput.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/components-color-picker.png" alt="A color picker with presets" loading="lazy" style="width:100%;height:auto" /><figcaption>ColorPicker.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/components-mention-field.png" alt="A mention field suggesting names" loading="lazy" style="width:100%;height:auto" /><figcaption>MentionField.</figcaption></figure>
</div>
{{< /guide-block >}}

## Progress, media and content

`CircularProgress` draws a ring for uploads, goals, and timers. `AudioWaveform` draws a voice message or a recording as bars you can scrub. `ExpandableText` shows the first lines of a long review with a "more" link. `ActivityTimeline` lays out order history, a support ticket or a delivery as a vertical timeline. `MasonryLayout` places tiles of different heights in equal columns, each in the shortest one, the way Pinterest does, with a column count that adapts to the width.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,180px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img src="/developer-guide/img/generated/components-audio-waveform.png" alt="An audio waveform" loading="lazy" style="width:100%;height:auto" /><figcaption>AudioWaveform.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/components-activity-timeline.png" alt="An activity timeline" loading="lazy" style="width:100%;height:auto" /><figcaption>ActivityTimeline.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/components-masonry-layout.png" alt="A masonry layout of tiles" loading="lazy" style="width:100%;height:auto" /><figcaption>MasonryLayout.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/components-circular-progress.png" alt="Circular progress rings" loading="lazy" style="width:100%;height:auto" /><figcaption>CircularProgress.</figcaption></figure>
</div>
{{< /guide-block >}}

All sixteen are ordinary components with UIIDs, so your theme styles them like anything else, and they behave the same on every platform.

## Charts, rewritten

Our old chart package came from a port of a charting library more than a decade ago. It worked, but it looked its age, it didn't animate, and touching a chart did nothing. `com.codename1.ui.chart` replaces it. It's a normal component drawn with the Codename One graphics pipeline, with no web view and no third-party library.

```java {class="cn1-no-try"}
Chart chart = new Chart().setTitle("Revenue")
        .setXAxis(new CategoryAxis(Axis.BOTTOM, "Q1", "Q2", "Q3", "Q4"));
chart.addSeries(new BarSeries("2025").setData(null, new double[]{120, 180, 150, 210}));
chart.addSeries(new LineSeries("Target").setDashed(true)
        .setData(null, new double[]{140, 160, 170, 190}));
form.add(BorderLayout.CENTER, chart);
```

![A bar chart of revenue with a dashed target line](/developer-guide/img/generated/chart-first.png)

There are 19 series types:

| Kind | Series |
| --- | --- |
| Basic | Bar, Line (straight, spline, stacked or filled), Scatter, Pie and doughnut, Radar, Histogram |
| Statistical | Box plot, Error bar, Range bar, Heatmap |
| Financial | Candlestick, Volume, Waterfall, plus indicators |
| Hierarchy | Treemap, Sunburst, Flame graph |
| Flow and gauges | Sankey, Funnel, Gauge |

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,240px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img src="/developer-guide/img/generated/chart-sankey.png" alt="A Sankey diagram" loading="lazy" style="width:100%;height:auto" /><figcaption>Sankey.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/chart-candlestick.png" alt="A candlestick chart" loading="lazy" style="width:100%;height:auto" /><figcaption>Candlesticks.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/chart-treemap.png" alt="A treemap" loading="lazy" style="width:100%;height:auto" /><figcaption>Treemap.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/chart-dark.png" alt="A chart in dark mode" loading="lazy" style="width:100%;height:auto" /><figcaption>Dark mode, automatically.</figcaption></figure>
</div>
{{< /guide-block >}}

What matters more than the list is how they behave:

- **Labels never overlap**, and axis ticks land on round numbers, without margins you tune by hand.
- **Data changes animate.** Add a point and the line grows to it.
- **Touch works.** Drag pans, pinch or the wheel zooms, double tap zooms in, tap shows a tooltip, tapping a legend entry hides its series, and `setBrushEnabled(true)` selects a range.
- **Accessibility is built in.** Arrow keys step through points and a screen reader announces each one. `toTable()` gives you the same data as a table.
- **The palette is color-blind safe**, follows dark mode, and handles right to left layouts.

Axes come as category, number, log, time and session time (which skips weekends, holidays and overnight hours the way trading charts do), with secondary axes when two series don't share a scale. Sparklines fit in a table cell. The old `com.codename1.charts` package still works, but it's deprecated and won't get new features.

You can see the new charts across every dashboard in [Bazaar](/blog/full-shopify-clone-in-three-days/): the conversion funnel, the sales over time, the goal rings on the merchant home screen.

![The Bazaar analytics screen built on the new charts](/developer-guide/img/bazaar-merchant-analytics-tablet.png)

The [Components chapter](/developer-guide/#_the_components_of_codename_one) has every component and every chart type. Tomorrow: {{< post-link path="/blog/stop-debuggers-hooks-and-keyloggers" text="the defenses a banking app needs" >}}.
