---
title: Questions of the Week XIX
slug: questions-of-the-week-xix
url: /blog/questions-of-the-week-xix/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xix.html
aliases:
- /blog/questions-of-the-week-xix.html
date: '2016-08-18'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xix/qanda-friday.jpg)

It’s [cucumber season](http://ubiquity.acm.org/article.cfm?id=1088429), there is little going on around here and elsewhere. Stackoverflow mirrors this general trend with seasonal low activity. Our update to the build servers today includes a lot of fixes but is still tame compared to the last update.

Onwards to the questions on stackoverflow:

### Cannot edit new locale in Codename One IntelliJ plugin

This is a bug in the current version of the plugins. A workaround is described in the answer…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39005898/cannot-edit-new-locale-in-codename-one-intellij-plugin)

### Some numbers in the application version are not taken into account

We simplified version numbers for portability but some edge cases with floating point behavior haunted us in the past

[Read on stackoverflow…​](http://stackoverflow.com/questions/39005240/some-numbers-in-the-application-version-are-not-taken-into-account)

### Build Canceled By User on BLE example

Sometimes we accidentally commit bad `build.xml` files from local development. To workaround this we automatically cancel broken builds.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38997905/codename-one-build-canceled-by-user-on-ble-example)

### HTC Performance Issue with CodeNameOne App?

Debugging performance issues is always a pain. We noticed that a lot of times a bug or exception was incorrectly classified as a performance issue. That is why you need to provide as much measurement and information as possible when discussing performance.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38989426/htc-preformance-issue-with-codenameone-app)

### How to handle exception java.net.ConnectionException explicitly

Error handling in networking isn’t always the most intuitive thing, and although we cover it a lot it’s still confusing.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38987976/codenameone-how-to-handle-exception-java-net-connectionexception-explicitly)

### Database object was released by the GC without being closed first

iOS is sensitive to threading issues within SQL access code.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38980128/database-object-was-released-by-the-gc-without-being-closed-first)

### Move the marker to selected location on MapContainer

Events in native widgets should be handled by the native widget itself.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38974391/move-the-marker-to-selected-location-on-mapcontainer)

### Virtual keyboard ios “done” button and toolbar disappears

Some of the virtual keyboard flags are a bit unintuitive.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38970748/cn1-virtual-keyboard-ios-done-button-and-toolbar-dissappears)

### How to add an ArcProgress (lib) to the TitleBar in codename one

If you are struggling with an effect please provide code and images to clarify the issue

[Read on stackoverflow…​](http://stackoverflow.com/questions/38968051/how-to-add-an-arcprogress-lib-to-the-titlebar-in-codename-one)

### ImageViewer isn’t working fine

`URLImage` doesn’t work well with the `ImageViewer`, there are some conflicting concepts there.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38945405/imageviewer-isnot-working-fine-codenameone)

### How to get the mobile number of current sim card in real device?

This is a pretty common FAQ and the answer is always the same:

> Notice that apps like whatsapp, uber, gettaxi etc. all ask you to type in your phone number then a verification code sent via an SMS…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38933059/codenameonehow-to-get-the-mobile-number-of-current-sim-card-in-real-device)

### Difference in GoogleMaps rendering

We generally recommend people use native maps as they look much better on the device. Unless there is some restriction or requirement this is probably the best approach.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38928726/difference-in-googlemaps-rendering)

### Hamburger Icon and Overflow Menu Size

Some customizations are harder to figure out without looking at the code, in most cases documentation exists but finding it might be challenging.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38928177/hamburger-icon-and-overflow-menu-size)

### Show blank google maps

The maps situation in Codename One is a bit confusing with native maps, the lightweight maps and even HTML maps which we haven’t promoted as for most purposes the native maps would be better.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38922265/show-blank-google-maps-in-codename-one)

### CodenameOne background task for BluetoothLe connection

Background location is pretty difficult to get right in cross platform solutions.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38917060/codenameone-background-task-for-bluetoothle-connection)

### Getting Locations Best Practice

Under normal circumstances we treat location as hybrid location where available (Android/iOS). In Android there are some nuances with Google play services.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38901298/getting-locations-best-practice)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
