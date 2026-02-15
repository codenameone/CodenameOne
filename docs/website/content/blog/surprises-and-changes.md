---
title: Surprises And Changes
slug: surprises-and-changes
url: /blog/surprises-and-changes/
original_url: https://www.codenameone.com/blog/surprises-and-changes.html
aliases:
- /blog/surprises-and-changes.html
date: '2013-11-03'
author: Shai Almog
---

![Header Image](/blog/surprises-and-changes/hqdefault.jpg)

  

We have constant Google alerts setup on various Codename One related keywords and the other day we got a lovely surprise in the form of this great presentation from Mateja Opacic who to my knowledge never contacted us or discussed this with us. Apparently he made this presentation at the  
[  
Coding Serbia Conference  
](http://codingserbia.com/)  
, which is really cool!  
  
  
He has a great presentation & demo.  

  
  
We added a few interesting features and fixes this past week, we now support adding array arguments for connection requests e.g. it would now be possible to do something like myConnectionReuest.addArgument(“key”, new String[] {“val1”, “val2”}); this will essentially add two key entries e.g. key=val1 and key=val2.

  
The LocationManager’s getCurrentLocation  
  
Sync method now allows an optional timeout argument which can be pretty useful.

  
Eric made an interesting contribution to the processing package expression language syntax  
  
[  
providing a contains operator  
](http://code.google.com/p/codenameone/issues/detail?id=928)  
.

  
We also added the AutoCompleteText and SpanLabel to the GUI builder and made some major fixes to the way the GUI builder handles composite components which should remove duplicate entries from such components property sheets.  

  
I’m about to commit a slightly compatibility wrecking change to Storage. We had an issue with files in storage that contained slashes and colons. This sometimes works but it can fail for many devices (justifiably). To solve this we will now sanitize storage names automatically and replace common illegal characters with underscore.  
  
  
  
This should work rather seamlessly for most use cases, however if you have existing files that use illegal characters or you rely on behavior related to these problematic characters you might run into problems e.g. saving “x?” then listing the storage will return “x_” instead.  
  
  
  
To disable this feature we are adding to Storage a flag called is/  
  
setNormalizeNames(), you can use this flag to indicate whether entries should be normalized. The default is true for normalization.  
  
  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — November 6, 2013 at 5:27 pm ([permalink](/blog/surprises-and-changes/#comment-21844))

> Anonymous says:
>
> Thanks for the updates.
>



### **Anonymous** — November 12, 2013 at 1:09 pm ([permalink](/blog/surprises-and-changes/#comment-21765))

> Anonymous says:
>
> nice! when will the update be released?
>



### **Anonymous** — November 12, 2013 at 3:47 pm ([permalink](/blog/surprises-and-changes/#comment-21834))

> Anonymous says:
>
> This week, hopefully tomorrow.
>



### **Anonymous** — November 20, 2013 at 5:51 am ([permalink](/blog/surprises-and-changes/#comment-21777))

> Anonymous says:
>
> i’ve got the update but the support for adding arrays as arguments in the connection request is not there or i’m missing something?
>



### **Anonymous** — November 20, 2013 at 1:00 pm ([permalink](/blog/surprises-and-changes/#comment-21794))

> Anonymous says:
>
> I’m guessing you updated the plugin and not the library. Go to project properties and select update client libraries.
>



### **Anonymous** — November 21, 2013 at 5:12 am ([permalink](/blog/surprises-and-changes/#comment-21750))

> Anonymous says:
>
> thanks! thought it was automatic when updating 😉
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
