---
title: Questions of the Week VI
slug: questions-of-the-week-vi
url: /blog/questions-of-the-week-vi/
original_url: https://www.codenameone.com/blog/questions-of-the-week-vi.html
aliases:
- /blog/questions-of-the-week-vi.html
date: '2016-05-19'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-vi/qanda-friday.jpg)

It’s been quite a busy week with many changes and updates, some of those were in line for a couple of tools we  
plan to introduce over the next few weeks. This week I also placed an interesting thread from the discussion group.  
Normally I try to keep this focused on stackoverflow but if a good thread comes up in the discussion group  
that raises an interesting topic I think this is a good place for it too.

### The removeCommand is not working for the toolbar commands

Removing a command from the `Toolbar` requires that we revalidate the layout, that isn’t as intuitive sometimes  
but it helps when more than one command is changed.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37314017/the-removecommand-is-not-working-for-the-toolbar-commands-codenameone)

### How to verify that X509TrustManager is correctly implemented on iOS/Android

We don’t hide or change the default SSL behavior in mobile OS’s at this time, you can do it if you want to go into  
the native functionality but the default should be reasonably secure.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37328988/how-to-verify-that-x509trustmanager-is-correctly-implemented-on-ios-android-with/37337767)

### What are the differences between CodenameOne getCurrentLocation methods

This question didn’t get much of an answer as much as “for your use case you should use neither”…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37332769/what-are-the-differences-between-codenameone-getcurrentlocation-methods/37337673)

### Cross-platform Project and Techniques

Dimitar raised a point that he considered about missing platform specific details in our documentation, this isn’t  
something that’s missing in the documentation as much as a conceptual difference between Codename One and  
tools such as Xamarin/React Native etc…​

He raised some interesting points that highlight the advantages and disadvantages of our approach to portability:

[Read in the discussion group…​](https://groups.google.com/d/msg/codenameone-discussions/YcMZRxw6Z8A/MgoNcmBeDgAJ)

### Data upload works on Android phone but fails on Iphone 4

While I answered that one the submitter pretty much investigated the whole thing on his own and mostly used  
stack overflow to line up his thought process. That’s a good way to ask a question…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37304600/codename-one-data-upload-works-on-android-phone-but-fails-on-iphone-4)

### How intercept the “Cancel” and “OK” actions belonging to the native IOS picker component?

One of the problems with native widgets is that they differ so much both in subtle and obvious ways. I can’t think of  
a component that gave us more grief than the picker.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37279614/how-intercept-the-cancel-and-ok-actions-belonging-to-the-native-ios-picker-c)

### Warning paint queue size exceeded, please watch the amount of repaint calls Error

This is a warning call we wrote when we coded LWUIT back in 2007. It was one of those failsafe “no one will ever  
get this warning” things…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37253460/warning-paint-queue-size-exceeded-please-watch-the-amount-of-repaint-calls-erro)

### Animate a gif file only once

We don’t use a lot of animated gifs in our apps and focus more on layout animations/transitions but we know  
quite a few people still use such effects and might find this useful.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37236397/animate-a-gif-file-only-once)

### Message.setAttachment is silently failing

It’s sometimes hard to track edge case device specific behaviors especially with the shifting ground of mobile  
OS targets.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37219177/message-setattachment-is-silently-failing)

### Different response codes between ipa and source code

Building on our servers vs. building from the source code they generate should be nearly identical but we can’t  
guarantee that because our server OS/xcode versions might differ.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37216348/codename1-different-response-codes-between-ipa-and-source-code)

### How to set FacebookUrlSchemeSuffix build hint

Some things we just never tried to do on our own so it’s great to find out that it’s possible to have two separate accounts  
with the same facebook app.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37204082/how-to-set-facebookurlschemesuffix-build-hint-in-codename-one)
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Andreas Grätz** — May 23, 2016 at 7:14 am ([permalink](/blog/questions-of-the-week-vi/#comment-22468))

> Andreas Grätz says:
>
> I have several questions:
>
> 1\. very simple: I want to create a fullscreen app, without any appearence of a toolbar, but at start and sometimes during running the toolbar appears although I called hideToolbar() (this works only AFTER the toolbar was initially shown.
>
> 2\. When do you support iOS9 split view and master-detail splitview on iPads?
>
> 3\. When do you support iPad Pro 12 inch native resolution?
>



### **Shai Almog** — May 24, 2016 at 4:08 am ([permalink](/blog/questions-of-the-week-vi/#comment-22578))

> Shai Almog says:
>
> 1\. Is this on iOS devices or everywhere? If you don’t want anything to appear set the title to “” and make sure there are no commands.
>
> 2/3. Good questions. We already support everything in terms of the code but we are currently still compiling on an older version of xcode due to logistical reasons that would require a painful migration. We’re announcing a migration process to the newest version of xcode this week so keep an eye on the blog. Once we have the newest version of xcode in the new build servers we’ll be able to address a lot of feature requests.
>



### **Andreas Grätz** — May 25, 2016 at 11:11 pm ([permalink](/blog/questions-of-the-week-vi/#comment-22876))

> Andreas Grätz says:
>
> 1\. Thanks! I have found a solution. I have to set the margins in the theme to 0, too.  
> 2\. I’m writing a business app and this app needs a master/detail view on tablets like this: [https://apppie.files.wordpr…]([https://apppie.files.wordpress.com/2015/03/screenshot-2015-03-08-20-20-24.png?w=474&h=393](https://apppie.files.wordpress.com/2015/03/screenshot-2015-03-08-20-20-24.png?w=474&h=393)) and this [https://developer.xamarin.c…](<https://developer.xamarin.com/recipes/ios/content_controls/split_view/Images/Picture_1.png>)  
> “Translated” to Codename One I need to place two “forms” side by side each with a title and actions/menus. The left form is the masterview and must have a constant(!) width on both landscape and portrait-mode. The problem is, that placing two forms in a (super-)form, it is not possible to set the widths and the menus don’t work.
>



### **Shai Almog** — May 26, 2016 at 3:57 am ([permalink](/blog/questions-of-the-week-vi/#comment-21412))

> Shai Almog says:
>
> I would not recommend using two forms. Probably the best approach for this specific UI is to go with the Toolbars side menu. Right now the permanent side menu isn’t toggleable and doesn’t have an orientation sensitive setting both of which are things it should have.
>
> You can also use an approach similar to the kitchen sink demo where we used containers for all the views in tablet mode and forms for phone modes.
>
> Ideally we want to have a more generic approach for this, we actually have an old attempt at a generic master detail API but it was pretty awful as it predated a lot of the newer ideas we have in the framework such as Toolbar. We might take a stab at that again as we are rewriting the kitchen sink demo.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
