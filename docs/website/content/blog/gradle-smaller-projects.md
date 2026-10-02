---
title: "Do You Prefer Gradle?"
slug: gradle-smaller-projects
url: /blog/gradle-smaller-projects/
date: '2026-10-05'
author: Shai Almog
description: "Optional Gradle projects remove empty platform modules, support standalone backends, and use the same build engine as Maven."
feed_html: '<img src="https://www.codenameone.com/blog/gradle-smaller-projects.jpg" alt="A smaller project tree with an optional Gradle build" /> Optional Gradle projects remove empty platform modules, support standalone backends, and use the same build engine as Maven.'
series: ["release-2026-10-02"]
---

![A smaller project tree with an optional Gradle build](/blog/gradle-smaller-projects.jpg)

You can now choose Gradle for a Codename One project. Start with fewer files, add platform directories when you need native code, or create a standalone backend without a mobile app attached. A smaller project is easier to browse and leaves less scaffolding competing for space in an LLM's context.

I like Maven XML. I know it is verbose. I also spent enough time at Sun Microsystems staring at deeply scripted, heavily patched `make` files to develop a lasting suspicion of clever build systems.

Someone starts with a reasonable shortcut. Someone else needs one more condition. A few years later the build has its own undocumented programming language and nobody wants to touch it. I would rather a build system be strict and limited. Give people enough power and, with the best intentions, they build a Frankenstein build.

