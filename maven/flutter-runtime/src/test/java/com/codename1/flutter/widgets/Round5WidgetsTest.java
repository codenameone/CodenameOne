/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import dart.runtime.Funcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The parts of this round's widget fixes that run without a display: which drag a
/// detector claims, the controller's client plumbing, and the image cache key.
class Round5WidgetsTest {

    @Test
    void aDetectorClaimsTheDragItHasCallbacksFor() {
        assertEquals(GestureOverlayRenderElement.DRAG_HORIZONTAL,
                GestureOverlayRenderElement.claimAxis(30, 5, false, true, false));
        assertEquals(GestureOverlayRenderElement.DRAG_VERTICAL,
                GestureOverlayRenderElement.claimAxis(5, 30, true, true, false));
        assertEquals(GestureOverlayRenderElement.DRAG_NONE,
                GestureOverlayRenderElement.claimAxis(5, 30, false, true, false),
                "a horizontal detector leaves a vertical drag to the enclosing scrollable");
        assertEquals(GestureOverlayRenderElement.DRAG_PAN,
                GestureOverlayRenderElement.claimAxis(5, 30, false, false, true));
    }

    @Test
    void aControllerDrivesItsAttachedListAndHearsItsScrolls() {
        ScrollController c = new ScrollController();
        assertFalse(c.hasClients());
        final double[] movedTo = {-1};
        ScrollController.Client list = new ScrollController.Client() {
            @Override
            public void scrollToOffset(double offset) {
                movedTo[0] = offset;
            }
        };
        c.attach(list);
        assertTrue(c.hasClients());
        c.jumpTo(120);
        assertEquals(120.0, movedTo[0], 0.0, "jumpTo moves the list, not only the model");
        final int[] notified = {0};
        c.addListener(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                notified[0]++;
            }
        });
        c.userScrolled(300, 1000, 500);
        assertEquals(300.0, c.offset(), 0.0, "a user scroll reaches offset");
        assertEquals(1, notified[0]);
        c.detach(list);
        assertFalse(c.hasClients());
    }

    @Test
    void theImageCacheKeyDoesNotCollideOnStringHashCodes() {
        assertEquals("Aa".hashCode(), "BB".hashCode());
        assertNotEquals(ImageRenderElement.storageKey("Aa", null), ImageRenderElement.storageKey("BB", null));
        java.util.Map<String, String> h1 = new java.util.LinkedHashMap<String, String>();
        h1.put("a", "1");
        h1.put("b", "2");
        java.util.Map<String, String> h2 = new java.util.LinkedHashMap<String, String>();
        h2.put("b", "2");
        h2.put("a", "1");
        assertEquals(ImageRenderElement.storageKey("u", h1), ImageRenderElement.storageKey("u", h2),
                "header order does not matter");
        h2.put("a", "other-user");
        assertNotEquals(ImageRenderElement.storageKey("u", h1), ImageRenderElement.storageKey("u", h2),
                "different credentials are a different entry");
    }
}
