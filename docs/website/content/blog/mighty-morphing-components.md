---
title: Mighty Morphing Components
slug: mighty-morphing-components
url: /blog/mighty-morphing-components/
original_url: https://www.codenameone.com/blog/mighty-morphing-components.html
aliases:
- /blog/mighty-morphing-components.html
date: '2014-10-25'
author: Shai Almog
---

![Header Image](/blog/mighty-morphing-components/mighty-morphing-components-1.png)

  
  
  
  
![Picture](/blog/mighty-morphing-components/mighty-morphing-components-1.png)  
  
  
  

  
  
One of the nice effects in the Android material design is the morphing effect where an element from the previous form (activity) animates to become a different component on a new activity. We’ve had a morph effect in the Container class since the original beta of Codename One but it didn’t work as a transition between forms and didn’t allow for multiple separate components to transition at once. 

To support this behavior we are adding a MorphTransition class which will be available in the next Codename One update, it allows you to use this effect without much of a hassle. To demonstrate this we made a few changes to the Kitchen Sink demo where we added a Label containing the icon/name of the demo to the top of each demo form, this allows us to morph the icon being clicked into that label. See the video here:  
  

  

* * *

  

  
Since the transition is created before the form exists we can’t reference explicit components within the form when creating the morph transition (in order to indicate which component becomes which) so we need to refer to them by name. This means we need to use setName(String) on the components in the source/destination forms so the transition will be able to find them.  
  

  

  
As you can see from the code above incorporating the morph transition is pretty trivial, we just added a new Label to which the button from the demo will morph and gave it a hardcoded name. Notice we needed to give the demo button a dynamic name based on the demo name. The reason is that in the specific demo there will be only one target but in the main form there are multiple demos.  
  
Then we installed the new transition on the main form and on the demo form, notice that we used the same order of from-to in both cases. The transition will play in reverse when going back so the from-to will be implicitly reversed by the back command.  
  
Also notice the structure of the MorphTransition command, create accepts the time in milliseconds (3000 for a slow transition) then you can chain as many morph commands as you want (one for each component pair) to indicate which component should become which.  
  

  

  

  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — October 27, 2014 at 4:31 pm ([permalink](https://www.codenameone.com/blog/mighty-morphing-components.html#comment-22116))

> Anonymous says:
>
> That’s very neat. Nice effect.
>



### **Anonymous** — November 1, 2014 at 1:27 pm ([permalink](https://www.codenameone.com/blog/mighty-morphing-components.html#comment-21849))

> Anonymous says:
>
> Great one! you guys are doing a great job keeping up with everything that matters.
>



### **Anonymous** — November 27, 2014 at 10:54 pm ([permalink](https://www.codenameone.com/blog/mighty-morphing-components.html#comment-22156))

> Anonymous says:
>
> Kool
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
