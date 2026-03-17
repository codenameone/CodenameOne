---
title: LAYOUT BASICS
slug: how-do-i-positioning-components-using-layout-managers
url: /how-do-i/how-do-i-positioning-components-using-layout-managers/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-positioning-components-using-layout-managers.html
tags:
- basic
- featured
- ui
description: Determine where components are placed especially when dealing with varied
  screen sizes/orientations
youtube_id: 4D_KUa2qv2o
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-5-1.jpg
---
{{< youtube "4D_KUa2qv2o" >}}

Layouts are the reason a Codename One UI can survive different screen sizes, orientations, pixel densities, and languages. Components do not live at fixed coordinates. They live inside `Container` objects, and those containers use layout managers to decide how much space each child gets and where it should appear.

The core problem layouts solve is portability. A button position that looks fine on one phone may be wrong on another. A label that fits in English may overflow in German. A design that works in portrait may fall apart in landscape. Absolute positioning looks tempting when a screen is simple, but it stops being practical as soon as the UI has to adapt. Layout managers let you describe intent instead of hard-coding coordinates: this component should stay on the left, this field should take the remaining width, this section should stack vertically, these buttons should all be the same size.

That is why the classic “label on the left, field on the right” example is still such a useful starting point. In Codename One you usually express that with a `BorderLayout`, putting the label in `BorderLayout.WEST` and the field in `BorderLayout.CENTER`. The layout then handles the resizing rules for you. The label keeps the width it needs, while the center component expands into the remaining space. Once that pattern clicks, you can start seeing a screen as a composition of small layout problems instead of one big manual positioning job.

The standard layouts each have a natural role. `FlowLayout` is still the simplest layout and is fine for small inline groups of components, but it is easy to outgrow. `BorderLayout` is one of the most useful outer layouts because it gives you strong structure: top, bottom, left, right, and a center area that consumes the remaining space. `BoxLayout.y()` is a great default for stacked content because it reads like the screen itself. `BoxLayout.x()` is good for small horizontal rows. `GridLayout` is useful when you genuinely want equal-sized components, such as button bars or icon grids. `TableLayout` is especially helpful for forms and data entry because it gives you more control over rows, columns, and spanning. `LayeredLayout` is the one to reach for when components need to sit on top of each other, such as floating actions, overlays, or decorative layers.

One detail that matters early is constraints. Some layouts need them and some do not. `BorderLayout` depends on explicit positions such as `CENTER` and `WEST`, while `BoxLayout` generally does not need extra constraints at all. Understanding that difference helps make the APIs feel much less arbitrary. A layout manager is not just “where children go”; it also defines what information you need to provide when you add those children.

The most effective way to build a real screen is usually to nest a few simple containers rather than hunting for one magical layout that does everything. A form might use `BorderLayout` at the top level, a `BoxLayout.y()` in the content area, and then small `BorderLayout` or `GridLayout` sections inside it. That is normal. Good layout code tends to mirror the visual structure of the screen. If a screen has a header, a scrolling body, and a floating action button, the container hierarchy should make that obvious.

Inspecting a real screen is often more useful than memorizing layout APIs in the abstract. If you open a working UI in the component inspector and look at the nesting, you quickly see why a `LayeredLayout` was used at the root, why a `BoxLayout.y()` holds a scrolling list, or why a `GridLayout` makes the swipe actions line up evenly. That is how layouts become intuitive.

The modern adjustment is not in the layout system itself so much as in how you split responsibilities. Layout code should mostly define structure and resizing behavior. CSS should do much more of the styling work: spacing, fonts, colors, borders, and visual states. If you find yourself using extra containers only to fake visual styling, that is often a sign that the visual concern belongs in CSS instead.

## Further Reading

- [Layout Basics](/layout-basics/)
- [Developer Guide](/developer-guide/)
- [Themeing](/themeing/)
- [Getting Started](/getting-started/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
