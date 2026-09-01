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
            check("".equals(out),
                    "a below-floor pin on " + configuration + " suppresses BOTH shims, "
                    + "since raising the sibling would strand the pinned one");
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
     * An exclusion written before the version block must not take the strict
     * marker with it. Cutting the statement from {@code exclude} to its end
     * did exactly that, and losing the strict marker is what turns this
     * class's constraint into a failed resolution rather than an override.
     */
    @Test
    public void anExclusionBeforeTheVersionBlockDoesNotHideTheStrictPin() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    compileOnly('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ exclude group: 'x', module: 'y'; version { strictly '1.7.22' } }\n");
        check(!out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the strict pin survives an exclusion written before it");

        String multiline = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    compileOnly('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') {\n"
                + "        exclude group: 'x', module: 'y'\n"
                + "        version { strictly '1.7.22' }\n"
                + "    }\n");
        check(!multiline.contains("kotlin-stdlib-jdk8:1.8.0"),
                "and the same written across lines");
    }

    /**
     * The English word is not the Gradle call. A reason string reading
     * "not strictly required outside debug" is prose, and reading it as a
     * strict version let a variant-only dependency switch the alignment off
     * for the release build -- the unsafe direction.
     */
    @Test
    public void theWordStrictlyInsideAStringIsNotAStrictPin() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    debugImplementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ because 'not strictly required outside debug' }\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a reason mentioning the word does not suppress the release constraint");

        // and the real call still does, so the tightening did not disarm it
        String real = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    debugImplementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ version { strictly '1.7.22' } }\n");
        check(!real.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an actual strict call is still honoured");
    }

    /**
     * Groovy allows whitespace around a map entry's colon, and the exact
     * substring match missed it. It matters because the same declaration can
     * carry a strict version, and missing it turns the constraint into a
     * failed resolution rather than an override.
     */
    @Test
    public void aMapEntryMayHaveSpaceAroundItsColon() {
        String spaced = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation(group : 'org.jetbrains.kotlin', "
                + "name : 'kotlin-stdlib-jdk8', version : '1.7.22')\n");
        check("".equals(spaced),
                "a spaced map entry still pins jdk8, below the floor so both go");

        String doubleQuoted = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation(group: \"org.jetbrains.kotlin\", "
                + "name:\"kotlin-stdlib-jdk8\", version: \"1.7.22\")\n");
        check("".equals(doubleQuoted),
                "and so does an unspaced double-quoted one");

        // A different artifact in the same shape must still not count.
        String other = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation(group : 'org.jetbrains.kotlin', "
                + "name : 'kotlin-reflect', version : '1.9.22')\n");
        check(other.contains("kotlin-stdlib-jdk8:1.8.0"),
                "naming a different Kotlin artifact does not pin jdk8");
    }

    /**
     * A strict pin on kotlin-stdlib itself blocks both shims, not one. The
     * shim at this floor depends on kotlin-stdlib at the same floor, so an app
     * strictly holding the base library below it cannot resolve either
     * constraint -- and the pre-merge family it is holding had no duplicate to
     * begin with, so constraining there turns a working build into
     * "Could not resolve ... {strictly 1.7.22}".
     */
    @Test
    public void aStrictPinOnTheBaseStdlibBlocksBothShims() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib:1.7.22') "
                + "{ version { strictly '1.7.22' } }\n");
        check("".equals(out),
                "a strict pre-merge base pin writes no constraints at all");

        // At or above the floor there is no conflict, so the block still goes in:
        // a shim requiring 1.8.0 is satisfied by a strict 1.9.22.
        String modern = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib:1.9.22') "
                + "{ version { strictly '1.9.22' } }\n");
        check(modern.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a strict modern base pin does not need the block suppressed");
    }

    /**
     * The two shims cannot be suppressed independently below the merge floor.
     * Measured with Gradle: an app pinning the whole family at 1.7.22 resolves
     * with no duplicate, and emitting only the surviving sibling raises
     * kotlin-stdlib to 1.8.0 -- which carries the jdk8 classes -- beside the
     * app's class-bearing jdk8 1.7.22 jar. That is this block manufacturing the
     * duplicate it exists to prevent, in a graph the app had arranged correctly.
     */
    @Test
    public void aPreMergeShimPinSuppressesItsSiblingToo() {
        String jdk8Pinned = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'\n");
        check("".equals(jdk8Pinned),
                "a pre-merge jdk8 pin takes the jdk7 constraint with it");

        String jdk7Pinned = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.22'\n");
        check("".equals(jdk7Pinned),
                "and the same the other way round");
    }

    /**
     * Above the floor they stay independent, because the sibling constraint
     * cannot strand a shim that is already merged-era. Without this the fix
     * above would have been "suppress everything whenever the app mentions
     * either artifact", which gives up alignment an app still needs.
     */
    @Test
    public void aMergedEraShimPinStillLeavesTheSiblingConstrained() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n");
        check(out.contains("kotlin-stdlib-jdk7:1.8.0"),
                "jdk7 is still constrained beside a merged-era jdk8 pin");
        check(!out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "and jdk8 is left to the app");
    }

    /**
     * A def reference whose closure spans lines needs the definition folded in
     * BEFORE closures are merged: the merge only absorbs into a statement that
     * already names the Kotlin group, and a statement referring to the
     * coordinate through a variable does not name it until the fold happens.
     * Running the passes the other way round left the closure unmerged and the
     * strict pin unseen.
     */
    @Test
    public void aDefReferenceWithAMultilineClosureIsStillAPin() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def stdlib = 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'\n"
                + "    implementation(stdlib) {\n"
                + "        version { strictly '1.7.22' }\n"
                + "    }\n");
        check("".equals(out),
                "the strict base pin behind a def with a multiline closure is honoured");
    }

    /**
     * Gradle's {@code !!} suffix is the strict-version shorthand, and missing
     * it produced the worst outcome available here. Measured: an app writing
     * kotlin-stdlib:1.7.22!! beside a pre-merge jdk8 resolves the coherent
     * 1.7.22 family on its own; with these constraints added it resolves
     * kotlin-stdlib 1.7.22 beside jdk7/jdk8 1.8.0, the EMPTY shims -- so the
     * jdk extension classes come from neither jar and the app fails at runtime
     * with a missing class instead of at build time with a duplicate one.
     */
    @Test
    public void theStrictShorthandCountsAsAStrictPin() {
        String base = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22!!'\n");
        check("".equals(base),
                "a !! pin on the base stdlib suppresses both shims");

        String shim = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!'\n");
        check("".equals(shim), "and a !! pin on a shim does too");

        // Above the floor the shorthand changes nothing: the constraints are still
        // satisfiable, so they are still written.
        String modern = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.9.22!!'\n");
        check(modern.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a merged-era !! pin does not need the block suppressed");
    }

    /**
     * Map notation quoted inside a reason is prose too. The coordinate matcher
     * had been taught to skip string literals and the map matcher beside it
     * had not, so a reason naming the artifact in map form read as a
     * declaration -- and since prose carries no version, the whole block was
     * suppressed rather than one artifact.
     */
    @Test
    public void mapNotationInsideAReasonIsStillProse() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('com.example:other:1.0') { because "
                + "\"avoid group: 'org.jetbrains.kotlin', name: 'kotlin-stdlib-jdk8'\" }\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "quoted map notation does not suppress");
        check(out.contains("kotlin-stdlib-jdk7:1.8.0"),
                "and does not take the whole block with it");

        // the real map form still counts
        String real = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation group: 'org.jetbrains.kotlin', "
                + "name: 'kotlin-stdlib-jdk8', version: '1.9.22'\n");
        check(!real.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a real map declaration still pins jdk8");
    }

    /**
     * A reason that OPENS with the coordinate is still a reason. Accepting any
     * literal starting with one let a warning about the duplicate
     * -- because 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22 causes
     * duplicate classes' -- switch off the constraint that prevents exactly
     * what it describes. Dependency notation carries no whitespace.
     */
    @Test
    public void aReasonOpeningWithTheCoordinateIsStillProse() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('com.example:foo:1.0') { because "
                + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22 causes duplicate classes' }\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a reason opening with the coordinate does not suppress");
    }

    /**
     * A coordinate may sit one hop away behind a def. Neither statement
     * carries both the configuration and the coordinate, so the strict pin was
     * invisible and the constraint made the build stop resolving.
     */
    @Test
    public void aCoordinateBehindADefIsStillADeclaration() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def jdk8 = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'\n"
                + "    implementation(jdk8) { version { strictly '1.7.22' } }\n");
        check("".equals(out),
                "the strict pin behind a def is honoured, and below the floor both go");
    }

    /**
     * The boundary, stated as a case so it is a decision rather than an
     * oversight. An interpolated VERSION is still recognised -- the artifact
     * name is literal there, and naming the artifact is what matters -- but a
     * coordinate assembled by concatenation is not in the text as a coordinate
     * at all, and recovering it needs Gradle to evaluate the script. The block
     * is written, which is the safe direction for everything except a strict
     * pin; a strict pin hidden this way is beyond what reading build-hint text
     * can reach, and the design that does not need to find the declaration is
     * the answer to that class rather than another pass here.
     */
    @Test
    public void aConcatenatedCoordinateIsNotRecovered() {
        // An interpolated version still names the artifact, so it IS recognised.
        String interpolatedVersion = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation(\"org.jetbrains.kotlin:kotlin-stdlib-jdk8:$v\")\n");
        check(!interpolatedVersion.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an interpolated version still names the artifact");

        // A coordinate assembled by concatenation is not a coordinate in the text.
        String concatenated = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:' + 'kotlin-stdlib-jdk8:1.7.22') "
                + "{ version { strictly '1.7.22' } }\n");
        check(concatenated.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a concatenated coordinate is left unrecognised, by design");
    }

    /**
     * A def that is not a string literal defines nothing here, and must not
     * corrupt the statement that uses the name.
     */
    @Test
    public void aNonLiteralDefIsIgnored() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def jdk8 = someFunction()\n"
                + "    implementation(jdk8)\n"
                + "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.22'\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an unresolvable def leaves jdk8 constrained");
        check(!out.contains("kotlin-stdlib-jdk7:1.8.0"),
                "and the real jdk7 declaration beside it still counts");
    }

    /**
     * Groovy's parenthesis-free map notation spreads one declaration over
     * several lines, held together by trailing commas. Splitting at those
     * newlines left the configuration, the group, the artifact and the closure
     * in four statements, none of which is a declaration on its own -- so a
     * strict pre-1.8 pin written that way was missed and the constraint made
     * resolution fail.
     */
    @Test
    public void aCommaContinuesAMultilineMapDeclaration() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation group: 'org.jetbrains.kotlin',\n"
                + "        name: 'kotlin-stdlib-jdk8',\n"
                + "        version: '1.7.22'\n");
        check("".equals(out),
                "a comma-continued map declaration pins jdk8, below the floor so both go");
    }

    /**
     * An enclosing block's opening brace is not the declaration's own closure.
     * A fragment putting its first dependency on the same line as
     * {@code dependencies &#123;} made that declaration swallow every following
     * statement up to the closing brace, so an unrelated strict pin further
     * down read as one on the stdlib and silenced the whole block.
     */
    @Test
    public void anEnclosingBlockBraceIsNotATrailingClosure() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "dependencies { implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.9.22'\n"
                + "    implementation('com.example:other:1.0') "
                + "{ version { strictly '1.7.22' } }\n"
                + "}\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the unrelated strict pin does not attach to the stdlib declaration");
    }

    /**
     * android.gradlePlugin is interpolated at top level right after
     * `apply plugin`, where a dependencies block of its own is valid and
     * reaches the same configurations -- so it has to be scanned like the
     * other app-controlled fragments. It was missed the same way
     * android.supportv4Dep was.
     */
    @Test
    public void theBuilderScansTheGradlePluginFragment() throws Exception {
        byte[] bytes = java.nio.file.Files.readAllBytes(new java.io.File(
                "src/main/java/com/codename1/builders/AndroidGradleBuilder.java").toPath());
        String builderSrc = new String(bytes, "UTF-8");
        int at = builderSrc.indexOf("KotlinStdlibAlignment.constraintsBlock(");
        check(at >= 0, "the builder calls the alignment");
        String call = builderSrc.substring(at, builderSrc.indexOf(";", at));
        check(call.contains("request.getArg(\"android.gradlePlugin\", \"\")"),
                "android.gradlePlugin reaches the generated script and must be scanned");
    }

    /**
     * The version comes from the strict call, not from a reason that mentions
     * one. Finding the call correctly and then reading the version with a
     * plain search let prose supply it, so a declaration whose real strict
     * version is compatible was judged on a number from its own comment.
     */
    @Test
    public void theStrictVersionComesFromTheCallNotTheProse() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib:1.9.22') "
                + "{ because \"strictly '1.7.22' is not intended\"; "
                + "version { strictly '1.9.22' } }\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the real strict version is above the floor, so the block is written");
    }

    /**
     * Brace balancing honours escapes, as the statement scanner does. A
     * declaration whose closure contained an escaped apostrophe had its real
     * closing brace ignored, so following dependencies merged into it and an
     * unrelated strict pin could be read as one on kotlin-stdlib.
     */
    @Test
    public void anEscapedQuoteDoesNotSwallowAClosingBrace() {
        // The base stdlib named without a strict version, then an UNRELATED strict
        // pin. Correct: neither suppresses, so the block is written. With the escape
        // mishandled the two statements merge, the merged statement both names
        // kotlin-stdlib and calls strictly '1.7.22', and the whole block disappears.
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib:1.9.22') "
                + "{ because 'can\\'t' }\n"
                + "    implementation('com.example:other:1.0') "
                + "{ version { strictly '1.7.22' } }\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an unrelated strict pin does not merge into the Kotlin declaration");
        check(out.contains("kotlin-stdlib-jdk7:1.8.0"),
                "and the block is written in full");
    }

    /**
     * An underscore is an identifier character. A configuration named
     * custom_implementation ended its embedded "implementation" on a boundary
     * that looked clean, so it read as the main configuration and suppressed a
     * constraint for a configuration that reaches nothing.
     */
    @Test
    public void aCustomConfigurationIsNotTheMainOne() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    custom_implementation "
                + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "custom_implementation is not the configuration being constrained");

        String suffixed = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation_extra "
                + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'\n");
        check(suffixed.contains("kotlin-stdlib-jdk8:1.8.0"),
                "nor is implementation_extra");

        // and the real one still is
        String real = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'\n");
        check(!real.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the main configuration still counts");
    }

    /**
     * A trailing closure may sit on the line after the call's closing
     * parenthesis. Gradle accepts it and the {@code strictly} inside really
     * does apply -- checked by watching a competing higher requirement fail
     * against it -- but the parenthesis depth is back to zero there, so the
     * closure landed in its own statement and its version was never
     * associated with the coordinate above it.
     */
    @Test
    public void aTrailingClosureOnTheNextLineBelongsToTheDeclaration() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation(\n"
                + "        'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'\n"
                + "    )\n"
                + "    { version { strictly '1.7.22' } }\n");
        check("".equals(out),
                "the strict pin in a next-line closure still suppresses the block");
    }

    /**
     * A coordinate inside a reason is prose. A coordinate lives in a string,
     * so "outside a string" cannot be the test here the way it is for
     * strictly -- what separates them is that a declaration's string OPENS
     * with the coordinate while prose merely contains it.
     */
    @Test
    public void aCoordinateInsideAReasonIsNotADeclaration() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('com.example:other:1.0') "
                + "{ because 'avoid org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22' }\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a coordinate mentioned in a reason does not count as a pin");

        // the real notation still does
        String real = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n");
        check(!real.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the dependency notation itself still counts");
    }

    /**
     * A qualified segment keeps its number. Reading {@code 20-RC} as zero made
     * 1.8.20-RC compare equal to the floor, and the qualifier rule then
     * classified a version well ABOVE the floor as below it -- suppressing an
     * alignment that was needed.
     */
    @Test
    public void aQualifiedSegmentKeepsItsNumber() {
        String above = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib:1.8.20-RC') "
                + "{ version { strictly '1.8.20-RC' } }\n");
        check(above.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a prerelease above the floor still gets the constraints");

        // and the prerelease OF the floor is still below it
        String atTheFloor = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib:1.8.0-RC2') "
                + "{ version { strictly '1.8.0-RC2' } }\n");
        check("".equals(atTheFloor),
                "a prerelease of the floor is still below it");
    }

    /**
     * A prerelease of the floor is below the floor. 1.8.0-RC2 is a published
     * Kotlin version whose numeric part compares equal to 1.8.0, so it read as
     * "at the floor" and the block was written -- whereupon the shims request
     * the FINAL 1.8.0 and cannot coexist with the strict prerelease.
     */
    @Test
    public void aPrereleaseOfTheFloorCountsAsBelowIt() {
        String rc = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib:1.8.0-RC2') "
                + "{ version { strictly '1.8.0-RC2' } }\n");
        check("".equals(rc), "a strict prerelease of the floor suppresses the block");

        // A qualifier above the floor changes nothing: rounding up keeps it above.
        String later = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib:1.9.22-RC') "
                + "{ version { strictly '1.9.22-RC' } }\n");
        check(later.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a prerelease above the floor still gets the constraints");
    }

    /**
     * A configuration name inside a reason string is prose. Accepting any
     * quoted occurrence -- which the dependencies.add spelling needed -- read
     * `because 'implementation workaround'` as a main-variant declaration and
     * suppressed the constraint for a dependency affecting only debug.
     */
    @Test
    public void aConfigurationNameInAReasonStringIsNotADeclaration() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    debugImplementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ because 'implementation workaround' }\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a reason mentioning the configuration does not make it a declaration");

        // and the add() spelling it was widened for still works
        String add = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    dependencies.add(\"runtimeOnly\", "
                + "\"org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22\")\n");
        check(!add.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the add() spelling is still recognised");
    }

    /**
     * kotlin-stdlib is a prefix of kotlin-stdlib-jdk8, so the base match has to
     * be exact. A loose one would read every shim declaration as a pin on the
     * base library and switch the whole block off.
     */
    @Test
    public void aStrictShimPinIsNotAPinOnTheBaseStdlib() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ version { strictly '1.7.22' } }\n");
        check("".equals(out),
                "a strict pre-merge shim pin suppresses both, not just its own");
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
     * The quoted spelling counts as well. Gradle's
     * {@code dependencies.add("runtimeOnly", "group:artifact:version")} names
     * the configuration as a string, so the token ends at a quote rather than
     * a space or a parenthesis, and the escape hatch has to recognise it for
     * the same reason it recognises the others.
     */
    @Test
    public void theQuotedAddSpellingCountsAsAPin() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    dependencies.add(\"runtimeOnly\", "
                + "\"org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22\")\n");
        check("".equals(out),
                "a quoted configuration name still pins jdk8, and a below-floor pin "
                + "suppresses both");
    }

    /**
     * A quoted configuration name on its own decides nothing, because it takes
     * the artifact coordinate on the same statement to make a declaration.
     */
    @Test
    public void aQuotedConfigurationNameAloneIsNotAPin() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def cfg = \"runtimeOnly\"\n"
                + "    implementation 'com.example:thing:1.0'\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "naming a configuration in a string does not suppress anything");
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
        check("".equals(out),
                "a wrapped declaration pins jdk8, below the floor so both go");
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
     * A comment delimiter inside a string is not a delimiter. A {@code /*} in
     * a quoted value used to open a block comment that swallowed the rest of
     * the fragment, taking an explicit strict pin with it -- and losing a
     * strict marker is what turns this class's constraint into a failed
     * resolution rather than an override.
     */
    @Test
    public void aCommentDelimiterInsideAStringIsNotADelimiter() {
        String blockOpener = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def marker = '/*'\n"
                + "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ version { strictly '1.7.22' } }\n");
        check(!blockOpener.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a /* inside a string does not swallow the pin that follows it");

        String lineOpener = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def marker = \"//\"\n"
                + "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n");
        check(!lineOpener.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a // inside a string does not comment out the line");
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
     * A Kotlin BOM no longer excuses the block either, at any version.
     *
     * <p>Measured against a graph carrying billing 9.1.0 and appcompat 1.6.1:
     * adding this block alongside kotlin-bom 1.9.22 gives byte-identical
     * resolution, because a BOM's constraints are not strict and the higher
     * version wins; alongside kotlin-bom 1.7.22 it is not merely harmless but
     * necessary, since a pre-merge BOM raises the jdk artifacts and cannot
     * pull kotlin-stdlib back down. Suppressing on a BOM was cosmetic where it
     * fired and wrong where it did not.</p>
     */
    @Test
    public void aKotlinBomNoLongerExcusesTheBlock() {
        String modern = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation platform('org.jetbrains.kotlin:kotlin-bom:1.9.22')\n");
        check(modern.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a modern BOM does not suppress, and does not need to");

        String preMerge = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation platform('org.jetbrains.kotlin:kotlin-bom:1.7.22')\n");
        check(preMerge.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a pre-merge BOM still gets the alignment it needs");
    }

    /**
     * And the case that removed the feature rather than patching it: a BOM
     * declared inside a condition cannot be known to be in force by reading
     * the text, so no reading of it decides anything any more.
     */
    @Test
    public void aConditionalBomDoesNotDecideAnything() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    if (project.hasProperty('useKotlinBom')) {\n"
                + "        implementation platform('org.jetbrains.kotlin:kotlin-bom:1.9.22')\n"
                + "    }\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a conditional BOM leaves the alignment in place");
    }

    /**
     * An escaped quote does not end a string. The statement scanner missed
     * this while the comment stripper beside it handled it, so a string
     * escaping its own apostrophe closed early and every following newline
     * read as being inside a string -- merging statements that must stay
     * apart, which lets a main-variant configuration token pair with a
     * debug-only coordinate.
     */
    @Test
    public void anEscapedQuoteDoesNotEndAString() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def marker = 'can\\'t'\n"
                + "    implementation 'com.android.billingclient:billing:9.1.0'\n"
                + "    debugImplementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the debug-only pin does not borrow the main statement's configuration");
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
