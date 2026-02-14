---
title: Cocoapods Support
slug: cocoapods
url: /blog/cocoapods/
original_url: https://www.codenameone.com/blog/cocoapods.html
aliases:
- /blog/cocoapods.html
date: '2016-05-28'
author: Steve Hannah
---

![Header Image](/blog/cocoapods/cocoapods.png)

[CocoaPods](https://cocoapods.org/) is a dependency manager for Swift and Objective-C Cocoa projects.  
It has over eighteen thousand libraries and can help you scale your projects elegantly. Cocoapods can be  
used in your Codename One project to include native iOS libraries without having to go through the hassle  
of bundling the actual library into your project. Rather than bundling .h and .a files in your ios/native directory,  
you can specify which “pods” your app uses via the `ios.pods` build hint. (There are other build hints also  
if you need more advanced features).

E.g.:

To include the [AFNetworking](https://github.com/AFNetworking/AFNetworking) library in your app use the build  
hint:
    
    
    ios.pods=AFNetworking

To include the [AFNetworking](https://github.com/AFNetworking/AFNetworking) version 3.0.x library in your app use  
the build hint:
    
    
    ios.pods=AFNetworking ~> 3.0

For full versioning syntax specifying pods see the  
[Podfile spec for the “pod” directive](https://guides.cocoapods.org/syntax/podfile.html#pod).

### Including Multiple Pods

Multiple pods can be separated by either commas or semi-colons in the value of the `ios.pods` build hint.  
E.g. To include GoogleMaps and AFNetworking, you could:
    
    
    ios.pods=GoogleMaps,AFNetworking

Or specifying versions:
    
    
    ios.pods=AFNetworking ~> 3.0,GoogleMaps

### Other Pod Related Build Hints

`ios.pods.platform` : The minimum platform to target. In some cases, Cocoapods require functionality that is  
not in older version of iOS. For example, the GoogleMaps pod requires iOS 7.0 or higher, so you would need to  
add the `ios.pods.platform=7.0` build hint.

`ios.pods.sources` : Some pods require that you specify a URL for the source of the pod spec. This may be  
optional if the spec is hosted in the central CocoaPods source (`<https://github.com/CocoaPods/Specs.git>`).

### Converting PodFile To Build Hints

Most documentation for Cocoapods “pods” provide instructions on what you need to add to your Xcode  
project’s PodFile. Here is an example from the GoogleMaps cocoapod to show you how a PodFile can be  
converted into equivalent build hints in a Codename One project.

The GoogleMaps cocoapod directs you to add the following to your PodFile:
    
    
    source 'https://github.com/CocoaPods/Specs.git'
    platform :ios, '7.0'
    pod 'GoogleMaps'

This would translate to the following build hints in your Codename One project:
    
    
    ios.pods.sources=https://github.com/CocoaPods/Specs.git
    ios.pods.platform=7.0
    ios.pods=GoogleMaps

__ |  Note that the `ios.pods.sources` directive is optional   
---|---
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chidiebere Okwudire** — January 24, 2017 at 2:14 am ([permalink](https://www.codenameone.com/blog/cocoapods.html#comment-23114))

> Chidiebere Okwudire says:
>
> This is really great! It’s made integrating the Parse iOS SDK *much* simpler. Thanks!!
>



### **3lix** — February 13, 2017 at 3:55 pm ([permalink](https://www.codenameone.com/blog/cocoapods.html#comment-24126))

> 3lix says:
>
> Would these pods work on all three platforms (iOS, Android and Windows) or are these iOS specific?
>



### **Shai Almog** — February 14, 2017 at 8:02 am ([permalink](https://www.codenameone.com/blog/cocoapods.html#comment-23239))

> Shai Almog says:
>
> No it’s designed for iOS native code.
>



### **Francesco Galgani** — October 17, 2018 at 9:40 pm ([permalink](https://www.codenameone.com/blog/cocoapods.html#comment-23942))

> Francesco Galgani says:
>
> Is there anything similar for Android?
>



### **Shai Almog** — October 18, 2018 at 4:04 am ([permalink](https://www.codenameone.com/blog/cocoapods.html#comment-23919))

> Shai Almog says:
>
> Sure, Gradle: [https://www.codenameone.com…](<https://www.codenameone.com/blog/tip-use-android-gradle-dependencies-native-code.html>)
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
