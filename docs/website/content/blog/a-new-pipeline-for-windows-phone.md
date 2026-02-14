---
title: A New Pipeline For Windows Phone
slug: a-new-pipeline-for-windows-phone
url: /blog/a-new-pipeline-for-windows-phone/
original_url: https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html
aliases:
- /blog/a-new-pipeline-for-windows-phone.html
date: '2014-02-02'
author: Shai Almog
---

![Header Image](/blog/a-new-pipeline-for-windows-phone/a-new-pipeline-for-windows-phone-1.png)

  
  
  
  
![Picture](/blog/a-new-pipeline-for-windows-phone/a-new-pipeline-for-windows-phone-1.png)  
  
  
  

The Windows Phone port is one of our most painful ports, the platforms is so fragmented, volatile and rigid its remarkably hard to extract a common porting layer that will satisfy our requirements. We’ve just updated our servers with the 3rd port we did for Windows Phone, its experimental so its off by default, to activate it just use win.newPipeline=true in your build arguments.  
  
  
  
  
As you may recall our first port targeted Windows Phone 7.5 and used XNA which Microsoft killed with Windows Phone 8.  
  
  
Our second port was  
  
  
  
silverlight based and tried to dynamically create a scene-graph structure to match the graphics we are drawing in code, this is a very “imaginative” approach and it worked for most cases but had a lot of issues worst of which was very bad graphics performance and paint artifacts that were very hard to fix. 

The third approach takes a very different direction, we effectively create a writeable bitmap object and draw onto a huge int array representing the screen. This means we draw everything. Our initial attempt at this tried to use silverlight for this but this performed very badly, so we ported Pisces to C# and use that to some degree to get basic graphics primitives and image blitting working on the platform. This isn’t the best approach in terms of performance but its better than what we have now and might be good enough for now. If this pans out we can always port some of the low level code to direct x using the basic API’s we already implemented.  
  
  
  
  
Please try this new port and let us know both here and in the discussion forum how it affects your application.  
  
  
  
  
  
  
This was quite a bit of work that took a lot of effort we have a pretty large pipeline of tasks ahead of us both in terms of bug fixes and pending features, so if a particular issue you filed wasn’t addressed please bare with us as we work thru the backlog. Its OK to remind us occasionally since things sometimes do fall thru the cracks.  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — February 4, 2014 at 8:25 am ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-21435))

> Anonymous says:
>
> Still some minor bugs but way better than the previous support. Keep up the good work !


### **Anonymous** — April 28, 2014 at 10:27 am ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-21837))

> Anonymous says:
>
> So far my development under WP has been going well, and I’m making a game. Performance wise, it’s “playable”, maybe not the best but I am using a low-end device for debugging. 
>
> Overall it’s a good port, but better performance through through directX would be nice! 🙂
>



### **Anonymous** — April 28, 2014 at 12:28 pm ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-21718))

> Anonymous says:
>
> Unfortunately I don’t think we will be able to use DirectX. The problem is that when you use DirectX we can’t use text and we can’t use widgets (e.g. TextInput), it might be possible to achieve something like that but it seems rather difficult in comparison to other platforms.
>



### **Anonymous** — April 30, 2014 at 5:59 am ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-21725))

> Anonymous says:
>
> No worries, the latest build of the game runs smooth on low-end devices! Had to improve some GC logic etc for the game. 
>
> It really does show how powerfull CodenameOne is, great job guys!
>



### **Anonymous** — July 24, 2014 at 4:28 am ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-22104))

> Anonymous says:
>
> Hi! 
>
> Is the win.newPipeline=true now on by default? I tried to build with and without it and the build seamed to generate the same .xap file.
>



### **Anonymous** — July 24, 2014 at 2:13 pm ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-21677))

> Anonymous says:
>
> Hi, 
>
> yes its the default. You can set it to false to disable it.
>



### **Anonymous** — October 30, 2014 at 5:57 am ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-21679))

> Anonymous says:
>
> Hello, has further improvement/updates been made since? Also what are the current limitations to this port compared to Android/IOs?
>



### **Anonymous** — October 30, 2014 at 9:42 am ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-21950))

> Anonymous says:
>
> The port is far inferior and there was no progress to speak of. From talking to our enterprise/corporate subscribers it seems they only want Windows Phone as a checklist feature and prefer we invest our efforts on the iOS/Android fronts.
>



### **Anonymous** — October 30, 2014 at 11:18 am ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-22290))

> Anonymous says:
>
> Thanks for the feedback. I am seeing WP gaining momentum and with Windows Phone 10, it might gain more in the near future. Its a pity CN1’s direction is currently dictated by current paying customers since your initial ethos was to create a toolchain that enables developers to write code once that would run on most major platforms. I don’t blame you Shai, but it would be so nice to see good support for at least these three major platforms, which might attract more paying customers. Some colleagues of mine are advising me to invest development in web apps which are more portable for major platforms in the long run.
>



### **Anonymous** — October 30, 2014 at 9:56 pm ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-22301))

