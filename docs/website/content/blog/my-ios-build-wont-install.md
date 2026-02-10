---
title: My iOS Build Won't Install
slug: my-ios-build-wont-install
url: /blog/my-ios-build-wont-install/
original_url: https://www.codenameone.com/blog/my-ios-build-wont-install.html
aliases:
- /blog/my-ios-build-wont-install.html
date: '2017-05-16'
author: Shai Almog
---

![Header Image](/blog/my-ios-build-wont-install/generic-java-1.jpg)

Recently I sent a build and had an issue installing it. It was late at night and I just forgot to add the build hint `ios.debug.archs=armv7` for installation on the 3rd gen iPad I was testing with. So we can all trip over basic mistakes when it comes to iOS installs. So for your convenience I made a list of common pitfalls you might run into if your iOS build won’t install on your device.

If you have access to a Mac you can connect a cable and open xcode where you can use the device explorer console to look at messages which sometimes give a clue about what went wrong. If not here is a laundry list of a few things that might fail:

  * Make sure you built the debug version and not the appstore version. The appstore version won’t install on the device and can only be distributed via Apple’s store or testflight

  * Verify that you are sending a 32 bit build in the build hints using the build hint `ios.debug.archs=armv7`. It’s only necessary if you have an older 32 bit device, see [this](https://www.codenameone.com/blog/moving-to-64bit-by-default.html). Notice that this only applies for debug builds, release builds include both 32 and 64 bit versions

__ |  Devices prior to iPad Air & iPhone 5s were 32 bit devices so iPhone 5s won’t need that flag but iPhone 5 or iPhone 5c will need it   
---|---  
  
  * Check the the UDID is correct – if you got the UDID from an app then it’s probably wrong as apps don’t have access to the device UDID anymore. The way to get the UDID is either thru iOS Settings app or itunes

  * Make sure the device isn’t locked for installing 3rd party apps. I’ve had this when trying to install on my kids tablet which I configured to be child safe. This is configured in the settings as parental controls

  * Check that you “own” the package name. E.g. if you previously installed an app with the same package name but a different certificate a new install will fail (this is true for Android too). So if you installed the kitchen sink from the store then built one of your own and installed it there will be a collision.  
Notice that this might be problematic if you use overly generic package names as someone else might have used them which is why you must always use your own domain

  * Make sure the device has a modern enough version of iOS for the dependencies. I think the current minimum for hello world is 6.0.1 but some apps might require a newer version e.g. Intercom requires OS 8 or newer

  * Verify that you are using Safari when installing on the device (if you tried via cable that’s not a problem), some developers had issues with firefox not launching the install process

  * Check that the build hint `ios.includePush` is set in a way that matches your iOS provisioning. So it must be false if you don’t have push within the provisioning profile

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
