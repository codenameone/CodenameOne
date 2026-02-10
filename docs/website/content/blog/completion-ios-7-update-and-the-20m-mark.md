---
title: Completion, iOS 7 Update And The 20M Mark
slug: completion-ios-7-update-and-the-20m-mark
url: /blog/completion-ios-7-update-and-the-20m-mark/
original_url: https://www.codenameone.com/blog/completion-ios-7-update-and-the-20m-mark.html
aliases:
- /blog/completion-ios-7-update-and-the-20m-mark.html
date: '2013-08-27'
author: Shai Almog
---

![Header Image](/blog/completion-ios-7-update-and-the-20m-mark/completion-ios-7-update-and-the-20m-mark-1.png)

  
  
  
[  
![Picture](/blog/completion-ios-7-update-and-the-20m-mark/completion-ios-7-update-and-the-20m-mark-1.png)  
](/img/blog/old_posts/completion-ios-7-update-and-the-20m-mark-large-2.png)  
  
  

Exciting news this week, Chen FINALLY did what we procrastinated on for so long and wrote an auto-complete text field! 

  
This is really easy to incorporate  
  
into your code, just replace your usage of TextField with AutoCompleteTextField and define the data that the auto complete should work from. There is a default implementation that accepts a String array or a ListModel for completion strings, this can work well for a “small” set of thousands (or tens of thousands) of entries.  
  
  
  
  
  
However, if you wish to query a database or a web service you will need to derive the class and perform more advanced filtering by overriding the  
  
filter method and the getSuggestionModel method. You might also need to invoke updateFilterList() if your filter algorithm is asynchronous.

  
Here is a sample of a simple auto-complete that doesn’t use the advanced features:  
  

* * *

As part of that feature Chen also added something very interesting to Form… Up until now the best way to draw on top of a Form was to create a glass pane create your own layered layout for the internal content pane.  
  
  
Chen added a cool new method to form:  
  
getLayeredPane(). 

  
Essentially a layered pane sits on top of your content pane and you can just place any component you want there just like you would in the content pane. Only difference is that it will be drawn on top of the content pane. Unlike the glasspane which is a drawing API and lacks interactivity the layered pane allows you to place pretty much any component there.  
  
  

**  
iOS 7 Update  
**  
  
According to the latest rumors Apple is about to release iOS 7 by September 10th, so here is a quick update on where we stand and what you need to know moving forward.

  
Just so we are 100% clear, all current builds work on iOS 7 devices. We have already migrated our devices to the latest beta’s and have verified that builds keep working!  

  
However, iOS 7 is a major departure in terms of design and some application behaviors. The biggest change is the move to  
[  
flat design  
](http://sachagreif.com/flat-pixels/)  
, Tope has a great blog post  
[  
detailing some of the migration efforts  
](http://www.appdesignvault.com/ios-7-update/)  
from a designers perspective.  

  
You can already update your user interface and theme to be more flat and modern (as well as your icon design etc.).  
  
  
  
We would ideally update some of our themes to match those expectations.

  
As people start migrating to OS 7 you might want to make your app OS 7 only and thus take advantage of the taller title area (the title area in OS 7 stretches behind the battery/time/network indicator) as well as some minor OS 7 tweaks and the default OS 7 look. To do that you will need to have your application compiled with XCode 5 which is currently in beta. We already tested this process and we will allow a build argument to use XCode 5 once the tools are released from beta. Naturally, our recommendation is that you do not use these tools until most users migrate (which I assume will be slow given how some people feel about iOS 7).  
  
  
  
  
  
**  
  
Traction, Testimonials & Trips  
  
**  
  
  
  
You might have noticed the big counter of installed devices we added to the home page, we are pretty thrilled having crossed the 20M device install base. Now lets work on adding a couple of zeros there…

  
We also added a testimonials popup to the home page (commented out due to an issue in Firefox), if you have a testimonial you would like to contribute to appear on our home page and an impressive public linked in profile we would really appreciate it  
  
if you take the time to  
[  
write something about us  
](/contact-us.html)  
(please include the linked in public profile link).

  
I will be speaking at both JavaZone and JavaOne (NetBeans day) this year. So I will be in both Oslo and San Francisco, if any of you are coming to these conference or live in the area I’d love to meet, just contact us via the  
[  
contact form  
](/contact-us.html)  
.  
  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — August 29, 2013 at 3:45 am ([permalink](https://www.codenameone.com/blog/completion-ios-7-update-and-the-20m-mark.html#comment-21745))

> Anonymous says:
>
> just thought you should know, that the title to this post is different, from the title on the front page of the website
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcompletion-ios-7-update-and-the-20m-mark.html)


### **Anonymous** — August 29, 2013 at 3:50 am ([permalink](https://www.codenameone.com/blog/completion-ios-7-update-and-the-20m-mark.html#comment-21788))

> Anonymous says:
>
> Thanks Kevin! 
>
> The dangers of Copy & Paste…
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcompletion-ios-7-update-and-the-20m-mark.html)


### **Anonymous** — September 11, 2014 at 1:25 pm ([permalink](https://www.codenameone.com/blog/completion-ios-7-update-and-the-20m-mark.html#comment-22224))

> Anonymous says:
>
> Is there any setModel method to pass the autocomplete data other than putting it in the contructor
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcompletion-ios-7-update-and-the-20m-mark.html)


### **Anonymous** — September 12, 2014 at 6:12 am ([permalink](https://www.codenameone.com/blog/completion-ios-7-update-and-the-20m-mark.html#comment-22061))

> Anonymous says:
>
> No but you can override the filter method to get more elaborate filtering capabilities.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcompletion-ios-7-update-and-the-20m-mark.html)


### **Anonymous** — January 27, 2015 at 6:55 am ([permalink](https://www.codenameone.com/blog/completion-ios-7-update-and-the-20m-mark.html#comment-22294))

> Anonymous says:
>
> always saying “you can filter”. please show us one example..
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcompletion-ios-7-update-and-the-20m-mark.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
