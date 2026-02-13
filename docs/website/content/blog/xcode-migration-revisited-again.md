---
title: Xcode Migration Revisited Again
slug: xcode-migration-revisited-again
url: /blog/xcode-migration-revisited-again/
original_url: https://www.codenameone.com/blog/xcode-migration-revisited-again.html
aliases:
- /blog/xcode-migration-revisited-again.html
date: '2016-08-16'
author: Shai Almog
---

![Header Image](/blog/xcode-migration-revisited-again/xcode-migration.jpg)

We tried migrating to the new iOS build servers before and decided to revert the change due to xcode crashes. After [a lot of work](/blog/xcode-migration-take-2.html) we think the new server code is ready for production. We will try to migrate to these new servers again on the 28th of August (Sunday) to minimize impact if something goes horribly wrong.

If you read the [old post](/blog/ios-server-migration-plan.html) covering the `iphone_new` build target you can skip the rest of the post as it’s pretty much the same.

Apple allows you to run an older version of xcode, but the newer versions of xcode always require the latest version of Mac OS X. The problem here is that the latest version of Mac OS X doesn’t support older versions of xcode so if we upgrade we won’t be able to support older versions of the build…​

This shouldn’t be a problem, we already run fine on the latest version of xcode without a problem. However, you might inadvertently have relied on some behaviors or functionality of the old xcode e.g. thru native code, third party cn1lib or slight behavior variation.

This means that once we upgrade there is no way to turn back. We’ll need to update the OS’s of the build servers and do this consistently so you will get consistent build results. This would also mean that some aspects of versioned builds might not handle all cases as the new iOS build servers won’t have the older version  
of xcode in place.

### The Migration Plan

We’ve setup a new build server as a “test pilot” to see that builds go thru as planned. You can/should test your app to see if it will work, please let us know if there are issues!

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

Assuming all goes well we will flip the switch and update some of the build servers to the latest OS. We’ll keep one build server around with the legacy OS and change it so you will need to explicitly send a build to it…​

This effectively means you will need to revert this `build.xml` change to keep building as the new servers will then use the “iphone” target. To build for the old build target you will need to do the inverse of the current change by sending a build to the `"iphone_old"` target.

We’ll support that for a while until we decide that the migration works at which point we will retire the old servers.

We already tried this migration once and decided to [walk it back](/blog/ios-migration-setback.html) as part of our commitment to stability and seamless migration.

The nice thing about this switch is that most developers won’t feel it. Your builds will use newer iOS capabilities without a single change made by you.

#### Bitcode

Probably the biggest issue with the migration is bitcode support which Apple introduced a while back. To understand bitcode you need to understand a bit of the background…​

iOS applications are physically Mac OS app bundles. The natively compiled binary can contain more than one processor architecture and does under normal circumstances. By default when you build an iOS appstore build you are building a 32 bit and 64 bit fat binary. Assuming you want to support the (Apple native) simulator too you will also need x86 binaries (this is a partial list).

When we look into more esoteric devices like the Apple Watch or TV the story becomes more complicated and would become worse as a result.

This is the problem bitcode aims to solve. Since Apple already uses LLVM which has an intermediate representation after compilation they might as well use that instead of the native OS executables. This would allow Apples servers to natively compile the app to any future processor platform without you changing your code…​ Effectively this is like a form of bytecode for Apples benefit.

Apple requires bitcode for Apple Watch apps and might require it for all apps in the future so it’s a good idea to turn it on. You can turn it on by setting the `ios.bitcode=true` build hint.

The main challenge with bitcode is 3rd party libraries, if you use any of such libraries they need bitcode support enabled otherwise the compiler will fail. Older libraries don’t have bitcode support so you might need to update native code to include that functionality.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
