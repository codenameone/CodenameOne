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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Kotlin stdlib alignment written into the generated Android
 * {@code build.gradle}.
 *
 * <p>Every case here is about restraint rather than about the block's text:
 * the alignment lands in the dependency graph of every AndroidX app, so the
 * cases that must produce nothing matter more than the one that must produce
 * something. The floor gets a test of its own because 1.8.0 is not a
 * preference -- it is the release where the two artifacts became empty
 * shims, and lowering it would reintroduce the duplicate class the block
 * exists to prevent.</p>
 */
public class KotlinStdlibAlignmentTest {

    private static String block() {
        return KotlinStdlibAlignment.constraintsBlock("implementation", false);
    }

    @Test
    public void constrainsBothJdkArtifactsToTheShimFloor() {
        String out = block();
        assertTrue(out.contains(
                "implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.0')"));
        assertTrue(out.contains(
                "implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0')"));
    }

    /**
     * jdk8 is the artifact that shows up in the duplicate class reports, but
     * it depends on jdk7, so leaving jdk7 alone would move the same failure
     * onto {@code kotlin.jdk7.AutoCloseableKt} rather than remove it.
     */
    @Test
    public void doesNotAlignJdk8Alone() {
        assertTrue(block().contains("kotlin-stdlib-jdk7"));
    }

    /**
     * 1.8.0 is the first release of the two jdk artifacts that carries no
     * classes. An older floor would still leave a real jar in the graph.
     */
    @Test
    public void theFloorIsTheVersionWhereTheClassesMoved() {
        assertEquals("1.8.0", KotlinStdlibAlignment.MERGED_STDLIB_FLOOR);
    }

    /** It is a constraints block, not a dependency declaration or a force. */
    @Test
    public void declaresConstraintsRatherThanDependencies() {
        String out = block();
        assertTrue(out.contains("constraints {"));
        assertFalse(out.contains("force"));
        int open = 0;
        int close = 0;
        for (int i = 0; i < out.length(); i++) {
            if (out.charAt(i) == '{') {
                open++;
            } else if (out.charAt(i) == '}') {
                close++;
            }
        }
        assertEquals(open, close);
    }

    /**
     * Gradle prints the reason next to the raised version in
     * {@code dependencyInsight}, and this constraint corresponds to nothing
     * in the developer's own project, so an unattributed one is a support
     * question waiting to happen.
     */
    @Test
    public void everyConstraintCarriesAReason() {
        String out = block();
        assertEquals(2, countOccurrences(out, "because '"));
        assertTrue(out.contains("Codename One"));
    }

    /**
     * The Kotlin Gradle plugin performs the same alignment itself, and the
     * Kotlin version a build compiles with can be older than the floor, so
     * pushing a newer stdlib underneath it would only earn a warning.
     */
    @Test
    public void emitsNothingWhenTheKotlinGradlePluginIsApplied() {
        assertEquals("", KotlinStdlibAlignment.constraintsBlock("implementation", true));
    }

    @Test
    public void emitsNothingWhenTheAppPinsAJdkArtifactItself() {
        assertEquals("", KotlinStdlibAlignment.constraintsBlock("implementation", false,
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n"));
        assertEquals("", KotlinStdlibAlignment.constraintsBlock("implementation", false,
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.22'\n"));
    }

    /**
     * A BOM aligns the whole {@code org.jetbrains.kotlin} group, which is a
     * superset of this block, so an app using one has already answered the
     * question.
     */
    @Test
    public void emitsNothingWhenTheAppUsesTheKotlinBom() {
        assertEquals("", KotlinStdlibAlignment.constraintsBlock("implementation", false,
                "    implementation platform('org.jetbrains.kotlin:kotlin-bom:1.9.22')\n"));
    }

    /**
     * The fragments arrive straight from build hints, so an unset hint shows
     * up as an empty string and an absent one can be null. Neither is a
     * reason to skip the alignment, and neither may throw.
     */
    @Test
    public void ignoresEmptyAndNullFragments() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation", false,
                "", null, "    implementation 'androidx.appcompat:appcompat:1.6.1'\n");
        assertTrue(out.contains("kotlin-stdlib-jdk8"));
        assertFalse(KotlinStdlibAlignment.appManagesKotlinStdlib((String[]) null));
    }

    /**
     * An unrelated Kotlin coordinate is not a pin. Only the two jdk
     * artifacts and the BOM decide who owns the alignment; matching
     * "kotlin" loosely would silently switch the fix off for any app that
     * happens to use a Kotlin library.
     */
    @Test
    public void anUnrelatedKotlinDependencyIsNotAPin() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation", false,
                "    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4'\n");
        assertTrue(out.contains("kotlin-stdlib-jdk8"));
    }

    /**
     * A pre-AndroidX project declares its dependencies on {@code compile},
     * and a constraints block on a configuration the project does not have
     * fails evaluation rather than being ignored.
     */
    @Test
    public void usesTheConfigurationItWasGiven() {
        String out = KotlinStdlibAlignment.constraintsBlock("compile", false);
        assertTrue(out.contains("compile('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0')"));
        assertFalse(out.contains("implementation("));
    }

    @Test
    public void emitsNothingWithoutAConfiguration() {
        assertEquals("", KotlinStdlibAlignment.constraintsBlock(null, false));
        assertEquals("", KotlinStdlibAlignment.constraintsBlock("   ", false));
    }

    /**
     * The block is concatenated inside the generated {@code dependencies}
     * block between the last dependency and the closing brace, so it has to
     * both start and end on its own line.
     */
    @Test
    public void isNewlineTerminatedForConcatenation() {
        String out = block();
        assertTrue(out.endsWith("}\n"));
        assertTrue(out.startsWith("    constraints {\n"));
    }

    /**
     * The half a unit test of the helper cannot see. The helper returning the
     * right text is worthless if the builder stops concatenating it, and that
     * is a one-character deletion in a 100-line string expression nothing
     * else would notice -- the build stays green and the duplicate class
     * comes back.
     *
     * <p>Source text, because the expression is a local inside a method
     * thousands of lines long that cannot be called without a whole staged
     * Android project.</p>
     */
    @Test
    public void theBuilderStillWritesItIntoTheDependenciesBlock() throws Exception {
        byte[] bytes = java.nio.file.Files.readAllBytes(new java.io.File(
                "src/main/java/com/codename1/builders/AndroidGradleBuilder.java").toPath());
        String src = new String(bytes, "UTF-8");
        int at = src.indexOf("\"dependencies {\\n\"");
        assertTrue(at >= 0);
        String block = src.substring(at, src.indexOf("+ \"}\\n\"", at));
        assertTrue(block.contains("+ kotlinStdlibConstraints"));
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int at = haystack.indexOf(needle);
        while (at >= 0) {
            count++;
            at = haystack.indexOf(needle, at + needle.length());
        }
        return count;
    }
}
