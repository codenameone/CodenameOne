---
title: Debug a Codename One app on an Android Device
slug: debug-a-codename-one-app-on-an-android-device
url: /blog/debug-a-codename-one-app-on-an-android-device/
original_url: https://www.codenameone.com/blog/debug-a-codename-one-app-on-an-android-device.html
aliases:
- /blog/debug-a-codename-one-app-on-an-android-device.html
date: '2016-05-30'
author: Shai Almog
---

![Header Image](/blog/debug-a-codename-one-app-on-an-android-device/mqdefault.jpg)

Debugging Codename One apps on iOS devices has been documented well with a video for years, we didn’t  
spend too much time outlining the Android counterpart mostly because we didn’t really use it as much and it  
was far simpler.

As Android Studio launched this actually became really easy as it was possible to actually open the gradle project  
in Android Studio and just run it. But due to the fragile nature of the gradle project this stopped working for our recent  
builds, this works for some cases but is a bit of a flaky touch & go.

__ |  If  
[these instructions](http://stackoverflow.com/questions/34430404/how-to-build-the-native-android-sources-from-codename-ones-build-server)  
work for you then you can ignore the video/instructions below. However, if they don’t then keep reading   
---|---  
  
Google has the tendency to change things frequently which makes documenting a process to work  
with Android much harder than the iOS equivalent.  
The method outlined in the ["how do i" video](/how-do-i/how-do-i-debug-on-an-android-device/) that we just launched  
should work regardless of future changes. It might not be the best way to do this but it’s simple and it works.

Here are the steps highlighted in the video:

  1. Check the include source flag in the IDE and send a build

  2. Download the sources.zip result from the build server

  3. Launch Android Studio and create a new project

  4. Make sure to use the same package and app name as you did in the Codename One project, select to not create an activity

  5. Unzip the sources.zip file and copy the `main` directory from its `src` directory to the Android Studio projects `src` directory  
make sure to overwrite files/directories.

  6. Copy its `libs` directory on top of the existing libs

  7. Copy the source gradle files dependencies content to the destination gradle file

  8. Connect your device and press the Debug button for the IDE

__ |  You might need to copy additional gradle file meta-data such as multi-dexing etc.   
---|---  
  
You might not need to repeat the whole thing with every build. E.g. it might be practical to only copy the `userSources.jar`  
from the libs directory to get the latest version of your code.and you can copy the `src/main` directory to get our  
up to date port.

### Refinement

There are many edge cases and hints that probably don’t fit into this process, let us know in the comments below  
about the difficulties and success you’ve had with this process and also provide tips about simpler hacks to  
build the code for device.

There is a portion we didn’t get into with the video, copying updated sources directly without sending a build.  
This is possible if you turn on the new Android Java 8 support. At this point you should be able to remove the libs  
jar file which contains your compiled data and place your source code directly into the native project for debugging  
on the device.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Nilmar Castro** — December 28, 2016 at 12:58 pm ([permalink](/blog/debug-a-codename-one-app-on-an-android-device/#comment-22976))

> Nilmar Castro says:
>
> Hello Shai,  
> I’m starting at codenameone. I am not fluent in English but I understand very well the majority of the tutorials that are presented. However in this specifically the presenter speaks very fast and it is not possible for me to understand as necessary.
>
> Is there any other?
>
> Thank you very much
>



### **Shai Almog** — December 29, 2016 at 5:41 am ([permalink](/blog/debug-a-codename-one-app-on-an-android-device/#comment-23219))

> Shai Almog says:
>
> Hi,  
> I prefer her voice to mine (I narrated the old videos) but thanks 😉
>
> In the newer videos we have subtitles that contain the full text that you can read in the youtube page. You can also see the full transcript of every one of the new videos in their "How Do I?" page.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
