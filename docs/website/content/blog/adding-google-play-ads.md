---
title: Adding Google Play Ad's
slug: adding-google-play-ads
url: /blog/adding-google-play-ads/
original_url: https://www.codenameone.com/blog/adding-google-play-ads.html
aliases:
- /blog/adding-google-play-ads.html
date: '2013-12-03'
author: Shai Almog
---

![Header Image](/blog/adding-google-play-ads/adding-google-play-ads-1.png)

  
  
  
  
![Ad](/blog/adding-google-play-ads/adding-google-play-ads-1.png)  
  
  
  

We are officially code frozen so only critical bug fixes should be resolved until the actual release. The very last feature to make it in is support for  
[  
Google Play Ads  
](https://developers.google.com/mobile-ads-sdk/)  
on iOS/Android. We currently only work with the Admob SDK as we move along we might add additional options. 

  
To enable mobile ads just  
[  
create an ad unit  
](https://apps.admob.com/?pli=1#monetize/adunit:create)  
in Admob’s website, you should end up with the key similar to this:  
  
ca-app-pub-8610616152754010/3413603324

  
To enable this for Android just define  
  
android.googleAdUnitId=  
  
  
ca-app-pub-8610616152754010/3413603324 in the build arguments and for iOS use the same as in ios.googleAdUnitId. The rest is seamless, the right ad will be created for you at the bottom of the screen and the form should automatically shrink to fit the ad. This shrinking is implemented differently between iOS and Android due to some constraints but the result should be similar and this should work reasonably well with device rotation as well.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **juanpgarciac** — May 22, 2015 at 9:17 pm ([permalink](/blog/adding-google-play-ads/#comment-22216))

> juanpgarciac says:
>
> There is a way to check the functionablity in the debug process ? cause I’d follow the steps and it doesn’t show anything… thanks for sharing
>



### **Shai Almog** — May 23, 2015 at 6:32 am ([permalink](/blog/adding-google-play-ads/#comment-22112))

> Shai Almog says:
>
> Its supposed to be seamless. On which device are you running into a problem?  
> Notice this will only work on devices and not the simulator.
>



### **App Maker** — January 10, 2016 at 9:30 pm ([permalink](/blog/adding-google-play-ads/#comment-22406))

> App Maker says:
>
> monetize option is not available! can u please tell me why? n how can i get it?!
>



### **Shai Almog** — January 11, 2016 at 3:22 am ([permalink](/blog/adding-google-play-ads/#comment-22498))

> Shai Almog says:
>
> We removed it as it was hard to maintain across IDE’s. We provided several newer monetization options including several ad based cn1libs [https://www.codenameone.com…](</cn1libs/>)
>
> And this option which doesn’t require the monetization section, just a build hint.
>



### **Pugazhendi E** — March 26, 2016 at 7:36 am ([permalink](/blog/adding-google-play-ads/#comment-22668))

> Pugazhendi E says:
>
> admob ads are not coming…I set the admob adunit ID in build arguments in netbeans ide…if i run a app, app ll work bt ads ll nt come…I followed the codename procedure… help me..
>



### **Shai Almog** — March 27, 2016 at 4:25 am ([permalink](/blog/adding-google-play-ads/#comment-22459))

> Shai Almog says:
>
> Is this on the device? Which device type?
>



### **Jean Carlos Rojas Ramirez** — June 8, 2016 at 11:35 pm ([permalink](/blog/adding-google-play-ads/#comment-22833))

> Jean Carlos Rojas Ramirez says:
>
> I am working on an app and I can add the hint for Android but I would like to know which I should use for windows phone. Thanks,
>



### **Shai Almog** — June 9, 2016 at 5:05 am ([permalink](/blog/adding-google-play-ads/#comment-21629))

> Shai Almog says:
>
> We don’t currently support Windows Phone with ads. AFAIK Googles AdMob isn’t available on Windows Phone.
>



### **Jean Carlos Rojas Ramirez** — June 9, 2016 at 5:24 am ([permalink](/blog/adding-google-play-ads/#comment-22826))

> Jean Carlos Rojas Ramirez says:
>
> Right now there is a version for windows phone for AdMob. If we build a windows phone app we can add the AdMob AdUnit for those builds.
>



### **Shai Almog** — June 10, 2016 at 3:46 am ([permalink](/blog/adding-google-play-ads/#comment-22612))

> Shai Almog says:
>
> OK. Either way Windows Phone is on the way out at Microsoft and I doubt Google would support that version. We are switching to the UWP port for universal Windows 10 support.
>



### **Pugazhendi E** — July 25, 2016 at 1:27 pm ([permalink](/blog/adding-google-play-ads/#comment-22843))

> Pugazhendi E says:
>
> yes sir..I ve tested this app with lenevo smartphone….app works well but ads ll not come
>



### **Shai Almog** — July 26, 2016 at 4:11 am ([permalink](/blog/adding-google-play-ads/#comment-22928))

> Shai Almog says:
>
> That’s probably related to these changes [https://github.com/codename…](<https://github.com/codenameone/CodenameOne/issues/1803>)
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
