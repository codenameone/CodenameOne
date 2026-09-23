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
package com.codename1.ui.css;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.CSSBorder;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.MutableResource;
import java.util.Hashtable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CSSThemeCompilerTest extends UITestBase {

    @Test
    public void runtimeHoverFollowsNormalDerivationAndChildOverrides() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Leaf{cn1-derive:Child;} Child{cn1-derive:Base;}"
                + "Child:hover{color:#334455;} Base:hover{color:#112233;background-color:#abcdef;}"
                + "Plain{cn1-derive:Other;} Other{color:#777777;}", resource, "Theme");
        UIManager.getInstance().addThemeProps(resource.getTheme("Theme"));
        for (String uiid : new String[]{"Child", "Leaf"}) {
            Button button = new Button();
            button.setUIID(uiid);
            assertNotNull(button.getHoverStyle(), uiid);
            assertEquals(0x334455, button.getHoverStyle().getFgColor());
            assertEquals(0xabcdef, button.getHoverStyle().getBgColor());
        }
        Button plain = new Button();
        plain.setUIID("Plain");
        org.junit.jupiter.api.Assertions.assertNull(plain.getHoverStyle());
    }

    @Test
    public void runtimeDarkHoverInheritanceRemainsDarkOnlyAndAvoidsCycles() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("DarkChild{cn1-derive:DarkBase;}"
                + "DarkOwn{cn1-derive:DarkBase;} DarkOwn:hover{color:#654321;}"
                + "@media (prefers-color-scheme: dark) { DarkBase:hover{color:#123456;background-color:#abcdef;} }"
                + "CycleA{cn1-derive:CycleB;} CycleB{cn1-derive:CycleA;} CycleA:hover{color:#abcdef;}", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        org.junit.jupiter.api.Assertions.assertNull(theme.get("DarkChild.hover#derive"));
        assertEquals("DarkBase.hover", theme.get("$DarkDarkChild.hover#derive"));
        org.junit.jupiter.api.Assertions.assertNull(theme.get("CycleA.hover#derive"));
        org.junit.jupiter.api.Assertions.assertNull(theme.get("CycleB.hover#derive"));
        Boolean previous = com.codename1.ui.CN.isDarkMode();
        try {
            com.codename1.ui.CN.setDarkMode(Boolean.TRUE);
            UIManager.getInstance().addThemeProps(theme);
            Button child = new Button();
            child.setUIID("DarkChild");
            assertNotNull(child.getHoverStyle());
            assertEquals(0x123456, child.getHoverStyle().getFgColor());
            Button own = new Button();
            own.setUIID("DarkOwn");
            assertEquals(0x654321, own.getHoverStyle().getFgColor());
            assertEquals(0xabcdef, own.getHoverStyle().getBgColor());
        } finally {
            com.codename1.ui.CN.setDarkMode(previous);
        }
    }

    @Test
    public void testCompilesThemeConstantsDeriveAndMutableImages() {
        CSSThemeCompiler compiler = new CSSThemeCompiler();
        MutableResource resource = new MutableResource();

        compiler.compile(
                ":root{--primary:#abc;}"
                + "@constants{spacing: 4px; primaryColor: var(--primary);}"
                + "Button{color:var(--primary);background-color:#112233;padding:1px 2px;cn1-derive:Label;}"
                + "Button:pressed{border-width:2px;border-style:solid;border-color:#ffffff;cn1-mutable-image:btnBg #ff00ff;}"
                + "Label{margin:2px 4px 6px 8px;}"
                + "Button{color:pink;text-align:center;border:1px solid #00ff00;}"
                + "Button.pressed{color:#00ff00;}",
                resource,
                "Theme"
        );

        Hashtable theme = resource.getTheme("Theme");
        assertEquals("ffc0cb", theme.get("Button.fgColor"));
        assertEquals("112233", theme.get("Button.bgColor"));
        assertEquals("255", theme.get("Button.transparency"));
        assertEquals("1,2,1,2", theme.get("Button.padding"));
        assertEquals("2,4,6,8", theme.get("Label.margin"));
        assertEquals("Label", theme.get("Button.derive"));
        assertEquals("#abc", theme.get("@primary"));
        assertEquals("4px", theme.get("@spacing"));
        assertEquals("#abc", theme.get("@primarycolor"));
        assertEquals(Integer.valueOf(Component.CENTER), theme.get("Button.align"));
        assertTrue(theme.get("Button.border") instanceof CSSBorder);
        assertEquals("00ff00", theme.get("Button.press#fgColor"));

        UIManager.getInstance().addThemeProps(theme);
        Button runtimeButton = new Button("Runtime");
        runtimeButton.setUIID("Button");
        assertEquals(0xffc0cb, runtimeButton.getUnselectedStyle().getFgColor());
        assertEquals(Component.CENTER, runtimeButton.getUnselectedStyle().getAlignment());
        assertNotNull(runtimeButton.getUnselectedStyle().getBorder());

        Image mutable = resource.getImage("btnBg");
        assertNotNull(mutable);
        assertNotNull(theme.get("Button.press#bgImage"));
    }
    @Test
    public void testThrowsOnMalformedCss() {
        CSSThemeCompiler compiler = new CSSThemeCompiler();
        MutableResource resource = new MutableResource();

        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                compiler.compile("Button{color:#12;}", resource, "Theme")
        );
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                compiler.compile("Button{color:#ff00ff;text-align:middle;}", resource, "Theme")
        );
        // Deliberately a nonsense pseudo state rather than a real-but-unimplemented one.
        // This assertion used ":hover", which stopped throwing the moment hover became a
        // supported state -- and then the test read as a regression instead of as a feature
        // landing. A name no state will ever be called keeps it testing what it is for:
        // that an unknown pseudo state is rejected rather than silently ignored.
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                compiler.compile("Button:notarealstate{color:#ff00ff;}", resource, "Theme")
        );
    }

    /**
     * Both spellings of the hover state reach the same prefix. This compiler accepts a pseudo
     * (`:hover`) and a dot-class (`.hover`) selector interchangeably - {@code selector()}
     * splits on whichever separator comes first - whereas the build-time compiler in
     * maven/css-compiler only understands the dot-class form, which is what
     * native-themes/README.md tells theme authors to write. Pinning both here means the
     * looser one cannot quietly drift.
     */
    @Test
    public void testCompilesHoverInBothSelectorSpellings() {
        CSSThemeCompiler compiler = new CSSThemeCompiler();
        MutableResource resource = new MutableResource();
        compiler.compile("Button{color:#111111;}"
                + "Button:hover{color:#222222;}"
                + "Label{color:#333333;}"
                + "Label.hover{color:#444444;}", resource, "Theme");

        Hashtable theme = resource.getTheme("Theme");
        assertEquals("222222", theme.get("Button.hover#fgColor"));
        assertEquals("444444", theme.get("Label.hover#fgColor"));
    }

    @Test
    public void testCompilesDarkModeMediaQueriesToDarkUiids() {
        CSSThemeCompiler compiler = new CSSThemeCompiler();
        MutableResource resource = new MutableResource();

        compiler.compile(
                "Button{color:#111111;}"
                + "@media (prefers-color-scheme: dark){"
                + "Button{color:#eeeeee;background-color:#000000;}"
                + "Button:pressed{color:#ff0000;}"
                + "}",
                resource,
                "Theme"
        );

        Hashtable theme = resource.getTheme("Theme");
        assertEquals("111111", theme.get("Button.fgColor"));
        assertEquals("eeeeee", theme.get("$DarkButton.fgColor"));
        assertEquals("000000", theme.get("$DarkButton.bgColor"));
        assertEquals("255", theme.get("$DarkButton.transparency"));
        assertEquals("ff0000", theme.get("$DarkButton.press#fgColor"));
    }

    @Test
    public void testCompilesUnselectedStateSelector() {
        CSSThemeCompiler compiler = new CSSThemeCompiler();
        MutableResource resource = new MutableResource();

        compiler.compile("Button.unselected{color:white;}", resource, "Theme");

        Hashtable theme = resource.getTheme("Theme");
        assertEquals("ffffff", theme.get("Button.fgColor"));
    }

    @Test
    public void testCompilesSideBorderShorthand() {
        CSSThemeCompiler compiler = new CSSThemeCompiler();
        MutableResource resource = new MutableResource();

        compiler.compile("SideCommand{border:none;border-bottom:2px solid #cccccc;}", resource, "Theme");

        Hashtable theme = resource.getTheme("Theme");
        assertTrue(theme.get("SideCommand.border") instanceof CSSBorder);
        String css = ((CSSBorder) theme.get("SideCommand.border")).toCSSString();
        assertTrue(css.contains("border-style:none none solid none"));
        assertTrue(css.contains("border-width:"));
        assertTrue(css.contains("2px"));
        assertTrue(css.contains("#ccccccff"));
    }

    @Test
    public void testCompilesSideBorderLonghands() {
        CSSThemeCompiler compiler = new CSSThemeCompiler();
        MutableResource resource = new MutableResource();

        compiler.compile(
                "SideCommand{border:none;border-top-width:3px;border-top-style:dashed;border-top-color:#ff0000;}",
                resource, "Theme");

        Hashtable theme = resource.getTheme("Theme");
        assertTrue(theme.get("SideCommand.border") instanceof CSSBorder);
        String css = ((CSSBorder) theme.get("SideCommand.border")).toCSSString();
        assertTrue(css.contains("border-style:dashed none none none"));
        assertTrue(css.contains("3px"));
        assertTrue(css.contains("#ff0000ff"));
    }

}
