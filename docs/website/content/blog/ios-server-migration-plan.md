---
title: iOS Server Migration Plan
slug: ios-server-migration-plan
url: /blog/ios-server-migration-plan/
original_url: https://www.codenameone.com/blog/ios-server-migration-plan.html
aliases:
- /blog/ios-server-migration-plan.html
date: '2016-05-24'
author: Shai Almog
---

![Header Image](/blog/ios-server-migration-plan/xcode-migration.jpg)

We were stuck on an “old” version of xcode in the build servers. This hasn’t been a big deal for most features  
but in some cases we are running into issues e.g. in using the full capabilities of the new iPad or 3d touch. The reason  
for this is Apples backwards compatibility policy.

Apple allows you to run an older version of xcode, but the newer versions of xcode always require the latest  
version of Mac OS X. The problem here is that the latest version of Mac OS X doesn’t support older versions  
of xcode so if we upgrade we won’t be able to support older versions of the build…​

Normally this shouldn’t be a problem, we already run fine on the latest version of xcode without a problem. However,  
you might inadvertently have relied on some behaviors or functionality of the old xcode e.g. thru native code,  
third party cn1lib or slight behavior variation.

Unfortunately, this means that once we upgrade there is no way to turn back. We’ll need to update the OS’s of  
the build servers and do this consistently so you will get consistent build results. This would also mean that some  
aspects of versioned builds will not work as smoothly as the new iOS build servers won’t have the older version  
of xcode in place.

### The Migration Plan

We’ve setup a new build server as a “test pilot” to see that builds go thru as planned. You can/should test your  
app to see if it will be affected by the migration, please let us know immediately if there are issues!

__ |  If you don’t do this we will not be able to go back!   
---|---  
  
To test your app on the test pilot build server open the build.xml and search for the string `"iphone"` **with the quotes**.  
Replace it with `"iphone_new"` .

E.g. notice the `targetType="iphone_new"` line below:
    
    
    <target name="build-for-ios-device" depends="clean,copy-ios-override,copy-libs,jar,clean-override">
        <codeNameOne
            jarFile="${dist.jar}"
            displayName="${codename1.displayName}"
            packageName = "${codename1.packageName}"
            mainClassName = "${codename1.mainName}"
            version="${codename1.version}"
            icon="${codename1.icon}"
            vendor="${codename1.vendor}"
            subtitle="${codename1.secondaryTitle}"
    
            targetType="iphone_new"
            certificate="${codename1.ios.debug.certificate}"
            certPassword="${codename1.ios.debug.certificatePassword}"
            provisioningProfile="${codename1.ios.debug.provision}"
            appid="${codename1.ios.appid}"
            />
    </target>

Assuming all goes well we will flip the switch and update some of the build servers to the latest OS. We’ll keep  
one build server around with the legacy OS and change it so you will need to explicitly send a build to it…​

This effectively means you will need to revert this `build.xml` change to keep building as the new servers will  
then use the “iphone” target. To build for the old build target you will need to do the inverse of the current change  
by sending a build to the `"iphone_old"` target.

We’ll support that for a while until we are convinced that the migration went smoothly at which point we will retire  
the server.

From past experience these things always generate issues for some developers that can sometimes take a while  
to resolve. That’s why it’s important to keep a legacy build server around. Unfortunately our experience here  
also taught us that once we provide a workaround people use that and don’t test the new update.

The nice thing about this switch is that most developers won’t really feel it. Your builds will use newer  
iOS capabilities without a single change made by you.

#### Bitcode

Probably the biggest issue with the migration is bitcode support which was introduced a while back. To understand  
bitcode you need to understand a bit of the background…​

iOS applications are really Mac OS app bundles. The natively compiled binary can contain more than one processor  
architecture and usually does. So by default when you build an iOS appstore build you are really building a 32 bit  
and 64 bit fat binary. Assuming you want to support the (Apple native) simulator too you will also need x86 binaries  
and this is just a partial list…​

When we look into more esoteric devices like the Apple Watch or TV things become more complicated and would  
obviously become worse as a result.

This is the problem bitcode aims to solve. Since Apple already uses LLVM which has an intermediate representation  
after compilation they might as well use that instead of the native OS executables. This would allow Apples servers  
to natively compile the app to any future processor platform without you changing your code…​ Effectively this is  
very much like a form of bytecode for Apples benefit.

Bitcode is required for Apple Watch apps and might become required for all apps in the future so it’s a good idea  
to leave it on.

The main challenge with bitcode is 3rd party libraries, if you use any of those they need to be compiled with bitcode  
support enabled otherwise the compiler will fail. Many libraries (especially older ones) don’t have bitcode support  
so you might need to update native code to include that functionality.

We intend to offer a flag to disable bitcode before we go into production with this version, however right now it’s  
not in the current version.

### Zlib QR/Barcode Support

A while back we announced that we are removing the QR/Bar code support from Codename One and moving  
it to an external library. We deprecated the API’s and starting from recent builds you will no longer be able to  
use the builtin API.

Zlib uses a native library that doesn’t support the new bitcode architecture expected in iOS. So once we removed  
it Codename One compiled without a problem with bitcode support turned on.

If you need support for QR/bar code scanning you need to migrate your code to use the new  
[cn1-codescan](https://github.com/codenameone/cn1-codescan/) library instead.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Shai Almog** — May 25, 2016 at 5:54 pm ([permalink](https://www.codenameone.com/blog/ios-server-migration-plan.html#comment-22598))

> Shai Almog says:
>
> We just added ios.bitcode=false as an option to disable bitcode. If you are running into issues please try that build hint and let us know either way.
>



### **Gareth Murfin** — May 27, 2016 at 12:06 am ([permalink](https://www.codenameone.com/blog/ios-server-migration-plan.html#comment-21511))

> Gareth Murfin says:
>
> Will try this out Shai and let you know the results. Cheers, Gaz.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
