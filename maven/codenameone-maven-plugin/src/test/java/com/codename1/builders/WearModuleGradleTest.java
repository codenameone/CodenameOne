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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// The Wear module's build.gradle is DERIVED from the phone module's by textual substitution,
/// which is cheap and total -- and every one of these substitutions has been wrong at least once
/// in a way no compiler could see. A generated Gradle file only fails when Gradle evaluates it,
/// which on CI is twenty minutes after the mistake.
///
/// These pin each substitution against a build.gradle shaped like the real one.
class WearModuleGradleTest {

    /// The shape that matters: a buildscript dependency block BEFORE the project one, an
    /// indented `dependencies {` and a column-zero `dependencies {`, plus the paths that have to
    /// be rewritten for a module one directory over.
    private static final String PHONE_GRADLE =
            "apply plugin: 'com.android.application'\n"
            + "buildscript {\n"
            + "    repositories {\n"
            + "        mavenCentral()\n"
            + "    }\n"
            + "    dependencies {\n"
            + "        classpath 'com.android.tools.build:gradle:8.1.4'\n"
            + "    }\n"
            + "}\n"
            + "\n"
            + "android {\n"
            + "    compileSdkVersion 34\n"
            + "    defaultConfig {\n"
            + "        applicationId \"com.mycompany.myapp\"\n"
            + "        minSdkVersion 24\n"
            + "        versionCode 100\n"
            + "    }\n"
            + "    signingConfigs {\n"
            + "        release {\n"
            + "            storeFile file(\"keyStore\")\n"
            + "        }\n"
            + "    }\n"
            + "}\n"
            + "\n"
            + "repositories {\n"
            + "    flatDir{\n"
            + "              dirs 'libs'\n"
            + "       }\n"
            + "}\n"
            + "\n"
            + "dependencies {\n"
            + "    implementation fileTree(dir: 'libs', include: ['*.jar'])\n"
            + "}\n";

    private static final String WEAR_DEPS =
            "    implementation 'androidx.wear.watchface:watchface-complications-data-source:1.2.1'\n";

    /// The REAL derivation, not a copy of it. A test that reproduced the substitutions would
    /// pass while the builder drifted away from it, which is the one failure mode these
    /// assertions exist to prevent.
    private static String deriveWearGradle() {
        return AndroidGradleBuilder.deriveWearGradle(PHONE_GRADLE, 100, 101, WEAR_DEPS);
    }

    /// The androidx.wear dependency must land in the PROJECT block. The buildscript block is
    /// indented and comes first, and String.replace hits every occurrence -- so the plain
    /// "dependencies {" anchor put an implementation() call inside buildscript's dependency
    /// handler, where the method does not exist and the whole :wear project failed to evaluate.
    @Test
    void theWearDependencyGoesInTheProjectBlockAndNotBuildscript() {
        String wear = deriveWearGradle();

        int buildscriptDeps = wear.indexOf("    dependencies {");
        int projectDeps = wear.indexOf("\ndependencies {");
        int wearDep = wear.indexOf("watchface-complications-data-source");

        assertTrue(buildscriptDeps >= 0 && projectDeps > buildscriptDeps, wear);
        assertTrue(wearDep > projectDeps,
                "the wear dependency must follow the project block, not buildscript's:\n" + wear);
    }

    /// Gradle resolves file("keyStore") relative to the project it appears in, and the key is
    /// written only to the app module -- so a verbatim copy made a release build fail to
    /// CONFIGURE, taking the phone artifact down with it.
    @Test
    void theSigningKeyIsReachedFromTheAppModule() {
        assertTrue(deriveWearGradle().contains("storeFile file(\"../app/keyStore\")"));
    }

    /// Libraries are shared rather than copied; a stale 'libs' path would resolve to an empty
    /// directory in the wear module and drop every submitted jar.
    @Test
    void librariesAreSharedFromTheAppModule() {
        String wear = deriveWearGradle();

        assertTrue(wear.contains("fileTree(dir: '../app/libs'"), wear);
        assertTrue(wear.contains("dirs '../app/libs'"), wear);
        assertTrue(!wear.contains("dir: 'libs'"), wear);
    }

    /// The watch outranks the phone so Play picks it on a watch, and only the wear module takes
    /// the 26 floor the androidx.wear libraries require.
    @Test
    void theWearModuleCarriesItsOwnVersionCodeAndFloor() {
        String wear = deriveWearGradle();

        assertTrue(wear.contains("versionCode 101"), wear);
        assertTrue(wear.contains("minSdkVersion 26"), wear);
        assertEquals(24, Integer.parseInt(
                PHONE_GRADLE.split("minSdkVersion ")[1].split("\n")[0].trim()),
                "the phone module keeps its own floor");
    }
}
