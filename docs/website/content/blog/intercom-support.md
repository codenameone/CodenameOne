---
title: Intercom Support
slug: intercom-support
url: /blog/intercom-support/
original_url: https://www.codenameone.com/blog/intercom-support.html
aliases:
- /blog/intercom-support.html
date: '2017-02-01'
author: Shai Almog
---

![Header Image](/blog/intercom-support/new-features-2.jpg)

We use [intercom.io](http://intercom.io) for our website support system you can see it as the chat button on the bottom right of the page. The true value of this tool is in it’s ability to deliver a unified interface everywhere, normally this stretches into native mobile apps as well. As a result we decided to port the native intercom device API to Codename One so it will be easy to deploy everywhere.

We added a new [cn1lib for intercom](https://github.com/codenameone/IntercomSupport/), it works on Android/iOS and will allow you to communicate with users of your app thru the app and web. It also allows for more advanced event based automation which is really useful when building a user funnel.

To get started install the cn1lib thru the extensions menu as usual. Then assuming you have an intercom.io account create Android/iOS apps there. From there you can get the keys for Android/iOS and use the following to activate intercom:
    
    
    Intercom.init("AndroidAppKey", "IosAppKey", "AppId");

Assuming intercom is supported on the platform `Intercom.getInstance()` will return a non-null value. You need to start by registering your user, if your app allows for login you can use the email or other credential you might have for binding the user identity using something like:
    
    
    Intercom.getInstance().registerIdentifiedUser(Registration.create().withEmail(usersEmail));

If not you can use `registerUnidentifiedUser()`.

__ |  You **MUST** register before using other API’s   
---|---  
  
Once you are registered you can check for messages and show the compose/discussion threads. You can use a UI like `FloatingActionButton` to trigger a chat with support etc.

Integrating Intercom was **very** easy, I’ll write some more about it next week.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
