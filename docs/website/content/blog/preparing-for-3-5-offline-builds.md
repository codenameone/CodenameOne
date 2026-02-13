---
title: Preparing for 3.5 & Offline Builds
slug: preparing-for-3-5-offline-builds
url: /blog/preparing-for-3-5-offline-builds/
original_url: https://www.codenameone.com/blog/preparing-for-3-5-offline-builds.html
aliases:
- /blog/preparing-for-3-5-offline-builds.html
date: '2016-07-06'
author: Shai Almog
---

![Header Image](/blog/preparing-for-3-5-offline-builds/codenameone35.jpg)

We are preparing for the 3.5 code freeze which should go into effect on July 26th. We are still not sure about the  
full set of features that will make it into 3.5 as the release is pretty close by now. However, we already have some cool  
tentative plans for 3.6 and beyond that we are already sketching out.

Unless there are major regressions we’ll deliver 3.5 on August 2nd and 3.6 on December 5th. At this time version  
3.7 is planned for April 4th 2017 and 3.8 for August 1st 2017.

You will notice we toned it down slightly to 3 releases a year instead of 4 which we feel would provide us a bit  
more breathing room as we move forward.

We don’t want to get too much into details but one of the features that we have been exploring for the 3.6 timeline  
is the ability to build offline, this is a feature we will only expose to the enterprise grade subscribers.

### Why only Enterprise?

There are several reasons, the technical one is that offline builds are no panacea. Things fail. The support effort  
for offline builds is huge, as evidence despite the fact that all of our code is open source very few people bothered  
trying to compile it because of the complexities.

We don’t think building offline is convenient and we always recommended avoiding it. When we build  
our own apps we use the cloud just like everyone else because it’s surprisingly faster and more convenient…​

However, some government and regulated industries have issues with SaaS delivered solutions and thus **must**  
use offline build. These organizations also require enterprise grade support for most cases and so it makes sense  
to bundle as an enterprise only solution.

#### Policy

Since offline builds will in effect be a component that is “installed locally” we think that the right thing to do is  
to treat this as “shrinkwrap software”. So once you download a version of the offline build tool from our servers  
this version will be there until you delete it.

It will not “dial home” or perform any such tests but it will be locked to your development machine.  
You will be allowed to keep using it based on the terms of the license. So you can keep shipping/building  
apps after canceling an enterprise subscription but you won’t be able to update to the latest Codename One  
version as it will include newer features.

#### Functionality

We currently only plan to support iOS & Android build targets for offline builds, the former will (naturally) require  
a Mac. We might add additional supported platforms based on user demand.

The process will not physically build the app. It will generate a native project for iOS/Android which you will need to  
open in xcode/Android Studio respectively and build manually.

#### Feedback

We are currently in the feedback gathering phase for this functionality so if this is something you are interested  
in and already have or plan to upgrade to enterprise we’d love to hear about that.

We won’t open this feature to other account tiers, we ran thru the costs/benefits and decided that this isn’t something  
we can justify. If you have a lower grade account you can use offline builds with the source code from github.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
