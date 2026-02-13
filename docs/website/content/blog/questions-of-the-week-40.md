---
title: Questions of the Week 40
slug: questions-of-the-week-40
url: /blog/questions-of-the-week-40/
original_url: https://www.codenameone.com/blog/questions-of-the-week-40.html
aliases:
- /blog/questions-of-the-week-40.html
date: '2017-01-26'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-40/qanda-friday2.jpg)

Today we have the first weekly release since the 3.6 release and as such it is choke full of changes which is natural given that we skipped a release and had a lot of code pending to “post release”. So please be vigilant especially if you use peer components and let us know about potential regressions ASAP.

an [anonymous](http://stackoverflow.com/users/7454657/anonymous) poster asked a recurring question that we still don’t have a great answer for, [how to capture timed video](http://stackoverflow.com/questions/41852856/how-to-automatically-start-video-recording-and-stop-automatically-after-a-predef). This is possible to do with audio using the `MediaManager` but it can’t be done with video which can only use `Capture`. However, thanks to the new `PeerComponent` work we might expose low level camera API’s including video/picture capture with viewfinder etc.

This was always possible to implement before (even for 3rd parties) using `PeerComponent` but with the recent z-order improvements the benefit becomes HUGE.

[Stefan](http://stackoverflow.com/users/5695429/stefan-eder) asked about [porting Flamingo SVG to Codename One](http://stackoverflow.com/questions/41847770/would-it-be-a-good-idea-to-use-flamingo-svg-transcoder-for-high-quality-svg-imag) which is something [I’ve been pondering too](https://github.com/codenameone/CodenameOne/issues/1890)!

If you are interested in fast cross platform SVG this is a great opportunity for community involvement.

[Stefan](http://stackoverflow.com/users/5695429/stefan-eder) also [asked this question](http://stackoverflow.com/questions/41760110/rectangles-are-not-drawn-as-expected) which is one of those niche pieces of knowledge most of us don’t notice until we bump our head against them.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
