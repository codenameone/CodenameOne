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

/**
 * The Kotlin stdlib alignment written into the generated Android
 * {@code build.gradle}.
 *
 * <p><b>The failure it prevents.</b> Kotlin 1.8.0 folded the contents of
 * {@code kotlin-stdlib-jdk7} and {@code kotlin-stdlib-jdk8} into
 * {@code kotlin-stdlib} and left the two jdk artifacts as empty shims that
 * only depend on it. Gradle resolves every module's version independently,
 * so a graph that asks for {@code kotlin-stdlib} at 1.8.0 or newer through
 * one path and {@code kotlin-stdlib-jdk8} at something older through
 * another ends up with two real jars carrying the same classes, and the
 * build dies in {@code checkReleaseDuplicateClasses}:</p>
 *
 * <pre>
 * Duplicate class kotlin.collections.jdk8.CollectionsJDK8Kt found in modules
 *   kotlin-stdlib-1.8.22.jar (org.jetbrains.kotlin:kotlin-stdlib:1.8.22) and
 *   kotlin-stdlib-jdk8-1.6.21.jar (org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.21)
 * </pre>
 *
 * <p>Nothing exotic is needed to produce it. A single ordinary dependency
 * does it on its own: {@code com.android.billingclient:billing:9.1.0} pulls
 * {@code androidx.core:core:1.15.0}, which reaches {@code kotlin-stdlib}
 * 1.8.22 through {@code core-ktx} and {@code kotlin-stdlib-jdk8} 1.6.21
 * through {@code lifecycle-runtime -> kotlinx-coroutines-android:1.6.4}.
 * Neither coordinate is anything Codename One asked for, which is what makes
 * the error so hard to read from the app side: the app declares one library
 * and the report names two Kotlin artifacts it has never heard of.</p>
 *
 * <p><b>Why Gradle does not sort this out by itself.</b> It normally would.
 * From 1.9.22 {@code kotlin-stdlib} publishes Gradle module metadata whose
 * {@code jvmApiElements} and {@code jvmRuntimeElements} variants carry
 * dependency constraints raising {@code kotlin-stdlib-jdk7} and
 * {@code kotlin-stdlib-jdk8} to {@value #MERGED_STDLIB_FLOOR} -- the exact
 * alignment below. The 1.8.x line, which is what the current AndroidX
 * releases resolve to, publishes <b>no {@code .module} file at all</b>, only
 * a POM, and a POM cannot express a constraint. So on 1.8.x there is nothing
 * telling Gradle the two artifacts overlap, and it has no way to find out.
 * This class supplies for 1.8.x what JetBrains supplies from 1.9.22 on.</p>
 *
 * <p><b>Why nothing else excuses the block either.</b> A Kotlin BOM used to
 * suppress it, on the reasoning that a BOM manages the whole
 * {@code org.jetbrains.kotlin} group. That went the way of the plugin check
 * and for the same measured reason: against a graph carrying billing 9.1.0
 * and appcompat 1.6.1, adding this block alongside {@code kotlin-bom:1.9.22}
 * produced byte-identical resolution, because a BOM's constraints are not
 * strict and the higher version simply wins. Alongside
 * {@code kotlin-bom:1.7.22} it is not merely harmless but necessary -- a
 * pre-merge BOM raises the jdk artifacts and cannot pull
 * {@code kotlin-stdlib} back down, which is the duplicate. Removing the case
 * also removes the question of whether a BOM declared inside an {@code if}
 * block is in force, which no amount of reading the text can answer.</p>
 *
 * <p><b>Why a constraint and not a force.</b> A constraint raises a version
 * and never lowers one, and never pulls a module into a graph that does not
 * already contain it. An app with no Kotlin anywhere is therefore completely
 * unaffected -- the block resolves to nothing. An app that does have the jdk
 * artifacts gets them at {@value #MERGED_STDLIB_FLOOR} or newer, which is
 * always a shim, so the duplicate cannot arise whichever version of
 * {@code kotlin-stdlib} the rest of the graph settles on. Forcing a fixed
 * version would instead override a newer one the app deliberately asked
 * for.</p>
 *
 * <p><b>Why the Kotlin Gradle plugin does not excuse this.</b> From 1.8.0
 * the plugin aligns the jdk variants itself, so it was tempting to skip the
 * block whenever a new enough one was applied. That skip is gone, for two
 * reasons that point the same way. It was never load-bearing: measured
 * against a graph carrying billing 9.1.0 and appcompat 1.6.1, adding this
 * block alongside plugin 1.9.22 and 1.8.22 produced byte-identical
 * resolution, because the plugin's alignment already lands at or above this
 * floor and a constraint never lowers a version. And it was not sound
 * either -- the plugin's alignment can be turned off with
 * {@code kotlin.stdlib.jdk.variants.version.alignment=false}, which this
 * builder preserves out of a project's existing gradle.properties, so
 * "a new enough plugin is applied" was never the same question as "the jdk
 * variants are aligned".</p>
 *
 * <p>Deleting the skip answers both at once and takes with it the version
 * parsing, the reading of {@code android.topDependency} and the hazard that
 * a commented-out plugin declaration above an active one decided the
 * outcome. An older plugin gets the block for the reason it always did:
 * the 1.7 line ADDS {@code kotlin-stdlib-jdk8} at its own pre-merge version,
 * so the class-bearing jar is guaranteed present and any dependency reaching
 * a merged stdlib collides with it --</p>
 *
 * <pre>
 * plugin 1.7.22 alone            stdlib 1.7.22 + jdk7/jdk8 1.7.22   no duplicate
 * plugin 1.7.22 + billing 9.1.0  stdlib 1.8.22 + jdk7/jdk8 1.7.22   DUPLICATE
 * the same, with this block      stdlib 1.8.22 + jdk7/jdk8 1.8.0    fixed
 * </pre>
 *
 * <p><b>The cost of that, stated plainly.</b> On that pre-1.8 plugin path,
 * an app whose graph contains no merged stdlib (the first row above) did not
 * need the block, and gets its stdlib family raised to
 * {@value #MERGED_STDLIB_FLOOR} anyway -- newer than the compiler in use,
 * which Kotlin warns about. That is deliberate. Gradle cannot express a
 * constraint conditional on what another module resolved to, so the choice is
 * between a warning in the case that did not need help and a failed build in
 * the case that did, and a warning is the better of the two.</p>
 *
 * <p>Extracted into a pure static helper so it is unit-testable without a
 * Gradle run and so the BuildDaemon copy stays trivially diffable --
 * <b>keep this file in sync with its twin in the other repository</b>.</p>
 */
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KotlinStdlibAlignment {

    /**
     * The first {@code kotlin-stdlib} release that absorbed the jdk7/jdk8
     * classes, which is therefore the first version of those two artifacts
     * that is an empty shim rather than a second copy of the classes.
     * Verified against the published jars: {@code kotlin-stdlib-jdk8:1.7.22}
     * carries 14 classes including {@code CollectionsJDK8Kt}, and
     * {@code kotlin-stdlib-jdk8:1.8.0} carries one and none of them.
     *
     * <p>It is also the exact floor {@code kotlin-stdlib:1.9.22}'s own module
     * metadata constrains them to, and the version from which the Kotlin
     * Gradle plugin performs this alignment itself, so this is JetBrains'
     * number in three separate places rather than one chosen here.</p>
     */
    public static final String MERGED_STDLIB_FLOOR = "1.8.0";

    /**
     * The two artifacts whose classes moved into {@code kotlin-stdlib}.
     *
     * <p>Both are aligned, and each is suppressed on its own. Suppressing
     * both because the app named one would leave the artifact it did not name
     * unconstrained, and that is not symmetrical: {@code jdk8} depends on
     * {@code jdk7}, so an app pinning jdk8 raises jdk7 with it, while an app
     * pinning jdk7 leaves jdk8 exactly where the graph put it -- the original
     * duplicate, intact, with the block that would have fixed it switched
     * off. They can be treated separately because their class sets are
     * disjoint ({@code kotlin.jdk7} / {@code kotlin.io.path} against
     * {@code kotlin.collections.jdk8} / {@code kotlin.streams.jdk8}), so
     * constraining one and not the other cannot make a new duplicate.</p>
     */
    private static final String[] ALIGNED_ARTIFACTS = {
        "kotlin-stdlib-jdk7",
        "kotlin-stdlib-jdk8"
    };

    /** The group every artifact this class reasons about belongs to. */
    private static final String KOTLIN_GROUP = "org.jetbrains.kotlin";

    /** The merged library both shims depend on at the floor. */
    private static final String BASE_STDLIB = "kotlin-stdlib";

    private KotlinStdlibAlignment() {
    }

    /**
     * The {@code constraints} block to append inside the generated
     * {@code dependencies { }}, or an empty string when no alignment should
     * be written.
     *
     * @param configuration the dependency configuration to declare the
     *   constraints on, {@code implementation} on any AndroidX project. The
     *   caller passes the same name it uses for the rest of the block so a
     *   legacy {@code compile} project stays consistent with itself.
     * @param appGradleFragments the Gradle text the app itself contributed
     *   ({@code gradleDependencies}, {@code android.gradleDep} and the like).
     *   An artifact the app names there is left to the app; the Kotlin BOM
     *   suppresses both. Null entries are ignored.
     * @return the block, newline terminated, or {@code ""}
     */
    public static String constraintsBlock(String configuration,
            String... appGradleFragments) {
        if (configuration == null || configuration.trim().length() == 0) {
            return "";
        }
        String config = configuration.trim();
        // "because" is not decoration: it is what `gradle dependencyInsight` prints
        // next to the raised version, and this constraint is otherwise unattributable
        // to anything in the developer's project.
        String because = "Codename One: kotlin-stdlib " + MERGED_STDLIB_FLOOR
                + " absorbed the jdk7/jdk8 classes and the 1.8.x line ships no "
                + "Gradle module metadata to say so, so these are raised to the "
                + "empty shims to avoid a duplicate class in checkDuplicateClasses";
        // A strict pin on the merged library itself blocks BOTH shims, because the
        // shim at this floor depends on kotlin-stdlib at the same floor. An app
        // strictly holding kotlin-stdlib below it therefore cannot resolve either
        // constraint, and the pre-merge family it is holding had no duplicate to
        // begin with -- so constraining there converts a working build into
        // "Could not resolve ... {strictly 1.7.22}", which is the one outcome this
        // class must never produce.
        if (strictlyPinsBaseStdlibBelowTheFloor(appGradleFragments)) {
            return "";
        }
        // failOnVersionConflict turns every disagreement into a build failure, and
        // raising a shim from 1.7.x to the floor IS a disagreement -- so in that mode
        // the block converts a graph that resolved coherently into
        // "Conflict found ... between versions 1.8 and 1.7". Nothing here can be
        // written that would not conflict, so nothing is.
        String[] active = activeLines(combined(appGradleFragments));
        for (int i = 0; i < active.length; i++) {
            // An ENFORCED platform is the one case a Kotlin BOM stands this down.
            // A plain `platform()` does not, and the class comment says why it was
            // measured not to: a BOM's constraints are ordinary, so the higher
            // version simply wins and these are harmless beside it. enforcedPlatform
            // is the other thing -- Gradle turns the same versions into STRICT
            // requirements -- so a pre-merge one strictly pins the family at 1.7.x
            // and a 1.8.0 requirement written beside it cannot resolve at all.
            if (namesAnEnforcedKotlinPlatformBelowTheFloor(active[i])) {
                return "";
            }
            if (!callsNamed(active[i], "failOnVersionConflict")) {
                continue;
            }
            // On a configuration that never receives the constraint it cannot
            // conflict with it. `configurations.create('tooling')
            // .resolutionStrategy.failOnVersionConflict()` governs a
            // configuration the app made and nothing extends.
            if (!governsTheConstrainedGraph(active[i], config)) {
                continue;
            }
            // A statement that governs the plugin classpath never arrives here:
            // both spellings of it -- a buildscript block and configurations
            // .classpath -- are blanked with the rest of that graph before any
            // scan runs. See governsThePluginClasspath.
            return "";
        }
        // The two shims cannot be suppressed independently when the app holds one of
        // them below the merge. Measured: an app pinning the whole family at 1.7.22
        // resolves with no duplicate, and emitting only the surviving sibling raises
        // kotlin-stdlib to 1.8.0 -- which carries the jdk8 classes -- beside the app's
        // class-bearing jdk8 1.7.22 jar. That is this block MAKING the duplicate it
        // exists to prevent, in a graph the app had arranged correctly.
        for (int i = 0; i < ALIGNED_ARTIFACTS.length; i++) {
            if (declaredBelowTheFloor(ALIGNED_ARTIFACTS[i], config, appGradleFragments)) {
                return "";
            }
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < ALIGNED_ARTIFACTS.length; i++) {
            if (declaresArtifact(ALIGNED_ARTIFACTS[i], config, appGradleFragments)) {
                continue;
            }
            out.append("        ").append(config)
               .append("('org.jetbrains.kotlin:").append(ALIGNED_ARTIFACTS[i])
               .append(':').append(MERGED_STDLIB_FLOOR).append("') {\n")
               .append("            because '").append(because).append("'\n")
               .append("        }\n");
        }
        if (out.length() == 0) {
            return "";
        }
        return "    constraints {\n" + out + "    }\n";
    }

    /**
     * Whether the app declares this shim at a version below the merge floor.
     *
     * <p>A version this cannot read counts as below it. An app naming one of
     * these artifacts at all is managing the family, and the harm of assuming
     * the worst is an alignment not written for an app that had already sorted
     * itself out, against a duplicate class manufactured in one that had.</p>
     */
    private static boolean declaredBelowTheFloor(String artifact, String configuration,
            String[] appGradleFragments) {
        String[] lines = activeLines(combined(appGradleFragments));
        {
            for (int j = 0; j < lines.length; j++) {
                // The configuration actually being constrained, rather than the two
                // that used to be named here. That was reported as letting a
                // runtimeOnly pre-merge pin suppress its own constraint and not its
                // sibling's; it did not, because declaresOnTheConstrainedConfiguration
                // ORs in every MAIN_CONFIGURATIONS entry whatever it is passed, so the
                // hard-coded names never restricted anything. Checked by putting them
                // back: the behaviour is identical. Passing the real configuration
                // regardless, because two names that look like a filter and are not
                // one will be read as a filter by the next person.
                if (!declaresArtifactOnLine(artifact, configuration, lines[j])) {
                    continue;
                }
                if (!namesArtifactAnywhere(lines[j], artifact)) {
                    continue;
                }
                // A rejection that removes the floor decides this on its own, whatever
                // the requirement beside it says. `require '1.+'; reject '[1.8.0,)'`
                // can only select a pre-merge 1.x, and reading the requirement alone
                // called that merged-era -- so its own constraint was skipped as
                // satisfied while the sibling was raised around it, which is the
                // duplicate again.
                if (rejectsTheFloor(lines[j])) {
                    return true;
                }
                String declared = declaredVersionOf(lines[j], artifact);
                if (declared != null && declared.endsWith(STRICT_SUFFIX)) {
                    declared = declared.substring(0,
                            declared.length() - STRICT_SUFFIX.length());
                }
                if (belowTheFloor(declared)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether this statement establishes a version for {@code artifact} that
     * Gradle will actually hold it to.
     */
    private static boolean bindsAVersion(String line, String artifact) {
        return declaredVersionOf(line, artifact) != null;
    }

    /**
     * Whether the text mentions one of the artifacts this class aligns.
     *
     * <p>Used to decide whether a statement absorbs the closure that follows it,
     * where the group alone was the trigger. A rule comparing only the name --
     * {@code if (d.requested.name == 'kotlin-stdlib') {} } with the useVersion on
     * the next line -- never named the group, so the condition and the body
     * stayed separate statements and neither said anything.</p>
     *
     * <p>Deliberately a plain mention rather than the careful reading
     * namesArtifactAnywhere does: gluing a closure onto a statement is bounded
     * to one declaration either way, and the careful question is asked later of
     * the merged text.</p>
     */
    /** Whether the statement continues the previous one with an else branch. */
    private static boolean continuesWithElse(String statement) {
        int i = skipBlanks(statement, 0);
        while (i < statement.length() && statement.charAt(i) == '}') {
            i = skipBlanks(statement, i + 1);
        }
        int end = i;
        while (end < statement.length() && isIdentifierChar(statement.charAt(end))) {
            end++;
        }
        return "else".equals(statement.substring(i, end));
    }

    private static boolean namesAnAlignedArtifact(String text) {
        // The base library's name, which is a prefix of both shims', so one test
        // covers the family. ALIGNED_ARTIFACTS is the two shims alone -- they are
        // what gets a constraint written -- and asking only those missed a rule
        // naming the base, which is the one whose version decides everything.
        return text.contains(BASE_STDLIB);
    }

    /** Whether the statement names the artifact, in either spelling. */
    private static boolean namesArtifactAnywhere(String line, String artifact) {
        return namesCoordinate(line, artifact)
                || (declaresMapEntry(line, "group", KOTLIN_GROUP)
                && declaresMapEntry(line, "name", artifact))
                // The bare artifact name is enough, without the group beside it. A
                // rule may compare one part only -- `if (d.requested.name ==
                // 'kotlin-stdlib') d.useVersion '1.7.22'` -- and it is in force
                // either way; requiring both left that override unread, so the
                // shims were raised to 1.8.0 around a base library the rule held at
                // 1.7.22. That build links and then fails at runtime, when the jdk
                // classes are in neither jar.
                //
                // Not when the statement declares some OTHER group, though: a fork
                // published as com.example:kotlin-stdlib-jdk8 is a different module
                // that happens to share a name, and reading it as the shim stood the
                // whole block down for a pre-merge version of somebody else's jar.
                //
                // Otherwise safe, because these three names are the whole question:
                // nothing else publishes kotlin-stdlib or its jdk shims, and the
                // literal has to BE the name, so a coordinate that merely contains
                // it does not match.
                //
                // A rule that compares the group to something else instead of
                // declaring it -- d.requested.group == 'com.example' -- is not
                // caught, and is left uncaught: telling a group literal from a
                // version or a classifier by shape is the kind of guess this class
                // keeps having to correct, and being wrong here only suppresses.
                || (holdsLiteral(line, artifact) && !namesAnotherGroup(line))
                // An override that names the GROUP on its own applies to every
                // module in it, this family included:
                //   if (d.requested.group == 'org.jetbrains.kotlin')
                //       d.useVersion '1.7.22'
                // is the canonical Gradle snippet, and it holds the base library
                // pre-merge while these constraints raise the shims to their empty
                // 1.8.0 jars -- the failure that reaches the device.
                //
                // The group has to be a literal of its OWN, which is what makes
                // this narrow: `force 'org.jetbrains.kotlin:kotlin-reflect:1.7.22'`
                // carries the group inside a coordinate and does not match, so an
                // override of an unrelated Kotlin module still leaves the block to
                // write. A rule that names the group AND some other artifact does
                // match, and stands the block down for a module it does not govern
                // -- the ambiguity resolved the way every other one here is,
                // because that costs an app the duplicate it already had.
                || (holdsLiteral(line, KOTLIN_GROUP) && callsForce(line, artifact)
                        && !namesOneOfTheFamily(line));
    }

    /**
     * Whether the statement names a particular member of the family, as a
     * literal of its own.
     *
     * <p>What stops the group-wide reading above from widening a rule that has
     * already narrowed itself. {@code group == '..' && name == 'kotlin-stdlib'}
     * governs the base library and nothing else, and reading it as governing the
     * family made the siblings look declared -- so their constraints were skipped
     * as already handled and the block came out empty. That is not the safe
     * direction: it leaves the duplicate exactly where it was.</p>
     */
    private static boolean namesOneOfTheFamily(String line) {
        if (holdsLiteral(line, BASE_STDLIB)) {
            return true;
        }
        for (int i = 0; i < ALIGNED_ARTIFACTS.length; i++) {
            if (holdsLiteral(line, ALIGNED_ARTIFACTS[i])) {
                return true;
            }
        }
        return false;
    }

    /** Whether the statement declares a group entry that is not Kotlin's. */
    private static boolean namesAnotherGroup(String line) {
        String group = mapEntryValue(line, "group");
        return group != null && !KOTLIN_GROUP.equals(group);
    }

    /**
     * Whether the statement contains {@code value} as a literal of its own.
     *
     * <p>A resolution rule names an artifact by comparing its parts --
     * {@code d.requested.group == 'org.jetbrains.kotlin' && d.requested.name ==
     * 'kotlin-stdlib'} -- which is neither a coordinate nor a map entry, so
     * neither of the shapes above saw it and a useVersion rewriting the base
     * library went unread.</p>
     */
    private static boolean holdsLiteral(String line, String value) {
        for (int i = 0; i < line.length(); i++) {
            if (!isLiteralStart(line, i)) {
                continue;
            }
            int end = endOfStringLiteral(line, i);
            if (value.equals(stringLiteralContent(line, i))
                    && !isReasonArgument(line, i)) {
                return true;
            }
            i = end;
        }
        return false;
    }

    /**
     * The version this statement declares for {@code artifact}: the third
     * segment of its coordinate, or the map form's {@code version:} entry.
     * Null when neither is readable, which callers treat as below the floor.
     */
    private static String declaredVersionOf(String line, String artifact) {
        // A rich version OVERRIDES the coordinate's own, so it is read first.
        //   implementation('...:kotlin-stdlib-jdk7:1.9.22') { version { strictly '1.7.22' } }
        // resolves strictly to 1.7.22, and reporting 1.9.22 read a pre-merge pin as
        // merged-era: the jdk7 constraint was skipped as already satisfied while jdk8
        // and the base were raised around it.
        String rich = richVersionIn(line);
        if (rich != null) {
            return rich;
        }
        // A rich version that is PRESENT but unreadable is not an invitation to
        // read the coordinate instead. `version { strictly providers
        // .gradleProperty('legacy').get() }` beside a merged-era coordinate
        // reported the coordinate, so a strict pin that may well be pre-merge read
        // as merged-era: its own constraint was skipped as satisfied while the
        // sibling was raised around it, which is the duplicate again. Unreadable is
        // the honest answer, and belowTheFloor treats it as below.
        if (callsStrictly(line) || callsNamed(line, USE_VERSION)) {
            return null;
        }
        String fromCoordinate = coordinateVersionOf(line, artifact);
        if (fromCoordinate != null) {
            return fromCoordinate;
        }
        String mapped = mapEntryValue(line, "version");
        if (mapped != null) {
            return mapped;
        }
        return null;
    }

    /**
     * Whether a soft {@code require} is the only thing holding this artifact.
     *
     * <p>Such a declaration is not management: the constraint raises it and the
     * two coexist. Anything that really pins -- a {@code strictly}, a force, a
     * rejection that closes the floor, or the {@code !!} suffix on the
     * requirement itself -- answers false, and so does a coordinate carrying its
     * own version, which is the app's chosen version rather than a floor under
     * it.</p>
     */
    private static boolean heldOnlyBySoftRequirement(String line, String artifact) {
        if (callsStrictly(line) || callsForce(line, artifact) || rejectsTheFloor(line)) {
            return false;
        }
        String declared = declaredVersionOf(line, artifact);
        if (declared == null || declared.endsWith(STRICT_SUFFIX)) {
            // Unreadable stays conservative, and the `!!` suffix is a pin.
            return false;
        }
        // Only BELOW the floor. A soft version there is the case this is for: the
        // constraint raises it and the two agree, so neither standing the block
        // down nor skipping that artifact is right -- both leave the shim
        // pre-merge beside whatever selected a merged-era base, which is the
        // duplicate this exists to prevent.
        //
        // At or above the floor a soft version already satisfies the constraint,
        // so leaving that artifact to the app costs nothing and says something
        // true: the app has it in hand. A plain coordinate is soft in exactly the
        // way a `require` is, which is why they are one question here now.
        return isAPlainVersion(declared) && belowTheFloor(declared);
    }

    /**
     * Whether the version is an ordinary one rather than a selector or an
     * unreadable reference.
     *
     * <p>What makes the exemption above safe is that the constraint can RAISE
     * the version: {@code 1.7.22} and a floor of 1.8.0 agree on 1.8.0. Nothing
     * else here can be raised that way. A range is satisfied or it is not --
     * {@code [1.0,1.5]} and 1.8.0 have no version in common, so exempting one
     * would write a constraint that cannot resolve. And a version this cannot
     * read at all, {@code $mystery} or {@code latest.release}, says nothing
     * about what it will be, which is the conservative path everywhere else.</p>
     */
    private static boolean isAPlainVersion(String version) {
        if (version.length() == 0 || !Character.isDigit(version.charAt(0))) {
            return false;
        }
        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);
            if (c == '[' || c == ']' || c == '(' || c == ')' || c == ','
                    || c == '+' || c == '$' || c == '{') {
                return false;
            }
        }
        return true;
    }

    /** The version the artifact's own coordinate carries, or null. */
    private static String coordinateVersionOf(String line, String artifact) {
        String coordinate = KOTLIN_GROUP + ":" + artifact + ":";
        // Past `using`, when there is one: a substitution names the replaced module
        // first and the replacement second, and it is the replacement that decides
        // what resolves.
        int from = afterCall(line, "using");
        String lowest = null;
        for (int i = from < 0 ? 0 : from; i < line.length(); i++) {
            char c = line.charAt(i);
            if (!isLiteralStart(line, i)) {
                continue;
            }
            int end = endOfStringLiteral(line, i);
            String literal = stringLiteralContent(line, i);
            // A literal ending AT the version separator carries no version: the
            // rest is concatenated on, as in ("...:kotlin-stdlib-jdk7:" + version).
            // Returning the empty string there read as a version below the floor and
            // suppressed the block for a declaration that may well be merged-era.
            // Unreadable is the honest answer, and it leaves both constraints to be
            // written -- which cannot conflict with a plain requirement, only with a
            // strict pin, and those are read before this.
            if (literal.startsWith(coordinate)
                    && literal.length() > coordinate.length()
                    && !hasWhitespace(literal)
                    && !isReasonArgument(line, i)) {
                // A reason can be nothing but a coordinate, and this scan reached it
                // before the map's own version: entry. namesCoordinate learned to
                // skip a reason and this did not, so the comment describing an old
                // artifact supplied the version for the declaration warning about it.
                String found = versionComponentOf(literal.substring(coordinate.length()));
                lowest = lower(lowest, found);
            }
            i = end;
        }
        return lowest;
    }

    /**
     * The lower of two versions for the same module, either of which may be
     * null for "not seen yet".
     *
     * <p>One statement can name a module twice: {@code force} takes varargs, so
     * {@code force 'g:a:1.9.22', 'g:a:1.7.22'} is one call listing the same
     * module at two versions. Reading the first reported the merged-era one and
     * wrote the shim constraints beside a base library that may be forced
     * pre-merge, which is the failure that reaches the device rather than the
     * build.</p>
     *
     * <p>The LOWER rather than the last. Which of two selectors for one module
     * Gradle keeps is not something this can establish from the text, and it
     * does not have to: the lower answer is right if Gradle takes it, and
     * conservative if Gradle takes the other, which is how this class resolves
     * every ambiguity it cannot evaluate. An unreadable version is already the
     * lowest answer there is.</p>
     */
    private static String lower(String held, String found) {
        if (held == null) {
            return found;
        }
        if (found == null) {
            return held;
        }
        return compareVersions(withoutStrictSuffix(found),
                withoutStrictSuffix(held)) < 0 ? found : held;
    }

    /**
     * The value of a {@code key: 'value'} map entry, or null.
     *
     * <p>The KEY is looked for outside string literals only. A reason quoting
     * the map form -- {@code because "avoid group: 'org.jetbrains.kotlin',
     * name: 'kotlin-stdlib-jdk8'"} -- otherwise read as a declaration of that
     * artifact, and since prose carries no version the whole block was
     * suppressed. Same rule as the coordinate matcher beside it, which is
     * where this had drifted apart from.</p>
     */
    /**
     * The version out of what follows {@code group:name:} in a coordinate.
     *
     * <p>Gradle's notation carries two optional modifiers after the version --
     * a classifier as a fourth colon-separated part, and an {@code @extension}
     * -- and both were being returned as part of the version. That leaves
     * {@code 1.7.22!!@jar}, which does not end in the strict marker, so a
     * strict pre-merge pin read as an ordinary one and the constraint was
     * written beside it.</p>
     */
    private static String versionComponentOf(String remainder) {
        int end = remainder.length();
        int at = remainder.indexOf('@');
        if (at >= 0) {
            end = at;
        }
        int classifier = remainder.indexOf(':');
        if (classifier >= 0 && classifier < end) {
            end = classifier;
        }
        return remainder.substring(0, end);
    }

    private static String mapEntryValue(String line, String key) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (isLiteralStart(line, i)) {
                // Groovy lets a map key be quoted -- ('group': '...', 'name': '...') --
                // and skipping every literal meant the key was never seen, so a
                // declaration written that way named no artifact at all.
                int quoted = endOfStringLiteral(line, i);
                if (key.equals(stringLiteralContent(line, i))) {
                    int at = skipBlanks(line, quoted + 1);
                    if (at < line.length() && line.charAt(at) == ':') {
                        String value = valueAfterColon(line, at);
                        if (value != null) {
                            return value;
                        }
                    }
                }
                i = quoted;
                continue;
            }
            if (!line.startsWith(key, i)) {
                continue;
            }
            boolean startsToken = i == 0 || !isIdentifierChar(line.charAt(i - 1));
            int after = i + key.length();
            if (!startsToken || (after < line.length()
                    && isIdentifierChar(line.charAt(after)))) {
                continue;
            }
            int j = skipBlanks(line, after);
            if (j >= line.length() || line.charAt(j) != ':') {
                continue;
            }
            String value = valueAfterColon(line, j);
            if (value != null) {
                return value;
            }
            i = j;
        }
        return null;
    }

    /**
     * The literal following the colon at {@code colonAt}, or null.
     *
     * <p>Shared by both spellings of a key, bare and quoted, so the delimiter
     * rule is read once. Stripping one character per side used to leave a
     * triple-quoted group or name wearing two quotes, and both then failed
     * their exact match.</p>
     */
    private static String valueAfterColon(String line, int colonAt) {
        int at = skipBlanks(line, colonAt + 1);
        if (at < line.length() && isLiteralStart(line, at)
                && endOfStringLiteral(line, at) < line.length()) {
            return stringLiteralContent(line, at);
        }
        return null;
    }

    /**
     * Whether the app strictly holds {@code kotlin-stdlib} itself below the
     * floor both shims depend on.
     *
     * <p>The artifact has to be matched exactly. {@code kotlin-stdlib} is a
     * prefix of {@code kotlin-stdlib-jdk8}, so a loose match would read every
     * shim declaration as a pin on the base library and switch the whole block
     * off. The character after the coordinate decides it: a colon starts the
     * version and a quote ends the coordinate, while anything else -- a
     * hyphen above all -- means this is a longer artifact name.</p>
     *
     * <p>An unreadable strict version counts as below the floor, because the
     * failure it guards against cannot be worked around by the app while the
     * duplicate class it risks instead can.</p>
     */
    private static boolean strictlyPinsBaseStdlibBelowTheFloor(String[] appGradleFragments) {
        String[] lines = activeLines(combined(appGradleFragments));
        {
            for (int j = 0; j < lines.length; j++) {
                if (!namesBaseStdlib(lines[j])) {
                    continue;
                }
                // Whether it is held strictly and what version it is held AT are two
                // questions. Asking only the second let a strict pin whose version is
                // unreadable -- version { strictly kotlinVersion } -- read as not
                // strict at all, which is the opposite of the conservative path this
                // documents everywhere else. belowTheFloor(null) is true for exactly
                // this reason, and guarding on non-null defeated it.
                if (holdsBaseStdlibStrictly(lines[j])
                        && belowTheFloor(strictVersionOfBaseStdlib(lines[j]))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The version this statement strictly holds {@code kotlin-stdlib} at, or
     * null when it does not hold it strictly.
     *
     * <p>Two spellings mean the same thing. The {@code strictly} call is one;
     * Gradle's {@code !!} suffix on the version is the other, and missing it
     * was not a near miss. Measured with Gradle: an app writing
     * {@code 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22!!'} beside a
     * pre-merge jdk8 resolves the coherent 1.7.22 family on its own, and with
     * this block's constraints added resolves kotlin-stdlib 1.7.22 beside
     * jdk7/jdk8 1.8.0 -- the EMPTY shims. The jdk extension classes are then
     * supplied by neither jar, and the app fails at runtime with a missing
     * class rather than at build time with a duplicate one. That is the worst
     * outcome available here, so the suffix is read as what it is.</p>
     */
    private static String strictVersionOfBaseStdlib(String line) {
        if (callsStrictly(line)) {
            return strictVersionIn(line);
        }
        if (callsForce(line, BASE_STDLIB)) {
            return declaredVersionOf(line, BASE_STDLIB);
        }
        String declared = declaredVersionOf(line, BASE_STDLIB);
        if (declared != null && declared.endsWith(STRICT_SUFFIX)) {
            return declared.substring(0, declared.length() - STRICT_SUFFIX.length());
        }
        return null;
    }

    /** Whether the statement holds the base library strictly, in either spelling. */
    private static boolean holdsBaseStdlibStrictly(String line) {
        return holdsStrictly(line, BASE_STDLIB);
    }

    /**
     * Whether the statement holds {@code artifact} strictly, in either
     * spelling.
     *
     * <p>One predicate because there were two, and they diverged: the bypass
     * that lets a strict pin escape the configuration filter asked only about
     * the {@code strictly} keyword, so
     * {@code debugImplementation '...jdk8:1.7.22!!'} was filtered out as a
     * variant declaration and got the constraint anyway -- against a strict
     * requirement that had resolved fine before it.</p>
     */
    private static boolean holdsStrictly(String line, String artifact) {
        if (callsStrictly(line) || callsForce(line, artifact)) {
            return true;
        }
        // A rejection manages the version from the other side, but only when it
        // actually leaves our floor nothing to select. rejectAll does; so does an
        // open-ended range starting at or below the floor, `reject '[1.8.0,)'`.
        // `reject '1.7.0'` does not -- 1.8.0 and everything after it are still
        // available, the graph still needs aligning, and treating every rejection as
        // management left the original duplicate unfixed.
        if (rejectsTheFloor(line)) {
            return true;
        }
        String declared = declaredVersionOf(line, artifact);
        return declared != null && declared.endsWith(STRICT_SUFFIX)
                && strictCoordinateIsDeclared(line, artifact);
    }

    /**
     * Whether the strict coordinate this statement carries is actually being
     * DECLARED, rather than merely passed to something.
     *
     * <p>The {@code !!} spelling skips the configuration check, because a strict
     * pin is honoured wherever it is declared. "Wherever" still means declared:
     * {@code logger.lifecycle('org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!')}
     * is a log line, and reading it as a pin stood the whole block down for an app
     * that had declared nothing at all.</p>
     *
     * <p>The discriminator is not a list of the calls that count -- configurations
     * are open ended, and every list of them in this class has needed correcting.
     * It is that a configuration is never reached through a receiver: an app
     * writes {@code implementation '...'} or {@code myCustomConfig '...'}, never
     * {@code project.implementation '...'}. So an unqualified call declares and a
     * qualified one does not, with the dependency handler itself as the exception
     * that {@code add} already needed.</p>
     *
     * <p>Only the coordinate spelling is asked. A {@code version: '1.7.22!!'} map
     * entry is a dependency notation and nothing else, and a version reached
     * through a rich-version closure has been read as syntax already.</p>
     */
    private static boolean strictCoordinateIsDeclared(String line, String artifact) {
        String coordinate = KOTLIN_GROUP + ":" + artifact + ":";
        for (int i = 0; i < line.length(); i++) {
            if (!isLiteralStart(line, i)) {
                continue;
            }
            int end = endOfStringLiteral(line, i);
            String literal = stringLiteralContent(line, i);
            if (literal.startsWith(coordinate)
                    && versionComponentOf(literal.substring(coordinate.length()))
                            .endsWith(STRICT_SUFFIX)) {
                return isDeclarationArgument(line, i);
            }
            i = end;
        }
        // No coordinate carries it, so it came from a map entry or a closure. A
        // map has to be DECLARED too: `def catalog = [group: '..', name:
        // 'kotlin-stdlib', version: '1.7.22!!']` is dependency-shaped data that
        // is never added to a configuration, and reading it as a strict pin stood
        // the whole block down for an app that had declared nothing.
        //
        // Safe to exclude here where the same exclusion was NOT safe for
        // enforcedPlatform: a map IS recorded as a definition's value, so
        // `implementation(catalog)` below carries it and is read there. The
        // platform's call expression is not recorded, which is why that one is
        // still honoured wherever it appears.
        //
        // An assignment before the map is what says it is stored rather than
        // declared, which needs no list of the calls that declare.
        return !isStoredRatherThanDeclared(line);
    }

    /** Whether an assignment precedes the map this statement carries. */
    private static boolean isStoredRatherThanDeclared(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (isLiteralStart(line, i)) {
                i = endOfStringLiteral(line, i);
                continue;
            }
            char c = line.charAt(i);
            if (c == '[' || followedByMapKeyColon(line, i)) {
                return false;
            }
            if (isAssignmentAt(line, i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether an unqualified call is one of the dependency handler's adders.
     *
     * <p>Named rather than matched by prefix. `add*` accepted any helper an app
     * had defined -- {@code def addNote = { config, text -> .. }} called with a
     * configuration and a coordinate declared a dependency as far as this was
     * concerned, and the constraint for that artifact was skipped as already
     * handled.</p>
     *
     * <p>A qualified call does NOT consult this: there the receiver settles it,
     * so a handler that grows a fourth adder keeps working through
     * {@code dependencies.whatever(..)}. What an unrecognised name costs is a
     * constraint written beside a declaration that was already there, which is
     * the direction this class errs in everywhere.</p>
     */
    private static boolean isADependencyHandlerAdder(String method) {
        for (int i = 0; i < DEPENDENCY_HANDLER_ADDERS.length; i++) {
            if (DEPENDENCY_HANDLER_ADDERS[i].equals(method)) {
                return true;
            }
        }
        return false;
    }

    /** DependencyHandler's methods that take a configuration and a notation. */
    private static final String[] DEPENDENCY_HANDLER_ADDERS = {
        "add",
        "addProvider",
        "addProviderBundle"
    };

    /** Whether the literal at {@code quoteAt} is an argument of a declaring call. */
    private static boolean isDeclarationArgument(String line, int quoteAt) {
        int i = skipBlanksBackward(line, quoteAt - 1);
        // Every parenthesis, not one. Groovy accepts a redundant pair --
        // `implementation(('g:a:1.7.22!!'))` -- and stepping over a single one
        // left the walk looking at the other, which is not an identifier, so the
        // strict pin read as nobody's argument and the constraints went in
        // against it. Done before the comma and brace are looked for, because a
        // wrapped LATER argument -- `add('impl', ('g:a:1.7.22!!'))` -- reaches
        // them only once the parentheses are behind it.
        while (i >= 0 && line.charAt(i) == '(') {
            i = skipBlanksBackward(line, i - 1);
        }
        if (i >= 0 && (line.charAt(i) == ',' || line.charAt(i) == '{')) {
            // Not the first thing the call was handed. A comma is where a
            // coordinate sits in `dependencies.add('implementation', 'g:a:1.7!!')`,
            // and a brace is where it sits inside a provider:
            // `dependencies.addProvider('implementation', providers.provider {
            // 'g:a:1.7!!' })`. Walking back one token found the punctuation and
            // stopped, so the strict pin the app really had went unread and the
            // constraints went in against it.
            //
            // The comma half was removed once as an exception that constrains
            // nothing, on the reasoning that such a call is recognised where the
            // CONFIGURATION name is read -- true for the shims, and not for the
            // base library, which has a scan of its own that comes through here.
            return isDeclarationCall(line, enclosingCallOf(line, quoteAt));
        }
        if (i < 0 || !isIdentifierChar(line.charAt(i))) {
            // Not an argument of anything -- a bare literal in a list, or an
            // assignment's value. The use of the name decides those, not this.
            return false;
        }
        return isDeclarationCall(line, i);
    }

    /**
     * The index of the last character of the call whose argument list encloses
     * {@code at}, or -1.
     *
     * <p>Found forward, so a parenthesis inside a string is not one. Groovy's
     * command syntax has no parentheses at all, and there the call is the
     * statement's first token.</p>
     */
    private static int enclosingCallOf(String line, int at) {
        List<Integer> opened = new ArrayList<Integer>();
        for (int i = 0; i < at && i < line.length(); i++) {
            if (isLiteralStart(line, i)) {
                i = endOfStringLiteral(line, i);
                continue;
            }
            char c = line.charAt(i);
            if (c == '(') {
                opened.add(Integer.valueOf(i));
            } else if (c == ')' && !opened.isEmpty()) {
                opened.remove(opened.size() - 1);
            }
        }
        // Outward until one of them is a CALL's parenthesis. A redundant pair has
        // no name in front of it, and stopping at the innermost reported the
        // punctuation before it -- so `add('impl', ('g:a:1.7.22!!'))` found a
        // comma where the call should be and read the pin as nobody's argument.
        for (int k = opened.size() - 1; k >= 0; k--) {
            int before = skipBlanksBackward(line, opened.get(k).intValue() - 1);
            if (before >= 0 && isIdentifierChar(line.charAt(before))) {
                return before;
            }
        }
        // No parentheses anywhere, so this is Groovy's command syntax and the call
        // is the statement's first token -- unless the statement is an assignment,
        // in which case there is no call at all and the literal is just a value.
        // Without that, `def all = ['g:a:1.7.22!!']` read its own `def` as the
        // declaring call.
        for (int i = 0; i < at && i < line.length(); i++) {
            if (isLiteralStart(line, i)) {
                i = endOfStringLiteral(line, i);
                continue;
            }
            if (isAssignmentAt(line, i)) {
                return -1;
            }
        }
        int first = skipBlanks(line, 0);
        int end = first;
        while (end < line.length() && isIdentifierChar(line.charAt(end))) {
            end++;
        }
        return end > first ? end - 1 : -1;
    }

    /**
     * Whether the call ending at {@code at} is one that can declare a
     * dependency.
     *
     * <p>A configuration is never reached through a receiver -- an app writes
     * {@code implementation '..'} or {@code myCustomConfig '..'}, never
     * {@code project.implementation '..'} -- so an unqualified call declares.
     * The dependency handler is the exception, because {@code add} really is
     * called on it.</p>
     */
    private static boolean isDeclarationCall(String line, int at) {
        if (at < 0 || !isIdentifierChar(line.charAt(at))) {
            return false;
        }
        int i = at;
        while (i >= 0 && isIdentifierChar(line.charAt(i))) {
            i--;
        }
        int dot = skipBlanksBackward(line, i);
        if (dot < 0 || line.charAt(dot) != '.') {
            return true;
        }
        int end = skipBlanksBackward(line, dot - 1);
        if (end < 0) {
            return false;
        }
        int start = end;
        while (start >= 0 && (isIdentifierChar(line.charAt(start))
                || (line.charAt(start) == '.' && start > 0
                        && isIdentifierChar(line.charAt(start - 1))))) {
            start--;
        }
        return lastSegmentIs(line.substring(start + 1, end + 1), "dependencies");
    }

    /** Gradle's strict-version shorthand, written after the version. */
    private static final String STRICT_SUFFIX = "!!";

    /** Whether the statement names {@code kotlin-stdlib} and not a longer name. */
    private static boolean namesBaseStdlib(String line) {
        return namesArtifactAnywhere(line, BASE_STDLIB);
    }

    /**
     * Whether a string literal in this statement IS the dependency notation
     * for {@code artifact}, rather than merely mentioning it.
     *
     * <p>A coordinate lives inside a string, so "outside a string" cannot be
     * the test the way it is for {@code strictly} or a configuration name.
     * What separates the two is where in the string it sits:
     * {@code 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22'} opens with it,
     * while {@code because 'avoid org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'}
     * is prose that happens to contain it -- and reading that prose as a
     * declaration dropped the constraint for an artifact nobody had pinned.</p>
     *
     * <p>The artifact is matched exactly: {@code kotlin-stdlib} is a prefix of
     * {@code kotlin-stdlib-jdk8}, so what follows the name has to be the
     * version separator or the end of the literal.</p>
     */
    private static boolean namesCoordinate(String line, String artifact) {
        String coordinate = KOTLIN_GROUP + ":" + artifact;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (!isLiteralStart(line, i)) {
                continue;
            }
            // The shared rule. This was the last scanner still tracking a single
            // delimiter character of its own: it closed a triple-quoted literal on
            // the second of the three, then read the third as a new opener, so a
            // reason written '''...''' lost its `because` and was taken for the
            // declaration it was warning about.
            int end = endOfStringLiteral(line, i);
            String literal = stringLiteralContent(line, i);
            // Dependency notation carries no whitespace; a reason sentence
            // does. Without that, a reason that merely OPENS with the
            // coordinate -- because 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:
            // 1.7.22 causes duplicate classes' -- read as the declaration it
            // was warning about, and switched off the constraint that would
            // have prevented exactly what it describes.
            if ((literal.equals(coordinate)
                    || literal.startsWith(coordinate + ":"))
                    && !hasWhitespace(literal)
                    && !isReasonArgument(line, i)
                    && !isAssignedValue(line, i)) {
                return true;
            }
            i = end;
        }
        return false;
    }

    /**
     * Whether the literal opening at {@code quoteAt} is being assigned to
     * something rather than handed to a dependency.
     *
     * <p>{@code def legacy = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22!!'}
     * names the artifact and carries a strict marker, and on that basis alone
     * it suppressed the whole block -- for a value that is never added to any
     * configuration and decides nothing. A definition becomes a declaration
     * when it is USED, and by then the name has been inlined and the usage is
     * what this reads.</p>
     *
     * <p>Narrower than requiring a configuration, which was the other way to
     * fix this and would have undone something deliberate: a strict pin on a
     * variant configuration still shares a classpath with the one being
     * constrained, so it suppresses on purpose. The distinction here is
     * between a value and a call, not between one configuration and
     * another.</p>
     */
    private static boolean isAssignedValue(String line, int quoteAt) {
        int i = skipBlanksBackward(line, quoteAt - 1);
        if (i < 0) {
            return false;
        }
        char before = line.charAt(i);
        // A map ENTRY's value is not handed to a dependency either:
        //   def catalog = [legacy: 'org.jetbrains.kotlin:...:1.7.22!!']
        // is a map of strings, and reading its entry as a strict declaration
        // suppressed the block for something never added to a configuration. The
        // dependency map form -- group:, name:, version: -- is read by
        // declaresMapEntry, which does not come through here.
        //
        // Only the colon. A bracket and a comma both looked like they belonged in
        // this set and neither does: forcedModules = ['org.jetbrains.kotlin:...']
        // and dependencies.add('implementation', '...') each put a coordinate that
        // really does decide something directly after one, and excluding them
        // stopped a genuine force from being seen. So a bare list of coordinates
        // assigned to a variable nothing uses still suppresses -- the narrower
        // reading, and the one the tests hold.
        if (before == ':') {
            return true;
        }
        return before == '='
                && (i == 0 || line.charAt(i - 1) != '=')
                && (i + 1 >= line.length() || line.charAt(i + 1) != '=');
    }

    /**
     * Whether the token ending at {@code end} is a named argument's key.
     *
     * <p>Gradle's parenthesis-free map form puts two bare tokens in a row --
     * {@code implementation group: group, name: '...'} -- which is exactly what
     * a typed declaration looks like to a token counter. Read as one, it
     * "declared" a variable called group with no initialiser and cleared the
     * real binding of that name, so the strict declaration that used it later
     * was never matched.</p>
     */
    private static boolean followedByMapKeyColon(String statement, int end) {
        int at = skipBlanks(statement, end);
        return at < statement.length() && statement.charAt(at) == ':'
                && (at + 1 >= statement.length() || statement.charAt(at + 1) != ':');
    }

    /**
     * Whether the literal opening at {@code quoteAt} is the argument of a
     * reason rather than a dependency.
     *
     * <p>A reason is usually prose and the whitespace rule catches it, but a
     * reason can be nothing BUT a coordinate -- {@code because
     * 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'} names the artifact it
     * is warning about and has no whitespace at all. Read as a declaration, it
     * supplied a pre-merge version and suppressed the entire block: the
     * comment describing the duplicate switched off the constraint that
     * prevents it.</p>
     */
    private static boolean isReasonArgument(String line, int quoteAt) {
        int i = quoteAt - 1;
        while (i >= 0 && (isBlank(line.charAt(i)) || line.charAt(i) == '(')) {
            i--;
        }
        int end = i + 1;
        while (i >= 0 && isIdentifierChar(line.charAt(i))) {
            i--;
        }
        return end > i + 1 && BECAUSE.equals(line.substring(i + 1, end));
    }

    private static final String BECAUSE = "because";

    /**
     * The app's fragments as the one script they become.
     *
     * <p>They are separate build hints but the builder concatenates them into
     * a single generated {@code build.gradle}, so a {@code def} written in one
     * is in scope for the next. Reading them apart lost the definition at the
     * boundary and a strict pin behind it went unseen -- which is the failure
     * this must never produce, since a constraint cannot coexist with a strict
     * version.</p>
     */
    private static String combined(String[] appGradleFragments) {
        if (appGradleFragments == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < appGradleFragments.length; i++) {
            if (appGradleFragments[i] == null) {
                continue;
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(appGradleFragments[i]);
        }
        return out.toString();
    }

    /** Whether the text contains any whitespace. */
    private static boolean hasWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** The version inside this statement's {@code strictly} call, or null. */
    private static String strictVersionIn(String statement) {
        return versionInCall(statement, STRICTLY);
    }

    /**
     * The version a rich-version closure declares, whichever keyword carries
     * it.
     *
     * <p>{@code strictly} is the one that changes whether the constraints can
     * coexist with the app's, and {@code useVersion} rewrites what was
     * requested on the way through, so both are read.</p>
     *
     * <p>{@code require} is read too, because it OVERRIDES the coordinate:
     * {@code implementation('..jdk7:1.7.22') { version { require '1.9.22' } }}
     * resolves 1.9.22, and reading the coordinate there called a merged-era
     * declaration pre-merge. Reading it is not the same as treating it as a
     * pin -- see heldOnlyBySoftRequirement, which is where that distinction
     * lives.</p>
     *
     * <p>{@code prefer} is deliberately NOT read here. A preference is soft:
     * Gradle takes it only when nothing stronger is in play, so a transitive
     * requirement for a pre-merge shim beats it and the class-bearing jar wins
     * anyway. Treating a preference as proof the artifact cannot resolve below
     * the floor suppressed the constraint that was the only thing standing
     * between that graph and the duplicate.</p>
     */
    private static String richVersionIn(String statement) {
        String strict = versionInCall(statement, STRICTLY);
        if (strict != null) {
            return strict;
        }
        // A resolution rule's useVersion is as authoritative as either: it rewrites
        // what was requested, silently, on the way through.
        String ruled = versionInCall(statement, USE_VERSION);
        if (ruled != null) {
            return ruled;
        }
        return versionInCall(statement, "require");
    }

    private static final String USE_VERSION = "useVersion";

    /** The quoted argument of {@code call}, found outside string literals. */
    private static String versionInCall(String statement, String call) {
        List<String> found = versionsInCall(statement, call);
        if (found.isEmpty()) {
            return null;
        }
        // The LAST of them, when they run one after another. Every keyword this is
        // asked about -- strictly, require, useVersion -- SETS the constraint
        // rather than adding to it, so a closure that calls one twice keeps what it
        // was set to last. Reading the first reported 1.9.22 for
        // `strictly '1.9.22'; strictly '1.7.22'` and wrote the shim constraints
        // beside a pin that was really pre-merge.
        //
        // But a conditional makes them ALTERNATIVES rather than a sequence:
        // `if (legacy) strictly '1.7.22' else strictly '1.9.22'` sets one or the
        // other, and which one is not readable here. The lowest is the answer then,
        // for the reason every unevaluable branch gets it -- a pre-merge version
        // that may be the live one has to stand the block down.
        if (!containsAConditional(statement) && !holdsATernary(statement)) {
            return found.get(found.size() - 1);
        }
        String lowest = null;
        for (int i = 0; i < found.size(); i++) {
            lowest = lower(lowest, found.get(i));
        }
        return lowest;
    }

    /** Whether the statement chooses between branches this cannot evaluate. */
    private static boolean containsAConditional(String statement) {
        for (int i = 0; i < statement.length(); i++) {
            if (isLiteralStart(statement, i)) {
                i = endOfStringLiteral(statement, i);
                continue;
            }
            if (!isIdentifierChar(statement.charAt(i))
                    || (i > 0 && isIdentifierChar(statement.charAt(i - 1)))) {
                continue;
            }
            int end = i;
            while (end < statement.length() && isIdentifierChar(statement.charAt(end))) {
                end++;
            }
            String token = statement.substring(i, end);
            // A switch arm is an alternative like any other, and `case` alone is
            // enough to say so -- reading the last version kept whichever arm was
            // written last rather than whichever runs.
            if ("if".equals(token) || "else".equals(token)
                    || "switch".equals(token) || "case".equals(token)
                    || "default".equals(token)) {
                return true;
            }
            i = end - 1;
        }
        return false;
    }

    /** Whether a question mark outside a literal makes this an expression branch. */
    private static boolean holdsATernary(String statement) {
        for (int i = 0; i < statement.length(); i++) {
            if (isLiteralStart(statement, i)) {
                i = endOfStringLiteral(statement, i);
                continue;
            }
            // `legacy ? strictly('1.7.22') : strictly('1.9.22')` chooses between
            // them exactly as an if/else does, and so does the elvis form. Safe
            // navigation is the one question mark that branches nothing, and it is
            // the only one followed by a dot.
            if (statement.charAt(i) == '?'
                    && (i + 1 >= statement.length() || statement.charAt(i + 1) != '.')) {
                return true;
            }
        }
        return false;
    }

    /**
     * Every quoted argument of every syntactic {@code call} in the statement, in
     * source order.
     *
     * <p>One call may carry several -- {@code reject} takes varargs -- and the
     * call may be made more than once. The two are the same thing to a caller
     * that has to consider the arguments together, so they arrive as one list.</p>
     */
    private static List<String> versionsInCall(String statement, String call) {
        List<String> found = new ArrayList<String>();
        // The same syntax-level call callsStrictly validated, not any occurrence of
        // the word: a reason reading `because "strictly '1.7.22' is not intended"`
        // otherwise supplies the version for a declaration whose real strict version
        // is something else entirely, and the wrong one decides whether the block is
        // written.
        for (int i = 0; i < statement.length(); i++) {
            if (isLiteralStart(statement, i)) {
                i = endOfStringLiteral(statement, i);
                continue;
            }
            if (!statement.startsWith(call, i)) {
                continue;
            }
            boolean startsToken = i == 0 || !isIdentifierChar(statement.charAt(i - 1));
            if (!startsToken) {
                continue;
            }
            int after = skipBlanks(statement, i + call.length());
            if (after < statement.length() && statement.charAt(after) == '(') {
                after = skipBlanks(statement, after + 1);
            }
            while (after < statement.length() && isLiteralStart(statement, after)) {
                int end = endOfStringLiteral(statement, after);
                if (end >= statement.length()) {
                    break;
                }
                // The literal's own delimiters, however many it has. Written
                // strictly """1.7.22""", the one-per-side slice returned
                // ""1.7.22"" -- which parsed as no version at all and only
                // reached the right answer because an unreadable version counts
                // as below the floor. Correct by accident is not correct.
                found.add(stringLiteralContent(statement, after));
                after = skipBlanks(statement, end + 1);
                if (after >= statement.length() || statement.charAt(after) != ',') {
                    break;
                }
                after = skipBlanks(statement, after + 1);
            }
            // One BEFORE the next unread character, because the loop's own step
            // lands on it. Advancing straight to it skipped a character, and while
            // this returned on the first call that cost nothing -- now that it
            // keeps looking, it landed inside `strictly` and read the second call
            // of a repeated pair as ordinary text.
            i = after - 1;
        }
        return found;
    }

    /**
     * Whether a strict version is below the floor the shims depend on.
     *
     * <p>A prerelease of the floor is below it. {@code 1.8.0-RC2} is a
     * published Kotlin version, and its numeric part compares equal to
     * {@code 1.8.0} -- so without this it read as "at the floor" and the block
     * was written, whereupon the shims request the FINAL 1.8.0 and cannot
     * coexist with the app's strict prerelease. A qualifier on any other
     * version is ignored, because rounding {@code 1.9.22-RC} up to
     * {@code 1.9.22} keeps it above the floor either way.</p>
     *
     * <p>Unreadable counts as below, because the failure it guards against
     * cannot be worked around by the app while the duplicate class it risks
     * instead can.</p>
     */
    private static boolean belowTheFloor(String version) {
        if (version == null) {
            return true;
        }
        String selector = version.trim();
        if (selector.length() == 0) {
            return true;
        }
        // "Below the floor" means "cannot resolve to the floor or above", which is
        // not the same as "starts below it". A range [1.7.0,1.9.0) begins below and
        // still selects a merged-era shim, so our constraint intersects it rather
        // than conflicting; reading the lower endpoint suppressed the block for a
        // declaration Gradle would have satisfied. Each selector shape answers the
        // question its own way, which is why they are separated here rather than
        // funnelled through one bound.
        char opening = selector.charAt(0);
        if (opening == '[' || opening == '(' || opening == ']') {
            return rangeCannotReachTheFloor(selector);
        }
        int dynamic = selector.indexOf(".+");
        if (dynamic >= 0) {
            // 1.7.+ cannot leave 1.7, so it is below. 1.+ can reach 1.9, so it is not.
            String prefix = selector.substring(0, dynamic);
            return compareVersions(prefix,
                    truncatedToSameDepth(MERGED_STDLIB_FLOOR, prefix)) < 0;
        }
        if ("+".equals(selector) || selector.startsWith("latest.")) {
            // `+` and Gradle's status selectors -- latest.release, latest.integration
            // -- have no ceiling at all, so they can always select a merged-era shim.
            // Compared as a literal, latest.release parsed as zero and read as the
            // oldest version there is.
            return false;
        }
        return literalBelowTheFloor(selector);
    }

    /**
     * The index just past a type-argument list starting at {@code at}, or
     * {@code at} when there is not one there.
     *
     * <p>Only identifiers, dots, commas, wildcards and array brackets may
     * appear inside, and the angle brackets have to balance. A `&lt;` that is
     * really the comparison operator fails both tests, so it is left where it
     * is rather than swallowing the rest of the statement.</p>
     */
    private static int endOfTypeArguments(String statement, int at) {
        if (at >= statement.length() || statement.charAt(at) != '<') {
            return at;
        }
        int depth = 0;
        for (int i = at; i < statement.length(); i++) {
            char c = statement.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            } else if (!isIdentifierChar(c) && c != '.' && c != ',' && c != '?'
                    && c != '[' && c != ']' && !Character.isWhitespace(c)) {
                return at;
            }
        }
        return at;
    }

    /**
     * Where a declaration may start in this statement.
     *
     * <p>A block opener shares the statement with what it opens --
     * {@code if (cond) { String dep = '...'}} -- and the walk that separates a
     * declaration from an assignment begins at the first token, which there is
     * {@code if}. It stopped at the parenthesis and the declaration behind it
     * was never recorded, so the pin that declaration held went unseen. The
     * {@code def} spelling never had this because it is searched for anywhere
     * in the statement.</p>
     *
     * <p>Only a brace BEFORE the assignment counts. In {@code Closure c = { .. }}
     * the brace IS the value, and starting after it would skip the name being
     * assigned to -- which is a declaration this already reads.</p>
     */
    private static int afterAnyBlockOpener(String statement) {
        int start = 0;
        for (int i = 0; i < statement.length(); i++) {
            if (isLiteralStart(statement, i)) {
                i = endOfStringLiteral(statement, i);
                continue;
            }
            char c = statement.charAt(i);
            if (c == '{') {
                start = i + 1;
            } else if (isAssignmentAt(statement, i)) {
                break;
            }
        }
        return start;
    }

    /** Whether the character at {@code at} is an assignment, not a comparison. */
    private static boolean isAssignmentAt(String statement, int at) {
        if (statement.charAt(at) != '=') {
            return false;
        }
        if (at + 1 < statement.length() && statement.charAt(at + 1) == '=') {
            return false;
        }
        // `>=`, `!=`, `+=` and the rest end in the same character and none of them
        // opens a declaration, so a comparison in an `if` would otherwise stop the
        // search before the brace it guards.
        return at == 0 || "=!<>+-*/%&|^~".indexOf(statement.charAt(at - 1)) < 0;
    }

    /**
     * Whether a dotted Gradle path ends in the given segment.
     *
     * <p>`ext`, `project.ext` and `rootProject.ext` all name the one extra
     * properties extension, so the segment that owns the property is the last
     * one rather than the whole qualifier.</p>
     */
    private static boolean lastSegmentIs(String path, String segment) {
        return segment.equals(path.substring(path.lastIndexOf('.') + 1));
    }

    /**
     * Whether the statement enforces a Kotlin platform that cannot reach the
     * floor.
     *
     * <p>A version this cannot read counts as below it, the same way a
     * declaration's does: an enforced platform is strict by construction, so
     * guessing that it is high enough is guessing that the constraints below
     * will resolve.</p>
     */
    private static boolean namesAnEnforcedKotlinPlatformBelowTheFloor(String line) {
        // Any statement that builds one counts, including `def bom =
        // enforcedPlatform('..')` that is never added to a configuration.
        // Reported as too broad, and correctly on the Gradle fact: the object it
        // returns constrains nothing until something declares it.
        //
        // Acting on that needs to tell "stored and never added" from "stored and
        // added below", and the second is why anyone stores one. The definition
        // machinery records literals and maps, not call expressions, so the name
        // carries nothing: measured by excluding the assigned form, the
        //   def legacyBom = enforcedPlatform('..kotlin-bom:1.7.22')
        //   implementation(legacyBom)
        // pair stops standing the block down and the constraints go in against a
        // BOM that really is strict and really is pre-merge. That is a failed
        // resolution; the cost of the present reading is the duplicate an app
        // already had, in an app that went out of its way to name a pre-merge
        // Kotlin BOM and then not use it.
        //
        // Revisit together with recording call-expression values, not before:
        // the exclusion is only safe once the add site carries the platform.
        List<String> enforced = versionsInCall(line, ENFORCED_PLATFORM);
        for (int i = 0; i < enforced.size(); i++) {
            String coordinate = enforced.get(i).trim();
            if (!coordinate.startsWith(KOTLIN_GROUP + ":")) {
                continue;
            }
            int version = coordinate.indexOf(':', KOTLIN_GROUP.length() + 1);
            if (version < 0) {
                // No version at all, so nothing says it reaches the floor.
                return true;
            }
            if (belowTheFloor(withoutStrictSuffix(
                    versionComponentOf(coordinate.substring(version + 1))))) {
                return true;
            }
        }
        // A platform takes a dependency notation, and a map is one:
        // enforcedPlatform(group: '..', name: 'kotlin-bom', version: '1.7.22').
        // There is no literal following the call at all in that spelling, so the
        // scan above found nothing and the enforced pre-merge BOM read as absent.
        // The same two entries the map form of a declaration is read by.
        if (callsNamed(line, ENFORCED_PLATFORM)
                && declaresMapEntry(line, "group", KOTLIN_GROUP)) {
            return belowTheFloor(withoutStrictSuffix(mapEntryValue(line, "version")));
        }
        return false;
    }

    /** A version without the {@code !!} that makes it strict, if it carries one. */
    private static String withoutStrictSuffix(String version) {
        if (version != null && version.endsWith(STRICT_SUFFIX)) {
            return version.substring(0, version.length() - STRICT_SUFFIX.length());
        }
        return version;
    }

    /** The platform spelling whose managed versions become strict. */
    private static final String ENFORCED_PLATFORM = "enforcedPlatform";

    /**
     * Whether the statement names the plugin classpath outright.
     *
     * <p>{@code configurations.classpath} is the buildscript's own, and a
     * strategy on it governs which plugin jars load -- never the app's
     * dependencies, which is all this class writes to. It is the spelling that
     * works at the top level, where there is no {@code buildscript} block
     * around it to say the same thing.</p>
     *
     * <p>Every scan sees the result, not just the one that reads
     * {@code failOnVersionConflict}: a force or a strict pin on that
     * configuration cannot conflict with these constraints either, and reading
     * one as the app managing the family left a real duplicate unfixed.</p>
     */
    private static boolean namesTheBuildscriptClasspath(String line) {
        return "classpath".equals(configurationNamedIn(line));
    }

    /**
     * The single configuration this statement names, or null when it names none
     * or cannot say which.
     *
     * <p>Every spelling goes through the same reading, because they are the same
     * question: {@code configurations.classpath},
     * {@code configurations['classpath']} and
     * {@code configurations.getByName('classpath')} all name one configuration,
     * and only the first was recognised -- so a plugin-classpath force written
     * either of the other two ways read as the app managing the family and stood
     * the whole block down.</p>
     *
     * <p>Null when a closure decides which configurations are meant:
     * {@code configurations.all { }}, {@code configureEach}, and
     * {@code matching { }} may all include the one being constrained, and no
     * name is available to say. That falls out of the syntax rather than a list
     * -- a lookup takes a string, a filter takes a closure -- so a selector
     * nobody anticipated reads as "cannot say", which is the answer that keeps
     * the constraint out of a graph that would fail on it.</p>
     */
    private static String configurationNamedIn(String line) {
        int at = -1;
        for (int i = 0; i < line.length(); i++) {
            if (isLiteralStart(line, i)) {
                i = endOfStringLiteral(line, i);
                continue;
            }
            int after = i + CONFIGURATIONS.length();
            if (line.startsWith(CONFIGURATIONS, i)
                    && (i == 0 || !isIdentifierChar(line.charAt(i - 1)))
                    && (after >= line.length()
                            || !isIdentifierChar(line.charAt(after)))) {
                at = after;
                break;
            }
        }
        if (at < 0) {
            return null;
        }
        int next = skipBlanks(line, at);
        if (next < line.length() && line.charAt(next) == '[') {
            return literalAfter(line, next + 1);
        }
        if (next >= line.length() || line.charAt(next) != '.') {
            return null;
        }
        int start = skipBlanks(line, next + 1);
        int end = start;
        while (end < line.length() && isIdentifierChar(line.charAt(end))) {
            end++;
        }
        if (end == start) {
            return null;
        }
        int after = skipBlanks(line, end);
        if (after < line.length() && line.charAt(after) == '(') {
            // A lookup carries the name as a string; a filter carries a closure
            // and says nothing about which configurations it will match.
            return literalAfter(line, after + 1);
        }
        if (after < line.length() && line.charAt(after) == '{') {
            return null;
        }
        return line.substring(start, end);
    }

    /** The content of the string literal starting at or after {@code from}. */
    private static String literalAfter(String line, int from) {
        int at = skipBlanks(line, from);
        if (at >= line.length() || !isLiteralStart(line, at)) {
            return null;
        }
        return stringLiteralContent(line, at);
    }

    private static final String BUILDSCRIPT_CLASSPATH = "configurations.classpath";

    /** Whether the statement rejects the floor and everything past it. */
    private static boolean rejectsTheFloor(String line) {
        if (callsNamed(line, "rejectAll")) {
            return true;
        }
        // Rejections ACCUMULATE -- reject takes varargs and may be called again --
        // and Gradle applies every one of them, so every selector is asked, not
        // just the first. Read one at a time, a pair that jointly removes the floor
        // looked harmless twice over.
        //
        // The question asked of each is only whether it removes the floor ITSELF.
        // It once also demanded that everything past the floor be gone, on the
        // reasoning that a higher version was still selectable -- but what this
        // block writes is a constraint on exactly 1.8.0, so the floor is the only
        // version whose availability it depends on. An app that rejects 1.8.0 has
        // said it does not want the version this pins to, which is the whole
        // signal this scan exists to read, and writing the constraint anyway asks
        // its graph to resolve to a version it excluded.
        List<String> rejected = versionsInCall(line, "reject");
        for (int i = 0; i < rejected.size(); i++) {
            if (rejectionRemovesTheFloor(rejected.get(i).trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether one rejection selector removes the floor version itself.
     *
     * <p>A prerelease of the floor is a different version from the floor, so
     * rejecting {@code 1.8.0-RC2} does not reject {@code 1.8.0}.</p>
     */
    private static boolean rejectionRemovesTheFloor(String selector) {
        if (selector.length() == 0) {
            return false;
        }
        char opening = selector.charAt(0);
        if (opening != '[' && opening != '(' && opening != ']') {
            // A plain version rejects exactly itself.
            return isTheFloor(selector);
        }
        int comma = selector.indexOf(',');
        if (comma < 0) {
            // [1.8.0] is an exact version written as a range.
            return isTheFloor(selector.substring(1,
                    Math.max(1, selector.length() - 1)).trim());
        }
        char closing = selector.charAt(selector.length() - 1);
        boolean excludesLower = opening == '(' || opening == ']';
        boolean excludesUpper = closing == ')' || closing == '[';
        String lower = selector.substring(1, comma).trim();
        if (lower.length() != 0) {
            int compared = compareVersions(lower, MERGED_STDLIB_FLOOR);
            if (compared > 0 || (compared == 0 && excludesLower)) {
                return false;
            }
        }
        String upper = selector.substring(comma + 1,
                Math.max(comma + 1, selector.length() - 1)).trim();
        if (upper.length() != 0) {
            int compared = compareVersions(upper, MERGED_STDLIB_FLOOR);
            if (compared < 0 || (compared == 0 && excludesUpper)) {
                return false;
            }
        }
        return true;
    }

    /** Whether a plain version literal IS the floor, prerelease and all. */
    private static boolean isTheFloor(String version) {
        return version.length() != 0
                && compareVersions(version, MERGED_STDLIB_FLOOR) == 0
                && !literalBelowTheFloor(version);
    }

    /** Whether a range excludes every version at or above the floor. */
    private static boolean rangeCannotReachTheFloor(String selector) {
        int comma = selector.indexOf(',');
        if (comma < 0) {
            // [1.8.0] is an exact version written as a range.
            String exact = selector.substring(1,
                    Math.max(1, selector.length() - 1)).trim();
            return exact.length() == 0 || literalBelowTheFloor(exact);
        }
        String upper = selector.substring(comma + 1,
                Math.max(comma + 1, selector.length() - 1)).trim();
        if (upper.length() == 0) {
            // [1.7.0,) has no ceiling at all.
            return false;
        }
        char closing = selector.charAt(selector.length() - 1);
        if (closing == ')' || closing == '[') {
            // Excluding its bound, the range stops short of it: at or below the floor
            // numerically means nothing at or above the floor is selectable.
            return compareVersions(upper, MERGED_STDLIB_FLOOR) <= 0;
        }
        // Including it, the bound itself is selectable -- so the question is exactly
        // the one asked of a plain version, prerelease and all. Comparing numerically
        // here read [1.7.0,1.8.0-RC2] as reaching the floor, when a release candidate
        // of it is below it and the constraint had nothing to resolve to.
        return literalBelowTheFloor(upper);
    }

    /** A plain version, with a prerelease at the floor counting as below it. */
    private static boolean literalBelowTheFloor(String version) {
        int compared = compareVersions(version, MERGED_STDLIB_FLOOR);
        if (compared != 0) {
            return compared < 0;
        }
        // At the floor numerically, only a PRERELEASE is below it. A dynamic marker
        // is not: 1.8.+ cannot resolve lower than 1.8.0, so it is at the floor and
        // the constraints are still satisfiable.
        return isPrerelease(version);
    }

    /** {@code version} cut to as many components as {@code sample} has. */
    private static String truncatedToSameDepth(String version, String sample) {
        int depth = 1;
        for (int i = 0; i < sample.length(); i++) {
            if (sample.charAt(i) == '.') {
                depth++;
            }
        }
        StringBuilder out = new StringBuilder();
        int seen = 0;
        for (int i = 0; i < version.length() && seen < depth; i++) {
            char c = version.charAt(i);
            if (c == '.') {
                seen++;
                if (seen >= depth) {
                    break;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * The lowest version a selector can resolve to, as far as the text says.
     *
     * <p>Gradle accepts more than a literal here, and each shape was read as
     * zero before: {@code [1.8.0]} is an exact range whose bracket stopped the
     * numeric parse, {@code [1.7.0,1.9.0)} is a range whose LOW end is what
     * matters for this question, and {@code 1.8.+} is a dynamic selector that
     * cannot go below 1.8.0. Reading any of them as zero classified a
     * merged-era declaration as pre-merge and dropped both constraints,
     * including the sibling's -- which is the one such a graph still needs.</p>
     */
    /**
     * Whether this version is a prerelease of its own numeric version, as
     * opposed to a dynamic selector. {@code 1.8.0-RC2} sorts below
     * {@code 1.8.0}; {@code 1.8.+} does not.
     */
    private static boolean isPrerelease(String version) {
        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);
            if (c == '.' || Character.isDigit(c)) {
                continue;
            }
            return c != '+';
        }
        return false;
    }

    /** Numeric dotted version compare; a missing segment counts as zero. */
    private static int compareVersions(String left, String right) {
        String[] l = left.split("\\.");
        String[] r = right.split("\\.");
        int len = Math.min(l.length, r.length);
        for (int i = 0; i < len; i++) {
            int a = parseSegment(l[i]);
            int b = parseSegment(r[i]);
            if (a != b) {
                return a < b ? -1 : 1;
            }
        }
        // Equal as far as both go, so the SHORTER one is lower. Gradle orders
        // `1.8` below `1.8.0`, and padding the missing segment with zero called
        // them equal -- so a strict `[1.7,1.8]` looked like it admitted the floor
        // when it stops just short of it, and the constraints went into a graph
        // that cannot resolve them.
        if (l.length != r.length) {
            return l.length < r.length ? -1 : 1;
        }
        return 0;
    }

    /**
     * A version segment's leading digits. {@code 20-RC} is 20, not zero:
     * reading it as zero made {@code 1.8.20-RC} compare equal to the 1.8.0
     * floor, and the qualifier rule then classified a version well ABOVE the
     * floor as below it, suppressing an alignment that was needed.
     */
    private static int parseSegment(String segment) {
        int to = 0;
        while (to < segment.length() && Character.isDigit(segment.charAt(to))) {
            to++;
        }
        if (to == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(segment.substring(0, to));
        } catch (NumberFormatException tooLong) {
            return 0;
        }
    }

    /**
     * Whether the app actively declares this {@code org.jetbrains.kotlin}
     * artifact, rather than merely mentioning its name somewhere in a Gradle
     * fragment.
     *
     * <p>The difference is the whole point, because both near misses produce
     * the failure this class exists to prevent -- suppressing the constraint
     * for an app that never pinned anything:</p>
     *
     * <pre>
     * // implementation platform('org.jetbrains.kotlin:kotlin-bom:1.9.22')
     * exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk8'
     * </pre>
     *
     * <p>The first is not a declaration at all. The second is the opposite of
     * one: a Gradle exclusion applies only to the dependency edge it is
     * written on, so an independent path can still bring the class-bearing jar
     * it names. Neither may switch the alignment off.</p>
     *
     * <p>Two spellings count as a declaration -- the colon-joined coordinate
     * and the map form -- because those are what a pin is actually written as.
     * A declaration inside {@code if (project.hasProperty('x'))} counts as
     * present, deliberately: whether it is in force is decided by Gradle at
     * evaluation time and cannot be read out of the text. Treating it as
     * present honours the documented promise at the cost of leaving a
     * duplicate the app already had; the alternative -- suppressing only on a
     * strict version, which is the one declaration a constraint cannot coexist
     * with -- removes that hazard along with everything else in this method,
     * and is a documented behaviour change rather than a bug fix, so it is a
     * decision for the project rather than something to slip in under a review
     * thread.
     * Anything else falls through to "not declared", which is the safe
     * direction: emitting a constraint the app did not need only raises an
     * artifact to a shim, while skipping one it did need fails the build.</p>
     */
    private static boolean declaresArtifact(String artifact, String configuration,
            String[] appGradleFragments) {
        String[] lines = activeLines(combined(appGradleFragments));
        {
            for (int j = 0; j < lines.length; j++) {
                if (declaresArtifactOnLine(artifact, configuration, lines[j])) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean declaresArtifactOnLine(String artifact, String configuration,
            String line) {
        // A strict version is honoured wherever it is declared, because a constraint
        // cannot coexist with one on any classpath both reach: measured, an app
        // strictly pinning jdk8 to 1.7.22 resolves fine on its own and fails outright
        // with this block's constraint added --
        //   Could not resolve org.jetbrains.kotlin:kotlin-stdlib-jdk8:{strictly 1.7.22}
        // That is worse than the duplicate class, because the app cannot work around
        // it, so a strict pin ends the question regardless of which configuration
        // carries it. A strict version at or above the floor loses nothing by this:
        // it is already a shim.
        //
        // Reviewed twice as too broad -- a strict pin on debugImplementation or
        // compileOnly does not manage releaseRuntimeClasspath, so suppressing the
        // whole artifact leaves the release graph unaligned. That is true, and it is
        // still the better of the two outcomes, because the constraint this block
        // writes is NOT release-scoped: it is declared on `implementation`, which
        // every variant inherits. There is no version of "constrain release but not
        // debug" available from one implementation constraint. So the choice for an
        // app with a strict pre-1.8 pin on a non-release configuration is:
        //   suppress   -- release keeps a duplicate it already had before this change
        //   emit       -- debug stops resolving, which it did fine before this change
        // The second breaks a build that works today, and this class has taken the
        // first everywhere else it has had to choose. Scoping the constraint to
        // `releaseImplementation` would satisfy both, and is deliberately not done:
        // naming a variant configuration that a given build type set may not have
        // fails the whole script at evaluation, which is a far larger blast radius
        // than the case it fixes. Revisit only with a project that actually has this
        // shape.
        //
        // Reviewed a third time with a sharper argument: a pin on a DETACHED
        // configuration -- annotationProcessor, kapt, ksp -- genuinely cannot
        // conflict, because unlike the variant configurations those do not extend
        // implementation, so suppressing on one leaves the release runtime graph
        // unaligned for nothing. The Gradle fact is right. What it asks for is not
        // available here: acting on it means deciding from a configuration's NAME
        // whether it shares a classpath with the one being constrained, and
        //   - Android synthesises a configuration per build type and flavour, so the
        //     names are open-ended: debugAnnotationProcessor, freeReleaseImplementation,
        //     and whatever the next plugin adds,
        //   - "does not extend implementation" is not the same question as "cannot
        //     conflict": compileOnly does not extend it either, yet compileClasspath
        //     extends both, so a strict pin there does conflict.
        // A name list that gets this wrong is not wrong symmetrically. Classifying a
        // conflicting configuration as detached emits the constraint beside a live
        // strict pin, which is measured to fail resolution outright -- and for the
        // `1.7.22!!` spelling to resolve quietly to the empty shims and throw
        // NoClassDefFoundError on the device instead. Classifying a detached one as
        // conflicting costs an app that had already pinned the family the duplicate
        // it already had. So this stays until the classification can be read from
        // something better than a name.
        // A soft requirement is not management, in either direction. The
        // constraint RAISES it -- `version { require '1.7.22' }` and a floor of
        // 1.8.0 resolve to 1.8.0 with no conflict -- so treating one as a pin
        // stood the block down for a shim this could have fixed, and treating it
        // as a declaration skipped that shim's constraint and left it pre-merge
        // beside a merged-era base. Both are the duplicate this exists to
        // prevent, kept rather than removed.
        //
        // Only when nothing else holds the artifact: a strictly, a force, a
        // rejection or the `!!` suffix on the requirement itself all pin, and a
        // coordinate that carries its own version is the app's chosen version,
        // whose measured behaviour is what the comment above describes.
        if (heldOnlyBySoftRequirement(line, artifact)) {
            return false;
        }
        if (!holdsStrictly(line, artifact)
                && !declaresOnTheConstrainedConfiguration(configuration, line)) {
            return false;
        }
        if (!holdsStrictly(line, artifact) && !bindsAVersion(line, artifact)) {
            // A declaration that pins nothing cannot stand in for the constraint. The
            // clearest case is a lone preference: our floor overrides it, so emitting
            // is harmless, while suppressing leaves a transitive pre-merge shim free
            // to win. A declaration with no version at all is the same argument.
            //
            // A STRICT pin is exempt, readable or not. `strictly kotlinVersion` takes
            // its version from a property this cannot evaluate, and reading that as
            // "binds nothing" emitted the constraint beside a pin that may well be
            // pre-merge -- the one direction that fails at runtime rather than in the
            // build. Unreadable falls back to the conservative answer here for the
            // same reason it does in belowTheFloor.
            return false;
        }
        // The same three spellings namesArtifactAnywhere reads, because there were
        // two lists and they diverged: this one knew the coordinate and the
        // group/name map, and not the bare name a resolution rule compares. So a
        // `force` naming a shim by coordinate stood the block down while a
        // `useVersion` holding the SAME shim at the same version did not -- and the
        // constraints went in beside a rule that keeps jdk8 pre-merge, raising jdk7
        // to its empty 1.8.0 shim around it. The base library had a scan of its own
        // and was never exposed to this, which is why it read as correct.
        return namesArtifactAnywhere(line, artifact);
    }

    /**
     * Whether the statement calls Gradle's {@code strictly}, as opposed to
     * merely containing the English word.
     *
     * <p>{@code because 'not strictly required outside debug'} is a reason
     * string, not a version constraint, and reading it as one let a
     * variant-only dependency switch the alignment off for the release build.
     * The discriminator is the one already used for comment delimiters and
     * statement separators: inside a string it is prose, outside it is
     * syntax.</p>
     */
    /**
     * Whether the statement calls Gradle's {@code force}.
     *
     * <p>A force is as absolute as a strict pin and worse to get wrong. It
     * does not conflict with a constraint, it silently wins: with
     * {@code resolutionStrategy.force 'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'}
     * the base library stays pre-merge while these constraints raise the shims
     * to their EMPTY 1.8.0 jars, so the jdk7/jdk8 classes end up in no selected
     * jar at all. Nothing fails in the build; it throws on the device. So a
     * forced version is read exactly like a strict one.</p>
     */
    private static boolean callsForce(String statement, String artifact) {
        // The method forms, which callsNamed now distinguishes from an assignment.
        // Gradle's ways of overriding a selected version: force and its setter, a
        // resolution rule's useVersion (a bare version) or useTarget (a whole
        // coordinate), and a dependency substitution. All of them win silently over
        // a constraint.
        //
        // An override is read as applying to any artifact the statement names, and
        // NOT bound to the branch that names it. Reported as imprecise, correctly: a
        // rule whose branches handle several Kotlin modules can override a sibling
        // while merely mentioning this one, and the block then stands down for an
        // artifact nobody managed.
        //
        // Binding the call to its predicate means modelling branches -- which
        // condition governs which statement -- and this class deliberately has no
        // such model. Everywhere a branch cannot be evaluated it resolves the
        // ambiguity toward suppression, for the reason written on the conditional
        // assignment path: emitting beside an override this could not see is the
        // failure that reaches the device, while suppressing costs an app the
        // duplicate it already had, which fails in checkDuplicateClasses where it
        // already was.
        //
        // The trade is the same here and the asymmetry is sharper, because a branch
        // model has far more spellings to get wrong than a version range -- and this
        // rule's narrowings have needed correcting in three consecutive commits, each
        // time for a spelling that looked handled. Revisit with a real project whose
        // rule branches this way, and a way to test the branch reading that does not
        // rest on the same reasoning that keeps being wrong.
        //
        // A substitution names TWO coordinates, which is why it was left out once:
        // the version scan takes the first literal, and that is the side being
        // REPLACED. The scan reads from after `using` now, so it takes the
        // replacement -- which is also the only side that carries a version in the
        // ordinary spelling, `substitute module('g:a') using module('g:a:1.7.22')`.
        if (callsNamed(statement, "force") || callsNamed(statement, "setForcedModules")
                || callsNamed(statement, USE_VERSION)
                || callsNamed(statement, "useTarget")) {
            return true;
        }
        if (callsNamed(statement, "substitute")) {
            // A substitution overrides only what it substitutes AWAY from. With the
            // artifact as the TARGET -- substitute module('com.example:source')
            // using module('...:kotlin-stdlib:1.7.22') -- the replacement is still
            // subject to ordinary conflict resolution, so an existing 1.8.22
            // requirement raises it and nothing is pinned; reading that as absolute
            // suppressed the block for a graph that had not been pinned at all.
            int using = afterCall(statement, "using");
            String replaced = using < 0 ? statement : statement.substring(0, using);
            return namesArtifactAnywhere(replaced, artifact);
        }
        // forcedModules is only ever written as an assignment, and assigning it any
        // module list is a force. `force` as a property is the one that has to be
        // read: `{ force = false }` explicitly turns forcing OFF, and accepting any
        // assignment after the word read that as an absolute pin -- suppressing the
        // block for a declaration that was asking for nothing of the kind.
        if (assignedValue(statement, "forcedModules") != null) {
            return true;
        }
        return "true".equals(assignedValue(statement, "force"));
    }

    /**
     * The value assigned to {@code name}, or null if it is not assigned here.
     * Read outside literals, like every other question about syntax.
     */
    private static String assignedValue(String statement, String name) {
        for (int i = 0; i < statement.length(); i++) {
            if (isLiteralStart(statement, i)) {
                i = endOfStringLiteral(statement, i);
                continue;
            }
            if (!statement.startsWith(name, i)) {
                continue;
            }
            boolean startsToken = i == 0 || !isIdentifierChar(statement.charAt(i - 1));
            int after = i + name.length();
            if (!startsToken || (after < statement.length()
                    && isIdentifierChar(statement.charAt(after)))) {
                continue;
            }
            int at = skipBlanks(statement, after);
            // += assigns too. forcedModules += ['...'] applies the force just as
            // forcedModules = ['...'] does, and requiring the bare = missed it.
            if (at + 1 < statement.length() && statement.charAt(at) == '+'
                    && statement.charAt(at + 1) == '=') {
                at++;
            }
            if (at >= statement.length() || statement.charAt(at) != '='
                    || (at + 1 < statement.length() && statement.charAt(at + 1) == '=')) {
                continue;
            }
            int from = skipBlanks(statement, at + 1);
            int to = from;
            while (to < statement.length() && !isBlank(statement.charAt(to))) {
                to++;
            }
            return statement.substring(from, to);
        }
        return null;
    }

    private static boolean callsStrictly(String statement) {
        return callsNamed(statement, STRICTLY);
    }

    /**
     * Whether {@code call} appears as a call, rather than inside a literal.
     *
     * <p>{@code assigned} also accepts the property form, {@code name = ...},
     * which only Gradle's forcedModules is written as. It is not offered to
     * every caller because `def strictly = false` is not a strict pin.</p>
     */
    /** Where {@code call}'s arguments begin, or -1 if it is not called here. */
    private static int afterCall(String statement, String call) {
        for (int i = 0; i < statement.length(); i++) {
            if (isLiteralStart(statement, i)) {
                i = endOfStringLiteral(statement, i);
                continue;
            }
            if (!statement.startsWith(call, i)) {
                continue;
            }
            boolean startsToken = i == 0 || !isIdentifierChar(statement.charAt(i - 1));
            int after = i + call.length();
            if (startsToken && after < statement.length()
                    && (isBlank(statement.charAt(after))
                            || statement.charAt(after) == '(')) {
                return after;
            }
        }
        return -1;
    }

    private static boolean callsNamed(String statement, String call) {
        for (int i = 0; i < statement.length(); i++) {
            if (isLiteralStart(statement, i)) {
                i = endOfStringLiteral(statement, i);
                continue;
            }
            if (!statement.startsWith(call, i)) {
                continue;
            }
            boolean startsToken = i == 0 || !isIdentifierChar(statement.charAt(i - 1));
            int after = i + call.length();
            if (!startsToken || after >= statement.length()) {
                continue;
            }
            char next = statement.charAt(after);
            if (next != '(' && !isBlank(next)) {
                continue;
            }
            // `force = false` is a property being SET, not a call, and reading it as
            // one turned an explicit "do not force" into an absolute pin. A call is
            // what is left after excluding the assignment.
            int assignment = skipBlanks(statement, after);
            if (assignment < statement.length() && statement.charAt(assignment) == '='
                    && (assignment + 1 >= statement.length()
                            || statement.charAt(assignment + 1) != '=')) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static final String STRICTLY = "strictly";

    /**
     * Whether the statement carries the Groovy map entry
     * {@code key: 'value'}, with whatever spacing the author used.
     *
     * <p>{@code name : 'kotlin-stdlib-jdk8'} is as valid as
     * {@code name: 'kotlin-stdlib-jdk8'}, and matching the exact substring
     * missed it -- which matters because the same declaration can carry a
     * strict version, and missing it turns this class's constraint into a
     * failed resolution.</p>
     */
    private static boolean declaresMapEntry(String line, String key, String value) {
        String found = mapEntryValue(line, key);
        return found != null && found.equals(value);
    }

    /**
     * Whether this character can be part of a Groovy identifier.
     *
     * <p>Not {@code isLetterOrDigit}: an underscore is neither, so a
     * configuration called {@code custom_implementation} ended its embedded
     * {@code implementation} on a boundary that looked clean and was read as
     * the main configuration -- suppressing a constraint for a configuration
     * that reaches nothing.</p>
     */
    /**
     * Whether the character is whitespace that separates tokens.
     *
     * <p>Spelled out as space-or-tab in two places, which meant a fragment with
     * Windows line endings put a carriage return after {@code strictly} and the
     * call stopped being a call. Line endings are not this class's business to
     * have an opinion about.</p>
     */
    private static boolean isBlank(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    private static boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    /**
     * The index of the quote closing the literal that opens at
     * {@code quoteAt}, or the text length when nothing closes it.
     *
     * <p>One implementation because there were several, and they drifted. Each
     * scanner in this class had its own copy of "walk to the closing quote",
     * some honouring backslash escapes and some not, and every divergence
     * turned into a defect: a statement scanner that stopped at the apostrophe
     * inside {@code 'can\'t'} merged statements that must stay apart, and a
     * brace counter that did the same swallowed a declaration's closing brace.
     * They call this now, so a fix reaches all of them.</p>
     */
    /**
     * The content of the literal opening at {@code quoteAt}, without its
     * delimiters.
     *
     * <p>Stripping one character from each end is wrong for a triple-quoted
     * literal, and every caller was doing exactly that: a coordinate written
     * with the long delimiter came back still wearing two quotes at each end,
     * so it had no readable version and the declaration was classified
     * pre-merge -- taking the whole block with it.</p>
     */
    private static String stringLiteralContent(String text, int quoteAt) {
        int end = endOfStringLiteral(text, quoteAt);
        int delimiter = delimiterLength(text, quoteAt);
        int from = Math.min(quoteAt + delimiter, text.length());
        int to = Math.max(from, Math.min(end + 1 - delimiter, text.length()));
        return text.substring(from, to);
    }

    /**
     * Whether a string literal opens at {@code at}, in any spelling Groovy has
     * for one.
     *
     * <p>This question is asked in eleven places, and the answer used to be
     * spelled out at each of them as "a quote is here". Every literal form
     * added since arrived as a review comment against one of those eleven --
     * triple quotes, then dollar-slashy in the comment scanner, then
     * dollar-slashy in the statement scanner, then dollar-slashy in the
     * coordinate matcher -- because teaching one site never taught the rest.
     * The form belongs here, once, where every scanner reads it.</p>
     */
    private static boolean isLiteralStart(String text, int at) {
        char c = text.charAt(at);
        if (c == '\'' || c == '"') {
            return true;
        }
        if (c == '$') {
            return at + 1 < text.length() && text.charAt(at + 1) == '/';
        }
        return c == '/' && opensASlashyLiteral(text, at);
    }

    /**
     * Whether a {@code /} at {@code at} opens a slashy literal rather than
     * dividing or opening a comment.
     *
     * <p>Declined once, on the grounds that telling these apart needs to know
     * whether an expression is expected here, which is parsing rather than
     * scanning. That was raised again with a better argument: NOT recognizing
     * the literal fails in the SAME direction as recognizing one that is not
     * there -- an apostrophe inside {@code /can't/} puts the quote scanner out
     * of step and hides whatever follows, exactly as swallowing a division
     * would. Given both mistakes cost the same, the question is only which is
     * likelier, and that is decidable: a literal can only open where an
     * expression may begin. After an identifier, a number or a closing
     * bracket -- which is every division a build script actually contains,
     * {@code total / 2}, {@code (a + b) / 2} -- it is division. The two
     * comment openers are excluded outright.</p>
     */
    private static boolean opensASlashyLiteral(String text, int at) {
        if (at + 1 < text.length()
                && (text.charAt(at + 1) == '/' || text.charAt(at + 1) == '*')) {
            return false;
        }
        int i = skipBlanksBackward(text, at - 1);
        if (i < 0) {
            return true;
        }
        // Asked the other way round, because asking it directly does not converge.
        // "Which characters may an expression follow" was extended by review four
        // times -- the closure arrow, the comparison, then Groovy's =~ and ==~ --
        // and each time the set was still missing whichever operator came next.
        // Division is the closed half: it needs a VALUE on its left, and there are
        // only so many things a value ends with. Everything else opens a literal,
        // including every operator nobody has thought of yet.
        char previous = text.charAt(i);
        if (previous == ')' || previous == ']' || previous == '}') {
            return false;
        }
        if (previous == '\'' || previous == '"') {
            return false;
        }
        if (previous >= '0' && previous <= '9') {
            return false;
        }
        if ((previous == '+' || previous == '-') && i > 0
                && text.charAt(i - 1) == previous) {
            // a++ / b and a-- / b: the increment yields the value being divided.
            return false;
        }
        if (!isIdentifierChar(previous)) {
            return true;
        }
        // A word: a variable is a value and a keyword is not, which is the whole
        // difference between `total / 2` and `return /can't/`.
        int tokenEnd = i + 1;
        while (i >= 0 && isIdentifierChar(text.charAt(i))) {
            i--;
        }
        String token = text.substring(i + 1, tokenEnd);
        return EXPRESSION_KEYWORDS.indexOf(" " + token + " ") >= 0;
    }

    /**
     * Groovy words after which an expression begins, so a slash is a literal
     * rather than a division. Reserved words cannot be variables, which is why
     * this can be read off the language rather than guessed at.
     */
    private static final String EXPRESSION_KEYWORDS =
            " return new in case else do while if throw assert yield instanceof ";

    /** The length of the delimiter opening at {@code at}. */
    private static int delimiterLength(String text, int quoteAt) {
        char quote = text.charAt(quoteAt);
        if (quote == '$') {
            return 2;
        }
        if (quote == '/') {
            return 1;
        }
        return quoteAt + 2 < text.length()
                && text.charAt(quoteAt + 1) == quote
                && text.charAt(quoteAt + 2) == quote ? 3 : 1;
    }

    private static int endOfStringLiteral(String text, int quoteAt) {
        char quote = text.charAt(quoteAt);
        if (quote == '$') {
            // $/ ... /$ -- the closer is two characters, and the content may hold
            // anything at all, which is the point of the form. Almost anything: the
            // dollar escapes itself and a slash, so $$ is a dollar and $/ is a
            // slash. Searching for the first "/$" substring found the slash of an
            // escaped $/ instead of the closer and ended the literal early, which
            // put the scanner back into code halfway through a string.
            for (int i = quoteAt + 2; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '$' && i + 1 < text.length()
                        && (text.charAt(i + 1) == '$' || text.charAt(i + 1) == '/')) {
                    i++;
                    continue;
                }
                if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '$') {
                    return i + 1;
                }
            }
            return text.length();
        }
        if (quote == '/') {
            // Groovy's slashy literals MAY span lines, so the closing slash is looked
            // for across them -- but only a literal that actually closes gets to. An
            // opener this misread, with no closing slash anywhere, would otherwise
            // swallow every statement after it, and a suppression reached that way is
            // the outcome this class must never produce. So: close where it closes,
            // and failing that, stop at the line it started on.
            int firstNewline = -1;
            for (int i = quoteAt + 1; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\\') {
                    i++;
                } else if (c == '/') {
                    return i;
                } else if (c == '\n' && firstNewline < 0) {
                    firstNewline = i;
                }
            }
            return firstNewline < 0 ? text.length() : firstNewline - 1;
        }
        // Groovy's triple-quoted literals are a different delimiter, not three of
        // this one. Treating the opener as a single quote made a triple-quoted note
        // close on the first apostrophe it contains -- can't, in the case that found
        // this -- and threw the rest of the fragment out of step, so a strict pin
        // after it was never seen.
        boolean tripled = quoteAt + 2 < text.length()
                && text.charAt(quoteAt + 1) == quote
                && text.charAt(quoteAt + 2) == quote;
        if (tripled) {
            for (int i = quoteAt + 3; i + 2 < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\\') {
                    i++;
                } else if (c == quote && text.charAt(i + 1) == quote
                        && text.charAt(i + 2) == quote) {
                    return i + 2;
                }
            }
            return text.length();
        }
        for (int i = quoteAt + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == quote) {
                return i;
            }
        }
        return text.length();
    }

    /**
     * The nearest index at or before {@code from} that is not whitespace, or
     * -1. The backward half of skipBlanks, and shared for the same reason: it
     * had been written out four times, three of them stopping at a space or a
     * tab, so a fragment with Windows line endings put a carriage return where
     * one of them was looking and the token behind it stopped being found.
     */
    private static int skipBlanksBackward(String text, int from) {
        int i = from;
        while (i >= 0 && isBlank(text.charAt(i))) {
            i--;
        }
        return i;
    }

    private static int skipBlanks(String line, int from) {
        int i = from;
        // isBlank, not a second opinion about what whitespace is. Spelled out as
        // space-or-tab here while the call detector had already learned about line
        // endings, so a CRLF fragment that split a map entry after its colon --
        //   implementation(group:
        //           'org.jetbrains.kotlin', ...
        // -- found no value at all, and the strict pin in that declaration went
        // unread. A statement can legitimately contain a newline; the splitter has
        // already decided where statements end before anything gets here.
        while (i < line.length() && isBlank(line.charAt(i))) {
            i++;
        }
        return i;
    }

    /**
     * Whether a declaration on this line reaches the same configuration the
     * constraints are written on.
     *
     * <p>A declaration on a variant or test configuration does not.
     * {@code debugImplementation platform('...kotlin-bom:1.9.22')} constrains
     * the debug variant alone, so treating it as the app managing the stdlib
     * removes the constraint from the release build that still needs it --
     * and the release build is the one that ships.</p>
     *
     * <p>The variant forms camel-case the configuration they derive from, so
     * requiring the configuration's own lowercase spelling as a whole token
     * excludes {@code debugImplementation}, {@code releaseImplementation} and
     * {@code testImplementation} without listing them, and cannot be defeated
     * by a variant name nobody thought of. The other main-variant
     * configurations are accepted alongside the one being written on; see
     * {@link #MAIN_CONFIGURATIONS}.</p>
     */
    private static boolean declaresOnTheConstrainedConfiguration(String configuration,
            String line) {
        if (declaresOn(configuration, line)) {
            return true;
        }
        for (int i = 0; i < MAIN_CONFIGURATIONS.length; i++) {
            if (declaresOn(MAIN_CONFIGURATIONS[i], line)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The dependency configurations of the main variant, which is the one the
     * constraints are written on.
     *
     * <p>Every one of these reaches the release RUNTIME classpath, which is the
     * one {@code checkReleaseDuplicateClasses} reads and therefore the only one
     * whose contents this class is trying to fix. {@code runtimeOnly} belongs
     * here for exactly that reason.</p>
     *
     * <p>{@code compileOnly} does not, and putting it here was a mistake made
     * by symmetry: a {@code compileOnly platform('...kotlin-bom')} is absent
     * from the runtime graph, so treating it as the app managing that graph
     * dropped the constraint from a classpath the app had not touched and left
     * the duplicate in place. A compile-only declaration that would collide
     * with the constraint is caught by the strict-version rule below instead,
     * which is where that concern actually belongs.</p>
     *
     * <p>Their variant and test forms camel-case the configuration they derive
     * from -- {@code testRuntimeOnly}, {@code debugCompileOnly},
     * {@code releaseApi} -- so matching the lowercase spelling as a whole token
     * accepts the main ones and excludes the rest without listing any of them,
     * whatever a variant happens to be called.</p>
     */
    private static final String[] MAIN_CONFIGURATIONS = {
        "implementation",
        "api",
        "runtimeOnly",
        "compile",
        "runtime"
    };

    /**
     * Whether a resolution strategy in this statement governs a configuration
     * that receives the emitted constraint.
     *
     * <p>It does unless the statement names one particular configuration that
     * is not among the constrained ones. Anything that leaves the selection to
     * a closure, and anything that does not go through {@code configurations}
     * at all, is assumed to reach: being wrong that way costs an app the
     * duplicate it already had, while being wrong the other way emits a
     * constraint into a graph whose strategy fails the build on it.</p>
     */
    private static boolean governsTheConstrainedGraph(String line, String configuration) {
        String named = configurationNamedIn(line);
        // No single configuration named, so which ones are meant is decided by a
        // closure this cannot evaluate -- `configurations.all { }`, and equally
        // `configurations.matching { it.name == 'releaseRuntimeClasspath' }.all`,
        // which really does select the graph being constrained. Reading a filter
        // as "some other configuration" put the constraints into a graph whose
        // strategy fails the build on the version they raise.
        return named == null || named.equals(configuration)
                || isAMainConfiguration(named)
                // A configuration the app made can still INHERIT the constraint:
                // `configurations.create('tooling').extendsFrom(configurations
                // .implementation)` is not an independent graph, and reading only
                // the name it was created under exempted it -- so the constraints
                // went into a graph that then failed on the version they raise.
                || extendsAConstrainedConfiguration(line, configuration);
    }

    /**
     * Whether the statement makes its configuration extend one the constraint is
     * written on.
     *
     * <p>Only what follows {@code extendsFrom} is read, because that is the one
     * API for inheritance and the parent is its argument. A configuration
     * extending something else -- {@code compileOnly}, say -- does not receive
     * the constraint, and answering otherwise would exempt nothing at all.</p>
     */
    private static boolean extendsAConstrainedConfiguration(String line,
            String configuration) {
        int at = afterCall(line, "extendsFrom");
        if (at < 0) {
            return false;
        }
        for (int i = at; i < line.length(); i++) {
            if (isLiteralStart(line, i)) {
                int end = endOfStringLiteral(line, i);
                String held = stringLiteralContent(line, i);
                if (held.equals(configuration) || isAMainConfiguration(held)) {
                    return true;
                }
                i = end;
                continue;
            }
            if (!isIdentifierChar(line.charAt(i))
                    || (i > at && isIdentifierChar(line.charAt(i - 1)))) {
                continue;
            }
            int end = i;
            while (end < line.length() && isIdentifierChar(line.charAt(end))) {
                end++;
            }
            String token = line.substring(i, end);
            if (token.equals(configuration) || isAMainConfiguration(token)) {
                return true;
            }
            i = end - 1;
        }
        return false;
    }

    /** Whether the name is one of the configurations the constraint is on. */
    private static boolean isAMainConfiguration(String name) {
        for (int i = 0; i < MAIN_CONFIGURATIONS.length; i++) {
            if (MAIN_CONFIGURATIONS[i].equals(name)) {
                return true;
            }
        }
        // And the resolvable classpaths, which EXTEND those and are where a
        // strategy actually runs: a failOnVersionConflict on
        // `configurations.releaseRuntimeClasspath` governs the graph these
        // constraints are resolved in, and reading it as some other configuration
        // put them into a graph that then failed on the version they raise.
        //
        // By suffix rather than by name, because Gradle synthesises one per
        // variant -- releaseRuntimeClasspath, debugCompileClasspath, and whatever
        // a flavour adds -- so no list of them can be complete. A configuration
        // the app named that way and did not wire up costs a suppression, which
        // is the direction this class takes everywhere.
        String lower = name.toLowerCase();
        return lower.endsWith("runtimeclasspath") || lower.endsWith("compileclasspath");
    }

    /** The container, as a token: what follows it says which configuration. */
    private static final String CONFIGURATIONS = "configurations";

    /** Whether this line declares on {@code configuration}, as a whole token. */
    private static boolean declaresOn(String configuration, String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (isLiteralStart(line, i)) {
                // The shared rule rather than a third hand-rolled quote scanner. This
                // one tracked a single delimiter character, so a triple-quoted name
                // was read as an empty string followed by unquoted text -- the same
                // defect that was live in the map-value and interpolation paths.
                int end = endOfStringLiteral(line, i);
                // A configuration name inside a string counts in one place only:
                // as the first argument of dependencies.add("runtimeOnly", "..").
                // Accepting any quoted occurrence read the word in a reason --
                // because 'implementation workaround' -- as a main-variant
                // declaration, which suppressed the constraint for a dependency
                // that only affects debug.
                if (end < line.length()
                        && stringLiteralContent(line, i).equals(configuration)
                        && isAddCallArgument(line, i)) {
                    return true;
                }
                i = end;
                continue;
            }
            if (line.startsWith(configuration, i)) {
                boolean startsToken = i == 0
                        || !isIdentifierChar(line.charAt(i - 1));
                int after = i + configuration.length();
                boolean endsToken = after < line.length()
                        && (isBlank(line.charAt(after)) || line.charAt(after) == '(');
                if (startsToken && endsToken) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether the string literal opening at {@code quoteAt} is the
     * configuration argument of an {@code add} call.
     *
     * <p>Both spellings count. Groovy's command syntax drops the parentheses --
     * {@code add 'implementation', 'group:artifact:version'} is as valid as
     * {@code add("implementation", "...")} -- and requiring the parenthesis
     * rejected a declaration that was carrying an explicit strict pin.</p>
     */
    private static boolean isAddCallArgument(String line, int quoteAt) {
        int i = skipBlanksBackward(line, quoteAt - 1);
        // Every parenthesis, for the reason isDeclarationArgument gives.
        while (i >= 0 && line.charAt(i) == '(') {
            i = skipBlanksBackward(line, i - 1);
        }
        if (i < 0 || !isIdentifierChar(line.charAt(i))) {
            return false;
        }
        int nameEnd = i;
        while (i >= 0 && isIdentifierChar(line.charAt(i))) {
            i--;
        }
        String method = line.substring(i + 1, nameEnd + 1);
        // The receiver decides when there is one, and the name when there is not.
        //
        // `add` was the only name accepted, so Gradle's provider form --
        // `dependencies.addProvider('implementation', ..)` -- was not read as a
        // declaration at all. The handler has three adders and may grow more, so
        // a call ON the handler is taken whatever it is called; an unqualified
        // one, which is the shorthand inside a dependencies closure, still has to
        // look like an adder, because `catalog.add(..)` adds to a version catalog
        // and `myList.add(..)` to a list, and neither declares anything.
        int dot = skipBlanksBackward(line, i);
        if (dot < 0 || line.charAt(dot) != '.') {
            return isADependencyHandlerAdder(method);
        }
        int end = skipBlanksBackward(line, dot - 1);
        if (end < 0) {
            return false;
        }
        int start = end;
        while (start >= 0 && (isIdentifierChar(line.charAt(start))
                || (line.charAt(start) == '.' && start > 0
                        && isIdentifierChar(line.charAt(start - 1))))) {
            start--;
        }
        return end > start
                && lastSegmentIs(line.substring(start + 1, end + 1), "dependencies");
    }

    /**
     * A fragment's lines with comments removed and exclusions dropped -- the
     * text that actually declares something.
     *
     * <p>Comment delimiters are only delimiters outside a string, which is the
     * same rule the statement scanner already applied to parentheses and
     * semicolons and which this had been missing. It matters in both
     * directions: {@code maven { url 'https://...' }} is an ordinary
     * declaration that a naive strip cuts in half, and a {@code /*} inside a
     * string used to open a block comment that swallowed the rest of the
     * fragment -- including, in the case that found this, an explicit strict
     * pin whose loss turns this class's constraint into a failed resolution.
     * Tracking quotes covers both, and replaces the narrower rule that only
     * spared a {@code //} following a colon.</p>
     *
     * <p>Regrouping into statements is {@link #statements}; this method only
     * removes the comments.</p>
     */
    private static String[] activeLines(String fragment) {
        if (fragment == null) {
            return new String[0];
        }
        StringBuilder out = new StringBuilder();
        boolean inBlockComment = false;
        for (int i = 0; i < fragment.length(); i++) {
            char c = fragment.charAt(i);
            if (inBlockComment) {
                if (c == '*' && i + 1 < fragment.length() && fragment.charAt(i + 1) == '/') {
                    inBlockComment = false;
                    i++;
                } else if (c == '\n') {
                    out.append(c);
                }
                continue;
            }
            if (isLiteralStart(fragment, i)) {
                // The shared rule, so triple-quoted literals and escapes are the
                // same thing here as everywhere else. This scanner and the statement
                // scanner below kept their own copies through the consolidation, and
                // the triple-quote fix reached neither until now.
                int end = endOfStringLiteral(fragment, i);
                out.append(fragment, i, Math.min(end + 1, fragment.length()));
                i = end;
                continue;
            }
            if (c == '/' && i + 1 < fragment.length()) {
                char next = fragment.charAt(i + 1);
                if (next == '*') {
                    inBlockComment = true;
                    i++;
                    // A comment IS whitespace in the language, so removing one
                    // without leaving any joined the tokens it separated:
                    // `strictly/* pin */'1.7.22'` became strictly'1.7.22', which is
                    // not a call to strictly, so the strict pin behind it was never
                    // seen. Groovy accepts the original and records {strictly 1.7.22}.
                    out.append(' ');
                    continue;
                }
                if (next == '/') {
                    // Either terminator. Groovy ends a line at a bare carriage
                    // return too, and searching only for the newline swallowed the
                    // whole remainder of a CR-only fragment as part of the comment
                    // -- including, in the case that found this, a strict pin.
                    while (i < fragment.length() && fragment.charAt(i) != '\n'
                            && fragment.charAt(i) != '\r') {
                        i++;
                    }
                    out.append('\n');
                    continue;
                }
            }
            out.append(c);
        }
        return statements(out.toString());
    }

    /**
     * Physical lines regrouped into the statements a declaration check can
     * actually read.
     *
     * <p>Two things a per-physical-line check gets wrong, both of which end
     * with an app's explicit pin ignored and the constraint written over the
     * top of it -- the opposite of what naming the artifact in a build hint
     * is documented to do:</p>
     *
     * <pre>
     * implementation(                                  configuration and coordinate
     *     'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'    land on different lines
     * )
     *
     * implementation('...kotlin-stdlib-jdk8:1.7.22') { exclude group: 'x' }
     *                                                  a real pin, dropped whole
     *                                                  for containing "exclude"
     * </pre>
     *
     * <p>So a line whose parentheses are still open is joined to the next.
     * Exclusions are left alone: they used to be cut out here, which was
     * needed while a declaration was recognised by the artifact name appearing
     * anywhere, and became both unnecessary and harmful once a declaration had
     * to be spelled as one. Unnecessary, because an exclusion writes
     * {@code group: '...', module: 'kotlin-stdlib-jdk8'} and never the
     * colon-joined coordinate or the {@code name:} map form the declaration
     * check looks for, so it cannot match one. Harmful, because cutting from
     * {@code exclude} to the end of the statement also threw away anything
     * after it -- an exclusion written before a
     * {@code version { strictly '1.7.22' } }} block took that block with it,
     * and losing the strict marker is what turns this class's constraint into
     * a failed resolution.</p>
     *
     * <p>A statement ends at a newline or at a semicolon, whichever comes
     * first, and neither ends one inside parentheses or inside a string. The
     * semicolon is not a nicety: this builder tells developers to separate
     * {@code android.gradleDep} statements "with ';' or a newline", so a hint
     * holding two declarations on one line is the documented shape. Splitting
     * on newlines alone let the configuration token of the first statement pair
     * with the coordinate of the second, which reads
     * {@code implementation 'x'; debugImplementation platform('...kotlin-bom')}
     * as a main-variant BOM and suppresses everything.</p>
     *
     * <p>Joining stops at the end of the fragment: text left with parentheses
     * open is unbalanced Gradle, and rather than glue the remainder into one
     * long line -- which would make unrelated statements look like a single
     * declaration, and suppression is the direction that must never be reached
     * by accident -- its lines are kept as they were.</p>
     */
    private static String[] statements(String text) {
        List<String> out = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isLiteralStart(text, i)) {
                // The shared rule: escapes and triple quotes handled in one place.
                // A literal that closed early here merged statements that must stay
                // apart, which lets one statement's configuration pair with another
                // statement's coordinate.
                int end = endOfStringLiteral(text, i);
                current.append(text, i, Math.min(end + 1, text.length()));
                i = end;
                continue;
            }
            // Brackets hold a statement together exactly as parentheses do. A force
            // is written across lines as
            //   resolutionStrategy.forcedModules = [
            //           'org.jetbrains.kotlin:kotlin-stdlib:1.7.22'
            //   ]
            // and splitting there left the assignment in one statement and its
            // coordinate in another, so neither said anything and the force went
            // unread. Counted together because the question is only ever "is this
            // newline inside something", not which kind of something.
            if (c == '(' || c == '[') {
                depth++;
            } else if (c == ')' || c == ']') {
                if (depth > 0) {
                    depth--;
                }
            } else if ((c == '\n' || c == '\r' || c == ';') && depth == 0) {
                // A bare carriage return ends a line in Groovy exactly as a newline
                // does. Recognising only the newline merged every statement of a
                // CR-only fragment into one, so a main-variant configuration paired
                // with a debug-only coordinate and that artifact read as declared.
                // CRLF is one break, not two: the newline behind a return is eaten
                // here rather than splitting again on an already-empty statement.
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                // A trailing comma continues the statement. Groovy's parenthesis-free
                // map notation spreads one declaration over several lines --
                //   implementation group: 'org.jetbrains.kotlin',
                //                  name: 'kotlin-stdlib-jdk8',
                //                  version: '1.7.22'
                // -- and splitting there left the configuration, the group, the
                // artifact and any closure in four statements, none of which is a
                // declaration on its own.
                if (c != ';' && endsWithComma(current)) {
                    current.append(' ');
                    continue;
                }
                // Groovy's explicit line continuation. `implementation \` with the
                // coordinate on the next line was split into a configuration with
                // no dependency and a coordinate with no configuration, so neither
                // said anything and the strict pin between them went unread.
                if (c != ';' && endsWithLineContinuation(current)) {
                    current.setLength(current.length() - 1);
                    current.append(' ');
                    continue;
                }
                if (c != ';' && opensAnUnbracedBody(current.toString())) {
                    // An `if (...)` with no brace takes the next line as its body, so
                    // splitting there put the condition in one statement and the body
                    // in another -- and a resolution rule written that way had the
                    // artifact named in the condition and the useVersion in the body,
                    // so neither statement said anything and the override went unread.
                    current.append(' ');
                    continue;
                }
                out.add(current.toString().replace('\n', ' ').replace('\r', ' '));
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            if (depth > 0) {
                // Unbalanced: keep the tail's physical lines apart rather than as one
                // statement. Gluing them would let a configuration from one and a
                // coordinate from another read as a single declaration, and
                // suppression is the direction that must never be reached by accident.
                String[] dangling = current.toString().split("\n");
                for (int i = 0; i < dangling.length; i++) {
                    out.add(dangling[i]);
                }
            } else {
                out.add(current.toString().replace('\n', ' ').replace('\r', ' '));
            }
        }
        // Definitions are folded in FIRST, because the merge below only absorbs a
        // closure into a statement that already names the Kotlin group -- and a
        // statement referring to the coordinate through a variable does not name it
        // until the fold has happened. Merging first left `implementation(stdlib) {`
        // unmerged, so its `strictly` was never associated with the coordinate.
        List<String> defined = inlineLiteralDefinitions(out);

        // A declaration's own configuration block belongs to it: the version that
        // decides this is written as `version { strictly '1.7.22' }` on the line after
        // the coordinate. Only a statement that already names the Kotlin group absorbs
        // its block, so a `dependencies {` or `android {` opening cannot swallow the
        // fragment -- the blast radius is one declaration, never the file.
        List<String> merged = new ArrayList<String>();
        for (int i = 0; i < defined.size(); i++) {
            String statement = defined.get(i);
            if (statement.contains(KOTLIN_GROUP) || namesAnAlignedArtifact(statement)) {
                // A trailing closure may sit on the line AFTER the call's closing
                // parenthesis -- Gradle accepts it and the strictly inside really does
                // apply, checked by watching a competing higher requirement fail
                // against it. The parenthesis depth is already back to zero there, so
                // without this the closure lands in its own statement and the version
                // it carries is never associated with the coordinate above it.
                // Past anything blank in between. A comment-only line leaves an empty
                // statement behind it, and looking only at the very next one left the
                // closure -- and the strict version inside it -- attached to nothing.
                int next = i + 1;
                while (next < defined.size() && defined.get(next).trim().length() == 0) {
                    next++;
                }
                while (next < defined.size() && opensAClosure(defined.get(next))) {
                    while (i < next) {
                        i++;
                    }
                    statement = statement + " " + defined.get(i);
                    next = i + 1;
                    while (next < defined.size()
                            && defined.get(next).trim().length() == 0) {
                        next++;
                    }
                }
                int braces = trailingBraceBalance(statement);
                while (braces > 0 && i + 1 < defined.size()) {
                    i++;
                    statement = statement + " " + defined.get(i);
                    braces += braceBalance(defined.get(i));
                }
                // An `else` is the same statement as the `if` before it, and the
                // condition that names the family is on the `if`. Left apart, the
                //   if (d.requested.name == 'kotlin-stdlib')
                //       d.useVersion '1.9.22'
                //   else
                //       d.useVersion '1.7.22'
                // rule offered only its first branch, so the version that decides
                // suppression was in a statement that named nothing. Adjacency, not
                // a reading of which branch runs: joined, the statement holds both
                // versions and the last one wins, which is the conservative answer
                // this class takes wherever it cannot evaluate a condition.
                while (i + 1 < defined.size() && continuesWithElse(defined.get(i + 1))) {
                    i++;
                    statement = statement + " " + defined.get(i);
                    int reopened = trailingBraceBalance(statement);
                    while (reopened > 0 && i + 1 < defined.size()) {
                        i++;
                        statement = statement + " " + defined.get(i);
                        reopened += braceBalance(defined.get(i));
                    }
                }
            }
            merged.add(statement);
        }
        return merged.toArray(new String[merged.size()]);
    }

    /**
     * Statements with {@code def name = 'literal'} definitions folded into the
     * places that use them.
     *
     * <p>A coordinate can sit one hop away:</p>
     *
     * <pre>
     * def jdk8 = 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22'
     * implementation(jdk8) { version { strictly '1.7.22' } }
     * </pre>
     *
     * <p>Neither statement carries both the configuration and the coordinate,
     * so the strict pin was invisible and the constraint made the build stop
     * resolving. One hop through a string literal is recoverable from the text
     * and is recovered here.</p>
     *
     * <p><b>Where this stops, deliberately.</b> A value built by
     * interpolation, by concatenation, from a map or a list, or returned by a
     * method is not in the text at all -- reading it needs Gradle to evaluate
     * the script, which nothing here can do. Those forms are left unrecognised
     * rather than guessed at, and that is a real limit of reading declarations
     * out of build-hint text rather than something another pass would fix. The
     * design that does not need to find the declaration at all -- constraining
     * unless a strict version says otherwise -- is the answer to that class,
     * and it is a decision about documented behaviour rather than a defect to
     * patch here.</p>
     */
    private static List<String> inlineLiteralDefinitions(List<String> statements) {
        // In statement order, and the definition is recorded AFTER its own statement
        // has been rewritten. A two-pass version substituted a variable's first value
        // into every use of it, including uses after a reassignment -- so
        //   def dep = '...kotlin-stdlib-jdk8:1.9.22'; debugImplementation(dep)
        //   dep = 'com.example:other:1.0'; implementation(dep)
        // made the LAST statement read as a main-variant Kotlin declaration.
        Map<String, String> literals = new LinkedHashMap<String, String>();
        List<String> out = new ArrayList<String>();
        // Gradle's extra properties are written both ways -- ext.kotlinVersion = '..'
        // and ext { kotlinVersion = '..' } -- and the closure form is at least as
        // common. Inside it a bare assignment really does bind a project-wide name,
        // which is exactly what the interpolation reads, so it is a definition there
        // and nowhere else: a bare `version = '1.0'` in an android block binds
        // nothing this can follow, and reading it as a definition would supply a
        // version to an unrelated $version.
        int extDepth = 0;
        // Whether a nested block runs is decided at evaluation time and cannot be
        // read here, so a name assigned inside one may hold either value. The
        // ambiguity is resolved toward suppression: emitting beside a pin this could
        // not see is the failure that reaches the device, while suppressing costs an
        // app the duplicate it already had.
        //
        // ANY open brace, not a list of the constructs that open one. That list was
        // if/else/while/for/switch/try/catch and it was already missing the closure
        // -- `def mutate = { dep = ... }` runs only if something calls it. Depth is
        // the closed half of the question: at the top level a statement runs, and
        // inside anything at all this cannot say. Costless to be wrong about, too,
        // since the flag only refuses to DISCARD a coordinate; a first definition is
        // still recorded at any depth, which is why a `def` inside dependencies { }
        // keeps working.
        int braceDepth = 0;
        ScopedNames scope = new ScopedNames();
        // A buildscript block configures the PLUGIN classpath, which is a separate
        // resolution from the app's and cannot conflict with what this writes into
        // dependencies { }. A force, a strict pin or a shim declaration in there was
        // being read as the app managing the family, so an app graph carrying a
        // pre-merge shim was left unaligned and still failed checkDuplicateClasses.
        //
        // The statements are blanked rather than dropped, and only AFTER their
        // definitions have been recorded: `buildscript { ext.kotlin_version = .. }`
        // followed by a dependency interpolating $kotlin_version is the ordinary
        // shape of a Kotlin project, and the definition really does bind
        // script-wide even though the declarations around it do not.
        int buildscriptDepth = 0;
        for (int i = 0; i < statements.size(); i++) {
            String statement = statements.get(i);
            boolean opensBuildscript = buildscriptDepth == 0
                    && opensAForeignScope(statement);
            boolean pluginScoped = buildscriptDepth > 0 || opensBuildscript
                    || namesTheBuildscriptClasspath(statement);
            out.add(pluginScoped ? "" : (literals.isEmpty()
                    ? statement
                    : withLiteralsInlined(statement, literals)));
            if (pluginScoped) {
                buildscriptDepth += braceBalance(statement);
                if (buildscriptDepth < 0) {
                    buildscriptDepth = 0;
                }
            }
            boolean opensExt = extDepth == 0 && opensAnExtraPropertiesBlock(statement);
            updateLiteralDefinitions(statement, literals, extDepth > 0 || opensExt,
                    braceDepth > 0, braceDepth, scope);
            if (extDepth > 0 || opensExt) {
                extDepth += braceBalance(statement);
                if (extDepth < 0) {
                    extDepth = 0;
                }
            }
            braceDepth += braceBalance(statement);
            if (braceDepth < 0) {
                braceDepth = 0;
            }
            scope.leaving(braceDepth, literals);
        }
        return out;
    }

    /**
     * Applies this statement's effect on the known definitions: a
     * {@code def name = 'literal'}, a reassignment of one already known, or a
     * reassignment to something unreadable, which forgets it rather than
     * leaving a stale value behind.
     */
    /**
     * The index of the {@code ]} closing the bracket at {@code from}, or -1.
     * Nested brackets and literals are skipped, so a map inside a list closes
     * where it really closes.
     */
    private static int closingBracket(String text, int from) {
        int depth = 0;
        for (int i = from; i < text.length(); i++) {
            if (isLiteralStart(text, i)) {
                i = endOfStringLiteral(text, i);
                continue;
            }
            char c = text.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Records a Groovy multiple assignment, and says whether the statement was
     * one.
     *
     * <p>{@code def (other, dep) = ['com.example:x:1.0', 'g:a:1.7.22!!']} binds
     * both names positionally. The walk for a single declaration expects an
     * identifier after {@code def} and finds a parenthesis, so it recorded
     * nothing at all and the pin the second name carried went unread.</p>
     *
     * <p>Each name may carry a type, as a single declaration may, so the NAME is
     * the last identifier of its element. An element whose value is neither a
     * literal nor a known name records nothing, which leaves it unknown rather
     * than wrong.</p>
     */
    private static boolean recordsADestructuring(String statement, int at,
            Map<String, String> literals, boolean conditional) {
        int i = skipBlanks(statement, at);
        if (i >= statement.length() || statement.charAt(i) != '(') {
            return false;
        }
        List<String> names = new ArrayList<String>();
        i++;
        while (i < statement.length() && statement.charAt(i) != ')') {
            i = skipBlanks(statement, i);
            String last = null;
            while (i < statement.length() && isIdentifierChar(statement.charAt(i))) {
                int start = i;
                while (i < statement.length() && isIdentifierChar(statement.charAt(i))) {
                    i++;
                }
                last = statement.substring(start, i);
                i = skipBlanks(statement, i);
            }
            names.add(last);
            if (i < statement.length() && statement.charAt(i) == ',') {
                i++;
            } else {
                break;
            }
        }
        if (i >= statement.length() || statement.charAt(i) != ')' || names.isEmpty()) {
            return false;
        }
        i = skipBlanks(statement, i + 1);
        if (i >= statement.length() || !isAssignmentAt(statement, i)) {
            return false;
        }
        i = skipBlanks(statement, i + 1);
        if (i >= statement.length() || statement.charAt(i) != '[') {
            // A list this cannot read binds every name to something unknown,
            // which is what recording nothing already means.
            return true;
        }
        int closes = closingBracket(statement, i);
        i++;
        for (int n = 0; n < names.size() && i < statement.length()
                && (closes < 0 || i < closes); n++) {
            i = skipBlanks(statement, i);
            String value = null;
            if (isLiteralStart(statement, i)) {
                int end = endOfStringLiteral(statement, i);
                if (end < statement.length()) {
                    value = expandedLiteral(statement, i, end, literals);
                    i = end + 1;
                }
            } else {
                int start = i;
                while (i < statement.length() && isIdentifierChar(statement.charAt(i))) {
                    i++;
                }
                if (i > start) {
                    value = literals.get(statement.substring(start, i));
                }
            }
            if (names.get(n) != null && value != null) {
                recordDefinition(literals, names.get(n), value, conditional);
            }
            i = skipBlanks(statement, i);
            if (i < statement.length() && statement.charAt(i) == ',') {
                i++;
            }
        }
        return true;
    }

    /**
     * Records a definition, or forgets it, unless doing so under a condition
     * would throw away the value that decides suppression.
     */
    private static void recordDefinition(Map<String, String> literals, String name,
            String value, boolean conditional) {
        if (conditional) {
            String known = literals.get(name);
            if (known != null && known.indexOf(KOTLIN_GROUP) >= 0
                    && (value == null || value.indexOf(KOTLIN_GROUP) < 0)) {
                return;
            }
        }
        if (value == null) {
            literals.remove(name);
        } else {
            literals.put(name, value);
        }
    }

    /**
     * Whether the statement opens a Gradle {@code ext { }} block, as a whole
     * token so that a dependency on {@code com.example:extras} does not.
     */
    private static boolean opensAnExtraPropertiesBlock(String statement) {
        return opensBlockNamed(statement, EXTRA_PROPERTIES);
    }

    /** Whether the statement opens a block named {@code name}. */
    private static boolean opensBlockNamed(String statement, String name) {
        // Outside literals, for the same reason the classpath check is: a block
        // opener quoted in a reason opens nothing, and treating one as the real
        // thing puts every statement after it in a scope it is not in.
        for (int at = 0; at < statement.length(); at++) {
            if (isLiteralStart(statement, at)) {
                at = endOfStringLiteral(statement, at);
                continue;
            }
            if (!statement.startsWith(name, at)) {
                continue;
            }
            int after = at + name.length();
            boolean startsToken = at == 0 || !isIdentifierChar(statement.charAt(at - 1));
            int brace = skipBlanks(statement, after);
            if (startsToken && (after >= statement.length()
                    || !isIdentifierChar(statement.charAt(after)))
                    && brace < statement.length() && statement.charAt(brace) == '{') {
                return true;
            }
        }
        return false;
    }

    /**
     * The depth at {@code index}, counting braces opened earlier in this
     * statement.
     *
     * <p>Depth is tracked between statements, so a closure opened and a local
     * declared on the SAME line looked like top level:
     * {@code ext.helper = { def dep = '...' }} recorded dep as if it belonged
     * to the script, and it then outlived the closure and shadowed the real
     * binding for everything after.</p>
     */
    private static int depthAt(String statement, int index, int base) {
        return base + braceBalance(statement.substring(0, Math.min(index, statement.length())));
    }

    /**
     * The names a scope introduced, so they can be taken back when it closes.
     *
     * <p>A `def` inside a closure or a method is local to it, and a single flat
     * map kept that value after the scope ended -- so an unrelated later
     * `implementation(dep)` was read as a declaration of whatever the nested
     * one held, and the constraint for that artifact was skipped as already
     * satisfied. Only DECLARATIONS are taken back: a bare assignment inside a
     * block updates the binding it found, which is why
     * `if (legacy) { dep = '...' }` still reaches the statements after it.</p>
     */
    private static final class ScopedNames {
        private final List<Object[]> introduced = new ArrayList<Object[]>();

        void declared(int depth, String name, Map<String, String> literals) {
            if (depth <= 0) {
                return;
            }
            introduced.add(new Object[] {
                Integer.valueOf(depth), name,
                literals.containsKey(name) ? Boolean.TRUE : Boolean.FALSE,
                literals.get(name)
            });
        }

        void leaving(int depth, Map<String, String> literals) {
            for (int i = introduced.size() - 1; i >= 0; i--) {
                Object[] entry = introduced.get(i);
                if (((Integer) entry[0]).intValue() <= depth) {
                    break;
                }
                introduced.remove(i);
                String name = (String) entry[1];
                if (Boolean.TRUE.equals(entry[2])) {
                    literals.put(name, (String) entry[3]);
                } else {
                    literals.remove(name);
                }
            }
        }
    }

    private static void updateLiteralDefinitions(String statement,
            Map<String, String> literals, boolean insideExtraProperties,
            boolean conditional, int depth, ScopedNames scope) {
        if (insideExtraProperties) {
            // The assignment may share the line with the brace that opened the block,
            // as `ext { kotlinVersion = '1.9.22' }` does, so read from after it.
            int brace = statement.indexOf('{');
            String body = brace >= 0 ? statement.substring(brace + 1) : statement;
            recordBareAssignment(body, literals);
        }
        int i = 0;
        boolean declared = false;
        boolean subscript = false;
        boolean callForm = false;
        // An extra property is NOT block scoped. A local declared inside a block
        // leaves with it, which is why declarations carry their depth -- but
        // `buildscript { ext.kotlin_version = '1.9.22' }` sets a project-wide
        // property, and it is the standard shape of a Kotlin Android script.
        // Recorded at the depth of the brace it sat in, it was discarded at the
        // closing brace, so the version every dependency below interpolated read
        // as unreadable, counted as below the floor, and stood the whole block
        // down -- the alignment never ran for the commonest project there is.
        boolean extraProperty = false;
        // Outside literals, like every other question about syntax. An unrestricted
        // search found `def` inside quoted prose -- println "def dep = '...'" -- and
        // recorded a declaration that never executes, overwriting the real binding
        // and making a later use read as something it is not.
        int at = afterCall(statement, DEF);
        if (at >= 0 && recordsADestructuring(statement, at, literals, conditional)) {
            return;
        }
        if (at >= 0) {
            declared = true;
            i = skipBlanks(statement, at);
        } else {
            i = skipBlanks(statement, afterAnyBlockOpener(statement));
            // Past any annotations first. A script field is written
            // `@groovy.transform.Field String dep = '...'`, and the walk below reads
            // identifier tokens -- so it stopped dead on the `@`, recorded nothing,
            // and the strict pin the field carried was never seen.
            while (i < statement.length() && statement.charAt(i) == '@') {
                i++;
                while (i < statement.length()
                        && (isIdentifierChar(statement.charAt(i))
                                || (statement.charAt(i) == '.'
                                        && i + 1 < statement.length()
                                        && isIdentifierChar(statement.charAt(i + 1))))) {
                    i++;
                }
                i = skipBlanks(statement, i);
                if (i < statement.length() && statement.charAt(i) == '(') {
                    // An annotation may carry arguments, and they may nest.
                    int open = 0;
                    while (i < statement.length()) {
                        char c = statement.charAt(i);
                        if (isLiteralStart(statement, i)) {
                            i = endOfStringLiteral(statement, i);
                        } else if (c == '(') {
                            open++;
                        } else if (c == ')') {
                            open--;
                            if (open == 0) {
                                i++;
                                break;
                            }
                        }
                        i++;
                    }
                    i = skipBlanks(statement, i);
                }
            }
            // A typed local declares just as much as def does, and it is written
            // with however many modifiers the author felt like: `String dep = ...`,
            // `final String dep = ...`, `private static final String dep = ...`.
            // Counting exactly two tokens read `String` as the name of a `final
            // String dep` and never recorded dep at all. So walk every identifier
            // token before the '=': more than one is a declaration whose name is the
            // last of them, exactly one is an assignment.
            int scan = i;
            int lastTokenStart = i;
            int lastTokenEnd = i;
            int tokens = 0;
            while (scan < statement.length() && isIdentifierChar(statement.charAt(scan))) {
                lastTokenStart = scan;
                tokens++;
                // A qualified name is ONE token: java.lang.String dep = '...' is a
                // declaration whose type happens to have dots in it, and stopping at
                // the first one read `java` as the type and `lang` as the name, so
                // dep was never recorded and the pin it carried never seen.
                while (scan < statement.length()
                        && (isIdentifierChar(statement.charAt(scan))
                                || (statement.charAt(scan) == '.'
                                        && scan + 1 < statement.length()
                                        && isIdentifierChar(statement.charAt(scan + 1))))) {
                    scan++;
                }
                // A type's arguments belong to the type: `Map<String, String> dep`
                // is a declaration whose type happens to be generic, and stopping at
                // the `<` read `Map` as the whole statement -- so dep was never
                // recorded and the strict pin the map held was never seen.
                int generics = endOfTypeArguments(statement, scan);
                if (generics > scan) {
                    scan = generics;
                }
                // And its dimensions, for the same reason: `String[] dep` is a
                // declaration too. Only an EMPTY pair, which nothing but an array
                // type is -- a subscript with something in it is `ext['dep']` or
                // `deps[0]`, and swallowing those would take the name with them.
                while (true) {
                    int empty = skipBlanks(statement, scan);
                    if (empty >= statement.length() || statement.charAt(empty) != '[') {
                        break;
                    }
                    int close = skipBlanks(statement, empty + 1);
                    if (close >= statement.length() || statement.charAt(close) != ']') {
                        break;
                    }
                    scan = close + 1;
                }
                lastTokenEnd = scan;
                scan = skipBlanks(statement, scan);
            }
            if (tokens > 1 && !followedByMapKeyColon(statement, lastTokenEnd)) {
                declared = true;
                i = lastTokenStart;
            } else if (tokens == 1) {
                // ext.kotlinVersion = '1.9.22' -- Gradle's extra properties, which is
                // how a project-wide version is nearly always written, and which
                // really does bind the bare name the interpolation then reads.
                // Restricted to that one owner on purpose: recording ANY dotted
                // assignment would let `somePlugin.version = '1.0'` supply the value
                // for an unrelated $version and turn an unreadable version into a
                // confidently wrong one, which is the direction that under-suppresses.
                String only = statement.substring(lastTokenStart, lastTokenEnd);
                int dot = only.lastIndexOf('.');
                int openBracket = skipBlanks(statement, lastTokenEnd);
                boolean subscripted = openBracket < statement.length()
                        && statement.charAt(openBracket) == '[';
                // The owner is the LAST segment of the qualifier, not the whole of
                // it, because the extension is reachable through the project too:
                // `project.ext.dep` and `rootProject.ext.dep` set the same property
                // the bare name goes on to read -- Gradle resolves a bare name up
                // the project hierarchy -- and comparing the prefix whole rejected
                // both, so a strict pre-merge pin held in one was never seen.
                // Addressing ANOTHER project cannot arrive here: `(` ends the token
                // walk above, so `project(':lib').ext.dep` never reads as one token
                // and the chain is always this script's own.
                int argument = skipBlanks(statement, lastTokenEnd);
                boolean called = argument < statement.length()
                        && statement.charAt(argument) == '(';
                if (dot > 0 && called && "set".equals(only.substring(dot + 1))
                        && lastSegmentIs(only.substring(0, dot), EXTRA_PROPERTIES)) {
                    // ext.set('dep', '...') is the extension's own setter, and the
                    // name is its first argument. Read as a dotted assignment it
                    // recorded a property called `set` and lost the real one, so a
                    // later force naming it through the bare name named nothing --
                    // and the constraints went in beside a force still in effect.
                    int nameAt = skipBlanks(statement, argument + 1);
                    if (nameAt < statement.length()
                            && isLiteralStart(statement, nameAt)
                            && delimiterLength(statement, nameAt) == 1) {
                        declared = true;
                        subscript = true;
                        callForm = true;
                        extraProperty = true;
                        i = nameAt + 1;
                    }
                } else if (dot > 0 && !subscripted
                        && lastSegmentIs(only.substring(0, dot), EXTRA_PROPERTIES)) {
                    declared = true;
                    extraProperty = true;
                    i = lastTokenStart + dot + 1;
                } else if (subscripted && lastSegmentIs(only, EXTRA_PROPERTIES)) {
                    // ext['dep'] = '...' is the subscript spelling of the same
                    // extension and the only one whose property name is a string
                    // rather than an identifier, so the walk above read `ext` as the
                    // name and recorded nothing the app could later refer to. Step
                    // inside the quote and let the identifier scan below take the
                    // name; the closing quote and bracket are stepped over after it.
                    int nameAt = skipBlanks(statement, openBracket + 1);
                    if (nameAt < statement.length()
                            && isLiteralStart(statement, nameAt)
                            && delimiterLength(statement, nameAt) == 1) {
                        declared = true;
                        subscript = true;
                        extraProperty = true;
                        i = nameAt + 1;
                    }
                }
            }
        }
        int nameStart = i;
        while (i < statement.length() && isIdentifierChar(statement.charAt(i))) {
            i++;
        }
        if (i == nameStart) {
            return;
        }
        String name = statement.substring(nameStart, i);
        if (subscript) {
            // Past the `']` the subscript form puts between the name and the `=`,
            // so the value is read the same way every other definition's is.
            if (i < statement.length() && !isIdentifierChar(statement.charAt(i))) {
                i++;
            }
            i = skipBlanks(statement, i);
            if (i < statement.length() && statement.charAt(i) == ']') {
                i++;
            }
        }
        if (!declared && !literals.containsKey(name)) {
            return;
        }
        // A brace this statement opened before the name guards it just as one on an
        // earlier line does. The flag arriving here is the depth the statement
        // STARTED at, so `if (cond) { dep = '...' }` written on one line read as an
        // unconditional reassignment and threw away the coordinate the condition
        // might never replace -- which is the pin, hidden, that this whole rule
        // exists to keep.
        if (depthAt(statement, nameStart, depth) > depth) {
            conditional = true;
        }
        if (declared) {
            scope.declared(extraProperty
                    ? 0 : depthAt(statement, nameStart, depth), name, literals);
        }
        i = skipBlanks(statement, i);
        // The setter's comma separates the name from the value exactly as the `=`
        // does in every other spelling, so it stands in for one here.
        if (callForm && i < statement.length() && statement.charAt(i) == ',') {
            i++;
        } else if (i >= statement.length() || statement.charAt(i) != '='
                || (i + 1 < statement.length() && statement.charAt(i + 1) == '=')) {
            if (declared) {
                // `def dep` with no value yet is still a name this knows about, and
                // recording it is what lets a later assignment be recognised as one.
                // Without it, `def dep` then `if (legacy) { dep = '...' }` left the
                // assignment looking like a write to something unrelated, so the
                // coordinate it carried was never learned. A null value inlines as
                // the name itself, which is what an unset variable should look like.
                literals.put(name, null);
            }
            return;
        }
        // Every declarator, not just the first: `def marker = 'x', dep = 'coord'`
        // declares two names, and stopping after one left the second unknown -- so
        // the strict pin the second carried was invisible to the statement using it.
        while (true) {
            i = skipBlanks(statement, i + 1);
            int end = -1;
            String value = null;
            if (i < statement.length() && isLiteralStart(statement, i)) {
                int closes = endOfStringLiteral(statement, i);
                if (closes < statement.length()) {
                    end = closes;
                    value = expandedLiteral(statement, i, closes, literals);
                }
            } else if (i < statement.length() && statement.charAt(i) == '[') {
                // A map factored into a variable is a declaration too:
                //   def dep = [group: '...', name: '...', version: '1.7.22']
                // Recorded as nothing, the statement using it named no artifact and
                // the strict pin it carried was invisible. Stored whole, it inlines
                // back into the usage and reads as the map form it is.
                int closes = closingBracket(statement, i);
                if (closes > i) {
                    end = closes;
                    // Through the same expansion a string definition gets, so a map
                    // that interpolates a known version -- version: "$v" -- carries
                    // the version rather than the text of the reference. Stored
                    // verbatim, "$v" read as no version at all, which counts as
                    // below the floor and stood the whole block down.
                    value = withLiteralsInlined(
                            statement.substring(i, closes + 1), literals);
                }
            } else if (i < statement.length() && isIdentifierChar(statement.charAt(i))) {
                // A value that is a NAME rather than a literal. `def forced =
                // coord` and `ext.set('forced', coord)` copy a binding that is
                // already known, and reading only literals recorded the new name as
                // unknown -- so a force through it named nothing and the
                // constraints went in beside a pin still in effect.
                //
                // Resolved from what is recorded rather than by inlining the whole
                // statement first, which substitutes the name being ASSIGNED as
                // readily as the one being read: `dep = somethingUnknown` became
                // `'...' = somethingUnknown`, so the reassignment was not seen at
                // all and the stale value survived it.
                int token = i;
                while (token < statement.length()
                        && isIdentifierChar(statement.charAt(token))) {
                    token++;
                }
                String alias = literals.get(statement.substring(i, token));
                if (alias != null && !followedByMapKeyColon(statement, token)) {
                    end = token - 1;
                    value = alias;
                }
            }
            recordDefinition(literals, name, value, conditional);
            if (end < 0) {
                return;
            }
            int comma = skipBlanks(statement, end + 1);
            if (comma >= statement.length() || statement.charAt(comma) != ',') {
                return;
            }
            int nextName = skipBlanks(statement, comma + 1);
            int nextEnd = nextName;
            while (nextEnd < statement.length()
                    && isIdentifierChar(statement.charAt(nextEnd))) {
                nextEnd++;
            }
            if (nextEnd == nextName) {
                return;
            }
            name = statement.substring(nextName, nextEnd);
            int assign = skipBlanks(statement, nextEnd);
            if (assign >= statement.length() || statement.charAt(assign) != '='
                    || (assign + 1 < statement.length()
                            && statement.charAt(assign + 1) == '=')) {
                return;
            }
            if (declared) {
                scope.declared(extraProperty
                        ? 0 : depthAt(statement, nextName, depth), name, literals);
            }
            i = assign;
        }
    }

    /**
     * The literal at {@code from}, with any definitions it interpolates already
     * expanded.
     *
     * <p>A definition may refer to an earlier one --
     * {@code def v = '1.8.0'; def dep = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$v"}
     * -- and storing it as written made the version the text {@code $v}, which
     * reads as no version and therefore as below the floor, suppressing the
     * block for a project that was already merged-era.</p>
     *
     * <p>The VALUE only. Expanding the whole statement first was tried and
     * substituted the NAME being assigned as well, so an unreadable
     * reassignment stopped forgetting the literal it replaced -- the same
     * mistake in the opposite direction, and the suite said so.</p>
     */
    private static String expandedLiteral(String text, int from, int end,
            Map<String, String> literals) {
        String literal = text.substring(from, end + 1);
        // Every literal form Groovy has interpolates EXCEPT the single-quoted
        // ones. Testing for a double quote missed the slashy forms, so a coordinate
        // assembled as $/...:$v/$ kept the text $v as its version -- no version,
        // therefore below the floor, therefore the block suppressed for a project
        // that was already merged-era.
        return text.charAt(from) != '\''
                ? withInterpolationsExpanded(literal, literals)
                : literal;
    }

    /** The statement with known definition names replaced by their literals. */
    private static String withLiteralsInlined(String statement,
            Map<String, String> literals) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < statement.length(); i++) {
            char c = statement.charAt(i);
            if (isLiteralStart(statement, i)) {
                int end = endOfStringLiteral(statement, i);
                String literal = statement.substring(i,
                        Math.min(end + 1, statement.length()));
                // A double-quoted string interpolates, so a known definition referred
                // to as $name or ${name} is the same one hop this already follows for
                // a bare token. Reading it as unreadable made a merged-era version
                // look pre-merge and dropped the sibling's constraint with it.
                // The same rule as expandedLiteral: everything but a single quote.
                out.append(c != '\'' ? withInterpolationsExpanded(literal, literals)
                        : literal);
                i = end;
                continue;
            }
            if (!isIdentifierChar(c)
                    || (i > 0 && isIdentifierChar(statement.charAt(i - 1)))) {
                out.append(c);
                continue;
            }
            int end = i;
            while (end < statement.length() && isIdentifierChar(statement.charAt(end))) {
                end++;
            }
            String token = statement.substring(i, end);
            String literal = literals.get(token);
            // A map KEY is not an expression, so it is not substituted. A local
            // named after the DSL key it supplies -- def group = '...'; then
            // implementation(group: group, ...) -- had both occurrences replaced,
            // turning `group:` into a quoted string and losing the map form
            // entirely, strict pin and all. Groovy's named arguments are exactly
            // "identifier immediately followed by a colon", which is what this asks.
            // The shared test, which skips blanks first: Groovy accepts
            // `group : group` with spaces around the colon, and looking only at the
            // character immediately after the token missed the key and substituted
            // it away again.
            boolean isMapKey = followedByMapKeyColon(statement, end);
            out.append(literal == null || isMapKey ? token : literal);
            i = end - 1;
        }
        return out.toString();
    }

    /** A double-quoted literal with known {@code $name} references expanded. */
    private static String withInterpolationsExpanded(String literal,
            Map<String, String> literals) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
            if (c != '$' || i + 1 >= literal.length()) {
                out.append(c);
                continue;
            }
            int nameStart = i + 1;
            boolean braced = literal.charAt(nameStart) == '{';
            if (braced) {
                nameStart++;
            }
            int nameEnd = nameStart;
            while (nameEnd < literal.length()
                    && isIdentifierChar(literal.charAt(nameEnd))) {
                nameEnd++;
            }
            if (nameEnd == nameStart
                    || (braced && (nameEnd >= literal.length()
                            || literal.charAt(nameEnd) != '}'))) {
                out.append(c);
                continue;
            }
            String value = literals.get(literal.substring(nameStart, nameEnd));
            if (value == null) {
                out.append(c);
                continue;
            }
            // Stored with its quotes, which do not belong inside another string --
            // and with however many of them the literal was written with. Stripping
            // one per side left a triple-quoted definition expanding to ""1.7.22"",
            // no version parsed out of it, and the constraint written beside a
            // strict pre-merge pin: the one outcome that fails at RUNTIME rather
            // than in the build. Found by sweeping equivalent spellings of a strict
            // pin, not by reading this line; the same one-character assumption was
            // live in two other places.
            out.append(stringLiteralContent(value, 0));
            i = braced ? nameEnd : nameEnd - 1;
        }
        return out.toString();
    }

    /**
     * Records {@code name = 'literal'} as a definition. Only ever called for
     * the inside of an extra-properties block, where a bare assignment does
     * bind a name the rest of the script can read.
     */
    private static void recordBareAssignment(String body, Map<String, String> literals) {
        int i = skipBlanks(body, 0);
        int nameStart = i;
        while (i < body.length() && isIdentifierChar(body.charAt(i))) {
            i++;
        }
        if (i == nameStart) {
            return;
        }
        String name = body.substring(nameStart, i);
        i = skipBlanks(body, i);
        if (i >= body.length() || body.charAt(i) != '='
                || (i + 1 < body.length() && body.charAt(i + 1) == '=')) {
            return;
        }
        i = skipBlanks(body, i + 1);
        if (i < body.length() && isLiteralStart(body, i)) {
            int end = endOfStringLiteral(body, i);
            if (end < body.length()) {
                literals.put(name, expandedLiteral(body, i, end, literals));
            }
            return;
        }
        // The other two shapes a value takes, which the ordinary definition path
        // already reads: a map, and a name that copies an earlier binding. An
        // `ext { dep = [group: '..', name: '..', version: '1.7.22!!'] }` block is
        // the project-wide spelling of the same thing, and reading only literals
        // left `dep` unknown -- so the declaration using it named no artifact and
        // the pin it carried went unread.
        if (i < body.length() && body.charAt(i) == '[') {
            int closes = closingBracket(body, i);
            if (closes > i) {
                literals.put(name,
                        withLiteralsInlined(body.substring(i, closes + 1), literals));
            }
            return;
        }
        if (i < body.length() && isIdentifierChar(body.charAt(i))) {
            int end = i;
            while (end < body.length() && isIdentifierChar(body.charAt(end))) {
                end++;
            }
            String alias = literals.get(body.substring(i, end));
            if (alias != null && !followedByMapKeyColon(body, end)) {
                literals.put(name, alias);
            }
        }
    }

    private static final String DEF = "def";

    /** Gradle's extra-properties prefix, the one dotted assignment worth reading. */
    private static final String EXTRA_PROPERTIES = "ext";

    /** The block that configures the plugin classpath rather than the app's. */
    private static final String BUILDSCRIPT = "buildscript";

    /**
     * Blocks whose contents configure something other than the application's
     * dependency graph.
     *
     * <p>{@code buildscript} is the plugin classpath. {@code testing} is the
     * JVM test suites block, whose nested {@code dependencies { }} belongs to a
     * suite's own configurations -- its {@code implementation} has the same
     * name as the app's and is a different thing, so reading a declaration
     * there as the app's skipped the constraint for an artifact the release
     * graph still carries.</p>
     *
     * <p>A list, unusually for this class, because the general question --
     * which enclosing blocks reach this project's graph -- has no closed
     * answer: {@code allprojects} does, {@code subprojects} does not, and a
     * plugin may add either kind. It is safe as a list because a scope missing
     * from it changes nothing: that block keeps being read as the app's, which
     * is what happens today, and the cost is the duplicate an app already had.
     * Inverting it -- treating every nested block as foreign -- is what is not
     * safe, because blanking an {@code allprojects} declaration emits a
     * constraint beside a pin that may be strict.</p>
     */
    private static final String[] FOREIGN_SCOPES = {
        "buildscript",
        "testing"
    };

    /** Whether the statement opens a block that is not the app's own graph. */
    private static boolean opensAForeignScope(String statement) {
        for (int i = 0; i < FOREIGN_SCOPES.length; i++) {
            if (opensBlockNamed(statement, FOREIGN_SCOPES[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the text so far is a control header whose body is the next line.
     *
     * <p>Read off the language's own keywords, which is a closed set -- unlike
     * the earlier use of a keyword list, which was answering "might this scope
     * run" and is better answered by counting braces. The question here is
     * different: an unbraced body belongs to the header above it, and only
     * these words introduce one.</p>
     */
    private static boolean opensAnUnbracedBody(String text) {
        // Whether the statement ENDS with a header, not whether it starts with
        // one. A rule is written with its openers and its condition on one line
        // and the body on the next:
        //   configurations.all { resolutionStrategy.eachDependency { d ->
        //           if (d.requested.name == 'kotlin-stdlib')
        //       d.useVersion '1.7.22'
        // Reading the first token found `configurations`, and the brace balance
        // is two open besides, so the condition and the useVersion were split into
        // separate statements -- neither of which says anything, which is how an
        // override in force went unread and the constraints went in beside it.
        int last = skipBlanksBackward(text, text.length() - 1);
        if (last < 0) {
            return false;
        }
        if (text.charAt(last) != ')') {
            // `else` stands alone; it is the only header with no condition.
            int start = last;
            while (start >= 0 && isIdentifierChar(text.charAt(start))) {
                start--;
            }
            return "else".equals(text.substring(start + 1, last + 1));
        }
        // The parenthesis that closes AT the end, found forward so a bracket
        // inside a string is not counted as one.
        List<Integer> opened = new ArrayList<Integer>();
        int opener = -1;
        for (int i = 0; i < text.length(); i++) {
            if (isLiteralStart(text, i)) {
                i = endOfStringLiteral(text, i);
                continue;
            }
            char c = text.charAt(i);
            if (c == '(') {
                opened.add(Integer.valueOf(i));
            } else if (c == ')' && !opened.isEmpty()) {
                int open = opened.remove(opened.size() - 1).intValue();
                if (i == last) {
                    opener = open;
                    break;
                }
            }
        }
        if (opener < 0) {
            return false;
        }
        int end = skipBlanksBackward(text, opener - 1);
        if (end < 0) {
            return false;
        }
        int start = end;
        while (start >= 0 && isIdentifierChar(text.charAt(start))) {
            start--;
        }
        // Only a header takes the next line as its body. `implementation('a:1.0')`
        // and `force('a:1.0')` end in a parenthesis too and take nothing.
        return UNBRACED_HEADERS.indexOf(
                " " + text.substring(start + 1, end + 1) + " ") >= 0;
    }

    /** The words that introduce a body, braced or not. */
    private static final String UNBRACED_HEADERS = " if else while for ";

    /** Whether the text so far ends with Groovy's line-continuation backslash. */
    private static boolean endsWithLineContinuation(StringBuilder current) {
        return current.length() > 0
                && current.charAt(current.length() - 1) == '\\';
    }

    /** Whether the text so far ends with a comma, ignoring trailing blanks. */
    private static boolean endsWithComma(StringBuilder text) {
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (isBlank(c)) {
                continue;
            }
            return c == ',';
        }
        return false;
    }

    /** Whether the statement is nothing but the start of a closure. */
    private static boolean opensAClosure(String statement) {
        String trimmed = statement.trim();
        return trimmed.startsWith("{");
    }

    /**
     * The brace balance of what follows a declaration's coordinate, which is
     * the only part that can be its own trailing closure.
     *
     * <p>Counting the whole statement caught the ENCLOSING block's opener when
     * a fragment put its first dependency on the same line as it --
     * {@code dependencies { implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.9.22'}
     * -- and the declaration then swallowed every following statement up to the
     * closing brace, so an unrelated {@code strictly} further down read as a
     * pin on the stdlib and silenced the whole block. A block opener sits
     * BEFORE the coordinate and a trailing closure after it, so counting from
     * the end of the last string literal separates them.</p>
     */
    private static int trailingBraceBalance(String statement) {
        // From the end of the COORDINATE literal, not the last literal: a trailing
        // closure carries strings of its own -- an exclusion's module name, a strict
        // version -- and counting after those missed the closure's own opening brace.
        for (int i = 0; i < statement.length(); i++) {
            char c = statement.charAt(i);
            if (!isLiteralStart(statement, i)) {
                continue;
            }
            int end = endOfStringLiteral(statement, i);
            if (stringLiteralContent(statement, i).startsWith(KOTLIN_GROUP)) {
                return braceBalance(statement.substring(Math.min(end + 1,
                        statement.length())));
            }
            i = end;
        }
        return braceBalance(statement);
    }

    /** How far a statement opens or closes braces, ignoring those in strings. */
    private static int braceBalance(String statement) {
        int depth = 0;
        for (int i = 0; i < statement.length(); i++) {
            char c = statement.charAt(i);
            if (isLiteralStart(statement, i)) {
                i = endOfStringLiteral(statement, i);
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
        }
        return depth;
    }

}
