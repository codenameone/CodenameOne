---
title: Turn It On (Or Off)
slug: turn-it-on-or-off
url: /blog/turn-it-on-or-off/
original_url: https://www.codenameone.com/blog/turn-it-on-or-off.html
aliases:
- /blog/turn-it-on-or-off.html
date: '2013-04-21'
author: Shai Almog
---

![Header Image](/blog/turn-it-on-or-off/hqdefault.jpg)

  

We just added a new On/Off switch to Codename One that should allow you to use this component which is very popular on iOS (and gaining some popularity on Android), this is a rather elaborate component because of its very unique design on iOS but we were able to accommodate most of the small behaviors of the component into our version and it seamlessly adapts between the Android style and the iOS style. 

  
This component will be a part of the next library update (couple of weeks) and will be available in the GUI builder as well as using code (as usual) it will carry the name OnOffSwitch.  

  
Initially I wanted the component to be a “drop in replacement” for checkbox by deriving from that class in order to implement this feature. However, the animation abilities of animated layout (which do lots of the effects you see on the Android version) drew me to using a Container. So you won’t be able to use the exact same code as a CheckBox.   

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
