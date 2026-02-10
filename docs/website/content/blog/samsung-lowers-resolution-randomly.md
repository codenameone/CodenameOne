---
title: Samsung Lowers Resolution Randomly
slug: samsung-lowers-resolution-randomly
url: /blog/samsung-lowers-resolution-randomly/
original_url: https://www.codenameone.com/blog/samsung-lowers-resolution-randomly.html
aliases:
- /blog/samsung-lowers-resolution-randomly.html
date: '2020-06-04'
author: Shai Almog
---

![Header Image](/blog/samsung-lowers-resolution-randomly/from-stack-overflow.jpg)

A few weeks ago we got [this question](https://stackoverflow.com/questions/61752978/codename-one-app-running-in-lower-resolution-android) on stackoverflow. At first I didn’t think this issue was special…​ But as the investigation continued it became clear that we’re facing a weird issue…​

The issue started innocently enough. A device whose native resolution is high was rendering the UI in low resolution. This can happen because of a new DPI setting or configuration in a new SDK.

But the odd thing was this: if the apps package name was changed the resolution went back to normal!

### It’s Samsung’s Fault

Skipping to the end: it’s Samsung’s fault. The problem starts shortly after the submission to the play store. Samsung classifies the app as a game and in order to increase performance it [runs it with a lower resolution/density](https://stackoverflow.com/questions/51014494/display-density-and-size-reduced-by-samsung-game-optimization).

Use the contact option from [this app](https://play.google.com/store/apps/details?id=com.samsung.android.gametuner.thin) and ask them to reclassify your app so it isn’t resized.

This information was available online but was remarkably hard to discover since we incorrectly assumed Google was at fault and didn’t think it was Samsung. There was no incriminating information in the console output that we could find or any hint of what had happened.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
