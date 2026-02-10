---
title: Deeper In The Renderer
slug: deeper-in-the-renderer
url: /blog/deeper-in-the-renderer/
original_url: https://www.codenameone.com/blog/deeper-in-the-renderer.html
aliases:
- /blog/deeper-in-the-renderer.html
date: '2013-12-17'
author: Shai Almog
---

![Header Image](/blog/deeper-in-the-renderer/deeper-in-the-renderer-1.jpg)

  
  
  
  
![List](/blog/deeper-in-the-renderer/deeper-in-the-renderer-1.jpg)  
  
  
  

When Chen initially drew up the proof of concept for LWUIT he copied the concept of list renderers from Swing, this was one of the most hotly debated issues between us. Whether we should aim for familiarity for Swing users or simplify what has always been an API that novices wrestle with. We eventually went with the familiarity approach, a decision we both agree today was short sighted. 

  
Unfortunately changing the renderer API isn’t practical at this stage but we simplified a lot thanks to the GenericListCellRenderer which powers GUI builder list renderers. We simplified this further with the MultiList component which has a default renderer that’s pretty powerful to begin with.  

  
However, this simplicity has resulted in developers using lists without  
  
quite understanding why they behave in this way. In a normal container all components are kept in a Tree like hierarchy that we can render (essentially similar to DOM). A list however doesn’t contain any components within it so it can hold a million entries with the same overhead it would have when holding 100 entries.

  
It works like this:  
  

  1.   
The EDT (event dispatch thread) asks the List to paint itself.  

  2.   
The List asks the ListModel for the values of the currently visible entries.  

  3.   
The List asks the renderer for a component representing every value (the same instance is recycled and discarded).  

  4.   
This List paints the renderer as a rubber stamp for every single value.  
  

  
  
So if the model has 1m entries we don’t need to create 1m components since we will reuse the renderer for every entry. We also don’t render elements that are invisible.

There are problems with this model though:  

  *   
Since the renderer is “stamped” (drawn then used for a different value) adding a listener to a component within the list won’t do what you expect.  

  *   
Obviously individual elements within the list entry can’t get focus and can’t be edited  
  

  *   
Within a List all entries have to be the exact same width/height. Otherwise its very expensive for us to calculate the list offset. ContainerList allows for variable height list but its performance/functionality is equivalent to Container so the benefits aren’t great.  

  *   
The performance of the model & the renderer must be very high otherwise the whole list will be impacted.  

  *   
Its hard to create animations (since there is no state).  
  

You might be asking yourself:  
  
Why should I use a List? Why not just use a List or a Container with BoxLayout Y?  

  
  
That’s actually our recommendation for most cases, List is often too much of a hassle for some use cases and we try to avoid it in many applications. It is now also possible to create  
[  
long scrolling containers  
](http://www.codenameone.com/3/post/2013/09/till-the-end-of-the-form.html)  
relatively easily although you might see performance degrading after a while.  
  
  
  
However List does have some serious advantages:  

  * Scale – it scales well to huge lists and maintains speed. 
  * Flexibility – list can be flicked to horizontal mode, center selection behavior and other such capabilities. 
  * MVC – the list makes working with a data model really easy, if you are well versed with MVC this could be a very powerful  

So assuming you pick list lets see how you can make better use of it.

  
H  
  
ow do you deal with events on a button click within a list renderer?  
  
  
So assuming I have a list renderer that has a button named X on the the side, dealing with this differs between a GUI builder app and a handcoded app.  
  
  
  
For a GUI builder app use the standard action listener for the list and within the action listener just write:  
  

* * *

Notice that the method above will not work for MultiList or a list containing MultiButton’s since those are composite components and can’t be separated to an individual component within.  
  
  
  
  
  
  
If you are building a handcoded renderer this is a bit harder:  

  
  
  
There are a few other features in the generic list cell renderer that aren’t well documented:  

  * The UIID of the renderer is based on the UIID of the renderer component we also add a focus component to the list whose UIID is based on: selected.getUIID() + “Focus”. So if your selected renderer container has the UIID MyRenderer, then you focus component will have the UIID MyRendererFocus.  
  
  

  *   
Items are always overriden by their hashtable/map value even when they are missing. So if you place an icon in the renderer but don’t put the icon value in the hashtable model it will be removed.  
  
A solution is to just name the component with the word fixed in the end (case insensitive) as in iconFixed. This means that the value you give the icon in the renderer won’t change.  

  *   
You can disable/enable entries within the list by using map.put(  
  
GenericListCellRenderer.ENABLED, Boolean.FALSE). Notice that once you do this you must do this for all the entries otherwise the renderers “rubber stamp” behavior will repeat the status of the last entry. 
  * You can implement a “Select All” entry for a checkbox list by using map.put(GenericListCellRenderer.SELECT_ALL_FLAG, Boolean.TRUE). This is pretty useful if you have a checkbox list. 
  * You can place the entry number within the list by creating a component named $number. It will start with 1 as the offset and not with 0. 
  * If you want to provide a different value when an entry is selected (e.g. different image when clicked) you can use # in front of the name e.g. to allow Icon to have a different image when selected put a value into #Icon. 

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — December 18, 2013 at 6:59 pm ([permalink](https://www.codenameone.com/blog/deeper-in-the-renderer.html#comment-24235))

> Anonymous says:
>
> Any chance of some sample code snippets for the “few other features in the generic list cell renderer that aren’t well documented”
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdeeper-in-the-renderer.html)


### **Anonymous** — December 19, 2013 at 3:19 am ([permalink](https://www.codenameone.com/blog/deeper-in-the-renderer.html#comment-21904))

> Anonymous says:
>
> They are all listed below with the relevant code where applicable. Which one of the bullets isn’t clear?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdeeper-in-the-renderer.html)


### **Anonymous** — December 19, 2013 at 3:22 am ([permalink](https://www.codenameone.com/blog/deeper-in-the-renderer.html#comment-21845))

> Anonymous says:
>
> the last two.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdeeper-in-the-renderer.html)


