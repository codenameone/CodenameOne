---
title: SendGrid cn1lib
slug: sendgrid-cn1lib
url: /blog/sendgrid-cn1lib/
original_url: https://www.codenameone.com/blog/sendgrid-cn1lib.html
aliases:
- /blog/sendgrid-cn1lib.html
date: '2018-08-13'
author: Shai Almog
---

![Header Image](/blog/sendgrid-cn1lib/meeting.jpg)

When we announced the migration to the new cloud servers one of the casualties was the cloud email API. This was a well intentioned API for sending an email from an app. Unfortunately we didn’t understand the complexities of modern mail systems well enough when we came up with this API. It turns out that this is pretty problematic. Mail servers get blacklisted and emails fail to deliver.

The problem is that this impacts everyone, if one bad actor sends spam with our cloud servers all of us get blacklisted…​

The solution is simple. We now have a [new cn1lib for SendGrid](https://github.com/codenameone/SendGridLib). If you aren’t familiar with [SendGrid](http://sendgrid.com/) it’s one of the several leading transactional e-mail providers. They offer 100 free emails per day. They provide a powerful developer REST API which we utilize in this cn1lib.

If you are using the cloud email feature we strongly suggest migrating your code to this cn1lib ASAP. We usually try to give more notice in advance but the app engine backend is something we need to remove in the near future.

Note that this only applies to the send email via cloud API’s and not to the standard send email API’s which are just fine!
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — August 14, 2018 at 12:39 pm ([permalink](https://www.codenameone.com/blog/sendgrid-cn1lib.html#comment-23792))

> Francesco Galgani says:
>
> Thank you. I suppose that you suggest to replace the use of `Message.sendMessageViaCloudSync`, while we can continue to use the `Log.sendLog`, right?
>



### **Shai Almog** — August 15, 2018 at 3:24 am ([permalink](https://www.codenameone.com/blog/sendgrid-cn1lib.html#comment-23963))

> Shai Almog says:
>
> Yes!
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
