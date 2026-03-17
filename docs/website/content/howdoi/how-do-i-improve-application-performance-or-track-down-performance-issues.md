---
title: IMPROVE APPLICATION PERFORMANCE OR TRACK DOWN PERFORMANCE ISSUES
slug: how-do-i-improve-application-performance-or-track-down-performance-issues
url: /how-do-i/how-do-i-improve-application-performance-or-track-down-performance-issues/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-improve-application-performance-or-track-down-performance-issues.html
tags:
- basic
description: Covers the basics of "what is slow" and how to find out what is bogging
  down your app
youtube_id: set12jVQl_A
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-28.jpg
---
{{< youtube "set12jVQl_A" >}} 

Performance work gets easier once you stop thinking in terms of vague slowness and start looking for specific expensive patterns. In Codename One, the same categories of mistakes show up repeatedly: too much work on the EDT, heavy list renderers, unnecessary image churn, overly dynamic text measurement, and drawing strategies that look harmless in code but cost a lot at runtime.

Lists are one of the first places to inspect. If you are using a list model, avoid changing it one item at a time when what you really mean is "replace a lot of data". Repeated add/remove events trigger repeated notification and repaint work. If you need to make a large change, replacing the model or batching the change is usually much cheaper than firing a long stream of individual updates.

Custom list models and renderers need even more discipline. Methods such as `getItemAt()` must be fast. They should not block on network access or heavy computation. If a list depends on remote data, return quickly and update later when the data arrives. The same principle applies to renderers: do not construct a fresh component tree every time the renderer is asked for output. Reuse renderer components and update their state. Creating components repeatedly inside the renderer is one of the easiest ways to destroy scrolling performance.

Text measurement is another hidden cost. Automatic line breaking and repeated calls that depend on string width can be expensive, especially when they happen frequently during rendering. If a screen is performance-sensitive, prefer simpler label usage and avoid forcing the UI to re-measure large amounts of text over and over when a more stable layout would do.

Images deserve just as much attention. A few sensible image draws are often cheaper than a huge number of tiny draw operations. That is one reason very small tiled images, overly fragmented borders, and repeated low-level drawing primitives can become surprisingly expensive. The old note about gradients is also still worth keeping: decorative gradients are a surprisingly common source of avoidable cost, and in many cases a simple image or cleaner styling choice is the better tradeoff.

Image scaling is another common trap. If you call scaling methods casually, you may be creating larger in-memory images than you realize. The source asset might be small on disk, but the scaled runtime image can cost a lot more RAM. That means scaling should be deliberate, cached where appropriate, and revisited if memory pressure starts to climb.

The right workflow is to measure first, then optimize. Use the performance tools, identify the component or pattern that is actually expensive, and fix that specific thing. Do not rewrite major pieces of a screen just because it feels slow. In Codename One, a small renderer mistake or image decision can dominate the cost of an otherwise reasonable UI.

## Further Reading

- [Performance Network Monitors](/performance-network-monitors/)
- [Developer Guide](/developer-guide/)
- [How Do I Find Problems In My Application, Using The Codename One Tools And The Standard IDE Tools](/how-do-i/how-do-i-find-problems-in-my-application-using-the-codename-one-tools-and-the-standard-ide-tools/)
- [In A Pinch](/blog/in-a-pinch/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
