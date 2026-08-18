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
package com.codename1.ui;

import com.codename1.junit.UITestBase;
import com.codename1.ui.events.PointerEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the rich pointer metadata pipeline: the implementation accepts metadata supplied by a
 * port, exposes it through {@link Display}, builds matching {@link PointerEvent} snapshots and
 * resets to safe defaults.
 */
class PointerMetadataTest extends UITestBase {

    @Test
    void defaultsAreSafe() {
        implementation.resetPointerEventMetadata();
        assertEquals(PointerEvent.BUTTON_PRIMARY, display.getPointerButton());
        assertEquals(PointerEvent.TYPE_UNKNOWN, display.getPointerType());
        assertEquals(1f, display.getPointerPressure(), 0.0001f);
        assertEquals(0f, display.getPointerTiltX(), 0.0001f);
        assertEquals(0f, display.getPointerContactSize(), 0.0001f);
        assertFalse(display.isStylusPointer());
    }

    @Test
    void metadataFlowsThroughDisplay() {
        implementation.setPointerEventMetadata(PointerEvent.BUTTON_SECONDARY,
                PointerEvent.MASK_SECONDARY, PointerEvent.TYPE_STYLUS, 0.75f, 30f, -10f, 0.4f,
                PointerEvent.MODIFIER_ALT, false);
        assertEquals(PointerEvent.BUTTON_SECONDARY, display.getPointerButton());
        assertEquals(PointerEvent.MASK_SECONDARY, display.getPressedButtonMask());
        assertEquals(PointerEvent.TYPE_STYLUS, display.getPointerType());
        assertEquals(0.75f, display.getPointerPressure(), 0.0001f);
        assertEquals(30f, display.getPointerTiltX(), 0.0001f);
        assertEquals(-10f, display.getPointerTiltY(), 0.0001f);
        assertEquals(0.4f, display.getPointerContactSize(), 0.0001f);
        assertTrue(display.isStylusPointer());
        implementation.resetPointerEventMetadata();
    }

    @Test
    void buildPointerEventSnapshotMatchesMetadata() {
        implementation.setPointerEventMetadata(PointerEvent.BUTTON_MIDDLE, PointerEvent.MASK_MIDDLE,
                PointerEvent.TYPE_MOUSE, 0.9f, 0, 0, 0.1f, PointerEvent.MODIFIER_CONTROL, false);
        PointerEvent e = implementation.buildPointerEvent(42, 84, false);
        assertEquals(42, e.getX());
        assertEquals(84, e.getY());
        assertEquals(PointerEvent.BUTTON_MIDDLE, e.getButton());
        assertEquals(PointerEvent.TYPE_MOUSE, e.getPointerType());
        assertEquals(0.9f, e.getPressure(), 0.0001f);
        assertTrue(e.isMiddleButton());
        assertTrue(e.isControlDown());
        assertFalse(e.isHovering());

        // hovering override is honored
        PointerEvent hover = implementation.buildPointerEvent(1, 2, true);
        assertTrue(hover.isHovering());
        implementation.resetPointerEventMetadata();
    }

    @Test
    void convenienceSettersUpdateIndividualFields() {
        implementation.resetPointerEventMetadata();
        implementation.setPointerType(PointerEvent.TYPE_ERASER);
        implementation.setPointerPressure(0.3f);
        implementation.setPointerTilt(5f, 6f);
        implementation.setPointerContactSize(0.2f);
        assertEquals(PointerEvent.TYPE_ERASER, display.getPointerType());
        assertTrue(display.isStylusPointer());
        assertEquals(0.3f, display.getPointerPressure(), 0.0001f);
        assertEquals(5f, display.getPointerTiltX(), 0.0001f);
        assertEquals(6f, display.getPointerTiltY(), 0.0001f);
        assertEquals(0.2f, display.getPointerContactSize(), 0.0001f);
        implementation.resetPointerEventMetadata();
    }

    @Test
    void getCurrentPointerEventNeverNull() {
        assertNotNull(display.getCurrentPointerEvent());
    }

    @Test
    void eachQueuedPointerEventKeepsTheMetadataThatArrivedWithIt() {
        // A port reports pointer metadata into a single mutable record immediately
        // before queueing the event, but the PointerEvent is not built until the event
        // is dispatched. A port that drains a burst -- the Win32 pump translates queued
        // messages before returning, and the GTK drain does the same -- therefore
        // overwrote that record several times before any of the burst was dispatched,
        // and every event came out carrying the *last* one's button and device type.
        // A secondary window's right click or pen event read as a left mouse click,
        // which loses a context menu or a stylus callback.
        Form f = new Form("burst");
        final java.util.List<Integer> buttons = new java.util.ArrayList<Integer>();
        final java.util.List<Integer> types = new java.util.ArrayList<Integer>();
        f.addPointerPressedListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                buttons.add(Integer.valueOf(display.getPointerButton()));
                types.add(Integer.valueOf(display.getPointerType()));
            }
        });
        f.show();
        flushSerialCalls();

        // Two presses queued back to back, exactly as a drained burst arrives, with no
        // dispatch in between.
        implementation.setPointerEventMetadata(PointerEvent.BUTTON_SECONDARY,
                PointerEvent.MASK_SECONDARY, PointerEvent.TYPE_STYLUS, 0.5f, 0, 0, 0, 0, false);
        display.pointerPressed(new int[]{10}, new int[]{10});
        implementation.setPointerEventMetadata(PointerEvent.BUTTON_PRIMARY,
                PointerEvent.MASK_PRIMARY, PointerEvent.TYPE_MOUSE, 1f, 0, 0, 0, 0, false);
        display.pointerPressed(new int[]{20}, new int[]{20});

        flushSerialCalls();

        assertEquals(2, buttons.size(), "both queued presses should have dispatched");
        assertEquals(PointerEvent.BUTTON_SECONDARY, buttons.get(0).intValue(),
                "the first press must keep its own button; taking the record as it "
                        + "stands at dispatch time gives it the second press's");
        assertEquals(PointerEvent.TYPE_STYLUS, types.get(0).intValue(),
                "and its own device type");
        assertEquals(PointerEvent.BUTTON_PRIMARY, buttons.get(1).intValue());
        assertEquals(PointerEvent.TYPE_MOUSE, types.get(1).intValue());

        implementation.resetPointerEventMetadata();
    }
}
