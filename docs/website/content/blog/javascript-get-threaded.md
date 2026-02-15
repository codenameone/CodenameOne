---
title: Javascript, Grow up and Get Threaded
slug: javascript-get-threaded
url: /blog/javascript-get-threaded/
original_url: https://www.codenameone.com/blog/javascript-get-threaded.html
aliases:
- /blog/javascript-get-threaded.html
date: '2015-08-23'
author: Steve Hannah
---

![Header Image](/blog/javascript-get-threaded/html5-banner.jpg)

The concept of threads in the Javascript community is a controversial one. The founders and leaders are dogmatically against threads, and have been from very early on. [This 2007 article by Brendan Eich](https://brendaneich.com/2007/02/threads-suck/) reveals his feeling on threads (they suck!) and the advancements in subsequent years seem to have followed this sentiment by vigorously avoiding anything thread-like.

Countless “solutions” and workarounds have been developed by clever programmers to try to improve the situation without actually adding threads. Web Workers solve some concurrency issues, but don’t come close to solving the general case, and Promises are currently a popular way to make threadless code more readable. Now with ES6, we have generators and the yield statement offering yet a new ugly workaround for this glaring language omission. These are all ugly patches that make code less readable and less maintainable, not more.

In the olden days, Javascript was only used for adding a little interactivity to web pages so keeping it “thead-less” made sense. It reduced complexity and precluded the possibility of running into complex race conditions or dead-locks related to threading. Guess what, Javascript, it’s not the olden days anymore. Serious developers are building serious apps in Javascript now, and they are bending over backwards to work around this deficiency.

Netflix talks a little bit about how they deal with asynchronous programming in [this video](https://www.youtube.com/watch?v=a8W5VVGO-jA). The first part of the video shows “the problem” – which includes race conditions, callback hell, etc… If you’ve developed anything of decent size in Javascript, you are well aware of these problems. Their solutions appear to be optimal considering Javascript’s lack of threads. But it just pains me to see everybody quite happy to jump through these hoops when the answer is staring them in the face. Just support threads!

[TeaVM](http://teavm.org) is a Java to Javascript compiler that supports threads by using static analysis to cleverly convert the application into continuations. I have been using it quite a bit over the past few months (we are using it as a platform to port Codename One into Javascript), and it has just reminded me how nice it is to program synchronously.

For example any time you are writing a function that has to call a “slow” function, like making a network request, accessing local data (e.g. IndexedDB), you have to break it up into multiple callbacks in Javascript. Rather than having a single “black box” function that takes inputs and returns outputs, you have to consider whether that function will need to access any services that require a callback.

Consider an example function that returns the current user’s age. Our first version might always have the user’s age stored in a variable so we don’t need to worry about callbacks
    
    
    function getAge() {
        return this.userAge;
    }

So you build other parts of the app to depend on this function. Then one day, your requirements change, and you **may** need to load the age from a data source.

e.g.
    
    
    function getAge() {
        if (this.userAge === -1) {
            self = this;
            requestUserAgeFromService(function(response) {
                self.userAge = response.age;
            });
        }
        return this.userAge;
    }

Well, the first time this is called, userAge won’t be initialized yet, so you need to jump through some hoops to make sure it’s loaded. For this reason, in Javascript, all APIs should really be written to use promises or callbacks JUST IN CASE you need to do something that you can’t do synchronously.

Look at the same function in Java with TeaVM:
    
    
    int getAge() {
        if (userAge === -1) {
            userAge = requestUserAgeFromService();
        }
        return userAge;
    }

We don’t know what kind of stuff needs to happen under the hood of `requestUserAgeFromService()`, and we shouldn’t need to. It’s a black box. IMO this code is much more readable, and far easier to maintain than any solution involving promises, callbacks, generators – or ANY solution that can currently be used in Javascript.

During the process of developing the Javascript port for Codename One, whenever I was adding a new feature, I would evaluate available Javascript APIs to see how best to achieve the feature. In many cases the APIs have required callbacks – because that’s how Javascript has to work. My first order of duty, then, is to write a synchronous wrapper API for the library so that I can use the code synchronously. This isn’t difficult using the built-in Java threading primitives like wait and notify. But the result is an API that is WAY easier to understand and maintain. And thus far more enjoyable to use.

This period of being able to build web apps with threading has spoiled me. I don’t think I can go back to just plain Javascript. The lack is just too present.

If you’re looking to build web apps that support threading, you can check out [TeaVM](http://www.teavm.org) and its related sub-project, [Flavour](https://github.com/konsoletyper/teavm-flavour), that provides bindings similar to Knockout and Angular. It’s not for every project, but once it reaches a certain size, being able to write code synchronously will pay dividends while providing a much more pleasant development experience. Codename One has supported threads from the beginning on every platform, and with the help of TeaVM we also support threads in the browser.

I have been developing in both Java and Javascript for years, so needing to use Java to have threads is no big deal, but for Javascript developers it may be more of a “change” than they want. It is especially for **those** developers that Javascript needs to grow up and get threaded.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **BrendanEich** — August 24, 2015 at 4:20 pm ([permalink](/blog/javascript-get-threaded/#comment-22161))

> BrendanEich says:
>
> This post is out of date in many ways, and it misses the reasons why threads suck for JS as a language used by millions of programmers (data races without any proof system like Rust’s ownership/type system to make races static errors; races leaking into pure JS objects; easily-misused primitives meant for compilers and experts, not appropriate in the most widely used programming language on the planet). Have you been tracking the SharedArrayBuffer and Atomics work by Lars T. Hansen at Mozilla? Or WebAssembly?
>
> Dogmatic equation of “threads” with “growing up” did not make the case. Compiling real C++ programs to asm.js did. SharedArrayBuffer and Atomics are not yet in standard JS, and even if they make it (which I think is likely; they’re on the agenda for the September TC39 meeting, but who knows?), they won’t be for almost all of the millions of hand-coding JS programmers. Because threads suck.
>
> /be
>



### **shannah78** — August 24, 2015 at 5:27 pm ([permalink](/blog/javascript-get-threaded/#comment-22204))

> shannah78 says:
>
> Thanks for commenting. Big fan of your work.
>
> I have to concede that threads are involved in most “newbie” mistakes in java. If I had a nickel for every time I had to point out “You’re modifying the UI off the EDT — big no-no”, I’d be … more wealthy than I am. And you might be right about threads not being needed by most Javascript developers. But…
>
> “””  
> easily-misused primitives meant for compilers and experts, not appropriate in the most widely used programming language on the planet
>
> “””
>
> This makes it sound like you have lower expectations of Javascript developers than other languages- as if JS is a training-wheel language that is only appropriate for newbies. I.e. don’t give them threads because they can’t handle threads. On the contrary, some of the best minds in the world are now working in Javascript for most of their productive hours. These guys can handle a few sharp objects.
>
> I do appreciate the caution in adding features that could confuse copy-paste coders, and things like SharedArrayBuffer/Atomics is definitely moving in the right direction. In fact due to this caution, Javascript is well positioned to add concurrency in a safe way.
>
> I’ve been doing some experimentation with [parse.com](<http://parse.com>) cloud functions – which requires me to use server-side JS – and I find myself wincing as I write the code and then realize — oh , have to make another db call here…. ok.. let’s wrap this in another promise, and change the flow of the entire function to work with promises now. Enjoyment factor just tanks as soon as I have to do that.
>
> I am watching the progress of WASM with anticipation, and I fully expect that JS will add threads – or at least the basis upon which threads could be implemented at some time in the future. I’d just like to see it sooner than later. I still contend that threads (or at least concurrency with easily shared memory) are a feature of any “grown-up” language, and JS devs need the option of removing the training wheels from time to time.
>



### **Jason Mulligan** — August 25, 2015 at 10:31 am ([permalink](/blog/javascript-get-threaded/#comment-22276))

> Jason Mulligan says:
>
> You don’t need threads in your “core” reactor, but do you need to understand how to write code for the env/language. Use promises for IO, events for user interaction and stateless functions for the rest; you’ll do fine. IPC may seem undesirable, but it forces you to really think about what you’re doing.
>



### **shannah78** — August 25, 2015 at 2:15 pm ([permalink](/blog/javascript-get-threaded/#comment-22084))

> shannah78 says:
>
> Of course you need to understand how to write code for the env/language. For JS you have three choices:  
> 1\. All public APIs need to be written with return vals going to callbacks.  
> 2\. All public APIs need to return promises.  
> 3\. You may need to change the public API every time you make an implementation change due to the need to do some optional IO.
>
> Syntactically this results in the degradation of a nice language to a horrible kludge of functions chained together. StratifiedJS and TeaVM have good solutions for this – but it still seems laughable that everyone is happy with this status quo.
>
> Of course you don’t *need* threads. But that’s an argument in minimality. You don’t *need* objects, or functions, or arrays, or etc… either. They just make the language nicer to use.
>
> Promises and other reactive patterns for working asynchronously are all very clever developments by very smart people to solve a problem that shouldn’t need to be solved.
>



### **Jason Mulligan** — August 25, 2015 at 2:47 pm ([permalink](/blog/javascript-get-threaded/#comment-21478))

> Jason Mulligan says:
>
> 1\. CSP  
> 2\. Futures  
> 3\. PEBCAK
>
> I disagree with the rest, as I can only assume you’ve trapped yourself with a way of thinking.
>



### **shannah78** — August 25, 2015 at 3:03 pm ([permalink](/blog/javascript-get-threaded/#comment-22433))

> shannah78 says:
>
> Thanks for the CSP ref. Wasn’t familiar with that one. Looks promising. PEBCAK must have been a self reference. Cheer up. It will be ok.
>



### **BrendanEich** — September 2, 2015 at 3:34 pm ([permalink](/blog/javascript-get-threaded/#comment-22366))

> BrendanEich says:
>
> I have appropriate expectations for most programmers of as widely used and heretofore single-threaded a language as JS is. The vast majority of JS programmers should not touch SharedArrayBuffer or Atomics, period, full stop — even if a few are expert enough to do so.
>
> We can haggle about how tiny vs. minuscule the fraction representing those few is, but it’s very small.
>
> /be
>



### **Alexey Andreev** — September 4, 2015 at 4:14 am ([permalink](/blog/javascript-get-threaded/#comment-24192))

> Alexey Andreev says:
>
> > easily-misused primitives meant for compilers and experts, not appropriate in the most widely used programming language on the planet
>
> There are still developers that can and want to deal with threads. 99% of developers use frameworks, and remaining 1% write them. The problem that the latter ones need more “powerful” features of a language, and they usually (I belive) realize what are they doing. Example is: when I write business code for an enterprise application in Java using Spring, Hibernate, Servlet container, I don’t bother with threads at all. On the other hand I don’t bother with “asyncronous” nature of underlying infrastructure, because there is not asynchronousity at all. Consider the following code:
>
> for (Role role : employee.getRoles()) {
>
> System.out.println(role.getName());
>
> }
>
> Dependending on configuration, this code may cause additional SQL query. I mean getRoles method that may be marked as lazy association, so instead of returning simple ArrayList, it returns Hibernate implementation of List, which executes an SQL query as soon as data get fetches from the list the first time. So Hibernate end-user simply uses it without knowing how how Hibernate really works and when it makes SQL queries.
>
> I can’t do the same trick with callbacks or promises, since I first have to declare all of my associations, that might cause SQL queries during their resolution, as returning promises of lists (or sets) instead of simple list.
>
> Callbacks can be easily implemented with threads. Actor model can be easily implemented with threads. STM can be (not quite easily) implemented with threads. But according to my experience with implementing threads via callbacks in TeaVM, this is pain in the ass, requiring you to use advanced compiler hacker knowledge.
>
> > they won’t be for almost all of the millions of hand-coding JS programmers
>
> what about millions of Java programmers?
>



### **Asmithdev** — September 14, 2015 at 7:30 pm ([permalink](/blog/javascript-get-threaded/#comment-22376))

> Asmithdev says:
>
> I guess you’ve never heard of [http://www.hamsters.io/](<http://www.hamsters.io/>)
>



### **Asmithdev** — September 14, 2015 at 7:38 pm ([permalink](/blog/javascript-get-threaded/#comment-22306))

> Asmithdev says:
>
> Threads don’t suck with the use of the current web worker API and transferrable objects…although I do agree the API itself sucks on many levels the memory model makes threading extremely safe, I’m not entirely sure why people are unwilling to open their minds up to multithreading in JavaScript as the problem isn’t the language but rather lack of creativity and dedication to making them usable in a practical way. I wrote WebHamsters ([http://hamsters.io](<http://hamsters.io>)) specifically for this very reason, the tools are freely available for use by anyone who cares to invest the time to learn them. I don’t know why you would throw the potential of 100’s of % performance improvements with automatic parallelization because something “sucks” on an idealogical level.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
