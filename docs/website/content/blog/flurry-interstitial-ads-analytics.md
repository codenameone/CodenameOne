---
title: Flurry Interstitial Ads & Analytics
slug: flurry-interstitial-ads-analytics
url: /blog/flurry-interstitial-ads-analytics/
original_url: https://www.codenameone.com/blog/flurry-interstitial-ads-analytics.html
aliases:
- /blog/flurry-interstitial-ads-analytics.html
date: '2015-03-01'
author: Shai Almog
---

![Header Image](/blog/flurry-interstitial-ads-analytics/flurry_logo.jpg)

![](/blog/flurry-interstitial-ads-analytics/flurry_logo.jpg)

Chen was working with a customer that needed some specific ad network support and decided to open source  
some of that work. We now have integration with Flurry both for its ads and [analytics](https://developer.yahoo.com/analytics/)  
support both of which are pretty cool and have some distinct advantages over the current Google equivalents. 

You can download the project [here](https://code.google.com/p/flurry-codenameone/), Chen wrote a  
very detailed [usage tutorial in the wiki page](https://code.google.com/p/flurry-codenameone/wiki/Usage)  
so there is no point in repeating it.

One of the big advantages here is that the flurry integration is a cn1lib project which makes it a great integration reference  
tutorial. We now have 2 ad networks integrated in that way which means that if you want to integrate another one you have a beaten path to follow.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — March 2, 2015 at 1:29 pm ([permalink](/blog/flurry-interstitial-ads-analytics/#comment-22119))

> Anonymous says:
>
> This looks interesting! 
>
> Questions that come to mind: 
>
> 1\. Is there any reason why the methods setUserID, setAge, and setGender are not exposed via FlurryManager? I see that they are defined in FlurryNative so I would expect that in the manager class as well. 
>
> 2\. Are there any restrictions for using crash reporting (cf. the cn1 in-built feature that requires a Pro license)? 
>
> Cheers
>



### **Anonymous** — March 3, 2015 at 2:04 am ([permalink](/blog/flurry-interstitial-ads-analytics/#comment-22295))

> Anonymous says:
>
> 1\. Those are probably an omission due to lack of information from the customer. 
>
> 2\. The crash reporting in flurry won’t give you any valuable information since Codename One apps are stripped by default so you will get giberrish. There are other complexities involved as well. 
>
> With the new VM we would like to upgrade the crash reporting capability to a level that would provide you with Java thread states which is far more useful than anything Flurry has for a Java developer. But that is still a way off.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
