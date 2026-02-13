---
title: Disable Screenshot, Copy & Paste
slug: disable-screenshots-copy-paste
url: /blog/disable-screenshots-copy-paste/
original_url: https://www.codenameone.com/blog/disable-screenshots-copy-paste.html
aliases:
- /blog/disable-screenshots-copy-paste.html
date: '2017-01-31'
author: Shai Almog
---

![Header Image](/blog/disable-screenshots-copy-paste/security.jpg)

Continuing our security trend from the past month we have a couple of new features for Android security that allow us to block the user from taking a screenshot or copying & pasting data from fields. Notice that these features might fail on jailbroken devices so you might want to [check for jailbreak/rooting](/blog/jailbreak-rooting-detection.html) first.

Blocking screenshots is an Android specific feature that can’t be implemented on iOS. This is implemented by classifying the app window as secure and you can do that via the build hint `android.disableScreenshots=true`. Once that is added screenshots should no longer work for the app, this might impact other things as well such as the task view etc.

We will add the ability to block copy & paste on Android in the coming update. We will add this feature to iOS as we move forward and it should work with the same semantics. You can block copy & paste globally or on a specific field, to block this globally use:
    
    
    Display.getInstance().setProperty("blockCopyPaste", "true");

To block this on a specific field do:
    
    
    textCmp.putClientProperty("blockCopyPaste", Boolean.TRUE);

__ |  Notice that the inverse of using `false` might not work as expected   
---|---

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
