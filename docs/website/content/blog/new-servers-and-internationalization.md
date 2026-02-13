---
title: New Servers And Internationalization
slug: new-servers-and-internationalization
url: /blog/new-servers-and-internationalization/
original_url: https://www.codenameone.com/blog/new-servers-and-internationalization.html
aliases:
- /blog/new-servers-and-internationalization.html
date: '2013-10-30'
author: Shai Almog
---

![Header Image](/blog/new-servers-and-internationalization/new-servers-and-internationalization-1.png)

  
  
  
[  
![Picture](/blog/new-servers-and-internationalization/new-servers-and-internationalization-1.png)  
](/img/blog/old_posts/new-servers-and-internationalization-large-2.png)  
  
  

Its been a busy week with the  
[  
LTS  
](http://www.luxoft.com/lts-luxoft-technology-series/)  
session, we barely had time to do much. However, we did do quite a lot of work during this week including some improvements for l10n and new server deployments. 

  
Those of you who followed the l10n video in the  
[  
How Do I?  
](/how-do-i.html)  
section or our  
[  
Udemy course  
](http://udemy.com/codenameone101/)  
  
should be familiar with the really cool seamless localization support available in Codename One. However, to translate the application we usually need to send the files to a professional translator… 

  
This can be a problem, while most translators are familiar with Java properties files we did run into some bugs (on their side) with the process. To solve some of these we added the ability to export localization as CSV and just recently we also added the ability to export/import it as Android String bundles. This should make it pretty easy to import existing Android localizations but the main use case is to work with translators with tools that are already optimized for Android.  
  
  
  
The CSV is also very convenient since it can be opened by any spreadsheet and edited freely. We went to great pains to make it excel compatible although there are some bug in Excel for Mac OS X.  
  
Importing for both formats and exporting Android XML’s will be available in the next plugin update.

  
  
Our server architecture is pretty elaborate with many different tiers and nodes. We have been using Amazon for some of our build servers and our prices there have skyrocketed recently due to the uptake of Codename One in the community and apparently also due to our lack of familiarity with AWS. 

  
It turns out Amazon has a  
[  
reserved instance  
](http://aws.amazon.com/ec2/faqs/#What_is_a_Reserved_Instance)  
  
feature which is pretty similar to standard hosting, rather than just migrate to using that we also decided to migrate to Linux which is both easier to maintain and stabler. So we created a new AWS image based on Linux and were able to setup part of the build cloud functionality on that which is really cool. This effectively means that our build servers for Android, J2ME & Blackberry will be stabler and more scalable as we move forward. They will also be slightly more cost effective which is a good thing for us. Unfortunately there is no similar solution for Mac or Windows Phone (Windows 8). 

  
I’m personally anxiously waiting for the new  
[  
Mac Pro  
](http://www.apple.com/mac-pro/)  
,  
  
I think we will be looking at build times of under a minute when running on that beast. This is something we would like to offer our pro/enterprise users when we can finally get such a machine going.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
