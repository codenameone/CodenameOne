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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * A combined iOS + native-Mac build hardens ONE shared jar, so it cannot harden one slice but not the
 * other; conflicting per-slice opt-outs must be rejected rather than silently applying one slice's choice.
 */
class IPhoneBuilderHardeningOptOutTest {

    @Test
    void combinedBuildRejectsConflictingPerSliceOptOuts() {
        // macNative=true and the two opt-outs disagree -> reject (a shared jar can't satisfy both).
        assertNotNull(IPhoneBuilder.combinedIosMacOptOutConflict(true, false, true),
                "ios opted out but mac on -> conflict");
        assertNotNull(IPhoneBuilder.combinedIosMacOptOutConflict(true, true, false),
                "mac opted out but ios on -> conflict");
    }

    @Test
    void agreeingOrNonCombinedBuildsAreAccepted() {
        // Agreeing opt-outs (both on / both off) are fine.
        assertNull(IPhoneBuilder.combinedIosMacOptOutConflict(true, true, true));
        assertNull(IPhoneBuilder.combinedIosMacOptOutConflict(true, false, false));
        // A plain iOS build (no Mac slice) never conflicts, whatever the flags say.
        assertNull(IPhoneBuilder.combinedIosMacOptOutConflict(false, true, false));
        assertNull(IPhoneBuilder.combinedIosMacOptOutConflict(false, false, true));
    }
}
