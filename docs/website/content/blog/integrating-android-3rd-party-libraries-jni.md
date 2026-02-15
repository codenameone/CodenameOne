---
title: Integrating Android 3rd Party Libraries & JNI
slug: integrating-android-3rd-party-libraries-jni
url: /blog/integrating-android-3rd-party-libraries-jni/
original_url: https://www.codenameone.com/blog/integrating-android-3rd-party-libraries-jni.html
aliases:
- /blog/integrating-android-3rd-party-libraries-jni.html
date: '2015-03-03'
author: Chen Fishbein
---

![Header Image](/blog/integrating-android-3rd-party-libraries-jni/binary.png)

![](/blog/integrating-android-3rd-party-libraries-jni/binary.png)

While its pretty easy to use [native interfaces](http://www.codenameone.com/how-do-i---access-native-device-functionality-invoke-native-interfaces.html)  
to write Android native code some things aren’t necessarily as obvious. E.g. if you want to integrate a 3rd party library, specifically one that includes native C JNI code this process is somewhat undocumented.  
If you need to integrate such a library into your native calls you have the following 3 options: 

  1. The first option (and the easiest one) is to just place a Jar file in the native/android directory.  
This will link your binary with the jar file. Just place the jar under the native/android and the build server will pick it up and will add it to the classpath.  
Notice that Android release apps are obfuscated by default which might cause issues with such libraries if they reference API’s that are unavailable on Android.  
You can workaround this by adding a build hint to the proguard obfuscation code that blocs the obfuscation of the problematic classes using the build hint: 
         
         android.proguardKeep=-keep class com.mypackage.ProblemClass { *; }

  2. The second option is to add an Android Library Project. Not all 3rd parties can be packaged as a simple jar, some 3rd parties needs to declare  
Activities add permissions, resources, assets, and/or even add native code (.so files). To link a Library project to your CN1 project open the Library  
project in Eclipse or Android Studio and make sure the project builds, after the project was built successfully remove the bin directory from the  
project and zip the whole project.  
Rename the extension of the zip file to .andlib and place the andlib file under native/android directory. The build server will pick it up and will link it to the project.
  3. We recently added a 3rd option :aar files. The aar file is a binary format from Google that represents an Android Library project. One of the problem with  
the Android Library projects was the fact that it required the project sources which made it difficult for 3rd party vendors to publish libraries, so android  
introduced the aar file which is a binary format that represents a Library project. To learn more about arr you can read  
[this](http://tools.android.com/tech-docs/new-build-system/aar-format).

You can link an aar file by placing it under the native/android and the build server will link it to the project.

To use any of the options above 3rd parties API’s you will need to create a  
[ NativeInterface](http://www.codenameone.com/how-do-i---access-native-device-functionality-invoke-native-interfaces.html) and access  
the libs API’s under the android implementation only section.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chidiebere Okwudire** — June 29, 2015 at 10:49 am ([permalink](/blog/integrating-android-3rd-party-libraries-jni/#comment-21599))

> Chidiebere Okwudire says:
>
> Hi Chen,
>
> Under option 1, can you elaborate what you mean by “API’s that are unavailable on Android” when talking about obfuscation?
>



### **John Markh** — January 18, 2017 at 7:13 pm ([permalink](/blog/integrating-android-3rd-party-libraries-jni/#comment-23235))

> John Markh says:
>
> I would be great to have a code example to, for example, using Android PackageManager to retrieve a list of installed applications.
>



### **Shai Almog** — January 19, 2017 at 6:15 am ([permalink](/blog/integrating-android-3rd-party-libraries-jni/#comment-24120))

> Shai Almog says:
>
> There are quite a few samples in the cn1libs section where pretty much all the libraries are open source.
>
> Using this query on github I was able to find several results: [https://github.com/search?q…]([https://github.com/search?q=codename1+PackageManager&type=Code&utf8=%E2%9C%93](https://github.com/search?q=codename1+PackageManager&type=Code&utf8=%E2%9C%93))
>



### **Shai Almog** — January 19, 2017 at 6:17 am ([permalink](/blog/integrating-android-3rd-party-libraries-jni/#comment-23146))

> Shai Almog says:
>
> Hi,  
> sorry for the late reply. Not sure why Chen didn’t answer back then…
>
> A JAR might import javax.swing and use it for some cases but might handle that case correctly by catching the class not found exception. However, this might collide with obfuscation that doesn’t like those sort of tricks…
>



### **Amina Benzerga** — July 13, 2017 at 3:31 pm ([permalink](/blog/integrating-android-3rd-party-libraries-jni/#comment-23516))

> Amina Benzerga says:
>
> the query does not work anymore, could you please give me a example? Thank you 🙂
>



### **Shai Almog** — July 14, 2017 at 6:35 am ([permalink](/blog/integrating-android-3rd-party-libraries-jni/#comment-21393))

> Shai Almog says:
>
> I see 11 results in the link above
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
