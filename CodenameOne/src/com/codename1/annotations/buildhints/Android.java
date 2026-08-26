/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.annotations.buildhints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Android build hints, checked by the compiler.
///
/// Place this on your application's main class -- the class named by
/// `codename1.mainName`. An attribute you do not set is not written at all, so
/// the builder's own default applies. Each attribute's `@Hint(def)` records
/// what that default is; the `default` clause below it is a neutral placeholder
/// with no meaning at runtime.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Android {

    @Hint(name = "android.activity.launchMode",
            def = "singleTop",
            platform = "android",
            doc = "Allows explicitly setting the `android:launchMode` attribute of the main activity in android. Default is \"singleTop,\" but for some applications you may need to change this behaviour. In particular, apps that are meant to open a file type will need to set this to \"singleTask.\" See https://developer.android.com/guide/topics/manifest/activity-element.html[Android docs for the activity element] for more information about the `android:launchMode` attribute.",
            consumedBy = {"AndroidGradleBuilder"})
    String activityLaunchMode() default "";

    @Hint(platform = "android",
            doc = "Produces an Android App Bundle (.aab) rather than an APK. Required for new Play Store submissions.",
            consumedBy = {"AndroidGradleBuilder"})
    boolean appBundle() default false;

    @Hint(kind = HintKind.VERSION,
            platform = "android",
            doc = "Android build-tools version. It also selects the compile SDK, so there is no separate compile-SDK hint.",
            consumedBy = {"AndroidGradleBuilder"})
    String buildToolsVersion() default "";

    @Hint(def = "enabled",
            platform = "android",
            doc = "Indicates whether the `RECORD_AUDIO` permission should be requested. Can be `enabled` or any other value to disable this option",
            consumedBy = {"AndroidGradleBuilder"})
    String captureRecord() default "";

    @Hint(def = "false",
            platform = "android",
            doc = "true/false defaults to true - indicates whether to include the debug version in the build. Defaults conditionally rather than to a fixed value: when android.release is on it defaults to false, and when release is off it defaults to true, so a build that selects neither still produces something installable (AndroidGradleBuilder.java:447-451).",
            consumedBy = {"AndroidGradleBuilder"})
    boolean debug() default false;

    @Hint(def = "false",
            platform = "android",
            doc = "Turns off R8, falling back to the older shrinker. Note that hardening requires R8, so this conflicts with harden.level.",
            consumedBy = {"AndroidGradleBuilder"})
    boolean disableR8() default false;

    @Hint(def = "true",
            platform = "android",
            doc = "Boolean true/false defaults to true. Allows disabling the proguard obfuscation even on release builds, notice that this isn't recommended",
            consumedBy = {"AndroidGradleBuilder"})
    boolean enableProguard() default false;

    @Hint(appendable = true,
            separator = ";",
            platform = "android",
            doc = "Gradle dependency statements to add to the app module, such as implementation 'com.example:lib:1.0'.",
            consumedBy = {"AndroidGradleBuilder"})
    String[] gradleDep() default {};

    @Hint(def = "false",
            platform = "android",
            doc = "Hides the Android status bar.",
            consumedBy = {"AndroidGradleBuilder"})
    boolean hideStatusBar() default false;

    @Hint(def = "auto",
            platform = "android",
            doc = "Maps to android:installLocation manifest entry defaults to auto. Can also be set to internalOnly or preferExternal.",
            consumedBy = {"AndroidGradleBuilder"})
    InstallLocation installLocation() default InstallLocation.AUTO;

    @Hint(platform = "android",
            doc = "The license key for the Android app, this is required if you use in-app purchase on Android",
            consumedBy = {"AndroidGradleBuilder"})
    String licenseKey() default "";

    @Hint(name = "android.min_sdk_version",
            def = "19",
            platform = "android",
            doc = "The least SDK required to run this app, the default value changes based on functionality but can be as low as 7. This corresponds to the XML attribute `android:minSdkVersion`.",
            consumedBy = {"AndroidGradleBuilder"})
    int minSdkVersion() default 0;

    @Hint(def = "true",
            platform = "android",
            doc = "Boolean true/false defaults to false. Multidex allows Android binaries to reference more than 65536 methods. This slows builds a bit so you have it off by default but if you get a build error mentioning this limit you should turn this on.",
            consumedBy = {"AndroidGradleBuilder"})
    boolean multidex() default false;

    @Hint(def = "true",
            platform = "android",
            doc = "Uses the current Firebase Cloud Messaging integration. Requires AndroidX and Gradle 8.13 or newer.",
            consumedBy = {"AndroidGradleBuilder"})
    boolean newFirebaseMessaging() default false;

    @Hint(appendable = true,
            separator = "\n",
            platform = "android",
            doc = "Arguments for the keep option in proguard allowing you to keep a pattern of files for example, `-keep class com.mypackage.ProblemClass { *; }`",
            consumedBy = {"AndroidGradleBuilder"})
    String[] proguardKeep() default {};

    @Hint(def = "true",
            platform = "android",
            doc = "true/false defaults to true - indicates whether to include the release version in the build",
            consumedBy = {"AndroidGradleBuilder"})
    boolean release() default false;

    @Hint(appendable = true,
            separator = "\n",
            platform = "android",
            doc = "Extra Gradle repositories to resolve dependencies from.",
            consumedBy = {"AndroidGradleBuilder", "MapsProviderInjector"})
    String[] repositories() default {};

    @Hint(platform = "android",
            doc = "The Android SDK the build compiles against. Unset, the build server uses the highest platform it has installed, so leaving this alone tracks the server rather than pinning a number. Not every target works: the source may have limitations, and not all SDK targets are installed.",
            consumedBy = {"AndroidGradleBuilder"})
    int targetSDKVersion() default 0;

    @Hint(name = "and.themeMode",
            platform = "android",
            doc = "`auto`, `modern` / `material`, `hololight` (default for existing apps), `legacy`. `auto` and `modern` / `material` opt in to the CSS-generated Android Material 3 theme from `native-themes/android-material/theme.css`. `hololight` is Android Holo Light (what the framework shipped on API 14+ before this refactor). `legacy` loads the pre-Holo Android theme. The legacy alias `cn1.androidTheme` is still accepted, and `and.hololight=true` still maps to `hololight`. The default stays on `hololight` for existing apps until you flip in a future release.",
            consumedBy = {"AndroidGradleBuilder"})
    AndroidThemeMode themeMode() default AndroidThemeMode.AUTO;

    @Hint(appendable = true,
            separator = "\n",
            platform = "android",
            doc = "Statements added to the top-level Gradle build file rather than the app module.",
            consumedBy = {"AndroidGradleBuilder"})
    String[] topDependency() default {};

    @Hint(platform = "android",
            doc = "Use Android X instead of support libraries. This will also run a find/replace on all source files to replace support libraries and artifacts with AndroidX equivalents.",
            consumedBy = {"AndroidGradleBuilder"})
    boolean useAndroidX() default false;

    @Hint(appendable = true,
            kind = HintKind.XML,
            platform = "android",
            doc = "defaults to an empty string. Allows developers of native Android code to add text within the application block to define things such as widgets, services etc.",
            consumedBy = {"AndroidGradleBuilder"})
    String xapplication() default "";

    @Hint(appendable = true,
            separator = "\n",
            platform = "android",
            doc = "Arbitrary text spliced into the generated app-module Gradle file.",
            consumedBy = {"AndroidGradleBuilder"})
    String[] xgradle() default {};

    @Hint(appendable = true,
            kind = HintKind.XML,
            platform = "android",
            doc = "more permissions for the Android manifest",
            consumedBy = {"AndroidGradleBuilder"})
    String xpermissions() default "";
}
