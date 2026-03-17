---
title: "Cutting Images in Photoshop"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Extracting a UI Design"
module_key: "03-extracting-a-ui-design"
module_order: 3
lesson_order: 2
weight: 5
is_course_lesson: true
description: "Extract only the image assets a Codename One UI actually needs from a design file."
---
> Module 3: Extracting a UI Design

{{< youtube exQ8xXAxtoU >}}

When you receive a design file, the goal is not to export everything. The goal is to extract only the assets that should really remain images in the final application. That sounds obvious, but it is one of the easiest mistakes to make when turning a design into code. If you export too much, you end up with a UI that is heavy, rigid, and difficult to adapt.

The video demonstrates this using Photoshop, and the basic workflow still holds up: isolate the part you need, remove anything that should remain live UI, and export only the visual element that actually belongs in an asset file. Today you might do the same work in Figma, Sketch, or another design tool, but the decision-making process is the same.

Background photos are a good example. If the image is purely decorative and really is a photo, then exporting it as an image makes sense. A JPEG is often appropriate there because file size matters and you do not need transparency. On the other hand, if a visual element needs transparency, sharp edges, or mask-style behavior, a PNG is often the better choice.

Rounded cards, masks, and border fragments should be handled with even more care. Sometimes a cropped image is the right answer. Sometimes a nine-piece border is the right answer. Sometimes the best answer is not an image at all because CSS, borders, padding, and standard components can express the same effect more cleanly. The lesson is useful because it forces you to ask that question for each element instead of exporting blindly.

This is also where asset discipline starts paying off. Remove text from exported buttons if the label should remain live and localizable. Keep decorative images separate from content images. Export the smallest useful region instead of the whole screen. Those choices make the final app easier to style, localize, and maintain.

So while the tooling in the video is older and Photoshop-specific, the principle is current: treat image extraction as selective engineering work, not as screenshot slicing. Every asset you keep should earn its place.

## Further Reading

- [Themeing](/themeing/)
- [How Do I Create A 9 Piece Image Border](/how-do-i/how-do-i-create-a-9-piece-image-border/)
- [How Do I Fetch An Image From The Resource File, Add A Multiimage](/how-do-i/how-do-i-fetch-an-image-from-the-resource-file-add-a-multiimage/)
- [Adapting a UI Design](/courses/course-01-java-for-mobile-devices/009-adapting-a-ui-design/)
