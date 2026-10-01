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
package com.codename1.ui.plaf;

import com.codename1.impl.NativeThemeSettings;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Font;
import java.util.Hashtable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UIManagerNativeSettingsTest extends UITestBase {
    private UIManager manager;

    @BeforeEach
    void resetSettings() {
        manager = UIManager.getInstance();
        display.setDarkMode(null);
        manager.setUseNativeColors(false);
        manager.setUseNativeFonts(false);
        manager.setUseLargerTextScale(false);
        implementation.setLargerTextEnabled(false);
        implementation.nativeThemeSettings = new NativeThemeSettings().color("accent-color", 0x00aa44)
                .font("native:", 24);
    }

    private Hashtable nativeTheme() {
        Hashtable theme = new Hashtable();
        Font font = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR);
        Font.clearDerivedFontCache();
        font = font.derive(16f, Font.STYLE_PLAIN);
        theme.put("@nativeThemeDefaultsBool", "true");
        theme.put("@accent-color", "112233");
        theme.put("@cn1-bind:Button.fgColor", "accent-color");
        theme.put("Button.fgColor", "112233");
        assertEquals(16f, font.getPixelSize(), "fixture");
        theme.put("font", font);
        theme.put("Button.font", font);
        theme.put("Heading.font", font.derive(32f, Font.STYLE_BOLD));
        return theme;
    }

    private float size(String uiid) { return manager.getComponentStyle(uiid).getFont().getPixelSize(); }
    private int color() { return manager.getComponentStyle("Button").getFgColor(); }

    @Test
    void optInsAreIndependentAndOffByDefault() {
        manager.setThemeProps(nativeTheme());
        assertEquals(0x112233, color());
        assertEquals(16, size("Button"));
        manager.setUseNativeColors(true);
        manager.refreshTheme();
        assertEquals(0x00aa44, color());
        assertEquals(16, size("Button"));
        manager.setUseNativeFonts(true);
        manager.refreshTheme();
        assertEquals(24, size("Button"));
        assertEquals(48, size("Heading"));
    }

    @Test
    void refreshAndDisablingRestoreOriginalsWithoutCompounding() {
        manager.setUseNativeColors(true);
        manager.setUseNativeFonts(true);
        manager.setUseLargerTextScale(true);
        implementation.setLargerTextEnabled(true);
        implementation.setLargerTextScale(1.5f);
        manager.setThemeProps(nativeTheme());
        for (int i = 0; i < 3; i++) {
            manager.refreshTheme();
            assertEquals(36, size("Button"));
            assertEquals(72, size("Heading"));
        }
        implementation.nativeThemeSettings = new NativeThemeSettings().color("accent-color", 0xcc6600)
                .font("native:", 20);
        manager.refreshNativeThemeSettings();
        assertEquals(30, size("Button"));
        assertEquals(0xcc6600, color());
        manager.setUseNativeFonts(false);
        manager.setUseNativeColors(false);
        manager.refreshTheme();
        assertEquals(24, size("Button"));
        assertEquals(0x112233, color());
        manager.setUseLargerTextScale(false);
        manager.refreshTheme();
        assertEquals(16, size("Button"));
    }

    @Test
    void zoomRetainsNativeFontRestoration() {
        assertZoomRetainsNativeFontRestoration(1f);
    }

    @Test
    void zoomRetainsNativeFontRestorationWithAccessibilityScaling() {
        assertZoomRetainsNativeFontRestoration(1.5f);
    }

    private void assertZoomRetainsNativeFontRestoration(float accessibilityScale) {
        manager.setUseNativeFonts(true);
        manager.setUseLargerTextScale(true);
        implementation.setLargerTextEnabled(true);
        implementation.setLargerTextScale(accessibilityScale);
        manager.setThemeProps(nativeTheme());
        Hashtable app = new Hashtable();
        app.put("App.font", Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR).derive(19f, Font.STYLE_PLAIN));
        manager.addThemeProps(app);
        manager.zoomFonts(2f);
        assertEquals(48f * accessibilityScale, size("Button"));
        assertEquals(96f * accessibilityScale, size("Heading"));
        assertEquals(38f * accessibilityScale, size("App"));
        for (int i = 0; i < 2; i++) {
            manager.refreshTheme();
            assertEquals(48f * accessibilityScale, size("Button"));
        }
        implementation.nativeThemeSettings = new NativeThemeSettings().font("native:", 20);
        manager.refreshNativeThemeSettings();
        assertEquals(40f * accessibilityScale, size("Button"));
        assertEquals(80f * accessibilityScale, size("Heading"));
        manager.setUseNativeFonts(false);
        manager.refreshTheme();
        assertEquals(32f * accessibilityScale, size("Button"));
        manager.setUseLargerTextScale(false);
        manager.refreshTheme();
        assertEquals(32f, size("Button"));
        assertEquals(38f, size("App"));
    }

    @Test
    void overlayRefreshUsesInheritedControlStylesOnInitialLoad() throws Exception {
        assertOverlayRefreshUsesInheritedControlStyles(false);
    }

    @Test
    void overlayRefreshUsesInheritedControlStylesWhenAddingTheme() throws Exception {
        assertOverlayRefreshUsesInheritedControlStyles(true);
    }

    private void assertOverlayRefreshUsesInheritedControlStyles(boolean add) throws Exception {
        manager.setUseNativeColors(true);
        manager.setUseNativeFonts(true);
        Hashtable overlay = new Hashtable();
        overlay.put("@overlayMarker", "loaded");
        com.codename1.ui.util.EditableResources resources = new com.codename1.ui.util.EditableResources();
        resources.setTheme("overlay", overlay);
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        resources.save(bytes);
        String resourceName = "native-settings-overlay-" + add;
        implementation.putResource("/" + resourceName + ".res",
                new java.io.ByteArrayInputStream(bytes.toByteArray()));
        Hashtable theme = nativeTheme();
        theme.put("CheckBox.font", theme.get("Button.font"));
        theme.put("CheckBox.fgColor", "112233");
        theme.put("@cn1-bind:CheckBox.fgColor", "accent-color");
        theme.put("@OverlayThemes", resourceName);
        final java.util.List<Style> refreshed = new java.util.ArrayList<Style>();
        LookAndFeel previous = manager.getLookAndFeel();
        manager.setLookAndFeel(new DefaultLookAndFeel(manager) {
            @Override
            public void refreshTheme(boolean complete) {
                super.refreshTheme(complete);
                refreshed.add(manager.getComponentStyle("CheckBox"));
            }
        });
        try {
            if (add) {
                manager.addThemeProps(theme);
            } else {
                manager.setThemeProps(theme);
            }
            assertEquals("loaded", manager.getThemeConstant("overlayMarker", "missing"));
            assertEquals(1, refreshed.size(), "Only the final composed theme should reach the look and feel");
            assertEquals(0x00aa44, refreshed.get(0).getFgColor());
            assertEquals(24f, refreshed.get(0).getFont().getPixelSize());
            assertEquals(0x00aa44, manager.getComponentStyle("CheckBox").getFgColor());
            assertEquals(24f, size("CheckBox"));
        } finally {
            manager.setLookAndFeel(previous);
        }
    }

    @Test
    void appLiteralsAndFontsWinAcrossSettingsChanges() {
        manager.setUseNativeColors(true);
        manager.setUseNativeFonts(true);
        manager.setThemeProps(nativeTheme());
        Hashtable app = new Hashtable();
        app.put("Button.fgColor", "abcdef");
        app.put("Button.font", Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR).derive(19f, Font.STYLE_PLAIN));
        manager.addThemeProps(app);
        manager.refreshTheme();
        assertEquals(0xabcdef, color());
        assertEquals(19, size("Button"));
        assertEquals(48, size("Heading"));
    }

    @Test
    void appPaletteOverrideAndExplicitFlagsWin() {
        manager.setUseNativeColors(true);
        manager.setUseNativeFonts(true);
        manager.setThemeProps(nativeTheme());
        Hashtable app = new Hashtable();
        app.put("@accent-color", "abcdef");
        app.put("@useNativeFontsBool", "false");
        manager.addThemeProps(app);
        manager.refreshTheme();
        assertEquals(0xabcdef, color());
        assertEquals(16, size("Button"));
        assertFalse(manager.isUseNativeFonts());
    }

    @Test
    void missingSettingsAndThemeReplacementUseFallbacks() {
        manager.setUseNativeColors(true);
        manager.setUseNativeFonts(true);
        manager.setThemeProps(nativeTheme());
        implementation.nativeThemeSettings = new NativeThemeSettings();
        manager.refreshNativeThemeSettings();
        assertEquals(0x112233, color());
        assertEquals(16, size("Button"));
        Hashtable app = new Hashtable();
        app.put("Button.fgColor", "998877");
        manager.setThemeProps(app);
        implementation.nativeThemeSettings = new NativeThemeSettings().color("accent-color", 0xff0000);
        manager.refreshNativeThemeSettings();
        assertEquals(0x998877, color());
    }

    @Test
    void unchangedSnapshotDoesNotRebuildTheme() {
        manager.setUseNativeColors(true);
        manager.setThemeProps(nativeTheme());
        int generation = UIManager.getThemeGeneration();
        manager.refreshNativeThemeSettings();
        assertEquals(generation, UIManager.getThemeGeneration());
    }

    @Test
    void nestedNativeThemePreservesInitialAppOverridesOnRefresh() {
        implementation.setNativeTheme(nativeTheme());
        manager.setUseNativeColors(true);
        manager.setUseNativeFonts(true);
        Hashtable app = new Hashtable();
        app.put("@includeNativeBool", "true");
        app.put("Button.fgColor", "778899");
        app.put("Heading.font", Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR).derive(27f, Font.STYLE_PLAIN));
        manager.setThemeProps(app);
        for (int i = 0; i < 3; i++) {
            manager.refreshTheme();
            assertEquals(0x778899, color());
            assertEquals(27, size("Heading"));
            assertEquals(24, size("Button"));
        }
    }

    @Test
    void themeConstantsCanEnableEachOptionAndBundledFontsStayUntouched() {
        Hashtable theme = nativeTheme();
        theme.put("@useNativeColorsBool", "true");
        theme.put("@useNativeFontsBool", "true");
        Font icon = Font.createTrueTypeFont("Icons", "icons.ttf").derive(23f, Font.STYLE_PLAIN);
        theme.put("Icon.font", icon);
        manager.setThemeProps(theme);
        assertEquals(0x00aa44, color());
        assertEquals(24, size("Button"));
        assertSame(icon, manager.getComponentStyle("Icon").getFont());
    }

    @com.codename1.junit.FormTest
    void borderAndVisibleComponentTrackSettingsAndRestore() {
        manager.setUseNativeColors(true);
        Hashtable theme = nativeTheme();
        theme.put("Button.bgColor", "112233");
        theme.put("@cn1-bind:Button.bgColor", "accent-color");
        theme.put("Button.border", RoundBorder.create().color(0x112233));
        manager.setThemeProps(theme);
        com.codename1.ui.Form form = display.getCurrent();
        com.codename1.ui.Button button = new com.codename1.ui.Button("Preview");
        form.add(button);
        form.revalidate();
        implementation.nativeThemeSettings = new NativeThemeSettings().color("accent-color", 0x556677);
        manager.refreshNativeThemeSettings();
        assertEquals(0x556677, button.getUnselectedStyle().getBgColor());
        assertEquals(0x556677, ((RoundBorder) button.getUnselectedStyle().getBorder()).getColor());
        implementation.nativeThemeSettings = new NativeThemeSettings();
        manager.refreshNativeThemeSettings();
        assertEquals(0x112233, button.getUnselectedStyle().getBgColor());
        assertEquals(0x112233, ((RoundBorder) button.getUnselectedStyle().getBorder()).getColor());
    }

    @Test
    void sharedBorderRestoresWithoutAFallbackPaletteConstant() {
        manager.setUseNativeColors(true);
        Hashtable theme = nativeTheme();
        theme.remove("@accent-color");
        RoundBorder border = RoundBorder.create().color(0x112233);
        for (String prefix : new String[]{"Button.", "Button.sel#"}) {
            theme.put(prefix + "bgColor", "112233");
            theme.put("@cn1-bind:" + prefix + "bgColor", "accent-color");
            theme.put(prefix + "border", border);
        }
        manager.setThemeProps(theme);
        assertEquals(0x00aa44, border.getColor());
        implementation.nativeThemeSettings = new NativeThemeSettings().color("accent-color", 0x556677);
        manager.refreshNativeThemeSettings();
        assertEquals(0x556677, border.getColor());
        implementation.nativeThemeSettings = new NativeThemeSettings();
        manager.refreshNativeThemeSettings();
        assertEquals(0x112233, border.getColor());
        assertEquals(0x112233, manager.getComponentStyle("Button").getBgColor());

        implementation.nativeThemeSettings = new NativeThemeSettings().color("accent-color", 0x778899);
        manager.refreshNativeThemeSettings();
        assertEquals(0x778899, border.getColor());
        manager.setUseNativeColors(false);
        manager.refreshTheme();
        assertEquals(0x112233, border.getColor());
        assertEquals(0x112233, manager.getComponentSelectedStyle("Button").getBgColor());
    }

    @Test
    void borderRestorationPreservesApplicationMutationsAndReleasesOldThemes() {
        manager.setUseNativeColors(true);
        Hashtable theme = nativeTheme();
        theme.remove("@accent-color");
        RoundBorder border = RoundBorder.create().color(0x112233);
        theme.put("Button.bgColor", "112233");
        theme.put("@cn1-bind:Button.bgColor", "accent-color");
        theme.put("Button.border", border);
        manager.setThemeProps(theme);
        border.color(0xff00ff);
        manager.setUseNativeColors(false);
        manager.refreshTheme();
        assertEquals(0xff00ff, border.getColor());

        manager.setUseNativeColors(true);
        manager.refreshTheme();
        assertEquals(0x00aa44, border.getColor());
        manager.setThemeProps(nativeTheme());
        assertEquals(0xff00ff, border.getColor(), "Old resources must not retain the OS tint");
    }

    @com.codename1.junit.FormTest
    void nativeRefreshPreservesProgrammaticStylesAndUpdatesResourceStyles() {
        manager.setUseNativeColors(true);
        Hashtable theme = nativeTheme();
        theme.put("Custom.fgColor", "101010");
        theme.put("Child.press#derive", "Custom");
        manager.setThemeProps(theme);
        Style normal = new Style();
        normal.setFgColor(0xabcdef);
        normal.setFont(Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR).derive(19f, Font.STYLE_PLAIN));
        Style selected = new Style();
        selected.setFgColor(0x123456);
        Style pressed = new Style();
        pressed.setFgColor(0x654321);
        manager.setComponentStyle("Custom", normal);
        manager.setComponentSelectedStyle("Custom", selected);
        manager.setComponentStyle("Custom", pressed, "press");
        com.codename1.ui.Button visible = new com.codename1.ui.Button("Custom");
        visible.setUIID("Custom");
        display.getCurrent().add(visible);
        assertEquals(0x00aa44, color()); // Populate a resource-derived cache too.

        implementation.nativeThemeSettings = new NativeThemeSettings().color("accent-color", 0x556677);
        manager.refreshNativeThemeSettings();
        assertEquals(0x556677, color(), "Resource styles must still refresh");
        assertEquals(0xabcdef, visible.getUnselectedStyle().getFgColor());
        assertEquals(19f, visible.getUnselectedStyle().getFont().getPixelSize());
        assertEquals(0x123456, visible.getSelectedStyle().getFgColor());
        assertEquals(0x654321, visible.getPressedStyle().getFgColor());
        com.codename1.ui.Button createdAfter = new com.codename1.ui.Button("After");
        createdAfter.setUIID("Custom");
        assertEquals(0xabcdef, createdAfter.getUnselectedStyle().getFgColor());

        // The caller still owns the installed objects after an appearance-only change.
        normal.setFgColor(0xaabbcc);
        implementation.nativeDarkMode = Boolean.TRUE;
        manager.refreshNativeThemeSettings();
        assertEquals(0xaabbcc, visible.getUnselectedStyle().getFgColor());
        assertEquals(0xaabbcc, manager.getComponentCustomStyle("Child", "press").getFgColor());
        normal.setFgColor(0xbbccdd);
        assertEquals(0xbbccdd, manager.getComponentCustomStyle("Child", "press").getFgColor());

        // An explicit theme replacement retains the existing reset semantics.
        manager.setThemeProps(theme);
        assertEquals(0x101010, manager.getComponentStyle("Custom").getFgColor());
        implementation.nativeDarkMode = Boolean.FALSE;
        manager.refreshNativeThemeSettings();
        assertEquals(0x101010, manager.getComponentStyle("Custom").getFgColor());
    }

    @Test
    void parsedStyleReplacementIsNotResurrectedByNativeRefresh() {
        manager.setThemeProps(nativeTheme());
        Style installed = new Style();
        installed.setFgColor(0xabcdef);
        manager.setComponentStyle("Custom", installed);
        manager.parseComponentStyle(null, null, "Custom", "fgColor:123456");
        implementation.nativeDarkMode = Boolean.TRUE;
        manager.refreshNativeThemeSettings();
        assertEquals(0x123456, manager.getComponentStyle("Custom").getFgColor());
    }

    @com.codename1.junit.FormTest
    void appearanceChangesRefreshVisibleStylesWithoutOptionalSettings() {
        Hashtable theme = nativeTheme();
        theme.put("$DarkButton.fgColor", "eeeeee");
        manager.setThemeProps(theme);
        com.codename1.ui.Button button = new com.codename1.ui.Button("Appearance");
        display.getCurrent().add(button);
        assertEquals(0x112233, button.getUnselectedStyle().getFgColor());
        int generation = UIManager.getThemeGeneration();
        manager.refreshNativeThemeSettings();
        assertEquals(generation, UIManager.getThemeGeneration());
        implementation.nativeDarkMode = Boolean.TRUE;
        manager.refreshNativeThemeSettings();
        assertEquals(0xeeeeee, button.getUnselectedStyle().getFgColor());
        assertFalse(manager.isUseNativeColors());
        assertFalse(manager.isUseNativeFonts());
        generation = UIManager.getThemeGeneration();
        manager.refreshNativeThemeSettings();
        assertEquals(generation, UIManager.getThemeGeneration());
        implementation.nativeDarkMode = Boolean.FALSE;
        manager.refreshNativeThemeSettings();
        assertEquals(0x112233, button.getUnselectedStyle().getFgColor());
    }

    @Test
    void explicitAppearanceOverrideIgnoresOsChangesWithoutOptIns() {
        display.setDarkMode(Boolean.FALSE);
        try {
            Hashtable theme = nativeTheme();
            theme.put("$DarkButton.fgColor", "eeeeee");
            manager.setThemeProps(theme);
            int generation = UIManager.getThemeGeneration();
            implementation.nativeDarkMode = Boolean.TRUE;
            manager.refreshNativeThemeSettings();
            assertEquals(generation, UIManager.getThemeGeneration());
            assertEquals(0x112233, color());
        } finally {
            display.setDarkMode(null);
        }
    }

    private static class CountingLayout extends com.codename1.ui.layouts.BoxLayout {
        int passes;

        CountingLayout() {
            super(com.codename1.ui.layouts.BoxLayout.Y_AXIS);
        }

        @Override
        public void layoutContainer(com.codename1.ui.Container parent) {
            passes++;
            super.layoutContainer(parent);
        }
    }

    private com.codename1.ui.Button addDeepTree(com.codename1.ui.Container root) {
        com.codename1.ui.Container parent = root;
        for (int i = 0; i < 20; i++) {
            com.codename1.ui.Container child = new com.codename1.ui.Container(com.codename1.ui.layouts.BoxLayout.y());
            parent.add(child);
            parent = child;
        }
        com.codename1.ui.Button leaf = new com.codename1.ui.Button("Font preview");
        parent.add(leaf);
        return leaf;
    }

    @com.codename1.junit.FormTest
    void appearanceRefreshLaysOutADeepFormOnce() {
        manager.setUseNativeFonts(true);
        manager.setThemeProps(nativeTheme());
        com.codename1.ui.Form form = display.getCurrent();
        CountingLayout layout = new CountingLayout();
        form.setLayout(layout);
        com.codename1.ui.Button leaf = addDeepTree(form);
        form.revalidate();
        layout.passes = 0;
        implementation.nativeThemeSettings = new NativeThemeSettings().font("native:", 32);
        manager.refreshNativeThemeSettings();
        assertEquals(1, layout.passes, "Nested containers must not each lay out the root");
        assertEquals(32f, leaf.getUnselectedStyle().getFont().getPixelSize());
    }

    @com.codename1.junit.FormTest
    void appearanceRefreshSchedulesOneDeepWindowLayout() throws Exception {
        implementation.setMultiWindowSupported(true);
        manager.setUseNativeFonts(true);
        manager.setThemeProps(nativeTheme());
        CountingLayout layout = new CountingLayout();
        com.codename1.ui.Window window = new com.codename1.ui.Window("Font preview", layout);
        try {
            com.codename1.ui.Button leaf = addDeepTree(window);
            window.show();
            window.revalidate();
            layout.passes = 0;
            implementation.nativeThemeSettings = new NativeThemeSettings().font("native:", 32);
            manager.refreshNativeThemeSettings();
            assertEquals(0, layout.passes, "Window layout should be deferred to the paint cycle");
            java.lang.reflect.Method flush = com.codename1.ui.Window.class.getDeclaredMethod("flushRevalidateQueue");
            flush.setAccessible(true);
            flush.invoke(window);
            assertEquals(1, layout.passes);
            assertEquals(32f, leaf.getUnselectedStyle().getFont().getPixelSize());
        } finally {
            window.dispose();
        }
    }

    @Test
    void explicitAppBorderIsNotRetintedByNativeBackground() {
        manager.setUseNativeColors(true);
        Hashtable theme = nativeTheme();
        theme.put("Button.bgColor", "112233");
        theme.put("@cn1-bind:Button.bgColor", "accent-color");
        theme.put("Button.border", RoundBorder.create().color(0x112233));
        manager.setThemeProps(theme);
        RoundBorder appBorder = RoundBorder.create().color(0xff00ff);
        Hashtable app = new Hashtable();
        app.put("Button.border", appBorder);
        manager.addThemeProps(app);
        manager.refreshTheme();
        assertEquals(0xff00ff, appBorder.getColor());
    }

    @Test
    void disabledCategoryChangesDoNotRebuildTheme() {
        manager.setUseNativeColors(true);
        manager.setThemeProps(nativeTheme());
        int generation = UIManager.getThemeGeneration();
        implementation.nativeThemeSettings = new NativeThemeSettings().color("accent-color", 0x00aa44)
                .font("Different", 40);
        manager.refreshNativeThemeSettings();
        assertEquals(generation, UIManager.getThemeGeneration());
    }

    @Test
    void shippedThemesExposeInheritableBodyFonts() throws Exception {
        String[] files = {"iOSModernTheme.res", "iOSModern27Theme.res", "AndroidMaterialTheme.res",
                "WindowsFluentTheme.res", "MacOSAquaTheme.res", "GnomeAdwaitaTheme.res"};
        java.io.File root = new java.io.File(System.getProperty("user.dir"));
        while (root != null && !new java.io.File(root, "native-themes").isDirectory()) { root = root.getParentFile(); }
        assertNotNull(root);
        for (String name : files) {
            java.io.File file = new java.io.File(new java.io.File(root, "Themes"), name);
            if (!file.isFile()) { continue; } // Core-only builds need no theme artifacts.
            com.codename1.ui.util.Resources resource;
            try (java.io.InputStream input = new java.io.FileInputStream(file)) {
                resource = com.codename1.ui.util.Resources.open(input);
            }
            Hashtable theme = resource.getTheme(resource.getThemeResourceNames()[0]);
            assertEquals("true", theme.get("@nativeThemeDefaultsBool"), name);
            manager.setUseNativeFonts(true);
            manager.setThemeProps(theme);
            assertEquals(24f, size("Label"), 0.01f, name);
            manager.refreshTheme();
            assertEquals(24f, size("Label"), 0.01f, name + " refreshed");
        }
    }

    @Test
    void snapshotRejectsInvalidFontSizesAndMalformedColors() {
        NativeThemeSettings settings = NativeThemeSettings.parse(
                "fontFamily=Sans\nfontSize=NaN\naccent-color=zzzzzz\ntext-color=112233\nunknown");
        assertNull(settings.getFontFamily());
        assertNull(settings.getColor("accent-color"));
        assertEquals("112233", settings.getColor("text-color"));
    }
}
