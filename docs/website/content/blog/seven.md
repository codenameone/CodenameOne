---
title: Seven
slug: seven
url: /blog/seven/
original_url: https://www.codenameone.com/blog/seven.html
aliases:
- /blog/seven.html
date: '2013-10-26'
author: Shai Almog
---

![Header Image](/blog/seven/seven-1.png)

  
  
  
[  
![Picture](/blog/seven/seven-1.png)  
](/img/blog/old_posts/seven-large-2.png)  
  
  

Its been a long time coming, the main delay in getting iOS 7 support into your hands has been with the need to upgrade all the servers. Apple seems to require OS upgrades as well when it updates xcode so we had to go one by one and update everything.  
  
  
This is now all in place and you can start building apps that take advantage of iOS 7 functionality and are built using xcode 5.  

  
When you send an app to the build server without doing anything it will still be built with the existing iOS 6  
  
theme and xcode 4.5 and will act like it has for the past year or so. However, we now provide you with the ability to use xcode 5 for building and indicate a theme mode.  
  
  
There are 4 theme modes:  

  *   
Default – this just means you don’t define a theme mode. Currently this is equivalent to legacy. In the future we will switch this to be equivalent to auto  

  *   
legacy – this will behave like iOS 6 regardless of the device you are running on.  

  *   
modern – this will behave like iOS 7 regardless of the device you are running on.  

  *   
auto – this will behave like iOS 6 on older devices and iOS 7 on newer devices  

You can define these by setting the build argument ios.themeMode to legacy, modern or auto.  
  
On NetBeans you can use the UI we just added in the latest plugin (seen in the screenshot here).

  
To use xcode 5 which you would probably want if you target the  
  
latest iOS 7 devices just define ios.xcode_version=5.0

  
Those of you who submitted apps to the store know that you need screenshots in the various resolutions to  
  
match, that is why we added iOS 7 versions of all the iOS device skins to the skin section. Just select More in the simulator skin menu to download additional iOS 7 skins.  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — October 28, 2013 at 1:51 pm ([permalink](https://www.codenameone.com/blog/seven.html#comment-21741))

> Anonymous says:
>
> Is a build done with the “auto” flag bigger in size? I presume it must pack both versions’ resources and then decide which one to use at runtime?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fseven.html)


### **Anonymous** — October 29, 2013 at 2:36 am ([permalink](https://www.codenameone.com/blog/seven.html#comment-22000))

> Anonymous says:
>
> Not noticeably. The resource file isn’t very big ~400k for each theme 800k for both. 
>
> This might sound like much for mobile developers but considering that a hello world currently starts at 4mb due to the overhead of the JVM this is small potatoes. 
>
> Keep in mind that iOS apps are bundled as app bundle directories so there is no overhead in runtime for any amount of files included in the bundle (although there is overhead for compiled code size but that’s a different story).
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fseven.html)


### **Anonymous** — January 23, 2014 at 11:16 am ([permalink](https://www.codenameone.com/blog/seven.html#comment-21967))

> Anonymous says:
>
> As per discussion earlier on on forum I was trying to “define OS by setting the build argument”. 
>
> But since I am using Eclipse – i cannot find WHERE I can do that manually as you mentioned before (to set ios.xcode_version=5.0) . Is it a part of build.xml (looked there could not find), or theme Constants (could not find it ether). 
>
> Please let me know
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fseven.html)


### **Anonymous** — January 23, 2014 at 4:30 pm ([permalink](https://www.codenameone.com/blog/seven.html#comment-24161))

> Anonymous says:
>
> In this case you will want ios.xcode_version=4.5 since now 5.0 is the default. Right click the project->Properties->Codename One 
>
> Select “Build Hints” tab at the top and press “Add”. 
>
> Theme constants are in the designer in the theme under the constants tab.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fseven.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
