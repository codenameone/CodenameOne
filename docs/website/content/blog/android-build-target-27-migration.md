---
title: Android Build Target 27 Migration
slug: android-build-target-27-migration
url: /blog/android-build-target-27-migration/
original_url: https://www.codenameone.com/blog/android-build-target-27-migration.html
aliases:
- /blog/android-build-target-27-migration.html
date: '2018-03-27'
author: Shai Almog
---

![Header Image](/blog/android-build-target-27-migration/android_studio.jpg)

A while back [Google announced](https://android-developers.googleblog.com/2017/12/improving-app-security-and-performance.html) that starting in August 2018 they will no longer accept applications targeting API levels below 26. With that in mind we plan to migrate our builds to use API level 27 which brings with it a lot of great new features but will probably break some things as we go through the migration. Please read this post carefully, I’ll try to cover everything.

Notice that this announcement means that we will need to start updating the API levels every year which is a much faster pace.

I’ve constructed this post as a set of questions/answers.

### What’s an API Level?

Every time Google releases a new version of Android it updates the API level e.g. currently Oreo (8.1) is API level 27.

When we build a native Android application we need to declare the “target” this means we compiled the project against this given API level. This is a double edged sword…​ When we pick a higher API level we can target new features of newer OS’s but we are also subject to new restrictions and changes.

E.g. when we migrated to API level 23 we had to change the way permissions are processed in applications. For Codename One code this was mostly seamless but if you relied on native code this sometimes triggered issues.

API level 27 can impact things such as background behavior of your application and can break some cn1libs/native code you might have in place.

FYI this is also explained in this [article](https://arstechnica.com/gadgets/2017/12/google-fights-fragmentation-new-android-features-to-be-forced-on-apps-in-2018/) from Ars.

### Will this Work for Older Devices?

The target API level doesn’t restrict older devices. For that we have a separate minimum target device and it indicates the lowest API level we support. Currently the default is 15 (Android 4.0.3 – Ice Cream Sandwich) but you can probably set it as low as 9 (Android 2.3 – Gingerbread) as long as you test the functionality properly and disable Google Play Services.

See [this](https://developer.android.com/guide/topics/manifest/uses-sdk-element.html) for a table of all the API levels from Google.

### What API Level do we use Now?

That depends on your app. Our default is 23 but some cn1libs set the API level to 25.

We chose to migrate slowly as level 23 is generally good and stable.

### Test This Now!

**Test API level 27 right now before we flip the switch!**

This is important as we want to iron out bugs/regressions before they impact everyone. You can enable this seamlessly by setting the build hint: `android.buildToolsVersion=27`

__ |  Remove this build hint after testing, otherwise when we migrate to a newer version later on it might fail!   
---|---  
  
This will implicitly set a lot of other values including the target level and it will change the gradle version from 2.12 to 4.6.

#### Other Benefits.

By flipping this switch the build should now work on Android Studio 3.x out of the box without the changes listed in [this tip](/blog/tip-include-source-android-studio-3.html). We also plan to enable other things in the resulting project such as using Googles builtin Java 8 support instead of ours (this isn’t enabled yet).

This will mean that native Android code would be able to use Java 8 features. Notice that this currently applies to the native interfaces only and not to the code in the Codename One implementation.

This should make it easier to work with some 3rd party libraries that already moved forward.

### When Will this Happen?

The 27 build target is available now using the build hint: `android.buildToolsVersion=27`.

Currently we are aiming to flip the switch by May. This might be pushed to a different date based on responses/feedback on the current status. We want to have enough time ahead so the July release of Codename One 5.0 (Social) will use this.

### Is this a Good Thing?

I think it is. It prevents stagnation within the appstore.

I still see apps in the stores that target Gingerbread (API level 9). That’s a problem both visually & in terms of permissions/security.

I don’t think it will do enough to combat fragmentation. Google will need to change a lot more to fix that pickle but it’s a baby step in the right direction.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — April 6, 2018 at 9:56 am ([permalink](/blog/android-build-target-27-migration/#comment-23555))

> Francesco Galgani says:
>
> I’ve done some tests with “android.buildToolsVersion=27” in Android 7 and Android 5 devices and I didn’t notice any difference 🙂  
> However, I don’t understand why the target API level doesn’t restrict older devices: if the API 27 is for Android 8.1, how is it possible that the older devices are supported? Is it possible because Codename One build servers don’t generate code that is supported only by recent devices?
>



### **Shai Almog** — April 7, 2018 at 4:42 am ([permalink](/blog/android-build-target-27-migration/#comment-23648))

> Shai Almog says:
>
> Good to hear.
>
> This is a feature from native Android not us. There are 2 different values: minimum API level and Target API level.
>
> When we use a feature from API 27 we check if the device supports API 27 first then call it. This means it will still work on an API 15 device without a problem.
>
> This is a “statement” from the app to the runtime environment saying we tested on an Android 8 device. That means that Google will enable small incompatible changes to Android once this is set. A good example is background behavior which will now be more aggressive.
>
> E.g. In API 23 (Andoid 6) Google introduced a new permission system.
>
> So if you had an Android 5 device and installed an app compiled with target 23 it would still work and show the permission prompt during install.
>
> If you have an Android 6 device and installed an old app (target smaller than 23) it would behave like an Android 5 device and show permissions during install.
>
> However, if both the app and the device are API 23 or newer the app would install instantly and prompt for permissions in runtime.
>



### **Denis** — May 12, 2018 at 8:45 am ([permalink](/blog/android-build-target-27-migration/#comment-23780))

> Denis says:
>
> Hi Shai,
>
> I have put android.buildToolsVersion=27 in build hints, however when I upload apk to Play Console, it warns that app still targets API 23, could you please take a look at screenshots and advise ?
>
> Thanks,  
> Denis
>
> [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/62af0edf5159ebad3d8a50e563768c190fbbffb7dcc7eafa527ca98370ecbed8.jpg>) [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/db0fa8391e530173e0dc310fc6dc110239bb8821342c652ac9907b326b25f9a9.jpg>)
>



### **Shai Almog** — May 13, 2018 at 4:25 am ([permalink](/blog/android-build-target-27-migration/#comment-23692))

> Shai Almog says:
>
> Hi,  
> what do you have within your [codenameone_settings.proper…](<http://codenameone_settings.properties>?) It looks like this isn’t passing through.
>



### **Denis** — May 13, 2018 at 6:38 am ([permalink](/blog/android-build-target-27-migration/#comment-23725))

> Denis says:
>
> Hi,  
> codename1.arg.android.buildToolsVersion=27 is there, so it looks correct as I understand, please confirm  
> Thanks
>



### **Shai Almog** — May 14, 2018 at 4:25 am ([permalink](/blog/android-build-target-27-migration/#comment-21475))

> Shai Almog says:
>
> Hi,  
> yes. But other build hints might collide with this functionality so are there other android.* build hints in the file?
>



### **Denis** — May 14, 2018 at 7:24 am ([permalink](/blog/android-build-target-27-migration/#comment-23556))

> Denis says:
>
> Hi Shai,
>
> yes, that makes, I don’t know why I haven’t post entire file right away ))  
> here it is, take a look please
>
> [android.playService.ads](<http://android.playService.ads>)=true  
> codename1.android.keystore=XXXXXXXXXXXXXXXXXXXXX  
> codename1.android.keystoreAlias=XXXXXXXXXXXXXXXX  
> codename1.android.keystorePassword=XXXXXXXXXXXXX  
> codename1.arg.android.buildToolsVersion=27  
> codename1.arg.android.debug=false  
> codename1.arg.android.licenseKey=XXXXXXXXXXXXXXX  
> codename1.arg.android.release=true  
> codename1.arg.android.statusbar_hidden=true  
> codename1.arg.android.xapplication=<meta-data android:name=”com.google.android.gms.version” android:value=”@integer/google_play_services_version”/><activity android:name=”com.google.android.gms.ads.AdActivity” android:configchanges=”keyboard|keyboardHidden|orientation|screenLayout|uiMode|screenSize|smallestScreenSize”/>  
> codename1.arg.ios.add_libs=AdSupport.framework;SystemConfiguration.framework;CoreTelephony.framework  
> codename1.arg.ios.newStorageLocation=true  
> codename1.arg.ios.objC=true  
> codename1.arg.ios.pods=,Firebase/Core,Firebase/AdMob  
> codename1.arg.ios.pods.platform=,7.0  
> codename1.arg.ios.pods.sources=,<https://[github.com/CocoaPods/Specs.git%5D(http://github.com/CocoaPods/Specs.git)>  
> codename1.arg.ios.statusbar_hidden=true  
> codename1.arg.java.version=8  
> codename1.displayName=XXXXXXXXXXXXXXXXXXXXXXXXXX  
> codename1.icon=icon.png  
> codename1.ios.certificate=  
> codename1.ios.certificatePassword=  
> codename1.ios.provision=  
> codename1.j2me.nativeTheme=nativej2me.res  
> codename1.languageLevel=5  
> codename1.mainName=XXXXXXXXXXXXXXXXXXXXXXXXXXXXX  
> codename1.packageName=com.manyukhin.XXXXXXXXXXXX  
> codename1.rim.certificatePassword=  
> codename1.rim.signtoolCsk=  
> codename1.rim.signtoolDb=  
> codename1.secondaryTitle=XXXXXXXXXXXXXXXXXXXXXXX  
> codename1.vendor=Denis Manyukhin  
> codename1.version=1.11
>



### **Shai Almog** — May 15, 2018 at 4:08 am ([permalink](/blog/android-build-target-27-migration/#comment-23790))

> Shai Almog says:
>
> Thanks,  
> it looks like you are missing codename1.arg. before the [android.playService.ads](<http://android.playService.ads>) but that’s unrelated.
>
> Looking again at the code I think you might need to explicitly specify android.sdkVersion=27 for this to work.
>



### **Denis** — May 15, 2018 at 4:25 am ([permalink](/blog/android-build-target-27-migration/#comment-23960))

> Denis says:
>
> Thanks Shai,  
> Soo, I should put sdkVersion build hint in [codenameone_settings.proper…](<http://codenameone_settings.properties>), right ?
>
> also it’s better to move [android.playService.ads](<http://android.playService.ads>) below Android build hints, is that correct ?
>



### **Shai Almog** — May 15, 2018 at 4:48 am ([permalink](/blog/android-build-target-27-migration/#comment-21644))

> Shai Almog says:
>
> If you edit [codenameone_settings.proper…](<http://codenameone_settings.properties>) you need to prefix it with codename1.arg. I suggest using the Codename One Setting UI under “Build Hints” to edit these and not edit the file directly.
>



### **Denis** — May 15, 2018 at 6:44 am ([permalink](/blog/android-build-target-27-migration/#comment-23699))

> Denis says:
>
> I have added android.sdkVersion=27 to Build hints via corresponding UI, it appears as codename1.arg.android.sdkVersion=27 in [codenameone_settings.proper…](<http://codenameone_settings.properties>),  
> but still the same warning in Google Play Console, app targeted to API 23, any ideas ?
>
> also I can’t see “[android.playService.ads](<http://android.playService.ads>)=true” in Build Hints UI, it only appears in [codenameone_settings.proper…](<http://codenameone_settings.properties>), is it ok ?
>



### **Denis** — May 15, 2018 at 9:20 pm ([permalink](/blog/android-build-target-27-migration/#comment-23716))

> Denis says:
>
> have you meant android.targetSDKVersion build hint ?  
> if not, may be it worth to set android.targetSDKVersion value explicitly ?
>



### **Shai Almog** — May 16, 2018 at 5:56 am ([permalink](/blog/android-build-target-27-migration/#comment-23767))

> Shai Almog says:
>
> Does targetSDKVersion solve this issue?  
> There is a bit of a mess of build hints here, we should clean it up a bit and ideally expose them in the Android section of Codename One Settings.
>
> Only things with the codename1.arg. prefix will appear in the build hints UI so that flag is effectively ignored.
>



### **Denis** — May 16, 2018 at 6:16 am ([permalink](/blog/android-build-target-27-migration/#comment-23693))

> Denis says:
>
> yes, android.targetSDKVersion solved the issue, no target API warnings, wondering if I shall keep android.sdkVersion and android.buildToolsVersion records in build hints
>



### **Shai Almog** — May 17, 2018 at 11:05 am ([permalink](/blog/android-build-target-27-migration/#comment-23931))

> Shai Almog says:
>
> We’ll switch all of these to default to 27 probably next weekend. I want to give this enough time before we release 5.0 in July.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
