---
title: 'TIP: Include Source with Android Studio 3.0'
slug: tip-include-source-android-studio-3
url: /blog/tip-include-source-android-studio-3/
original_url: https://www.codenameone.com/blog/tip-include-source-android-studio-3.html
aliases:
- /blog/tip-include-source-android-studio-3.html
date: '2018-03-05'
author: Shai Almog
---

![Header Image](/blog/tip-include-source-android-studio-3/tip.jpg)

I covered the [include source feature](https://www.codenameone.com/how-do-i---use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc.html) extensively. For the most part it’s the simplest way to debug an application directly on the device. When I made that video the current version of Android Studio and Gradle were much older. We still use API version 23 on the build servers to keep everything compatible but you might want to use a newer version of the IDE.

In that case you can use the newer version 3.0+ of Android Studio but you will need to make a few updates to the process.

__ |  Notice that this is a temporary thing as eventually we’ll update the build servers to emit newer target versions too as features & devices proliferate   
---|---  
  
### Automatic Update

One of the features in Android Studio is its ability to automatically update the project. You should use that feature to update gradle to the latest version and the SDK version as well. I’ve tested version 26 and it seems to work fine but I haven’t done any stress testing and wouldn’t recommend it other than for debugging purposes.

__ |  If you submit an APK to Google play with a newer SDK version they will no longer accept an older version of the same APK!   
---|---  
  
Once the automatic update is done you will need to update a few files and settings.

### gradle.properties

In this file you just need to add the line:
    
    
    android.enableAapt2=false

This will fix issues with styles later on.

### Style Files

There are 3 style files named `styles.xml` under the directories: `res/values`, `res/values-v11` & `res/values-v21`.

They appear in the IDE as one file with 3 versions. The default versions from our servers include some `@` characters in the wrong place that Google doesn’t play nicely with. Since they are auto-generated they are badly formatted and hard to fix manually. So here are the full versions of all 3 files that you can paste on top of the existing files.

`res/values/styles.xml` :
    
    
    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <style name="CustomTheme" parent="android:Theme.Black">
            <item name="attr/cn1Style">@style/CN1.EditText.Style</item>
        </style>
        <attr name="cn1Style" format="reference" />
        <style name="CN1.EditText.Style" parent="@android:style/Widget.EditText">
            <item name="android:textCursorDrawable">@null</item>
        </style>
    </resources>

`res/values-v11/styles.xml` :
    
    
    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <style name="CustomTheme" parent="@android:style/Theme.Holo.Light">
            <item name="attr/cn1Style">@style/CN1.EditText.Style</item>
            <item name="android:windowActionBar">false</item>
            <item name="android:windowTitleSize">0dp</item>
        </style>
        <style name="CN1.EditText.Style" parent="@android:style/Widget.EditText">
            <item name="android:textCursorDrawable">@null</item>
        </style>
    </resources>

`res/values-v21/styles.xml` :
    
    
    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <style name="CustomTheme" parent="@android:style/Theme.Material.Light">
            <item name="attr/cn1Style">@style/CN1.EditText.Style</item>
            <item name="android:windowActionBar">false</item>
            <item name="android:windowTitleSize">0dp</item>
            <item name="android:colorPrimary">@color/colorPrimary</item>
            <item name="android:colorPrimaryDark">@color/colorPrimaryDark</item>
            <item name="android:colorAccent">@color/colorAccent</item>
       </style>
        <style name="CN1.EditText.Style" parent="@android:style/Widget.EditText">
            <item name="android:textCursorDrawable">@null</item>
        </style>
    </resources>

### Finally

All of this should work and run with Android Studio 3.0. Ideally this blog post will be unnecessary by the time you read it as we’d move forward to align with the current approach from Google.

I’m not sure when we’ll upgrade from 23 as it’s a relatively good target version, to a large degree it depends on feedback from you.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
