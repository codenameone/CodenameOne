---
title: Code Freeze for 3.4
slug: code-freeze-for-3-4
url: /blog/code-freeze-for-3-4/
original_url: https://www.codenameone.com/blog/code-freeze-for-3-4.html
aliases:
- /blog/code-freeze-for-3-4.html
date: '2016-04-25'
author: Shai Almog
---

![Header Image](/blog/code-freeze-for-3-4/release3.4.jpg)

Today we are going into code freeze for Codename One 3.4 which is due one week from now, because of the  
fast release cycle we don’t need more than a week of code freeze to stabilize our current release.

The code freeze applies only to the Codename One libraries and ports as those are the parts that will be inherent  
to the release.

### How does the Code Freeze Work?

The pieces of Codename One that are frozen are effectively the entire github repository with the exception  
of the Codename One designer and JavaDoc comments. That means that we will avoid any non-critical  
commits to that change anything other than the designer or docs until next Tuesday.

Notice that plugins etc. move at their own pace and are a separate entity from the release cycle.

We introduce a 3.4 branch into the git repository so you can reference the 3.4 version in the future. For critical  
issues that must be introduced to the 3.4 release we review each commits and merge it into the 3.4 branch.  
Commits will not go directly into the 3.4 branch and will instead go into the trunk where they will be cherry picked  
into the branch.

Because of this we will skip the Friday releases during the actual release phase and focus on getting 3.4 out  
of the door. Once that is out we will return to our regularly scheduled release cycle.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
