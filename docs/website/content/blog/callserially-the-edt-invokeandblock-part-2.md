---
title: CallSerially The EDT & InvokeAndBlock (Part 2)
slug: callserially-the-edt-invokeandblock-part-2
url: /blog/callserially-the-edt-invokeandblock-part-2/
original_url: https://www.codenameone.com/blog/callserially-the-edt-invokeandblock-part-2.html
aliases:
- /blog/callserially-the-edt-invokeandblock-part-2.html
date: '2014-11-09'
author: Shai Almog
---

![Header Image](/blog/callserially-the-edt-invokeandblock-part-2/callserially-the-edt-invokeandblock-part-2-1.png)

  
  
  
[  
![Picture](/blog/callserially-the-edt-invokeandblock-part-2/callserially-the-edt-invokeandblock-part-2-1.png)  
](/img/blog/old_posts/callserially-the-edt-invokeandblock-part-2-large-2.png)  
  
  

  
  
  
  
The  
[  
last time we talked about the EDT  
](http://www.codenameone.com/blog/callserially-the-edt-invokeandblock-part-1)  
we covered some of the basic ideas, such as call serially etc. We left out two major concepts that are somewhat more advanced. 

**  
Invoke And Block  
**  
  
When we write typical code in Java we like that code to be in sequence as such:  
  
doOperationA();  
  
doOperationB();  
  
doOperationC();

This works well normally but on the EDT it might be a problem, if one of the operations is slow it might slow the whole EDT (painting, event processing etc.). Normally we can just move operations into a separate thread e.g.:  
  
doOperationA();  
  
new Thread() {  
  
public void run() {  
  
doOperationB();  
  
}  
  
}).start();  
  
doOperationC();

Unfortunately, this means that operation C will happen in parallel to operation C which might be a problem… E.g. instead of using operation names lets use a more “real world” example:  
  
updateUIToLoadingStatus();  
  
readAndParseFile();  
  
updateUIWithContentOfFile();

Notice that the first and last operations must be conducted on the EDT but the middle operation might be really slow!  
  
Since updateUIWithContentOfFile needs readAndParseFile to be before it doing the new thread won’t be enough. Our automatic approach is to do something like this:  
  
updateUIToLoadingStatus();  
  
new Thread() {  
  
public void run() {  
  
readAndParseFile();  
  
updateUIWithContentOfFile();  
  
}  
  
}).start();

But updateUIWithContentOfFile should be executed on the EDT and not on a random thread. So the right way to do this would be something like this:

updateUIToLoadingStatus();  
  
new Thread() {  
  
public void run() {  
  
readAndParseFile();  
  
Display.getInstance().callSerially(new Runnable() {  
  
public void run() {  
  
updateUIWithContentOfFile();  
  
}  
  
});  
  
}  
  
}).start();

This is perfectly legal and would work reasonably well, however it gets complicated as we add more and more features that need to be chained serially after all these are just 3 methods!

Invoke and block solves this in a unique way you can get almost the exact same behavior by using this:  
  
updateUIToLoadingStatus();  
  
Display.getInstance().invokeAndBlock(new Runnable() {  
  
public void run() {  
  
readAndParseFile();  
  
}  
  
});  
  
updateUIWithContentOfFile();

Invoke and block effectively blocks the current EDT in a legal way. It spawns a separate thread that runs the run() method and when that run method completes it goes back to the EDT. All events and EDT behavior still works while invokeAndBlock is running, this is because invokeAndBlock() keeps calling the main thread loop internally.

Notice that this comes at a slight performance penalty and that nesting invokeAndBlocks (or over using them) isn’t recommended. However, they are very convenient when working with multiple threads/UI.

**  
Why Would I Invoke callSerially when I’m on the EDT already?  
**  
  
We discussed callSerially in the previous post but one of the misunderstood topics is why would we ever want to invoke this method when we are still on the EDT. The original version of LWUIT used to throw an IllegalArgumentException if callSerially was invoked on the EDT since it seemed to make no sense.  
  
However, it does make some sense and we can explain that using an example. E.g. say we have a button that has quite a bit of functionality tied to its events e.g.:  
  
1\. A user added an action listener to show a Dialog.  
  
2\. A framework the user installed added some logging to the button.  
  
3\. The button repaints a release animation as its being released.

However, this might cause a problem if the first event that we handle (the dialog) might cause an issue to the following events. E.g. a dialog will block the EDT (using invokeAndBlock), events will keep happening but since the event we are in “already happened” the button repaint and the framework logging won’t occur. This might also happen if we show a form which might trigger logic that relies on the current form still being present.

One of the solutions to this problem is to just wrap the action listeners body with a callSerially. In this case the callSerially will postpone the event to the next cycle (loop) of the EDT and let the other events in the chain complete. Notice that you shouldn’t use this normally since it includes an overhead and complicates application flow, however when you run into issues in event processing I suggest trying this to see if its the cause.  
  

  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — March 2, 2015 at 7:34 pm ([permalink](https://www.codenameone.com/blog/callserially-the-edt-invokeandblock-part-2.html#comment-22285))

> Anonymous says:
>
> Hi, 
>
> just want to inform that this page is containing typo mistake..which may lead to wrong assumption for new developers.. 
>
> Its here: 
>
> “Unfortunately, this means that operation C will happen in parallel to operation C which might be a problem…” 
>
> I think one of the C should be B 
>
> Please don’t take it wrong, just wanted to make CN1 cleanNshine. 
>
> Thanks
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcallserially-the-edt-invokeandblock-part-2.html)


### **Anonymous** — March 3, 2015 at 2:02 am ([permalink](https://www.codenameone.com/blog/callserially-the-edt-invokeandblock-part-2.html#comment-22353))

> Anonymous says:
>
> Thanks for the catch!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcallserially-the-edt-invokeandblock-part-2.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
