---
title: Questions of the Week 30
slug: questions-of-the-week-30
url: /blog/questions-of-the-week-30/
original_url: https://www.codenameone.com/blog/questions-of-the-week-30.html
aliases:
- /blog/questions-of-the-week-30.html
date: '2016-11-03'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-30/qanda-friday2.jpg)

30 is probably the perfect time to stop with Roman numerals…​ We don’t want to give search engines the wrong  
idea.  
This has been a really busy week with the new [Phoenix demo](/blog/template-phoenix-ui.html) and quite  
a few other developments. October is finally over and we are all back home from vacations & travels. We are still  
in “recovery mode” but looking at the pipeline we have pretty exciting things coming up!

Chen has made huge strides with the new GUI builder, to such an extent that we decided to release a plugin update  
and not just a regular update. We’ll write more next week about some of the changes that are included in this update.

On stack overflow things were as usual:

### How to simulate click/tap in Codename One?

Generally this sort of code should work fine unless something odd happens or a special case…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40388364/how-to-simulate-click-tap-in-codename-one)

### DNS problems in Codename One

This is quite misleading, it turns out that this is just another case of confusion over Apples strict HTTPS policy

[Read on stackoverflow…​](http://stackoverflow.com/questions/40387820/dns-problems-in-codename-one)

### Make Codename One text area full width

When changing style values it is crucial to do this in both the selected and unselected modes

[Read on stackoverflow…​](http://stackoverflow.com/questions/40375922/make-codename-one-text-area-full-width)

### Button from an iframe in WebBrowser does not work

Setting properties for browser options in a device dependent and we allow doing that to configure quite a few features on Android/iOS

[Read on stackoverflow…​](http://stackoverflow.com/questions/40364417/button-from-an-iframe-in-webbrowser-does-not-work)

### New Gui does not accept placement “Right” on a command

This was a bug that Chen solved really quick, please report GUI builder bugs otherwise we can’t possibly refine the tool

[Read on stackoverflow…​](http://stackoverflow.com/questions/40353662/new-gui-does-not-accept-placement-right-on-a-command)

### Dragging connected Lines together

This was pretty unclear to me, it turns out the question was about multi-touch…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40351779/codename-one-dragging-connected-lines-together)

### Picker & GUI Builder, how to set the type

Since the type property for picker objects are not exposed in the GUI Builder, what’s the proper way to set the type via code for a picker object placed using the GUI Builder? I’ve tried doing so …​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40348105/picker-gui-builder-how-to-set-the-type)

### Received exception: SSL handshake failed

Server certificates and JavaSE are pretty confusing

[Read on stackoverflow…​](http://stackoverflow.com/questions/40347706/received-exception-ssl-handshake-failed)

### No animation with snapToGrid scrolling and tensileDragEnabled set to false

I’m not so sure why this would happen but if my suggested workaround “works” then we might be able to fix this

[Read on stackoverflow…​](http://stackoverflow.com/questions/40342090/no-animation-with-snaptogrid-scrolling-and-tensiledragenabled-set-to-false)

### The codenameon demos under netbean 8.2 do not work

This was a regression in the NetBeans plugin. We fixed it in recent updates and also mention a workaround in the post

[Read on stackoverflow…​](http://stackoverflow.com/questions/40313225/the-codenameon-demos-under-netbean-3-2-do-not-work)

### Codename One Issue publishing ios 64 bit app on store

I’m still not sure why he was getting this, we generate a 64+32 bit fat binary for appstore builds

[Read on stackoverflow…​](http://stackoverflow.com/questions/40307900/codename-one-issue-publishing-ios-64-bit-app-on-store)

### ImageViewer produce blank image on ipad but work fine on android and simulator

These things usually happen due to corrupt images often due to assumptions about the behavior of `InputStream` that  
are misplaced

[Read on stackoverflow…​](http://stackoverflow.com/questions/40301199/imageviewer-produce-blank-image-on-ipad-but-work-fine-on-android-and-simulator)

### How do I change the style of a floatingactionbutton?

As luck would have it we just released an extensive demo where we styled it a lot. We also enhanced its style-ability  
quite a bit this week…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40293341/how-do-i-change-the-style-of-a-floatingactionbutton)

### Speech Recognition simple example on Android in Codename One

Most such issues (this one include) are due to permission problems

[Read on stackoverflow…​](http://stackoverflow.com/questions/40290208/speech-recognition-simple-example-on-android-in-codename-one)

### Jar size 50MB limit

I explained here why we set the limit at 50mb. Turns out that in this case it helped pinpoint a developer error

[Read on stackoverflow…​](http://stackoverflow.com/questions/40289063/jar-size-50mb-limit)

### Select an Audio File from File System

This is much easier now with the new file picker cn1lib from Steve

[Read on stackoverflow…​](http://stackoverflow.com/questions/40285172/select-an-audio-file-from-file-system)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
