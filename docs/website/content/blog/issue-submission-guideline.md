---
title: Issue Submission Guidelines
slug: issue-submission-guideline
url: /blog/issue-submission-guideline/
original_url: https://www.codenameone.com/blog/issue-submission-guideline.html
aliases:
- /blog/issue-submission-guideline.html
date: '2016-04-26'
author: Shai Almog
---

![Header Image](/blog/issue-submission-guideline/how-to-use-the-codename-one-sources.jpg)

One of the best things in running an open source project is the high quality issues, we don’t always respond  
immediately and sometimes things get lost under our piles of work but we do appreciate the time you  
take to file issues.

We’d like to define some guidelines for issue submission, this will make our job easier in processing/assigning &  
resolving issues as soon as possible. We don’t want this post to deter you from submitting an issue, it’s here to help.  
If something is unclear in this post just go ahead and submit or ask a question in the comments section below.

### Should I Submit an Issue?

If you aren’t sure this is a bug we suggest googling and asking on [stackoverflow](http://stackoverflow.com/tags/codenameone)  
or in the [discussion forum](/discussion-forum.html).

If it’s a question, it probably doesn’t belong in the issue tracker and should go in the support channels.

### Where to Submit an Issue?

If you have an issue with any codenameone project they should all go into the  
[main Codename One issue tracker](http://github.com/codenameone/CodenameOne/issues/).  
This even includes issues with the [codenameone.com](https://www.codenameone.com/) website, the plugins &  
even the cn1libs hosted under the codenameone user account.

However, other cn1libs that are hosted under different user accounts should use their respective issue trackers!

### Should I Edit the Issue or Just Post Comments?

When we read an issue we usually start with the top description (the first comment). It needs to be as up to date  
as possible and include the elements described in the next question. So please edit it with new information  
related to the test case.

Two things you should keep in mind:

  * Don’t edit the question in a way that removes/changes previous meaning. E.g. if you remove information from  
the issue this will create a situation where comments might become unclear. Try to cleanup & elaborate without  
distorting too much meaning

  * After editing is done post a comment. Editing doesn’t trigger a notification to people watching the project  
and so developers might be unaware that you made an edit

### What Should an Issue Include?

The issue should obviously include a description of the problem and ideally a test case to reproduce it.  
Make sure to highlight if the issue occurs on a device (include OS versions tested), simulator or both!

Unless the issue is specifically related to the GUI builder we prefer the issue will be a small handcoded application coded  
from the bare bones template. Ideally only the `start()` method should be included if that is possible.

__ |  The reason we prefer it this way is that we already have test case apps with the right provisioning/certificates etc.  
in this way we can just paste your code directly into our existing test app and deploy   
---|---  
  
Source should be embedded using a backtick notation into the question followed by the word java on the first line e.g.
    
    
    ````java
    Your source goes here...
    ````

Should produce Java syntax highlighted code in place.

If you have a really complex project and there is no way to simplify it you can use either [gist](http://gist.github.com/)  
or github itself and post a link to a project you committed.

The reason we prefer this is that we can then browse the source quickly to find issues without downloading it and  
instantly determine the right person to deal with a given issue.

#### Screenshots & Videos

Screenshots are often crucial pieces in describing some issues, if there is any visual aspect to your issue please include  
a screenshot!

You can grab a screenshot off of any device, please don’t use a camera to grab a device screenshot…​

__ |  You can capture the screen on your iOS device using the Sleep/Wake and Home buttons. Press and hold  
the Sleep/Wake button on the top or side of your iPhone, iPad, or iPod touch, then immediately press and release  
the Home button. You can find the screenshot in your Photos app   
---|---  
  
__ |  You can grab a screenshot of an Android device by holding the volume down button and the power button   
---|---  
  
Elements such as animations or artifacts require video you can grab a video of an iOS device using one  
of the [techniques described here](http://www.apptamin.com/blog/capture-iphone-ipad-screen-video/).  
Android 4.4 or newer requires the Android SDK to achieve a similar result using  
[this technique](https://developer.android.com/tools/help/shell.html#screenrecord).  
Using these techniques will allow a non-grainy video that’s easy to understand even for elaborate/refined issues.

We recommend uploading the video to youtube as “unlisted” video and embedding the link into the issue.  
This allows anyone to view the video almost instantly even on mobile while wading thru issues. This also

#### Resources/Images

Some issues require a resource file or image files. These can be added as attachments to the issue but we’d appreciate  
if issues can be reproduced without such files if possible.

### In Closing

Issues are probably the best way most of us can do to help a project like Codename One or any other open  
source project move forward. They are a great help and we really appreciate the effort that goes into them even  
when they are not up to the standards above.

However, if you follow the guidelines above it will make our work easier and it will make resolution of such  
issues faster.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Jérémy MARQUER** — April 27, 2016 at 4:31 pm ([permalink](/blog/issue-submission-guideline/#comment-22662))

> Jérémy MARQUER says:
>
> Hi,  
> Can you help me with this issue please : [https://github.com/shannah/…](<https://github.com/shannah/cn1-freshdesk/issues/3>)  
> I can’t build anymore for Android until library with andlib format is included in this cn1lib. Thanks.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
