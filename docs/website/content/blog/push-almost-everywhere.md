---
title: Push (Almost) Everywhere
slug: push-almost-everywhere
url: /blog/push-almost-everywhere/
original_url: https://www.codenameone.com/blog/push-almost-everywhere.html
aliases:
- /blog/push-almost-everywhere.html
date: '2017-02-27'
author: Shai Almog
---

![Header Image](/blog/push-almost-everywhere/push-megaphone.png)

Just last week I mentioned that we wanted to get push working on Windows & we’re pretty thrilled to announce that it’s available already…​ This means that we now support push on pretty much any major device with the exception of the Safari web browser.

As part of this change we also decided to change the push keys for the JavaScript push implementation so I suggest waiting for the next update before you push out a new version of your app. The crux of the issue was that the web push key included illegal characters for the web so the fix when using the client side send push API’s is to encode the arguments to the server.

However, considering that someone might not do that change or might have other bugs in his system we decided to base64 the keys of the JavaScript/Windows push API’s. This way they will be safe to use anywhere regardless of encoding.

The server side push API changed a bit (we updated the developer guide to reflect this) as Windows needs additional arguments. Specifically the push servers expect two new arguments: `sid` & `client_secret` both of which you can get from the Windows Store when submitting your app.

This values are also required on the client but the `Push` API was getting a bit big so we ended up rewriting the `Push` API to use a builder pattern. So instead of using a single `Push.sendPush(…​)` static call we use something like this:
    
    
    new Push(PUSH_TOKEN, "Hello World", deviceKey)
        .apnsAuth(cert, pass, ITUNES_PRODUCTION_PUSH)
        .gcmAuth(GCM_SERVER_API_KEY)
        .wnsAuth(WNS_SID, WNS_CLIENT_SECRET)
        .send();

This allows more flexibility e.g. if our app isn’t available for iOS or we know we are sending to one device that isn’t an iOS device we can just avoid the `apnsAuth` entry altogether. It’s also far more descriptive and intuitive.

### Finally

We need to make push simpler and smoother, we need to redo the video tutorials for it which are out of date by now and we need the ability to track users in a generic way to make push “seamless”.

However, having said all that I think we are in a great place in terms of push support as we now support almost every platform that allows it and the functionality is very solid.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
