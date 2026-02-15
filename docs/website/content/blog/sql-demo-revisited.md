---
title: SQL Demo Revisited
slug: sql-demo-revisited
url: /blog/sql-demo-revisited/
original_url: https://www.codenameone.com/blog/sql-demo-revisited.html
aliases:
- /blog/sql-demo-revisited.html
date: '2016-10-18'
author: Shai Almog
---

![Header Image](/blog/sql-demo-revisited/sql-playground.jpg)

The SQL demo has been on my “todo” list for months. It’s really hard to create a compelling demo for something  
as boring (by design) as SQL so this was a big procrastination target. After we built the SQL explorer tool for the developer  
guide it became apparent to me that this could be the basis for the new SQL demo, just provide the ability to  
type in arbitrary SQL and see it work…​

However, this doesn’t provide enough as beginners might not grasp the full scope of the capabilities, so to make  
this interesting we added a special tutorial mode that just shows you the queries you can execute. I initially wanted  
to do something more elaborate like a walkthru tutorial but time constraints blocked that option.

You’ll notice that I’m not embedding the JavaScript version of the demo into this post although you can see it from  
the [demo page](/sql-playground-sql-tutorial-in-the-browser-iphone-ios-android-windows/). The reason for that is unique.

Browsers don’t support SQL, there was an attempt at getting a standard out but it didn’t last and all vendors  
deprecated their respective SQL support although some still kept it (notably Chrome & Safari). So the demo  
works well on Chrome & Safari but not on Firefox (no idea about IE/Edge). There are also a couple of bugs in the  
SQL support in the JavaScript port making the situation slightly worse.

One of the things we were able to achieve is to make the demo submittable to app stores and as a result it’s  
already on  
[Google Play](https://play.google.com/store/apps/details?id=com.codename1.apps.db),  
[Microsoft Store](https://www.microsoft.com/store/apps/9nblggh42q9v) and on  
[itunes](https://itunes.apple.com/us/app/sql-playground/id1166517470).

Check out the full source code in the [github repository for the project](https://github.com/codenameone/SQLSample).

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
