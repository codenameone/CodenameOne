---
title: 'TIP: Don''t use File – Save As in Photoshop, only use Export'
slug: tip-dont-use-file-save-as-in-photoshop
url: /blog/tip-dont-use-file-save-as-in-photoshop/
original_url: https://www.codenameone.com/blog/tip-dont-use-file-save-as-in-photoshop.html
aliases:
- /blog/tip-dont-use-file-save-as-in-photoshop.html
date: '2016-10-23'
author: Shai Almog
---

![Header Image](/blog/tip-dont-use-file-save-as-in-photoshop/just-the-tip.jpg)

This isn’t a programming tip, but since we [discussed photoshop quite a few times before](/blog/psd-to-app-revisited.html)  
it’s probably an important subject. Especially because we made this exact mistake in those tutorials and  
I’ve spent the better part of the day tracking this with no result…​

I’ve worked on extracting UI’s from PSD files for a while now and I was on my way to build a new UI when suddenly  
I noticed my theme file was **HUGE**. Not just “big”, **HUGE**.

Looking at the images I’ve noticed they were ridiculously big so I started doing the typical things (bluring, scaling,  
reducing quality) all of which didn’t work until I was able to come up with an odd workaround by saving as  
JPEG2000 and then resaving as a regular file.

Then I noticed that this didn’t just impact JPEG files…​ PNG files that contained nothing other than black and white  
pixels were over 800kb in size!

That’s when the hex editor came up and showed that something was really broken, the file was filled with gibberish  
meta-data that I was able to find in the photoshop UI but there is no place to edit this data.

I was eventually able to find [this](https://forums.adobe.com/thread/1596435) and several other sources that confirmed  
that this behavior is “by design” which is insane…​ I found no way to remove the meta-data or edit it after the fact.  
Even OptiPNG wouldn’t remove it!

So the solution is simple if a bit obtuse, when you are ready to export an image always use File → Export &  
never use File → Save As. Notice that this only impacts regular images. Multi-Images are automatically  
converted and are not impacted as a result.

### Edit in Photoshop

If we are already on the subject of photoshop here is another tip that has been there since before Codename One  
but most developers are probably unfamiliar with…​

You can edit every image File directly in photoshop without re-importing or any complex task. To do so just  
use the Edit button above the image. This also works for multi-images although obviously it only impacts  
one of the resolutions.

For this to work the default action for PNG/JPG in your system must be photoshop (or GIMP if you are so inclined)  
we implicitly do the rest. Just edit the file and save to see the changes reflected in the designer tool.

__ |  This might not work in Linux due to the horrible support of the JavaSE `Desktop` API in Linux   
---|---  
  
![You can edit images from the designer tool directly in Photoshop](/blog/tip-dont-use-file-save-as-in-photoshop/edit-in-photoshop.png)

Figure 1. You can edit images from the designer tool directly in Photoshop

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
