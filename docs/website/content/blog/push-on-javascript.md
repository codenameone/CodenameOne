---
title: Push on JavaScript
slug: push-on-javascript
url: /blog/push-on-javascript/
original_url: https://www.codenameone.com/blog/push-on-javascript.html
aliases:
- /blog/push-on-javascript.html
date: '2017-02-20'
author: Shai Almog
---

![Header Image](/blog/push-on-javascript/push-megaphone.png)

Up until now push notification in Codename One only worked for Android & iOS devices. This is about to change this weekend when the JavaScript port should (almost) seamlessly start working with push!  
This is pretty cool as push to the web is a pain with every browser taking a somewhat different route but with Codename One this will “mostly” work.

The JavaScript port will generate push keys that match the notation `cn1-web`.

At this time we support:

  * Firefox (Version 50)

  * Chrome (Version 49)

  * Opera (Version 42)

  * Chrome for Android (Version 56)

__ |  Edge/IE don’t currently support push. We might support Edge as it becomes available there   
---|---  
  
Firefox doesn’t require any special setup for Push. If your main class implements the `PushCallback` interface, it should **just work**.

Chrome uses GCM for its push notifications – the same system that Android uses. The directions for setting up a GCM account are the same as provided in the Android section, and you can reuse the same `GCM_SENDER_ID` and `GCM_SERVER_KEY` values. You need to add a build hint so that the GCM_SENDER_ID will be added to the app’s manifest file:  
You don’t need to do anything special and your push callback will be invoked when you send a push to the given key. That’s pretty cool.

So currently we use the `setProperty()` method of `Display` to set the sender ID but this won’t work for Chrome as we need that value during build time. Just move that value to the build hint `gcm.sender_id` and you can remove the `setProperty` statement once this change goes live as it will become implicit.

__ |  Notice we used `gcm.sender_id` and not `javascript.sender_id` to highlight that this value is common for GCM implementations   
---|---  
  
Safari is problematic as it doesn’t use the same semantics of any other push service. It doesn’t act like iOS or like other browsers. So at this time we don’t plan on supporting Safari as it’s audience is mostly in mobile where we have proper native app support.

### Availability & More

Push on UWP is something we hope to introduce in the near future as well. We began some work on this but it is not ready at this time. Hopefully we’ll have a completely universal push solution in the near future.

We will update the new push support this Friday which should include a push server update. Once that update goes in push for web should “just work” but would require a new build.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
