---
title: Maven Project Structure
slug: maven-project-structure
url: /blog/maven-project-structure/
original_url: https://www.codenameone.com/blog/maven-project-structure.html
aliases:
- /blog/maven-project-structure.html
date: '2021-04-12'
author: Steve Hannah
description: As a follow-up to our recent announcement about transitioning to Maven,
  this post provides an overview of the new project structure.
---

As a follow-up to our recent announcement about transitioning to Maven, this post provides an overview of the new project structure.

## Tip

> If you want to follow along with this tutorial, you can [quickly generate a new project](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html#generating-new-project) using [Codename One intializr](https://start.codenameone.com/).

The new project structure is based on the [cn1app-archetype](https://github.com/shannah/cn1-maven-archetypes/tree/master/cn1app-archetype). You can see the basic file layout by looking at the [archetype-resources](https://github.com/shannah/cn1-maven-archetypes/tree/master/cn1app-archetype/src/main/resources/archetype-resources) directory of that project.

## Some Highlights:

****1. This is a multi-module project**** with sub modules for each target platform.
  
  
2. It comes bundled with a ****maven wrapper script****, and utility shell (and bat) scripts to facility command-line usage.
  
  
3. The includes NetBeans and IntelliJ configuration files to provide better integration with those IDEs.
  
  
****4. The “common” module contains all of the cross-platform stuff****, and is the most direct successor to the old Ant project structure.

i. codenameone\_settings.properties is found in the common module.
  
  
       ii. CSS files are in common/src/main/css.
  
  
       iii. Java files are in common/src/main/java.
  
  
       iv. Resources are in common/src/main/resources.
  
  
       v. Kotlin files are in common/src/main/kotlin.
  
  
       vi. GUIBuilder files are in common/src/main/guibuilder.
  
  
       vii. Unit tests are in common/src/test/java.

****5. CSS By Detault****. Projects are set to use CSS by default. You can edit the styles in common/src/main/css/theme.css.
  
  
6. You can install cn1lib dependencies either via a Maven <dependency> snippet in your common/pom.xml file, or using **Codename One Settings**. You can Also use the [install-cn1lib goal](https://shannah.github.io/codenameone-maven-manual/#_install_legacy_cn1lib_install_cn1lib).

## Creating a New Project

The easiest way to create a new project is using [Codename One initializr](https://start.codenameone.com/). This will allow you to generate and download a starter project that is ready to run, debug, and build.

### Modules

Let’s now go through each module in the project and discuss its purpose.

## common

A Codename One application. All of your cross-platform application code goes in this module.

## android

Module containing native Android code such as native interface implementations.

## ios

Module containing native iOS code, such as native interface implementations.

## javase

Module containing native JavaSE code, such as native interface implementations.

## javascript

Module containing native javascript code for the Javascript port.

## win

Module containing native Windows UWP code for the UWP windows port.

## cn1libs

Module where legacy cn1libs will be installed the cn1:install-cn1lib goal.

### Shell Scripts

The archetype project includes run.sh and build.sh shell scripts, as well as their Windows counterparts run.bat and build.bat. These scripts are convenience wrappers for common commands that you may wish to perform on the Command-line. e.g.

## Running project in the Codename One Simulator

```bash
				
					./run.sh

				
			
```

## Building a cross-platform JavaSE desktop app locally

```bash
				
					./build.sh

				
			
```

## Generating Xcode project locally

```bash
				
					./build.sh ios_source
# project will be generated in ios/target directory
				
			
```

## Generating Android Studio project locally

```bash
				
					./build.sh android_source
# project will be generated in android/target directory.
				
			
```

## Building iOS app using build server

```bash
				
					./build.sh ios

				
			
```

## Building Android app using build server

```bash
				
					./build.sh android

				
			
```

## Opening Codename One Settings

```bash
				
					./run.sh settings

				
			
```

## Updating Codename One

```bash
				
					./run.sh update
				
			
```

## Tip

> The run and build scripts are very thin wrappers over mvn. Open these scripts in your text editor to see the exact mvn commands that they run.

### The Raw Maven Commands

The Codename One Maven plugin includes several goals, some of which are executed internally at the appropriate phase of the Maven lifecycle. Others are meant to be executed by you. Typically, you would run the Maven goals in the root module.
  
  
By default, only the “common” module is activated. You can activate other modules by specifying them with the codename1.platform property. e.g. If you wanted to build the javase module, you would do:

```java
				
					mvn package -Dcodename1.platform=javase

				
			
```

If, instead, you wanted to build for iOS, you would do:

```java
				
					mvn package -Dcodename1.platform=ios

				
			
```

## Tip

> The codename1.platform property, if supplied, should correspond to the names of the platform-specific module projects. i.e. It supports values "javascript", "javase", "ios", "android", and "win".

When performing builds (e.g. mvn package), you may also need to specify which build target you wish to use, as most platforms have more than one build target. For example the javase platform is used for the simulator, Mac desktop apps, Windows desktop apps, and JavaSE desktop apps. You should use the codename1.buildTarget property to differentiate.

e.g. To build a Mac Desktop app you would run:

```java
				
					mvn package -Dcodename1.platform=javase \
  -Dcodename1.buildTarget=mac-os-x-desktop
				
			
```

And to build for Windows Desktop, you would run:

```java
				
					mvn package -Dcodename1.platform=javase \
  -Dcodename1.buildTarget=windows-desktop
				
			
```

## Why 2 Properties?

You might be wondering why we need to include both codename1.platform and codename1.buildTarget properties explicitly. The codename1.buildTarget property should correspond to a unique codename1.platform value (e.g. codename1.buildTarget=mac-os-x-desktop implies codename1.platform=javase), so why can’t we just omit the codename1.platform property and let the build system “figure it out”? Indeed!

The reason is because Maven doesn’t allow you to do this in a way that will support our needs. The codename1.platform property is used for more than just activating the correct module. It is used by cn1lib projects that are listed as dependencies also to include the correct artifacts for the current build. If this property isn’t provided on the command-line, then it won’t be available early enough in the Maven reactor process to activate all of the needed modules and artifacts.

If you know a clever solution to this issue, I’m all ears. My solution was to just provide the build.sh and build.bat scripts to wrap the common goals and include the needed properties at that level.

Check out the [build.sh script source](https://github.com/shannah/cn1-maven-archetypes/blob/00729f9f03b1952eafcd425abbb6a64879242a89/cn1app-archetype/src/main/resources/archetype-resources/build.sh) for a list of the most-common build commands.

### In the IDE

As mentioned above, we’ve done some work to add better integration than default to IntelliJ and NetBeans. If you open the project in IntelliJ, the Configuration menu will include options for all of the common commands including building the project for each target platform, running in the simulator, opening Codename Settings, and updating Codename One.

NetBeans includes similar support, and we plan to add “special” support for Eclipse and VSCode in the near future.

See [Getting Started with the Bare-bones Java App Project](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html) for more in-depth coverage of each IDE including screenshots.

### Where to go from here

Our Maven support represents months of careful work, and there is much more that could be discussed. Over the coming weeks I’ll be writing more blog posts on various aspects of this support. In the mean time, you can check out the [Getting Started tutorial](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html) and the [Codename One Maven Developer Guide](https://shannah.github.io/codenameone-maven-manual/) for a deeper dive.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **Eric Gbofu** — April 13, 2021 at 6:01 pm ([permalink](/blog/maven-project-structure/#comment-24445))

> Eric Gbofu says:
>
> Hello
>
> I have a few things to report about Maven support
>
> 1) Normally, a Kotlin project accepts a mix of Java and Kotlin files. With the old Ant support, I could mix \* .kt and \* .java files in the same project and everything worked fine. With this new Maven support, when I mix these two file formats in a kotlin project, the first run goes fine and everything displays fine, but when I make changes to the \* .java file and re executes the code, the simulator starts but the modifications are applied. Any modification that is done after the first execution in the java file is not applied at all. With Ant, this problem was not present. What is the cause?
>
> 2) With the previous Ant build system, we can see methods documentation in the IDE. With Maven, this documentation is no longer displayed. Can you fix that please ?
>
> 3) In the documentation you show how to create CN1 maven project via the command line and Codename One initializer. Can you also add in the documentation how to do it directly via the IDE without going through the two previous options?
>
> Thanks
>



### **Eric Gbofu** — April 13, 2021 at 10:00 pm ([permalink](/blog/maven-project-structure/#comment-24446))

> Eric Gbofu says:
>
> Answers for my previous questions: <https://github.com/codenameone/CodenameOne/issues/3390>
>



### **Antonio Rios** — March 20, 2022 at 3:49 pm ([permalink](/blog/maven-project-structure/#comment-24512))

> Antonio Rios says:
>
> Excellent post Steve!
>
> Thanks for all this great information. I completed watching the introductory free course from the academy and Shai told me that you guys are now using maven and css instead of old way with the IDE plugins. So I follow this instructions and the developer guide to try the new way of creating projects with maven. I ran into a couple of problems that took me some time to figure out since I’m new to this. If it helps I’m using OpenJDK 17 on puppy linux and vim as my lightweight IDE and running everything in the terminal. I’m really impress how well everything works on my 7 year old laptop.
>
> I used the Bare-bones Java App with the option “Other” for the IDE maven starter project. When I tried to run the project the simulator would not launch because the ‘MaxPermSize=128M’ VM option was unrecognized. I was able to fix this problem by commenting out line 612 from the pom.xml file in the javase folder.
>
> ”  
> [INFO] — exec-maven-plugin:3.0.0:exec (run-in-simulator) @ learningapp-javase —  
> Unrecognized VM option ‘MaxPermSize=128M’  
> Error: Could not create the Java Virtual Machine.  
> Error: A fatal exception has occurred. Program will exit.  
> [ERROR] Command execution failed.  
> org.apache.commons.exec.ExecuteException: Process exited with an error: 1 (Exit value: 1)  
> ”
>
> The second problem I had was when trying to open the settings by using the command run.sh settings. I kept getting an error message stating that no plugin was found for prefix ‘cn’ in the current project and in the plugin groups. Eventually I figured out the problem was in the run.sh file so I had to append a 1 to cn on line 14 so it would call “cn1:settings”.
>
> ”  
> [ERROR] No plugin found for prefix ‘cn’ in the current project and in the plugin groups [org.apache.maven.plugins, org.codehaus.mojo] available from the repositories [local (/root/.m2/repository), central (<https://repo.maven.apache.org/maven2>)] -> [Help 1]  
> “
>



### **Steve Hannah** — March 21, 2022 at 11:48 am ([permalink](/blog/maven-project-structure/#comment-24513))

> Steve Hannah says:
>
> Thanks for sharing this. I have made these changes to the archetype so that they will be part of the next update (7.0.62). Officially we support JDK8 and JDK11, not JDK17 yet, which is the reason for this failure. (i.e. an alternate fix for the first error would have been to change to JDK11).
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
