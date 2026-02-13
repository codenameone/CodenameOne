---
title: 'TIP: Uninstall cn1lib'
slug: tip-uninstall-cn1lib
url: /blog/tip-uninstall-cn1lib/
original_url: https://www.codenameone.com/blog/tip-uninstall-cn1lib.html
aliases:
- /blog/tip-uninstall-cn1lib.html
date: '2018-03-26'
author: Shai Almog
---

![Header Image](/blog/tip-uninstall-cn1lib/tip.jpg)

A while back a question was asked on stack overflow [How do you uninstall an extension using the CodenameOne Settings tool in NetBeans?](https://stackoverflow.com/questions/46985038/how-do-you-uninstall-an-extension-using-the-codenameone-settings-tool-in-netbean)

Unfortunately this isn’t automatic due to the way cn1libs are implemented. In some cases you need to uninstall a cn1lib if you no longer need its functionality and this is far from seamless.

These are the steps you need to take:

  1. Remove the files with the name of the extension (the `.cn1lib` and the `.ver` file) from the `lib` directory – you can see them in the files tab in NetBeans or in the file explorer of your OS.

  2. Open Codename One Settings → Build Hints & remove the `ios.****`& `android.` entries you didn’t add manually

  3. Right click project and select Codename One → Refresh Client Libs

This last step will recreate the `ios.****`and`android.` entries needed by other cn1libs you might have installed.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
