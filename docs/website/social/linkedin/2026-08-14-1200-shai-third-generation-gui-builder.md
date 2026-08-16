---
title: "A visual builder should understand the project"
slug: 2026-08-14-1200-shai-third-generation-gui-builder
platform: linkedin
account: shai
source_slug: third-generation-gui-builder
publish_at: '2026-08-14T12:00:00'
timezone: Asia/Jerusalem
image: /blog/third-generation-gui-builder.jpg
---

We have rebuilt the Codename One GUI Builder again. This is its third generation.

The second rewrite got the hard part right. Steve Hannah built guided layout on top of `LayeredLayout`, with sibling references, responsive insets, matching sizes, and baseline alignment.

The problem changed when Codename One moved to Maven. Opening a separate editor for every generated form made navigation the slow part of a visual tool. Styling lived in CSS, source generation lived in Maven, and the builder still behaved as if one form were the project.

The third-generation builder keeps Steve's layout model and changes the unit of work. One Maven goal opens every `.gui` form, the live canvas, the real `theme.css`, and protected Java regions in one workspace.

The post also covers the debugger, Linux video, ParparVM memory, Google Sign-In, and security work that shipped this week.

{{canonical}}
