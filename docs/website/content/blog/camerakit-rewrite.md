---
title: Camera Kit Rewrite
slug: camerakit-rewrite
url: /blog/camerakit-rewrite/
original_url: https://www.codenameone.com/blog/camerakit-rewrite.html
aliases:
- /blog/camerakit-rewrite.html
date: '2019-04-29'
author: Shai Almog
---

![Header Image](/blog/camerakit-rewrite/camera-kit-banner.jpg)

The native low level camera API on Android is a disaster. It’s often cited as one of the worst API’s on Android. To make matters worse there are two separate API’s `Camera` and `Camera2` (yes really). You need to use `Camera2` where it’s available but that means no support for older devices. To solve this we picked the Android Camera Kit library when we started building our low level camera support. This proved to be a mistake.

Camera Kit was supposed to reach 1.0 status for about a year now. It constantly moved the goal posts and eventually moved the entire implementation to Android X which meant breaking compatibility on a large scale with existing code. After waiting endlessly for 1.0 we eventually tried to move to the latest beta but this proved unworkable due to the usage of Android X. So we decided to solve this by moving to [Golden Eye](https://github.com/infinum/Android-GoldenEye). It’s a far simpler solution and as a result a stabler one.

__ |  As I write this it looks like Camera Kit is moving towards 1.0 but it’s still unclear   
---|---  
  
We still kept the name for the cn1lib which might breed some confusion so we’re open to suggestions here and might update it in the future.

As part of this overhaul we the library now supports the JavaScript port. It doesn’t support all the features but you should be able to get low level camera access in your web apps as well.

The nice thing is that the API is mostly unchanged. It can still be used with roughly the same API as we had before: <https://github.com/codenameone/CameraKitDemo/>
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Thomas McNeill** — May 1, 2019 at 12:20 am ([permalink](https://www.codenameone.com/blog/camerakit-rewrite.html#comment-23550))

> Thomas McNeill says:
>
> Awesome. Call it CameraKit2. I was trying to use it in the middle of the transition and was having issues. Glad it’s out now.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcamerakit-rewrite.html)


### **Durank** — May 3, 2019 at 5:03 pm ([permalink](https://www.codenameone.com/blog/camerakit-rewrite.html#comment-24028))

> Durank says:
>
> how can I launch camera in landscape with this api?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcamerakit-rewrite.html)


### **Shai Almog** — May 4, 2019 at 4:43 am ([permalink](https://www.codenameone.com/blog/camerakit-rewrite.html#comment-24114))

> Shai Almog says:
>
> Force landscape in the app using setPortait (this will work on Android only) then embed the camera in the form.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcamerakit-rewrite.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
