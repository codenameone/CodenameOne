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
 * The Kotlin stdlib alignment written into the generated Android
 * {@code build.gradle}.
 *
 * <p>Every case here is about restraint rather than about the block's text:
 * the alignment lands in the dependency graph of every AndroidX app, so the
 * cases that must produce nothing matter more than the one that must produce
 * something. The two that must NOT produce nothing --
 * {@link #aPreMergeKotlinPluginStillGetsTheAlignment()} and
 * {@link #pinningOneJdkArtifactLeavesTheOtherConstrained()} -- are the ones
 * that caught a real over-suppression, so treat a change that makes either
 * pass vacuously as a regression.</p>
 */
public class KotlinStdlibAlignmentTest {
    private static String block() {
        return KotlinStdlibAlignment.constraintsBlock("implementation", null);
    }

    /**
     * jdk8 is the artifact the duplicate class reports name, but the real
     * graph resolves both to the same old version, so aligning jdk8 alone
     * would move the failure onto {@code kotlin.jdk7.AutoCloseableKt} rather
     * than remove it.
     */
    @Test
    public void constrainsBothJdkArtifactsToTheShimFloor() {
        String out = block();
        check(out.contains(
                "implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.0')"),
                "jdk7 is aligned");
        check(out.contains(
                "implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0')"),
                "jdk8 is aligned");
    }

    /**
     * 1.8.0 is the first release of the two jdk artifacts that carries no
     * classes. An older floor would still leave a real jar in the graph.
     */
    @Test
    public void theFloorIsTheVersionWhereTheClassesMoved() {
        check("1.8.0".equals(KotlinStdlibAlignment.MERGED_STDLIB_FLOOR),
                "the floor is the version where the classes moved");
    }

    /** It is a constraints block, not a dependency declaration or a force. */
    @Test
    public void declaresConstraintsRatherThanDependencies() {
        String out = block();
        check(out.contains("constraints {"), "it is a constraints block");
        check(!out.contains("force"), "it constrains rather than forces");
        int open = 0;
        int close = 0;
        for (int i = 0; i < out.length(); i++) {
            if (out.charAt(i) == '{') {
                open++;
            } else if (out.charAt(i) == '}') {
                close++;
            }
        }
        check(open == close, "the block's braces balance");
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
        check(countOccurrences(out, "because '") == 2,
                "both constraints say why they are there");
        check(out.contains("Codename One"), "the reason names who wrote it");
    }

    /**
     * From 1.8.0 the Kotlin Gradle plugin aligns the jdk variants itself, so
     * the block would be a no-op.
     */
    @Test
    public void skipsOnlyAKotlinPluginThatAlignsItself() {
        check("".equals(KotlinStdlibAlignment.constraintsBlock(
                "implementation", "1.8.0")),
                "the release that starts aligning is skipped");
        check("".equals(KotlinStdlibAlignment.constraintsBlock(
                "implementation", "1.9.22")),
                "a newer plugin is skipped");
        check(KotlinStdlibAlignment.alignsItsOwnJdkVariants("2.0.0"),
                "a major bump still aligns");
        check(KotlinStdlibAlignment.alignsItsOwnJdkVariants("1.9.22-RC2"),
                "a qualifier does not hide an aligning version");
    }

    /**
     * The case that made this a version test rather than an is-a-plugin-applied
     * test. On the {@code android.useGradle8=false} path the builder selects
     * Kotlin 1.7.22, which predates the merge and does not align. Worse, the
     * 1.7 plugin ADDS {@code kotlin-stdlib-jdk8} at its own version, so the
     * pre-merge real jar is guaranteed present; any dependency reaching a
     * merged stdlib then collides with it. Measured with Gradle: plugin 1.7.22
     * plus billing 9.1.0 resolves kotlin-stdlib 1.8.22 beside jdk7/jdk8
     * 1.7.22, which is the duplicate. Skipping there shipped the bug.
     */
    @Test
    public void aPreMergeKotlinPluginStillGetsTheAlignment() {
        check(KotlinStdlibAlignment.constraintsBlock("implementation", "1.7.22")
                .contains("kotlin-stdlib-jdk8"),
                "a pre-merge plugin still gets the alignment");
        check(!KotlinStdlibAlignment.alignsItsOwnJdkVariants("1.7.22"),
                "1.7.22 does not align");
        check(!KotlinStdlibAlignment.alignsItsOwnJdkVariants("1.6.21"),
                "1.6.21 does not align");
    }

    /**
     * An app declaring {@code kotlin-gradle-plugin:$kotlin_version} parses to
     * nothing. Unknown must read as "does not align" -- guessing the other way
     * switches the fix off silently.
     */
    @Test
    public void anUnreadablePluginVersionStillGetsTheAlignment() {
        check(!KotlinStdlibAlignment.alignsItsOwnJdkVariants(""),
                "no plugin does not align");
        check(!KotlinStdlibAlignment.alignsItsOwnJdkVariants(null),
                "a null version does not align");
        check(!KotlinStdlibAlignment.alignsItsOwnJdkVariants("$kotlin_version"),
                "a Gradle variable does not read as aligning");
        check(KotlinStdlibAlignment.constraintsBlock(
                "implementation", "$kotlin_version").contains("kotlin-stdlib-jdk8"),
                "an unreadable version still gets the alignment");
    }

    /**
     * Suppression is per artifact. jdk8 depends on jdk7, so an app pinning
     * jdk8 raises jdk7 with it -- but an app pinning jdk7 leaves jdk8 exactly
     * where the graph put it, and dropping the whole block there would leave
     * the original duplicate intact with its fix switched off.
     */
    @Test
    public void pinningOneJdkArtifactLeavesTheOtherConstrained() {
        String pinnedJdk7 = KotlinStdlibAlignment.constraintsBlock(
                "implementation", null,
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.22'\n");
        check(pinnedJdk7.contains("kotlin-stdlib-jdk8"),
                "pinning jdk7 leaves jdk8 constrained");
        check(!pinnedJdk7.contains("kotlin-stdlib-jdk7:1.8.0"),
                "the artifact the app pinned is left to the app");

        String pinnedJdk8 = KotlinStdlibAlignment.constraintsBlock(
                "implementation", null,
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n");
        check(pinnedJdk8.contains("kotlin-stdlib-jdk7"),
                "pinning jdk8 leaves jdk7 constrained");
        check(!pinnedJdk8.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the artifact the app pinned is left to the app");

        String pinnedBoth = KotlinStdlibAlignment.constraintsBlock(
                "implementation", null,
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.22'\n"
                + "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n");
        check("".equals(pinnedBoth),
                "an app managing both gets no block at all, not an empty one");
    }

    /**
     * A BOM manages the whole {@code org.jetbrains.kotlin} group, jdk7 and
     * jdk8 included, so a new enough one is the single marker that suppresses
     * both constraints.
     *
     * <p>New enough is the whole point. A BOM raises the jdk artifacts but
     * cannot pull {@code kotlin-stdlib} down -- a platform contributes
     * constraints and the highest version still wins -- so a pre-merge BOM
     * leaves a merged stdlib beside class-bearing jdk jars, which is the
     * duplicate. Measured: kotlin-bom 1.7.22 against a graph wanting stdlib
     * 1.8.22 resolves jdk7/jdk8 to 1.7.22, still class-bearing.</p>
     */
    @Test
    public void emitsNothingWhenTheAppUsesTheKotlinBom() {
        check("".equals(KotlinStdlibAlignment.constraintsBlock(
                "implementation", null,
                "    implementation platform('org.jetbrains.kotlin:kotlin-bom:1.9.22')\n")),
                "an app using a merged-era Kotlin BOM is left alone");
        check("".equals(KotlinStdlibAlignment.constraintsBlock(
                "implementation", null,
                "    implementation platform('org.jetbrains.kotlin:kotlin-bom:1.8.0')\n")),
                "the BOM at the merge itself is enough");
    }

    /**
     * The counterpart, and the reason the BOM is read by version rather than
     * by presence: a pre-merge BOM does not make the graph safe, so it must
     * not switch the block off.
     */
    @Test
    public void aPreMergeKotlinBomStillGetsTheAlignment() {
        String out = KotlinStdlibAlignment.constraintsBlock(
                "implementation", null,
                "    implementation platform('org.jetbrains.kotlin:kotlin-bom:1.7.22')\n");
        check(out.contains("kotlin-stdlib-jdk7:1.8.0"),
                "a pre-merge BOM still gets jdk7 aligned");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a pre-merge BOM still gets jdk8 aligned");
    }

    /**
     * A BOM whose version is a Gradle variable reads as unknown, and unknown
     * must not suppress -- the same fail-safe the plugin version gets.
     */
    @Test
    public void anUnreadableBomVersionStillGetsTheAlignment() {
        String out = KotlinStdlibAlignment.constraintsBlock(
                "implementation", null,
                "    implementation platform(\"org.jetbrains.kotlin:kotlin-bom:$kotlinVersion\")\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an unreadable BOM version still gets the alignment");
    }

    /**
     * A commented-out declaration is not a declaration. The same hazard the
     * VPN manifest checks cover, in the same builder's hint text: a developer
     * parks a line with {@code //} and the substring match reads it as a live
     * pin, switching off the alignment for an app that pinned nothing.
     */
    @Test
    public void aCommentedOutDeclarationIsNotADeclaration() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation", null,
                "    // implementation platform('org.jetbrains.kotlin:kotlin-bom:1.9.22')\n"
                + "    // implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n");
        check(out.contains("kotlin-stdlib-jdk7:1.8.0"), "a commented-out BOM does not suppress");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"), "a commented-out pin does not suppress");

        String blockComment = KotlinStdlibAlignment.constraintsBlock("implementation", null,
                "    /* implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22' */\n");
        check(blockComment.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a block-commented pin does not suppress");
    }

    /**
     * An exclusion is the opposite of a pin. It applies only to the dependency
     * edge it is written on, so an independent path still brings the
     * class-bearing jar -- reading it as "the app manages this" removes the
     * constraint precisely where it is still needed.
     */
    @Test
    public void anExclusionIsNotAPin() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation", null,
                "    implementation('com.example:thing:1.0') {\n"
                + "        exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk8'\n"
                + "    }\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "excluding jdk8 on one edge does not switch its constraint off");
    }

    /**
     * The map form is a real pin and is honoured, so the stricter matching did
     * not simply narrow to one spelling.
     */
    @Test
    public void theMapFormCountsAsAPin() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation", null,
                "    implementation group: 'org.jetbrains.kotlin', "
                + "name: 'kotlin-stdlib-jdk8', version: '1.9.22'\n");
        check(!out.contains("kotlin-stdlib-jdk8:1.8.0"), "the map form pins jdk8");
        check(out.contains("kotlin-stdlib-jdk7:1.8.0"), "and leaves jdk7 constrained");
    }

    /**
     * A repository URL is not a comment. Stripping from every {@code //} would
     * cut {@code maven { url 'https://...' }} in half, and these fragments do
     * carry repository URLs.
     */
    @Test
    public void aUrlIsNotAComment() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation", null,
                "    maven { url 'https://example.com/repo' }\n"
                + "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n");
        check(!out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the pin after a URL line is still seen");
    }

    /**
     * The fragments arrive straight from build hints, so an unset hint shows
     * up as an empty string and an absent one can be null. Neither is a
     * reason to skip the alignment, and neither may throw.
     */
    @Test
    public void ignoresEmptyAndNullFragments() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation", null,
                "", null, "    implementation 'androidx.appcompat:appcompat:1.6.1'\n");
        check(out.contains("kotlin-stdlib-jdk8"),
                "an empty or absent hint is not a pin");
    }

    /**
     * An unrelated Kotlin coordinate is not a pin. Only the two jdk artifacts
     * and the BOM decide who owns the alignment; matching "kotlin" loosely
     * would silently switch the fix off for any app that happens to use a
     * Kotlin library.
     */
    @Test
    public void anUnrelatedKotlinDependencyIsNotAPin() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation", null,
                "    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4'\n");
        check(out.contains("kotlin-stdlib-jdk8"),
                "a coroutines dependency does not switch the alignment off");
    }

    /**
     * A pre-AndroidX project declares its dependencies on {@code compile},
     * and a constraints block on a configuration the project does not have
     * fails evaluation rather than being ignored.
     */
    @Test
    public void usesTheConfigurationItWasGiven() {
        String out = KotlinStdlibAlignment.constraintsBlock("compile", null);
        check(out.contains("compile('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0')"),
                "the caller's configuration is used");
        check(!out.contains("implementation("),
                "no other configuration is assumed");
    }

    @Test
    public void emitsNothingWithoutAConfiguration() {
        check("".equals(KotlinStdlibAlignment.constraintsBlock(null, null)),
                "a null configuration writes nothing");
        check("".equals(KotlinStdlibAlignment.constraintsBlock("   ", null)),
                "a blank configuration writes nothing");
    }

    /**
     * The block is concatenated inside the generated {@code dependencies}
     * block between the last dependency and the closing brace, so it has to
     * both start and end on its own line.
     */
    @Test
    public void isNewlineTerminatedForConcatenation() {
        String out = block();
        check(out.endsWith("}\n"), "it ends its own line");
        check(out.startsWith("    constraints {\n"), "it starts its own line");
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

    private static void check(boolean condition, String message) {
        assertTrue(condition, message);
    }
}
