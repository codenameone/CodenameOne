---
title: Performance Improvements On iOS & Some Other News
slug: performance-improvements-on-ios-some-other-news
url: /blog/performance-improvements-on-ios-some-other-news/
original_url: https://www.codenameone.com/blog/performance-improvements-on-ios-some-other-news.html
aliases:
- /blog/performance-improvements-on-ios-some-other-news.html
date: '2013-03-18'
author: Shai Almog
---

![Header Image](/blog/performance-improvements-on-ios-some-other-news/codename-one-charts-1.png)

The other day one of our pro users sent us an app he is working on (which looks great and will hopefully be submitted to the gallery), he was experiencing major performance degradation on iOS compared to Android. Initially I couldn’t find anything wrong with the app so I started debugging and benchmarking the hell out of it. 

  
Turns out that we had a bug with table cells in which we made the cell transparent regardless of theme settings, this had to do with the old way in which we drew the table cell border which is no longer relevant. Anyway, this triggered a problem when he tried to set the cell background. His solution was to use a gradient and set the source/destination color to the same value!  
  
  
  
Naturally this slowed down performance considerably… However, even when that was fixed there was still some sluggishness that was obvious and specific to iOS.  

  
I spent some time with the device native profiler and found a major performance bug in scaled image drawing that occurred when  
  
drawing the same image in more than one resolution on the same page. Fixing that improved the performance noticeably. I then profiled again and found an issue with the speed of draw string, we cache textures of Strings in the native code and it seems that this cache was filling up too fast on my ipad. I increased the cache size and tripled it for the iPad which has a large size and can fit more strings on the screen. 

  
Doing all of the above has made the applications noticeably faster in every way and might improve the performance of your apps just by sending a new build.  

  
Other than that we are now starting to deploy SSD based Mac build servers. This roughly halves the time it takes to build a native iPhone app in CN1! I hope to convert all our servers to these beasts, I was able to build the Facebook demo in under 3 minutes!  
  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — March 19, 2013 at 8:08 pm ([permalink](https://www.codenameone.com/blog/performance-improvements-on-ios-some-other-news.html#comment-21931))

> Anonymous says:
>
> Great work! Things just keep getting better and better. You guys are so amazing.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fperformance-improvements-on-ios-some-other-news.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
