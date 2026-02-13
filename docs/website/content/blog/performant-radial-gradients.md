---
title: Performant Radial Gradients
slug: performant-radial-gradients
url: /blog/performant-radial-gradients/
original_url: https://www.codenameone.com/blog/performant-radial-gradients.html
aliases:
- /blog/performant-radial-gradients.html
date: '2016-08-23'
author: Shai Almog
---

![Header Image](/blog/performant-radial-gradients/gradients.png)

One of the first Codename One performance tips is: “Don’t use gradients”. We already [wrote about improved performance to gradients](/blog/should-we-use-gradients.html) in the past but that covered linear gradients and didn’t cover radials on iOS.

With recent commits radial gradients are now performant on iOS/Android and elsewhere. On iOS Steve implemented gradients as a shader which should deliver great performance.

I’m not sure if this is good enough to deliver the level of performance we see from image borders and it’s harder to work with low level graphics primitives.

I think the [old assessments](/blog/should-we-use-gradients.html) still stand. Gradients perform better so it might not be a problem to use them, but I would still use & recommend images over them.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
