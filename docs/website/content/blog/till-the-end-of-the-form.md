---
title: Till The End Of The Form
slug: till-the-end-of-the-form
url: /blog/till-the-end-of-the-form/
original_url: https://www.codenameone.com/blog/till-the-end-of-the-form.html
aliases:
- /blog/till-the-end-of-the-form.html
date: '2013-09-01'
author: Shai Almog
---

![Header Image](/blog/till-the-end-of-the-form/till-the-end-of-the-form-1.jpg)

  
  
  
  
![Picture](/blog/till-the-end-of-the-form/till-the-end-of-the-form-1.jpg)  
  
  
  

We’ve had pull to refresh for quite some time which is a really nice feature useful for pulling new updates. We also always had infinite lists using a smart list model approach, however up until now we didn’t have a standard implementation of an infinite container with arbitrary components. 

  
In some of the newer web UI’s such as Tumblr and Twitter the data is fetched dynamically when you reach a fixed location in the form, this is a simpler approach than the one demonstrated by the list model but in some regards its more practical. A user can’t just start jumping around and fetching the entire list, this works better with most REST API’s and is pretty powerful on its own.  

  
My initial thought was to create a Container subclass or even add support for this into Container itself, but eventually I came to the conclusion that this is really unnecessary and we can accomplish something like this without modifying the internal code. For this purpose we created the  
  
InfiniteScrollAdapter which is a really simple class that binds to a container and gives you the ability to add components to it. The API is remarkably simple you just invoke the static method InfiniteScrollAdapter.createInfiniteScroll() with an empty container then wait for it to invoke the runnable you submit to it.  
  
  
The runnable will be invoked on the EDT so be sure not to block it (unless you use an AndWait or invokeAndBlock method), in it you can fetch data and once you are done add any set of components you like using the  
  
addMoreComponents() method. Notice that you shouldn’t just add/remove components on your own since this will mess up the container.

  
Here is a simple example that adds buttons and sleeps to simulated slow network activity:  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — September 2, 2013 at 11:15 am ([permalink](/blog/till-the-end-of-the-form/#comment-21693))

> Anonymous says:
>
> Nice !
>



### **Anonymous** — January 4, 2014 at 11:20 am ([permalink](/blog/till-the-end-of-the-form/#comment-21868))

> Anonymous says:
>
> I want to add/remove components dynamically not infinitely like here. 
>
> For example if click on ‘make’ button it will create new 5 buttons. When i again click on ‘make’ again it should remove previous 5 buttons and add new 5 buttons (like a search application where new search appears with new keywords and previous one is gone)…. 
>
> Any help appreciated ….
>



### **Anonymous** — January 4, 2014 at 5:00 pm ([permalink](/blog/till-the-end-of-the-form/#comment-21747))

> Anonymous says:
>
> I don’t understand the problem? 
>
> Just place the 5 entries in a container and replace the container when the button is pressed. That isn’t infinite scrolling.
>



### **Anonymous** — January 28, 2014 at 9:16 am ([permalink](/blog/till-the-end-of-the-form/#comment-22048))

> Anonymous says:
>
> Hello, 
>
> The above example works, but not when I’m asynchronously creating Components in the invokeAndBlock code. 
>
> I’m actually creating a List of components which I’ll pass to addMoreComponents; but it seems the invokeAndBlock method doesn’t wait until all netwerk processing is done and the list is fully available. Because when the addMoreComponents is called, the list is still empty. 
>
> I must be missing something… 
>
> Wim
>



### **Anonymous** — January 28, 2014 at 10:39 am ([permalink](/blog/till-the-end-of-the-form/#comment-21975))

> Anonymous says:
>
> Hello, 
>
> I managed to workaround the threading issue by using the list in my network call back by checking the size of it. If it contains an equal number of items then my network calls then I know that my network code is currently processing the last request. 
>
> When that is the case I can call addMoreComponents, and clear the list afterwards. 
>
> One catch was that I had a deadlock when another form (not displayed yet) did a new WebBrowser() in the constructor => in InfiniteScrollAdapter the infiniteContainer.animateLayoutAndWait(300); blocked everything. 
>
> Fixed this by initalizing the webBrowser only when I needed it. 
>
> PS: Passing an array with one element to addMoreComponents gives an ArrayOutOfBounds exception.
>



### **Anonymous** — January 28, 2014 at 5:02 pm ([permalink](/blog/till-the-end-of-the-form/#comment-21860))

> Anonymous says:
>
> Your basic premise is wrong. InvokeAndBlock runs off the EDT so changing components from there will fail and is wrong. You should use the EDT violation detection tool in the simulator to detect such bugs. 
>
> Once you do stuff off the EDT things will fail in odd and unpredictable ways, what you got working might not work on the device.
>



### **Anonymous** — January 29, 2014 at 4:25 am ([permalink](/blog/till-the-end-of-the-form/#comment-21898))

> Anonymous says:
>
> I created the Component objects in the invokeAndBlock, but didn’t add them to the adapter with addMoreComponents, I just added them to a List. Later when I’m back on the EDT, I called addMoreComponents with that List. 
>
> I’ll check the EDT violation detection tool to be sure though. 
>
> Wim
>



### **Anonymous** — August 22, 2014 at 3:11 am ([permalink](/blog/till-the-end-of-the-form/#comment-22211))

> Anonymous says:
>
> I think that I have same problem: 
>
> For example: 
>
> We have 100 elements in container. When user goes to the end of the list, I want to add 10 elements to the end of the list and remove first 10 elements and vice versa. I want to get constant number of elements in container.
>



### **Anonymous** — August 22, 2014 at 1:20 pm ([permalink](/blog/till-the-end-of-the-form/#comment-21442))

> Anonymous says:
>
> We designed this for adding only but it should be possible to remove the first 10 elements from the container just as well. What did you try?
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
