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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class CssDiagnosticsTest {
    @Test
    void anUnsupportedPropertyNamesTheOneItMisspells() {
        assertEquals("Unsupported CSS property text-algn [CN1-CSS-PROPERTY]. Did you mean text-align?",
                CssDiagnostics.unsupportedProperty("text-algn"));
        assertEquals("Unsupported CSS property backgrund-color [CN1-CSS-PROPERTY]. Did you mean background-color?",
                CssDiagnostics.unsupportedProperty("backgrund-color"));
        String far = CssDiagnostics.unsupportedProperty("grid-template-areas");
        assertTrue(far.startsWith("Unsupported CSS property grid-template-areas [CN1-CSS-PROPERTY]"), far);
        assertFalse(far.contains("Did you mean"), far);
    }

    @Test
    void anInvalidColorNamesTheColorItMisspells() {
        try {
            Color.web("blu", 1.0);
            fail("blu is not a color");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().startsWith(
                    "Invalid color specification: Color string was blu [CN1-CSS-COLOR]. Did you mean blue?"),
                    expected.getMessage());
        }
        String noGuess = CssDiagnostics.invalidColor("#12", Arrays.asList("red", "blue"));
        assertFalse(noGuess.contains("Did you mean"), noGuess);
        assertTrue(noGuess.endsWith("rgba(r, g, b, a)."), noGuess);
    }

    /// The suggestion list is a copy of the property switch in CSSTheme.apply; every
    /// entry must be a label there, so a renamed property cannot be suggested.
    @Test
    void everySuggestedPropertyIsOneTheCompilerHandles() throws Exception {
        File src = new File("src/main/java/com/codename1/designer/css/CSSTheme.java");
        String text = new String(Files.readAllBytes(src.toPath()), StandardCharsets.UTF_8);
        int start = text.indexOf("public void apply(Element style, String property, LexicalUnit value)");
        assertTrue(start > 0, "CSSTheme.apply moved");
        Matcher m = Pattern.compile("case \"([a-zA-Z0-9-]+)\"").matcher(text.substring(start));
        java.util.Set<String> labels = new java.util.HashSet<String>();
        while (m.find()) {
            labels.add(m.group(1));
        }
        for (String p : CssDiagnostics.PROPERTIES) {
            assertTrue(labels.contains(p), p + " is suggested but CSSTheme.apply has no case for it");
        }
    }
}
