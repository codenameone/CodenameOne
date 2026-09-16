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
package com.codename1.designer.css;

import com.codename1.ui.plaf.RoundRectBorder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;

/**
 * Supported unblurred positive-spread shadows compile to native rounded borders.
 *
 * <p>The resource format has always carried RoundRectBorder's shadow -- shadowBlur,
 * shadowOpacity, shadowSpread, shadowX and shadowY all round-trip through
 * {@code Resources} cases 0xff13 and 0xff15 -- and {@code createRoundRectBorder} has
 * always translated every one of them. That code was unreachable:
 * {@code canBeAchievedWithRoundRectBorder} rejected any {@code box-shadow} outright, so
 * an elevated surface fell through to a CEF-rasterized 9-piece image border and, in a
 * native theme compiled with {@code strictNoCef}, to a hard build failure. Every desktop
 * design language is built on elevation, so the themes were approximating shadows with
 * flat strokes.</p>
 *
 * <p>These tests pin the boundary: what is now drawn natively, and what still legitimately
 * needs rasterization because the format cannot express it.</p>
 */
public class CSSBoxShadowNativeBorderTest {

    @BeforeAll
    static void installHeadlessImplementation() throws Exception {
        HeadlessTestSupport.installHeadlessImplementation();
    }

    /**
     * The case the desktop themes need: a rounded, elevated surface. It must compile to a
     * RoundRectBorder carrying the shadow, and must NOT ask for an image border.
     */
    @Test
    void testSimpleBlackShadowCompilesToRoundRectBorder() throws Exception {
        Hashtable theme = compile("Card { background-color: #ffffff; border-radius: 2.1mm;"
                + " box-shadow: 0 1px 0px 1px rgba(0,0,0,0.13); }");
        Object border = theme.get("Card.border");
        assertInstanceOf(RoundRectBorder.class, border, "Card.border");
        RoundRectBorder rr = (RoundRectBorder) border;
        assertTrue(rr.getShadowOpacity() > 0,
                "shadow opacity should carry the rgba alpha, was " + rr.getShadowOpacity());
        assertTrue(rr.getShadowSpread() > 0f, "explicit spread must survive");
        assertTrue(rr.getShadowBlur() == 0f, "explicit zero blur must survive");
        assertTrue(rr.getCornerRadius() > 0f,
                "corner radius should survive, was " + rr.getCornerRadius());
    }

    /**
     * An elevated surface with square corners is still a RoundRectBorder, just one with
     * cornerRadius 0. Without this the rule falls past the RoundRectBorder branch, past the
     * CSSBorder branch (which rejects shadows), and into rasterization.
     */
    @Test
    void testShadowWithoutRadiusStillCompilesToRoundRectBorder() throws Exception {
        Hashtable theme = compile("Flat { background-color: #ffffff;"
                + " box-shadow: 0 1px 0px 1px rgba(0,0,0,0.2); }");
        assertInstanceOf(RoundRectBorder.class, theme.get("Flat.border"), "Flat.border");
    }

    /**
     * An inset shadow has no RoundRectBorder equivalent -- it only draws an outer drop
     * shadow -- so it must still be refused rather than silently drawn as an outer one.
     */
    @Test
    void testInsetShadowIsStillRefusedInNoCefMode() throws Exception {
        assertRefusedByStrictNoCef("Inset { background-color: #ffffff; border-radius: 2mm;"
                + " box-shadow: inset 0 1px 0px 1px rgba(0,0,0,0.3); }", "inset shadow");
    }

    /**
     * A tinted shadow must still be refused. The format stops at shadowY and never reads a
     * shadow colour, so accepting one would round-trip it to black and drop the hue with
     * nothing downstream able to tell.
     */
    @Test
    void testColouredShadowIsStillRefusedInNoCefMode() throws Exception {
        assertRefusedByStrictNoCef("Tinted { background-color: #ffffff; border-radius: 2mm;"
                + " box-shadow: 0 1px 0px 1px rgba(255,0,0,0.5); }", "coloured shadow");
    }

