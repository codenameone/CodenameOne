---
title: Spanning, Caching, Native Fonts and more
slug: spanning-caching-native-fonts-and-more
url: /blog/spanning-caching-native-fonts-and-more/
original_url: https://www.codenameone.com/blog/spanning-caching-native-fonts-and-more.html
aliases:
- /blog/spanning-caching-native-fonts-and-more.html
date: '2013-07-24'
author: Shai Almog
---

![Header Image](/blog/spanning-caching-native-fonts-and-more/spanning-caching-native-fonts-and-more-1.png)

  
  
  
[  
![Picture](/blog/spanning-caching-native-fonts-and-more/spanning-caching-native-fonts-and-more-1.png)  
](/img/blog/old_posts/spanning-caching-native-fonts-and-more-large-3.png)  
  
  

When you have so many features as Codename One has things sometimes slip, a feature gets lost and even the guys who created it forget it was there. There are two such stories in this post one dates back to the very start of Codename One….  
  
  
But first lets start with the other one. A month or so ago we added a component called SpanButton which has been requested by developers for quite a while. This is effectively a button that can break lines like a text area, its really a Lead Component internally (but that’s another post).  
  
  
  
However, someone (me) forgot to make the class public before committing and we just didn’t notice that.  

  
  
I  
  
n the release  
  
we made today the class is both public and it is also in the designer tool so you can just drag it into place just like any other button.  
  
It has features and behaviors similar to MultiButton since it too is a lead component based composite.

**  
CachedDataService  
**  
  
  
  
The CachedDataService is one of those services we added ages ago because we thought its important to have, then completely forgot about it. As it happens we forgot to write about it and so no one really used it to any extent (since it didn’t work). These past two weeks a developer on the forum (Fabrice Kabongo) drew my attention to the fact that this class doesn’t work and also provided helpful suggestions for fixes, I wrote it yet didn’t even recall its purpose.

  
This is a shame since the class is actually pretty useful and its possible we should update our internal code to use it E.g. say you have an image stored locally as  
  
image X. Normally the ImageDownloadService will never check for update if it has a local cache of the image. This isn’t a bad thing, its pretty efficient.  
  
  
However, it might be important to update the image if it changed but you don’t want to fetch the whole thing…  

  
The cached data service will fetch data if it isn’t cached locally and cache it. When you "refresh" it will send a special HTTP request that will only send back the data if it has been updated since the last refresh.  
  

* * *

  
  
  
[  
![Inspiration in a box](/blog/spanning-caching-native-fonts-and-more/spanning-caching-native-fonts-and-more-2.png)  
](http://www.inspirationinabox.net/we-love-helvetica-tantissimi-esempi-di-design-usando-il-font-helvetica/)  
  
Image copyright: Inspiration in a box  
  

  
**  
Using Helvetica on iOS  
**  
  
Chen was dealing with some designers this week and was facing requirements to incorporate Helvetica derivatives into an iOS application. Normally we allow developers to just embed a  
[  
TTF into the application  
](http://codenameone.blogspot.com/2012/11/fonts-revisited.html)  
and it "just works" but Helvetica is builtin to iOS and shouldn’t be used on other platforms (due to copyrights). So how do you use a font such as HelveticaNeue?  
  
Simple: Font helveticaNeue = Font.createTrueTypeFont("HelveticaNeue", null); 

This will "just work" even though the TTF file name is null it will just search the system. Notice that it will crash if the font doesn’t exist so use it carefully!  
  
To get a specific size in pixels e.g. 14p just go with: Font fontNeue14 = helveticaNeue.derive(14, Font.STYLE_PLAIN); 

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — July 25, 2013 at 9:49 am ([permalink](/blog/spanning-caching-native-fonts-and-more/#comment-24233))

> Anonymous says:
>
> Thanks guys. I was thinking of how to implement that in an upcoming project. 🙂
>



### **Anonymous** — July 25, 2013 at 10:43 am ([permalink](/blog/spanning-caching-native-fonts-and-more/#comment-21699))

> Anonymous says:
>
> I was wondering how CachedData was supposed to work, now I will give it a shot. 
>
> About the TT fonts, I believe it is possible to add this support to j2me/BB devices, at least I was able to do some basic rendering without anti-aliasing
>



### **Anonymous** — July 25, 2013 at 2:36 pm ([permalink](/blog/spanning-caching-native-fonts-and-more/#comment-21843))

> Anonymous says:
>
> TTF files are supported on RIM. On J2ME they can’t be supported though. We have bitmap font support for J2ME devices but we don’t recommend using it.
>



### **Anonymous** — July 25, 2013 at 3:59 pm ([permalink](/blog/spanning-caching-native-fonts-and-more/#comment-21764))

> Anonymous says:
>
> Nice to know RIM supports TTF, about J2ME I do believe it can support TTF, and it would replace the bitmap font for sure.
>



### **Anonymous** — September 10, 2014 at 6:28 pm ([permalink](/blog/spanning-caching-native-fonts-and-more/#comment-21685))

> Anonymous says:
>
> The example code shown above for CacheDataService has a bug (aside from the second related bug that prevents it from even compiling). 
>
> The actionPerformed method body shown is trying to re-save the old CachedData object, not the new one with the updated data ( d does not get updated in-place inside CachedDataService.updateData ). The updated value is sent in the ActionEvent to the ActionListener). 
>
> The actionPerformed method body should be something like this instead: 
>
> NetworkEvent ne = (NetworkEvent)ev; 
>
> CachedData newCachedData = (CachedData)ne.getMetaData(); 
>
> Storage.getInstance().writeObject("LocallyCachedData", newCachedData);
>



### **Anonymous** — September 11, 2014 at 1:43 am ([permalink](/blog/spanning-caching-native-fonts-and-more/#comment-22188))

> Anonymous says:
>
> Thanks 😉
>



### **Anonymous** — November 6, 2014 at 6:46 am ([permalink](/blog/spanning-caching-native-fonts-and-more/#comment-22189))

> Anonymous says:
>
> I have created one bitmap to change the font style and the create LabelFont and add the font . 
>
> The LabelFont uiid is used in Label and test on emulator , the new font is displayed but the font is not displayed in Mobile and tablet , why?
>



### **Anonymous** — November 6, 2014 at 3:27 pm ([permalink](/blog/spanning-caching-native-fonts-and-more/#comment-22225))

> Anonymous says:
>
> Do you mean bitmap font? 
>
> Which font are you using?
>



### **Anonymous** — November 7, 2014 at 3:47 am ([permalink](/blog/spanning-caching-native-fonts-and-more/#comment-22106))

> Anonymous says:
>
> yes its bitmap font 
>
> And I am using these magneto monospaced fonts but the font is shown in emulator but not the font is not displayed in mobile and tablet
>



### **Anonymous** — November 8, 2014 at 12:15 pm ([permalink](/blog/spanning-caching-native-fonts-and-more/#comment-22031))

> Anonymous says:
>
> Don’t use bitmap fonts, we deprecated those. Use TTF fonts.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
