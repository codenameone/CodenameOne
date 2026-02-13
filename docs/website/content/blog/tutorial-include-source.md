---
title: Tutorial – Include Source
slug: tutorial-include-source
url: /blog/tutorial-include-source/
original_url: https://www.codenameone.com/blog/tutorial-include-source.html
aliases:
- /blog/tutorial-include-source.html
date: '2017-09-06'
author: Shai Almog
---

![Header Image](/blog/tutorial-include-source/learn-codenameone-1.jpg)

I redid the include source tutorial which was really old by now and included some outdated “facts” while missing key information. Include source allows us to get the native OS project source code on build, this allows us to debug and profile on the devices.

New Codename One users often mix up [native interfaces](/how-do-i---access-native-device-functionality-invoke-native-interfaces.html) and include source. You shouldn’t change the code generated with “include source”. It’s for debugging purposes only. If you need to write native code just use native interfaces to do so, you can then use include source and the native debugger to debug your native interfaces.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
