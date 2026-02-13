---
title: Badges
slug: badges
url: /blog/badges/
original_url: https://www.codenameone.com/blog/badges.html
aliases:
- /blog/badges.html
date: '2014-05-13'
author: Shai Almog
---

![Header Image](/blog/badges/badges-1.png)

  
  
  
  
![Picture](/blog/badges/badges-1.png)  
  
  
  

iOS allows us to send a push notification to trigger a numeric badge on the application icon, this is something you could do with Codename One for quite some time although it was mostly undocumented. You could send a push notification with the type 100 and the number for the badge and that number would appear on the icon, when you launch the app the next time the default behavior is to clear the badge value.  
  
  
  
  
There is also an option to send push type 101 and provide a badge number semi-colon and a message e.g. use a message body such as this: “3;This message is shown to the user with number 3 badge”. Obviously, this feature will only work for iOS so don’t send these push types to other platforms…  
  
  
  
  
  
In addition to that we have added an option to change the badge number, this is useful if you want the badge to represent the unread count within your application. To do this we added two methods to display: isBadgingSupported() & setBadgeNumber.  
  
  
Notice that even if isBadgingSupported will return true, it will not work unless you activate push support!  
  
  
  
  
  
To truly utilize this you might need to disable the clearing of the badges on startup which you can do with the build argument ios.enableBadgeClear=false  
  
  
  
  
On a separate note we also added the ability to scale images based on aspect ratio  
  
  
to the ImageIO class with a new method saveAndKeepAspect which should provide more memory efficient scaling. This is important for Android which manages image memory quite poorly.  
  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — September 4, 2014 at 7:13 am ([permalink](https://www.codenameone.com/blog/badges.html#comment-22029))

> Anonymous says:
>
> I’ve made a mistake in this post. Number badges should be separated by a space not a semicolon so this “3;This message is shown to the user with number 3 badge” should be really: 
>
> “3 This message is shown to the user with number 3 badge”
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbadges.html)


### **Omar Suleiman** — January 14, 2018 at 12:42 pm ([permalink](https://www.codenameone.com/blog/badges.html#comment-24155))

> Omar Suleiman says:
>
> We tried more time to find badges with android but as some comments in stackoverflow is only working on IOS, so when will it become available on android? .
>
> Thanks
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbadges.html)


### **Shai Almog** — January 15, 2018 at 6:45 am ([permalink](https://www.codenameone.com/blog/badges.html#comment-23742))

> Shai Almog says:
>
> Hi,  
> I’ve answered this here: [https://stackoverflow.com/q…](<https://stackoverflow.com/questions/48250031/add-badge-for-android-with-codenameone>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbadges.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
