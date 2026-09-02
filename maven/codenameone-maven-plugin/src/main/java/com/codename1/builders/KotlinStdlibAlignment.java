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

import java.util.Locale;

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
     * The names that mean "this family".
     *
     * <p>{@code kotlin-stdlib} prefixes all three of the modules themselves --
     * {@code kotlin-stdlib}, {@code kotlin-stdlib-jdk7} and
     * {@code kotlin-stdlib-jdk8}. {@code kotlin-bom} is the platform that
     * manages them, and it is the real coordinate: there is no
     * {@code kotlin-stdlib-bom}, which is what an earlier test here asserted
     * against, so an enforced BOM went unseen.</p>
     */
    private static final String[] FAMILY_NAMES = {
        "kotlin-stdlib",
        "kotlin-bom"
    };

    /**
     * Text that means the Kotlin toolchain is in this build at all.
     *
     * <p>Applying a Kotlin Gradle plugin makes it the owner of the stdlib: it
     * declares one at the compiler's own version, and on the Gradle 6 and 7
     * path that version is below this floor. This stands the alignment down
     * without the family being named anywhere, because whoever applied the
     * plugin need not have named it -- the plugin does that itself. It is the
     * same reason as {@code projectCompilesKotlin}, reached the other way: that
     * one is a source scan under {@code src/main/java}, and Kotlin can arrive
     * from a source set the scan never looks at.</p>
     */
    private static final String[] KOTLIN_TOOLCHAIN_WORDS = {
        "kotlin-gradle-plugin",
        "kotlin-android",
        "org.jetbrains.kotlin.android"
    };

    /**
     * The words that can hold a version where this would raise it.
     *
     * <p>Gradle's ways of doing that, plus {@code failOnVersionConflict}, which
     * turns the raise itself into a build failure. Matched as plain text: the
     * point of this list is to be crude and complete rather than precise, since
     * a false match only declines to help.</p>
     *
     * <p>Lower case, because the text is lower cased before the search. The
     * same act appears in more than one spelling -- {@code force} the command
     * and {@code setForcedModules} the setter -- and a case-sensitive
     * {@code force} finds the first and misses the second.</p>
     *
     * <p>{@code require} is here for its bounded form. A plain
     * {@code require '1.7.22'} is soft and the constraint raises it happily,
     * but {@code require '[1.7,1.8)'} excludes the floor, and no version then
     * satisfies both. Standing down for the unbounded case as well is the
     * cheap side of the trade.</p>
     */
    private static final String[] PINNING_WORDS = {
        "strictly",
        "!!",
        "force",
        "reject",
        "require",
        "enforcedplatform",
        "useversion",
        "usetarget",
        "substitute",
        "failonversionconflict"
    };

    private KotlinStdlibAlignment() {
    }

    /**
     * The {@code constraints} block to append inside the generated
     * {@code dependencies { }}, or an empty string when no alignment should be
     * written.
     *
     * @param projectCompilesKotlin whether the project has Kotlin sources, and
     *   so gets a Kotlin Gradle plugin and a stdlib declaration at the
     *   compiler's own version. That version is the one the app's own classes
     *   are compiled against, and it is below this floor on the Gradle 6 and 7
     *   path. Raising the shims there pulls the base stdlib up with them --
     *   the empty shims depend on it -- and a compiler reading a stdlib newer
     *   than itself reports "Module was compiled with an incompatible version
     *   of Kotlin", turning a Kotlin app that builds today into one that does
     *   not. This alignment is for the Java-only graph that reaches the shims
     *   transitively; where the project compiles Kotlin, the Kotlin plugin
     *   owns the family.
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
            boolean projectCompilesKotlin, String... appGradleFragments) {
        if (configuration == null || configuration.trim().length() == 0) {
            return "";
        }
        if (projectCompilesKotlin) {
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
        // Locale.ENGLISH, not the default: a Turkish default locale lower cases
        // I to a dotless i, which turns "STRICTLY" into "str\u0131ctly" and
        // matches nothing. The same trap is commented in AndroidGradleBuilder.
        String text = all.toString().toLowerCase(Locale.ENGLISH);
        for (int i = 0; i < KOTLIN_TOOLCHAIN_WORDS.length; i++) {
            if (text.indexOf(KOTLIN_TOOLCHAIN_WORDS[i]) >= 0) {
                return true;
            }
        }
        boolean namesTheFamily = false;
        for (int i = 0; i < FAMILY_NAMES.length; i++) {
            if (text.indexOf(FAMILY_NAMES[i]) >= 0) {
                namesTheFamily = true;
                break;
            }
        }
        if (!namesTheFamily) {
            // Nothing else here can be about this family. Note what is NOT
            // reachable from the Gradle text: a gradle.lockfile, which Gradle
            // enforces as a strict constraint. This builder writes the project
            // from scratch and has no dependency locking, no lock file and no
            // hint that ships one, so there is no lock to read -- and locking
            // with no lock state does nothing. Revisit this if the builder ever
            // grows a way to carry files into the generated project.
            return false;
        }
        for (int i = 0; i < PINNING_WORDS.length; i++) {
            if (text.indexOf(PINNING_WORDS[i]) >= 0) {
                return true;
            }
        }
        return containsAVersionRange(text);
    }

    /**
     * Whether the text carries a Gradle version range, which needs no keyword
     * at all.
     *
     * <p>{@code 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:[1.7,1.8)'} is an
     * ordinary coordinate as far as the vocabulary above is concerned, and it
     * excludes the floor: constraining to 1.8.0 leaves Gradle nothing that
     * satisfies both, so a build that resolves today stops resolving.</p>
     *
     * <p>Two signatures. A bracket against a digit -- {@code [1.7.22]} admits
     * exactly one version and has no comma at all -- and a comma with digits on
     * its left and digits or a closing bracket on its right, which is the only
     * place a comma appears inside a version. Map notation --
     * {@code group: 'x', name: 'y', version: '1.8.0'} -- puts a quote to the
     * left of every comma and is the case this must not fire on, since
     * declaring the family that way is ordinary and gets raised. Anything else
     * that happens to put a digit either side of a comma is a false match, and
     * a false match only declines to help.</p>
     */
    private static boolean containsAVersionRange(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '[' && c != ']') {
                continue;
            }
            int after = i + 1;
            while (after < text.length() && text.charAt(after) == ' ') {
                after++;
            }
            if (after < text.length() && text.charAt(after) >= '0'
                    && text.charAt(after) <= '9') {
                return true;
            }
        }
        for (int i = text.indexOf(','); i >= 0; i = text.indexOf(',', i + 1)) {
            int before = i - 1;
            while (before >= 0 && text.charAt(before) == ' ') {
                before--;
            }
            int after = i + 1;
            while (after < text.length() && text.charAt(after) == ' ') {
                after++;
            }
            if (before < 0 || after >= text.length()) {
                continue;
            }
            char left = text.charAt(before);
            char right = text.charAt(after);
            if (left >= '0' && left <= '9'
                    && ((right >= '0' && right <= '9')
                        || right == ')' || right == ']' || right == '[')) {
                return true;
            }
        }
        return false;
    }
}
