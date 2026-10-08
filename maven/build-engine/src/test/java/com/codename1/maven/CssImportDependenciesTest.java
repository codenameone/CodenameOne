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
package com.codename1.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CssImportDependenciesTest {

    private static File write(File f, String text) throws Exception {
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    @Test
    void findsImportsFromOutsideTheDirectoryThroughAChain(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        File css = new File(dir, "app/src/main/css");
        write(new File(css, "theme.css"),
                "/* @import \"../ignored.css\"; */\n@import \"parts/local.css\";\n@IMPORT url(../../../../shared/base.css);\n");
        write(new File(css, "parts/local.css"), "Label { color: red; }\n");
        File base = write(new File(dir, "shared/base.css"), "@import 'deeper/colors.css';\n");
        File colors = write(new File(dir, "shared/deeper/colors.css"), "@import \"base.css\";\n@import \"http://x/y.css\";\n");
        write(new File(dir, "app/src/main/ignored.css"), "");

        Set<File> found = CssImportDependencies.outside(css);

        assertEquals(2, found.size(), found.toString());
        assertTrue(found.contains(base.getCanonicalFile()));
        assertTrue(found.contains(colors.getCanonicalFile()));
    }

    @Test
    void anEditToAnOutsideImportMovesTheModificationTime(@TempDir Path tmp) throws Exception {
        File dir = tmp.toFile();
        File css = new File(dir, "css");
        write(new File(css, "theme.css"), "@import \"../shared.css\";\n");
        File shared = write(new File(dir, "shared.css"), "Label { color: red; }\n");
        assertTrue(shared.setLastModified(1700000000000L));

        assertEquals(1700000000000L, CssImportDependencies.lastModified(css));
        assertEquals(0L, CssImportDependencies.lastModified(new File(dir, "missing")));
    }
}
