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

public enum Template {
    BAREBONES(false, "/barebones-src.zip", "/barebones-css.zip", "/barebones-pom.xml", "com.example.myapp", "MyAppName", null,
            null, new String[0]),
    // The same two libraries kotlin-pom.xml's kotlin profile declares (at the Gradle
    // Kotlin version, see GeneratorModel.KOTLIN_VERSION). The Kotlin Gradle plugin
    // itself is added to the plugins {} block by GeneratorModel.
    KOTLIN(true, "/kotlin-src.zip", "/barebones-css.zip", "/kotlin-pom.xml", "com.example.myapp", "MyAppName", null,
            null, new String[] {
                    "implementation(\"org.jetbrains.kotlin:kotlin-stdlib:" + GeneratorModel.KOTLIN_VERSION + "\")",
                    "implementation(\"org.jetbrains:annotations:13.0\")"
            }),
    // A whole application rather than a starting point: a ride-hailing app with a
    // rider, a driver and an admin mode, and the server it talks to. It is the
    // project in scripts/wayline, which CI builds and tests; the resources named
    // here are derived from it by scripts/sync-initializr-wayline.py and never
    // edited by hand.
    //
    // Its preview is a resource of its own, read when the template is chosen,
    // and not an image of the theme: the theme is loaded at start-up, so a
    // preview kept there is paid for by everyone who opens the page, whichever
    // template they pick.
    //
    // Its `shared` module is what makes it Maven only. The contract the app and
    // the server both compile against is a module of its own, and a Gradle project
    // from the Initializr is a single project with nowhere to put one.
    WAYLINE(false, "/wayline-src.zip", "/wayline-css.zip", "/wayline-pom.xml",
            "com.codenameone.examples.wayline", "Wayline", "/wayline-preview.jpg",
            "The ride-hailing template is an app, a server and the contract module they share; "
                    + "an Initializr Gradle project is a single project.",
            new String[0], "/wayline-modules.zip", "/wayline-settings.properties");

    /// The resource holding the picture shown for a template that has no live
    /// preview, or null for one that has.
    public final String IMAGE_NAME;
    public final boolean IS_KOTLIN;
    public final String SOURCE_ZIP;
    public final String CSS;
    public final String POM_XML;
    public final String SOURCE_PACKAGE;
    public final String SOURCE_MAIN_CLASS;
    /// Why this template cannot be generated as a Gradle project, or null when it can.
    public final String GRADLE_UNSUPPORTED_REASON;
    /// Kotlin DSL lines for the `dependencies {}` block of a Gradle project's
    /// build.gradle.kts -- the Gradle form of what the template's pom declares.
    public final String[] GRADLE_DEPENDENCIES;
    /// The modules of a full-stack template, a zip laid out as the project is --
    /// `shared/...`, `backend/...` -- or null for a template that is an app alone.
    /// Its `backend/` replaces the generic server every other project gets.
    public final String MODULES_ZIP;
    /// Build hints the template's application needs on top of a plain project's
    /// `codenameone_settings.properties`, or null.
    public final String SETTINGS;

    Template(boolean isKotlin, String sourceZip, String css, String pomXml, String sourcePackage, String sourceMainClass,
             String imageName, String gradleUnsupportedReason, String[] gradleDependencies) {
        this(isKotlin, sourceZip, css, pomXml, sourcePackage, sourceMainClass, imageName,
                gradleUnsupportedReason, gradleDependencies, null, null);
    }

    Template(boolean isKotlin, String sourceZip, String css, String pomXml, String sourcePackage, String sourceMainClass,
             String imageName, String gradleUnsupportedReason, String[] gradleDependencies, String modulesZip, String settings) {
        IS_KOTLIN = isKotlin;
        SOURCE_ZIP = sourceZip;
        CSS = css;
        POM_XML = pomXml;
        SOURCE_PACKAGE = sourcePackage;
        SOURCE_MAIN_CLASS = sourceMainClass;
        IMAGE_NAME = imageName;
        GRADLE_UNSUPPORTED_REASON = gradleUnsupportedReason;
        GRADLE_DEPENDENCIES = gradleDependencies;
        MODULES_ZIP = modulesZip;
        SETTINGS = settings;
    }

    /// Whether the template is an app together with its server: the project is
    /// then Maven, has the `backend` and `shared` modules, and targets Java 17.
    public boolean isFullStack() {
        return MODULES_ZIP != null;
    }

    public boolean supportsGradle() {
        return GRADLE_UNSUPPORTED_REASON == null;
    }
}
