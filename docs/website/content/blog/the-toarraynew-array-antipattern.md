---
title: The toArray(new Array) Antipattern
slug: the-toarraynew-array-antipattern
url: /blog/the-toarraynew-array-antipattern/
original_url: https://www.codenameone.com/blog/the-toarraynew-array-antipattern.html
aliases:
- /blog/the-toarraynew-array-antipattern.html
date: '2015-01-11'
author: Shai Almog
---

**NOTE** : The information in this blog post is out of date. Codename One now fully supports the **Collection.toArray(T[] arr)** method, including the case where **arr** is an array of size **0**.

![Header Image](/blog/the-toarraynew-array-antipattern/the-toarraynew-array-antipattern-1.jpg)

  
  
  
  
![Picture](/blog/the-toarraynew-array-antipattern/the-toarraynew-array-antipattern-1.jpg)  
  
  
  

  
  
  
A recent issue in the issue tracker on the new iOS VM reminded me of a serious pet peeve and big design mistake in the Java Collections API, something that is just unfixable and wrong yet appears often in code from developers trying to be clever.

If you have a collection and you want to convert it to an array you can do something like:  
  
Object[] myArray = c.toArray();

Unfortunately, this will always be an array of objects and not of your desired type… So if the collection is one of String you would really want to do something like:  
  
String[] array = new String[c.size()];  
  
c.toArray(array);

This works great but takes two lines… Which is why you can also do something like this:  
  
String[] array = (String[])c.toArray(new String[c.size()]);

So far so good… The problem is that this also works:  
  
String[] array = (String[])c.toArray(new String[0]);

It will produce an array response that is equal to the size of c and is of type String[] and not Object[]. This will fail on the new iOS VM and should really never be used…

Java usually takes the approach of “fail fast”, which means that if code fails it should do so ASAP and not “try to recover” which might cause bugs to remain hidden. This isn’t such a case.

If the array past to the toArray method is too small, the code has a fallback. The problem is that the fallback is REALLY bad!  
  
It uses reflection to detect the array type and allocate a whole new array. This is really slow and that essentially means the original allocated array is just completely redundant garbage. And all this just saves a single boilerplate method call:  
  
String[] array = (String[])c.toArray(new String[  
**  
c.size()  
**  
]);

Don’t do that, on Java SE/EE either. It has reflection and a better optimizer but even the best optimizer can’t eliminate something this bad…  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Sebastian Sickelmann** — January 20, 2016 at 4:32 am ([permalink](/blog/the-toarraynew-array-antipattern/#comment-22417))

> Sebastian Sickelmann says:
>
> You should also read [http://shipilev.net/blog/20…](<http://shipilev.net/blog/2016/arrays-wisdom-ancients/>) and maybe relativize some statements about Java SE and it’s reflection and optimizing possibilities.


### **Shai Almog** — January 20, 2016 at 4:37 am ([permalink](/blog/the-toarraynew-array-antipattern/#comment-22665))

> Shai Almog says:
>
> You are aware I worked for Sun and did quite a bit of JIT development 😉
>
> I am fully aware of what a high end JIT can do to code that’s repeated often. We are talking AOT mobile device compilation where those aren’t an option… Generally I don’t see a reason to “force” a JIT to do work, if I can write the original code correctly for the first pass of the JIT rather than just being lazy and passing 0 why not do that?
>
> Notice that newer versions of the JVM are talking about new language features such as the ability to use generics efficiently with primitives. This would also pose a problem with relying on behaviors like this…


### **Sebastian Sickelmann** — January 20, 2016 at 3:29 pm ([permalink](/blog/the-toarraynew-array-antipattern/#comment-22499))

> Sebastian Sickelmann says:
>
> Well just wanted to mention the fairly new article regarding the same programming pattern. And if I get Aleksey right it is actually the other way around. By passing an non empty array you make it harder for the JIT at least for the HotspotVM to deliver best performance. It is totally clear to me that in other VM Implementation or in AOT Optimization scenarios for mobiles it may be the other way around.


### **Shai Almog** — January 21, 2016 at 3:43 am ([permalink](/blog/the-toarraynew-array-antipattern/#comment-22592))

> Shai Almog says:
>
> Its interesting to read but I disagree with the conclusion as it hinges on micro-benchmarks which often give a wrong impression especially with a JIT as “insane” as hotspot.
>
> One thing that does make a lot of sense is that toArray without arguments is the fastest and that’s probably the best tip you can get here as it makes sense performance wise.
>
> Since hotspot takes a while to “warmup” and would do it for every call you make to toArray() I’m not so sure if this would still perform as nicely in real world scenarios. Every JIT engineer I know would recommend that you write code that is “correct” rather than optimize to a JIT. To me allocating a 0 size array as a “hint” is incorrect code and it does perform slower on anything other than a fully optimized hotspot path.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
