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

import java.util.HashSet;
import java.util.Set;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class JavaSEPortFontMappingTest {

    private Boolean originalIsIOS;
    private JavaSEPort originalInstance;
    private boolean instanceCaptured;
    private final java.util.Map<String, Object> originalFonts = new java.util.HashMap<String, Object>();

    @BeforeEach
    public void captureInstance() throws Exception {
        for (String name : new String[]{"nativeTheme", "desktopNativeFonts", "fontFaceSystem", "fontFaceProportional",
                "fontFaceMonospace", "fontFacesExplicitlyConfigured", "DEFAULT_FONT", "autoAdjustFontSize"}) {
            Field f = JavaSEPort.class.getDeclaredField(name);
            f.setAccessible(true);
            originalFonts.put(name, f.get(null));
        }
        Field desktop = JavaSEPort.class.getDeclaredField("desktopNativeFonts");
        desktop.setAccessible(true);
        desktop.setBoolean(null, false);
        // new JavaSEPort() inside the loadTrueTypeFont tests overwrites the
        // global JavaSEPort.instance via the port's constructor. Other test
        // classes (CodenameOneExtensionTest) reach back through that static
        // to drive the live Display, so leaking the throwaway port across
        // test classes causes order-dependent failures.
        originalInstance = JavaSEPort.instance;
        instanceCaptured = true;
    }

    @AfterEach
    public void tearDown() throws Exception {
        for (java.util.Map.Entry<String, Object> entry : originalFonts.entrySet()) {
            Field f = JavaSEPort.class.getDeclaredField(entry.getKey());
            f.setAccessible(true);
            f.set(null, entry.getValue());
        }
        JavaSEPort.clearAvailableFontNamesLowercaseForTest();
        if (originalIsIOS != null) {
            setIsIOS(originalIsIOS.booleanValue());
        }
        if (instanceCaptured) {
            JavaSEPort.instance = originalInstance;
        }
    }

    private void setIsIOS(boolean value) throws Exception {
        Field f = JavaSEPort.class.getDeclaredField("isIOS");
        f.setAccessible(true);
        if (originalIsIOS == null) {
            originalIsIOS = Boolean.valueOf(f.getBoolean(null));
        }
        f.setBoolean(null, value);
    }

    @Test
    public void testFindFirstInstalledFontCandidateUsesCandidateOrder() {
        Set<String> installed = new HashSet<String>();
        installed.add("sf pro display");
        installed.add("helvetica neue");

        String out = JavaSEPort.findFirstInstalledFontCandidate(
                new String[] {"SF Pro Text", "SF Pro Display", "Helvetica Neue"},
                installed
        );

        assertEquals("SF Pro Display", out);
    }

    @Test
    public void testNativeFontNameForIOSReturnsNullWhenNoCandidatesInstalled() {
        Set<String> installed = new HashSet<String>();
        installed.add("roboto");

        String out = JavaSEPort.nativeFontNameForIOS("native:MainRegular", installed);
        assertNull(out);
    }

    @Test
    public void testNativeFontNameForIOSReturnsFirstMatchingFamily() {
        Set<String> installed = new HashSet<String>();
        installed.add("sf pro text");
        installed.add("helvetica neue");

        String out = JavaSEPort.nativeFontNameForIOS("native:ItalicRegular", installed);
        assertEquals("SF Pro Text", out);
    }
    
    @Test
    public void testLoadTrueTypeFontUsesInstalledIOSCandidateWhenPresent() throws Exception {
        Set<String> installed = new HashSet<String>();
        installed.add("helvetica neue");
        JavaSEPort.setAvailableFontNamesLowercaseForTest(installed);
        setIsIOS(true);

        JavaSEPort port = new JavaSEPort();
        Object out = port.loadTrueTypeFont("native:MainRegular", "native:MainRegular");

        assertNotNull(out);
        assertEquals("Helvetica Neue", ((java.awt.Font) out).getName());
    }

    @Test
    public void testLoadTrueTypeFontFallsBackWhenNoIOSFamilyInstalled() throws Exception {
        JavaSEPort.setAvailableFontNamesLowercaseForTest(new HashSet<String>());
        setIsIOS(true);

        JavaSEPort port = new JavaSEPort();
        Object out = port.loadTrueTypeFont("native:MainRegular", "native:MainRegular");

        assertNotNull(out);
        assertEquals(java.awt.Font.class, out.getClass());
    }
    @Test
    public void desktopAliasesUseConfiguredFaceAndPreserveVariantsWhenDerived() throws Exception {
        setIsIOS(false);
        JavaSEPort.setFontFaces("Serif", "SansSerif", "Monospaced");
        JavaSEPort.setNativeTheme("/MacOSAquaTheme.res");
        JavaSEPort port = new JavaSEPort();
        String[] weights = {"Thin", "Light", "Regular", "Bold", "Black"};
        Float[] values = {java.awt.font.TextAttribute.WEIGHT_EXTRA_LIGHT, java.awt.font.TextAttribute.WEIGHT_LIGHT,
            java.awt.font.TextAttribute.WEIGHT_REGULAR, java.awt.font.TextAttribute.WEIGHT_BOLD,
            java.awt.font.TextAttribute.WEIGHT_HEAVY};
        for (String prefix : new String[]{"Main", "Italic"}) {
            for (int i = 0; i < weights.length; i++) {
                String alias = "native:" + prefix + weights[i];
                java.awt.Font font = (java.awt.Font) port.loadTrueTypeFont(alias, alias);
                java.awt.Font derived = (java.awt.Font) port.deriveTrueTypeFont(font, 17f, 0);
                assertEquals("Serif", derived.getFamily());
                Object actualWeight = derived.getAttributes().get(java.awt.font.TextAttribute.WEIGHT);
                assertEquals(values[i], actualWeight == null ? java.awt.font.TextAttribute.WEIGHT_REGULAR : actualWeight);
                assertEquals("Italic".equals(prefix), derived.isItalic());
            }
        }
        JavaSEPort.setNativeTheme("/Custom.res");
        java.awt.Font legacy = (java.awt.Font) port.loadTrueTypeFont("native:MainRegular", "native:MainRegular");
        org.junit.jupiter.api.Assertions.assertTrue(legacy.getName().startsWith("Roboto"));
    }

    @Test
    public void desktopAliasSelectsInstalledHostFamilyBeforeMobileFallback() throws Exception {
        setIsIOS(true); // A desktop skin override must win over the skin's mobile alias mapping.
        Field explicit = JavaSEPort.class.getDeclaredField("fontFacesExplicitlyConfigured");
        explicit.setAccessible(true);
        explicit.setBoolean(null, false);
        String family = JavaSEPort.IS_MAC ? ".AppleSystemUIFont" : (JavaSEPort.IS_LINUX ? "Cantarell" : "Segoe UI Variable Text");
        Set<String> installed = new HashSet<String>();
        installed.add(family.toLowerCase(java.util.Locale.ROOT));
        JavaSEPort.setAvailableFontNamesLowercaseForTest(installed);
        JavaSEPort.setNativeTheme("/WindowsFluentTheme.res");
        java.awt.Font out = (java.awt.Font) new JavaSEPort().loadTrueTypeFont("native:MainRegular", "native:MainRegular");
        assertEquals(family, out.getName());
    }
}
