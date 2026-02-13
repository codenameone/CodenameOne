---
title: Questions of the Week XXI
slug: questions-of-the-week-xxi
url: /blog/questions-of-the-week-xxi/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xxi.html
aliases:
- /blog/questions-of-the-week-xxi.html
date: '2016-09-01'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xxi/qanda-friday.jpg)

August is finally over and we can get back to our more usual brisk pace of progress!  
The xcode migration which was one of the biggest pains to go thru is also mostly behind us and we can now  
turn our gaze to improving Codename One and its general usage experience.

This week was mostly uneventful in terms of stack overflow, the biggest news is the migration on Android to API  
level 23 which might include some issues.

### Codename one concurrent animations?

There are many things we can do with animations thanks to the new animation manager!

[Read on stackoverflow…​](http://stackoverflow.com/questions/39266547/codename-one-concurrent-animations)

### When call up from Background my Codename App Shows bug

Mobile app lifecycle has always been a difficult subject. I think we simplified this but it’s still non-trivial especially  
with suspend/resume.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39261420/when-call-up-from-background-my-codename-app-shows-bug)

### Online database Connection

Connecting to a database thru the mobile network is a problematic idea

[Read on stackoverflow…​](http://stackoverflow.com/questions/39254383/codename-one-online-database-connection)

### How to validate form in codeonename

People often confuse the new/old GUI builder and try to create a form on the old one in a non-GUI builder project

[Read on stackoverflow…​](http://stackoverflow.com/questions/39250756/how-to-validate-form-in-codeonename)

### Generating certificate for iOS using codename one certificate wizard

Yes you need to pay Apple to build native apps for a device you purchased. This is true even when working natively  
although later versions of xcode allow you to circumvent that slightly.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39248820/generating-certificate-for-ios-using-codename-one-certificate-wizard)

### “Unrecognized Selector Sent to Instance” error in iOS native code

Two really confusing things for developers new to Objective-C that had me stumped too:

  * Argument names are a part of the method signature. If you change the name of the argument you break the code!

  * The compiler doesn’t check that a method (message) exists. This is common in scripting languages but odd for  
a compiled language

[Read on stackoverflow…​](http://stackoverflow.com/questions/39220219/unrecognized-selector-sent-to-instance-error-in-ios-native-code)

### Error=13, Permission denied when adding event in CodenameOne

The old designer is bound to the IDE in a problematic way so if more than one IDE instance exists or if the IDE  
installation isn’t standard things can get “weird”. This only exists in NetBeans as we found more elegant  
ways of addressing this binding in other IDE’s.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39212609/error-13-permission-denied-when-adding-event-in-codenameone)

### What are the exact steps to deploy application on Windows Phone

Those are listed in the developer guide for UWP, Windows Phone port is phasing out

[Read on stackoverflow…​](http://stackoverflow.com/questions/39205818/what-are-the-exact-steps-to-deploy-application-on-windows-phone)

### How to customize the look of a Tab for Android in codename one?

The need to override the border UIID element is often confusing.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39203294/how-to-customize-the-look-of-a-tab-for-android-in-codename-one)

### How to remove commands added to overflow menu

This was missing from the API, we added a new API to remove overflow commands explicitly

[Read on stackoverflow…​](http://stackoverflow.com/questions/39200432/how-to-remove-commands-added-to-overflow-menu)

### Need to set java8 in codenameone project with IDE Myeclipse 2015

Java 8 support is painful under Eclipse where the ini needs to be edited

[Read on stackoverflow…​](http://stackoverflow.com/questions/39200158/need-to-set-java8-in-codenameone-project-with-ide-myeclipse-2015)

### UWP app name can’t chinese. name change to?

There was an issue with setting UWP application names to some locales this should be fixed in todays builds

[Read on stackoverflow…​](http://stackoverflow.com/questions/39182370/codename-one-uwp-app-app-name-cant-chinese-name-change-to)

### Google Play Warning: SSL Error Handler Vulnerability

I’m not sure why this error message was received. If other users got it we’d like to know as we didn’t get it for our  
submissions. The code mentioned in the message doesn’t exist in our workspace so maybe it’s related to a cn1lib?

[Read on stackoverflow…​](http://stackoverflow.com/questions/39177995/codenameone-google-play-warning-ssl-error-handler-vulnerability)

### Specify absolute position of GUI element in CN1

I’m still not sure I understand the question correctly, I try to communicate more clearly but sometimes wires don’t  
cross properly and the original intention of the question gets lost

[Read on stackoverflow…​](http://stackoverflow.com/questions/39173876/specify-absolute-position-of-gui-element-in-cn1)

### How to get mobile info in codenameone?

This isn’t so much a Codename One question, it’s a mobile question. The answer is easy on iOS: you can’t.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39167856/how-to-get-mobile-info-in-codenameone)

### Constraints (West, center…) not visible in new GUI Builder

The UX of the new GUI builder still needs some work. The biggest thing we need to fix is having the tree always  
open so developers can see the selected component.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39165967/codename-one-constraints-west-center-not-visible-in-new-gui-builder)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
