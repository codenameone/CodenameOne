---
title: Simple Stuff
slug: simple-stuff
url: /blog/simple-stuff/
original_url: https://www.codenameone.com/blog/simple-stuff.html
aliases:
- /blog/simple-stuff.html
date: '2013-10-20'
author: Shai Almog
---

![Header Image](/blog/simple-stuff/simple-stuff-1.jpg)

  
  
  
  
![Simple](/blog/simple-stuff/simple-stuff-1.jpg)  
  
  
  

I hope you registered for the upcoming  
[  
LTS lecture  
](http://www.luxoft.com/lts-luxoft-technology-series/java-mobile-applications/)  
, last week there were close to 1000 developers registered!  
  
  
  
  
  
  
Sometimes the most important features we can add are really simple things that we just procrastinated on forever. Case in point: multi-line labels. 

  
For years we have been explaining to people over and over again: “Just use TextArea and invoke setUIID(“Label”)”.  

  
This has three major  
  
issues:  

  1.   
People keep asking (always a bad sign, means the API isn’t intuitive enough).  

  2.   
They also need to do other stuff – setFocusable(false), setEditable(false) so this is starting to get pretty verbose.  

  3.   
This isn’t localizable!  

  
When we  
[  
localize  
](/how-do-i.html)  
an app the labels/buttons etc. (buttons derive from labels) are localized seamlessly. However, TextArea’s are not localized since we assume they will contain data which you normally don’t localize (user data is already in the right language). The problem is that in this case we do want to localize the label since this is really “a label”.  

  
So I broke down, climbed down from the tree and added a SpanLabel class which is  
[  
solving RFE 919  
](http://code.google.com/p/codenameone/issues/detail?id=919)  
.  
  
Simple yet very helpful.

  
A similar situation exists in the location API code, we get a question once a week: why doesn’t getCurrentLocation() work?  
  
  
  
So we start explaining that you need to bind a listener etc. bla, bla, bla…. Not very helpful…  
  
  
  
If there is boilerplate you need to write maybe we should write it for you. This is what we did in the past when people had issues with the complex async version of capture. We added a synchronous version that was much simpler and the questions practically disappeared from the mailing list! So we are doing the same for the location API, we now have a new method:  
  
getCurrentLocationSync();  
  
  
  
which will return your location or null if there is an error.  
  
  
  
Simple! 

  
You should show a progress dialog while it is running since it might take some time.  
  
  
  
Its not an ideal way to track location, this is only for relatively simple use cases.  
  
  
  
  
  
On a different note I was reviewing code for one of our enterprise customers and on a call with said customer he pointed out that they re-did some of our GoogleAnalitics work. Turns out that when we weren’t looking Google published a REST API for the tool that allows tracking applications more effectively!  
  
  
Back when they launched their new analytics for applications support, they didn’t have a REST API so we had to use the WEB API which sucks!  

  
The customer in question agreed to contribute the code which I assimilated into our Analytics API. Its off by default but if you will invoke AnaliticsService.  
  
setAppsMode(true) app tracking should start using the new Google API which might have quite a few advantages for you. We didn’t get a chance to test it much but it should be pretty cool.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — October 21, 2013 at 6:37 pm ([permalink](https://www.codenameone.com/blog/simple-stuff.html#comment-21896))

> Anonymous says:
>
> Thanks. Mulitiline-Labels were really missing. 
>
> Can we use SpanLabel to build Mulitline-Buttons somehow, for example by manipulating uuid? 
>
> I have build Mullitiline-Buttons in LWUIT with Containers and leadComponent, the solution is quite long and event handling also got also more complex.
>



### **Anonymous** — October 22, 2013 at 3:08 am ([permalink](https://www.codenameone.com/blog/simple-stuff.html#comment-24256))

> Anonymous says:
>
> We’ve had SpanButton for quite a while now, I think I even blogged about it.
>



### **Anonymous** — October 31, 2013 at 5:21 pm ([permalink](https://www.codenameone.com/blog/simple-stuff.html#comment-24234))

> Anonymous says:
>
> How about updating the JavaDocs? 
>
> [codenameone_docs_demos_2013…](<http://codenameone_docs_demos_20130518.zip>) is the latest available on [http://www.codenameone.com/…](<http://www.codenameone.com/download.html>)
>



### **Anonymous** — November 1, 2013 at 4:29 am ([permalink](https://www.codenameone.com/blog/simple-stuff.html#comment-21700))

> Anonymous says:
>
> Thanks for the suggestion. We are slow with updating that file (only with major version updates) mostly because of the hassle required to do so. 
>
> The javadocs are all up here [https://codenameone.googlec…](<https://codenameone.googlecode.com/svn/trunk/CodenameOne/javadoc/index.html>) with a pretty recent version. 
>
> We are looking into automating the creation of this file but in the past we ran into some issues there.
>



### **Anonymous** — September 25, 2020 at 11:38 am ([permalink](https://www.codenameone.com/blog/simple-stuff.html#comment-21391))

> Anonymous says:
>
> [deleted]
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
