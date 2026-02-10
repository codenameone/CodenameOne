---
title: Hiding, URL Security & Advocacy
slug: hiding-url-security-advocacy
url: /blog/hiding-url-security-advocacy/
original_url: https://www.codenameone.com/blog/hiding-url-security-advocacy.html
aliases:
- /blog/hiding-url-security-advocacy.html
date: '2015-11-08'
author: Shai Almog
---

![Header Image](/blog/hiding-url-security-advocacy/hiding-security-advocacy.png)

A common trick for animating Components in Codename One is to set their preferred size to 0 and then invoke  
`animateLayout()` thus triggering an animation to hide said component. There are several issues  
with this trick but one of the biggest ones is the fact that `setPreferredSize` has been deprecated  
for quite a while. 

We recently added a `setHidden`/`isHidden` method pair that effectively encapsulates  
this functionality and a bit more. This shouldn’t be confused with `setVisible`/`isVisible`  
that just toggle the visibility of the component.  
One of the issues `setHidden` tries to solve is the fact that preferred size doesn’t include the margin  
in the total and thus a component might still occupy space despite being hidden. To solve this the margin is set to 0  
when hiding and restored to its original value when showing the component again by resetting the UIID  
(which resets all style modifications).  
This functionality might be undesired which is why we have a version of the `setHidden` method that  
accepts a boolean flag indicating whether the margin/UIID should be manipulated. You can effectively  
hide/show a component without deprecated code using something like this: 
    
    
    Button toHide = new Button("Will Be Hidden");
    Button hide = new Button("Hide It");
    hide.addActionListener((e) -> {
        hide.setEnabled(false);
        boolean t = !toHide.isHidden();
        toHide.setHidden(t);
        toHide.getParent().animateLayoutAndWait(200);
        toHide.setVisible(t);
        hide.setEnabled(true);
    });

Notice that in the first/last lines of the event processing I block the button from getting additional events. Since  
the code is sequential this works rather well and the button won’t get duplicate events during the animation.  
Codename One currently doesn’t support concurrent animations so its up to you as a developer to serialize  
your animation requests in the framework. 

#### Accessing Insecure URL’s In iOS 9

Due to recent security exploits Apple blocked some access to insecure URL’s which means that http code that  
worked before might stop working for you on iOS 9. This is generally a good move, you should use https and  
avoid http as much as possible but that’s sometimes impractical especially when working with an internal  
or debug environment (setting up SSL is a pain). 

We considered adding the required build hints by default but it seems that Apple will reject your app if you just  
include that and don’t have a good reason. We could have done it for debug only but then people might have  
run into it in production.  
The solution at the moment is to use the venerable `ios.plistInject` build hint and set it to:  
`<key>NSAppTransportSecurity</key><dict><key>NSAllowsArbitraryLoads</key><true/></dict>`  
Read more about this in [the discussion forum post](https://groups.google.com/d/msg/codenameone-discussions/5jrBWjgcarM/Q5z28PsrEgAJ). 

#### New Advocacy Group

We’ve posted about this to the discussion forum but if you subscribe via email it might have gotten buried.  
If you want to help Codename one and its related 3rd party projects by contributing at most 5 minutes per  
day please join [this group](https://groups.google.com/forum/#!forum/codename-one-advocacy).  
Thru this group we (and you if you have a Codename One relevant project such as an advocacy site, tutorial,  
library or blog post) can post links and ask for social help: e.g. upvote, like, share, retweet, etc. 

With social networks its pretty hard to get your voice out as people get drowned with messaging from all directions.  
In this way we can both help you guys to get noticed by the community at large and you can help us to spread  
our message further thru social clout.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
