---
title: Deprecations, Simplified cn1lib installs & Theme Layering
slug: deprecations-simplified-cn1lib-installs-theme-layering
url: /blog/deprecations-simplified-cn1lib-installs-theme-layering/
original_url: https://www.codenameone.com/blog/deprecations-simplified-cn1lib-installs-theme-layering.html
aliases:
- /blog/deprecations-simplified-cn1lib-installs-theme-layering.html
date: '2015-08-25'
author: Shai Almog
---

![Header Image](/blog/deprecations-simplified-cn1lib-installs-theme-layering/ios-cert-wizard-blog-post-header.png)

#### Deprecations

We decided to discontinue support for building without a certificate, this support was added initially because  
generating an iOS certificate was so difficult and we wanted developers to see that “it works” before committing  
to the expense. However, this process is wrought with bugs that are often hard to trace back and error prone.  
Added to that is the fact that we now have the new [certificate wizard](/blog/ios-certificate-wizard.html)  
which makes the process simpler thus removing the final blocker (no need for a Mac).  
We will block this functionality in the build servers by next week and thru the plugin after that. 

We also decided to remove the ability to push with a null device id with the new push servers overhaul.  
It was a tough decision to make but I’m sure you will get behind it when  
you see the other features we intend to add specifically: delivery reports, status & batched pushes.  
We’ll probably restore this functionality in the future in a different form that will allow other features such as  
smart segmentations etc. The API will probably take a very different form and be designed for server side  
usage. 

Last but not least, we still kept the original web UI that Codename One used in the old appspot server. If you  
are still using that UI then it will be discontinued soon! We suggest you migrate to the new web UI in this  
website.  
If there are features or capabilities missing from the current web UI please let us know. 

#### Build Hints in cn1libs

Some cn1libs are pretty simple to install, just place them under the lib directory and refresh. However, many of the more  
elaborate cn1libs need some pretty complex configurations. This is the case when native code is involved where  
we need to add permissions or plist entries for the various native platforms to get everything to work. This makes  
the cn1lib’s helpful but less than seamless which is where we want to go. 

If you don’t intend to write a cn1lib you can skip to the next section, for you this post just means that future cn1lib  
install instructions would no longer include build hints… However, if you are writing cn1libs then this is a pretty big  
new feature… 

We now support two new files that can be placed into the cn1lib root and exist when you create a new library using  
the new project wizard: `codenameone_library_required.properties` & `codenameone_library_appended.properties`. 

In these files you can just write a build hint as `codename1.arg.ios.plistInject=...` for the various  
hints. The obvious question is why do we need to files? 

There are two types of build hints: required and appended. Required build hints can be something like `ios.objC=true`  
which we want to always work. E.g. if a cn1lib defines `ios.objC=true` and another cn1lib defines  
`ios.objC=false` things won’t work since one cn1lib won’t get what it needs…  
In this case we’d  
want the build to fail so we can remove the faulty cn1lib. 

An appended property would be something like `codename1.arg.ios.plistInject=<key>UIBackgroundModes</key><array><string>audio</string> </array>`  
Notice that this can still collide e.g. if a different cn1lib defines its own background mode… However, there are  
many valid cases where `ios.plistInject` can be used for other things. In this case we’ll append  
the content of the `ios.plistInject` into the build hint if its not already there. 

There are a couple of things you need to keep in mind: 

  * This code happens with every “refresh libs” call not dynamically on the server. This means it should be pretty  
simple for the developer to investigate issues in this process. 
  * Changing flags is problematic – there is no “uninstall” process. Since the data is copied into the `codenameone_settings.properties`  
file. If you need to change a flag later on you might need to alert users to make changes to their properties essentially  
negating the value of this feature… So be very careful when adding properties here. 

