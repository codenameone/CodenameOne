---
title: Fail Fast & Margin/Padding Performance
slug: fail-fast-margin-padding-performance
url: /blog/fail-fast-margin-padding-performance/
original_url: https://www.codenameone.com/blog/fail-fast-margin-padding-performance.html
aliases:
- /blog/fail-fast-margin-padding-performance.html
date: '2016-11-22'
author: Shai Almog
---

![Header Image](/blog/fail-fast-margin-padding-performance/phone-espresso.jpg)

One of the frustrating parts in Codename One is builds failing in the cloud, the expectation is that a build that passes  
locally would pass in the cloud and that is something we strive to have at all times. One of the more common  
failures for new developers is due to refactoring of the main class or changing the signatures of the methods  
e.g. adding a throws clause to `start()`.

Starting with the next library update when you run a project it will use the main class defined in `codenameone_settings.properties`  
and not the one in the Run arguments. This means that developers who refactored a class will instantly see  
this failing in the simulator before sending the build and would realize they did something wrong.

We will also fail if `start()` or one of the other methods in the main class declares a `throws` clause. This happens  
a lot of times because new developers use IDE auto-correct suggestions and add a such a clause automatically.  
Hopefully existing/working applications won’t be impacted by this…​

### Faster Margin/Padding

Up until recently the official way to get the padding/margin of a component was something like this:
    
    
    int paddingLeft = style.getPadding(cmp.isRTL(), Component.LEFT);

That seems simple enough but there are a lot of hidden problems here. Normally this wouldn’t be a big deal but  
both padding and margin are used in performance critical paths for rendering which impacts performance directly.

__ |  Performance critical paths are places in the code that can be invoked 60 times per second e.g. in the  
painting logic, they must be **really** fast   
---|---  
  
The get padding method is implemented like this:
    
    
    public int getPadding(boolean rtl, int orientation) {
        int v = getPaddingValue(rtl, orientation);
        return convertUnit(paddingUnit, v, orientation);
    }
    public int getPaddingValue(boolean rtl, int orientation) {
        if (orientation < Component.TOP || orientation > Component.RIGHT) {
            throw new IllegalArgumentException("wrong orientation " + orientation);
        }
    
        if (rtl) {
            switch(orientation) {
                case Component.LEFT:
                    orientation = Component.RIGHT;
                    break;
                case Component.RIGHT:
                    orientation = Component.LEFT;
                    break;
            }
        }
    
        return padding[orientation];
    }

I’ll skip the `convertUnit` call since that’s pretty much fixed but as you can see there are several problems:

  1. We will always have an `if` on RTL even if we don’t need it. We don’t always need it e.g. in the case for  
top/bottom or a case where we need both left & right

  2. We need to check that the orientation is valid

  3. We have a redundant method call to the value method

  4. All of these get compounded for cases where we need 2 orientations at once

To solve these issues we replaced thru out the entire code all usage of these methods with these:
    
    
    public int getPaddingLeft(boolean rtl);
    public int getPaddingRight(boolean rtl);
    public int getPaddingTop();
    public int getPaddingBottom();
    public int getPaddingLeftNoRTL();
    public int getPaddingRightNoRTL();
    public int getHorizontalPadding();
    public int getVerticalPadding();

We did the same for margin which I’m not listing here as it is practically identical. As you can see from the implementation  
of `getPaddingLeft` it is **much** faster/smaller than **getPadding** :
    
    
    public int getPaddingLeft(boolean rtl) {
        if (rtl) {
            return convertUnit(paddingUnit, padding[Component.RIGHT], Component.RIGHT);
        }
        return convertUnit(paddingUnit, padding[Component.LEFT], Component.LEFT);
    }

The other methods provide similar optimizations that should align across the board.

The performance difference probably won’t be noticeable for most use cases. However I still think there is value  
in understanding this. If you understand this change you can look for similar problematic usages in your code  
and also within ours. When a method is deep enough within the call stack it becomes invisible to profilers and  
we no longer see it. It’s important to challenge that and inspect the low level implementations especially if they have  
been in the code for years.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Linsong Wang** — December 1, 2016 at 5:57 pm ([permalink](/blog/fail-fast-margin-padding-performance/#comment-22836))

> Linsong Wang says:
>
> One request for this [codenameone_settings.proper…](<http://codenameone_settings.properties>) file: please do not automatically update the timestamp in the comment lines at the top of file, or remove comments completely. And, please keep a defined order of these properties.  
> The constant change of this file (even there is no real content change) causes headache when team members work together with one git repo.
>



### **Shai Almog** — December 2, 2016 at 5:30 am ([permalink](/blog/fail-fast-margin-padding-performance/#comment-23007))

> Shai Almog says:
>
> That’s a great idea, it was annoying to me too but I didn’t think of a solution until you asked… The problem is that we use java.util.Properties which uses Hashtable and is effectively broken in that sense. I can probably adapt this [http://stackoverflow.com/qu…](<http://stackoverflow.com/questions/17011108/how-can-i-write-java-properties-in-a-defined-order>) to use globally and keep a consistent order.  
> We’ll still need to update the libVersion but it would make merging easier.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
