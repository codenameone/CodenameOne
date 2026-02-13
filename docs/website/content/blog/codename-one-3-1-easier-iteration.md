---
title: Codename One 3.1 & Easier Iteration
slug: codename-one-3-1-easier-iteration
url: /blog/codename-one-3-1-easier-iteration/
original_url: https://www.codenameone.com/blog/codename-one-3-1-easier-iteration.html
aliases:
- /blog/codename-one-3-1-easier-iteration.html
date: '2015-07-12'
author: Shai Almog
---

![Header Image](/blog/codename-one-3-1-easier-iteration/3.1.jpg)

Its been a busy month and getting busier by the moment, we are preparing for App Engines suspension of  
its blobstore service which will be coming around soon. This effectively means older crash report email  
functionality will be stopped for older apps (just rebuild the app for the emails to work again).   
We are also getting ready for Codename One 3.1 which we have tentatively scheduled for July 27th. This  
release will include a weeks worth of code freeze and will be  
the first of our new policy for faster release schedules. 

Some features we wanted to make it will have to go in to 3.2 but overall we are pretty thrilled with the shorter  
release cycle which makes a whole lot of sense in the mobile industry where things change so frequently.  
We will release an updated plugin tomorrow morning which should include a lot of the newer features for 3.1. 

#### Easier Iteration on Containers

I often write code that needs to iterate over the components of a container in the form: 
    
    
    int count = cont.getComponentCount();
    for(int iter = 0 ; iter < count ; iter++) {
       Component c = cont.getComponentAt(iter);
       // do something with c
    }

This is tedious and annoying, so with the latest version of Codename One it implements the `Iterable` interface  
which allows this instead: 
    
    
    for(Component c : cont) {
       // do something with c
    }

Which is obviously easier, its not necessarily as efficient but its easier to write. These are the sort of features  
we should add into Codename One all over e.g. we just fixed `Element` from the XML parsing  
code to allow iteration over its children in a similar way.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
