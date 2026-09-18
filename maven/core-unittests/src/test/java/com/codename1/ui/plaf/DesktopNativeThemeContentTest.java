/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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

import com.codename1.junit.UITestBase;
import com.codename1.ui.util.Resources;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Asserts that the three desktop native themes really carry the desktop chrome, by reading
 * the compiled {@code .res} rather than the CSS.
 *
 * <p>This exists because of a failure mode this work already hit once: a theme edit that
 * scored identically to no edit, because the class loader reached a stale copy. A test that
 * reads the CSS would have been green for that too. Reading the built resource is the only
 * check that says the edit survived the compiler AND landed in the file every consumer
 * stages.</p>
 *
 * <p>Skipped, not failed, when {@code Themes/} has not been built -- the same policy
 * {@code NativeThemeBindingsTest} uses, so a checkout that has not run
 * {@code scripts/build-native-themes.sh} does not report a failure it cannot fix. CI runs
 * that script before the desktop legs.</p>
 */
public class DesktopNativeThemeContentTest extends UITestBase {

    private static final String[] DESKTOP_THEMES = {
        "WindowsFluentTheme.res", "MacOSAquaTheme.res", "GnomeAdwaitaTheme.res",
    };

    /** The two behaviours that used to need a port-side hook and are now the theme's own. */
    @Test
    public void everyDesktopThemeTurnsOnTheDesktopBehaviours() throws Exception {
        for (String name : DESKTOP_THEMES) {
            Hashtable theme = loadTheme(name);
            if (theme == null) {
                return;
            }
            assertEquals("true", theme.get("@interactiveScrollBool"),
                    name + " must turn the fading touch indicator into a real scrollbar");
            assertEquals("true", theme.get("@defaultNativeWindowModeBool"),
                    name + " must open dialogs as real operating system windows");
            assertEquals("24", theme.get("@scrollThumbMinSizeInt"),
                    name + " must keep the thumb grabbable on long content");
            assertNotNull(theme.get("@separatorThicknessMM"),
                    name + " must size the Separator rule");
        }
    }

    /**
     * The scrollbar the desktop actually draws. Until this change all three themes carried
     * {@code DesktopScrollThumb { cn1-derive: ScrollThumb; }} and nothing else, which is a
     * desktop scrollbar with no gutter and no highlight.
     */
    @Test
    public void everyDesktopThemeStylesTheInteractiveScrollbar() throws Exception {
        for (String name : DESKTOP_THEMES) {
            Hashtable theme = loadTheme(name);
            if (theme == null) {
                return;
            }
            assertNotNull(theme.get("DesktopScroll.padding"),
                    name + ": the gutter width IS DesktopScroll's horizontal padding");
            assertNotNull(theme.get("DesktopHorizontalScroll.padding"),
                    name + ": and the horizontal gutter is its vertical padding");

            // The highlight is asserted as a DIFFERENCE, not as a key. cn1-derive emits the
            // whole state family -- sel#, press#, dis# -- by copying the base, so
            // `DesktopScrollThumb { cn1-derive: ScrollThumb; }` produces a sel#bgColor that
            // is present, identical to the base, and therefore an invisible highlight. A
            // non-null assertion passes on exactly the defect this replaces. Measured: the
            // reverted theme emitted sel#bgColor = 8a8a8a against bgColor = 8a8a8a.
            //
            // Note also which states: sel# and press#, never hover#. LookAndFeel's
            // InteractiveScrollThumb returns getSelectedStyle() under the pointer and
            // getPressedStyle() while dragged.
            assertNotNull(theme.get("DesktopScrollThumb.bgColor"),
                    name + ": the thumb needs a colour of its own, not the mobile one");
            assertDiffers(theme, name, "DesktopScrollThumb.bgColor",
                    "DesktopScrollThumb.sel#bgColor", "the thumb must visibly highlight"
                            + " under the pointer");
            assertDiffers(theme, name, "DesktopScrollThumb.bgColor",
                    "DesktopScrollThumb.press#bgColor", "and visibly again while dragged");

            assertNotNull(theme.get("DesktopScrollThumb.margin"),
                    name + ": the thumb is inset from the track by its own margin");

            // Same in dark, and additionally NOT the light colour. cn1-derive is flattened
            // against the light parent at compile time, so the reverted theme's dark thumb
            // came out 8a8a8a -- the LIGHT mobile grey -- rather than the dark one.
            assertDiffers(theme, name, "$DarkDesktopScrollThumb.bgColor",
                    "$DarkDesktopScrollThumb.sel#bgColor",
                    "the dark thumb must highlight too");
            assertDiffers(theme, name, "DesktopScrollThumb.bgColor",
                    "$DarkDesktopScrollThumb.bgColor",
                    "and must not be the light colour flattened through a derive");
        }
    }

