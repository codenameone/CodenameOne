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
 * Regression tests for dark-mode media query compilation into $Dark UIIDs.
 */
public class CSSDarkModeMediaQueryTest {

    @BeforeAll
    static void installHeadlessImplementation() throws Exception {
        HeadlessTestSupport.installHeadlessImplementation();
    }

    @Test
    void testDarkMediaCompilesToDarkUiids() throws Exception {
        Path cssFile = Files.createTempFile("cn1-dark-media", ".css");
        Path resFile = Files.createTempFile("cn1-dark-media", ".res");
        try {
            String css = "Button { color: #111111; }"
                    + "@media (prefers-color-scheme: dark) {"
                    + "  Button { color: #eeeeee; background-color: #000000; }"
                    + "  Button.selected { color: #ff0000; }"
                    + "}";
            Files.write(cssFile, css.getBytes(StandardCharsets.UTF_8));

            CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
            theme.resourceFile = resFile.toFile();
            theme.res = new com.codename1.ui.util.EditableResourcesForCSS(resFile.toFile());
            theme.res.setTheme("Theme", new Hashtable());
            theme.updateResources();

            Hashtable themeProps = theme.res.getTheme("Theme");
            assertEquals("111111", themeProps.get("Button.fgColor"), "Base style fgColor");
            assertEquals("EEEEEE", themeProps.get("$DarkButton.fgColor"), "Dark style fgColor");
            assertEquals("000000", themeProps.get("$DarkButton.bgColor"), "Dark style bgColor");
            assertEquals("255", themeProps.get("$DarkButton.transparency"), "Dark style transparency");
            assertEquals("FF0000", themeProps.get("$DarkButton.sel#fgColor"), "Dark selected fgColor");
        } finally {
            deleteIfExists(cssFile);
            deleteIfExists(resFile);
        }
    }

    /**
     * Regression: the dark-mode rewriter must not trigger on the literal
     * "@media (prefers-color-scheme:" string sitting inside a header
     * comment. Before the fix it swallowed everything up to the next {,
     * treated the subsequent block's properties as dark selectors, and
     * ran the tokenizer off EOF later on.
     */
    @Test
    void testAtMediaInsideHeaderCommentIsIgnored() throws Exception {
        Path cssFile = Files.createTempFile("cn1-dark-comment", ".css");
        Path resFile = Files.createTempFile("cn1-dark-comment", ".res");
        try {
            String css = "/* header doc mentions @media (prefers-color-scheme: dark) for reference */\n"
                    + "#Constants { tabsGridBool: true; }\n"
                    + "Button { color: #111111; }\n"
                    + "@media (prefers-color-scheme: dark) {\n"
                    + "  Button { color: #eeeeee; }\n"
                    + "}\n";
            Files.write(cssFile, css.getBytes(StandardCharsets.UTF_8));

            CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
            theme.resourceFile = resFile.toFile();
            theme.res = new com.codename1.ui.util.EditableResourcesForCSS(resFile.toFile());
            theme.res.setTheme("Theme", new Hashtable());
            theme.updateResources();

            Hashtable themeProps = theme.res.getTheme("Theme");
            assertEquals("111111", themeProps.get("Button.fgColor"), "Light Button fgColor survives comment");
            assertEquals("EEEEEE", themeProps.get("$DarkButton.fgColor"), "Real dark block still compiles");
            assertEquals("true", themeProps.get("@tabsGridBool"), "#Constants block isn't mangled");
        } finally {
            deleteIfExists(cssFile);
            deleteIfExists(resFile);
        }
    }

    /**
     * A later literal colour must cancel an earlier var() binding for the same
     * property. Theme generation files are concatenated after the shared sheet
     * and override its rules; without this the runtime applyThemeBindings pass
     * re-applied the shared sheet's `var(--accent-color-dark)` over the override.
     */
    @Test
    void testLaterLiteralColourDropsVarBinding() throws Exception {
        Path cssFile = Files.createTempFile("cn1-binding-override", ".css");
        Path resFile = Files.createTempFile("cn1-binding-override", ".res");
        try {
            String css = "#Constants { --accent-color: #007aff; --accent-color-dark: #0a84ff; }"
                    + "Tab.pressed { color: var(--accent-color, #007aff); }"
                    + "Tab.selected { color: var(--accent-color, #007aff); }"
                    + "@media (prefers-color-scheme: dark) {"
                    + "  Tab.pressed { color: var(--accent-color-dark, #0a84ff); }"
                    + "}"
                    + "Tab.pressed { color: #0065d1; }"
                    + "@media (prefers-color-scheme: dark) {"
                    + "  Tab.pressed { color: #5eefff; }"
                    + "}";
            Files.write(cssFile, css.getBytes(StandardCharsets.UTF_8));

            CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
            theme.resourceFile = resFile.toFile();
            theme.res = new com.codename1.ui.util.EditableResourcesForCSS(resFile.toFile());
            theme.res.setTheme("Theme", new Hashtable());
            theme.updateResources();

            Hashtable themeProps = theme.res.getTheme("Theme");
            assertEquals("0065D1", themeProps.get("Tab.press#fgColor"), "overridden light colour");
            assertEquals("5EEFFF", themeProps.get("$DarkTab.press#fgColor"), "overridden dark colour");
            assertNull(themeProps.get("@cn1-bind:Tab.press#fgColor"), "light binding survived the override");
            assertNull(themeProps.get("@cn1-bind:$DarkTab.press#fgColor"), "dark binding survived the override");
            // An untouched var() colour keeps its binding.
            assertNotNull(themeProps.get("@cn1-bind:Tab.sel#fgColor"), "unrelated binding lost");
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

    private static void assertNull(Object actual, String message) {
        if (actual != null) {
            throw new AssertionError(message + " actual=" + actual);
        }
    }

    private static void assertNotNull(Object actual, String message) {
        if (actual == null) {
            throw new AssertionError(message);
        }
    }
}
