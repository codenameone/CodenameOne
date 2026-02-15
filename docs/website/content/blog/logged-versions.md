---
title: Logged Versions
slug: logged-versions
url: /blog/logged-versions/
original_url: https://www.codenameone.com/blog/logged-versions.html
aliases:
- /blog/logged-versions.html
date: '2016-07-24'
author: Shai Almog
---

![Header Image](/blog/logged-versions/generic-java-1.jpg)

When we get a crash report one of our first questions is “when did you build this?”.  
The answer is often too vague to trace the specific version, so we end up with a mixture of guessing.

The main issue is that there are different version values. They conflict with one another. They can be confusing and they can be inaccurate.

Starting with the latest update we now have a file in the root of Codename One called `cn1-version-numbers` and it includes two numbers that might not be accurate but will give us a general ballpark of the relevant version. The latter number is our internal SVN version number but the former can be interesting to you too. These numbers will appear as below when you make your first `Log.p(String)` call:
    
    
    [EDT] 0:0:0,0 - Codename One revisions: 4ee2778c79ad5eaadd2344bc0f215a82483421cb
    1955

The former is a GIT version, this can help you browse the git repository at that specific version by using a UWL like this:

`[version](https://github.com/codenameone/CodenameOne/tree/)`

E.g. the URL matching the version above would look like that:

<https://github.com/codenameone/CodenameOne/tree/4ee2778c79ad5eaadd2344bc0f215a82483421cb>
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **adalbert** — February 10, 2018 at 10:41 am ([permalink](/blog/logged-versions/#comment-23630))

> adalbert says:
>
> Why in e.g. url in file cn1-version-numbers there is 83179fd0246f2f2114eded43beffb51c5c7aa4d6 not 4ee2778c79ad5eaadd2344bc0f215a82483421cb?  
> [https://github.com/codename…](<https://github.com/codenameone/CodenameOne/blob/4ee2778c79ad5eaadd2344bc0f215a82483421cb/CodenameOne/src/cn1-version-numbers>)
>



### **Shai Almog** — February 11, 2018 at 7:26 am ([permalink](/blog/logged-versions/#comment-23811))

> Shai Almog says:
>
> Because when we do a build it generates a new key matching the current tree which is slightly newer. Both should be roughly from the same time frame.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
