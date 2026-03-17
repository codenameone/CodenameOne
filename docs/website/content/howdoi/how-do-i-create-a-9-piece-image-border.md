---
title: CUSTOMIZE COMPONENT BORDERS AND BACKGROUNDS
slug: how-do-i-create-a-9-piece-image-border
url: /how-do-i/how-do-i-create-a-9-piece-image-border/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-create-a-9-piece-image-border.html
tags:
- basic
- ui
description: The 9-piece image border is a cornerstone tool for theme design
youtube_id: ACsZ8qiwR8Q
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/10/Mask-Group-7.png
---
{{< youtube "ACsZ8qiwR8Q" >}}

Backgrounds and borders in Codename One are easiest to understand once you stop treating them as a pile of theme options and start thinking in terms of rendering precedence. A component does not simply combine every visual setting you give it. Some background choices override others. Borders can override background images. Gradients can override plain background colors. If a component is not rendering the way you expected, the first question is often not "did my value save?" but "which style layer is actually winning?"

The older video explains this through the designer tool, but the concepts matter just as much when you style with CSS. In modern projects, CSS should usually be the main styling path, while the designer and resource editor are lower-level tools you use when you genuinely need them. The visual rules themselves are still the same: understand the border type, understand the background type, and understand how state-specific styles inherit from one another.

The most important image-border concept is still the 9-piece border. It exists because some shapes need to scale without looking stretched or blurry. Instead of treating a border as one image, you split it into corners, edges, and a center. The corners remain fixed, while the edge and center regions tile or expand as needed. That lets the component grow while preserving the look of the decorative frame.

Cutting a good 9-piece border is mostly about judgment. Keep the corners isolated so they are never tiled. Keep distinctive details such as speech-bubble arrows or ornamental edges out of the tiled regions, otherwise they will smear when the component grows. The old wizard-based workflow still illustrates this well, even if the everyday styling recommendation today is more CSS-first than designer-first.

State handling matters just as much as the base border. A component rarely has only one appearance. Buttons, for example, usually need unselected, pressed, and selected variants. If you only style the default state, the component may suddenly pick up a strange native border or inconsistent highlight on another state. This is one reason the video spends time on empty borders and inherited styles: clearing or overriding the wrong inherited border can be the difference between a clean result and a confusing one.

9-piece borders also interact with device density. If you are using raster imagery, you need to think about how the pieces scale across different densities. Multi-images are still relevant in that sense, but in a modern project the first question should be whether the effect really needs raster artwork at all. If a clean CSS border or a round border can achieve the same visual result, that is often easier to maintain and easier to adapt.

The broader lesson is that visual styling should be deliberate. Use the right border type for the job, style all of the component states that matter, and prefer simpler modern styling mechanisms when they can produce the same effect. 9-piece borders are still useful, but they are no longer the default answer to every styling problem.

## Further Reading

- [Themeing](/themeing/)
- [Developer Guide](/developer-guide/)
- [How Do I Create A Simple Theme](/how-do-i/how-do-i-create-a-simple-theme/)
- [Work With Multi Images And Device Densities](/how-do-i/how-do-i-fetch-an-image-from-the-resource-file-add-a-multiimage/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
