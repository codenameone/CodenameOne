---
title: SEO, Gallery, Performance, JavaScript & Gradle
slug: seo-gallery-performance-javascript-gradle
url: /blog/seo-gallery-performance-javascript-gradle/
original_url: https://www.codenameone.com/blog/seo-gallery-performance-javascript-gradle.html
aliases:
- /blog/seo-gallery-performance-javascript-gradle.html
date: '2016-02-16'
author: Shai Almog
---

![Header Image](/blog/seo-gallery-performance-javascript-gradle/parparvm-blog.jpg)

We are trying to improve the visibility of Codename One within search engines and as part of that  
we think we can also help you guys improve your visibility. When you submit your apps to the app gallery we  
provide dofollow links to your website/app store listings. We’d appreciate if you place dofollow links back to  
<https://www.codenameone.com/> which you can place in a very hidden/unobtrusive way. This helps us increase  
our page rank and as a result will improve your page rank as we link back to you.

To add your app to the app gallery just comment on the post below with full details/link to your app. Make sure  
to include the package name, we need to verify that the app was built with Codename One and we don’t accept  
any other apps so please don’t post such entries as they would be deleted and comment links are “nofollow”  
anyway.

FYI the moderation system might block posts with links and put them in the moderation queue, we get a notification  
on that and approve the comments if they are indeed valid.

### Performance Optimizations For ParparVM

