---
title: Debating Brendan Eich Over Android-OpenJDK Move
slug: debating-brendan-eich-over-android-openjdk-move
url: /blog/debating-brendan-eich-over-android-openjdk-move/
original_url: https://www.codenameone.com/blog/debating-brendan-eich-over-android-openjdk-move.html
aliases:
- /blog/debating-brendan-eich-over-android-openjdk-move.html
date: '2016-01-05'
author: Shai Almog
---

![Header Image](/blog/debating-brendan-eich-over-android-openjdk-move/dukeandroid.png)

I had a big post ready for today but after a long twitter debate with  
[@BrendanEich](https://twitter.com/BrendanEich) I had to write a followup as  
twitter is a poor medium for that level of debate.  
This started with a [  
blog post from Andreas Gal](http://andreasgal.com/2016/01/05/oracle-sinks-its-claws-into-android/) who effectively took the exact opposite stance to mine on  
[Google’s move to OpenJDK](/blog/analysis-google-moving-to-openjdk-what-that-really-means.html).  
Then Mr. Eich picked it up: 

> .[@mosheeshel](https://twitter.com/mosheeshel) [@enleeten](https://twitter.com/enleeten) [@hhariri](https://twitter.com/hhariri) [@Codename_One](https://twitter.com/Codename_One) We don't know enough yet, but I agree with [@andreasgal](https://twitter.com/andreasgal) that code changes show cost+risk go up.
> 
> — BrendanEich (@BrendanEich) [January 5, 2016](https://twitter.com/BrendanEich/status/684454476028227584)

To be fair I’m biased. I work on a [cross platform mobile toolchain](/) and I’m an ex-Sun  
guy. But I think that the fact that the senior Java/Android community has embraced this change as a positive one  
says a lot. Also Android compatibility is crucial in our line of business where we need code to work for all  
versions of Android without a problem…  
Lets go over the claims made both on Twitter and in the article. 

#### Oracle sinks its claws into Android

Using a title like this is misleading, clickbait & antagonistic.  
I don’t think Oracle made any friends with the lawsuit, overall the vibe in the Java community is that  
this was a mistake that didn’t help anyone. The thing is that OpenJDK is GPL and so Oracle doesn’t really  
“own” it and doesn’t really gain much except in maybe some “respect”. 

OpenJDK is GPL+CPE so Google can use it without paying Oracle a dime (just like Linux distros do). It also  
features a patent grant which means that if Google passes the TCK’s (meaning they are Java compatible which  
is a **good thing(tm)**) they will receive a license for the patents and Oracle can’t legally sue  
them over patents used in the VM! 

So effectively the title is pretty wrong, Google gets newer up to date Java code for free and the Java OSS community  
can focus on one code base (for security analysis, performance etc.) instead of two. Compatibility will rise  
although its already there or 99.99% there. 

#### This Was Forced Thru Legal Process

Totally true. That doesn’t make it bad for us although one could wish that peace was reached without the legal system.  
Google took a calculated risk when Android was young which made a lot of sense when Sun owned Java.  
The reason for taking a specific action doesn’t mean its bad, I would wager that one of the reasons Google  
avoided adopting OpenJDK in the past was legal as well. 

#### Apache License vs. GPL License

As I was writing this a [  
great article was posted that pretty much answers this better than I could](http://ebb.org/bkuhn/blog/2016/01/05/jdk-in-android.html). I chose to leave my  
thoughts below since its simpler to read.  
Its harder to get people to adopt GPL, but they did it with Linux despite there being BSD licensed alternatives  
that are pretty good. Google already used Linux as the core so already has some GPL code.  
The original post already corrected the fact that OpenJDK didn’t exist when Android came out. But that wasn’t  
the sticking point with Sun. Google wanted the Apache license and Sun was making a lot of money over mobile  
so this was a big problem/conflict. 

The reason Google insisted on Apache was due to operators who were the gate keepers Google needed to get  
past to control mobile. Now that its already in and controls the market adding more GPL code is meaningless just  
as operators have gone down in their power since the days Android launched. Overall this changed nothing,  
in Android M you had X lines of GPL code from Linux and now you will have more lines of GPL+CPE code  
no real difference. 

#### All this code and technology churn will have massive implications for Android

> [@Codename_One](https://twitter.com/Codename_One) [@deanrl](https://twitter.com/deanrl) [@enleeten](https://twitter.com/enleeten) [@hhariri](https://twitter.com/hhariri) To recap, [@andreasgal](https://twitter.com/andreasgal) & I say MLOCs of forced change carry hi incompat risk, all agree that'd suck.
> 
> — BrendanEich (@BrendanEich) [January 6, 2016](https://twitter.com/BrendanEich/status/684609887817445376)

This is far from accurate. To be fair, only Google engineers know the approximate extent of work and only  
after the fact will we fully know the implications. However, I’ve ported JVM’s for years both at Sun  
& at Codename One. I’m very familiar with the issues of getting code to work on the desktop  
VM and the mobile VM and specifically with Android issues.   
From my experience Android VM compatibility issues are 80% vendor/operator issues (weird stuff with  
device specific behaviors) and 20% issues in the `android.*` packages. We ran into no  
issue whatsoever with the harmony code being different from Java 7 or 8 in terms of compatibility.  
Its possible that such edge cases exist but I doubt it would be a big deal and it would allow better  
server/mobile shared code. 

> [@deanrl](https://twitter.com/deanrl) [@Codename_One](https://twitter.com/Codename_One) [@mosheeshel](https://twitter.com/mosheeshel) [@enleeten](https://twitter.com/enleeten) [@hhariri](https://twitter.com/hhariri) [@andreasgal](https://twitter.com/andreasgal) Developer reward is later and second or third order. Risk+cost first order.
> 
> — BrendanEich (@BrendanEich) [January 5, 2016](https://twitter.com/BrendanEich/status/684520929800491008)

The existing Harmony classes are old and stale, no one touched them in years so a big change of removing  
and replacing them with other classes will take some effort from Google’s developers. That’s a fact.  
Its years worth of changes that now Google needs to do in a single swoop but its something Google  
**should** have done in the past to bring newer classes/features into place. The alternatives  
are doing nothing or doing this slowly at their own pace both of which would be bad. 

Java has amazing TCK’s that check compatibility, the likelyhood of something breaking is relatively low.  
We have been porting Java code to Android for years with absolutely no issues related to these API’s so on  
the surface this looks like something that would go unnoticed for most existing apps. If something will stop  
working or behave radically different Google has the ability to work in compatibility mode for that specific  
API builtin to their manifest target SDK system. 

While this is big change in terms of lines of code most of those lines are already written and well tested so  
it looks like a big change but is much smaller in scope than the LOC would make it seem. Prior changes made  
by Google even for Marshmallow are huge earth shattering changes to the basic permissions system of Android  
that has been in place since 1.x days. That is probably a far bigger problem for everyday Android developers  
than this change.  
Device compatibility with no-brand devices on the far east market and non-google devices are also far bigger problems  
for an Android developer than this will ever be. 

> [@deanrl](https://twitter.com/deanrl) [@Codename_One](https://twitter.com/Codename_One) [@mosheeshel](https://twitter.com/mosheeshel) [@enleeten](https://twitter.com/enleeten) [@hhariri](https://twitter.com/hhariri) [@andreasgal](https://twitter.com/andreasgal) 2/ Bill Joy of Sun persuaded us to kill Kipp's VM as bug4bug is impossible.
> 
> — BrendanEich (@BrendanEich) [January 6, 2016](https://twitter.com/BrendanEich/status/684526309540679681)

I worked at Sun for years and never got facetime with Bill Joy… Ugh.   
What was true in 1995 is no longer true today, Java now has a robust TCK and language specification. It  
has quite a few separate compliant VM’s and is pretty well understood. I think Google did an amazing  
job at compliance and its clear they followed the JVM specification well. 

#### This Change Won’t Benefit Users

> [@deanrl](https://twitter.com/deanrl) [@Codename_One](https://twitter.com/Codename_One) [@mosheeshel](https://twitter.com/mosheeshel) [@enleeten](https://twitter.com/enleeten) [@hhariri](https://twitter.com/hhariri) [@andreasgal](https://twitter.com/andreasgal) Developer reward is later and second or third order. Risk+cost first order.
> 
> — BrendanEich (@BrendanEich) [January 5, 2016](https://twitter.com/BrendanEich/status/684520929800491008)

While there are some changes that benefit users directly, most software development changes don’t. Not  
all developers benefit from every language/API feature either.  
Users will benefit from the fact that code bases are unified and security auditing will now focus on a more  
uniform code base. A lot of the standard Java tools might work better with Android as a result e.g. the current  
level of Android profiling support is HORRIBLE when compared to desktop Java or even iOS.  
iOS has excellent profiling support thanks to d-trace (technology from Sun) so hopefully we’ll see an eventual  
peace process and more collaboration to benefit everyone. 

#### Final Word

> [@Codename_One](https://twitter.com/Codename_One) [@mosheeshel](https://twitter.com/mosheeshel) [@enleeten](https://twitter.com/enleeten) [@hhariri](https://twitter.com/hhariri) [@andreasgal](https://twitter.com/andreasgal) Any nontrivial work raises costs+risks. As technologist & founder I see it plainly.
> 
> — BrendanEich (@BrendanEich) [January 5, 2016](https://twitter.com/BrendanEich/status/684457722071269376)

I think this sums up a lot of the claims. Yes this will take Google some effort. Yes a few apps might be  
affected, I’d wager this would be far less than those affected by the Marshmallow changes and Google  
has the tools to alleviate a lot of that problems (sdk version hints).  
Would Google have done it without the lawsuit?  
Maybe… If there was no lawsuit and Java 8 was out, maybe Google would have adopted OpenJDK or maybe  
they would have dedicated the engineering effort to add this support into Android’s Harmony. Either way  
a lot of engineering effort would have gone out whether the lawsuit was there or not. 

Google’s current version of Java is old and stale, replacing it is a good thing for everyone even though its  
not free of any costs. But everything has a cost and this one is probably worth paying in the long run.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Bo83** — January 6, 2016 at 5:20 pm ([permalink](https://www.codenameone.com/blog/debating-brendan-eich-over-android-openjdk-move.html#comment-22182))

> Bo83 says:
>
> great article and I get the overall gist of what you are saying but I would need a dictionary of terms and a day to study it to really read this article and understand it intelligently and thoroughly. I just want to write code with the latest and greatest; can’t we all just get along 🙂
>



### **Shai Almog** — January 6, 2016 at 5:28 pm ([permalink](https://www.codenameone.com/blog/debating-brendan-eich-over-android-openjdk-move.html#comment-22522))

> Shai Almog says:
>
> Thanks.
>
> It was actually fun debating him since he is a good debater (and REALLY smart). I just really can’t stand debating with “catch phrases”. Technical debates require proof and context which is problematic in twitter.
>



### **Chad** — January 6, 2016 at 10:56 pm ([permalink](https://www.codenameone.com/blog/debating-brendan-eich-over-android-openjdk-move.html#comment-22389))

> Chad says:
>
> IMO phrases like “Java has amazing TCK’s that check compatibility” and “It has quite a few separate compliant VM’s” are disingenuous. This is what killed Harmony in the first place, they couldn’t get a TCK because Oracle didn’t like their license. I would love to make my own open source compatible JVM, but I will never see the TCK. I think it’s harmful to pretend it’s a “good thing(tm)” that implementations have to pass a hidden test suite whose keyholders may make demands of your software. Sure it means I can’t call my JVM “Java”, but that’s the problem here. There is only one open-source compliant JVM implementation base that I am aware of, and that is a bad thing.
>



### **Shai Almog** — January 7, 2016 at 3:37 am ([permalink](https://www.codenameone.com/blog/debating-brendan-eich-over-android-openjdk-move.html#comment-22671))

> Shai Almog says:
>
> Interesting point, I skimmed over the angle of a VM implementer as its probably less interesting to most of the readership!
>
> First let me clarify that my bias is towards Google as I think they were in the right side technically as clean room implementations should be legal. We also use some code from Harmony in Codename One so I’m very much in favor of that.
>
> Sun was the one that refused the TCK license to Harmony, its an important distinction with all the people pilling hate on Oracle I don’t think we need to add something else there 😉  
> (Although to be fair Oracle didn’t fix anything in this regard)
>
> We make our own VM too (ParparVM) so obviously to be compliant we also need to pass the TCK now (see my previous analysis on the subject) which might also be a problem. If you use OpenJDK and its license you can probably apply to access for that as I’m sure Google has. Since they are/were a Java licensee I think they might already have that access.
>
> For a small VM (like us) this is more problematic so I totally agree that the opaqueness of the TCK process is a problem. FYI I did “see” and worked a lot with the TCK on older versions of Java to get our VM’s thru compliance. For Sun’s VM’s it was mostly trivial since the JIT/VM was reused but there are a lot of weird edge cases tested by the TCK.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
