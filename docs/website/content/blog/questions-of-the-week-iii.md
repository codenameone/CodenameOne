---
title: Questions Of The Week III
slug: questions-of-the-week-iii
url: /blog/questions-of-the-week-iii/
original_url: https://www.codenameone.com/blog/questions-of-the-week-iii.html
aliases:
- /blog/questions-of-the-week-iii.html
date: '2016-04-28'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-iii/qanda-friday.jpg)

We’re releasing new plugins today in for the 3.4 release, if there are major regressions we’ll push out new  
versions for the release itself but if they are stable they will be the actual release versions. We had a great  
week on stackoverflow as well with many excellent questions. As usual this post isn’t exhaustive and doesn’t cover  
all the questions asked, but it should provide a sense of the top discussions of the week.

### Turning off android.permission.INTERNET in CodenameOne

Codename One includes two default permissions on Android, Internet and storage. We turned them on by default  
since detecting an actual need for one of those permissions is really hard.

[Read on stackoverflow…​](http://stackoverflow.com/questions/36902376/turning-off-android-permission-internet-in-codenameone)

### Is there a companion to the Form.onShow() method I can use when a Form is hidden?

This is often confusing to new Codename One developers. Unlike desktop developers don’t explicitly dispose a  
window and so the lifecycle isn’t clear. Since the old GUI builder handles things differently from regular applications  
this gets even more confusing…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/36896110/is-there-a-companion-to-the-form-onshow-method-i-can-use-when-a-form-is-hidden)

### Rotate image in CodeNameOne?

Using a gesture to rotate an image in the `ImageViewer` is something we don’t support at the moment but this  
is easily doable by deriving the component. This demonstrates the power and customizability of Codename One.

[Read on stackoverflow…​](http://stackoverflow.com/questions/36842361/rotate-image-in-codenameone)

### Can’t get setScrollVisible() to work

Scroll behavior is a painful subject we should simplify, one of the confusing parts is the scrollability of a `Form`  
which is really the scrollability of the content pane.

[Read on stackoverflow…​](http://stackoverflow.com/questions/36842004/cant-get-setscrollvisible-to-work)

### animateLayout on expanding component is not working

Layout animations conflict with `revalidate()` calls, it’s important to use one or the other and not both.

[Read on stackoverflow…​](http://stackoverflow.com/questions/36839685/animatelayout-on-expanding-component-is-not-working-codenameone)

### How to get a circular layout in codenameone

I recall that someone posted a circular layout a while back, if you have it please add a better answer.

[Read on stackoverflow…​](http://stackoverflow.com/questions/36822176/how-to-get-a-circular-layout-in-codenameone)

### different colors on ComboBox

`ComboBox` UIID’s are some of the more confusing style elements we have.

[Read on stackoverflow…​](http://stackoverflow.com/questions/36807425/different-colors-on-combobox)

### Getting Codenameone to respect the proxy settings on a windows desktop build

This is a great idea I wasn’t aware of this setting in Java. We should use it for all of our tools including the build  
client tools to simplify proxy issues.

[Read on stackoverflow…​](http://stackoverflow.com/questions/36790406/getting-codenameone-to-respect-the-proxy-settings-on-a-windows-desktop-build)

### Codename one Facebook login email = null

Facebook login works in a very inconsistent way, e.g. sometimes you will get an email login and sometimes you won’t.

[Read on stackoverflow…​](http://stackoverflow.com/questions/36919187/codename-one-facebook-login-email-null)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
