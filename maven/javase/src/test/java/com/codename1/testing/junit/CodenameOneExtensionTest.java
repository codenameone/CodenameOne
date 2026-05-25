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

import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.plaf.UIManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sanity tests for {@link CodenameOneExtension}. The Codename One simulator
 * needs a real AWT environment (it constructs a {@code JFrame} during init);
 * on a true-headless JVM the extension itself aborts the class via
 * {@code TestAbortedException} (see {@link CodenameOneExtension#beforeAll}),
 * which JUnit reports as "skipped" rather than "failed". This local
 * annotation only catches the case where {@code java.awt.headless=true}
 * is explicitly set; the AWT auto-detected headless case is handled by
 * the extension.
 */
@CodenameOneTest
@SimulatorProperty(name = "cn1.test.classLevel", value = "yes")
@DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
public class CodenameOneExtensionTest {

    @Test
    public void displayIsInitializedAndClassLevelPropertyApplied() {
        assertTrue(Display.isInitialized(), "Display should be running by the time a test executes");
        assertEquals("yes", Display.getInstance().getProperty("cn1.test.classLevel", null),
                "class-level @SimulatorProperty must reach Display.getProperty");
    }

    @Test
    @RunOnEdt
    public void runsOnEdtWhenAnnotated() {
        assertTrue(CN.isEdt(), "@RunOnEdt method must execute on the Codename One EDT");
    }

    @Test
    public void doesNotRunOnEdtWithoutAnnotation() {
        assertFalse(CN.isEdt(), "tests without @RunOnEdt should stay on the JUnit worker thread");
    }

    @Test
    @SimulatorProperty(name = "cn1.test.methodLevel", value = "ok")
    public void methodLevelPropertyApplied() {
        assertEquals("ok", Display.getInstance().getProperty("cn1.test.methodLevel", null),
                "method-level @SimulatorProperty must be visible inside the test body");
    }

    @Test
    @SimulatorProperties({
            @SimulatorProperty(name = "cn1.test.multi.a", value = "1"),
            @SimulatorProperty(name = "cn1.test.multi.b", value = "2")
    })
    public void containerAnnotationAppliesAllEntries() {
        Display d = Display.getInstance();
        assertEquals("1", d.getProperty("cn1.test.multi.a", null));
        assertEquals("2", d.getProperty("cn1.test.multi.b", null));
    }

    @Test
    @SimulatorProperty(name = "cn1.test.systemScoped", value = "v", scope = SimulatorProperty.Scope.SYSTEM)
    public void systemScopedPropertyAfterInitIsIgnoredByExtension() {
        // System-scoped properties only take effect *before* Display init; once
        // Display is up they would be a no-op. The extension deliberately
        // skips them so this test guards against a regression that would
        // silently start mutating the JVM's System properties from a method-
        // level annotation.
        assertFalse("v".equals(System.getProperty("cn1.test.systemScoped")),
                "SYSTEM-scoped properties must not be applied after Display init");
    }

    @Test
    @LargerText(scale = 1.6f)
    public void largerTextScaleApplied() {
        assertEquals(1.6f, Display.getInstance().getLargerTextScale(), 0.001f,
                "@LargerText must flow through to Display.getLargerTextScale()");
        assertTrue(Display.getInstance().isLargerTextEnabled(),
                "scale > 1.0 should flip the larger-text flag on");
    }

    @Test
    @LargerText(scale = 1.0f)
    public void largerTextScaleDefaultClears() {
        // The method-level annotation must override any inherited class-level
        // state and put us back at 1.0x. There is no class-level @LargerText
        // here, but the assertion still guards the "@LargerText(scale=1.0f)
        // turns the mode off" contract documented on the annotation.
        assertEquals(1.0f, Display.getInstance().getLargerTextScale(), 0.001f);
        assertFalse(Display.getInstance().isLargerTextEnabled());
    }

    @Test
    @Orientation(Orientation.Value.LANDSCAPE)
    public void orientationLandscapeApplied() {
        assertFalse(Display.getInstance().isPortrait(),
                "@Orientation(LANDSCAPE) must flip Display.isPortrait() to false");
    }

    @Test
    @Orientation(Orientation.Value.PORTRAIT)
    public void orientationPortraitApplied() {
        assertTrue(Display.getInstance().isPortrait(),
                "@Orientation(PORTRAIT) must keep Display.isPortrait() true");
    }

    @Test
    @DarkMode
    public void darkModeEnabled() {
        Boolean dark = Display.getInstance().isDarkMode();
        assertNotNull(dark, "Display.isDarkMode() must reflect the override");
        assertTrue(dark.booleanValue(), "@DarkMode must put Display in dark mode");
    }

    @Test
    @DarkMode(enabled = false)
    public void darkModeDisabled() {
        Boolean dark = Display.getInstance().isDarkMode();
        assertNotNull(dark);
        assertFalse(dark.booleanValue(),
                "@DarkMode(enabled=false) must put Display in light mode");
    }

    @Test
    @RTL
    public void rtlEnabled() {
        assertTrue(UIManager.getInstance().getLookAndFeel().isRTL(),
                "@RTL must flip the look-and-feel into right-to-left mode");
    }

    @Test
    @RTL(enabled = false)
    public void rtlExplicitlyDisabled() {
        assertFalse(UIManager.getInstance().getLookAndFeel().isRTL(),
                "@RTL(enabled=false) must restore left-to-right");
    }

    @Test
    @Theme("/iOSModernTheme.res")
    public void themeLoadedByResourcePath() {
        // Verifies the path-based form of @Theme runs end-to-end against a
        // real .res bundled into the simulator jar. Asserting on individual
        // theme keys would bind this test to theme internals, so we only
        // check that the UIManager now reports an installed theme by name.
        assertNotNull(UIManager.getInstance().getThemeName(),
                "@Theme must leave a named theme installed on UIManager");
    }

    @Test
    @Theme(nativeTheme = NativeTheme.ANDROID_MATERIAL)
    public void themeLoadedByNativeThemeEnum() {
        // Verifies the enum-based form of @Theme resolves to the correct
        // bundled .res. ANDROID_MATERIAL is deliberately different from the
        // path-based test above so a stale theme leaking across tests would
        // be caught by the name check below.
        assertNotNull(UIManager.getInstance().getThemeName(),
                "@Theme(nativeTheme=...) must leave a named theme installed on UIManager");
        assertEquals("/AndroidMaterialTheme.res",
                NativeTheme.ANDROID_MATERIAL.resourcePath(),
                "NativeTheme.ANDROID_MATERIAL.resourcePath() must point at the bundled .res");
        assertEquals("Android Material",
                NativeTheme.ANDROID_MATERIAL.displayName(),
                "NativeTheme.ANDROID_MATERIAL.displayName() must mirror the simulator menu label");
    }
}