### **Anonymous** — December 19, 2013 at 3:27 am ([permalink](https://www.codenameone.com/blog/deeper-in-the-renderer.html#comment-21766))

> Anonymous says:
>
> There is no code involved for the first. In the GUI builder just place a component and name it $number. In the UI this will be rendered as 1, 2, 3 etc. based on the offset. 
>
> The second case allows you to provide different values for selected/unselected states. So if you have a model with values on the keys of the hashtable that you want to appear differently when pressed you can use that. A common use case is a different icon when the entry is selected (to match the colors) so normally I would place the icon as hash.put(“icon”, myUnselectedIcon); which will work both for selected/unselected states of the renderer. If I want a different icon design for the selected state I can use: hash.put(“#icon”, mySelectedIcon) as well. This will appear when the entry is selected.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdeeper-in-the-renderer.html)


### **Anonymous** — May 10, 2014 at 5:43 am ([permalink](https://www.codenameone.com/blog/deeper-in-the-renderer.html#comment-22013))

> Anonymous says:
>
> Hi, 
>
> I have noticed that the “setFocus(Boolean focus)” is deprecated. I am currently using this to show the selected cell in a list (custom cell renderer based upon the default renderer). 
>
> Unfortunately I can not find the alternative method when the setFocus should disappear. 
>
> Can u provide some insight into this??
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdeeper-in-the-renderer.html)


### **Anonymous** — May 10, 2014 at 12:00 pm ([permalink](https://www.codenameone.com/blog/deeper-in-the-renderer.html#comment-21660))

> Anonymous says:
>
> Good question! 
>
> We deprecated this method since people kept using it instead of requestFocus() for standard components. However, the renderer is indeed a special case where it is needed. 
>
> Unfortunately there is no way in Java to indicate “don’t use this method here but only use it there” so we use the relatively coarse tool of deprecation. In hindsight we should have done renderers completely differently if at all but that is already water under the bridge.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdeeper-in-the-renderer.html)


### **Anonymous** — August 14, 2014 at 2:21 am ([permalink](https://www.codenameone.com/blog/deeper-in-the-renderer.html#comment-22144))

> Anonymous says:
>
> how to add different online images on each items of list ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdeeper-in-the-renderer.html)


### **Anonymous** — August 14, 2014 at 3:46 am ([permalink](https://www.codenameone.com/blog/deeper-in-the-renderer.html#comment-22005))

> Anonymous says:
>
> Set a different URL for the URLImage attribute in the model.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdeeper-in-the-renderer.html)


### **Anonymous** — August 14, 2014 at 5:41 am ([permalink](https://www.codenameone.com/blog/deeper-in-the-renderer.html#comment-21444))

> Anonymous says:
>
> I have used container which contains labels with name address and image. And want to use this container in list row and set image to image label and set address to address label. 
>
> I am able to pass address to its label with hash_table in model as shown below. 
>
> hash_table.put(“address”,”Nepal”); 
>
> hash_table.put(“image”,”res_Image”); 
>
> But problem is unable to pass online image 
>
> can you give me simple codes for using online image in list ? 
>
> And how to use URL Image in model (by code) ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdeeper-in-the-renderer.html)


### **Kaneda** — December 11, 2015 at 1:18 pm ([permalink](https://www.codenameone.com/blog/deeper-in-the-renderer.html#comment-21623))

> Kaneda says:
>
> I want to create a list of event like attach picture, i read this page i know i must not use List to build that, but do you have an example, a tutorial to do ?
>
> Thanks you
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdeeper-in-the-renderer.html)


### **Shai Almog** — December 12, 2015 at 5:30 am ([permalink](https://www.codenameone.com/blog/deeper-in-the-renderer.html#comment-22221))

> Shai Almog says:
>
> I would just create a box layout container similar to the PropertyCross demo with containers within it.  
> Every container would have the image label on top in the center with some padding defined to take up the right size and background image behavior defined as “SCALE_TO_FIT” which will allow the image on top to look like that.  
> I noticed that the Oct 23rd container has a carousel, if you need that to animate/move manually that is also easily doable thru replace animation.
>
> The bottom container can be a standard BoxLayout.X_AXIS with two BoxLayout.Y_AXIS within it. The first box Y would container the date as month string and number and the second would contain the title/subtitle (possibly as span label).
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdeeper-in-the-renderer.html)


### **Kaneda** — December 14, 2015 at 11:10 am ([permalink](https://www.codenameone.com/blog/deeper-in-the-renderer.html#comment-22300))

> Kaneda says:
>
> I try it 🙂
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdeeper-in-the-renderer.html)


### **Kaneda** — December 14, 2015 at 4:07 pm ([permalink](https://www.codenameone.com/blog/deeper-in-the-renderer.html#comment-21499))

> Kaneda says:
>
> It works well 🙂 , and i use :
>
> Label myLabel = new Label(“My Title”);  
> ImageDownloadService.createImageToStorage(thumb_url, myLabel, guid, new Dimension(Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight()));
>
> But, the text of the label doesn’t display, how can I display the text hover the image. I think the image is not a background image.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdeeper-in-the-renderer.html)


### **Shai Almog** — December 15, 2015 at 5:15 am ([permalink](https://www.codenameone.com/blog/deeper-in-the-renderer.html#comment-21490))

> Shai Almog says:
>
> I think the text is implicitly set to blank on download.
>
> Try placing a label with a text in a Container next to a label with the downloadable image.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdeeper-in-the-renderer.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
