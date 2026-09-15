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
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.css.CSSThemeCompiler;
import com.codename1.ui.util.MutableResource;
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
     * Hover outranks focus. Pointing at a focused control shows the hover fill on every
     * desktop; the focus indicator is drawn separately rather than being this style.
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

    /**
     * A UIID that declares hover only in dark mode still counts as declaring it, because the
     * CSS compiler emits a `@media (prefers-color-scheme: dark)` block as `$Dark&lt;UIID&gt;`.
     */
    @Test
    public void darkOnlyHoverDeclarationIsStillADeclaration() {
        Hashtable theme = new Hashtable();
        theme.put("Button.bgColor", "112233");
        theme.put("$DarkButton.hover#bgColor", "44ff88");
        UIManager.getInstance().setThemeProps(theme);

        Component c = new Button("dark");
        c.setUIID("Button");
        assertNotNull(c.getHoverStyle(),
                "a dark-only hover declaration must still register as declared");
    }
}
