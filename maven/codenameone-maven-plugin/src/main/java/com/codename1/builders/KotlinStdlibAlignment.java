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
 * {@code kotlin-stdlib} and left the two shims empty. A graph that reaches
 * {@code kotlin-stdlib} 1.8 or newer through one dependency and an older
 * {@code kotlin-stdlib-jdk8} through another therefore carries the same classes
 * twice, and the build fails in {@code checkDuplicateClasses} naming Kotlin
 * artifacts the app never asked for. The 1.8.x line ships no Gradle module
 * metadata saying the two overlap, so nothing tells Gradle to align them; from
 * 1.9.22 JetBrains ships that metadata itself. This supplies for 1.8.x what
 * JetBrains supplies later.</p>
 *
 * <p><b>Why a constraint.</b> A constraint raises a version and never lowers
 * one, and never pulls a module into a graph that does not already contain it.
 * An app with no Kotlin anywhere is completely unaffected -- the block resolves
 * to nothing.</p>
 *
 * <p><b>Why the guard is blunt.</b> The one thing a constraint at the floor can
 * break is an app that FIRMLY holds a member of the family below it: a strict
 * pin, a force, a rejection, an enforced BOM, or a conflict-failing resolution
 * strategy. Such a graph resolves coherently today, and a constraint requiring
 * 1.8.0 turns it into {@code Could not resolve ... {strictly 1.7.22}} -- the one
 * outcome this must never produce.</p>
 *
 * <p>Deciding that by reading the app's Gradle text properly needs a Groovy
 * parser. This class WAS one: some 2,200 lines and 130 methods tracking
 * definitions, scopes, map notation, rich versions, resolution rules and
 * component selection. It was reviewed into the ground, and rightly -- every
 * round turned up another spelling it read wrongly, because Groovy has
 * unboundedly many of them and an approximate parser is unboundedly wrong. None
 * of that machinery ever changed the outcome for the graphs this exists to fix,
 * which reach the shims transitively and name them nowhere.</p>
 *
 * <p>So the question is asked bluntly: does the app's Gradle text name this
 * family at all, AND mention any of the words that can hold a version down? If
 * so, stand down and say so in the log. That over-suppresses -- a {@code force}
 * on an unrelated library in a script that also happens to name
 * {@code kotlin-stdlib} is enough, and a word inside a comment or a string
 * counts. Over-suppressing costs an app the duplicate it already had, which is
 * exactly what {@code android.kotlinStdlibAlignment=false} does deliberately.
 * Under-suppressing breaks a build that works today. The asymmetry is the whole
 * design.</p>
 */
public class KotlinStdlibAlignment {

    /**
     * The version at which the shims became empty, and the floor this raises
     * them to.
     */
    public static final String MERGED_STDLIB_FLOOR = "1.8.0";

    /** The two shims whose classes moved, and which this raises. */
    private static final String[] ALIGNED_ARTIFACTS = {
        "kotlin-stdlib-jdk7",
        "kotlin-stdlib-jdk8"
    };

    /**
     * The family, as the one name that prefixes all three of them --
     * {@code kotlin-stdlib}, {@code kotlin-stdlib-jdk7} and
     * {@code kotlin-stdlib-jdk8}.
     */
    private static final String STDLIB_FAMILY = "kotlin-stdlib";

    /**
     * The words that can hold a version where this would raise it.
     *
     * <p>Gradle's ways of doing that, plus {@code failOnVersionConflict}, which
     * turns the raise itself into a build failure. Matched as plain text: the
     * point of this list is to be crude and complete rather than precise, since
     * a false match only declines to help.</p>
     */
    private static final String[] PINNING_WORDS = {
        "strictly",
        "!!",
        "force",
        "reject",
        "enforcedPlatform",
        "useVersion",
        "useTarget",
        "substitute",
        "failOnVersionConflict"
    };

    private KotlinStdlibAlignment() {
    }

    /**
     * The {@code constraints} block to append inside the generated
     * {@code dependencies { }}, or an empty string when no alignment should be
     * written.
     *
     * @param configuration the dependency configuration to declare the
     *   constraints on, {@code implementation} on any AndroidX project. The
     *   caller passes the same name it uses for the rest of the block so a
     *   legacy {@code compile} project stays consistent with itself.
     * @param appGradleFragments the Gradle text the app itself contributed
     *   ({@code android.gradleDep}, {@code android.xgradle} and the like).
     *   Order and nesting do not matter -- the whole lot is read as one piece of
     *   text. Null entries are ignored.
     * @return the block, newline terminated, or {@code ""}
     */
    public static String constraintsBlock(String configuration,
            String... appGradleFragments) {
        if (configuration == null || configuration.trim().length() == 0) {
            return "";
        }
        if (appPinsTheStdlibFamily(appGradleFragments)) {
            return "";
        }
        String config = configuration.trim();
        // "because" is not decoration: it is what `gradle dependencyInsight`
        // prints next to the raised version, and this constraint is otherwise
        // unattributable to anything in the developer's project.
        String because = "Codename One: kotlin-stdlib " + MERGED_STDLIB_FLOOR
                + " absorbed the jdk7/jdk8 classes and the 1.8.x line ships no "
                + "Gradle module metadata to say so, so these are raised to the "
                + "empty shims to avoid a duplicate class in checkDuplicateClasses";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < ALIGNED_ARTIFACTS.length; i++) {
            out.append("        ").append(config)
               .append("('org.jetbrains.kotlin:").append(ALIGNED_ARTIFACTS[i])
               .append(':').append(MERGED_STDLIB_FLOOR).append("') {\n")
               .append("            because '").append(because).append("'\n")
               .append("        }\n");
        }
        return "    constraints {\n" + out + "    }\n";
    }

    /**
     * Whether the app's own Gradle text suggests it holds this family where the
     * constraint would raise it.
     *
     * <p>Both halves are required. An app that never names the family cannot be
     * pinning it, and one that names it without any of these words is declaring
     * an ordinary version -- which is a SOFT requirement in Gradle, so the
     * constraint raises it and the two agree.</p>
     *
     * <p>Public because the builder logs a notice when it answers yes: an
     * alignment that silently does not happen is the kind of thing support
     * cannot explain afterwards.</p>
     */
    public static boolean appPinsTheStdlibFamily(String... appGradleFragments) {
        if (appGradleFragments == null) {
            return false;
        }
        StringBuilder all = new StringBuilder();
        for (int i = 0; i < appGradleFragments.length; i++) {
            if (appGradleFragments[i] != null) {
                all.append(appGradleFragments[i]).append('\n');
            }
        }
        String text = all.toString();
        if (text.indexOf(STDLIB_FAMILY) < 0) {
            return false;
        }
        for (int i = 0; i < PINNING_WORDS.length; i++) {
            if (text.indexOf(PINNING_WORDS[i]) >= 0) {
                return true;
            }
        }
        return false;
    }
}
