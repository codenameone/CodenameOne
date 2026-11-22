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
package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.FontImage;

import static org.junit.jupiter.api.Assertions.*;

class EmptyStateTest extends UITestBase {

    @FormTest
    void actionIsHiddenUntilSet() {
        EmptyState e = new EmptyState(FontImage.MATERIAL_INBOX, "Nothing", "Here");
        assertEquals("EmptyState", e.getUIID());
        assertTrue(e.getActionButton().isHidden());
        final int[] taps = {0};
        e.setAction("Retry", ev -> taps[0]++);
        assertFalse(e.getActionButton().isHidden());
        assertEquals("Retry", e.getActionButton().getText());
        e.getActionButton().pressed();
        e.getActionButton().released();
        assertEquals(1, taps[0]);
    }

    @FormTest
    void nullPartsAreHidden() {
        EmptyState e = new EmptyState((char) 0, null, null);
        assertTrue(e.getPreferredH() >= 0);
    }
}
