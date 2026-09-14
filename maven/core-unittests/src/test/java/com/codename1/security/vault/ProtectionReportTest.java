/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security.vault;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// The three-answer capability model, and the rule that keeps it honest.
class ProtectionReportTest extends UITestBase {

    @Test
    void unconsideredProtectionsAreUnknownNotNo() {
        ProtectionReport report = ProtectionReport.builder()
                .set(Protection.PERSISTENT, true).build();
        assertEquals(ProtectionReport.YES, report.answer(Protection.PERSISTENT));
        assertEquals(ProtectionReport.UNKNOWN, report.answer(Protection.HARDWARE_BACKED));
    }

    @Test
    void unknownDoesNotSatisfyARequirement() {
        // The rule the whole model rests on. A platform that cannot say whether a key is hardware
        // backed has not provided hardware backing, and an application that required it has to be
        // refused rather than reassured.
        ProtectionReport report = ProtectionReport.builder()
                .set(Protection.HARDWARE_BACKED, ProtectionReport.UNKNOWN).build();
        assertFalse(report.provides(Protection.HARDWARE_BACKED));
        assertFalse(report.satisfies(new Protection[] {Protection.HARDWARE_BACKED}));
        assertEquals(Protection.HARDWARE_BACKED,
                report.firstUnmet(new Protection[] {Protection.HARDWARE_BACKED}));
    }

    @Test
    void noneAnswersNoToEverything() {
        ProtectionReport none = ProtectionReport.none();
        Protection[] all = Protection.values();
        for (int iter = 0; iter < all.length; iter++) {
            assertEquals(ProtectionReport.NO, none.answer(all[iter]), all[iter].name());
        }
    }

    @Test
    void unknownAnswersUnknownToEverything() {
        // Distinct from none(): a store that could not be reached is not a store that was reached
        // and provides nothing.
        ProtectionReport unknown = ProtectionReport.unknown();
        Protection[] all = Protection.values();
        for (int iter = 0; iter < all.length; iter++) {
            assertEquals(ProtectionReport.UNKNOWN, unknown.answer(all[iter]), all[iter].name());
        }
    }

    @Test
    void anUnrecognisedValueBecomesUnknownNotYes() {
        // A port that passed 1 meaning "true" gets YES; one that passed 2 meaning something else
        // must not accidentally claim a protection.
        ProtectionReport report = ProtectionReport.builder()
                .set(Protection.OS_PROTECTED, 2).build();
        assertEquals(ProtectionReport.UNKNOWN, report.answer(Protection.OS_PROTECTED));
    }

    @Test
    void firstUnmetNamesTheMissingOne() {
        ProtectionReport report = ProtectionReport.builder()
                .set(Protection.PERSISTENT, true)
                .set(Protection.ENCRYPTED_AT_REST, true)
                .set(Protection.OS_PROTECTED, false).build();
        assertNull(report.firstUnmet(new Protection[] {
            Protection.PERSISTENT, Protection.ENCRYPTED_AT_REST}));
        assertEquals(Protection.OS_PROTECTED, report.firstUnmet(new Protection[] {
            Protection.PERSISTENT, Protection.OS_PROTECTED}));
    }

    @Test
    void nullRequirementsAreSatisfied() {
        assertTrue(ProtectionReport.none().satisfies(null));
        assertNull(ProtectionReport.none().firstUnmet(null));
    }
}
