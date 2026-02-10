---
title: IOS 8 Installs & Customizing the Pull To Refresh
slug: ios-8-installs-customizing-the-pull-to-refresh
url: /blog/ios-8-installs-customizing-the-pull-to-refresh/
original_url: https://www.codenameone.com/blog/ios-8-installs-customizing-the-pull-to-refresh.html
aliases:
- /blog/ios-8-installs-customizing-the-pull-to-refresh.html
date: '2014-09-21'
author: Shai Almog
---

![Header Image](/blog/ios-8-installs-customizing-the-pull-to-refresh/ios-8-installs-customizing-the-pull-to-refresh-1.jpg)

  
  
  
  
![Picture](/blog/ios-8-installs-customizing-the-pull-to-refresh/ios-8-installs-customizing-the-pull-to-refresh-1.jpg)  
  
  
  

One of our pro users alerted us to an issue with iOS 8 that might trigger a case where apps can’t be installed OTA (Over the air, via QR or email links) and only installed via itunes. We didn’t experience this issue ourselves but it seems to be affecting many developers (not Codename One specific). This is due to caching of package bundle ID’s and he provided some resources here that might be of interest:  
  
[  
https://buildozer.io/ios8  
](https://buildozer.io/ios8)  
  
[  
http://stackoverflow.com/questions/25887805/ios-8-gm-cant-install-ipa  
](http://stackoverflow.com/questions/25887805/ios-8-gm-cant-install-ipa)  
  
[  
http://stackoverflow.com/questions/25772664/enterprise-app-update-distribution-on-ios-8  
](http://stackoverflow.com/questions/25772664/enterprise-app-update-distribution-on-ios-8)  
  
[  
http://support.hockeyapp.net/discussions/problems/26683-not-able-to-download-apps-ios8-beta-5-autoupdate-manually-etc#comment_34327687  
](http://support.hockeyapp.net/discussions/problems/26683-not-able-to-download-apps-ios8-beta-5-autoupdate-manually-etc#comment_34327687)

  
  
  
  
On an unrelated subject the  
[  
pull to refresh  
](http://www.codenameone.com/blog/pull-to-refresh-several-new-how-do-i-videos)  
functionality was a bit uncustomizable up until now, this all changed with the latest release and should now be far more customizable. To replace the icon you can define the theme constant pullToRefreshImage to the right image. To customize the appearance of the text etc. you can now use the PullToRefresh UIID and to customize the text just define the following constants in the localization bundle:  
  
  
pull.release (defaults to “Release to refresh…”) & pull.down (defaults to “Pull down do refresh…”).  
  
  
  
  
  
As you may have noticed we are deep into the last minute JavaOne preparations so everything we do is around that work. During the weekend of Friday the 26th both Chen and myself will be in transit to San Francisco and so we will not be able to address support queries or answer forum questions during that time. During that entire week we will have relatively limited forum presence due to our workload at that time.  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — October 7, 2014 at 7:02 pm ([permalink](https://www.codenameone.com/blog/ios-8-installs-customizing-the-pull-to-refresh.html#comment-22041))

> Anonymous says:
>
> The solution to the iOS8 issue seems to be to put some random characters into the bundle ID of the plist file. As this is managed by CN1 can you enable this at your end? Its affecting me for my installs as I usually run on Linux and therefore don’t have (or want) iTunes just to install.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fios-8-installs-customizing-the-pull-to-refresh.html)


### **Anonymous** — October 7, 2014 at 11:24 pm ([permalink](https://www.codenameone.com/blog/ios-8-installs-customizing-the-pull-to-refresh.html#comment-22069))

> Anonymous says:
>
> That is exactly why we have the ios.plistInject build argument.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fios-8-installs-customizing-the-pull-to-refresh.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
