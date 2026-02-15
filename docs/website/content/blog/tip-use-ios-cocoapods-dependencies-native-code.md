---
title: 'TIP: Use iOS CocoaPods Dependencies in Native Code'
slug: tip-use-ios-cocoapods-dependencies-native-code
url: /blog/tip-use-ios-cocoapods-dependencies-native-code/
original_url: https://www.codenameone.com/blog/tip-use-ios-cocoapods-dependencies-native-code.html
aliases:
- /blog/tip-use-ios-cocoapods-dependencies-native-code.html
date: '2017-02-12'
author: Shai Almog
---

![Header Image](/blog/tip-use-ios-cocoapods-dependencies-native-code/tip.jpg)

Last week I talked about [using gradle dependencies](/blog/tip-use-android-gradle-dependencies-native-code.html) to build native code, this week I’ll talk about the iOS equivalent: CocoaPods. We’ve [discussed CocoaPods before](/blog/cocoapods.html) but this bares repeating especially in the context of a specific cn1lib like [intercom](/blog/intercom-support.html).

CocoaPods allow us to add a native library dependency to iOS far more easily than Gradle. However, I did run into a caveat with target OS versioning. By default we target iOS 7.0 or newer which is supported by Intercom only for older versions of the library. Annoyingly CocoaPods seemed to work, to solve this we had to explicitly define the build hint `ios.pods.platform=8.0` to force iOS 8 or newer.

Including intercom itself required a single build hint: `ios.pods=Intercom` which you can obviously extend by using commas to include multiple libraries. You can search the [cocoapods website](https://cocoapods.org/) for supported 3rd party libraries which includes everything you would expect. One important advantage when working with CocoaPods is the faster build time as the upload to the Codename One website is smaller and the bandwidth we have to CocoaPods is faster. Another advantage is the ability to keep up with the latest developments from the library suppliers.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **3lix** — February 13, 2017 at 5:02 pm ([permalink](/blog/tip-use-ios-cocoapods-dependencies-native-code/#comment-23342))

> 3lix says:
>
> I am not familiar with cocoapods.  
> I have two questions:  
> 1) When adding a a cocoapod library would it work on all three platforms (iOS , Android and Windows? )  
> 2) In your previous post you had mentioned that there are over 18000 libraries. [https://www.codenameone.com…](</blog/cocoapods/>) Do they need to be converted to CN1LIBS before use (some sort of a JAVA api) ?
>



### **Shai Almog** — February 14, 2017 at 8:02 am ([permalink](/blog/tip-use-ios-cocoapods-dependencies-native-code/#comment-23351))

> Shai Almog says:
>
> CocoaPods are used with native code similarly to the Android gradle build options being used for native code. This is important since libraries for iOS can sometimes be pretty huge (100mb+) thus become impractical for build processes.
>
> You need to build a cn1lib to wrap every library that’s required, e.g. Native Google Maps support transitioned to using CocoaPods instead of just bundling the maps. This was a HUGE benefit as it allowed us to keep up with the latest version from Google with no effort. But it also made the build MUCH faster as the original build size approached 50mb and would take forever to upload each time, not it can even be used by free accounts as it clocks under the free quota size.
>



### **3lix** — February 14, 2017 at 4:13 pm ([permalink](/blog/tip-use-ios-cocoapods-dependencies-native-code/#comment-22856))

> 3lix says:
>
> Thank you for the explanation. This makes sense now
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
