---
title: Questions of the Week XXV
slug: questions-of-the-week-xxv
url: /blog/questions-of-the-week-xxv/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xxv.html
aliases:
- /blog/questions-of-the-week-xxv.html
date: '2016-09-29'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xxv/qanda-friday.jpg)

We released an update a day early to deal with some regressions we had related to minor method signature changes  
triggered by an overhaul of pre-released versions. Other than that this week has been a bit uneventful in terms of  
our work.

On stack overflow things were pretty standard:

### How do I import an existing android studio project to eclipse so that it is native and becomes a codename application?

We currently support NetBeans here but this is a bit problematic regardless

[Read on stackoverflow…​](http://stackoverflow.com/questions/39753647/how-do-i-import-an-existing-android-studio-project-to-eclipse-so-that-it-is-nati)

### How to read textfile line by line into textarea in codename one

There are other ways to do this but this is the shortest/simplest

[Read on stackoverflow…​](http://stackoverflow.com/questions/39745935/how-to-read-textfile-line-by-line-into-textarea-in-codename-one)

### Data Storage Solutions

We recommend looking thru the developer guide to get started with those things

[Read on stackoverflow…​](http://stackoverflow.com/questions/39742145/data-storage-solutions-codenameone-java-data)

### Filter optimization on set of containers

`invokeAndBlock` is a tool that is still very cryptic to developers

[Read on stackoverflow…​](http://stackoverflow.com/questions/39739186/codenameone-filter-optimization-on-set-of-containers)

### When I open the Eclipse application I get this exit code error issue. How do I resolve this?

Eclipse is a bit painful with the various versions of Windows

[Read on stackoverflow…​](http://stackoverflow.com/questions/39731558/when-i-open-the-eclipse-application-i-get-this-exit-code-error-issue-how-do-i-r)

### Network check works in android but same doesn’t work in iOS

Some low level features just don’t exist in iOS

[Read on stackoverflow…​](http://stackoverflow.com/questions/39717469/network-check-works-in-android-but-same-doesnt-work-in-ios)

### NullPointerException called on the current variable in codename one

There are several issues with the code but the null pointer is the least of them

[Read on stackoverflow…​](http://stackoverflow.com/questions/39711361/nullpointerexception-called-on-the-current-variable-in-codename-one)

### Two AutoCompleteTextField in same form

Filter for an autocomplete should be “smart” and cache data as much as possible

[Read on stackoverflow…​](http://stackoverflow.com/questions/39711095/two-autocompletetextfield-in-same-form)

### Retrieve IMSI by using CodenameOne, Cordova and AngularJS

iOS etc. don’t provide access to low level details such as IMSI

[Read on stackoverflow…​](http://stackoverflow.com/questions/39701421/retrieve-imsi-by-using-codenameone-cordova-and-angularjs)

### keyboard issue in iOS – codenameone

It’s hard to get the VKB to act “just right” so we need accurate test cases where we can reproduce all the odd edge cases

[Read on stackoverflow…​](http://stackoverflow.com/questions/39698878/keyboard-issue-in-ios-codenameone)

### Flip transition of the form is not smooth in iOS

The transitions that require a screenshot would be slightly slower on iOS as the pipeline for offscreen buffers is much slower

[Read on stackoverflow…​](http://stackoverflow.com/questions/39694708/flip-transition-of-the-form-is-not-smooth-in-ios)

### How to attach a command to longpress on a command in the Toolbar?

We don’t have that builtin but since you can do anything you want in the `Toolbar` it is possible

[Read on stackoverflow…​](http://stackoverflow.com/questions/39690474/how-to-attach-a-command-to-longpress-on-a-command-in-the-toolbar)

### Cannot find type System.ApplicationException in module CommonLanguageRuntimeLibrary

This isn’t really a Codename One question but rather usage of the code we released for the UWP port

[Read on stackoverflow…​](http://stackoverflow.com/questions/39683977/cannot-find-type-system-applicationexception-in-module-commonlanguageruntimelibr)

### TableLayout heightPercentage: percentage of what?

That’s a good question, we simplified layout by blocking recursion so the percentage is based on guesses of eventual  
size

[Read on stackoverflow…​](http://stackoverflow.com/questions/39681343/tablelayout-heightpercentage-percentage-of-what)

### Rebuild list of containers without scrolling the list

This is a bit problematic due to code that tries to “do the right thing”

[Read on stackoverflow…​](http://stackoverflow.com/questions/39675988/rebuild-list-of-containers-without-scrolling-the-list)

### How to detect a ‘pinch out’ in a list of containers?

This isn’t builtin but can probably be done using some low level API’s

[Read on stackoverflow…​](http://stackoverflow.com/questions/39673861/how-to-detect-a-pinch-out-in-a-list-of-containers)

### Setup Google Analytics with Codename One

Google analytics has some configuration complexities

[Read on stackoverflow…​](http://stackoverflow.com/questions/39666886/setup-google-analytics-with-codename-one)

### InfiniteContainer Scrolling

This might also be a regression related to new peer. It’s very hard to see in newer devices but you see it for a split  
second in very old phones

[Read on stackoverflow…​](http://stackoverflow.com/questions/39644972/codenameone-infinitecontainer-scrolling)

### Media has stopped working the way it should

This might be a regression related to the migration to the Android new peer mode

[Read on stackoverflow…​](http://stackoverflow.com/questions/39404180/codename-one-mediaplayer-doesnt-release-video-on-form-change/39422767?noredirect=1#comment66823603_39422767)

### IOS Build fails with annotations

We don’t support runtime annotations due to problematic use cases involved. We need to work on improving the  
failure related to that or implementing it

[Read on stackoverflow…​](http://stackoverflow.com/questions/39637998/ios-build-fails-with-annotations)

### Scrolling issue with keyboard

There are some odd edge cases with the virtual keyboards in the various OS’s. Normally native widget libraries  
recommend various hacks but we try to make it seamless

[Read on stackoverflow…​](http://stackoverflow.com/questions/39636049/codenameone-scrolling-issue-with-keyboard)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
