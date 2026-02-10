---
title: Date Util
slug: date-util
url: /blog/date-util/
original_url: https://www.codenameone.com/blog/date-util.html
aliases:
- /blog/date-util.html
date: '2018-04-04'
author: Shai Almog
---

![Header Image](/blog/date-util/new-features-3.jpg)

Timezones suck. Especially daylight saving. I don’t mind moving the clock or losing an hour of sleep as much as the programming bugs related to that practice. The thing that sucks even more is Java’s old date/time API.  
This was publicly acknowledged by the Java community with JSR 310 which replaced the Java Date & Time API’s however due to its complexity we still don’t have it yet. As a small workaround we created a small API to perform some common date calculations.

`DateUtil` allows you to check if a day is in the daylight saving era or if it isn’t. It works consistently on all platforms without a problem e.g.:
    
    
    DateUtil du = new DateUtil();
    Log.p("Currently in daylight savings time? "+du.inDaylightTime(new Date()));
    Log.p("Offset: "+du.getOffset(new Date().getTime()));
    
    Date dec30 = new Date(1483056000000l);
    Log.p("Dec 30 is daylight savings time? "+du.inDaylightTime(dec30));
    Log.p("Offset: "+du.getOffset(dec30.getTime()));

The `DateUtil` constructor can take a `TimeZone` as parameter. Without it, it uses the default `TimeZone`.

### Completion Listeners

Media allows us to track whether it finished playing or not when we first set it up. After that point you were on your own.

Last week we added a new ability to bind a completion listener after the fact and potentially have multiple listeners:
    
    
    MediaManager.addCompletionHandler(myMediaObject, () -> Log.p("This is a runnable callback"));

### Partial Round

I’ve been working on improving [this issue](https://github.com/codenameone/CodenameOne/issues/2350). The UI part isn’t there yet but the code is…​

The gist of it is that with the round rect border we currently have 3 options:

  * All corners should be rounded

  * Only the top corners

  * Only the bottom corners

The issue pointed out a use case for some of the corners and I can think of a case where I’d like the left or right corners rounded…​

With that in mind I decided the right thing to do is offer control over individual corners. This is possible only in code at the moment but would hopefully make it to the designer tool too at some point:
    
    
    RoundRectBorder rb = RoundRectBorder.create().bottomLeftMode(false);

This would create a border whose corners are round except for the bottom left corner. While I was working on the class I also improved the performance/memory overhead of the border for solid colors.

### Support for PATCH HTTP Request In Rest

The `Rest` class now [supports the HTTP PATCH method](https://github.com/codenameone/CodenameOne/issues/2372) which was missing from the API before. It’s not as common as other API’s so it went unnoticed for a while.

It works pretty much like every other [Rest API request](https://www.codenameone.com/blog/terse-rest-api.html).
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — April 9, 2018 at 12:07 pm ([permalink](https://www.codenameone.com/blog/date-util.html#comment-23804))

> Francesco Galgani says:
>
> Thank you very much for the partial RoundRectBorder, it works as expected. I hope you can integrate it in the Designer 🙂
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdate-util.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
