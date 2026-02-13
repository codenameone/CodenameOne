---
title: 'TIP: Avoid 2D Arrays'
slug: tip-avoid-2d-arrays
url: /blog/tip-avoid-2d-arrays/
original_url: https://www.codenameone.com/blog/tip-avoid-2d-arrays.html
aliases:
- /blog/tip-avoid-2d-arrays.html
date: '2016-12-04'
author: Shai Almog
---

![Header Image](/blog/tip-avoid-2d-arrays/just-the-tip.jpg)

In the first betas of Codename One we had a lot of bugs related to 2D arrays due to XMLVM.  
We no longer use XMLVM but the recommendation to avoid 2D arrays remains. We still use them in some occasions  
e.g. in the creation of a `DefaultTableModel` but the implementation discards them in favor of an `ArrayList` internally.

__ |  To be fair, a 2D array will be faster than an `ArrayList` so in this case we discarded them due to their lack of  
flexibility   
---|---  
  
### What’s the Problem?

To understand the problem of 2D arrays we need to understand how they work. When we write something like this:
    
    
    int[][] myArray = new int[] {
        {1, 2, 3},
        {4, 5, 6},
        {7, 8, 9}
    };

What we are “effectively” doing is this:
    
    
    int[]  a1 =  {1, 2, 3};
    int[]  a2 =  {4, 5, 6};
    int[]  a3 =  {7, 8, 9};
    Object[] myArray = {a1, a2, a3};

That sounds like a “small” semantic difference but it’s a huge one!

That means we have 4 arrays where we should have had just 1. That’s far more objects to GC & allocate. That  
also means that every array lookup has to happen twice. The problem here is that these semantics are often  
used when processing large data such as screen images that might be pretty big at which case you need to  
multiply that overhead by 1024.

There is one exception where the length of the individual entries isn’t uniform, however that’s a pretty rare use  
case. Most such cases use collection classes and not multi-dimensional arrays.

### Faster Way

We do a lot of image RGB manipulations and we always use a single dimension array. This is **far** more performant  
and pretty easy to use. When we want to access an individual pixel we can do:
    
    
    pixels[x + y * width] = newValue;

It might seem that the overhead of multiplication and addition would cancel out the benefit of the array lookup.  
That’s not true as those operations are remarkably fast by comparison especially with AoT compilation. JIT’s might  
be able to do some magic with 2D arrays but even if they can do that they would still need more GC and allocation  
work as both of those are unavoidable. So a 1D array would always be superior in Java.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
