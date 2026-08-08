/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.hardening;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** The mapping must carry the real source filename for classes whose name can't reconstruct it. */
public class MappingWriterTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File mappingWith(String body) throws Exception {
        File f = tmp.newFile("mapping.txt");
        Files.write(f.toPath(), body.getBytes(Charset.forName("UTF-8")));
        return f;
    }

    private String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), Charset.forName("UTF-8"));
    }

    @Test
    public void injectsIndentedSourceFileMetadataForNonDefaultFilesOnly() throws Exception {
        File map = mappingWith("com.foo.Screen -> a:\n"
                + "    void onClick() -> b\n"
                + "com.foo.Widget -> c:\n");
        Map<String, String> sf = new HashMap<String, String>();
        sf.put("com.foo.Screen", "Screen.kt");   // Kotlin: name can't reconstruct the file
        // Widget deliberately absent: its file is the synthesized default, so nothing to record.

        MappingWriter.injectSourceFiles(map, sf);
        String out = read(map);
        // The comment is INDENTED and placed immediately after the Screen class line, so the retrace
        // parser attaches it to Screen (a column-0 comment would be skipped).
        assertTrue(out, out.contains("com.foo.Screen -> a:\n"
                + "    # {\"id\":\"sourceFile\",\"fileName\":\"Screen.kt\"}\n"));
        // The member line survives and stays under Screen.
        assertTrue(out, out.contains("void onClick() -> b"));
        // Widget got no comment (not in the map).
        assertFalse(out, out.contains("Widget.kt"));
        assertFalse(out, out.contains("\"fileName\":\"Widget"));
    }

    @Test
    public void noMapOrMissingFileIsANoOp() throws Exception {
        File map = mappingWith("com.foo.A -> a:\n");
        MappingWriter.injectSourceFiles(map, null);
        MappingWriter.injectSourceFiles(map, new HashMap<String, String>());
        assertTrue(read(map).contains("com.foo.A -> a:"));
        // A non-existent file must not throw.
        MappingWriter.injectSourceFiles(new File(tmp.getRoot(), "nope.txt"),
                java.util.Collections.singletonMap("com.foo.A", "A.kt"));
    }
}