    @Test
    void testExplicitNonpositiveSpreadRequiresRasterization() throws Exception {
        for (String spread : new String[]{"", "0", "0px", "0mm", "-1px", "0.5px"}) {
            for (String radius : new String[]{"", "border-radius: 2mm;"}) {
                assertRefusedByStrictNoCef("Card { background-color: #ffffff; " + radius
                        + " box-shadow: 0 0 0px " + spread + " rgba(0,0,0,0.2); }",
                        "explicit spread " + spread);
            }
        }
    }

    @Test
    void testExplicitZeroBlurRemainsNativeAcrossSpreads() throws Exception {
        for (String spread : new String[]{"1px", "4px", "8px"}) {
            RoundRectBorder border = (RoundRectBorder) compile("Card { background-color: white;"
                    + " box-shadow: 0 0.5px 0 " + spread + " black; }").get("Card.border");
            assertTrue(border.getShadowBlur() == 0, "native blur must stay zero for " + spread);
            assertTrue(border.getShadowSpread() > 0, "software shadow needs positive spread");
        }
    }

    @Test
    void testNonzeroBlursRequireRasterizationRegardlessOfSpread() throws Exception {
        for (String spread : new String[]{"1px", "4px", "8px"}) {
            for (String blur : new String[]{"0.5px", "4px", "12px", "0.1mm", "0.01cm", "0.5pt"}) {
                for (String radius : new String[]{"", "border-radius: 2mm;"}) {
                    assertRefusedByStrictNoCef("Card { background-color: white; " + radius
                            + " box-shadow: 0 1px " + blur + " " + spread + " black; }",
                            "blur " + blur + " with spread " + spread);
                }
            }
        }
    }

    @Test
    void testUnitlessZeroOffsetsMatchPixelZero() throws Exception {
        RoundRectBorder unitless = (RoundRectBorder) compile("Card { background-color: #ffffff;"
                + " box-shadow: 0 0 0px 1px rgba(0,0,0,0.2); }").get("Card.border");
        RoundRectBorder pixels = (RoundRectBorder) compile("Card { background-color: #ffffff;"
                + " box-shadow: 0px 0px 0px 1px rgba(0,0,0,0.2); }").get("Card.border");
        assertTrue(unitless.getShadowX() == pixels.getShadowX(), "CSS zero x offsets must be equivalent");
        assertTrue(unitless.getShadowY() == pixels.getShadowY(), "CSS zero y offsets must be equivalent");
        RoundRectBorder ratios = (RoundRectBorder) compile("Card { background-color: #ffffff;"
                + " box-shadow: 0px 0px 0px 1px rgba(0,0,0,0.2);"
                + " cn1-box-shadow-h: 0; cn1-box-shadow-v: 0; }").get("Card.border");
        assertTrue(ratios.getShadowX() == 0 && ratios.getShadowY() == 0,
                "explicit CN1 properties retain their ratio semantics");
    }

    @Test
    void testFractionalNativeSpreadsKeepTheirPrecision() throws Exception {
        String[] spreads = {"0.2mm", "0.02cm", "0.6pt", "1.5px"};
        float[] expectedMM = {0.2f, 0.2f, 0.6f * 25.4f / 72f,
                1.5f * 25.4f / 72f};
        for (int i = 0; i < spreads.length; i++) {
            RoundRectBorder border = (RoundRectBorder) compile("Card { background-color: #ffffff;"
                    + " box-shadow: 0 0.1px 0px " + spreads[i] + " rgba(0,0,0,0.2); }").get("Card.border");
            assertTrue(Math.abs(border.getShadowSpread() - expectedMM[i]) < 0.0001f,
                    "fractional spread changed for " + spreads[i] + ": " + border.getShadowSpread());
            assertTrue(!Float.isInfinite(border.getShadowY()) && !Float.isNaN(border.getShadowY()),
                    "offset conversion must be independent of headless Display density");
        }
        RoundRectBorder border = (RoundRectBorder) compile("Card { background-color: #ffffff;"
                + " box-shadow: 0.5px 1.5px 0px 1.5px black; }").get("Card.border");
        assertTrue(Math.abs(border.getShadowX() - (0.5f - 0.5f / 3f)) < 0.0001f,
                "fractional x offset must survive");
        assertTrue(Math.abs(border.getShadowY()) < 0.0001f, "fractional y offset must survive");
        assertTrue(border.getShadowBlur() == 0f, "explicit zero blur must survive");
    }

