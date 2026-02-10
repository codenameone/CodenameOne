---
title: 'TIP: IntelliJ/IDEA RAM'
slug: tip-intellij-idea-ram
url: /blog/tip-intellij-idea-ram/
original_url: https://www.codenameone.com/blog/tip-intellij-idea-ram.html
aliases:
- /blog/tip-intellij-idea-ram.html
date: '2018-04-02'
author: Shai Almog
---

![Header Image](/blog/tip-intellij-idea-ram/tip.jpg)

I’m used to NetBeans but if I will ever switch it will be to IntelliJ/IDEA. It’s a great IDE. I just need to rewire the muscle memory of my fingers for it. The Codename One plugin support on IntelliJ should be as good as the NetBeans support as the code is very similar. There are however a couple of pitfalls that a lot of people trip over which I’d like to discuss.

### Tree Mode

IntelliJ defaults to showing errors as a “tree”. It parses the output of the app to the console and shows “pretty” output. Unfortunately this doesn’t always play nice with Codename One and hides things like compiler errors.

We strongly recommend you disable that setting…​

![Toggling off the Tree Mode](/blog/tip-intellij-idea-ram/intellij-tree-mode-toggle.png)

Figure 1. Toggling off the Tree Mode

### Out of Memory in Ant Build

When you start working and your code grows (especially with Kotlin) you might run into an out of memory error during compilation. You can resolve that by setting the memory available to Ant as explained in this image.

![Configuring the Memory Available to Ant](/blog/tip-intellij-idea-ram/intellij-ant-configuration.png)

Figure 2. Configuring the Memory Available to Ant
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **davidwaf** — April 4, 2018 at 8:16 am ([permalink](https://www.codenameone.com/blog/tip-intellij-idea-ram.html#comment-21431))

> Netbeans here too. Can’t just switch.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-intellij-idea-ram.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
