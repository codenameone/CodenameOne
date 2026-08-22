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
import static org.junit.jupiter.api.Assertions.assertFalse;

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
            + "}\n"
            // The generated file really does repeat both block openings: a second, top-level
            // dependency block for the instrumentation deps, and a second android block that the
            // coverage harness appends. Both are here because an insertion that lands in them is
            // the bug this fixture exists to catch.
            + "\n"
            + "dependencies {\n"
            + "    androidTestImplementation \"androidx.test:runner:1.5.2\"\n"
            + "}\n"
            + "\n"
            + "android {\n"
            + "    buildTypes {\n"
            + "        debug {\n"
            + "            testCoverageEnabled true\n"
            + "        }\n"
            + "    }\n"
            + "}\n";

    private static final String WEAR_DEPS =
            "    implementation 'androidx.wear.watchface:watchface-complications-data-source:1.2.1'\n"
            + "    implementation 'androidx.wear.tiles:tiles:1.4.1'\n"
            + "    implementation 'androidx.concurrent:concurrent-futures:1.1.0'\n"
            + "    implementation 'com.google.guava:guava:31.1-android'\n";

    /// The REAL derivation, not a copy of it. A test that reproduced the substitutions would
    /// pass while the builder drifted away from it, which is the one failure mode these
    /// assertions exist to prevent.
    private static String deriveWearGradle() {
        return AndroidGradleBuilder.deriveWearGradle(PHONE_GRADLE, 100, 101, WEAR_DEPS);
    }

    /// The source set belongs in the module's OWN android block and nowhere else. A coverage
    /// harness appends a second one to add a build type, and String.replace rewrites every
    /// occurrence -- so the plain anchor put a second copy of the source set in a block that only
    /// wanted a buildTypes entry.
    @Test
    void theSourceSetIsInsertedExactlyOnce() {
        String wear = deriveWearGradle();

        int first = wear.indexOf("java.srcDirs = ['../app/src/main/java'");
        assertTrue(first >= 0, "the shared source set must be there:\n" + wear);
        assertEquals(-1, wear.indexOf("java.srcDirs = ['../app/src/main/java'", first + 1),
                "the source set was inserted into more than one android block:\n" + wear);
    }

    /// Same reasoning one level up: the generated file carries an androidTest dependency block
    /// after the project one, and the androidx.wear libraries have no business in it.
    @Test
    void theWearDependencyIsInsertedExactlyOnce() {
        String wear = deriveWearGradle();

        int first = wear.indexOf("watchface-complications-data-source");
        assertTrue(first >= 0, "the wear dependency must be there:\n" + wear);
        assertEquals(-1, wear.indexOf("watchface-complications-data-source", first + 1),
                "the wear dependency was inserted into more than one block:\n" + wear);
        assertTrue(wear.indexOf("androidTestImplementation") > first,
                "the instrumentation block must still follow, untouched:\n" + wear);
    }

    /// android.xgradle_default_config lets a project add its own declarations inside
    /// defaultConfig, and they land AFTER the generated ones -- so rewriting the generated
    /// versionCode left the project's value effective and the Wear artifact quietly kept the
    /// phone's. A trailing block is evaluated last whatever the file above it says.
    @Test
    void theWearVersionCodeSurvivesADefaultConfigHint() {
        String withHint = PHONE_GRADLE.replace(
                "        versionCode 100\n",
                "        versionCode 100\n        versionCode 777\n");

        String wear = AndroidGradleBuilder.deriveWearGradle(withHint, 100, 101, WEAR_DEPS);

        int hint = wear.lastIndexOf("versionCode 777");
        int ours = wear.lastIndexOf("versionCode 101");
        assertTrue(hint >= 0, "the fixture must still carry the hint:\n" + wear);
        assertTrue(ours > hint,
                "the Wear version code must be declared after the hint's:\n" + wear);
    }

    /// Same reasoning for the floor: a hint declaring minSdkVersion 21 would otherwise build the
    /// Wear module below the API level the complication and Tile libraries require.
    @Test
    void theWearFloorSurvivesADefaultConfigHint() {
        String withHint = PHONE_GRADLE.replace(
                "        minSdkVersion 24\n",
                "        minSdkVersion 24\n        minSdkVersion 21\n");

        String wear = AndroidGradleBuilder.deriveWearGradle(withHint, 100, 101, WEAR_DEPS);

        int hint = wear.lastIndexOf("minSdkVersion 21");
        int ours = wear.lastIndexOf("minSdkVersion 26");
        assertTrue(hint >= 0, wear);
        assertTrue(ours > hint, "the Wear floor must be declared after the hint's:\n" + wear);
    }

    /// ...but only for the libraries that need it. A companion watch app using just the
    /// lifecycle or the Data Layer has always run on the Wear OS 2 baseline.
    @Test
    void aWatchWithNoSurfacesKeepsTheWearOsTwoBaseline() {
        String wear = AndroidGradleBuilder.deriveWearGradle(PHONE_GRADLE, 100, 101, "");

        assertFalse(wear.contains("minSdkVersion 26"),
                "nothing here needs API 26:\n" + wear);
        assertTrue(wear.contains("versionCode 101"),
                "the version code still has to outrank the phone:\n" + wear);
    }

    /// TileService.onTileRequest returns a ListenableFuture, whose class ships in
    /// com.google.guava:listenablefuture:1.0. Guava publishes the SAME coordinate at
    /// 9999.0-empty-to-avoid-conflict-with-guava holding NO classes, for builds that carry full
    /// Guava; anything pulling the marker wins the version comparison and the real jar drops out
    /// -- CameraX's graph does exactly that. Supplying Guava is what makes the marker correct.
    /// Forcing 1.0 back instead put ListenableFuture in two jars for a project whose graph
    /// already had Guava, and failed checkDuplicateClasses.
    @Test
    void aTileBringsGuavaSoListenableFutureHasSomewhereToComeFrom() {
        String deps = AndroidGradleBuilder.watchSurfaceDependencyBlock(
                "implementation", true, "1.2.1", "1.4.1", "1.2.1", "31.1-android");

        assertTrue(deps.contains("androidx.concurrent:concurrent-futures"), deps);
        assertTrue(deps.contains("com.google.guava:guava:31.1-android"),
                "the empty marker artifact leaves ListenableFuture with no provider:\n" + deps);
        assertFalse(deps.contains("resolutionStrategy"),
                "forcing the 1.0 jar duplicates the class wherever full Guava is present:\n"
                        + deps);
    }

    /// A kind with no rectangular family earns no Tile, and then none of it is needed.
    @Test
    void aComplicationOnlyKindPullsNoTileDependencies() {
        String deps = AndroidGradleBuilder.watchSurfaceDependencyBlock(
                "implementation", false, "1.2.1", "1.4.1", "1.2.1", "31.1-android");

        assertTrue(deps.contains("watchface-complications-data-source:1.2.1"), deps);
        assertFalse(deps.contains("tiles"), deps);
        assertFalse(deps.contains("guava"), deps);
        assertFalse(deps.contains("concurrent-futures"), deps);
    }

    /// A legacy support-library build writes "compile" throughout, and these lines have to match
    /// the block they are inserted into.
    @Test
    void theDependencyKeywordFollowsTheBuild() {
        String deps = AndroidGradleBuilder.watchSurfaceDependencyBlock(
                "compile", true, "1.2.1", "1.4.1", "1.2.1", "31.1-android");

        assertFalse(deps.contains("implementation "), deps);
        assertTrue(deps.contains("    compile 'com.google.guava:guava:"), deps);
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
