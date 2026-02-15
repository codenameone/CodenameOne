---
title: Changes to 2017 Milestones and DST Hotfix
slug: changes-2017-milestones-dst-hotfix
url: /blog/changes-2017-milestones-dst-hotfix/
original_url: https://www.codenameone.com/blog/changes-2017-milestones-dst-hotfix.html
aliases:
- /blog/changes-2017-milestones-dst-hotfix.html
date: '2017-03-20'
author: Shai Almog
---

![Header Image](/blog/changes-2017-milestones-dst-hotfix/generic-java-2.jpg)

We released an important fix for an issue with daylight saving in north America (DST), if you are experiencing weird issues only on iOS that could be attributed to time problems then please send a new build to see if the fix works correctly. Our iOS VM code made some assumptions about DST which were apparently false. We chose to release it outside of our regular update schedule due to the significance of this issue.

### Release Schedule

Due to the tight schedule of the bootcamp we’re postponing some of the previously announced to 2017 milestones so we won’t need to split our focus. The bootcamp ends on May 1st and might require some more effort after it completes and we don’t want to draw the attention from the release. Since our set of open issues is ridiculously large there is just no other way.

The [3.7 milestone](https://github.com/codenameone/CodenameOne/milestone/9) was planned for May 9th. We’ve decided to postpone it to June 27th (a date I just randomly picked with no reason at all).

The [3.8 milestone](https://github.com/codenameone/CodenameOne/milestone/10) was planned for October 4th, I’m not a fan of December releases and even November is problematic but I don’t want to slip the release into 2018 so we will go for November 14th as the new release date.

### Our Activity During the Bootcamp

During the weeks of the bootcamp the blog won’t post updates unless there is something urgent that we need to communicate. We won’t do the standard Friday release either during those dates. Since the bootcamp is split in the middle we will have regular activity during the off week of the bootcamp.

Support which I usually handle will be split between employees with Steve taking most of the public support effort and the pro/enterprise support aliases. Things should function as usual for the most par however if you are one of the people who emails me directly instead of the support alias, or one of the people who doesn’t mention his pro/enterprise account in his email you might not get an answer.

So please make sure to include that information in support queries (in general not just now).
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chris** — March 22, 2017 at 5:50 pm ([permalink](/blog/changes-2017-milestones-dst-hotfix/#comment-23212))

> Chris says:
>
> New Build still I do see there is a 1 hour delay for DST. On simulator, it was working properly but not on iOS.
>



### **Shai Almog** — March 23, 2017 at 5:55 am ([permalink](/blog/changes-2017-milestones-dst-hotfix/#comment-23221))

> Shai Almog says:
>
> We had a bug in the first fix (worked on the simulator but failed on devices) we’ve since patched it and got confirmation it works. Can you test again?
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
