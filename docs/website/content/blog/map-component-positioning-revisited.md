---
title: Map Component Positioning Revisited
slug: map-component-positioning-revisited
url: /blog/map-component-positioning-revisited/
original_url: https://www.codenameone.com/blog/map-component-positioning-revisited.html
aliases:
- /blog/map-component-positioning-revisited.html
date: '2018-04-09'
author: Shai Almog
---

![Header Image](/blog/map-component-positioning-revisited/maps.jpg)

I published two articles on `MapLayout` [here](https://www.codenameone.com/blog/map-layout-update.html) and [here](https://www.codenameone.com/blog/tip-map-layout-manager.html). After all that work they are now effectively obsolete thanks to a new API in `MapContainer` that builds component placement directly into the map itself.

Unfortunately the Google API doesn’t let us position components (native or otherwise) accurately as it pans the map. This creates a small delay when panning/zooming as the components try to catch up to the map. The only workaround is to convert the components to images and ask the map to move images within it. Then convert the images back when the map finishes panning. That’s exactly what Steve implemented within the native maps.

For you as a developer this is all seamless. If you use the map API and add components into the map they will “magically” update to the right position & transition between rendering as an image and as a component. If you would like more control over this process you can override the new `toImage()` method of `Component` to tune that behavior.

The new API works just like the regular marker API, you can add a component to the map at a given latitude/longitude position using:
    
    
    map.addMarker(component,location);

You can also anchor the location to a specific alignment vertically and horizontally.

This makes the Uber demo app much simpler.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — April 10, 2018 at 1:50 pm ([permalink](https://www.codenameone.com/blog/map-component-positioning-revisited.html#comment-23732))

> Francesco Galgani says:
>
> It’s a very good news 🙂
>
> However, in the past (in one of my StackOverflow questions) you suggested me to implement a new custom layout extending the abstract class Layout. I’ve done it and it solved my problem, I used your MapLayout as an example of custom external Layout. My suggestion is to add some documentation (in the developer guide and/or in a tutorial) on the basic steps to implement a simple custom layout, because your example of MapLayout is quite complex. Thank you 🙂
>



### **Shai Almog** — April 11, 2018 at 7:16 am ([permalink](https://www.codenameone.com/blog/map-component-positioning-revisited.html#comment-23844))

> Shai Almog says:
>
> The original version of that layout was simpler. The complexity came from communicating with the underlying map API. We used to have a different sample which I can no longer find but it wasn’t very realistic. The gist of the layout manager is just the logic within layoutContainer. Not much more. You can check out the existing layout managers for samples.
>
> Unfortunately I can’t think of a layout manager that I would want to write that isn’t easier to implement with existing layout managers.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
