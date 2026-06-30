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
 * Unit tests for the immutable {@link PointerEvent} value object covering the button, pointer type,
 * pressure, tilt, contact size and modifier accessors and their helper predicates.
 */
class PointerEventTest {

    @Test
    void exposesAllConstructorValues() {
        PointerEvent e = new PointerEvent(10, 20, PointerEvent.BUTTON_SECONDARY,
                PointerEvent.MASK_PRIMARY | PointerEvent.MASK_SECONDARY, PointerEvent.TYPE_STYLUS,
                0.5f, 12.5f, -3.0f, 0.25f, PointerEvent.MODIFIER_SHIFT | PointerEvent.MODIFIER_CONTROL, true);
        assertEquals(10, e.getX());
        assertEquals(20, e.getY());
        assertEquals(PointerEvent.BUTTON_SECONDARY, e.getButton());
        assertEquals(PointerEvent.MASK_PRIMARY | PointerEvent.MASK_SECONDARY, e.getButtonMask());
        assertEquals(PointerEvent.TYPE_STYLUS, e.getPointerType());
        assertEquals(0.5f, e.getPressure(), 0.0001f);
        assertEquals(12.5f, e.getTiltX(), 0.0001f);
        assertEquals(-3.0f, e.getTiltY(), 0.0001f);
        assertEquals(0.25f, e.getContactSize(), 0.0001f);
        assertTrue(e.isHovering());
    }

    @Test
    void pointerTypePredicates() {
        assertTrue(touchEvent().isTouch());
        assertFalse(touchEvent().isStylus());

        PointerEvent stylus = typed(PointerEvent.TYPE_STYLUS);
        assertTrue(stylus.isStylus());
        assertFalse(stylus.isEraser());
        assertFalse(stylus.isMouse());

        PointerEvent eraser = typed(PointerEvent.TYPE_ERASER);
        assertTrue(eraser.isStylus());
        assertTrue(eraser.isEraser());

        assertTrue(typed(PointerEvent.TYPE_MOUSE).isMouse());
    }

    @Test
    void buttonPredicates() {
        assertTrue(button(PointerEvent.BUTTON_PRIMARY).isPrimaryButton());
        assertTrue(button(PointerEvent.BUTTON_SECONDARY).isSecondaryButton());
        assertTrue(button(PointerEvent.BUTTON_MIDDLE).isMiddleButton());
        assertFalse(button(PointerEvent.BUTTON_PRIMARY).isSecondaryButton());
    }

    @Test
    void modifierPredicates() {
        PointerEvent e = new PointerEvent(0, 0, PointerEvent.BUTTON_PRIMARY, PointerEvent.MASK_PRIMARY,
                PointerEvent.TYPE_MOUSE, 1f, 0, 0, 0,
                PointerEvent.MODIFIER_SHIFT | PointerEvent.MODIFIER_META, false);
        assertTrue(e.isShiftDown());
        assertTrue(e.isMetaDown());
        assertFalse(e.isControlDown());
        assertFalse(e.isAltDown());
    }

    @Test
    void buttonConstantsAreDistinct() {
        assertEquals(-1, PointerEvent.BUTTON_NONE);
        assertEquals(0, PointerEvent.BUTTON_PRIMARY);
        assertNotEquals(PointerEvent.BUTTON_PRIMARY, PointerEvent.BUTTON_SECONDARY);
        assertNotEquals(PointerEvent.BUTTON_SECONDARY, PointerEvent.BUTTON_MIDDLE);
        // mask bits must not overlap
        assertEquals(0, PointerEvent.MASK_PRIMARY & PointerEvent.MASK_SECONDARY);
        assertEquals(0, PointerEvent.MASK_MIDDLE & PointerEvent.MASK_BACK);
    }

    private static PointerEvent touchEvent() {
        return typed(PointerEvent.TYPE_TOUCH);
    }

    private static PointerEvent typed(int type) {
        return new PointerEvent(0, 0, PointerEvent.BUTTON_PRIMARY, PointerEvent.MASK_PRIMARY, type,
                1f, 0, 0, 0, 0, false);
    }

    private static PointerEvent button(int button) {
        return new PointerEvent(0, 0, button, PointerEvent.MASK_PRIMARY, PointerEvent.TYPE_MOUSE,
                1f, 0, 0, 0, 0, false);
    }
}
