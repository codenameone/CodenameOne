---
title: Fast Push Communication Using PubNub
slug: fast-push-communication-using-pubnub
url: /blog/fast-push-communication-using-pubnub/
original_url: https://www.codenameone.com/blog/fast-push-communication-using-pubnub.html
aliases:
- /blog/fast-push-communication-using-pubnub.html
date: '2013-11-23'
author: Shai Almog
---

![Header Image](/blog/fast-push-communication-using-pubnub/fast-push-communication-using-pubnub-1.jpg)

  
  
  
  
![Picture](/blog/fast-push-communication-using-pubnub/fast-push-communication-using-pubnub-1.jpg)  
  
  
  

This came out 5 months ago but the guy from  
[  
PubNub  
](http://www.pubnub.com)  
didn’t update us on the status so we just didn’t notice this until now. That’s probably a good sign of us being too busy. There is now official PubNub integration for Codename One, which means you can get push like fast 2 way communications between devices without writing too much code. 

  
How does it work?  
  
  
  
PubNub has CDN like operations around the world, this means they can keep a high performance channel to your devices and send peer to peer or server/peer messages REALLY fast.  

  
How is this different from just using Push notification?  
  
  
  
These aren’t separate ideas, push is generally good if the application isn’t running. Its also good for very small messages and provides no delivery guarantees. So using push for a chat application might not be the right thing but using PubNub for that makes perfect sense.  

  
There is now a  
[  
Codename One SDK from PubNub  
](http://www.pubnub.com/docs/java/codenameone/codenameone-sdk.html)  
and  
[  
a tutorial  
](http://www.pubnub.com/docs/java/codenameone/tutorial/data-push.html)  
  
related to the SDK that should help you get started right away and even a  
[  
quick start tutorial  
](http://www.pubnub.com/docs/java/codenameone/tutorial/quick-start.html)  
. Normally we write a tutorial for things such as these but the guys at PubNub did such a good job at making this really simple, there is really no need to do so. Check out their work and let us know what you think.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — April 22, 2014 at 9:28 am ([permalink](https://www.codenameone.com/blog/fast-push-communication-using-pubnub.html#comment-22003))

> Anonymous says:
>
> Just spent a day looking at this finally, its kind of tough to get started for the first couple of hours but after that its plain sailing, made a whatsapp clone in 7 hrs that runs on all mobiles, thanks to codenameone 😀 Im amazed pubnub have put together such a decent and problem free API and extremely bloody thankful.
>



### **Anonymous** — September 6, 2014 at 8:07 pm ([permalink](https://www.codenameone.com/blog/fast-push-communication-using-pubnub.html#comment-24239))

> Anonymous says:
>
> Please someone help me out,I don’t know where to start in pubnub
>



### **Anonymous** — September 7, 2014 at 3:00 am ([permalink](https://www.codenameone.com/blog/fast-push-communication-using-pubnub.html#comment-21878))

> Anonymous says:
>
> Did you go thru the tutorial on their website?
>



### **Anonymous** — September 7, 2014 at 8:31 am ([permalink](https://www.codenameone.com/blog/fast-push-communication-using-pubnub.html#comment-21733))

> Anonymous says:
>
> I did but not understanding anything there.
>



### **Anonymous** — September 7, 2014 at 1:31 pm ([permalink](https://www.codenameone.com/blog/fast-push-communication-using-pubnub.html#comment-22040))

> Anonymous says:
>
> This [http://www.pubnub.com/docs/…](<http://www.pubnub.com/docs/java/codenameone/tutorial/quick-start.html>) seems really simple. Do you have any Java programming experience?
>



### **Anonymous** — September 18, 2014 at 5:09 am ([permalink](https://www.codenameone.com/blog/fast-push-communication-using-pubnub.html#comment-22198))

> Anonymous says:
>
> Helo Gareth,am jude.Am trying to create a mobile chat application for my project.I want to create something similar to whatsap and skype for android,but am not sure how to go about this.Please can you give me tips on how to do this project.Thanks in advance
>



### **Linsong Wang** — December 19, 2016 at 8:08 pm ([permalink](https://www.codenameone.com/blog/fast-push-communication-using-pubnub.html#comment-23161))

> Linsong Wang says:
>
> Pubnub is deprecating 3.x version, and 4.x is the recommended now. Any plan to upgrade corresponding cn1lib?
>



### **Shai Almog** — December 20, 2016 at 5:28 am ([permalink](https://www.codenameone.com/blog/fast-push-communication-using-pubnub.html#comment-23277))

> Shai Almog says:
>
> We didn’t build that cn1lib. It was built by pubnub. I think there are no technical issues in porting it to the new API as Codename One supports sockets/websockets and allows any form of communication including native interfaces.
>
> However, I don’t think they see enough paying user engagement from Codename One so if you rely on their service you need to write to them and ask for this.
>
> On our side we’d like to offer a better webservice oriented tool that will work with websockets for efficient universal communications. There is no current ETA for this though as this is still on the drawing board.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
