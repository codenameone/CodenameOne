---
title: Longstanding Issue Of Back Button In BiDi/RTL
slug: longstanding-issue-of-back-button-in-bidirtl
url: /blog/longstanding-issue-of-back-button-in-bidirtl/
original_url: https://www.codenameone.com/blog/longstanding-issue-of-back-button-in-bidirtl.html
aliases:
- /blog/longstanding-issue-of-back-button-in-bidirtl.html
date: '2013-06-26'
author: Shai Almog
---

![Header Image](/blog/longstanding-issue-of-back-button-in-bidirtl/longstanding-issue-of-back-button-in-bidirtl-1.png)

  
  
  
[  
![Picture](/blog/longstanding-issue-of-back-button-in-bidirtl/longstanding-issue-of-back-button-in-bidirtl-1.png)  
](/img/blog/old_posts/longstanding-issue-of-back-button-in-bidirtl-large-3.png)  
  
  

We’ve had a long standing annoyance with Codename One’s RTL (Right To Left languages, e.g. Arabic/Hebrew etc.) where the back button was still pointing in the wrong direction on iOS.  
  
This was REALLY annoying to such a great extent that up until recently we recommended that people don’t use RTL on iOS devices. 

Well we finally fixed it, what took us so long was the desire to “do it right” but we couldn’t figure out what the right thing was?

Basically the iOS 6 back button is broken since it has a very specific direction and angle it can’t be cut as a 9-piece border and has to actually appear as a horizontal image border. My initial thoughts were to allow a component to define an RTL specific style attribute (in this case border), however doing something like this would be painful and just the thought of going back and editing all the themes seemed excessive. 

So I added a method into the image class called mirror() which you can now invoke to get an exact mirror image of the given image. and into the border class we added the ability to get an RTL border, this is only implemented for horizontal image borders which is the source of this specific problem. This isn’t generic but it works rather well and seamlessly! The back button will “just reverse” on RTL regardless of the theme you use and will act as you would expect starting with the upcoming update.  

* * *

  
  
  
[  
![Codename One Java Rock Stars](/blog/longstanding-issue-of-back-button-in-bidirtl/longstanding-issue-of-back-button-in-bidirtl-2.jpg)  
](/img/blog/old_posts/longstanding-issue-of-back-button-in-bidirtl-large-4.jpg)  
  
  

As a side note, we will not be traveling to Java One this year as all our talks got rejected. This is after having packed rooms and rock star trophy from the previous Java One. Its a shame that Oracle is such a monocolture where you can’t talk about alternative approaches within their conference. I can’t say I’m too disappointed about not going, since leaving Moscone Java One hasn’t been the same.  
  
  
  
  
I am very excited about attending JavaZone which looks like a REALLY cool conference by some very cool guys, can’t wait to go there. 

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
