---
title: Questions of the Week XXII
slug: questions-of-the-week-xxii
url: /blog/questions-of-the-week-xxii/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xxii.html
aliases:
- /blog/questions-of-the-week-xxii.html
date: '2016-09-08'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xxii/qanda-friday.jpg)

We made a lot of changes to Codename One over the past week but eventually decided to postpone the plugin  
update to next week so we can do more work on the GUI builder. We have quite a few new features and fixes lined  
up for next week already.

On stackoverflow things were as usual:

### how to use phone contacts instead of gmail contacts for social app in codenameone

Getting device contacts is actually easier than getting gmail contacts

[Read on stackoverflow…​](http://stackoverflow.com/questions/39385647/how-to-use-phone-contacts-instead-of-gmail-contacts-for-social-app-in-codenameon)

### New GUI builder from CodeNameOne introduces exception?

There seems to be a problem in the GUI builder IDE integration, this should be fixed in the next plugin update

[Read on stackoverflow…​](http://stackoverflow.com/questions/39374361/new-gui-builder-from-codenameone-introduces-exception)

### Save a photo with a specific name and compressed size in codenameone

You can select the size in megabytes in advance but you can monitor the size and control it via resolution change

[Read on stackoverflow…​](http://stackoverflow.com/questions/39365968/save-a-photo-with-a-specific-name-and-compressed-size-in-codenameone#39366991)

### Java 8 and Java time

Codename One supports a subset of Java 8 with subset being an important keyword here…​ I’d like to explain this  
into greater details, perhapse with a blog post

[Read on stackoverflow…​](http://stackoverflow.com/questions/39362030/java-8-and-java-time)

### Convert time to utc

Timezones are a pain no matter which technology you use, they are just awful

[Read on stackoverflow…​](http://stackoverflow.com/questions/39360951/codenameone-convert-time-to-utc)

### Install Error for iOS

Ideally we’ll mirror and eventually remove the appspot servers completely

[Read on stackoverflow…​](http://stackoverflow.com/questions/39356554/codenameone-install-error-for-ios)

### iOS Build Server Issue

With the update coming out today we should have more logging that will allow tracking down these issues

[Read on stackoverflow…​](http://stackoverflow.com/questions/39348741/ios-build-server-issue)

### Reflection not working in ios in codename one

Dynamic class loading “might” work but it isn’t reliable across platforms and might run into issues with obfuscation  
or optimizers. You should use class literals instead

[Read on stackoverflow…​](http://stackoverflow.com/questions/39343822/reflection-not-working-in-ios-in-codename-one)

### Codename One and jformdesigner

Some AWT/Swing GUI builder code can be copied to Codename One “as is” with package names and a few class  
name changes

[Read on stackoverflow…​](http://stackoverflow.com/questions/39339559/codenameone-and-jformdesigner)

### Sidemenu customization

We don’t currently support tinting the form on which the side menu is overlaid

[Read on stackoverflow…​](http://stackoverflow.com/questions/39338487/sidemenu-customization)

### How to change the background of an SVG in Codename One

Codename One doesn’t **really** support SVG’s. We convert them to PNG when importing them to the IDE.  
We might improve on that in the future but the demand isn’t great…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39327110/how-to-change-the-background-of-an-svg-in-codename-one)

### How to kill web browser instance after close form

You can explicitly call `destroy()` but often you need to sign out first

[Read on stackoverflow…​](http://stackoverflow.com/questions/39326383/codename-one-how-to-kill-web-browser-instance-after-close-form)

### Java Scanner – Mobile App Build Error

Sometimes in the attempts to workaround/fix a problem you create a bigger problem.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39324956/codename-one-java-scanner-mobile-app-build-error)

### how to show gmail contact in android after signin with google in codenaoneone

You can access the Google+ contacts as well as the on-device contacts

[Read on stackoverflow…​](http://stackoverflow.com/questions/39318354/how-to-show-gmail-contact-in-android-after-signin-with-google-in-codenaoneone)

### Invoke codename code from native interface

There are several tricks to do this listed in the developer guide…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39305641/codename-one-invoke-codename-code-from-native-interface)

### AdMob ads not showing in iOS

This isn’t the first time banner ads posed a problem, we made a mistake when we integrated them into the build  
flow and should have integrated them as a cn1lib

[Read on stackoverflow…​](http://stackoverflow.com/questions/39294975/admob-ads-not-showing-in-ios)

### showForm – java.lang.reflect.InvocationTargetException

The error message from the simulator is somewhat unintuitive sometimes as it is wrapped in the simulator code.  
The actual exception was the `NullPointerException` below the `InvocationTargetException`

[Read on stackoverflow…​](http://stackoverflow.com/questions/39292658/showform-java-lang-reflect-invocationtargetexception)

### Codenameone plugin giving null pointer exception

Handling the case where the `.gui` file is missing or in a different location is a bit problematic in the various IDE’s.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39290307/codenameone-plugin-giving-null-pointer-exception)

### Graphics drawing code generates blank images, only on iOS

This is covered in an issue submission so we need to evaluate this thru the issue

[Read on stackoverflow…​](http://stackoverflow.com/questions/39283163/graphics-drawing-code-generates-blank-images-only-on-ios)

### Getting line numbers in stack traces

We use the Android VM as-is so the stack trace lines are pretty much what Android gives us

[Read on stackoverflow…​](http://stackoverflow.com/questions/39280499/getting-line-numbers-in-stack-traces)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
