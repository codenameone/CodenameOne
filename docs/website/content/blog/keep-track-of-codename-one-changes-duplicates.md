---
title: Keep Track of Codename One Changes & Duplicates
slug: keep-track-of-codename-one-changes-duplicates
url: /blog/keep-track-of-codename-one-changes-duplicates/
original_url: https://www.codenameone.com/blog/keep-track-of-codename-one-changes-duplicates.html
aliases:
- /blog/keep-track-of-codename-one-changes-duplicates.html
date: '2016-05-31'
author: Shai Almog
---

![Header Image](/blog/keep-track-of-codename-one-changes-duplicates/how-to-use-the-codename-one-sources.jpg)

One of my pet peeves when we switched to github was that email notifications never worked for me. For most  
repositories I had to setup my own account just to get emails. I’m guessing that this is a common problem for  
those of us who are used to emails notifying us of changes.

So we went over all the repositories at [github.com/codenameone](http://github.com/codenameone) and made  
them all send an email to the [codenameone-commits google group](https://groups.google.com/forum/#!forum/codenameone-commits).

So you can now subscribe to email notifications from [this group](https://groups.google.com/forum/#!forum/codenameone-commits)  
and you will get an email every time one of us commits something to the repositories of Codename One!

You will notice that the group has some commits from 2012…​

This is actually an old group that was used for commit notification a while back and eventually got deprecated  
as it wasn’t necessary for google code. There was really no point in creating a new group as this one worked fine.

__ |  If you see an issue in a commit please don’t post to that group as we’d like to keep it completely based on  
the automated emails   
---|---  
  
### ConnectionRequest Duplicates

[ConnectionRequest](/javadoc/com/codename1/io/ConnectionRequest/)  
had special protection from duplicate requests for the same data. This was added to prevent cases  
such as multiple image downloaders fetching the same image over and over again.

The mistake was that we set the duplicate detection mode to be on by default which made some networking  
issues really hard to track down consistently. We decided to flip the switch `setDuplicateSupported(true)` will no  
longer be required as it will be the default.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
