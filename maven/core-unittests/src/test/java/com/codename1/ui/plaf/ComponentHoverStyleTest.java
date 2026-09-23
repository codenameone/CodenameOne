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

import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.css.CSSThemeCompiler;
import com.codename1.ui.util.MutableResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hover style state, which the desktop native themes are built on and which the
 * mobile ones never needed.
 *
 * <p>The property worth defending here is the one that is easy to lose: hover must be
 * <em>opt-in per UIID</em>. {@link UIManager#getComponentCustomStyle(String, String)} never
 * returns null, so asking it for a state the theme says nothing about yields a copy of the
 * blank default style - white background, black foreground. Building a hover style
 * unconditionally would therefore repaint every hovered component of every application
 * written before hover existed, the instant the pointer crossed it, with no obvious cause.
 * So a theme with no hover entries must produce no hover style at all.</p>
 */
public class ComponentHoverStyleTest extends UITestBase {

    /** A theme that predates hover: hovering must change nothing whatsoever. */
    @Test
    public void themeWithoutHoverEntriesHasNoHoverStyle() {
        Hashtable theme = new Hashtable();
        theme.put("Button.bgColor", "112233");
        UIManager.getInstance().setThemeProps(theme);

        Button b = new Button("plain");
        b.setUIID("Button");
        Style before = b.getStyle();

        assertNull(b.getHoverStyle(), "a theme with no hover# entries must yield no hover style");
        b.setHovered(true);
        assertTrue(b.isHovered(), "the flag itself still tracks the pointer");
        assertSame(before, b.getStyle(), "getStyle must fall through to the ordinary chain");
        assertEquals(0x112233, b.getStyle().getBgColor());
    }

    @Test
    public void programmaticallyInstalledHoverIsRenderedAndClearedWithTheTheme() {
        UIManager manager = UIManager.getInstance();
        Hashtable theme = new Hashtable();
        theme.put("Button.bgColor", "112233");
        manager.setThemeProps(theme);
        Button button = new Button("installed hover");
        assertNull(button.getHoverStyle());
        manager.getComponentCustomStyle("Button", "hover");
        assertNull(button.getHoverStyle(), "a generated fallback does not declare hover");

        Style installed = new Style();
        installed.setBgColor(0x44ff88);
        manager.setComponentStyle("Button", installed, "hover");
        button.setHovered(true);
        assertEquals(0x44ff88, button.getStyle().getBgColor());
        button.getStyle().setBgColor(0x123456);
        assertEquals(0x44ff88, installed.getBgColor(), "components receive defensive copies");

        installed.setBgColor(0xabcdef);
        button.refreshTheme(false);
        assertEquals(0xabcdef, button.getStyle().getBgColor(), "refresh reads later mutations");
        Style replacement = new Style();
        replacement.setBgColor(0x765432);
        manager.setComponentStyle("Button", replacement, "hover");
        button.refreshTheme(false);
        assertEquals(0x765432, button.getStyle().getBgColor(), "replacement bypasses old caches");

        manager.setThemeProps(theme);
        button.refreshTheme(false);
        assertNull(button.getHoverStyle(), "installations have the same lifetime as the theme");
        assertEquals(0x112233, button.getStyle().getBgColor());
    }

    /** A theme that declares hover gets it. */
    @Test
    public void declaredHoverStyleAppliesWhileHovered() {
        Hashtable theme = new Hashtable();
        theme.put("Button.bgColor", "112233");
        theme.put("Button.hover#bgColor", "44ff88");
        UIManager.getInstance().setThemeProps(theme);

        Button b = new Button("hoverable");
        b.setUIID("Button");
        assertEquals(0x112233, b.getStyle().getBgColor(), "not hovered yet");

        assertNotNull(b.getHoverStyle());
        b.setHovered(true);
        assertEquals(0x44ff88, b.getStyle().getBgColor(), "hovered");

        b.setHovered(false);
        assertEquals(0x112233, b.getStyle().getBgColor(), "pointer moved away");
    }

    /**
     * Buttons retain hover feedback while focused. Text inputs separately preserve
     * their selected style because it carries the editing focus indicator.
     */
    @Test
    public void hoverOutranksSelectedStyle() {
        Hashtable theme = new Hashtable();
        theme.put("Button.bgColor", "112233");
        theme.put("Button.sel#bgColor", "0000ff");
        theme.put("Button.hover#bgColor", "44ff88");
        UIManager.getInstance().setThemeProps(theme);

        Button b = new Button("both");
        b.setUIID("Button");
        b.setHovered(true);
        assertEquals(0x44ff88, b.getStyle().getBgColor(),
                "hover must win over the selected/focus style");
    }

    @FormTest
    public void focusedTextInputsKeepTheirFocusStyleDuringHover() {
        Form form = new Form(BoxLayout.y());
        Button other = new Button("other");
        TextField field = new TextField("field");
        TextArea area = new TextArea("area");
        Container leadRow = new Container(BoxLayout.y());
        TextField lead = new TextField("lead");
        leadRow.add(lead);
        form.add(field).add(area).add(leadRow).add(other);
        form.show();
        leadRow.setLeadComponent(lead);
        for (TextArea input : new TextArea[]{field, area, lead}) {
            Style hover = new Style(input.getUnselectedStyle());
            hover.setBorder(Border.createLineBorder(1, 0x777777));
            input.setHoverStyle(hover);
            input.getSelectedStyle().setBorder(Border.createLineBorder(2, 0x0078d4));
            form.setFocused(input);
            input.setHovered(true);
            assertSame(input.getSelectedStyle(), input.getStyle(), "focus survives a stationary pointer");
            if (input == lead) {
                assertSame(leadRow.getSelectedStyle(), leadRow.getStyle(), "lead styling follows its focused input");
            }
            input.setEnabled(false);
            assertSame(input.getDisabledStyle(), input.getStyle());
            input.setEnabled(true);
            form.setFocused(other);
            assertSame(hover, input.getStyle(), "unfocused inputs still show hover feedback");
            input.setHovered(false);
        }
    }

    /** Disabled still outranks everything, as it does for pressed. */
    @Test
    public void disabledOutranksHover() {
        Hashtable theme = new Hashtable();
        theme.put("Button.bgColor", "112233");
        theme.put("Button.dis#bgColor", "888888");
        theme.put("Button.hover#bgColor", "44ff88");
        UIManager.getInstance().setThemeProps(theme);

        Button b = new Button("off");
        b.setUIID("Button");
        b.setEnabled(false);
        b.setHovered(true);
        assertEquals(0x888888, b.getStyle().getBgColor(), "disabled must win over hover");
    }

    /** Changing the UIID must drop the cached hover style with the rest of the states. */
    @Test
    public void changingUiidRebuildsTheHoverStyle() {
        Hashtable theme = new Hashtable();
        theme.put("Button.hover#bgColor", "44ff88");
        theme.put("Other.hover#bgColor", "ff0000");
        UIManager.getInstance().setThemeProps(theme);

        Button b = new Button("switch");
        b.setUIID("Button");
        b.setHovered(true);
        assertEquals(0x44ff88, b.getStyle().getBgColor());

        b.setUIID("Other");
        assertEquals(0xff0000, b.getStyle().getBgColor(),
                "the hover style must be rebuilt for the new UIID, not reused");
    }

    /**
     * End to end through the runtime CSS compiler: a `.hover` rule in a stylesheet reaches a
     * live component's render.
     *
     * <p>This is a second, independent CSS implementation from the build-time one in
     * {@code maven/css-compiler} - they share no code - and it rejected every pseudo state it
     * did not recognise, so a sheet using `.hover` threw "Unsupported pseudo state" rather
     * than quietly ignoring the rule. Both compilers have to agree on the `hover#` prefix or
     * the same stylesheet means different things depending on when it was compiled.</p>
     */
    @Test
    public void runtimeCssCompilerCompilesHoverThroughToTheRender() {
        CSSThemeCompiler compiler = new CSSThemeCompiler();
        MutableResource resource = new MutableResource();
        compiler.compile("Button{background-color:#112233;}"
                + "Button.hover{background-color:#44ff88;}", resource, "Theme");

        UIManager.getInstance().setThemeProps(resource.getTheme("Theme"));

        Button b = new Button("css");
        b.setUIID("Button");
        assertEquals(0x112233, b.getStyle().getBgColor(), "not hovered");
        b.setHovered(true);
        assertEquals(0x44ff88, b.getStyle().getBgColor(), "hovered, straight from the stylesheet");
    }

    /// Dark mode is global state on Display, so a test that sets it has to put it back or
    /// every later test in the run inherits it.
    @AfterEach
    public void resetDarkMode() {
        display.setDarkMode(null);
    }

    /// A UIID that declares hover ONLY in dark mode declares it only WHILE dark mode is on.
    ///
    /// The CSS compiler emits a `@media (prefers-color-scheme: dark)` block as
    /// `$Dark<UIID>`, so `$DarkButton.hover#` is a real hover declaration -- for dark mode.
    /// Counting it in light mode too made getHoverStyle() go on to request the LIGHT
    /// `Button.hover#` key, which does not exist, and getComponentCustomStyle builds a style
    /// out of blank defaults from a missing key: hovering would drop the button to the
    /// default colours instead of leaving its normal light style alone.
    @Test
    public void darkOnlyHoverDeclarationDoesNotApplyInLightMode() {
        display.setDarkMode(Boolean.FALSE);
        Hashtable theme = new Hashtable();
        theme.put("Button.bgColor", "112233");
        theme.put("$DarkButton.hover#bgColor", "44ff88");
        UIManager.getInstance().setThemeProps(theme);

        Component c = new Button("light");
        c.setUIID("Button");
        assertNull(c.getHoverStyle(),
                "a dark-only hover declaration must not register while light mode is active");
        c.setHovered(true);
        assertEquals(0x112233, c.getStyle().getBgColor(),
                "hovering must leave the normal light style untouched");
    }

    /// The other half of the same rule: in dark mode the $Dark declaration IS the hover
    /// style, and hovering has to pick up the colour it declares.
    @Test
    public void darkOnlyHoverDeclarationAppliesInDarkMode() {
        display.setDarkMode(Boolean.TRUE);
        Hashtable theme = new Hashtable();
        theme.put("Button.bgColor", "112233");
        theme.put("$DarkButton.hover#bgColor", "44ff88");
        UIManager.getInstance().setThemeProps(theme);

        Component c = new Button("dark");
        c.setUIID("Button");
        assertNotNull(c.getHoverStyle(),
                "a dark-only hover declaration is a declaration while dark mode is active");
        c.setHovered(true);
        assertEquals(0x44ff88, c.getStyle().getBgColor(), "hovered, from the $Dark block");
    }
    @Test
    public void programmaticHoverSurvivesMergedRefreshWithoutThemeHover() {
        UIManager.getInstance().setThemeProps(new Hashtable());
        Button button = new Button("local hover");
        Style hover = new Style();
        hover.setBgColor(0x44ff88);
        button.setHoverStyle(hover);
        button.refreshTheme(true);
        assertNotNull(button.getHoverStyle());
        assertEquals(0x44ff88, button.getHoverStyle().getBgColor());
    }

    @Test
    public void removingThemeHoverKeepsOnlyLocalOverrides() {
        Hashtable theme = new Hashtable();
        theme.put("Button.hover#bgColor", "44ff88");
        theme.put("Button.hover#fgColor", "ff0000");
        UIManager.getInstance().setThemeProps(theme);
        Button button = new Button("local override");
        button.getHoverStyle().setBgColor(0x123456);
        Hashtable replacement = new Hashtable();
        replacement.put("Button.fgColor", "112233");
        UIManager.getInstance().setThemeProps(replacement);
        button.refreshTheme(true);
        assertNotNull(button.getHoverStyle());
        assertEquals(0x123456, button.getHoverStyle().getBgColor());
        assertEquals(0x112233, button.getHoverStyle().getFgColor(),
                "removed theme hover properties must not survive as local overrides");
    }

    @Test
    public void removingUnmodifiedThemeHoverDropsTheCachedStyle() {
        Hashtable theme = new Hashtable();
        theme.put("Button.hover#bgColor", "44ff88");
        UIManager.getInstance().setThemeProps(theme);
        Button button = new Button("theme hover");
        assertNotNull(button.getHoverStyle());
        UIManager.getInstance().setThemeProps(new Hashtable());
        button.refreshTheme(true);
        assertNull(button.getHoverStyle());
    }

    @Test
    public void inlineAllStylesOverlayHoverBeforeAndAfterThemeRefresh() {
        for (boolean merge : new boolean[]{true, false}) {
            Hashtable theme = new Hashtable();
            theme.put("Button.hover#bgColor", "445566");
            theme.put("Button.hover#fgColor", "112233");
            UIManager.getInstance().setThemeProps(theme);
            Button button = new Button("inline hover");
            button.setInlineStylesTheme(new MutableResource());
            button.setInlineAllStyles("fgColor:ff0000; padding:7px; font:18px");
            button.setHovered(true);
            assertEquals(0xff0000, button.getStyle().getFgColor());
            assertEquals(7, button.getStyle().getPaddingTop());
            assertEquals(button.getUnselectedStyle().getFont().getPixelSize(), button.getStyle().getFont().getPixelSize());
            assertEquals(0x445566, button.getStyle().getBgColor(), "unspecified properties retain the hover theme");

            button.setInlineAllStyles("fgColor:00ff00; padding:9px; font:20px");
            assertEquals(0x00ff00, button.getStyle().getFgColor(), "changing inline-all invalidates cached hover");
            Hashtable replacement = new Hashtable();
            replacement.put("Button.hover#bgColor", "abcdef");
            replacement.put("Button.hover#fgColor", "654321");
            UIManager.getInstance().setThemeProps(replacement);
            button.refreshTheme(merge);
            assertEquals(0x00ff00, button.getStyle().getFgColor());
            assertEquals(9, button.getStyle().getPaddingTop());
            assertEquals(button.getUnselectedStyle().getFont().getPixelSize(), button.getStyle().getFont().getPixelSize());
            assertEquals(0xabcdef, button.getStyle().getBgColor());
        }
    }

    @Test
    public void inlineAllDoesNotCreateAnUndeclaredHoverStateOrBypassResourceRequirement() {
        UIManager.getInstance().setThemeProps(new Hashtable());
        Button legacy = new Button("legacy inline");
        legacy.setInlineStylesTheme(new MutableResource());
        legacy.setInlineAllStyles("fgColor:ff0000");
        legacy.setHovered(true);
        assertNull(legacy.getHoverStyle());
        assertEquals(0xff0000, legacy.getStyle().getFgColor());

        Hashtable theme = new Hashtable();
        theme.put("Button.hover#fgColor", "112233");
        UIManager.getInstance().setThemeProps(theme);
        Button noResources = new Button("no inline resource context");
        noResources.setInlineAllStyles("fgColor:ff0000");
        noResources.setHovered(true);
        assertEquals(0x112233, noResources.getStyle().getFgColor());
    }

}
