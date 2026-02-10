---
title: Camera Kit – Low Level Camera API
slug: camerakit-low-level-camera-api
url: /blog/camerakit-low-level-camera-api/
original_url: https://www.codenameone.com/blog/camerakit-low-level-camera-api.html
aliases:
- /blog/camerakit-low-level-camera-api.html
date: '2018-03-28'
author: Shai Almog
---

![Header Image](/blog/camerakit-low-level-camera-api/camera-kit-banner.jpg)

When we introduced support for z-ordering of peer components in Codename One we listed two major targets. The first was already available: Map. The second was still pending: Camera.  
Our current `Capture` API is very high level and removes a lot of the control from the developer. In order to give developers a high level of control we created [Camera Kit](https://github.com/codenameone/CameraKitCodenameOne/tree/master).

[Camera Kit](https://github.com/codenameone/CameraKitCodenameOne/tree/master) is based on a native [Android Camera Kit project](https://github.com/CameraKit/camerakit-android/) whose API we used to implement the Android port and for inspiration. This new API works on Android & iOS at this time. It allows you to grab photos/videos & view the camera preview like you would any other media.

You can overlay your Codename One widgets on top of the camera view as you can see in the project sample and screenshot.

This effectively makes a lot of previously impossible use cases possible. E.g. grabbing a photo after a given interval, grabbing a video for a fixed number of seconds. Placing a UI element on top of the camera view etc.

One of the really cool things about this is that it’s entirely in a cn1lib. That means you can grok the code without understanding Codename One. You can fix issues and add functionality without knowing too much.

As a sidenote the reason we picked up this task is because an enterprise customer asked for this…​ If you can afford an enterprise account we really appreciate it and everyone gets the benefit of the new functionality!  
So if the company you work for can purchase an enterprise account this can help everyone who uses Codename One.

### Status & Future

Right now only a part of the functionality is implemented on iOS with some nuances still missing. On Android we get errors due to a bug in the native Android Camera Kit library. Since the native library is heading for a 1.0 version this should be resolved when we update to that.

I would love to see ports of this to other Codename One platforms and the simulator. We’ll take a look at these as we move forward based on user demand.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Amuche Chimezie** — March 29, 2018 at 3:45 pm ([permalink](https://www.codenameone.com/blog/camerakit-low-level-camera-api.html#comment-23779))

> Amuche Chimezie says:
>
> Awesome!! Finally.. Thank you Shai
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcamerakit-low-level-camera-api.html)


### **thunderkilll** — April 27, 2018 at 5:47 am ([permalink](https://www.codenameone.com/blog/camerakit-low-level-camera-api.html#comment-23805))

> thunderkilll says:
>
> please i have question how can i change the file Path trajectory String filePath = Capture.capturePhoto();  
> and instead the filePath = “http://localhost/images/”+imageName ;
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcamerakit-low-level-camera-api.html)


### **Shai Almog** — April 28, 2018 at 5:32 am ([permalink](https://www.codenameone.com/blog/camerakit-low-level-camera-api.html#comment-23734))

> Shai Almog says:
>
> That’s a URL not a file path. Your app needs to save the file to a path you “own” which is the file system app home directory. Can you clarify what you are trying to do?
>
> Notice that you can’t copy a file everywhere in a mobile OS as devices isolate the apps from one another to prevent security exploits.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcamerakit-low-level-camera-api.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
