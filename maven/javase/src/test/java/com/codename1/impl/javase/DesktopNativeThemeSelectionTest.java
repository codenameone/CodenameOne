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
    @org.junit.jupiter.api.AfterEach
    void resetFontInventory() {
        JavaSEPort.clearAvailableFontNamesLowercaseForTest();
    }

    @Test
    void systemFaceUsesTheInstalledWindowsFamilyName() {
        java.util.Set<String> installed = new java.util.HashSet<String>();
        installed.add("segoe ui variable");
        JavaSEPort.setAvailableFontNamesLowercaseForTest(installed);
        assertEquals("Segoe UI Variable", JavaSEPort.defaultSystemFontForTheme("win", "/WindowsFluentTheme.res"));
        installed.clear();
        installed.add("segoe ui");
        assertEquals("Segoe UI", JavaSEPort.defaultSystemFontForTheme("win", "/WindowsFluentTheme.res"));
    }

    @Test
    void mobileHintsDoNotSelectDesktopThemesInSimulatorOrPackagedApps() throws Exception {
        String[] keys = {"codename1.arg.desktop.themeMode", "codename1.arg.nativeTheme",
                "codename1.arg.cn1.nativeTheme"};
        String[] previous = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            previous[i] = System.getProperty(keys[i]);
            System.clearProperty(keys[i]);
        }
        java.lang.reflect.Method resolver = JavaSEPort.class.getDeclaredMethod("resolveAutoNativeTheme", String.class);
        resolver.setAccessible(true);
        try {
            for (String hint : new String[]{keys[1], keys[2]}) {
                for (String mode : new String[]{"modern", "custom", "legacy"}) {
                    System.setProperty(hint, mode);
                    for (String host : new String[]{"win", "mac", "linux"}) {
                        assertNull(resolver.invoke(null, host), hint + "=" + mode);
                        assertEquals("/NativeTheme.res", JavaSEPort.resolvePackagedDesktopNativeTheme(host, new Properties()));
                    }
                    if ("custom".equals(mode)) {
                        assertNull(resolver.invoke(null, "ios"));
                        assertNull(resolver.invoke(null, "and"));
                    } else if ("modern".equals(mode)) {
                        assertEquals("iOSModernTheme", resolver.invoke(null, "ios"));
                        assertEquals("AndroidMaterialTheme", resolver.invoke(null, "and"));
                    }
                }
                System.clearProperty(hint);
            }
            System.setProperty(keys[1], "custom");
            System.setProperty(keys[0], "auto");
            assertEquals("WindowsFluentTheme", resolver.invoke(null, "win"));
            assertEquals("/WindowsFluentTheme.res", JavaSEPort.resolvePackagedDesktopNativeTheme("win", new Properties()));
        } finally {
            for (int i = 0; i < keys.length; i++) {
                if (previous[i] == null) {
                    System.clearProperty(keys[i]);
                } else {
                    System.setProperty(keys[i], previous[i]);
                }
            }
        }
    }

    /// nativeTheme=native is the one cross-platform value that also reaches desktop.
    /// The test above pins the other half of the rule -- that modern, legacy and custom
    /// do not -- and the two together are the whole contract, so neither is complete
    /// without the other.
    @Test
    void theSharedNativeValueAlsoSelectsTheDesktopTheme() throws Exception {
        String[] keys = {"codename1.arg.desktop.themeMode", "codename1.arg.nativeTheme",
                "codename1.arg.cn1.nativeTheme"};
        String[] previous = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            previous[i] = System.getProperty(keys[i]);
            System.clearProperty(keys[i]);
        }
        java.lang.reflect.Method resolver = JavaSEPort.class.getDeclaredMethod("resolveAutoNativeTheme", String.class);
        resolver.setAccessible(true);
        try {
            for (String hint : new String[]{keys[1], keys[2]}) {
                System.setProperty(hint, "native");
                assertEquals("WindowsFluentTheme", resolver.invoke(null, "win"), hint);
                assertEquals("MacOSAquaTheme", resolver.invoke(null, "mac"), hint);
                assertEquals("GnomeAdwaitaTheme", resolver.invoke(null, "linux"), hint);
                assertEquals("/WindowsFluentTheme.res",
                        JavaSEPort.resolvePackagedDesktopNativeTheme("win", new Properties()), hint);
                // The mobile half of the hint is unchanged: native is modern plus desktop,
                // not a different mobile theme.
                assertEquals("iOSModernTheme", resolver.invoke(null, "ios"), hint);
                assertEquals("AndroidMaterialTheme", resolver.invoke(null, "and"), hint);
                // An explicit desktop hint still decides, in both directions.
                System.setProperty(keys[0], "legacy");
                assertNull(resolver.invoke(null, "win"), hint);
                System.setProperty(keys[0], "aqua");
                assertEquals("MacOSAquaTheme", resolver.invoke(null, "win"), hint);
                System.clearProperty(keys[0]);
                System.clearProperty(hint);
            }
        } finally {
            for (int i = 0; i < keys.length; i++) {
                if (previous[i] == null) {
                    System.clearProperty(keys[i]);
                } else {
                    System.setProperty(keys[i], previous[i]);
                }
            }
        }
    }

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
    void simulatorPseudoSkinKeepsCustomDistinctFromLegacyAtInstallation() throws Exception {
        String previousMode = System.getProperty("codename1.arg.desktop.themeMode");
        String previousForced = System.getProperty("cn1.forceSimulatorTheme");
        java.lang.reflect.Field nativeTheme = field("nativeTheme");
        Object previousTheme = nativeTheme.get(null);
        java.util.Map<String, Object> previousFonts = saveFontState();
        // The Native Theme menu preference outranks the hint; pin it to "auto" so a
        // developer's own simulator choice cannot leak into this test.
        System.setProperty("cn1.forceSimulatorTheme", "auto");
        try {
            for (String[] host : new String[][]{{"win", "/WindowsFluentTheme.res"},
                    {"mac", "/MacOSAquaTheme.res"}, {"linux", "/GnomeAdwaitaTheme.res"}}) {
                for (String mode : new String[]{"auto", " custom ", "legacy", "invalid", "CUSTOM"}) {
                    System.setProperty("codename1.arg.desktop.themeMode", mode);
                    JavaSEPort.setSimulatorDesktopNativeTheme(host[0], false);
                    String expected = "custom".equalsIgnoreCase(mode.trim()) ? null
                            : ("auto".equals(mode) ? host[1] : "/iOS7Theme.res");
                    assertEquals(expected, nativeTheme.get(null), host[0] + " / " + mode);
                }
                JavaSEPort.setSimulatorDesktopNativeTheme(host[0], true);
                assertEquals("/winTheme.res", nativeTheme.get(null), "the explicit UWP skin preference still wins");
            }
        } finally {
            nativeTheme.set(null, previousTheme);
            restoreFontState(previousFonts);
            if (previousMode == null) {
                System.clearProperty("codename1.arg.desktop.themeMode");
            } else {
                System.setProperty("codename1.arg.desktop.themeMode", previousMode);
            }
            if (previousForced == null) {
                System.clearProperty("cn1.forceSimulatorTheme");
            } else {
                System.setProperty("cn1.forceSimulatorTheme", previousForced);
            }
        }
    }

    @Test
    void desktopPseudoSkinHonoursTheNativeThemeMenu() {
        // A named menu choice wins over the hint, on every host, for desktop and mobile themes.
        for (String host : new String[]{"win", "mac", "linux"}) {
            assertEquals("/WindowsFluentTheme.res",
                    JavaSEPort.resolveSimulatorDesktopNativeTheme(host, "WindowsFluentTheme", "legacy"));
            assertEquals("/MacOSAquaTheme.res",
                    JavaSEPort.resolveSimulatorDesktopNativeTheme(host, "MacOSAquaTheme", "adwaita"));
            assertEquals("/GnomeAdwaitaTheme.res",
                    JavaSEPort.resolveSimulatorDesktopNativeTheme(host, "GnomeAdwaitaTheme", null));
            assertEquals("/iOS7Theme.res",
                    JavaSEPort.resolveSimulatorDesktopNativeTheme(host, "iOS7Theme", "native"));
        }
        // auto, embedded, unset and an unknown name all defer to the hint.
        for (String choice : new String[]{"auto", "embedded", null, "", "NoSuchTheme"}) {
            assertEquals("/MacOSAquaTheme.res",
                    JavaSEPort.resolveSimulatorDesktopNativeTheme("mac", choice, "native"), String.valueOf(choice));
            assertEquals("/iOS7Theme.res",
                    JavaSEPort.resolveSimulatorDesktopNativeTheme("win", choice, null), String.valueOf(choice));
            assertNull(JavaSEPort.resolveSimulatorDesktopNativeTheme("linux", choice, "custom"), String.valueOf(choice));
        }
    }

    @Test
    void osAppearanceQueriesParseEachPlatformsAnswer() {
        assertEquals(Boolean.TRUE, JavaSEPort.parseMacAppearance("Dark\n"));
        assertEquals(Boolean.FALSE, JavaSEPort.parseMacAppearance(
                "The domain/default pair of (kCFPreferencesAnyApplication, AppleInterfaceStyle) does not exist\n"));
        assertNull(JavaSEPort.parseMacAppearance(null));

        assertEquals(Boolean.TRUE, JavaSEPort.parseGnomeColorScheme("'prefer-dark'\n"));
        assertEquals(Boolean.FALSE, JavaSEPort.parseGnomeColorScheme("'prefer-light'\n"));
        assertEquals(Boolean.FALSE, JavaSEPort.parseGnomeColorScheme("'default'\n"));
        assertNull(JavaSEPort.parseGnomeColorScheme("No such key \u201ccolor-scheme\u201d\n"));
        assertNull(JavaSEPort.parseGnomeColorScheme(null));

        String key = "\r\nHKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\r\n";
        assertEquals(Boolean.TRUE, JavaSEPort.parseWindowsAppsUseLightTheme(
                key + "    AppsUseLightTheme    REG_DWORD    0x0\r\n\r\n"));
        assertEquals(Boolean.FALSE, JavaSEPort.parseWindowsAppsUseLightTheme(
                key + "    AppsUseLightTheme    REG_DWORD    0x1\r\n\r\n"));
        assertEquals(Boolean.FALSE, JavaSEPort.parseWindowsAppsUseLightTheme(
                key + "    AppsUseLightTheme    REG_DWORD    0x0a\r\n"), "0x0a is not zero");
        assertEquals(Boolean.FALSE, JavaSEPort.parseWindowsAppsUseLightTheme(
                "ERROR: The system was unable to find the specified registry key or value.\r\n"));
        assertNull(JavaSEPort.parseWindowsAppsUseLightTheme(null));
    }

    @Test
    void systemFontsAreOptInAndExplicitFacesRemainOverrides() throws Exception {
        java.util.Set<String> installed = new java.util.HashSet<String>();
        installed.add("segoe ui variable text");
        installed.add(".applesystemuifont");
        installed.add("cantarell");
        JavaSEPort.setAvailableFontNamesLowercaseForTest(installed);
        for (String platform : new String[]{"win", "mac", "linux"}) {
            String legacy = "win".equals(platform) ? "ArialUnicodeMS" : "Arial";
            for (String resource : new String[]{null, "/NativeTheme.res", "/iOS7Theme.res", "/Custom.res"}) {
                assertEquals(legacy, JavaSEPort.defaultSystemFontForTheme(platform, resource));
            }
            String modern = "win".equals(platform) ? "Segoe UI Variable Text"
                    : ("mac".equals(platform) ? ".AppleSystemUIFont" : "Cantarell");
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
                "fontFacesExplicitlyConfigured", "desktopNativeFonts", "DEFAULT_FONT", "autoAdjustFontSize"}) {
            state.put(name, field(name).get(null));
        }
        return state;
    }

    private static void restoreFontState(java.util.Map<String, Object> state) throws Exception {
        for (java.util.Map.Entry<String, Object> entry : state.entrySet()) {
            field(entry.getKey()).set(null, entry.getValue());
        }
    }

    /// The simulator must resolve the same generation the device would, because
    /// "it looked right in the simulator" is the whole point of the simulator.
    /// IOSImplementation#modernThemeResourceName is the device half.
    @Test
    public void iosThemeGenerationSelectsTheMatchingModernTheme() throws Exception {
        String key = "codename1.arg.ios.themeGeneration";
        String modeKey = "codename1.arg.ios.themeMode";
        String prevGen = System.getProperty(key);
        String prevMode = System.getProperty(modeKey);
        java.lang.reflect.Method resolver =
                JavaSEPort.class.getDeclaredMethod("resolveAutoNativeTheme", String.class);
        resolver.setAccessible(true);
        try {
            System.setProperty(modeKey, "modern");

            System.clearProperty(key);
            assertEquals("iOSModernTheme", resolver.invoke(null, "ios"),
                    "unset must stay on generation 26 so an existing project does not move");

            System.setProperty(key, "26");
            assertEquals("iOSModernTheme", resolver.invoke(null, "ios"));

            System.setProperty(key, "27");
            assertEquals("iOSModern27Theme", resolver.invoke(null, "ios"));

            // The generation is orthogonal to the look: it is consulted only
            // when the theme resolves to modern, and must not leak into the
            // legacy modes.
            System.setProperty(modeKey, "ios7");
            assertEquals("iOS7Theme", resolver.invoke(null, "ios"),
                    "themeGeneration must not affect a non-modern themeMode");
            System.setProperty(modeKey, "legacy");
            assertEquals("iPhoneTheme", resolver.invoke(null, "ios"));
        } finally {
            if (prevGen == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, prevGen);
            }
            if (prevMode == null) {
                System.clearProperty(modeKey);
            } else {
                System.setProperty(modeKey, prevMode);
            }
        }
    }
}
