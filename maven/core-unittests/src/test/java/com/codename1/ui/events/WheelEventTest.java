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
package com.codename1.ui.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WheelEvent} covering its deltas, precise flag, modifiers and the inherited
 * consume behavior used to suppress the default scroll gesture.
 */
class WheelEventTest {

    @Test
    void exposesDeltasAndModifiers() {
        WheelEvent e = new WheelEvent("src", 5, 6, -3, 12, true,
                PointerEvent.MODIFIER_CONTROL);
        assertEquals(5, e.getX());
        assertEquals(6, e.getY());
        assertEquals(-3, e.getDeltaX());
        assertEquals(12, e.getDeltaY());
        assertTrue(e.isPrecise());
        assertTrue(e.isControlDown());
        assertFalse(e.isShiftDown());
        assertEquals(ActionEvent.Type.PointerWheel, e.getEventType());
    }

    @Test
    void consumeSuppressesDefault() {
        WheelEvent e = new WheelEvent("src", 0, 0, 0, 10, false, 0);
        assertFalse(e.isConsumed());
        e.consume();
        assertTrue(e.isConsumed());
    }

    @Test
    void notchedWheelIsNotPrecise() {
        WheelEvent e = new WheelEvent("src", 0, 0, 0, 40, false, 0);
        assertFalse(e.isPrecise());
        assertEquals(0, e.getModifiers());
    }
}
