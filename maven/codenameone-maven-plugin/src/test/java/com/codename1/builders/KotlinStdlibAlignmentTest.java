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

/**
 * The alignment is a Gradle constraint plus one blunt reason not to write it.
 *
 * <p>These cover what the feature promises: the graph it fixes, the graphs it
 * must not touch, and the guarantee that it can never fail a build. There is
 * deliberately nothing here about Groovy syntax -- the class no longer reads
 * any, and the suite that did was 5,652 lines chasing spellings that never
 * changed an outcome.</p>
 */
class KotlinStdlibAlignmentTest {

    private static final String JDK7 = "org.jetbrains.kotlin:kotlin-stdlib-jdk7";
    private static final String JDK8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8";

    /**
     * The graph this exists for: the old shim arrives transitively and the app's
     * own Gradle never mentions Kotlin at all.
     */
    @Test
    void aGraphThatNamesNoKotlinIsAligned() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'androidx.appcompat:appcompat:1.6.1'\n",
                "    implementation 'com.android.billingclient:billing:9.1.0'\n");
        assertTrue(out.contains("'" + JDK7 + ":1.8.0'"), "jdk7 is raised: " + out);
        assertTrue(out.contains("'" + JDK8 + ":1.8.0'"), "jdk8 is raised: " + out);
        assertTrue(out.startsWith("    constraints {"), "as a constraints block: " + out);
        assertTrue(out.contains("because 'Codename One:"),
                "with a because, which is what dependencyInsight prints: " + out);
    }

    /** A constraint pulls nothing into a graph that does not have it. */
    @Test
    void anEmptyProjectStillGetsTheFloor() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation", "");
        assertTrue(out.contains(":1.8.0"), "the block is written unconditionally");
    }

    /**
     * The constraint goes on the configuration the caller is already using, so a
     * legacy {@code compile} project stays consistent with itself.
     */
    @Test
    void theConstraintFollowsTheCallersConfiguration() {
        assertTrue(KotlinStdlibAlignment.constraintsBlock("compile", "")
                .contains("compile('" + JDK7 + ":1.8.0')"), "compile");
        assertTrue(KotlinStdlibAlignment.constraintsBlock("implementation", "")
                .contains("implementation('" + JDK7 + ":1.8.0')"), "implementation");
        assertTrue("".equals(KotlinStdlibAlignment.constraintsBlock(null, "")),
                "and no configuration means no block");
        assertTrue("".equals(KotlinStdlibAlignment.constraintsBlock("  ", "")),
                "nor does a blank one");
    }

    /**
     * An ordinary version is a SOFT requirement in Gradle: the constraint raises
     * it and the two agree. Declaring the shim is therefore not a reason to
     * stand down -- if it were, the app that declares an old one directly would
     * keep the duplicate this exists to remove.
     */
    @Test
    void anOrdinaryDeclarationIsRaisedNotHonoured() {
        assertTrue(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation '" + JDK8 + ":1.7.22'\n")
                        .contains(":1.8.0"),
                "a pre-merge declaration is raised");
        assertTrue(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation '" + JDK8 + ":1.9.22'\n")
                        .contains(":1.8.0"),
                "and a merged-era one is unaffected by a floor beneath it");
    }

    /**
     * The one thing a constraint at the floor can break: an app that firmly
     * holds a member of the family below it resolves coherently today, and a
     * constraint requiring 1.8.0 turns that into a resolution failure.
     */
    @Test
    void anAppThatPinsTheFamilyIsLeftAlone() {
        String[] pinned = {
            "    implementation '" + JDK8 + ":1.7.22!!'\n",
            "    implementation('" + JDK8 + "') { version { strictly '1.7.22' } }\n",
            "    configurations.all { resolutionStrategy.force '" + JDK8 + ":1.7.22' }\n",
            "    implementation('" + JDK8 + "') { version { reject '[1.8.0,)' } }\n",
            "    implementation(enforcedPlatform("
                    + "'org.jetbrains.kotlin:kotlin-stdlib-bom:1.7.22'))\n",
            "    configurations.all { resolutionStrategy.eachDependency { d ->\n"
                    + "        if (d.requested.name == 'kotlin-stdlib') "
                    + "d.useVersion '1.7.22'\n    } }\n",
            "    configurations.all { resolutionStrategy.componentSelection { all { s ->\n"
                    + "        if (s.candidate.module == 'kotlin-stdlib-jdk8') "
                    + "s.reject('x')\n    } } }\n",
            "    configurations.all { resolutionStrategy.failOnVersionConflict() }\n"
                    + "    implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'\n",
        };
        for (int i = 0; i < pinned.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    pinned[i]);
            assertTrue("".equals(out),
                    "<<" + pinned[i].trim() + ">> holds the family, got <<" + out + ">>");
        }
    }

    /**
     * Both halves are required, and the asymmetry is deliberate. A pinning word
     * with no mention of this family cannot be pinning it; a mention with no
     * pinning word is an ordinary declaration, which the constraint raises.
     */
    @Test
    void bothHalvesOfTheGuardAreRequired() {
        assertTrue(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    configurations.all { resolutionStrategy.force "
                        + "'com.squareup.okhttp3:okhttp:4.0.0' }\n")
                        .contains(":1.8.0"),
                "a force on someone else is not a pin on this family");
        assertTrue(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.9.22'\n")
                        .contains(":1.8.0"),
                "and naming the family without pinning it is an ordinary declaration");

        // It over-suppresses on purpose: the words are matched as plain text, so
        // one in a comment or an unrelated string counts. That costs an app the
        // duplicate it already had, which android.kotlinStdlibAlignment=false
        // does deliberately; the other direction breaks a build that works.
        assertTrue("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    // we used to force kotlin-stdlib here\n")),
                "a pinning word in a comment stands it down, which is the safe way "
                        + "to be wrong");
    }

    /** The floor is the version at which the shims became empty. */
    @Test
    void theFloorIsWhereTheClassesMoved() {
        assertTrue("1.8.0".equals(KotlinStdlibAlignment.MERGED_STDLIB_FLOOR),
                "1.8.0 is where kotlin-stdlib absorbed the jdk7/jdk8 classes");
    }

    /**
     * Null and empty fragments are ordinary input: the builder passes whatever
     * hints the project happens to have, and most projects have none of them.
     */
    @Test
    void missingFragmentsAreNotAnError() {
        assertTrue(KotlinStdlibAlignment.constraintsBlock("implementation",
                        (String[]) null).contains(":1.8.0"),
                "no fragments at all");
        assertTrue(KotlinStdlibAlignment.constraintsBlock("implementation",
                        null, "", null).contains(":1.8.0"),
                "and a mix of null and empty ones");
    }

    /**
     * Every fragment the app controls has to reach the scan. A hint that is
     * added to the generated script and not passed here is a pin this cannot
     * see, which is the one way to get the dangerous answer.
     */
    @Test
    void theBuilderPassesEveryAppControlledFragment() throws Exception {
        String src = new String(java.nio.file.Files.readAllBytes(new java.io.File(
                "src/main/java/com/codename1/builders/AndroidGradleBuilder.java")
                .toPath()), "UTF-8");
        int at = src.indexOf("KotlinStdlibAlignment.constraintsBlock(");
        assertTrue(at >= 0, "the builder calls the alignment");
        String call = src.substring(at, src.indexOf(";", at));
        String[] hints = {
            "android.gradlePlugin", "android.gradle.androidx",
            "android.xgradle_default_config", "android.supportv4Dep",
            "android.gradleDep", "android.xgradle",
        };
        for (String hint : hints) {
            // With the quotes. One hint name is a prefix of another, so a bare
            // contains() stayed true after the argument was deleted.
            assertTrue(call.contains("\"" + hint + "\""),
                    "the alignment is not told about the " + hint
                            + " hint, which reaches the generated script");
        }
        String[] locals = {
            "kotlinRuntimeDependency", "additionalDependencies",
            "aiExtraGradleDependencies", "aarDependencies", "injectRepo",
            "gradleDependency",
        };
        for (String local : locals) {
            assertTrue(call.contains(local),
                    "the alignment is not told about " + local
                            + ", which reaches the generated script");
        }
    }

    /**
     * The alignment is an optimisation over a build that already worked apart
     * from one duplicate class, and it runs on every AndroidX build -- so its
     * worst case has to be "emit nothing", never a failed build.
     */
    @Test
    void theAlignmentCannotFailTheBuild() {
        String[] hostile = {
            null, "", "   ", "'", "{", "}", "(((", ")))",
            "implementation '", "kotlin-stdlib", "!!", "strictly",
            "kotlin-stdlib   strictly",
        };
        for (int i = 0; i < hostile.length; i++) {
            KotlinStdlibAlignment.constraintsBlock("implementation", hostile[i]);
            KotlinStdlibAlignment.appPinsTheStdlibFamily(hostile[i]);
        }
        assertTrue(true, "no input produces an exception");
    }
}
