---
title: Offline Build
slug: offline-build
url: /blog/offline-build/
original_url: https://www.codenameone.com/blog/offline-build.html
aliases:
- /blog/offline-build.html
date: '2016-08-09'
author: Shai Almog
---

![Header Image](/blog/offline-build/phone-espresso.jpg)

We finished the final major piece of the offline build offering that we [announced in July](/blog/preparing-for-3.5-offline-builds.html)!  
This Sunday we will update the plugins to include this ability as an option. Once installed you can use the instructions below to install the offline build service.

In this post I’ll explain the process and then follow up with some expected questions/answers. If you have questions/comments about this process please use the comment section below.

### Getting Started

We support iOS/Android targets for offline build and we require an Enterprise grade subscription as explained [here](/blog/preparing-for-3.5-offline-builds.html).

__ |  If you signup for Enterprise and cancel you can still do the offline build. You won’t be able to update the builder though   
---|---  
  
#### Prerequisites for iOS Builds

You need the following installed tools/versions for Codename One’s offline build process:

  * Mac ideally with El Capitan (the current version of Mac OS)

  * Xcode 7+

  * Oracle’s JDK 8

  * Cocoapods – in the terminal type `sudo gem install cocoapods --pre`.

  * xcodeproj – in the terminal type `sudo gem install xcodeproj`

#### Prerequisites for Android Builds

Android builds need the following:

  * Android Studio

  * Oracle’s JDK 8

  * Gradle version 2.11 (as of this writing)

### Installation

To build offline you need to install the offline builder code which is a stripped down version of the build servers. When you install a version of the offline builder it maps to the time in which you downloaded it…​

That means that features like [versioned builds](/how-do-i---get-repeatable-builds-build-against-a-consistent-version-of-codename-one-use-the-versioning-feature.html) won’t work. You can download/keep multiple offline builders and toggle between them which is similar in scope.

E.g. if you installed an offline builder then installed a newer version and the newer version has a bug you can revert to the old version. Notice that the older version might not have features that exist in a newer version.

__ |  Installation requires an enterprise account, you might need to re-login in the Codename One Settings UI   
---|---  
  
To install an offline builder open the Codename One Preferences (settings) UI by right clicking the project and selecting Codename One → Codname One Settings.

![Open Codename One settings](/blog/offline-build/newsettings-ui.png)

Figure 1. Open Codename One settings

__ |  Even though the settings are a part of a project, the offline build settings are global and apply to all the projects…​   
---|---  
  
Once the Codename One settings UI launches select the Offline Builds entry:

![Offline build entry](/blog/offline-build/offline-builds-section.png)

Figure 2. Offline build entry

This should launch the settings UI which would be blank the first time around:

![Offline builds setting UI](/blog/offline-build/offline-builds-settings.png)

Figure 3. Offline builds setting UI

When you ar e in this form you can press the download button to download the current version from the build server. If there is no update nothing will happen. If there is the latest version will download and tag with a version number/date.

You can see/change the selected version in this UI. This allows building against an older version. You can also delete older builds to save space.

### Building

Offline building is almost like building with the cloud. In the right click menu you can select one of the offline build targets as such:

![The offline build targets](/blog/offline-build/offline-build-targets.png)

Figure 4. The offline build targets

Once selected build generates a project under the `build/and` or `build/iphone` respectively.

Open these directories in Android Studio or xcode to run/build in the native IDE to the device or native emulator/simulator.

__ |  Build deletes previous offline builds, if you want to keep the sources of a build you need to move it to a different directory!   
---|---  
  
To get this to work with Android Studio you will need one more step. You will need to configure Android studio to use your local version of gradle 2.11 by following these steps:

  * Open the Android Studio preferences

![Android Studio Preferences](/blog/offline-build/android-studio-preferences.png)

Figure 5. Android Studio Preferences

  * Select Build, Execution, Deployment → Build Tools → Gradle

  * Select the Use Local gradle distribution

  * Press the …​ and pick your local gradle 2.11 install

![Local gradle config](/blog/offline-build/offline-gradle-config.png)

Figure 6. Local gradle config

### FAQ

#### Should I use the Offline Builder?

Probably not.

Cloud build is far more convenient, simple. Doesn’t require any installs (other than the plugin) and is much faster.

We built this tool for developers who work in situations that prohibit cloud build. E.g. government, banking etc. where regulation is restrictive.

#### Can I Move/Backup my Builders?

No.

We protect all the builders to avoid abuse. If you backup and restore on a new system the builders might stop working even if you are a paying enterprise customer.

#### Can I install the builders for all our developers?

Our licensing terms require a parallel developer seat for the Codename One developers in your company. If you have 5 Codename One developers they must all have an enterprise developer account to comply.

