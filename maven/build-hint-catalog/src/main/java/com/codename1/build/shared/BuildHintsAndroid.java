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
package com.codename1.build.shared;

import com.codename1.build.shared.BuildHints.Hint;

import java.util.List;

/**
 * Android build hints, including the {@code and.} override aliases.
 *
 * <p>Seeded by mining every {@code getArg} call site in the builders, so the
 * name and the default match what the build actually reads. Curated entries
 * carry an annotation attribute and, where the domain is provably closed, an
 * enum; the rest are described but set through
 * {@code codenameone_settings.properties}.</p>
 *
 * <p>Split out of {@link BuildHints} because a single class initializer
 * holding every entry would exceed the JVM's 64KB per-method limit.</p>
 */
final class BuildHintsAndroid {

    private BuildHintsAndroid() {
    }

    static void register(List<Hint> h) {
        // Not an abbreviation: the builder reads android.captureRecord and then
        // lets and.captureRecord override it, so the two name ONE setting.
        // Without the alias, @Android(captureRecord) and a properties line
        // spelling it the short way are not seen as a conflict -- and the
        // properties line wins, leaving the compile-checked annotation silently
        // ineffective, which is the whole failure this feature exists to remove.
        h.add(new Hint("and.captureRecord")
                .aliasOf("android.captureRecord")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Override alias of `android.captureRecord`, read after it and winning when set."));

        // Same override relationship (AndroidGradleBuilder reads the long name and
        // then this one). Not annotated today, so nothing can conflict with it yet
        // -- recorded so Settings collapses the pair, and so annotating the long
        // name later cannot reintroduce the captureRecord bug.
        h.add(new Hint("and.facebook_permissions")
                .aliasOf("android.facebook_permissions")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder", "IPhoneBuilder")
                .doc("Override alias of `android.facebook_permissions`, read after it and winning "
                        + "when set. `IPhoneBuilder` also falls back to it when "
                        + "`ios.facebook_permissions` is unset."));

        h.add(new Hint("and.themeMode")
                .annotatedAs(HintGroup.ANDROID, "themeMode")
                .values("AndroidThemeMode", "auto", "modern", "hololight", "legacy")
                // AndroidImplementation.installNativeTheme compares against these
                // too; see the note on ios.themeMode about why they are not
                // constants.
                .valueAliases("material", "modern", "holo", "hololight")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("`auto`, `modern` / `material`, `hololight` (default for existing apps), `legacy`. `auto` "
                        + "and `modern` / `material` opt in to the CSS-generated Android Material 3 theme from "
                        + "`native-themes/android-material/theme.css`. `hololight` is Android Holo Light (what the "
                        + "framework shipped on API 14+ before this refactor). `legacy` loads the pre-Holo Android "
                        + "theme. The legacy alias `cn1.androidTheme` is still accepted, and `and.hololight=true` "
                        + "still maps to `hololight`. The default stays on `hololight` for existing apps until you "
                        + "flip in a future release."));

        h.add(new Hint("android.NotificationChannel.description")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("Remote notifications")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.NotificationChannel.enableLights")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.NotificationChannel.enableVibration")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.NotificationChannel.id")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("cn1-channel")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.NotificationChannel.importance")
                .group(HintGroup.ANDROID)
                .type(HintType.INT)
                .def("2")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.NotificationChannel.lightColor")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.NotificationChannel.name")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("Notifications")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.NotificationChannel.vibrationPattern")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.accessibilityGuard")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.accessibilityGuard.allow")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.accessibilityGuard.mode")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("exit")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.activity.launchMode")
                .annotatedAs(HintGroup.ANDROID, "activityLaunchMode")
                .type(HintType.STRING)
                .def("singleTop")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Allows explicitly setting the `android:launchMode` attribute of the main activity in "
                        + "android. Default is \"singleTop,\" but for some applications you may need to change this "
                        + "behaviour. In particular, apps that are meant to open a file type will need to set this "
                        + "to \"singleTask.\" See "
                        + "https://developer.android.com/guide/topics/manifest/activity-element.html[Android docs "
                        + "for the activity element] for more information about the `android:launchMode` attribute."));

        h.add(new Hint("android.activityClassBody")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.activityClassImports")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.adaptiveIconBackground")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("#ffffff")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Background color to use for adaptive icons when `android.enableAdaptiveIcons=true` and "
                        + "no background image is supplied. Defaults to `#ffffff` and is written as "
                        + "`@color/ic_launcher_background`."));

        h.add(new Hint("android.adaptiveIconBackgroundImage")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Optional path (relative to the root of the native Android project) to an image file to "
                        + "use as the adaptive icon background when `android.enableAdaptiveIcons=true`. If this "
                        + "property is set, it overrides `android.adaptiveIconBackground`."));

        h.add(new Hint("android.allowBackup")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.androidAuto.messaging")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.androidAuto.minCarApiLevel")
                .group(HintGroup.ANDROID)
                .type(HintType.INT)
                .def("1")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.androidAuto.navigation")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.androidAuto.poi")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.anyDensity")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.apacheLegacy")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.appBundle")
                .annotatedAs(HintGroup.ANDROID, "appBundle")
                .type(HintType.BOOLEAN)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Produces an Android App Bundle (.aab) rather than an APK. Required for new Play Store "
                        + "submissions."));

        h.add(new Hint("android.appReview.version")
                .group(HintGroup.ANDROID)
                .type(HintType.VERSION)
                .def("2.0.1")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.ar.required")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.arrcompile")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.arrimplementation")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.asyncPaint")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to true. Toggles the Android pipeline between the legacy "
                        + "pipeline (false) and new pipeline (true)"));

        h.add(new Hint("android.background_push_handling")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.billingclient.version")
                .group(HintGroup.ANDROID)
                .type(HintType.VERSION)
                .def("4.0.0")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.blockExternalStoragePermission")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to false. Disables the external storage (SD card) permission"));

        h.add(new Hint("android.blockLabel")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to false. Leaves `android:label` off the generated "
                        + "`<application>` tag so a label set through `android.xapplication_attr` or a merged "
                        + "manifest is the one that survives. Honoured by the wear module's tag as well as the "
                        + "phone's."));

        h.add(new Hint("android.blockReadMediaPermissions")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false, defaults to the value of `android.blockExternalStoragePermission`. "
                        + "Suppresses the `READ_MEDIA_VIDEO` and `READ_MEDIA_AUDIO` permissions that playing a URI "
                        + "adds on API 33 and above"));

        h.add(new Hint("android.bluetooth.neverForLocation")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.bluetooth.required")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.buildToolsVersion")
                .annotatedAs(HintGroup.ANDROID, "buildToolsVersion")
                .type(HintType.VERSION)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Android build-tools version. It also selects the compile SDK, so there is no separate "
                        + "compile-SDK hint."));

        h.add(new Hint("android.captureRecord")
                .annotatedAs(HintGroup.ANDROID, "captureRecord")
                .type(HintType.STRING)
                .def("enabled")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Indicates whether the `RECORD_AUDIO` permission should be requested. Can be `enabled` or "
                        + "any other value to disable this option"));

        h.add(new Hint("android.carAppVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.VERSION)
                .def("1.4.0")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.credentialsPlayServicesVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.credentialsVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.VERSION)
                .def("1.3.0")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.cusom_layout")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.cusom_layout1")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Applies to any number of layouts as long as they're in sequence (for example, "
                        + "android.cusom_layout2, android.cusom_layout3 etc.). Will write the content of the "
                        + "argument as a layout XML file and give it the name `cusom_layout1.xml` onwards. This can "
                        + "be used by native code to work with XML files"));

        h.add(new Hint("android.customActivity")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("CodenameOneActivity")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.customTabsVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.VERSION)
                .def("1.8.0")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.debug")
                .annotatedAs(HintGroup.ANDROID, "debug")
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("true/false defaults to true - indicates whether to include the debug version in the "
                        + "build. Defaults conditionally rather than to a fixed value: when android.release is on "
                        + "it defaults to false, and when release is off it defaults to true, so a build that "
                        + "selects neither still produces something installable "
                        + "(AndroidGradleBuilder.java:447-451)."));

        h.add(new Hint("android.decouplePlayServiceVersions")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.delayPushCompletion")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.disableR8")
                .annotatedAs(HintGroup.ANDROID, "disableR8")
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Turns off R8, falling back to the older shrinker. Note that hardening requires R8, so "
                        + "this conflicts with harden.level."));

        h.add(new Hint("android.disableR8FullMode")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.disableScreenshots")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.enableAdaptiveIcons")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder", "CN1BuildMojo")
                .doc("Boolean true/false defaults to false. Enables Android adaptive icon generation in "
                        + "Android Gradle builds. When enabled, Codename One generates `mipmap` launcher resources "
                        + "(`ic_launcher`, `ic_launcher_foreground`, and adaptive XML in `mipmap-anydpi-v26`) and "
                        + "uses them in the application manifest (`android:icon` and `android:roundIcon`)."));

        h.add(new Hint("android.enableProguard")
                .annotatedAs(HintGroup.ANDROID, "enableProguard")
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to true. Allows disabling the proguard obfuscation even on "
                        + "release builds, notice that this isn't recommended"));

        h.add(new Hint("android.excludeBolts")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.extendAppCompatActivity")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.facebookSdkVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.VERSION)
                .def("16.2.0")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.facebook_permissions")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("\"public_profile\",\"email\",\"user_friends\"")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Permissions for Facebook used in the Android build target, applicable only if Facebook "
                        + "native integration is used."));

        h.add(new Hint("android.file_paths")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("    <files-path name=\"app_files\" path=\".\" />")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.firebaseAnalytics")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.firebaseAnalyticsVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.VERSION)
                .def("21.5.0")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.firebaseCoreVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.firebaseMessagingVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.foldableSupport")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.forceJava8Builder")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.foregroundServiceType")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("dataSync")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.fridaDetection")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to false. Indicates whether the app should check for the "
                        + "presence of the https://www.frida.re/[Frida] dynamic instrumentation toolkit on the "
                        + "device. If Frida is detected, the app will exit. This uses the "
                        + "[frida-blocker](https://github.com/shannah/frida-blocker) library to perform the frida "
                        + "detection."));

        h.add(new Hint("android.fullScreenIntent")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.googleAdUnitId")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Allows integrating admob/google play ads, this is effectively identical to "
                        + "google.adUnitId but only applies to Android"));

        h.add(new Hint("android.googleAdUnitTestDevice")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("C6783E2486F0931D9D09FABC65094FDF")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Device key used to mark a specific Android device as a test device for Google Play ads "
                        + "defaults to C6783E2486F0931D9D09FABC65094FDF"));

        h.add(new Hint("android.gpsPermission")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Indicates whether the GPS permission should be requested, it's autodetected by default "
                        + "if you use the location API. But, some code might want to explicitly define it"));

        h.add(new Hint("android.gradle.androidx")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING_LIST)
                .separator("\n")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.gradleDep")
                .annotatedAs(HintGroup.ANDROID, "gradleDep")
                .type(HintType.STRING_LIST)
                .separator(";")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Gradle dependency statements to add to the app module, such as implementation "
                        + "'com.example:lib:1.0'."));

        h.add(new Hint("android.gradlePlugin")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING_LIST)
                .separator("\n")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.hce")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.hceAids")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("F0010203040506")
                .platform("android")
                .consumedBy("AndroidGradleBuilder", "IPhoneBuilder"));

        h.add(new Hint("android.hceCategory")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("other")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.hceDescription")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.hceRequireUnlock")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.headphoneCallback")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to false. When set to true it assumes the main class has two "
                        + "methods: `headphonesConnected` & `headphonesDisconnected` which it invokes appropriately "
                        + "as needed"));

        h.add(new Hint("android.health.background")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.health.connectVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("1.1.0-alpha07")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.health.history")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.health.privacyPolicyUrl")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.health.read")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.health.write")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.hideOverlayWindows")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to false. Declares the "
                        + "`android.permission.HIDE_OVERLAY_WINDOWS` permission needed by "
                        + "`DeviceIntegrity.setHideOverlayWindows()` on Android 12+, for apps that call the runtime "
                        + "API without enabling `android.tapjackingGuard`. A normal install-time permission, so the "
                        + "user sees no prompt."));

        h.add(new Hint("android.hideStatusBar")
                .annotatedAs(HintGroup.ANDROID, "hideStatusBar")
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Hides the Android status bar."));

        h.add(new Hint("android.hms.pushVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("6.3.0.302")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.home.playServicesVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("16.0.0-beta1")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.includeGPlayServices")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("*Deprecated, please android.playService.+++*+++!* Indicates whether Google Play Services "
                        + "should be included into the build, defaults to false but that might change based on the "
                        + "functionality of the application and other build hints. Adding Google Play Services "
                        + "support allows you to use a more refined location implementation and invoke some Google "
                        + "specific functionality from native code."));

        h.add(new Hint("android.includeMavenCentral")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.installLocation")
                .annotatedAs(HintGroup.ANDROID, "installLocation")
                .values("InstallLocation", "auto", "internalOnly", "preferExternal")
                .def("auto")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Maps to android:installLocation manifest entry defaults to auto. Can also be set to "
                        + "internalOnly or preferExternal."));

        h.add(new Hint("android.java8")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.keyboardOpen")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to true. Toggles the new async keyboard mode that leaves the "
                        + "keyboard open while you move between text components"));

        h.add(new Hint("android.largeScreens")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.licenseKey")
                .annotatedAs(HintGroup.ANDROID, "licenseKey")
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("The license key for the Android app, this is required if you use in-app purchase on "
                        + "Android"));

        h.add(new Hint("android.locales")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.manifest.queries")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Embeds XML content into the <queries> section of the Android manifest file. This is "
                        + "https://developer.android.com/training/package-visibility[required in Android 11 for "
                        + "package visibility]. See "
                        + "https://developer.android.com/guide/topics/manifest/queries-element[queries element "
                        + "Android documentation]."));

        h.add(new Hint("android.messagingService")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.migrateToAndroidX")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.maps.provider")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("MapsProviderInjector")
                .doc("Android's own native map provider, overriding `maps.provider`."));

        h.add(new Hint("android.min_sdk_version")
                .annotatedAs(HintGroup.ANDROID, "minSdkVersion")
                .type(HintType.INT)
                .def("19")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("The least SDK required to run this app, the default value changes based on functionality "
                        + "but can be as low as 7. This corresponds to the XML attribute `android:minSdkVersion`."));

        h.add(new Hint("android.mockLocation")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to true. Toggles the mock location permission which is on by "
                        + "default, this allows easier debugging of Android device location based services"));

        h.add(new Hint("android.mopubId")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.multidex")
                .annotatedAs(HintGroup.ANDROID, "multidex")
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to false. Multidex allows Android binaries to reference more "
                        + "than 65536 methods. This slows builds a bit so you have it off by default but if you get "
                        + "a build error mentioning this limit you should turn this on."));

        h.add(new Hint("android.newFirebaseMessaging")
                .annotatedAs(HintGroup.ANDROID, "newFirebaseMessaging")
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Uses the current Firebase Cloud Messaging integration. Requires AndroidX and Gradle 8.13 "
                        + "or newer."));

        h.add(new Hint("android.nonconsumable")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Comma delimited string of items that are non-consumable in the in-app purchase API"));

        h.add(new Hint("android.normalScreens")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.onCreate")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playIntegrity")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playIntegrity.verifyUrl")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playIntegrityVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.VERSION)
                .def("1.4.0")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.ads")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.analytics")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.appInvite")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.auth")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.base")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.cast")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.drive")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.fitness")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.games")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.gcm")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.identity")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.indexing")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.location")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.maps")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.nearby")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.panorama")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.plus")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.safetynet")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.vision")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.wallet")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playService.wearable")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.playServicesVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("The version number of play services to build against. Experimental. **Use with caution** "
                        + "as building against versions other than the server default may introduce "
                        + "incompatibilities with some Codename One APIs."));

        h.add(new Hint("android.proguardKeep")
                .annotatedAs(HintGroup.ANDROID, "proguardKeep")
                .type(HintType.STRING_LIST)
                .separator("\n")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Arguments for the keep option in proguard allowing you to keep a pattern of files for "
                        + "example, `-keep class com.mypackage.ProblemClass { *; }`"));

        h.add(new Hint("android.proguardKeepOverride")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("Exceptions, InnerClasses, Signature, Deprecated, SourceFile, LineNumberTable, *Annotation*, EnclosingMethod")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.pushSound")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.pushVibratePattern")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Comma delimited long values to describe the push pattern of vibrate used for the "
                        + "`setVibrate` native method"));

        h.add(new Hint("android.release")
                .annotatedAs(HintGroup.ANDROID, "release")
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("true/false defaults to true - indicates whether to include the release version in the "
                        + "build"));

        h.add(new Hint("android.removeBasePermissions")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to false. Disables the built-in permissions specifically "
                        + "`INTERNET` permission (that is, no networking...)"));

        h.add(new Hint("android.repositories")
                .annotatedAs(HintGroup.ANDROID, "repositories")
                .type(HintType.STRING_LIST)
                .separator("\n")
                .platform("android")
                .consumedBy("AndroidGradleBuilder", "MapsProviderInjector")
                .doc("Extra Gradle repositories to resolve dependencies from."));

        h.add(new Hint("android.requestReadMediaPermissions")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to false. Declares `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` "
                        + "and `READ_MEDIA_AUDIO` on API 33 and above even when the build detected no media "
                        + "playback. `READ_MEDIA_IMAGES` is only ever added by this hint"));

        h.add(new Hint("android.rootCheck")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to false. Indicates whether the app should check for root "
                        + "access on the device. If root access is detected, the app will exit."));

        h.add(new Hint("android.rootbeerVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.VERSION)
                .def("0.1.0")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.shareFilter")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.sharedUserId")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Allows adding a manifest attribute for the sharedUserId option"));

        h.add(new Hint("android.sharedUserLabel")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Allows adding a manifest attribute for the sharedUserLabel option"));

        h.add(new Hint("android.shrinkResources")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to false. Used only in conjunction with "
                        + "android.enableProguard. Strips out unused resources to reduce apk size. Since 7.0"));

        h.add(new Hint("android.smallScreens")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to true. Corresponds to the `android:smallScreens` XML "
                        + "attribute and allows disabling the support for small phones"));

        h.add(new Hint("android.stack_size")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Size in bytes for the Android stack thread"));

        h.add(new Hint("android.statusbar_hidden")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("true/false defaults to false. When set to true hides the status bar on Android devices."));

        h.add(new Hint("android.store_ids")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.streamMode")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("The mode in which the volume key should behave, defaults to OS default. Allows setting "
                        + "it to `music` for music playback apps"));

        h.add(new Hint("android.stringsXml")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Allows injecting more entries into the strings.xml file using a value that includes "
                        + "something like this `<string name=\"key1\">value1</string><string "
                        + "name=\"key2\">value2</string>`"));

        h.add(new Hint("android.style")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Allows injecting more data into the `styles.xml` file right before the closing resources "
                        + "tag"));

        h.add(new Hint("android.supportScreens")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.supportv4Dep")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING_LIST)
                .separator("\n")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.surfaces.complicationUpdateSeconds")
                .group(HintGroup.ANDROID)
                .type(HintType.INT)
                .def("0")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("`UPDATE_PERIOD_SECONDS` on the generated complication service. Zero, the default, means "
                        + "the system never polls on a timer and the complication updates only when the app "
                        + "pushes new data."));

        h.add(new Hint("android.surfaces.exactAlarms")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.tapjackingGuard")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to false. Switches on tapjacking / screen-overlay protection "
                        + "at launch, so touches that arrive while another app's window covers this one are "
                        + "detected and dropped. See the security chapter."));

        h.add(new Hint("android.tapjackingGuard.hideOverlays")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to true. Also asks Android 12+ to hide overlay windows drawn "
                        + "over the app, which is the only mitigation that covers native peer components, and "
                        + "declares the `HIDE_OVERLAY_WINDOWS` permission it requires. Only relevant if "
                        + "`android.tapjackingGuard=true`."));

        h.add(new Hint("android.tapjackingGuard.mode")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("block")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("`block` (default), `strict`, `report` or `off`. `block` drops gestures that start on a "
                        + "fully obscured window, `report` only observes, `strict` also drops touches where only "
                        + "part of the window is covered (which benign system UI can trigger). Only relevant if "
                        + "`android.tapjackingGuard=true`."));

        h.add(new Hint("android.targetSDKVersion")
                .annotatedAs(HintGroup.ANDROID, "targetSDKVersion")
                .type(HintType.INT)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Indicates the Android SDK used to compile the Android build defaults to 21. Notice that "
                        + "not all targets will work since the source might have some limitations and not all SDK "
                        + "targets are installed on the build servers."));

        h.add(new Hint("android.textureView")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.theme")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("Light")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Light or Dark defaults to Light. On Android 4+ the default Holo theme is used to render "
                        + "the native widgets sometimes and this indicates whether holo light or holo dark is used. "
                        + "This doesn't affect the Codename One theme but that might change in the future."));

        h.add(new Hint("android.topDependency")
                .annotatedAs(HintGroup.ANDROID, "topDependency")
                .type(HintType.STRING_LIST)
                .separator("\n")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Statements added to the top-level Gradle build file rather than the app module."));

        h.add(new Hint("android.tv")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("true/false (defaults to false). Marks the build as an Android TV / Google TV app. Adds "
                        + "the `LEANBACK_LAUNCHER` intent category to the launcher activity (so the app appears on "
                        + "the TV home screen), declares the `android.software.leanback` feature, makes "
                        + "`android.hardware.touchscreen` optional (so it installs on touchless TVs), and generates "
                        + "a 320×180 launcher banner (`@drawable/tv_banner`) from the app icon. The same APK still "
                        + "installs and runs on phones and tablets, and `CN.isTV()` returns true at runtime on a "
                        + "TV."));

        h.add(new Hint("android.useAndroidX")
                .annotatedAs(HintGroup.ANDROID, "useAndroidX")
                .type(HintType.BOOLEAN)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Use Android X instead of support libraries. This will also run a find/replace on all "
                        + "source files to replace support libraries and artifacts with AndroidX equivalents."));

        h.add(new Hint("android.useGradle8")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.versionCode")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Allows overriding the auto generated version number with a custom internal version "
                        + "number specifically used for the XML attribute `android:versionCode`"));

        h.add(new Hint("android.watchModule")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Boolean true/false defaults to true. Set to false to build the phone app alone in a "
                        + "companion build: the wearable link stays, no watch module is generated, and the "
                        + "phone output matches what it was before the watch app existed."));

        h.add(new Hint("android.watchVersionCode")
                .group(HintGroup.ANDROID)
                .type(HintType.INT)
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("The wear module's version code, stated outright. Play requires it to be higher than the "
                        + "phone's, so a value other than a whole number above `android.versionCode` fails the "
                        + "build rather than being replaced without a word. Leave it unset to derive the value "
                        + "from `android.watchVersionCodeOffset`."));

        h.add(new Hint("android.watchVersionCodeOffset")
                .group(HintGroup.ANDROID)
                .type(HintType.INT)
                .def("100000000")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("How far above the phone's version code the wear module's sits when "
                        + "`android.watchVersionCode` is unset. The default leaves room for the phone app to "
                        + "keep incrementing without ever catching up."));

        h.add(new Hint("android.wear")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.wear.complicationsVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("1.2.1")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Version of `androidx.wear.watchface:watchface-complications-data-source` added to the "
                        + "wear module. Kept out of `android.gradleDependencies` because that hint feeds the "
                        + "phone module too, and these libraries declare minSdk 26."));

        h.add(new Hint("android.wear.guavaVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("31.1-android")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Version of `com.google.guava:guava` added to the wear module alongside the tiles and "
                        + "complications libraries, which need it at runtime."));

        h.add(new Hint("android.wear.protoLayoutVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("1.2.1")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Version of the `androidx.wear.protolayout` libraries the generated tile service builds "
                        + "its layout with."));

        h.add(new Hint("android.wear.tilesVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .def("1.4.1")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Version of `androidx.wear.tiles` added to the wear module when the app declares a tile."));

        h.add(new Hint("android.wear.standalone")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.web_loading_hidden")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("true/false defaults to false - set to true to hide the progress indicator that appears "
                        + "when loading a web page on Android."));

        h.add(new Hint("android.windowVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.VERSION)
                .def("1.3.0")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.xactivity")
                .group(HintGroup.ANDROID)
                .type(HintType.XML)
                .separator("")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Allows injecting more attributes into the `activity` tag in the Android XML"));

        h.add(new Hint("android.xapplication")
                .annotatedAs(HintGroup.ANDROID, "xapplication")
                .type(HintType.XML)
                .separator("")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("defaults to an empty string. Allows developers of native Android code to add text within "
                        + "the application block to define things such as widgets, services etc."));

        h.add(new Hint("android.xapplication_attr")
                .group(HintGroup.ANDROID)
                .type(HintType.XML)
                .separator(" ")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Allows injecting more attributes into the `application`` tag in the Android XML"));

        h.add(new Hint("android.xgradle")
                .annotatedAs(HintGroup.ANDROID, "xgradle")
                .type(HintType.STRING_LIST)
                .separator("\n")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Arbitrary text spliced into the generated app-module Gradle file."));

        h.add(new Hint("android.xgradle_default_config")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING_LIST)
                .separator("\n")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.xintent_filter")
                .group(HintGroup.ANDROID)
                .type(HintType.XML)
                .separator("")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("Allows adding an intent filter to the main android activity"));

        h.add(new Hint("android.xlargeScreens")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.xlayout_attr")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.xmanifest")
                .group(HintGroup.ANDROID)
                .type(HintType.XML)
                .separator("")
                .platform("android")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.xpermissions")
                .annotatedAs(HintGroup.ANDROID, "xpermissions")
                .type(HintType.XML)
                .separator("")
                .platform("android")
                .consumedBy("AndroidGradleBuilder")
                .doc("more permissions for the Android manifest"));
    }
}
