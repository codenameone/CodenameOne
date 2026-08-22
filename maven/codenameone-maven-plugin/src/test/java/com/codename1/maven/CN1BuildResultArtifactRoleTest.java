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

    /// A role suffix is a claim about a set. An app named "fitness-wear" returns one APK whose
    /// base ends in "-wear" and it IS the primary artifact -- reading the name alone copied it to
    /// <finalName>-wear.apk under a classifier and left the artifact the build was for missing.
    @Test
    void aLoneArtifactIsPrimaryWhateverItIsCalled() {
        java.util.Set<String> none = new java.util.HashSet<String>();

        assertEquals("", CN1BuildMojo.roleSuffixFor("fitness-wear", ".apk", none));
        assertEquals("", CN1BuildMojo.roleSuffixFor("fitness-wear-debug", ".apk", none));
    }

    /// ...and when the phone artifact did come back, the suffixed one is the companion.
    @Test
    void aSuffixedArtifactBesideAPrimaryOneIsTheCompanion() {
        java.util.Set<String> apk = new java.util.HashSet<String>();
        apk.add(".apk");

        assertEquals("-wear", CN1BuildMojo.roleSuffixFor("myapp-wear", ".apk", apk));
        assertEquals("-wear-debug", CN1BuildMojo.roleSuffixFor("myapp-wear-debug", ".apk", apk));
        assertEquals("", CN1BuildMojo.roleSuffixFor("myapp", ".apk", apk));
    }

    /// Per extension, because a build can return a primary APK and no primary AAB.
    @Test
    void theQuestionIsAskedPerExtension() {
        java.util.Set<String> apkOnly = new java.util.HashSet<String>();
        apkOnly.add(".apk");

        assertEquals("-wear", CN1BuildMojo.roleSuffixFor("myapp-wear", ".apk", apkOnly));
        assertEquals("", CN1BuildMojo.roleSuffixFor("myapp-wear", ".aab", apkOnly));
    }
}
