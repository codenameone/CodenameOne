---
title: Synchronous InfiniteContainer
slug: synchronous-inifinitecontainer
url: /blog/synchronous-inifinitecontainer/
original_url: https://www.codenameone.com/blog/synchronous-inifinitecontainer.html
aliases:
- /blog/synchronous-inifinitecontainer.html
date: '2016-06-26'
author: Shai Almog
---

![Header Image](/blog/synchronous-inifinitecontainer/generic-java-2.jpg)

[InfiniteContainer](/javadoc/com/codename1/ui/InfiniteContainer/) and  
[InfiniteScrollAdapter](/javadoc/com/codename1/components/InfiniteScrollAdapter/)  
revolutionized the way we think about Codename One. Up until their introduction we advocated lists for large  
sets of components and this is no longer the case.

However, `InfiniteContainer` has a controversial feature even within out team. It violates the EDT **on purpose** …​

`InfiniteContainer` allows you to “fetch” data dynamically into the container as the user scrolls down. The definition  
of “fetch” is problematic though. Up until now the [fetch](/javadoc/com/codename1/ui/InfiniteContainer/#fetchComponents-int-int-)  
method was invoked in a separate thread. This was documented in the class but it is pretty problematic as the  
method returns an array of components.

In practice creating a component in a separate shouldn’t pose a problem, yes it does violate the core Codename One  
principal of always doing everything on the EDT but construction **should** work. However, this does include some  
problems:

  * The EDT violation detection marks such code as violating

  * The most common case (networking) already works rather well off the EDT and doesn’t need a separate thread

  * This is inconsistent with the rest of Codename One

  * There are some edge cases where this might trigger a real EDT violation e.g. if component construction triggers  
an event thus creating a race condition with the EDT

So to workaround this we added the new method: `protected boolean isAsync()`.

If you override this method to return `false` the `fetch` method will be invoked on the EDT but this might break  
compatibility so currently this method is set to return `true`.

We currently plan to change the method to return `false` by default within a couple of weeks. This might change  
based on feedback we get from developers.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
