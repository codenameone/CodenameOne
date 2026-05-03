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
                "Controls the Codename One look & feel on iOS and Android. "
                + "Modern themes are generated from CSS under native-themes/; "
                + "legacy themes remain selectable via the values below.");

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
    }

    /** Idempotent setter: does not overwrite user / project-level hint metadata. */
    private static void set(String suffix, String value) {
        String key = "codename1.arg." + suffix;
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
