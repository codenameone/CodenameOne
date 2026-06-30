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
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the device posture and multi-display capability APIs and their safe defaults on a
 * non-foldable test implementation, plus posture listener dispatch.
 */
class DevicePostureTest extends UITestBase {

    @Test
    void getInstanceNeverNull() {
        assertNotNull(DevicePosture.getInstance());
        assertSame(DevicePosture.getInstance(), display.getDevicePosture());
    }

    @Test
    void defaultsForNonFoldableDevice() {
        DevicePosture p = DevicePosture.getInstance();
        assertFalse(p.isFoldable());
        assertFalse(display.isFoldable());
        assertEquals(DevicePosture.POSTURE_UNKNOWN, p.getPosture());
        assertEquals(-1, p.getHingeAngle());
        assertEquals(DevicePosture.FOLD_ORIENTATION_NONE, p.getFoldOrientation());
        assertFalse(p.isSeparating());
        assertFalse(p.isTableTop());
        assertNull(p.getFoldBounds(null));
    }

    @Test
    void multiDisplayDefaults() {
        assertFalse(display.isDesktopMode());
        assertTrue(display.getDisplayCount() >= 1);
        assertEquals(display.getDisplayCount() > 1, display.isExternalDisplayConnected());
    }

    @Test
    void postureConstantsAreDistinct() {
        assertNotEquals(DevicePosture.POSTURE_FLAT, DevicePosture.POSTURE_HALF_OPENED);
        assertNotEquals(DevicePosture.POSTURE_HALF_OPENED, DevicePosture.POSTURE_CLOSED);
        assertNotEquals(DevicePosture.FOLD_ORIENTATION_VERTICAL, DevicePosture.FOLD_ORIENTATION_HORIZONTAL);
    }

    @Test
    void postureListenerIsNotified() {
        final AtomicReference<ActionEvent> received = new AtomicReference<ActionEvent>();
        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                received.set(evt);
            }
        };
        display.addPostureListener(l);
        try {
            display.postureChanged();
            DisplayTest.flushEdt();
            // give the serial call a moment to drain on slower CI machines
            for (int i = 0; received.get() == null && i < 100; i++) {
                DisplayTest.flushEdt();
            }
            assertNotNull(received.get());
            assertEquals(ActionEvent.Type.PostureChange, received.get().getEventType());
        } finally {
            display.removePostureListener(l);
        }
    }
}
