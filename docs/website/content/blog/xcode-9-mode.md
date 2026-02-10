---
title: Xcode 9.2 Mode
slug: xcode-9-mode
url: /blog/xcode-9-mode/
original_url: https://www.codenameone.com/blog/xcode-9-mode.html
aliases:
- /blog/xcode-9-mode.html
date: '2018-01-31'
author: Shai Almog
---

![Header Image](/blog/xcode-9-mode/xcode-migration.jpg)

Every time we switch a version of xcode in the build servers things go haywire because of minor behavioral changes from Apple. Over the holidays we started a long and painful migration to xcode 9.2 which required an update to the Mac OS versions on our servers. Thankfully this wasn’t as bad as the old xcode 5 to 7.3 migration where the old build code literally stopped working…​

This makes things FAR easier, but the migration itself still didn’t work on first try due to changes in the signing process. Over the past week we were able to address those final issues and xcode 9.2 builds are now functional!

__ |  Building for xcode 9.2 is still experimental at this time so be sure to test and submit issues!   
---|---  
  
### Toggling the Xcode Versions

You can toggle the xcode 9.2 build by using the build hint:

`ios.xcode_version=9.2`

The default currently maps to 7.3 but at some point in the future we will probably flip the default. If at that point you would want to try the old version you could use the reverse:

`ios.xcode_version=7.3`

Notice that we will make a clear announcement about flipping the default xcode version.

### Things to Notice

Currently the splash screen seems problematic. We are working on resolving that.

Push and local notifications might experience issues when running on xcode 9.2 as there were some changes related to those API’s.

#### Permissions

iOS has tightened the requirements around API access permissions and now expects description entries for every problematic API. E.g. if your app uses the camera, Apple expects you to include a description explaining why you need camera access.

The same holds true for other API’s.

When our simulator encounters usage of those API’s it automatically adds the appropriate build hint containing default description text. This will help you get a build through but you might need to customize that text before submission to Apple.

Common permission descriptions you will need are: `ios.NSCameraUsageDescription`, `ios.NSContactsUsageDescription`, `ios.NSLocationAlwaysUsageDescription`, `NSLocationUsageDescription`, `ios.NSMicrophoneUsageDescription`, `ios.NSPhotoLibraryAddUsageDescription`, `ios.NSSpeechRecognitionUsageDescription`, `ios.NSSiriUsageDescription`.

You will notice they all follow the convention `ios.NSXXXUsageDescription`. The XXX portion maps to the way Apple represents this permission in the plist file. You can see the full list from Apple [here](https://developer.apple.com/library/content/documentation/General/Reference/InfoPlistKeyReference/Articles/CocoaKeys.html).

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
