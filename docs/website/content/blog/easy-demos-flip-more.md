---
title: Easy Demos, Flip & More
slug: easy-demos-flip-more
url: /blog/easy-demos-flip-more/
original_url: https://www.codenameone.com/blog/easy-demos-flip-more.html
aliases:
- /blog/easy-demos-flip-more.html
date: '2015-02-22'
author: Shai Almog
---

![Header Image](/blog/easy-demos-flip-more/flip.png)

![](/blog/easy-demos-flip-more/flip.png)

One of the pains in Codename One is the access to the demos, yes we have the downloadable demo bundle and the SVN but for a compete novice to Codename One this isn’t front and center. Chen decided to address that by embedding the latest versions of the demos both into the Eclipse and the NetBeans plugins, now when you create a new Codename One project you can also create a demo project and “just run it”. This allows you to quickly learn/debug our sample code which should help with the Codename One learning curve. 

Chen and Steve also adapted the code built by Steve as part of the new graphics pipeline for [perspective transforms](http://www.codenameone.com/blog/perspective-transform)  
and introduced a cool new [flip transition](/javadoc/com/codename1/ui/animations/FlipTransition.html) that you can see in the video below, its really trivial to use just set form.setTransitionOutAnimation(new FlipTransition()); and that’s it. This is already in the current plugin and you can play with it right now! 

As part of ongoing support for customers we also introduced improved location support on Android which uses the new Google Play API’s that include hybrid location. This means location should be more accurate and return a result faster, this will only be used if you integrate the Google Play Services support (which you can now rely on if you write Android native interfaces). To do that just add the build hint android.includeGPlayServices=true and this will be used seamlessly. 

The [infinite adapter API](http://www.codenameone.com/blog/till-the-end-of-the-form) is sometimes  
[less intuitive](/javadoc/com/codename1/components/InfiniteScrollAdapter.html)  
to developers who just want to work with a Container that makes a request for more components, to simplify this use case we added a new  
[InfiniteContainer API](/javadoc/com/codename1/ui/InfiniteContainer.html)  
which is based on the infinite adapter but provides a more intuitive (albeit less flexible) API. When using the infinite container API you need to subclass it  
and override the fetch components method to fetch additional entries.

Last but not least we now have a [simple weak hash map implementation](/javadoc/com/codename1/ui/util/WeakHashMap.html) that is very similar to the one in java util but under our own package at the moment.  
This is a very useful class for caching temporary objects.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
