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
package com.codename1.tools.translator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The bulk comparison must fail a check that proves nothing, not just one
/// whose output differs: equal output from a check that printed nothing, or
/// that threw out of its body on HotSpot, passed silently.
class BulkCompareTest {
    private static List<String> compare(String hotspot, String parparvm) {
        Map<String, List<String>> expected = BulkConformanceIntegrationTest.parse(hotspot);
        Map<String, List<String>> actual = BulkConformanceIntegrationTest.parse(parparvm);
        List<String> failures = new ArrayList<>();
        BulkConformanceIntegrationTest.compare(expected, actual, failures);
        return failures;
    }

    @Test
    void equalOutputPasses() {
        assertEquals(0, compare("### a\nx=1\n", "### a\nx=1\n").size());
    }

    @Test
    void differentOutputFails() {
        assertEquals(1, compare("### a\nx=1\n", "### a\nx=2\n").size());
    }

    @Test
    void aCheckThatPrintsNothingFails() {
        List<String> failures = compare("### a\n### b\nx=1\n", "### a\n### b\nx=1\n");
        assertEquals(1, failures.size(), failures.toString());
        assertTrue(failures.get(0).startsWith("a: printed nothing"), failures.toString());
    }

    @Test
    void aDiagnosticLineIsNotEvidence() {
        assertEquals(1, compare("### a\n[GC-INSTR] x\n", "### a\n").size());
    }

    @Test
    void aCheckThatThrowsOnHotSpotFailsEvenWhenParparVmMatches() {
        String run = "### a\nx=1\nTHREW java.lang.NullPointerException: null\n";
        List<String> failures = compare(run, run);
        assertEquals(1, failures.size(), failures.toString());
        assertTrue(failures.get(0).contains("threw out of its body on HotSpot"), failures.toString());
    }

    @Test
    void aSkippedCheckStillFailsWhenParparVmThrows() {
        List<String> failures = compare("### a\nSKIPPED jdk differs\n",
                "### a\nTHREW java.lang.NullPointerException: null\n");
        assertEquals(1, failures.size(), failures.toString());
        assertTrue(failures.get(0).contains("skipped on HotSpot"), failures.toString());
    }

    @Test
    void aSkippedCheckIgnoresOrdinaryDifferences() {
        assertEquals(0, compare("### a\nSKIPPED jdk differs\n", "### a\nx=2\n").size());
    }
}
