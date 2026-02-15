---
title: Video, New Defaults & BarCode/QRCode Changes
slug: video-new-defaults-barcode-qrcode-changes
url: /blog/video-new-defaults-barcode-qrcode-changes/
original_url: https://www.codenameone.com/blog/video-new-defaults-barcode-qrcode-changes.html
aliases:
- /blog/video-new-defaults-barcode-qrcode-changes.html
date: '2015-12-27'
author: Shai Almog
---

![Header Image](/blog/video-new-defaults-barcode-qrcode-changes/hqdefault.jpg)

We released a new version of the introducing Codename One video almost a month ago but we just neglected  
to highlight it in the blog. Our old videos are pretty dated by now and we use far better toolchains for video production  
today, so we are in the process of redoing all our old videos. This is a long and tedious process that we do  
while producing newer content, fixes and moving forward. So the timeline of such updates is quite volatile.  
Check out the new video below. 

#### Gradle & Keyboard Default Switch

We just flipped the switch on two major changes in the Android build. All Android builds from now on will  
default to gradle instead of Ant. This is far superior to Ant builds as its the way forward for Google and it  
allows you to [  
open the resulting project in Android studio](http://stackoverflow.com/questions/34430404/how-to-build-the-native-android-sources-from-codename-ones-build-server).   
We are flipping this switch to test this functionality before 3.3 rolls out. If this triggers issues for you please  
report it at once, you can also disable it manually using the build hint `android.gradle=false`. 

The new Android input mode is pretty different to the previous keyboard input mode, it should bring Android  
more in line with the iOS port and modernize the UI quite a bit. This might introduce some incompatibilities  
or issues, be sure to report them!   
You can manually disable this build hint using the `android.keyboardOpen=false` build hint. 

#### Barcode/QR Code Deprecation

When we originally launched the barcode/qrcode API’s we didn’t yet have cn1libs so we just added this to the  
API core. Now that cn1libs have been around for ages and also used to implement  
[another barcode/QR code scanner API](https://github.com/littlemonkeyltd/QRScanner)  
we think its time to deprecate the API and move it into a separate cn1lib here:  
<https://github.com/codenameone/cn1-codescan>. 

After 3.3 we will remove this API for good from the core and you will need to migrate to the cn1lib (which should  
be pretty trivial). The reasoning is actually rather simple besides slightly faster link times…  
This API requires a binary native library that has been a thorn in our side every time Apple changed something  
e.g. bitcode support, x64 etc. Every such change would have been seamless and instant were it not for this  
library.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **davidwaf** — December 29, 2015 at 7:59 pm ([permalink](/blog/video-new-defaults-barcode-qrcode-changes/#comment-22605))

> davidwaf says:
>
> Just an observation: with the switch to gradle, i notice when using native android libraries, you now only accept bundling them as aar and no longer andlib, and merging of android manifests is switched on by default. Which is good. I ran into a small build error because my native lib had declared permisions:
>
> <uses-permission android_name=”android.permission.INTERNET”/>  
> <uses-permission android_name=”android.permission.ACCESS_NETWORK_STATE”/>
>
> and merging fails. It fails on duplicate entries in resultant manifest file. It seems the CN1 build system also generates these permissions. Also i had to remove
>
> android_icon=”@drawable/ic_launcher”  
> android_label=”@string/app_name”
>
> attributes from application tag from my native library manifest, since they conflicted during merge (I assume the CN1 build system generates these too, so the merge fails).  
> Any way, these were small changes i had to make, after which my builds started working as usual again.


### **Shai Almog** — December 30, 2015 at 3:48 am ([permalink](/blog/video-new-defaults-barcode-qrcode-changes/#comment-22424))

> Shai Almog says:
>
> Thanks for the headsup! That’s exactly why we turned on the default now instead of waiting for 3.3.
>
> We wanted to iron out the kinks.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
