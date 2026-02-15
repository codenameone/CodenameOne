---
title: Why Mobile Web Is Slow?
slug: why-mobile-web-is-slow
url: /blog/why-mobile-web-is-slow/
original_url: https://www.codenameone.com/blog/why-mobile-web-is-slow.html
aliases:
- /blog/why-mobile-web-is-slow.html
date: '2013-07-13'
author: Shai Almog
---

![Header Image](/blog/why-mobile-web-is-slow/why-mobile-web-is-slow-1.jpg)

  
  
  
[  
![Picture](/blog/why-mobile-web-is-slow/why-mobile-web-is-slow-1.jpg)  
](/img/blog/old_posts/why-mobile-web-is-slow-large-2.jpg)  
  
  

**  
Clarification update:  
**  
For some reason people read this as a rebuttal of  
[  
Drew Crawford  
](http://sealedabstract.com/rants/why-mobile-web-apps-are-slow/)  
article,  
**  
it is not  
**  
. It is merely a response, I accept almost everything he said but have a slightly different interpretation on some of the points. 

  
  
Over the weekend quite a few people wrote to me about a  
[  
well researched article written by Drew Crawford  
](http://sealedabstract.com/rants/why-mobile-web-apps-are-slow/)  
where he gives some insights about why the mobile web/JavaScript is slow and will not (for the foreseeable future) compete with native code. My opinion of the article is mixed, it is well written and very well researched, I also agree with a few of the points but I think that despite getting some of the conclusions wrong I think his reasoning is inaccurate. 

  
But first lets start with who I am (Shai Almog) and what I did, I wrote a lot of Java VM code when consulting for Sun Microsystems. I did this on mobile devices that had a fracti  
  
on of the RAM/CPU available for today’s devices. Today I’m the co-founder of Codename One where I regularly write low level Android, iOS, RIM, Windows Phone etc. code to allow out platform to work everywhere seamlessly. So I have pretty decent qualification to discuss devices, their performance issues etc.

Lets start by the bottom line of my opinion:  

  1.   
I think Drew didn’t cover the slowest and biggest problem in web technologies: the DOM.  

  2.   
His claims regarding GC/JIT are inaccurate.  

**  
  
  
So why is JavaScript slow?  
**  
  
  
  
  
People refer to performance in many ways, but generally most of us think of performance in terms of UI sluggishness.  
  
In fact JavaScript can’t technically perform slowly since it is for most intents and purposes single threaded (ignoring the joke that is web workers), so long running JavaScript code that will take 50 seconds just won’t happen (you will get constant browser warnings). Its all just UI stalls or what w  
  
e call “perceived performance”.  

  
Perceived performance is pretty hard to measure but its pretty easy to see why it sucks on web UI’s: DOM.  

  
To understand this you need to understand how DOM works: every element within the page is a box whose size/flow can be determined via content manipulation and style manipulation. Normally this could be very efficient since the browser could potentially optimize the living hell out of rendering this data. However, JavaScript allows us to change DOM on the fly and actually requires that to create most UI’s. The problem is that “reflow” is a really difficult concept, when you have a small amount of data or simple layout the browsers amazing rendering engines can do wonders. However, when dependencies become complex and the JavaScript changes a root at a “problematic” point it might trigger deep reflow calculations that can appear very slow. This gets worse since the logic is so deep in the browser and its performance overhead you can end up with a performance penalty that’s browser specific and really hard to track.  

  
  
To make matters worse, many small things such as complex repeat patterns, translucency layers etc. make optimizing/benchmarking such UI’s really difficult.  

* * *

## Why Java Is Fast & Objective-C Is Slow  
  

The rest of the article talks a lot about native code and how fast it is, unfortunately it ignores some basic facts that are pretty important while repeating some things that aren’t quite accurate. 

  
The first thing people need to understand about Objective-C: it isn’t C.  
  
  
  
C is fast, pretty much as fast as could be when done right.  

  
Objective-C doesn’t use methods like Java/C++/C#, it uses messages like Smalltalk. This effectively means it always performs dynamic binding and invoking a message is REALLY slow in Objective-C. At least two times slower than statically compiled Java.  
  
  
  
A JIT can (and does) produce faster method invocations  
  
than a static compiler since it can perform dynamic binding and even virtual method inlining e.g. removing a setter/getter overhead! Mobile JITs are usually simpler than desktop JITs but they can still do a lot of these optimizations.  
  
  
  
  
  
We used to do some pretty amazing things with Java VMs on devices that had less than 1mb of RAM in some cases, you can check out the rather  
[  
old blog from Mark Lam  
](https://weblogs.java.net/blog/mlam/archive/2007/02/when_is_softwar.html)  
about some of the things Sun used to do here.  

**  
  
But iPhone is faster than Android?  
  
  
  
**  
  
Is it?  
  
  
iPhone has better perceived performance. Apps seem to launch instantly since they have hardcoded splash images (impractical for Android which has too many flavors and screen sizes). The animations in iOS are amazingly smooth (although Android with project butter is pretty much there too), these aren’t written in Objective-C… All the heavy lifting animations you see in iOS are performed on the GPU using CoreAnimation, Objective-C is only a thin API on top of that.  
  
  
  
  
  
  
Getting back to the point though  
  
  
he is 100% right about JavaScript not being a good language to optimize, it doesn’t handle typing strictly which makes JITs far too complex. The verification process in Java is HUGELY important, once it has run the JIT can make a lot of assumptions and be very simple. Hence it can be smaller which means better utilization of the CPU cache, this is hugely important since bytecode is smaller than machine code in the case of Java.  
  
CPU cache utilization is one of the most important advantages of native code when it comes to raw performance. On the desktop the cache is already huge but on mobile its small and every cache miss costs precious CPU cycles. Even elaborate benchmarks usually sit comfortably within  
  
  
a CPU cache, but a large/complex application that makes use of external modules is problematic. But I digress….

  
Proving that JavaScripts strictness is problematic is really easy all we need to do is look at  
[  
the work Mozilla did with ASM.js  
](http://arstechnica.com/information-technology/2013/05/native-level-performance-on-the-web-a-brief-examination-of-asm-js/)  
which brings JavaScript performance to a completely different place. Remove abilities from JavaScript and make it strict: it becomes fast.  
  
  
  
  
  
**  
  
Are GCs Expensive  
  
**  
  
Yes they have a cost, no its not a big deal.  
  
  
  
  
  
  
ARC is an Apple “workaround” for their awful GC.  
  
  
Writing a GC is painful for a language like Objective-C which inherits the “problematic” structure of C pointers (pointer arithmetic’s and  
  
memory manipulation) and adds to it plenty of complexities of its own. I’m not saying a GC is trivial in a managed language like Java but it is a panacea by comparison.

  
The problem with GC is in its unpredictable nature. A gc might suddenly “decide” it needs to stop the world and literally trash your framerate, this is problematic for games and smooth UI’s. However, there is a very simple solution: Don’t allocate when you need fast performance. This is good practice regardless of whether you are using a GC since allocation/deallocation of memory are slow operations (in fact game programmers NEVER allocate during game level execution).  

  
This isn’t really hard, you just make sure that while you are performing an animation or within a game level you don’t make any allocations. The GC is unlikely to kick in and your performance will be predictable and fast. ARC on the other hand doesn’t allow you to do that since ARC instantly deallocates an object you finished working with (just to clarify: reference counting is used and instantly means when the ref count reaches 0). While its faster than a full GC cycle or manual reference counting its still pretty slow  
  
. So yet you can hand code faster memory management code in C and get better performance in that way, however for very complex applications (and UI is pretty complex not to mention the management of native peers) you will end up with crashes. To avoid your crashes you add checks and safeties which go against the basic performance penalties you are trying to counter.

Furthermore, good JITs can detect various pattens such as allocations that are tied together and unify the memory allocation/deallocation. They can also reallocate elements into the stack frame rather than heap when they detect specific allocation usage. Unfortunately, while some of these allocation patterns were discussed by teams when I was at Sun I don’t know if these were actually implemented (mostly because Sun focused on server GCs/JITs that have a very different type of requirements).

  
The article also mentions desktop GCs being optimized for larger heap spaces and  
[  
a study from 2005  
](http://www-cs.canisius.edu/~hertzm/gcmalloc-oopsla-2005.pdf)  
that “proves it”. This is true for desktop GCs but isn’t true for mobile GCs, e.g. Monty (Sun’s VM) had the ability to GC the actual compiled machine code. So effectively if your app was JITed and took too much space in RAM for an execution path you no longer use much, Monty could just collect that memory (the desktop JIT to my knowledge was never this aggressive).  
  
A proper GC optimized for mobile devices and smaller heap overhead will be slower than some of the better desktop GCs but it can actually reduce memory usage compared to native code (by removing unused code paths). Just so we can talk scales, our code performed really well on a 2mb 240×320 Nokia device and weaker devices than that. It ran smoothly animations and everything, including GC.  
  
  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — July 15, 2013 at 10:22 am ([permalink](/blog/why-mobile-web-is-slow/#comment-24246))

> Anonymous says:
>
> Note that the title of this post is not proper English.
>



### **Anonymous** — July 15, 2013 at 11:19 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21963))

> Anonymous says:
>
> The number one thing I’m seeing here is that you have absolutely no data to back you up whatsoever. For example, 
>
> “A JIT can (and does) produce faster method invocations than a static compiler since it can perform dynamic binding and even virtual method inlining e.g. removing a setter/getter overhead!” 
>
> Nice theory. Here’s another theory. Since JITs have to run side by side with the application, and every cycle a JIT spends optimizing, the application can’t spend executing, a JIT can never match a static compiler. Not to mention, congratulations on inlining your shitty unnecessary getters and setters- in other languages, they wouldn’t be written in the first place. 
>
> Prove me wrong- but since you present absolutely no evidence for anything you’ve said, then I’m not really feeling the pressure here.
>



### **Anonymous** — July 15, 2013 at 11:27 am ([permalink](/blog/why-mobile-web-is-slow/#comment-24157))

> Anonymous says:
>
> Your unconditional statements are simply incorrect 
>
> “in fact game programmers NEVER allocate during game level execution” 
>
> That’s simply not true. Complicated games might use pooling memory managers, but from the point of actual code, it’s freeing and allocating data allright. On some operating systems using pooling memory might just not be needed for a fast execution, because normal allocators can cope with the task. 
>
> “So yet you can hand code faster memory management code in C and get better performance in that way, however for very complex applications (…) you will end up with crashes. To avoid your crashes you add checks and safeties which go against the basic performance penalties you are trying to counter.” 
>
> You are assuming the only ways to use manage memory is either raw C or fully GCed Java. How exactly is “pretty slow” for ARC we’re talking about? You seem to forget that deterministic destruction does exactly mean we can destruct when it’s convenient, not when the GC chooses so. As for crashes, I think you simply lack knowledge about alternatives to the extreme ends of the scale.
>



### **Anonymous** — July 15, 2013 at 11:30 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21888))

> Anonymous says:
>
> Fair enough, I was far more lazy than Drew and just pulled out of my experience rather than worked 😉 
>
> You can google project monty and read some of the stuff that came out from Sun regarding mobile JIT’s [http://www.slideshare.net/C…](<http://www.slideshare.net/Cameroon45/the-project-monty-virtual-machine>) 
>
> You can also pick up an Asha device and play with it a bit after reading its CPU/RAM spec… Its no iPhone, but with those specs its pretty amazing. 
>
> In objective C you use properties which map to… getters/setters and the same applies for all property types. 
>
> JIT’s do have an overhead, however since every application spends most of its time doing nothing there is plenty of time for a JIT to optimize. Unless a JIT is a caching JIT you will notice some overhead on startup (unless its a caching JIT). 
>
> Notice that I don’t think this can be “proved”, perceived performance is too difficult to measure properly and the JIT overhead is a flaky hard to measure property. I would prefer that people understand the difference between “soft facts” and “hard facts” e.g. JavaScript as it is now is hard to optimize is a hard fact. JIT/GC’s are inherently slow is debateable (and I take the position that they are not).
>



### **Anonymous** — July 15, 2013 at 11:38 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21638))

> Anonymous says:
>
> Never might have been a strong word, but I think we generally agree. 
>
> I pulled that out of my experience working for EA’s Janes franchise in the 90’s (that was C++ code) and with newer mobile game developers today. If you have a rendering intensive operation you want every bit of CPU. Allocators and deallocators are slow and generate memory fragmentation which requires compacting. 
>
> True ARC is more predictable in terms of performance, I specifically made a point of mentioning how a GC cycle can crash framerate even for a fast GC. 
>
> My point is that you can use GC and get both decent performance without a major memory overhead increase. Yes you would need to be aware of memory management, but you need to be even more aware of that when using ARC.
>



### **Anonymous** — July 15, 2013 at 11:51 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21784))

> Anonymous says:
>
> ARC does not “Immediately dealloc” the object unless you specifically gave it a weak (Auto zeroing) reference. A strong referenced object will be held onto until you zero it out yourself so if there’s a specifc reason not to dealloc an object while you’re executing then just *don’t* until you’re done doing whatever Uber important process. Now this is almost never an issue however I bring it up in reply to your equally unlikely case of specific object dealloc actually slowing something down. Also lets keep in mind you can jump in and out of C at any time so if you have some really low level stuff just write it in C, use the correct tool for the Job OR .. Why we don’t use JS to do jobs that C should be used for and don’t use C to do jobs JS should be used for. Got it? Kthx Bye.
>



### **Anonymous** — July 15, 2013 at 11:56 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21856))

> Anonymous says:
>
> Where did I imply that ARC immediately deallocates? If I did so I need to fix that. 
>
> I specifically discussed the approach of going into C where I covered the iOS performance explaining why core animation is so fast. 
>
> Deallocating is a slow operation in any language, its slow in C. Its slower in C++ (destructors) and slower yet in Objective-C. Arc also adds the overhead of reference counting pools (which isn’t big but its there). 
>
> GC arguably has a bigger overhead and is less predictable than ARC, I never denied that.
>



### **Anonymous** — July 15, 2013 at 11:56 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21908))

> Anonymous says:
>
> So, your response to a very well researched and documented post is to post your gut feeling, and then argue that the reader should be doing the research to back up your claim? Show us the numbers. And it is nonsense to say that it can’t be measured properly. Of course it can. Maybe *you* can’t. More likely you just *haven’t*. Which is fair, but it does mean that this post is just empty words.
>



### **Anonymous** — July 15, 2013 at 12:05 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21902))

> Anonymous says:
>
> @jalf I don’t see the animosity here? 
>
> I started by explaining that I write an opinion out of my personal experience of writing VM’s for Sun Microsystems. I don’t need to research the field in which I have been coding for the past 14 years (this specific field I have been coding MUCH longer than that). 
>
> Which statement specifically do you have an issue with? I don’t think a single statement outright contradicted a fact claimed in Drew’s article other than the GC paper which is pretty flawed for the mobile use case (it measured desktop VM’s you can’t take that into mobile). 
>
> If you think JIT’s have an overhead (which is a claim I didn’t see in Drew’s article) you are correct, that’s a fact. Is it a big deal? 
>
> That depends. 
>
> I linked to Mark’s article in the blog where he provides some deeper technical assembly opcodes to back some of the claims of JIT overhead. What sort of proof are you looking for?
>



### **Anonymous** — July 15, 2013 at 1:11 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21971))

> Anonymous says:
>
> Thanks! Finally some deeper thought on Drew’s article. I think the major problem is that Drew pulls out a lot of references and citations, but I don’t necessarily think they back up his claims fully since they apply for different situations. 
>
> In my experience, C-style languages are hard to beat, but it does require an extraordinary amount of time to get that code right and optimized. Usually, a GC will buy you some development speed and if you know what you are doing, it also takes some quite nitpicky tuning to get C to be more allocation efficient than a wel-written GC. 
>
> And for the nonbelievers: Many malloc()/free() routines nowadays are multi-core aware and they do contain garbage-collection style tricks to speed them up. So even manual memory management uses some of the same tricks as state-of-the-art garbage collectors.
>



### **Anonymous** — July 15, 2013 at 1:50 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21695))

> Anonymous says:
>
> “ARC on the other hand doesn’t allow you to do that since ARC instantly deallocates an object you finished working with.”
>



### **Anonymous** — July 15, 2013 at 3:09 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21760))

> Anonymous says:
>
> Arghhh, one doesn’t put an apostrophe with plural cases: GCs, JITs, VMs, etc.!!!!
>



### **Anonymous** — July 15, 2013 at 3:14 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21894))

> Anonymous says:
>
> Great follow-up article, Shai. I think the criticism you’re receiving by some commenters here isn’t really fair. 
>
> I posted the following comment on Drew’s article, that I’m reposting here just because I think it supports what you’re trying to say here: 
>
> I just want to offer an alternate take on the paper cited ([http://www-cs.canisius.edu/…](<http://www-cs.canisius.edu/~hertzm/gcmalloc-oopsla-2005.pdf>)) regarding performance vs heap-size of garbage collectors. 
>
> If you focus on the “best” garbage collection algorithm they used in their experiments (GenMS) the performance hit is only about 1.75x with a heap size of twice the minimum required footprint. By the time you reach available memory of 3 to 4 times, the size, performance reaches about parity with manual memory management. 
>
> Given that this study was done in 2005, and assuming that GC architects are aware of these results, I think it is fair to assume that modern day garbage collectors will perform at least as well as the best algorithms in this study. 
>
> Therefore, the conclusion that you need 6 times more memory available than your app requires in order to have good performance in a managed environment is an exaggeration. In reality you can probably achieve good results with less than twice the amount of memory. 
>
> This certainly seems like a reasonable trade-off in all cases except the most performance-critical applications.
>



### **Anonymous** — July 15, 2013 at 3:20 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21752))

> Anonymous says:
>
> Perhaps the phrasing was wrong here, I didn’t want to get into the details of the reference counting algorithm and how it works internally.
>



### **Anonymous** — July 15, 2013 at 3:25 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21942))

> Anonymous says:
>
> Finally a comment I can answer with “fixed that”.
>



### **Anonymous** — July 15, 2013 at 3:26 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21749))

> Anonymous says:
>
> Terms like “perceived performance” and “speed” are too vague. Think: thoughput,latency and the jitter on them. Android does have at least one enormous problem relative to iOS, and it’s latency/jitter; which is really obvious when making real-time audio apps (ie: audio responds to finger movements within 5ms latency and jitter). Most of the Android devices I have used exhibit high latency in the user interface for everything, including the web browser. A lot of IOS apps use very little Objective-C and are mostly in C for the exact reasons you mentioned; to have the app stop doing alloc/free while the app runs (some places you have no control, like with touches coming in from the OS, etc). It’s really unfortunate that none of the popular operating systems are based on real-time operating systems, and none of the common languages are appropriate for making real-time applications; and actually kind of ironic given that mobile devices fit the profile of embeded devices doing signal processing (phone signals, accelerometers, audio, camera, etc).
>



### **Anonymous** — July 15, 2013 at 3:32 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21738))

> Anonymous says:
>
> Exactly, also high performance games in Android are mostly written in C. I’m not a huge Dalvik expert and as far as I understand they optimized for very different things (Sun engineers claimed Sun’s JIT’s were MUCH faster than Dalvik but I didn’t measure it myself). 
>
> However, from my experience working with Android I’d bet this is more due to the shoddy work they did with media than anything else. They implemented the media in the native layer where you have to go back and forth from Dalvik to native for every little thing and those trips back and forth (and through threads) are just performance killers. Obviously this is a complete guess.
>



### **Anonymous** — July 15, 2013 at 3:32 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21755))

> Anonymous says:
>
> “[…] congratulations on inlining your shitty unnecessary getters and setters- in other languages, they wouldn’t be written in the first place” 
>
> Ignoring the tone of your post for second, are you saying that C++ for example does not have getters/setters ? Besides inlining in the JVM goes beyond getters and setters. 
>
> Of course you forgot to mention that a JIT compiler has access to information about the actual HW it is running on (memory, CPU type & version, cache size, …) and program hotspots that a static compiler have no access to. But it would not fit your story …
>



### **Anonymous** — July 15, 2013 at 3:55 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21769))

> Anonymous says:
>
> In a this-tech-vs-that-tech debate, there is another variable only touched on in Drew’s article, which is the the programmer’s quality of execution of development itself. That could be limited by skill level or even by the rush of dealing with deadlines. Even though manual C-style memory management helps with writing a program that fits in a small memory profile, that really doesn’t matter if it only runs for 3 seconds before segfaulting. GC languages make it impossible at the lowest level to accidentally release an object that’s in use. I think Obj-C ARC deserves credit for narrowing the avoiding-stupid-mistakes gap to some degree, but it still can’t offer the guarantees that a GC’ed language can. 
>
> This is not theoretical for me, I’m actually writing a cross platform OpenGL/DirectX graphics intensive app using MonoGame and Xamarin’s cross platform C#. Using C# on an iOS game while keeping a high framerate means doing manual memory management. In that situation, the GC ironically isn’t really used for memory management for most of the program’s execution, but rather as just a a part of basic architectural protection from access violations. And it’s awesome. 
>
> In my opinion, this discussion really boils down to memory management, since (outside the DOM, as Shai noted) execution speed differences don’t add up to enough to matter for probably 99% of apps. If you’re writing heavy numerical calculations etc., then you’re probably invoking a native library for that part. 
>
> The goal is not to run out your project’s budget due to being a perfectionist: “real artists ship.”
>



### **Anonymous** — July 15, 2013 at 3:59 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-24232))

> Anonymous says:
>
> Agreed, the biggest factor in performance is developer skill.
>



### **Anonymous** — July 15, 2013 at 4:32 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21698))

> Anonymous says:
>
> The “Class 3” argument from scripting language people assumes that the hardware is underutilized – like a big desktop machine. 
>
> If each instruction expands into 10 for the sake of it being in FooScript, and you run it at 100% utilization on your desktop… then port it to C and it runs at 10% utilization…but they might complete in roughly the same time because the C code spends 90% of its time waiting on devices and network responses. And here it looks like there was no justification for porting it to C. 
>
> But if you assume that battery consumption is proportional to utilization, then it matters. If the demand spikes to 10x what it previously was, then the FooScript implementation can no longer keep up, queueing work at 10x the rate that it can do it. The FooScript app is already at 100% utilization and won’t go faster, running at 10% of the C implementation which is now also at 100% but keeping up. 
>
> This is what will happen when your have 10 cloud-based instances that would ran at 10% on separate VMs, and your cloud provider stuffs all 10 apps into one machine to reduce its own power consumption. (I think this a lot of why Google is pushing Go, btw.) 
>
> Power consumption is proportional to the cube of clockspeed, from what I have read. That implies slowing down the cores as much as possible while having as many cores as possible. 
>
> Then you have the phenomenon where 1 user request hits 100 servers in the background, and 99 of them come back in a few milliseconds, but 1 comes back in 1 second; which would cause the entire request to take 1 full second. Etc. You get to the point where real-time responses start to matter a lot on what seemed like a throughput oriented system when you started. 
>
> Don’t get me wrong… C is a horrible language for writing applications in, because it’s so unsafe, and pointer aliasing blinds the compiler to the obvious. People raised on desktop systems see the answer in Python/Java/C#, etc. But they bring convenience while throwing all resource consumption guarantees out the window. 
>
> Between handling massive concurrency (to not bottleneck at any one core or resource), to the extra complexity in dealing with distributed computing… The current tools don’t resemble the problems we are dealing with now…. (… SIMD/Vector/GPU, co-routines, real-time deadlines, lock-free algorithms, message-passing, power reduction …). The underlying OS needs to be real-time to support any real-time applications. A lot of apps will need to be real-time. Most of them will need strong guarantees on resource consumption as well.
>



### **Anonymous** — July 15, 2013 at 4:34 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21842))

> Anonymous says:
>
> Rob, 
>
> What really matters at the end is what is perceived by the user. 
>
> As far as I know the best Android devices are getting better – and cost as much as Apple ones -, though they don’t feel as fast as iOS ones, even “old” phones like iPhone 3GS or 4 (I have a 4 and even heavy games are pretty fast). 
>
> The problem would be the that most Android phones are in the cheap market, using old Android versions, with poor hardware. In that niche, maybe Firefox OS could do a better job. 
>
> That might not be the problem with Java itself, that always takes the blame, but the ecosystem it’s built. Also, for newer Android versions, it seems that even thought we still have a very fragmented market, which makes the devices not be as good as iOS ones, Google has done a lot of improvements. 
>
> Particularly, I think we all benefit from free market and having more than one good device, but I stick with Apple for the moment [and it’s usability].
>



### **Anonymous** — July 15, 2013 at 4:39 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21763))

> Anonymous says:
>
> While I agree with most of your points I think Java is ideal for mobile exactly for these reasons. Embedded systems (as phones are) are really hard to optimize for, by having a relatively high abstraction layer you allow the OS vendor a lot of leeway in performing such optimizations. 
>
> If you will read Marks posts which I linked in the article you will see that a lot of the cost in Java VM is actually very low in terms of power supply.
>



### **Anonymous** — July 15, 2013 at 4:53 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21832))

> Anonymous says:
>
> “So yet you can hand code faster memory management code in C and get better performance in that way, however for very complex applications (and UI is pretty complex not to mention the management of native peers) you will end up with crashes. To avoid your crashes you add checks and safeties which go against the basic performance penalties you are trying to counter.” 
>
> False dichotomy. 
>
> In modern programming languages, like Rust or Parasail, we can get zero-overhead automatic resource management and 100% memory safety: 
>
> – [http://pcwalton.github.io/b…](<http://pcwalton.github.io/blog/2013/06/02/removing-garbage-collection-from-the-rust-language/>) 
>
> – [http://air.mozilla.org/regi…](<http://air.mozilla.org/region-based-storage-management-parasailing-without-a-garbage-chute/>) 
>
> It’s worth contrasting this with older, traditional languages, like Java or OO-COBOL, which use very old and inefficient methods, that don’t even work well anyway, like GC (which only applies to memory [instead of resources, like threads, in general], doesn’t guarantee memory safety [hello java.lang.NullPointerException!], and is very hard to get it right in an increasingly complex parallel environment [hello GC thread contentions!]), and leads to lower programmer productivity (who wants to manually deal with managing resources with manually writing boilerplate constructs like try/finally pattern, or try-with-resources? weren’t we supposed to leave manual resource management in the old C days? oh, wait, I forgot, Java hasn’t really improved upon C other than some extremely limited aspects of memory management, and still fails *hard* for all of the other resources). 
>
> GC was invented in the 1950s, it’s ancient, unnecessarily slow, and limited; the world is moving on — let’s not be stuck in the GCed past! 😉
>



### **Anonymous** — July 15, 2013 at 4:54 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-24249))

> Anonymous says:
>
> “ARC on the other hand doesn’t allow you to do that since ARC instantly deallocates an object you finished working with.” 
>
> >instantly
>



### **Anonymous** — July 15, 2013 at 4:55 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21713))

> Anonymous says:
>
> I clarified that in the post, instantly when the ref count reaches 0.
>



### **Anonymous** — July 15, 2013 at 5:02 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21825))

> Anonymous says:
>
> Fair enough. 
>
> The discussion was about Objective-C, C, JavaScript and other managed languages though. 
>
> Obviously we should throw away all ideas invented in the 20th century like that damn microprocessor and move forward to those optical quantum machines 😉
>



### **Anonymous** — July 15, 2013 at 5:08 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21665))

> Anonymous says:
>
> Well, OK, fair enough back at you, since RBSM was also invented in the 20th century (just later than GC) I guess we can keep some stuff 😉 
>
> I guess I just get occasionally tired/frustrated of that false dichotomy, all too prevalent in the managed-vs-native discussions, where it’s all too often black-and-white (especially with statements suggesting getting crashes without GC, as if the NullPointerException didn’t exist). 
>
> I wish we could move beyond that and consider more modern solutions, that’s all 🙂
>



### **Anonymous** — July 15, 2013 at 5:21 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21810))

> Anonymous says:
>
> “doesn’t guarantee memory safety [hello java.lang.NullPointerException!]” 
>
> Actually a null pointer exception is an example of memory safety in action. In unmanaged languages, if you try to access uninitialized memory, you don’t get a null pointer exception. You just get access to the memory containing who knows what. This is the source of most serious software security exploits.
>



### **Anonymous** — July 15, 2013 at 5:53 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21775))

> Anonymous says:
>
> I think the reason you are getting so much animosity is that you have taken Drew’s post as an excuse to fight a battle which is not the battle Drew is interested in — and you’ve jumped into this hijacking with nothing useful to add to the situation. 
>
> Drew’s point was that Javascript is slow, and that it therefore inappropriate for certain types of code. 
>
> He gave some hypotheses for why it is slow, but the hypotheses are less important than the claim. 
>
> You have taken his point to ignore the issue of Javascript performance and launch another round of “rah rah Java is awesome; Objective C sux”. It’s your blog, you’re entitled to do that. But don’t be surprised when people who read this post and hope for something as informed as Drew’s post are disappointed to see yet another damn content-free argumentative post of the sort we’ve all read a million times in our lives.
>



### **Anonymous** — July 15, 2013 at 5:59 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21817))

> Anonymous says:
>
> Glad someone is making a counter argument. I posted on the other thread and got moderated away when I disagreed. For the record DOM performance is definitely the biggest hit in my experience. I discovered the Mac version of Chrome appears to not be H/W accelerated when drawing – made painfully clear when a web app I was working on was smooth in VMWare in IE8 on XP(!), but running at about 10-15 FPS native OS. Safari (native) ran it at 60FPS. The code was a simple JQuery transition, so hardly anything unusual. 
>
> Regarding the language war, you seem keen to add Objective-C into the mix. Objective-C has a vital secret weapon you make light of – just recode the slow bit in C. This has a much larger effect than all the JITing of Java, and Obj-C makes it trivial. E.g. Arbitrary JSON parser took 50 milliseconds on a certain block of data in Objective C. This became 10 microseconds when converted to a raw C SAX style parser – yes its convoluted, but this type of performance boost is much harder to come by with JITter clutter in the way. 
>
> PS Sometimes memory must be freed and you don’t mention that you can null objects giving a clue to the GC to free memory now – I found it a huge boost in complex realtime Java UI.
>



### **Anonymous** — July 15, 2013 at 6:42 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21807))

> Anonymous says:
>
> Actually, it depends on the definition you’re using: 
>
> “Memory safety is a crucial and desirable property for any piece of software. Its absence is a major source for software bugs which can lead to abrupt termination of software execution, but also, and sometimes even more dangerous, can be turned into a malicious tool: most of the recent security vulnerabilities are due to memory safety violations. ” 
>
> Source: [http://dl.acm.org/citation….](<http://dl.acm.org/citation.cfm?id=1690881>) 
>
> Yes, in the Java world the second part is usually assumed the sole focus (and it’s granted that it can be “even more dangerous” to access memory this way, I don’t think anyone disputes that), but that doesn’t mean the first part (abrupt execution termination) is not a problem. It’s certainly a problem for the end-user, who in practice doesn’t care whether the software crashed because of a segmentation fault of a C program or an unhandled java.lang.NullPointerException in a Java program. 
>
> And GC is not enough to guarantee that — it’s worth noting that this is the official stance taken in the draft JSR-302 Safety Critical Java (SCJ) specification: 
>
> [http://dl.acm.org/citation….](<http://dl.acm.org/citation.cfm?id=2402685>) 
>
> So, in fact, SCJ has stronger memory safety guarantees while dropping GC (would you call it “unmanaged”? :]). 
>
> Similarly, modern programming languages also offer stronger memory safety guarantees while dropping GC (see above). 
>
> And I think it’s fair to characterize some of these as unmanaged, too.
>



### **Anonymous** — July 15, 2013 at 7:13 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-24252))

> Anonymous says:
>
> My thoughts: 
>
> 1) The jazz about DOM is absolutely true. JavaScript can affect any element in a DOM tree and changing elements within the DOM tree forces a web-engine to perform relayout, which just plain sucks. Additionally, all open source web engines (Gecko, any WebKit flavor ad Blink) have that their rendering stack is entirely imperative. The punchline is this: imperative rendering is fine for CPU’s but horror slow for GPU’s. Most WebKit flavors essentially do all drawing to offscreen buffers with the -CPU- and then instruct the GPU to perform compositing of those buffers to the screen. Sadly, the way that WebKit works, this is the best path. Indeed, if one were to use GL backed SKIA (or for that matter a GL backed QPainter for QtWebKit), performance is often SLOWER than with the CPU backed rendering (this is the case for mobile strongly at times). Needless to say, web rendering is much, much slower than it should be. 
>
> 2) When folks say keep all allocations out of one’s main loops to make sure GC does not activate, I cringe inside. What that means is this: for games all objects and memory must be allocated at start up. The result is that the game then make it’s own “object/memory” manager to make sure the GC does not kick in. The net effect is that then such games in reality end up doing manual memory management anyways. For UI’s it sucks. It sucks because then a developer needs to *somehow* make a user event driven program that somehow delays allocating until after it display the response to an event.. that means that objects needs to be pre-allocating before the event, and that after the screen is updated, the application generates it’s own event to itself to perform the next pre-allocation and one must home that the user does not send another event too fast if the GC gets invoked. So that just plain sucks. 
>
> 3) On the subject of GC and graphics. In an application that uses hardware to render (for example uses OpenGL, JS woubd be WebGL), an application will need to -by hand- make the necessary GL calls to release graphics resources. So all the graphic stuff, needs to be freed manually. The up shot is that in a GC environment, one then needs to make sure that all the objects using the release graphics resource and “not in use”, just as the case for manual memory allocation. 
>
> That GC to perform well requires something like 2-6 times the actual memory used (depending on what one references) can be a major show stopper. Additionally, that GC is not deterministic can also me a major show stopper. 
>
> As for the war between JIT/interpreted languages and compiled languages: compiled is just going to win. Indeed, the Java.asm jazz at best is only twice as slow as native C code. You read that right, at best only twice as slow. 
>
> The sick twisted thing about the Java.asm thing: it is an asm like spittle to which to compile C programs with the hope that the JavaScript interpreter will run fast. In all honesty this is somewhat silly: the correct solution is to pass along LLVM byte code and let the local system convert that to native code. That conversion is very, very fast and can more often than not be performed while the resources(graphics, audio, etc) of a WebApp are downloading. 
>
> Nevertheless, WebApps are going to suck because they must render their UI’s through CSS/HTML and get them respond to user events by modifying the DOM with JavaScript. The only way out of this mess, is to not use CSS/HTML to format the UI, but rather bring up a canvas and draw the UI one self. Before anyone jumps up and down and says that is slow, it is only quasi-slow. For WebKit the 2D Canvas is implemented with the exact same stuff as the drawing of web page elements anyways. The only catch is that when doing the canvas thing, there might be an extra blit of your UI (from canvas offscreen buffer to screen or offscreen buffer of the layer of the canvas).
>



### **Anonymous** — July 15, 2013 at 9:20 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21792))

> Anonymous says:
>
> md, so tell me what really large applications have been written in these languages you’re talking about. Applications I can buy or use. New languages are infinitely fast and they can do everything better than languages that are already deployed. Until a few tens of millions of dollars have been spent writing applications with them. Then a realistic view is possible. Not before. 
>
> I remember the all the promise of Java and C#. But at this point I know of companies that have spent hundreds of thousands and in some cases millions or tens of millions of dollars to attempt to build significant business critical applications in these languages and failed. Tell me why your new shiny magical languages are better. And provide proof. Proof means successfully delivered, non-trivial applications. 
>
> The languages you cite may be better. But maybe not. And you can’t tell yet.
>



### **Anonymous** — July 15, 2013 at 10:52 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21865))

> Anonymous says:
>
> One benefit of native language is that is something is slow you *can* optimise it. Objective-C messaging slow you down in a tight loop? Stick to C for that loop or a well known technique since the NextStep days is to use methodForSelector: and get direct access to what you want to call to remove the messaging cost where it matters. On the other hand, if the GC gets in your way, you’re done. 
>
> So yes, you are right about the cost of messaging, but the programmer can fix the problem where it matters. It’s pretty hard to bypass the JIT or GC if I would need to…
>



### **Anonymous** — July 16, 2013 at 1:13 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21744))

> Anonymous says:
>
> Fair enough, I did take his post in a different direction although the reason I responded was that so many people turned to me asking for my take on it. 
>
> It seems that people weren’t clear about my opinion regarding Drew’s post (which is pretty positive I clarified it further on the top). 
>
> I don’t think I changed the subject, I said that reflows are the problems most people perceive when they see slow web performance which is the one thing he didin’t discuss and is very widely documented online. Then I discussed my personal experience (and gave some relevance) links where the situation he describes isn’t black and white where JIT/GC == slow. 
>
> I did flamebait Objective-C that’s true. But I’m kind of tired of Objective-C developers calling themselves “native” when it really isn’t C. You pay a price for Objective-C that you don’t pay even in Java for messages, so deriding slow performance of all managed code in the world and while being an iOS developer is something that needs to be taken down a notch. 
>
> I’m surprised by the animosity from some people who seem to actually agree with most of my points?
>



### **Anonymous** — July 16, 2013 at 1:28 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21787))

> Anonymous says:
>
> Thanks for the detailed comment. 
>
> 2\. I don’t like that either. However, I clarified this a bit. This is only true for high rendering FPS games where you code in OGL anyway. Allocations would be done in the GL memory which is native anyway and so do not fall under the GC allocator. 
>
> 3\. I don’t think the 2 times memory overhead is factual. It is based on a study conducted on the desktop. As I said, just pick up an Asha device and benchmark it, Nokia has 2mb devices and they are very fast with the GC. 
>
> True since the GC is proprietary I can’t benchmark using the studies methodology but I can tell you that mobile device GC’s are very different from desktop GC’s. It is non-deterministic though which is problematic for some use cases and requires some defensive programming. 
>
> Let’s agree to disagree on JITs 😉
>



### **Anonymous** — July 16, 2013 at 1:31 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21735))

> Anonymous says:
>
> Agreed, the ease of writing C in Objective-C is great! 
>
> Its also possible (although not as easy) in Java to invoke native C code and that is done for critical code.
>



### **Anonymous** — July 16, 2013 at 3:42 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21706))

> Anonymous says:
>
> If you are rendering on the CPU you are doing it wrong. 
>
> Which might explain a bit of the other nonsense.
>



### **Anonymous** — July 16, 2013 at 4:35 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21692))

> Anonymous says:
>
> Read the other comments here (rather than being Anti) specifically about Webkit and GPU optimizations for DOM. 
>
> The heavy lifting is done by the GPU but want to keep the CPU in low utilization to feed the GPU so this doesn’t really negate anything. Furthermore, with mobile system on a chip design the separation of CPU/GPU isn’t as clean as it is on the desktop. 
>
> Hopefully you will be less “anti” and more proactive.
>



### **Anonymous** — July 16, 2013 at 6:15 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21802))

> Anonymous says:
>
> We’ve been doing that with Android, and I can say this is a yuck solution. Firstly all the debuggers are broken. You can sometimes get them stable enough to debug, but going seamlessly between Java and C? No. This leaves you log debugging one side and then log debugging the other. A big time waster. Admittedly tools can be fixed, and beaten into something sort of working, but they aren’t easy or clean. 
>
> Second Java contexts just look alien in C. Manipulating the data leads to lots of construction code. Double (different) prototypes of every function. Linker fun with C++ vs C… the list is long. 
>
> Objective-C or should I say (Objective-C++?) really is in a different world here.
>



### **Anonymous** — July 16, 2013 at 6:15 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21956))

> Anonymous says:
>
> “This is only true for high rendering FPS games where you code in OGL anyway” 
>
> That statement does not really make any sense: OGL is an API, that API has bindings to multiple languages where the languages spread across compiled, interpreted, managed and unmanaged. On the subject of managed language, the whole point of GC is so that a a developer does not need to devote the time, and hence the skill investment, to do manual memory management. Once you have something OGL where graphics resources need to be allocated and deallocated by using the API, a developer needs to do it. The terrible catch is this: if a developer has not done this a great deal and does not have the skill to do it, they are hosed. 
>
> Going further: for an application where performance guarantees are required (essentially any user event driven UI) then managed memory is a liability. In an idea world, it would be nice to have an option to be able to turn the GC on and off. Off during a loop, on during idle. The catch is that for user event driven programs, the GC needs to be done when an event comes… or the ability to interrupt the GC when the event comes, but that requites support within the VM and language. Atleast it is doable on paper though. 
>
> In an ideal, potentially fantasy, world a GC would not stop the program at all. I think that the overhead to do that though would be horrible. 
>
> At any rate, from my perspective, if a developer cannot accurately track what they have or have not allocated then that developer does not have any real idea how many bytes their application really consumes, as such it’s suitability on a mobile device is questionable. What remains for the main use cases of GC’s is for to handle the icky case of multiple users for a single object together with circular references.. the exact cases where reference counting and object owner ship are unclear… however, more often that not a little clear thinking kills those issues off. 
>
> I admit though, from a get code out the door faster point of view, GC can be great and it stops memory leaks… but it has a huge performance penalty that is very spiky and unpredictable.
>



### **Anonymous** — July 16, 2013 at 6:18 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21880))

> Anonymous says:
>
> Calling C code(or other compiled languages) from a Java VM has a pretty high overhead though, so it needs to be done with care and taste. Additionally getting the data between the two is a pain.
>



### **Anonymous** — July 16, 2013 at 7:01 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21702))

> Anonymous says:
>
> I agreed with Sandy’s post and with both your comments. JNI is painful, doable but painful.
>



### **Anonymous** — July 16, 2013 at 7:09 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21871))

> Anonymous says:
>
> Did you use OGL bindings for Java? 
>
> They require that you manage texture memory manually (just like OGL in C). 
>
> There are hard realtime GC’s for Java that have consistent predictable GC pauses but they aren’t used in mobile programming since that isn’t considered an RTOS. There are also concurrent GC’s but those are designed for huge server heaps where stopping the world to GC can take many seconds. 
>
> There are many things GC authors can do and actually do, however when it comes to framerate sensitive games its hard to rely on that vagueness. Just to be clear, I’m not advocating the writing of framerate sensitive OGL code in Java. I’m just having a fun theoretical debate 😉 
>
> For mobile game programming, if you need FPS nothing beats C.
>



### **Anonymous** — July 16, 2013 at 8:57 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21617))

> Anonymous says:
>
> Sorry, but thats the problem with your article. Too many guesses and wild claims with very 0 backup. You know, computer science is still considered ‘science’ for a reason 😉
>



### **Anonymous** — July 16, 2013 at 9:04 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21757))

> Anonymous says:
>
> Pip. I’m using my actual name and experience here as a reference. Feel free to check my work on OSS software and linked in page to see that I am fully aware of the facts here. 
>
> Where did I claim something I can’t substantiate as a fact without explicitly separating it as an assumption that is hard to substantiate? 
>
> Where did I make a wild claim that contradicted something? DOM reflow having an overhead? Google it. Pretty obvious. Objective-C messages being slow (linked in the comments), memory allocation/dealocation being expensive (pretty easy fact). GC has an overhead but its manageable? 
>
> Science also includes interpretation for the data, the facts here are still in the flexible stage where they can fit many different theories otherwise we would all be programming with the “best language” and “best OS”.
>



### **Anonymous** — July 16, 2013 at 9:27 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21929))

> Anonymous says:
>
> I would agree, if the option of semi-automatic GC were there. but, no!
>



### **Anonymous** — July 16, 2013 at 9:32 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21627))

> Anonymous says:
>
> “Are GCs Expensive 
>
> Yes they have a cost, no its not a big deal.” 
>
> I think this is the best example ever of what i meant !! 
>
> You simply say it is negligible, while there are so many article already showing that that is not the case, and that is not the case with Java !!!
>



### **Anonymous** — July 16, 2013 at 9:41 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21979))

> Anonymous says:
>
> Actually the article Drew linked to showed that GC’s perform REALLY well they only take up too much memory (that’s in 2005). Embedded GC’s used in J2ME have no such overhead (e.g. Nokia devices with less than 2mb of RAM performing really well). Problem is that you can’t really test those GC’s against “no GC at all”. 
>
> However, my claim isn’t about that. If you are writing an FPS heavy game (for which C is still the best, not Objective-C or Java) or a quick animation within your app you would do anything to avoid GC and GC aware allocations (just pre-allocate the memory). This isn’t hard to do. 
>
> The lovely part about a GC is that even if it is 5 times slower it runs on idle CPU. If you don’t have plenty of idle CPU on a mobile device then your problem isn’t GC, its battery life.
>



### **Anonymous** — July 16, 2013 at 10:17 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21964))

> Anonymous says:
>
> On that note, I know of companies that have spent millions of dollars building business critical applications with java and c# and have gotten a good return on investment for them.
>



### **Anonymous** — July 16, 2013 at 10:24 am ([permalink](/blog/why-mobile-web-is-slow/#comment-24158))

> Anonymous says:
>
> Not to mention, most companies build their business around C, *Java*, *Objective-C* and *C++*, if we are not mentioning only web languages. 
>
> If we were not to consider Desktop or foundation software, probably we would mostly see Java on top and then C# and PHP. 
>
> I guess for most business what matter is the value delivered to the user. I mean, it can’t be slow, but it doesn’t need to be real-time or close to it. 
>
> Tiobe index: [http://www.tiobe.com/index….](<http://www.tiobe.com/index.php/content/paperinfo/tpci/index.html>)
>



### **Anonymous** — July 16, 2013 at 12:27 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21889))

> Anonymous says:
>
> Sure it’s only true how the user perceives the performance in the end. But it’s not just lag waiting for web pages to download on a mobile device; the battery life plays a lot into the perception of how good a mobile device. 
>
> But my perspective: I spent a few years building music instruments on iPad/iPhone where an extra 10ms of latency is the difference between something that’s playable and something that’s a useless piece of junk (ie: you can’t do fast arpeggio and scale runs with high latency/jitter). 
>
> I use Java/C#/Python, etc in my day job for server apps, with only a few spots in C, and wish the port all of that work to Java. 
>
> But on mobile devices, the apps are small enough that C is usually ok, and there isn’t enough headroom for a nice language. Real-Time is the main issue, battery life the second issue. Maybe at some point, Go/Rust/Parasail might be alternatives; even thinking about Ada now. As ubiquitous as C is, every API you use has its own set of (possibly undocumented) rules on what it does with its pointers, and it’s basically a useless partner in peer-reviewing for the kind of bugs that any modern compiler would find given a properly designed language.
>



### **Anonymous** — July 16, 2013 at 1:19 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21639))

> Anonymous says:
>
> pip, If you don’t like it, don’t use it. If you think it’s too unpredictable, don’t use it. But saying it’s universally inadequate for event driven UIs is just silly. Even on mobile devices, I don’t think it’s UNIVERSALLY true. 
>
> The first event-driven UI I ever used was something called Smalltalk/V, which ran on top of MSDOS much faster than Windows and provided a development environment, VM and room for applications in 64K. It’s successor, V/286 was used to build event-driven, networked applications for factory applications in the late 80s and early 90s, in less than 16MB of memory. You’ve ridden in the output of those factories, above 30,000 feet. 
>
> In that environment, almost any c code I encountered could be written faster in a bytecode interpreted language with a good class library, without a JIT and with garbage collection. Not because the c couldn’t have been faster, but because the amount of code that needed to be written, the deadline, the c developers skill level and the tools and effort available to kill memory leaks. 
>
> That situation still exists today, all over the world, in the corporate sector. And it even may apply in the mobile space, when building apps for less than some number of users. 100, say or 1000.
>



### **Anonymous** — July 16, 2013 at 1:39 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21785))

> Anonymous says:
>
> durkin, I agree with you. I wasn’t saying it couldn’t be done, both are very successful languages. My point was only that Java and C# have not lived up to the hype that was presented about them before any application base existed. 
>
> Lucas, most non-tech companies BUY C/C++ applications, but can’t afford or aren’t willing to pay for custom development in those languages. Java, yes, C# yes, Objective-C probably not unless the company is very large and in a media related business. In June, 2013 total non-Windows OS use on the desktop was about 9%. COBOL is barely visible in the media, but lots of big companies still run on it. 
>
> Lucas, most non-tech companies BUY C/C++ applications, but can’t afford or aren’t willing to pay for custom development in those languages. Java, yes, Objective-C probably not unless the company is very large and in a media related business. In June, 2013 total non-Windows OS use on the desktop was about 9%.
>



### **Anonymous** — July 16, 2013 at 3:29 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21857))

> Anonymous says:
>
> We all know that android has better hardware than iphone because is needed to run the crappy java at reasonable speed. So whatever you’ve tried to explain here is available only in theory if you don’t take everything into account, java is not native and will never run faster than objc.
>



### **Anonymous** — July 16, 2013 at 3:36 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21909))

> Anonymous says:
>
> Android is Dalvik not Java, the VM is completely different. OTOH pick any Asha device and play with it. Its no iPhone in terms of feel (no GPU) but its CPU also sucks and it has 3-4mb of RAM.
>



### **Anonymous** — July 16, 2013 at 5:51 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21903))

> Anonymous says:
>
> @Cristi, I’m not sure that Obj-C’s message passing is faster than Dalvik’s method invocation, after run through the Android JIT compiler. Obj-C is sort of its own animal, and you can’t compare it to other languages under the assumption that it’s exactly equivalent to C/C++. Performance sensitive Obj-C apps regularly drop into C or C++, just like performance sensitive Android Dalvik/”Java” apps do. It’s a bit easier to do that from Obj-C, but at that point it wouldn’t typically be referred to as Obj-C anymore. (Question of semantics, I suppose.)
>



### **Anonymous** — July 16, 2013 at 10:58 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21972))

> Anonymous says:
>
> I take issue with the supposition that it’s impossible to microoptimize when targeting the JVM. Take a look at the implementation of Bagwell’s hash tries used in Clojure — their design actually takes into account CPU cache line widths, and they’re crazy fast as a result.
>



### **Anonymous** — July 17, 2013 at 1:06 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21955))

> Anonymous says:
>
> Apple says that method calls are cached as they are used, after a while it becomes almost as a function call. You can also find the IMP and call it directly. 
>
> “To speed the messaging process, the runtime system caches the selectors and addresses of methods as they are used. There’s a separate cache for each class, and it can contain selectors for inherited methods as well as for methods defined in the class. Before searching the dispatch tables, the messaging routine first checks the cache of the receiving object’s class (on the theory that a method that was used once may likely be used again). If the method selector is in the cache, messaging is only slightly slower than a function call. Once a program has been running long enough to “warm up” its caches, almost all the messages it sends find a cached method. Caches grow dynamically to accommodate new messages as the program runs.”
>



### **Anonymous** — July 17, 2013 at 1:24 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21761))

> Anonymous says:
>
> With all that caching message passing in objective c approaches about 3 times slower than java method invocations. It isn’t hard to set up benchmarks to verify this yourself. I’ve done several tests myself for pure curiosity’s sake.
>



### **Anonymous** — July 17, 2013 at 2:19 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21895))

> Anonymous says:
>
> Shai is right about Java performance, vs obj-c, c, etc. This is not a controversial fact among anyone who has actually measured the language performance on varying platforms. It is also true that iOS has very well designed frameworks that cover this difference, The reality is the whole industry would be better of if Apple had kept their Java wrappers of cocoa and then ported cocoa to actual java just as they ported WebObjects, WebObjects by the way was quite bit faster in its Java incarnation. 
>
> As for the silliness of commenters about allocation speed etc, well look any if us worth our weight know you are hinting to your runtime with your allocation patterns and it’s better to have your app come to steady state and then not allocate just to do normal work, like render a frame, etc. 
>
> Objective c is like the bad hair on a beautiful woman (iOS). We put up with it, maybe even fondle it, out of love for what it connects us to. 
>
> Having done deep performance work for Apple, Sun, and many others in both the c layers and Java and objective c, I just say hey, it’s worth listening to our blogger here. If you would rather not, that’s ok. Just remember we tried to tell you 😉
>



### **Anonymous** — July 17, 2013 at 2:41 am ([permalink](/blog/why-mobile-web-is-slow/#comment-24247))

> Anonymous says:
>
> I dug up this old simple benchmark of Towers of Hanoi and ran it again on my Macbook Air. It shows Java running over 5 times faster than Objective-C. You can verify this easily by copying/building/running the code on your local machine. 
>
> [https://gist.github.com/ano…](<https://gist.github.com/anonymous/6017944>)
>



### **Anonymous** — July 17, 2013 at 5:20 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21943))

> Anonymous says:
>
> I will attribute your statement to inexperience 😉 Static optimization is optimal only with a closed world assumption. As soon as you load an additional class, all bets are off. For example, a JIT might know that a method which may be overridden is actually NOT overridden and just inline the call. A static compiler cannot know that. Btw: AOT (ahead of time compilation) has been a feature in IBM’s J9 VM since the dawn of time.
>



### **Anonymous** — July 17, 2013 at 5:29 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21667))

> Anonymous says:
>
> Not true: Blackberry 10 is based on QNX. It doesn’t come more real time than that.
>



### **Anonymous** — July 17, 2013 at 5:55 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21740))

> Anonymous says:
>
> Funny 🙂 
>
> Most modern (managed) languages use a GC and everyone using those seem perfectly fine with how they work (including me). So I can conclude, from asking for a half baked solution, that you indeed lack that “developer skill” to work with it properly.
>



### **Anonymous** — July 17, 2013 at 2:51 pm ([permalink](/blog/why-mobile-web-is-slow/#comment-21999))

> Anonymous says:
>
> Actually, the music app developer I worked with did port some of his apps to the BB10. Everybody who has used it claims that the BB10 is by far the best implementation; because of the responsiveness. Animoog got ported to it as well; and people who use it also say that it has great responsiveness. 
>
> Unfortunately though, making cheap apps for the BB10 doesn’t work business-wise because the user base is just too small. It might work if it were packaged/marketed as a dedicated music device though; but not as an actual Android/iOS competitor. 
>
> Mobile music apps’ killer hardware platform would have: a real-time OS (QNX?) and a pressure-sensitive screen. Every existing hardware choice already has enough throughput, and most have a fast enough touch screen.
>



### **Anonymous** — July 24, 2013 at 12:54 am ([permalink](/blog/why-mobile-web-is-slow/#comment-21770))

> Anonymous says:
>
> “Note that the title of this post is not [written in] proper English.”
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
