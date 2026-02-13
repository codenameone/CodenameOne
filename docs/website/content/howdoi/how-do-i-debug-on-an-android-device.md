---
title: DEBUG A CODENAME ONE APPLICATION ON AN ANDROID DEVICE
slug: how-do-i-debug-on-an-android-device
url: /how-do-i/how-do-i-debug-on-an-android-device/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-debug-on-an-android-device.html
tags:
- advanced
- debugging
description: Debugging on a device using the Android Studio IDE
youtube_id: 008AK1GfHA8
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-14.jpg
---

{{< youtube "008AK1GfHA8" >}} 

#### Transcript

In this short video I’ll try to explain how to debug a Codename One application on an Android device.  
This video assumes you are familiar with the basics of Codename One and have Android studio installed.

We start by opening the settings selecting the basic section and checking the “include source” checkbox. Now we can send a build as usual to Android.  
In the build server results you will see the additional sources file which I will download.

Next I’ll launch the Android studio IDE and proceed to create a new project. Within this project I’m pasting in the main class and package names from Codename One. I will leave the rest as the default in this next step and in the final step of the wizard I will select “no activity” as we already have everything.

Now that a project is created I’ll unzip the source files. I’ll then copy all the relevant files to the newly created project.  
I copy the main directory from within the src to the target src & I select to replace all the files. Next I copy the libs directory content to the equivalent directory in the native project. Finally I open the project gradle file as well as the source gradle file. I copy the dependencies from the source gradle to the app gradle dependencies section.

Some additional copying of gradle script snippets might be required based on your app!

Next we need to connect our device to the computer and press the debug button. After waiting for a long time the app will appear on the device.

Thanks for watching, please let us know what you think and get help at [codenameone.com](https://www.codenameone.com/).

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