[Sanny Sanoff](https://github.com/sannysanoff) submitted a major  
[pull request](https://github.com/codenameone/CodenameOne/pull/1688) that’s already on the build servers.  
It’s a very ambitious change and based on our internal benchmarks it does improve quite a few things in terms of  
on-device performance.

It’s hard to tell how much of a difference will be felt in real world applications with changes like this as VM’s  
are pretty different from one another. We find that the biggest performance differences we see is when we  
improve the implementation of core API logic e.g. string handling primitives etc.

### JavaScript Builds & Static Initializers

With the recent news that JavaScript builds are now available to a wider audience we also ran into some issues.

We are working on releasing an updated version of the VM based on the latest code from [TeaVM](http://teavm.org)  
which will hopefully solve some of these problems. Since this is a big change it is taking a while but we hope to finish  
it this week…​

One point that didn’t get enough attention when we released the original JavaScript port was an issue it has with  
static initializers. [TeaVM](http://teavm.org) does some pretty insane things to make threading “feel” real despite  
the fact that the browser is single threaded. Essentially every call to `wait`/`notify` etc. triggers a split in the generated  
code that allows the VM to switch co-operatively to other code. For 98% of the code this is totally seamless as  
proper code would eventually block somewhere.

However, static initializers are a bit of a special case here. It seems that its impossible to split them in a way  
that allows this functionality. So you can still use static initializers but you can’t call `wait`/`notify` etc. within them.

Normally that’s not a big deal, however it becomes a big deal when you realize that this also applies to code you  
invoke indirectly e.g. image loading IO would trigger `wait` for the IO operation to complete internally. The workaround  
is to do image loading lazily and not in the static initializer, ideally we’d like a more robust workaround but since  
this is a rather elaborate issue I’m not really sure how practical that would be.

### Retry on the Google Play Services Patch

We had some issues with gradle Android builds last week and we eventually changed the default build back to  
ant just so we could get thru the week. We restored gradle builds today and hopefully fixed the issues with the  
build process, if you notice anything this might be due to that…​

Please let us know at once if there are any recent regressions related to the Android build here.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chibuike Mba** — February 19, 2016 at 2:56 pm ([permalink](/blog/seo-gallery-performance-javascript-gradle/#comment-22426))

> Chibuike Mba says:
>
> Hi Shai, we have CodenameOne logo and link on our app website: [http://ozioma.net](<http://ozioma.net>)
>
> Our app “Ozioma” is already on the CodenameOne app gallery here: [https://www.codenameone.com…](</featured-ozioma/>) but I want to change the screenshots of the app to the newer version screenshot, how can I do that?
>
> App package id: net.chibex.ozioma
>
> Thanks.
>



### **Fabrizio Grassi** — February 20, 2016 at 12:16 am ([permalink](/blog/seo-gallery-performance-javascript-gradle/#comment-22618))

> Fabrizio Grassi says:
>
> Hi Shai, I just added your logo to my app site: [http://apps-rbryx.rhcloud.com](<http://apps-rbryx.rhcloud.com>)  
> right now iJobClock is my only app.
>
> package id: org.rbryx.iJobClock
>
> Many thanks
>



### **Shai Almog** — February 20, 2016 at 4:07 am ([permalink](/blog/seo-gallery-performance-javascript-gradle/#comment-22628))

> Shai Almog says:
>
> Great. Can I just grab the screenshots from your home page and update the app?
>



### **Shai Almog** — February 20, 2016 at 4:07 am ([permalink](/blog/seo-gallery-performance-javascript-gradle/#comment-22482))

> Shai Almog says:
>
> Thanks, adding it soon!
>



### **Chibuike Mba** — February 20, 2016 at 8:28 am ([permalink](/blog/seo-gallery-performance-javascript-gradle/#comment-22583))

> Chibuike Mba says:
>
> Yes, some of them not all. 5 screenshots are ok.
>
> Thanks.
>



### **Shai Almog** — February 21, 2016 at 4:01 am ([permalink](/blog/seo-gallery-performance-javascript-gradle/#comment-22350))

> Shai Almog says:
>
> Thanks. Its updated now. Might take a moment for images to refresh in the CDN.
>
> App looks REALLY great!
>
> When is the iOS version coming?
>



### **Chibuike Mba** — February 21, 2016 at 11:40 am ([permalink](/blog/seo-gallery-performance-javascript-gradle/#comment-21624))

> Chibuike Mba says:
>
> OK Shai, Thanks.
>
> iOS version will be coming in the next version of the app before the end of April this year.
>
> Thanks once again, I appreciate.
>



### **Yaakov Gesher** — March 1, 2016 at 10:07 am ([permalink](/blog/seo-gallery-performance-javascript-gradle/#comment-21493))

> Yaakov Gesher says:
>
> I just uploaded my app to Google Play: [https://play.google.com/sto…](<https://play.google.com/store/apps/details?id=il.co.medonline.doctorapp>). iOS version coming soon!
>



### **sao** — March 1, 2016 at 11:12 am ([permalink](/blog/seo-gallery-performance-javascript-gradle/#comment-21554))

> sao says:
>
> Hi Shai, this is the google play id/link of the SNOS app:  
> [https://play.google.com/sto…](<https://play.google.com/store/apps/details?id=com.teledom.snosapp>)  
> package id: com.teledom.snosapp
>
> NB: The app is yet to be marketed and taken to markets for clients to start using, thus the download rate from Google Play is still very low at the moment.
>
> Thank you!
>
> Afam
>



### **Shai Almog** — March 2, 2016 at 4:17 am ([permalink](/blog/seo-gallery-performance-javascript-gradle/#comment-22535))

> Shai Almog says:
>
> Added. Thanks.
>



### **Shai Almog** — March 2, 2016 at 4:17 am ([permalink](/blog/seo-gallery-performance-javascript-gradle/#comment-22270))

> Shai Almog says:
>
> Added. Thanks!
>



### **James van Kessel** — March 9, 2017 at 8:54 pm ([permalink](/blog/seo-gallery-performance-javascript-gradle/#comment-21567))

> James van Kessel says:
>
> Hello Shai, Here’s the Google Play version of my new app (My First real app published to the app store!): [https://play.google.com/sto…](<https://play.google.com/store/apps/details?id=ca.zettabot.trainingtool>)  
> package id: ca.zettabot.trainingtool
>
> Apple version is pending on the wise meditations of the Apple Gurus in the sky…
>



### **Shai Almog** — March 10, 2017 at 8:41 am ([permalink](/blog/seo-gallery-performance-javascript-gradle/#comment-23225))

> Shai Almog says:
>
> Hi,  
> that’s great to hear. We’ll add this soon. Keep us posted when the app makes it to itunes.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
