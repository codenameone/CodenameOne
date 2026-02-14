---
title: Updates and Holidays
slug: updates-holidays
url: /blog/updates-holidays/
original_url: https://www.codenameone.com/blog/updates-holidays.html
aliases:
- /blog/updates-holidays.html
date: '2017-11-29'
author: Shai Almog
---

![Header Image](/blog/updates-holidays/new-features-6.jpg)

Before I go into the details a quick announcement, we need to update some of our push servers. We will have a short amount of downtime on Sunday December 3rd around 8AM GMT. This update should be very fast and barely noticeable but it might impact some push message deliverability for a short period.

Some of us will be on vacation around December but I’ll personally still work during the month. However, I won’t post regular blog updates until mid January as the traffic during the Christmas/new year season is relatively low and I’m afraid some important updates might slip between the cracks. We do plan to push out a plugin update version 3.8.1 during this time as we have some new features and bug fixes pending.

Steve already did a lot of work on continuous integration/TDD (Test Driven Development) support and Travis CI in particular. We already have a blog post pending but since some of the functionality requires a plugin update we’ll publish that in 2018 when people will actually read the post…​ If you want to get a teaser you can check out this [wiki page](https://github.com/codenameone/CodenameOne/wiki/Travis-CI-Integration).

We also pushed in a lot of fixes in the past few weeks and a couple of new features…​

### South Component

One of the common RFE’s in side menu bar is the ability to add a component to the “south” part of the side menu. Up until now we had various patches and workarounds to allow this but these often required some “dubious” hacks.

We now have a new API that works with the on-top and permanent side menu:
    
    
    toolbar.setComponentToSideMenuSouth(myComponent);

This places the component below the side menu bar. Notice that this component controls its entire UIID and is separate from the `SideNavigationPanel` UIID so if you set that component you might want to place it within a container that has the `SideNavigationPanel` UIID so it will blend with the rest of the UI.

### Unit Tests in the Core

We moved the unit tests for Codename One from Steve’s repo to the Codename One repo. You can see most of the unit test code [here](https://github.com/codenameone/CodenameOne/tree/master/tests/core/test/com/codename1/ui) we hope to add more extensive tests as we run into regressions and implement new functionality.

### Default Gap

The `Label` component might have been one of our mistakes when designing Codename One. It embeds too much functionality into a single component with the icon and the text. If I would start Codename One over again I’d separate the image and text functionality and use a layout/container approach.

Case in point: the gap between the label text and the icon. This defaults to 2 pixels and very few people know how to change it (it’s with the `setGap` method).

Two pixels is ridiculous for most cases and really hard to customize. We can/should fix this in the themes but I’m afraid this might break a lot of “working” code.

We added a theme constant `labelGap` which is a floating point value you can specify in millimeters that will allow you to determine the default gap for a label. We also added the method `Label.setDefaultGap(int)` which determines the default gap in pixels.

I’m conflicted about the right way to “fix this”:

  * Set the default gap to 1mm in the `Label` class

  * Set the theme constant to 1 or 2mm in the native themes

  * Change the default template to the project to include `Label.setDefaultGap(convertToPixels(1));`

I’m very much inclined to do the 1st option but I’m concerned about compatibility which is why the last option is interesting too. I’m not too crazy about littering our hello world application with hacks though.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — December 1, 2017 at 8:36 pm ([permalink](https://www.codenameone.com/blog/updates-holidays.html#comment-24154))

> Francesco Galgani says:
>
> I read the “Travis CI Integration” by Steve on the wiki page you linked. It’s very interesting, it’s something that I’m looking for. My question are:  
> – When will it be available?  
> – Are the tests that we can use the ones recorded with the “Test Recorder” of the Simulator?  
> – Can you improve the “Test Recorder” and produce more documentation, guidelines and/or examples about it?  
> – Are the tests against real devices or simulated devices?  
> – You ask for enterprise account to test on Android and iOS, so… do you have your own device farm that is for your enterprise users?  
> – Can we have a video recording and/or screenshot recording of the app during the tests?
>
> Thank you for the reply.


### **Shai Almog** — December 2, 2017 at 5:10 am ([permalink](https://www.codenameone.com/blog/updates-holidays.html#comment-23741))

> Shai Almog says:
>
> – When we do a plugin update which will probably be this month or early January  
> – Yes  
> – If there are specific things you’d like to improve in the recorder please file an RFE in the issue tracker with detailed suggestion. We currently only have the JavaDocs for the test API’s that you can refer to. I might add a testing module to the online course in the future.  
> – By default tests are against our simulator  
> – If you have an enterprise account you can build against physical devices and run the tests on a device farm. We don’t manage our own device farm as this can become a HUGE problem well outside of our scope. We can connect to any standard appium device farm and let you run your tests there. We shouldn’t enter that field as device farm providers provide a level of flexibility such as “Run the test on device X located on operator network Y with locale Z”. That’s a level of specialization we can’t compete with.  
> – That’s offered by the device farm providers

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
