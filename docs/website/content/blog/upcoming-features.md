---
title: Upcoming Features
slug: upcoming-features
url: /blog/upcoming-features/
original_url: https://www.codenameone.com/blog/upcoming-features.html
aliases:
- /blog/upcoming-features.html
date: '2013-12-29'
author: Shai Almog
---

![Header Image](/blog/upcoming-features/upcoming-features-1.png)

  
  
  
  
![Picture](/blog/upcoming-features/upcoming-features-1.png)  
  
  
  

December has been pretty busy with the release of 2.0 but January will probably be MUCH busier since we have a major backlog of features requested by enterprise and pro users. We are already well under way with quite a few interesting features but most of them won’t be landing for the next couple of weeks since we need the trunk to remain stable for now. 

Before I get started, if you are interested in Socket support for Codename One and native interface’s you should check out the work  
[  
Steve Hannah  
](http://sjhannah.com/)  
did on a  
[  
socket library  
](https://github.com/shannah/CN1Sockets)  
for Codname One. Its also a great reference implementation on how one would write a native library implementation!

We have a separate socket implementation of our own which has a somewhat different approach, we should be releasing it at some point before 2.1 and it is on our roadmap for the next couple of months. Currently we plan to only support TCP streams and we might support server sockets on select platforms (currently iOS server socket isn’t planned).

We are doing quite a bit of work for our enterprise customers, we already completed the effort for two major requests. The first asked for improved text input in iOS where today we always fold the virtual keyboard when a user finished editing the text field. In fact we have an  
[  
issue  
](http://code.google.com/p/codenameone/issues/detail?id=361)  
which we tried to resolve in the past and deemed it to be impossible to fix. Well… Few things are impossible when an enterprise customer makes a formal request so we spent quite a few days trying to resolve this, after the next server update you should be able to see the fruits of this labor by defining the build argument ios.keyboardOpen=true which will keep the keyboard always open when you start editing unless you press the “Done” button.  
  
As part of this work we also added a feature to text area that allows it to grow up to a limit so using textArea.setGrowLimit(int) will limit the amount of rows to which the text area grows based on user input.

Another enterprise developer needed to detect a disconnect of headphones which is pretty niche but we added that as well. Starting with the next server update you should be able to define the build arguments ios.headphoneCallback=true and android.headphoneCallback=true  
  
Once those are defined you should also define the callback methods with these signatures in your main class:  
  
public void headphonesDisconnected() {  
  
}

public void headphonesConnected() {  
  
}

They should be invoked based on headphone state.

We will also be releasing support for desktop builds, this support will be limited to pro users since a typical desktop app includes the full JVM within it making it a hefty download that puts a toll on our servers. You would effectively get a Mac OS DMG or a Windows MSI/EXE to install a Codename One app that would include pretty much everything you would expect. We’ll write a more detailed tutorial when that functionality launches.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — December 31, 2013 at 11:45 am ([permalink](https://www.codenameone.com/blog/upcoming-features.html#comment-21703))

> Anonymous says:
>
> Regarding PC/MAC builds, am disappointed it is Pro-only, and I suspect there is a sweet spot somewhere between Basic and Pro. $70 per month more for this single and very attractive feature is too much to take. Would love an option for the do-it-yourself crowd.
>



### **Anonymous** — December 31, 2013 at 4:37 pm ([permalink](https://www.codenameone.com/blog/upcoming-features.html#comment-21922))

> Anonymous says:
>
> We were asked that a couple of times. Unfortunately the features that are important to you are not those that are important to other people and so forth. 
>
> Pro & enterprise users are what keeps us in business and they effectively subsidize basic & free accounts. We ran the numbers on providing a cheaper account in the middle between pro/basic and it just won’t add up.
>



### **Anonymous** — January 24, 2014 at 6:20 am ([permalink](https://www.codenameone.com/blog/upcoming-features.html#comment-21892))

> Anonymous says:
>
> Too bad. I could use the the pro account myself but it is too expensive for amateur developer. I would love if you consider it again in the future.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
