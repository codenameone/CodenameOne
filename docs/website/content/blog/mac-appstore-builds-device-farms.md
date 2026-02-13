---
title: Mac Appstore Builds & Device Farms
slug: mac-appstore-builds-device-farms
url: /blog/mac-appstore-builds-device-farms/
original_url: https://www.codenameone.com/blog/mac-appstore-builds-device-farms.html
aliases:
- /blog/mac-appstore-builds-device-farms.html
date: '2017-11-07'
author: Shai Almog
---

![Header Image](/blog/mac-appstore-builds-device-farms/mac.jpg)

Steve has been pretty busy. We have new support for Mac Appstore builds as part of our desktop build process. That means you can build a signed Mac desktop app with Codename One which required a bit of work with previous releases. He also adapted our automated tests for Codename One so they would run on device farms and test against major versions of Android.

### Mac Builds

In order to use these changes you’ll need to install the latest release candidate for Codename One which includes an updated version of Codename One Settings. Once you do that check out [this new section](/manual/appendix-mac.html) of the developer guide that covers the process of building and signing the Mac app.

Mac OS is locked down to some degree and if you ship an app outside of the app store you might run into problems. With this you can get the same benefit as you would get with itunes for iOS app sales on the Mac desktop. Since we already support UWP shipping through the Microsoft Windows Store is already possible.

### Device Farm Testing

One of the more important features we are still lacking is device farm testing. We have support for auto-tests and quite a few other capabilities but running tests automatically for all devices using a cloud based device farm is something we just never got around to do.

Apparently we’re already there, when we commit a change to Codename One or a pull request it will now test that change on the simulator as well as on multiple versions of Android out of the box. The complexity of adding automated iOS tests is due to the complexity of the fact that we are testing an “unpublished” version of Codename One but this should be relatively easy to accomplish for your apps.

Steve also added a status badge to the project that highlights when all the tests pass in green so you know if something is failing just by looking at our [github project page](http://github.com/codenameone/CodenameOne/).

We hope to add automated iOS tests for better stability of Codename One but as I mentioned before this isn’t trivial.

#### Device Farms and Your Projects

We already discussed CI support for your project but not the full TDD process. Now that we have a bit more experience with these device farms we hope to leverage that experience to make TDD trivial in Codename One.

This is hugely important, when you build multiple applications TDD allows you to change code rapidly without concern that you might run into a regression. This is hugely important in the volatile world of mobile development where new mobile OS’s/devices come out on a daily basis. Codename One does a lot of the heavy lifting here but no technology is perfect. With this approach you can instantly test on devices that you don’t have in your possession and instantly get a notice if a commit of your broke something.

That’s pretty cool…​

We’ll hopefully post a more in-depth tutorial on this within the next couple of months. Notice that this will require the synchronous build functionality of the enterprise accounts.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — November 11, 2017 at 11:52 pm ([permalink](https://www.codenameone.com/blog/mac-appstore-builds-device-farms.html#comment-23614))

> Francesco Galgani says:
>
> At the moment, is any automated test on a Codename One app possible? In other words, is the test-driven development possible?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fmac-appstore-builds-device-farms.html)


### **Shai Almog** — November 12, 2017 at 5:19 am ([permalink](https://www.codenameone.com/blog/mac-appstore-builds-device-farms.html#comment-24211))

> Shai Almog says:
>
> Sure. You have a test recorder built into the simulator and you can run automated tests with the simulator. You can send device builds with the test code in the enterprise subscription and get the binaries to run on your device. The one feature that was missing was the “run them automatically for you on the device”. That feature is coming soon after the 3.8 release.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fmac-appstore-builds-device-farms.html)


### **Francesco Galgani** — November 14, 2017 at 1:47 am ([permalink](https://www.codenameone.com/blog/mac-appstore-builds-device-farms.html#comment-23867))

> Francesco Galgani says:
>
> Thank you. My problem is that in the developer guide I found very few information (in the section 13.6 “Device Testing Framework/Unit Testing”, pages 412-413) and in the Codename One website I found only a very short tutorial of two minutes: [https://www.codenameone.com…](<https://www.codenameone.com/blog/test-it.html>)
>
> I didn’t find any more information. I read the API and I also tried a pull request: [https://github.com/codename…](<https://github.com/codenameone/CodenameOne/pull/2276>)
>
> However, in my research of information the test capability is quite undocumented. I didn’t find any documentation about how to run a test on a real device. The API is not very well documented. I didn’t find any mention of the test recorder in the Codename One Academy courses, I discovered it because this blog post. So… are there more information elsewhere? Can you add a better video-tutorial soon? Thank you
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fmac-appstore-builds-device-farms.html)


### **Shai Almog** — November 14, 2017 at 4:59 am ([permalink](https://www.codenameone.com/blog/mac-appstore-builds-device-farms.html#comment-21588))

> Shai Almog says:
>
> We will focus more on TDD and CI with 4.0 and already have some work pending right after the release.
>
> In the developer guide there is a section on continuous integration but we intend to do more and also release projects that already include the full set of scripts to work with existing CI solutions and appium (on device testing farms). This will be out relatively soon and we’ll post blog updates as this is published.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fmac-appstore-builds-device-farms.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
