---
title: Questions of the Week XV
slug: questions-of-the-week-xv
url: /blog/questions-of-the-week-xv/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xv.html
aliases:
- /blog/questions-of-the-week-xv.html
date: '2016-07-21'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xv/qanda-friday.jpg)

We are on the final stretch of 3.5 getting ready for the codefreeze that will go into effect in the middle of next week. With that in mind we have last minute features we are trying to get out of the door and unfortunately had to skip some big tasks. One of the big problems is the iOS server migration which we couldn’t fit into the schedule. The peer component changes made it to Android but aren’t set as the default, this makes the most sense in the current state. We’ll probably flip the switch to the new peer components after the release.

There are other issues that didn’t make it but we are still rather please with this release and we think it follows  
the trends we set before of refinement, stability & ease of use. We’ll write more about those as when we make the  
final announcement.

In other news Stackoverflow introduced a new documentation feature which looks interesting…​

We already setup a [Codename One section](http://stackoverflow.com/documentation/codenameone)  
although it’s hard to tell how this will play with our existing docs. I’ve tried it a bit and I think it still needs  
a bit of work although I love some of the interface interface elements. If we get community involvement  
there we might migrate our documentation effort.

### How to make use of keep-alive in ConnectionRequest in Codename One?

We have an undocumented way to do keep alive, but we have some better options. My favorite is the websockets  
which seem to be the direction where everyone is heading.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38480147/how-to-make-use-of-keep-alive-in-connectionrequest-in-codename-one)

### How do I maintain my app’s background while a website is being displayed?

Peer components are challenging, but our latest set of improvements probably won’t make much of a difference  
in this question. It’s hard to tell though since no code or screenshot was provided.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38493965/how-do-i-maintain-my-apps-background-while-a-website-is-being-displayed)

### Integrating Android Code in Codenamone

Native code support is pretty easy but sometimes you need more control over Android native code e.g. thru  
resource files, c files etc.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38475113/integrating-android-code-in-codenamone)

### Network request timeout or network lost or slow network not getting handle properly in codenameone

Currently Codename One has a connection timeout but not a read timeout value which is a bit more “challenging”  
across platforms. This is something we should probably address.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38475063/network-request-timeout-or-network-lost-or-slow-network-not-getting-handle-prope)

### Scrollvisible is not working in barebone code

Scrollability is a really difficult concept especially in Codename One where touch scrolling is pretty challenging  
as it doesn’t allow nesting etc.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38472117/scrollvisible-is-not-working-in-barebone-code)

### CodenameOne IOS app crashing due to null threadsToDelete

This looks like a very interesting crash but those things are **really** hard to debug/track without a reasonable test case.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38471064/codenameone-ios-app-crashing-due-to-null-threadstodelete)

### How to access the source directory in codename one

Two of the biggest complexities in Codename One are the missing `java.new.URL` and `java.io.File`. We might need  
to just add these API’s as stub calls to the “real” implementations.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38453717/how-to-access-the-source-directory-in-codename-one)

### form.addOrientationListener(new ActionListener() not being called on keyboard open

Here is an example of a change in the API behavior that effectively fixed a bug serving as the basis of a different bug.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38450513/form-addorientationlistenernew-actionlistener-not-being-called-on-keyboard-op)

### Build iOS failed after changing back to the old xCode build servers

This is actually because of the Google+ support changes even on iOS Google’s incompatible changes are a pain.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38444722/build-ios-failed-after-changing-back-to-the-old-xcode-build-servers)

### How to install skins in NetBeans codename one plugin

This should work out of the box, if any of you guys are running into issues here we’d appreciate some help in  
tracking those issues.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38439868/how-to-install-skins-in-netbeans-codename-one-plugin)

### Using Native Android Bluetooth Support for Codename

Our newly announced bluetooth support is really bluetoothle which most of the industry is focusing around.  
However, there is still usage of the older bluetooth standard in quite a lot of devices.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38434999/using-native-android-bluetooth-support-for-codename)

### What is the format of an AARRGGBB array?

Some things are pretty obvious to us as guys who have done mobile Java UI’s for over a dozen years, but they  
are not always clear and it’s often hard for us to see the complexities.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38428069/what-is-the-format-of-an-aarrggbb-array)

### How to make JSON PUT request through Codename one API

Posting a custom body isn’t hard but we’d like to make it easier as we move forward, this will allow us to create  
terse webservice usage.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38427137/how-to-make-json-put-request-through-codename-one-api)

### IOS Debug Build without Developer Account

This was something we supported in the early days of Codename One, but it created a lot of problems so we decided  
to discontinue this functionality.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38425446/ios-debug-build-without-developer-account)

### Is it possible with Codename One to take a temporary photo?

We try to make photos consistent between platforms but they are pretty different. Ideally we’d like to provide better  
camera controls but those API’s are very low level and thus fragmented.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38425101/is-it-possible-with-codename-one-to-take-a-temporary-photo)
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Eric** — July 22, 2016 at 1:38 pm ([permalink](/blog/questions-of-the-week-xv/#comment-22705))

> Eric says:
>
> Hello,  
> I think there is a regression with today’s update. I can’t open my Accordion menu anymore to see his content. Can you check it please ? Thanks!
>



### **Shai Almog** — July 22, 2016 at 1:55 pm ([permalink](/blog/questions-of-the-week-xv/#comment-22960))

> Shai Almog says:
>
> Thanks for the headsup. I committed a fix that broke some of the core animations, we’re putting out a fix that should be up in an hour or so.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
