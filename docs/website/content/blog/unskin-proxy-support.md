---
title: Unskin & Proxy Support
slug: unskin-proxy-support
url: /blog/unskin-proxy-support/
original_url: https://www.codenameone.com/blog/unskin-proxy-support.html
aliases:
- /blog/unskin-proxy-support.html
date: '2016-06-05'
author: Shai Almog
---

![Header Image](/blog/unskin-proxy-support/proxy-settings.png)

With the upcoming library update this weekend we will remove the venerable (old) skins that are baked into the  
simulator. This means that they will no longer be immediately accessible but you can still download all of them  
thru the Skins → More menu option.

The chief motivations for this are to keep the distribution smaller (our plugin just crossed the 100MB mark which  
isn’t great) & to keep the skins up to date. Some of the newer skins (e.g. iPad pro) are HUGE and bundling them  
is impractical.

This means that if you use a different simulator of the builtin ones it will flip to the iPhone 3gs simulator which is  
the only one we will ship by default. To fix this just select the More option and download any skin you like.

### Working Behind a Proxy

In this weeks library update we released an important feature: Proxy support.

Previously setting up a proxy required doing this globally in the Java settings which are somewhat hidden. This  
new UI allows you to determine a custom setting for Codename One and also allows you to pick the system  
proxy which is what most of us would expect to be the default.

This is all part of the work done on [issue 1740](https://github.com/codenameone/CodenameOne/issues/1740) and  
should apply to apps built with Codename One seamlessly.

Starting with the next plugin update the setting for the proxy picked in the simulator will also apply to builds sent  
to the cloud servers so this is will allow you to avoid previous hacks such as editing the `build.xml` to add proxy  
settings.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
