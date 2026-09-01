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
        if (appGradleFragments == null) {
            return false;
        }
        for (int i = 0; i < appGradleFragments.length; i++) {
            String[] lines = activeLines(appGradleFragments[i]);
            for (int j = 0; j < lines.length; j++) {
                if (!callsStrictly(lines[j]) || !namesBaseStdlib(lines[j])) {
                    continue;
                }
                if (belowTheFloor(strictVersionIn(lines[j]))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether the statement names {@code kotlin-stdlib} and not a longer name. */
    private static boolean namesBaseStdlib(String line) {
        return namesCoordinate(line, BASE_STDLIB)
                || (line.contains(KOTLIN_GROUP)
                        && declaresMapEntry(line, "name", BASE_STDLIB));
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
        char quote = 0;
        int stringStart = -1;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quote != 0) {
                if (c == '\\' && i + 1 < line.length()) {
                    i++;
                } else if (c == quote) {
                    String literal = line.substring(stringStart + 1, i);
                    // Dependency notation carries no whitespace; a reason sentence
                    // does. Without that, a reason that merely OPENS with the
                    // coordinate -- because 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:
                    // 1.7.22 causes duplicate classes' -- read as the declaration it
                    // was warning about, and switched off the constraint that would
                    // have prevented exactly what it describes.
                    if ((literal.equals(coordinate)
                            || literal.startsWith(coordinate + ":"))
                            && !hasWhitespace(literal)) {
                        return true;
                    }
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
                stringStart = i;
            }
        }
        return false;
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
        // The same syntax-level call callsStrictly validated, not any occurrence of
        // the word: a reason reading `because "strictly '1.7.22' is not intended"`
        // otherwise supplies the version for a declaration whose real strict version
        // is something else entirely, and the wrong one decides whether the block is
        // written.
        for (int i = 0; i < statement.length(); i++) {
            char c = statement.charAt(i);
            if (c == '\'' || c == '"') {
                i = endOfStringLiteral(statement, i);
                continue;
            }
            if (!statement.startsWith(STRICTLY, i)) {
                continue;
            }
            boolean startsToken = i == 0 || !isIdentifierChar(statement.charAt(i - 1));
            if (!startsToken) {
                continue;
            }
            int after = skipBlanks(statement, i + STRICTLY.length());
            if (after < statement.length() && statement.charAt(after) == '(') {
                after = skipBlanks(statement, after + 1);
            }
            if (after < statement.length()
                    && (statement.charAt(after) == '\''
                            || statement.charAt(after) == '"')) {
                int end = endOfStringLiteral(statement, after);
                if (end < statement.length()) {
                    return statement.substring(after + 1, end);
                }
            }
            i = after;
        }
        return null;
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
        int compared = compareVersions(version, MERGED_STDLIB_FLOOR);
        if (compared != 0) {
            return compared < 0;
        }
        return hasQualifier(version);
    }

    /** Whether the version carries anything after its numeric segments. */
    private static boolean hasQualifier(String version) {
        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);
            if (c != '.' && !Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    /** Numeric dotted version compare; a missing segment counts as zero. */
    private static int compareVersions(String left, String right) {
        String[] l = left.split("\\.");
        String[] r = right.split("\\.");
        int len = Math.max(l.length, r.length);
        for (int i = 0; i < len; i++) {
            int a = i < l.length ? parseSegment(l[i]) : 0;
            int b = i < r.length ? parseSegment(r[i]) : 0;
            if (a != b) {
                return a < b ? -1 : 1;
            }
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
        if (appGradleFragments == null) {
            return false;
        }
        for (int i = 0; i < appGradleFragments.length; i++) {
            String[] lines = activeLines(appGradleFragments[i]);
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
        if (!callsStrictly(line)
                && !declaresOnTheConstrainedConfiguration(configuration, line)) {
            return false;
        }
        if (namesCoordinate(line, artifact)) {
            return true;
        }
        // group: 'org.jetbrains.kotlin', name: 'kotlin-stdlib-jdk8', version: '...'
        return line.contains(KOTLIN_GROUP) && declaresMapEntry(line, "name", artifact);
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
    private static boolean callsStrictly(String statement) {
        for (int i = 0; i < statement.length(); i++) {
            char c = statement.charAt(i);
            if (c == '\'' || c == '"') {
                i = endOfStringLiteral(statement, i);
                continue;
            }
            if (statement.startsWith(STRICTLY, i)) {
                boolean startsToken = i == 0
                        || !isIdentifierChar(statement.charAt(i - 1));
                int after = i + STRICTLY.length();
                boolean endsToken = after < statement.length()
                        && (statement.charAt(after) == ' '
                                || statement.charAt(after) == '('
                                || statement.charAt(after) == '\t');
                if (startsToken && endsToken) {
                    return true;
                }
            }
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
        int at = line.indexOf(key);
        while (at >= 0) {
            boolean startsToken = at == 0
                    || !isIdentifierChar(line.charAt(at - 1));
            if (startsToken) {
                int i = skipBlanks(line, at + key.length());
                if (i < line.length() && line.charAt(i) == ':') {
                    i = skipBlanks(line, i + 1);
                    if (i < line.length()
                            && (line.charAt(i) == '\'' || line.charAt(i) == '"')) {
                        char q = line.charAt(i);
                        int end = line.indexOf(q, i + 1);
                        if (end > i && line.substring(i + 1, end).equals(value)) {
                            return true;
                        }
                    }
                }
            }
            at = line.indexOf(key, at + 1);
        }
        return false;
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
    private static int endOfStringLiteral(String text, int quoteAt) {
        char quote = text.charAt(quoteAt);
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

    private static int skipBlanks(String line, int from) {
        int i = from;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
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

    /** Whether this line declares on {@code configuration}, as a whole token. */
    private static boolean declaresOn(String configuration, String line) {
        char quote = 0;
        int stringStart = -1;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quote != 0) {
                if (c == '\\' && i + 1 < line.length()) {
                    i++;
                } else if (c == quote) {
                    // A configuration name inside a string counts in one place only:
                    // as the first argument of dependencies.add("runtimeOnly", "..").
                    // Accepting any quoted occurrence read the word in a reason --
                    // because 'implementation workaround' -- as a main-variant
                    // declaration, which suppressed the constraint for a dependency
                    // that only affects debug.
                    if (line.substring(stringStart + 1, i).equals(configuration)
                            && isAddCallArgument(line, stringStart)) {
                        return true;
                    }
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
                stringStart = i;
                continue;
            }
            if (line.startsWith(configuration, i)) {
                boolean startsToken = i == 0
                        || !isIdentifierChar(line.charAt(i - 1));
                int after = i + configuration.length();
                boolean endsToken = after < line.length()
                        && (line.charAt(after) == ' ' || line.charAt(after) == '('
                                || line.charAt(after) == '\t');
                if (startsToken && endsToken) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether the string literal opening at {@code quoteAt} is an add() argument. */
    private static boolean isAddCallArgument(String line, int quoteAt) {
        int i = quoteAt - 1;
        while (i >= 0 && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i--;
        }
        if (i < 0 || line.charAt(i) != '(') {
            return false;
        }
        i--;
        while (i >= 0 && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i--;
        }
        return i >= 2 && "add".equals(line.substring(i - 2, i + 1))
                && (i - 3 < 0 || !isIdentifierChar(line.charAt(i - 3)));
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
        char quote = 0;
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
            if (quote != 0) {
                out.append(c);
                if (c == '\\' && i + 1 < fragment.length()) {
                    out.append(fragment.charAt(i + 1));
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
                out.append(c);
                continue;
            }
            if (c == '/' && i + 1 < fragment.length()) {
                char next = fragment.charAt(i + 1);
                if (next == '*') {
                    inBlockComment = true;
                    i++;
                    continue;
                }
                if (next == '/') {
                    while (i < fragment.length() && fragment.charAt(i) != '\n') {
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
        char quote = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                current.append(c);
                // Escapes, because the comment stripper beside this already handles
                // them: 'can\'t' otherwise closes the string on the apostrophe it is
                // escaping, and every following newline is read as being inside a
                // string rather than ending a statement -- which merges statements
                // that must stay apart.
                if (c == '\\' && i + 1 < text.length()) {
                    current.append(text.charAt(i + 1));
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (depth > 0) {
                    depth--;
                }
            } else if ((c == '\n' || c == ';') && depth == 0) {
                // A trailing comma continues the statement. Groovy's parenthesis-free
                // map notation spreads one declaration over several lines --
                //   implementation group: 'org.jetbrains.kotlin',
                //                  name: 'kotlin-stdlib-jdk8',
                //                  version: '1.7.22'
                // -- and splitting there left the configuration, the group, the
                // artifact and any closure in four statements, none of which is a
                // declaration on its own.
                if (c == '\n' && endsWithComma(current)) {
                    current.append(' ');
                    continue;
                }
                out.add(current.toString().replace('\n', ' '));
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
                out.add(current.toString().replace('\n', ' '));
            }
        }
        // A declaration's own configuration block belongs to it: the version that
        // decides this is written as `version { strictly '1.7.22' }` on the line after
        // the coordinate. Only a statement that already names the Kotlin group absorbs
        // its block, so a `dependencies {` or `android {` opening cannot swallow the
        // fragment -- the blast radius is one declaration, never the file.
        List<String> merged = new ArrayList<String>();
        for (int i = 0; i < out.size(); i++) {
            String statement = out.get(i);
            if (statement.contains(KOTLIN_GROUP)) {
                // A trailing closure may sit on the line AFTER the call's closing
                // parenthesis -- Gradle accepts it and the strictly inside really does
                // apply, checked by watching a competing higher requirement fail
                // against it. The parenthesis depth is already back to zero there, so
                // without this the closure lands in its own statement and the version
                // it carries is never associated with the coordinate above it.
                while (i + 1 < out.size() && opensAClosure(out.get(i + 1))) {
                    i++;
                    statement = statement + " " + out.get(i);
                }
                int braces = trailingBraceBalance(statement);
                while (braces > 0 && i + 1 < out.size()) {
                    i++;
                    statement = statement + " " + out.get(i);
                    braces += braceBalance(out.get(i));
                }
            }
            merged.add(statement);
        }
        return inlineLiteralDefinitions(merged);
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
    private static String[] inlineLiteralDefinitions(List<String> statements) {
        Map<String, String> literals = new LinkedHashMap<String, String>();
        for (int i = 0; i < statements.size(); i++) {
            collectLiteralDefinition(statements.get(i), literals);
        }
        String[] out = new String[statements.size()];
        for (int i = 0; i < statements.size(); i++) {
            out[i] = literals.isEmpty()
                    ? statements.get(i)
                    : withLiteralsInlined(statements.get(i), literals);
        }
        return out;
    }

    /** Records a {@code def name = 'literal'} definition, if this is one. */
    private static void collectLiteralDefinition(String statement,
            Map<String, String> literals) {
        int at = statement.indexOf(DEF);
        if (at < 0) {
            return;
        }
        boolean startsToken = at == 0 || !isIdentifierChar(statement.charAt(at - 1));
        if (!startsToken) {
            return;
        }
        int i = skipBlanks(statement, at + DEF.length());
        int nameStart = i;
        while (i < statement.length() && isIdentifierChar(statement.charAt(i))) {
            i++;
        }
        if (i == nameStart) {
            return;
        }
        String name = statement.substring(nameStart, i);
        i = skipBlanks(statement, i);
        if (i >= statement.length() || statement.charAt(i) != '=') {
            return;
        }
        i = skipBlanks(statement, i + 1);
        if (i >= statement.length()
                || (statement.charAt(i) != '\'' && statement.charAt(i) != '"')) {
            return;
        }
        int end = endOfStringLiteral(statement, i);
        if (end >= statement.length()) {
            return;
        }
        literals.put(name, statement.substring(i, end + 1));
    }

    /** The statement with known definition names replaced by their literals. */
    private static String withLiteralsInlined(String statement,
            Map<String, String> literals) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < statement.length(); i++) {
            char c = statement.charAt(i);
            if (c == '\'' || c == '"') {
                int end = endOfStringLiteral(statement, i);
                out.append(statement, i, Math.min(end + 1, statement.length()));
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
            out.append(literal == null ? token : literal);
            i = end - 1;
        }
        return out.toString();
    }

    private static final String DEF = "def";

    /** Whether the text so far ends with a comma, ignoring trailing blanks. */
    private static boolean endsWithComma(StringBuilder text) {
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == ' ' || c == '\t' || c == '\r') {
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
            if (c != '\'' && c != '"') {
                continue;
            }
            int end = endOfStringLiteral(statement, i);
            if (statement.substring(i + 1, Math.min(end, statement.length()))
                    .startsWith(KOTLIN_GROUP)) {
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
            if (c == '\'' || c == '"') {
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