    /**
     * The surfaces Codename One still draws itself on a desktop: the context menu, the
     * overflow menu, the tooltip and the dialog's command area. None was defined by any
     * desktop theme, so each fell through to UIManager's blank default -- black on white,
     * on a dark window.
     */
    @Test
    public void everyDesktopThemeStylesTheSurfacesCn1StillDraws() throws Exception {
        String[] required = {
            "PopupContentPane.bgColor", "CommandList.margin", "Command.fgColor",
            "Command.sel#bgColor", "TooltipDialog.bgColor", "Tooltip.fgColor",
            "DialogCommandArea.padding",
            "Separator.fgColor", "GroupBox.border", "GroupBoxTitle.fgColor",
            "Link.fgColor", "Stepper.margin", "StepperField.bgColor", "StepperButton.bgColor",
            "ToolbarSearch.bgColor", "AccordionHeader.padding", "AccordionItem.padding",
            "SelectedTab.bgColor", "UnselectedTab.fgColor",
        };
        for (String name : DESKTOP_THEMES) {
            Hashtable theme = loadTheme(name);
            if (theme == null) {
                return;
            }
            for (String key : required) {
                assertNotNull(theme.get(key), name + " is missing " + key);
            }
        }
    }

    /**
     * Every colour-bearing addition needs a dark counterpart. cn1-derive is flattened
     * against the LIGHT parent at compile time, so a derived UIID gets a concrete copy of
     * the light colours and no $Dark entry at all -- the trap the existing themes already
     * carry a comment about, and the one this change had to avoid fourteen more times.
     */
    @Test
    public void everyColouredAdditionHasADarkCounterpart() throws Exception {
        String[] required = {
            "$DarkPopupContentPane.bgColor", "$DarkCommand.fgColor", "$DarkTooltip.fgColor",
            "$DarkTooltipDialog.bgColor", "$DarkSeparator.fgColor", "$DarkGroupBoxTitle.fgColor",
            "$DarkLink.fgColor", "$DarkStepperField.bgColor", "$DarkStepperButton.bgColor",
            "$DarkToolbarSearch.bgColor", "$DarkSelectedTab.bgColor", "$DarkUnselectedTab.fgColor",
        };
        for (String name : DESKTOP_THEMES) {
            Hashtable theme = loadTheme(name);
            if (theme == null) {
                return;
            }
            for (String key : required) {
                assertNotNull(theme.get(key), name + " is missing " + key);
            }
        }
    }

    /**
     * macOS restyles none of these controls on rollover -- the captured AppKit reference
     * says so, and eighteen {@code .hover} rules were removed from the Aqua theme for that
     * reason. Adding hover rules back in a later sweep is an easy mistake to make, so it is
     * asserted rather than remembered.
     *
     * <p>The scrollbar knob is the deliberate exception: {@code NSScroller} does darken under
     * the pointer, and it expresses that through sel#/press#, not hover#.</p>
     */
    @Test
    public void aquaAddsNoHoverRules() throws Exception {
        Hashtable theme = loadTheme("MacOSAquaTheme.res");
        if (theme == null) {
            return;
        }
        String[] mustNotHover = {
            "Command.hover#bgColor", "StepperButton.hover#bgColor", "Link.hover#fgColor",
            "AccordionHeader.hover#bgColor", "UnselectedTab.hover#bgColor",
        };
        for (String key : mustNotHover) {
            assertNull(theme.get(key), "Aqua must not restyle on rollover: " + key);
        }
    }

    /** Windows and GNOME do restyle on rollover, and must say so. */
    @Test
    public void fluentAndAdwaitaDoAddHoverRules() throws Exception {
        String[] withHover = {"WindowsFluentTheme.res", "GnomeAdwaitaTheme.res"};
        for (String name : withHover) {
            Hashtable theme = loadTheme(name);
            if (theme == null) {
                return;
            }
            assertNotNull(theme.get("Command.hover#bgColor"),
                    name + ": a menu item must light up under the pointer");
            assertNotNull(theme.get("StepperButton.hover#bgColor"),
                    name + ": so must a stepper button");
        }
    }

    /**
     * Asserts two theme entries are both present and hold different values.
     *
     * <p>Present-and-equal is the failure this exists to catch: it is what a
     * {@code cn1-derive} produces, and it renders as a control that does not react.</p>
     */
    private static void assertDiffers(Hashtable theme, String themeName, String baseKey,
                                      String variantKey, String why) {
        Object base = theme.get(baseKey);
        Object variant = theme.get(variantKey);
        assertNotNull(base, themeName + " is missing " + baseKey);
        assertNotNull(variant, themeName + " is missing " + variantKey);
        assertNotEquals(String.valueOf(base), String.valueOf(variant),
                themeName + ": " + why + " (" + baseKey + " and " + variantKey
                        + " are both " + base + ")");
    }

    private static Hashtable loadTheme(String fileName) throws Exception {
        File themeFile = locateNativeTheme(fileName);
        if (themeFile == null) {
            return null;
        }
        Resources res;
        InputStream stream = new FileInputStream(themeFile);
        try {
            res = Resources.open(stream);
        } finally {
            stream.close();
        }
        String[] themeNames = res.getThemeResourceNames();
        if (themeNames == null || themeNames.length == 0) {
            fail(fileName + " carries no theme");
        }
        Hashtable theme = res.getTheme(themeNames[0]);
        assertNotNull(theme, fileName + " theme is empty");
        assertTrue(theme.size() > 0, fileName + " theme has no entries");
        return theme;
    }

    private static File locateNativeTheme(String fileName) {
        File cwd = new File(".").getAbsoluteFile();
        for (int i = 0; i < 6 && cwd != null; i++) {
            File candidate = new File(cwd, "Themes/" + fileName);
            if (candidate.isFile()) {
                return candidate;
            }
            cwd = cwd.getParentFile();
        }
        return null;
    }
}
