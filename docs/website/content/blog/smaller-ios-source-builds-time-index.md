---
title: Smaller iOS Source Builds, Timer & Index
slug: smaller-ios-source-builds-time-index
url: /blog/smaller-ios-source-builds-time-index/
original_url: https://www.codenameone.com/blog/smaller-ios-source-builds-time-index.html
aliases:
- /blog/smaller-ios-source-builds-time-index.html
date: '2015-06-16'
author: Shai Almog
---

![Header Image](/blog/smaller-ios-source-builds-time-index/iOS.png)

Up until now when building with include sources for iOS we also included a btres directory which was necessary for the  
old VM but no longer necessary in the new VM. This increased the distribution size considerably and we are now  
removing it to speed up the builds and reduce server costs. When we were in the process of reviewing the sizes of  
apps we noticed quite a few apps with resources weighing well over 5mb which would probably cause performance  
issues for your application, we suggest reviewing such apps and [optimizing them](/blog/shrinking-sizes-optimizing.html). 

On a separate issue the `Timer` class seems to have been leaking threads for some reason on iOS.  
As we looked into it the code for the class was very complex so we simplified it considerably. If you are using  
`java.util.Timer` please pay attention to changes in behavior that might have occurred because of this  
change and let us know if you run into such issues. 

We also made a big change to the way exception handling works in the new VM last week, it solved some hidden issues  
that might have occurred with exceptions thrown within a synchronized block. If you had some hard to track issues  
that might be related we suggest you try again with a fresh build. Other than that we made some fixes to the charts  
code and performance improvements on devices for that code. 

#### JavaDoc Index

A while back I was sitting with a developer whose been programming with Java and Codename One for quite some time,  
yet didn’t find a feature that he was searching for in the [JavaDoc](/javadoc/). This brought to my attention  
the fact that quite a few people forget about the existence of the [JavaDoc](/javadoc/) index file  
(just click the index link on the top right). 

You can just use the search function of the browser to find methods or documentation of interest regardless of the classes  
in which the method is defined. This is useful when you are looking for a specific API and don’t have much to go on.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Yaakov Gesher** — June 23, 2015 at 8:23 am ([permalink](https://www.codenameone.com/blog/smaller-ios-source-builds-time-index.html#comment-22322))

> Yaakov Gesher says:
>
> I think another important feature to help developers get better access to documentation would be a search box in the blog. The blog entries contain so much invaluable information, and yet there doesn’t seem to be a way to search the blog.
>



### **Shai Almog** — June 23, 2015 at 2:17 pm ([permalink](https://www.codenameone.com/blog/smaller-ios-source-builds-time-index.html#comment-22127))

> Shai Almog says:
>
> We’d love to have a site wide search feature that would ideally include the javadocs, developer guide etc. unfortunately this is none trivial.  
> Notice that you can easily search [codenameone.com](<http://codenameone.com>) by typing [site:codenameone.com](site:codenameone.com) your query into google e.g.: [https://www.google.com/sear…](<https://www.google.com/search?q=site:codenameone.com+MultiButton>)
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