> Anonymous says:
>
> Our direction was always dictated by paying users as we repeatedly stated in the discussion forum. 
>
> Windows phone isn’t growing anywhere and got 3 total rewrites so far because of Microsoft’s flakiness. 
>
> Try getting a non-trivial webapp to work on the mobile version of internet explorer not to mention the browser changes made in the Android 4.x branch which have no workarounds available then talk about the “portability of the web” nonsense. Anyone who says web is portable didn’t actually use it professionally on a wide scale.
>



### **Anonymous** — October 31, 2014 at 6:03 am ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-21799))

> Anonymous says:
>
> Shai, when people look at a product offered, they usually look at the company’s first web page (at least that the case with me), which states very clearly your support for major platforms such as Android, IOs, BlackBerry and Windows Phone so forgive me for not looking at your discussion forums about how they are ‘really’ supported and at what level. It initially sounds to me you support everything quite well to some extent until I read this blog article of yours. 
>
> Agreed, Windows Phone may not have gained much traction until now, but in the country where I live, I see more people using it than iPhones surprisingly, as it is the case in some parts of Europe. I don’t understand your comment about blaming Microsoft for your support hindrance, but it would be disingenuous to blame Microsoft if you are unable to make it work properly with your platform’s architecture. 
>
> My friend has a company that writes web apps quite successfully for various clients, both big and small. So yes, it all depends on the nature of the app, but your comment is a little strong about people that still write web apps. 
>
> Looking at your pricing package I would have loved to pay as a corporate or enterprise customer if you support Windows Phone quite well. Do you support BB10? I reckon your customers are using the Android port for running on that platform. Going back to the main discussion, even if I am a paying customer, I won’t have the guarantee you will support Windows Phone platform because the majority of your paying customers don’t want it as a main feature (until they decide to) and I cannot compete with the majority which is my main concern and its difficult for me to invest money into your services with this uncertainty. Thanks and all the best to you, Jan.
>



### **Anonymous** — October 31, 2014 at 8:47 am ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-21822))

> Anonymous says:
>
> If you were a corporate customer it would be financially viable for us to take the 3-6 man months of engineering effort required to rewrite the Windows Phone port. 
>
> We do give corporate/enterprise customers the guarantee that their priority use cases will be properly supported. Obviously the effort required for Windows Phone is much bigger than most of the other tasks we face mostly due to inherent design choices made by MS. 
>
> Just so I’m clear about the word majority, we have a few pro customers who want Windows Phone, but no Enterprise/Corporate users who are interested in it. 
>
> We have a couple of pro users to whom BB10 is important but they are satisfied with the option of using the APK and we try to keep the chief functionality level there working with BB10.
>



### **Anonymous** — December 27, 2014 at 6:32 pm ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-22249))

> Anonymous says:
>
> Hi, 
>
> is there any progress in developing Apps for Win 8.x Phones? 
>
> What is about ‘packing’ a wep app core within codenameone? 
>
> is there any help article available for signing the app? 
>
> Thanks, Jan
>



### **Anonymous** — December 28, 2014 at 4:44 am ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-22042))

> Anonymous says:
>
> Hi, 
>
> there is no need/option to sign for Windows Phone. MS signs on its own and since Windows Phone doesn’t have an OTA install option (without the store) there is no real need. Currently none of our enterprise customers have made a formal request to justify the effort on Windows Phone. We support desktop development which should work well for the more popular Windows platforms such as the Surface Pro.
>



### **Anonymous** — February 14, 2015 at 8:32 pm ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-24177))

> Anonymous says:
>
> Hi Shai, 
>
> i tried to build a very simple app and deploy it to my 
>
> Lumia 630 phone by clicking on the *.XAP File uploaded 
>
> on OneDrive. 
>
> It says something like (in german): 
>
> This enterprise app cannot be installed… 
>
> Do i have to sign it with windows phone sdk? 
>
> I understand your last answer in the way that 
>
> this is not necessary / possible ? 
>
> Thanks for an answer, 
>
> Jan
>



### **Anonymous** — February 15, 2015 at 5:21 am ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-22102))

> Anonymous says:
>
> Windows Phone is the only “modern” mobile OS that doesn’t support installing apps over the air (or by click) only via cable sync or thru the store beta test process.
>



### **Anonymous** — February 23, 2015 at 8:52 pm ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-24196))

> Anonymous says:
>
> Hi Shai, 
>
> sorry, but no success: 
>
> – I tried to install the app via SD card, 
>
> – it does not appears in the App “Store” 
>
> – clicking on it in WP 8 file manager doesn’t work 
>
> – i try to build it actuallay again (24-02-2015) 
>
> the message stays the same (… cannot be installed) 
>
> Oliver 
>
> P.S. 
>
> The same code (simple list of buttons) runs even 
>
> on my very old sony ericsson k800i as j2me app… 
>
> any idea?
>



### **Anonymous** — February 24, 2015 at 3:45 am ([permalink](https://www.codenameone.com/blog/a-new-pipeline-for-windows-phone.html#comment-22168))

> Anonymous says:
>
> See [http://www.codenameone.com/…](<http://www.codenameone.com/blog/installing-on-a-windows-phone-device>)
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
