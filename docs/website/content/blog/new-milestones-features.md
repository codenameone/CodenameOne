---
title: New Milestones and Features
slug: new-milestones-features
url: /blog/new-milestones-features/
original_url: https://www.codenameone.com/blog/new-milestones-features.html
aliases:
- /blog/new-milestones-features.html
date: '2017-10-30'
author: Shai Almog
---

![Header Image](/blog/new-milestones-features/codenameone-3-8.jpg)

We will enter code freeze for Codename One 3.8 next week and have a lot of things to clear off the table in order to get there!  
The first order of business is that there will be no Codename One 3.9…​ Instead we will go right to 4.0 and switch to major version number update scheme only.

This will align us better with industry standards. We spent ages in 3.x and far less in 1.x or 2.x numbering. That doesn’t make sense. It’s hard to tell what feature is big enough for a “major version number update” and it doesn’t indicate as much with the fast pace of releases. So switching to major version numbers in terms of milestones will simplify and give a better indication of the work we put in.

We will enter code freeze next week on the 7th of November with release of 3.8 scheduled for the 14th.

### Eclipse Oxygen Issue

Some eclipse users with the latest version of Oxygen (4.7.1a) have [complained about an issue](https://stackoverflow.com/questions/46822921/cant-run-simulator/46926736) with the simulator. This seems to be a [bug](https://bugs.eclipse.org/bugs/show_bug.cgi?id=526441) or change in the latest version of eclipse that we are trying to nail down.

There is a workaround mentioned in the stackoverflow [answer](https://stackoverflow.com/questions/46822921/cant-run-simulator/46926736) which should be pretty easy to follow. However, if this doesn’t work as expected you might want to use an older version of eclipse until this is fixed. We support versions from Neon 2 and up.

### On Top Sidemenu

We switched this this on by default this weekend and already found a regression that eluded us before with the overflow menu. This is now fixed in the GIT but there might be more issues, hopefully we’ll be able to resolve everything before the code freeze so please check your code and report back to us ASAP if you encounter issues!

### Automated Tests

While we have some internal tests for Codename One most of them are ad-hoc and not exactly organized. Steve setup the groundwork with a new [automated test repository](https://github.com/shannah/cn1-unit-tests) that is now also bound to our travis CI setup in github.

This means you can add a test case together with a bug report or pull request. This could help tremendously in improving the long term stability of Codename One!

### callSeriallyOnIdle

One of the features that we added and totally slipped under the radar is the new `callSeriallyOnIdle` method which essentially works exactly like `callSerially` but doesn’t perform its work until the device is idling. If you have multiple entries in this call it will serialize them and won’t do them all at once.

The main use case is for tasks that might be CPU intensive but should be done on the EDT. This allows you to postpone such a task for a moment where the EDT will sleep anyway. A good example is processing images for scrolling. You would want the scrolling to still be smooth with the placeholder images and have the processing only when the user isn’t active.

This feature is a drop-in replacement for `callSerially` and in the next update will be available in the `CN` class too as well as `Display`.

### Caching cn1lib & SMS Activation Improvements

[Yaakov](https://github.com/jegesh) added a [cn1lib](https://github.com/jegesh/cn1-object-cacher) for caching objects as JSON representations or as he put it in his description:

> This CodenameOne library enables apps to easily cache data downloaded from the app server (or any other source) in the device’s file system. Cached data is saved in JSON format, one file per object type. Caching data can potentially improve the app’s performance and responsivity, cut down on cell data usage, extend battery life, and enable ‘offline mode’.

[Diamond](https://github.com/diamonddevgroup/) fixed an NPE in both of the cn1libs for SMS activation so if you use one of them I suggest updating the library to the latest to avoid that.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
