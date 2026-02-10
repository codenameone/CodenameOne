---
title: Migrating from Parse to Back4app
slug: migrating-from-parse-back4app
url: /blog/migrating-from-parse-back4app/
original_url: https://www.codenameone.com/blog/migrating-from-parse-back4app.html
aliases:
- /blog/migrating-from-parse-back4app.html
date: '2017-01-16'
author: Shai Almog
---

![Header Image](/blog/migrating-from-parse-back4app/parse.com-post-header.jpg)

A couple of years ago I wrote an app for my spouses yoga studio for managing her student list. I intended to open source it but the code is a bit messy and I can’t seem to find the time/energy to clean it up. I used the excellent [parse4cn1](https://github.com/sidiabale/parse4cn1) library from [Chidiebere Okwudire](https://www.smash-ict.com/) during the height of Parse.

As Parse ended I started thinking about contingency plans but after a few emails with Chidi and his [posts](https://www.codenameone.com/blog/how-i-chose-my-replacement-for-parse-com.html) detailing the various [options](https://www.codenameone.com/blog/how-i-chose-my-replacement-for-parse-com-part-2.html) I thought it might be possible to take the “lazy approach”.

Since the app is mostly for personal use I wasn’t faced with the prospect of replacing a “live” server which would have probably made the migration difficult. Since the app doesn’t use any “difficult” feature like push or complex server code this would be a trivial migration. Still I braced myself for hiccups…​

Looking at the options available I chose to go with [back4app](https://www.back4app.com/), it’s the first one I picked so I have no idea if other options are better/worse. I liked how easy they made the migration and I’m all for taking the lazy option when it’s available.

The migration didn’t require anything on the parse side, I just signed up for back4app and then selected the migration option. They offered two options for the migration supposedly an easy and a more custom migration. I clicked easy and it worked but I wish there was a description there detailing what I’m choosing (e.g. if I click easy and it doesn’t work can I have a do over? And if so why not do easy by default and offer the advanced option when something doesn’t work?).

After clicking the easy option I got some emails from parse and the UI showed my app from Parse being migrated at 0%. Since I don’t have much of a database I assumed this would be instant and it wasn’t. It took a bit under 10 minutes so your millage may vary, during those 10 minutes I tried to reload the site and something didn’t work but afterwards things started working and seemed in order so it might have been a small hiccup.

The end result looks great and the only thing I had to change in the app to get it to work was this:
    
    
        private static final String PARSE_SERVER = "https://parseapi.back4app.com";
    
        Parse.initialize(PARSE_SERVER, APPLICATION_ID, CLIENT_KEY);

I had to update the parse library since I was still using an older version now the `initialize` method takes the new server URL and I could set the back4app URL to the parse SDK. That one small change was enough and everything started working.

### Future

I like parse a lot, now that it’s open and has some solid servers it might be superior to firebase as it provides options and can’t be brought down by one company. I’d be very interested to hear about other companies besides back4app, I think that having multiple companies in this field will help them all grow in a similar way that multiple Linux distributions helped redhat grow.

I wonder if parse hosting will become a commodity in the future and I’d really like to hear stories/explanations about migrating live users from one parse host to another. If that is feasible parse might be a really good option for a generic app backend.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Ricardo Vicari** — March 11, 2017 at 12:51 am ([permalink](https://www.codenameone.com/blog/migrating-from-parse-back4app.html#comment-23398))

> Ricardo Vicari says:
>
> Shai, you dont sleep??? 🙂
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fmigrating-from-parse-back4app.html)


### **Ricardo Vicari** — March 11, 2017 at 12:53 am ([permalink](https://www.codenameone.com/blog/migrating-from-parse-back4app.html#comment-23462))

> Ricardo Vicari says:
>
> Shai, I currently have a company that develops solutions with totalcross ([www.totalcross.com](<http://www.totalcross.com>)) but I’m thinking of migrating to codenameone, but I’m worried about performance, we have running applications that have 100, 200 thousand records with SQLite and SQLite native wheel Inside Totalcross, how does it work in cn1?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fmigrating-from-parse-back4app.html)


### **Shai Almog** — March 11, 2017 at 6:19 am ([permalink](https://www.codenameone.com/blog/migrating-from-parse-back4app.html#comment-23329))

> Shai Almog says:
>
> I haven’t used totalcross since it was superwaba (back in the palmpilot days). Back then it was an interpreter, no idea what they are doing now.
>
> Since Codename One translates code to native and compiles it we have native speeds. In this particular case the speed will be limited by sqlite not by us. Either way, do a test case and benchmark to see how it will work.
>
> You can also write native code to optimize cases that we might not handle efficiently enough.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fmigrating-from-parse-back4app.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
