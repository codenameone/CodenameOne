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
 * metadata saying the two overlap; from 1.9.22 JetBrains ships it.</p>
 *
 * <p><b>Why a capability and not a version constraint.</b> This was a
 * constraint raising both shims to the floor, and a constraint <em>raises a
 * version</em> -- which is a thing an app can be holding down. Measured against
 * a real Gradle, a strict pin or a {@code reject} on a shim turns into
 * {@code Could not resolve ... {strictly 1.6.21}}: a build that resolved before
 * the alignment and does not after it. Guarding that by reading the app's own
 * Gradle for signs of a pin is an unbounded problem, and every round of review
 * found another spelling it missed.</p>
 *
 * <p>Declaring the overlap as a <em>capability</em> has no such failure mode. It
 * states a fact -- from the floor up, {@code kotlin-stdlib} provides what the
 * shims provide -- and lets Gradle drop the redundant shim. No version moves,
 * so there is nothing for a pin, a force, an enforced BOM, a range, a lock or a
 * Kotlin compiler version to conflict with, and nothing to detect. That is why
 * this class has no inputs.</p>
 *
 * <p><b>Measured, not reasoned.</b> The emitted script was run against real
 * Gradle 6.5 (the builder's default) and 8.5 resolving from Maven Central, and
 * end to end through {@code checkDebugDuplicateClasses} in a real Android
 * project on AGP 8.1.4:</p>
 *
 * <ul>
 *   <li>stdlib 1.8.10 with {@code kotlin-stdlib-jdk8:1.6.21} -- the customer's
 *       failure, reproduced exactly: "Duplicate class
 *       kotlin.collections.jdk8.CollectionsJDK8Kt found in modules
 *       kotlin-stdlib-1.8.10 and kotlin-stdlib-jdk8-1.6.21". The task fails
 *       without this script and passes with it.</li>
 *   <li>the same, with the shim pinned {@code strictly}, or with
 *       {@code reject '[1.8.0,)'} -- resolves, and the Android build succeeds.
 *       The constraint version this replaced failed both, in the Android build
 *       too: "Could not resolve org.jetbrains.kotlin:kotlin-stdlib-jdk8:
 *       {strictly 1.6.21}". That is the whole reason for the change.</li>
 *   <li>an all-1.7 project -- untouched, because the stdlib only supersedes
 *       from the floor up, so shims that still carry real classes stay. This is
 *       also why a Kotlin compiler older than the floor is not a problem:
 *       nothing raises the stdlib under it.</li>
 *   <li>stdlib 1.8.0 with a NEWER {@code kotlin-stdlib-jdk8:1.9.0} -- resolves
 *       to 1.9.0 throughout, exactly as the untouched graph does. An earlier
 *       version of this reused the shims' own capability and evicted that shim,
 *       taking its requirement on stdlib 1.9.0 with it and silently downgrading
 *       the base module to 1.8.0.</li>
 *   <li>stdlib 1.9.22, a graph with no Kotlin at all, and this same rule applied
 *       twice -- all inert or clean.</li>
 *   <li>{@code failOnVersionConflict} with an old shim fails identically with
 *       this script and with no script at all: that graph is already broken.</li>
 * </ul>
 */
public class KotlinStdlibAlignment {

    /**
     * The version at which the shims became empty, and the version from which
     * {@code kotlin-stdlib} is declared to provide their capabilities.
     */
    public static final String MERGED_STDLIB_FLOOR = "1.8.0";

    /** The two shims whose classes moved into {@code kotlin-stdlib}. */
    private static final String[] ALIGNED_ARTIFACTS = {
        "kotlin-stdlib-jdk7",
        "kotlin-stdlib-jdk8"
    };

    /**
     * The group of the capability this declares, and the name suffix.
     *
     * <p>Ours, deliberately, rather than reusing the shims' own implicit
     * capability. That one is held by EVERY version of a shim, including the
     * empty ones at or above the floor -- and a conflict there has no right
     * answer: dropping the shim loses its requirement on a newer stdlib and
     * silently downgrades the base module, while dropping the stdlib leaves a
     * graph of empty shims with no stdlib in it at all. Both were measured.</p>
     *
     * <p>A capability only this declares is held by exactly two things: a
     * {@code kotlin-stdlib} at or above the floor, which supersedes the shims,
     * and a shim below it, which is superseded. So the conflict exists where the
     * duplicate exists and nowhere else. It cannot be removed from the shims
     * instead -- {@code removeCapability} does not remove an implicit one, which
     * was tried and measured too.</p>
     */
    private static final String CAPABILITY_GROUP = "com.codenameone";

    /** @see #CAPABILITY_GROUP */
    private static final String CAPABILITY_SUFFIX = "-superseded";

    private KotlinStdlibAlignment() {
    }

    /**
     * The alignment, as a self-contained script to append after the generated
     * {@code dependencies { }} block.
     *
     * <p>Self-contained because it needs two different scopes: the component
     * metadata rule belongs inside {@code dependencies}, the resolution
     * strategy outside it. It opens its own {@code dependencies} block rather
     * than making the caller splice two pieces into two places.</p>
     *
     * @return the script, newline terminated
     */
    public static String alignmentScript() {
        String major = MERGED_STDLIB_FLOOR.substring(0, MERGED_STDLIB_FLOOR.indexOf('.'));
        String rest = MERGED_STDLIB_FLOOR.substring(MERGED_STDLIB_FLOOR.indexOf('.') + 1);
        String minor = rest.substring(0, rest.indexOf('.'));
        String atOrAbove = "major > " + major + " || (major == " + major
                + " && minor >= " + minor + ")";
        String below = "major < " + major + " || (major == " + major
                + " && minor < " + minor + ")";

        StringBuilder out = new StringBuilder();
        out.append("\n")
           .append("// Codename One: kotlin-stdlib ").append(MERGED_STDLIB_FLOOR)
           .append(" absorbed the kotlin-stdlib-jdk7 and kotlin-stdlib-jdk8\n")
           .append("// classes and the 1.8.x line ships no Gradle module metadata saying so, so\n")
           .append("// a graph holding stdlib at or above that and an older shim carries the same\n")
           .append("// classes twice and fails checkDuplicateClasses. This states the overlap as a\n")
           .append("// capability and lets Gradle drop the superseded shim. It raises no version,\n")
           .append("// so it cannot conflict with a pin, a force, a BOM or the Kotlin in use.\n")
           .append("// Turn it off with the build hint android.kotlinStdlibAlignment=false.\n")
           .append("dependencies {\n")
           .append("    components.withModule('org.jetbrains.kotlin:kotlin-stdlib') { details ->\n")
           .append(versionGuard("        ", atOrAbove))
           .append("            allVariants {\n")
           .append("                withCapabilities {\n");
        for (int i = 0; i < ALIGNED_ARTIFACTS.length; i++) {
            out.append("                    addCapability('").append(CAPABILITY_GROUP)
               .append("', '").append(ALIGNED_ARTIFACTS[i]).append(CAPABILITY_SUFFIX)
               .append("', details.id.version)\n");
        }
        out.append("                }\n")
           .append("            }\n")
           .append(versionGuardEnd("        "))
           .append("    }\n");
        for (int i = 0; i < ALIGNED_ARTIFACTS.length; i++) {
            out.append("    components.withModule('org.jetbrains.kotlin:")
               .append(ALIGNED_ARTIFACTS[i]).append("') { details ->\n")
               .append(versionGuard("        ", below))
               .append("            allVariants {\n")
               .append("                withCapabilities {\n")
               .append("                    addCapability('").append(CAPABILITY_GROUP)
               .append("', '").append(ALIGNED_ARTIFACTS[i]).append(CAPABILITY_SUFFIX)
               .append("', details.id.version)\n")
               .append("                }\n")
               .append("            }\n")
               .append(versionGuardEnd("        "))
               .append("    }\n");
        }
        out.append("}\n")
           .append("configurations.all {\n")
           .append("    resolutionStrategy.capabilitiesResolution {\n");
        for (int i = 0; i < ALIGNED_ARTIFACTS.length; i++) {
            out.append("        withCapability('").append(CAPABILITY_GROUP).append(':')
               .append(ALIGNED_ARTIFACTS[i]).append(CAPABILITY_SUFFIX).append("') {\n")
               .append("            def stdlib = candidates.find {\n")
               .append("                it.id instanceof org.gradle.api.artifacts.component"
                       + ".ModuleComponentIdentifier &&\n")
               .append("                        it.id.module == 'kotlin-stdlib'\n")
               .append("            }\n")
               .append("            if (stdlib != null) {\n")
               .append("                select(stdlib)\n")
               .append("            }\n")
               .append("        }\n");
        }
        out.append("    }\n")
           .append("}\n");
        return out.toString();
    }

    /** Opens a try block that reads the module version and tests {@code test}. */
    private static String versionGuard(String indent, String test) {
        return indent + "try {\n"
                + indent + "    def parts = details.id.version.split('[.-]')\n"
                + indent + "    def major = parts[0].toInteger()\n"
                + indent + "    def minor = parts[1].toInteger()\n"
                + indent + "    if (" + test + ") {\n";
    }

    /**
     * Closes it. A version this cannot read is left alone -- doing nothing
     * leaves the duplicate the app already had, and guessing could drop a shim
     * whose classes are still the only copy.
     */
    private static String versionGuardEnd(String indent) {
        return indent + "    }\n"
                + indent + "} catch (Exception ignored) {\n"
                + indent + "}\n";
    }
}
