---
title: Different Icons
slug: different-icons
url: /blog/different-icons/
original_url: https://www.codenameone.com/blog/different-icons.html
aliases:
- /blog/different-icons.html
date: '2016-09-11'
author: Shai Almog
---

![Header Image](/blog/different-icons/kitchensink-2-flat.jpg)

When we designed the icon for the new Kitchen Sink demo we tried to use material design principals. We thought it  
would look reasonable on iOS but it looked awful. So we decided to adapt the design and create a separate  
yet similar icon for iOS.

This is actually quite common but many developers aren’t aware of how easy it is to do these sort of things. The  
build process for Codename One is mostly transparent and you can replace/customize pieces of it. In this  
case we edited the `build.xml` file and modified this build target:
    
    
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
    
            targetType="iphone"
            certificate="${codename1.ios.debug.certificate}"
            certPassword="${codename1.ios.debug.certificatePassword}"
            provisioningProfile="${codename1.ios.debug.provision}"
            appid="${codename1.ios.appid}"
            automated="${automated}"
            />
    </target>

We changed one line:
    
    
            icon="iosicon.png"

We also had to do the same change in the release target:
    
    
    <target name="build-for-ios-device-release" depends="clean,copy-ios-override,copy-libs,jar,clean-override">

This is really convenient as it allows you to override and automate many stages of the build. Every device build  
has a target and you can modify things such as which files get packaged.

### XML Updates

The pitfall with this technique is that XML files are updated occasionally. The Codename One Settings application  
or the IDE preferences will detect out of date `build.xml` files and offer to update them, this might be required to  
take advantage of newer features such as offline build…​

This update will eliminate your changes so you will need to reintegrate them. If you have many changes just move them  
to an external file that you can include in the right spot so re-integrating changes is easy. Also make sure to keep  
the build.xml in version control so an accidental overwrite won’t destroy your work.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **bryan** — September 16, 2016 at 5:29 am ([permalink](https://www.codenameone.com/blog/different-icons.html#comment-22726))

> bryan says:
>
> Re Kitchen Sink – I’ve been looking at the source to see how some of the stuff in the demo is done, and it would be really good if there were more comments or description in the code (or accompanying the demo) so developers could copy some of the techniques used. There’s a bunch of stuff obviously possible with CN1, but much of it is hidden away and not always obvious that what’s possible.
>



### **Shai Almog** — September 16, 2016 at 9:41 am ([permalink](https://www.codenameone.com/blog/different-icons.html#comment-23091))

> Shai Almog says:
>
> Here is a neat trick you can do:  
> Press the edit icon on the top right and just insert your comments or even “can you explain this?” comments. Once done just click the pull request button.
>
> I will merge the pull request and then fix the “explain this” comments… You don’t even need to check out the project.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
