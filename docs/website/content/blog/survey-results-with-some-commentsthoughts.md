---
title: Survey Results With Some Comments/Thoughts
slug: survey-results-with-some-commentsthoughts
url: /blog/survey-results-with-some-commentsthoughts/
original_url: https://www.codenameone.com/blog/survey-results-with-some-commentsthoughts.html
aliases:
- /blog/survey-results-with-some-commentsthoughts.html
date: '2013-01-05'
author: Shai Almog
---

![Header Image](/blog/survey-results-with-some-commentsthoughts/survey-results-with-some-commentsthoughts-1.png)

Thanks for answering our questions about what you want in Codename One 1.1, your answers were very interesting and your comments are always helpful. Before getting to the actual results I’d like to cover some of the comments made in some of the responses which I think broadcast that we need to communicate what we have better: 

A frequent request is for more ad network integration  
_  
“AdMob, iAd support and more”  
_  
we are working on those but getting the agreements through and adapting them is difficult. These two specific networks are specifically difficult, Google is very rigid on AdMob and Apple is worse. We are constantly talking with networks about adding support for them (e.g. Zooz and Startapp which were recently integrated), but this is a time consuming slow process.

A comment asked for:  
_  
“Lists that require less code to implement.”  
_  
. Did you checkout MultiList? Check out the up to date developer guide which covers them, they are pretty lean and also very easy to work with in the GUI builder.

One poster answered  
_  
“Fast rendering on iOS (Slow performance on iPad 1 / iOS 5.5.1)”  
_  
to which I would like to answer that rendering is pretty fast already. If you feel performance is slow a survey isn’t the place to say that, use the discussion forum and read the developer guide tips.  
  
Mutable images are inherently slow on iOS and drawing most graphics primitives isn’t as fast, however image drawing etc. should be pretty fast.

One poster asked for  
_  
“Debugging on real device directly from Eclipse/Netbeans”  
_  
which we plan to do in the future and actually have all the pieces in place for this. It would just require quite a major effort from us to actually accomplish this so this isn’t planned for 1.1.

We got one request for  
_  
“Proximity Sensor Support”  
_  
, I’m guessing that we would have to implement bluetooth, NFC, sockets, motion sensor and only then proximity if at all. There is just too much ground to cover in these things.

Another commenter asked for  
_  
“Annotations Support to make compilation metaprogramming”  
_  
. We already support static annotations, dynamic annotations won’t be possible on older platforms (which might be fine) but would require some reflection capabilities. Since a mobile application is statically linked runtime annotations might be completely redundant since we know of all classes during compile time, that won’t change since we translate bytecode to native.  
  
Most of the functionality used by annotations is useful with dynamic loading, I would be interested to hear use cases though so feel free to submit issues with specific cases you would want to enable.

Lets look at the results and then I’ll try to interpret them. Feel free to give your own spin in the comments below.  
  
The first chart represents the primary feature, listed by rank below. 

[  
![](/blog/survey-results-with-some-commentsthoughts/survey-results-with-some-commentsthoughts-1.png)  
  
](http://1.bp.blogspot.com/-gE-Ly54c7q4/UOhJ5GIyIOI/AAAAAAAAZyM/oZv0N4L-cbA/s1600/chart_2.png)

  1. Windows Phone 8 support 32.56% 
  2. Library support 20.93% 
  3. New layout managers 9.30% 
  4. Continuous integration 6.98% 
  5. Improved 2D vector graphics 6.98% 
  6. Facebook/Google+ side menu 6.98% 
  7. Infinite scroll/pull to refresh 4.65% 
  8. bluetooth 4.65% 
  9. Easy web service wizard 2.33% 
  10. Theme customizer/colorization tool 2.33% 
  11. Charts API (graphs) 2.33% 

Everything below this got 0 percent. 

The second chart shows the options when picking one of 3 options: 

[  
![](/blog/survey-results-with-some-commentsthoughts/survey-results-with-some-commentsthoughts-2.png)  
](http://1.bp.blogspot.com/-fxNbgGVP2N4/UOhJ5TV-OvI/AAAAAAAAZyQ/AmlTqkAC22Q/s1600/chart_3.png)

  1. Windows Phone 8 support 17.22% 
  2. Library support 16.56% 
  3. Charts API (graphs) 9.27% 
  4. Improved 2D vector graphics 7.28% 
  5. Infinite scroll/pull to refresh 6.62% 
  6. New layout managers 6.62% 
  7. Facebook/Google+ side menu 5.96% 
  8. Continuous integration 5.30% 
  9. Theme customizer/colorization tool 5.30% 
  10. Automatic device testing 4.64% 
  11. Matisse like layout in the GUI builder 3.97% 
  12. Easy web service wizard 3.31% 
  13. Support Amazon purchase & push API’s 2.65% 
  14. IntelliJ Idea support 2.65% 
  15. Auto complete wizard 1.99% 
  16. bluetooth 0.66% 

My interpretation of the data is this:  

  * People want Windows Phone 8 support. We are working on getting it into 1.1. 
  * Library support, again a major requested feature although I’m not sure people understand what it means. This doesn’t mean you could take any arbitrary JAR off the internet. We already announced this feature for 1.1 so we are good here. 
  * New layout manager made a surprise showing at the top… I think this generally says people find layout managers difficult and hope we would have a better option. But its very likely a new layout manager will suffer from different set of problems from the current ones. I’d be happy if people who voted for this can post examples of what they would want in the comments. 
  * It surprised me that no one voted for the Matisse like layout manager in the GUI builder. I’m guessing people didn’t understand that this meant placing components in arbitrary locations and having it “just work” (sort of). 
  * Continuous integration made a great showing, I really want it as part of 1.1. 
  * I’m guessing most people who voted for improved vector graphics assume this is about performance or maybe SVG support. If I’m wrong about this feel free to correct me in the comments below. I’d like to start some of the work for this and would especially love to add features such as perspective transform. 
  * It surprised me how low bluetooth scored, we will probably not have it for 1.1 either unless we get a code contribution there. 
  * Charts have a really high secondary option position and a really low 1st priority position. I think its an important feature but I’m still undecided since its a VERY complex feature to properly integrate. 

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — January 28, 2013 at 5:09 pm ([permalink](https://www.codenameone.com/blog/survey-results-with-some-commentsthoughts.html#comment-21815))

> Anonymous says:
>
> The work I’ve done on the issue indicates a chart is simply another GUI component which is hooked up in a structured way to some non-trivial data source. This doesn’t seem to be complex, other than the Java2D programming required for good rendering on multiple platforms. Have you considered licensing iReports? It’s Java and open source so a port to mobiles adding GUI events to the charts to make them into interactive components might be the most efficient option.
>



### **Anonymous** — January 29, 2013 at 3:36 am ([permalink](https://www.codenameone.com/blog/survey-results-with-some-commentsthoughts.html#comment-21962))

> Anonymous says:
>
> There are many charting frameworks we can port such as JGraph or JChart etc. The main issue though is getting the underlying graphics capability to support these framework, e.g. a more elaborate affine implementation with a shapes/stroke API. That would require some serious work.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
