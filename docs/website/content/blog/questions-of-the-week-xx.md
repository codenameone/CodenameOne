---
title: Questions of the Week XX
slug: questions-of-the-week-xx
url: /blog/questions-of-the-week-xx/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xx.html
aliases:
- /blog/questions-of-the-week-xx.html
date: '2016-08-25'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xx/qanda-friday.jpg)

We will make another attempt to migrate to the [new xcode 7.x build servers](/blog/xcode-migration-take-2.html) this Sunday. This might introduce some disruptions to your iOS builds but those should be fixable.

One of the stackoverflow discussions below sparked an [interesting group discussion](https://groups.google.com/d/msg/codenameone-discussions/baW-85V9d08/zItzsmmeDAAJ) about improving the UI design of a Codename One app. If you feel your app could use a design/UX improvement feel free to post it to the forum. We are no substitute to a proper UX/UI designer but having worked on a lot of apps we might be able to help a bit with some feedback/ideas.

Our updated release from this week contains some bug fixes but nothing groundbreaking.

Onwards to the questions on stackoverflow:

### Rounded URLImage not displaying

It would be great if we could find a way to workaround this so it would throw an exception.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39127298/codename-one-rounded-urlimage-not-displaying)

### Get current Date without time on CN1

Date/time is painful. I wish we did have JSR310, ideally we’ll port it to Codename One at some point.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39126567/get-current-date-without-time-on-cn1)

### Virtual keyboard not getting displayed in android device

You can’t really customize virtual keyboards on native devices unless you replace them completely which isn’t very portable…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39124530/codenameone-virtual-keyboard-not-getting-displayed-in-android-device)

### Handle clicks on Image in ImageViewer (Codenameone)

We don’t provide an action listener on the `ImageViewer` because it is not a button…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39116109/handle-clicks-on-image-in-imageviewer-codenameone)

### Validator not highlighting required fields

Validator appends `Invalid` to the end of the UIID’s. Make sure to define the `*Invalid` UIID in the theme.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39108862/codenameone-validator-not-highlighting-required-fields)

### Facebook login not being fired on Android

This was a regression related to the intent issue mentioned below

[Read on stackoverflow…​](http://stackoverflow.com/questions/39090161/codename-one-facebook-login-not-being-fired-on-android)

### Background fetch not firing on ios

Features related to background execution are sensitive so it’s important to verify that everything in the iOS `stop()` method is correct…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39089139/codename-one-background-fetch-not-firing-on-ios)

### Are there “helpers” in Codename One to write Material Design apps as in Android

There are several classes that provide material design look in Codename One but the most important thing to do is understand the goals

[Read on stackoverflow…​](http://stackoverflow.com/questions/39087345/are-there-helpers-in-codename-one-to-write-material-design-apps-as-in-android)

### Are there “helpers” in Codename One to write Material Design apps as in Android

### Apple Developer and AppStore Cert Not generated in codename one

There was a certificate generation regression this week due to changes made on Apples website. We later resolved those and this should be working again…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39077825/apple-developer-and-appstore-cert-not-genrated-in-codename-one)

### Scrollbars recently appeared

I’m not sure why this workaround might be necessary

[Read on stackoverflow…​](http://stackoverflow.com/questions/39072866/scrollbars-recently-appeared)

### Remove sidemenubar from screen(form)

`SideMenuBar.closeCurrentMenu()` is sometimes unintuitive…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39072529/codenameoneremove-sidemenubar-from-screenform)

### Limit size of an image (up to 1 MB) while uploading

You can’t do that. The only thing you can do is save, check size rinse repeat…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39071491/limit-size-of-an-image-up-to-1-mb-while-uploading)

### How to post JSON to a REST webservice in codenameone

The `postRequest()` method name is confusing…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39063909/how-to-post-json-to-a-rest-webservice-in-codenameone)

### Will Codename one application work with Appium?

Probably not. But we’d like to.  
If you are an enterprise developer who would like to see this feature please contact us…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39054799/will-codename-one-application-work-with-appium)

### How to integrate ZXing source lib into my project so i not need to install ZXING App

This was asked quite a few times and eventually Nick from [littlemonkey](http://www.littlemonkey.co.nz/) got fed up with it and built his own cn1lib…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39052849/codename-one-how-to-integrate-zxing-source-lib-into-my-project-so-i-not-need-t)

### Does Codename one support Split Pane UI component?

Not but it should be reasonably easy to build

[Read on stackoverflow…​](http://stackoverflow.com/questions/39051718/does-codename-one-support-split-pane-ui-component)

### Setting values to each comboBox item

This question demonstrates exactly why we are moving away from `List`. Understanding that a model represents the logical value is pretty hard for developers…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39051527/setting-values-to-each-combobox-item)

### How to get unique identification number of iphone

iOS is generally resistant to such information, we have the UDID but since apps may depend on it we can’t provide better options today.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39037814/how-to-get-unique-identifiacation-number-of-iphone)

### IntentResultListener not being called

We had some issues with monitoring intents on Android that should be resolved now. This also impacted QR scanning and other issues

[Read on stackoverflow…​](http://stackoverflow.com/questions/39019635/codenameone-intentresultlistener-not-being-called)

### Qrcode (zxing) scan successful but not returning to main form

QR scanning had some issues on Android due to intent changes

[Read on stackoverflow…​](http://stackoverflow.com/questions/39015449/codename-one-qrcode-zxing-scan-successful-but-not-returning-to-main-form)

### CodeScanner not working

This is the same question as the last one…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39047443/cn1-codescanner-not-working)

### Barcode scanning broken

Because we shifted from regular scanning to the cn1lib etc. things might have broken in between. They work well in the latest version

[Read on stackoverflow…​](http://stackoverflow.com/questions/39070080/barcode-scanning-broken)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
