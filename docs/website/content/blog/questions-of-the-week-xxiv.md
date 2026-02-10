---
title: Questions of the Week XXIV
slug: questions-of-the-week-xxiv
url: /blog/questions-of-the-week-xxiv/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xxiv.html
aliases:
- /blog/questions-of-the-week-xxiv.html
date: '2016-09-22'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xxiv/qanda-friday.jpg)

We’ve had a very challenging week with a lot of backend work for some enterprise customers so we didn’t get as  
much done as we do most weeks. I can say that I’ve seen some of the apps in the pipeline and I’m very excited  
about the things to come…​

Todays update includes many bug fixes on Android, UWP but not much in terms of new features.

On stack overflow things were pretty standard:

### CodenameOne set indexing of fields for virtual keyboard

The order of the fields is determined by the focus and that can be manipulated using `setNextFocusDown`

[Read on stackoverflow…​](http://stackoverflow.com/questions/39636049/codenameone-scrolling-issue-with-keyboard)

### Login form is shown for a couple of second in iOS

The iOS section in the developer guide explains the iOS splash screen system in-depth

[Read on stackoverflow…​](http://stackoverflow.com/questions/39621816/login-form-is-shown-for-a-couple-of-second-in-ios)

### How to show side menu icon in particular forms only

Just don’t add commands into that form that go into the side menu. That’s pretty easy to do with the `Toolbar` class

[Read on stackoverflow…​](http://stackoverflow.com/questions/39621681/how-to-show-side-menu-icon-in-particular-forms-only)

### Codenameone Detect keyboard showing

Detecting a keyboard isn’t good practice since the behavior differs a lot between iOS/Android etc.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39619899/codenameone-detect-keyboard-showing)

### Hiding element while preserving its space in page layout

`setVisible` hides elements like the title mentioned but in his case he was asking about `setEnabled`

[Read on stackoverflow…​](http://stackoverflow.com/questions/39610496/hiding-element-while-preserving-its-space-in-page-layout)

### Codename One class file for PrintWriter not found

The webservice wizard is a bit confusing, it’s important to generate the server code to a server project

[Read on stackoverflow…​](http://stackoverflow.com/questions/39604919/codename-one-class-file-for-printwriter-not-found)

### Is it possible to arrange commands properly on a GUI Element in iOS?

If you aren’t using `Toolbar` yet we recommend switching to it for better control

[Read on stackoverflow…​](http://stackoverflow.com/questions/39597084/is-it-possible-to-arrange-commands-properly-on-a-gui-element-in-ios)

### theme.res not found on Codenameone IntellijIDEA IDE

This was caused because he picked the Android SDK instead of the JDK when creating the project

[Read on stackoverflow…​](http://stackoverflow.com/questions/39585091/theme-res-not-found-on-codenameone-intellijidea-ide)

### Codenameone Plugin not Loaded in Android Studio

We don’t support Android Studio, Google broke a lot of things in IntelliJ to make it work for Android

[Read on stackoverflow…​](http://stackoverflow.com/questions/39584424/codenameone-plugin-not-loaded-in-android-studio)

### Auto start, cross platform, background mobile web service with codename one or cordova

There are limitations of mobile native OS’s at play and you hit them sooner than you hit the limitations of Codename One

[Read on stackoverflow…​](http://stackoverflow.com/questions/39574700/auto-start-cross-platform-background-mobile-web-service-with-codename-one-or-c)

### How to avoid that swiping a SwipeableContainer also creates an event in the top container?

This includes some “black magic” to get this working…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39558166/how-to-avoid-that-swiping-a-swipeablecontainer-also-creates-an-event-in-the-top)

### Can I use an SVG image as an button icon?

We don’t really support SVG, ideally we’ll improve that but native platforms don’t support it either…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39552432/can-i-use-an-svg-image-as-an-button-icon)

### Recommended way to change the size of the Toolbar in Codename One

We rely heavily on the fact that `Container` is transparent and has 0 padding/margin

[Read on stackoverflow…​](http://stackoverflow.com/questions/39545709/recommended-way-to-change-the-size-of-the-toolbar-in-codename-one)

### Change CodenameOne InfiniteContainer PullToRefresh Behaviour

You can override the refresh method in that class

[Read on stackoverflow…​](http://stackoverflow.com/questions/39535879/change-codenameone-infinitecontainer-pulltorefresh-behaviour)

### Date Time Picker on double tap display typing Feature

We recommend using separate date picker & time picker. You should avoid the Date & Time picker as that is a  
feature that is iOS specific.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39532337/date-time-picker-on-double-tap-display-typing-feature)

### GUI builder does not show up

Eclipse has slightly slower update cycles than NetBeans/IntelliJ at this time. Normally that isn’t a problem but  
with a fast evolving tool like the GUI builder it might be a bit of a hindrance.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39525787/gui-builder-does-not-show-up)

### Apps Error no virtual method

This was a result of using a pre-release version of the API as we made minor adjustments to the method signatures

[Read on stackoverflow…​](http://stackoverflow.com/questions/39525646/codenameone-apps-error-no-virtual-method)

### Ticker mode

You can enable tickering for any component that derives label which includes buttons etc.

[Read on stackoverflow…​](http://stackoverflow.com/questions/39519089/ticker-mode-in-codename-one)

### Size of URLImage as a Label icon

`URLImage` resizes and caches the data based on placeholder size and settings

[Read on stackoverflow…​](http://stackoverflow.com/questions/39517531/size-of-urlimage-as-a-label-icon)

### connection and Toastbar not displaying issue

`ToastBar` is bound to a form so if you are on a different form this might be a problem

[Read on stackoverflow…​](http://stackoverflow.com/questions/39512651/connection-and-toastbar-not-displaying-issue)

### downloadUrlToStorageInBackground in ImageList model for imageViewer downloads & overrides the image every time

The download method always downloads and doesn’t check if the file is already there…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39512460/downloadurltostorageinbackground-in-imagelist-model-for-imageviewer-downloads)

### Trouble changing background color of TextField in Codename One

The requirement to define the border as empty breeds a lot of confusion

[Read on stackoverflow…​](http://stackoverflow.com/questions/39509897/trouble-changing-background-color-of-textfield-in-codename-one)

### Managing Demo / Full version of my app in Codename One

This is a common case that I was pretty sure we had documented somewhere, it should probably be in the developer  
guide somewhere

[Read on stackoverflow…​](http://stackoverflow.com/questions/39505331/managing-demo-full-version-of-my-app-in-codename-one)

### UWP SQLite CodenameOne with native interface

This was actually a pretty interesting question as it made me think on the potential approaches for a relatively complex  
missing feature

[Read on stackoverflow…​](http://stackoverflow.com/questions/39500153/uwp-sqlite-codenameone-with-native-interface)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
