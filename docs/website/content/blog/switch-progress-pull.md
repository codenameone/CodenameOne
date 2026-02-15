---
title: Switch, Progress and Pull to Refresh
slug: switch-progress-pull
url: /blog/switch-progress-pull/
original_url: https://www.codenameone.com/blog/switch-progress-pull.html
aliases:
- /blog/switch-progress-pull.html
date: '2018-10-10'
author: Shai Almog
---

![Header Image](/blog/switch-progress-pull/pixel-perfect.jpg)

Some of our older components were developed years ago. As Android and iOS slowly converged their UI paradigms we got stuck supporting odd/outdated functionality as designs shifted. Three great examples are pull to refresh, `OnOffSwitch` and the `InfiniteProgress` features.

`OnOffSwitch` contained labels both in iOS & Android. On Android it was literally a button that was moved back and forth. Today both OS’s use a simple switch graphic. When we developed the original component we didn’t have the same level of graphic drawing capability that we have today, that made the design of the iOS version even harder.

Pull to refresh was a feature twitter introduced. It became a hit among developers and slowly made its way into OS’s each of which implemented it differently. The original approach was very iOS centric and relied on the way drag works on that platform. Newer approaches such as the one in material design use an overlay approach. This also brings us to the `InfiniteProgress` class which is technically a trivial class, however in material design it has a very distinct special effect that’s hard to replicate in the current design.

### Switch

People have been complaining about the `OnOffSwitch` [for a while](https://github.com/codenameone/CodenameOne/issues/2556). Recently [ramsestom](https://github.com/ramsestom) contributed some Android code to improve this behavior and we decided to take the plunge.

Fixing the old class would have been hard. It was designed to support two very different component types and includes a lot of kludges necessary for the limited graphics capabilities of Codename One 1.0. So we needed a clean break with the new `Switch` class.

![The New Switch Class in Action](/blog/switch-progress-pull/switch-android-ios.png)

Figure 1. The New Switch Class in Action

The new class is trivial and works as a drop-in replacement for `OnOffSwitch`. The great thing about it is the newfound ability to customize everything through the theme/css. You can learn more about that in the [class JavaDoc](/javadoc/com/codename1/components/Switch/).

### InfiniteProgress

Despite its huge legacy there was no need to rewrite `InfiniteProgress` since the class is much simpler. However, since the new material design progress behavior might not be to everyones liking its off by default. We’ll switch this on for Android when we feel it’s stable enough.

You can activate the experimental mode either through code using:
    
    
    InfiniteProgress.setDefaultMaterialDesignMode(true);

Or by setting the theme constant `infiniteProgressMaterialModeBool` to `true`. Both of these will impact the pull to refresh and progress animation.

Notice that the implementation of pull to refresh would be completely different when this is invoked so it’s very possible that quite a few things would break or misbehave. Proceed with caution and let us know!

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