The rule of thumb is that a build hint that has a numeric or boolean value is always required. If an entry has a string that you can append with another string  
then its probably an appended entry  
These build hints are probably of the “required” type: 
    
    
    android.debug	
    android.release	
    android.installLocation 	
    android.licenseKey	
    android.stack_size
    android.statusbar_hidden
    android.googleAdUnitId
    android.includeGPlayServices
    android.headphoneCallback
    android.gpsPermission
    android.asyncPaint
    android.supportV4
    android.theme
    android.cusom_layout1
    android.versionCode
    android.captureRecord
    android.removeBasePermissions
    android.blockExternalStoragePermission
    android.min_sdk_version
    android.smallScreens
    android.streamMode
    android.enableProguard
    android.targetSDKVersion
    android.web_loading_hidden
    facebook.appId
    ios.keyboardOpen
    ios.project_type
    ios.newStorageLocation
    ios.prerendered_icon
    ios.application_exits
    ios.themeMode
    ios.xcode_version
    javascript.inject_proxy
    javascript.minifying
    javascript.proxy.url
    javascript.sourceFilesCopied
    javascript.teavm.version
    rim.askPermissions
    google.adUnitId
    ios.includePush
    ios.headphoneCallback
    ios.enableAutoplayVideo
    ios.googleAdUnitId
    ios.googleAdUnitIdPadding
    ios.enableBadgeClear
    ios.locationUsageDescription
    ios.bundleVersion
    ios.objC
    ios.testFlight
    desktop.width
    desktop.height
    desktop.adaptToRetina
    desktop.resizable
    desktop.fontSizes
    desktop.theme
    desktop.themeMac
    desktop.themeWin
    desktop.windowsOutput
    noExtraResources	
    j2me.iconSize

These build hints should probably be appended 
    
    
    android.xapplication	
    android.xpermissions	
    android.xintent_filter	
    android.facebook_permissions
    android.stringsXml
    android.style
    android.nonconsumable
    android.xapplication_attr
    android.xactivity
    android.pushVibratePattern
    android.proguardKeep
    android.sharedUserId	
    android.sharedUserLabel	
    ios.urlScheme
    ios.interface_orientation
    ios.plistInject	
    ios.facebook_permissions
    ios.applicationDidEnterBackground
    ios.viewDidLoad
    ios.glAppDelegateHeader
    ios.glAppDelegateBody
    ios.beforeFinishLaunching
    ios.afterFinishLaunching
    ios.add_libs

#### Theme Layering

Theme layering is something that comes up often in support queries and we don’t have a good reference for it  
so I decided to write a quick tutorial on that.  
There are two use cases in which you would want to use layering: 

  1. You want a **slightly** different theme in one platform
  2. You want the ability to customize your theme for a specific use case, e.g. let a user select larger fonts

This is actually pretty easy to do and doesn’t require re-doing the entire theme. You can do something very similar  
to the cascading effect of CSS where a theme is applied “on top” of another theme. To do that just add a new  
theme, make sure to remove the include native theme constant in the new theme. Then in the new theme define  
the changes e.g. if you just want a larger default font define only that property for all the relevant UIID’s and ignore  
all other properties! 

For a non-gui builder app the theme loading looks like this: 
    
    
    public void init(Object context) {
        try {
            theme = Resources.openLayered("/theme");
            UIManager.getInstance().setThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

Or for newer apps like this: 
    
    
    theme = UIManager.initFirstTheme("/theme");

You should fix it to look like this: 
    
    
    theme = UIManager.initNamedTheme("/theme", "Theme");

Notice, this assumes the name of your main theme is “Theme” (not the layer theme you just added). This is important  
since the original code relies on the theme being in the 0 position in the theme name array which might not be  
the case!  
Then when you want to add a layer just use: 
    
    
    UIManager.getInstance().addThemeProps(theme.getTheme("NameOfLayerTheme"));

The `addThemeProps` call will layer the secondary theme on top of the primary “Theme” and  
keep the original UIID’s defined in “Theme” intact. 

For a GUI builder app this is just as simple, you can override the initTheme method in the state machine in  
exactly the same way to specify the theme name explicitly: 
    
    
    protected void initTheme(Resources res) {
        UIManager.getInstance().setThemeProps(res.getTheme("Theme"));
    }

Then use the `addThemeProps` anywhere you want, notice that you can just get the theme using  
`fetchResourceFile()` in the state machine. 

If you apply theme changes to a running application you can use `Form`‘s `refreshTheme()`  
to update the UI instantly and provide visual feedback for the theme changes.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
