---
title: Removing Old Dashboard
slug: removing-old-dashboard
url: /blog/removing-old-dashboard/
original_url: https://www.codenameone.com/blog/removing-old-dashboard.html
aliases:
- /blog/removing-old-dashboard.html
date: '2020-08-27'
author: Shai Almog
---

![Header Image](/blog/removing-old-dashboard/generic-java-2.jpg)

We announced a while back that we’re working on a new website. This work was 90% complete but we decided to scrap it in favor of a complete rewrite of the site. That means some links might break and some functionality might be impacted but this is a crucial change for the continued growth of the company.

One thing that we plan to remove entirely is the [old dashboard](/build-server.html). It will be removed on September 18th in favor of [the build cloud](https://cloud.codenameone.com/buildapp/index.html). Please let us know of any missing features or problems so we can address them in time for the migration.

We intend to provide support for:

  * Emailing the build results

  * QR code for OTA install

This is in addition to the UI overhaul which is worked on [here](https://github.com/codenameone/CodenameOne/issues/3177).
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Durank** — September 11, 2020 at 1:19 pm ([permalink](/blog/removing-old-dashboard/#comment-24335))

> [Durank](https://avatars0.githubusercontent.com/u/16245755?v=4) says:
>
> but this page it’ll continue working separately on the Dashboard or you will migrate all your web side?
>



### **Shai Almog** — September 12, 2020 at 4:45 am ([permalink](/blog/removing-old-dashboard/#comment-24328))

> Shai Almog says:
>
> We will migrate the entire site. To test this out we’ll remove this page earlier than the rest of the site but we will replace the entire thing. Since the new site will have a different architecture the current page just can’t physically work.
>



### **Carlos Verdier** — September 16, 2020 at 7:19 am ([permalink](/blog/removing-old-dashboard/#comment-24341))

> [Carlos Verdier](https://lh3.googleusercontent.com/-y-v_mMAwszk/AAAAAAAAAAI/AAAAAAAAAAA/AMZuucmcoea9nf4P3gRHGzB7T7jxG98R1w/photo.jpg) says:
>
> For some reason, the “Install on device” and other links don’t work properly on my iPad (13.7). I need to repeatedly tap on the link and eventually it works.
>



### **Francesco Galgani** — September 16, 2020 at 8:33 am ([permalink](/blog/removing-old-dashboard/#comment-24342))

> [Francesco Galgani](https://lh6.googleusercontent.com/-4K0ax_DVJf4/AAAAAAAAAAI/AAAAAAAAAAA/AMZuuckEd1kcni0y8k6NMzNtxwOCEPatQQ/photo.jpg) says:
>
> I have the same issue. I reported it here: <https://github.com/codenameone/CodenameOne/issues/3264>
>



### **Shai Almog** — September 17, 2020 at 4:12 am ([permalink](/blog/removing-old-dashboard/#comment-24343))

> Shai Almog says:
>
> We’re looking into this. It’s also possible this is related to limits in the HTML standard with opening links via code.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
