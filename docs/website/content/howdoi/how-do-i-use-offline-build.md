---
title: USE OFFLINE BUILD
slug: how-do-i-use-offline-build
url: /how-do-i/how-do-i-use-offline-build/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-use-offline-build.html
tags:
- pro
description: Build without the cloud build servers
youtube_id: IqsUSCgSVTo
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-18.jpg
---

{{< youtube "IqsUSCgSVTo" >}} 

#### Transcript

In this short video we will discuss the offline build feature which allows you to build native iOS & Android applications on a Mac without using the Codename One build servers. This is a feature designed for enterprise Codename One subscribers!  
It’s a complex feature to use and requires enterprise level support. However, once you install an offline builder you can keep using it even after your enterprise subscription elapses. In that sense it’s more like shrink wrap software than a subscription service.

Offline build includes most of the translation functionality of the build servers, it doesn’t include the compilation stage.  
As such the tool generates a native OS project that you then open in the native IDE either xcode or Android Studio.  
As such you don’t need an internet connection at all when using this tool except during the installation/update stages. Once a version is installed it will keep working.

As I mentioned before we see the offline builder tool as a shrink wrapped packaged application. That means you get ownership of a specific version of the builder as an enterprise user. Notice that this ownership isn’t transferable. You won’t get support or updates if you cancel the subscription, moving the builder to a new machine won’t be supported either.

The reason we think enterprise subscription is required is the complexity of offline building. A lot of things can go wrong in the process and it has a few external dependencies that are hard to support. So we recommend using the online build system, internally at Codname One we rarely use offline build as the build servers are often faster than our local machines!

The main reason we added offline build support is for government and institutions that due to heavy regulation can’t use the cloud. In those industries this is the only option to get at least some of the benefits of Codename One.

Before we begin we have some tools we need installed, notice that while the Android build should work on Windows our focus and testing has been on Macs as the iOS offline build requires Macs and an installation of xcode. Please check the developer guide for the exact versions of the tools as these change occasionally as we move forward… Currently xcode 7.3 is required but we are in the process of migrating to xcode 8 so please verify these versions.

We need the Oracle JDK 8.

Cocoapods allows us to install extensions in the iOS build.

xcodeproj generates the xcode project in the iOS build.

You need a current version of Android Studio and also the standard Android command line SDK. Make sure to install all the extensions in the command line SDK.

You need a standalone version of gradle which is currently 2.11, again check with us if things don’t work as expected.

You can install cocoapods and xcodeproj by typing these commands in your command prompt in the Mac OS terminal. Now that we have everything in place lets get started.

In Codename One Settings click on “Offline Builds”. I already have several builders installed but you would probably not. You will notice each builder has a version and date. A builder is a snapshot of our build server logic downloaded at a specific date.

You can use the Download button to download a new version of the builder, notice that this will do nothing if there is no new version…

If there is a new version and you have an enterprise subscription it will open a download UI and fetch the latest version of the builder.

Once downloaded the builder will be added automatically to the list of offline builders you have installed in your system.

You can delete unused builders and also select a specific builder version that you want to work with. This allows you to target a fixed version of the build server and keep historical versions for reference. If you discover a regression you can revert to a specific version that works for you until a fix is made. When you download a new version it is implicitly selected.

Notice that you need to configure the location of the Android command line SDK and the location of your gradle binary.

After all of this preparation lets build an Android version of this app we right click the project and select Android Offline Build.

Running this command takes a few seconds and it will generate a native OS Android project for us once the build completes successfully.

This is the project folder in the finder, the native OS project is under the build/and directory under the name of the native OS project which in this case is the native Map. One caveat to mention is that if we generate an offline build our previous offline build in the folder will be deleted even if it’s for a different platform. If you need to keep this folder copy it to somewhere else…

We can now launch Android Studio and open the project folder in that location. At this point things should be very familiar if you watches the include native sources tutorial.

Notice that I cut this short a little, Android Studio is slow and takes time to load… You will eventually see a gradle compilation error, we need to open the project preferences to fix this.  
Inside the preferences we need to select the build tools section and pick the current local copy of gradle which I mentioned earlier. Right now we need gradle 2.11 but again this could change so we check with our support if this changes in the future. Once the local gradle is configured you can just press OK and everything will update automatically. You should now be able to run/build for device/emulator natively!

So lets move to iOS where everything should be just as familiar… This time I’ll select “iOS Debug Offline Build”.

This takes MUCH longer to complete and runs for minutes sometimes, one of the things it does is run the app multiple times to grab the splash screen screenshots and then it translates the bytecode to C and installs cocoapod libraries if you have such dependencies. Notice that if you depend on cocoapods they might connect to the cocoapods site to download, this is something you should be able to configure in the coacoapods toolchain.  
Once compilation is done we can look again at the directory.

The directory is again similar to the include source results. Under build/iphone you will see the dist directory where you can open the project. Notice that the file you should open is the xcworkspace file and NOT the xcodeproj file!

Once you do that running and debugging should work but some users experience an odd caveat where the simulator doesn’t launch. You can fix this by selecting “Edit Scheme” then selecting the run entry and making sure your app is selected in the combo box for run. Once all of that is in place everything should work as expected.

Thanks for watching, I hope you found this helpful

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
