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
 * Gradle 6.5 (the builder's default) and 8.5, resolving against Maven Central:</p>
 *
 * <ul>
 *   <li>stdlib 1.8.10 with {@code kotlin-stdlib-jdk8:1.6.21} -- the duplicate,
 *       reproduced; the shims are dropped and it resolves.</li>
 *   <li>the same, with the shim pinned {@code strictly}, or with
 *       {@code reject '[1.8.0,)'} -- resolves. The constraint version failed
 *       both.</li>
 *   <li>an all-1.7 project -- untouched, because the capability is only
 *       declared from the floor up, so shims that still carry real classes stay.
 *       This is also why a Kotlin compiler older than the floor is not a
 *       problem: nothing raises the stdlib under it.</li>
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
        String floorMajor = MERGED_STDLIB_FLOOR.substring(
                0, MERGED_STDLIB_FLOOR.indexOf('.'));
        String rest = MERGED_STDLIB_FLOOR.substring(
                MERGED_STDLIB_FLOOR.indexOf('.') + 1);
        String floorMinor = rest.substring(0, rest.indexOf('.'));

        StringBuilder out = new StringBuilder();
        out.append("\n")
           .append("// Codename One: kotlin-stdlib ").append(MERGED_STDLIB_FLOOR)
           .append(" absorbed the kotlin-stdlib-jdk7 and kotlin-stdlib-jdk8\n")
           .append("// classes and the 1.8.x line ships no Gradle module metadata saying so,\n")
           .append("// so a graph holding both carries the same classes twice and fails\n")
           .append("// checkDuplicateClasses. Declaring the overlap as a capability lets\n")
           .append("// Gradle drop the redundant shim. It raises no version, so it cannot\n")
           .append("// conflict with a pin, a force, a BOM or the Kotlin compiler in use.\n")
           .append("// Turn it off with the build hint android.kotlinStdlibAlignment=false.\n")
           .append("dependencies {\n")
           .append("    components.withModule('org.jetbrains.kotlin:kotlin-stdlib') { details ->\n")
           .append("        try {\n")
           .append("            def parts = details.id.version.split('[.-]')\n")
           .append("            def major = parts[0].toInteger()\n")
           .append("            def minor = parts[1].toInteger()\n")
           .append("            if (major > ").append(floorMajor)
           .append(" || (major == ").append(floorMajor)
           .append(" && minor >= ").append(floorMinor).append(")) {\n")
           .append("                allVariants {\n")
           .append("                    withCapabilities {\n");
        for (int i = 0; i < ALIGNED_ARTIFACTS.length; i++) {
            out.append("                        addCapability('org.jetbrains.kotlin', '")
               .append(ALIGNED_ARTIFACTS[i])
               .append("', details.id.version)\n");
        }
        out.append("                    }\n")
           .append("                }\n")
           .append("            }\n")
           .append("        } catch (Exception ignored) {\n")
           .append("            // A version this cannot read is left alone. Doing nothing leaves\n")
           .append("            // the duplicate the app already had; guessing could drop a shim\n")
           .append("            // whose classes are still the only copy.\n")
           .append("        }\n")
           .append("    }\n")
           .append("}\n")
           .append("configurations.all {\n")
           .append("    resolutionStrategy.capabilitiesResolution {\n");
        for (int i = 0; i < ALIGNED_ARTIFACTS.length; i++) {
            out.append("        withCapability('org.jetbrains.kotlin:")
               .append(ALIGNED_ARTIFACTS[i]).append("') {\n")
               .append("            def stdlib = candidates.find { it.id.module == 'kotlin-stdlib' }\n")
               .append("            if (stdlib != null) {\n")
               .append("                select(stdlib)\n")
               .append("            }\n")
               .append("        }\n");
        }
        out.append("    }\n")
           .append("}\n");
        return out.toString();
    }
}
