---
title: Zulu Desktop Builds
slug: zulu-desktop-builds
url: /blog/zulu-desktop-builds/
original_url: https://www.codenameone.com/blog/zulu-desktop-builds.html
aliases:
- /blog/zulu-desktop-builds.html
date: '2019-04-10'
author: Shai Almog
---

![Header Image](/blog/zulu-desktop-builds/new-features-6.jpg)

Sometimes you pick up a task and know in advance it’s going to be hell to implement it. Sometimes, the hell you were expecting turns out to be **WAY** worse than you anticipated. This is the case for modern Java desktop deployment. Amazingly, Java was able to take one of the worse deployment processes possible and make it **MUCH** worse than before.

If you haven’t used the Codename One desktop port I’ll sum it up to you. We use `javapackager` (formerly `javafxpackager`) to essentially wrap the jar file as a DMG/pkg on Mac OS and as an EXE/MSI on Windows. This "worked" in the broad sense of the word. It had a lot of issues and we had to create a lot of workarounds.

However, Oracle effectively killed Java 8. This left us in a bind with an out of date VM that we need to maintain. Some Java advocates came out a while back with an open document claiming that ["Java is still free"](https://docs.google.com/document/d/1nFGazvrCvHMZJgFstlbzoHjpAVwv5DEdnaBr_5pKuHo/edit#heading=h.p3qt2oh5eczi)…​

If you think that a 23 page document explaining that something is "free" sounds problematic…​ Well, you have a point.

### ZuluFX

Unfortunately we need JavaFX. My [opinion of JavaFX](/blog/should-oracle-spring-clean-javafx/) hasn’t changed, if anything I think it’s worse off than ever before. But in the Java world there is no other option. We need video, HTML and other capabilities for our simulator and desktop port so we need JavaFX support.

Luckily, Azul releases a packaged OpenJDK distribution called [ZuluFX](https://www.azul.com/downloads/zulu/zulufx/) which includes JavaFX within sparing us the need to package it ourselves. Unfortunately, basic things in `javapackager` just don’t work properly with Zulu. It seems it picks the full JDK instead of the JRE causing it to produce a 200mb hello world application. It doesn’t work at all on Windows and on Mac OS it’s remarkably flaky, it just fails on random.

After struggling with this approach for a couple of weeks we gave up. Using `javapackager` is no longer a tenable approach so we’re using a manual approach of generating installers using 3rd party tools. Technically this is the same approach used by `javapackager` internally so this shouldn’t be too different, just a whole lot of more work for us.

### New Build Hints

This should land on the build servers tomorrow but this is highly experimental so I would suggest caution for at least a few weeks. It’s very likely compilation will fail for some builds right now as we’re ironing out the kinks. Still if you run into a problem let us know as we might not be aware of it.

You can use the build hints `win.desktop-vm=zuluFx8` or `mac.desktop-vm=zuluFx8` to hint that you want to use the Zulu VM instead of the default Oracle JRE. Once this becomes stable we might flip the switch and make this the default target moving forward as it gives us more control over the end result.

At the moment we only support unsigned EXE/DMG targets for this. Hopefully we’ll be able to address the full range of supported targets as we migrate to this approach.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
