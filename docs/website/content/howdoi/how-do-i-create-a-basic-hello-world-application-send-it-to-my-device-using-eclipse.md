---
title: CREATE A BASIC HELLO WORLD APPLICATION & SEND IT TO MY DEVICE USING ECLIPSE
slug: how-do-i-create-a-basic-hello-world-application-send-it-to-my-device-using-eclipse
url: /how-do-i/how-do-i-create-a-basic-hello-world-application-send-it-to-my-device-using-eclipse/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-create-a-basic-hello-world-application-send-it-to-my-device-using-eclipse.html
tags:
- basic
- ui
description: Getting started guide for Eclipse developers
youtube_id: fmNpMFLwABA
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-21.jpg
---

{{< youtube "fmNpMFLwABA" >}} 

#### Installing The Eclipse Plugin

For details please check out the [download section](/download/).

In this short video we’ll walk you thru the very basics of Codename One.

Codename One allows Java developers to write native mobile apps for all devices easily. It’s open source, free and works with all IDE’s.

We can install Codename One by going to the plugin update center, typing in “Codename” and following the installation wizard.  
We can now create a new project, we need to select Codename One and once we do so we are faced with the “New Project Dialog”.

In this dialog we must first define the package name, you must pick a correct package name as changing this later is challenging.  
Next we can pick one of the builtin themes. If you want something very bare bones go with native, I will use red for this demo.  
Last but not least we need to pick the app type, I recommend checking out the getting started app. However, for simplicity’s sake I’m picking the “Bare Bones” hand coded application as it contains the least amount of code.

When we press the finish button the new app is created in the IDE. You will notice two major files, the theme resource file and the main source file. Let’s look at the generated code.  
In the main file we have four life cycle methods: init, start, stop and destroy.

Init is called once per application launch to setup things such as themes. You should use this instead of using the constructor.  
Start is invoked when the app is started or restored from minimized state. You use it to show the UI.  
Stop is invoked when the app is minimized & destroy might be invoked for a complete exit.

Let’s add a button to the new app: the code is rather trivial. We just add a button object to the parent form. We then bind an action listener to the button and show a dialog within that action logic.  
We can now run the simulator in the IDE by pressing the play button, you can manipulate the simulator using the simulate menu. You can switch devices using the skins menu as such.

Next we’ll open the designer by double clicking the theme file. The theme allows us to customize the appearance of the application. We can double click any entry or add new entries to customize their look. E.g. we set the button foreground to yellow and we can now rerun the simulator with this result.

Next we’ll open the designer by double clicking the theme file. The theme allows us to customize the appearance of the application. We can double click any entry or add new entries to customize their look. E.g. we set the button foreground to yellow and we can now rerun the simulator with this result.

The designer tool is also used for countless other features, such as: resolution independent images, localization and more!

The most important thing is running the resulting app on my devices, to do that we right click the project and select send Android build. You will notice there are many other build targets e.g. iOS. etc.).  
Once a build is made navigate to the [build server at codenameone.com](/build-server/) and select your build entry. You can then either email the link to yourself using the dedicated button or just scan the QR code in the page. This will allow you to download and install the app to your device.

Here is actual device footage for the app we just built!

iOS. apps are slightly more challenging, we need certificates from Apple in order to build a native app. For those you need an Apple developer account, once you have that in order just use the certificate wizard to generate all of the required certificates and you can then follow the exact same procedure used for Android.

Thanks for watching, please let us know what you think and get help at [codenameone.com](https://www.codenameone.com/).

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
