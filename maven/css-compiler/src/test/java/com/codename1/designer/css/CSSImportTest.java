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
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// `@import` used to be parsed and then ignored: the imported rules were
/// missing from the theme with no error. These tests pin that it is honoured,
/// and that what cannot be honoured is refused with its reason.
class CSSImportTest {

    @BeforeAll
    static void installHeadlessImplementation() throws Exception {
        HeadlessTestSupport.installHeadlessImplementation();
    }

    private static File write(File f, String content) throws IOException {
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    private static Hashtable compile(File cssFile) throws Exception {
        File resFile = new File(cssFile.getParentFile(), "theme.res");
        CSSTheme theme = CSSTheme.load(cssFile.toURI().toURL());
        assertNotNull(theme);
        theme.cssFile = cssFile;
        theme.resourceFile = resFile;
        theme.res = new com.codename1.ui.util.EditableResourcesForCSS(resFile);
        theme.res.setTheme("Theme", new Hashtable());
        theme.createImageBorders();
        theme.updateResources();
        return theme.res.getTheme("Theme");
    }

    @Test
    void importedRulesAreInTheThemeAheadOfTheImportingFilesOwn(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        write(new File(dir, "base.css"), "Title { color: #111111; } Body { color: #222222; }\n");
        File root = write(new File(dir, "theme.css"), "@import \"base.css\";\nTitle { color: #333333; }\n");

        Hashtable theme = compile(root);
        assertEquals("222222", theme.get("Body.fgColor"), "a rule only the imported file has");
        assertEquals("333333", theme.get("Title.fgColor"), "the importing file's rule comes later and wins");
    }

    @Test
    void importsNestAndBothSpellingsWork(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        write(new File(dir, "parts/deep/leaf.css"), "Leaf { color: #0a0b0c; }\n");
        write(new File(dir, "parts/mid.css"), "@import url(\"deep/leaf.css\");\nMid { color: #010203; }\n");
        File root = write(new File(dir, "theme.css"), "@import 'parts/mid.css';\nRoot { color: #040506; }\n");

        Set<File> imported = CssImports.collect(root);
        assertEquals(2, imported.size(), imported.toString());

        Hashtable theme = compile(root);
        assertEquals("0A0B0C", theme.get("Leaf.fgColor"), "a file imported by an imported file");
        assertEquals("010203", theme.get("Mid.fgColor"));
        assertEquals("040506", theme.get("Root.fgColor"));
    }

    @Test
    void aUrlInAnImportedFileStaysRelativeToThatFile(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        File part = new File(dir, "parts/part.css");
        String rebased = CssImports.rebaseUrls(
                "A { background-image: url(img/a.png); } B { background-image: url('../b.png'); }"
                + " C { background-image: url(\"https://example.com/c.png\"); } D { background-image: url(/abs/d.png); }",
                part.getParentFile(), dir);
        assertTrue(rebased.contains("url(\"parts/img/a.png\")"), rebased);
        assertTrue(rebased.contains("url(\"b.png\")"), rebased);
        assertTrue(rebased.contains("url(\"https://example.com/c.png\")"), rebased);
        assertTrue(rebased.contains("url(\"/abs/d.png\")"), rebased);
    }

    @Test
    void anImportInsideACommentOrARuleIsNotAnImport(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        File root = write(new File(dir, "theme.css"),
                "/* @import \"never.css\"; */\nNote { color: #abcdef; }\n");
        Set<File> imported = new LinkedHashSet<File>();
        String text = new String(Files.readAllBytes(root.toPath()), StandardCharsets.UTF_8);
        assertEquals(text, CssImports.inline(root, text, imported), "a commented-out import is left as it is");
        assertTrue(imported.isEmpty());
    }

    @Test
    void aMissingImportFailsTheCompileNamingTheFile(@TempDir Path tmp) throws Exception {
        File root = write(new File(tmp.toFile(), "theme.css"), "@import \"gone.css\";\nA { color: red; }\n");
        IOException ex = assertThrows(IOException.class, () -> CSSTheme.load(root.toURI().toURL()));
        assertTrue(ex.getMessage().contains("names a file that does not exist"), ex.getMessage());
        assertTrue(ex.getMessage().contains("gone.css"), ex.getMessage());
    }

    @Test
    void aCircularImportFailsTheCompileShowingTheCycle(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        write(new File(dir, "a.css"), "@import \"b.css\";\n");
        write(new File(dir, "b.css"), "@import \"a.css\";\n");
        File root = write(new File(dir, "theme.css"), "@import \"a.css\";\n");
        IOException ex = assertThrows(IOException.class, () -> CSSTheme.load(root.toURI().toURL()));
        assertTrue(ex.getMessage().contains("Circular @import: theme.css -> a.css -> b.css -> a.css"),
                ex.getMessage());
    }

    @Test
    void aRemoteImportIsRefused(@TempDir Path tmp) throws Exception {
        File root = write(new File(tmp.toFile(), "theme.css"), "@import url(https://example.com/x.css);\n");
        IOException ex = assertThrows(IOException.class, () -> CSSTheme.load(root.toURI().toURL()));
        assertTrue(ex.getMessage().contains("only local files can be imported"), ex.getMessage());
    }
}
