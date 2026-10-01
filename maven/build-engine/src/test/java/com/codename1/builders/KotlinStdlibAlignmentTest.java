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
 * The alignment emits one script and takes no input, so there is little here.
 *
 * <p>What the script MEANS was measured against a real Gradle 6.5 and 8.5
 * resolving from Maven Central -- the duplicate graph, a strict pin, a reject, a
 * force, an enforced BOM, a range, an all-1.7 project and a Kotlin-free one. No
 * unit test can see resolution, so these pin the properties that make those
 * outcomes hold, and the class javadoc records the runs.</p>
 */
class KotlinStdlibAlignmentTest {

    /** The overlap is stated as a capability, for both shims. */
    @Test
    void theScriptStatesTheOverlapAsACapability() {
        String s = KotlinStdlibAlignment.alignmentScript();
        assertTrue(s.contains("components.withModule('org.jetbrains.kotlin:kotlin-stdlib')"),
                "the rule is on kotlin-stdlib, which is what gained the classes");
        assertTrue(s.contains("addCapability('com.codenameone', 'kotlin-stdlib-jdk7-superseded'"),
                "jdk7");
        assertTrue(s.contains("addCapability('com.codenameone', 'kotlin-stdlib-jdk8-superseded'"),
                "jdk8");
    }

