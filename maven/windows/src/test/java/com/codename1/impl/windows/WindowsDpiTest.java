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

import com.codename1.ui.Display;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WindowsDpiTest {
    @Test
    void monitorChangesRefreshConversionsAndAppearanceEventsRetainDpi() throws Exception {
        java.lang.reflect.Field singleton = WindowsImplementation.class.getDeclaredField("INSTANCE");
        singleton.setAccessible(true);
        Object previous = singleton.get(null);
        try {
            final int[] refresh = new int[2];
            WindowsImplementation port = new WindowsImplementation() {
                @Override
                void refreshForDpiChange() {
                    refresh[0]++;
                    refresh[1] = convertToPixels(25, true);
                }
            };
            assertEquals(Math.round(25 * (96 / 25.4f)), port.convertToPixels(25, true));
            int[] dpis = {144, 192, 288, 96};
            int[] densities = {Display.DENSITY_MEDIUM, Display.DENSITY_HIGH,
                    Display.DENSITY_VERY_HIGH, Display.DENSITY_LOW};
            for (int i = 0; i < dpis.length; i++) {
                port.dispatchThemeSettingsChanged(dpis[i]);
                int pixels = Math.round(25 * (dpis[i] / 25.4f));
                assertEquals(pixels, port.convertToPixels(25, true));
                assertEquals(pixels, port.convertToPixels(25, false));
                assertEquals(i + 1, refresh[0]);
                assertEquals(pixels, refresh[1]);
                assertEquals(densities[i], port.getDeviceDensity());
                port.dispatchThemeSettingsChanged(dpis[i]);
                port.dispatchThemeSettingsChanged(0);
                port.dispatchThemeSettingsChanged(-1);
                assertEquals(i + 1, refresh[0]);
                assertEquals(pixels, port.convertToPixels(25, true));
                assertEquals(densities[i], port.getDeviceDensity());
            }
        } finally {
            singleton.set(null, previous);
        }
    }
}
