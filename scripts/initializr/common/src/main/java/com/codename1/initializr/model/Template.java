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
    BAREBONES(false, false, "/barebones-src.zip", "/barebones-css.zip", "/barebones-pom.xml", null, "com.example.myapp", "MyAppName", null,
            null, new String[0]),
    // The same two libraries kotlin-pom.xml's kotlin profile declares (at the Gradle
    // Kotlin version, see GeneratorModel.KOTLIN_VERSION). The Kotlin Gradle plugin
    // itself is added to the plugins {} block by GeneratorModel.
    KOTLIN(true, false, "/kotlin-src.zip", "/barebones-css.zip", "/kotlin-pom.xml", null, "com.example.myapp", "MyAppName", null,
            null, new String[] {
                    "implementation(\"org.jetbrains.kotlin:kotlin-stdlib:" + GeneratorModel.KOTLIN_VERSION + "\")",
                    "implementation(\"org.jetbrains:annotations:13.0\")"
            }),
    // Grub is written against CodeRAD 1 (its models extend the CodeRAD 1 Entity
    // class, which CodeRAD 2 turned into an interface), and that CodeRAD is the
    // bundled grub-cn1libs.zip build -- a jar with no published coordinates. A
    // Gradle project consumes cn1libs by Maven coordinates only, so there is
    // nothing to point cn1lib(...) at.
    GRUB(false, true, "/grub-src.zip", "/grub-css.zip", "/grub-pom.xml", "/grub-cn1libs.zip", "com.codename1.demos.grub", "Grub", "grub.png",
            "Grub needs the bundled CodeRAD 1 library, which is not published to a Maven repository; "
                    + "Gradle projects can only use cn1libs by Maven coordinates.",
            new String[0]),
    // Tweet's cn1libs are published, so its pom's dependencies translate directly,
    // and the Gradle plugin's generateGuiSources turns src/main/rad/views into the
    // Abstract* view classes. What stops it is the next step: the classes its sources
    // import (SignupPage, *ModelWrapper, *Controller) are written by
    // coderad-annotation-processor 2.0.5 inside javac, and that processor locates the
    // project by walking up from user.dir for a pom.xml
    // (HelperFunctions.findPom(new File(System.getProperty("user.dir"))) then
    // .getParentFile()) -- under Gradle there is none, so compileJava fails with a
    // NullPointerException. Measured against the 8.0-SNAPSHOT plugin. Clear the
    // reason once CodeRAD's processor can find a Gradle project; the lines below are
    // already the right dependencies.
    TWEET(false, true, "/tweet-src.zip", "/tweet-css.zip", "/tweet-pom.xml", null, "com.example.myapp", "MyAppName", "tweet.png",
            "The Tweet template's CodeRAD annotation processor (2.0.5) finds its project through a pom.xml, "
                    + "so it cannot run in a Gradle build yet.",
            new String[] {
                    "cn1lib(\"com.codenameone:coderad-lib:2.0.5\")",
                    "cn1lib(\"com.codenameone:tweet-app-ui-kit-lib:1.0-pre1\")",
                    "annotationProcessor(\"com.codenameone:coderad-annotation-processor:2.0.5\")"
            });

    public final String IMAGE_NAME;
    public final boolean IS_KOTLIN;
    public final boolean USES_CODERAD;
    public final String SOURCE_ZIP;
    public final String CSS;
    public final String POM_XML;
    public final String CN1LIB_ZIP;
    public final String SOURCE_PACKAGE;
    public final String SOURCE_MAIN_CLASS;
    /// Why this template cannot be generated as a Gradle project, or null when it can.
    public final String GRADLE_UNSUPPORTED_REASON;
    /// Kotlin DSL lines for the `dependencies {}` block of a Gradle project's
    /// build.gradle.kts -- the Gradle form of what the template's pom declares.
    public final String[] GRADLE_DEPENDENCIES;

    Template(boolean isKotlin, boolean usesCodeRad, String sourceZip, String css, String pomXml, String cn1libZip, String sourcePackage, String sourceMainClass,
             String imageName, String gradleUnsupportedReason, String[] gradleDependencies) {
        IS_KOTLIN = isKotlin;
        USES_CODERAD = usesCodeRad;
        SOURCE_ZIP = sourceZip;
        CSS = css;
        POM_XML = pomXml;
        CN1LIB_ZIP = cn1libZip;
        SOURCE_PACKAGE = sourcePackage;
        SOURCE_MAIN_CLASS = sourceMainClass;
        IMAGE_NAME = imageName;
        GRADLE_UNSUPPORTED_REASON = gradleUnsupportedReason;
        GRADLE_DEPENDENCIES = gradleDependencies;
    }

    public boolean supportsGradle() {
        return GRADLE_UNSUPPORTED_REASON == null;
    }
}
