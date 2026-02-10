---
title: Questions of the Week XXIX
slug: questions-of-the-week-xxix
url: /blog/questions-of-the-week-xxix/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xxix.html
aliases:
- /blog/questions-of-the-week-xxix.html
date: '2016-10-27'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xxix/qanda-friday2.jpg)

This was a busy week with a lot of work going on under the hood especially on the new GUI builder.  
We are also working on a pretty cool new demo that we’ll ideally launch next week. Our release today  
only includes the libraries making it smaller, we would probably release a plugin update next Friday as there  
are many changes that warrant an update.

On stack overflow things were as usual:

### Push notifications not registering on ios 10

We have users with iOS 10 devices so this is probably not the case

[Read on stackoverflow…​](http://stackoverflow.com/questions/40271939/codename-one-push-notifications-not-registering-on-ios-10)

### Modal Dialog does not appear

EDT violations are typically the reason for this

[Read on stackoverflow…​](http://stackoverflow.com/questions/40268968/modal-dialog-does-not-appear)

### How to create Facebook-like notification badges in CodenameOne

This is a frequent enough question it warrants a sample

[Read on stackoverflow…​](http://stackoverflow.com/questions/40256864/how-to-create-facebook-like-notification-badges-in-codenameone)

### Launching touch events programatically

These sort of things are really easy in Codename One thanks to the lightweight nature of the framework

[Read on stackoverflow…​](http://stackoverflow.com/questions/40248903/launching-touch-events-programatically)

### Center a label in the south container of a border layout in Codename One

The need for `revalidate()` on changes is often confusing to developers but it is pivotal to Codename One

[Read on stackoverflow…​](http://stackoverflow.com/questions/40244583/center-a-label-in-the-south-container-of-a-border-layout-in-codename-one)

### Unable to use jar

When using a native jar code completion won’t work but it should be there…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40227815/codename-one-unable-to-use-jar)

### How to integrate Other Ads network on codename one app

The best place to start is to look at sample cn1libs that we already integrated

[Read on stackoverflow…​](http://stackoverflow.com/questions/40223452/how-to-integrate-other-ads-network-on-codename-one-app)

### Which app store?

We don’t provide an API to return the source appstore URL at this time, it’s not quite portable as far as I can tell

[Read on stackoverflow…​](http://stackoverflow.com/questions/40208229/which-app-store)

### URLImage fetch method not working

We clarified the JavaDocs for the fetch method to make it clear that this isn’t the way it would behave

[Read on stackoverflow…​](http://stackoverflow.com/questions/40198257/urlimage-fetch-method-not-working)

### Trouble reading from Storage

There are posts where you are given a lot of information and are missing just the one tiny bit that would help you  
answer…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40193347/trouble-reading-from-storage)

### How to insert data in codename one db using prepared statement?

That’s an interesting behavior of the sqlite implementation

[Read on stackoverflow…​](http://stackoverflow.com/questions/40189644/how-to-insert-data-in-codename-one-db-using-prepared-statement)

### After latest update the java file from the gui do strange things

We had a regression in the GUI builder that shipped with the 3.5.5 plugin which is why we had to push an update  
shortly after

[Read on stackoverflow…​](http://stackoverflow.com/questions/40186919/after-latest-update-the-java-file-from-the-gui-do-strange-things)

### GoogleAds Does’t show up

This frustrating issue is caused by the https restrictions of iOS

[Read on stackoverflow…​](http://stackoverflow.com/questions/40179299/googleads-doest-show-up)

### Storage throwing EOF & NullPointer Exception

There are specific API’s that are substituted by the externalization code

[Read on stackoverflow…​](http://stackoverflow.com/questions/40177594/codename-one-storage-throwing-eof-nullpointer-exception)

### Codename one expect feature on DateTime Component

The picker is problematic as it is a native widget, we can’t offer anything that isn’t offered by the native OS

[Read on stackoverflow…​](http://stackoverflow.com/questions/40173859/codename-one-expect-feature-on-datetime-component)

### How to add text to the textfield inside dataChangedListener in codename one?

I am using TextField and on addDataChangedListener I am trying to add some character to the text field if length of the text field is 2 but it is not working for me.can you please help me how to …​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40172061/how-to-add-text-to-the-textfield-inside-datachangedlistener-in-codename-one)

### Workaround for applying a mask to an image in Codename One

There are some complex image modes in Codename One but most of them are convertable to standard raster images

[Read on stackoverflow…​](http://stackoverflow.com/questions/40163082/workaround-for-applying-a-mask-to-an-image-in-codename-one)

### How do I restore a deleted multi-image

We generally recommend keeping the res directory in source control so this dosen’t become an issue

[Read on stackoverflow…​](http://stackoverflow.com/questions/40162111/how-do-i-restore-a-deleted-multi-image)

### FloatingActionButton Sub can’t call another form in Codename One

This should have been serialized with the dialog disposal code, it should be fixed now

[Read on stackoverflow…​](http://stackoverflow.com/questions/40151304/floatingactionbutton-sub-cant-call-another-form-in-codename-one)

### How to get app version (Play / iTunes) stores using Codename one

We don’t know the itunes version but we can give you the Codename One version of the app.

[Read on stackoverflow…​](http://stackoverflow.com/questions/40149737/how-to-get-app-version-play-itunes-stores-using-codename-one)

### How to resolve null when using webservices in codename one

This is a bit of a long thread, kudos to Diamond for taking the time on this

[Read on stackoverflow…​](http://stackoverflow.com/questions/40146488/how-to-resolve-null-when-using-webservices-in-codename-one)

### Back navigation does not stop youtube video in WebBrowser

This is turning into a FAQ, the `destroy()` method closes the browser completely

[Read on stackoverflow…​](http://stackoverflow.com/questions/40142655/back-navigation-does-not-stop-youtube-video-in-webbrowser)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
