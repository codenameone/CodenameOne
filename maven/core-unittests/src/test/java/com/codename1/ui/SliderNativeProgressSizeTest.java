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

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.plaf.UIManager;
import java.util.Hashtable;
import static org.junit.jupiter.api.Assertions.*;

class SliderNativeProgressSizeTest extends UITestBase {
    @FormTest
    void thinProgressTrackReservesTextHeightOnlyWhenTextIsEnabled() {
        Hashtable theme = new Hashtable();
        theme.put("@progressTrackThicknessMM", "0.3");
        UIManager.getInstance().setThemeProps(theme);
        Slider progress = new Slider();
        progress.setEditable(false);
        progress.getAllStyles().setPadding(2, 2, 0, 0);
        progress.getAllStyles().setBorder(null);
        progress.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE));
        int trackHeight = Math.max(2, Display.getInstance().convertToPixels(0.3f));
        int textHeight = progress.getStyle().getFont().getHeight();
        assertEquals(trackHeight + 4, progress.getPreferredH());
        progress.setRenderPercentageOnTop(true);
        assertTrue(progress.getPreferredH() >= textHeight + 4, "percentage text must fit after toggling a cached size");
        progress.setRenderPercentageOnTop(false);
        assertEquals(trackHeight + 4, progress.getPreferredH());
        progress.setRenderValueOnTop(true);
        assertTrue(progress.getPreferredH() >= textHeight + 4, "value text must fit after toggling a cached size");
        progress.setRenderValueOnTop(false);
        assertEquals(trackHeight + 4, progress.getPreferredH());
        progress.setEditable(true);
        assertTrue(progress.getPreferredH() >= Font.getDefaultFont().getHeight() + 4,
                "editable sliders must discard the cached thin progress height");
        progress.setEditable(false);
        assertEquals(trackHeight + 4, progress.getPreferredH(),
                "switching back to a progress bar must discard the cached slider height");
        progress.setInfinite(true);
        assertTrue(progress.getPreferredH() >= Font.getDefaultFont().getHeight() + 4,
                "indeterminate mode must discard the cached thin-track height");
        progress.setInfinite(false);
        assertEquals(trackHeight + 4, progress.getPreferredH(),
                "returning to determinate mode must discard the cached full height");
    }
}
