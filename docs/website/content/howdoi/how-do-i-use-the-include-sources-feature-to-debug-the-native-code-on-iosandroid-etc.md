---
title: USE THE INCLUDE SOURCES FEATURE TO DEBUG THE NATIVE CODE ON IOS/ANDROID ETC.
slug: how-do-i-use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc
url: /how-do-i/how-do-i-use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc.html
tags:
- pro
description: The source is useful for debugging on device, looking under the hood
  and writing native interfaces
youtube_id: 6oTy-LcTm0s
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-7-1.jpg
---

{{< youtube "6oTy-LcTm0s" >}} 

#### Transcript

In this video I’ll discuss the “include source” feature. With include source we can get the native OS source code as a result of a build. This allows us to debug & profile on devices.  
Notice that this isn’t meant for manual native OS coding as we have the native interfaces feature that allows you to write native code. Still include source is a very useful tool when working with native interfaces and I recommend checking that video out too.

We can activate include source by launching Codename One Settings and checking the include source flag, once that is done we can send a build to the servers.

The resulting builds for iOS and Android will include an additional sources file that includes a native OS project. In this case we see two builds of the kitchen sink demo.

For iOS we have the sources tar.bz2 file which includes an xcode project.

For Android we have a sources.zip file which includes an Android Studio gradle project. We’ll go over both soon but first I want to discuss a couple of points

When you build a Codename One project with include source turned on we just zip the project we generated in the server.  
  
This slows down the build which is why this isn’t on by default.  
  
You need a basic or higher subscription for this to work since it slows down the build.  
  
I’ll only discuss iOS & Android in this video but include source works for other platforms too.  

When we download and unzip the respective source archives we can see the Android & iOS source structures.

The iOS sources are stored under the dist directory.  
You will notice two important files the xcodeproj which intuitively seems like the “right file” but it’s not and you shouldn’t open it!  
Instead you need to open the xcworkspace file… This file includes the full project and that’s the project that will run. When we double click the xcworkspace file we get a warning about a file downloaded from the internet and then xcode launches.

In the launched file we can just press play to run the project in the native simulator or on device. Notice you can connect your device and run directly on it!

The iOS simulator launches and you can run the app using Apples native functionality although Apple doesn’t implement all features in the simulator. A good example is push which doesn’t work on the native simulator and only works on the device.

The iOS simulator launches and you can run the app using Apples native functionality although Apple doesn’t implement all features in the simulator. A good example is push which doesn’t work on the native simulator and only works on the device.

Running in the native OS is valuable because you can debug and profile on the device. This is the about dialog for the app, lets put a breakpoint on the dialog show functionality so we can debug that.

I’ll put a breakpoint on all dialogs by searching for the Dialog class. This class is translated to native code as a file ending with Dialog.m. Notice the files are just the package names and class names with underscores.  
I’ll search for the show method that accepts 4 strings. The method naming convention is three underscores after the method name followed by the argument types. This allows method overloading. Objects are listed with their full class name of java lang String. This makes finding the right show method pretty easy, we can now set a breakpoint here.

In the simulator I reopen the dialog and now the breakpoint is triggered. I can inspect the variables by hovering over them. I can look at them in the variable section at the bottom too. Printouts appear on the bottom right and the full stack is on the left where I can see the full class and method names. I can click specific stack frames and walk up the stack to inspect the methods that triggered the show method all the way back to the original lambda call from the kitchen sink demo.  
This is a remarkably powerful tool and it’s especially useful when debugging native interfaces.

Moving on to Android we launch Android Studio and open an existing Android Studio Project. We select the directory containing the gradle script file. We get a warning about the gradle location in the server and then wait a long while for Android studio to launch…

Once Android studio launched it will fail to compile the code, we need to open the preferences UI for Android studio to fix that.

The preferences can be opened from the menu in Mac and Windows although in slightly different locations.

Inside the preferences we need to select the build tools section and pick the current local copy of gradle which you can download from the web. Right now we need gradle 2.11 but this could change so we suggest checking out with our support if this changes in the future. Once the local gradle is configured you can just press OK and everything will update automatically.

Now we can just press the debug button and pick the device in the UI, notice that on Android debugging on devices is MUCH faster than using the Android emulator which is why I’m using a device here.

Let’s place a breakpoint in Dialog like we did in the iOS version, we can see the source code of Codename One and the Android implementation right here and open the Dialog class. Within this class we can find the show method with 4 strings and set a breakpoint here. Now I can just click the dialog button on the device…

And we hit the breakpoint… Again we can step thru the stack and inspect variables but notice an interesting thing. When I try to go to the classes from the kitchen sink it shows me binaries not sources.  
The reason for this is simple. When you build a native app only the binary classes are sent to the server. In Android we can just package the class files and move along, in iOS we translate all the class files to C files so we have native sources for everything.

Thanks for watching, I hope you found this helpful.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
