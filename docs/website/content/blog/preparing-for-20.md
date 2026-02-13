---
title: Preparing For 2.0
slug: preparing-for-20
url: /blog/preparing-for-20/
original_url: https://www.codenameone.com/blog/preparing-for-20.html
aliases:
- /blog/preparing-for-20.html
date: '2013-11-17'
author: Shai Almog
---

![Header Image](/blog/preparing-for-20/preparing-for-20-1.png)

  
  
  
[  
![Picture](/blog/preparing-for-20/preparing-for-20-1.png)  
](/img/blog/old_posts/preparing-for-20-large-2.png)  
  
  

We are now finalizing the features for Codename One 2.0. Yes we are skipping the 1.2 revision and going up to the 2nd generation which we feel is warranted given the amount of features added since the May release of 1.1.  
  
  
We will make the release in December which we will probably spend in code freeze so you won’t see new features from us during that month that are not bug fixes or completely separate features (e.g. IDEA plugin).  

  
Most of the features for 2.0 match the current version here is what we still don’t have and hope to include in 2.0:  

  *   
InteliJ IDEA plugin  

  *   
Native Facebook SDK support (ideally with native sharing on iOS too)  

  *   
Fixes for Windows Phone issues  

There are a many issues and features which we would like to push into the release so we will try to get as many of them out there. Time permitting we would like to add a new solution for ad’s that would hopefully solve those pains once and for all.

  
Once this is out we can probably start debating on the wishlist for 2.1, feel free to he  
  
chime in in the discussion here.  
  
  
If you have an issue that you feel is particularly important and should be addressed for 2.0 please comment on that within the issue so we won’t miss it.  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — November 19, 2013 at 11:53 am ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21696))

> Anonymous says:
>
> IntelliJ IDEA plugin with 2.0 – nice! 
>
> Albeit the answers in forum concerning web deployment but if there is even a slight chance exporting to some kind of JS or even GWT so we can target easily Web – this will be ideal.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — November 19, 2013 at 3:54 pm ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21921))

> Anonymous says:
>
> We’d like to do something like this but there are some serious technical hurdles. I’m optimistic about [http://wiki.apidesign.org/w…](<http://wiki.apidesign.org/wiki/Bck2Brwsr>) but they seem adamant about not supporting threads. Maybe this an be hacked in some way without violating their VM. Maybe something like this can be hacked with GWT, either way would work for us but we need threads. 
>
> I occasionally have some thoughts about doing a threadless version of Codename One but that is a bit difficult to achieve with things such as [Dialog.show](<http://Dialog.show>) etc. being so ingrained.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — November 19, 2013 at 8:03 pm ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21812))

> Anonymous says:
>
> What would really make my life easier is the ability to put a res file containing Forms into a CN1 Library Project, then run those Forms from multiple Apps. I am developing a suite of Apps each of which have 6 or 7 common forms and its a pain to redevelop them for each App. And it will be a pain to maintain them.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — November 20, 2013 at 8:19 am ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21819))

> Anonymous says:
>
> Thanks for the feedback Steve. We are looking at some approaches to this especially with a pending rewrite of the designer tool. I’m not sure if that will make it to .2.1 though. Currently it seems that it will have to wait for the release after that.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — November 20, 2013 at 4:19 pm ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21867))

> Anonymous says:
>
> On the Windows 8 front the biggest issue for me is the Database class. Last I checked is was not there, will it make it into 2.0?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — November 21, 2013 at 2:49 am ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21720))

> Anonymous says:
>
> This won’t make it for 2.0. 
>
> The main issue with Windows Phone’s SQL support is that SQlite is only supported from the C layer and this is pretty difficult to circumvent.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — November 21, 2013 at 4:19 am ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21708))

> Anonymous says:
>
> I think the biggest issue for me is the ability to include files (res files, images, etc) from multiple folders. 
>
> I too am developing a suite of apps and have about 600 files (mostly images, sound files, etc) included in my source folder. 
>
> For every app I create I have to manually sync these folders. 
>
> Another big issue – Graphics. 
>
> I need to be able to use different width lines, anti aliasing, brushes, etc., in order to give an enriched experience. Really miss that!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — November 21, 2013 at 5:39 am ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21958))

> Anonymous says:
>
> You should use multiple resource files for this. Using files in the src dir is a recipe for portability nightmares e.g. recently a developer in our forum had an issue because she named a PNG x.1.png and it worked on Android/Simulator while failing on iOS. You get all sorts of weird stuff when working with files and file hierarchies. 
>
> You can use the new team mode of the designer tool so you can incorporate resources from your designer more quickly. 
>
> You can also sync folders as part of the build script (ant is pretty powerful). 
>
> Graphics is a HUGE issue for us, frankly I’m shocked we reached 2.0 without a complete rewrite of that. We are working hard to bring a new graphics engine to iOS (which will allow us to bring it to Android & the simulator too). The problem is that the native engine isn’t appropriate for what we need (its scene-graph) and most OpenGL based engines are geared for the needs of games. We have a project underway but there is no way it will be ready in time for the code freeze so it will have to wait for post 2.0. I’m hoping it will be ready for January but we are still in the exploratory stages (although outlook seems good).
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — November 21, 2013 at 7:28 am ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21881))

