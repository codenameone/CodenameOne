---
title: Properties & Continued Terseness
slug: properties-continued-terseness
url: /blog/properties-continued-terseness/
original_url: https://www.codenameone.com/blog/properties-continued-terseness.html
aliases:
- /blog/properties-continued-terseness.html
date: '2015-11-03'
author: Shai Almog
---

![Header Image](/blog/properties-continued-terseness/cross-platform-shoes.jpg)

We’ve been working on some pretty exciting things recently trying to get them out of the door. As part of  
that work we added some API’s specifically one that probably should have been a part of Codename One 1.0:  
`Properties` file support… 

Working with properties files under Codename One should be identical to working with them under standard  
Java only instead of using `java.util.Properties` we need to use `com.codename1.io.Properties`.  
This allows the implementation to be updated more easily and more consistent across platforms. We  
also fixed some minor historic behaviors in properties e.g. making it derive from `HashMap<String, String>`  
instead of `Hashtable` which makes it both faster and removes the need for casts when working  
with it!  
  
I find properties to be much easier to work with than XML for simple key/value cases. 

#### Terseness Continues

I’ve been on a terse code mission for a while, trying to reduce verbosity in our code which now thanks to lambdas  
is truly standing out. As you recall we added an `encloseIn` API to `Container`,  
this allowed great code like: 
    
    
    Container c = Container.encloseIn(new BoxLayout(BoxLayout.Y_AXIS), myFirstCmp, mySecondCmp, myThirdCmp);

Which is way better than: 
    
    
    Container c = new Container(new BoxLayout(BoxLayout.Y_AXIS));
    c.addComponent(myFirstCmp);
    c.addComponent(mySecondCmp);
    c.addComponent(myThirdCmp);

But its still not great, the part that bothered me is the layout creation code… What we really need is the layout  
and this allowed me to re-think this API and instead come up with this: 
    
    
    Container c = BoxLayout.encloseY(myFirstCmp, mySecondCmp, myThirdCmp);

Which I think expresses what we are trying to do just as well, while removing some boilerplate. Obviously there is  
also an `encloseX` method to correspond. I’ve also encapsulated a similar common use case for BorderLayout  
using: 
    
    
    Container c = BorderLayout.center(centerCmp);

Which is equivalent to: 
    
    
    Container c = new Container(new BorderLayout());
    c.addComponent(BorderLayout.CENTER, centerCmp);

Another long term pet peeve I addressed was that `Form` is missing a layout constructor. This  
is problematic especially due to the fact that we repeated Swing/AWT’s mistake of using `FlowLayout`  
as the default layout!  
In terms of performance this means that every form created allocates a `FlowLayout` and then  
allocates a new layout to replace it. By adding the layout for the content pane in the constructor we effectively  
solve that issue and reduce a line of code. 

And finally I also added an `add(Image)` method to `Container` which is similar  
to the `add(String)` method and is really a shorthand for `add(new Label(img))`.  
Its not a big deal but small things like that make the code slightly more readable.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **davidwaf** — November 4, 2015 at 9:35 pm ([permalink](https://www.codenameone.com/blog/properties-continued-terseness.html#comment-22255))

> davidwaf says:
>
> Properties are so so welcome !
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
