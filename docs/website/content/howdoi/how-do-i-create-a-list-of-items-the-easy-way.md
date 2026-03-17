---
title: CREATE A LIST OF ITEMS
slug: how-do-i-create-a-list-of-items-the-easy-way
url: /how-do-i/how-do-i-create-a-list-of-items-the-easy-way/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-create-a-list-of-items-the-easy-way.html
tags:
- basic
- ui
description: Infinite lists of items are powerful tools
youtube_id: 0m7Bay4g93k
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-11-1.jpg
---
{{< youtube "0m7Bay4g93k" >}}

If you need to show a vertical list of items in Codename One, the first question is not "how do I use `List`?" It is "what kind of scrolling UI am I actually building?" The video makes a point that still matters: for many ordinary mobile screens, a vertically stacked container of components is easier to reason about than the older `List` API.

A simple and effective pattern is to use a container with `BoxLayout.y()` and place it in a scrollable area. That gives you full control over each row. You can use `MultiButton`, your own custom components, or any other component structure that fits the design. For small and medium-sized data sets, this is often the cleanest way to build a list-like screen.

The point where this changes is scale. If the data set is large, you should not create hundreds or thousands of row components eagerly. That is where `InfiniteContainer` and related lazy-loading patterns become valuable. Instead of building the entire list at once, you fetch components in batches as the user scrolls. The video uses contacts as an example, and that is still a good way to think about it: load enough to keep the UI responsive, then fetch more as needed.

Scrolling behavior is part of the design, not an afterthought. A scrollable list container needs room to stretch properly, which is why placing it in the `CENTER` of a `BorderLayout` is such a common pattern. Nested scrolling should also be treated carefully, because touch interfaces are much harder to use when multiple scrollable areas compete for the same gesture.

Lazy-loading also changes how you think about per-row work. If an item depends on expensive data such as an image, do not block the row creation path longer than necessary. Show a placeholder, let the row appear quickly, and then fill in the heavier data when the application has time. The older example uses contact images and idle callbacks to illustrate this, and the underlying lesson is still sound: rows should become visible fast, then improve gracefully as more data arrives.

Search fits naturally into this model when you treat the visible data as a view over a larger data set. Change the filter, refresh the list, and let the fetch logic rebuild the visible subset instead of trying to mutate dozens of existing components manually. That keeps the mental model simple and scales better as the screen evolves.

The main thing that changed since the video is not the underlying idea but the broader UI workflow around it. In modern projects, the structure of the row still belongs in code and layout, but much of the visual styling should be pushed into CSS. That keeps complex list screens easier to maintain as they grow.

## Further Reading

- [Layout Basics](/layout-basics/)
- [Developer Guide](/developer-guide/)
- [How Do I Positioning Components Using Layout Managers](/how-do-i/how-do-i-positioning-components-using-layout-managers/)
- [How Do I Improve Application Performance Or Track Down Performance Issues](/how-do-i/how-do-i-improve-application-performance-or-track-down-performance-issues/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
