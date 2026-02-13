---
title: On Device Web Server
slug: on-device-web-server
url: /blog/on-device-web-server/
original_url: https://www.codenameone.com/blog/on-device-web-server.html
aliases:
- /blog/on-device-web-server.html
date: '2018-08-27'
author: Shai Almog
---

![Header Image](/blog/on-device-web-server/new-features-3.jpg)

I try to write about every new feature or capability we introduce but this isn’t always possible. Tasks sometimes weigh me down and as they do so I sometimes find something that I totally neglected as it was released. Such is the case with the [CN1Webserver](https://github.com/shannah/CN1Webserver) library which we launched over 6 months ago.

It got lost and in fact it came out during such a busy period I completely forgot about it.

This cn1lib lets you create a simple web server on the device. You can then connect to this server either from the device itself (the more common use case) or remotely.

This sounds insane but it’s a surprisingly common trick used by developers to get around odd device limitations. I first ran into this use case around 2010. Back then a company I was consulting for needed a way to process media files before playback (to apply DRM). This was impossible in Android at the time and it’s still challenging. Their solution was genius and trivial: they implemented a webserver on the device.

This way they could download and decode the file locally, then playback from a local URL on the device itself. The devices media API was oblivious to the fact that DRM was used in playback.

There are quite a few additional cases such as the ability to embed HTML/JS code that needs server interaction in order to work. For most of us this feature is pretty niche but the fact that you can accomplish it with a simple plugin is huge.

You can install the cn1lib like you can any other cn1lib from the Extension Manager. You will need additional setup so it will work correctly on the devices as [explained in the github project](https://github.com/shannah/CN1Webserver).
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — August 28, 2018 at 3:30 pm ([permalink](https://www.codenameone.com/blog/on-device-web-server.html#comment-24043))

> Francesco Galgani says:
>
> When I think to a server, I have in mind Apache, Tomcat, IIS… Which type of content and code can be used in that CN1 server? Only static content of html/js?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fon-device-web-server.html)


### **Shai Almog** — August 29, 2018 at 3:27 pm ([permalink](https://www.codenameone.com/blog/on-device-web-server.html#comment-23821))

> Shai Almog says:
>
> You can dynamically generate files to the file system where this server will serve them. I think it should be pretty easy to create a servlet like API to serve truly dynamic content through this API though.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fon-device-web-server.html)


### **Carlos** — February 22, 2019 at 10:29 am ([permalink](https://www.codenameone.com/blog/on-device-web-server.html#comment-24076))

> Carlos says:
>
> Could this server accept post operations, so binary files could be remotely submitted to the device?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fon-device-web-server.html)


### **Shai Almog** — February 23, 2019 at 5:39 am ([permalink](https://www.codenameone.com/blog/on-device-web-server.html#comment-24046))

> Shai Almog says:
>
> I don’t think so. A device can be hidden behind a NAT so even if you enable it this would be problematic to implement.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fon-device-web-server.html)


### **Carlos** — February 23, 2019 at 6:06 pm ([permalink](https://www.codenameone.com/blog/on-device-web-server.html#comment-24111))

> Carlos says:
>
> I’m thinking I just need sending files within the local network, so the NAT wouldn’t be a problem. What I want to do is sending files from a desktop java app to a CN1 app. What would be the best way to achieve that? I was thinking websockets, but the files could be quite large, so I’m not sure that is a good choice…
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fon-device-web-server.html)


### **Shai Almog** — February 24, 2019 at 4:32 am ([permalink](https://www.codenameone.com/blog/on-device-web-server.html#comment-24089))

> Shai Almog says:
>
> I would use the cloud as a middle tier for most cases as it makes things simpler. However, if you don’t want that I’d use a server on the desktop which ideally supports websockets. Then use the websockets to push an event to the connected client. Based on such an event I’d trigger a download from the client.
>
> This would save battery life. You can also use push notification instead of websockets to further save on battery life and send the event even when the app is in the background with visual notification to the user.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fon-device-web-server.html)


### **Carlos** — February 24, 2019 at 7:42 pm ([permalink](https://www.codenameone.com/blog/on-device-web-server.html#comment-23824))

> Carlos says:
>
> Great, thank you. I’ll try these solutions and see which one fits best for my needs.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fon-device-web-server.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
