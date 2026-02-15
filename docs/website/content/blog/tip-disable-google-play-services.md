---
title: 'Tip: Disable Google Play Services'
slug: tip-disable-google-play-services
url: /blog/tip-disable-google-play-services/
original_url: https://www.codenameone.com/blog/tip-disable-google-play-services.html
aliases:
- /blog/tip-disable-google-play-services.html
date: '2016-10-09'
author: Shai Almog
---

![Header Image](/blog/tip-disable-google-play-services/google-play-services.png)

Google Play Services is a proprietary set of tools that Google licenses to vendors under limited conditions.  
In recent years more and more features go into Google Play Services making it harder to build an app without it.

In our effort to “do the right thing” we include some of the Google Play Services tools into applications to remain  
compatible with code that requires these libraries. This also makes some features (such as location) work better.  
However,if you don’t use: Ads, Push, Maps or Location then you might not need Google Play Services at all…​

At this time the overhead of including this subset of Google play services is 1.5mb to your final APK size. This  
isn’t much but if your app isn’t big then this might double its size and slow down the cloud build process.

Once you declare that one Google play service isn’t needed we implicitly assume you understand what you are  
doing and let you pick the services manually so just setting the build hint `android.playService.ads=false` reduced  
those 1.5mb from the app size with no ill effect.

In fact one of the services we include is the ads service and Google notices that when you submit an app. It  
produces a warning if you add an app with the ads framework but declare that you don’t have ads, this small  
trick removes it.

Check out the [build hints](/manual/advanced-topics.html) section in the developer guide for more information on how you can deeply customize  
Codename One.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Sadart** — October 10, 2016 at 2:44 pm ([permalink](/blog/tip-disable-google-play-services/#comment-22844))

> Sadart says:
>
> Thanks for the tip. I have already seen the 1.5mb app size reduction in my latest build following the tip.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
