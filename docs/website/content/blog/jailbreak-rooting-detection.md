---
title: Jailbreak/Rooting Detection
slug: jailbreak-rooting-detection
url: /blog/jailbreak-rooting-detection/
original_url: https://www.codenameone.com/blog/jailbreak-rooting-detection.html
aliases:
- /blog/jailbreak-rooting-detection.html
date: '2017-01-23'
author: Shai Almog
---

![Header Image](/blog/jailbreak-rooting-detection/security.jpg)

iOS & Android are walled gardens which is both a blessing and a curse. Looking at the bright side the walled garden aspect of locked down devices means the devices are more secure by nature. E.g. on a PC that was compromised I can detect the banking details of a user logging into a bank. But on a phone it would be much harder due to the deep process isolation.

This isn’t true for jailbroken or rooted devices. In these devices security has been compromised often with good intentions (opening up the ecosystem) but it can also be used as a step in a serious attack on an application!

For obvious reasons it’s really hard to accurately detect a jailbroken or rooted device but when possible if you have a high security app you might want to block the functionality or even raise a “silent alarm” in such a case. To detect this we are introducing a new method:
    
    
    if(Display.getInstance().isJailbrokenDevice()) {
        // probably jailbroken or rooted
    } else {
       // probably not
    }

Notice that this is all “probably”, we can’t be 100% sure as there are no official ways to detect that. That is why it’s crucial to encrypt everything and assume the device was compromised to begin with when dealing with very sensitive data. Still it’s worthwhile to use these API’s to make the life of an attacker just a little bit harder.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
