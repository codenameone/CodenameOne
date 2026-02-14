---
title: Moving to Maven
slug: moving-to-maven
url: /blog/moving-to-maven/
original_url: https://www.codenameone.com/blog/moving-to-maven.html
aliases:
- /blog/moving-to-maven.html
date: '2021-03-29'
author: Steve Hannah
description: Codename One is migrating to Maven. This will simplify some aspects of
  our build process and update/dependency management.
---

Codename One is migrating to Maven. This will simplify some aspects of our build process and update/dependency management.

As the headline states, we’re moving to Maven, and leaving the ant-infested old build system behind. To be honest, I love using Ant. It is quick and dirty, and let’s me assemble ad-hoc build workflows with little effort.

Migrating to Maven, at times, felt like strapping on a pair of cement boots. It is much more rigid than Ant, and much more opinionated. It’s the “Maven” way, or you’re in for a world of pain.

### Why Maven?

So, you may ask, if Maven is so opinionated and painful, why are we adopting it? The reason is simply because it is worth it. Maven’s rigidity encourages better project hygiene — Builds that “just work“, without having to read through a page of build instructions, or embarking on a dependency scavenger hunt. Once you agree to surrender some of the flexibility that Ant provided, you can begin to enjoy a better developer experience, all round.

Dependencies can be added as XML snippets to the pom.xml file rather than downloaded manually. And they can be removed, or upgraded to new versions just as easily.

The Maven eco-system is mature and diverse, providing all kinds of plugins to augment your build process. Maven’s rigidity helps all of these plugins work together as if on an assembly line.

Maven projects are also well supported by all major IDEs. Rather than maintaining a separate project type for each IDE, we can provide a single Maven project archetype that can be used by any IDE – or even no IDE if you prefer.

### New Features

As part of this move, we’ve included a few new features, some of which are simply by-products of using Maven.

## Local Builds

