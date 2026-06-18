/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.javase;

/**
 * Registers schema metadata for the native-theme build hints
 * (ios.themeMode, and.themeMode, nativeTheme) so that the
 * Build Hints UI inside the Codename One Simulator can show them as
 * labelled Select dropdowns instead of opaque key/value entries.
 *
 * <p>The deprecated keys {@code cn1.nativeTheme} and
 * {@code cn1.androidTheme} are still honored at runtime but are no
 * longer surfaced in the schema - new projects should use
 * {@code nativeTheme} / {@code and.themeMode} (matching the
 * {@code ios.themeMode} pattern).
 *
 * <p><b>Why this class exists:</b> {@link com.codename1.impl.javase.BuildHintEditor}
 * is the dialog that lets developers set build hints from the
 * Simulator menu (Project &rarr; Build Hints). It populates its rows by
 * scanning system properties whose keys match
 * {@code codename1.arg.{{ HintName }}.<field>} (label / type / values
 * / description / group). Hints contributed by cn1libs typically
 * register themselves via that property convention from the cn1lib's
 * own code, but the three hints introduced by the CSS-driven
 * native-themes work are framework-level - they are not part of any
 * cn1lib - and need to be visible to every project, including the
 * very first one a new developer creates. Without this class the
 * dropdowns would not appear and users would have to type the hint
 * name and value by hand into {@code codenameone_settings.properties},
 * which most developers would never discover.
 *
 * <p>This is <b>not</b> related to live CSS recompilation. The CSS
 * watcher in the Simulator is a separate component; this class only
 * publishes the build-hint schema.
 *
 * <p><b>Lifecycle:</b> {@link #register()} is invoked once from
 * {@code Simulator.main(String[])} during simulator startup, before
 * the BuildHintEditor reads its registry. Re-invoking is harmless -
 * each {@link System#setProperty(String, String)} call simply
 * overwrites the previous value. Hints set here can still be
 * overridden by per-project properties or by a cn1lib that registers
 * the same key with different metadata.
 */
final class BuildHintSchemaDefaults {

    private BuildHintSchemaDefaults() {
    }

