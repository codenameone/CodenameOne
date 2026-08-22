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
package com.codename1.maven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// A build may return more than one artifact of the same kind -- an Android companion build
/// hands back the phone APK and the Wear APK beside it. The result extractor used to name every
/// entry `target/&lt;finalName&gt;&lt;extension&gt;`, keyed on the extension alone, so two `.apk`
/// entries collapsed onto one path and the last one written won. That corrupts the PRIMARY
/// artifact, not merely the secondary one, and it does it silently.
class CN1BuildResultArtifactRoleTest {

    @Test
    void wearArtifactKeepsItsRoleSuffix() {
        assertEquals("-wear", CN1BuildMojo.roleSuffixOf("myapp-wear"));
        assertEquals("-wear", CN1BuildMojo.roleSuffixOf("wear-release-wear"));
    }

    /// Anything not on the closed role list is the primary artifact and must keep the plain
    /// name it has always had, so an unrelated build's output cannot be re-routed by accident.
    @Test
    void everythingElseIsThePrimaryArtifact() {
        assertEquals("", CN1BuildMojo.roleSuffixOf("myapp"));
        assertEquals("", CN1BuildMojo.roleSuffixOf("app-release"));
        assertEquals("", CN1BuildMojo.roleSuffixOf("wearable"));
        assertEquals("", CN1BuildMojo.roleSuffixOf("-wearing"));
        assertEquals("", CN1BuildMojo.roleSuffixOf(""));
        assertEquals("", CN1BuildMojo.roleSuffixOf(null));
    }

    private static java.util.Map<String, java.util.Set<String>> returned(String... names) {
        java.util.Map<String, java.util.Set<String>> out =
                new java.util.HashMap<String, java.util.Set<String>>();
        for (String name : names) {
            int dot = name.lastIndexOf('.');
            String ext = name.substring(dot);
            java.util.Set<String> bases = out.get(ext);
            if (bases == null) {
                bases = new java.util.HashSet<String>();
                out.put(ext, bases);
            }
            bases.add(name.substring(0, dot));
        }
        return out;
    }

    /// A role suffix is a claim about a set. An app named "fitness-wear" returns one APK whose
    /// base ends in "-wear" and it IS the primary artifact -- reading the name alone copied it to
    /// <finalName>-wear.apk under a classifier and left the artifact the build was for missing.
    @Test
    void aLoneArtifactIsPrimaryWhateverItIsCalled() {
        java.util.Map<String, java.util.Set<String>> one = returned("fitness-wear.apk");

        assertEquals("", CN1BuildMojo.roleSuffixFor("fitness-wear", ".apk", one));
    }

    /// ...and when the phone artifact did come back, the suffixed one is the companion.
    @Test
    void aSuffixedArtifactBesideAPrimaryOneIsTheCompanion() {
        java.util.Map<String, java.util.Set<String>> pair =
                returned("myapp.apk", "myapp-wear.apk", "myapp-wear-debug.apk");

        assertEquals("-wear", CN1BuildMojo.roleSuffixFor("myapp-wear", ".apk", pair));
        assertEquals("-wear-debug", CN1BuildMojo.roleSuffixFor("myapp-wear-debug", ".apk", pair));
        assertEquals("", CN1BuildMojo.roleSuffixFor("myapp", ".apk", pair));
    }

    /// The hard case: an app whose own name ends in the suffix AND has a companion. Nothing here
    /// is unsuffixed, so "is there a primary" cannot separate them -- but stripping the suffix
    /// can, because only the companion names something else in the set.
    @Test
    void anAppNamedWearWithACompanionKeepsBothArtifacts() {
        java.util.Map<String, java.util.Set<String>> both =
                returned("fitness-wear.apk", "fitness-wear-wear.apk");

        assertEquals("", CN1BuildMojo.roleSuffixFor("fitness-wear", ".apk", both));
        assertEquals("-wear", CN1BuildMojo.roleSuffixFor("fitness-wear-wear", ".apk", both));
    }

    /// Per extension, because a build can return a companion APK and no companion AAB.
    @Test
    void theQuestionIsAskedPerExtension() {
        java.util.Map<String, java.util.Set<String>> mixed =
                returned("myapp.apk", "myapp-wear.apk", "myapp-wear.aab");

        assertEquals("-wear", CN1BuildMojo.roleSuffixFor("myapp-wear", ".apk", mixed));
        assertEquals("", CN1BuildMojo.roleSuffixFor("myapp-wear", ".aab", mixed));
    }
}
