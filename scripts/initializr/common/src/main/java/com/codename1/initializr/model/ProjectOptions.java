/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.initializr.model;

public final class ProjectOptions {
    public enum PreviewLanguage {
        ENGLISH("English", "en", false),
        FRENCH("Fran\u00e7ais", "fr", false),
        GERMAN("Deutsch", "de", false),
        SPANISH("Espa\u00f1ol", "es", false),
        ITALIAN("Italiano", "it", false),
        PORTUGUESE("Portugu\u00eas", "pt", false),
        DUTCH("Nederlands", "nl", false),
        CHINESE_SIMPLIFIED("\u4e2d\u6587 (\u7b80\u4f53)", "zh_CN", false),
        JAPANESE("\u65e5\u672c\u8a9e", "ja", false),
        KOREAN("\ud55c\uad6d\uc5b4", "ko", false),
        ARABIC("\u0627\u0644\u0639\u0631\u0628\u064a\u0629", "ar", true),
        HEBREW("\u05e2\u05d1\u05e8\u05d9\u05ea", "he", true);

        public final String label;
        public final String bundleSuffix;
        public final boolean rtl;

        PreviewLanguage(String label, String bundleSuffix, boolean rtl) {
            this.label = label;
            this.bundleSuffix = bundleSuffix;
            this.rtl = rtl;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum ThemeMode {
        LIGHT,
        DARK
    }

    public enum Accent {
        DEFAULT,
        TEAL,
        BLUE,
        ORANGE
    }

    public enum JavaVersion {
        JAVA_17("Java 17"),
        JAVA_8("Java 8");

        public final String label;

        JavaVersion(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /// The build tool the generated project uses. Maven is the default and the
    /// only one that offers Java 8; Gradle projects are single-project builds
    /// driven by the `com.codenameone` Gradle plugin and always target Java 17.
    public enum BuildTool {
        MAVEN("Maven"),
        GRADLE("Gradle");

        public final String label;

        BuildTool(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /// What the generated project holds: an app, an app with a `backend/` server
    /// beside it, or a server on its own.
    ///
    /// A Maven project has the choice only when the plugin it is generated against
    /// can build an app without its platform modules
    /// ([GeneratorModel#isMavenLayoutChoiceOffered()]). Against an older plugin a
    /// Maven download is the full multi-module layout, which always carries the
    /// backend module behind the `backend` profile, so APP and APP_WITH_BACKEND are
    /// the same download there and GeneratorModel refuses BACKEND_ONLY.
    public enum ProjectType {
        APP("App"),
        APP_WITH_BACKEND("App + backend"),
        BACKEND_ONLY("Backend only");

        public final String label;

        ProjectType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public final ThemeMode themeMode;
    public final Accent accent;
    public final boolean roundedButtons;
    public final boolean includeLocalizationBundles;
    public final PreviewLanguage previewLanguage;
    public final JavaVersion javaVersion;
    public final String customThemeCss;
    public final BuildTool buildTool;
    public final ProjectType projectType;
    /// Whether a Maven app is generated with every platform module (`javase/`,
    /// `android/`, `ios/`, ...) rather than the minimal layout, where `common/`
    /// builds every platform itself. Ignored for Gradle and for a backend-only
    /// project, and against a plugin that predates the minimal layout.
    public final boolean allPlatformModules;
    /// The brand colour of a template whose stylesheet is built on variables
    /// ([Template#isFullStack()]), as 0xRRGGBB, or -1 for the template's own.
    public final int brandColor;
    /// The application's icon as a PNG, or null for the stock one. Not copied:
    /// the options are a value, and nothing writes to the array.
    public final byte[] iconPng;

    public ProjectOptions(ThemeMode themeMode, Accent accent, boolean roundedButtons,
                          boolean includeLocalizationBundles, PreviewLanguage previewLanguage,
                          JavaVersion javaVersion) {
        this(themeMode, accent, roundedButtons, includeLocalizationBundles, previewLanguage, javaVersion, null);
    }

    public ProjectOptions(ThemeMode themeMode, Accent accent, boolean roundedButtons,
                          boolean includeLocalizationBundles, PreviewLanguage previewLanguage,
                          JavaVersion javaVersion, String customThemeCss) {
        this(themeMode, accent, roundedButtons, includeLocalizationBundles, previewLanguage, javaVersion,
                customThemeCss, BuildTool.MAVEN, ProjectType.APP);
    }

    public ProjectOptions(ThemeMode themeMode, Accent accent, boolean roundedButtons,
                          boolean includeLocalizationBundles, PreviewLanguage previewLanguage,
                          JavaVersion javaVersion, String customThemeCss,
                          BuildTool buildTool, ProjectType projectType) {
        this(themeMode, accent, roundedButtons, includeLocalizationBundles, previewLanguage, javaVersion,
                customThemeCss, buildTool, projectType, false);
    }

    public ProjectOptions(ThemeMode themeMode, Accent accent, boolean roundedButtons,
                          boolean includeLocalizationBundles, PreviewLanguage previewLanguage,
                          JavaVersion javaVersion, String customThemeCss,
                          BuildTool buildTool, ProjectType projectType, boolean allPlatformModules) {
        this(themeMode, accent, roundedButtons, includeLocalizationBundles, previewLanguage, javaVersion,
                customThemeCss, buildTool, projectType, allPlatformModules, -1, null);
    }

    private ProjectOptions(ThemeMode themeMode, Accent accent, boolean roundedButtons,
                           boolean includeLocalizationBundles, PreviewLanguage previewLanguage,
                           JavaVersion javaVersion, String customThemeCss,
                           BuildTool buildTool, ProjectType projectType, boolean allPlatformModules,
                           int brandColor, byte[] iconPng) {
        this.brandColor = brandColor;
        this.iconPng = iconPng;
        this.themeMode = themeMode;
        this.accent = accent;
        this.roundedButtons = roundedButtons;
        this.includeLocalizationBundles = includeLocalizationBundles;
        this.previewLanguage = previewLanguage == null ? PreviewLanguage.ENGLISH : previewLanguage;
        this.javaVersion = javaVersion == null ? JavaVersion.JAVA_17 : javaVersion;
        this.customThemeCss = customThemeCss;
        this.buildTool = buildTool == null ? BuildTool.MAVEN : buildTool;
        this.projectType = projectType == null ? ProjectType.APP : projectType;
        this.allPlatformModules = allPlatformModules;
    }

    /// A copy of these options with a different build tool and project type.
    public ProjectOptions withBuild(BuildTool buildTool, ProjectType projectType) {
        return new ProjectOptions(themeMode, accent, roundedButtons, includeLocalizationBundles, previewLanguage,
                javaVersion, customThemeCss, buildTool, projectType, allPlatformModules, brandColor, iconPng);
    }

    /// A copy of these options that does or does not ask for every platform module;
    /// see [#allPlatformModules].
    public ProjectOptions withPlatformModules(boolean all) {
        return new ProjectOptions(themeMode, accent, roundedButtons, includeLocalizationBundles, previewLanguage,
                javaVersion, customThemeCss, buildTool, projectType, all, brandColor, iconPng);
    }

    /// A copy of these options in another colour scheme: `brand` as 0xRRGGBB or -1
    /// for the template's own colour, and square or rounded corners. Read by the
    /// templates styled through variables; see [#brandColor].
    public ProjectOptions withScheme(int brand, boolean rounded) {
        return new ProjectOptions(themeMode, accent, rounded, includeLocalizationBundles, previewLanguage,
                javaVersion, customThemeCss, buildTool, projectType, allPlatformModules, brand, iconPng);
    }

    /// These options as a full-stack template takes them
    /// ([Template#isFullStack()]): a Maven project with its server, in Java 17,
    /// without the language bundles a bare project can be given. What the template
    /// leaves open -- the platform modules, the colours, the icon -- is kept.
    public ProjectOptions forFullStack() {
        return new ProjectOptions(themeMode, accent, roundedButtons, false, previewLanguage,
                JavaVersion.JAVA_17, customThemeCss, BuildTool.MAVEN, ProjectType.APP_WITH_BACKEND,
                allPlatformModules, brandColor, iconPng);
    }

    /// A copy of these options with `png` as the application's icon, or the stock
    /// icon when it is null.
    public ProjectOptions withIcon(byte[] png) {
        return new ProjectOptions(themeMode, accent, roundedButtons, includeLocalizationBundles, previewLanguage,
                javaVersion, customThemeCss, buildTool, projectType, allPlatformModules, brandColor, png);
    }

    public boolean isGradle() {
        return buildTool == BuildTool.GRADLE;
    }

    public static ProjectOptions defaults() {
        return new ProjectOptions(ThemeMode.LIGHT, Accent.DEFAULT, true, false, PreviewLanguage.ENGLISH, JavaVersion.JAVA_17);
    }
}
