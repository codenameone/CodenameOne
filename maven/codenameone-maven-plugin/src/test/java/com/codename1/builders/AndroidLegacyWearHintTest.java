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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The legacy {@code android.wear} / {@code android.wear.standalone} pair.
 *
 * <p>The relationship is directional and was inverted once already: Wear mode implied standalone,
 * but the standalone sub-hint never implied Wear mode. Inverting it hands a legacy PHONE project
 * the API 23 floor and a required {@code android.hardware.type.watch} feature, which makes Play
 * filter the APK off every phone -- a shipping app made undeliverable, with no build error to
 * show for it.</p>
 */
class AndroidLegacyWearHintTest {

    /** The regression: a stray standalone sub-hint must not turn a phone build into a Wear build. */
    @Test
    void standaloneAloneDoesNotEnableWearMode() {
        assertFalse(AndroidGradleBuilder.legacyWearMode("false"));
        assertFalse(AndroidGradleBuilder.legacyWearStandalone("false", "true"));
        assertFalse(AndroidGradleBuilder.legacyWearStandalone("", "true"));
    }

    @Test
    void androidWearEnablesWearMode() {
        assertTrue(AndroidGradleBuilder.legacyWearMode("true"));
        assertFalse(AndroidGradleBuilder.legacyWearMode("TRUE"));
        assertFalse(AndroidGradleBuilder.legacyWearMode(""));
    }

    /** android.wear=true implied standalone, so an absent sub-hint keeps that behaviour. */
    @Test
    void wearImpliesStandaloneUnlessExplicitlyOptedOut() {
        assertTrue(AndroidGradleBuilder.legacyWearStandalone("true", ""));
        assertTrue(AndroidGradleBuilder.legacyWearStandalone("true", null));
        assertTrue(AndroidGradleBuilder.legacyWearStandalone("true", "true"));
        assertFalse(AndroidGradleBuilder.legacyWearStandalone("true", "false"));
        assertFalse(AndroidGradleBuilder.legacyWearStandalone("true", "  false  "));
    }
}
