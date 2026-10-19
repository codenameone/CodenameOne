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
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.CSSBorder;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.MutableResource;
import java.util.Hashtable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        // Theme storage order is top,bottom,left,right; CSS writes top,right,bottom,left.
        assertEquals("1,1,2,2", theme.get("Button.padding"));
        assertEquals("2,6,8,4", theme.get("Label.margin"));
        assertEquals("Label", theme.get("Button.derive"));
        assertEquals("#abc", theme.get("@primary"));
        assertEquals("4px", theme.get("@spacing"));
        // A constant resolving to a color is stored as plain hex; :root's own stays as written.
        assertEquals("aabbcc", theme.get("@primaryColor"));
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

    @Test
    public void testHashConstantsBlockKeepsCaseAndDecodesQuotes() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants {\n"
                + "    includeNativeBool: true;\n"
                + "    customMessage: \"Hello\";\n"
                + "    singleQuotedBool: 'true';\n"
                + "    withSemicolon: \"a;b\";\n"
                + "    --accentColor: #ff2d95;\n"
                + "}\n"
                + "Label{color:var(--accentColor);}", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("true", theme.get("@includeNativeBool"));
        assertFalse(theme.containsKey("@includenativebool"));
        assertEquals("Hello", theme.get("@customMessage"));
        assertEquals("true", theme.get("@singleQuotedBool"));
        assertEquals("a;b", theme.get("@withSemicolon"));
        // Plain hex, as the build-time compiler stores a color constant.
        assertEquals("ff2d95", theme.get("@accentColor"));
        assertEquals("ff2d95", theme.get("Label.fgColor"));
        // The block is constants only, never a UIID called "#Constants".
        for (Object key : theme.keySet()) {
            assertFalse(String.valueOf(key).indexOf("Constants") > -1, String.valueOf(key));
        }
    }

    @Test
    public void testLegacyConstantsBlockStillWorksAndMayFollowItsUse() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Label{color:var(--tint);}"
                + "@constants{ tint: #102030; quoted: \"x y\"; plain: 12; }"
                + "@constants{ second: 'two'; }", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("102030", theme.get("@tint"));
        assertEquals("x y", theme.get("@quoted"));
        assertEquals("12", theme.get("@plain"));
        assertEquals("two", theme.get("@second"));
        assertEquals("102030", theme.get("Label.fgColor"));
    }

    @Test
    public void testRootCustomPropertiesKeepCase() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile(":root{--mainColor:#123456;}Label{color:var(--mainColor);}",
                resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("#123456", theme.get("@mainColor"));
        assertEquals("123456", theme.get("Label.fgColor"));
    }

    @Test
    public void testBoxShorthandForEveryLength() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("One{padding:5px;}"
                + "Two{padding:1px 2px;}"
                + "Three{padding:1px 2px 3px;}"
                + "Four{padding:1px 2px 3px 4px;}", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("5,5,5,5", theme.get("One.padding"));
        assertEquals("1,1,2,2", theme.get("Two.padding"));
        assertEquals("1,3,2,2", theme.get("Three.padding"));
        assertEquals("1,3,4,2", theme.get("Four.padding"));
        byte px = Style.UNIT_TYPE_PIXELS;
        assertArrayEquals(new byte[]{px, px, px, px}, (byte[]) theme.get("Four.padUnit"));
    }

    @Test
    public void testIssueExampleResolvesToTheRightSides() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Label {\n    padding: 1px 2px 3px 4px;\n    margin: 1mm 2mm;\n}",
                resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("1,3,4,2", theme.get("Label.padding"));
        assertEquals("1,1,2,2", theme.get("Label.margin"));
        byte mm = Style.UNIT_TYPE_DIPS;
        assertArrayEquals(new byte[]{mm, mm, mm, mm}, (byte[]) theme.get("Label.marUnit"));

        UIManager.getInstance().addThemeProps(theme);
        Style s = new Label("x").getUnselectedStyle();
        assertEquals(1f, s.getPaddingFloatValue(false, Component.TOP), 0f);
        assertEquals(2f, s.getPaddingFloatValue(false, Component.RIGHT), 0f);
        assertEquals(3f, s.getPaddingFloatValue(false, Component.BOTTOM), 0f);
        assertEquals(4f, s.getPaddingFloatValue(false, Component.LEFT), 0f);
        assertEquals(1f, s.getMarginFloatValue(false, Component.TOP), 0f);
        assertEquals(2f, s.getMarginFloatValue(false, Component.RIGHT), 0f);
        assertEquals(1f, s.getMarginFloatValue(false, Component.BOTTOM), 0f);
        assertEquals(2f, s.getMarginFloatValue(false, Component.LEFT), 0f);
        assertEquals(mm, s.getMarginUnit()[Component.LEFT]);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getPaddingUnit()[Component.LEFT]);
    }

    @Test
    public void testMixedUnitsKeepEachSidesUnit() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Mixed{padding:1mm 2vw 2.5pt 3px;}"
                + "Mixed:pressed{margin:1.5mm 2cm;}", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        // top=1mm, bottom=2.5pt (a density independent pixel, rounded), left=3px, right=2vw.
        assertEquals("1,3,3,2", theme.get("Mixed.padding"));
        assertArrayEquals(new byte[]{Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_PIXELS,
                        Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_VW},
                (byte[]) theme.get("Mixed.padUnit"));
        assertEquals("1.5,1.5,20,20", theme.get("Mixed.press#margin"));
        byte mm = Style.UNIT_TYPE_DIPS;
        assertArrayEquals(new byte[]{mm, mm, mm, mm}, (byte[]) theme.get("Mixed.press#marUnit"));
        org.junit.jupiter.api.Assertions.assertNull(theme.get("Mixed.marUnit"));
    }

    @Test
    public void testRejectsMalformedBoxValues() {
        CSSThemeCompiler compiler = new CSSThemeCompiler();
        MutableResource resource = new MutableResource();
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                compiler.compile("Label{padding:1px 2px 3px 4px 5px;}", resource, "Theme"));
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                compiler.compile("Label{margin:wide;}", resource, "Theme"));
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                compiler.compile("Label{font-size:huge;}", resource, "Theme"));
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                compiler.compile("Label{font-size:not-small;}", resource, "Theme"));
        // The build-time compiler has no percentage inset.
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                compiler.compile("Label{padding:10%;}", resource, "Theme"));
    }

    @Test
    public void testFontSizeWithFamilyResolvesToTheRequestedSize() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Label {\n    font-family: \"native:MainRegular\";\n    font-size: 3mm;\n}",
                resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        Font font = (Font) theme.get("Label.font");
        assertNotNull(font);
        assertTrue(font.isTTFNativeFont());
        assertEquals(Display.getInstance().convertToPixels(30, true) / 10f, font.getPixelSize(), 0.001f);

        UIManager.getInstance().addThemeProps(theme);
        assertEquals(font, new Label("x").getUnselectedStyle().getFont());
    }

    @Test
    public void testFontSizeUnitsAndStateWithoutFamily() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Sized{font-size:12px;}"
                + "Sized:selected{font-size:18pt;}"
                + "Split{font-family:'native:MainBold';}"
                + "Split{font-size:2mm;}", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals(12f, ((Font) theme.get("Sized.font")).getPixelSize(), 0.001f);
        // 18pt is 6.35mm, the build-time compiler's conversion.
        float pt = Display.getInstance().convertToPixels((int) (18f / 72f * 25.4f * 10), true) / 10f;
        assertEquals(pt, ((Font) theme.get("Sized.sel#font")).getPixelSize(), 0.001f);
        // Family and size declared in two rules for the same UIID still meet.
        Font split = (Font) theme.get("Split.font");
        assertEquals(Display.getInstance().convertToPixels(20, true) / 10f, split.getPixelSize(), 0.001f);
    }

    @Test
    public void testColorConstantsAreStoredAsPlainHex() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{ infiniteDefaultColor: #f00; rgbColor: rgb(1, 2, 3);"
                + " quotedHash: \"#abc\"; word: red; }", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("ff0000", theme.get("@infiniteDefaultColor"));
        assertEquals(0xff0000, Integer.parseInt((String) theme.get("@infiniteDefaultColor"), 16));
        assertEquals("010203", theme.get("@rgbColor"));
        // A quoted value is a string, not a color.
        assertEquals("#abc", theme.get("@quotedHash"));
        assertEquals("red", theme.get("@word"));
    }

    @Test
    public void testImageConstantsResolveToTheResourceImage() {
        MutableResource resource = new MutableResource();
        Image spinner = Image.createImage(4, 4, 0xff00ff00);
        resource.setImage("spinner.png", spinner);
        new CSSThemeCompiler().compile("#Constants{ infiniteImage: spinner.png; }", resource, "Theme");
        assertTrue(resource.getTheme("Theme").get("@infiniteImage") instanceof Image);
        UIManager.getInstance().addThemeProps(resource.getTheme("Theme"));
        assertNotNull(UIManager.getInstance().getThemeImageConstant("infiniteImage"));

        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () -> new CSSThemeCompiler()
                .compile("#Constants{ missingImage: nothing.png; }", new MutableResource(), "Theme"));

        // Through a custom property, quoted or not.
        MutableResource viaVar = new MutableResource();
        viaVar.setImage("spinner.png", spinner);
        new CSSThemeCompiler().compile(":root{ --spinner: spinner.png; --q: \"spinner.png\"; }"
                + "#Constants{ infiniteImage: var(--spinner); quotedImage: var(--q); }", viaVar, "Theme");
        assertTrue(viaVar.getTheme("Theme").get("@infiniteImage") == spinner);
        assertTrue(viaVar.getTheme("Theme").get("@quotedImage") == spinner);
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () -> new CSSThemeCompiler()
                .compile(":root{ --x: nothing.png; } #Constants{ goneImage: var(--x); }",
                        new MutableResource(), "Theme"));
    }

    @Test
    public void testStateFontInheritsTheUndeclaredHalfFromTheBase() {
        MutableResource resource = new MutableResource();
        // The state rule comes first on purpose: the merge must not depend on rule order.
        new CSSThemeCompiler().compile("Label:selected{font-size:20px;}"
                + "Label{font-family:'native:MainBold';font-size:2mm;}"
                + "Label:pressed{font-family:'native:MainLight';}"
                + "@media (prefers-color-scheme: dark){ Label:disabled{color:#ffffff;font-size:9px;} }",
                resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        Font selected = (Font) theme.get("Label.sel#font");
        assertEquals(20f, selected.getPixelSize(), 0.001f);
        assertEquals("native:MainBold", fontName(selected));
        Font pressed = (Font) theme.get("Label.press#font");
        assertEquals(Display.getInstance().convertToPixels(20, true) / 10f, pressed.getPixelSize(), 0.001f);
        assertEquals("native:MainLight", fontName(pressed));
        // A dark rule is its own element in the build-time compiler: no light family reaches it.
        Font dark = (Font) theme.get("$DarkLabel.dis#font");
        assertEquals(9f, dark.getPixelSize(), 0.001f);
        assertEquals("native:MainRegular", fontName(dark));
    }

    @Test
    public void testColorConstantFromVarIsStoredAsPlainHex() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile(":root{--c:#f00;}"
                + ":root{--name:hello;}"
                + "#Constants{infiniteDefaultColor:var(--c); label:var(--name);}", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("ff0000", theme.get("@infiniteDefaultColor"));
        assertEquals("hello", theme.get("@label"));
    }

    @Test
    public void testLaterDeclarationWinsBetweenRootAndConstants() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile(":root{--accent:#00ff00;} #Constants{--accent:#ff0000;}"
                + "Label{color:var(--accent);}", resource, "Theme");
        assertEquals("ff0000", resource.getTheme("Theme").get("@accent"));
        assertEquals("ff0000", resource.getTheme("Theme").get("Label.fgColor"));

        resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{--accent:#ff0000;} :root{--accent:#00ff00;}"
                + "Label{color:var(--accent);}", resource, "Theme");
        // Only #Constants exports: the later :root changes var(--accent), not @accent.
        assertEquals("ff0000", resource.getTheme("Theme").get("@accent"));
        assertEquals("00ff00", resource.getTheme("Theme").get("Label.fgColor"));
        // A constant declared after the rule that uses it is not seen by that rule: the build-time
        // parser resolves var() in source order, #Constants blocks included.
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () -> new CSSThemeCompiler()
                .compile("Label{color:var(--late);} #Constants{--late:#0000ff;}", new MutableResource(), "Theme"));
    }

    @Test
    public void testStateFontFollowsTheDeriveChain() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Child:selected{font-size:20px;}"
                + "Child{cn1-derive:Middle;}"
                + "Middle{cn1-derive:Base;}"
                + "Base:selected{font-family:'native:MainBold';}"
                + "Base{font-family:'native:MainLight';font-size:2mm;}"
                + "Cyc1{cn1-derive:Cyc2;} Cyc2{cn1-derive:Cyc1;} Cyc1:pressed{font-size:7px;}",
                resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        Font selected = (Font) theme.get("Child.sel#font");
        assertEquals(20f, selected.getPixelSize(), 0.001f);
        // Base's selected family wins over Base's unselected one: same state first.
        assertEquals("native:MainBold", fontName(selected));
        Font cyclic = (Font) theme.get("Cyc1.press#font");
        assertEquals(7f, cyclic.getPixelSize(), 0.001f);
        assertEquals("native:MainRegular", fontName(cyclic));
    }

    @Test
    public void testImageConstantMaySeeAnImageARuleCreates() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Maker{cn1-mutable-image:spinner #ffffff;}"
                + "#Constants{infiniteImage:spinner;}", resource, "Theme");
        assertTrue(resource.getTheme("Theme").get("@infiniteImage") == resource.getImage("spinner"));
    }

    @Test
    public void testOnlyTheWinningConstantDeclarationIsReinterpreted() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile(":root{--x:#00ff00;}"
                + "#Constants{token:var(--x);} #Constants{token:\"#abc\";}", resource, "Theme");
        assertEquals("#abc", resource.getTheme("Theme").get("@token"));
    }

    @Test
    public void testQuotedBraceDoesNotEndTheConstantsBlock() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{customMessage:\"a}b\"; enabled:true;}"
                + "Label{color:#ff0000;}", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("a}b", theme.get("@customMessage"));
        assertEquals("true", theme.get("@enabled"));
        assertEquals("ff0000", theme.get("Label.fgColor"));
    }

    @Test
    public void testDerivedStateFontBeatsTheChildsUnselectedFont() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Child{cn1-derive:Base;font-family:'native:MainLight';}"
                + "Base:selected{font-family:'native:MainBold';}"
                + "Child:selected{font-size:20px;}", resource, "Theme");
        Font selected = (Font) resource.getTheme("Theme").get("Child.sel#font");
        assertEquals(20f, selected.getPixelSize(), 0.001f);
        assertEquals("native:MainBold", fontName(selected));
    }

    @Test
    public void testRgbaColorConstantKeepsItsRgb() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile(":root{--half:rgba(0, 0, 255, 0.25);}"
                + "#Constants{infiniteDefaultColor: rgba(255, 0, 0, 0.5); viaVar: var(--half);}",
                resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("ff0000", theme.get("@infiniteDefaultColor"));
        assertEquals("0000ff", theme.get("@viaVar"));
    }

    @Test
    public void testCommentMarkersInsideQuotesAreText() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("/* a real comment */ #Constants{ message: \"a/*b*/c\"; open: '/*'; }"
                + "Label{color:#ff0000;}", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("a/*b*/c", theme.get("@message"));
        assertEquals("/*", theme.get("@open"));
        assertEquals("ff0000", theme.get("Label.fgColor"));
    }

    @Test
    public void testStateFontIgnoresTheUniversalStyle() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("* { font-family: 'native:MainBold'; } Label:selected { font-size: 20px; }",
                resource, "Theme");
        Font selected = (Font) resource.getTheme("Theme").get("Label.sel#font");
        assertEquals(20f, selected.getPixelSize(), 0.001f);
        // The build-time compiler ignores `*` rules, so nothing is inherited from it.
        assertEquals("native:MainRegular", fontName(selected));
    }

    @Test
    public void testCyclicConstantsAreRejected() {
        // The build-time compiler adds a custom property before it evaluates it, so a self or
        // mutual reference is a cycle that fails the build. A plain name is no variable, so a
        // reference to it is undefined and the constant is left out instead.
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () -> new CSSThemeCompiler()
                .compile("#Constants{ --a: var(--a); }", new MutableResource(), "Theme"));
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () -> new CSSThemeCompiler()
                .compile("#Constants{ --b: var(--c); --c: var(--b); }", new MutableResource(), "Theme"));
        MutableResource cyclic = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{ a: var(--a); }", cyclic, "Theme");
        assertFalse(cyclic.getTheme("Theme").containsKey("@a"));
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () -> new CSSThemeCompiler()
                .compile(":root{ --a: var(--b); } :root{ --b: var(--a); } Label{ color: var(--a); }",
                        new MutableResource(), "Theme"));
        // Two uses of one constant are not a cycle. The build-time constants emitter writes only
        // the leading unit of the chain.
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{ --c: #ff0000; pair: var(--c) var(--c); }", resource, "Theme");
        assertEquals("ff0000", resource.getTheme("Theme").get("@pair"));
    }

    @Test
    public void testPlainConstantDoesNotHideAVariableOfTheSameName() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{--accent:#f00;} #Constants{accent:blue;}"
                + " Label{color:var(--accent);}", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("blue", theme.get("@accent"));
        assertEquals("ff0000", theme.get("Label.fgColor"));
    }

    @Test
    public void testEscapedFontFamilyIdentifierIsDecoded() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Label{font-family:native\\:MainBold;font-size:2mm;}", resource, "Theme");
        assertEquals("native:MainBold", fontName((Font) resource.getTheme("Theme").get("Label.font")));
    }

    @Test
    public void testExponentConstantIsRejected() {
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                new CSSThemeCompiler().compile("#Constants{ scale: 1e2; }", new MutableResource(), "Theme"));
    }

    @Test
    public void testLaterRootPropertyChangesTheVariableNotTheConstant() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{--accent:#f00;} :root{--accent:#0f0;}"
                + " Label{color:var(--accent);}", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("ff0000", theme.get("@accent"));
        assertEquals("00ff00", theme.get("Label.fgColor"));
    }

    @Test
    public void testVariableAliasIsResolvedWhereItIsUsed() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{--base:#f00;--alias:var(--base);--base:#00f;}"
                + " Label{color:var(--alias);}", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("ff0000", theme.get("@alias"));
        assertEquals("0000ff", theme.get("Label.fgColor"));
    }

    @Test
    public void testColorVariableKeepsItsCssSpellingInABorder() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{--accent:#f00;} Label{border:1px solid var(--accent);}",
                resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("ff0000", theme.get("@accent"));
        assertNotNull(theme.get("Label.border"));
    }

    @Test
    public void testMultiTokenAliasExportsItsLeadingToken() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{--x:foo; pair:var(--x) bar;}", resource, "Theme");
        assertEquals("foo", resource.getTheme("Theme").get("@pair"));
    }

    @Test
    public void testExponentInsetWithUnitIsRejected() {
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                new CSSThemeCompiler().compile("Label{padding:1e2px;}", new MutableResource(), "Theme"));
    }

    @Test
    public void testUnescapedNewlineInStringIsRejected() {
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                new CSSThemeCompiler().compile("#Constants{message:\"first\nsecond\";}", new MutableResource(), "Theme"));
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{message:\"first\\\nsecond\";}", resource, "Theme");
        assertEquals("firstsecond", resource.getTheme("Theme").get("@message"));
    }

    @Test
    public void testQuotedVarTextIsNotResolved() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile(":root{ --name: world; --rootQuoted: \"var(--name)\"; }"
                + "#Constants{ message: \"var(--name)\"; plain: var(--name); }", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("var(--name)", theme.get("@message"));
        assertEquals("var(--name)", theme.get("@rootQuoted"));
        assertEquals("world", theme.get("@plain"));
    }

    @Test
    public void testRulesSeeTheFinalValueOfARedeclaredConstant() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{--accent:#ff0000;} Label{color:var(--accent);}"
                + " #Constants{--accent:#0000ff;}"
                + ":root{--bg:#ff0000;} Button{background-color:var(--bg);} :root{--bg:#00ff00;}",
                resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("0000ff", theme.get("@accent"));
        // The rule sits between the two blocks, so it sees the first value.
        assertEquals("ff0000", theme.get("Label.fgColor"));
        // A :root property takes effect where it is declared, as in the build-time parser.
        assertEquals("ff0000", theme.get("Button.bgColor"));
        assertEquals("#00ff00", theme.get("@bg"));
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () -> new CSSThemeCompiler()
                .compile("Label{color:var(--late);} :root{--late:#ff0000;}", new MutableResource(), "Theme"));
    }

    @Test
    public void testStateDeriveIsFollowedAtEveryHop() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Base{font-family:'native:MainBold';}"
                + "Middle:selected{cn1-derive:Base;}"
                + "Child:selected{cn1-derive:Middle;font-size:20px;}", resource, "Theme");
        assertEquals("native:MainBold", fontName((Font) resource.getTheme("Theme").get("Child.sel#font")));
    }

    @Test
    public void testAliasOfAQuotedConstantStaysText() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{--literal:\"#abc\"; --alias:var(--literal); twice:var(--alias);"
                + " --color:#abc; colorAlias:var(--color);}", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("#abc", theme.get("@alias"));
        assertEquals("#abc", theme.get("@twice"));
        assertEquals("aabbcc", theme.get("@colorAlias"));
    }

    @Test
    public void testCn1ColorFunctionsInConstants() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{ a: cn1rgb(255,0,0); b: cn1rgba(0,255,0,0.5); }",
                resource, "Theme");
        assertEquals("ff0000", resource.getTheme("Theme").get("@a"));
        assertEquals("00ff00", resource.getTheme("Theme").get("@b"));
    }

    @Test
    public void testQuotedConstantStaysTextThroughAnAlias() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{--name:world;--message:\"var(--name)\";alias:var(--message);}",
                resource, "Theme");
        assertEquals("var(--name)", resource.getTheme("Theme").get("@alias"));
    }

    @Test
    public void testStateFontFollowsTheStatesOwnDerive() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Base{font-family:'native:MainBold';}"
                + "Child:selected{cn1-derive:Base;font-size:20px;}", resource, "Theme");
        Font selected = (Font) resource.getTheme("Theme").get("Child.sel#font");
        assertEquals(20f, selected.getPixelSize(), 0.001f);
        assertEquals("native:MainBold", fontName(selected));
    }

    @Test
    public void testQuotedConstantsDecodeHexEscapes() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{ a: \"\\41\"; b: \"\\41 B\"; c: \"\\1F600\"; d: \"x\\\"y\"; }",
                resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("A", theme.get("@a"));
        assertEquals("AB", theme.get("@b"));
        assertEquals(new String(Character.toChars(0x1F600)), theme.get("@c"));
        assertEquals("x\"y", theme.get("@d"));
    }

    @Test
    public void testSecondCompileKeepsEarlierLiteralConstants() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{message:\"var(--name)\";}", resource, "Theme");
        new CSSThemeCompiler().compile("Label{color:#ff0000;}", resource, "Theme");
        assertEquals("var(--name)", resource.getTheme("Theme").get("@message"));
    }

    @Test
    public void testSiblingStateFontUsesADeriveDeclaredInAnotherState() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Base{font-family:'native:MainBold';}"
                + "Child:selected{cn1-derive:Base;}"
                + "Child:pressed{font-size:20px;}", resource, "Theme");
        assertEquals("native:MainBold", fontName((Font) resource.getTheme("Theme").get("Child.press#font")));
    }

    @Test
    public void testRelativeFontSizeKeywordsAreRejected() {
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () -> new CSSThemeCompiler()
                .compile("Label{font-size:smaller;}", new MutableResource(), "Theme"));
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () -> new CSSThemeCompiler()
                .compile("Label{font-size:larger;}", new MutableResource(), "Theme"));
    }

    @Test
    public void testCrlfLineContinuationInAQuotedConstant() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{a:\"first\\\r\nsecond\";b:\"x\\\ry\";}", resource, "Theme");
        assertEquals("firstsecond", resource.getTheme("Theme").get("@a"));
        assertEquals("xy", resource.getTheme("Theme").get("@b"));
    }

    @Test
    public void testCustomPropertyInARuleAppliesInDeclarationOrder() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Label{--accent:#ff0000;color:var(--accent);--accent:#0000ff;}",
                resource, "Theme");
        assertEquals("ff0000", resource.getTheme("Theme").get("Label.fgColor"));
    }

    @Test
    public void testUnitlessDecimalInsetIsRejected() {
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () -> new CSSThemeCompiler()
                .compile("Label{padding:1.5;}", new MutableResource(), "Theme"));
    }

    @Test
    public void testLastStateDeriveWinsForTheOwningUiid() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("BaseA{font-family:'native:MainBold';}"
                + "BaseB{font-family:'native:MainLight';}"
                + "Child:selected{cn1-derive:BaseA;}"
                + "Child:disabled{cn1-derive:BaseB;}"
                + "Child:pressed{font-size:20px;}", resource, "Theme");
        assertEquals("native:MainLight", fontName((Font) resource.getTheme("Theme").get("Child.press#font")));
    }

    @Test
    public void testSecondCompileKeepsTheFamilyOfAnEarlierFont() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Label{font-family:'native:MainBold';font-size:10px;}", resource, "Theme");
        new CSSThemeCompiler().compile("Label:selected{font-size:20px;}", resource, "Theme");
        assertEquals("native:MainBold", fontName((Font) resource.getTheme("Theme").get("Label.sel#font")));
    }

    @Test
    public void testSameStateRecompileKeepsTheEarlierFamily() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Label{font-family:'native:MainBold';font-size:10px;}", resource, "Theme");
        new CSSThemeCompiler().compile("Label{font-size:20px;}", resource, "Theme");
        assertEquals("native:MainBold", fontName((Font) resource.getTheme("Theme").get("Label.font")));
    }

    @Test
    public void testAliasOfAnEarlierCompilesLiteralStaysText() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{message:\"var(--name)\";}", resource, "Theme");
        new CSSThemeCompiler().compile("#Constants{alias:var(--message);}", resource, "Theme");
        assertEquals("var(--name)", resource.getTheme("Theme").get("@alias"));
    }

    @Test
    public void testAliasOfAnEarlierUnquotedColorBecomesAColor() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile(":root{--accent:#abc;}", resource, "Theme");
        new CSSThemeCompiler().compile("#Constants{alias:var(--accent);}", resource, "Theme");
        assertEquals("aabbcc", String.valueOf(resource.getTheme("Theme").get("@alias")).toLowerCase());
    }

    @Test
    public void testRecompileKeepsAMonospaceFamily() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Label{font-family:monospace;}", resource, "Theme");
        new CSSThemeCompiler().compile("Label{font-size:20px;}", resource, "Theme");
        assertEquals(Font.FACE_MONOSPACE, ((Font) resource.getTheme("Theme").get("Label.font")).getFace());
    }

    @Test
    public void testDeclarationPriorityIsNotPartOfTheValue() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{includeNativeBool:true !important;msg:\"a !important\";}",
                resource, "Theme");
        assertEquals("true", resource.getTheme("Theme").get("@includeNativeBool"));
        assertEquals("a !important", resource.getTheme("Theme").get("@msg"));
    }

    @Test
    public void testQuotedCustomPropertyIsNotAColorInAStyle() {
        MutableResource resource = new MutableResource();
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () -> new CSSThemeCompiler()
                .compile("#Constants{--accent:\"#f00\";} Label{color:var(--accent);}", resource, "Theme"));
        new CSSThemeCompiler().compile("#Constants{--accent:\"#f00\";}", resource, "Theme");
        assertEquals("#f00", resource.getTheme("Theme").get("@accent"));
    }

    @Test
    public void testTheLastDeriveWinsAlsoForAStateWithItsOwn() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("BaseA{font-family:'native:MainBold';}"
                + "BaseB{font-family:'native:MainLight';}"
                + "Child:selected{cn1-derive:BaseA;}"
                + "Child:disabled{cn1-derive:BaseB;}"
                + "Child:selected{font-size:20px;}", resource, "Theme");
        assertEquals("native:MainLight", fontName((Font) resource.getTheme("Theme").get("Child.sel#font")));
    }

    @Test
    public void testFamilyWalkContinuesPastANativeFont() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("A{font-family:'native:MainBold',monospace;}"
                + "B{font-family:'native:MainBold','native:MainLight';}", resource, "Theme");
        assertEquals(Font.FACE_MONOSPACE, ((Font) resource.getTheme("Theme").get("A.font")).getFace());
        assertEquals("native:MainLight", fontName((Font) resource.getTheme("Theme").get("B.font")));
    }

    @Test
    public void testJavaOnlyNumberSpellingsAreRejected() {
        for (String bad : new String[]{"1fpx", "NaNpx", "Infinitypx", "1dpx", "0x1p3px", "1.px", "1e1px"}) {
            assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () -> new CSSThemeCompiler()
                    .compile("Label{padding:" + bad + ";}", new MutableResource(), "Theme"), bad);
        }
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Label{padding:.5mm 10px +2px -0px;}", resource, "Theme");
        assertEquals("0.5,2,0,10", resource.getTheme("Theme").get("Label.padding"));
    }

    @Test
    public void testConstantsAliasKeepsTheValueAtItsDeclaration() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{--accent:#f00;alias:var(--accent);--accent:#00f;"
                + "early:var(--later);--later:#0f0;--lit:\"#abc\";litAlias:var(--lit);}", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("ff0000", String.valueOf(theme.get("@alias")).toLowerCase());
        assertEquals("0000ff", String.valueOf(theme.get("@accent")).toLowerCase());
        // A forward reference is undefined where it is declared.
        assertFalse(theme.containsKey("@early"));
        assertEquals("#abc", theme.get("@litAlias"));
    }

    @Test
    public void testVarFallbackIsUsedForAnUndefinedVariable() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{accentColor:var(--missing, #f00);--set:#00f;"
                + "setColor:var(--set, #f00);} Label{color:var(--nope, rgb(0, 255, 0));}", resource, "Theme");
        Hashtable theme = resource.getTheme("Theme");
        assertEquals("ff0000", String.valueOf(theme.get("@accentColor")).toLowerCase());
        assertEquals("0000ff", String.valueOf(theme.get("@setColor")).toLowerCase());
        assertEquals("00ff00", String.valueOf(theme.get("Label.fgColor")).toLowerCase());
    }

    private static String fontName(Font font) {
        try {
            java.lang.reflect.Field id = Font.class.getDeclaredField("fontUniqueId");
            id.setAccessible(true);
            return (String) id.get(font);
        } catch (ReflectiveOperationException err) {
            throw new AssertionError(err);
        }
    }


    @Test
    void testPriorityAfterAStringHoldingTheOtherQuoteIsStripped() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { msg: \"don't\" !important; }", res, "Theme");
        assertEquals("don't", res.getTheme("Theme").get("@msg"));
    }

    @Test
    void testLengthAndPercentageConstantsAreRejected() {
        for (String value : new String[]{"50%", "2mm", "1.5px", "url(foo.png)"}) {
            assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                    new CSSThemeCompiler().compile("#Constants { progress: " + value + "; }", new MutableResource(), "Theme"));
        }
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { progress: \"50%\"; }", res, "Theme");
        assertEquals("50%", res.getTheme("Theme").get("@progress"));
    }

    @Test
    void testNumericConstantsAreStoredAsTheBuildSerializesThem() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { scale: 1.50; count: 001; whole: 2.0; flag: true; }", res, "Theme");
        Hashtable theme = res.getTheme("Theme");
        assertEquals("1.5", theme.get("@scale"));
        assertEquals("1", theme.get("@count"));
        assertEquals("2", theme.get("@whole"));
        assertEquals("true", theme.get("@flag"));
    }

    @Test
    void testAKeywordSizeKeepsTheInheritedNativeFamily() {
        MutableResource resource = new MutableResource();
        CSSThemeCompiler compiler = new CSSThemeCompiler();
        compiler.compile("Label{font-family:'native:MainBold';font-size:2mm;}", resource, "Theme");
        compiler.compile("Label{font-size:large;}", resource, "Theme");
        Font font = (Font) resource.getTheme("Theme").get("Label.font");
        assertEquals("native:MainBold", fontName(font));
        // Font.derive hands back the cached base font for its own natural height, so the height
        // is compared rather than the requested pixel size.
        assertEquals(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE).getHeight(),
                font.getHeight());
    }

    @Test
    void testUnitlessZeroFontSizeIsAccepted() {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile("Label{font-size:0;}", resource, "Theme");
        assertNotNull(resource.getTheme("Theme").get("Label.font"));
    }

    @Test
    void testEscapedAndNonAsciiIdentifierConstants() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { a: caf\u00e9; b: foo\\ bar; c: \\41 x; }", res, "Theme");
        Hashtable theme = res.getTheme("Theme");
        assertEquals("caf\u00e9", theme.get("@a"));
        assertEquals("foo bar", theme.get("@b"));
        assertEquals("Ax", theme.get("@c"));
    }

    @Test
    void testWhitespaceInsideVarIsAllowed() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile(":root{--accent:#f00} Label{color:var( --accent )}", res, "Theme");
        assertEquals("ff0000", res.getTheme("Theme").get("Label.fgColor"));
    }

    @Test
    void testAConstantWithAnUndefinedVariableIsLeftOut() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { label: var(--missing); other: var(--label); kept: var(--missing, x); }",
                res, "Theme");
        Hashtable theme = res.getTheme("Theme");
        assertFalse(theme.containsKey("@label"));
        assertFalse(theme.containsKey("@other"));
        assertEquals("x", theme.get("@kept"));
    }

    @Test
    void testUnterminatedVarIsRejected() {
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                new CSSThemeCompiler().compile("#Constants { label: var(--name; }", new MutableResource(), "Theme"));
    }

    @Test
    void testExponentUnitlessInsetIsRejected() {
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                new CSSThemeCompiler().compile("Label{padding:1e2;}", new MutableResource(), "Theme"));
    }

    @Test
    void testFractionalPixelFontSizeTakesTheIntegerPart() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("Label{font-family:'native:MainBold';font-size:12.5px;} Small{font-family:'native:MainBold';font-size:0.5px;}",
                res, "Theme");
        assertEquals(12f, ((Font) res.getTheme("Theme").get("Label.font")).getPixelSize(), 0.001f);
        assertEquals(0.5f, ((Font) res.getTheme("Theme").get("Small.font")).getPixelSize(), 0.001f);
    }

    @Test
    void testAFallbackIsStoredByItsTokenType() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { message: var(--missing, \"hello\"); scale: var(--missing, 1.50); }",
                res, "Theme");
        Hashtable theme = res.getTheme("Theme");
        assertEquals("hello", theme.get("@message"));
        assertEquals("1.5", theme.get("@scale"));
    }

    @Test
    void testPriorityWithWhitespaceAfterTheBang() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { includeNativeBool: true ! important; }", res, "Theme");
        assertEquals("true", res.getTheme("Theme").get("@includeNativeBool"));
    }

    @Test
    void testAnOrdinaryConstantIsNotACustomProperty() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { accent: #f00; --real: #0f0; alias: var(--accent); other: var(--real); }",
                res, "Theme");
        Hashtable theme = res.getTheme("Theme");
        assertFalse(theme.containsKey("@alias"));
        assertEquals("00ff00", theme.get("@other"));
    }

    @Test
    void testAColorNamedImageIsStoredAsAColor() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { placeholderImage: #f00; }", res, "Theme");
        assertEquals("ff0000", res.getTheme("Theme").get("@placeholderImage"));
    }

    @Test
    void testAnImageAliasKeepsTheImageAtItsDeclaration() {
        MutableResource res = new MutableResource();
        Image first = Image.createImage(3, 3);
        res.setImage("first.png", first);
        res.setImage("second.png", Image.createImage(4, 4));
        new CSSThemeCompiler().compile("#Constants { --icon: \"first.png\"; navImage: var(--icon); --icon: \"second.png\"; }",
                res, "Theme");
        assertEquals(3, ((Image) res.getTheme("Theme").get("@navImage")).getWidth());
    }

    @Test
    void testDefaultPropertiesAreAlsoStoredAtTheRoot() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("Default{color:#ff0000;font-size:20px;}", res, "Theme");
        Hashtable theme = res.getTheme("Theme");
        assertEquals("ff0000", theme.get("fgColor"));
        assertEquals(20f, ((Font) theme.get("font")).getPixelSize(), 0.001f);
    }

    @Test
    void testUniversalRulesSetNoFont() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("*{font-size:20px;}", res, "Theme");
        assertFalse(res.getTheme("Theme").containsKey("Component.font"));
    }

    @Test
    void testEscapedIdentifiers() {
        MutableResource res = new MutableResource();
        Image icon = Image.createImage(5, 5);
        res.setImage("my icon", icon);
        new CSSThemeCompiler().compile("#Constants { emptyImage: my\\ icon; message: foo\\!important; }", res, "Theme");
        Hashtable theme = res.getTheme("Theme");
        assertEquals(5, ((Image) theme.get("@emptyImage")).getWidth());
        assertEquals("foo!important", theme.get("@message"));

        res = new MutableResource();
        res.setImage("my icon", icon);
        new CSSThemeCompiler().compile("#Constants { --icon: my\\ icon; navImage: var(--icon); }", res, "Theme");
        assertEquals(5, ((Image) res.getTheme("Theme").get("@navImage")).getWidth());
    }

    @Test
    void testDirectMultiTokenConstantExportsItsLeadingToken() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{ pair: foo bar; num: 1.50 2; col: #f00 x; str: \"a b\" c; }",
                res, "Theme");
        Hashtable theme = res.getTheme("Theme");
        assertEquals("foo", theme.get("@pair"));
        assertEquals("1.5", theme.get("@num"));
        assertEquals("ff0000", theme.get("@col"));
        assertEquals("a b", theme.get("@str"));
    }

    @Test
    void testLaterRootReplacesALegacyConstant() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("@constants { accent: red; } :root { --accent: blue; }", res, "Theme");
        assertEquals("blue", res.getTheme("Theme").get("@accent"));
    }

    @Test
    void testSelfReferencingCustomPropertyIsACycle() {
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () -> new CSSThemeCompiler()
                .compile("#Constants { --accent: var(--accent, #f00); }", new MutableResource(), "Theme"));
    }

    @Test
    void testPressedDeriveIsNotPromotedToTheOwner() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("Base{font-family:native:MainBold;font-size:2mm;}"
                + " Child:pressed{cn1-derive:Base;} Child:selected{font-size:20px;}", res, "Theme");
        assertEquals("native:MainRegular", fontName((Font) res.getTheme("Theme").get("Child.sel#font")));
    }

    @Test
    void testImageConstantLooksUpItsLeadingToken() {
        MutableResource res = new MutableResource();
        res.setImage("icon", Image.createImage(4, 4));
        new CSSThemeCompiler().compile("#Constants { navImage: icon extra; }", res, "Theme");
        assertEquals(4, ((Image) res.getTheme("Theme").get("@navImage")).getWidth());
    }

    @Test
    void testEscapedRedeclarationHidesAnEarlierCompilesValue() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { --abc: red; }", res, "Theme");
        new CSSThemeCompiler().compile("#Constants { early: var(--abc); --a\\62 c: blue; }", res, "Theme");
        assertFalse(res.getTheme("Theme").containsKey("@early"));
        assertEquals("blue", res.getTheme("Theme").get("@abc"));
    }

    @Test
    void testImageAliasChainKeepsItsQuotedName() {
        MutableResource res = new MutableResource();
        res.setImage("my icon", Image.createImage(5, 5));
        new CSSThemeCompiler().compile("#Constants { --name: \"my icon\"; --alias: var(--name);"
                + " navImage: var(--alias); }", res, "Theme");
        assertEquals(5, ((Image) res.getTheme("Theme").get("@navImage")).getWidth());
    }

    @Test
    void testEscapedSemicolonStaysInTheIdentifier() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { message: foo\\;bar; }", res, "Theme");
        assertEquals("foo;bar", res.getTheme("Theme").get("@message"));
    }

    @Test
    void testDarkFontDoesNotFollowTheLightDerive() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("Child{cn1-derive:Base;}"
                + " @media (prefers-color-scheme: dark){ Base{font-family:native:MainBold;font-size:2mm;}"
                + " Child{font-size:20px;} }", res, "Theme");
        assertEquals("native:MainRegular", fontName((Font) res.getTheme("Theme").get("$DarkChild.font")));
    }

    @Test
    void testOutOfRangeRgbChannelIsClamped() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { accent: rgb(200%, 300, -5); }", res, "Theme");
        assertEquals(0xffff00, Integer.parseInt((String) res.getTheme("Theme").get("@accent"), 16));
    }

    @Test
    void testEscapedQuoteOutsideAStringOpensNoString() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { message: foo\\\"bar; } Label{padding:1px;}", res, "Theme");
        assertEquals("foo\"bar", res.getTheme("Theme").get("@message"));
    }

    @Test
    void testDarkFontIgnoresALightDeriveFromAnEarlierCompile() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("Child{cn1-derive:Base;}", res, "Theme");
        new CSSThemeCompiler().compile("@media (prefers-color-scheme: dark){"
                + " Base{font-family:native:MainBold;font-size:2mm;} Child{font-size:20px;} }", res, "Theme");
        assertEquals("native:MainRegular", fontName((Font) res.getTheme("Theme").get("$DarkChild.font")));
    }

    @Test
    void testConstantsInASelectorList() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants, Other { accentColor: #ff0000; }", res, "Theme");
        assertEquals("ff0000", res.getTheme("Theme").get("@accentColor"));
    }

    @Test
    void testDeriveTargetIsUnquotedAndDecoded() {
        for (String target : new String[]{"\"Base\"", "B\\61se"}) {
            MutableResource res = new MutableResource();
            new CSSThemeCompiler().compile("Base{font-family:native:MainBold;font-size:2mm;}"
                    + " Child{cn1-derive:" + target + ";font-size:20px;}", res, "Theme");
            assertEquals("Base", res.getTheme("Theme").get("Child.derive"), target);
            assertEquals("native:MainBold", fontName((Font) res.getTheme("Theme").get("Child.font")), target);
        }
    }

    @Test
    void testHexEscapeEndsAtAnyCssWhitespace() {
        for (String ws : new String[]{"\r", "\f", "\r\n"}) {
            MutableResource res = new MutableResource();
            new CSSThemeCompiler().compile("#Constants{a\\62" + ws + "c: hello;}", res, "Theme");
            assertEquals("hello", res.getTheme("Theme").get("@abc"));
        }
    }

    @Test
    void testEscapedVarReferenceIsDecoded() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { --a\\62 c: red; alias: var(--a\\62 c); }", res, "Theme");
        assertEquals("red", res.getTheme("Theme").get("@alias"));
    }

    @Test
    void testCustomPropertyMayStartWithADigit() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { --1accent: red; }", res, "Theme");
        assertEquals("red", res.getTheme("Theme").get("@1accent"));
    }

    @Test
    void testEmptyColorComponentIsRejected() {
        for (String color : new String[]{"rgb(255,,0)", "rgb(255,0,)", "rgba(255,,0,0)"}) {
            assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () -> new CSSThemeCompiler().compile(
                    "#Constants { accent: " + color + "; }", new MutableResource(), "Theme"), color);
        }
    }

    @Test
    void testExponentFontSizeIsRejected() {
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                new CSSThemeCompiler().compile("Label{font-size:1e2px;}", new MutableResource(), "Theme"));
        new CSSThemeCompiler().compile("Label{font-size:1rem;}", new MutableResource(), "Theme");
    }

    @Test
    void testConstantNameMustBeAnIdentifier() {
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                new CSSThemeCompiler().compile("#Constants{1message: hello;}", new MutableResource(), "Theme"));
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{a\\62 c: hello;}", res, "Theme");
        assertEquals("hello", res.getTheme("Theme").get("@abc"));
        res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants{a\\:b: hello;}", res, "Theme");
        assertEquals("hello", res.getTheme("Theme").get("@a:b"));
    }

    @Test
    void testImageAliasOfAColorOrNumberIsNotAnImage() {
        MutableResource res = new MutableResource();
        new CSSThemeCompiler().compile("#Constants { --tone: #f00; --n: 3; placeholderImage: var(--tone);"
                + " countImage: var(--n); }", res, "Theme");
        Hashtable theme = res.getTheme("Theme");
        assertEquals("ff0000", theme.get("@placeholderImage"));
        assertEquals("3", theme.get("@countImage"));
    }
}
