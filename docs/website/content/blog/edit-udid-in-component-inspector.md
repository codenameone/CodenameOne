---
title: Edit UDID in Component Inspector
slug: edit-udid-in-component-inspector
url: /blog/edit-udid-in-component-inspector/
original_url: https://www.codenameone.com/blog/edit-udid-in-component-inspector.html
aliases:
- /blog/edit-udid-in-component-inspector.html
date: '2016-04-17'
author: Shai Almog
---

![Header Image](/blog/edit-udid-in-component-inspector/edit-udid.png)

One of the hard things to debug in Codename One is UIID/Padding/Margin placement which is often tricky to  
get “just right”. I use the [Component Inspector](https://www.codenameone.com/manual/index.html) quite a lot to  
review a layout that misbehaves and gain further insight into what’s happening in runtime.

__ |  You can gain insight into the Codename One component hierarchy by running the simulator and selecting the  
Simulate → Component Inspector menu option. This will present the component hierarchy as a navigatable tree   
---|---  
  
Up until now the Component Inspector was a “read only” tool, with the coming update it will gain the ability to edit  
the UIID field!

This would make some things remarkably trivial as you could find if that pesky extra space is coming from the theme  
by just changing the UIID to `Container`. It also helps identify components more easily within the inspector and  
see under the hood of Codename One (e.g. in the [Toolbar](https://www.codenameone.com/javadoc/com/codename1/ui/Toolbar.html)  
area).

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
