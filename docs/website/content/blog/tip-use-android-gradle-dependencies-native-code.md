---
title: 'TIP: Use Android Gradle Dependencies in Native Code'
slug: tip-use-android-gradle-dependencies-native-code
url: /blog/tip-use-android-gradle-dependencies-native-code/
original_url: https://www.codenameone.com/blog/tip-use-android-gradle-dependencies-native-code.html
aliases:
- /blog/tip-use-android-gradle-dependencies-native-code.html
date: '2017-02-05'
author: Shai Almog
---

![Header Image](/blog/tip-use-android-gradle-dependencies-native-code/tip.jpg)

Integrating a native OS library isn’t hard but it sometimes requires some juggling. Most instructions target developers working with xcode or Android Studio & you need to twist your head around them. In Android the steps for integration in most modern libraries include a gradle dependency.

E.g. we recently published a library that added support for [Intercom](/blog/intercom-support.html). The native Android integration instructions for the library looked like this:

Add the following dependency to your app’s `build.gradle` file:
    
    
    dependencies {
        compile 'io.intercom.android:intercom-sdk:3.+'
    }

Which instantly raises the question: “How in the world do I do that in Codename One”?

Well, it’s actually pretty simple. You can add the build hint:
    
    
    android.gradleDep=compile 'io.intercom.android:intercom-sdk:3.+'

This would “work” but there is a catch…​

You might need to define the specific version of the Android SDK used and specific version of Google play services version used. Intercom is pretty sensitive about those and demanded that we also add:
    
    
    android.playServices=9.8.0
    android.sdkVersion=25

Once those were defined the native code for the Android implementation became trivial to write and the library was easy as there were no jars to include.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
