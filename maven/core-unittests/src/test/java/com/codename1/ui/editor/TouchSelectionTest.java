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

package com.codename1.ui.editor;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Image;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The touch selection chrome both editors share: handles, toolbar actions and dismissal.
class TouchSelectionTest extends UITestBase {
    private final List<Integer> actions = new ArrayList<Integer>();
    private boolean editable = true;

    private TouchSelection chrome() {
        actions.clear();
        return new TouchSelection(new TouchSelection.Host() {
            public int[] edgeRect(boolean start) {
                return start ? new int[] {40, 100, 20} : new int[] {120, 100, 20};
            }

            public boolean canEdit() {
                return editable;
            }

            public void toolbarAction(int action) {
                actions.add(Integer.valueOf(action));
            }

            public Font chromeFont() {
                return Font.getDefaultFont();
            }
        });
    }

    private static Component owner() {
        Component c = new com.codename1.ui.Label();
        c.setWidth(400);
        c.setHeight(400);
        return c;
    }

    private static void paint(TouchSelection t, Component c) {
        t.paint(Image.createImage(400, 400).getGraphics(), c);
    }

    @Test
    void toolbarRunsActionsAndHandlesAreHit() {
        editable = true;
        TouchSelection t = chrome();
        Component c = owner();
        t.setShown(true);
        paint(t, c);
        int[] cut = t.buttonBounds(1);
        assertNotNull(cut);
        assertEquals(TouchSelection.START_HANDLE, t.press(c, 40, 95));
        assertEquals(TouchSelection.END_HANDLE, t.press(c, 120, 125));
        assertEquals(TouchSelection.TOOLBAR, t.press(c, cut[0] + 2, cut[1] + 2));
        assertEquals(TouchSelection.CUT, actions.get(0).intValue());
        assertFalse(t.isShown());
    }

    @Test
    void readOnlyOffersCopyAndSelectAllAndAMissDismisses() {
        editable = false;
        TouchSelection t = chrome();
        Component c = owner();
        t.setShown(true);
        paint(t, c);
        assertNull(t.buttonBounds(2));
        int[] all = t.buttonBounds(1);
        assertEquals(TouchSelection.TOOLBAR, t.press(c, all[0] + 2, all[1] + 2));
        assertEquals(TouchSelection.SELECT_ALL, actions.get(0).intValue());
        assertTrue(t.isShown());
        assertEquals(TouchSelection.MISSED, t.press(c, 390, 390));
        assertFalse(t.isShown());
    }
}
