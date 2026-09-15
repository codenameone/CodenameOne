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
 * A drop shadow is a native primitive, not a rasterized image.
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
                + " box-shadow: 0 2px 4px rgba(0,0,0,0.13); }");
        Object border = theme.get("Card.border");
        assertInstanceOf(RoundRectBorder.class, border, "Card.border");
        RoundRectBorder rr = (RoundRectBorder) border;
        assertTrue(rr.getShadowOpacity() > 0,
                "shadow opacity should carry the rgba alpha, was " + rr.getShadowOpacity());
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
                + " box-shadow: 0 1px 3px rgba(0,0,0,0.2); }");
        assertInstanceOf(RoundRectBorder.class, theme.get("Flat.border"), "Flat.border");
    }

    /**
     * An inset shadow has no RoundRectBorder equivalent -- it only draws an outer drop
     * shadow -- so it must still be refused rather than silently drawn as an outer one.
     */
    @Test
    void testInsetShadowIsStillRefusedInNoCefMode() throws Exception {
        assertRefusedByStrictNoCef("Inset { background-color: #ffffff; border-radius: 2mm;"
                + " box-shadow: inset 0 2px 4px rgba(0,0,0,0.3); }", "inset shadow");
    }

    /**
     * A tinted shadow must still be refused. The format stops at shadowY and never reads a
     * shadow colour, so accepting one would round-trip it to black and drop the hue with
     * nothing downstream able to tell.
     */
    @Test
    void testColouredShadowIsStillRefusedInNoCefMode() throws Exception {
        assertRefusedByStrictNoCef("Tinted { background-color: #ffffff; border-radius: 2mm;"
                + " box-shadow: 0 2px 6px rgba(255,0,0,0.5); }", "coloured shadow");
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
