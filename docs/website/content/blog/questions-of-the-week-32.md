---
title: Questions of the Week 32
slug: questions-of-the-week-32
url: /blog/questions-of-the-week-32/
original_url: https://www.codenameone.com/blog/questions-of-the-week-32.html
aliases:
- /blog/questions-of-the-week-32.html
date: '2016-11-17'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-32/qanda-friday2.jpg)

I practically had a vacation from blogging these past two weeks with all the great posts from Steve. I’ve put some  
work into something interesting but it’s not yet ready for prime time. If you follow the git commits you might have  
noticed we’ll discuss it when we think it’s ready.

This week we (thankfully) aren’t releasing a plugin update and just releasing the standard weekly release as usual.  
Hopefully, things will cool down a bit and we’ll be able to sprint to the 3.6 release easily.

On stack overflow things were as usual:

### Codename one Mock location permission is added, how to remove it?

This is a feature we added for a user a while back, we provided a way to disable this in a build hint

[Read on stackoverflow…​](http://stackoverflow.com/questions/40640120/codename-one-mock-location-permission-is-added-how-to-remove-it)

### Codename one project Run issue after it was running

It’s important to keep the jars and classpath as they were

[Read on stackoverflow…​](http://stackoverflow.com/questions/40631786/codename-one-project-run-issue-after-it-was-running)

### Determine how much of a component is visible on screen

This is problematic as the abstraction in the hierarchy can’t really tell that, but normally you don’t need to know that…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40630593/determine-how-much-of-a-component-is-visible-on-screen)

### Multiple image selection from gallery

This isn’t supported currently

[Read on stackoverflow…​](http://stackoverflow.com/questions/40619973/multiple-image-selection-from-gallery)

### Manually Editing Codename One (Old) UI Designer Built Project

This is a bit challenging but possible

[Read on stackoverflow…​](http://stackoverflow.com/questions/40619487/manually-editing-codename-one-old-ui-designer-built-project)

### File chooser dialog

We just posted about this in [yesterdays blog post](/blog/native-file-open-dialogs.html)

[Read on stackoverflow…​](http://stackoverflow.com/questions/40611518/codename-one-file-chooser-dialog)

### How to call Codenameone java file from a html file?

The JavaScript bridge is designed with this exact purpose in mind

[Read on stackoverflow…​](http://stackoverflow.com/questions/40610657/how-to-call-codenameone-java-file-from-a-html-file)

### Password hide in textfield codenameone

Various Android virtual keyboards act oddly sometimes

[Read on stackoverflow…​](http://stackoverflow.com/questions/40606612/password-hide-in-textfield-codenameone)

### A webBrowser form crashes the app when backed

There usually isn’t much we can do against a hard crash in Android unless it’s a known issue and even then…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40606281/a-webbrowser-form-crashes-the-app-when-backed)

### Getting Uncaught ReferenceError: camera is not defined

The simulator can’t handle some of the more advanced HTML capabilities because of limitations in JavaFX

[Read on stackoverflow…​](http://stackoverflow.com/questions/40588416/getting-uncaught-referenceerror-camera-is-not-defined)

### captureAudio example in codenameone not working on device simulator

The simulator doesn’t support actual audio capture only using a file

[Read on stackoverflow…​](http://stackoverflow.com/questions/40576111/captureaudio-example-in-codenameone-not-working-on-device-simulator)

### WebBrowser issue (add infinite progress in web browser)

Peer components are problematic with things like overlays on top

[Read on stackoverflow…​](http://stackoverflow.com/questions/40572673/webbrowser-issue-add-infinite-progress-in-web-browser)

### Side menu transition speed

This can be controlled via theme constants although more often than not when people complain about this it means  
the UI has some performance issues

[Read on stackoverflow…​](http://stackoverflow.com/questions/40572457/side-menu-transition-speed)

### Google Maps on iOS error: is depending on legacy on-demand authorization, which is not supported for new apps

You need to define the iOS location string so positioning will work correctly

[Read on stackoverflow…​](http://stackoverflow.com/questions/40571870/google-maps-on-ios-error-is-depending-on-legacy-on-demand-authorization-which)

### Downloading a pdf file of larger size(like 30Mb) fails

Thre is probably a redirect involved here…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40570745/downloading-a-pdf-file-of-larger-sizelike-30mb-fails)

### Is it possible with Codename One to display the camera output in real time?

Yes and no, at this time this requires native interfaces but it should be very possible

[Read on stackoverflow…​](http://stackoverflow.com/questions/40570438/is-it-possible-with-codename-one-to-display-the-camera-output-in-real-time)

### CodenameOne GoogleConnect success callback is not triggered on Android

This seems to be a regression with the API level 23 migration

[Read on stackoverflow…​](http://stackoverflow.com/questions/40567146/codenameone-googleconnect-success-callback-is-not-triggered-on-android)

### send multiple binary files to server

This is strait forward

[Read on stackoverflow…​](http://stackoverflow.com/questions/40562569/send-multiple-binary-files-to-server-codename-one)

### Removing TitleArea from a specific

There are some artifacts that require some work to remove/change

[Read on stackoverflow…​](http://stackoverflow.com/questions/40558168/removing-titlearea-from-a-specific-codenameone-form)

### codenameone POST request BODY

Both should be completely interchangeable in a seamless way

[Read on stackoverflow…​](http://stackoverflow.com/questions/40557730/codenameone-post-request-body)

### Display entire json response message not just a single node

The answer just requires the removal of a single line of code…​.

[Read on stackoverflow…​](http://stackoverflow.com/questions/40556515/codenameone-display-entire-json-response-message-not-just-a-single-node)

### To show that the new update is available in app store

I hope to blog about a simple method of doing this next week as we’d like to do this to some of our more elaborate deployment scenarios

[Read on stackoverflow…​](http://stackoverflow.com/questions/40547234/to-show-that-the-new-update-is-available-in-app-store)

### Back from a form to a particular tab – codenameone

This should be pretty seamless but often things don’t work as expected in the old GUI builder. In the new one this  
just wouldn’t happen…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40547067/back-from-a-form-to-a-particular-tab-codenameone)

### Codename one uwp build failed

We introduced native interfaces support for UWP which broke some older cn1libs

[Read on stackoverflow…​](http://stackoverflow.com/questions/40546461/codename-one-uwp-build-failed)

### Portrait images are rotated when displayed

This is something we need to address…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40541204/portrait-images-are-rotated-when-displayed)

### Implementing a fixed size virtual UI card

We have such a UI in serveral demos most notably the kitchen sink

[Read on stackoverflow…​](http://stackoverflow.com/questions/40540513/implementing-a-fixed-size-virtual-ui-card)

### How to call codenameone java class method from javascript?

The JavaScript package goes a long way to help with such interaction

[Read on stackoverflow…​](http://stackoverflow.com/questions/40529849/how-to-call-codenameone-java-class-method-from-javascript)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
