---
title: Switching Defaults
slug: switching-defaults
url: /blog/switching-defaults/
original_url: https://www.codenameone.com/blog/switching-defaults.html
aliases:
- /blog/switching-defaults.html
date: '2014-01-20'
author: Shai Almog
---

![Header Image](/blog/switching-defaults/switching-defaults-1.png)

  
  
  
  
![Picture](/blog/switching-defaults/switching-defaults-1.png)  
  
  
  

As you may know Apple will require that all applications submitted next month would be compiled with XCode 5 and target iOS 7 primarily. We  
[  
supported this for quite some time  
](http://www.codenameone.com/3/post/2013/10/seven.html)  
however the default was still set to the legacy support first. Today we are deploying an update which will make xcode 5.0 the default and make the theme use iOS 7 theme styling when running on an iOS 7 device.  
  
  
  
  
  
We are also updating the plugin to default to iOS 7 skins and adding the old iOS 6 related skins under the “more” menu in the device section, this will allow you to target existing devices more effectively.  
  
  
  
  
Notice that components such as Spinner do not work as they should in iOS 7, you should switch to using the Picker component. It provides native functionality when running on the device. This is due to a radical design of the picker UI in iOS 7.  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
