---
title: Questions of the Week XVII
slug: questions-of-the-week-xvii
url: /blog/questions-of-the-week-xvii/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xvii.html
aliases:
- /blog/questions-of-the-week-xvii.html
date: '2016-08-04'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xvii/qanda-friday.jpg)

We just flipped the switch on [InfiniteContainer](/blog/synchronous-inifinitecontainer.html) which is now more  
synchronous and complies with the EDT policy by default. We are already back to committing changes and  
have been busy updating with all of the things that didn’t make it into 3.5.

A lot of those changes are a part of the release we made today & we have quite a few other things in the pipeline.

Onwards to the questions on stackoverflow:

### SpanLabel in Codename one with BoxLayout along the y axis does not span

If you invoke `setScrollable(true)` you are effectively allowing scroll on the X axis too which is probably not what you want…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38750854/spanlabel-in-codename-one-with-boxlayout-along-the-y-axis-does-not-span)

### Is there a way to specify the size of an image in the resource file?

Dealing with the amount of densities and the related complexities is one of the hardest problems of mobile development

[Read on stackoverflow…​](http://stackoverflow.com/questions/38732438/is-there-a-way-to-specify-the-size-of-an-image-in-the-resource-file)

### Picker object

Pickers have a lot of limits to allow more flexibility on the native layer. A lowest common denominator approach  
that’s enforced due to the usage of native peers.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38732432/codename-one-picker-object)

### How do I set the color of the radio button’s control?

We made radio buttons use icon fonts in recent versions which we tend to prefer over setting their icons in the  
theme constants but that too can be done.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38732233/how-do-i-set-the-color-of-the-radio-buttons-control)

### Codenameone:Unable to set floating hint to textfield

`FloatingHint` hides the `TextField` within it so you shouldn’t add the `TextField` again.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38720094/codenameoneunable-to-set-floating-hint-to-textfield)

### Image selected from Camera Roll shows up as black screen

Not sure why this is happening to her so if other people are seeing similar issues please let us know…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38719391/image-selected-from-camera-roll-shows-up-as-black-screen)

### Passing array of strings containing float values to POST method

This is clearly a problem in the PHP server code but since there is no code and I have no PHP experience I can’t  
help much…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38718299/passing-array-of-strings-containing-float-values-to-post-method)

### Unable to call service from simulator

Could we have phrased the https warning using clearer syntax?

[Read on stackoverflow…​](http://stackoverflow.com/questions/38718101/codenameone-unable-to-call-service-from-simulator)

### Fly over animation

Animating between 2 disconnected containers is challenging in Codename One

[Read on stackoverflow…​](http://stackoverflow.com/questions/38710316/codename-one-fly-over-animation)

### Cannot override dragfinished method

I have no idea why this is happening to this guy…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38709119/cannot-override-dragfinished-method#38709119)

### How to hash data with custom key using SHA1Digest algorithm

Encryption is such a painful subject and the bouncy castle cn1lib is really under-documented. We’d love to have  
a cn1lib section in the developer guide that covers all the big cn1lib’s and their use cases but the effort is huge  
and we don’t have the resources…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38673149/how-to-hash-data-with-custom-key-using-sha1digest-algorithm-in-codenameone)

### Why is my Codename One Project “not a visual project type”?

These are some of the reasons we are migrating to the new GUI builder. The old GUI builder was very confusing  
to developers that are new to Codename One.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38669751/why-is-my-codename-one-project-not-a-visual-project-type)

### How do I control the width of the overflow menu?

The overflow menu is a bit confusing as it uses the old `List` based menu behavior and that’s much harder to theme  
or customize than the buttons we use elsewhere.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38671424/how-do-i-control-the-width-of-the-overflow-menu)

### Setting colors for the overflow menu

Another example of the problematic nature of the overflow menu list…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38669420/setting-colors-for-the-overflow-menu)

### Annotation Processing

I think we need a better “unified” approach for what is typically handled via reflection/bytecode manipulation  
etc.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38667201/codename-one-annotation-processing)

### Sharing vs. Copying

Copy/Paste hasn’t been so much in demand on mobiles since the advent of share capabilities which answer  
most of the use cases for copying.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38657660/sharing-vs-copying)

### Why isn’t my generated keystore file showing up?

We have seamless tools to generate keystores within the settings UI.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38639277/why-isnt-my-generated-keystore-file-showing-up)

### Codenameone: Unable to find packages during build

This is one of the first things people run into when working with Codename One, some things just don’t work  
because a specific Java feature is missing.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38636801/codenameone-unable-to-find-packages-during-build)

### Can I turn off banner ads in code?

No. We are moving away from the way in which we implemented banner ads to using cn1libs as much as possible.  
This gives the developers more flexibility both in using the code and working with the ad provider.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38631317/can-i-turn-off-banner-ads-in-code)

### Codenameone IOS app is crashing randomly

These things are painfully hard to track down and debug…​ There are many reasons for a potential crash and  
in most cases it’s just an out of memory crash. Those usually appear within the crash log.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38629002/codenameone-ios-app-is-crashing-randomly)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