    static void register() {
        // Group.
        set("{{@nativeTheme}}.label", "Native Theme");
        set("{{@nativeTheme}}.description",
                "Controls the Codename One look & feel on iOS, Android, and "
                + "the JavaScript port (browser OS auto-detection: iOS/Mac "
                + "browsers get the iOS theme, everything else gets the "
                + "Android theme). Modern themes are generated from CSS "
                + "under native-themes/; legacy themes remain selectable "
                + "via the values below.");

        // Cross-platform meta hint.
        set("{{#nativeTheme#nativeTheme}}.label", "Shared override");
        set("{{#nativeTheme#nativeTheme}}.type", "Select");
        set("{{#nativeTheme#nativeTheme}}.values", "modern,legacy,custom");
        set("{{#nativeTheme#nativeTheme}}.description",
                "Overrides both iOS and Android native theme selection. "
                + "\"modern\" = liquid glass / Material 3. \"legacy\" = iOS 7 "
                + "flat / Android Holo Light. \"custom\" disables the framework "
                + "default and expects the app to install its own. "
                + "(Deprecated alias: cn1.nativeTheme.)");

        // iOS.
        set("{{#nativeTheme#ios.themeMode}}.label", "iOS theme");
        set("{{#nativeTheme#ios.themeMode}}.type", "Select");
        set("{{#nativeTheme#ios.themeMode}}.values", "auto,modern,ios7,legacy");
        set("{{#nativeTheme#ios.themeMode}}.description",
                "auto = modern (default). modern / liquid = Liquid Glass. "
                + "ios7 / flat = pre-liquid flat iOS 7 theme. "
                + "legacy / iphone = pre-iOS7 theme.");

        // Android.
        set("{{#nativeTheme#and.themeMode}}.label", "Android theme");
        set("{{#nativeTheme#and.themeMode}}.type", "Select");
        set("{{#nativeTheme#and.themeMode}}.values", "auto,modern,hololight,legacy");
        set("{{#nativeTheme#and.themeMode}}.description",
                "auto = modern (default). modern / material = Material 3. "
                + "hololight = Android Holo Light (API 14+). legacy = pre-Holo "
                + "Android theme. (Deprecated alias: cn1.androidTheme; "
                + "and.hololight=true is also accepted for back-compat.)");

        // watchOS native build (Apple Watch). Adds a watchOS app target to the
        // iOS Xcode project, rendering the CN1 UI via the Core Graphics backend.
        set("{{@watchNative}}.label", "Apple Watch (watchOS)");
        set("{{@watchNative}}.description",
                "Builds an Apple Watch app from the same project, rendering the "
                + "Codename One UI on watchOS via the Core Graphics backend. The "
                + "watch app is a separate arm64_32 target; in the default "
                + "companion mode it is embedded in the iOS .ipa and installs "
                + "with the phone app.");

        set("{{#watchNative#watchNative.enabled}}.label", "Enable watchOS target");
        set("{{#watchNative#watchNative.enabled}}.type", "Select");
        set("{{#watchNative#watchNative.enabled}}.values", "false,true");
        set("{{#watchNative#watchNative.enabled}}.description",
                "When true, adds an Apple Watch app target to the generated "
                + "Xcode project. Also auto-enabled whenever codename1.watchMain "
                + "is declared next to codename1.mainName in "
                + "codenameone_settings.properties, so the double app is produced "
                + "as part of the regular iPhone build. Requires the Ruby "
                + "xcodeproj gem (bundled with CocoaPods).");

        set("{{#watchNative#watchNative.mainClass}}.label", "Watch lifecycle class");
        set("{{#watchNative#watchNative.mainClass}}.type", "String");
        set("{{#watchNative#watchNative.mainClass}}.description",
                "Fully-qualified watch entry/lifecycle class. Normally set via "
                + "codename1.watchMain; this hint is an override. May equal the "
                + "phone main class - a distinct class lets the watch slice "
                + "tree-shake from its own root. Defaults to the phone main class "
                + "when watchNative.enabled=true without a watch entry.");

        set("{{#watchNative#watchNative.distribution}}.label", "Distribution");
        set("{{#watchNative#watchNative.distribution}}.type", "Select");
        set("{{#watchNative#watchNative.distribution}}.values", "companion,standalone");
        set("{{#watchNative#watchNative.distribution}}.description",
                "companion = the watch app is embedded in the iOS app and "
                + "installs with it (WKCompanionAppBundleIdentifier pinned to "
                + "the iOS bundle). standalone = an independent watch-only app.");

        set("{{#watchNative#watchNative.bundleId}}.label", "Watch bundle identifier");
        set("{{#watchNative#watchNative.bundleId}}.type", "String");
        set("{{#watchNative#watchNative.bundleId}}.description",
                "Bundle id of the watch app. Defaults to <package>.watchkitapp.");

        set("{{#watchNative#watchNative.minDeploymentTarget}}.label", "Minimum watchOS version");
        set("{{#watchNative#watchNative.minDeploymentTarget}}.type", "String");
        set("{{#watchNative#watchNative.minDeploymentTarget}}.description",
                "WATCHOS_DEPLOYMENT_TARGET for the watch target. Defaults to 10.0 "
                + "(single-target WKApplication apps + WidgetKit complications).");

        set("{{#watchNative#watchNative.teamId}}.label", "Apple team id");
        set("{{#watchNative#watchNative.teamId}}.type", "String");
        set("{{#watchNative#watchNative.teamId}}.description",
                "Development team for signing the watch target. Defaults to the "
                + "iOS team id (ios.teamId / ios.release.teamId).");

        set("{{#watchNative#watchNative.displayName}}.label", "Watch app name");
        set("{{#watchNative#watchNative.displayName}}.type", "String");
        set("{{#watchNative#watchNative.displayName}}.description",
                "Name shown under the watch app icon. Defaults to the app display "
                + "name (codename1.displayName), then the main class name.");

        set("{{#watchNative#watchNative.embedCompanion}}.label", "Embed in iOS app");
        set("{{#watchNative#watchNative.embedCompanion}}.type", "Select");
        set("{{#watchNative#watchNative.embedCompanion}}.values", "false,true");
        set("{{#watchNative#watchNative.embedCompanion}}.description",
                "When true (companion distribution), adds the watch app as a build "
                + "dependency of the iOS app so the pair archives together. Off by "
                + "default so the iOS build is unaffected; enable it for a packaged "
                + "companion submission.");

        // Wear OS native build (Android). A Wear OS app is a regular Android app
        // that declares the watch hardware feature; the CN1 UI renders through
        // the normal Android pipeline (no separate backend, unlike watchOS).
        set("{{@androidWear}}.label", "Wear OS (Android)");
        set("{{@androidWear}}.description",
                "Builds the Android app as a Wear OS app: declares the watch "
                + "hardware feature, marks the app standalone (runs without a "
                + "paired phone app) and raises the minimum SDK to the Wear OS 2.0 "
                + "baseline (API 23). CN.isWatch() returns true at runtime via "
                + "PackageManager.FEATURE_WATCH. Independent of the Apple Watch "
                + "build; enable both to target both wearables.");

        set("{{#androidWear#android.wear}}.label", "Enable Wear OS build");
        set("{{#androidWear#android.wear}}.type", "Select");
        set("{{#androidWear#android.wear}}.values", "false,true");
        set("{{#androidWear#android.wear}}.description",
                "When true, marks the Android build as a Wear OS app (manifest "
                + "uses-feature android.hardware.type.watch, standalone meta-data, "
                + "minimum SDK floor API 23). With the hint off the manifest is "
                + "unchanged.");

        set("{{#androidWear#android.wear.standalone}}.label", "Standalone Wear app");
        set("{{#androidWear#android.wear.standalone}}.type", "Select");
        set("{{#androidWear#android.wear.standalone}}.values", "true,false");
        set("{{#androidWear#android.wear.standalone}}.description",
                "Declares the Wear app standalone (com.google.android.wearable."
                + "standalone), so it installs and runs directly on the watch "
                + "without a companion phone app. Defaults to true. Only applies "
                + "when android.wear=true.");
    }

    /** Idempotent setter: does not overwrite user / project-level hint metadata. */
    private static void set(String suffix, String value) {
        String key = "codename1.arg." + suffix;
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
