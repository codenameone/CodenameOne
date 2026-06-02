---
title: 'TIP: Use Weak References'
slug: tip-weak-references
url: /blog/tip-weak-references/
original_url: https://www.codenameone.com/blog/tip-weak-references.html
aliases:
- /blog/tip-weak-references.html
date: '2017-10-16'
author: Shai Almog
---

![Header Image](/blog/tip-weak-references/tip.jpg)

One of the less familiar features of Java is the mess of weak/soft/phantom references. This is a confusing mess and it’s compounded by the fact that other languages (such as the reference counting Swift/Objective-C) have used these terms with a different meaning. To simplify this weak references in a garbage collected language allows you to keep a pointer (reference) to an object that won’t force it to stay in RAM.

This can be made clearer by this sample:
    
    
    class MyObject {
        private Object otherObjectInCache;
    
        // ... rest of code
    }

Let’s imagine that `otherObjectInCache` is an object that’s expensive to load e.g. an image. As long as `MyObject` is in RAM the `otherObjectInCache` won’t be removed from RAM and this might take up a lot of memory that could be used elsewhere…​

The thing is we don’t know. We might have a lot of RAM but we might not, this varies based on the device and checking OS memory is problematic in multi-tasking OS’s and GC languages. What we really want is:

  * If we have extra RAM we want to keep the object in memory

  * If we run out of RAM we are happy to just reload the object as needed

A weak reference allows you to keep a reference to an object that can still be removed by the GC if it needs the RAM. Standard Java code for weak references looks like this:
    
    
    import java.lang.ref.WeakReference;
    
    class MyObject {
        private WeakReference otherObjectInCache;
    
        public void codeThatNeedsObject() {
            if(otherObjectInCache != null) {
                 Object cachedData = otherObjectInCache.get();
                 if(cachedData != null) {
                    // use cachedData
                    return;
                 }
            }
    
            // object was gc’d
           Object cachedData = loadObject();
    
            // store the object in the weak reference
            otherObjectInCache = new WeakReference(cachedData);
        }
        // ... rest of code
    }

Notice that once the `cachedData` variable is not null we have a hard reference in RAM so that object won’t be GC’d until hard references are gone.

This works rather well in Codename One and elsewhere but there is an even better option: `SoftReference`. Weak references are GC’d relatively aggressively but this isn’t as true for `SoftReference`. Unfortunately not all target OS’s of Codename One support `SoftReference` so we can’t support the official JDK version of that. That’s why we have a slightly different API that you should probably use:
    
    
    static import com.codename1.ui.CN.*;
    
    class MyObject {
        private Object otherObjectInCache;
    
        public void codeThatNeedsObject() {
            if(otherObjectInCache != null) {
                 Object cachedData = extractHardRef(otherObjectInCache);
                 if(cachedData != null) {
                    // use cachedData
                    return;
                 }
            }
    
            // object was gc’d
           Object cachedData = loadObject();
    
            // store the object in the weak reference
            otherObjectInCache = createSoftWeakRef(cachedData);
        }
        // ... rest of code
    }

__ |  This relies on a change to the `CN` class that will be a part of next week’s update, right now you can use the `Display` class which provides this functionality   
---|---
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Martin Grajcar** — November 4, 2018 at 4:38 pm ([permalink](/blog/tip-weak-references/#comment-24098))

> Martin Grajcar says:
>
> After reading this, I was rather confused (Why no generics? Where are the methods? Why Object?). After downloading the sources and using `grep -r`, I found `Display#createSoftWeakRef`, so it has got better. Then I’ve found `WeakHashMap`, which is probably what we should use most of the time, right?
>



### **Shai Almog** — November 5, 2018 at 5:25 am ([permalink](/blog/tip-weak-references/#comment-23910))

> Shai Almog says:
>
> The static API in CN mentioned a bit lower in the post is a newer version of the API in `Display`. The original API predated generification of the code base so it stayed that way. Might be worth updating for generics at some point.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
