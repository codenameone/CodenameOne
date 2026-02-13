---
title: 'TIP: Obfuscation Mapping File'
slug: tip-obfuscation-mapping-file
url: /blog/tip-obfuscation-mapping-file/
original_url: https://www.codenameone.com/blog/tip-obfuscation-mapping-file.html
aliases:
- /blog/tip-obfuscation-mapping-file.html
date: '2018-10-15'
author: Shai Almog
---

![Header Image](/blog/tip-obfuscation-mapping-file/tip.jpg)

Proguard is one of the most disliked aspects of Android programming. Developers attack it left and right because there are so many nuances to it. That’s a huge mistake, proguard is one of the most important tools in our development toolchain. It makes our apps slightly more secure, much smaller and even slightly faster. Codename One apps use proguard by default for Android. This is a huge benefit in our case because the limits related to obfuscation are very similar to the limits related to portability.

However, one of the side effects of obfuscation is jumbled stack traces. Normally this isn’t a “big deal” since line numbers are still correct. But when we have multiple releases it might be harder to track some of these line numbers through tags and history.

That’s where the mapping file you get when sending an Android build becomes useful.

![Mapping file in Build Results](/blog/tip-obfuscation-mapping-file/mapping-build.png)

Figure 1. Mapping file in Build Results

You can use proguard to de-obfuscate mappings manually using the [retrace command](https://www.guardsquare.com/en/products/proguard/manual/retrace) but that’s not necessarily helpful.

Google Play includes a section for crashes and ANR’s (Application Not Responding) reported by users. These include stack traces but they might come from a wide range of versions and might be confusing to track.

![The Crashes & ANR's Section](/blog/tip-obfuscation-mapping-file/android-vitals.png)

Figure 2. The Crashes & ANR’s Section

The problem is that you can get crashes that are very unreadable due to obfuscation. That’s where the `mapping.txt` file becomes useful. After you upload the APK file and submit it the next step is the Deobfuscation files section. Here you can upload the mapping file matching your version, this will create slightly more readable stack traces for you.

Notice that the `mapping.txt` file differs between builds even if you didn’t change anything so make sure to use the file matching the APK you uploaded.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
