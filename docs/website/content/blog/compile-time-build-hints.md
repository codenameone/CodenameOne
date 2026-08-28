---
title: "Build Hints That Fail Before the Build Server"
slug: compile-time-build-hints
url: /blog/compile-time-build-hints/
date: '2026-09-02'
author: Shai Almog
description: "Codename One now exposes 87 common build hints as Java annotations, giving the compiler enough information to reject misspelled names, wrong types, and unsupported values before a build reaches the server."
feed_html: '<img src="https://www.codenameone.com/blog/compile-time-build-hints.jpg" alt="Build hint annotations passing through the Java compiler before reaching the Codename One build server" /> Codename One now exposes 87 common build hints as Java annotations, giving the compiler enough information to reject misspelled names, wrong types, and unsupported values before a build reaches the server.'
series: ["release-2026-08-28"]
---

![Build hint annotations passing through the Java compiler before reaching the Codename One build server](/blog/compile-time-build-hints.jpg)

A bad configuration line can be worse than a failed build. If the build stays green while ignoring the setting, you usually discover the mistake in a generated native project or on a device.

[PR #5586](https://github.com/codenameone/CodenameOne/pull/5586) moves 87 common Codename One build hints into Java's type system. A misspelled hint is now an unknown annotation attribute. A wrong value type or unsupported option stops at compilation instead of traveling to the build server as an inert string.

This change is part of the [native windows weekly release](/blog/native-desktop-windows/). It is available through the Codename One Maven repository, so existing projects need the repository and plugin repository entries shown at the top of that post.

## Why build hints existed in the first place

The original Codename One build server changed frequently. We needed to expose a new Android, iOS, or packaging switch without waiting for every IDE plugin to ship an updated settings screen.

Build hints separated those two release clocks. The plugin could send an open-ended map of strings. A builder could start reading a new key immediately:

```properties
codename1.arg.<name>=<value>
```

That escape hatch still does useful work more than a decade later. It lets a CN1Lib contribute native dependencies and lets a project tune low-level output without a new plugin protocol for every option.

The cost is that neither Java nor the plugin knew what most of those strings meant. The prefix confused people. Names and capitalization had to be exact. Values with commas, semicolons, or XML had their own merge rules. A name that no builder read was accepted, uploaded, and discarded.

This line looks plausible:

```properties
# Accepted by the old path, but ignored by the Android builder
codename1.arg.android.minSdkVersion=24
```

The builder reads `android.min_sdk_version`, not `android.minSdkVersion`. The project asked for API 24, the build succeeded, and the request used the builder's default.

## The compiler now checks the common path

The same setting can live on the application's main class:

```java
import com.codename1.annotations.buildhints.Android;
import com.codename1.annotations.buildhints.AndroidMinSdk;
import com.codename1.annotations.buildhints.Build;
import com.codename1.annotations.buildhints.DesktopBuild;
import com.codename1.annotations.buildhints.DesktopTitleBar;
import com.codename1.annotations.buildhints.Ios;
import com.codename1.annotations.buildhints.ThemeMode;
import com.codename1.annotations.buildhints.Toggle;
import com.codename1.system.Lifecycle;

@Android(
        themeMode = ThemeMode.MODERN,
        minSdkVersion = AndroidMinSdk.API_24)
@Build(nativeTheme = ThemeMode.MODERN)
@DesktopBuild(
        titleBar = DesktopTitleBar.NATIVE,
        width = 1280,
        height = 800)
@Ios(
        themeMode = ThemeMode.MODERN,
        newStorageLocation = Toggle.ON,
        pods = {"Intercom", "AFNetworking"})
public class MyApplication extends Lifecycle {
}
```

`minSdkVersion` takes `AndroidMinSdk`, so an integer does not compile. `titleBar` takes `DesktopTitleBar`, so autocomplete shows the accepted values and a made-up constant fails at the same line.

Seven annotations cover the common groups: `@Ios`, `@Android`, `@DesktopBuild`, `@Build`, `@Hardening`, `@IosPrivacy`, and `@OnDeviceDebug`. Ten enums describe closed value sets. Open-ended values remain strings.

{{< mermaid >}}
flowchart TD
    subgraph Old[Properties-only path]
        P[codename1.arg name and string value] --> R[Build request]
        R --> K{Does a builder read this exact key?}
        K -->|Yes| U[Apply value]
        K -->|No| S[Green build, setting ignored]
    end

    subgraph New[Compiler-checked path]
        A[Annotation on main class] --> J{javac checks attribute, type, and enum}
        J -->|Invalid| F[Compile error at the declaration]
        J -->|Valid| M[process-annotations]
        M --> W[The same name and string wire format]
        W --> B[Existing builder]
    end
{{< /mermaid >}}

The builders did not need a replacement protocol. `BuildHintAnnotationProcessor` converts the checked declaration into the same key and string value they already consume. Old projects, older framework versions, and the build service keep speaking the existing wire format.

## An unset annotation does not freeze a server default

The build service still needs room to change. An Android packaging default that made sense ten years ago might be wrong for the current Play Store.

That is why no annotation attribute has a meaningful default. An empty string, an empty array, zero, `Toggle.DEFAULT`, or an enum's unset marker all mean that the application said nothing. The processor emits no hint, leaving the current builder default in control.

There are no `boolean` attributes. Consider an `appBundle` attribute with a Java default of `false`. The Android builder currently defaults that hint to `true`. Compiling `false` into every application would silently override the server even when the developer never selected it. `Toggle.ON`, `Toggle.OFF`, and `Toggle.DEFAULT` preserve all three states.

This keeps the reason build hints existed. The server can evolve without requiring a client release for every default change. The difference is that an explicit value can now be checked.

## Arrays carry the merge rule

Complex values exposed another weak point in the string form. Some lists use commas, some use semicolons, and a CN1Lib may append its own entry to the application's value.

An annotation expresses a hint that accepts library additions as an array:

```java
@Ios(pods = {"Intercom", "AFNetworking"})
```

The hint metadata records the wire key, separator, and whether a CN1Lib may append. The processor and library merger use that metadata instead of maintaining separate hand-written separator lists.

{{< mermaid >}}
flowchart LR
    A[Main-class annotations] --> C[Compiler checks]
    C --> M[Generated hint manifest]
    P[Properties-only long tail] --> G[Catalog-aware merge]
    M --> G
    L[CN1Lib contributions] --> G
    G --> O[Command-line overlay]
    O --> R[Existing build request]
    R --> B[Unchanged builders]
{{< /mermaid >}}

A command-line `-Dcodename1.arg.<name>=...` override still wins. A CN1Lib can still append to a hint whose metadata permits library additions. Declaring the same hint in an annotation and `codenameone_settings.properties` is different: the build stops and names the duplicate rather than guessing which source you intended.

## One catalog replaces five drifting descriptions

The old hint set was partially restated in the developer guide, the Settings tool, the simulator, the Maven plugin's separator map, and an agent reference. Only 147 of roughly 520 names appeared in more than one of those places.

The annotations are now the source of truth for the 87 hints they expose. The remaining hints live in `maven/build-hint-catalog`, including dynamic families and build-service-only settings. Together they describe all 497 hints read by the builders. The generated developer guide table now contains 577 rows with type, default, and annotation columns.

The Settings tool reads the catalog instead of scraping an AsciiDoc table and guessing types from prose. It validates closed value sets and identifies hints owned by an annotation, so it cannot add a duplicate property behind your back.

![The Codename One Settings build-hint editor](/blog/standalone-codename-one-settings/settings-build-hints.png)

_The properties editor remains available for the long tail. Its schema now comes from the same catalog as the guide and compiler tooling._

Two CI gates protect the catalog. One fails when framework code reads a hint the catalog does not describe, or when our documentation names a hint no builder reads. The second renders the annotation-backed editor and guide data to prove generation still produces the complete set.

## Migrating an existing project

After updating the project POM and Codename One version, run:

```shell
mvn cn1:migrate-build-hints
```

The goal moves every property backed by an annotation when it can reproduce the value faithfully. That includes open-ended strings such as `facebook.appId`, `gcm.sender_id`, and `ios.teamId`. Hints without annotations, including dynamic families, stay in `codenameone_settings.properties`; values that cannot be translated without changing their meaning stay there too. The goal then runs the annotation processing path and verifies that every moved setting reached the generated manifest. If verification fails, it restores both files.

Older projects might not bind `process-annotations` to the module that compiles the main class. The build refuses to ignore the annotations and reports the missing execution. Add this to the Codename One Maven plugin configuration when needed:

```xml
<execution>
    <id>cn1-process-classes</id>
    <phase>process-classes</phase>
    <goals>
        <goal>process-annotations</goal>
    </goals>
</execution>
```

Some settings must remain properties. `codename1.arg.java.version` chooses the compiler that must compile the annotated class, so reading it from that class would be too late. Dynamic families such as `android.permission.<NAME>` also stay in the file because an annotation cannot expose an arbitrary map key.

## Compile failures are the compatibility policy

The annotation form intentionally does not promise source compatibility for build hints. If a weekly release renames an attribute, changes its type, or removes an accepted value, the project stops compiling at the declaration. That is the desired failure mode.

The build service still accepts the old string protocol. An already-built project keeps building. If an annotation disappears and you need the previous spelling, the properties form remains available.

This matters most for settings that affect more than appearance. `@Hardening` and `@IosPrivacy` now make common security and privacy configuration visible to the compiler. A security setting that is misspelled is no security setting at all.

Start with `mvn cn1:migrate-build-hints`, review the generated annotations, and keep the properties that the goal leaves behind. The split is deliberate: checked declarations for the stable, common path and strings where the build still needs an open-ended escape hatch.

---

## Discussion

_Which build hint has been the easiest one to mistype or misunderstand in your projects?_

{{< giscus >}}
