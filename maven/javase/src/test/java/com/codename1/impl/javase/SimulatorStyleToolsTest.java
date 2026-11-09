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
package com.codename1.impl.javase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatorStyleToolsTest {

    private static final String CSS = ":root {\n  --brand: #3366ff;\n}\n\n"
            + "/* Button { color: red; } is a comment */\n"
            + "Button {\n  color: #000;\n  padding: 1mm 2mm\n}\n\n"
            + "@media (min-width: 600px) {\n  Label { color: blue; }\n}\n"
            + "Title, Label { font-size: 3mm; }\n";

    private static String set(String selector, String property, String value) {
        return SimulatorStyleTools.CssRule.set(CSS, selector, property, value);
    }

    @Test
    void replacesAValueInPlace() {
        assertEquals(CSS.replace("color: #000;", "color: var(--brand);"), set("Button", "color", "var(--brand)"));
    }

    @Test
    void addsAfterALastDeclarationWithoutASemicolon() {
        assertEquals(CSS.replace("padding: 1mm 2mm\n", "padding: 1mm 2mm;\n  margin: 0;\n"),
                set("Button", "margin", "0"));
    }

    @Test
    void removesTheDeclarationAndItsLine() {
        assertEquals(CSS.replace("  color: #000;\n", ""), set("Button", "color", ""));
    }

    @Test
    void ignoresCommentsAndMediaRulesAndNormalizesSelectorLists() {
        SimulatorStyleTools.CssRule r = SimulatorStyleTools.CssRule.find(CSS, "Title,Label");
        assertEquals(14, r.line(CSS));
        assertEquals("[font-size: 3mm]", r.declarations(CSS).toString());
        assertNull(SimulatorStyleTools.CssRule.find(CSS, "Label"));
        assertEquals(6, SimulatorStyleTools.CssRule.find(CSS, "Button").line(CSS));
    }

    @Test
    void appendsAMissingRule() {
        assertEquals(CSS + "\nCard {\n    margin: 1mm;\n}\n", set("Card", "margin", "1mm"));
        assertEquals(CSS, set("Card", "margin", ""));
    }

    @Test
    void refusesValuesThatEscapeTheDeclaration() {
        assertFalse(SimulatorStyleTools.isPlainToken("red; } Evil { color: blue", "{};"));
        assertFalse(SimulatorStyleTools.isPlainToken("red /* x", "{};"));
        assertTrue(SimulatorStyleTools.isPlainToken("rgb(1, 2, 3)", "{};"));
    }

    @Test
    void diffShowsOnlyTheChangedHunk() {
        String diff = SimulatorStyleTools.unifiedDiff(CSS, set("Button", "color", "red"), "theme.css");
        assertEquals("--- a/theme.css\n+++ b/theme.css\n@@ -4,7 +4,7 @@\n \n /* Button { color: red; } is a comment */\n"
                + " Button {\n-  color: #000;\n+  color: red;\n   padding: 1mm 2mm\n }\n \n", diff);
        assertEquals("", SimulatorStyleTools.unifiedDiff(CSS, CSS, "theme.css"));
    }

    @Test
    void aCommentBeforeTheDeclarationDoesNotHideIt() {
        String css = "Button {\n    /* primary */\n    color: red;\n}\n";
        assertEquals("Button {\n    /* primary */\n    color: blue;\n}\n",
                SimulatorStyleTools.CssRule.set(css, "Button", "color", "blue"));
        assertEquals("Button {\n    /* primary */\n}\n", SimulatorStyleTools.CssRule.set(css, "Button", "color", ""));
        assertEquals("Button { /* x */  }", SimulatorStyleTools.CssRule.set("Button { /* x */ color: red; }",
                "Button", "color", ""));
    }

    @Test
    void aBraceInsideAStringClosesNothing() {
        String css = "#Constants { title: \"a}b\"; }\nButton { color: red; }\n";
        assertEquals(css.replace("red", "blue"), SimulatorStyleTools.CssRule.set(css, "Button", "color", "blue"));
    }

    @Test
    void crlfFilesKeepCrlf() {
        String css = "Button {\r\n    color: red;\r\n}\r\n";
        assertEquals("Button {\r\n    color: red;\r\n    margin: 0;\r\n}\r\n",
                SimulatorStyleTools.CssRule.set(css, "Button", "margin", "0"));
        assertEquals("Button {\r\n}\r\n", SimulatorStyleTools.CssRule.set(css, "Button", "color", ""));
        assertEquals(css + "\r\nCard {\r\n    margin: 0;\r\n}\r\n",
                SimulatorStyleTools.CssRule.set(css, "Card", "margin", "0"));
    }
}
