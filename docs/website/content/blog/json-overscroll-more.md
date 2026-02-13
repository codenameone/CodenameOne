---
title: JSON, Overscroll & More
slug: json-overscroll-more
url: /blog/json-overscroll-more/
original_url: https://www.codenameone.com/blog/json-overscroll-more.html
aliases:
- /blog/json-overscroll-more.html
date: '2014-02-23'
author: Shai Almog
---

![Header Image](/blog/json-overscroll-more/json-overscroll-more-1.png)

  
  
  
  
![Overscroll](/blog/json-overscroll-more/json-overscroll-more-1.png)  
  
  
  

Its been pretty busy around here the last couple of weeks. However, we still introduced a couple of new API’s and abilities besides the many bug fixes that constantly go in.  
  
  
We deprecated JSONParser.parse(Reader) in favor of Map<String, Object> parseJSON(Reader i). This is effectively the exact same class with one minor difference, it returns HashMaps/ArrayLists rather than Hashtables/Vectors to represent the hierarchy.  
  
Besides the modern aspect of the new collections they are also slightly faster due to the lack of synchronization calls.  
  
  
  
  
We finally implemented RFE  
[  
738  
](https://code.google.com/p/codenameone/issues/detail?id=738)  
which asks for improved overscroll behavior, we can’t get it to be 100% accurate but we got it as close as possible with the current rendering architecture.  
  
  
  
  
With MultiList/GenenricListCellRenderer one of the common issues is making a UI where a specific component within the list renderer has a different UIID style. E.g. this can be helpful to mark a label within the list as red, for instance in cases of a list of monetary transactions. Up until now the only answer we had was: you need to create your own renderer. With the latest version of GenericListCellRenderer (MultiList uses GenericListCellRenderer internally) we have another option.  
  
  
  
  
Normally to build the model for a renderer of this type we use something like:  
  
  
map.put(“componentName”, “Component Value”);  
  
  
  
  
What if we want  
  
  
  
  
  
  
  
componentName to be red? Just use:  
  
  
  
map.put(“componentName_uiid”, “red”); 

  
This will apply the uiid “red” to the component which you can then style in the them. Notice that once you start doing this you need to define this entry for all entries e.g.:  
  
  
map.put(“componentName_uiid”, “blue”);  
  
  
  
  
Otherwise the component will stay red for the next entry (since renderer acts like a  
[  
rubber stamp  
](http://www.codenameone.com/3/post/2013/12/deeper-in-the-renderer.html)  
).  
  
  
  
  
Last but not least we felt the need to simplify the very common task of download. Up until now downloading a file required a bit of code, where you needed to open a connection request and do quite a few other things. There is really no justification for that.  
  
  
So we added to Util the following methods: downloadUrlToFileSystemInBackground, downloadUrlToStorageInBackground, downloadUrlToFile & downloadUrlToStorage.  
  
  
These all delegate to a new feature we added to ConnectionRequest called: ConnectionRequest.setDestinationStorage(fileName)/  
  
  
  
  
  
  
  
ConnectionRequest.setDestinationFile(fileName);  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
Which simplifies the whole process of downloading a file.  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — February 26, 2014 at 4:31 pm ([permalink](https://www.codenameone.com/blog/json-overscroll-more.html#comment-21814))

> Anonymous says:
>
> These changes are in the netbeans plugin?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fjson-overscroll-more.html)


### **Anonymous** — February 27, 2014 at 3:51 am ([permalink](https://www.codenameone.com/blog/json-overscroll-more.html#comment-21779))

> Anonymous says:
>
> Not yet, soon.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fjson-overscroll-more.html)


### **Anonymous** — March 24, 2014 at 12:27 pm ([permalink](https://www.codenameone.com/blog/json-overscroll-more.html#comment-21983))

> Anonymous says:
>
> Hi Shai, luis already asked and you said soon. Have you got a date on that ? Thanks
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fjson-overscroll-more.html)


### **Anonymous** — March 24, 2014 at 4:32 pm ([permalink](https://www.codenameone.com/blog/json-overscroll-more.html#comment-21438))

> Anonymous says:
>
> No. 
>
> We released the plugin almost a month ago I forgot the exact date.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fjson-overscroll-more.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
