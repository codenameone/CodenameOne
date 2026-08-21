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
}
