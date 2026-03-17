---
title: TAKE A PICTURE WITH THE CAMERA
slug: how-do-i-take-a-picture-with-the-camera
url: /how-do-i/how-do-i-take-a-picture-with-the-camera/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-take-a-picture-with-the-camera.html
tags:
- basic
description: Using the capture API to take a picture
youtube_id: nF4eqzVcsic
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-29.jpg
---
{{< youtube "nF4eqzVcsic" >}}

Taking a picture in Codename One is conceptually simple: ask the platform to capture an image, get back a file, load the image you actually want to display, and then update the UI. The details matter because camera images can be much larger than the UI really needs.

The `Capture` API supports several capture workflows, including direct image capture and variants that return later through a callback. The most practical lesson from the video is still the most important one: do not casually load the full original camera image if all you need is something that fits on screen. High-resolution camera output can consume a lot of memory, and many apps only need a resized version for preview or upload.

That is why scaling at capture time is often the right default. If the goal is to show a preview inside the app, scaling the image to something close to the display width is usually much safer than loading the raw camera file and hoping memory usage stays reasonable. The video demonstrates this with a width-based capture and aspect-ratio preservation, and that is still a sensible pattern.

The next practical detail is that the UI needs to react to the new image. If you replace the icon on a label or another display component, the form may need to revalidate or repaint so the layout can account for the new content. This is especially noticeable when the placeholder and the captured image have very different sizes.

The simulator behavior is also worth understanding. On the simulator, capture usually behaves more like a file chooser than a real camera. On a device, it invokes the actual camera flow. That means simulator testing is good for the code path, but device testing is still necessary for the real user experience, permissions, and camera integration behavior.

The modern advice here is mostly about restraint. Capture the image you need, not the largest image the device can produce. Resize early when appropriate, keep memory in mind, and treat image loading as a UI and performance concern, not just a feature checkbox.

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Improve Application Performance Or Track Down Performance Issues](/how-do-i/how-do-i-improve-application-performance-or-track-down-performance-issues/)
- [How Do I Use Storage, File System And SQL](/how-do-i/how-do-i-use-storage-file-system-sql/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
