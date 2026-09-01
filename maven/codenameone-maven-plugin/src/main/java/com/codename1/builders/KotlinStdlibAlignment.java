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
import java.util.List;

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

    /**
     * The marker that can suppress the whole block rather than one artifact.
     * A BOM manages every module in the {@code org.jetbrains.kotlin} group,
     * jdk7 and jdk8 included, so a new enough one answers the question for
     * both artifacts at once.
     *
     * <p><b>Only a new enough one.</b> A BOM raises the jdk artifacts but
     * cannot pull {@code kotlin-stdlib} back down, because a platform
     * contributes constraints and the highest version still wins. So a
     * pre-merge BOM leaves exactly the arrangement this class exists to
     * prevent -- measured, with the same graph as the class comment's
     * table:</p>
     *
     * <pre>
     * no BOM           stdlib 1.8.22 + jdk7/jdk8 1.6.21   duplicate
     * kotlin-bom 1.7.22  stdlib 1.8.22 + jdk7/jdk8 1.7.22   STILL a duplicate
     * kotlin-bom 1.9.22  all 1.9.22, jdk artifacts shims    safe
     * </pre>
     *
     * <p>The BOM is therefore tested by version, exactly like the Kotlin
     * Gradle plugin above it, and for the same reason: presence is not
     * alignment.</p>
     */
    private static final String KOTLIN_BOM = "kotlin-bom";

    /** The coordinate a BOM's version is read from. */
    private static final String KOTLIN_BOM_COORDINATE =
            "org.jetbrains.kotlin:kotlin-bom:";

    /** The group every artifact this class reasons about belongs to. */
    private static final String KOTLIN_GROUP = "org.jetbrains.kotlin";

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
        if (declaresArtifact(KOTLIN_BOM, config, appGradleFragments)
                && atOrPastTheMerge(declaredVersion(
                        KOTLIN_BOM_COORDINATE, config, appGradleFragments))) {
            return "";
        }
        // "because" is not decoration: it is what `gradle dependencyInsight` prints
        // next to the raised version, and this constraint is otherwise unattributable
        // to anything in the developer's project.
        String because = "Codename One: kotlin-stdlib " + MERGED_STDLIB_FLOOR
                + " absorbed the jdk7/jdk8 classes and the 1.8.x line ships no "
                + "Gradle module metadata to say so, so these are raised to the "
                + "empty shims to avoid a duplicate class in checkDuplicateClasses";
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
     * Whether a Kotlin version is at or past the release that merged the jdk
     * artifacts away, and therefore aligns them wherever it is in force --
     * as the Gradle plugin's version or as a BOM's.
     *
     * <p>Unknown reads as false everywhere it is used. A version that cannot
     * be parsed -- {@code kotlin-gradle-plugin:$kotlin_version} and
     * {@code kotlin-bom:$kotlinVersion} both produce one -- must not silently
     * switch the alignment off.</p>
     */
    private static boolean atOrPastTheMerge(String kotlinVersion) {
        if (kotlinVersion == null) {
            return false;
        }
        // Shared with the Health Connect floor check rather than parsed again here:
        // it already drops a qualifier, which rounds a prerelease up to its release
        // and is the forgiving direction for a floor.
        String numeric = HealthManifestFragments.numericVersionPrefix(
                kotlinVersion.trim());
        if (numeric == null) {
            return false;
        }
        return compareVersions(numeric, MERGED_STDLIB_FLOOR) >= 0;
    }

    /**
     * The version an app's own Gradle text declares immediately after
     * {@code coordinate}, or null when it declares none there or writes one
     * this cannot read -- a Gradle variable rather than a literal.
     */
    private static String declaredVersion(String coordinate, String configuration,
            String[] appGradleFragments) {
        if (appGradleFragments == null) {
            return null;
        }
        for (int i = 0; i < appGradleFragments.length; i++) {
            // The same active text the declaration check reads, so a commented-out
            // BOM cannot supply the version that suppresses the block.
            String[] lines = activeLines(appGradleFragments[i]);
            for (int j = 0; j < lines.length; j++) {
                String fragment = lines[j];
                // Same configuration filter as the declaration check, so a debug-only
                // BOM cannot supply the version that suppresses the main variant's
                // constraints.
                if (!declaresOnTheConstrainedConfiguration(configuration, fragment)) {
                    continue;
                }
                int at = fragment.indexOf(coordinate);
                if (at < 0) {
                    continue;
                }
                int from = at + coordinate.length();
                int to = from;
                while (to < fragment.length()
                        && "0123456789.".indexOf(fragment.charAt(to)) >= 0) {
                    to++;
                }
                while (to > from && fragment.charAt(to - 1) == '.') {
                    to--;
                }
                return to > from ? fragment.substring(from, to) : null;
            }
        }
        return null;
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
        if (!line.contains("strictly")
                && !declaresOnTheConstrainedConfiguration(configuration, line)) {
            return false;
        }
        if (line.contains(KOTLIN_GROUP + ":" + artifact)) {
            return true;
        }
        // group: 'org.jetbrains.kotlin', name: 'kotlin-stdlib-jdk8', version: '...'
        return line.contains(KOTLIN_GROUP)
                && (line.contains("name: '" + artifact + "'")
                        || line.contains("name: \"" + artifact + "\""));
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
        int at = line.indexOf(configuration);
        while (at >= 0) {
            boolean startsToken = at == 0
                    || !Character.isLetterOrDigit(line.charAt(at - 1));
            int after = at + configuration.length();
            boolean endsToken = after < line.length()
                    && (line.charAt(after) == ' ' || line.charAt(after) == '('
                            || line.charAt(after) == '\t');
            if (startsToken && endsToken) {
                return true;
            }
            at = line.indexOf(configuration, at + 1);
        }
        return false;
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
                if (c == quote) {
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
                int braces = braceBalance(statement);
                while (braces > 0 && i + 1 < out.size()) {
                    i++;
                    statement = statement + " " + out.get(i);
                    braces += braceBalance(out.get(i));
                }
            }
            merged.add(statement);
        }
        return merged.toArray(new String[merged.size()]);
    }

    /** How far a statement opens or closes braces, ignoring those in strings. */
    private static int braceBalance(String statement) {
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < statement.length(); i++) {
            char c = statement.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
        }
        return depth;
    }

    /** Numeric dotted version compare; a missing segment counts as zero. */
    private static int compareVersions(String left, String right) {
        String[] l = left.split("\\.");
        String[] r = right.split("\\.");
        int len = Math.max(l.length, r.length);
        for (int i = 0; i < len; i++) {
            int a = i < l.length ? parse(l[i]) : 0;
            int b = i < r.length ? parse(r[i]) : 0;
            if (a != b) {
                return a < b ? -1 : 1;
            }
        }
        return 0;
    }

    private static int parse(String segment) {
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException notANumber) {
            return 0;
        }
    }
}
