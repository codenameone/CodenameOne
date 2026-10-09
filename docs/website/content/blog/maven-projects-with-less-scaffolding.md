---
title: "Smaller Simpler Maven Projects"
slug: maven-projects-with-less-scaffolding
url: /blog/maven-projects-with-less-scaffolding/
date: '2026-10-15'
author: Shai Almog
description: "New Maven apps default to a root POM and common module. Build the same targets, keep native source directories, and add platform modules only when they need separate configuration."
feed_html: '<img src="https://www.codenameone.com/blog/maven-projects-with-less-scaffolding.jpg" alt="A smaller Maven project grows platform modules as needed" /> New Maven apps default to a root POM and common module. Build the same targets, keep native source directories, and add platform modules only when they need separate configuration.'
series: ["release-2026-10-09"]
---

![A smaller Maven project grows platform modules as needed](/blog/maven-projects-with-less-scaffolding.jpg)

Opening a new application should show you the application. A directory full of platform modules makes you learn the build's structure before you have written a screen.

[Last week we added smaller Gradle projects](/blog/gradle-smaller-projects/). Maven now starts small too. The application archetype defaults to the root POM and `common`, with no platform modules. You can still run the simulator and build the target apps. This closes our [weekly release series](/blog/android-apps-beyond-android/).

## The default tree matches the first task

The relevant part of a new project looks like this:

```text
my-app/
    pom.xml
    common/
        pom.xml
        codenameone_settings.properties
        src/main/java/
        src/main/resources/
```

This is a shortened tree, not a count of every generated resource or launcher script. The structural count is **two POMs**. Source, CSS and resources remain where an existing Maven user expects them.

When a platform has no module, `common` handles its build. These commands keep their meaning:

```bash
mvn cn1:run
mvn package -Dcodename1.platform=android
mvn package -Pexecutable-jar -Dcodename1.platform=javase
```

The desktop executable JAR goes under `common/target` in this layout. Platform modules become useful when a target needs independent dependencies or plugins, rather than because every possible target needs a directory on day one.

## A native implementation does not need another POM

`mvn cn1:generate-native-interfaces` still creates platform-specific source directories. An Android implementation can live under `android/src/main/java`, and an iOS implementation under `ios/src/main/objectivec`, without either directory becoming a Maven module.

The build treats a platform as a separate module when `<platform>/pom.xml` exists. Native source directories alone do not make that decision. JavaSE implementations are compiled into `common/target/cn1-javase/classes`, separate from application classes, so they are not accidentally uploaded into a device build.

{{< mermaid >}}
flowchart LR
    Common[Common application module] --> Android[Android build]
    Common --> IOS[iOS build]
    Common --> Desktop[Desktop build]
    Native[Native source directories] --> Common
    Separate[Optional platform POM] --> Custom[Separate platform configuration]
{{< /mermaid >}}

This also keeps the Android migration introduced on Friday approachable. Imported Android sources live under `common/src/main/android`; your initial port need not begin by learning every target module's POM.

## Ask for the structure you need

The archetype's `platformModules` property defaults to `none`. `all` restores the full platform layout, and a comma-separated value such as `javase,android` creates only those modules. Its `projectType` defaults to `app`; `app-with-backend` adds the server, and `backend-only` creates a standalone server project.

For example, these are **options to append to your normal archetype generation command**, not commands on their own:

```text
-DplatformModules=javase,android
-DprojectType=app-with-backend
```

A backend-only project has its POM and `src/main/java` at the root. It needs no empty client beside it:

```bash
./mvnw cn1:backend
./mvnw cn1:backend-package
```

The minimal default applies to newly generated projects using the updated archetype. It does not delete modules from an existing checkout. Before simplifying an older project, check for platform-specific build configuration rather than assuming its POMs are empty scaffolding.

## Less structure to explain and inspect

I still prefer Maven's explicit XML for build configuration. I also prefer having less of it to read. Those preferences can coexist.

A smaller project tree helps a new contributor find the code that matters. It also leaves fewer generated configuration files for an LLM to misread when asked to change one screen. That is a reduction in surrounding material, not a guarantee that generated code is correct. Review the diff and run the relevant application behavior just as before.

[PR #5937](https://github.com/codenameone/CodenameOne/pull/5937) introduced these project shapes. The current [archetype reference](/developer-guide/#maven-minimal-layout) defines how target selection and native directories work. Start with the default, then add a platform module when you have a concrete setting to put in it.

## Discussion

_How many of your platform POMs contain configuration you actually maintain?_

{{< giscus >}}