> Anonymous says:
>
> Would be nice to add push capability to windows ports.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — November 23, 2013 at 1:40 pm ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21619))

> Anonymous says:
>
> Is there a date for database support in Windows Phone 8? 
>
> Does it have to be SQLite? I think your Database class is generic enough to support another implementation, if there were one. 
>
> Now that we are out on Android and iOS all we hear is when will Windows phone support be available. Nobody is even talking about Blackberry anymore. I need to come up with a plan.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — November 23, 2013 at 2:36 pm ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-22024))

> Anonymous says:
>
> Martin, if I recall correctly you have a pro account right? Contact me thru email about this, we can discuss options/schedules etc.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — November 24, 2013 at 11:27 am ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21918))

> Anonymous says:
>
> Do the 2.0 include the bluetooth support? I will really happy if 2.0 have bluetooth support, course this is the reason my project still stay in J2ME.. 
>
> Thank you.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — November 24, 2013 at 4:48 pm ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21753))

> Anonymous says:
>
> Are you using the push capabilities in other platforms?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — November 24, 2013 at 4:53 pm ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21981))

> Anonymous says:
>
> That’s currently not slated. We might do something small but right now most of our pro or enterprise subscribers didn’t ask for this and its a good thing because its REALLY hard (I think one might have mentioned it at some point but not sure about that either). 
>
> Bluetooth is implemented completely differently between OS’s, PhoneGap which has been around quite a while still doesn’t have a bluetooth plugin after all these years. So its pretty hard to abstract it in a portable way.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — November 25, 2013 at 5:36 am ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21966))

> Anonymous says:
>
> On Android and iOs but not on CodenameOne. Thats why I want to port my products to use CodenameOne next year to support more platforms (Blackberry 10 previous and Windows 7.5 – 8) but sad to see its not supported on Windows. In fact, Windows port is quite immature at this stage compared to your other ports if I am not mistaken. As for BB10, I reckon its your Android port that will be running on there? Will there be a BB10 native port in the near future?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — November 25, 2013 at 9:57 am ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-24160))

> Anonymous says:
>
> None of our pro/enterprise customers asked for Windows Phone push so it isn’t there and so far isn’t planned. This can obviously change based on request from enterprise customers or several pro customers. 
>
> We won’t support the BB10 directly but our Android port works well on it (we have a couple of pro users for whom this is important so we made sure of that). The process is a bit awkward but it works.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — December 10, 2013 at 10:10 pm ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21990))

> Anonymous says:
>
> Do you have plans for including Socket support?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — December 10, 2013 at 10:11 pm ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21758))

> Anonymous says:
>
> Do you have plans for including Socket support on this release?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — December 11, 2013 at 9:34 am ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21897))

> Anonymous says:
>
> We do have it in our wishlist for 2.1. It won’t be java.net.Socket but we do want to offer simple TCP client/server sockets.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — January 6, 2014 at 7:00 am ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21790))

> Anonymous says:
>
> Hi, 
>
> My entreprise is planning to build mobile app using bluetooth. It’ll be great if you can include a support for that. Let us know when it’ll be done, so we’ll get an entreprise subscription. 
>
> Regards
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — January 6, 2014 at 8:00 am ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21721))

> Anonymous says:
>
> Sorry for some reason I didn’t answer this. Odd. 
>
> Sockets are landing soon.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — January 6, 2014 at 8:04 am ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21709))

> Anonymous says:
>
> Hi Kenny, 
>
> it works the other way around. Get an enterprise subscription then we will implement the features you need. 
>
> We get a lot of similar requests like that for various features and its impossible for us to separate the serious customers especially thru the internet. 
>
> Right now we are busy implementing the priorities of our existing enterprise subscribers none of which asked for bluetooth so this won’t get done for 2.1 either unless you actually purchase and make an official request.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — February 20, 2014 at 11:21 am ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21717))

> Anonymous says:
>
> Hi Shai, 
>
> I’m not very sure so can you confirm that the release 2.0 of Codenameone is available ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)


### **Anonymous** — February 20, 2014 at 2:40 pm ([permalink](https://www.codenameone.com/blog/preparing-for-20.html#comment-21829))

> Anonymous says:
>
> For ages. Plugin version != general Codename One version.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreparing-for-20.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
