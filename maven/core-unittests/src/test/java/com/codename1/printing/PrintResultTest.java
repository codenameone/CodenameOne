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
package com.codename1.printing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic coverage for the immutable {@link PrintResult}: the three status
 * factories, the mutually-exclusive predicate accessors, the failure message,
 * and {@code toString}.
 */
class PrintResultTest {

    @Test
    void completedResult() {
        PrintResult r = PrintResult.completed();
        assertEquals(PrintResult.STATUS_COMPLETED, r.getStatus());
        assertTrue(r.isCompleted());
        assertFalse(r.isCancelled());
        assertFalse(r.isFailed());
        assertNull(r.getError());
        assertEquals("PrintResult{COMPLETED}", r.toString());
    }

    @Test
    void cancelledResult() {
        PrintResult r = PrintResult.cancelled();
        assertEquals(PrintResult.STATUS_CANCELLED, r.getStatus());
        assertFalse(r.isCompleted());
        assertTrue(r.isCancelled());
        assertFalse(r.isFailed());
        assertNull(r.getError());
        assertEquals("PrintResult{CANCELLED}", r.toString());
    }

    @Test
    void failedResultCarriesMessage() {
        PrintResult r = PrintResult.failed("no printer");
        assertEquals(PrintResult.STATUS_FAILED, r.getStatus());
        assertFalse(r.isCompleted());
        assertFalse(r.isCancelled());
        assertTrue(r.isFailed());
        assertEquals("no printer", r.getError());
        assertEquals("PrintResult{FAILED no printer}", r.toString());
    }

    @Test
    void failedResultToleratesNullMessage() {
        PrintResult r = PrintResult.failed(null);
        assertTrue(r.isFailed());
        assertNull(r.getError());
        assertEquals("PrintResult{FAILED null}", r.toString());
    }

    @Test
    void statusConstantsAreDistinct() {
        assertNotEquals(PrintResult.STATUS_COMPLETED, PrintResult.STATUS_CANCELLED);
        assertNotEquals(PrintResult.STATUS_CANCELLED, PrintResult.STATUS_FAILED);
        assertNotEquals(PrintResult.STATUS_COMPLETED, PrintResult.STATUS_FAILED);
    }
}
