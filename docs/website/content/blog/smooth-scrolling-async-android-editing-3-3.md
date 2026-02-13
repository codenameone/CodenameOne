---
title: Smooth Scrolling, Async Android Editing & 3.3
slug: smooth-scrolling-async-android-editing-3-3
url: /blog/smooth-scrolling-async-android-editing-3-3/
original_url: https://www.codenameone.com/blog/smooth-scrolling-async-android-editing-3-3.html
aliases:
- /blog/smooth-scrolling-async-android-editing-3-3.html
date: '2015-12-20'
author: Shai Almog
---

![Header Image](/blog/smooth-scrolling-async-android-editing-3-3/3.3-coming-soon.jpg)

Steve has implemented one of our long time RFE’s: Async editing in Android. This effectively means that the  
Android keyboard will behave as it does in iOS and won’t fold/open as you move between text fields. It will  
also remain open as you scroll the screen and won’t resize the UI as it opens!  
As you can imagine this is a huge change so it is currently off by default but we will flip the change sometime  
next week to be the default. 

To toggle this option use the build hint `android.keyboardOpen` which can be true to enable this  
or false to disable this. This is a big change so keep your eyes open to regressions and  
[report them ASAP](https://github.com/codenameone/CodenameOne/issues/). 

#### Performance Overhaul

We used to always laugh at the Android team for whining over how difficult it is to optimize Android for devices with  
“only” 64mb of RAM. We can work on devices that have 2mb of RAM and leave room to spare!  
The performance of Codename One has always been exceptional in this regard because we just didn’t have a choice  
when running on feature phones. This was pretty true for quite a while but with recent changes to the underlying  
implementation of Android basic things that were quite performant became slow so we had to replace the pipeline. 

Android itself finally invested some effort in performance with their “project butter” and as a result the level of  
smoothness on newer Android devices is spectacular. Unfortunately, we haven’t kept up with that. Worse our  
scrolling behavior was something we designed at Sun based on guesses of how the iPhone scrolling acts. These  
guesses weren’t as good as some of the newer attempts in this field. 

We are attacking this in several ways. Steve wrote a completely new scrollng motion model that feels more native  
and is based on some studies of these algorithms done over the past 8 years. It feels much better than the previous  
friction motion and is currently on by default for all new builds and the new version of the simulator. If you  
still want the old version to work you can still access that using the theme constant `ScrollMotion=Friction`. 

We also worked on the performance of Codename One itself, especially for very large data sets e.g. a  
scrollable Y `Container` with a `BoxLayout.Y_AXIS` that includes many containers  
with labels/images (5,000 was the common test case). We shifted around many method calls and its now pretty  
smooth to scroll such a UI on relatively old Android/iOS devices. But on the older devices its still not perfect so  
we’d like to make this completely smooth even there! 

We’re still working on a major refactoring here and two of the victims are `DefaultLookAndFeel` &amp `LookAndFeel`.  
Both of these classes had larger roles to play in LWUIT but as we moved to Codename One and matured the  
theme system they became mostly redundant. Unfortunately, they contain a lot of functionality some of which  
we intend to keep.   
We decided to to deprecate both of these classes to get started in this long term migration. However, some  
complex functionality such as deriving and overriding these classes will no longer work in upcoming builds! 

The reason for this is simple, we are moving a lot of the paint work into the core OS porting layer which will allow us  
to optimize that functionality using native capabilities that aren’t available in the Codename One abstraction layer.  
E.g. at this time we are working on rewriting the label/button rendering code in C which should make it as performant  
as native OS code. 

#### Version 3.3

We passed the middle of the road sign for 3.3 which should come out on January 27th. So far we are very pleased  
with the rapid release roadmap and we think it really helped tightening the quality and reliability of Codename One.  
It also made [  
versioned builds](/how-do-i---get-repeatable-builds-build-against-a-consistent-version-of-codename-one-use-the-versioning-feature.html) into a far more viable feature than it was prior to 3.1! 

With this in mind we’ll continue the 3 month release schedule, tentatively this is what we are thinking about for 2016  
releases: 

  1. 3.3 – January 27th
  2. 3.4 – May 3rd
  3. 3.5 – August 2nd
  4. 3.6 – December 5th

All of those are still subject to change both in dates and numbering scheme e.g. we might decide that version  
4 is warranted for a particularly exceptional release.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
