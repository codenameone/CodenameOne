---
title: Questions of the Week 44
slug: questions-of-the-week-44
url: /blog/questions-of-the-week-44/
original_url: https://www.codenameone.com/blog/questions-of-the-week-44.html
aliases:
- /blog/questions-of-the-week-44.html
date: '2017-02-22'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-44/qanda-friday2.jpg)

This weeks release adds support for JavaScript push but I’d recommend you don’t use it in production yet…​ The main issue is that we **might** change the way JavaScript push keys are generated so it might be better to put this on hold or only experiment with this for now. Regardless it’s a pretty big change to the push server so we have the old version standing by and might revert it if things get hairy.

We don’t have huge new features but we did make some big changes, e.g. the [pull request from jaanushansen](https://github.com/codenameone/CodenameOne/pull/2048) is pretty ambitious and might impact Android text editing significantly so keep your eye out on this.

We also changed the behavior of IMEI on Android tablets following this [pull request from JrmyDev](https://github.com/codenameone/CodenameOne/pull/2047).

We’ve had a lot of activity on stackoverflow this past week, here are some interesting posts:

First [Jeremy](http://stackoverflow.com/users/5427671/j%c3%a9r%c3%a9my-marquer) asked about debugging [using the Codename One sources when using the Eclipse IDE](http://stackoverflow.com/questions/42398095/how-to-use-codenameone-source-into-eclipse-project). Currently our instructions are exclusive to NetBeans. I recall a community member did this a while back but I can’t find the thread in the developer group. If anyone recalls that (or if you did this yourself) please take a look at that post and we’d appreciate an edit to the developer guide too!

[Jordan](http://stackoverflow.com/users/7600458/jordan-rey) asked a very different question that I think people should ask more often: [how to create the UI in this screenshot](http://stackoverflow.com/questions/42374880/having-trouble-creating-this-form-in-the-gui-builder). Ideally we’d have a gallery with common screenshots and instructions on how to implement every such UI as most UI patterns are very similar.

[Graham](http://stackoverflow.com/users/515377/graham) asked [whether Kotlin can be used with Codename One](http://stackoverflow.com/questions/42329892/can-i-use-kotlin-with-codename-one) I explained the challenges and then Steve clarified some of the finer points of the process. The short answer is “not officially”, it’s possible but some work might be required.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **3lix** — February 24, 2017 at 5:15 pm ([permalink](https://www.codenameone.com/blog/questions-of-the-week-44.html#comment-23168))

> 3lix says:
>
> Is 3.6.0 the lastest update?  
> I thought last week you mentioned pushing updates: [https://www.codenameone.com…](<https://www.codenameone.com/blog/questions-of-the-week-43.html>)  
> But I think the lastest update I see is 3.6.0 [https://www.codenameone.com…](<https://www.codenameone.com/files/netbeans/updates.xml>) released on 2017/01/10.  
> Thanks!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fquestions-of-the-week-44.html)


### **Shai Almog** — February 25, 2017 at 5:29 am ([permalink](https://www.codenameone.com/blog/questions-of-the-week-44.html#comment-21571))

> Shai Almog says:
>
> FYI if you include URL’s they go into moderation automatically to prevent SEO spam. We approve automatically if it’s relevant.  
> Right now we are only pushing updates to the libraries/servers. A plugin update is a bigger deal and we don’t do it as often. We aim for once a month or two.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fquestions-of-the-week-44.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
