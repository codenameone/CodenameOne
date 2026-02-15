---
title: Facebook, Better Input & Improving Community Support
slug: facebook-better-input-improving-community-support
url: /blog/facebook-better-input-improving-community-support/
original_url: https://www.codenameone.com/blog/facebook-better-input-improving-community-support.html
aliases:
- /blog/facebook-better-input-improving-community-support.html
date: '2013-11-19'
author: Shai Almog
---

![Header Image](/blog/facebook-better-input-improving-community-support/facebook-better-input-improving-community-support-1.png)

  
  
  
  
![Facebook Login](/blog/facebook-better-input-improving-community-support/facebook-better-input-improving-community-support-1.png)  
  
  
  

Facebook has changed the rules yet again, this time related to logging in to Facebook from 3rd party applications. Up until now we used the web based OAuth approach for logging into Facebook since that allowed us to keep the same implementation across Android, iOS, Blackberry etc. without changing anything. However, the new Facebook 2 factor authentication is a very complex process and Facebook really went overboard with a very elaborate mobile API. 

  
So we had no choice but to integrate the native Facebook SDK when necessary (you will not “pay for it” if you don’t use it). This is currently in beta so the API is very likely to change which is why we are not docume  
  
nting it yet by writing a proper tutorial. This isn’t essential yet since the current Facebook API is still working and well documented. If you would like to take a look its within the social package, we would appreciate feedback, ideas and use cases.

  
On a different issue the just released version of the plugin addresses some issues with text input in the simulator (and Java SE port)  
  
related to  
[  
this issue  
](http://code.google.com/p/codenameone/issues/detail?id=957)  
. The crux of the issue lies in the usage of the highly broken AWT text field. Up until now we wanted to transition to the Swing text field but had serious flickering issues. I was finally able to resolve these issues in the latest version which will hopefully improve the simulators behavior in this regard.  
  
  
We also made some changes to reduce the flickering of the native Webkit when interacting with JavaFX, this is a huge step towards producing a robust desktop app port in the future.  

  
We had a lot of complaints and issues with the Google groups support forums, this is a huge problem but in the past most other solutions were either high maintenance or SPAM magnets. While we will probably keep and support the Google groups until Google actually kills the product (which might be next week with those guys) we are considering a Facebook group as a secondary support option.  
  
  
  
However, we will only do that if there is community interest in this so if you would like a Facebook group and promise to join then just vote below.   
  
  

* * *

  
Loading…  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — November 20, 2013 at 12:57 pm ([permalink](/blog/facebook-better-input-improving-community-support/#comment-22010))

> Anonymous says:
>
> Can you elaborate on this line: (you will not “pay for it” if you don’t use it) 
>
> Any idea on how much this will cost to use?
>



### **Anonymous** — November 20, 2013 at 12:59 pm ([permalink](/blog/facebook-better-input-improving-community-support/#comment-24254))

> Anonymous says:
>
> In size not financially 😉 
>
> It won’t increase your app size by bundling in the Facebook SDK if you don’t use it. I should be clearer on those things.
>



### **Anonymous** — November 20, 2013 at 11:51 pm ([permalink](/blog/facebook-better-input-improving-community-support/#comment-21746))

> Anonymous says:
>
> Um, believe it or not – not every developer has a facebook account but almost all of them are in linkedin – linkedin groups may make more sense.
>



### **Anonymous** — November 21, 2013 at 2:46 am ([permalink](/blog/facebook-better-input-improving-community-support/#comment-21789))

> Anonymous says:
>
> We already have a LinkedIn group that isn’t very active. Its sort of a control to see whether a potential Facebook group will see more activity.
>



### **Anonymous** — November 21, 2013 at 5:27 am ([permalink](/blog/facebook-better-input-improving-community-support/#comment-21804))

> Anonymous says:
>
> I have all Facebook domains mapped to 0.0.0.0 in my hosts file because I especially DON’T want those buggers tracking me all over the Net. 
>
> Google Groups seem to work OK. 
>
> You could always use Usenet 🙂
>



### **Anonymous** — November 23, 2013 at 5:17 am ([permalink](/blog/facebook-better-input-improving-community-support/#comment-22055))

> Anonymous says:
>
> I know spam is an issue when creating a forum. It would be nice to have things more organized with sub sections, especially if it was accessible using the Tapatalk app. Google Groups mobile website is an absolute pain and I enjoy reading while away from my computer. 
>
> It may be plausible to have a password generated from within codenameone settings in the IDE or something along those lines to access the forum, that way people who are actually using codenameone would get the password as that is the majority of users in the forum anyway, that may eliminate a lot of spamming. That way you could have a real forum site like XDA with multiple sections and would be much easier to find already asked and answered questions.
>



### **Anonymous** — November 23, 2013 at 5:29 am ([permalink](/blog/facebook-better-input-improving-community-support/#comment-21873))

> Anonymous says:
>
> Actually Google groups introduced the ability to use categories which is pretty similar to proper sections. That would be very useful. 
>
> However, they neglected a proper migration path so if we add categories all our existing posts mostly disappear and you won’t see them under any category. 
>
> An admin can start going over messages and manually categorizing them one by one, however that’s obviously tedious and painful.
>



### **Anonymous** — November 27, 2013 at 8:55 am ([permalink](/blog/facebook-better-input-improving-community-support/#comment-21891))

> Anonymous says:
>
> Go for project management system like Redmine. Then everything will be in one place: issue tracking, wiki, forum, etc. Easy to make relations, and build valuable documentation extended by tips & trick shared by community on forum. Keeping those things separated and using multiple providers does not help…
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
