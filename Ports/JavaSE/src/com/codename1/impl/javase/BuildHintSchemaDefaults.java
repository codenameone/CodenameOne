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
 * Registers BuildHintEditor schema defaults for the native-theme build
 * hints introduced in the CSS-driven native-themes work. The editor
 * (BuildHintEditor) discovers known hints by scanning System properties
 * matching {@code codename1.arg.{{ HintName }}.<field>}; cn1libs and the
 * framework itself can register hints by setting such properties before
 * the editor loads.
 *
 * <p>Called from {@link Simulator#main(String[])} so the Native Theme
 * group appears in the Build Hints dialog by default.
 */
final class BuildHintSchemaDefaults {

    private BuildHintSchemaDefaults() {
    }

    static void register() {
        // Group.
        set("{{@nativeTheme}}.label", "Native Theme");
        set("{{@nativeTheme}}.description",
                "Controls the Codename One look & feel on iOS and Android. "
                + "Modern themes are generated from CSS under native-themes/; "
                + "legacy themes remain selectable via the values below.");

        // Cross-platform meta hint.
        set("{{#nativeTheme#cn1.nativeTheme}}.label", "Shared override");
        set("{{#nativeTheme#cn1.nativeTheme}}.type", "Select");
        set("{{#nativeTheme#cn1.nativeTheme}}.values", "modern,legacy,custom");
        set("{{#nativeTheme#cn1.nativeTheme}}.description",
                "Overrides both iOS and Android native theme selection. "
                + "\"modern\" = liquid glass / Material 3. \"legacy\" = iOS 7 "
                + "flat / Android Holo Light. \"custom\" disables the framework "
                + "default and expects the app to install its own.");

        // iOS.
        set("{{#nativeTheme#ios.themeMode}}.label", "iOS theme");
        set("{{#nativeTheme#ios.themeMode}}.type", "Select");
        set("{{#nativeTheme#ios.themeMode}}.values", "auto,modern,ios7,legacy");
        set("{{#nativeTheme#ios.themeMode}}.description",
                "auto = modern (default). modern / liquid = Liquid Glass. "
                + "ios7 / flat = pre-liquid flat iOS 7 theme. "
                + "legacy / iphone = pre-iOS7 theme.");

        // Android.
        set("{{#nativeTheme#cn1.androidTheme}}.label", "Android theme");
        set("{{#nativeTheme#cn1.androidTheme}}.type", "Select");
        set("{{#nativeTheme#cn1.androidTheme}}.values", "material,hololight,legacy");
        set("{{#nativeTheme#cn1.androidTheme}}.description",
                "material = Material 3 (default). hololight = Android Holo "
                + "Light (API 14+). legacy = pre-Holo Android theme. "
                + "and.hololight=true is accepted for back-compat.");
    }

    /** Idempotent setter: does not overwrite user / project-level hint metadata. */
    private static void set(String suffix, String value) {
        String key = "codename1.arg." + suffix;
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
