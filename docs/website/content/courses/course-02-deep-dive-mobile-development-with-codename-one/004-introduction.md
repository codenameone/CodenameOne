---
title: "Introduction"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Extracting a UI Design"
module_key: "03-extracting-a-ui-design"
module_order: 3
lesson_order: 1
weight: 4
is_course_lesson: true
description: "Learn how to turn a design mockup into a practical Codename One interface."
---
> Module 3: Extracting a UI Design

{{< youtube BXwh2T7wvfc >}}

This module is about the gap between a static design and a real application screen. A mockup can show you the look of the product, but it does not tell you which parts should become live UI, which parts should remain image assets, which elements need to stretch, and which details are safe to simplify.

The example in the video starts from a Photoshop design and turns it into a Codename One mockup. The same exercise still matters today, but the tools around it have changed. Many teams now start from Figma or similar design tools rather than PSD files, and the implementation should usually lean on CSS for styling instead of older theme-designer workflows. The core skill, though, has not changed at all: learn to read a design structurally instead of treating it like a screenshot you have to reproduce pixel by pixel.

When you look at a design, start by asking what is truly interactive. Buttons, search fields, lists, toolbars, filters, and cards usually want to be real components. Photos, illustrations, and a few special decorative details may need to stay as assets. Once you separate those two categories, the rest of the implementation becomes far more manageable.

The other important idea in this module is that a good adaptation does not have to be a perfect clone. Mobile UIs live on different devices, densities, and font renderers. It is often better to preserve the visual hierarchy, spacing, and behavior of the design than to overfit to one mockup and end up with something brittle. Material icons, reusable borders, and framework-native layout behavior often produce a better result than trying to freeze every pixel exactly as it appeared in the design file.

In the lessons that follow, the design gets broken down into manageable parts: assets that need to be extracted, layout decisions, CSS styling, and the component structure that makes the screen behave like a real application. That is the right mindset for design implementation in Codename One. Start with structure, then layer styling and assets on top.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Themeing](/themeing/)
- [Layout Basics](/layout-basics/)
- [How Do I Create A 9 Piece Image Border](/how-do-i/how-do-i-create-a-9-piece-image-border/)
