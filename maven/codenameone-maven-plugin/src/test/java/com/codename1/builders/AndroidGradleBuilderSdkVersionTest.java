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

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AndroidGradleBuilderSdkVersionTest {

    @Test
    void newestInstalledPlatformIsReportedAsItsMajorApiLevel() {
        AndroidGradleBuilder b = new AndroidGradleBuilder();
        assertEquals(36, b.newestInstalledPlatformApi(Arrays.asList("30", "33", "36")));
        // The regression this exists for. From API 36 Google publishes
        // MINOR-versioned platforms, so the SDK scan yields "37.0" -- and this
        // value becomes the target SDK, which is parsed as an int without a
        // guard. Returning the major is what keeps that parse alive.
        assertEquals(37, b.newestInstalledPlatformApi(Arrays.asList("36", "37.0")));
        assertEquals(36, b.newestInstalledPlatformApi(Arrays.asList("36.1")));
        // Extension platforms do not parse and must not win; a developer who has
        // one alongside a real platform still gets the real one.
        assertEquals(36, b.newestInstalledPlatformApi(Arrays.asList("36", "36-ext18")));
        assertEquals(0, b.newestInstalledPlatformApi(Arrays.asList("36-ext18")));
        assertEquals(0, b.newestInstalledPlatformApi(Collections.<String>emptyList()));
    }

    @Test
    void compileSdkSuppressionValueCarriesTheMinorFromApi37() {
        // AGP matches this against the platform's api string and names the exact
        // string it wants in the warning: "37.0". The bare major suppresses
        // nothing.
        assertEquals("37.0", AndroidGradleBuilder.suppressUnsupportedCompileSdkValue(37));
        assertEquals("38.0", AndroidGradleBuilder.suppressUnsupportedCompileSdkValue(38));
        // Below 37 nothing warns -- AGP's maximum recommended compile SDK is 36.1 --
        // so these keep the spelling they already had.
        assertEquals("36", AndroidGradleBuilder.suppressUnsupportedCompileSdkValue(36));
        assertEquals("35", AndroidGradleBuilder.suppressUnsupportedCompileSdkValue(35));
    }

    @Test
    void raisesCompileSdkToTargetSdk() {
        assertEquals("36",
                AndroidGradleBuilder.ensureCompileSdkAtLeastTarget("33", "36"));
    }

    @Test
    void doesNotLowerCompileSdk() {
        assertEquals("36",
                AndroidGradleBuilder.ensureCompileSdkAtLeastTarget("36", "35"));
    }

    @Test
    void handlesLegacyCompileSdkNotation() {
        assertEquals("36",
                AndroidGradleBuilder.ensureCompileSdkAtLeastTarget("'android-21'", "36"));
    }

    @Test
    void preservesNonNumericCompileSdk() {
        assertEquals("'android-Baklava'",
                AndroidGradleBuilder.ensureCompileSdkAtLeastTarget("'android-Baklava'", "36"));
    }

    @Test
    void suppliesMissingCompileSdk() {
        assertEquals("36",
                AndroidGradleBuilder.ensureCompileSdkAtLeastTarget(null, "36"));
    }
}
