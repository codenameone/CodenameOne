---
title: Dark Mode
slug: dark-mode
url: /blog/dark-mode/
original_url: https://www.codenameone.com/blog/dark-mode.html
aliases:
- /blog/dark-mode.html
date: '2020-06-25'
author: Shai Almog
---

![Header Image](/blog/dark-mode/dark-mode.jpg)

We recently added support to detect whether a device is running in dark/light mode based on [this issue](https://github.com/codenameone/CodenameOne/issues/2979). Some of the code in the implementation is also derived from that issue submitted by [Javier](https://github.com/javieranton-zz).

### Detecting Dark Mode

Dark mode can be detected using APIs in the [CN](/javadoc/com/codename1/ui/CN/) and [Display](/javadoc/com/codename1/ui/Display/) classes. Specifically `isDarkMode()` and `setDarkMode(Boolean)`.

Notice that `isDarkMode()` returns `Boolean` and not `boolean`. This means that `null` is a valid value for this method. The case of `null` indicates that dark mode detection isn’t available or isn’t working on this platform.

You can override the dark mode setting for the platform using `setDarkMode()`.

At this time dark mode detection only works on iOS, Android and JavaScript. We tried adding desktop support for that, but it proved a bit challenging. UWP detection isn’t supported at the moment.

We don’t currently have an event based API for dark mode detection. While nice, this isn’t universally supported and can be circumvented with a simple timer.

### Native/Builtin Theme Support

Ideally, the app would just switch to dark mode seamlessly but right now this isn’t the case. The theme constant `darkModeBool` changes some deep core theme styles to match dark mode if `isDarkMode()` is true. In the future it might trigger a dark version of the native theme. At the moment we don’t have dark versions of the themes.

To create a dark version of your app create a new resource file for dark mode and load it conditionally based on dark mode. You can do the same for CSS by creating a CSS file called `dark.css` and editing your `build.xml` file to replicate the CSS conversion line. Specifically:
    
    
    <target name="-cn1-compile-css" if="codename1.cssTheme">
        <java jar="${user.home}/.codenameone/designer_1.jar" fork="true" failonerror="true">
            <jvmarg value="-Dcli=true"/>
            <arg value="-css"/>
            <arg file="css/theme.css"/>
            <arg file="src/theme.res"/>
        </java>
        <java jar="${user.home}/.codenameone/designer_1.jar" fork="true" failonerror="true">
            <jvmarg value="-Dcli=true"/>
            <arg value="-css"/>
            <arg file="css/dark-theme.css"/>
            <arg file="src/dark-theme.res"/>
        </java>
    </target>

Then in Java code you can load a different theme and change themes at runtime using our builtin theming.

__ |  You can put two themes in a single resource file but since most settings can’t be reused between dark/light the benefit is limited   
---|---  
  
You can load a new resource file theme by using:
    
    
    theme = UIManager.initFirstTheme("/resource");

You can then apply the theme to the current form dynamically using `refreshTheme()` on the form.

### Moving Forward

We hope to add additional dark/light templates and also port the native themes to include dark counterparts. This work isn’t scheduled yet but this is the direction.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Mohammed Hussein** — August 28, 2022 at 8:55 pm ([permalink](/blog/dark-mode/#comment-24542))

> Mohammed Hussein says:
>
> Hi Shai, just landed on this blog!! while searching on an issue with CSS theme switching, here’s what I did and please keep me right if misunderstood:  
> * I migrated successfully to Maven using my existing theme.css,  
> * following the KitchenSink demo, I try to add another dark-them.css, using :  
> `  
> Resources resources = Resources.openLayered( "/dark-theme");  
> `
>
> Sadly, I keep getting “/dark-theme.res not found” and no sign to the .res file on my folders!
>
> Also, please note that, build.xml does not exists on my Maven project!,
>
> Can you please advise with any hints on how to switch my theme with CSS?
>
> Best regards,
>



### **Mohammed Hussein** — September 1, 2022 at 8:46 am ([permalink](/blog/dark-mode/#comment-24544))

> Mohammed Hussein says:
>
> Thank you Shai,
>
> For those landing on this and using Maven, please note the following:
>
> Compiling the dark theme is not yet supported by the Maven plugin. An issue for this can be track on:  
> <https://github.com/codenameone/CodenameOne/issues/3623>
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
