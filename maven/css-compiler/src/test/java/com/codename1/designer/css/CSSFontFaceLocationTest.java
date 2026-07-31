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

import com.codename1.ui.EditorTTFFont;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Hashtable;

/**
 * Regression tests for where an {@code @font-face} {@code src:} URL is allowed
 * to point. A relative URL resolves against the directory holding the CSS file,
 * so a font sitting directly beside {@code theme.css} is just as valid as one in
 * a subdirectory. The documentation used to imply a {@code fonts/} subdirectory
 * was required, and these tests pin the looser contract down.
 *
 * <p>They also cover the quoted multi-word family name. An unquoted
 * {@code font-family: TestFont Bold} parses as two idents and only the first is
 * read back, so quoting is what keeps two weights apart.</p>
 */
public class CSSFontFaceLocationTest {

    /**
     * The icon font already carried by the CSSFontFaceTest sample, reused here
     * so the module doesn't need a second font of its own.
     */
    private static final String FIXTURE = "TestFont.ttf";

    @BeforeAll
    static void installHeadlessImplementation() throws Exception {
        HeadlessTestSupport.installHeadlessImplementation();
    }

    /**
     * A font file dropped straight into the CSS directory, with no
     * subdirectory, has to resolve and ship.
     */
    @Test
    void testFontBesideThemeCssResolves() throws Exception {
        Path cssDir = Files.createTempDirectory("cn1-font-root");
        Path outDir = Files.createTempDirectory("cn1-font-root-out");
        try {
            copyFixture(cssDir.resolve("TestFont-Regular.ttf"));
            Path cssFile = cssDir.resolve("theme.css");
            Files.write(cssFile, ("@font-face {"
                    + "  font-family: \"TestFont\";"
                    + "  src: url(TestFont-Regular.ttf);"
                    + "}"
                    + "Label { font-family: \"TestFont\"; font-size: 3mm; }")
                    .getBytes(StandardCharsets.UTF_8));

            Hashtable themeProps = compile(cssFile, outDir.resolve("theme.res"));

            EditorTTFFont font = fontFor(themeProps, "Label.font");
            assertNotNull(font.getFontFile(), "Label.font resolved to a font file");
            assertEquals("TestFont-Regular.ttf", font.getFontFile().getName(), "Resolved font file");
            assertTrue(outDir.resolve("TestFont-Regular.ttf").toFile().exists(),
                    "Font deployed next to theme.res");
        } finally {
            deleteTree(cssDir);
            deleteTree(outDir);
        }
    }

    /**
     * The subdirectory form keeps working, and the deployed copy is flattened
     * to the bare file name because the runtime forbids a path separator in a
     * true type font name.
     */
    @Test
    void testFontInSubdirectoryResolvesAndDeploysFlat() throws Exception {
        Path cssDir = Files.createTempDirectory("cn1-font-sub");
        Path outDir = Files.createTempDirectory("cn1-font-sub-out");
        try {
            Path fontsDir = cssDir.resolve("fonts");
            Files.createDirectories(fontsDir);
            copyFixture(fontsDir.resolve("TestFont-Regular.ttf"));
            Path cssFile = cssDir.resolve("theme.css");
            Files.write(cssFile, ("@font-face {"
                    + "  font-family: \"TestFont\";"
                    + "  src: url(fonts/TestFont-Regular.ttf);"
                    + "}"
                    + "Label { font-family: \"TestFont\"; font-size: 3mm; }")
                    .getBytes(StandardCharsets.UTF_8));

            Hashtable themeProps = compile(cssFile, outDir.resolve("theme.res"));

            EditorTTFFont font = fontFor(themeProps, "Label.font");
            assertNotNull(font.getFontFile(), "Label.font resolved to a font file");
            assertEquals("TestFont-Regular.ttf", font.getFontFile().getName(), "Resolved font file");
            assertTrue(outDir.resolve("TestFont-Regular.ttf").toFile().exists(),
                    "Font deployed flat next to theme.res");
        } finally {
            deleteTree(cssDir);
            deleteTree(outDir);
        }
    }

    /**
     * Two weights, two quoted family names, two distinct files. This is the
     * shape the guide tells people to use, since the {@code font-weight}
     * descriptor on {@code @font-face} is not consulted when a family is
     * matched.
     */
    @Test
    void testQuotedMultiWordFamilyKeepsWeightsApart() throws Exception {
        Path cssDir = Files.createTempDirectory("cn1-font-weights");
        Path outDir = Files.createTempDirectory("cn1-font-weights-out");
        try {
            copyFixture(cssDir.resolve("TestFont-Regular.ttf"));
            copyFixture(cssDir.resolve("TestFont-Bold.ttf"));
            Path cssFile = cssDir.resolve("theme.css");
            Files.write(cssFile, ("@font-face {"
                    + "  font-family: \"TestFont\";"
                    + "  src: url(TestFont-Regular.ttf);"
                    + "}"
                    + "@font-face {"
                    + "  font-family: \"TestFont Bold\";"
                    + "  src: url(TestFont-Bold.ttf);"
                    + "}"
                    + "Label { font-family: \"TestFont\"; font-size: 3mm; }"
                    + "Title { font-family: \"TestFont Bold\"; font-size: 4mm; }")
                    .getBytes(StandardCharsets.UTF_8));

            Hashtable themeProps = compile(cssFile, outDir.resolve("theme.res"));

            assertEquals("TestFont-Regular.ttf", fontFor(themeProps, "Label.font").getFontFile().getName(),
                    "Regular weight");
            assertEquals("TestFont-Bold.ttf", fontFor(themeProps, "Title.font").getFontFile().getName(),
                    "Bold weight resolved through the quoted family name");
        } finally {
            deleteTree(cssDir);
            deleteTree(outDir);
        }
    }

    private static Hashtable compile(Path cssFile, Path resFile) throws Exception {
        CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
        theme.resourceFile = resFile.toFile();
        theme.res = new com.codename1.ui.util.EditableResourcesForCSS(resFile.toFile());
        theme.res.setTheme("Theme", new Hashtable());
        theme.updateResources();
        return theme.res.getTheme("Theme");
    }

    private static EditorTTFFont fontFor(Hashtable themeProps, String key) {
        Object font = themeProps.get(key);
        assertNotNull(font, "Theme property " + key);
        assertTrue(font instanceof EditorTTFFont, key + " is a true type font, was " + font.getClass());
        return (EditorTTFFont) font;
    }

    /**
     * Writes the shared TTF fixture out under whatever name the test needs. The
     * tests only care about which file a family resolves to, not about what the
     * glyphs look like, so one fixture stands in for every weight.
     */
    private static void copyFixture(Path dest) throws IOException {
        try (InputStream in = CSSFontFaceLocationTest.class.getResourceAsStream(FIXTURE)) {
            assertNotNull(in, "Test fixture " + FIXTURE);
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteTree(Path path) {
        File file = path.toFile();
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteTree(child.toPath());
            }
        }
        file.delete();
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertNotNull(Object actual, String message) {
        if (actual == null) {
            throw new AssertionError(message + " was null");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
