---
title: The Native Version of Build isn't Coming to iOS
slug: build-app-not-coming-ios
url: /blog/build-app-not-coming-ios/
original_url: https://www.codenameone.com/blog/build-app-not-coming-ios.html
aliases:
- /blog/build-app-not-coming-ios.html
date: '2019-03-26'
author: Shai Almog
---

![Header Image](/blog/build-app-not-coming-ios/codename-one-build.jpg)

When we [released Codename One 6.0](https://www.codenameone.com/blog/codename-one-6-0-chat-live.html) we mentioned that Codename One build is going through the approval process on iOS. We didn’t mention that this was a process where Apple repeatedly rejected us and we had to appeal over and over again. 

We wanted to have a native app because they look/feel slightly better. We also wanted native in-app-purchase as an alternative to PayPal. But it seems Apple won’t allow a native app to do basic things that a web app can easily pull off on its platform. 

For iOS we had to hide the fact that builds can exist for other OS’s. So we can’t mention Android support or even show the logo. That’s prohibited by Apples terms. But the sticking point was the ability to install the app you built. A pretty basic feature for an app building service. It works for web apps without a problem, we just launch a link and the app installed. 

It seems that Apple guideline 2.5.2 prohibits 3rd party app installations. This makes a lot of sense on the surface. It’s meant to stop spam applications from constantly pushing you to install additional apps or stop a hacker from leveraging a loophole. But this is a legitimate use that works on the web and it’s crucial for this type of app. In a sense these guidelines make native apps less powerful than their web based equivalents.

After spending ages back and forth with Apples bureaucrats over this requirement it seems that this just won’t happen. But I also had an epiphany of sort…

### Web is Sometimes Better

I’m strongly in the “native first” camp as this is our core business: we sell a cross platform native development tool. While it supports targeting web UI’s as well, that’s a secondary function and not its primary value.

However, in this specific case, Apples restrictions make no sense. Amazingly, web provides the same level of functionality as the native app. It also has a couple of other advantages:

  * Fast/instant deployment

  * No restrictions about mentioning other platforms

  * Subscriptions – Without the “Apple tax”

The disadvantages for our case are:

  * No appstore discoverability - This isn’t a big deal as there are so many apps in the stores today

  * No in-app-subscriptions - This is a benefit and a curse. In app purchase is a “low touch” solution for subscriptions but the cost and restrictions are prohibitive

  * No push – This is the most painful downside of this, it seems all browsers on iOS don’t support push at this time

Another disadvantage is the seamless login support. We invoke the app with a token from the web in order to login without a password. That’s both secure and convenient as one doesn’t need to type a password on the device.

This is problematic for the web app and as a result we need to show a typical email/password box. That’s not a deal breaker but it’s still a slight annoyance.

### Do We Still Need Native?

Not as much as we used to and this gap is eroding fast. There are still annoyances with small things that are trivial to accomplish in native but require multiple steps in the web version. But for the most part they are easy to circumvent. 

One of the nice things about cross platform development is the fact that we don’t rely on Apples whims. We can just release the web version of the app and keep working. That’s a huge advantage. It’s still not perfect but it looks as close as possible to a native app even though it’s technically a web app. So for this particular case this can work.

For the Android version we still have the native app that works just fine. It also looks slightly better than the web version on the same device (mostly due to fonts). It supports push and in-app-purchase so it provides that full app experience.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — March 31, 2019 at 8:13 am ([permalink](https://www.codenameone.com/blog/build-app-not-coming-ios.html#comment-23958))

> Francesco Galgani says:
>
> Dear Shai,  
> I understand the problem: I have an app with high votes on Android (4.8/5) and +1000 installations, but Apple rejected the same identical app multiple times because the reviewers consider it “useless”…
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
