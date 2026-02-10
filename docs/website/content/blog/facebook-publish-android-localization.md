---
title: Facebook Publish & Android Localization
slug: facebook-publish-android-localization
url: /blog/facebook-publish-android-localization/
original_url: https://www.codenameone.com/blog/facebook-publish-android-localization.html
aliases:
- /blog/facebook-publish-android-localization.html
date: '2014-05-25'
author: Shai Almog
---

![Header Image](/blog/facebook-publish-android-localization/facebook-publish-android-localization-1.png)

  
  
  
  
![Picture](/blog/facebook-publish-android-localization/facebook-publish-android-localization-1.png)  
  
  
  

The Facebook native SDK for iOS and Android is difficult. It layers a great deal of complex permissions and concepts that seem obvious for engineers in Facebook but not so obvious for the casual observer. In the past Facebook allowed you to just request a write permission and you would receive such a permission, however recent SDK’s force you to request a read only permission which you then need to elevate to a write permission.  
  
  
  
  
  
Not the most intuitive approach although understandable in terms of data security,  
  
but it is badly implemented and to make matters worse its horribly broken in some SDK versions (Chen spent the day just because the version we used happened to be such a version). Regardless, we now have the ability to elevate Facebook permissions as part of the Facebook connect API in the social package. You just need to ask for publish permissions and once the callback is invoked with a success message you can write to the Facebook wall.  
  
  
  
  
A simpler approach is usually to just use the Share button which requires no special permissions so if you just want to share something we would recommend doing that. It maps to native iOS/Android functionality and is pretty powerful since it can map to Twitter and other apps installed on the device too.  
  
  
  
  
On a different subject, localization is a pretty easy task in Codename One thanks to the  
[  
i18n support  
](/how-do-i---localizetranslate-my-application-apply-i18nl10n-internationalizationlocalization-to-my-app.html)  
builtin to our tools. Our i18n tools are unique since they don’t require you to implement anything in the code and you can instantly start localizing without wide sweeping changes. However, the Android APK doesn’t detect our localization as part of your application and might incorrectly assume the APK contains a single language. To combat that we are introducing the build argument: android.locales=local;local;locale  
  
  
Where you can specify the supported languages for the APK, locale can be any 2 letter language code such as fr for French, en for English etc. Notice that these are standard combinations based on an iso standard.  
  
  
  
  
We also allow specifying the default language of a Windows Phone app using the win.locale=en-US build argument where you can customize the locale to any one you would like.  
  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
