---
title: KitchenSink UWP
slug: kitchensink-uwp
url: /blog/kitchensink-uwp/
original_url: https://www.codenameone.com/blog/kitchensink-uwp.html
aliases:
- /blog/kitchensink-uwp.html
date: '2016-09-19'
author: Shai Almog
---

![Header Image](/blog/kitchensink-uwp/kitchensink-sidemenu.png)

The latest plugin update includes a new kitchen sink version that includes quite a few bug fixes, enhancements  
and a new side menu design that looks much better. We also got the demo published on the Windows Store  
thru the UWP port which isn’t so much a testament to the maturity of the port as much as it is to the limited  
testing done by Microsoft.

The demo on UWP is still broken in some fundamental ways e.g. shape graphics required for the clock are still  
not ready. Quite a few other things should be implemented but it should still install & run. You can install this  
demo on a Windows 10 device or desktop from [here](https://www.microsoft.com/en-us/store/p/codename-one-kitchen-sink/9nblggh528c7).

We made a lot of fixed for the UWP port over the past few days so if you were having issues they might have been  
resolved by now. There are still some pieces of functionality missing which we hope to get to during the 3.6  
line but realistically a production release of UWP that is “feature complete” might wait to 3.7.

Notice that the version of the kitchen sink that is currently in the store (2.01) predates some of the latest fixes,  
we’ve submitted updates to the versions in all the stores but naturally the update process takes time for approval  
on iOS/Windows. Version 2.02 should contain most of the fixes for the kitchen sink and the current batch of UWP  
fixes, since that platform is evolving faster we might need to update it further.

### Commenting the Demo

Bryan [posted a comment](http://www.codenameone.com/blog/different-icons.html#comment-2898582862) on  
a previous post & I think this bares repeating in a post:

> Re Kitchen Sink – I’ve been looking at the source to see how some of the stuff in the demo is done, and it would  
>  be really good if there were more comments or description in the code (or accompanying the demo) so  
>  developers could copy some of the techniques used. There’s a bunch of stuff obviously possible with CN1,  
>  but much of it is hidden away and not always obvious that what’s possible.

That’s great feedback, you can do this to help us find these cases that are unclear:

Go to the sources that aren’t clear, press the edit icon on the top right and just insert your comments or even  
“can you explain this?” comments.

Once you are done with the change just click the pull request button.

We will merge the pull request and then fix the “explain this” comments. You don’t even need to check out the project.

This doesn’t just apply to the kitchen sink!

You can do this to any demo or even Codename One itself, if code isn’t clear or a comment isn’t clear just let us  
know directly in the code. This way the entire community can benefit from this.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
