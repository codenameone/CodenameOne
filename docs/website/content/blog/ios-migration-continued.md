---
title: iOS Migration Continued
slug: ios-migration-continued
url: /blog/ios-migration-continued/
original_url: https://www.codenameone.com/blog/ios-migration-continued.html
aliases:
- /blog/ios-migration-continued.html
date: '2016-06-08'
author: Shai Almog
---

![Header Image](/blog/ios-migration-continued/xcode-migration.jpg)

We announced our [plans to migrate to the newest version of xcode](/blog/ios-server-migration-plan.html)  
recently and so far these plans have gone rather well with most tests passing without a problem. We did decide  
to disable bitcode by default which means the new build hint `ios.bitcode` will now default to `false` to avoid issues  
with some libraries that are still not up to date.

One point of confusion from that update was whether `iphone_new` should still be used. The answer is **NO**.

You should use the `iphone` build target as before and we will switch the servers seamlessly.

We will perform the migration to the new servers over the course of the next 2 months as we prepare for 3.5.  
Since 3.5 is slated for early August we’d like to have the new servers in place by then…​

### Switch Transitory Period

During the switch transitory period we will allow building to a special `iphone_old` target to workaround potential  
server regressions. If you built to the `iphone_new` this is the same thing and you should skip ahead if not then  
read this for instructions on how to temporarily workaround regressions.

__ |  You **MUST** report all regressions to us at once!  
If you don’t file issues we will not know a regression exists and might continue the migration plan assuming everything  
works.   
---|---  
  
Open the `build.xml` file & search for the string `"iphone"` **with the quotes**.  
Replace it with `"iphone_old"` .

E.g. notice the `targetType="iphone_old"` line below:
    
    
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
    
            targetType="iphone_old"
            certificate="${codename1.ios.debug.certificate}"
            certPassword="${codename1.ios.debug.certificatePassword}"
            provisioningProfile="${codename1.ios.debug.provision}"
            appid="${codename1.ios.appid}"
            />
    </target>

### Trial Switch

On Sunday June 19th we will switch to the new build servers so all standard `iphone` builds will reach the new  
servers. We will also setup the old servers as iphone_old servers.

On that day some builds might get “lost” in the ether of migration so if a build gets stuck cancel it…​

This will be a trial run, if everything will “just work” which is a possibility (however unlikely) this will be it.

However, if issues are reported we will probably revert back to the old servers.

### Complete Switch

Assuming the trial teaches us about some issues we might do another trial but our final switch target date is  
July 3rd. Assuming things mostly work our correctly we aim to bring down some of our build server capacity for  
OS upgrade in preparation of the full switch.

This will mark the full switch to the new version of xcode.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
