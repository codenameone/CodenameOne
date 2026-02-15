---
title: WKWebView and PRs
slug: wkwebview-prs
url: /blog/wkwebview-prs/
original_url: https://www.codenameone.com/blog/wkwebview-prs.html
aliases:
- /blog/wkwebview-prs.html
date: '2019-10-04'
author: Shai Almog
---

![Header Image](/blog/wkwebview-prs/new-features-5.jpg)

We still have features to cover from our summer vacation but we need to make a short de-tour through newer things that landed recently. One of the big highlights is the switch to [WKWebView](/blog/wkwebview/). We effectively changed the default iOS browser component to `WKWebView` instead of `UIWebView`. This resolved warnings Apple started sending out to developers about using the out of date `UIWebView`.

This mostly went unnoticed by most developers as it should. But if your browser starts acting up this is the reason. There isn’t much we can do here as we knew that day would come where Apple will demand a switch.

### Video Library

[Francesco Galgani](https://github.com/jsfan3/) created an impressive [cn1lib for low level video access](https://github.com/jsfan3/CN1Libs-VideoOptimizer/) named “VideoOptimizer”.  
It supports compressing video files for distribution, grabbing video frame screenshots, getting the duration of a video etc.

This is pretty cool and also a pretty difficult task as it involved integrating the ffmpeg native library in an Android build with an AAR to package the whole thing.

### ReleasableComponent

[ramsestom](https://github.com/ramsestom) implemented a generic interface for releasable components in [PR #2910](https://github.com/codenameone/CodenameOne/pull/2910). Before this Codename One had special cases for `Button` so if a user pressed a button and didn’t release it we made sure to let the button know about this at some point…​

This is now generic via the new `ReleasableComponent` (named `IReleasable` in the PR which was renamed in the following [commit](https://github.com/codenameone/CodenameOne/commit/da75eef0c247d8d29039b0abea963efd80023909)).

### Popup Direction and Mime Guessing

[Francesco Galgani](https://github.com/jsfan3/) submitted two PRs. First [#2914](https://github.com/codenameone/CodenameOne/pull/2914) which lets you explicitly set the auto complete popup direction e.g.:
    
    
    autoComplete.setPopupPosition(AutoCompleteTextField.POPUP_POSITION_OVER);

Valid values are: `POPUP_POSITION_AUTO`, `POPUP_POSITION_OVER` and `POPUP_POSITION_UNDER`.

The [second PR #2925](https://github.com/codenameone/CodenameOne/pull/2925) includes an API to guess common file mime types from the first few bytes of a file. Using `Util.guessMimeType` e.g.:
    
    
    String mimeType = Util.guessMimeType(fileOrStorageFile);
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Durank** — October 22, 2019 at 8:54 pm ([permalink](/blog/wkwebview-prs/#comment-24264))

> [Durank](https://avatars0.githubusercontent.com/u/16245755?v=4) says:
>
> hi, In download the video library and I refresh my cn1 libs but the class VideoOptimizer isn’t imported. Please hel me.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
