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
package com.codename1.impl.javase;

import com.codename1.impl.NativeThemeSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeThemeSettingsSimulationTest {
    private JavaSEPort previous;

    @org.junit.jupiter.api.BeforeEach
    void savePort() { previous = JavaSEPort.instance; }

    @org.junit.jupiter.api.AfterEach
    void restorePort() { JavaSEPort.instance = previous; }

    @Test
    void simulatorSnapshotsAreDefensiveAndDeterministic() {
        JavaSEPort port = new JavaSEPort();
        NativeThemeSettings input = new NativeThemeSettings().color("accent-color", 0x123456).font("Dialog", 19);
        port.setSimulatorNativeThemeSettings(input);
        input.color("accent-color", 0xffffff);
        assertEquals("123456", port.getNativeThemeSettings().getColor("accent-color"));
        port.getNativeThemeSettings().color("accent-color", 0);
        assertEquals("123456", port.getNativeThemeSettings().getColor("accent-color"));
        assertEquals(19f, port.getNativeThemeSettings().getFontSize());
    }

    @Test
    void selectedUiFontPreservesRequestedSizeAndStyle() {
        JavaSEPort port = new JavaSEPort();
        java.awt.Font font = (java.awt.Font) port.loadNativeThemeFont("Serif", "native:MainBold", 23,
                java.awt.Font.BOLD | java.awt.Font.ITALIC);
        assertEquals("Serif", font.getFamily());
        assertEquals(23f, font.getSize2D());
        assertTrue(font.isBold());
        assertTrue(font.isItalic());
    }
}
