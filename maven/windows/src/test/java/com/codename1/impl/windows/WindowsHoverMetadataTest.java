/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.impl.windows;

import com.codename1.ui.events.PointerEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WindowsHoverMetadataTest {
    @Test
    void mouseAndPenHoverReplaceContactMetadataBeforeDispatch() throws Exception {
        java.lang.reflect.Field singleton = WindowsImplementation.class.getDeclaredField("INSTANCE");
        singleton.setAccessible(true);
        Object previous = singleton.get(null);
        try {
            final int[] received = new int[3];
            final int[] expectedType = new int[1];
            WindowsImplementation port = new WindowsImplementation() {
                protected void windowPointerHover(int windowId, int x, int y) {
                    assertEquals(expectedType[0], getPointerType());
                    assertEquals(PointerEvent.BUTTON_NONE, getPointerButton());
                    assertEquals(0, getPointerButtonMask());
                    assertEquals(0f, getPointerPressure());
                    assertEquals(0f, getPointerTiltX());
                    assertEquals(0f, getPointerTiltY());
                    assertEquals(0f, getPointerContactSize());
                    assertEquals(0, getPointerModifiers());
                    assertTrue(isPointerHovering());
                    received[0] = windowId; received[1] = x; received[2] = y;
                }
            };
            // Native keys: no source flag means mouse; bit 512 means pen.
            for (int key : new int[]{0, 512}) {
                expectedType[0] = key == 0 ? PointerEvent.TYPE_MOUSE : PointerEvent.TYPE_STYLUS;
                for (int source : new int[]{PointerEvent.TYPE_TOUCH, PointerEvent.TYPE_MOUSE,
                        PointerEvent.TYPE_STYLUS}) {
                    for (int[] event : new int[][]{{0, 10, 20}, {7, 10, 20}, {0, -1, -1}, {7, -1, -1}}) {
                        port.setPointerEventMetadata(PointerEvent.BUTTON_PRIMARY, PointerEvent.MASK_PRIMARY,
                                source, 1f, 2f, 3f, 4f, 5, false);
                        port.dispatchPointerHover(event[0], event[1], event[2], key);
                        assertArrayEquals(event, received);
                    }
                }
            }
        } finally {
            singleton.set(null, previous);
        }
    }
}
