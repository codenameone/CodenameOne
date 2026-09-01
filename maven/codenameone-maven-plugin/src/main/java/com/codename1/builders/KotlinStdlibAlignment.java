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
 * <p><b>Why the Kotlin Gradle plugin only sometimes excuses this.</b> From
 * 1.8.0 the plugin aligns the jdk variants itself, so the block would be a
 * no-op and is skipped. An older plugin does not, and skipping there was a
 * real bug: the versions were resolved with Gradle rather than reasoned
 * about, and a project on the {@code android.useGradle8=false} path -- where
 * this builder selects Kotlin 1.7.22 -- resolves like this:</p>
 *
 * <pre>
 * plugin 1.7.22 alone            stdlib 1.7.22 + jdk7/jdk8 1.7.22   no duplicate
 * plugin 1.7.22 + billing 9.1.0  stdlib 1.8.22 + jdk7/jdk8 1.7.22   DUPLICATE
 * the same, with this block      stdlib 1.8.22 + jdk7/jdk8 1.8.0    fixed
 * </pre>
 *
 * <p>Note the middle row is worse than a transitive accident: the 1.7.x
 * plugin <i>adds</i> {@code kotlin-stdlib-jdk8} at its own version, so the
 * older real jar is guaranteed present rather than merely possible, and any
 * dependency that reaches a merged stdlib collides with it. Hence the test
 * is the applied plugin's version, not whether a plugin is applied at all,
 * and an unreadable version counts as "does not align" so the block is
 * written rather than skipped.</p>
 *
 * <p><b>The cost of that, stated plainly.</b> On the same pre-1.8 plugin
 * path, an app whose graph contains no merged stdlib (the first row above)
 * did not need the block, and gets its stdlib family raised to
 * {@value #MERGED_STDLIB_FLOOR} anyway -- newer than the compiler in use,
 * which Kotlin warns about. That is deliberate. Gradle cannot express a
 * constraint conditional on what another module resolved to, so the choice
 * is between a warning in the case that did not need help and a failed build
 * in the case that did, and a warning is the better of the two. Raising the
 * builder's own pre-Gradle-8 Kotlin default would remove even that, and is a
 * bigger change than this one should carry.</p>
 *
 * <p>Extracted into a pure static helper so it is unit-testable without a
 * Gradle run and so the BuildDaemon copy stays trivially diffable --
 * <b>keep this file in sync with its twin in the other repository</b>.</p>
 */
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
     * The marker that suppresses the whole block rather than one artifact.
     * A BOM aligns every module in the {@code org.jetbrains.kotlin} group,
     * which is a superset of what this class does, so an app using one has
     * already answered the question for both artifacts.
     */
    private static final String KOTLIN_BOM = "kotlin-bom";

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
     * @param appliedKotlinPluginVersion the version of the Kotlin Gradle
     *   plugin this build applies, or null/empty when it applies none. Only
     *   {@value #MERGED_STDLIB_FLOOR} and newer align the jdk variants
     *   themselves; anything older, or anything this cannot read, is treated
     *   as not aligning and the block is written.
     * @param appGradleFragments the Gradle text the app itself contributed
     *   ({@code gradleDependencies}, {@code android.gradleDep} and the like).
     *   An artifact the app names there is left to the app; the Kotlin BOM
     *   suppresses both. Null entries are ignored.
     * @return the block, newline terminated, or {@code ""}
     */
    public static String constraintsBlock(String configuration,
            String appliedKotlinPluginVersion, String... appGradleFragments) {
        if (alignsItsOwnJdkVariants(appliedKotlinPluginVersion)) {
            return "";
        }
        if (configuration == null || configuration.trim().length() == 0) {
            return "";
        }
        if (contains(KOTLIN_BOM, appGradleFragments)) {
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
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < ALIGNED_ARTIFACTS.length; i++) {
            if (contains(ALIGNED_ARTIFACTS[i], appGradleFragments)) {
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
     * Whether a Kotlin Gradle plugin of this version aligns the jdk stdlib
     * variants on its own, making this class's block a no-op.
     *
     * <p>Answered from the version rather than from "is a plugin applied",
     * because the two differ exactly where it matters. Unknown reads as
     * false: a version that cannot be parsed -- an app declaring
     * {@code kotlin-gradle-plugin:$kotlin_version} produces one -- must not
     * silently switch the alignment off.</p>
     */
    public static boolean alignsItsOwnJdkVariants(String kotlinPluginVersion) {
        if (kotlinPluginVersion == null) {
            return false;
        }
        // Shared with the Health Connect floor check rather than parsed again here:
        // it already drops a qualifier, which rounds a prerelease up to its release
        // and is the forgiving direction for a floor.
        String numeric = HealthManifestFragments.numericVersionPrefix(
                kotlinPluginVersion.trim());
        if (numeric == null) {
            return false;
        }
        return compareVersions(numeric, MERGED_STDLIB_FLOOR) >= 0;
    }

    /** Whether any fragment names this coordinate. */
    private static boolean contains(String marker, String[] appGradleFragments) {
        if (appGradleFragments == null) {
            return false;
        }
        for (int i = 0; i < appGradleFragments.length; i++) {
            String fragment = appGradleFragments[i];
            if (fragment != null && fragment.contains(marker)) {
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
