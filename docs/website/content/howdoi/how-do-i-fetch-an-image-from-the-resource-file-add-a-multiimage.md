---
title: WORK WITH MULTI IMAGES AND DEVICE DENSITIES
slug: how-do-i-fetch-an-image-from-the-resource-file-add-a-multiimage
url: /how-do-i/how-do-i-fetch-an-image-from-the-resource-file-add-a-multiimage/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-fetch-an-image-from-the-resource-file-add-a-multiimage.html
tags:
- basic
- ui
description: Understand the complexity of phone/tablet images
youtube_id: sK-u1TBWFX8
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-3-1.jpg
---
{{< youtube "sK-u1TBWFX8" >}}

Image handling on mobile starts with density, not just resolution. Two devices can have very different physical sizes and very different pixel densities, which means the same raster image can feel too small, too large, or too soft depending on where it is shown. That is why Codename One introduced multi-images and why density still matters even in a modern CSS-first project.

The important distinction is between raster and vector assets. Raster formats such as PNG and JPEG are fixed collections of pixels. If you scale them too aggressively, quality drops. Vector assets can be redrawn cleanly at different sizes. That is why `FontImage` and icon-font based workflows are so valuable: when an icon can be expressed as a vector, you avoid many of the density problems that ordinary raster assets create.

Multi-images exist for the cases where you really do need raster artwork. Instead of shipping one image and scaling it poorly on-device, you prepare density-appropriate variants so the runtime can choose the asset that best matches the device. The older designer workflow made this explicit through multi-image tooling. The idea is still valid today, even though many modern projects will rely more heavily on vector icons and CSS styling than older designer-heavy apps did.

This also means that not every screen should just "scale up" for tablets. The video makes an important point here: higher-resolution devices should often show more content, not just bigger versions of the same phone UI. A tablet layout that merely enlarges every icon and spacing value can feel wasteful. Density-aware images are part of adaptation, but layout choices are just as important.

In practical terms, use vectors where you can, multi-images where you must, and on-device scaling carefully. If you do need to generate or manipulate scaled rasters, remember that the memory cost at runtime can be much higher than the compressed file size suggests. This is one of the reasons image-heavy screens often need explicit memory discipline.

The older page talks about fetching images from the resource file through the resource APIs. That is still correct for apps that keep assets in resource bundles. In a more modern setup, the broader principle matters more than the exact tool: keep image handling density-aware, prefer vectors for icons, and avoid assuming one raster size will feel correct everywhere.

## Further Reading

- [Themeing](/themeing/)
- [Developer Guide](/developer-guide/)
- [How Do I Create A 9 Piece Image Border](/how-do-i/how-do-i-create-a-9-piece-image-border/)
- [How Do I Improve Application Performance Or Track Down Performance Issues](/how-do-i/how-do-i-improve-application-performance-or-track-down-performance-issues/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