    @Test
    void testOffsetsOutsideNativeSpreadRequireRasterization() throws Exception {
        for (String offset : new String[]{"2px", "-2px", "0.1cm", "-1mm", "2pt"}) {
            for (String axes : new String[]{offset + " 0", "0 " + offset}) {
                assertRefusedByStrictNoCef("Card { background-color: white; border-radius: 2mm;"
                        + " box-shadow: " + axes + " 0px 1px black; }", "offset " + axes);
            }
        }
        for (String ratio : new String[]{"-0.1", "1.1"}) {
            assertRefusedByStrictNoCef("Card { background-color: white; border-radius: 2mm;"
                    + " box-shadow: 0 0 0px 1px black; cn1-box-shadow-h: " + ratio + "; }",
                    "position ratio " + ratio);
        }
    }

    @Test
    void testOffsetsAtNativeSpreadBoundaryRemainSupported() throws Exception {
        for (String axes : new String[]{"1px -1px", "-1px 1px", "0 0", "0.1cm -1mm"}) {
            String spread = axes.contains("cm") ? "1mm" : "1px";
            RoundRectBorder border = (RoundRectBorder) compile("Card { background-color: white;"
                    + " box-shadow: " + axes + " 0px " + spread + " black; }").get("Card.border");
            assertTrue(border.getShadowX() >= 0 && border.getShadowX() <= 1, "x ratio in range");
            assertTrue(border.getShadowY() >= 0 && border.getShadowY() <= 1, "y ratio in range");
        }
    }

    /** Compiles the sheet and returns the resulting theme properties. */
    private static Hashtable compile(String css) throws Exception {
        Path cssFile = Files.createTempFile("cn1-box-shadow", ".css");
        Path resFile = Files.createTempFile("cn1-box-shadow", ".res");
        try {
            Files.write(cssFile, css.getBytes(StandardCharsets.UTF_8));
            CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
            theme.resourceFile = resFile.toFile();
            theme.res = new com.codename1.ui.util.EditableResourcesForCSS(resFile.toFile());
            theme.res.setTheme("Theme", new Hashtable());
            theme.updateResources();
            return theme.res.getTheme("Theme");
        } finally {
            deleteIfExists(cssFile);
            deleteIfExists(resFile);
        }
    }

    /**
     * Asserts the sheet is rejected by the no-cef gate, which is the failure a native-theme
     * build sees. strictNoCef is a static flag, so it is restored in a finally block or it
     * leaks into every later test in the JVM.
     */
    private static void assertRefusedByStrictNoCef(String css, String what) throws Exception {
        Path cssFile = Files.createTempFile("cn1-box-shadow-reject", ".css");
        Path resFile = Files.createTempFile("cn1-box-shadow-reject", ".res");
        boolean previous = CSSTheme.strictNoCef;
        try {
            Files.write(cssFile, css.getBytes(StandardCharsets.UTF_8));
            CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
            theme.resourceFile = resFile.toFile();
            theme.res = new com.codename1.ui.util.EditableResourcesForCSS(resFile.toFile());
            theme.res.setTheme("Theme", new Hashtable());
            CSSTheme.strictNoCef = true;
            try {
                theme.createImageBorders(null);
            } catch (IllegalStateException expected) {
                return;
            }
            throw new AssertionError("Expected " + what + " to be refused in no-cef mode");
        } finally {
            CSSTheme.strictNoCef = previous;
            deleteIfExists(cssFile);
            deleteIfExists(resFile);
        }
    }

    private static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private static void assertInstanceOf(Class<?> expected, Object actual, String message) {
        if (!expected.isInstance(actual)) {
            throw new AssertionError(message + " expected a " + expected.getSimpleName()
                    + " but was " + (actual == null ? "null" : actual.getClass().getName()));
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
