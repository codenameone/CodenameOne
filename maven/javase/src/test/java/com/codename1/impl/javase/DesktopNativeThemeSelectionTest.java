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
import static org.junit.jupiter.api.Assertions.assertNull;

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
            for (String mode : new String[]{"legacy", "invalid"}) {
                theme.setProperty("desktop.themeMode", mode);
                assertEquals("/NativeTheme.res", JavaSEPort.resolvePackagedDesktopNativeTheme("mac", theme));
            }
            theme.setProperty("desktop.themeMode", " custom ");
            assertNull(JavaSEPort.resolvePackagedDesktopNativeTheme("mac", theme));
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
        java.util.Map<String, Object> previousFonts = saveFontState();
        try {
            System.setProperty("codename1.arg.desktop.themeMode", "fluent");
            JavaSEPort.setNativeTheme("/NativeTheme.res");
            assertEquals("/WindowsFluentTheme.res", nativeTheme.get(null));
            System.setProperty("codename1.arg.desktop.themeMode", "custom");
            JavaSEPort.setNativeTheme("/NativeTheme.res");
            assertNull(nativeTheme.get(null), "custom must not install the legacy framework base");
            JavaSEPort.setNativeTheme("/ApplicationCustomTheme.res");
            assertEquals("/ApplicationCustomTheme.res", nativeTheme.get(null));
        } finally {
            nativeTheme.set(null, previousTheme);
            restoreFontState(previousFonts);
            if (previousMode == null) {
                System.clearProperty("codename1.arg.desktop.themeMode");
            } else {
                System.setProperty("codename1.arg.desktop.themeMode", previousMode);
            }
        }
    }

    @Test
    void systemFontsAreOptInAndExplicitFacesRemainOverrides() throws Exception {
        for (String platform : new String[]{"win", "mac", "linux"}) {
            String legacy = "win".equals(platform) ? "ArialUnicodeMS" : "Arial";
            for (String resource : new String[]{null, "/NativeTheme.res", "/iOS7Theme.res", "/Custom.res"}) {
                assertEquals(legacy, JavaSEPort.defaultSystemFontForTheme(platform, resource));
            }
            String modern = "win".equals(platform) ? "Segoe UI Variable Text"
                    : ("mac".equals(platform) ? "SF Pro Text" : "Cantarell");
            for (String resource : new String[]{"/WindowsFluentTheme.res", "/MacOSAquaTheme.res", "/GnomeAdwaitaTheme.res"}) {
                assertEquals(modern, JavaSEPort.defaultSystemFontForTheme(platform, resource));
            }
        }
        java.util.Map<String, Object> previous = saveFontState();
        java.lang.reflect.Field nativeTheme = field("nativeTheme");
        Object previousTheme = nativeTheme.get(null);
        try {
            field("fontFacesExplicitlyConfigured").set(null, false);
            Object defaultFont = field("DEFAULT_FONT").get(null);
            JavaSEPort.setNativeTheme("/MacOSAquaTheme.res");
            assertEquals(defaultFont, field("DEFAULT_FONT").get(null), "theme selection must not resize default-font controls");
            String host = JavaSEPort.IS_MAC ? "mac" : (JavaSEPort.IS_LINUX ? "linux" : "win");
            assertEquals(JavaSEPort.defaultSystemFontForTheme(host, "/MacOSAquaTheme.res"), field("fontFaceSystem").get(null));
            JavaSEPort.setNativeTheme((String) null);
            assertEquals(JavaSEPort.defaultSystemFontForTheme(host, null), field("fontFaceSystem").get(null));
            JavaSEPort.setFontFaces("ExplicitFace", "ExplicitProportional", "ExplicitMonospace");
            JavaSEPort.setNativeTheme("/MacOSAquaTheme.res");
            assertEquals("ExplicitFace", field("fontFaceSystem").get(null));
        } finally {
            nativeTheme.set(null, previousTheme);
            restoreFontState(previous);
        }
    }

    private static java.lang.reflect.Field field(String name) throws Exception {
        java.lang.reflect.Field out = JavaSEPort.class.getDeclaredField(name);
        out.setAccessible(true);
        return out;
    }

    private static java.util.Map<String, Object> saveFontState() throws Exception {
        java.util.Map<String, Object> state = new java.util.HashMap<String, Object>();
        for (String name : new String[]{"fontFaceSystem", "fontFaceProportional", "fontFaceMonospace",
                "fontFacesExplicitlyConfigured", "DEFAULT_FONT", "autoAdjustFontSize"}) {
            state.put(name, field(name).get(null));
        }
        return state;
    }

    private static void restoreFontState(java.util.Map<String, Object> state) throws Exception {
        for (java.util.Map.Entry<String, Object> entry : state.entrySet()) {
            field(entry.getKey()).set(null, entry.getValue());
        }
    }

}