That is a personal preference. Plenty of developers I respect love Gradle, or at least appreciate what it lets them do. This week we added [optional Gradle support](https://github.com/codenameone/CodenameOne/pull/5922) because it lets us offer something useful: a smaller Codename One project, including a standalone backend with no client scaffolding.

## Start with fewer files

The Maven app layout has a root POM and platform modules ready for native code. That makes the structure explicit, but a new app often has no code in most of those places.

A Gradle app starts with the sources and configuration it needs. Native directories appear when you generate native interfaces. A backend subproject appears when you add a backend.

For a concrete example, we converted the checked-in HelloCodenameOne sample with the current converter:

| Project files | Maven sample | Converted Gradle sample |
| --- | ---: | ---: |
| Total files, including app sources and resources | 358 | 305 |
| Maven POMs | 9 | 0 |
| Gradle Kotlin configuration files | 0 | 2 |

That removes **53 files, about 15% of the tree**. We counted the tracked sample before conversion and all files in the converter's new directory afterward, excluding build output and caches. This is the existing demonstration app, with its resources and Kotlin source, rather than a comparison of two different Hello World programs.

The generated `settings.gradle.kts` is small enough to show in full, with its explanatory comments removed:

```kotlin
pluginManagement {
    repositories {
        maven("https://repo.codenameone.com/maven2")
        gradlePluginPortal()
    }
}
plugins {
    id("com.codenameone") version "8.0-SNAPSHOT"
}
rootProject.name = "HelloCodenameOne"
```

The sample conversion uses the development version. Select the release containing this support when it is available. The app's own dependencies and Kotlin plugin go in `build.gradle.kts`; the framework's platform builders come from the Codename One plugin.


```text
MyApp/
  settings.gradle.kts
  gradle.properties
  gradlew
  gradle/wrapper/
  codenameone_settings.properties
  icon.png
  src/main/java/
  src/main/css/theme.css
  src/main/resources/
```

Add `build.gradle.kts` when you have dependencies or build configuration of your own. Kotlin source belongs in `src/main/kotlin` when the project applies the Kotlin plugin.

That smaller tree helps a human understand an unfamiliar project. It can also reduce the irrelevant files an agent sees when building its context. The file reduction is measurable; the context saved depends on which files your agent reads.

## Keep the same builds when you switch

We did not implement a second set of platform builders. The Gradle plugin calls the shared engine used by Maven. That includes upload staging, local builders, CSS compilation and bytecode compliance checks.

{{< mermaid >}}
flowchart TD
    Maven[Maven plugin] --> Engine[Shared Codename One build engine]
    Gradle[Gradle plugin] --> Engine
    Engine --> CSS[Compile CSS and check bytecode]
    Engine --> Local[Generate local platform projects]
    Engine --> Upload[Stage cloud build upload]
{{< /mermaid >}}

From a generated Gradle app:

```bash
./gradlew run
./gradlew cn1Test
./gradlew buildJavascriptLocal
./gradlew buildIosXcodeProject -Popen=false
```

`run` starts the simulator. `cn1Test` uses the simulator test runner. The last two commands build the JavaScript port locally and generate an Xcode project. Tasks that submit cloud builds are available too, with the same service requirements as Maven.

There are limits. Running Gradle requires JDK 17 or newer and Gradle 8.5 or newer; the generated wrapper supplies Gradle. These projects target Java 17. They do not support legacy `.cn1lib` files or the old resource Designer task. The OpenAPI, gRPC and GraphQL generators and on-device debugging helpers still have Maven goals without equivalent Gradle tasks. The [workflow chapter](/developer-guide/gradle-project-workflow/) lists the supported surface.

## Build a standalone Java backend

In the [Initializr](/initializr/), choose **Gradle** as the build tool and **Backend only** as the project type. The server lives at the root, with its Java source and application properties. There is no client theme, simulator module or collection of empty native directories.

For a generated project named `MyService`:

```bash
CN1_PROFILE=dev ./gradlew runBackend
```

That runs the development server on the JVM. Stop it before changing the backend code; this is a restart workflow, not hot reload. To build and run the native executable:

```bash
./gradlew backendPackage
CN1_PROFILE=dev PORT=9000 ./build/MyService
```

The development profile uses the generated in-memory SQLite configuration. Without it, the starter reads its production settings and expects `DATABASE_URL`. The executable's name follows the project name.

An existing Gradle app can gain a server with:

```bash
./gradlew addBackend
CN1_PROFILE=dev ./gradlew :backend:runBackend
./gradlew :backend:backendPackage
```

I hope people will try the backend on its own. It is still experimental, but a small independent service can reveal useful constraints without requiring a mobile application first. The [weekly overview](/blog/java-server-work-before-startup/) shows the Spring-style programming model it now supports.

## Try Gradle alongside your Maven project

The converter leaves the original project in place and writes a sibling project. Replace `VERSION` with the Codename One plugin release containing this feature:

```bash
mvn com.codenameone:codenameone-maven-plugin:VERSION:convert-to-gradle \
  -Dcn1.sourceProject=/path/to/MyApp \
  -Dcn1.outputDir=/path/to/MyApp-gradle
```

The destination must be empty or absent. The conversion carries sources, resources, CSS and platform implementations into the new layout, and raises the application's bytecode target to Java 17. A customized backend comes across; the untouched backend skeleton is optional. Legacy `.cn1lib` dependencies stop conversion with their names rather than silently disappearing.

Open the converted project and run its tests before switching the team's build. Your old checkout remains available for comparison.

## Find the API for the code you are writing

Last week we split the Developer Guide into chapters. Before that, we rebuilt the Javadocs. A backend-only project made the next problem obvious: a client API page is the wrong answer when you are writing a server.

[PR #5918](https://github.com/codenameone/CodenameOne/pull/5918) separates the [client API](/javadoc/) from the [backend API](/backend/javadoc/). The references label their audience and link shared classes to their counterpart. Backend classes belong in the backend reference; a UI implementation package does not belong there just because it exists in the repository.

| What you are writing | Reference to start with |
| --- | --- |
| A form, component or device integration | [Client Javadocs](/javadoc/) |
| A controller, database service or scheduled job | [Backend Javadocs](/backend/javadoc/) |
| Shared models and mapping code | The shared class page, with its counterpart link |

The distinction helps readers and code-generation tools for the same reason: it reduces the chance of choosing an API that exists in the wrong runtime. Both references are checked with `doclint`, so malformed documentation has a build-time consequence too.

## Try Gradle 9 for your Android build

The optional Gradle project layout and the Gradle version used inside a generated Android project are different choices. You can keep your Codename One app on Maven and still test the newer Android builder.

In Settings, under **Build Hints**:

```properties
android.gradleVersion=9
```

Or in `codenameone_settings.properties`:

```properties
codename1.arg.android.gradleVersion=9
```

The current builder pairs Gradle 9.8.0 with Android Gradle Plugin 9.4.1. It adjusts the generated project for removed DSL elements and AGP's built-in Kotlin support. This selects the experimental Gradle 9 / Android Gradle Plugin 9 path from [PR #5919](https://github.com/codenameone/CodenameOne/pull/5919). It is opt-in. Much of the surrounding tooling and library configuration still assumes Gradle 8. Test your actual native dependencies and generated Android project before adopting it; a successful empty app is not the same test.

I expect to keep using Maven for plenty of projects. Supporting Gradle gives teams another way into the same build machinery, and gives a small server a project tree that looks like a small server.

---

## Discussion

_How much freedom do you want a build system to give the next person maintaining it?_

{{< giscus >}}
