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
 * {@link #aKotlinPluginNoLongerExcusesTheBlock()} and
 * {@link #pinningOneJdkArtifactLeavesTheOtherConstrained()} -- are the ones
 * that caught a real over-suppression, so treat a change that makes either
 * pass vacuously as a regression.</p>
 */
public class KotlinStdlibAlignmentTest {
    private static String block() {
        return KotlinStdlibAlignment.constraintsBlock("implementation");
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
     * A Kotlin plugin no longer excuses the block, whatever its version.
     *
     * <p>Skipping for a 1.8+ plugin was never load-bearing -- measured against
     * a graph carrying billing 9.1.0 and appcompat 1.6.1, adding this block
     * alongside plugin 1.9.22 and 1.8.22 produced byte-identical resolution --
     * and it was not sound either, because the plugin's alignment can be
     * turned off with kotlin.stdlib.jdk.variants.version.alignment=false,
     * which this builder preserves out of a project's gradle.properties.
     * Emitting unconditionally answers both, and takes the version parsing and
     * the commented-plugin hazard with it.</p>
     */
    @Test
    public void aKotlinPluginNoLongerExcusesTheBlock() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation");
        check(out.contains("kotlin-stdlib-jdk7:1.8.0"), "jdk7 is aligned regardless");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"), "jdk8 is aligned regardless");
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
                "implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.22'\n");
        check(pinnedJdk7.contains("kotlin-stdlib-jdk8"),
                "pinning jdk7 leaves jdk8 constrained");
        check(!pinnedJdk7.contains("kotlin-stdlib-jdk7:1.8.0"),
                "the artifact the app pinned is left to the app");

        String pinnedJdk8 = KotlinStdlibAlignment.constraintsBlock(
                "implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n");
        check(pinnedJdk8.contains("kotlin-stdlib-jdk7"),
                "pinning jdk8 leaves jdk7 constrained");
        check(!pinnedJdk8.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the artifact the app pinned is left to the app");

        String pinnedBoth = KotlinStdlibAlignment.constraintsBlock(
                "implementation",
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
                "implementation",
                "    implementation platform('org.jetbrains.kotlin:kotlin-bom:1.9.22')\n")),
                "an app using a merged-era Kotlin BOM is left alone");
        check("".equals(KotlinStdlibAlignment.constraintsBlock(
                "implementation",
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
                "implementation",
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
                "implementation",
                "    implementation platform(\"org.jetbrains.kotlin:kotlin-bom:$kotlinVersion\")\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an unreadable BOM version still gets the alignment");
    }

    /**
     * The builder has to hand over every app-controlled fragment that reaches
     * the generated dependencies block, not the ones that came to mind.
     * android.supportv4Dep was missed that way: it is written into that block
     * a few lines below the constraints, so an app pinning a jdk artifact
     * through it would have had the pin ignored and the constraint written
     * over the top.
     *
     * <p>The list is checked against the builder's source rather than
     * re-derived, because the failure is an omission and an omission is
     * invisible to a test that only exercises what is passed.</p>
     */
    @Test
    public void theBuilderPassesEveryAppControlledDependencyFragment() throws Exception {
        byte[] bytes = java.nio.file.Files.readAllBytes(new java.io.File(
                "src/main/java/com/codename1/builders/AndroidGradleBuilder.java").toPath());
        String src = new String(bytes, "UTF-8");
        int at = src.indexOf("KotlinStdlibAlignment.constraintsBlock(");
        check(at >= 0, "the builder calls the alignment");
        // To the statement terminator, not to "));" -- that lands on the closing paren
        // of the LAST argument and slices it in half, so the final fragment never
        // matched and the check failed for the wrong reason.
        String call = src.substring(at, src.indexOf(";", at));
        // The call form, not the bare hint name: the comment above the argument list
        // names android.supportv4Dep too, so matching the name alone passed with the
        // argument deleted. Checked by deleting it, which is the only way that kind of
        // vacuity shows up.
        String[] fragments = {
            "additionalDependencies,",
            "aiExtraGradleDependencies.toString(),",
            "request.getArg(\"android.gradleDep\", \"\")",
            "request.getArg(\"android.supportv4Dep\", \"\")",
            "request.getArg(\"android.xgradle\", \"\")",
        };
        for (String fragment : fragments) {
            check(call.contains(fragment),
                    "the alignment is not told about " + fragment
                    + ", which reaches the generated dependencies block");
        }
    }

    /**
     * A commented-out declaration is not a declaration. The same hazard the
     * VPN manifest checks cover, in the same builder's hint text: a developer
     * parks a line with {@code //} and the substring match reads it as a live
     * pin, switching off the alignment for an app that pinned nothing.
     */
    @Test
    public void aCommentedOutDeclarationIsNotADeclaration() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    // implementation platform('org.jetbrains.kotlin:kotlin-bom:1.9.22')\n"
                + "    // implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n");
        check(out.contains("kotlin-stdlib-jdk7:1.8.0"), "a commented-out BOM does not suppress");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"), "a commented-out pin does not suppress");

        String blockComment = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    /* implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22' */\n");
        check(blockComment.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a block-commented pin does not suppress");
    }

    /**
     * A declaration on a variant or test configuration does not reach the one
     * the constraints are written on, so it cannot stand in for a pin.
     * debugImplementation of a new-enough BOM constrains the debug variant
     * alone -- suppressing on it removes the constraint from the release build
     * that still needs it, and the release build is the one that ships.
     */
    @Test
    public void aVariantOnlyDeclarationDoesNotSuppress() {
        String debugBom = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    debugImplementation platform('org.jetbrains.kotlin:kotlin-bom:1.9.22')\n");
        check(debugBom.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a debug-only BOM does not suppress the main variant");

        String testPin = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    testImplementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n");
        check(testPin.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a test-only pin does not suppress the main variant");

        String releasePin = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    releaseImplementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n");
        check(releasePin.contains("kotlin-stdlib-jdk8:1.8.0"),
                "even a release-only pin is not the configuration being constrained");
    }

    /**
     * Every main-variant configuration reaches a classpath the constraint also
     * reaches, so a pin on any of them is the app managing the artifact.
     * runtimeOnly is the one that made this a list rather than two names: a
     * strict pin there did not get overridden by the emitted 1.8.0 constraint,
     * it made the resolution fail outright.
     */
    @Test
    public void aPinOnAnyMainConfigurationSuppresses() {
        String[] configurations = {"implementation", "api", "runtimeOnly",
            "compile", "runtime"};
        for (String configuration : configurations) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    " + configuration
                    + "('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22')\n");
            check(!out.contains("kotlin-stdlib-jdk8:1.8.0"),
                    "a pin on " + configuration + " is the app managing jdk8");
            check(out.contains("kotlin-stdlib-jdk7:1.8.0"),
                    "and jdk7 is still constrained after a " + configuration + " pin");
        }
    }

    /**
     * compileOnly is NOT one of them, and adding it by symmetry was the
     * mistake. A compileOnly declaration is absent from the release runtime
     * classpath, which is the one checkReleaseDuplicateClasses reads, so
     * treating it as management of that graph drops the constraint from a
     * classpath the app never touched and leaves the duplicate in place.
     */
    @Test
    public void aCompileOnlyDeclarationDoesNotManageTheRuntimeGraph() {
        String bom = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    compileOnly platform('org.jetbrains.kotlin:kotlin-bom:1.9.22')\n");
        check(bom.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a compileOnly BOM does not align the runtime graph");
    }

    /**
     * A strict version ends the question wherever it is declared, because a
     * constraint cannot coexist with one on any classpath both reach.
     * Measured with Gradle: the same graph resolves on its own and fails with
     * this block's constraint added --
     * "Could not resolve kotlin-stdlib-jdk8:{strictly 1.7.22}". That is worse
     * than the duplicate, because the app cannot work around it.
     */
    @Test
    public void aStrictPinIsHonouredOnAnyConfiguration() {
        String releaseStrict = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    releaseImplementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') {\n"
                + "        version { strictly '1.7.22' }\n"
                + "    }\n");
        check(!releaseStrict.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a strict release pin is left to the app");

        String compileOnlyStrict = KotlinStdlibAlignment.constraintsBlock(
                "implementation",
                "    compileOnly('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ version { strictly '1.7.22' } }\n");
        check(!compileOnlyStrict.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a strict compileOnly pin is honoured even though compileOnly alone is not");
    }

    /**
     * The block absorbed for that check belongs to the declaration that opened
     * it and no further. A dependencies or android block must not swallow the
     * fragment: only a statement already naming the Kotlin group absorbs one.
     */
    @Test
    public void anUnrelatedBlockDoesNotSwallowTheFragment() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "dependencies {\n"
                + "    implementation('com.example:thing:1.0') { version { strictly '1.0' } }\n"
                + "    debugImplementation platform('org.jetbrains.kotlin:kotlin-bom:1.9.22')\n"
                + "}\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an unrelated strict block does not suppress, and the debug BOM still does not");
    }

    /**
     * And their variant and test forms still do not, which is the property the
     * whole-token lowercase match buys without listing a single variant name.
     */
    @Test
    public void theVariantFormsOfThoseConfigurationsStillDoNot() {
        String[] variants = {"testRuntimeOnly", "debugRuntimeOnly", "androidTestImplementation",
            "releaseCompileOnly", "debugApi", "testCompile"};
        for (String variant : variants) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    " + variant
                    + "('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22')\n");
            check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                    variant + " does not reach the constrained configuration");
        }
    }

    /**
     * api is a real pin on the main variant and is honoured, so the
     * configuration filter did not narrow to a single keyword.
     */
    @Test
    public void anApiDeclarationCountsAsAPin() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    api 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n");
        check(!out.contains("kotlin-stdlib-jdk8:1.8.0"), "api pins jdk8");
        check(out.contains("kotlin-stdlib-jdk7:1.8.0"), "and leaves jdk7 constrained");
    }

    /**
     * An exclusion is the opposite of a pin. It applies only to the dependency
     * edge it is written on, so an independent path still brings the
     * class-bearing jar -- reading it as "the app manages this" removes the
     * constraint precisely where it is still needed.
     */
    @Test
    public void anExclusionIsNotAPin() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('com.example:thing:1.0') {\n"
                + "        exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk8'\n"
                + "    }\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "excluding jdk8 on one edge does not switch its constraint off");
    }

    /**
     * A declaration wrapped across lines is still a declaration. The
     * configuration and the coordinate land on different physical lines, and
     * reading them separately ignored an explicit pin and wrote the constraint
     * over it -- the opposite of what naming the artifact in a build hint is
     * documented to do.
     */
    @Test
    public void aDeclarationSplitAcrossLinesIsStillAPin() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation(\n"
                + "        'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'\n"
                + "    )\n");
        check(!out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a wrapped declaration pins jdk8");
        check(out.contains("kotlin-stdlib-jdk7:1.8.0"),
                "and leaves jdk7 constrained");
    }

    /**
     * An inline exclusion on a declaring line does not cancel the declaration.
     * Dropping the whole line for containing "exclude" threw away a real pin;
     * only what follows the exclusion has to be ignored.
     */
    @Test
    public void anInlineExclusionDoesNotCancelTheDeclaration() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ exclude group: 'com.example', module: 'thing' }\n");
        check(!out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the declaration survives its own inline exclusion");
    }

    /**
     * And the standalone exclusion still is not a pin -- truncating at
     * "exclude" leaves nothing in front of it.
     */
    @Test
    public void aStandaloneExclusionIsStillNotAPin() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('com.example:thing:1.0') {\n"
                + "        exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk8'\n"
                + "    }\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an exclusion on its own line is still not a pin");
    }

    /**
     * A semicolon ends a statement, because this builder tells developers to
     * separate android.gradleDep statements "with ';' or a newline" -- two
     * declarations on one line is the documented shape, not an edge case.
     * Splitting on newlines alone let the first statement's configuration
     * token pair with the second statement's coordinate, so a debug-only BOM
     * read as a main-variant one and suppressed everything.
     */
    @Test
    public void aSemicolonEndsAStatement() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'com.android.billingclient:billing:9.1.0'; "
                + "debugImplementation platform('org.jetbrains.kotlin:kotlin-bom:1.9.22')\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a debug BOM after a semicolon does not borrow the previous "
                + "statement's configuration");

        // The same shape where the pin IS on the main variant still suppresses, so
        // the split did not simply stop every semicolon-separated value from working.
        String pinned = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'com.android.billingclient:billing:9.1.0'; "
                + "implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n");
        check(!pinned.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a real pin after a semicolon is still a pin");
    }

    /**
     * A semicolon inside a string or inside parentheses is not a separator.
     */
    @Test
    public void aSemicolonInsideAStringOrParensIsNotASeparator() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation(\n"
                + "        'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'\n"
                + "    )\n");
        check(!out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a wrapped declaration still pins");

        String quoted = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22' "
                + "// note; with a semicolon\n");
        check(!quoted.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a semicolon in a trailing comment does not split the declaration off");
    }

    /**
     * Unbalanced parentheses must not glue the fragment into one line: that
     * would let a configuration from one statement and a coordinate from
     * another read as a single declaration, and suppression is the direction
     * that must never be reached by accident.
     */
    @Test
    public void unbalancedParenthesesDoNotGlueStatementsTogether() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation(\n"
                + "    testImplementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a dangling paren does not turn a test-only pin into a main-variant one");
    }

    /**
     * The map form is a real pin and is honoured, so the stricter matching did
     * not simply narrow to one spelling.
     */
    @Test
    public void theMapFormCountsAsAPin() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
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
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
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
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
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
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
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
        String out = KotlinStdlibAlignment.constraintsBlock("compile");
        check(out.contains("compile('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0')"),
                "the caller's configuration is used");
        check(!out.contains("implementation("),
                "no other configuration is assumed");
    }

    @Test
    public void emitsNothingWithoutAConfiguration() {
        check("".equals(KotlinStdlibAlignment.constraintsBlock(null)),
                "a null configuration writes nothing");
        check("".equals(KotlinStdlibAlignment.constraintsBlock("   ")),
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
