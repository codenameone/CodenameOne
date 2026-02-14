---
title: GC Crashes & Bugs
slug: gc-crashes-bugs
url: /blog/gc-crashes-bugs/
original_url: https://www.codenameone.com/blog/gc-crashes-bugs.html
aliases:
- /blog/gc-crashes-bugs.html
date: '2015-03-30'
author: Shai Almog
---

![Header Image](/blog/gc-crashes-bugs/hqdefault.jpg)

As part of tracking a bug in iOS media playback Steve hit upon some code that recreated the OpenGL framebuffer  
pretty much all the time. This was there to allow device rotation to work, but was implemented incorrectly…   
After this fix animations and UI is much smoother on iOS, if you notice any potential issues let us know. 

In other news we just fixed two big GC issues in the new VM. The first was a rather complex edge case with GC firing  
up during native code where more than one allocation was made during the second (or later) allocations  
(yes edge case). Since objects allocated in native code still don’t have hard Java references the GC can’t “see”  
those references and thus wiped them. The solution was pretty simple, we now toggle a flag that essentially  
blocks GC from happening while we are in such a native method… 

The other issue was arguably harder to catch since it was more conceptual, but the fix consisted of exactly two  
lines of code… As you recall we use a mark-sweep algorithm to clean up the objects, new objects were implicitly  
created as marked which seemed to make a lot of sense (we create an object we will probably need it!).  
However, this created a rather tricky situation… If a user creates a new array e.g.: 
    
    
    List objectsInListThatArentYetMarked = ...;
    Object[] newArrayToContainObjects = new Object[objectsInListThatArentYetMarked.size()];
    objectsInListThatArentYetMarked.toArray(newArrayToContainObjects);
    objectsInListThatArentYetMarked = null;
    

In this case objectsInListThatArentYetMarked will get wiped by the GC since its no longer necessary and won’t  
be marked. Since the array is newly created it will be marked, so when our mark algorithm reaches it then it will  
see a marked object and won’t traverse further (otherwise we will get into infinite recursions and performance penalties).  
The solution is actually quite simple, we give newly created objects a special case -1 value and so they will  
be marked as usual, but won’t be GC’d. 

Steve also made some fixes for premature object deletion, e.g. if we invoke a native method in C which uses  
iOS’s dispatch_async calls (their equivalent of the callSerially method). By the time the async block is reached  
the String argument might have been GC’d. However, there is a special case for releasing the NSString asynchronously  
just to avoid that. Steve made sure we don’t try to access Java objects that might be GC’d anywhere in our code. 

### Update On JavaScript VM

Steve made some pretty remarkable progress with the JavaScript VM and posted the video below showing  
off the unmodified poker demo running in a browser!  
For those of you who haven’t followed this, the JavaScript VM port essentially works just like every other Codename One  
port where our build servers translate the java bytecode into JavaScript with the right porting layer.  
Unlike GWT this port works with threading code, unlike other solutions (such as echo2) this is a purely client  
side implementation. This would not have been possible just a couple of years ago but since JavaScript  
VM’s have come such a long way its actually proving to be a pretty interesting port.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **shannah78** — April 6, 2015 at 6:07 pm ([permalink](https://www.codenameone.com/blog/gc-crashes-bugs.html#comment-22043))

> shannah78 says:
>
> I would like to add that the fundamental advancement that has allowed us to run multithreaded code has nothing to do with the “Javascript VM”. Rather it is that we are using TeaVM to convert to javascript, and it includes a support for threads. This port would not have been possible without TeaVM or without Alexey Andreev’s help (TeaVM’s author). For more about TeaVM, check out [https://github.com/konsolet…](<https://github.com/konsoletyper/teavm>)
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
