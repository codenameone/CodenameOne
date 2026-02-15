---
title: 'Analysis: Google Moving to OpenJDK, What That Really Means'
slug: analysis-google-moving-to-openjdk-what-that-really-means
url: /blog/analysis-google-moving-to-openjdk-what-that-really-means/
original_url: https://www.codenameone.com/blog/analysis-google-moving-to-openjdk-what-that-really-means.html
aliases:
- /blog/analysis-google-moving-to-openjdk-what-that-really-means.html
date: '2015-12-29'
author: Shai Almog
---

![Header Image](/blog/analysis-google-moving-to-openjdk-what-that-really-means/dukeandroid.png)

I’ve been following the news breaking since yesterday as  
[a hacker news post](https://news.ycombinator.com/item?id=10803775)  
highlighted that an Android commit included OpenJDK files. This is amazing news and a huge step forward  
for Java, its still unclear if this is just another move or the inkling of a settlement between Google and  
Oracle but I’m very hopeful that this is indeed a settlement. So far Google wouldn’t comment on the  
court case and [  
whether it was settled since its still ongoing.](http://venturebeat.com/2015/12/29/google-confirms-next-android-version-wont-use-oracles-proprietary-java-apis/)  
**Disclaimer:** I worked at Sun/Oracle but had no internal knowledge of any of the things  
discussed here. The information here is collected from public sources, my understanding of the mentality of  
said companies & from our following the case at [Codename One](/) (we consulted IP lawyers quite a bit  
when we started and this case was always on our mind). 

I’ve read a lot of the comments in the reddit & hacker news threads and they seem to include a few upvotes  
on comments that are just plainly wrong here is a brief FAQ/informational on these common misquotes. 

#### Android will move to Hotspot/JIT

That’s unlikely. Java compatibility and compliance with OpenJDK doesn’t require that you use the JIT that ships  
with it or any of its source code for that matter. Quite a few compliant implementations don’t.  
While Hotspot will probably beat ART for anything other than startup time performance on mobile is quite  
different than desktop performance. Battery life is more crucial than speed and I doubt hotspot is  
optimized for that. 

#### Swing/AWT/FX Will Finally Be Supported On Android

There is no indication of that and it seems very unlikely.  
Google can be Java compliant by supporting a subset of Java and this is even easier thank to the modularity  
changes in Java 9. Swing/AWT/FX compliance complicate this whole process to a completely different level. 

#### Google Was At Fault

There were several claims like this e.g. Google copied code etc.  
I don’t like the fact that Android isn’t Java compatible and forked, but I generally disagree with that statement.  
It might have been wrong “morally” to fork Java, but I don’t think it was wrong legally.  
In the discovery phase of the trial only one small method was shown to be a direct copy and the judge  
dismissed that as ridiculous.   
Google didn’t violate the trademarks of Java and the coffee cup which are an important tool to keep Java clean.  
E.g. they never claimed that Android runs Java, it runs Android/Dalvik and now ART. It compiles Java source code  
which is a big leap.  
The claim is about copyrighting the API, that’s a problematic claim since Google did use a clean room implementation  
of the public API’s. The supreme court effectively said that clean room implementations of public API’s are  
illegal! 

That’s a pretty bad thing since copyright is implicit. Its owned even if the person publishing the material doesn’t  
explicitly write that little (c) you see next to various types of work. So if you ever implemented an API you are  
now effectively using copyrighted code! 

Most programmers who understand this think that Google acted based on “fair use” which means they didn’t  
actually violate the rights of the copyright holder. 

#### Oracle is a greedy litigious company

Not really.  
Oracle does sue companies but generally larger companies that can afford this, I’m unaware of them suing  
a startup or other small companies (feel free to correct me if I’m wrong here). It is far more profit driven than  
Sun was. I really loved Sun and loved working there, I can’t say the same about Oracle…  
But to be totally fair, Sun no longer exists in part because it was mismanaged and maybe not “greedy” enough.  
Having a strong “landlord” for Java might be disturbing in some regards but it has its advantages. I think  
anyone trying to show Oracle off as “evil” is plain wrong. 

#### This is like the Microsoft Lawsuit

No.   
Microsoft was a Java licensee and took the code to create a non-compliant implementation. Google was  
a licensee but the Android division was not. The fact that Google was a licensee for Java didn’t factor into  
the trial to my non-lawyer knowledge. 

#### This is about Java Compatibility

While Java compatibility is important and Google did do some damage there (not as much as the lawsuit did but  
still), this wasn’t the reason for the case.  
The lawsuit originally mentioned a 6bn USD figure for compensation and Google was willing to pay 100M USD  
to settle… This was like most lawsuits are about money.  
Its not necessarily bad to sue about money but this clearly cost more than it should have for both sides as  
it hurt Java in the market and that hurt two of its biggest users (Oracle & Google). Sun used to make  
a per device license fee for every J2ME phone sold, that was a huge bucket of money. I think Sun lost  
that revenue stream because it just neglected to update J2ME for more than a decade and when it finally did it  
was way too late. 

#### Is This Good For Java?

Yes. Without a question!  
Some claim the lawsuit should have been decisively won by Google, I think that would have been great because  
as stated above I don’t think the copyright clause is good for the industry as a whole. But that ship has sailed  
and now the best thing for Java is ending the hostility and unifying behind one standard Java so its great. 

Furthermore, ending this hostility means Java proved its chops in court which is a huge milestone. Developers  
often have the justify-able backlash when the legal system is involved choosing to go with a technology that’s  
more open (e.g. WebAssembly). Technologies like that might have hidden elements that can be  
sued over that we aren’t even aware of e.g. just using a GIF file was ground for legal action a few years ago…  
This is problematic with more open standards as there isn’t a single “landlord” to carry the technology forward  
in such a court case. 

Java has seen quite a few days in court and one of the nice things about OpenJDK is that it includes a patent  
license provision. This is rare and really valuable, if Google and Oracle settle over OpenJDK it pretty much means  
we can all align behind it peacefully and litigation would become far less likely. 

Furthermore, I think this would be great for Android as it will eventually move to a newer more modern version  
of Java and might enjoy better tooling as a result. It would also mean that some optimizations that might have  
been avoided by Android’s Runtime due to potential patent litigation might be applied to ART leading to  
additional benefits. 

The reduced stress among Google developers about IP cleanliness could also boost the productivity at Google  
and allow the Android developers to focus on building a better product. 

#### What About Older Versions of Android?

For those we will be able to use something like retrolambda like we do in Codename One. 

#### How Will This Affect Codename One?

Right now we tried to avoid OpenJDK code as much as possible with the same basic thought pattern Google  
took of using a clean room implementation to protect ourselves from future IP claims.  
We are following this closely, if this happens we’ll align ourselves to be compliant with CLDC 8 which is  
a valid Java 8 subset. This shouldn’t require much works as we are already 50% there and  
[ParparVM](https://github.com/codenameone/CodenameOne/tree/master/vm)  
etc. should be trivial to move to that level of compliance. 

This would hopefully give us a path to move forward in a way that would keep everyone happy. 

#### Summary

This is great news for all Java developers everywhere!  
Whether you work on Android, server, mobile or desktop!  
This could be the start of the long anticipated “peace process” or at least a ceasefire between Google & Oracle.  
This could allow us all to align behind one Java version eventually (taking into consideration the slow Android  
update process). It could help bring Java back into vogue with some developers who considered the closed  
nature of Java problematic.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Luix** — December 30, 2015 at 2:26 pm ([permalink](/blog/analysis-google-moving-to-openjdk-what-that-really-means/#comment-22616))

> Luix says:
>
> Re: “is it good for Java?” The main issue here is Java isn’t good enough for anyone. Most of the apps running in that resource hog are chucks of code blindly stolen from previous apps/programs. Most of the developers just steal the code and mend it to perform more or less decently, and when that’s not enough, they just simply demand more hardware.
>
> I don’t mean to start a flame war, but IMHO Google should ditch the Java approach all together and find a better replacement.
>



### **Shai Almog** — December 30, 2015 at 2:31 pm ([permalink](/blog/analysis-google-moving-to-openjdk-what-that-really-means/#comment-22626))

> Shai Almog says:
>
> You don’t mean to start a flame war but you post a “Java sux” comment in a Java blog. And with a Darth Vader avatar no less 😉
>
> Copy and paste == code reuse. Bad programmers exist everywhere. Java is actually pretty efficient (e.g. embedded etc.) and performant when compared to pretty much all alternatives.
>
> Google analyzed the options before picking Java, there are no realistic alternatives for Java and none are better performing.
>



### **Luix** — December 30, 2015 at 2:44 pm ([permalink](/blog/analysis-google-moving-to-openjdk-what-that-really-means/#comment-22636))

> Luix says:
>
> An excellent answer. I apologize, English isn’t my main language. Java *was* the best alternative when Android started walking its first steps, but then again, a lot of water has gone below that bridge.
>
> As a SysAdmin I deal with poorly written Java code all the time (i.e. the developers simply complain about their code running slow on expensive hardware because they don’t have enough resources). I’d like to see a path to better resource usage rather than expecting the system to grow in size. That’s just an unrealistic approach. I reckon it’s Google’s fault for not steering the development in that direction, but then again, Java might be the de facto standard, but it doesn’t mean it couldn’t be improved.
>



### **Shai Almog** — December 30, 2015 at 2:53 pm ([permalink](/blog/analysis-google-moving-to-openjdk-what-that-really-means/#comment-22456))

> Shai Almog says:
>
> We discussed this a bit in the hacker news thread. One of the problems with Java on the server/desktop is the lack of MVM (or isolates as they are called today) which allows multiple Java instances to share more state and thus take up less RAM/CPU. Back in the day this wasn’t a priority for Sun as it was selling big iron hardware and it would just mean “buy more hardware”.
>
> I totally see the issues you are running against, some Java apps are just ridiculous and its often hard for us to gauge what the hell is needed in terms of resources. One annoyance I had with servers was that the Xmx switch for the JVM to indicate the maximum memory has no value for the admin. It only allocates the memory visible to the Java application but not the memory I need to give to the server. So if I use -Xmx2gb the app might take up 2.5gb because of various VM overhead issues. This makes it pretty painful, but that’s an implementation problem more than an inherent issue in the Java language.
>



### **Luix** — December 30, 2015 at 3:01 pm ([permalink](/blog/analysis-google-moving-to-openjdk-what-that-really-means/#comment-21500))

> Luix says:
>
> Again, I appreciate the time you took to make me a little less ignorant.
>
> Like you said, a language isn’t inherently bad or good, it’s the use we give to it what draws a positive/negative value.
>
> Anyway, I salute the step Google took to a open source implementation, and agree with you in your considerations about what that means in terms of growth for the community and the language itself. I just hope Google would tighten the development guidelines the way they did with the visual ones with Material Design.
>
> All I have left are my best wishes for the upcoming new year.
>



### **bryan** — January 2, 2016 at 10:10 pm ([permalink](/blog/analysis-google-moving-to-openjdk-what-that-really-means/#comment-22440))

> bryan says:
>
> What you said makes no sense at all. Java the language is fine (unless your preference is functional languages), and the JVM (which as Shai has noted is NOT what Google use) works just fine also. Back in the day almost all feature phones had Java baked in, and given the hardware constraints, Java apps worked remarkably well (and worked even better with LWUIT – thanks Shai/Chen), so to say Java is intrinsically slow or a resource hog is nonsense.
>



### **Adam** — January 6, 2016 at 7:19 pm ([permalink](/blog/analysis-google-moving-to-openjdk-what-that-really-means/#comment-22657))

> Adam says:
>
> “While Hotspot will probably beat ART for anything other than startup time performance on mobile is quite different than desktop performance. Battery life is more crucial than speed and I doubt hotspot is optimized for that.”
>
> Are these really competing goals? Something that executes in fewer CPU cycles will result in longer battery life. AOT is always better than JIT for most things (unless they can only be known at runtime), but as far as a JIT engine goes, faster is better for everybody.
>



### **Shai Almog** — January 6, 2016 at 7:30 pm ([permalink](/blog/analysis-google-moving-to-openjdk-what-that-really-means/#comment-22514))

> Shai Almog says:
>
> Not quite so simple… we have two cases:
>
> a. Really complex calculations that take up 100% of the CPU. JIT has the advantage of determining all the branches that never change and optimizing them away. Inlining methods more aggressively etc.
>
> b. Most apps – cpu is needed for very short bursts and idle afterwards. JIT has plenty of CPU cycles left to optimize the short bursts to be much faster/smaller. On mobile devices this is good since it makes better use of CPU caches.
>
> AOT will always be slower than a good JIT since it has no way of optimizing away a branch statement that never changes. These are really expensive. Virtual method inlining (which leads to cross method optimizations) is also a HUGE benefit.
>
> Reducing battery usage is a different deal though and memory constraints on the device are also different. A jit can over optimize and regress. It can choose to use IDLE CPU to do various tasks which when optimized for power consumption it won’t. At Sun we had several very different JIT implementations, the mobile JIT’s had a far simpler architecture than hotspot. You even see this in the two modes hotspot features (server & client).
>
> Anyway, I digress. Its a bit more complex than that.
>



### **Adam** — January 6, 2016 at 7:46 pm ([permalink](/blog/analysis-google-moving-to-openjdk-what-that-really-means/#comment-22257))

> Adam says:
>
> Yes, a lot of that fits into my “unless they can only be known at runtime” parenthetical there, and I agree, there are limits to ahead of time compilation where a JIT can do much better.
>
> That being said, what on earth would the JIT be doing that is consuming otherwise idle CPU cycles that aren’t otherwise necessary tasks? Overly aggressive garbage collection? Maybe I’m only looking at this from an application level lens when I should be viewing from a virtual machine lens.
>



### **Shai Almog** — January 6, 2016 at 7:55 pm ([permalink](/blog/analysis-google-moving-to-openjdk-what-that-really-means/#comment-22664))

> Shai Almog says:
>
> Think of a JIT as a profiler that runs on your device in your users hands.
>
> The profiler ran while the user used the application, since every user uses the app differently the JIT has an option to optimize based on how the user worked and idle is the perfect time to do it.
>
> Hotspot will further optimize but needs to decide when to stop and also when to revert an optimization that didn’t generate the right results (optimizations aren’t always clean cut).
>
> Mobile JIT’s aren’t as aggressive but they do something weird. They GC jitted code. E.g. if RAM is low or if a piece of code was used a few times and then no longer called then wasting RAM on compiled code is redundant.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
