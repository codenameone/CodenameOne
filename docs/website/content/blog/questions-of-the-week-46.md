---
title: Questions of the Week 46
slug: questions-of-the-week-46
url: /blog/questions-of-the-week-46/
original_url: https://www.codenameone.com/blog/questions-of-the-week-46.html
aliases:
- /blog/questions-of-the-week-46.html
date: '2017-03-09'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-46/qanda-friday2.jpg)

This has been one of the more exhausting weeks for me in recent years. I don’t remember this level of exhaustion since those first few months of launching Codename One. Getting everything ready is the hard part, doing the actual bootcamp is the relatively easy part…​  
Despite all of that we still got some things done this week.

Steve reworked the google maps cn1lib to have a web fallback option. He wrote a pretty big post on it but I didn’t want it to get lost in the whole “bootcamp launch” posts so we’ll post it next week. Our update of the week includes some bug fixes and a bit of new functionality that we will cover in posts next week.

Stack overflow was busy as usual. [HelloWorld](http://stackoverflow.com/users/6351897/helloworld) opened the subject of out of memory on an application. These things are remarkably hard to debug and we [are still conducting this discussion](http://stackoverflow.com/questions/42618206/avoiding-out-of-memory-in-codename-one).

[Julien Sosin](http://stackoverflow.com/users/7335469/julien-sosin) experienced issues with debug build installation, I wrote a [detailed step by step](http://stackoverflow.com/questions/42585376/cant-install-development-build) that might be useful  
[Beck](http://stackoverflow.com/users/4930974/beck) asked about storage not getting wiped. It turns out that Google changed the policy on backup to have it on by default. You can use the new `android.allowBackup=false` build hint to disable that.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
