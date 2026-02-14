---
title: It's In The Bag
slug: its-in-the-bag
url: /blog/its-in-the-bag/
original_url: https://www.codenameone.com/blog/its-in-the-bag.html
aliases:
- /blog/its-in-the-bag.html
date: '2013-08-18'
author: Shai Almog
---

![Header Image](/blog/its-in-the-bag/its-in-the-bag-1.png)

  
  
  
[  
![GridBagLayout Swing vs. Codename One](/blog/its-in-the-bag/its-in-the-bag-1.png)  
](/img/blog/old_posts/its-in-the-bag-large-2.png)  
  
  

GridBag that is. So GridBagLayout is  
[  
one of the most notorious of the layout managers in Java  
](http://madbean.com/anim/totallygridbag/)  
in fact for many developers it symbolizes the failure of the layout manager concept. That is the main reason why we never added it. 

  
Last week I had a very interesting conversation with a very prominent Swing developer  
  
and he asked me whether we had gridbag support. I answered that we do not and repeated the regular “no one likes it” line, turns out he does like it and has a lot of Swing code that uses GridBag!  
  
  
  
Interesting…

  
Porting a Swing/AWT layout manager to Codename One is pretty close to trivial, there are very few things you need to actually change.  
  

  1.   
Codename One doesn’t have Insets, I added some support for them in order to port gridbag but components in Codename One have a Margin they need to consider instead of the insets (the padding is in the preferred size).  

  2.   
AWT layout managers also synchronize a lot on the AWT thread. This is no longer necessary since Codename One is single threaded like Swing.  

  3.   
Components are positioned relatively to container so the layout code can start at 0, 0 (otherwise it will be slightly offset).  

Other than those things its mostly just fixing method signatures and import statements which are slightly different. Pretty trivial stuff and GridBagLayout from project Harmony is now working on Codename One.

  
  
So to show this off I  
[  
took this code from the Java tutorial  
](http://docs.oracle.com/javase/tutorial/uiswing/layout/gridbag.html)  
and ported it to Codename One, pretty easy stuff:  
  
  

* * *

The code is almost the same although I did need to make some adaptations e.g. JButton to Button, add to addComponent and reverse the order of arguments to addComponent. Other than that it was pretty trivial.  
  
  
  
Now this stuff probably won’t make it into the GUI builder in the forseeable future, but if you are hellbent on GridBag or porting some Swing code this should be pretty convenient. Its also a great case study if you want to port some of your other favorite layout managers such as MiG or FormLayout.  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — August 19, 2013 at 11:51 pm ([permalink](https://www.codenameone.com/blog/its-in-the-bag.html#comment-21776))

> Anonymous says:
>
> I agree that GridBagLayout was hell to configure by hand, however I learned to like it more since the NetBeans team created their fancy customizer a few years ago… I wonder how easy/hard it would be to reuse it in CN1’s own designer ?
>



### **Anonymous** — September 5, 2013 at 3:35 pm ([permalink](https://www.codenameone.com/blog/its-in-the-bag.html#comment-21803))

> Anonymous says:
>
> Yeah, I used gridbag a lot. It might be painful, but sometimes it was the best thing to do. I tried some of the other replacements and they were even more difficult and were not hand modifiable.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
