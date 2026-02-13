---
title: Post Message
slug: post-message
url: /blog/post-message/
original_url: https://www.codenameone.com/blog/post-message.html
aliases:
- /blog/post-message.html
date: '2019-06-17'
author: Shai Almog
---

![Header Image](/blog/post-message/html5-banner.jpg)

`BrowserComponent` is a pretty powerful tool when you just want to integrate HTML into your application. We use it a lot in native apps, but surprisingly it’s just as useful when we compile an app as a web application. It lets us embed HTML into the web application. But there’s a big caveat known as SOP when we do that.

### SOP and CORS

SOP is the “Same Origin Policy” enforced by browsers. It’s prevents CSRF (Cross Site Request Forgery) which essentially lets a site pretend it’s a different site.

SOP makes sure all requests to the site come from the same origin, it’s enforced by the browser and essentially blocks manipulations across domains. This sounds great but if you need to communicate between two sites it might be a problem. That’s where CORS comes in.

CORS (Cross Origin Resource Sharing) is essentially the SOP compliant way for a website to communicate with a different website. There are HTTP headers you need to add etc. I won’t go into it as there are [better resources on all of this](https://spring.io/guides/gs/rest-service-cors/).

### Post Message

So lets say we have a browser component from a different domain embedded in our app. Normally in a native app that’s no problem. We can inject JavaScript into it and do whatever we want.

However with the JavaScript port this becomes a bit of an issue because of SOP. `postMessage` makes the communication between the two origins possible but to get it to work you need to have code that handles sends the messages on both sides.

For this we now have the following API’s in `BrowserComponent`:
    
    
    public void postMessage(String message, String targetOrigin);

__ |  This API will work correctly in native apps as well so you can still enjoy portability   
---|---  
  
This method Calls the `postMessage()` method on the webpage’s `window` object.  
To receive a message, the web page should register a “message” event listener, just as it would to receive messages from other windows in the browser. See [MDN docs](https://developer.mozilla.org/en-US/docs/Web/API/Window/postMessage) for `postMessage()` for more information.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
