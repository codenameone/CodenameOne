---
title: You Can Bet On It
slug: you-can-bet-on-it
url: /blog/you-can-bet-on-it/
original_url: https://www.codenameone.com/blog/you-can-bet-on-it.html
aliases:
- /blog/you-can-bet-on-it.html
date: '2013-08-04'
author: Shai Almog
---

![Header Image](/blog/you-can-bet-on-it/hqdefault.jpg)

  

Recently the guys from Software Developer Journal contacted us about writing a  
[  
mobile gaming article  
](http://sdjournal.org/game-development-how-to-become-a-millionaire-preorder/)  
, since quite a few Codename One developers use it to write games we decided to accept the offer and I wrote an article titled: “Writing casual games In Java for mobile devices”. In this article we create a simple poker game mockup that is fully functional (although it doesn’t include betting, AI or networking), but you can drag the cards to have them replaced and click the deck to deal new cards. 

  
The game took 270 lines of code (including imports,  
  
its a bit larger now since I commented the code quite a bit  
  
), it took me roughly 2 hours to write that code.  
  
The issue will be published in the 19th of August but you can  
[  
preorder it here  
](http://sdjournal.org/game-development-how-to-become-a-millionaire-preorder/)  
. If there is major interest in this I can expand the demo a bit more to include additional features.

  
As a teaser I took a very unique approach to writing the game, rather than use graphics I used components and leveraged the resolution independence of Codename One as much as possible.  

  
BTW I understand that a  
[  
Yaniv Nitzan of SOOMLA  
](http://soom.la/)  
who wrote a  
[  
popular guest post  
](http://www.codenameone.com/3/post/2013/03/5-tips-for-gamifying-your-mobile-app.html)  
here will also be  
[  
featured in this issue  
](http://sdjournal.org/game-development-how-to-become-a-millionaire-preorder/)  
.  
  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — March 13, 2015 at 7:05 am ([permalink](https://www.codenameone.com/blog/you-can-bet-on-it.html#comment-22313))

> Anonymous says:
>
> <div class=”blogCommentText”>
>
> Wow, CN1 is becoming more and more versatile all the time! I’d be curious to know if you had to bend some parts in unexpected ways to make this work…any new animations? a new layout manager? I don’t suppose so since you mentioned it took only a handful of lines of code… After trying the “components” approach instead of directly painting stuff yourself, is this something you would recommend for other casual games? This could open up a whole new family of CN1 apps! 
>
> Good job Shai! 
>
> </div>


### **Anonymous** — March 13, 2015 at 7:05 am ([permalink](https://www.codenameone.com/blog/you-can-bet-on-it.html#comment-22321))

> Anonymous says:
>
> <div class=”blogCommentText”>
>
> Thanks. 
>
> Not really. I did make one small change that all game developers using Codename One asked: I added the ability to open a resource file based on a specific DPI. This is useful in overriding the default Codename One behavior for DPI. For game development you expect tablets to act somewhat differently. 
>
> All the logic is standard Codename One animations and action listeners. The dragging is just the builtin drag and drop support and the layouts are just border layout, layered layout, grid layout and BoxLayout X. Nothing unique. 
>
> </div>

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
