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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The `windows.embedded` hints reach the kiosk unit as `CN1_DEFAULT_*` defines.
class WindowsEmbeddedTest {

    private static BuildRequest request(String board) {
        BuildRequest r = new BuildRequest();
        r.putArgument("windows.embedded", "true");
        if (board != null) {
            r.putArgument("windows.embedded.board", board);
        }
        return r;
    }

    @Test
    void aTouchPanelHidesTheCursor() {
        BuildRequest r = request(null);
        assertTrue(WindowsNativeBuilder.isEmbeddedRequested(r));
        WindowsEmbeddedProfile p = WindowsEmbeddedProfile.forBoard(null);
        assertEquals("generic-x64", p.name);
        assertEquals("CN1_DEFAULT_ROTATION=0;CN1_DEFAULT_DPI=0;CN1_DEFAULT_MONITOR=0;CN1_DEFAULT_CURSOR=0",
                WindowsNativeBuilder.embeddedDefines(r, p));
    }

    @Test
    void anArmBoardImpliesArm64() {
        assertEquals(WindowsNativeBuilder.ARCH_ARM64, WindowsEmbeddedProfile.forBoard("imx8").arch);
        assertEquals(WindowsNativeBuilder.ARCH_ARM64, WindowsEmbeddedProfile.forBoard("qualcomm-iot").arch);
    }

    @Test
    void theHintsOverrideTheProfileAndBadValuesFallBack() {
        BuildRequest r = request("generic-x64-mouse");
        r.putArgument("windows.embedded.rotation", "90");
        r.putArgument("windows.embedded.dpi", "160");
        r.putArgument("windows.embedded.monitor", "1");
        WindowsEmbeddedProfile p = WindowsEmbeddedProfile.forBoard("generic-x64-mouse");
        assertEquals("CN1_DEFAULT_ROTATION=90;CN1_DEFAULT_DPI=160;CN1_DEFAULT_MONITOR=1;CN1_DEFAULT_CURSOR=1",
                WindowsNativeBuilder.embeddedDefines(r, p));
        r.putArgument("windows.embedded.rotation", "45");
        r.putArgument("windows.embedded.cursor", "false");
        r.putArgument("windows.embedded.dpi", "lots");
        assertEquals("CN1_DEFAULT_ROTATION=0;CN1_DEFAULT_DPI=0;CN1_DEFAULT_MONITOR=1;CN1_DEFAULT_CURSOR=0",
                WindowsNativeBuilder.embeddedDefines(r, p));
    }

    @Test
    void notRequestedByDefault() {
        assertFalse(WindowsNativeBuilder.isEmbeddedRequested(new BuildRequest()));
    }
}
