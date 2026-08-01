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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Hashtable;

/**
 * The compile has to reject an {@code @font-face} the runtime can't honor,
 * because the two disagree about what a true type font is: the compiler parses
 * fonts with {@code java.awt.Font.createFont(TRUETYPE_FONT, ...)}, which also
 * accepts OpenType, while {@code Font.createTrueTypeFont} rejects any name not
 * ending in {@code .ttf} and the iOS build registers only {@code .ttf} files.
 * An OpenType font used to compile clean and fail on the device.
 *
 * @see CSSFontFaceLocationTest for the placements that must keep working
 */
public class CSSFontFaceValidationTest {

    private static final String FIXTURE = "TestFont.ttf";

    @BeforeAll
    static void installHeadlessImplementation() throws Exception {
        HeadlessTestSupport.installHeadlessImplementation();
    }

    @Test
    void testOpenTypeFontIsRejected() throws Exception {
        String message = assertCompileFails(
                "@font-face { font-family: \"TestFont\"; src: url(TestFont-Regular.otf); }"
                        + "Label { font-family: \"TestFont\"; }",
                "TestFont-Regular.otf");
        assertContains(message, "OpenType fonts aren't supported");
        assertContains(message, "TestFont-Regular.otf");
    }

    /**
     * Declared but never referenced still fails. A rule is only resolved when
     * some style uses the family, so without this the typo would surface on
     * whichever later build first referenced it.
     */
    @Test
    void testUnreferencedOpenTypeFontIsRejected() throws Exception {
        String message = assertCompileFails(
                "@font-face { font-family: \"TestFont\"; src: url(TestFont-Regular.otf); }"
                        + "Label { color: #ff0000; }",
                "TestFont-Regular.otf");
        assertContains(message, "OpenType fonts aren't supported");
    }

    /**
     * The runtime's {@code endsWith(".ttf")} is case-sensitive, so an upper-case
     * extension fails on device even though the file really is TrueType.
     */
    @Test
    void testUpperCaseExtensionIsRejected() throws Exception {
        String message = assertCompileFails(
                "@font-face { font-family: \"TestFont\"; src: url(TestFont-Regular.TTF); }"
                        + "Label { font-family: \"TestFont\"; }",
                "TestFont-Regular.TTF");
        assertContains(message, "case-sensitive");
    }

    @Test
    void testMissingFontFileIsRejected() throws Exception {
        String message = assertCompileFails(
                "@font-face { font-family: \"TestFont\"; src: url(NotThere.ttf); }"
                        + "Label { font-family: \"TestFont\"; }",
                null);
        assertContains(message, "doesn't exist");
    }

    /**
     * Merge mode syncs only the CSS directory, so a font reached through {@code ../}
     * resolves for the author and breaks in a real build.
     */
    @Test
    void testFontOutsideTheCssDirectoryIsRejected() throws Exception {
        Path root = Files.createTempDirectory("cn1-font-outside");
        Path outDir = Files.createTempDirectory("cn1-font-outside-out");
        try {
            Path cssDir = root.resolve("css");
            Files.createDirectories(cssDir);
            copyFixture(root.resolve("Stray.ttf"));
            Path cssFile = cssDir.resolve("theme.css");
            Files.write(cssFile, ("@font-face { font-family: \"TestFont\"; src: url(../Stray.ttf); }"
                    + "Label { font-family: \"TestFont\"; }").getBytes(StandardCharsets.UTF_8));

            String message = compileExpectingFailure(cssFile, outDir.resolve("theme.res"));
            assertContains(message, "outside the directory holding the CSS file");
        } finally {
            deleteTree(root);
            deleteTree(outDir);
        }
    }

    /**
     * Fonts are deployed by file name alone, so two rules naming identically
     * named files in different directories would silently clobber each other.
     */
    @Test
    void testDuplicateFontFileNamesAreRejected() throws Exception {
        Path cssDir = Files.createTempDirectory("cn1-font-dupe");
        Path outDir = Files.createTempDirectory("cn1-font-dupe-out");
        try {
            Path nested = cssDir.resolve("bold");
            Files.createDirectories(nested);
            copyFixture(cssDir.resolve("TestFont.ttf"));
            copyFixture(nested.resolve("TestFont.ttf"));
            Path cssFile = cssDir.resolve("theme.css");
            Files.write(cssFile, ("@font-face { font-family: \"TestFont\"; src: url(TestFont.ttf); }"
                    + "@font-face { font-family: \"TestFont Bold\"; src: url(bold/TestFont.ttf); }"
                    + "Label { font-family: \"TestFont\"; }").getBytes(StandardCharsets.UTF_8));

            String message = compileExpectingFailure(cssFile, outDir.resolve("theme.res"));
            assertContains(message, "resolve to different files that are both named TestFont.ttf");
        } finally {
            deleteTree(cssDir);
            deleteTree(outDir);
        }
    }

