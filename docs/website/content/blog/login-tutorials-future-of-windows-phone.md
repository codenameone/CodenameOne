---
title: Login Tutorials & Future Of Windows Phone
slug: login-tutorials-future-of-windows-phone
url: /blog/login-tutorials-future-of-windows-phone/
original_url: https://www.codenameone.com/blog/login-tutorials-future-of-windows-phone.html
aliases:
- /blog/login-tutorials-future-of-windows-phone.html
date: '2015-06-30'
author: Shai Almog
---

![Header Image](/blog/login-tutorials-future-of-windows-phone/google-sign-in.png)

Facebook & Google login have been a source of a bit of pain mostly because of the obtuse and confusing  
documentation from both companies. Chen spent some time writing up tutorials for both  
[Facebook Login](/how-to-integrate-facebook-login-with-codename-one/) & [Google Login](/google-login.html)  
that should help you get started with applications that use such login options. 

#### Future Of Windows Phone

I’ve been holding back on writing this before we have further information and can properly asses the situation. However… since  
MS is taking its time we’d rather discuss the future of Windows Phone once rather than all over the different forums/social and support channels.  
As you know we wrote roughly 3 different Windows Phone ports, the first targeted Windows Phone 7.5 (mango) which  
relied on functionality introduced in that version and immediately killed in 8.0! 

We should have taken the hint that MS isn’t serious about Windows Phone back then but we made a second attempt  
for support with a port to Windows Phone 8. This port used the official API’s but they weren’t very suitable for a tool  
like Codename One and this performed really badly, it had horrible bugs to boot.  
The third attempt which is now live uses Sun’s Pisces framework to abstract graphics which is a real hack, it  
“works” but is pretty limited and still performs poorly. 

We were thinking about a complete rewrite on top of DirectX for quite some time, however such an effort would require  
a huge expense in time and resources on something that none of our enterprise customers considered crucial.  
A few pro users asked for that (less than 4 if I recall correctly) but that can’t justify what is effectively close to a 100K USD  
expense in time and effort. 

For quite a while there were rumors that Windows will feature Android compatibility and MS recently announced  
[just that](https://dev.windows.com/en-us/uwp-bridges/project-astoria).  
We don’t yet know if it will work with Codename One but it should be relatively easy to support Windows devices  
using a compatibility layer such as this and effectively skip a lot of the complexity.   
Since this technology isn’t out yet and we don’t know what will be required from us to be compatible we can’t  
make any guarantees or even assumptions on how/whether this will work. However, this does look like the best  
way to support Windows Phone and make the port far more maintainable/performant/feature complete. 

The only downside to this is that this will probably only work on Windows 10 and newer devices. With the  
JavaScript port you would still be able to target older devices if needed.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Hitesh Rawtani** — July 10, 2015 at 4:22 am ([permalink](/blog/login-tutorials-future-of-windows-phone/#comment-22202))

> Hitesh Rawtani says:
>
> Hi Shai, first of all thank you for this wonderful framework. I used to use LWUIT for cross platform java mobile apps and CodenameOne looks fantastic for the different range of mobile platforms we have today.
>
> I was trying to integrate google login in one of my apps but didn’t see the GoogleConnect class to be available. Does that part of code go into the native implementation for each platform?
>



### **Shai Almog** — July 10, 2015 at 2:05 pm ([permalink](/blog/login-tutorials-future-of-windows-phone/#comment-22274))

> Shai Almog says:
>
> We need to update the plugin version to include these new features. It should be out next week.
>



### **Hitesh Rawtani** — July 11, 2015 at 6:42 am ([permalink](/blog/login-tutorials-future-of-windows-phone/#comment-22082))

> Hitesh Rawtani says:
>
> Great. I look forward to the update.
>



### **J.C** — July 23, 2015 at 11:36 am ([permalink](/blog/login-tutorials-future-of-windows-phone/#comment-22394))

> J.C says:
>
> It looks like MS is only supporting Web Apps and Xamarin for cross platform compatibility and not native Android Java Apps in Visual Studio 2015 which is a let down. This means CodenameOne will not be able to run properly on newer Win10 devices or even older devices. Not sure how good is the JavaScript version, but I think native is much better. Too bad really.
>



### **Fabrício Cabeça** — July 30, 2015 at 11:33 am ([permalink](/blog/login-tutorials-future-of-windows-phone/#comment-21602))

> Fabrício Cabeça says:
>
> I agree, that’s why I decided to write a windows port myself, it is still under heavy development but I can’t see anything that can’t be accomplished right now and uses WinRT which means it will work with the universal apps model. I don’t blame the cn1 team, Microsoft really didn’t help constantly changing frameworks and not giving basic graphics support.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
