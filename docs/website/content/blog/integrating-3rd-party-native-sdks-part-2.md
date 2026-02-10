---
title: Integrating 3rd Party Native SDKs Part II
slug: integrating-3rd-party-native-sdks-part-2
url: /blog/integrating-3rd-party-native-sdks-part-2/
original_url: https://www.codenameone.com/blog/integrating-3rd-party-native-sdks-part-2.html
aliases:
- /blog/integrating-3rd-party-native-sdks-part-2.html
date: '2015-10-04'
author: Steve Hannah
---

![Header Image](/blog/integrating-3rd-party-native-sdks-part-2/native-sdks-header.jpg)

This blog post is part two in a three-part series on integrating 3rd party native SDKs into Codename One application. I recommend you start with part one in this series as it will give you much-needed context to understand the procedures described in part two and three.

In [part one](http://www.codenameone.com/blog/integrating-3rd-party-native-sdks-part-1.html), we described the design and development of the public API and native interfaces of the Codename One FreshDesk library. In this installment we’ll move onto the development of the Android side of the native interface.

## Step 5: Implementing the Native Interface in Android

Now that we have set up our public API and our native interface, it is time to work on the native side of things. You can generate stubs for all platforms in your IDE (Netbeans in my case), by right clicking on the `MobihelpNative` class in the project explorer and selecting “Generate Native Access”.

![c9d4b9cc 61f6 11e5 8b67 4691600188cd](/blog/integrating-3rd-party-native-sdks-part-2/c9d4b9cc-61f6-11e5-8b67-4691600188cd.png)

This will generate a separate directory for each platform inside your project’s `native` directory:

![eef6d078 61f6 11e5 91cd 2e1836916359](/blog/integrating-3rd-party-native-sdks-part-2/eef6d078-61f6-11e5-91cd-2e1836916359.png)

Inside the `android` directory, this generates a `com/codename1/freshdesk/MobihelpNativeImpl` class with stubs for each method.

Our implementation will be a thin wrapper around the native Android SDK. See the source [here](https://github.com/shannah/cn1-freshdesk/blob/master/cn1-freshdesk-demo/native/android/com/codename1/freshdesk/MobihelpNativeImpl.java).

**Some highlights:**

  1. `Context` : The native API requires us to pass a **context** object as a parameter on many methods. This should be the context for the current activity. It will allow the FreshDesk API to know where to return to after it has done its thing. Codename One provides a class called `AndroidNativeUtil` that allows us to retrieve the app’s Activity (which includes the Context). We’ll wrap this with a convenience method in our class as follows:
         
         private static Context context() {
             return com.codename1.impl.android.AndroidNativeUtil.getActivity().getApplicationContext();
         }

This will enable us to easily wrap the freshdesk native API. E.g.:
         
         public void clearUserData() {
             com.freshdesk.mobihelp.Mobihelp.clearUserData(context());
         }

  2. `runOnUiThread()` – Many of the calls to the FreshDesk API may have been made from the Codename One EDT. However, Android has its own event dispatch thread that should be used for interacting with native Android UI. Therefore, any API calls that look like they initiate some sort of native Android UI process should be wrapped inside Android’s `runOnUiThread()` method which is similar to Codename One’s `Display.callSerially()` method. E.g. see the `showSolutions()` method:
         
         public void showSolutions() {
                 activity().runOnUiThread(new Runnable() {
                     public void run() {
                         com.freshdesk.mobihelp.Mobihelp.showSolutions(context());
                     }
                 });
         
             }

(Note here that the `activity()` method is another convenience method to retrieve the app’s current `Activity` from the `AndroidNativeUtil` class).

  3. **Callbacks**. We discussed, in detail, the mechanisms we put in place to enable our native code to perform callbacks into Codename One. You can see the native side of this by viewing the `getUnreadCountAsync()` method implementation:
         
         public void getUnreadCountAsync(final int callbackId) {
                 activity().runOnUiThread(new Runnable() {
                     public void run() {
                         com.freshdesk.mobihelp.Mobihelp.getUnreadCountAsync(context(), new com.freshdesk.mobihelp.UnreadUpdatesCallback() {
                             public void onResult(com.freshdesk.mobihelp.MobihelpCallbackStatus status, Integer count) {
                                 MobihelpNativeCallback.fireUnreadUpdatesCallback(callbackId, status.ordinal(), count);
                             }
                         });
                     }
                 });
         
             }

## Step 6: Bundling the Native SDKs

The last step (at least on the Android side) is to bundle the FreshDesk SDK. For Android, there are a few different scenarios you’ll run into for embedding SDKs:

  1. **The SDK includes only Java classes** – NO XML UI files, assets, or resources that aren’t included inside a simple .jar file. In this case, you can just place the .jar file inside your project’s `native/android` directory.

  2. **The SDK includes some XML UI files, resources, and assets.** In this case, the SDK is generally distributed as an Android project folder that can be imported into an Eclipse or Android studio workspace. In general, in this case, you would need to zip the entire directory and change the extension of the resulting .zip file to “.andlib”, and place this in your project’s `native/android` directory.

  3. **The SDK is distributed as an`.aar` file** – In this case you can just copy the `.aar` file into your `native/android` directory.

### The FreshDesk SDK

The FreshDesk (aka Mobihelp) SDK is distributed as a project folder (i.e. scenario 2 from the above list). Therefore, our procedure is to download the SDK ([download link](https://s3.amazonaws.com/assets.mobihelp.freshpo.com/sdk/mobihelp_sdk_android.zip)), and rename it from `mobihelp_sdk_android.zip` to `mobihelp_sdk_android.andlib`, and copy it into our `native/android` directory.

#### Dependencies

Unfortunately, in this case there’s a catch. The Mobihelp SDK includes a dependency:

> Mobihelp SDK depends on AppCompat-v7 (Revision 19.0+) Library. You will need to update project.properties to point to the Appcompat library.

If we look inside the `project.properties` file (inside the Mobihelp SDK directory— i.e. you’d need to extract it from the zip to view its contents), you’ll see the dependency listed:
    
    
    android.library.reference.1=../appcompat_v7

I.e. it is expecting to find the `appcompat_v7` library located in the same parent directory as the Mobihelp SDK project. After a little bit of research (if you’re not yet familiar with the Android AppCompat support library), we find that the `AppCompat_v7` library is part of the Android Support library, which can can installed into your local Android SDK using Android SDK Manager. [Installation processed specified here](https://developer.android.com/tools/support-library/setup.html).

After installing the support library, you need to retrieve it from your Android SDK. You can find that .aar file inside the `ANDROID_HOME/sdk/extras/android/m2repository/com/android/support/appcompat-v7/19.1.0/` directory (for version 19.1.0). The contents of that directory on my system are:
    
    
    appcompat-v7-19.1.0.aar		appcompat-v7-19.1.0.pom
    appcompat-v7-19.1.0.aar.md5	appcompat-v7-19.1.0.pom.md5
    appcompat-v7-19.1.0.aar.sha1	appcompat-v7-19.1.0.pom.sha1

There are two files of interest here:

  1. appcompat-v7-19.1.0.aar – This is the actual library that we need to include in our project to satisfy the Mobisdk dependency.

  2. appcompat-v7-19.1.0.pom – This is the Maven XML file for the library. It will show us any dependencies that the appcompat library has. We will also need to include these dependencies:
         
         <dependencies>
             <dependency>
               <groupId>com.android.support</groupId>
               <artifactId>support-v4</artifactId>
               <version>19.1.0</version>
               <scope>compile</scope>
             </dependency>
           </dependencies>

i.e. We need to include the `support-v4` library version 19.1.0 in our project. This is also part of the Android Support library. If we back up a couple of directories to: `ANDROID_HOME/sdk/extras/android/m2repository/com/android/support`, we’ll see it listed there:
         
         appcompat-v7			palette-v7
         cardview-v7			recyclerview-v7
         gridlayout-v7			support-annotations
         leanback-v17			support-v13
         mediarouter-v7			support-v4
         multidex			test
         multidex-instrumentation

\+ And if we look inside the appropriate version directory of `support-v4` (in `ANDROID_HOME/sdk/extras/android/m2repository/com/android/support/support-v4/19.1.0`), we see:
         
         support-v4-19.1.0-javadoc.jar		support-v4-19.1.0.jar
         support-v4-19.1.0-javadoc.jar.md5	support-v4-19.1.0.jar.md5
         support-v4-19.1.0-javadoc.jar.sha1	support-v4-19.1.0.jar.sha1
         support-v4-19.1.0-sources.jar		support-v4-19.1.0.pom
         support-v4-19.1.0-sources.jar.md5	support-v4-19.1.0.pom.md5
         support-v4-19.1.0-sources.jar.sha1	support-v4-19.1.0.pom.sha1

Looks like this library is pure Java classes, so we only need to include the `support-v4-19.1.0.jar` file into our project. Checking the `.pom` file we see that there are no additional dependencies we need to add.

So, to summarize our findings, we need to include the following files in our `native/android` directory:

  1. appcompat-v7-19.1.0.aar

  2. support-v4-19.1.0.jar

And since our Mobihelp SDK lists the appcompat_v7 dependency path as “../appcompat_v7” in its project.properties file, we are going to rename `appcompat-v7-19.1.0.aar` to `appcompat_v7.aar`.

When all is said and done, our `native/android` directory should contain the following:
    
    
    appcompat_v7.aar	mobihelp.andlib
    com			support-v4-19.1.0.jar

## Step 7 : Injecting Android Manifest and Proguard Config

The final step on the Android side is to inject necessary permissions and services into the project’s AndroidManifest.xml file.

We can find the manifest file injections required by opening the `AndroidManifest.xml` file from the MobiHelp SDK project. Its contents are as follows:
    
    
    <?xml version="1.0" encoding="utf-8"?>
    <manifest xmlns_android="http://schemas.android.com/apk/res/android"
        package="com.freshdesk.mobihelp"
        android_versionCode="1"
        android_versionName="1.0" >
    
         <uses-sdk
            android_minSdkVersion="10" />
    
        <uses-permission android_name="android.permission.INTERNET" />
        <uses-permission android_name="android.permission.READ_LOGS" />
        <uses-permission android_name="android.permission.ACCESS_NETWORK_STATE" />
        <uses-permission android_name="android.permission.WRITE_EXTERNAL_STORAGE" />
    
        <application>
            <activity
                android_name="com.freshdesk.mobihelp.activity.SolutionArticleListActivity"
                android_configChanges="orientation|screenSize"
                android_theme="@style/Theme.Mobihelp"
                android_windowSoftInputMode="adjustPan" >
            </activity>
            <activity
                android_name="com.freshdesk.mobihelp.activity.FeedbackActivity"
                android_configChanges="keyboardHidden|orientation|screenSize"
                android_theme="@style/Theme.Mobihelp"
                android_windowSoftInputMode="adjustResize|stateVisible" >
            </activity>
            <activity
                android_name="com.freshdesk.mobihelp.activity.InterstitialActivity"
                android_configChanges="orientation|screenSize"
                android_theme="@style/Theme.AppCompat">
    		</activity>
            <activity
                android_name="com.freshdesk.mobihelp.activity.TicketListActivity"
                android_parentActivityName="com.freshdesk.mobihelp.activity.SolutionArticleListActivity"
                android_theme="@style/Theme.Mobihelp" >
    
                <!-- Parent activity meta-data to support 4.0 and lower -->
                <meta-data
                    android_name="android.support.PARENT_ACTIVITY"
                    android_value="com.freshdesk.mobihelp.activity.SolutionArticleListActivity" />
            </activity>
            <activity
                android_name="com.freshdesk.mobihelp.activity.SolutionArticleActivity"
                android_parentActivityName="com.freshdesk.mobihelp.activity.SolutionArticleListActivity"
                android_theme="@style/Theme.Mobihelp"
                android_configChanges="orientation|screenSize|keyboard|keyboardHidden" >
    
                <!-- Parent activity meta-data to support 4.0 and lower -->
                <meta-data
                    android_name="android.support.PARENT_ACTIVITY"
                    android_value="com.freshdesk.mobihelp.activity.SolutionArticleListActivity" />
            </activity>
            <activity
                android_name="com.freshdesk.mobihelp.activity.ConversationActivity"
                android_parentActivityName="com.freshdesk.mobihelp.activity.SolutionArticleListActivity"
                android_theme="@style/Theme.Mobihelp"
                android_windowSoftInputMode="adjustResize|stateHidden" >
    
                <!-- Parent activity meta-data to support 4.0 and lower -->
                <meta-data
                    android_name="android.support.PARENT_ACTIVITY"
                    android_value="com.freshdesk.mobihelp.activity.SolutionArticleListActivity" />
            </activity>
            <activity
                android_name="com.freshdesk.mobihelp.activity.AttachmentHandlerActivity"
                android_configChanges="keyboardHidden|orientation|screenSize"
                android_parentActivityName="com.freshdesk.mobihelp.activity.SolutionArticleListActivity"
                android_theme="@style/Theme.Mobihelp" >
    
                <!-- Parent activity meta-data to support 4.0 and lower -->
                <meta-data
                    android_name="android.support.PARENT_ACTIVITY"
                    android_value="com.freshdesk.mobihelp.activity.SolutionArticleListActivity" />
            </activity>
    
            <service android_name="com.freshdesk.mobihelp.service.MobihelpService" />
    
            <receiver android_name="com.freshdesk.mobihelp.receiver.ConnectivityReceiver" >
                <intent-filter>
                    <action android_name="android.net.conn.CONNECTIVITY_CHANGE" />
                </intent-filter>
            </receiver>
        </application>
    
    </manifest>

We’ll need to add the `<uses-permission>` tags and all of the contents of the `<application>` tag to our manifest file. Codename One provides the following build hints for these:

  1. `android.xpermissions` – For your `<uses-permission>` directives. Add a build hint with name `android.xpermissions`, and for the value, paste the actual `<uses-permission>` XML tag.

  2. `android.xapplication` – For the contents of your `<application>` tag.

### Proguard Config

For the release build, we’ll also need to inject some proguard configuration so that important classes don’t get stripped out at build time. The FreshDesk SDK instructions state:

> If you use Proguard, please make sure you have the following included in your project’s proguard-project.txt
>     
>     
>     -keep class android.support.v4.** { *; }
>     -keep class android.support.v7.** { *; }

In addition, if you look at the `proguard-project.txt` file inside the Mobihelp SDK, you’ll see the rules:
    
    
    -keep public class * extends android.app.Service
    -keep public class * extends android.content.BroadcastReceiver
    -keep public class * extends android.app.Activity
    -keep public class * extends android.preference.Preference
    -keep public class com.freshdesk.mobihelp.exception.MobihelpComponentNotFoundException
    
    -keepclassmembers class * implements android.os.Parcelable {
      public static final android.os.Parcelable$Creator *;
    }

We’ll want to merge this and then paste them into the build hint `android.proguardKeep` of our project.

## Troubleshooting Android Stuff

If, after doing all this, your project fails to build, you can enable the “Include Source” option of the build server, then download the source project, open it in Eclipse or Android Studio, and debug from there.

## In Our Next Instalment…​

In the third and final part of this blog series, we will shift our focus to the iOS side of the library.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
