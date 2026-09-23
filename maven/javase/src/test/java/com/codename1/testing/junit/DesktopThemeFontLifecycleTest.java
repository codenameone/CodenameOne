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

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.Font;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

@CodenameOneTest
@RunOnEdt
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
class DesktopThemeFontLifecycleTest {
    private static Object originalMode;
    private static Object originalFamily;
    private static Object originalTheme;

    private static Object portField(String name) throws Exception {
        Field field = JavaSEPort.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    @BeforeAll
    static void captureFontConfiguration() throws Exception {
        originalMode = portField("desktopNativeFonts");
        originalFamily = portField("fontFaceSystem");
        originalTheme = portField("nativeTheme");
    }

    private void assertDesktopAliases() throws Exception {
        assertEquals(Boolean.TRUE, portField("desktopNativeFonts"));
        java.awt.Font alias = (java.awt.Font) Font.createTrueTypeFont("native:MainRegular")
                .derive(17, Font.STYLE_PLAIN).getNativeFont();
        Method resolver = JavaSEPort.class.getDeclaredMethod("defaultSystemFontForTheme", String.class, String.class);
        resolver.setAccessible(true);
        String host = JavaSEPort.IS_MAC ? "mac" : (JavaSEPort.IS_LINUX ? "linux" : "win");
        String family = (String) resolver.invoke(null, host, "/WindowsFluentTheme.res");
        assertEquals(family, portField("fontFaceSystem"));
        java.awt.Font expected = new java.awt.Font(family, java.awt.Font.PLAIN, 17);
        assertEquals(expected.getFamily(), alias.getFamily());
        assertEquals(originalTheme, portField("nativeTheme"), "test font scope must not replace the application theme");
    }

    @Test @Order(1) @Theme(nativeTheme = NativeTheme.WINDOWS_FLUENT)
    void fluentUsesDesktopAliases() throws Exception { assertDesktopAliases(); }

    @Test @Order(2) @Theme(nativeTheme = NativeTheme.MACOS_AQUA)
    void aquaUsesDesktopAliases() throws Exception { assertDesktopAliases(); }

    @Test @Order(3) @Theme(nativeTheme = NativeTheme.GNOME_ADWAITA)
    void adwaitaUsesDesktopAliases() throws Exception { assertDesktopAliases(); }

    @Test @Order(4)
    void unthemedTestRestoresOriginalFontConfiguration() throws Exception {
        assertEquals(originalMode, portField("desktopNativeFonts"));
        assertEquals(originalFamily, portField("fontFaceSystem"));
    }

    @Test @Order(5) @Theme(nativeTheme = NativeTheme.ANDROID_MATERIAL)
    void mobileThemeDoesNotUseDesktopAliases() throws Exception {
        assertEquals(Boolean.FALSE, portField("desktopNativeFonts"));
    }

    @Test @Order(6)
    void failedResourceLoadRestoresFontConfiguration() throws Exception {
        Method install = CodenameOneExtension.class.getDeclaredMethod("installTheme", String.class,
                org.junit.jupiter.api.extension.ExtensionContext.class);
        install.setAccessible(true);
        InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                () -> install.invoke(null, "/missing-theme-for-lifecycle-test.res", null));
        assertTrue(failure.getCause() instanceof java.io.IOException);
        assertEquals(originalMode, portField("desktopNativeFonts"));
        assertEquals(originalFamily, portField("fontFaceSystem"));
    }
}
