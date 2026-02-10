---
title: 'TIP: Don''t Use Push as a Communication Protocol'
slug: tip-dont-use-push-as-communication-protocol
url: /blog/tip-dont-use-push-as-communication-protocol/
original_url: https://www.codenameone.com/blog/tip-dont-use-push-as-communication-protocol.html
aliases:
- /blog/tip-dont-use-push-as-communication-protocol.html
date: '2018-11-12'
author: Shai Almog
---

![Header Image](/blog/tip-dont-use-push-as-communication-protocol/tip.jpg)

Apple introduced push notification at a time when iOS apps didn’t support multi-tasking. It was used as an intrusive notification system that allowed an app to communicate it had something important to tell you. Back then push messages would trigger a dialog box as it predated the pull down notification tray pioneered by Android.

The purpose of push on iOS is visual notification. You can send non-visual meta-data but that’s almost an afterthought in iOS.

On Android push was designed as an overarching general purpose communication protocol. It’s far more powerful and Google wanted developers to use it as the actual communication protocol. In fact the visual notification of push in Android is almost an afterthought and is handled by background code within the activity.

Developers coming from the Android ecosystem tend to think of push as a communication protocol and want to use that. That’s a bad idea for iOS and not a great idea for Android either.  
We suggest using a proper communication protocol e.g. WebSockets. You should use push only for marketing related notifications and application notices in the background. That is far more portable, powerful and doesn’t suffer from the limitations of push (e.g. permissions).

E.g. in [Codename One Build](https://www.codenameone.com/blog/build-app-on-ios.html) we use networking as such:

  * Most logic is handled through WebServices – these are use easy to build/maintain and debug

  * Events such as a new build are sent thorough a WebSocket connection – that removes the need for polling and is very fast

  * When a build is completed we send a push notification and a WebSocket event – if the app is running the WebSocket event will work. If it isn’t you will get push notification notice. If you disable push notifications everything would still work

This is the best approach for networking infrastructure and we recommend most apps follow this approach. Here are a few problems in push:

  * Push needs to go thru the Apple/Google servers which are unreliable complex and incompatible

  * Push doesn’t work everywhere e.g. kindle or all ports e.g. JavaScript push doesn’t work for all browsers

  * Push can be disabled by the user which means you literally can’t rely on it working even on supported platforms

  * You need your own servers to handle the push sending and batching anyway so you won’t be able to go “serverless” with push

  * Push enforces size limits on messages

  * Push doesn’t “really” work when an app is in the background in iOS. In iOS a push notification that includes a visual payload will show that e.g. an icon, badge, message, sound etc. even in a background app. However, the non-visual payload won’t be delivered to your app if it isn’t running

All things considered push makes sense only for visual notifications and marketing as an addition to your communication protocol not as a replacement.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
