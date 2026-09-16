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
 * Please contact Codename One through http://www.codenameone.com/ if
 * you need additional information or have any questions.
 */
package com.codename1.impl.javase;

import com.codename1.io.Properties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DesktopNativeThemeSelectionTest {
    @Test
    void packagedHintSelectsTheHostThemeWithoutSourceSettings() {
        String previous = System.getProperty("codename1.arg.desktop.themeMode");
        System.clearProperty("codename1.arg.desktop.themeMode");
        try {
            Properties theme = new Properties();
            theme.setProperty("desktop.themeMode", "auto");
            assertEquals("/WindowsFluentTheme.res", JavaSEPort.resolvePackagedDesktopNativeTheme("win", theme));
            assertEquals("/MacOSAquaTheme.res", JavaSEPort.resolvePackagedDesktopNativeTheme("mac", theme));
            assertEquals("/GnomeAdwaitaTheme.res", JavaSEPort.resolvePackagedDesktopNativeTheme("linux", theme));
            for (String[] entry : new String[][]{{"fluent", "WindowsFluentTheme"},
                    {"aqua", "MacOSAquaTheme"}, {"adwaita", "GnomeAdwaitaTheme"}}) {
                theme.setProperty("desktop.themeMode", entry[0]);
                assertEquals("/" + entry[1] + ".res", JavaSEPort.resolvePackagedDesktopNativeTheme("mac", theme));
            }
            for (String mode : new String[]{"legacy", "custom", "invalid"}) {
                theme.setProperty("desktop.themeMode", mode);
                assertEquals("/NativeTheme.res", JavaSEPort.resolvePackagedDesktopNativeTheme("mac", theme));
            }
            System.setProperty("codename1.arg.desktop.themeMode", "fluent");
            assertEquals("/WindowsFluentTheme.res", JavaSEPort.resolvePackagedDesktopNativeTheme("mac", theme),
                    "a launch-time override takes precedence over the packaged hint");
        } finally {
            if (previous == null) {
                System.clearProperty("codename1.arg.desktop.themeMode");
            } else {
                System.setProperty("codename1.arg.desktop.themeMode", previous);
            }
        }
    }
    @Test
    void legacyStubCallUsesTheHintButExplicitResourcesRemainOverrides() throws Exception {
        String previousMode = System.getProperty("codename1.arg.desktop.themeMode");
        java.lang.reflect.Field nativeTheme = JavaSEPort.class.getDeclaredField("nativeTheme");
        nativeTheme.setAccessible(true);
        Object previousTheme = nativeTheme.get(null);
        try {
            System.setProperty("codename1.arg.desktop.themeMode", "fluent");
            JavaSEPort.setNativeTheme("/NativeTheme.res");
            assertEquals("/WindowsFluentTheme.res", nativeTheme.get(null));
            JavaSEPort.setNativeTheme("/ApplicationCustomTheme.res");
            assertEquals("/ApplicationCustomTheme.res", nativeTheme.get(null));
        } finally {
            nativeTheme.set(null, previousTheme);
            if (previousMode == null) {
                System.clearProperty("codename1.arg.desktop.themeMode");
            } else {
                System.setProperty("codename1.arg.desktop.themeMode", previousMode);
            }
        }
    }

}