    /**
     * NO VERSION IS EVER RAISED. This is the whole reason the class has no
     * inputs: a constraint raises a version, which an app can be holding down,
     * and detecting that from Gradle text is unbounded. A capability moves
     * nothing, so a shim version must appear nowhere as a requested version.
     */
    @Test
    void theScriptRequiresNoVersionOfAnything() {
        // The DIRECTIVES, not the prose: the script's own comment explains that
        // it cannot conflict with a force, and matching that read as the script
        // issuing one.
        StringBuilder code = new StringBuilder();
        String[] lines = KotlinStdlibAlignment.alignmentScript().split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].trim().startsWith("//")) {
                code.append(lines[i]).append('\n');
            }
        }
        String s = code.toString();
        assertTrue(!s.contains("constraints {"),
                "a constraints block would raise a version: " + s);
        assertTrue(!s.contains("kotlin-stdlib-jdk7:" + KotlinStdlibAlignment.MERGED_STDLIB_FLOOR)
                        && !s.contains("kotlin-stdlib-jdk8:"
                                + KotlinStdlibAlignment.MERGED_STDLIB_FLOOR),
                "no shim is asked for at a version");
        assertTrue(!s.contains("strictly") && !s.contains("force")
                        && !s.contains("substitute"),
                "and nothing else moves a version either");
    }

    /**
     * The capability is declared only from the floor up. Below it the shims
     * still carry the only copy of their classes, so dropping one would remove
     * them -- and a project compiling against an older Kotlin keeps its own
     * stdlib untouched, which is why the compiler version cannot be a problem.
     */
    @Test
    void theCapabilityStartsWhereTheClassesMoved() {
        String s = KotlinStdlibAlignment.alignmentScript();
        assertTrue("1.8.0".equals(KotlinStdlibAlignment.MERGED_STDLIB_FLOOR),
                "1.8.0 is where kotlin-stdlib absorbed the jdk7/jdk8 classes");
        assertTrue(s.contains("major > 1 || (major == 1 && minor >= 8)"),
                "the guard is derived from that floor: " + s);
    }

    /** The conflict resolves to the stdlib, never to whichever version is higher. */
    @Test
    void theConflictResolvesToTheStdlib() {
        String s = KotlinStdlibAlignment.alignmentScript();
        assertTrue(s.contains("withCapability('com.codenameone:kotlin-stdlib-jdk7-superseded')")
                        && s.contains("withCapability('com.codenameone:kotlin-stdlib-jdk8-superseded')"),
                "both capabilities are resolved");
        assertTrue(s.contains("def stdlib = candidates.find {")
                        && s.contains("it.id.module == 'kotlin-stdlib'"),
                "the stdlib is the candidate selected");
        assertTrue(s.contains("if (stdlib != null)"),
                "and it is not selected when it is absent");
        assertTrue(s.contains("it.id instanceof org.gradle.api.artifacts.component"
                        + ".ModuleComponentIdentifier"),
                "a project candidate has no module property; reading one throws "
                        + "MissingPropertyException and Gradle reports "
                        + "'Capability resolution rule failed'");
    }

    @Test
    void theShimOnlyClaimsToBeSupersededBelowTheFloor() {
        // The half that keeps a NEWER shim alive. A shim at or above the floor
        // is empty and duplicates nothing, and its only contribution is a
        // requirement on a stdlib at its own version. Making it conflict evicts
        // it and that requirement with it, silently downgrading the base module
        // -- measured: stdlib 1.8.0 with kotlin-stdlib-jdk8 1.9.0 resolves to
        // 1.9.0 untouched, and to 1.8.0 when the shim is made to conflict.
        String s = KotlinStdlibAlignment.alignmentScript();
        int shimRule = s.indexOf(
                "components.withModule('org.jetbrains.kotlin:kotlin-stdlib-jdk8')");
        assertTrue(shimRule >= 0, "the shim carries a rule of its own: " + s);
        String rule = s.substring(shimRule);
        rule = rule.substring(0, rule.indexOf("\n    }"));
        assertTrue(rule.contains("minor < 8"),
                "and it claims to be superseded only BELOW the floor: " + rule);
        assertTrue(!rule.contains("minor >= 8"),
                "never at or above it: " + rule);
    }

    @Test
    void theCapabilityIsOursAndNotTheShimsOwn() {
        // Reusing the shims' implicit capability makes every version of a shim
        // conflict with the stdlib, including the empty ones, and that conflict
        // has no right answer: dropping the shim downgrades the base module,
        // dropping the stdlib leaves empty shims and no stdlib at all. Both were
        // measured. A capability only this declares is held by exactly the two
        // modules that actually overlap.
        String s = KotlinStdlibAlignment.alignmentScript();
        assertTrue(!s.contains("addCapability('org.jetbrains.kotlin'"),
                "the shims' own capability is never reused: " + s);
        assertTrue(s.contains("'com.codenameone'"),
                "the capability is ours");
    }

    /**
     * The rule runs on every AndroidX build, so its worst case has to be
     * "do nothing". A version it cannot parse leaves the graph exactly as it
     * found it, which is the duplicate the app already had.
     */
    @Test
    void theRuleCannotFailTheBuild() {
        String s = KotlinStdlibAlignment.alignmentScript();
        int rule = s.indexOf("components.withModule");
        int guard = s.indexOf("try {");
        int caught = s.indexOf("catch (Exception ignored)");
        assertTrue(rule >= 0 && guard > rule && caught > guard,
                "the version read is inside a try/catch: " + s);
    }

    /**
     * It carries its own scopes. The metadata rule has to be inside
     * {@code dependencies} and the resolution strategy outside it, so the script
     * opens both rather than being spliced into two places by the caller.
     */
    @Test
    void theScriptBringsItsOwnScopes() {
        String s = KotlinStdlibAlignment.alignmentScript();
        assertTrue(s.contains("dependencies {") && s.contains("configurations.all {"),
                "both scopes: " + s);
        assertTrue(s.contains("android.kotlinStdlibAlignment=false"),
                "and it names the hint that switches it off, in the generated file "
                        + "where somebody debugging a build will actually see it");
    }

    @Test
    void theGateIsNotAboutAndroidX() throws Exception {
        // The duplicate has nothing to do with AndroidX. Reproduced with
        // android.useAndroidX=false explicitly set on AGP 8.1.4:
        // checkDebugDuplicateClasses fails there exactly as it does with
        // AndroidX on, so gating on it left those builds broken.
        String src = new String(java.nio.file.Files.readAllBytes(new java.io.File(
                "src/main/java/com/codename1/builders/AndroidGradleBuilder.java").toPath()), "UTF-8");
        int at = src.indexOf("String kotlinStdlibAlignment = \"\";");
        assertTrue(at >= 0, "the builder has the alignment gate");
        String gate = src.substring(at, src.indexOf("}", src.indexOf("if (", at)));
        assertTrue(!gate.contains("useAndroidX"),
                "the gate does not turn on AndroidX: " + gate);
        assertTrue(gate.contains("gradleVersionInt >= 6"),
                "it does keep the Gradle floor, which capabilitiesResolution needs");
        assertTrue(gate.contains("android.kotlinStdlibAlignment"),
                "and the opt-out hint");
    }

    /**
     * Appended AFTER the dependencies block. Inside it, the
     * {@code configurations.all} half would be a syntax error in the generated
     * script -- which no unit test on the emitted string alone would catch.
     */
    @Test
    void theBuilderAppendsItAfterTheDependenciesBlock() throws Exception {
        String src = new String(java.nio.file.Files.readAllBytes(new java.io.File(
                "src/main/java/com/codename1/builders/AndroidGradleBuilder.java").toPath()), "UTF-8");
        int at = src.indexOf("+ kotlinStdlibAlignment");
        assertTrue(at >= 0, "the builder appends the alignment");
        String before = src.substring(0, at);
        assertTrue(before.lastIndexOf("+ \"}\\n\"") > before.lastIndexOf("\"dependencies {"),
                "the dependencies block is closed before the alignment is appended");
        assertTrue(src.contains("KotlinStdlibAlignment.alignmentScript()"),
                "and it is the whole script, with no arguments to get wrong");
    }
}

