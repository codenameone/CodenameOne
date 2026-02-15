---
title: Questions of the Week XVIII
slug: questions-of-the-week-xviii
url: /blog/questions-of-the-week-xviii/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xviii.html
aliases:
- /blog/questions-of-the-week-xviii.html
date: '2016-08-11'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xviii/qanda-friday.jpg)

This has been a very busy week with huge changes and new features to the code base. Because of that we will  
shift the Friday release this week to Sunday so we’ll be able to revert the deployment if build issues occur without  
affecting too many people.

Besides the big changes and new plugin versions we’re also including a major overhaul of the Android background  
processes that should improve the behavior of Codename One apps for background tasks. Notice that this is a pretty  
huge change that might include regressions so be sure to report them as soon as possible.

Onwards to the questions on stackoverflow:

### Using gui builder and css

This is really a question about layering themes from two different sources

[Read on stackoverflow…​](http://stackoverflow.com/questions/38895190/codename-one-using-gui-builder-and-css)

### NativeInterface – ClassNotFoundException on NativeLookup

Native interfaces are sometimes missing the JavaSE impl directory in some cases. This shouldn’t pose a problem though.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38878031/codenameone-nativeinterface-classnotfoundexception-on-nativelookup)

### Scheduling local notification for a specific date and repeat yearly

We don’t support more complex/long term repeat scheduling because of the complexity with scheduling such  
repeat activities in a portable way.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38836749/scheduling-local-notification-for-a-specific-date-and-repeat-yearly)

### Codename One Pubnub alternatives CN1lib

I really like PubNub and don’t really see the need for an alternative. One of the nice things about PubNub is that  
the client libraries are open source.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38835334/codename-one-pubnub-alternatives-cn1lib)

### Get all file names in src folder, codenameone project

“Files” in the source directory aren’t really “files” in the typical sense. Sometimes they can be a part of an APK  
which isn’t exactly like a jar. Other times they can be a part of an app bundle which is a bit more complex with some  
odd restrictions…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38832485/get-all-file-names-in-src-folder-codenameone-project)

### Query regarding to suspended mode

Suspend/resume behavior is pretty tricky on mobile devices. We tried to simplify it as much as reasonably possible.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38822394/codenameone-query-regarding-to-suspended-mode)

### Write/Read the image from fileSystemStorage and readjust it before saving

Manipulating and resizing images is generally pretty simple but there are edge cases that are pretty hard to deal with.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38819891/write-read-the-image-from-filesystemstorage-and-readjust-it-before-saving)

### Google Maps – some markers not appearing

Turns out the longitude and latitude values were reversed.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38817608/codenameone-google-maps-some-markers-not-appearing)

### Where exactly are dialog text, OK button, and Cancel buttons defined or inherited from in CodenameOne?

When asking a question it’s best to state what you are trying to do. Sometimes the answer is that you are asking  
the wrong question…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38809474/where-exactly-are-dialog-text-ok-button-and-cancel-buttons-defined-or-inherite)

### Resize the image before uploading images from openGallery method

`ImageIO` allows you to resize an image to arbitrary sizes on the filesystem using low level system API’s without  
physically opening an image file.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38804948/resize-the-image-before-uploading-images-from-opengallery-method)

### How to check internet connection and suspended mode

Internet connectivity is tricky since we might be connected to a router but it might not have further connectivity…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38804543/codenameone-how-to-check-internet-connection-and-suspended-mode)

### Alert Dialog message

You can customize any component within Codename One’s UI

[Read on stackoverflow…​](http://stackoverflow.com/questions/38802224/codenameone-alert-dialog-message)

### How to make a Line graph in cn1

The charts API in Codename One is pretty darn complex and the demo doesn’t do enough to simplify that, ideally  
we’ll make simpler chart demos that would be easier to work with. I’d also like a simpler API that would only  
encapsulate the more common use cases.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38794921/how-to-make-a-line-graph-in-cn1)

### We want to show the exact seconds of system on each seconds

This question should probably have been tagged Java more than Codename One.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38792823/codenameonewe-want-to-show-the-exact-seconds-of-system-on-each-seconds)

### Display date-time details on an image in codenameone

Mutable images provide a lot of flexibility to Codename One UI’s

[Read on stackoverflow…​](http://stackoverflow.com/questions/38788013/display-date-time-details-on-an-image-in-codenameone)

### Background fetch in IOS not working

On iOS you’ll have less control over background fetch than you do on Android

[Read on stackoverflow…​](http://stackoverflow.com/questions/38784064/background-fetch-in-ios-not-working)

### How to release CODENAMEONE form including all it’s components while navigating one page to other?

You don’t release forms. Once they have no more references they get GC’d however even one component in RAM  
can lead back to the parent form so you need to track the references you keep.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38783287/how-to-release-codenameone-form-including-all-its-components-while-navigating-o)

### How can I programmatically ‘set’ all parameters of a Dialog, such as ‘dialog title’, ‘ok text’, etc., without having string literals as parameters?

Most people use the static `Dialog` methods without realizing you can create an instance of the `Dialog` class &  
populate it with arbitrary data.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38778943/how-can-i-programmatically-set-all-parameters-of-a-dialog-such-as-dialog-tit)

### How to change the style of a button on release outside of button boundaries

The title is misleading, the real issue relates to the different style objects for component states.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38776739/how-to-change-the-style-of-a-button-on-release-outside-of-button-boundaries)

### How to handle a button pressed when it’s in a list cell

The short answer is “don’t”. Use a `Container` as we explain in the [List JavaDocs](/javadoc/com/codename1/ui/List/)…​d

[Read on stackoverflow…​](http://stackoverflow.com/questions/38773910/how-to-handle-a-button-pressed-when-its-in-a-list-cell)

### CodeName One – Image gets pixelated when scaling

Getting smooth images that adapt to the various devices is always challenging. Ideally you want to scale as little  
as possible on the device and as few times as possible.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38773301/codename-one-image-gets-pixelated-when-scaling)

### Validator for ComboBox and phone number

You can add an arbitrary constraint to anything in the Codename One validation framework

[Read on stackoverflow…​](http://stackoverflow.com/questions/38772987/validator-for-combobox-and-phn-no)

### My CodeNameOne-App stoped to work in HTTPS

Server certificates for https are always a pain, usually it’s a one time thing though.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38757168/my-codenameone-app-stoped-to-work-in-https)

### LocalNotifications Issues in Android tablets and iPad devices

On iOS local notifications only get delivered when the app is in the background…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38760356/localnotifications-issues-in-android-tablets-and-ipad-devices)

### Non-persistence of class variables in StateMachine in CodenameOne

Yet another example demonstrating why we are eager to move to the new GUI builder `initVars()` is a problematic  
hack that confuses many…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38762175/non-persistence-of-class-variables-in-statemachine-in-codenameone)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
