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
            // The dependency fragments now reach the call concatenated into ONE
            // wrapper, because the generated script has one dependencies { } and
            // a closure each made a scope boundary Gradle does not have. Matched
            // on the concatenation operator so that deleting an argument fails
            // this, which matching the bare hint name did not -- the comment
            // above the list names some of them too.
            "+ additionalDependencies",
            "+ aiExtraGradleDependencies.toString()",
            "+ request.getArg(\"android.gradleDep\", \"\")",
            "+ request.getArg(\"android.supportv4Dep\", \"\")",
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
                    + "('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!')\n");
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
                + "name : 'kotlin-stdlib-jdk8', version : '1.7.22!!')\n");
        check("".equals(spaced),
                "a spaced map entry still pins jdk8, below the floor so both go");

        String doubleQuoted = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation(group: \"org.jetbrains.kotlin\", "
                + "name:\"kotlin-stdlib-jdk8\", version: \"1.7.22!!\")\n");
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
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!'\n");
        check("".equals(jdk8Pinned),
                "a pre-merge jdk8 pin takes the jdk7 constraint with it");

        String jdk7Pinned = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.22!!'\n");
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
     * Every fragment the generated dependencies block is built from is handed
     * to the alignment, and in the same order.
     *
     * <p>Read off the builder's own concatenation rather than listed here,
     * because a list here is a second copy of the truth and it was already
     * wrong: kotlinRuntimeDependency carries requireKotlinStdlib, so an app
     * asking for {@code 1.7.22!!} had a strict pre-merge pin on the base
     * library that nothing in the scan could see. Two more fragments were
     * missing beside it. A test that enumerates cannot go stale the way the
     * list did.</p>
     */
    @Test
    public void everyFragmentOfTheGeneratedBlockIsScanned() throws Exception {
        byte[] bytes = java.nio.file.Files.readAllBytes(new java.io.File(
                "src/main/java/com/codename1/builders/AndroidGradleBuilder.java").toPath());
        String builderSrc = new String(bytes, "UTF-8");
        int at = builderSrc.indexOf("KotlinStdlibAlignment.constraintsBlock(");
        check(at >= 0, "the builder calls the alignment");
        // Comments go first, THEN the terminator is found: a semicolon inside the
        // call's own explanatory comment truncated this slice and the test then
        // reported arguments missing that were plainly there.
        String fromCall = builderSrc.substring(at).replaceAll("//[^\n]*", "");
        String call = fromCall.substring(0, fromCall.indexOf(";"));

        // The WHOLE generated script, not just its dependencies block. The block was
        // the wrong boundary: android.gradle.androidx is interpolated inside
        // android { }, where a project.configurations.all { ... force } is accepted
        // and executed, so a fragment there decides what resolves just as much as a
        // declaration does. Bounding this test at the block is what let that through.
        int blockAt = builderSrc.indexOf("String gradleProps = ");
        check(blockAt >= 0, "the generated script is found");
        int blockEnd = builderSrc.indexOf("Gradle File start", blockAt);
        check(blockEnd > blockAt, "and its end");
        String block = builderSrc.substring(blockAt, blockEnd).replaceAll("//[^\n]*", "");

        // What the block is concatenated FROM: java expressions, not literals.
        // Keyed by where they appear, because the two spellings have to come back
        // interleaved: collecting all the hints and then all the locals produced a
        // list in neither the script's order nor the call's, and the order half of
        // this test then failed on a call that was right.
        java.util.TreeMap<Integer, String> byPosition = new java.util.TreeMap<Integer, String>();
        java.util.regex.Matcher hint = java.util.regex.Pattern
                .compile("getArg\\(\"([a-zA-Z0-9._]+)\"").matcher(block);
        while (hint.find()) {
            byPosition.put(Integer.valueOf(hint.start()), "getArg(\"" + hint.group(1) + "\"");
        }
        // Every fragment that carries app-supplied text, by either route: a getArg
        // read straight into the script, or a local that was ASSIGNED from one.
        // Requiring only the direct reads missed injectRepo, which holds
        // android.repositories and is interpolated into the repositories closure --
        // where a project.configurations.all { force } runs perfectly well. The
        // locals are found by looking at how they are built, not by knowing their
        // names, because knowing their names is what keeps being wrong.
        java.util.Set<String> carriesAppText = new java.util.HashSet<String>();
        java.util.regex.Matcher assigned = java.util.regex.Pattern
                .compile("\\b([a-z][a-zA-Z0-9]*)\\s*(?:=|\\+=)[^;\n]*getArg\\(")
                .matcher(builderSrc);
        while (assigned.find()) {
            carriesAppText.add(assigned.group(1));
        }
        check(carriesAppText.contains("injectRepo"),
                "the scan for hint-carrying locals works: " + carriesAppText);
        java.util.regex.Matcher carrier = java.util.regex.Pattern
                .compile("\\+\\s*(?:addNewlineIfMissing\\()?([a-z][a-zA-Z0-9]*)\\b")
                .matcher(block);
        while (carrier.find()) {
            if (carriesAppText.contains(carrier.group(1))) {
                byPosition.put(Integer.valueOf(carrier.start(1)), carrier.group(1));
            }
        }
        int dependenciesAt = block.indexOf("\"dependencies {");
        check(dependenciesAt >= 0, "the dependencies block is inside the script");
        java.util.regex.Matcher name = java.util.regex.Pattern
                .compile("\\+\\s*(?:addNewlineIfMissing\\()?([a-z][a-zA-Z0-9]*)\\b")
                .matcher(block);
        while (name.find()) {
            String token = name.group(1);
            if (name.start(1) < dependenciesAt) {
                continue;
            }
            // The configuration itself is passed as the first argument, and the
            // block this test is about is the alignment's own output.
            if ("compile".equals(token) || "kotlinStdlibConstraints".equals(token)
                    || "addNewlineIfMissing".equals(token)) {
                continue;
            }
            // `request` in request.getArg("x") is the receiver, not a fragment --
            // that one is already counted under its hint name. Matched on the call
            // rather than the name, so a local that merely has methods on it
            // (aiExtraGradleDependencies.toString()) still counts.
            if (block.startsWith(".getArg(", name.end(1))) {
                continue;
            }
            byPosition.put(Integer.valueOf(name.start(1)), token);
        }
        // EVERY occurrence, not one per name. A fragment interpolated twice is
        // executed twice -- injectRepo goes into the buildscript repositories and
        // again into the project ones after the android block -- and Gradle runs
        // both, so a name it binds is restored at the second. Collapsing them let
        // the scan keep whatever an intervening fragment had reassigned, and read
        // a later use of that name as something it is not. The dedup was put here
        // to make the ordering check pass; the search below starts after the last
        // match instead, which is what a repeated argument actually needs.
        java.util.List<String> fragments =
                new java.util.ArrayList<String>(byPosition.values());
        check(fragments.size() >= 6,
                "the block really was parsed, found " + fragments);
        check(java.util.Collections.frequency(fragments, "injectRepo") == 2,
                "the script interpolates injectRepo twice, found " + fragments);

        int previous = -1;
        for (int i = 0; i < fragments.size(); i++) {
            String fragment = fragments.get(i);
            int passed = call.indexOf(fragment, previous + 1);
            check(passed >= 0, "the alignment is given " + fragment
                    + " at occurrence " + i + ", which the generated block contains "
                    + "but the call does not");
            previous = passed;
        }
    }

    /**
     * The alignment can never fail a build. It reads developer-authored Groovy
     * with a hand-written scanner, on every AndroidX build there is, to decide
     * something that is an optimisation over a build which already worked --
     * so an index defect in it must cost that one app its constraint, not
     * every app its build. The guard is asserted here rather than trusted,
     * because nothing else in the suite would notice it being refactored away.
     */
    @Test
    public void theAlignmentCannotFailTheBuild() throws Exception {
        byte[] bytes = java.nio.file.Files.readAllBytes(new java.io.File(
                "src/main/java/com/codename1/builders/AndroidGradleBuilder.java").toPath());
        String builderSrc = new String(bytes, "UTF-8");
        int at = builderSrc.indexOf("KotlinStdlibAlignment.constraintsBlock(");
        check(at >= 0, "the builder calls the alignment");
        String before = builderSrc.substring(0, at);
        check(before.lastIndexOf("try {") > before.lastIndexOf("catch ("),
                "the call is inside a try block");
        String after = builderSrc.substring(at);
        int handler = after.indexOf("catch (RuntimeException");
        check(handler >= 0, "and a RuntimeException handler follows it");
        // Past the handler's own reasoning, which is longer than the code.
        String body = after.substring(handler,
                Math.min(handler + 2000, after.length()));
        check(body.indexOf("kotlinStdlibConstraints = \"\"") >= 0,
                "which falls back to emitting nothing");
        check(body.indexOf("log(") >= 0,
                "and says so, rather than swallowing the defect");
    }

    /**
     * A map key may be quoted. Skipping every literal meant the key was never
     * seen, so a declaration written that way named no artifact at all and the
     * strict pin inside it went with it.
     */
    @Test
    public void aMapKeyMayBeQuoted() {
        String[] quotes = {"'", "\"", "'''", "\"\"\""};
        for (int q = 0; q < quotes.length; q++) {
            String u = quotes[q];
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    implementation(" + u + "group" + u + ": 'org.jetbrains.kotlin', "
                    + u + "name" + u + ": 'kotlin-stdlib-jdk8', "
                    + u + "version" + u + ": '1.7.22!!')\n");
            check("".equals(out),
                    "a key quoted with " + u + " is still a key, got <<" + out + ">>");
        }

        // Mixed spellings in one declaration, which Groovy also accepts.
        String mixed = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('group': 'org.jetbrains.kotlin', "
                + "name: 'kotlin-stdlib-jdk8', \"version\": '1.7.22!!')\n");
        check("".equals(mixed), "mixed key spellings, got <<" + mixed + ">>");

        // And a merged-era one written the same way keeps the sibling aligned.
        String modern = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('group': 'org.jetbrains.kotlin', "
                + "'name': 'kotlin-stdlib-jdk7', 'version': '1.9.22')\n");
        check(modern.contains("kotlin-stdlib-jdk8:1.8.0")
                        && !modern.contains("kotlin-stdlib-jdk7:1.8.0"),
                "the merged-era declaration is read, got <<" + modern + ">>");
    }

    /**
     * strictly, require and useVersion SET the constraint rather than adding to
     * it, so a closure that calls one twice keeps the last value. Reading the
     * first wrote the shim constraints beside a pin that was really pre-merge.
     */
    @Test
    public void theLastCallOfARepeatedSetterIsTheOneThatCounts() {
        String[] spellings = {
            "        version { strictly '1.9.22'; strictly '1.7.22' }\n",
            "        version {\n            strictly '1.9.22'\n"
                    + "            strictly '1.7.22'\n        }\n",
            "        version { require '1.9.22'; require '1.7.22!!' }\n",
        };
        for (int i = 0; i < spellings.length; i++) {
            String declaration = "    implementation("
                    + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8') {\n"
                    + spellings[i] + "    }\n";
            check("".equals(KotlinStdlibAlignment.constraintsBlock(
                            "implementation", declaration)),
                    "the last value stands the block down, in <<" + spellings[i] + ">>");
        }

        // And the other way round, so this is the last value rather than the
        // lowest: set back UP to a merged-era version the sibling is still raised.
        String raised = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8') {\n"
                + "        version { strictly '1.7.22'; strictly '1.9.22' }\n    }\n");
        check(raised.contains("kotlin-stdlib-jdk7:1.8.0")
                        && !raised.contains("kotlin-stdlib-jdk8:1.8.0"),
                "and the last value is read even raising, got <<" + raised + ">>");
    }

    /**
     * Rejections accumulate -- reject takes varargs and may be called again -- and
     * Gradle applies every selector, so every one is asked whether it removes the
     * floor. Reading only the first missed a pair whose second selector was the
     * one that removed it.
     */
    @Test
    public void rejectionsAreCombinedBeforeTheFloorIsCalledReachable() {
        // What this block writes is a constraint on exactly 1.8.0, so a rejection
        // that removes the floor removes the only version it can resolve to --
        // whether or not it leaves higher ones. Written second, the selector that
        // removes it was not being read at all.
        String[] removeTheFloor = {
            "reject '1.8.0'",
            "reject '[1.8.0]'",
            "reject '(1.8.0,)', '1.8.0'",
            "reject('1.8.0', '(1.8.0,)')",
            "reject '[1.8.0,)'",
            "reject '(1.7.0,)'",
            "reject '[1.7.0,1.9.0]'",
            "reject '[1.7.0,1.8.0]'",
            "reject '[1.7.0,1.8.5]', '[1.8.6,1.9.0]'",
            "reject '1.8.0'\n            reject '(1.8.0,)'",
        };
        for (int i = 0; i < removeTheFloor.length; i++) {
            check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                            rejecting(removeTheFloor[i]))),
                    "<<" + removeTheFloor[i] + ">> takes the floor away");
        }

        // And these leave it exactly where the constraint needs it. Reading any of
        // them as management would leave the duplicate this block exists to
        // prevent -- an exclusive bound does not reject the bound itself.
        String[] leaveTheFloor = {
            "reject '(1.8.0,)'",
            "reject '[1.9.0,)'",
            "reject '[1.7.0,1.8.0)'",
            "reject '(1.8.0,1.9.0]'",
            "reject '[1.7.0,1.7.9]', '[1.8.1,1.9.0]'",
            "reject '1.7.0'",
            // A prerelease of the floor is a different version from the floor.
            "reject '1.8.0-RC2', '(1.8.0,)'",
        };
        for (int i = 0; i < leaveTheFloor.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation",
                            rejecting(leaveTheFloor[i]))
                            .contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + leaveTheFloor[i] + ">> still leaves the floor selectable");
        }
    }

    /** A jdk8 declaration whose rich version requires anything and rejects this. */
    private static String rejecting(String rejections) {
        return "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8') {\n"
                + "        version { require '1.+'; " + rejections + " }\n    }\n";
    }

    /**
     * The two shapes a value takes that the bare-assignment path did not read.
     * An {@code ext { dep = [..] }} block is the project-wide spelling of a map
     * definition, and a name on the right copies an earlier binding; reading
     * only literals left both unknown, so the declaration using one named no
     * artifact and the pin it carried went unread.
     */
    @Test
    public void anExtraPropertiesClosureReadsEveryKindOfValue() {
        String pin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!";
        String use = "    implementation(dep)\n";
        String[] bound = {
            "    ext {\n        dep = [group: 'org.jetbrains.kotlin', "
                    + "name: 'kotlin-stdlib-jdk8', version: '1.7.22!!']\n    }\n",
            "    def coord = '" + pin + "'\n    ext {\n        dep = coord\n    }\n",
            "    ext {\n        dep = '" + pin + "'\n    }\n",
        };
        for (int i = 0; i < bound.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    bound[i] + use);
            check("".equals(out), "<<" + bound[i].trim() + ">> binds dep, got <<"
                    + out + ">>");
        }

        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    ext {\n        dep = [group: 'org.jetbrains.kotlin', "
                        + "name: 'kotlin-stdlib-jdk8', version: '1.9.22']\n    }\n" + use)
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "a merged-era map through the same route is read too");
    }

    /**
     * Groovy's multiple assignment binds several names at once. The walk for a
     * single declaration expects an identifier after {@code def} and finds a
     * parenthesis, so it recorded nothing and the pin one of the names carried
     * was invisible.
     */
    @Test
    public void aMultipleAssignmentBindsEveryName() {
        // The coordinate in the list is SOFT and the pin is at the use site, so
        // the binding is the only thing that can connect them. Putting the pin
        // in the list instead makes the list itself suppress, and the test then
        // passes whether the names are bound or not -- which is how the first
        // version of this went vacuous.
        String coord = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22";
        String use = "    implementation(dep) { version { strictly '1.7.22' } }\n";
        String[] destructured = {
            "    def (other, dep) = ['com.example:x:1.0', '" + coord + "']\n",
            "    def (dep, other) = ['" + coord + "', 'com.example:x:1.0']\n",
            // Each name may carry a type, as a single declaration may.
            "    def (String other, String dep) = ['com.example:x:1.0', '"
                    + coord + "']\n",
        };
        for (int i = 0; i < destructured.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    destructured[i] + use);
            check("".equals(out), "<<" + destructured[i].trim()
                    + ">> binds dep, got <<" + out + ">>");
        }

        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    def (other, dep) = ['com.example:x:1.0', "
                        + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22']\n"
                        + "    implementation(dep)\n")
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "a merged-era element is read too");
        // A list this cannot read binds every name to something unknown, which
        // is what recording nothing already means.
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    def (other, dep) = someCall()\n"
                        + "    implementation(dep)\n")
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "an unreadable list binds nothing");
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    def dep = '" + coord + "'\n" + use)),
                "and a plain def still works");
    }

    /**
     * Two identifiers in a row are as often a parenthesis-free call as a typed
     * local. Read as a declaration, {@code println dep} cleared the very binding
     * it was printing, so the pin that name carried was gone by the time
     * anything used it.
     */
    @Test
    public void aCommandCallDoesNotClearItsArgument() {
        String coord = "org.jetbrains.kotlin:kotlin-stdlib:1.7.22";
        String use = "    implementation(dep) { version { strictly '1.7.22' } }\n";
        String[] kept = {
            "    def dep = '" + coord + "'\n    println dep\n",
            "    def dep = '" + coord + "'\n    logger dep\n",
            "    def dep = '" + coord + "'\n",
            // A real typed declaration WITH a value still binds.
            "    String dep = '" + coord + "'\n",
        };
        for (int i = 0; i < kept.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    kept[i] + use);
            check("".equals(out), "<<" + kept[i].trim() + ">> keeps dep bound, got <<"
                    + out + ">>");
        }

        // And `def` with no value is still a name this knows about, which is what
        // lets the assignment below it be recognised as one.
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    def dep\n    if (legacy) { dep = '" + coord + "' }\n"
                        + use)),
                "a valueless def still introduces the name");
    }

    /**
     * A conditional swap between two coordinates of this family is a choice
     * between two of ours, and which arm runs is not readable here. Taking the
     * replacement let {@code def dep = '..jdk8:1.7.22'} followed by
     * {@code if (useNew) dep = '..jdk8:1.9.22'} read as merged-era, so the
     * declaration below it needed no constraint -- and with the condition false
     * the class-bearing 1.7.22 jar is still there.
     */
    @Test
    public void aConditionalSwapKeepsTheLowerCoordinate() {
        String jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8";
        String use = "    implementation(dep)\n";
        String[] swaps = {
            "    def dep = '" + jdk8 + ":1.7.22'\n"
                    + "    if (useNew) dep = '" + jdk8 + ":1.9.22'\n",
            "    def dep = '" + jdk8 + ":1.7.22'\n"
                    + "    if (useNew) {\n        dep = '" + jdk8 + ":1.9.22'\n    }\n",
            // The same choice written the other way round. Its assignment sits
            // after a header with no brace, which was not read as an assignment
            // at all -- so the name kept the merged-era value it started with.
            "    def dep = '" + jdk8 + ":1.9.22'\n"
                    + "    if (legacy) dep = '" + jdk8 + ":1.7.22'\n",
        };
        for (int i = 0; i < swaps.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    swaps[i] + use);
            check(out.contains("kotlin-stdlib-jdk7:1.8.0")
                            && out.contains("kotlin-stdlib-jdk8:1.8.0"),
                    "the pre-merge arm may be the live one, so the shim is raised; "
                            + "got <<" + out + ">>");
        }

        // Two merged-era coordinates leave the artifact to the app, and an
        // UNCONDITIONAL raise still replaces what it replaces.
        String[] settled = {
            "    def dep = '" + jdk8 + ":1.9.22'\n"
                    + "    if (useNew) dep = '" + jdk8 + ":1.9.24'\n",
            "    def dep = '" + jdk8 + ":1.7.22'\n    dep = '" + jdk8 + ":1.9.22'\n",
        };
        for (int i = 0; i < settled.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    settled[i] + use);
            check(!out.contains("kotlin-stdlib-jdk8:1.8.0")
                            && out.contains("kotlin-stdlib-jdk7:1.8.0"),
                    "a merged-era binding stands in for its own constraint, got <<"
                            + out + ">>");
        }
    }

    /**
     * Whether a keyword was CALLED settles which one speaks, and what it was
     * called with is a separate question. Falling through on a null let
     * {@code require '1.9.22'; strictly providers.gradleProperty('k').get()}
     * report the requirement, so a shim whose strict version may be pre-merge
     * read as merged-era and only its sibling was raised.
     */
    @Test
    public void aCalledKeywordSettlesItReadableOrNot() {
        String jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8";
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation('" + jdk8 + "') { version { "
                        + "require '1.9.22'; strictly providers"
                        + ".gradleProperty('legacy').get() } }\n")),
                "an unreadable strictly is not the requirement beside it");
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    configurations.all { resolutionStrategy.eachDependency "
                        + "{ d ->\n        if (d.requested.name == "
                        + "'kotlin-stdlib-jdk8') { d.useVersion someProperty }\n"
                        + "    } }\n")),
                "and neither is an unreadable useVersion");

        // A readable one still overrides the requirement beside it.
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation('" + jdk8 + "') { version { "
                        + "require '1.7.22'; strictly '1.9.22' } }\n")
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "a readable strictly speaks for the declaration");
    }

    /**
     * Groovy accepts parentheses around a stored value, and one that did not
     * START with a literal was recorded as unknown -- so the pin it held was
     * invisible to whatever used the name.
     */
    @Test
    public void aStoredValueMayBeWrapped() {
        String jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8";
        String use = "    implementation(dep)\n";
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    def dep = ('" + jdk8 + ":1.7.22!!')\n" + use)),
                "one pair of parentheses");
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    def dep = (( '" + jdk8 + ":1.7.22!!' ))\n" + use)),
                "and two, with spaces");
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    def dep = ('" + jdk8 + ":1.9.22')\n" + use)
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "a wrapped merged-era value is read too");
    }

    /**
     * The two android fragments run inside ONE {@code android { }} closure in
     * the generated script -- androidx directly in it, the default config in its
     * defaultConfig block. A synthetic closure each made a scope boundary Gradle
     * does not have, so a name the first defined was gone before the second used
     * it.
     */
    @Test
    public void theAndroidFragmentsShareOneClosure() throws Exception {
        String shared = KotlinStdlibAlignment.constraintsBlock("implementation",
                "android {\n"
                + "def dep = 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22!!'\n"
                + "\ndefaultConfig {\n"
                + "project.dependencies.add('implementation', dep)\n"
                + "\n}\n}\n");
        check("".equals(shared),
                "the name survives into the default config, got <<" + shared + ">>");

        // Handed over as a closure each, it does not -- which is what the builder
        // was doing and what the source check below now forbids.
        String split = KotlinStdlibAlignment.constraintsBlock("implementation",
                "android {\ndef dep = 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22!!'\n}\n",
                "android {\ndefaultConfig {\nproject.dependencies.add("
                        + "'implementation', dep)\n}\n}\n");
        check(!"".equals(split), "a closure each loses it, which is the bug");

        // The half above proves the alignment honours the scope it is GIVEN.
        // This half proves the builder gives it one: with a synthetic closure
        // each the arguments are still in the right ORDER, so the enumeration
        // test passes either way and only this catches it.
        String builderSrc = new String(java.nio.file.Files.readAllBytes(
                new java.io.File("src/main/java/com/codename1/builders/AndroidGradleBuilder.java").toPath()), "UTF-8");
        int at = builderSrc.indexOf("KotlinStdlibAlignment.constraintsBlock(");
        check(at >= 0, "the builder calls the alignment");
        String call = builderSrc.substring(at, builderSrc.indexOf(";", at))
                .replaceAll("//[^\n]*", "");
        // Split at the commas that separate ARGUMENTS -- the ones outside
        // parentheses -- and require a single argument to carry both hints.
        java.util.List<String> arguments = new java.util.ArrayList<String>();
        int depth = 0;
        int start = call.indexOf('(') + 1;
        boolean quoted = false;
        for (int i = start; i < call.length(); i++) {
            char c = call.charAt(i);
            if (quoted) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    quoted = false;
                }
                continue;
            }
            if (c == '"') {
                quoted = true;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (depth == 0) {
                    arguments.add(call.substring(start, i));
                    break;
                }
                depth--;
            } else if (c == ',' && depth == 0) {
                arguments.add(call.substring(start, i));
                start = i + 1;
            }
        }
        boolean together = false;
        for (int i = 0; i < arguments.size(); i++) {
            if (arguments.get(i).indexOf("android.gradle.androidx") >= 0
                    && arguments.get(i).indexOf("android.xgradle_default_config") >= 0) {
                together = true;
            }
        }
        check(together, "the two android fragments are handed over in ONE closure, "
                + "and the call splits them across arguments: " + arguments);
    }

    /**
     * The shapes this round found, each a valid Gradle spelling that read as
     * something it is not.
     */
    @Test
    public void everySelectorAndHandlerSpellingIsRead() {
        String open = "    configurations.all {\n        resolutionStrategy {\n"
                + "            componentSelection {\n";
        String close = "            }\n        }\n    }\n";

        // A closure passed in parentheses is the same call as a trailing one.
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    configurations.all { resolutionStrategy"
                        + ".componentSelection({ rules ->\n        rules.all { s -> if "
                        + "(s.candidate.module == 'kotlin-stdlib-jdk8') "
                        + "s.reject('x') }\n    }) }\n")),
                "a parenthesised componentSelection is still one");

        // A rule keyed on another Kotlin module cannot reject either shim, so
        // matching the group prefix alone gave away the alignment for nothing.
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        open + "                withModule('org.jetbrains.kotlin:"
                        + "kotlin-reflect') { s -> s.reject('x') }\n" + close)
                        .contains("kotlin-stdlib-jdk8:1.8.0"),
                "a rule on kotlin-reflect touches neither shim");
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        open + "                withModule('org.jetbrains.kotlin:"
                        + "kotlin-stdlib-jdk8') { s -> s.reject('x') }\n" + close)),
                "and one on a shim still stands the block down");

        // The constraint handler takes a configuration and a notation too.
        String[] handlers = {
            "    constraints.add('implementation', "
                    + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!')\n",
            "    dependencies.constraints.add('implementation', "
                    + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!')\n",
        };
        for (int i = 0; i < handlers.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    handlers[i]);
            check("".equals(out), "<<" + handlers[i].trim() + ">> is a strict pin, "
                    + "got <<" + out + ">>");
        }

        // A subprojects block configures the children, not this application.
        String children = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    subprojects {\n        dependencies {\n            implementation("
                + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22')\n        }\n    }\n");
        check(children.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a subproject declaration is not the app's, got <<" + children + ">>");

        // allprojects DOES include this one, which is the distinction.
        String every = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    allprojects {\n        dependencies {\n            implementation("
                + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22')\n        }\n    }\n");
        check(!every.contains("kotlin-stdlib-jdk8:1.8.0")
                        && every.contains("kotlin-stdlib-jdk7:1.8.0"),
                "an allprojects declaration is the app's too, got <<" + every + ">>");
    }

    /**
     * A selection rule may name its module by whole coordinate --
     * {@code withModule('org.jetbrains.kotlin:kotlin-stdlib-jdk8')} -- which is
     * neither the bare artifact name nor the group on its own, so a rule written
     * that way looked like it concerned nothing of ours.
     */
    @Test
    public void aSelectionRuleMayNameItsModuleByCoordinate() {
        String open = "    configurations.all {\n        resolutionStrategy {\n"
                + "            componentSelection {\n";
        String close = "            }\n        }\n    }\n";
        String ours = KotlinStdlibAlignment.constraintsBlock("implementation",
                open + "                withModule('org.jetbrains.kotlin:"
                + "kotlin-stdlib-jdk8') { selection ->\n"
                + "                    if (selection.candidate.version == '1.8.0') {\n"
                + "                        selection.reject('unsupported')\n"
                + "                    }\n                }\n" + close);
        check("".equals(ours), "the rule may reject the floor, got <<" + ours + ">>");

        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        open + "                withModule('com.squareup.okhttp3:"
                        + "okhttp') { selection ->\n"
                        + "                    selection.reject('unsupported')\n"
                        + "                }\n" + close)
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "a rule on another module rejects nothing this writes");
    }

    /**
     * A component-selection block holds a rule per {@code all { }}, and the
     * predicate that names this family has to be in the SAME rule as the
     * rejection. Accumulated across the block, a rule that merely mentions
     * Kotlin paired up with a sibling that rejects something else, so the block
     * stood down for a rejection that could not touch it -- which leaves the
     * duplicate exactly where it was.
     */
    @Test
    public void aRejectionCountsOnlyInTheRuleThatNamesTheFamily() {
        String open = "    configurations.all {\n        resolutionStrategy {\n"
                + "            componentSelection {\n";
        String close = "            }\n        }\n    }\n";
        String logsKotlin = "                all { s ->\n                    if "
                + "(s.candidate.module == 'kotlin-stdlib') "
                + "{ logger.info(s.candidate.version) }\n                }\n";
        String rejectsOther = "                all { s ->\n                    if "
                + "(s.candidate.module == 'okhttp') "
                + "{ s.reject('unsupported') }\n                }\n";
        String rejectsOurs = "                all { s ->\n                    if "
                + "(s.candidate.module == 'kotlin-stdlib-jdk8') "
                + "{ s.reject('unsupported') }\n                }\n";

        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        open + logsKotlin + rejectsOther + close)
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "neither rule can reject the floor");

        String[] rejecting = {
            open + rejectsOther + rejectsOurs + close,
            open + rejectsOurs + close,
        };
        for (int i = 0; i < rejecting.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    rejecting[i]);
            check("".equals(out), "a rule that names and rejects stands the block "
                    + "down, got <<" + out + ">>");
        }
    }

    /**
     * A component-selection rule is normally written over several lines, and
     * then its opener, its predicate and its reject are three statements. The
     * one-statement reading saw none of them together, so a rule that removes
     * the very version this writes left the constraint nothing to resolve to.
     */
    @Test
    public void aComponentSelectionRuleIsReadAcrossItsWholeBody() {
        String multiline = "    configurations.all {\n        resolutionStrategy {\n"
                + "            componentSelection {\n                all { selection ->\n"
                + "                    if (selection.candidate.module == "
                + "'kotlin-stdlib-jdk8'\n"
                + "                            && selection.candidate.version == "
                + "'1.8.0') {\n"
                + "                        selection.reject('unsupported')\n"
                + "                    }\n                }\n            }\n"
                + "        }\n    }\n";
        String out = KotlinStdlibAlignment.constraintsBlock("implementation", multiline);
        check("".equals(out), "the rule may reject the floor, got <<" + out + ">>");

        String[] harmless = {
            // Rejecting something else.
            "    configurations.all {\n        resolutionStrategy {\n"
                    + "            componentSelection {\n                all { s ->\n"
                    + "                    if (s.candidate.module == 'okhttp') {\n"
                    + "                        s.reject('unsupported')\n"
                    + "                    }\n                }\n            }\n"
                    + "        }\n    }\n",
            // Rejecting nothing.
            "    configurations.all {\n        resolutionStrategy {\n"
                    + "            componentSelection {\n                all { s ->\n"
                    + "                    logger.info(s.candidate.module)\n"
                    + "                }\n            }\n        }\n    }\n",
            // On a configuration the constraint never reaches.
            "    configurations.create('tooling') {\n        resolutionStrategy {\n"
                    + "            componentSelection {\n                all { if "
                    + "(it.candidate.group == 'org.jetbrains.kotlin') it.reject('x') }\n"
                    + "            }\n        }\n    }\n",
        };
        for (int i = 0; i < harmless.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation", harmless[i])
                            .contains("kotlin-stdlib-jdk7:1.8.0"),
                    "rule " + i + " rejects nothing this writes");
        }

        // And the scan does not swallow what comes after a harmless rule.
        check(!KotlinStdlibAlignment.constraintsBlock("implementation",
                        harmless[1] + "    implementation 'org.jetbrains.kotlin:"
                        + "kotlin-stdlib-jdk8:1.9.22'\n")
                        .contains("kotlin-stdlib-jdk8:1.8.0"),
                "a declaration after the rule is still read");
    }

    /**
     * A call with no literal argument still HAPPENED, and what it set is
     * unknown. Recorded as nothing at all, a conditional mixing an unreadable
     * arm with a readable one looked like a single readable branch -- so the
     * lowest was the arm that could be read, and the constraints went in beside
     * a pin that may well be pre-merge.
     */
    @Test
    public void anUnreadableArmIsAnAlternativeToo() {
        String jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8";
        String[] mixed = {
            "    implementation('" + jdk8 + "') { version { if (legacy) "
                    + "strictly providers.gradleProperty('k').get() "
                    + "else strictly '1.9.22' } }\n",
            "    implementation('" + jdk8 + "') { version { legacy ? "
                    + "strictly(providers.gradleProperty('k').get()) "
                    + ": strictly('1.9.22') } }\n",
        };
        for (int i = 0; i < mixed.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    mixed[i]);
            check("".equals(out), "the unreadable arm may be the live one, got <<"
                    + out + ">>");
        }

        // A SEQUENCE ending in a readable call is still read: there the last one
        // wins, and it is known.
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation('" + jdk8 + "') { version { "
                        + "strictly providers.gradleProperty('k').get(); "
                        + "strictly '1.9.22' } }\n")
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "a sequence ending readable is read");
    }

    /**
     * A component-selection rule rejects CANDIDATES, outside any declaration, so
     * the rejection reading that lives on a declaration never saw it. Such a rule
     * can remove the very version this writes, and the constraint then has
     * nothing to resolve to.
     */
    @Test
    public void aComponentSelectionRuleMayRejectTheFloor() {
        String[] rejecting = {
            "    configurations.all { resolutionStrategy.componentSelection { all { "
                    + "if (it.candidate.module == 'kotlin-stdlib-jdk8' && "
                    + "it.candidate.version == '1.8.0') it.reject('unsupported') } } }\n",
            "    configurations.all { resolutionStrategy.componentSelection { all { "
                    + "if (it.candidate.group == 'org.jetbrains.kotlin') "
                    + "it.reject('unsupported') } } }\n",
        };
        for (int i = 0; i < rejecting.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    rejecting[i]);
            check("".equals(out), "a rule that may reject the floor stands the block "
                    + "down, got <<" + out + ">>");
        }

        // A rule that rejects nothing, one that does not mention this family, and
        // one on a configuration the constraint never reaches all leave it alone.
        String[] harmless = {
            "    configurations.all { resolutionStrategy.componentSelection { all { "
                    + "logger.info(it.candidate.module) } } }\n",
            "    configurations.all { resolutionStrategy.componentSelection { all { "
                    + "if (it.candidate.module == 'okhttp') it.reject('x') } } }\n",
            "    configurations.create('tooling').resolutionStrategy"
                    + ".componentSelection { all { if (it.candidate.group == "
                    + "'org.jetbrains.kotlin') it.reject('x') } }\n",
        };
        for (int i = 0; i < harmless.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation", harmless[i])
                            .contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + harmless[i].trim() + ">> rejects nothing this writes");
        }
    }

    /**
     * A value that is a NAME rather than a literal copies a binding that is
     * already known. Reading only literals recorded the new name as unknown, so
     * a force through it named nothing and the constraints went in beside a pin
     * still in effect.
     */
    @Test
    public void aDefinitionMayCopyAnotherOne() {
        String pre = "org.jetbrains.kotlin:kotlin-stdlib:1.7.22";
        String force = "    configurations.all { resolutionStrategy.force forced }\n";
        String[] copies = {
            "    def coord = '" + pre + "'\n    def forced = coord\n",
            "    def coord = '" + pre + "'\n    ext.set('forced', coord)\n",
            "    def coord = '" + pre + "'\n    ext.forced = coord\n",
            "    def a = '" + pre + "'\n    def b = a\n    def forced = b\n",
        };
        for (int i = 0; i < copies.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    copies[i] + force);
            check("".equals(out), "the copy carries the coordinate, got <<"
                    + out + ">>");
        }

        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    def coord = 'org.jetbrains.kotlin:kotlin-stdlib:1.9.22'\n"
                        + "    def forced = coord\n" + force)
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "a merged-era one leaves the alignment alone");
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    def forced = whateverThisIs\n" + force)
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "and copying something unknown binds nothing");
    }

    /**
     * A plain coordinate version is a SOFT requirement in Gradle, exactly as a
     * rich {@code require} is, and below the floor the constraint raises it. So
     * an app that declares an old shim directly is aligned rather than left
     * alone: standing the block down there, or skipping that artifact's own
     * constraint, kept the pre-merge shim beside whatever selected a merged-era
     * base -- the duplicate this exists to prevent, in the graph it exists for.
     */
    @Test
    public void aSoftPreMergeDeclarationIsRaisedRatherThanHonoured() {
        String jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8";
        String[] soft = {
            "    implementation '" + jdk8 + ":1.7.22'\n",
            "    implementation('" + jdk8 + ":1.7.22')\n",
            "    implementation group: 'org.jetbrains.kotlin', "
                    + "name: 'kotlin-stdlib-jdk8', version: '1.7.22'\n",
            "    implementation('" + jdk8 + "') { version { require '1.7.22' } }\n",
        };
        for (int i = 0; i < soft.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    soft[i]);
            check(out.contains("kotlin-stdlib-jdk7:1.8.0")
                            && out.contains("kotlin-stdlib-jdk8:1.8.0"),
                    "<<" + soft[i].trim() + ">> is raised, and its sibling with it, "
                            + "got <<" + out + ">>");
        }

        // Anything that really PINS it still stands the block down, because the
        // constraint cannot raise those.
        String[] firm = {
            "    implementation '" + jdk8 + ":1.7.22!!'\n",
            "    implementation('" + jdk8 + "') { version { strictly '1.7.22' } }\n",
            "    configurations.all { resolutionStrategy.force '" + jdk8 + ":1.7.22' }\n",
            "    implementation('" + jdk8 + "') { version { require '1.+'; "
                    + "reject '[1.8.0,)' } }\n",
            // A range is satisfied or it is not: `[1.0,1.5]` and 1.8.0 have no
            // version in common, so it cannot be raised either.
            "    implementation '" + jdk8 + ":[1.0,1.5]'\n",
            // And a version this cannot read says nothing about what it will be.
            "    implementation \"" + jdk8 + ":$mystery\"\n",
        };
        for (int i = 0; i < firm.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    firm[i]);
            check("".equals(out), "<<" + firm[i].trim()
                    + ">> cannot be raised, got <<" + out + ">>");
        }

        // At or above the floor a soft version already satisfies the constraint,
        // so that artifact is still left to the app and only its sibling raised.
        String merged = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation '" + jdk8 + ":1.9.22'\n");
        check(merged.contains("kotlin-stdlib-jdk7:1.8.0")
                        && !merged.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a merged-era declaration stands in for its own constraint, got <<"
                        + merged + ">>");
    }

    /**
     * A bare carriage return ends a line in Groovy exactly as a newline does.
     * The comment scan learned that; the statement splitter had not, so every
     * statement of a CR-only fragment merged into one and a main-variant
     * configuration paired with a debug-only coordinate.
     */
    @Test
    public void aBareCarriageReturnSeparatesStatements() {
        String jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8";
        String separate = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'com.example:other:1.0'\r"
                + "    debugImplementation '" + jdk8 + ":1.9.22'\r");
        check(separate.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a debug-only coordinate is not the main declaration, got <<"
                        + separate + ">>");

        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation 'com.example:other:1.0'\r"
                        + "    implementation '" + jdk8 + ":1.7.22!!'\r")),
                "and a pin on its own CR-terminated line is read");

        // CRLF is one break, not two.
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation 'com.example:other:1.0'\r\n"
                        + "    debugImplementation '" + jdk8 + ":1.9.22'\r\n")
                        .contains("kotlin-stdlib-jdk8:1.8.0"),
                "CRLF does not split twice");

        // Everything that continues a statement across a newline continues it
        // across a carriage return.
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation \\\r        '" + jdk8 + ":1.7.22!!'\r")),
                "a line continuation still continues");
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    configurations.all { resolutionStrategy.eachDependency "
                        + "{ d ->\r        if (d.requested.name == 'kotlin-stdlib')\r"
                        + "            d.useVersion '1.7.22'\r    } }\r")),
                "an unbraced body still joins its condition");
    }

    /**
     * Gradle orders a shortened version below a longer one: {@code 1.8} is below
     * {@code 1.8.0}. Padding the missing segment with zero called them equal, so
     * a strict range that stops just short of the floor looked like it admitted
     * it and the constraints went into a graph that cannot resolve them.
     */
    @Test
    public void aShortenedUpperBoundStopsShortOfTheFloor() {
        String jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8";
        String[] capped = {
            "    implementation('" + jdk8 + "') { version { strictly '[1.7,1.8]' } }\n",
            "    implementation('" + jdk8 + "') { version { strictly '[1.7,1.8.0)' } }\n",
        };
        for (int i = 0; i < capped.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    capped[i]);
            check("".equals(out), "<<" + capped[i].trim()
                    + ">> cannot admit the floor, got <<" + out + ">>");
        }

        String[] reaching = {
            "    implementation('" + jdk8 + "') { version { strictly '[1.7,1.8.0]' } }\n",
            "    implementation('" + jdk8 + "') { version { strictly '[1.7,1.9]' } }\n",
        };
        for (int i = 0; i < reaching.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation",
                            reaching[i]).contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + reaching[i].trim() + ">> admits the floor");
        }
    }

    /**
     * A ternary chooses between its arms exactly as an if/else does, and so does
     * an elvis. Read as a sequence, only the last setter counted -- so the arm
     * holding a strict pre-merge version was passed over.
     */
    @Test
    public void aTernaryChoosesBetweenVersionsToo() {
        String jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8";
        String[] branched = {
            "    implementation('" + jdk8 + "') { version { "
                    + "legacy ? strictly('1.7.22') : strictly('1.9.22') } }\n",
            // A switch arm is an alternative like any other.
            "    implementation('" + jdk8 + "') { version { switch (mode) { "
                    + "case 'legacy': strictly '1.7.22'; break; "
                    + "default: strictly '1.9.22' } } }\n",
            "    implementation('" + jdk8 + "') { version { switch (mode) { "
                    + "case 'modern': strictly '1.9.22'; break; "
                    + "default: strictly '1.7.22' } } }\n",
            "    implementation('" + jdk8 + "') { version { "
                    + "legacy ? strictly('1.9.22') : strictly('1.7.22') } }\n",
            "    implementation('" + jdk8 + "') { version { "
                    + "legacy ?: strictly('1.9.22') ; strictly '1.7.22' } }\n",
        };
        for (int i = 0; i < branched.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    branched[i]);
            check("".equals(out), "either arm may run, got <<" + out + ">>");
        }

        // Safe navigation is the one question mark that chooses nothing, so the
        // setters either side of it stay a sequence and the last one wins. Both
        // versions are readable on purpose: an unreadable one would suppress for
        // a different reason and test nothing.
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation('" + jdk8 + "') { version { "
                        + "strictly '1.7.22'; strictly '1.9.22' } "
                        + "because project?.name.toString() }\n")
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "safe navigation is not a branch");
    }

    /**
     * Groovy ends a line at a bare carriage return too. Searching only for the
     * newline swallowed the whole remainder of a CR-only fragment as part of a
     * line comment -- including the strict pin that followed it.
     */
    @Test
    public void aLineCommentEndsAtEitherTerminator() {
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    // explanation\r    implementation("
                        + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                        + "{ version { strictly '1.7.22' } }\r")),
                "the pin after a CR-terminated comment is still read");

        // And the comment still hides what is on ITS own line.
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    // implementation 'org.jetbrains.kotlin:"
                        + "kotlin-stdlib-jdk8:1.7.22!!'\n    implementation "
                        + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n")
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "a commented-out pin is still a comment");
    }

    /**
     * An unqualified call has to be one of the dependency handler's own adders.
     * Matched by prefix, any helper an app had defined -- {@code def addNote = {
     * config, text -> .. }} -- declared a dependency as far as this was
     * concerned, and that artifact's constraint was skipped as already handled.
     */
    @Test
    public void anUnqualifiedAdderIsNamedNotPrefixed() {
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    def addNote = { config, text -> println text }\n"
                        + "    addNote('implementation', 'org.jetbrains.kotlin:"
                        + "kotlin-stdlib-jdk8:1.9.22')\n")
                        .contains("kotlin-stdlib-jdk8:1.8.0"),
                "an app helper declares nothing");

        String pin = "org.jetbrains.kotlin:kotlin-stdlib:1.7.22!!";
        String[] adders = {
            "    dependencies {\n        add 'implementation', '" + pin + "'\n    }\n",
            "    dependencies {\n        addProvider 'implementation', '"
                    + pin + "'\n    }\n",
            // A qualified call does not consult the name at all, so a handler
            // that grows a fourth adder keeps working through its receiver.
            "    dependencies.whateverTheyAddNext('implementation', '" + pin + "')\n",
        };
        for (int i = 0; i < adders.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    adders[i]);
            check("".equals(out), "<<" + adders[i].trim() + ">> declares, got <<"
                    + out + ">>");
        }
    }

    /**
     * {@code ext.set('dep', '...')} is the extension's own setter, and the name
     * is its first argument. Read as a dotted assignment it recorded a property
     * called {@code set} and lost the real one, so a later reference through the
     * bare name named nothing -- and the constraints went in beside a force that
     * was still in effect.
     */
    @Test
    public void theExtraPropertiesSetterBindsItsFirstArgument() {
        String pre = "org.jetbrains.kotlin:kotlin-stdlib:1.7.22";
        String use = "    configurations.all { resolutionStrategy.force stdlib }\n";
        String[] setters = {
            "    ext.set('stdlib', '" + pre + "')\n",
            "    project.ext.set('stdlib', '" + pre + "')\n",
            "    ext.set(\"stdlib\", \"" + pre + "\")\n",
        };
        for (int i = 0; i < setters.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    setters[i] + use);
            check("".equals(out), "<<" + setters[i].trim()
                    + ">> binds stdlib, got <<" + out + ">>");
        }

        // Merged-era through the same setter leaves the alignment to be written.
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    ext.set('stdlib', 'org.jetbrains.kotlin:"
                        + "kotlin-stdlib:1.9.22')\n" + use)
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "a merged-era force leaves the alignment alone");

        // The property is bound under its own name, and no property called `set`
        // is created. A soft coordinate emits either way -- the constraint raises
        // it -- so the strict spelling is what shows the binding.
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    ext.set('stdlib', '" + pre + "!!')\n"
                        + "    implementation stdlib\n")),
                "the name carries the strict pin");
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    ext.set('stdlib', '" + pre + "!!')\n"
                        + "    implementation set\n")
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "and nothing is bound under the setter's own name");

        // A set() on something that is not the extension binds nothing.
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    someMap.set('stdlib', '" + pre + "')\n" + use)
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "someMap.set is not the extension");
    }

    /**
     * Two calls one after another are a sequence and the last wins; two in the
     * arms of a conditional are alternatives, and which one runs is not readable
     * here. Taking the last of THOSE wrote the shim constraints beside a strict
     * pre-merge pin that may well be the live branch.
     */
    @Test
    public void aConditionalMakesTheCallsAlternatives() {
        String jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8";
        String[] branched = {
            "    implementation('" + jdk8 + "') { version { "
                    + "if (legacy) strictly '1.7.22' else strictly '1.9.22' } }\n",
            "    implementation('" + jdk8 + "') { version { "
                    + "if (legacy) strictly '1.9.22' else strictly '1.7.22' } }\n",
        };
        for (int i = 0; i < branched.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    branched[i]);
            check("".equals(out), "either arm may run, got <<" + out + ">>");
        }

        // A plain sequence still keeps what it was set to last, in both
        // directions -- that is what makes this about branches and not about
        // taking the lowest version anywhere.
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation('" + jdk8 + "') { version { "
                        + "strictly '1.9.22'; strictly '1.7.22' } }\n")),
                "a sequence ending pre-merge stands the block down");
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation('" + jdk8 + "') { version { "
                        + "strictly '1.7.22'; strictly '1.9.22' } }\n")
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "and one ending merged-era does not");
    }

    /**
     * A rich version that is PRESENT but unreadable is not an invitation to read
     * the coordinate instead. Reported as merged-era, such a declaration had its
     * own constraint skipped as satisfied while the sibling was raised around it.
     */
    @Test
    public void anUnreadableStrictVersionIsNotTheCoordinate() {
        String jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8";
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation('" + jdk8 + ":1.9.22') { version { "
                        + "strictly providers.gradleProperty('legacy').get() } }\n")),
                "an unreadable strictly is not the coordinate's version");
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    configurations.all { resolutionStrategy.eachDependency "
                        + "{ d ->\n        if (d.requested.name == "
                        + "'kotlin-stdlib-jdk8') d.useVersion someProperty\n    } }\n")),
                "and neither is an unreadable useVersion");

        // A readable one still overrides the coordinate, which is the case that
        // put the rich reading here in the first place.
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation('" + jdk8 + ":1.7.22') { version { "
                        + "strictly '1.9.22' } }\n")
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "a readable strictly is read past the coordinate");
    }

    /**
     * A configuration the app made can still INHERIT the constraint.
     * {@code configurations.create('tooling').extendsFrom(configurations
     * .implementation)} is not an independent graph, and reading only the name
     * it was created under exempted it -- so the constraints went into a graph
     * that then fails on the version they raise.
     */
    @Test
    public void aCustomConfigurationMayInheritTheConstraint() {
        String conflict = ".resolutionStrategy.failOnVersionConflict()\n";
        String[] inheriting = {
            "    configurations.create('tooling').extendsFrom("
                    + "configurations.implementation)" + conflict,
            "    configurations.create('tooling').extendsFrom("
                    + "configurations.api)" + conflict,
            "    configurations.create('tooling').extendsFrom("
                    + "configurations.getByName('implementation'))" + conflict,
            "    configurations.tooling.extendsFrom("
                    + "configurations.implementation)" + conflict,
        };
        for (int i = 0; i < inheriting.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    inheriting[i]);
            check("".equals(out), "<<" + inheriting[i].trim()
                    + ">> inherits what these constrain, got <<" + out + ">>");
        }

        // Extending something the constraint is NOT on does not inherit it, or
        // the exemption would cover nothing at all.
        String[] independent = {
            "    configurations.create('tooling').extendsFrom("
                    + "configurations.compileOnly)" + conflict,
            "    configurations.create('tooling').extendsFrom("
                    + "configurations.other)" + conflict,
            "    configurations.create('tooling')" + conflict,
            "    configurations.tooling" + conflict,
        };
        for (int i = 0; i < independent.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation",
                            independent[i]).contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + independent[i].trim() + ">> governs another graph");
        }
    }

    /**
     * The resolvable classpaths EXTEND the constrained configurations and are
     * where a resolution strategy actually runs, so a conflict check on one
     * governs the graph these constraints are resolved in.
     */
    @Test
    public void aResolvableClasspathInheritsTheConstraint() {
        String conflict = ".resolutionStrategy.failOnVersionConflict()\n";
        String[] inheriting = {
            "    configurations.releaseRuntimeClasspath" + conflict,
            "    configurations.runtimeClasspath" + conflict,
            "    configurations.debugCompileClasspath" + conflict,
            "    configurations.getByName('releaseRuntimeClasspath')" + conflict,
        };
        for (int i = 0; i < inheriting.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    inheriting[i]);
            check("".equals(out), "<<" + inheriting[i].trim()
                    + ">> resolves what these constrain, got <<" + out + ">>");
        }
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    configurations.tooling" + conflict)
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "a configuration that is neither still governs another graph");
    }

    /**
     * Groovy's explicit line continuation joins two physical lines into one
     * statement. Split at the newline, the configuration had no dependency and
     * the coordinate had no configuration, so neither said anything.
     */
    @Test
    public void anEscapedNewlineContinuesTheStatement() {
        String jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8";
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation \\\n        '" + jdk8 + ":1.7.22!!'\n")),
                "the continued statement carries its strict pin");
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation \\\n        '" + jdk8 + ":1.9.22'\n")
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "and a merged-era one is read as the declaration it is");
    }

    /**
     * Groovy accepts a redundant parenthesis, and the walk stepped over one.
     * Looking at the other it found something that is not an identifier, so the
     * strict pin read as nobody's argument and the constraints went in against
     * it.
     */
    @Test
    public void aRedundantParenthesisIsStillTheSameArgument() {
        String pin = "org.jetbrains.kotlin:kotlin-stdlib:1.7.22!!";
        String[] declarations = {
            "    implementation(('" + pin + "'))\n",
            "    implementation((('" + pin + "')))\n",
            "    implementation( ( '" + pin + "' ) )\n",
            "    implementation('" + pin + "')\n",
            "    implementation '" + pin + "'\n",
            // A later argument reaches the comma only once the parentheses are
            // behind it, and the enclosing call is the one with a NAME in front
            // of it -- a redundant pair has none, so the search goes outward.
            "    dependencies.add('implementation', ('" + pin + "'))\n",
            "    dependencies.addProvider('implementation', "
                    + "providers.provider { ('" + pin + "') })\n",
        };
        for (int i = 0; i < declarations.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    declarations[i]);
            check("".equals(out), "<<" + declarations[i].trim()
                    + ">> declares a strict pin, got <<" + out + ">>");
        }

        // Wrapping something in parentheses does not make it a declaration.
        String[] strangers = {
            "    (('" + pin + "'))\n",
            "    logger.lifecycle(('" + pin + "'))\n",
            "    myList.add('implementation', ('" + pin + "'))\n",
            "    def all = [('" + pin + "')]\n",
        };
        for (int i = 0; i < strangers.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation",
                            strangers[i]).contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + strangers[i].trim() + ">> declares nothing");
        }
    }

    /**
     * The dependency handler has three adders and may grow more, and the
     * coordinate one of them is handed may sit inside a provider closure. Only
     * {@code add} with the coordinate as a direct argument was read, so Gradle's
     * provider form declared nothing as far as this was concerned.
     */
    @Test
    public void theDependencyHandlerHasMoreThanOneAdder() {
        String pin = "org.jetbrains.kotlin:kotlin-stdlib:1.7.22!!";
        String[] declarations = {
            "    dependencies.addProvider('implementation', "
                    + "providers.provider { '" + pin + "' })\n",
            "    dependencies.addProvider('implementation', '" + pin + "')\n",
            "    dependencies.addProviderBundle('implementation', '" + pin + "')\n",
            "    dependencies {\n        addProvider 'implementation', '"
                    + pin + "'\n    }\n",
            "    dependencies.add('implementation', '" + pin + "')\n",
            "    dependencies {\n        add 'implementation', '" + pin + "'\n    }\n",
            "    implementation(providers.provider { '" + pin + "' })\n",
        };
        for (int i = 0; i < declarations.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    declarations[i]);
            check("".equals(out), "<<" + declarations[i].trim()
                    + ">> declares a strict pin, got <<" + out + ">>");
        }

        // The name matters where there is no receiver to check, which is the
        // shorthand inside a dependencies closure: read as a declaration, the
        // artifact it names is left to the app and only its sibling is raised.
        String merged = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    dependencies {\n        addProvider 'implementation', "
                + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n    }\n");
        check(merged.contains("kotlin-stdlib-jdk7:1.8.0")
                        && !merged.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a bare addProvider declares its artifact, got <<" + merged + ">>");

        // The receiver still decides for a qualified call, and an unqualified one
        // still has to look like an adder. Neither a list nor a version catalog
        // declares anything, and a literal that is nobody's argument declares
        // nothing either.
        String[] strangers = {
            "    myList.add('implementation', '" + pin + "')\n",
            "    catalog.add('implementation', '" + pin + "')\n",
            "    logger.lifecycle('" + pin + "')\n",
            "    def all = ['" + pin + "']\n",
            "    def make = { '" + pin + "' }\n",
        };
        for (int i = 0; i < strangers.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation",
                            strangers[i]).contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + strangers[i].trim() + ">> declares nothing");
        }
    }

    /**
     * Every spelling of "which configuration" goes through one reading, because
     * they are the same question. A lookup carries the name as a string, a
     * filter carries a closure and says nothing -- so a selector nobody
     * anticipated reads as "cannot say", which keeps the constraint out of a
     * graph that would fail on it.
     */
    @Test
    public void everySpellingOfAConfigurationIsReadTheSameWay() {
        String conflict = ".resolutionStrategy.failOnVersionConflict()\n";
        String[] reaching = {
            "    configurations.all { resolutionStrategy.failOnVersionConflict() }\n",
            "    configurations.configureEach { resolutionStrategy"
                    + ".failOnVersionConflict() }\n",
            // A filter may select the constrained graph and there is no name to
            // say otherwise. Reading one as "some other configuration" put the
            // constraints into a graph whose strategy fails the build on them.
            "    configurations.matching { it.name == 'releaseRuntimeClasspath' }"
                    + ".all { resolutionStrategy.failOnVersionConflict() }\n",
            "    configurations.implementation" + conflict,
            "    configurations.getByName('implementation')" + conflict,
            "    configurations['implementation']" + conflict,
            "    resolutionStrategy.failOnVersionConflict()\n",
        };
        for (int i = 0; i < reaching.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    reaching[i]);
            check("".equals(out), "<<" + reaching[i].trim()
                    + ">> may reach the constrained graph, got <<" + out + ">>");
        }

        String[] elsewhere = {
            "    configurations.create('tooling')" + conflict,
            "    configurations.tooling" + conflict,
            "    configurations.getByName('tooling')" + conflict,
            "    configurations['tooling']" + conflict,
        };
        for (int i = 0; i < elsewhere.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation",
                            elsewhere[i]).contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + elsewhere[i].trim() + ">> governs another graph");
        }

        // The plugin classpath is the same question asked of the same reading,
        // and only its dotted spelling had been recognised.
        String force = ".resolutionStrategy.force "
                + "'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'\n";
        String[] pluginOnly = {
            "    configurations.classpath" + force,
            "    configurations['classpath']" + force,
            "    configurations.getByName('classpath')" + force,
            "    buildscript.configurations['classpath']" + force,
        };
        for (int i = 0; i < pluginOnly.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation",
                            pluginOnly[i]).contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + pluginOnly[i].trim() + ">> is the plugin's graph");
        }
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    configurations.all" + force)),
                "and a force on the app's own graph still counts");
    }

    /**
     * Dependency-shaped data is not a dependency. A map stored in a variable and
     * never added to a configuration was read as a strict declaration, standing
     * the block down for an app that had declared nothing.
     *
     * <p>Safe to exclude here where the same exclusion was not safe for
     * enforcedPlatform: a map IS recorded as a definition's value, so a later
     * {@code implementation(catalog)} carries it and is read there.</p>
     */
    @Test
    public void aStrictMapHasToBeDeclaredToCount() {
        String map = "[group: 'org.jetbrains.kotlin', name: 'kotlin-stdlib', "
                + "version: '1.7.22!!']";
        String[] stored = {
            "    def catalog = " + map + "\n",
            "    ext.catalog = " + map + "\n",
        };
        for (int i = 0; i < stored.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation", stored[i])
                            .contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + stored[i].trim() + ">> declares nothing");
        }

        String[] declared = {
            "    def catalog = " + map + "\n    implementation(catalog)\n",
            "    implementation(" + map + ")\n",
            "    implementation group: 'org.jetbrains.kotlin', "
                    + "name: 'kotlin-stdlib', version: '1.7.22!!'\n",
            "    dependencies.add('implementation', " + map + ")\n",
        };
        for (int i = 0; i < declared.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    declared[i]);
            check("".equals(out), "<<" + declared[i].trim()
                    + ">> is a strict declaration, got <<" + out + ">>");
        }
    }

    /**
     * A coordinate can be a LATER argument: {@code dependencies.add('impl',
     * 'g:a:1.7.22!!')} puts it after a comma, and walking back one token found
     * the comma and stopped. The shims survived that because the call is also
     * recognised where the CONFIGURATION name is read; the base library has a
     * scan of its own that does not go through there, so its strict pin was
     * emitted straight over.
     */
    @Test
    public void aCoordinateMayBeALaterArgument() {
        String base = "org.jetbrains.kotlin:kotlin-stdlib:1.7.22!!";
        String[] declarations = {
            "    dependencies.add('implementation', '" + base + "')\n",
            "    project.dependencies.add('implementation', '" + base + "')\n",
            "    dependencies {\n        add 'implementation', '" + base + "'\n    }\n",
            "    dependencies.add('implementation', "
                    + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!')\n",
        };
        for (int i = 0; i < declarations.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    declarations[i]);
            check("".equals(out), "<<" + declarations[i].trim()
                    + ">> is a strict declaration, got <<" + out + ">>");
        }

        // The receiver still decides. A list is not a dependency handler, and a
        // coordinate handed to one is not declared.
        String[] strangers = {
            "    myList.add('implementation', '" + base + "')\n",
            "    logger.lifecycle('implementation', '" + base + "')\n",
        };
        for (int i = 0; i < strangers.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation",
                            strangers[i]).contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + strangers[i].trim() + ">> declares nothing");
        }
    }

    /**
     * A test suite's nested {@code dependencies { }} configures the suite's own
     * configurations. Its {@code implementation} has the same name as the app's
     * and is a different thing, so reading a declaration there as the app's
     * skipped the constraint for an artifact the release graph still carries.
     */
    @Test
    public void aNestedTestSuiteIsNotTheApplicationGraph() {
        String suite = "    testing {\n        suites {\n            test {\n"
                + "                dependencies {\n"
                + "                    implementation('org.jetbrains.kotlin:"
                + "kotlin-stdlib-jdk8:1.9.22')\n"
                + "                }\n            }\n        }\n    }\n";
        String out = KotlinStdlibAlignment.constraintsBlock("implementation", suite);
        check(out.contains("kotlin-stdlib-jdk8:1.8.0")
                        && out.contains("kotlin-stdlib-jdk7:1.8.0"),
                "the suite's declaration leaves both constrained, got <<" + out + ">>");

        // The app's own block, which looks the same one level up, still counts.
        String own = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    dependencies {\n        implementation('org.jetbrains.kotlin:"
                + "kotlin-stdlib-jdk8:1.9.22')\n    }\n");
        check(!own.contains("kotlin-stdlib-jdk8:1.8.0")
                        && own.contains("kotlin-stdlib-jdk7:1.8.0"),
                "the app's own declaration is still read, got <<" + own + ">>");
    }

    /**
     * failOnVersionConflict turns a disagreement into a build failure, so the
     * block stands down for it -- but only where it governs a configuration that
     * receives the constraint. One the app created and nothing extends never
     * sees it, and standing down there left the shim unaligned for nothing.
     */
    @Test
    public void aConflictStrategyCountsOnlyWhereTheConstraintReaches() {
        String tail = ".resolutionStrategy.failOnVersionConflict()\n";
        String[] elsewhere = {
            "    configurations.create('tooling')" + tail,
            "    configurations.tooling" + tail,
            "    configurations.getByName('tooling')" + tail,
        };
        for (int i = 0; i < elsewhere.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation",
                            elsewhere[i]).contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + elsewhere[i].trim() + ">> governs another graph");
        }

        String[] reaching = {
            "    configurations.all { resolutionStrategy.failOnVersionConflict() }\n",
            "    configurations.configureEach { resolutionStrategy"
                    + ".failOnVersionConflict() }\n",
            "    configurations.implementation" + tail,
            "    configurations.getByName('implementation')" + tail,
            // Not going through `configurations` at all, so it cannot be placed:
            // assumed to reach, because the other way emits into a graph that
            // fails the build outright.
            "    resolutionStrategy.failOnVersionConflict()\n",
        };
        for (int i = 0; i < reaching.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    reaching[i]);
            check("".equals(out), "<<" + reaching[i].trim()
                    + ">> reaches the constrained graph, got <<" + out + ">>");
        }
    }

    /**
     * The canonical Gradle rule puts its openers, its condition and its body on
     * separate lines, and every one of those splits was losing the override.
     */
    @Test
    public void aResolutionRuleSurvivesEveryLineBreakInIt() {
        String open = "    configurations.all { resolutionStrategy.eachDependency "
                + "{ d ->\n";
        String close = "    } }\n";
        String[] rules = {
            // The reported one: openers and condition on one line, body on the
            // next. The first token is `configurations` and two braces are open,
            // so neither test for an unbraced body saw a header here.
            open + "        if (d.requested.name == 'kotlin-stdlib')\n"
                    + "            d.useVersion '1.7.22'\n" + close,
            // Braced, which is how it is usually written. The condition and the
            // body were only glued together when the GROUP appeared, and a rule
            // comparing the name alone never names it.
            open + "        if (d.requested.name == 'kotlin-stdlib') {\n"
                    + "            d.useVersion '1.7.22'\n        }\n" + close,
            open + "        if (d.requested.group == 'org.jetbrains.kotlin') {\n"
                    + "            d.useVersion '1.7.22'\n        }\n" + close,
            // An else is the same statement as its if, and the condition that
            // names the family is on the if.
            open + "        if (d.requested.name != 'kotlin-stdlib')\n"
                    + "            d.useVersion '1.9.22'\n"
                    + "        else\n            d.useVersion '1.7.22'\n" + close,
            open + "        while (d.requested.name == 'kotlin-stdlib')\n"
                    + "            d.useVersion '1.7.22'\n" + close,
            // The openers, the condition and the body on three different
            // lines is one shape; all the openers AND the condition on ONE
            // line is another, and there the statement begins with
            // `configurations` and holds two open braces, so reading the first
            // token found no header at all.
            "    configurations.all { resolutionStrategy.eachDependency { d -> "
                    + "if (d.requested.name == 'kotlin-stdlib')\n"
                    + "        d.useVersion '1.7.22'\n" + close,
            "    configurations.all { resolutionStrategy.eachDependency { d -> "
                    + "if (d.requested.group == 'org.jetbrains.kotlin')\n"
                    + "        d.useVersion '1.7.22'\n" + close,
            // And the spellings that already worked still do.
            open + "        if (d.requested.name == 'kotlin-stdlib') "
                    + "d.useVersion '1.7.22'\n" + close,
        };
        for (int i = 0; i < rules.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    rules[i]);
            check("".equals(out), "rule " + i + " holds the base library pre-merge, "
                    + "got <<" + out + ">>");
        }

        // A trailing parenthesis that is not a header takes no body with it, or
        // every declaration would swallow the line after it.
        String[] independent = {
            "    dependencies {\n        implementation('com.example:x:1.0')\n"
                    + "        implementation('org.jetbrains.kotlin:"
                    + "kotlin-stdlib-jdk8:1.9.22')\n    }\n",
            "    configurations.all { resolutionStrategy.force"
                    + "('com.example:x:1.0') }\n"
                    + "    implementation 'org.jetbrains.kotlin:"
                    + "kotlin-stdlib-jdk8:1.9.22'\n",
            // A parenthesis inside a string is not a parenthesis.
            "    println 'if (x)'\n    implementation "
                    + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n",
        };
        for (int i = 0; i < independent.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation",
                            independent[i]).contains("kotlin-stdlib-jdk7:1.8.0"),
                    "line " + i + " does not take the next one with it");
        }

        // A rule that names the group and then narrows to ONE artifact governs
        // that artifact only: reading it as governing the family made the
        // siblings look declared and the block came out empty, which leaves the
        // duplicate exactly where it was.
        String narrowed = KotlinStdlibAlignment.constraintsBlock("implementation",
                open + "        if (d.requested.group == 'org.jetbrains.kotlin' && "
                + "d.requested.name == 'kotlin-stdlib') d.useVersion '1.9.22'\n"
                + close);
        check(narrowed.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a narrowed merged-era rule keeps the alignment, got <<"
                        + narrowed + ">>");
    }

    /**
     * One statement can name a module twice: {@code force} takes varargs, so
     * {@code force 'g:a:1.9.22', 'g:a:1.7.22'} is one call listing the same
     * module at two versions. Reading the first reported the merged-era one and
     * wrote the shim constraints beside a base library that may be forced
     * pre-merge -- the failure that reaches the device rather than the build.
     */
    @Test
    public void aForceMayListOneModuleMoreThanOnce() {
        String base = "org.jetbrains.kotlin:kotlin-stdlib:";
        // Either order reaches the same verdict, because the answer does not
        // depend on which selector Gradle keeps.
        String[] orders = {
            "'" + base + "1.9.22', '" + base + "1.7.22'",
            "'" + base + "1.7.22', '" + base + "1.9.22'",
            "'" + base + "1.9.22', '" + base + "1.7.22', '" + base + "1.9.22'",
        };
        for (int i = 0; i < orders.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    configurations.all { resolutionStrategy.force "
                    + orders[i] + " }\n");
            check("".equals(out), "<<" + orders[i]
                    + ">> forces the base library pre-merge, got <<" + out + ">>");
        }

        // A force that never goes below the floor still leaves the block to write.
        String merged = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    configurations.all { resolutionStrategy.force '" + base
                + "1.9.22', '" + base + "1.8.10' }\n");
        check(merged.contains("kotlin-stdlib-jdk7:1.8.0"),
                "a merged-era force leaves the alignment alone, got <<" + merged + ">>");
    }

    /**
     * The {@code !!} spelling skips the configuration check, because a strict pin
     * is honoured wherever it is declared. "Wherever" still means declared: a
     * coordinate merely passed to something -- a log line, a list -- is not a
     * dependency, and reading one as a pin stood the whole block down for an app
     * that had declared nothing.
     */
    @Test
    public void aStrictCoordinateHasToBeDeclaredToCount() {
        String pin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!";
        String[] declarations = {
            "    implementation '" + pin + "'\n",
            "    implementation('" + pin + "')\n",
            // Whatever the configuration is called: the rule is that a
            // configuration is never reached through a receiver, not a list of
            // the ones that count.
            "    myCustomConfig '" + pin + "'\n",
            "    debugImplementation '" + pin + "'\n",
            "    kapt('" + pin + "')\n",
            "    constraints {\n        implementation '" + pin + "'\n    }\n",
            "    dependencies.add('implementation', '" + pin + "')\n",
            // The spellings that carry the version somewhere other than the
            // coordinate are not asked the question at all.
            "    implementation group: 'org.jetbrains.kotlin', "
                    + "name: 'kotlin-stdlib-jdk8', version: '1.7.22!!'\n",
            "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8') "
                    + "{ version { strictly '1.7.22' } }\n",
        };
        for (int i = 0; i < declarations.length; i++) {
            check("".equals(KotlinStdlibAlignment.constraintsBlock(
                            "implementation", declarations[i])),
                    "<<" + declarations[i].trim() + ">> declares a strict pin");
        }

        String[] merelyCarried = {
            "    logger.lifecycle('" + pin + "')\n",
            "    project.logger.info('" + pin + "')\n",
            "    myList.add('" + pin + "')\n",
            "    def all = ['" + pin + "']\n",
        };
        for (int i = 0; i < merelyCarried.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation",
                            merelyCarried[i]).contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + merelyCarried[i].trim() + ">> declares nothing");
        }
    }

    /**
     * A fragment interpolated twice is executed twice. injectRepo goes into the
     * buildscript repositories and again into the project ones after the android
     * block, so a name it binds is restored at the second -- and scanning it once
     * left the scan holding whatever came between.
     *
     * <p>Reported with a reassignment inside {@code android { }}, which does not
     * reproduce: a brace this class can see makes the assignment conditional, and
     * a conditional reassignment already refuses to discard a Kotlin coordinate.
     * The shape that does reach it is an UNCONDITIONAL one, which app text
     * produces by closing its own wrapper early -- so the replay is what the
     * script does, and the scan follows it rather than the argument for it.</p>
     */
    @Test
    public void aFragmentInterpolatedTwiceIsScannedTwice() {
        String pin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22";
        String repositories = "project.ext.dep = '" + pin + "'\n";
        String reassign = "dep = 'com.example:other:1.0'\n";
        String use = "implementation(dep) { version { strictly '1.7.22' } }\n";

        String replayed = KotlinStdlibAlignment.constraintsBlock("implementation",
                "buildscript {\nrepositories {\n" + repositories + "}\n}\n",
                reassign,
                "repositories {\n" + repositories + "}\n",
                "dependencies {\n" + use + "}\n");
        check("".equals(replayed),
                "the second execution restores the coordinate, got <<" + replayed + ">>");

        String once = KotlinStdlibAlignment.constraintsBlock("implementation",
                "repositories {\n" + repositories + "}\n",
                reassign,
                "dependencies {\n" + use + "}\n");
        check(once.contains("kotlin-stdlib-jdk8:1.8.0"),
                "and without it the scan keeps the reassigned value, got <<"
                        + once + ">>");

        // The reported spelling, kept because it is the one a reader will try: a
        // reassignment the class can see a brace around is conditional either way.
        String guarded = KotlinStdlibAlignment.constraintsBlock("implementation",
                "repositories {\n" + repositories + "}\n",
                "android {\n" + reassign + "}\n",
                "dependencies {\n" + use + "}\n");
        check("".equals(guarded),
                "a guarded reassignment never hid the pin, got <<" + guarded + ">>");
    }

    /**
     * A resolution rule may compare one part of the coordinate only -- the name
     * is unambiguous on its own -- and it is in force either way. Requiring the
     * group beside it left the override unread, so the shims were raised to
     * their empty 1.8.0 jars around a base library the rule held at 1.7.22:
     * a build that links and then throws on the device.
     */
    @Test
    public void aResolutionRuleMayNameTheArtifactAlone() {
        String[] artifacts = {
            "kotlin-stdlib", "kotlin-stdlib-jdk7", "kotlin-stdlib-jdk8",
        };
        for (int i = 0; i < artifacts.length; i++) {
            // Every spelling of the same override reaches the same verdict. The two
            // predicates that identify an artifact had diverged, so a force naming
            // a shim by coordinate stood the block down while a useVersion holding
            // the SAME shim at the same version did not.
            String[] overrides = {
                "    configurations.all { resolutionStrategy.eachDependency { d ->\n"
                        + "        if (d.requested.name == '" + artifacts[i] + "') "
                        + "d.useVersion '1.7.22'\n    } }\n",
                "    configurations.all { resolutionStrategy.eachDependency { d ->\n"
                        + "        if (d.requested.group == 'org.jetbrains.kotlin' && "
                        + "d.requested.name == '" + artifacts[i] + "') "
                        + "d.useVersion '1.7.22'\n    } }\n",
                "    configurations.all { resolutionStrategy.force "
                        + "'org.jetbrains.kotlin:" + artifacts[i] + ":1.7.22' }\n",
            };
            for (int j = 0; j < overrides.length; j++) {
                check("".equals(KotlinStdlibAlignment.constraintsBlock(
                                "implementation", overrides[j])),
                        "<<" + overrides[j].trim() + ">> is an override in force");
            }
        }

        // A fork under another group shares the name and is a different module.
        String fork = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation(group: 'com.example', "
                + "name: 'kotlin-stdlib-jdk8', version: '1.0')\n");
        check(fork.contains("kotlin-stdlib-jdk8:1.8.0"),
                "another group's artifact is not the shim, got <<" + fork + ">>");

        // A name in a reason is not a reference to anything, and an unrelated
        // rule binds nothing.
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    implementation('com.example:x:1.0') "
                        + "{ because 'replaces kotlin-stdlib' }\n")
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "a name in a reason is prose");
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    configurations.all { resolutionStrategy.eachDependency "
                        + "{ d ->\n        if (d.requested.name == 'okhttp') "
                        + "d.useVersion '3.0.0'\n    } }\n")
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "an unrelated rule binds nothing");
    }

    /**
     * A platform takes a dependency notation, and a map is one. There is no
     * literal following the call in that spelling, so an enforced pre-merge BOM
     * written as a map read as absent entirely.
     */
    @Test
    public void anEnforcedPlatformMayBeWrittenAsAMap() {
        String[] managing = {
            "    implementation(enforcedPlatform(group: 'org.jetbrains.kotlin', "
                    + "name: 'kotlin-bom', version: '1.7.22'))\n",
            "    implementation(enforcedPlatform(version: '1.7.22', "
                    + "group: 'org.jetbrains.kotlin', name: 'kotlin-bom'))\n",
            "    implementation(enforcedPlatform(group: 'org.jetbrains.kotlin', "
                    + "name: 'kotlin-bom'))\n",
        };
        for (int i = 0; i < managing.length; i++) {
            check("".equals(KotlinStdlibAlignment.constraintsBlock(
                            "implementation", managing[i])),
                    "<<" + managing[i].trim() + ">> manages the family");
        }

        String[] harmless = {
            "    implementation(enforcedPlatform(group: 'org.jetbrains.kotlin', "
                    + "name: 'kotlin-bom', version: '1.9.22'))\n",
            "    implementation(enforcedPlatform(group: 'com.squareup.okhttp3', "
                    + "name: 'okhttp-bom', version: '3.0.0'))\n",
            // A plain platform is not strict in either spelling.
            "    implementation(platform(group: 'org.jetbrains.kotlin', "
                    + "name: 'kotlin-bom', version: '1.7.22'))\n",
        };
        for (int i = 0; i < harmless.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation", harmless[i])
                            .contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + harmless[i].trim() + ">> leaves the alignment alone");
        }
    }

    /**
     * An ENFORCED Kotlin platform is the one case a BOM stands the block down.
     * The class comment records why a plain {@code platform()} does not -- its
     * constraints are ordinary, so the higher version wins and these are
     * harmless beside it -- and {@code enforcedPlatform} is the other thing:
     * Gradle makes the same managed versions strict, so a pre-merge one pins
     * the family at 1.7.x and a 1.8.0 requirement beside it cannot resolve.
     */
    @Test
    public void anEnforcedPreMergeKotlinPlatformManagesTheFamily() {
        String[] enforced = {
            "    implementation(enforcedPlatform("
                    + "'org.jetbrains.kotlin:kotlin-bom:1.7.22'))\n",
            "    implementation enforcedPlatform("
                    + "'org.jetbrains.kotlin:kotlin-bom:1.7.22')\n",
            "    api(enforcedPlatform(\"org.jetbrains.kotlin:kotlin-bom:1.7.22\"))\n",
            "    def kv = '1.7.22'\n    implementation(enforcedPlatform("
                    + "\"org.jetbrains.kotlin:kotlin-bom:$kv\"))\n",
            // A version this cannot read is not proof it reaches the floor, and a
            // prerelease of the floor is below it.
            "    implementation(enforcedPlatform('org.jetbrains.kotlin:kotlin-bom'))\n",
            "    implementation(enforcedPlatform("
                    + "'org.jetbrains.kotlin:kotlin-bom:1.8.0-RC2'))\n",
        };
        for (int i = 0; i < enforced.length; i++) {
            check("".equals(KotlinStdlibAlignment.constraintsBlock(
                            "implementation", enforced[i])),
                    "<<" + enforced[i].trim() + ">> manages the family");
        }

        String[] harmless = {
            // At or past the floor it already agrees with these constraints.
            "    implementation(enforcedPlatform("
                    + "'org.jetbrains.kotlin:kotlin-bom:1.9.22'))\n",
            "    implementation(enforcedPlatform("
                    + "'org.jetbrains.kotlin:kotlin-bom:1.8.0'))\n",
            "    implementation(enforcedPlatform("
                    + "'com.squareup.okhttp3:okhttp-bom:4.0.0'))\n",
            // A plain platform is not strict, whatever version it names.
            "    implementation(platform('org.jetbrains.kotlin:kotlin-bom:1.7.22'))\n",
            "    implementation(platform('org.jetbrains.kotlin:kotlin-bom:1.9.22'))\n",
            // And the word in a reason is not the call.
            "    implementation('com.example:x:1.0') { because 'unlike "
                    + "enforcedPlatform(\\\"org.jetbrains.kotlin:kotlin-bom:1.7.22\\\")' }\n",
        };
        for (int i = 0; i < harmless.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation", harmless[i])
                            .contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + harmless[i].trim() + ">> leaves the alignment alone");
        }
    }

    /**
     * Quoted syntax is not syntax. A raw search for the plugin classpath, or for
     * a block opener, read the words in a {@code because} reason as the real
     * thing -- blanking the declaration that carried them, strict pin and all,
     * or putting every statement after it in a scope it was never in.
     */
    @Test
    public void syntaxQuotedInProseIsNotSyntax() {
        String pin = "org.jetbrains.kotlin:kotlin-stdlib:1.7.22!!";
        String[] prose = {
            "    implementation('" + pin + "') "
                    + "{ because 'match configurations.classpath' }\n",
            "    implementation('" + pin + "') { because 'as buildscript { } does' }\n",
            "    implementation('" + pin + "') { because 'set in ext { } above' }\n",
            "    implementation('" + pin + "') {\n"
                    + "        because 'configurations.classpath'\n    }\n",
            "    println 'buildscript { classpath }'\n    implementation('" + pin + "')\n",
        };
        for (int i = 0; i < prose.length; i++) {
            check("".equals(KotlinStdlibAlignment.constraintsBlock(
                            "implementation", prose[i])),
                    "the pin in <<" + prose[i].trim() + ">> is still read");
        }

        // The real spellings still say what they say.
        String[] real = {
            "    buildscript { dependencies { classpath '" + pin + "' } }\n",
            "    configurations.classpath.resolutionStrategy.force '" + pin + "'\n",
        };
        for (int i = 0; i < real.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation", real[i])
                            .contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + real[i].trim() + ">> is the plugin's graph");
        }

        // And a quoted block opener does not put what follows it in a scope.
        String scoped = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    println 'ext {'\n    def kv = '1.9.22'\n"
                + "    implementation \"org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kv\"\n");
        check(scoped.contains("kotlin-stdlib-jdk7:1.8.0"),
                "a quoted opener opens nothing, got <<" + scoped + ">>");
    }

    /**
     * {@code add} is an ordinary method name. Reading any call of it as a
     * dependency declaration let an unrelated API -- a version catalog, a list --
     * claim an artifact the app had never put in its graph, and the constraint
     * that artifact needed was skipped as already handled.
     */
    @Test
    public void anAddCallMustBeOnADependencyHandler() {
        String pin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22";
        String[] handlers = {
            "    dependencies.add('implementation', '" + pin + "!!')\n",
            "    project.dependencies.add('implementation', '" + pin + "!!')\n",
            "    dependencies {\n        add 'implementation', '" + pin + "!!'\n    }\n",
        };
        for (int i = 0; i < handlers.length; i++) {
            check("".equals(KotlinStdlibAlignment.constraintsBlock(
                            "implementation", handlers[i])),
                    "<<" + handlers[i].trim() + ">> declares a dependency");
        }

        String[] strangers = {
            "    catalog.add('implementation', '" + pin + "')\n",
            "    myList.add('implementation', '" + pin + "')\n",
            "    deps.add('implementation', '" + pin + "')\n",
        };
        for (int i = 0; i < strangers.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation",
                            strangers[i]).contains("kotlin-stdlib-jdk8:1.8.0"),
                    "<<" + strangers[i].trim() + ">> declares nothing");
        }
    }

    /**
     * A block opener shares the statement with what it opens. The walk that
     * reads a typed declaration began at the first token -- {@code if} -- and
     * stopped at its parenthesis, so the declaration behind it, and the pin that
     * declaration held, were never recorded.
     */
    @Test
    public void aDeclarationMayFollowABlockOpenerOnTheSameStatement() {
        String pin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22";
        String tail = " implementation(dep) { version { strictly '1.7.22' } } }\n";
        String[] openers = {
            "    if (true) { String dep = '" + pin + "';",
            "    if (a >= b) { String dep = '" + pin + "';",
            "    if (true) { def dep = '" + pin + "';",
            "    if (a) { if (b) { Map<String, String> m = [:]; String dep = '" + pin + "';",
        };
        for (int i = 0; i < openers.length; i++) {
            check("".equals(KotlinStdlibAlignment.constraintsBlock(
                            "implementation", openers[i] + tail)),
                    "<<" + openers[i].trim() + ">> declares dep");
        }

        // The brace that IS the value must not be mistaken for one that opens a
        // block, or the name being assigned to is skipped.
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    Closure c = { }\n    String dep = '" + pin + "'\n"
                        + "    implementation(dep) { version { strictly '1.7.22' } }\n")),
                "a closure assignment is still a declaration");

        // And a reassignment the brace GUARDS is still conditional, however it is
        // spelled -- taking it unconditionally throws away the coordinate the
        // condition may never replace.
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    def dep = 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'; "
                        + "if (project.hasProperty('other')) "
                        + "{ dep = 'com.example:other:1.0' }; "
                        + "implementation(dep) { version { strictly '1.7.22' } }\n")),
                "a one-line conditional reassignment stays conditional");
    }

    /**
     * A {@code buildscript} block configures the plugin classpath. That is a
     * separate resolution from the app's and cannot conflict with anything
     * written into {@code dependencies { }}, so an override or a shim
     * declaration there is not the app managing the family -- reading it as one
     * left an app graph carrying a pre-merge shim unaligned.
     */
    @Test
    public void aBuildscriptBlockIsNotTheApplicationGraph() {
        String pin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22";
        String[] pluginOnly = {
            "    buildscript { configurations.all { resolutionStrategy.force "
                    + "'org.jetbrains.kotlin:kotlin-stdlib:1.7.22' } }\n",
            "    buildscript {\n        dependencies {\n"
                    + "            classpath '" + pin + "!!'\n        }\n    }\n",
            "    buildscript {\n        dependencies {\n            classpath('" + pin
                    + "') { version { strictly '1.7.22' } }\n        }\n    }\n",
            // The spelling that names the configuration outright still counts
            // wherever it is written, including outside a buildscript block.
            "    configurations.classpath.resolutionStrategy.force '" + pin + "'\n",
        };
        for (int i = 0; i < pluginOnly.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation", pluginOnly[i])
                            .contains("kotlin-stdlib-jdk7:1.8.0"),
                    "<<" + pluginOnly[i].trim() + ">> is the plugin's graph");
        }

        // The app's own declarations are unaffected, before or after one.
        String after = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    buildscript { dependencies { classpath "
                + "'com.android.tools.build:gradle:8.1.0' } }\n"
                + "    dependencies { implementation '" + pin + "!!' }\n");
        check("".equals(after), "an app pin after a buildscript block still counts, "
                + "got <<" + after + ">>");
    }

    /**
     * An extra property is not block scoped, and
     * {@code buildscript { ext.kotlin_version = '..' }} is how a Kotlin Android
     * script is written. Discarded with the brace it sat in, the version every
     * dependency below interpolated read as unreadable -- which counts as below
     * the floor -- and the alignment never ran at all.
     */
    @Test
    public void anExtraPropertyOutlivesTheBlockItWasSetIn() {
        String[] definitions = {
            "    buildscript {\n        ext.kv = 'V'\n    }\n",
            "    buildscript { ext.kv = 'V' }\n",
            "    buildscript {\n        ext['kv'] = 'V'\n    }\n",
            "    someBlock {\n        ext.kv = 'V'\n    }\n",
            "    ext.kv = 'V'\n",
            "    ext { kv = 'V' }\n",
        };
        String use = "    dependencies {\n        implementation "
                + "\"org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kv\"\n    }\n";
        for (int i = 0; i < definitions.length; i++) {
            String merged = KotlinStdlibAlignment.constraintsBlock("implementation",
                    definitions[i].replace("'V'", "'1.9.22'") + use);
            check(merged.contains("kotlin-stdlib-jdk7:1.8.0")
                            && !merged.contains("kotlin-stdlib-jdk8:1.8.0"),
                    "<<" + definitions[i].trim() + ">> is readable below, got <<"
                            + merged + ">>");
            check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                            definitions[i].replace("'V'", "'1.7.22!!'") + use)),
                    "and a pre-merge one stands the block down");
        }

        // A local really is block scoped, and must not start outliving its block
        // just because an extra property does.
        String local = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    someBlock {\n        def kv = '1.9.22'\n    }\n" + use);
        check("".equals(local),
                "a local does not escape its block, got <<" + local + ">>");
    }

    /**
     * A type is a type however it is spelled. The walk that separates a
     * declaration from an assignment stopped at the first character that is not
     * part of an identifier, so a generic or array type ended the statement and
     * the name it declared -- and the pin that name held -- was never recorded.
     */
    @Test
    public void aTypeMayBeGenericOrAnArray() {
        String map = "[group: 'org.jetbrains.kotlin', "
                + "name: 'kotlin-stdlib-jdk8', version: '1.7.22']";
        String use = "    implementation(dep) { version { strictly '1.7.22' } }\n";
        String[] types = {
            "def", "String", "Map", "Map<String, String>", "HashMap<String,String>",
            "java.util.Map<java.lang.String, String>", "Map<String, ? extends Object>",
            "List<Map<String, String>>", "final Map<String, String>", "String[]",
            "Map<String, String[]>", "String[][]",
        };
        for (int i = 0; i < types.length; i++) {
            check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                            "    " + types[i] + " dep = " + map + "\n" + use)),
                    "<<" + types[i] + ">> declares dep");
        }

        // The angle bracket really has to be a type argument list. A comparison is
        // not one, and swallowing it would take the rest of the statement with it;
        // neither is a subscript with something in it, which is how an extra
        // property is named.
        check(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    if (someVersion < 5) { }\n"
                        + "    implementation('org.jetbrains.kotlin:"
                        + "kotlin-stdlib-jdk8:1.9.22')\n")
                        .contains("kotlin-stdlib-jdk7:1.8.0"),
                "a comparison is not a type argument list");
        check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                        "    ext['dep'] = 'org.jetbrains.kotlin:"
                        + "kotlin-stdlib-jdk8:1.7.22'\n" + use)),
                "and a subscript with a name in it still names a property");
    }

    /**
     * The extra properties extension is reachable through the project, and its
     * property may be subscripted rather than dotted. Both spellings set the
     * property the bare name goes on to read, and neither was recorded, so a
     * strict pre-merge pin held in one was emitted straight over.
     */
    @Test
    public void anExtraPropertyIsFoundThroughEverySpellingOfIt() {
        String pin = "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'";
        String use = "    implementation(dep) { version { strictly '1.7.22' } }\n";
        String[] definitions = {
            "    ext.dep = " + pin + "\n",
            "    project.ext.dep = " + pin + "\n",
            "    rootProject.ext.dep = " + pin + "\n",
            "    ext['dep'] = " + pin + "\n",
            "    ext[\"dep\"] = " + pin + "\n",
            "    project.ext['dep'] = " + pin + "\n",
        };
        for (int i = 0; i < definitions.length; i++) {
            check("".equals(KotlinStdlibAlignment.constraintsBlock("implementation",
                            definitions[i] + use)),
                    "the pin in <<" + definitions[i].trim() + ">> stands the block down");
        }

        // The owner still has to BE the extension: a property of anything else
        // does not bind the bare name, and reading one as though it did is how an
        // unreadable version becomes a confidently wrong one.
        String[] strangers = {
            "    somePlugin.dep = " + pin + "\n",
            "    extras.dep = " + pin + "\n",
            "    myext.dep = " + pin + "\n",
            "    notext['dep'] = " + pin + "\n",
        };
        for (int i = 0; i < strangers.length; i++) {
            check(KotlinStdlibAlignment.constraintsBlock("implementation",
                            strangers[i] + use).contains("kotlin-stdlib-jdk8:1.8.0"),
                    "<<" + strangers[i].trim() + ">> does not bind dep");
        }

        // And a merged-era coordinate held the same way is still read as a
        // declaration, so the artifact it names is left alone and its sibling is
        // the only one raised.
        String merged = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    project.ext['dep'] = "
                + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n"
                + "    implementation(dep)\n");
        check(merged.contains("kotlin-stdlib-jdk7:1.8.0")
                        && !merged.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the subscripted declaration is read, got <<" + merged + ">>");
    }

    /**
     * A stored map goes through the same expansion a stored string does, so a
     * version interpolated into it carries the version rather than the text of
     * the reference -- {@code "$v"} read as no version at all, which counts as
     * below the floor and stood the whole block down.
     */
    @Test
    public void aStoredMapExpandsWhatItInterpolates() {
        String modern = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def v = '1.9.22'\n"
                + "    def dep = [group: 'org.jetbrains.kotlin', "
                + "name: 'kotlin-stdlib-jdk7', version: \"$v\"]\n"
                + "    implementation(dep)\n");
        check(modern.contains("kotlin-stdlib-jdk8:1.8.0")
                        && !modern.contains("kotlin-stdlib-jdk7:1.8.0"),
                "the interpolated version is read, got <<" + modern + ">>");

        String old = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def v = '1.7.22!!'\n"
                + "    def dep = [group: 'org.jetbrains.kotlin', "
                + "name: 'kotlin-stdlib-jdk7', version: \"$v\"]\n"
                + "    implementation(dep)\n");
        check("".equals(old),
                "and a pre-merge one still suppresses, got <<" + old + ">>");
    }

    /**
     * A map factored into a variable is a declaration too. Recorded as
     * nothing, the statement using it named no artifact and the strict pin it
     * carried was invisible.
     */
    @Test
    public void aMapMayBeFactoredIntoAVariable() {
        String pinned = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def dep = [group: 'org.jetbrains.kotlin', "
                + "name: 'kotlin-stdlib-jdk8', version: '1.7.22']\n"
                + "    implementation(dep) { version { strictly '1.7.22' } }\n");
        check("".equals(pinned), "the map is carried to its usage, got <<" + pinned + ">>");

        String modern = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def dep = [group: 'org.jetbrains.kotlin', "
                + "name: 'kotlin-stdlib-jdk7', version: '1.9.22']\n"
                + "    implementation(dep)\n");
        check(modern.contains("kotlin-stdlib-jdk8:1.8.0")
                        && !modern.contains("kotlin-stdlib-jdk7:1.8.0"),
                "and read for what it declares, got <<" + modern + ">>");

        // A map of unrelated strings is still not a declaration.
        String catalog = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def catalog = [legacy: "
                + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!']\n"
                + "    implementation 'androidx.appcompat:appcompat:1.6.1'\n");
        check(catalog.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a catalog still decides nothing, got <<" + catalog + ">>");
    }

    /**
     * A rejection is read for exactly what it removes, and it decides
     * reachability on its own -- a requirement beside it cannot select what
     * the rejection has taken away.
     */
    @Test
    public void aRejectionIsReadForWhatItRemoves() {
        String[][] cases = {
            {"reject '[1.8.0,)'", ""},
            {"reject '[1.7.0,)'", ""},
            {"rejectAll()", ""},
            // require cannot select what reject removed
            {"require '1.+'; reject '[1.8.0,)'", ""},
            // an EXCLUSIVE lower bound leaves the floor itself selectable
            {"reject '(1.8.0,)'", "kotlin-stdlib-jdk7:1.8.0"},
            {"reject ']1.8.0,)'", "kotlin-stdlib-jdk7:1.8.0"},
            {"reject '[1.9.0,)'", "kotlin-stdlib-jdk7:1.8.0"},
            {"require '1.+'", "kotlin-stdlib-jdk7:1.8.0"},
        };
        for (int i = 0; i < cases.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8') "
                    + "{ version { " + cases[i][0] + " } }\n");
            if (cases[i][1].length() == 0) {
                check("".equals(out),
                        cases[i][0] + " leaves nothing at the floor, got <<" + out + ">>");
            } else {
                check(out.contains(cases[i][1]),
                        cases[i][0] + " leaves the floor selectable, got <<" + out + ">>");
            }
        }
    }

    /**
     * A strategy on {@code configurations.classpath} governs the plugin
     * classpath, which is not where these constraints go -- so standing the
     * block down for it would leave a real duplicate unfixed for a setting
     * that cannot conflict with anything written here.
     */
    @Test
    public void aBuildscriptStrategyIsNotTheAppsGraph() {
        String plugin = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    buildscript { configurations.classpath.resolutionStrategy"
                + ".failOnVersionConflict() }\n");
        check(plugin.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a classpath-only strategy leaves the alignment alone, got <<"
                        + plugin + ">>");

        // The app's own configurations still stand it down.
        String app = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    configurations.all { resolutionStrategy.failOnVersionConflict() }\n");
        check("".equals(app), "the app's graph still does, got <<" + app + ">>");
    }

    /**
     * A rejection only manages the version when it leaves the floor nothing to
     * select. Reading every rejection as management left the original
     * duplicate unfixed for an app that had rejected something else entirely.
     */
    @Test
    public void aRejectionCountsOnlyWhenItReachesTheFloor() {
        String[] closing = {"reject '[1.8.0,)'", "rejectAll()", "reject '(,1.8.0]'"};
        for (int i = 0; i < closing.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8') "
                    + "{ version { " + closing[i] + " } }\n");
            check("".equals(out),
                    closing[i] + " leaves nothing at the floor, got <<" + out + ">>");
        }

        // `(,1.8.0]` was once here, on the reasoning that it leaves 1.8.1 -- but
        // it INCLUDES the floor, and the floor is the only version a constraint
        // written at exactly 1.8.0 can resolve to.
        String[] leaving = {"reject '1.7.0'", "reject '[1.9.0,)'", "reject '(,1.8.0)'"};
        for (int i = 0; i < leaving.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8') "
                    + "{ version { " + leaving[i] + " } }\n");
            check(out.contains("kotlin-stdlib-jdk7:1.8.0"),
                    leaving[i] + " still leaves the floor selectable, got <<"
                            + out + ">>");
        }
    }

    /**
     * A declaration written inside quoted prose never executes. An
     * unrestricted search for {@code def} found one there and recorded it,
     * overwriting a real binding so a later use read as something else.
     */
    @Test
    public void aDeclarationInsideProseIsNotADeclaration() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def dep = 'com.example:other:1.0'\n"
                + "    println \"def dep = "
                + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\"\n"
                + "    implementation(dep)\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the quoted declaration is ignored, got <<" + out + ">>");

        // A real one directly after it still counts.
        String real = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    println \"nothing to see\"\n"
                + "    def dep = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!'\n"
                + "    implementation(dep)\n");
        check("".equals(real), "a real declaration still counts, got <<" + real + ">>");
    }

    /**
     * Groovy accepts spaces around a map key's colon, and looking only at the
     * character immediately after the token missed the key and substituted it
     * away -- losing the map form and the strict pin inside it.
     */
    @Test
    public void aMapKeyMayBeSpacedFromItsColon() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def group = 'org.jetbrains.kotlin'\n"
                + "    implementation(group : group, name : 'kotlin-stdlib-jdk8', "
                + "version : '1.7.22') { version { strictly '1.7.22' } }\n");
        check("".equals(out), "the spaced map form is read, got <<" + out + ">>");
    }

    /**
     * With failOnVersionConflict every disagreement is a build failure, and
     * raising a shim to the floor IS a disagreement -- so the block would turn
     * a graph that resolved coherently into one that does not resolve at all.
     * Nothing can be written here that would not conflict, so nothing is.
     */
    @Test
    public void nothingIsWrittenWhenConflictsAreFatal() {
        String fatal = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    configurations.all { resolutionStrategy.failOnVersionConflict() }\n");
        check("".equals(fatal), "the block stands down, got <<" + fatal + ">>");

        // The words in a reason are prose, here as everywhere else.
        String prose = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('a:b:1.0') "
                + "{ because 'we do not failOnVersionConflict here' }\n");
        check(prose.contains("kotlin-stdlib-jdk8:1.8.0"),
                "prose does not stand it down, got <<" + prose + ">>");
    }

    /**
     * An unbraced body belongs to the header above it. A resolution rule
     * written that way had the artifact named in the condition and the
     * override in the body, and splitting at the newline left neither
     * statement saying anything.
     */
    @Test
    public void anUnbracedBodyStaysWithItsCondition() {
        String rule = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    configurations.all { resolutionStrategy.eachDependency { d ->\n"
                + "        if (d.requested.group == 'org.jetbrains.kotlin' "
                + "&& d.requested.name == 'kotlin-stdlib')\n"
                + "            d.useVersion '1.7.22'\n"
                + "    } }\n");
        check("".equals(rule), "the rule is read across the newline, got <<" + rule + ">>");

        // Two ordinary declarations on consecutive lines are still two statements.
        String separate = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'androidx.appcompat:appcompat:1.6.1'\n"
                + "    implementation 'com.google.code.gson:gson:2.10.1'\n");
        check(separate.contains("kotlin-stdlib-jdk8:1.8.0"),
                "ordinary lines still separate, got <<" + separate + ">>");
    }

    /**
     * A rejection manages the version from the other side. Rejecting every
     * version our floor could resolve to leaves the graph nothing to select,
     * so writing the constraint anyway makes it unsatisfiable.
     */
    @Test
    public void aRejectionIsVersionManagement() {
        String[] rules = {"reject '[1.8.0,)'", "rejectAll()"};
        for (int i = 0; i < rules.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8') "
                    + "{ version { " + rules[i] + " } }\n");
            check("".equals(out),
                    rules[i] + " suppresses the block, got <<" + out + ">>");
        }
    }

    /**
     * A fragment is scanned inside the closure that surrounds it in the
     * generated file. Handed over bare, a local declared in the repositories
     * closure outlived it and shadowed a real binding for everything after --
     * which reads a later use as a declaration and skips that constraint.
     */
    @Test
    public void aFragmentKeepsItsGeneratedScope() throws Exception {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "ext.dep = 'com.example:other:1.0'\n",
                "repositories {\n"
                        + "def dep = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n}\n",
                "dependencies {\nimplementation(dep)\n}\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the repository-local name does not escape, got <<" + out + ">>");

        // The half above proves the alignment honours a scope it is GIVEN. This half
        // proves the builder gives it one: passing the fragments bare is what the
        // report was about, and a test that hands over pre-wrapped text would pass
        // with the builder unchanged -- which it did, until this was added.
        byte[] bytes = java.nio.file.Files.readAllBytes(new java.io.File(
                "src/main/java/com/codename1/builders/AndroidGradleBuilder.java").toPath());
        String builderSrc = new String(bytes, "UTF-8");
        int at = builderSrc.indexOf("KotlinStdlibAlignment.constraintsBlock(");
        check(at >= 0, "the builder calls the alignment");
        String fromCall = builderSrc.substring(at).replaceAll("//[^\n]*", "");
        String call = fromCall.substring(0, fromCall.indexOf(";"));
        int blockAt = builderSrc.indexOf("String gradleProps = ");
        check(blockAt >= 0, "the generated script is found");
        int blockEnd = builderSrc.indexOf("Gradle File start", blockAt);
        check(blockEnd > blockAt, "and its end");
        String block = builderSrc.substring(blockAt, blockEnd)
                .replaceAll("//[^\n]*", "");
        String[] scopes = {
            "repositories {", "buildscript {", "android {", "dependencies {",
        };
        for (int i = 0; i < scopes.length; i++) {
            check(call.indexOf(scopes[i]) >= 0,
                    "fragments are handed over inside their " + scopes[i]
                            + " scope, which the call does not show");
        }

        // And the scope has to be the RIGHT one, read off the script rather than
        // named here. A fragment interpolated at two places sits in two different
        // scopes -- injectRepo is inside buildscript { repositories { } } once and
        // a bare repositories { } the second time -- so the call must wrap the two
        // occurrences differently. Checking only that each was wrapped somehow let
        // the first be handed over as an app-graph repositories block, which is a
        // different question from the one Gradle asks there.
        java.util.List<java.util.List<String>> scriptScopes =
                new java.util.ArrayList<java.util.List<String>>();
        java.util.List<String> stack = new java.util.ArrayList<String>();
        boolean inLiteral = false;
        StringBuilder literal = new StringBuilder();
        for (int i = 0; i < block.length(); i++) {
            char c = block.charAt(i);
            if (inLiteral) {
                if (c == '\\') {
                    i++;
                    continue;
                }
                if (c == '"') {
                    inLiteral = false;
                    continue;
                }
                if (c == '{') {
                    String head = literal.toString().trim();
                    int space = head.lastIndexOf(' ');
                    stack.add(space < 0 ? head : head.substring(space + 1));
                    literal.setLength(0);
                } else if (c == '}') {
                    if (!stack.isEmpty()) {
                        stack.remove(stack.size() - 1);
                    }
                    literal.setLength(0);
                } else {
                    literal.append(c);
                }
                continue;
            }
            if (c == '"') {
                inLiteral = true;
                literal.setLength(0);
                continue;
            }
            if (block.startsWith("injectRepo", i)
                    && (i == 0 || !Character.isJavaIdentifierPart(block.charAt(i - 1)))
                    && !Character.isJavaIdentifierPart(
                            block.charAt(i + "injectRepo".length()))) {
                scriptScopes.add(new java.util.ArrayList<String>(stack));
            }
        }
        check(scriptScopes.size() == 2,
                "the script interpolates injectRepo twice, found " + scriptScopes);
        check(!scriptScopes.get(0).equals(scriptScopes.get(1)),
                "and in two different scopes, found " + scriptScopes);

        // Split at the commas that separate ARGUMENTS, which are the ones outside
        // parentheses: the nearest comma before the token is the one inside
        // .replace("%s", injectRepo), and slicing there left no wrapper to check.
        java.util.List<String> arguments = new java.util.ArrayList<String>();
        int depth = 0;
        int start = call.indexOf('(') + 1;
        boolean quoted = false;
        for (int i = start; i < call.length(); i++) {
            char c = call.charAt(i);
            if (quoted) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    quoted = false;
                }
                continue;
            }
            if (c == '"') {
                quoted = true;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (depth == 0) {
                    arguments.add(call.substring(start, i));
                    break;
                }
                depth--;
            } else if (c == ',' && depth == 0) {
                arguments.add(call.substring(start, i));
                start = i + 1;
            }
        }
        java.util.List<String> passing = new java.util.ArrayList<String>();
        for (int i = 0; i < arguments.size(); i++) {
            if (arguments.get(i).indexOf("injectRepo") >= 0) {
                passing.add(arguments.get(i));
            }
        }
        check(passing.size() == scriptScopes.size(),
                "the call passes injectRepo once per interpolation, found " + passing);

        for (int i = 0; i < scriptScopes.size(); i++) {
            String wrapper = passing.get(i);
            java.util.List<String> scope = scriptScopes.get(i);
            for (int j = 0; j < scope.size(); j++) {
                check(wrapper.indexOf(scope.get(j) + " {") >= 0,
                        "occurrence " + i + " of injectRepo is handed over inside "
                                + scope + ", and its wrapper <<" + wrapper.trim()
                                + ">> does not open " + scope.get(j));
            }
            for (int j = 0; j < scopes.length; j++) {
                String other = scopes[j].substring(0, scopes[j].indexOf(' '));
                check(scope.contains(other) || wrapper.indexOf(scopes[j]) < 0,
                        "occurrence " + i + " of injectRepo is not inside " + other
                                + " in the script, but its wrapper <<"
                                + wrapper.trim() + ">> opens one");
            }
        }
    }

    /**
     * A bracket holds a statement together exactly as a parenthesis does. A
     * force written across lines had its assignment in one statement and its
     * coordinate in another, so neither said anything and the force went
     * unread while the shims were raised around it.
     */
    @Test
    public void aBracketHoldsAStatementTogether() {
        String across = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    configurations.all {\n"
                + "        resolutionStrategy.forcedModules = [\n"
                + "                'org.jetbrains.kotlin:kotlin-stdlib:1.7.22!!'\n"
                + "        ]\n"
                + "    }\n");
        check("".equals(across),
                "the force spans lines, got <<" + across + ">>");

        String mapAcross = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation([\n"
                + "        group: 'org.jetbrains.kotlin',\n"
                + "        name: 'kotlin-stdlib-jdk8',\n"
                + "        version: '1.7.22!!'\n"
                + "    ])\n");
        check("".equals(mapAcross),
                "and so does a map written across them, got <<" + mapAcross + ">>");

        // Statements that are NOT inside brackets still separate, which is what
        // stops one declaration's configuration pairing with another's coordinate.
        String separate = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'androidx.appcompat:appcompat:1.6.1'\n"
                + "    implementation 'com.google.code.gson:gson:2.10.1'\n");
        check(separate.contains("kotlin-stdlib-jdk7:1.8.0")
                        && separate.contains("kotlin-stdlib-jdk8:1.8.0"),
                "ordinary declarations still split, got <<" + separate + ">>");
    }

    /**
     * Gradle's parenthesis-free map form puts two bare tokens in a row, which
     * is what a typed declaration looks like to a token counter. Read as one,
     * {@code implementation group: group, ...} "declared" a variable called
     * group with no initialiser and cleared the real binding of that name, so
     * the strict declaration using it later was never matched.
     */
    @Test
    public void aNamedArgumentIsNotADeclaration() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def group = 'org.jetbrains.kotlin'\n"
                + "    implementation group: group, name: 'other', version: '1.0'\n"
                + "    implementation(group: group, name: 'kotlin-stdlib', "
                + "version: '1.7.22') { version { strictly '1.7.22' } }\n");
        check("".equals(out),
                "the binding survives the map-form declaration, got <<" + out + ">>");
    }

    /**
     * A map entry's value is not handed to a dependency. A catalog of strings
     * carrying a strict-looking coordinate suppressed the block for something
     * never added to any configuration.
     */
    @Test
    public void aMapEntryValueIsNotADeclaration() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def catalog = [legacy: "
                + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!']\n"
                + "    implementation 'androidx.appcompat:appcompat:1.6.1'\n");
        check(out.contains("kotlin-stdlib-jdk7:1.8.0")
                        && out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a catalog entry decides nothing, got <<" + out + ">>");

        // But a coordinate in a forcedModules list decides everything, and it sits
        // right after a bracket -- which is why only the colon is read this way.
        String forced = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    configurations.all { resolutionStrategy.forcedModules = "
                + "['org.jetbrains.kotlin:kotlin-stdlib:1.7.22'] }\n");
        check("".equals(forced), "a forced module is still read, got <<" + forced + ">>");
    }

    /**
     * A closure opened and a local declared on the same line is still a
     * closure. Depth is tracked between statements, so that local looked like
     * it belonged to the script and outlived the closure it was written in.
     */
    @Test
    public void aSameLineClosureIsStillAScope() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    ext.dep = 'com.example:other:1.0'\n"
                + "    ext.helper = { def dep = "
                + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22' }\n"
                + "    implementation(dep)\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the closure's local does not escape it, got <<" + out + ">>");
    }

    /**
     * The backward scans know what whitespace is too. Three of them stopped at
     * a space or a tab, so a fragment with Windows line endings put a carriage
     * return where they were looking and the token behind it stopped being
     * found -- a `because` on the line above its argument, for one, which then
     * read as a declaration rather than as prose.
     */
    @Test
    public void aTokenIsStillFoundAcrossAnyLineEnding() {
        String[] endings = {"\r\n", "\n", "\r"};
        for (int i = 0; i < endings.length; i++) {
            String reason = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    implementation('com.example:other:1.0') { because" + endings[i]
                    + " 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!' }\n");
            check(reason.contains("kotlin-stdlib-jdk8:1.8.0"),
                    "the reason is still prose across " + endings[i].length()
                            + " line-ending chars, got <<" + reason + ">>");

            String added = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    dependencies.add(" + endings[i]
                    + " 'implementation'," + endings[i]
                    + " 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!')\n");
            check("".equals(added),
                    "and an add() call is still an add() call, got <<" + added + ">>");
        }
    }

    /**
     * {@code +=} assigns too: forcedModules += ['...'] applies the force just
     * as an ordinary assignment does.
     */
    @Test
    public void anAdditiveAssignmentStillAssigns() {
        String[] operators = {"=", "+="};
        for (int i = 0; i < operators.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    configurations.all { resolutionStrategy.forcedModules "
                    + operators[i] + " ['org.jetbrains.kotlin:kotlin-stdlib:1.7.22'] }\n");
            check("".equals(out),
                    "forcedModules " + operators[i] + " is a force, got <<" + out + ">>");
        }

        // A comparison is not an assignment, and neither reads as a force.
        String compared = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    if (resolutionStrategy.forcedModules == "
                + "['org.jetbrains.kotlin:kotlin-stdlib:1.7.22']) { }\n");
        check(compared.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a comparison is not a force, got <<" + compared + ">>");
    }

    /**
     * A declaration may be annotated. A script field is written
     * {@code @groovy.transform.Field String dep = '...'}, and the walk that
     * reads modifiers and a type stopped dead on the {@code @}.
     */
    @Test
    public void aDeclarationMayBeAnnotated() {
        String[] annotations = {
            "@groovy.transform.Field",
            "@Field",
            "@SuppressWarnings('unused')",
            "@groovy.transform.Field @SuppressWarnings('unused')",
        };
        for (int i = 0; i < annotations.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    " + annotations[i]
                    + " String dep = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'\n"
                    + "    implementation(dep) { version { strictly '1.7.22' } }\n");
            check("".equals(out),
                    "the field annotated " + annotations[i] + " is recorded, got <<"
                            + out + ">>");
        }
    }

    /**
     * A declaration may introduce several names at once, and the one that
     * matters is not always the first.
     */
    @Test
    public void everyDeclaratorIsRecorded() {
        String second = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def marker = 'x', dep = 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'\n"
                + "    implementation(dep) { version { strictly '1.7.22' } }\n");
        check("".equals(second), "the second declarator is recorded, got <<" + second + ">>");

        String third = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def a = 'x', b = 'y', "
                + "dep = 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'\n"
                + "    implementation(dep) { version { strictly '1.7.22' } }\n");
        check("".equals(third), "and the third, got <<" + third + ">>");

        // The first still is, and a declarator list does not invent bindings: a name
        // that was never declared stays unknown.
        String first = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def dep = 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22', marker = 'x'\n"
                + "    implementation(dep) { version { strictly '1.7.22' } }\n");
        check("".equals(first), "the first is unaffected, got <<" + first + ">>");

        String unknown = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def marker = 'x', other = 'y'\n"
                + "    implementation(dep)\n");
        check(unknown.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an undeclared name is still unknown, got <<" + unknown + ">>");
    }

    /**
     * Line endings do not change what a map entry says, here either. The call
     * detector had already learned that and this shared skip had not, so a
     * CRLF fragment that split an entry after its colon found no value at all.
     */
    @Test
    public void aMapEntryMayBeSplitByAnyLineEnding() {
        String[] endings = {"\r\n", "\n", "\r"};
        for (int i = 0; i < endings.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    implementation(group:" + endings[i]
                    + " 'org.jetbrains.kotlin', name:" + endings[i]
                    + " 'kotlin-stdlib-jdk8', version:" + endings[i]
                    + " '1.7.22!!')" + endings[i]);
            check("".equals(out),
                    "the map entry survives the line ending, got <<" + out + ">>");
        }
    }

    /**
     * A name a nested scope introduced goes away with it. A `def` inside a
     * closure or a method is local to it, and keeping that value afterwards
     * made an unrelated later use look like a declaration of whatever the
     * nested one held -- so that artifact's constraint was skipped as already
     * satisfied while its sibling was raised around it.
     */
    @Test
    public void aNameIntroducedInsideAScopeLeavesWithIt() {
        String nested = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def dep = 'com.example:other:1.0'\n"
                + "    def helper() {\n"
                + "        def dep = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n"
                + "    }\n"
                + "    implementation(dep)\n");
        check(nested.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the nested local does not reach the statement after it, got <<"
                        + nested + ">>");

        // An ASSIGNMENT inside a block is a different thing: it updates the binding
        // it found, so it does reach what follows. This is the distinction the fix
        // rests on, and it is asserted so a later "just clear the scope" cannot
        // quietly take it away.
        String assigned = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def dep = 'com.example:other:1.0'\n"
                + "    if (legacy) {\n"
                + "        dep = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n"
                + "    }\n"
                + "    implementation(dep)\n");
        check(!assigned.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an assignment inside a block still reaches what follows, got <<"
                        + assigned + ">>");
    }

    /**
     * A value that is only assigned is not a declaration. A definition naming
     * the artifact and carrying a strict marker suppressed the whole block on
     * that basis alone, for a value never added to any configuration -- and a
     * definition becomes a declaration when it is USED, by which point the
     * name has been inlined and the usage is what gets read.
     */
    @Test
    public void anAssignedValueIsNotADeclaration() {
        String unused = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def legacy = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!'\n"
                + "    implementation 'androidx.appcompat:appcompat:1.6.1'\n");
        check(unused.contains("kotlin-stdlib-jdk7:1.8.0")
                        && unused.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an unused definition decides nothing, got <<" + unused + ">>");

        // Used, it decides everything.
        String used = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def legacy = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!'\n"
                + "    implementation legacy\n");
        check("".equals(used), "the same value, used, suppresses; got <<" + used + ">>");
    }

    /**
     * A substitution overrides only what it substitutes AWAY from. With the
     * artifact as the target the replacement still goes through ordinary
     * conflict resolution, so an existing requirement raises it and nothing is
     * pinned -- reading that as absolute suppressed the block for a graph that
     * had not been pinned at all.
     */
    @Test
    public void aSubstitutionOverridesOnlyItsSource() {
        String source = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    configurations.all { resolutionStrategy.dependencySubstitution { "
                + "substitute module('org.jetbrains.kotlin:kotlin-stdlib') "
                + "using module('org.jetbrains.kotlin:kotlin-stdlib:1.7.22') } }\n");
        check("".equals(source),
                "substituting the stdlib itself is an override, got <<" + source + ">>");

        String target = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    configurations.all { resolutionStrategy.dependencySubstitution { "
                + "substitute module('com.example:source') "
                + "using module('org.jetbrains.kotlin:kotlin-stdlib:1.7.22') } }\n");
        check(target.contains("kotlin-stdlib-jdk7:1.8.0")
                        && target.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the stdlib merely as a target is not, got <<" + target + ">>");
    }

    /**
     * A rule reaches the app's configurations from inside the android block
     * too, so a fragment interpolated there decides what resolves just as much
     * as a declaration does. This is the shape the alignment could not see when
     * only the dependencies block was scanned.
     */
    @Test
    public void aRuleInsideTheAndroidBlockIsStillARule() {
        String force = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    project.configurations.all { resolutionStrategy.force "
                + "'org.jetbrains.kotlin:kotlin-stdlib:1.7.22' }\n");
        check("".equals(force),
                "a project-qualified force suppresses, got <<" + force + ">>");

        String substitution = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    configurations.all { resolutionStrategy.dependencySubstitution { "
                + "substitute module('org.jetbrains.kotlin:kotlin-stdlib') "
                + "using module('org.jetbrains.kotlin:kotlin-stdlib:1.7.22') } }\n");
        check("".equals(substitution),
                "a substitution onto a pre-merge version suppresses, got <<"
                        + substitution + ">>");

        // The replacement is what decides, not the module being replaced: this one
        // raises the library and takes nothing away.
        String raising = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    configurations.all { resolutionStrategy.dependencySubstitution { "
                + "substitute module('org.jetbrains.kotlin:kotlin-stdlib:1.7.22') "
                + "using module('org.jetbrains.kotlin:kotlin-stdlib:1.9.22') } }\n");
        check(raising.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a substitution raising the library keeps the alignment, got <<"
                        + raising + ">>");
    }

    /**
     * {@code useTarget} replaces the whole coordinate rather than the version,
     * and overrides just as absolutely as {@code useVersion} does.
     */
    @Test
    public void aRuleThatRetargetsIsStillARule() {
        String old = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    configurations.all { resolutionStrategy.eachDependency { d -> "
                + "d.useTarget 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22' } }\n");
        check("".equals(old),
                "retargeting the base library pre-merge suppresses, got <<" + old + ">>");

        String modern = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    configurations.all { resolutionStrategy.eachDependency { d -> "
                + "d.useTarget 'org.jetbrains.kotlin:kotlin-stdlib:1.9.22' } }\n");
        check(modern.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a merged-era retarget keeps the alignment, got <<" + modern + ">>");
    }

    /**
     * A resolution rule's {@code useVersion} rewrites what was requested,
     * silently, on the way through -- so it holds the library as firmly as a
     * force does. Such a rule names its artifact by comparing the parts, which
     * is neither a coordinate nor a map entry, and was read as naming nothing.
     */
    @Test
    public void aResolutionRuleHoldsWhatItRewrites() {
        String rule = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    configurations.all { resolutionStrategy.eachDependency { d -> "
                + "if (d.requested.group == 'org.jetbrains.kotlin' && "
                + "d.requested.name == 'kotlin-stdlib') d.useVersion '1.7.22' } }\n");
        check("".equals(rule),
                "a rule pinning the base library pre-merge suppresses, got <<"
                        + rule + ">>");

        // Rewriting it to a merged-era version takes nothing away.
        String modern = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    configurations.all { resolutionStrategy.eachDependency { d -> "
                + "if (d.requested.group == 'org.jetbrains.kotlin' && "
                + "d.requested.name == 'kotlin-stdlib') d.useVersion '1.9.22' } }\n");
        check(modern.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a merged-era rule keeps the alignment, got <<" + modern + ">>");
    }

    /**
     * A variable declared without a value is still a name this knows, and
     * recording it is what lets a later assignment be recognised as one.
     */
    @Test
    public void aDeclarationWithoutAValueIsStillADeclaration() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def dep\n"
                + "    if (legacy) {\n"
                + "        dep = 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'\n"
                + "    }\n"
                + "    implementation(dep) { version { strictly '1.7.22' } }\n");
        check("".equals(out),
                "the conditional assignment is seen, got <<" + out + ">>");
    }

    /**
     * A qualified type is one token. Stopping at its first dot read
     * {@code java} as the type and {@code lang} as the name, so the variable
     * was never recorded -- while {@code ext.kotlinVersion}, which is a dotted
     * TARGET rather than a dotted type, still has to be read the other way.
     */
    @Test
    public void aQualifiedTypeIsOneToken() {
        String[] types = {"java.lang.String", "String", "final java.lang.String"};
        for (int i = 0; i < types.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    " + types[i]
                    + " dep = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'\n"
                    + "    implementation(dep) { version { strictly '1.7.22' } }\n");
            check("".equals(out),
                    "a local of type " + types[i] + " is recorded, got <<" + out + ">>");
        }

        // The dotted extra property is still a property, not a type.
        String ext = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    ext.kotlinVersion = '1.9.22'\n"
                + "    implementation \"org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion\"\n");
        check(ext.contains("kotlin-stdlib-jdk8:1.8.0")
                        && !ext.contains("kotlin-stdlib-jdk7:1.8.0"),
                "ext.kotlinVersion still binds its name, got <<" + ext + ">>");
    }

    /**
     * An assignment inside ANY open brace is one whose execution this cannot
     * establish, closures included: {@code def mutate = { dep = '...' }} runs
     * only if something calls it. Named control structures were listed here
     * once and the list was already missing this.
     */
    @Test
    public void anAssignmentInsideAClosureDoesNotHideAPin() {
        String multiLine = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def dep = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'\n"
                + "    def mutate = {\n"
                + "        dep = 'com.example:other:1.0'\n"
                + "    }\n"
                + "    implementation(dep) { version { strictly '1.7.22' } }\n");
        check("".equals(multiLine),
                "the pin survives an uninvoked closure, got <<" + multiLine + ">>");

        // A first definition is still recorded at any depth, which is what keeps a
        // `def` inside dependencies { } working.
        String nested = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    dependencies {\n"
                + "        def dep = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'\n"
                + "        implementation(dep) { version { strictly '1.7.22' } }\n"
                + "    }\n");
        check("".equals(nested),
                "a definition inside a block is still read, got <<" + nested + ">>");
    }

    /**
     * A release candidate of the floor is below the floor, wherever it appears.
     * As a range's inclusive ceiling it was compared numerically and read as
     * reaching the floor, so the constraints went in with nothing to resolve
     * to.
     */
    @Test
    public void aPrereleaseCeilingDoesNotReachTheFloor() {
        String rc = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib') "
                + "{ version { strictly '[1.7.0,1.8.0-RC2]' } }\n");
        check("".equals(rc),
                "a prerelease ceiling cannot reach the floor, got <<" + rc + ">>");

        // The release itself can, and does.
        String release = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:[1.7.0,1.8.0]'\n");
        check(release.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an inclusive release ceiling does, got <<" + release + ">>");
    }

    /**
     * A coordinate concatenated onto a partial literal has no version here,
     * and unreadable is the honest answer -- reading the empty string as a
     * version put it below the floor and suppressed the block for a
     * declaration that may well be merged-era.
     */
    @Test
    public void aConcatenatedVersionIsNotAnEmptyOne() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation(\"org.jetbrains.kotlin:kotlin-stdlib-jdk7:\" "
                + "+ kotlinVersion)\n");
        check(out.contains("kotlin-stdlib-jdk7:1.8.0")
                        && out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "both constraints are written, got <<" + out + ">>");
    }

    /**
     * Gradle's status selectors have no ceiling, so they can select a
     * merged-era shim. Compared as literals they parsed as zero, which is the
     * oldest version there is.
     */
    @Test
    public void aStatusSelectorCanReachTheFloor() {
        String[] selectors = {"latest.release", "latest.integration", "+"};
        for (int i = 0; i < selectors.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:"
                    + selectors[i] + "'\n");
            check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                    selectors[i] + " keeps the sibling aligned, got <<" + out + ">>");
        }
    }

    /**
     * A name assigned inside a conditional may hold either value, because
     * whether the branch runs is decided at evaluation time. The ambiguity is
     * resolved toward suppression: emitting beside a pin this could not see is
     * the failure that reaches the device.
     *
     * <p>The single-line spelling was never affected -- braces do not split
     * statements, so {@code if (c) { dep = '...' }} arrives as one statement
     * that assigns nothing -- and it is asserted here so the difference is not
     * mistaken for a gap later.</p>
     */
    @Test
    public void aConditionalReassignmentDoesNotHideAPin() {
        String multiLine = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def dep = 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'\n"
                + "    if (project.hasProperty('other')) {\n"
                + "        dep = 'com.example:other:1.0'\n"
                + "    }\n"
                + "    implementation(dep) { version { strictly '1.7.22' } }\n");
        check("".equals(multiLine),
                "the pin survives a conditional reassignment, got <<" + multiLine + ">>");

        String oneLine = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def dep = 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'; "
                + "if (project.hasProperty('other')) { dep = 'com.example:other:1.0' }; "
                + "implementation(dep) { version { strictly '1.7.22' } }\n");
        check("".equals(oneLine),
                "and the one-line form, which never assigned at all, got <<"
                        + oneLine + ">>");

        // The other direction was already safe and stays that way: a conditional
        // assignment TO a Kotlin coordinate is taken, because taking it suppresses.
        String gained = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def dep = 'com.example:other:1.0'\n"
                + "    if (project.hasProperty('old')) {\n"
                + "        dep = 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'\n"
                + "    }\n"
                + "    implementation(dep) { version { strictly '1.7.22' } }\n");
        check("".equals(gained),
                "a conditional assignment to a coordinate is seen, got <<" + gained + ">>");

        // Unconditionally, a reassignment still replaces what it replaces.
        String plain = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def dep = 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'\n"
                + "    dep = 'com.example:other:1.0'\n"
                + "    implementation(dep)\n");
        check(plain.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an unconditional reassignment still applies, got <<" + plain + ">>");
    }

    /**
     * A local may be named after the DSL key it supplies. Substituting every
     * occurrence turned {@code group:} into a quoted string and lost the map
     * form entirely, taking the strict pin inside it with it.
     */
    @Test
    public void aLocalNamedAfterAMapKeyDoesNotReplaceTheKey() {
        String[] keys = {"group", "name", "version"};
        for (int k = 0; k < keys.length; k++) {
            String value = "version".equals(keys[k]) ? "1.7.22!!"
                    : "name".equals(keys[k]) ? "kotlin-stdlib-jdk8"
                    : "org.jetbrains.kotlin";
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    def " + keys[k] + " = '" + value + "'\n"
                    + "    implementation(group: "
                    + ("group".equals(keys[k]) ? "group" : "'org.jetbrains.kotlin'")
                    + ", name: "
                    + ("name".equals(keys[k]) ? "name" : "'kotlin-stdlib-jdk8'")
                    + ", version: "
                    + ("version".equals(keys[k]) ? "version" : "'1.7.22!!'")
                    + ")\n");
            check("".equals(out),
                    "the map form survives a local called " + keys[k]
                            + ", got <<" + out + ">>");
        }
    }

    /**
     * A rich version overrides the coordinate's own. Reporting the coordinate
     * read a pre-merge pin as merged-era, so its own constraint was skipped as
     * already satisfied while the sibling and the base were raised around it.
     */
    @Test
    public void aRichVersionOverridesTheCoordinate() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.22') "
                + "{ version { strictly '1.7.22' } }\n");
        check("".equals(out),
                "the strict 1.7.22 is what decides, got <<" + out + ">>");

        // And the other way round: a merged-era rich version over an old coordinate.
        String modern = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.22') "
                + "{ version { require '1.9.22' } }\n");
        check(modern.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a required 1.9.22 is read past the old coordinate, got <<" + modern + ">>");
    }

    /**
     * Setting a property is not calling a method. {@code { force = false }}
     * explicitly turns forcing OFF, and reading the word as a force turned an
     * ordinary version request into an absolute pin -- suppressing the block
     * for a declaration asking for nothing of the kind.
     */
    @Test
    public void aForceThatIsSwitchedOffIsNotAForce() {
        String off = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib:1.7.22') "
                + "{ force = false }\n");
        check(off.contains("kotlin-stdlib-jdk7:1.8.0")
                        && off.contains("kotlin-stdlib-jdk8:1.8.0"),
                "force = false does not suppress, got <<" + off + ">>");

        String on = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib:1.7.22') "
                + "{ force = true }\n");
        check("".equals(on), "force = true does, got <<" + on + ">>");
    }

    /**
     * Every literal form Groovy has interpolates except the single-quoted
     * ones, so a coordinate assembled inside any of the others carries its
     * definitions with it.
     */
    @Test
    public void everyInterpolatingLiteralExpandsItsDefinitions() {
        String[] assembled = {
            "\"org.jetbrains.kotlin:kotlin-stdlib-jdk7:$v\"",
            "\"\"\"org.jetbrains.kotlin:kotlin-stdlib-jdk7:$v\"\"\"",
            "$/org.jetbrains.kotlin:kotlin-stdlib-jdk7:$v/$",
        };
        for (int i = 0; i < assembled.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    def v = '1.9.22'\n"
                    + "    def dep = " + assembled[i] + "\n"
                    + "    implementation dep\n");
            check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                    "the sibling is aligned for <<" + assembled[i] + ">>, got <<" + out + ">>");
            check(!out.contains("kotlin-stdlib-jdk7:1.8.0"),
                    "and the declaration is read as merged-era, got <<" + out + ">>");
        }

        // A single-quoted literal does not interpolate, so $v is not a version and
        // the conservative answer stands.
        String literal = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def v = '1.9.22'\n"
                + "    def dep = 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:$v'\n"
                + "    implementation dep\n");
        check("".equals(literal),
                "an uninterpolated $v is not a version, got <<" + literal + ">>");
    }

    /**
     * A reason is prose to the version scan as well. It reached the reason's
     * coordinate before the map's own version entry, so the comment describing
     * an old artifact supplied the version for the declaration warning about
     * it -- and took the whole block down.
     */
    @Test
    public void aReasonDoesNotSupplyTheVersion() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation(group: 'org.jetbrains.kotlin', "
                + "name: 'kotlin-stdlib-jdk7', version: '1.9.22') "
                + "{ because 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.22' }\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the sibling is still aligned, got <<" + out + ">>");
        check(!out.contains("kotlin-stdlib-jdk7:1.8.0"),
                "and the declared 1.9.22 is what was read, got <<" + out + ">>");
    }

    /**
     * Every spelling Gradle has for a force is a force. {@code force} is the
     * method, {@code setForcedModules} its setter, {@code forcedModules} the
     * property, and all three hold a module absolutely -- so all three leave
     * these constraints raising the shims to empty jars beside a base library
     * that stayed pre-merge.
     */
    @Test
    public void everySpellingOfAForceIsAForce() {
        String[] spellings = {
            "    configurations.all { resolutionStrategy.force "
                    + "'org.jetbrains.kotlin:kotlin-stdlib:1.7.22' }\n",
            "    configurations.all { resolutionStrategy.setForcedModules("
                    + "'org.jetbrains.kotlin:kotlin-stdlib:1.7.22') }\n",
            "    configurations.all { resolutionStrategy.forcedModules = "
                    + "['org.jetbrains.kotlin:kotlin-stdlib:1.7.22'] }\n",
            "    configurations.all { resolutionStrategy.forcedModules="
                    + "['org.jetbrains.kotlin:kotlin-stdlib:1.7.22'] }\n",
        };
        for (int i = 0; i < spellings.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    spellings[i]);
            check("".equals(out),
                    "the force is read from <<" + spellings[i] + ">>, got <<" + out + ">>");
        }
    }

    /**
     * A definition may interpolate an earlier one. Recorded as written, the
     * version stayed the text {@code $v} -- no version, so below the floor,
     * so the whole block suppressed for a project that was already
     * merged-era and still had a duplicate to fix.
     */
    @Test
    public void aDefinitionMayInterpolateAnEarlierOne() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def v = '1.9.22'\n"
                + "    def dep = \"org.jetbrains.kotlin:kotlin-stdlib-jdk7:$v\"\n"
                + "    implementation dep\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the sibling is still aligned, got <<" + out + ">>");
        check(!out.contains("kotlin-stdlib-jdk7:1.8.0"),
                "and the merged-era declaration is left alone, got <<" + out + ">>");

        // The same chain below the floor is still below it.
        String old = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def v = '1.7.22!!'\n"
                + "    def dep = \"org.jetbrains.kotlin:kotlin-stdlib-jdk7:$v\"\n"
                + "    implementation dep\n");
        check("".equals(old),
                "a pre-merge chain still suppresses, got <<" + old + ">>");
    }

    /**
     * Line endings are not this class's business to have an opinion about. A
     * fragment written on Windows put a carriage return after
     * {@code strictly}, and the token-end test accepted only a space, a tab or
     * an open parenthesis -- so the call stopped being a call and the pin
     * behind it was never read.
     */
    @Test
    public void aCarriageReturnSeparatesTokensLikeAnyOtherBlank() {
        String[] endings = {"\r\n", "\n", "\r"};
        for (int i = 0; i < endings.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    implementation('org.jetbrains.kotlin:kotlin-stdlib')" + endings[i]
                    + "    { version { strictly" + endings[i] + "        '1.7.22' } }"
                    + endings[i]);
            check("".equals(out),
                    "the strict call survives the line ending, got <<" + out + ">>");
        }
    }

    /**
     * A trailing closure still belongs to its call with a blank line between
     * them. Comment stripping leaves an empty statement where a comment-only
     * line was, and looking at only the very next statement left the closure
     * -- and the strict version inside it -- attached to nothing.
     */
    @Test
    public void aTrailingClosureSurvivesABlankLine() {
        String[] between = {
            "\n",
            "\n    // why this pin is here\n",
            "\n\n    /* and a block one */\n\n",
        };
        for (int i = 0; i < between.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    implementation('org.jetbrains.kotlin:kotlin-stdlib:1.7.22')"
                    + between[i]
                    + "    { version { strictly '1.7.22' } }\n");
            check("".equals(out),
                    "the closure is still the call's, across <<"
                            + between[i].replace("\n", "\\n") + ">>, got <<" + out + ">>");
        }
    }

    /**
     * Inside a dollar-slashy literal the dollar escapes itself and a slash, so
     * {@code $/} is a slash and not the closer. Taking the first {@code /$}
     * substring ended the literal early and put the scanner back into code
     * halfway through a string.
     */
    @Test
    public void aDollarEscapeDoesNotCloseADollarSlashyLiteral() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def marker = $/not closed $/$ can't/$; "
                + "implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ version { strictly '1.7.22' } }\n");
        check("".equals(out),
                "the escaped delimiter did not end the literal, got <<" + out + ">>");

        // And $$ is a dollar, not the start of one.
        String dollars = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def marker = $/cost $$5 can't/$; "
                + "implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ version { strictly '1.7.22' } }\n");
        check("".equals(dollars),
                "an escaped dollar is content, got <<" + dollars + ">>");
    }

    /**
     * Removing a comment leaves the whitespace it was. A comment separates
     * tokens in the language, so deleting it outright joined them:
     * {@code strictly/* pin *}{@code /'1.7.22'} became strictly'1.7.22', which
     * is not a call to strictly, and the strict pin behind it was never seen.
     */
    @Test
    public void removingACommentLeavesTheWhitespaceItWas() {
        String[] joined = {
            "    implementation('org.jetbrains.kotlin:kotlin-stdlib') "
                    + "{ version { strictly/* pin */'1.7.22' } }\n",
            "    def/* local */dep = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'; "
                    + "implementation(dep) { version { strictly '1.7.22' } }\n",
            "    implementation/* which */('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!')\n",
        };
        for (int i = 0; i < joined.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation", joined[i]);
            check("".equals(out),
                    "the comment did not join the tokens around it in <<" + joined[i]
                            + ">>, got <<" + out + ">>");
        }
    }

    /**
     * A slash after anything that is not a value opens a literal.
     *
     * <p>Swept over the operators rather than asserted one at a time, because
     * one at a time is how the rule was built and it took four review rounds
     * to still be incomplete: the closure arrow, then the comparison, then
     * Groovy's {@code =~} and {@code ==~}. The code no longer enumerates this
     * half at all -- it enumerates the closed one, what a value can end with --
     * so this test is where the open half is written down.</p>
     */
    @Test
    public void aSlashAfterAnythingThatIsNotAValueOpensALiteral() {
        String[] operators = {
            "=", "=~", "==~", "~", "->", ",", "(", "[", ":", "&&", "||",
            "!", "?", "+", "-", "*", "%", "^", "|", "&", "<", ">", "<=", ">=",
            "==", "!=", "<<", "?:",
        };
        for (int i = 0; i < operators.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    def marker = [ name " + operators[i] + " /can't/ ]; "
                    + "implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                    + "{ version { strictly '1.7.22' } }\n");
            check("".equals(out),
                    "a slash after " + operators[i]
                            + " opens a literal, so the pin behind it is still seen; got <<"
                            + out + ">>");
        }
    }

    /**
     * And a slash after a value divides it. This is the half the code
     * enumerates, so it is the half that must stay closed: adding to it is how
     * a division starts swallowing the statements after it.
     */
    @Test
    public void aSlashAfterAValueDividesIt() {
        String[] values = {
            "total", "2", "count()", "sizes[0]", "(a + b)", "1.5", "n++", "n--",
        };
        for (int i = 0; i < values.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    def ratio = " + values[i] + " / divisor; "
                    + "implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                    + "{ version { strictly '1.7.22' } }\n");
            check("".equals(out),
                    "dividing " + values[i]
                            + " does not swallow the pin after it; got <<" + out + ">>");
        }
    }

    /**
     * A forced version suppresses the block, like a strict one.
     *
     * <p>A force does not conflict with a constraint, it wins over it without
     * a word: force the base library to 1.7.22 and these constraints still
     * raise the shims to their EMPTY 1.8.0 jars, so the jdk7/jdk8 classes are
     * in no selected jar at all. The build is green and the app throws on the
     * device, which is the one outcome worth all of this machinery.</p>
     */
    @Test
    public void aForcedVersionIsHeldAsFirmlyAsAStrictOne() {
        String[] forced = {
            "    configurations.all { resolutionStrategy.force "
                    + "'org.jetbrains.kotlin:kotlin-stdlib:1.7.22' }\n",
            "    configurations.all { resolutionStrategy { force "
                    + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22' } }\n",
            "    configurations.all {\n        resolutionStrategy {\n"
                    + "            force 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'\n"
                    + "        }\n    }\n",
        };
        for (int i = 0; i < forced.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation", forced[i]);
            check("".equals(out),
                    "a forced pre-merge version suppresses the block, from <<"
                            + forced[i] + ">> got <<" + out + ">>");
        }

        // A force at or above the floor takes nothing away: it is already a shim.
        String modern = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    configurations.all { resolutionStrategy.force "
                + "'org.jetbrains.kotlin:kotlin-stdlib:1.9.22' }\n");
        check(modern.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a merged-era force still gets the alignment, got <<" + modern + ">>");

        // And the word in a reason is prose, as everywhere else.
        String prose = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('com.example:other:1.0') "
                + "{ because 'we force nothing here' }\n");
        check(prose.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the word force in prose is not a force, got <<" + prose + ">>");
    }

    /**
     * A slashy literal may open after a closure arrow, and may run past the
     * end of its line -- but only when it actually closes. An opener misread
     * with no closing slash anywhere would swallow every statement after it,
     * and a suppression reached that way says nothing about the app.
     */
    @Test
    public void aSlashyLiteralSpansLinesOnlyWhenItCloses() {
        String afterArrow = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def marker = { -> /can't/ }; "
                + "implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ version { strictly '1.7.22' } }\n");
        check("".equals(afterArrow),
                "a slashy literal after a closure arrow, got <<" + afterArrow + ">>");

        String multiline = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def marker = /first\n        still can't/\n"
                + "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ version { strictly '1.7.22' } }\n");
        check("".equals(multiline),
                "a literal that spans lines keeps its content, got <<" + multiline + ">>");

        // Unterminated: whatever that slash was, it does not reach the next line.
        String unterminated = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def ratio = a / b\n"
                + "    implementation 'androidx.appcompat:appcompat:1.6.1'\n");
        check(unterminated.contains("kotlin-stdlib-jdk7:1.8.0")
                        && unterminated.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an unclosed slash stops at its line, got <<" + unterminated + ">>");
    }

    /**
     * A coordinate may carry a classifier and an {@code @extension} after its
     * version, and neither is part of the version. Returning them made
     * {@code 1.7.22!!@jar} not end in the strict marker, so a strict pre-merge
     * pin read as an ordinary one and the constraint went in beside it.
     */
    @Test
    public void aModifierAfterTheVersionIsNotPartOfIt() {
        String[] pinned = {
            "org.jetbrains.kotlin:kotlin-stdlib:1.7.22!!@jar",
            "org.jetbrains.kotlin:kotlin-stdlib:1.7.22!!:sources",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!@aar",
        };
        for (int i = 0; i < pinned.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    implementation '" + pinned[i] + "'\n");
            check("".equals(out),
                    "the strict marker is still read in <<" + pinned[i]
                            + ">>, got <<" + out + ">>");
        }

        // And a merged-era one with the same modifiers is still merged-era.
        String modern = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22@jar'\n");
        check(!modern.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a modern version is read past its modifier too, got <<" + modern + ">>");
    }

    /**
     * A slashy literal may follow a keyword. Division needs a value on its
     * left and a keyword is not one, so `return /can't/` opens a literal for
     * the same reason `= /can't/` does -- and read as a quote instead, the
     * apostrophe swallows whatever declaration follows it.
     */
    @Test
    public void aSlashyLiteralMayFollowAKeyword() {
        String[] keywords = {"return", "in", "new"};
        for (int k = 0; k < keywords.length; k++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    def note = { " + keywords[k] + " /can't/ }; "
                    + "implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                    + "{ version { strictly '1.7.22' } }\n");
            check("".equals(out),
                    "a slashy literal after " + keywords[k]
                            + " does not hide the pin, got <<" + out + ">>");
        }

        // A word that is not a keyword is a variable, and dividing it is division.
        String division = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def ratio = total / count; "
                + "implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ version { strictly '1.7.22' } }\n");
        check("".equals(division),
                "and dividing a variable is still division, got <<" + division + ">>");
    }

    /**
     * A coordinate keeps its meaning in every literal form Groovy has, the
     * slashy ones included. Recognising a form in the scanners but not in the
     * matchers left the pin visible to neither.
     */
    @Test
    public void aSlashyCoordinateIsStillACoordinate() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation($/org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22/$) "
                + "{ version { strictly '1.7.22' } }\n");
        check("".equals(out), "a dollar-slashy coordinate is read, got <<" + out + ">>");

        String plain = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def marker = /can't/; "
                + "implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ version { strictly '1.7.22' } }\n");
        check("".equals(plain),
                "an apostrophe inside a slashy literal is not a quote, got <<" + plain + ">>");
    }

    /**
     * Division is not a literal. The slashy form is only recognised where an
     * expression may begin, because reading `total / 2` as an opener would
     * swallow everything up to the next slash -- which is the same failure,
     * from the opposite direction, as not recognising the literal at all.
     */
    @Test
    public void divisionIsNotASlashyLiteral() {
        String[] arithmetic = {
            "def half = total / 2",
            "def part = (a + b) / 2",
            "def ratio = sizes[0] / sizes[1]",
        };
        for (int i = 0; i < arithmetic.length; i++) {
            // On ONE line with the pin, because a slashy literal stops at the end of
            // its line: put the pin on the next one and a swallowed division costs
            // nothing, which is a test that passes with the guard removed.
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    " + arithmetic[i]
                    + "; implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                    + "{ version { strictly '1.7.22' } }\n");
            check("".equals(out),
                    "division does not swallow what follows <<" + arithmetic[i]
                            + ">>, got <<" + out + ">>");
        }
    }

    /**
     * Extra properties are written as a closure at least as often as with a
     * dot, and inside one a bare assignment really does bind the name the
     * interpolation reads.
     */
    @Test
    public void anExtraPropertiesClosureDefinesItsNames() {
        String[] spellings = {
            "    ext { kotlinVersion = '1.9.22' }\n",
            "    ext {\n        kotlinVersion = '1.9.22'\n    }\n",
            "    ext {\n        kotlinVersion = '1.9.22'\n        somethingElse = 'x'\n    }\n",
        };
        for (int i = 0; i < spellings.length; i++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    spellings[i]
                    + "    implementation \"org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion\"\n");
            check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                    "the sibling is aligned for <<" + spellings[i] + ">>, got <<" + out + ">>");
            check(!out.contains("kotlin-stdlib-jdk7:1.8.0"),
                    "and the merged-era declaration is left alone, got <<" + out + ">>");
        }

        // Outside such a block a bare assignment binds nothing this can follow.
        String elsewhere = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    android { kotlinVersion = '1.9.22' }\n"
                + "    implementation \"org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion\"\n");
        check("".equals(elsewhere),
                "an assignment outside ext stays unreadable, got <<" + elsewhere + ">>");
    }

    /**
     * Gradle's extra properties are how a project-wide Kotlin version is
     * nearly always written, and the bare name the interpolation reads really
     * is bound by them. Stopping at {@code ext} left the version unreadable,
     * which counts as below the floor -- so a project already on a merged-era
     * Kotlin had the whole block suppressed and kept whatever pre-merge shim a
     * transitive dependency dragged in.
     */
    @Test
    public void anExtraPropertyDefinesTheVersionItInterpolates() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    ext.kotlinVersion = '1.9.22'\n"
                + "    implementation \"org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion\"\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the sibling is still aligned, got <<" + out + ">>");
        check(!out.contains("kotlin-stdlib-jdk7:1.8.0"),
                "and the merged-era declaration is left alone, got <<" + out + ">>");

        // Only that one prefix: any dotted assignment would let an unrelated
        // property supply a version it does not bind, which turns an unreadable
        // version into a confidently wrong one.
        String unrelated = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    somePlugin.kotlinVersion = '1.9.22'\n"
                + "    implementation \"org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion\"\n");
        check("".equals(unrelated),
                "an unrelated dotted assignment stays unreadable, got <<" + unrelated + ">>");
    }

    /**
     * A reason is prose however it is quoted. Read as a declaration, the
     * comment describing the duplicate switches off the constraint that
     * prevents it -- which is the whole block gone because of a warning about
     * the thing the block exists to fix.
     */
    @Test
    public void aReasonIsProseInEveryDelimiter() {
        String[] quotes = {"'", "\"", "'''", "\"\"\""};
        for (int q = 0; q < quotes.length; q++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    implementation('com.example:other:1.0') { because " + quotes[q]
                            + "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22"
                            + quotes[q] + " }\n");
            check(out.contains("kotlin-stdlib-jdk7:1.8.0")
                            && out.contains("kotlin-stdlib-jdk8:1.8.0"),
                    "a reason quoted with " + quotes[q]
                            + " is not a declaration, got <<" + out + ">>");
        }
    }

    /**
     * How a literal is delimited changes nothing about what it says, so the
     * same declaration written four ways produces the same block. Asserted as
     * an equivalence rather than case by case because the strict sweep already
     * passed the triple-quoted spelling for the wrong reason: the version came
     * back as {@code ""1.9.22""}, no version parsed out of it, and an
     * unreadable version counts as below the floor -- which happens to be the
     * safe answer, so nothing failed while the read was wrong.
     */
    @Test
    public void theDelimiterDoesNotChangeWhatADeclarationSays() {
        String[] quotes = {"'", "\"", "'''", "\"\"\""};
        String[] shapes = {
            "implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8') "
                    + "{ version { strictly %s1.9.22%s } }",
            "implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8') "
                    + "{ version { require %s1.9.22%s } }",
            "implementation group: %sorg.jetbrains.kotlin%s, "
                    + "name: %skotlin-stdlib-jdk8%s, version: '1.9.22'",
            "dependencies.add(%simplementation%s, "
                    + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22')",
        };
        for (int s = 0; s < shapes.length; s++) {
            String expected = null;
            for (int q = 0; q < quotes.length; q++) {
                String text = shapes[s].replace("%s", quotes[q]);
                String out = KotlinStdlibAlignment.constraintsBlock("implementation", text);
                if (expected == null) {
                    expected = out;
                    continue;
                }
                check(expected.equals(out),
                        "the delimiter does not change the answer for <<" + text
                                + ">>: expected <<" + expected + ">> got <<" + out + ">>");
            }
        }
    }

    /**
     * A pre-merge shim added through {@code dependencies.add} suppresses the
     * block, whichever delimiter names the configuration. Emitting beside it
     * raises kotlin-stdlib past the app's own class-bearing 1.7.22 jar, which
     * is this block manufacturing the duplicate it exists to prevent.
     */
    @Test
    public void anAddedPreMergeShimSuppressesTheBlock() {
        String[] quotes = {"'", "\"", "'''", "\"\"\""};
        for (int q = 0; q < quotes.length; q++) {
            String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                    "    dependencies.add(" + quotes[q] + "implementation" + quotes[q]
                            + ", 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!')\n");
            check("".equals(out),
                    "an added pre-merge shim suppresses the block, named with "
                            + quotes[q] + " but got <<" + out + ">>");
        }
    }

    /**
     * The complement of the sweep below, and the direction that fails in
     * silence: an app whose Gradle text says nothing about Kotlin still gets
     * both constraints. Every recognition rule added to this class is a new
     * way to conclude "the app has this covered", and concluding it wrongly
     * does not fail anything -- it just hands the duplicate class back to the
     * app this whole change exists to fix, with no signal anywhere.
     */
    @Test
    public void ordinaryProjectTextStillGetsTheAlignment() {
        String[] ordinary = {
            "implementation 'androidx.appcompat:appcompat:1.6.1'",
            "implementation('com.android.billingclient:billing:9.1.0')",
            "implementation group: 'com.google.android.material', name: 'material', "
                    + "version: '1.11.0'",
            "def v = '1.7.22'\nimplementation(\"com.squareup.okhttp3:okhttp:$v\")",
            "annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'",
            "implementation fileTree(dir: 'libs', include: ['*.jar'])",
            // Commented out is not declared, in either comment syntax.
            "// implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'",
            "/* implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22!!' */",
            // And a reason string is prose, not a pin.
            "implementation('a:b:1.0') { because 'strictly 1.7.22 was never wanted' }",
            "testImplementation 'junit:junit:4.13.2'",
            "",
        };
        String[] decorations = {
            "%s", "    %s", "dependencies {\n%s\n}", "%s // note",
            "%s\nimplementation 'com.google.code.gson:gson:2.10.1'",
        };
        for (int o = 0; o < ordinary.length; o++) {
            for (int d = 0; d < decorations.length; d++) {
                String text = decorations[d].replace("%s", ordinary[o]);
                String out = KotlinStdlibAlignment.constraintsBlock("implementation", text);
                check(out.contains("kotlin-stdlib-jdk7:1.8.0")
                                && out.contains("kotlin-stdlib-jdk8:1.8.0"),
                        "both constraints are still written for <<" + text
                                + ">> but got <<" + out + ">>");
            }
        }
    }

    /**
     * Every equivalent way of writing a strict pre-merge pin suppresses the
     * block. This is a sweep rather than an example, because the examples were
     * being found one review comment at a time while the same defect sat in
     * three different places: a triple-quoted definition expanded to
     * {@code ""1.7.22""}, no version was parsed out of it, and the constraint
     * went in beside the strict pin -- which does not fail the build, it
     * silently strips the classes and throws NoClassDefFoundError on the
     * device. That is the one outcome this class must never produce, so the
     * property is asserted over the whole spelling space and not over the
     * spellings somebody happened to think of.
     */
    @Test
    public void everySpellingOfAStrictPreMergePinSuppressesTheBlock() {
        String[] artifacts = {"kotlin-stdlib", "kotlin-stdlib-jdk7", "kotlin-stdlib-jdk8"};
        String[] quotes = {"'", "\"", "'''", "\"\"\""};
        String[] configurations = {"implementation", "api", "compile"};
        int checked = 0;
        for (int a = 0; a < artifacts.length; a++) {
            String coordinate = "org.jetbrains.kotlin:" + artifacts[a];
            for (int q = 0; q < quotes.length; q++) {
                String u = quotes[q];
                for (int c = 0; c < configurations.length; c++) {
                    String on = configurations[c];
                    String[] forms = {
                        on + "(" + u + coordinate + ":1.7.22!!" + u + ")",
                        // A classifier or @extension sits after the version, not in it.
                        on + "(" + u + coordinate + ":1.7.22!!@jar" + u + ")",
                        on + " " + u + coordinate + ":1.7.22!!" + u,
                        on + "(" + u + coordinate + u + ") { version { strictly "
                                + u + "1.7.22" + u + " } }",
                        on + "(" + u + coordinate + u + ")\n{ version { strictly "
                                + u + "1.7.22" + u + " } }",
                        "def v = " + u + "1.7.22" + u + "\n" + on + "(\""
                                + coordinate + ":$v!!\")",
                        "def d = " + u + coordinate + ":1.7.22!!" + u + "\n" + on + "(d)",
                        // A strict pin whose version this cannot evaluate is still a
                        // strict pin; unreadable has to fall to the conservative side.
                        on + "(" + u + coordinate + u + ") { version { strictly kotlinVersion } }",
                        // The definition carries the coordinate and NOTHING else --
                        // no version, no !! -- so the only thing that can suppress is
                        // the name being carried across to the strict usage. Written
                        // with the marker in the definition instead, these passed with
                        // the inlining switched off: that line names the artifact and
                        // ends in !!, so it suppressed on its own and the test proved
                        // nothing. However many modifiers the local was written with:
                        "String d = " + u + coordinate + u + "; " + on
                                + "(d) { version { strictly " + u + "1.7.22" + u + " } }",
                        "final String d = " + u + coordinate + u + "; " + on
                                + "(d) { version { strictly " + u + "1.7.22" + u + " } }",
                        "private static final String d = " + u + coordinate + u + "; " + on
                                + "(d) { version { strictly " + u + "1.7.22" + u + " } }",
                        // An apostrophe inside a dollar-slashy literal is not a quote.
                        "def m = $/can't/$; " + on + "(" + u + coordinate + ":1.7.22!!" + u + ")",
                    };
                    for (int f = 0; f < forms.length; f++) {
                        String[] decorated = {
                            forms[f],
                            "    " + forms[f],
                            "\t" + forms[f] + "   ",
                            forms[f] + " // a note",
                            "/* lead */ " + forms[f],
                            "dependencies {\n" + forms[f] + "\n}",
                            "repositories { mavenCentral() }\n" + forms[f],
                            forms[f] + "\nimplementation 'androidx.appcompat:appcompat:1.6.1'",
                            "implementation 'androidx.appcompat:appcompat:1.6.1'\n" + forms[f],
                        };
                        for (int d = 0; d < decorated.length; d++) {
                            checked++;
                            String out = KotlinStdlibAlignment.constraintsBlock(
                                    "implementation", decorated[d]);
                            check("".equals(out),
                                    "a strict pre-merge pin suppresses the block, written as <<"
                                            + decorated[d] + ">> but got <<" + out + ">>");
                        }
                    }
                }
            }
        }
        check(checked > 2000, "the sweep really ran over the matrix: " + checked);
    }

    /**
     * A preference is soft: Gradle takes it only when nothing stronger is in
     * play, so a transitive requirement for a pre-merge shim beats it. Reading
     * one as proof the artifact cannot resolve below the floor suppressed the
     * constraint that was the only thing standing between that graph and the
     * duplicate.
     */
    @Test
    public void aPreferenceDoesNotStandInForTheConstraint() {
        String modern = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8') "
                + "{ version { prefer '1.9.22' } }\n");
        check(modern.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a preferred version does not suppress the constraint");

        String old = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8') "
                + "{ version { prefer '1.7.22' } }\n");
        check(old.contains("kotlin-stdlib-jdk8:1.8.0"),
                "and neither does an old one, which the floor simply overrides");

        // A requirement AT OR ABOVE the floor does stand in for it: it already
        // satisfies the constraint, so leaving that artifact to the app says
        // something true. Below the floor it does not -- there the constraint
        // raises it, and skipping is what left a softly-required shim pre-merge.
        String required = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8') "
                + "{ version { require '1.9.22' } }\n");
        check(!required.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a merged-era requirement stands in for the constraint");

        String raised = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8') "
                + "{ version { require '1.7.22' } }\n");
        check(raised.contains("kotlin-stdlib-jdk8:1.8.0"),
                "and a pre-merge one is raised rather than honoured");

        // A requirement that OVERRIDES a coordinate is still read as the version
        // that declaration carries -- soft is about whether it pins, not about
        // whether it is read.
        String overridden = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ version { require '1.9.22' } }\n");
        check(overridden.contains("kotlin-stdlib-jdk7:1.8.0"),
                "the requirement is read past the coordinate, got <<"
                        + overridden + ">>");
    }

    /**
     * A map value written with the long delimiter keeps its content. Stripping
     * one character per side left the group and name wearing two quotes, so
     * both failed their exact match and the declaration was ignored.
     */
    @Test
    public void aTripleQuotedMapValueKeepsItsContent() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation group: '''org.jetbrains.kotlin''', "
                + "name: '''kotlin-stdlib-jdk8''', version: '1.9.22'\n");
        check(!out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a triple-quoted map declaration still pins its artifact");
    }

    /**
     * A typed local declares as much as def does.
     */
    @Test
    public void aTypedLocalDefinesACoordinate() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    String dep = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'\n"
                + "    implementation(dep) { version { strictly '1.7.22' } }\n");
        check("".equals(out), "a typed local carries the coordinate too");
    }

    /**
     * Groovy's dollar-slashy literal may open with a slash, which the comment
     * scanner read as a line comment and used to discard the rest of the
     * fragment, strict pin included. The plain slashy form is recognised too
     * now, positionally -- see divisionIsNotASlashyLiteral for the half of
     * that rule which says what is NOT a literal.
     */
    @Test
    public void aDollarSlashyLiteralDoesNotOpenAComment() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                // On one line, because a line comment only reaches the end of its
                // own line: put the pin on the next one and the test passes with
                // the literal unrecognised, which proves nothing.
                "    def marker = $//*/$; "
                + "implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22') "
                + "{ version { strictly '1.7.22' } }\n");
        check("".equals(out),
                "the strict pin after a dollar-slashy literal is still seen");
    }

    /**
     * A reason can be nothing BUT a coordinate, so the whitespace rule does not
     * catch it. `because 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'`
     * names the artifact it warns about; read as a declaration it supplied a
     * pre-merge version and suppressed the whole block -- the comment
     * describing the duplicate switching off the constraint that prevents it.
     */
    @Test
    public void aReasonThatIsOnlyACoordinateIsStillAReason() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('com.example:other:1.0') "
                + "{ because 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22' }\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a bare-coordinate reason does not declare anything");
        check(out.contains("kotlin-stdlib-jdk7:1.8.0"),
                "and does not take the block with it");
    }

    /**
     * A triple-quoted coordinate keeps its version. Stripping one character
     * per side left the long delimiter's extra quotes on the content, so the
     * version was unreadable and the declaration read as pre-merge.
     */
    @Test
    public void aTripleQuotedCoordinateKeepsItsVersion() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation '''org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.22'''\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a merged-era triple-quoted declaration leaves the sibling constrained");
        check(!out.contains("kotlin-stdlib-jdk7:1.8.0"),
                "and pins its own artifact");
    }

    /**
     * The fragments are separate build hints but one generated script, so a
     * def written in one is in scope for the next. Reading them apart lost the
     * definition at the boundary and the strict pin behind it went unseen.
     */
    @Test
    public void aDefinitionCrossesTheFragmentBoundary() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def stdlib = 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'\n",
                "    implementation(stdlib) { version { strictly '1.7.22' } }\n");
        check("".equals(out),
                "a definition in one fragment reaches a use in the next");
    }

    /**
     * A triple-quoted literal is a different delimiter, not three of the same
     * one. Reading its opener as a single quote made it close on the first
     * apostrophe inside it and threw every following statement out of step, so
     * a strict pin after it was never seen.
     */
    @Test
    public void aTripleQuotedLiteralDoesNotEndOnItsOwnApostrophe() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def note = '''can't stop'''\n"
                + "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!'\n");
        check("".equals(out),
                "the strict pin after a triple-quoted note is still seen");
    }

    /**
     * A runtimeOnly pre-merge pin suppresses both constraints.
     *
     * <p>This pins existing behaviour rather than verifying a fix: it was
     * reported as broken, and reverting the change it prompted leaves this
     * passing, because the configuration predicate accepts every main
     * configuration whatever it is handed. Kept because the behaviour is worth
     * holding, and labelled so nobody reads it as proof of something it does
     * not test.</p>
     */
    @Test
    public void aRuntimeOnlyPreMergePinSuppressesBoth() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    runtimeOnly 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!'\n");
        check("".equals(out),
                "a runtimeOnly pre-merge pin takes the sibling constraint with it");
    }

    /**
     * The strict bypass reads both spellings. Asking only about the strictly
     * keyword let a !! pin on a variant configuration be filtered out as a
     * variant declaration and get the constraint anyway -- against a strict
     * requirement that resolved fine before it.
     */
    @Test
    public void aShorthandPinOnAVariantIsStillStrict() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    debugImplementation "
                + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!'\n");
        check("".equals(out),
                "a !! pin on a variant configuration is honoured like a strictly call");

        // and a variant declaration that is NOT strict still does not suppress
        String plain = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    debugImplementation "
                + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n");
        check(plain.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a plain variant declaration still does not suppress");
    }

    /**
     * A known definition referred to as $name inside a double-quoted string is
     * the same one hop already followed for a bare token. Reading it as
     * unreadable made a merged-era version look pre-merge and took the
     * sibling's constraint down with it.
     */
    @Test
    public void aKnownDefinitionExpandsInsideAnInterpolatedString() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def kotlinVersion = '1.9.22'\n"
                + "    implementation \"org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion\"\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the expanded version is merged-era, so the sibling stays constrained");

        String braced = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def v = '1.7.22!!'\n"
                + "    implementation \"org.jetbrains.kotlin:kotlin-stdlib-jdk7:${v}\"\n");
        check("".equals(braced), "and a pre-merge one still suppresses both");

        // an UNKNOWN name stays unreadable, which is the conservative path
        String unknown = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation \"org.jetbrains.kotlin:kotlin-stdlib-jdk7:$mystery\"\n");
        check("".equals(unknown), "an unknown name is still unreadable");
    }

    /**
     * Definitions are applied in statement order. Substituting a variable's
     * FIRST value into every use of it made a statement after a reassignment
     * read as a declaration of the old value -- turning a debug-only Kotlin
     * pin into a main-variant one and dropping the jdk8 constraint.
     */
    @Test
    public void aReassignedVariableUsesItsCurrentValue() {
        // Ordered the other way round on purpose: with a two-pass map the LAST value
        // wins everywhere, so the main declaration above the reassignment reads as a
        // Kotlin pin it never was. Only walking in order gets this right.
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def dep = 'com.example:other:1.0'\n"
                + "    implementation(dep)\n"
                + "    dep = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n"
                + "    debugImplementation(dep)\n");
        check(out.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the main declaration uses the value in force where it stands");

        // and a reassignment to something unreadable forgets the name rather than
        // leaving the old value standing
        String forgotten = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def dep = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'\n"
                + "    dep = someFunction()\n"
                + "    implementation(dep)\n");
        check(forgotten.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an unreadable reassignment forgets the old literal");
    }

    /**
     * The map form has to match the declared GROUP, not the group appearing
     * anywhere. A fork under another group whose reason merely mentions
     * org.jetbrains.kotlin combined with an unrelated artifact name and read
     * as a Kotlin shim -- and since its version was below the floor, both
     * constraints went.
     */
    @Test
    public void theMapFormMatchesTheDeclaredGroup() {
        String fork = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation(group: 'com.example', name: 'kotlin-stdlib-jdk8', "
                + "version: '1.0') { because 'fork of org.jetbrains.kotlin' }\n");
        check(fork.contains("kotlin-stdlib-jdk8:1.8.0"),
                "another group's artifact is not our shim");
        check(fork.contains("kotlin-stdlib-jdk7:1.8.0"),
                "and it does not take the block with it");

        // the real map form still counts
        String real = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation group: 'org.jetbrains.kotlin', "
                + "name: 'kotlin-stdlib-jdk8', version: '1.9.22'\n");
        check(!real.contains("kotlin-stdlib-jdk8:1.8.0"),
                "the real group still pins jdk8");
    }

    /**
     * A rich-version closure can say what version is meant with a keyword
     * other than strictly. Reading only strictly left `version { require }`
     * with no version, which the conservative path treated as below the floor
     * -- dropping both constraints for a declaration already merged-era.
     */
    @Test
    public void aRequiredRichVersionIsAVersionToo() {
        String required = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk7') "
                + "{ version { require '1.9.22' } }\n");
        check(required.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a required merged-era version leaves the sibling constrained");

        String preferred = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk7') "
                + "{ version { prefer '1.9.22' } }\n");
        check(preferred.contains("kotlin-stdlib-jdk8:1.8.0"),
                "and so does a preferred one");

        // Below the floor it does NOT take both. This once asserted the
        // opposite, which was the wrong call: a requirement is soft, so the
        // constraint raises it and the two resolve to 1.8.0 together. Standing
        // the block down there left the shim at 1.7.22 beside whatever selected
        // a merged-era base -- the duplicate this exists to prevent, in the graph
        // it exists for.
        String old = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk7') "
                + "{ version { require '1.7.22' } }\n");
        check(old.contains("kotlin-stdlib-jdk7:1.8.0")
                        && old.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a soft pre-merge requirement is raised, not honoured, got <<"
                        + old + ">>");

        // The `!!` suffix inside a requirement is Gradle's strict shorthand, and
        // that one really does pin.
        String strict = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk7') "
                + "{ version { require '1.7.22!!' } }\n");
        check("".equals(strict), "a strict requirement still takes both, got <<"
                + strict + ">>");
    }

    /**
     * Gradle accepts more than a literal version, and each shape parsed as
     * zero before -- classifying a merged-era declaration as pre-merge and
     * dropping BOTH constraints, including the sibling's, which is the one
     * such a graph still needs. What matters is the lowest version the
     * selector can resolve to.
     */
    @Test
    public void aVersionSelectorIsReadByItsLowerBound() {
        // exact range at the floor: not below it
        String exact = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:[1.8.0]'\n");
        check(exact.contains("kotlin-stdlib-jdk8:1.8.0"),
                "an exact merged-era range leaves the sibling constrained");

        // dynamic selector that cannot go below the floor
        String dynamic = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.+'\n");
        check(dynamic.contains("kotlin-stdlib-jdk8:1.8.0"),
                "1.8.+ cannot resolve below the floor");

        // A range that STARTS below the floor but can still select above it keeps
        // the alignment. This was the conservative case once, on the grounds that
        // the range reaches below the floor at all; the question that decides
        // resolution is the other end. Gradle picks the highest version satisfying
        // every constraint, so [1.7.0,1.9.0) selects a merged-era shim and our
        // constraint on the SIBLING intersects that rather than conflicting with it
        // -- and suppressing instead left an old transitive jdk8 unaligned beside a
        // merged stdlib, which is the duplicate this class exists to prevent.
        //
        // The residual case is a range whose upper versions do not exist in the
        // repository, where Gradle falls back to something pre-merge and the
        // duplicate returns. That fails loudly in checkDuplicateClasses, which is
        // where the app already was, so it is the better of the two.
        String spanning = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:[1.7.0,1.9.0)'\n");
        check(spanning.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a range that can select above the floor keeps the sibling aligned, got <<"
                        + spanning + ">>");

        // A range that CANNOT reach the floor still suppresses: the constraint would
        // have nothing to resolve to and the build would fail outright.
        String capped = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:[1.6.0,1.8.0)'\n");
        check("".equals(capped),
                "a range capped below the floor suppresses, got <<" + capped + ">>");

        String low = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:[1.6.0,1.7.9]'\n");
        check("".equals(low), "and so does one entirely below it, got <<" + low + ">>");

        // 1.7.+ cannot leave 1.7; 1.+ can reach 1.9.
        String narrow = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.+'\n");
        check("".equals(narrow), "1.7.+ cannot reach the floor, got <<" + narrow + ">>");
        String wide = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.+'\n");
        check(wide.contains("kotlin-stdlib-jdk8:1.8.0"),
                "1.+ can, got <<" + wide + ">>");

        // and a dynamic selector below the floor likewise
        String oldDynamic = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.+'\n");
        check("".equals(oldDynamic), "1.7.+ is below the floor");
    }

    /**
     * Whether a declaration is strict and what version it is strict AT are two
     * questions. Asking only the second let `version { strictly kotlinVersion }`
     * read as not strict at all -- the opposite of the conservative path
     * documented everywhere else, and the one case where being wrong costs a
     * failed resolution rather than an override.
     */
    @Test
    public void aStrictPinWithAnUnreadableVersionStillSuppresses() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib:1.7.22') "
                + "{ version { strictly kotlinVersion } }\n");
        check("".equals(out),
                "a strict pin whose version cannot be read takes the conservative path");
    }

    /**
     * Groovy's command syntax drops the parentheses, and requiring them
     * rejected a declaration carrying an explicit strict pin.
     */
    @Test
    public void theParenthesisFreeAddFormCounts() {
        String out = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    add 'implementation', "
                + "'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!'\n");
        check("".equals(out),
                "add without parentheses is still an add");

        // and a quoted configuration name with no add in front still counts for nothing
        String bare = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    def cfg = 'implementation'\n"
                + "    something cfg, 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'\n");
        check(bare.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a quoted configuration name without an add does not declare anything");
    }

    /**
     * A rich-version closure carries the version instead of the coordinate.
     * Reading no version there made a merged-era declaration look below the
     * floor, which took the SIBLING's constraint down with it -- and the
     * sibling is the one the graph still needed.
     */
    @Test
    public void aRichVersionClosureSuppliesTheVersion() {
        String modern = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk7') "
                + "{ version { strictly '1.9.22' } }\n");
        check(modern.contains("kotlin-stdlib-jdk8:1.8.0"),
                "a merged-era jdk7 declaration leaves the jdk8 constraint standing");
        check(!modern.contains("kotlin-stdlib-jdk7:1.8.0"),
                "and jdk7 itself is left to the app");

        // Below the floor it still takes both, which is the case that rule exists for.
        String preMerge = KotlinStdlibAlignment.constraintsBlock("implementation",
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk7') "
                + "{ version { strictly '1.7.22' } }\n");
        check("".equals(preMerge),
                "a pre-merge rich-version declaration still suppresses both");
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
                "    implementation('org.jetbrains.kotlin:' + 'kotlin-stdlib-jdk8:1.7.22!!') "
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
                + "        version: '1.7.22!!'\n");
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
                "    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!'\n");
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
                + "\"org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!\")\n");
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
                + "\"org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!\")\n");
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
                + "        'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!'\n"
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
                "    implementation('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!') "
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
                + "        'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!'\n"
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
        // The GENERATED block, which is concatenated onto the script with a
        // leading `+`. The alignment's own argument opens with the same text and
        // now comes first in the file, so anchoring on the text alone found that
        // instead and looked for the constraints inside it.
        int at = src.indexOf("+ \"dependencies {\\n\"");
        assertTrue(at >= 0);
        String block = src.substring(at, src.indexOf("+ \"}\\n\"", at));
        assertTrue(block.contains("+ kotlinStdlibConstraints"));
    }

    private static void check(boolean condition, String message) {
        assertTrue(condition, message);
    }
}
