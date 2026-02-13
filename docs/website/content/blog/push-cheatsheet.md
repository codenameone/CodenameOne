---
title: Push Cheatsheet
slug: push-cheatsheet
url: /blog/push-cheatsheet/
original_url: https://www.codenameone.com/blog/push-cheatsheet.html
aliases:
- /blog/push-cheatsheet.html
date: '2019-09-27'
author: Steve Hannah
---

![Header Image](/blog/push-cheatsheet/push-megaphone.png)

Push support is one of the most complicated features to set up, due to all of the red tape you have to cut through on each platform. Each platform has its own series of hurdles you have to jump through. Apple (iOS) requires you to generate push certificates. Google (Android) requries you to set up a project in Firebase console. Microsoft requires you to register your app for the Windows store. And that’s just the beginning.

Our developer’s guide provides detailed instructions on how to perform each of these steps to get up and running, but it is quite verbose. If you’ve already gone through the guide to setup push for your first app, you may not require quite such a detailed guide to help you with your 2nd app. If you’re like me, you just want a birds-eye view of what is required to get push working, and not necessarily a step-by-step tutorial on how to meet those requirements.

So…​ I have created a [Push Cheatsheet](files/push-cheatsheet.pdf), to serve as a quick reference for setting up push notifications in your mobile apps.

[![Image 160919 061502.600](/blog/push-cheatsheet/Image-160919-061502.600.png)](/files/push-cheatsheet.pdf)

As with all CN1 docs, this cheatsheet is a living document that will evolve based on your feedback. You can always download the latest PDF at <https://www.codenameone.com/files/push-cheatsheet.pdf> .

I have tried to include all of the pertinent details required to get Push working on all platforms. Each platform has its own “box” with setup instructions. It also includes a minimal code snippet to show how to implement Push support in your codename one app – in particular how to register the device for push, and receive push messages. There is a box for “Sending a Push”, which shows the format of an HTTP request that you can use to send a push message to your app’s users.

__ |  GET parameters in this snippet are color-coded and cross-referenced with other parts of the cheatsheet so you can easily see where to find this information. E.g. The `FCM_SERVER_API_KEY` in the HTTP request is purple and bold, corresponding to its mention in the “Android Client Setup” box, so you can see that this value comes from the google firebase console.   
---|---  
  
Finally, there is a section documenting the different push message type values, so you can quickly decide which type of push is appropriate for a given situation.

Please let us know if you can think of anything that would improve the utility of this cheatsheet.

Happy pushing!

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
