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

import com.codename1.ui.css.CSSThemeCompiler;
import com.codename1.ui.util.MutableResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Hashtable;

/**
 * The runtime compiler in core ({@link CSSThemeCompiler}, used for live reload and by
 * applications that build a theme on the device) against this one, on the declarations both
 * support: the same sheet has to store the same constants, padding, margin and unit metadata.
 */
public class RuntimeCompilerParityTest {

    private static final String CSS = "#Constants {\n"
            + "    includeNativeBool: true;\n"
            + "    customMessage: \"Hello\";\n"
            + "    quotedFlagBool: 'false';\n"
            + "    accentColor: #ff2d95;\n"
            + "    shortColor: #abc;\n"
            + "    switchTrackScaleY: 1.5;\n"
            + "    pctColor: rgb(100%, 0%, 50%);\n"
            + "    clampColor: rgb(200%, 300, 0);\n"
            + "}\n"
            + "Label { padding: 1px 2px 3px 4px; margin: 1mm 2mm; }\n"
            + "One { padding: 5px; margin: 0.5mm; }\n"
            + "Three { padding: 1mm 2px 3mm; }\n"
            + "Mixed { padding: 1mm 2px 3pt 4mm; margin: 1px 2mm 3px 4mm; }\n"
            + "Mixed.pressed { padding: 2mm 1mm; }\n"
            + "Mixed.selected { margin: 3pt; }\n";

    @BeforeAll
    static void installHeadlessImplementation() throws Exception {
        HeadlessTestSupport.installHeadlessImplementation();
    }

    @Test
    void constantsMatch() throws Exception {
        Hashtable standard = compileStandard(CSS);
        Hashtable runtime = compileRuntime(CSS);
        for (String key : new String[]{"@includeNativeBool", "@customMessage", "@quotedFlagBool"}) {
            assertEquals(standard.get(key), runtime.get(key), key);
        }
        // Hex case differs between the compilers for every color, not just constants (the
        // build-time one writes upper case); both are read with Integer.parseInt(value, 16).
        for (String key : new String[]{"@accentColor", "@shortColor", "@pctColor", "@clampColor"}) {
            assertEquals(Integer.valueOf(Integer.parseInt((String) standard.get(key), 16)),
                    Integer.valueOf(Integer.parseInt((String) runtime.get(key), 16)), key);
        }
        assertEquals("true", runtime.get("@includeNativeBool"), "@includeNativeBool");
        assertEquals("Hello", runtime.get("@customMessage"), "@customMessage");
    }

    @Test
    void spacingMatches() throws Exception {
        Hashtable standard = compileStandard(CSS);
        Hashtable runtime = compileRuntime(CSS);
        String[] keys = {
                "Label.padding", "Label.margin", "Label.padUnit", "Label.marUnit",
                "One.padding", "One.margin", "One.padUnit", "One.marUnit",
                "Three.padding", "Three.padUnit",
                "Mixed.padding", "Mixed.margin", "Mixed.padUnit", "Mixed.marUnit",
                "Mixed.press#padding", "Mixed.press#padUnit",
                "Mixed.sel#margin", "Mixed.sel#marUnit"};
        for (String key : keys) {
            Object expected = standard.get(key);
            Object actual = runtime.get(key);
            if (expected == null || actual == null) {
                throw new AssertionError(key + " standard=" + expected + " runtime=" + actual);
            }
            if (expected instanceof byte[]) {
                if (!Arrays.equals((byte[]) expected, (byte[]) actual)) {
                    throw new AssertionError(key + " units standard=" + Arrays.toString((byte[]) expected)
                            + " runtime=" + Arrays.toString((byte[]) actual));
                }
            } else if (!Arrays.equals(floats((String) expected), floats((String) actual))) {
                throw new AssertionError(key + " standard=" + expected + " runtime=" + actual);
            }
        }
    }

    private static float[] floats(String box) {
        String[] parts = box.split(",");
        float[] out = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Float.parseFloat(parts[i].trim());
        }
        return out;
    }

    private static Hashtable compileRuntime(String css) {
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile(css, resource, "Theme");
        return resource.getTheme("Theme");
    }

    private static Hashtable compileStandard(String css) throws Exception {
        Path cssFile = Files.createTempFile("cn1-parity", ".css");
        Path resFile = Files.createTempFile("cn1-parity", ".res");
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