Due to popular demand, we have provided support for building iOS, Android, and Cross-platform JavaSE Desktop apps locally. i.e. No build server or Codename One account required. More on this in a future blog post, but you can read more about it [here](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html#_building_the_project).

[![Building JavaSE Desktop App](/blog/moving-to-maven/intellij-javase-desktop-app.png)](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html#javase)

## First-Class Kotlin Support

Kotlin support is now provided directly using the official Kotlin Maven plugin, and it is included into the Codename One Application archetype by default. Previously we had been providing Kotlin support via a cn1lib that included a Kotlin runtime, which we maintained ourselves by periodically updating it to newer versions of Kotlin.

For more information about developing Codename One apps in Kotlin, see [Getting Started with the Bare-bones Kotlin App Template](https://shannah.github.io/cn1app-archetype-kotlin-template/getting-started.html).

## Support for Jar Dependencies

You can now use regular Java jar dependencies in your projects, with the caveat that your build will fail if you use any APIs (transitively) that aren’t supported by Codename One. You can paste regular dependency snippets from Maven central into your projects. In the past, only cn1libs were supported, as they ensured compliance with the Codename One API.

This is made possible by improvements to our compile-time compliance check which incorporates Proguard to determine which classes and methods are used in your app, then throw an error if it identifies any unsupported APIs.

## Warning

> It isn’t yet clear how useful this "jar" support will be, as many Jar dependencies on Maven central are directed at server-side audiences and are fundamentally incompatible with Codename One. For this reason, I still prefer to use cn1libs.

## Publish your Libraries on Maven Central

You can now deploy your Codename One Library projects to Maven central so that others can use them by pasting a dependency snippet into the the pom.xml file of their Application projects. Cn1libs listed in Codename One Settings can now include a Maven dependency snippet which will be used when adding the library to a Maven project.

### A Small Change in the Workflow

The transition to Maven should be nearly seamless, but you will notice a few changes in the development workflow.

## The "Old" Ant Workflow

1

#### Install Codename One

2

#### Create New Project

3

#### Develop & Debug App

4

#### Add cn1libs

5

#### Create a Build

The old Ant workflow worked as follows:

1. Install the Codename One Plugin in your IDE via the “Install Plugin” mechanism in the IDE. We provide plugins for IntelliJ, NetBeans, and Eclipse.

2. Create a new **Codename One Application** project using the “New Project” option in your IDE.

3. Use the IDE’s editor to develop and debug your application.

4. Add cn1libs (plugins) to your project using **Codename One Settings**.

5. Use the **Codename One** menu in the IDE to perform tasks like building your project for iOS or Android.

## The New Maven Workflow

1

#### Codename One initializer

2

#### Open in IDE

3

#### Develop & Debug App

4

#### Add cn1libs

5

#### Create a Build

With the Maven transition, you no longer need to install a plugin in your IDE, since all of the Major IDEs know how to speak maven out of the box. Instead you can use our Maven project archetypes to create new Application or Library projects – or use the new [**Codename One initializr** tool](https://start.codenameone.com/) to generate a new project for you from a growing selection of Application templates. More on that later.

So the workflow (with IDE) becomes:

1. Generate a new project from an application template using [Codename One initializr](https://start.codenameone.com/).

2. Open the project in your preferred IDE.

3. Use the IDE’s editor to develop and debug your application.

4. Add cn1libs (plugins) to your project using **Codename One Settings** – ****or by pasting a regular Maven dependency snippet into your pom.xml file****.

5. Use the provided configuration options in the IDE to build the project for various platforms such as iOS, Android, etc…​

Alternatively, and additionally, everything can be done via the command-line, if you prefer to run, debug, and build your projects that way.

[![Codename One initializr](/blog/moving-to-maven/codenameone-initializr-screenshot.png)](https://start.codenameone.com/)

### Migrating Existing Projects

**That new workflow is all fine and dandy for new projects, but what about my existing Codename One application project that I’ve been working on for the past 5 years?**

You’re in luck. We’ve included a [migration tool](https://shannah.github.io/codenameone-maven-manual/#migrate-existing-project) in the maven plugin that will convert Ant projects into the new Maven project format. All you need to do is run a single command, and you’ll be all set up to use Maven, like the rest of the cool kids.

### Getting Started

Create your first Maven project right now using the [Codename One initializr](https://start.codenameone.com/) tool.

Read about the nuts and bolts in [the Codename One Maven Developers Guide](https://shannah.github.io/codenameone-maven-manual/).

Watch the video demo of [Codename One initializr](https://dev.to/shannah/online-tool-to-generate-ios-android-starter-project-k7h).
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **Javier Anton** — March 29, 2021 at 11:53 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24419))

> Javier Anton says:
>
> Please fix the sample code at <https://shannah.github.io/codenameone-maven-manual/#migrate-existing-project>
>
> There should be a space after each -D and there shouldn’t be backslashes after each line. I think that copying the command into HTML messed it up somehow
>



### **Steve Hannah** — March 29, 2021 at 11:59 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24420))

> Steve Hannah says:
>
> Looks correct to me. Those slashes escape the new line chars so it can be displayed on multiple lines. You can paste that directly into Mac or Linux. Or Windows even, if running bash.
>



### **Javier Anton** — March 30, 2021 at 7:42 am ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24421))

> Javier Anton says:
>
> Nevermind. I was running Powershell on Windows. In that case, what I said is required
>



### **Steve Hannah** — March 30, 2021 at 12:29 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24426))

> Steve Hannah says:
>
> Thanks. I’ll add a note in the docs about Windows.
>



### **Javier Anton** — March 30, 2021 at 9:48 am ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24422))

> Javier Anton says:
>
> After migrating from Ant to Maven, I had to add the maven android dependency to the pom.xml in order to compile my android native source:
>
> 29
>
> com.google.android  
> android  
> ${android.platform}  
> system  
> ${user.home}/.codenameone/android-${android.platform}/android.jar
>



### **Javier Anton** — March 30, 2021 at 9:49 am ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24423))

> Javier Anton says:
>
> seems the comments can’t contain xml
>



### **Javier Anton** — March 30, 2021 at 9:53 am ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24424))

> Javier Anton says:
>
> Actually, that was wrong. I still can’t compile because my new maven project doesn’t detect the android package of my native sources. And I can’t delete my previous comments, which makes this page look kind of ugly. Enough feedback for today! 😛
>



### **Steve Hannah** — April 1, 2021 at 12:23 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24436))

> Steve Hannah says:
>
> I see the issue. Add the following to the <build> section of your android/pom.xml file.  
> <sourceDirectory>src/main/empty</sourceDirectory>
>



### **Steve Hannah** — March 30, 2021 at 12:28 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24425))

> Steve Hannah says:
>
> You shouldn’t need to do that. The android native source doesn’t get compiled. It is sent to the build server as source. Are you getting an error message when you try to build?
>



### **Javier Anton** — March 30, 2021 at 7:44 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24427))

> Javier Anton says:
>
> Yes, I was getting an error message when building from Android. It started with it complaining about the android package. I know that this wasn’t happening before (Ant) and thought that it was now (Maven) required to use the Android SDK. Why could it be that my project is saying this after migrating to Maven?
>



### **Steve Hannah** — March 30, 2021 at 7:59 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24428))

> Steve Hannah says:
>
> What IDE are you using, or are you building from Command-line?  
> You should always build from the root project (not the sub-modules), and it will include appropriate sub-modules according to your flags.
>
> If building from command line, either use the build.sh (or build.bat) script, or see the maven commands executed by these scripts to see how to run the maven commands directly.
>
> If building from the IDE, use the preset build options.  
> For building Android using the build server (i.e. `mvn package -Dcodename1.platform=android -Dcodename1.buildTarget=android-device`) you do NOT need to have the android SDK installed.
>
> You would only need the android SDK installed if you were doing a local android build (e.g. `mvn package -Dcodename1.platform=android -Dcodename1.buildTarget=android-source`).
>



### **Javier Anton** — March 30, 2021 at 11:51 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24429))

> Javier Anton says:
>
> I removed my android sdk deps. Now I am stuck here (android sdk error no longer shows):
>
> “Failed to merge properties from library com.some.package:SomeApp-GoogleMaps. Property codename1.arg.ios.glAppDelegateHeader has a conflict”
>
> I use the GooglMaps cn1lib. My delegate’s header already contains something and is as follows:  
> \#include “com\_some\_something.h” \#import “com\_some\_more\_something.h”
>
> GoogleMaps cn1lib’s del header appends: \n\#import “GoogleMaps/GoogleMaps.h”
>
> If I remove my del. header’s build hint, the error goes away. But the same error then appears but complaining about the pods build hint. Is Maven having trouble merging build hints from cn1libs with the app’s?
>
> These errors happen both in PowerShell and in NetBeans


### **Steve Hannah** — March 31, 2021 at 12:14 am ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24430))

> Steve Hannah says:
>
> 1. Check the cn1.version property in your pom.xml file. If it’s not 7.0.17 start over (migrate the ant project again).  
> 2. If it is 7.0.17, then please describe the exact command you are running, and what output you are getting.


### **Javier Anton** — March 31, 2021 at 8:05 am ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24431))

> Javier Anton says:
>
> Checked, it’s 7.0.17.
>
> On PowerShell:
>
> mvn package -D codename1.platform=android -D codename1.buildTarget=android-device -e
>
> On NetBeans:
>
> 1. Select main project.  
> 2. Select “Android App” from the config dropdown.  
> 3. Press build or run
>
> I just tried “Clean and Rebuild” and now my project doesn’t detect Android native code anymore. I think I’ll have to re-run the migration later today and see if something changes


### **Javier Anton** — March 31, 2021 at 7:25 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24432))

> Javier Anton says:
>
> OK so I re-ran the migration and got to the same place.
>
> -Can compile local javase fine  
> -Can run simulator  
> -Can’t compile Android because it looks for android package in native sources and then it fails:  
> >”com/namespace/SomeActivity.java:[8,19] package android.app does not exist”  
> -Can’t compile iOS because it complains about build hints conflict as stated before:  
> >”Failed to merge properties from library com.groups:NewId-GoogleMaps. Property codename1.arg.ios.glAppDelegateHeader has a conflict”
>
> The command used for the migration is as follows (PowerShell):  
> SET CN1\_VERSION=7.0.17  
> mvn com.codenameone:codenameone-maven-plugin:${CN1\_VERSION}:generate-app-project -D archetypeGroupId=com.codename1 -D archetypeArtifactId=cn1app-archetype -D archetypeVersion=${CN1\_VERSION} -D artifactId=NewId -D groupId=com.my.namespace -D version=1.0-SNAPSHOT -D interactiveMode=false -D sourceProject=folderWithAndProject
>
> The migration says that all is successful
>
> Feel free to send me an email or anything, I can jump on a call too if required. Unless you can think of anything I will stop trying to fix this for now as I feel I have hit a dead end


### **Steve Hannah** — March 31, 2021 at 9:25 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24433))

> Steve Hannah says:
>
> I found an bug with the migration of cn1libs that include required library properties. It is fixed and will be part of the the 7.0.19 release. You’ll need to run migration again with that version number. That release has already been deployed to maven central, but it usually takes a few hours before it is available.
>



### **Javier Anton** — April 1, 2021 at 8:51 am ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24434))

> Javier Anton says:
>
> Some good news. With 7.0.19, iOS no longer complains about a build hint conflict and builds.  
> Android builds still try to find the android SDK for native sources and fail


### **Steve Hannah** — April 1, 2021 at 12:21 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24435))

> Steve Hannah says:
>
> I see the issue. Add the following to the <build> section of your android/pom.xml file.  
> <sourceDirectory>src/main/empty</sourceDirectory>
>



### **Javier Anton** — April 1, 2021 at 2:45 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24437))

> Javier Anton says:
>
> Nice one – it works
>



### **Eric Gbofu** — April 1, 2021 at 4:15 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24438))

> Eric Gbofu says:
>
> Hi Steve,
>
> When i use the project template downloaded from the Codename One initializr, everything works fine but when i want to do the same thing using the intellij idea project creation wizard to create a new project using the Codename One maven app archetype, it doesn’t work and i have the following error during the project generation process in batch mode
>
> Here is the error message :  
> Failed to execute goal org.apache.maven.plugins:maven-archetype-plugin:3.2.0:generate (default-cli) on project standalone-pom: Archetype com.codenameone:cn1app-archetype:LATEST is not configured  
> Property mainName is missing.
>
> Property mainName is missing. Add -DmainName=someValue
>
> PS: Maven is new for me.
>



### **Steve Hannah** — April 1, 2021 at 4:50 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24439))

> Steve Hannah says:
>
> Yes. That’s right. You need to add the mainName property. There are other properties you can add also, such as cn1Version (to set explicit codename one version).
>



### **Eric Gbofu** — April 1, 2021 at 5:06 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24440))

> Eric Gbofu says:
>
> Thanks Steve! I did it and it works.
>
> One last question. Will you remove completely the Codename One plugin in a near future to only adopt the maven project or you will modify it according to the new way to create projects ?
>
> Regards
>



### **Steve Hannah** — April 1, 2021 at 5:41 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24441))

> Steve Hannah says:
>
> I believe the plan is to deprecate the Codename One plugins. They will not be modified to support Maven. IDEs all have built-in support for Maven. We’ll be focusing on tailoring the application project archetype to work smoothly with each IDE. This will provide a more robust and consistent experience for developers across all IDEs.
>



### **Eric Gbofu** — April 1, 2021 at 9:43 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24442))

> Eric Gbofu says:
>
> Ok! Thanks for the clarification


### **Eric Gbofu** — April 1, 2021 at 11:29 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24443))

> Eric Gbofu says:
>
> About my question, i want to know if you can add the mainName property directly into the archetype so the developer will not have to add it manually every time that he wants to create a new project ?
>



### **Eric Gbofu** — April 1, 2021 at 11:30 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24444))

> Eric Gbofu says:
>
> About my first question, i want to know if you can add the mainName property directly into the archetype so the developer will not have to add it manually every time that he wants to create a new project ? Is there a problem to do it that way?
>



### **Raazia Tariq** — February 22, 2024 at 9:42 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24596))

> Raazia Tariq says:
>
> General error during conversion: Conflicting module versions. Module [groovy-all is loaded in version 2.4.16 and you are trying to load version 2.4.8..
>
> I got this error while converting an existing project.
>



### **Shai Almog** — February 23, 2024 at 3:02 am ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24597))

> Shai Almog says:
>
> Personally I found it’s easier and more reliable to create a new project using <https://start.codenameone.com> and then copying over the source/css/codenameone\_settings.properties on top of the new project (while removing cn1lib build hints). Then reinstalling the cn1libs.
>



### **Raazia Tariq** — February 23, 2024 at 4:16 pm ([permalink](https://www.codenameone.com/blog/moving-to-maven.html#comment-24598))

> Raazia Tariq says:
>
> I was able to convert the project successfully. Now, I am stuck on upgrading the project to support higher version of android and iOS. Is there any documentation regarding that?
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
