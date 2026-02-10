---
title: Code Freeze for 3.5
slug: code-freeze-for-3-5
url: /blog/code-freeze-for-3-5/
original_url: https://www.codenameone.com/blog/code-freeze-for-3-5.html
aliases:
- /blog/code-freeze-for-3-5.html
date: '2016-07-25'
author: Shai Almog
---

![Header Image](/blog/code-freeze-for-3-5/codenameone35.jpg)

Today we are going into code freeze for Codename One 3.5 which is due one week from now, because of the fast release cycle we don’t need more than a week of code freeze to stabilize our current release.

The code freeze applies to the Codename One libraries and ports as those are the parts that are inherent to the release.

### What Didn’t Make the Release

We have many new features and great capabilities that we were able to introduce for 3.5. We will go into those with our release announcement.

The biggest “miss” we have in this release is the new iOS build servers with xcode 7+. We wanted to get them out before the release but this requires more work than we initially expected to get right. We remain committed to going thru with this process and will do so after the 3.5 release.

We didn’t finish modernizing and updating all the demos with this release. It’s disappointing but not crucial as most of the big ones are already done and the process is half way thru.

The new peer components implementation isn’t yet ready for primetime and isn’t the default (yet). It’s still not implemented on iOS where the implementation seems to be more challenging in some regards.

### How does the Code Freeze Work?

The pieces of Codename One that we freeze are effectively the entire github repository except for the Codename One designer and JavaDoc comments. That means that we will avoid any non-critical commits to that change anything other than the designer or docs until next Tuesday.

Notice that plugins etc. move at their own pace and are a separate entity from the release cycle.

We introduce a 3.5 branch into the git repository so you can reference the 3.5 version in the future. We review commits related to critical issues and merge them into the 3.5 branch. Commits will not go directly into the 3.5 branch and will instead go into the trunk where we cherry pick them into the branch.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
