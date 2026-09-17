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
package com.codename1.testing.junit;

import com.codename1.ui.Display;
import com.codename1.ui.Slider;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;

@CodenameOneTest
@RunOnEdt
@DarkMode(enabled = false)
@DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
class NativeProgressThemeTest {
    private void assertNativeTrackHeight() {
        Slider progress = new Slider();
        progress.setUIID("ProgressBar");
        progress.setEditable(false);
        progress.setRenderPercentageOnTop(false);
        progress.setRenderValueOnTop(false);
        float thickness = Float.parseFloat(UIManager.getInstance()
                .getThemeConstant("progressTrackThicknessMM", "0"));
        int expected = Math.max(2, Display.getInstance().convertToPixels(thickness))
                + progress.getStyle().getVerticalPadding();
        assertEquals(expected, progress.getPreferredH(),
                "bundled CSS pill borders must keep the thin native progress path");
    }

    @Test @Theme(nativeTheme = NativeTheme.WINDOWS_FLUENT)
    void fluentPillsRemainNative() { assertNativeTrackHeight(); }

    @Test @Theme(nativeTheme = NativeTheme.MACOS_AQUA)
    void aquaPillsRemainNative() { assertNativeTrackHeight(); }

    @Test @Theme(nativeTheme = NativeTheme.GNOME_ADWAITA)
    void adwaitaPillsRemainNative() { assertNativeTrackHeight(); }

    @Test @Theme(nativeTheme = NativeTheme.IOS_MODERN)
    void iosPillsRemainNative() { assertNativeTrackHeight(); }

    @Test @DarkMode @Theme(nativeTheme = NativeTheme.WINDOWS_FLUENT)
    void darkFluentPillsRemainNative() { assertNativeTrackHeight(); }

    @Test @DarkMode @Theme(nativeTheme = NativeTheme.MACOS_AQUA)
    void darkAquaPillsRemainNative() { assertNativeTrackHeight(); }

    @Test @DarkMode @Theme(nativeTheme = NativeTheme.GNOME_ADWAITA)
    void darkAdwaitaPillsRemainNative() { assertNativeTrackHeight(); }

    @Test @DarkMode @Theme(nativeTheme = NativeTheme.IOS_MODERN)
    void darkIosPillsRemainNative() { assertNativeTrackHeight(); }

}
