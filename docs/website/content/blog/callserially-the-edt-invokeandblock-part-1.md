---
title: CallSerially The EDT & InvokeAndBlock (Part 1)
slug: callserially-the-edt-invokeandblock-part-1
url: /blog/callserially-the-edt-invokeandblock-part-1/
original_url: https://www.codenameone.com/blog/callserially-the-edt-invokeandblock-part-1.html
aliases:
- /blog/callserially-the-edt-invokeandblock-part-1.html
date: '2014-10-18'
author: Shai Almog
---

![Header Image](/blog/callserially-the-edt-invokeandblock-part-1/callserially-the-edt-invokeandblock-part-1-1.png)

  
  
  
[  
![Picture](/blog/callserially-the-edt-invokeandblock-part-1/callserially-the-edt-invokeandblock-part-1-1.png)  
](/img/blog/old_posts/callserially-the-edt-invokeandblock-part-1-large-2.png)  
  
  

  
  
  
We last explained some of the concepts behind the EDT in 2008 so its high time we wrote about it again, there is a section about it in the developer guide as well as in the courses on Udemy but since this is the most important thing to understand in Codename One it bares repeating. 

One of the nice things about the EDT is that many of the concepts within it are similar to the concepts in pretty much every other GUI environment (Swing/FX, Android, iOS etc.). So if you can understand this explanation this might help you when working in other platforms too.

Codename One can have as many threads as you want, however there is one thread created internally in Codename One named “EDT” for Event Dispatch Thread. This name doesn’t do the thread justice since it handles everything including painting etc. 

You can imagine the EDT as a loop such as this:  
  
while(codenameOneRunning) {  
  
performEventCallbacks();  
  
performCallSeriallyCalls();  
  
drawGraphicsAndAnimations();  
  
sleepUntilNextEDTCycle();  
  
}

The general rule of the thumb in Codename One is: Every time Codename One invokes a method its probably on the EDT (unless explicitly stated otherwise), every time you invoke something in Codename One it should be on the EDT (unless explicitly stated otherwise).

There are a few notable special cases:  
  
1\. NetworkManager/ConnectionRequest – use the network thread internally and not the EDT. However they can/should be invoked from the EDT.  
  
2\. BrowserNavigationCallback – due to its unique function it MUST be invoked on the native browser thread.  
  
3\. Displays invokeAndBlock/startThread – create completely new threads.

Other than those pretty much everything is on the EDT. If you are unsure you can use the Display.isEDT method to check whether you are on the EDT or not.

**  
EDT Violations  
**  
  
You can violate the EDT in two major ways:  
  
1\. Call a method in Codename One from a thread that isn’t the EDT thread (e.g. the network thread or a thread created by you).  
  
2\. Do a CPU intensive task (such as reading a large file) on the EDT – this will effectively block all event processing, painting etc. making the application feel slow.

Luckily we have a tool in the simulator: the EDT violation detection tool. This effectively prints a stack trace to suspect violations of the EDT. Its not fool proof and might land your with false positives but it should help you with some of these issues which are hard to detect.

So how do you prevent an EDT violation?  
  
To prevent abuse of the EDT thread (slow operations on the EDT) just spawn a new thread using either new Thread(), Display.startThread or invokeAndBlock (more on that later).  
  
Then when you need to broadcast your updates back to the EDT you can use callSerially or callSeriallyAndWait.

**  
CallSerially  
**  
  
callSerially invokes the run() method of the runnable argument it receives on the Event Dispatch Thread. This is very useful if you are on a separate thread but is also occasionally useful when we are on the EDT and want to postpone actions to the next cycle of the EDT (more on that next time).  
  
callSeriallyAndWait is identical to call serially but it waits for the callSerially to complete before returning. For obvious reasons it can’t be invoked on the EDT.

In the second part of this mini tutorial I will discuss invokeAndBlock and why we might want to use callSerially when we already are on the EDT.

**  
Update:  
**  
You can read part 2 of this post  
[  
here  
](http://www.codenameone.com/blog/callserially-the-edt-invokeandblock-part-2)  
.  
  

  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
