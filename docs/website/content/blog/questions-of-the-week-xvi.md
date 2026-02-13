---
title: Questions of the Week XVI
slug: questions-of-the-week-xvi
url: /blog/questions-of-the-week-xvi/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xvi.html
aliases:
- /blog/questions-of-the-week-xvi.html
date: '2016-07-28'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xvi/qanda-friday.jpg)

We are deep within the 3.5 code freeze and this adds up to a remarkably busy week, we’ve got an even  
busier week with the release itself. Because of the code freeze there is no need for a Friday release today  
so we are skipping it this week.

So lets get right down to the questions from stackoverflow…​

### CodenameOne ImageViewer blocks scroll

Scrolling and `ImageViewer` don’t mix as the scrolling might collide with the panning/swiping.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38626396/codenameone-imageviewer-blocks-scroll)

### How to automatically reload a screen(form) after a given period of inactivity

Chen recommended using the `UITimer`, there are quite a few other approaches but this is probably the simplest…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38616952/how-to-automatically-reload-a-screenform-after-a-given-period-of-inactivity-in)

### Send HTTP query with verb different from GET / POST

We should possibly make these more intuitive for common REST commands…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38612622/send-http-query-with-verb-different-from-get-post-in-codename-one)

### How to detect device back button event

`setBackCommand` is a really complex API that behaves differently in many situations…​.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38608611/how-to-detect-device-back-button-event-in-codenameone)

### how to insert break line for long text on button in codenameone using gui builder

You can use the `SpanButton` class.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38606369/how-to-insert-break-line-for-long-text-on-button-in-codenameone-using-gui-builde)

===How to use Sensors in Codename one

<http://stackoverflow.com/questions/38598478/how-to-use-sensors-in-codename-oneThe> question is in relation to side swipe not so much sensors…​

### How do I scroll down a scrollable container

We have MANY scroll related methods and it can be a bit confusing sometimes.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38594709/how-do-i-scroll-down-a-scrollable-container-in-codename-one)

### Why does the android button have a different appearance than the IOS button

Steve explains the CSS answer, in general it’s because the button has a “Border” which takes precedence over color…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38592240/codename-one-why-does-the-android-button-have-a-different-appearance-than-the-i)

### Can’t Get Android Native Access

Mistyping a build hint name is often painful to detect…​.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38568447/cant-get-android-native-access)

### Keystore not generating

Generating keys/certificates for Android should be really simple so I don’t quite get that issue.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38567286/keystore-not-generating)

### How to center input text in textfields?

Centered text fields are nice for some UI designs but in terms of native editing…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38566624/how-to-center-input-text-in-textfields)

### App update Notifications in Codename One

Generally push is the best approach to provide update notifications but notifications and toast can also work well…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38559706/app-update-notifications-in-codename-one)

### Dynamic AutoComplete

Luckily our demo shows exactly how this functionality is supposed to work…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38558118/dynamic-autocomplete)

### Hello world application build fail (CodenameOne in Eclipse Juno)

Apparently Eclipse Juno has issues with Java 8 support, not being an Eclipse guy I might have gotten this wrong…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38552510/hello-world-application-build-fail-codenameone-in-eclipse-juno)

### Is hint like feature available in pickers?

No, however there are other ways to work/indicate with pickers

[Read on stackoverflow…​](http://stackoverflow.com/questions/38548851/is-hint-like-feature-available-in-pickers)

### parse4cn1 callback usage example

It’s good to see developers still picking parse4cn1 in the “post parse” era…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38542197/parse4cn1-callback-usage-example)

### Date comparison fails d1.compareTo(d2)

`compareTo` isn’t available in `Date` in this particular case the substitution is relatively simple though…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38536024/date-comparison-fails-d1-comparetod2)

### CodenameOne MapContainer Zoom Level

Maybe we should deprecate MapComponent as so many developers get it wrong…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38532009/codenameone-mapcontainer-zoom-level)

### Data not block size aligned in codenameone BouncyCastle (No Padding)

Don’t use `String` for binary data, this is a source of huge/painful bugs. It works on Java SE but then you run into  
really complex edge cases on the devices…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38526228/data-not-block-size-aligned-in-codenameone-bouncycastle-no-padding)

### How to make circle shape buttons in codenameone with GUI builder

We really need to make it clear to developers that 9-piece borders can’t be used with circles…​ Maybe an AI of sort…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38524015/how-to-make-circle-shape-buttons-in-codenameone-with-gui-builder)

### Notification service Codename One

This isn’t so much about notification as much as about the location API but it’s really unclear from the question…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38518839/notification-service-codename-one)

### “Bluetooth failes to initialize” when automating Bluetooth steps

One of the drawbacks of picking up the pre-existing BluetoothLE support is that we get this support “as is” and  
only enhanced it slightly.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38508966/bluetooth-failes-to-initialize-when-automating-bluetooth-steps)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