E.g. You can’t have one enterprise account and 4 basic accounts.

The reason behind this is simple, in the past we saw a lot of funneling from developers who built such a licensing structure.

#### What Happens if I Cancel?

If you cancel your enterprise subscription all your existing installed offline builders should work as before but you won’t be able to update them or get support for this.

#### When are Versions Released?

We will try to keep this in the same release pace as library updates i.e. once a week typically on a Friday.

#### Are Version Numbers Sequential?

They grow but we sometimes skip versions. Versions map to our cloud deployment versioning scheme and we might skip versions in some cases.

#### Why is this Feature Limited to Enterprise Subscribers?

This is a complex tool to support & maintain. SaaS has a well defined business model where we can reduce prices and maintenance costs.

Offline builds are more like a shrinkwrap business model in which case our pricing needs to align itself to shrinkwrap pricing models for long term sustainability.

The main use case this product tries to address is government and highly regulated industries who are in effect enterprise users.

#### How Different is the Code From Cloud Builds?

We use the same code as we do in the cloud build process with minor modifications in the process. Since the cloud servers are setup by us they work differently but should align reasonably well.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Sadart** — August 10, 2016 at 3:50 pm ([permalink](https://www.codenameone.com/blog/offline-build.html#comment-22936))

> Great work! This one rocks.
>



### **Gareth Murfin** — August 10, 2016 at 4:14 pm ([permalink](https://www.codenameone.com/blog/offline-build.html#comment-22683))

> Wow!!! This is amazing! One question, and I know the answer I think 🙂 But would it be possible to get this working using a mac in the cloud? I guess it would actually, if you simply install all the stuff on that mac, for building, and code locally then pull the code on the mac. Fantastic idea, also silences a lot of the people who say “no way im doing cloud builds”.. Can’t wait to try it.
>



### **Shai Almog** — August 11, 2016 at 4:43 am ([permalink](https://www.codenameone.com/blog/offline-build.html#comment-22654))

> I think it would be painful but we would be oblivious to the fact that a Mac is in the cloud. Notice that you won’t be able to use a guest account as this is bound to a machine.
>



### **Diamond** — August 30, 2016 at 8:40 am ([permalink](https://www.codenameone.com/blog/offline-build.html#comment-22750))

> Instead of going through the Mac-in-cloud installation process pains, why not just send a normal CN1 cloud build. Both will require internet connectivity and Mac-in-cloud build will be more difficult to execute. Also, Mac-in-cloud will require an additional space to install necessary pre-requisites and this won’t be free.
>



### **Gareth Murfin** — January 16, 2017 at 2:05 pm ([permalink](https://www.codenameone.com/blog/offline-build.html#comment-22899))

> Also, if you are building offline does that mean you can use cn1 for free?
>



### **Shai Almog** — January 17, 2017 at 5:26 am ([permalink](https://www.codenameone.com/blog/offline-build.html#comment-23068))

> Yes and No.  
> Just like you can use the source for free the enterprise offline build is free once you have it installed. Updates however, aren’t. So if you don’t need to update cn1 or get bug fixes/support then yes it’s free.
>



### **Khoi Minh Vo** — October 31, 2019 at 10:38 am ([permalink](https://www.codenameone.com/blog/offline-build.html#comment-24263))

> [Khoi Minh Vo](https://secure.gravatar.com/avatar/9bf0364bd5ab4a95f6f990ebafd68a26?s=80&d=identicon) says:
>
> Can UWP get offline build?
>



### **Shai Almog** — November 1, 2019 at 4:18 am ([permalink](https://www.codenameone.com/blog/offline-build.html#comment-24260))

> Shai Almog says:
>
> Yes. If an enterprise customer asks for it we’ll do it.
>



### **Julio Valeriron Ochoa** — September 30, 2022 at 4:48 pm ([permalink](https://www.codenameone.com/blog/offline-build.html#comment-24545))

> Julio Valeriron Ochoa says:
>
> Can I use offline build with your source code cn1 locally fro free?
>



### **Shai Almog** — October 1, 2022 at 2:06 am ([permalink](https://www.codenameone.com/blog/offline-build.html#comment-24547))

> Shai Almog says:
>
> No. But you can build offline for free with the maven build now: <https://www.codenameone.com/blog/moving-to-maven.html>
>



### **Julio Valeriron Ochoa** — September 30, 2022 at 4:51 pm ([permalink](https://www.codenameone.com/blog/offline-build.html#comment-24546))

> Julio Valeriron Ochoa says:
>
> For example imagine that I found a bug in your source code an I want to fix. I’t possible locally with your source code in my pc to modify and compile my cn1 source code?
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
