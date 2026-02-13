---
title: Removing the Old Preferences
slug: removing-old-preferences
url: /blog/removing-old-preferences/
original_url: https://www.codenameone.com/blog/removing-old-preferences.html
aliases:
- /blog/removing-old-preferences.html
date: '2018-09-04'
author: Shai Almog
---

![Header Image](/blog/removing-old-preferences/generic-java-2.jpg)

A while back we introduced Codename One Settings which superceded the old approach built in the IDE itself. This allowed us to consolidate code and move faster. That’s how we were able to implement more wizards for things like CSS support etc.

Up until now we just left the old UI in place. People are still used to it. But it has a lot of bugs and causes confusion as developers launch the old UI instead of the new one. So with version 5.0 we’ll remove the old preferences UI and leave Codename One Settings.

We’ll also remove the deprecated build targets from the menu and try to streamline the first usage of Codename One a bit. Most of these changes shouldn’t be noticeable for most developers. However, from experience I’m sure there are quite a few developers out there that didn’t move to Codename One Settings.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
