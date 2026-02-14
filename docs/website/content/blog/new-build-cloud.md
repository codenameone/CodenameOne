---
title: New Build Cloud
slug: new-build-cloud
url: /blog/new-build-cloud/
original_url: https://www.codenameone.com/blog/new-build-cloud.html
aliases:
- /blog/new-build-cloud.html
date: '2018-07-01'
author: Shai Almog
---

![Header Image](/blog/new-build-cloud/generic-java-2.jpg)

**This is important!** We will replace the entire build infrastructure of Codename One over this weekend. That means that you might see disruptions in service through the weekend but please report them to us as we might not be aware!  
We are **finally** removing the last remaining pieces of the horrible mess that is Google App Engine from our backend code. This is a huge job and is sure to cause some disruption.

In the long term this is great news. It means our servers will be modernized. As a result they will be better equipped to adopt new features that we’ve rejected in the past due to the age of the infrastructure. The downside is this bump in the road.

A huge part of the difficulty is switching the servers while avoiding disruption and letting you all upgrade your plugins at your own pace. We tried to create the new server in such a way that it would proxy into the old server so builds would seamlessly work even if you still use an old plugin. This should work great for simple cases but might introduce issues with edge cases. E.g. if you have two machines and only updated the plugin/libraries in one of them.

This might create regressions as the infrastructure is quite complicated but we hope we can resolve them quickly now that we have better control over the server process.

### A Few Things Will Change

As part of this change we need to change/deprecate some niche features that haven’t been used as much:

  * `Log.getUniqueDeviceId()` will return `-1` after this update and will no longer work for new builds. You will need to switch to `Log.getUniqueDeviceKey()` which returns a unique string

  * `AnalyticsService` will now default to app mode. The old web mode is now officially deprecated and will stop working in the future

  * Cloud email which is used via the `sendMessageViaCloud` API will no longer work at some point (it does work now but we will eventually retire it). If you need an equivalent API we will introduce something via a cn1lib such as SendGrid integration etc.

Other than that this change should be almost seamless for your app logic…​

### No New UI Right Now

This change won’t change a lot in terms of features right now. In the near future this will enable us to completely rebuild the UI of the build server. We have some big plans for that and I hope we’ll be able to deliver on them now that we have better control.

But first I need to finish the work on the damn book. After this whole side track on the server. While we’re on the subject, thank you all who sent feedback on the first chapter it was super helpful!  
I hope to have the second chapter out and about this week, it will take a while to complete everything as I’m going top-to-bottom through the book and it will take a while to finish the appendices.

Once the book is done I’ll publish the first two chapters and the appendices here for free.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — July 4, 2018 at 12:39 am ([permalink](https://www.codenameone.com/blog/new-build-cloud.html#comment-24010))

> Francesco Galgani says:
>
> In my apps I rely on sendMessageViaCloudSync to send to myself screenshots of the apps. I also use it to allow users to send to me technical assistance requestes. So… I hope that you will remove this API to send email only after an alternative method for your supported platforms will be ready. I also use Log.getUniqueDeviceId(), however replacing it with Log.getUniqueDeviceKey() it’s not a problem.
>



### **Shai Almog** — July 4, 2018 at 4:24 am ([permalink](https://www.codenameone.com/blog/new-build-cloud.html#comment-24016))

> Shai Almog says:
>
> Right now it will continue working. It invokes app engine so until we physically turn off the servers it will still work.
>
> The reason we need to remove this is conceptual. We use one email provider to send an email for all users, but one bad actor who sends spam emails can destroy the deliverability of emails from everyone. This is already the case where we got blacklisted by some spam filters like spamcop.
>
> The solution includes two separate actions. First we need to separate the email account so you would send using sendgrid, AWS or mailgun. All of these have a free quota e.g. 10k emails so you’d still be able to use them for free. Here we can offer a cn1lib that will interact with one of those. But there’s a problem. The credentials for these API’s should be in the server so a server will perform the operation. Right now we don’t store any private data in the cloud and don’t have the UI to do so.
>
> Sending credentials from the client is possible but is inherently insecure. It’s not a big deal as those would be free accounts but still I wouldn’t want to throw away one broken solution for another…
>
> Thanks for the headsup on using this, I’ll try to give this more thought and find a long term solution before we shut down the old server.
>



### **Francesco Galgani** — July 4, 2018 at 10:35 am ([permalink](https://www.codenameone.com/blog/new-build-cloud.html#comment-24022))

> Francesco Galgani says:
>
> Thank you Shai, I understood what you wrote, however I have some dubts. I’m trying to expose my doubts in this comment. Because your current API can send emails only to the email account of the developer who sent the app to your build servers, I have difficult to understand that “sendMessageViaCloudSync” can be used to spam (assuming that a developer cannot spam to theirself). Moreover, this API is available only to Pro accounts, so I suppose that is virtually impossible that somebody pays a Pro account to send unwanted emails to theirself. In my experience, it’s very difficult to buy an IP that it’s not already inserted in one or more spam blacklists, so it’s normal that the IPs of your servers can be in one or more blacklists. Because all these facts, I don’t see a conceptual issue in your API… of course, I can be wrong. Thank you for any further clarification.
>



### **Shai Almog** — July 5, 2018 at 4:01 am ([permalink](https://www.codenameone.com/blog/new-build-cloud.html#comment-23557))

> Shai Almog says:
>
> We just customize the from field. We don’t actually send an email as “you” since we use our servers to do so. That means the reputation for sending is shared with every Codename One user out there as the SMTP server IP address is what counts for a lot of spam filtering solutions e.g. Spam Cop.
>
> Otherwise every spammer could just randomly change the from field and spam away from one server (they actually do that but it’s useless). That means that if one Codename One user sends bad emails and his users flag them as spam we all pay…
>
> The solution is different servers with different IP addresses that allow us to isolate ourselves from one another and build our own email sending reputation. Thankfully all email providers have a relatively generous free monthly quota (around 10,000 emails) so the question becomes how do we expose access to these services without risking your credentials… Thinking about this further I’m afraid there is no easy answer, there will always be a bit of a risk to credentials but I have an idea on how to accomplish this in a way that’s relatively intuitive.
>
> Please file an issue on this I’ll try to push out a cn1lib to address this soon.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
