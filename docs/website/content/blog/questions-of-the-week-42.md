---
title: Questions of the Week 42
slug: questions-of-the-week-42
url: /blog/questions-of-the-week-42/
original_url: https://www.codenameone.com/blog/questions-of-the-week-42.html
aliases:
- /blog/questions-of-the-week-42.html
date: '2017-02-09'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-42/qanda-friday2.jpg)

This weeks update will go out a bit late or maybe even tomorrow. The reason for the postponing is the scope of the changes. We’ve made a lot of changes to [ParparVM](https://github.com/codenameone/CodenameOne/tree/master/vm) and we are very concerned that there will be regressions. So we would like this update to go in later in case we need to revert that work. Steve wrote a detailed post about the changes he made to ParparVM which we will post next week…​

Besides that with the 64bit change to the build I’m sure there will be a lot of feedback on this update during the week. There are quite a few other interesting things in this update including the big change to the way redirects are handled on iOS which might be significant as well.

In terms of questions we had quite a few interesting questions but nothing that would be interesting to the general public. We did have two interesting pull requests this week starting with the redirect fix [I wrote about](https://github.com/codenameone/CodenameOne/pull/2030) and followed by this [new feature from Diamond](https://github.com/codenameone/CodenameOne/pull/2033).

Diamond also submitted [this pull request](https://github.com/codenameone/CodenameOne/pull/2036) just this morning. It’s a trivial change that I think most of you can probably do and appreciate. This is probably my favorite type of improvement as it doesn’t disrupt much and makes Codename One just a bit easier to use. It’s a great way to get your feet wet when submitting pull requests.

So instead of questions I’d like to remind you how easy it is to submit a pull request and add to Codename One the stuff that you need. The importance of contributing to Codename One isn’t about our need of your code, it’s about taking part in shaping the tool that we use every day to something that suits us all. It’s about participation!

It’s really easy to [submit a pull request](http://www.codenameone.com/blog/how-to-use-the-codename-one-sources.html), you don’t even need to submit code changes to fix a typo or [edit the developer guide docs](http://www.codenameone.com/blog/wiki-parparvm-performance-actionevent-type.html).

Even if we ask you for a change we really appreciate the involvement as we think it makes Codename One better and it makes you better programmers.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **João Bastos** — February 19, 2017 at 9:53 am ([permalink](https://www.codenameone.com/blog/questions-of-the-week-42.html#comment-23019))

> João Bastos says:
>
> Shai, in the line of Diamond trivial change, that i really love 🙂 i think that it would be nice to have a MultiButton method like:  
> MultiBtn.setTextLines(textline1,textline2,textline3,textline4);
>
> i keep doing this a lot:  
> MultiBtn.setTextLine1(“name”);  
> MultiBtn.setTextLine2(“address”);  
> MultiBtn.setTextLine3(“city”);  
> MultiBtn.setTextLine4(“phonenumber”);
>
> Just a thought, dont know if it already exists, but havent found it.  
> P.S.- I know i should get my feet wet…but lets go one step at a time
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fquestions-of-the-week-42.html)


### **Shai Almog** — February 20, 2017 at 8:28 am ([permalink](https://www.codenameone.com/blog/questions-of-the-week-42.html#comment-22823))

> Shai Almog says:
>
> Go ahead and contribute this, it’s easy. I would recommend using a varargs argument so one can submit only some of the lines and not all 4 so it will be setTextLines(String… lines).
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fquestions-of-the-week-42.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
