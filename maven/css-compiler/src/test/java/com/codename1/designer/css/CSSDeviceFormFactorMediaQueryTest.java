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
 * Regression tests for device form-factor media queries compiling into
 * {@code device-<type>-} prefixed UIIDs. The {@code tv} and {@code watch}
 * form factors reuse the same generic {@code device-*} mechanism that already
 * backs {@code device-desktop}/{@code device-tablet}/{@code device-phone};
 * at runtime Resources.loadTheme selects the matching prefix via
 * Display.isTV()/isWatch().
 */
public class CSSDeviceFormFactorMediaQueryTest {

    @BeforeAll
    static void installHeadlessImplementation() throws Exception {
        HeadlessTestSupport.installHeadlessImplementation();
    }

    @Test
    void testTvMediaCompilesToDeviceTvUiids() throws Exception {
        Path cssFile = Files.createTempFile("cn1-tv-media", ".css");
        Path resFile = Files.createTempFile("cn1-tv-media", ".res");
        try {
            String css = "Button { color: #111111; }"
                    + "@media device-tv {"
                    + "  Button { color: #00ff00; background-color: #001100; }"
                    + "}";
            Files.write(cssFile, css.getBytes(StandardCharsets.UTF_8));

            CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
            theme.resourceFile = resFile.toFile();
            theme.res = new com.codename1.ui.util.EditableResourcesForCSS(resFile.toFile());
            theme.res.setTheme("Theme", new Hashtable());
            theme.updateResources();

            Hashtable themeProps = theme.res.getTheme("Theme");
            assertEquals("111111", themeProps.get("Button.fgColor"), "Base Button fgColor");
            assertEquals("00FF00", themeProps.get("device-tv-Button.fgColor"), "TV Button fgColor");
            assertEquals("001100", themeProps.get("device-tv-Button.bgColor"), "TV Button bgColor");
        } finally {
            deleteIfExists(cssFile);
            deleteIfExists(resFile);
        }
    }

    @Test
    void testWatchMediaCompilesToDeviceWatchUiids() throws Exception {
        Path cssFile = Files.createTempFile("cn1-watch-media", ".css");
        Path resFile = Files.createTempFile("cn1-watch-media", ".res");
        try {
            String css = "Button { color: #111111; }"
                    + "@media device-watch {"
                    + "  Button { color: #2222ff; }"
                    + "}";
            Files.write(cssFile, css.getBytes(StandardCharsets.UTF_8));

            CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
            theme.resourceFile = resFile.toFile();
            theme.res = new com.codename1.ui.util.EditableResourcesForCSS(resFile.toFile());
            theme.res.setTheme("Theme", new Hashtable());
            theme.updateResources();

            Hashtable themeProps = theme.res.getTheme("Theme");
            assertEquals("111111", themeProps.get("Button.fgColor"), "Base Button fgColor");
            assertEquals("2222FF", themeProps.get("device-watch-Button.fgColor"), "Watch Button fgColor");
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