    /**
     * Two families pointing at the SAME file is a legitimate alias, not a
     * collision -- the deploy copy is idempotent, so nothing is overwritten.
     * Only genuinely different files sharing a name are an error.
     */
    @Test
    void testTwoFamiliesMaySharedOneFontFile() throws Exception {
        Path cssDir = Files.createTempDirectory("cn1-font-alias");
        Path outDir = Files.createTempDirectory("cn1-font-alias-out");
        try {
            copyFixture(cssDir.resolve("TestFont.ttf"));
            Path cssFile = cssDir.resolve("theme.css");
            Files.write(cssFile, ("@font-face { font-family: \"Body\"; src: url(TestFont.ttf); }"
                    + "@font-face { font-family: \"Caption\"; src: url(TestFont.ttf); }"
                    + "Label { font-family: \"Body\"; font-size: 3mm; }"
                    + "SmallLabel { font-family: \"Caption\"; font-size: 2mm; }")
                    .getBytes(StandardCharsets.UTF_8));

            CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
            theme.resourceFile = outDir.resolve("theme.res").toFile();
            theme.res = new com.codename1.ui.util.EditableResourcesForCSS(theme.resourceFile);
            theme.res.setTheme("Theme", new Hashtable());
            theme.updateResources();
        } finally {
            deleteTree(cssDir);
            deleteTree(outDir);
        }
    }

    /**
     * A remote font is judged by its URL alone, so a bad one is caught without
     * the compile reaching out to the network.
     */
    @Test
    void testRemoteOpenTypeFontIsRejectedWithoutDownloading() throws Exception {
        String message = assertCompileFails(
                "@font-face { font-family: \"TestFont\"; "
                        + "src: url(https://example.invalid/fonts/TestFont.otf); }"
                        + "Label { font-family: \"TestFont\"; }",
                null);
        assertContains(message, "OpenType fonts aren't supported");
    }

    /** A well-formed sheet must still compile, or the check is worthless. */
    @Test
    void testValidTrueTypeFontStillCompiles() throws Exception {
        Path cssDir = Files.createTempDirectory("cn1-font-ok");
        Path outDir = Files.createTempDirectory("cn1-font-ok-out");
        try {
            copyFixture(cssDir.resolve("TestFont-Regular.ttf"));
            Path cssFile = cssDir.resolve("theme.css");
            Files.write(cssFile, ("@font-face { font-family: \"TestFont\"; src: url(TestFont-Regular.ttf); }"
                    + "Label { font-family: \"TestFont\"; font-size: 3mm; }")
                    .getBytes(StandardCharsets.UTF_8));

            CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
            theme.resourceFile = outDir.resolve("theme.res").toFile();
            theme.res = new com.codename1.ui.util.EditableResourcesForCSS(theme.resourceFile);
            theme.res.setTheme("Theme", new Hashtable());
            theme.updateResources();
        } finally {
            deleteTree(cssDir);
            deleteTree(outDir);
        }
    }

    /**
     * Writes the sheet into a scratch CSS directory and returns the failure
     * message. {@code fontFile}, when given, is created so the test proves the
     * rule was rejected on its name rather than for being absent.
     */
    private static String assertCompileFails(String css, String fontFile) throws Exception {
        Path cssDir = Files.createTempDirectory("cn1-font-invalid");
        Path outDir = Files.createTempDirectory("cn1-font-invalid-out");
        try {
            if (fontFile != null) {
                copyFixture(cssDir.resolve(fontFile));
            }
            Path cssFile = cssDir.resolve("theme.css");
            Files.write(cssFile, css.getBytes(StandardCharsets.UTF_8));
            return compileExpectingFailure(cssFile, outDir.resolve("theme.res"));
        } finally {
            deleteTree(cssDir);
            deleteTree(outDir);
        }
    }

    private static String compileExpectingFailure(Path cssFile, Path resFile) throws Exception {
        CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
        theme.resourceFile = resFile.toFile();
        theme.res = new com.codename1.ui.util.EditableResourcesForCSS(resFile.toFile());
        theme.res.setTheme("Theme", new Hashtable());
        try {
            theme.updateResources();
        } catch (IllegalArgumentException expected) {
            return expected.getMessage();
        }
        throw new AssertionError("Expected the compile to fail for " + cssFile);
    }

    private static void copyFixture(Path dest) throws IOException {
        try (InputStream in = CSSFontFaceValidationTest.class.getResourceAsStream(FIXTURE)) {
            if (in == null) {
                throw new AssertionError("Test fixture " + FIXTURE + " was null");
            }
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

    private static void assertContains(String actual, String expected) {
        if (actual == null || actual.indexOf(expected) < 0) {
            throw new AssertionError("Expected the error to mention \"" + expected + "\" but it was: " + actual);
        }
    }
}
