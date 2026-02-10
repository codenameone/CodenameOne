---
title: Questions of the Week 35
slug: questions-of-the-week-35
url: /blog/questions-of-the-week-35/
original_url: https://www.codenameone.com/blog/questions-of-the-week-35.html
aliases:
- /blog/questions-of-the-week-35.html
date: '2016-12-08'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-35/qanda-friday2.jpg)

This week has been a bit slow with features and external progress as we’ve started to focus on the issues for  
the January release of 3.6. During December we’ll probably pause blogging between December 22nd and January  
2nd as it would probably get lost to the ether for most of our audience.

I’ll still be working and at least some of our team will, but we’ll focus on support and features/issues which will  
give us a lot to write about when we get back.

Todays update doesn’t include anything exceptional just a couple of new features and fixes.

[Stuart Brand](http://stackoverflow.com/users/5114135/stuart-brand) asks why the  
[enter key doesn’t show on the text field](http://stackoverflow.com/questions/40977725/codename-one-textfield-on-android-no-enter-key)  
but this is somewhat misleading…​ Which is why you must always ask with your intention.

At first I thought he meant the standard return key so my answer reflected that, newline isn’t on by default  
with `TextField`. But it seems he was looking for a “done” key which you can determine for Android using the  
client properties to pass a hint to the text field.

__ |  Another wrong path here was the mixed terminology of “key”. In Codename One keys refer to physical  
keys and not virtual keys   
---|---  
  
[Mamatha Damuluri](http://stackoverflow.com/users/6924648/mamatha-damuluri) asks  
[how one would know if an app is minimized.](http://stackoverflow.com/questions/40990021/how-to-know-the-app-is-closed-or-not-when-i-have-minimize-the-app-and-side-swipe)  
[Tim Weber](http://stackoverflow.com/users/5371574/tim-weber) provided a great answer for that. Lifecycle  
is one the harder things in mobile development. We tried to simplify it as much as possible but there  
are always pitfalls. Check out the first chapter of the developer guide when we cover some of these concepts…​

[tizbn](http://stackoverflow.com/users/854401/tizbn) asked how to  
[set the icon gap variable within a command](http://stackoverflow.com/questions/41008679/set-gap-between-text-and-icon-of-command)  
this is pretty easy but a lot of people might not know the answer which again involves a client property.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
