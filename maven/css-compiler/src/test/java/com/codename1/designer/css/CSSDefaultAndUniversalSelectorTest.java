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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;

/**
 * The two selectors that look like they style "everything": {@code Default}, which does,
 * and the universal selector {@code *}, which never has.
 *
 * <p>{@code Default} compiles to the theme's unqualified keys -- {@code transparency},
 * {@code bgColor} -- which are what a UIID no theme declares starts from. The desktop
 * native themes depend on that to keep an application's own UIIDs see-through, so the
 * shape of what it emits is pinned here rather than only in the themes that use it.</p>
 *
 * <p>{@code *} used to compile into a UIID literally named "null", because the parser
 * reports it as an element selector with a null name. Nothing reads those keys, so the
 * rule did nothing and said nothing. It is now dropped with a warning that names
 * {@code Default}.</p>
 */
public class CSSDefaultAndUniversalSelectorTest {

    @BeforeAll
    static void installHeadlessImplementation() throws Exception {
        HeadlessTestSupport.installHeadlessImplementation();
    }

    /** What a theme has to carry for an undeclared UIID to paint no background. */
    @Test
    void testDefaultSelectorEmitsTheUnqualifiedTransparency() throws Exception {
        Hashtable theme = compile("Default { background-color: rgba(255, 255, 255, 0); }"
                + "Label { color: #111111; }");
        assertEquals("0", theme.get("transparency"), "default transparency");
        assertEquals("ffffff", lower(theme.get("bgColor")), "default colour is kept white, only the alpha moves");
        // Every state, or a focused, pressed or disabled control of an undeclared UIID
        // would turn opaque again.
        assertEquals("0", theme.get("sel#transparency"), "selected default transparency");
        assertEquals("0", theme.get("press#transparency"), "pressed default transparency");
        assertEquals("0", theme.get("dis#transparency"), "disabled default transparency");
        // Default is a default, not a parent: it must not be copied into the UIIDs the
        // sheet declares, or it would override what they inherit from a native theme.
        assertEquals(null, theme.get("Label.transparency"), "a declared UIID is left alone");
    }

    /** {@code transparent} moves the default colour to black as well as the alpha. */
    @Test
    void testTransparentKeywordAlsoSetsABlackDefaultColour() throws Exception {
        Hashtable theme = compile("Default { background-color: transparent; }");
        assertEquals("0", theme.get("transparency"), "default transparency");
        assertEquals("000000", theme.get("bgColor"), "transparent compiles to a black default colour");
    }

    @Test
    void testUniversalSelectorIsDroppedRatherThanCompiledIntoAUiidNamedNull() throws Exception {
        Hashtable theme = compile("* { background: transparent; margin: 0; }"
                + "*.pressed { color: #ff0000; }"
                + "Label { color: #111111; }"
                + "@media (prefers-color-scheme: dark) { * { color: #ffffff; } }");
        for (Object key : theme.keySet()) {
            String k = (String) key;
            if (k.indexOf("null") > -1) {
                throw new AssertionError("the universal selector was compiled into " + k);
            }
        }
        assertEquals("111111", theme.get("Label.fgColor"), "the rules beside it still compile");
        // Not folded into the declared UIIDs and not promoted to the default either: a
        // sheet with a `* { margin: 0 }` reset must keep compiling to what it always did.
        assertEquals(null, theme.get("Label.transparency"), "not folded into a declared UIID");
        assertEquals(null, theme.get("Label.margin"), "not folded into a declared UIID");
        assertEquals(null, theme.get("transparency"), "not promoted to the default style");
        assertEquals(null, theme.get("margin"), "not promoted to the default style");
    }

    private static String lower(Object value) {
        if (value == null) {
            return null;
        }
        // Hex digits only, so a character fold is exact; String.toLowerCase is locale
        // sensitive and has no place on a token.
        StringBuilder sb = new StringBuilder();
        String s = value.toString();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return sb.toString();
    }

    private static Hashtable compile(String css) throws Exception {
        Path cssFile = Files.createTempFile("cn1-default", ".css");
        Path resFile = Files.createTempFile("cn1-default", ".res");
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

    private static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
