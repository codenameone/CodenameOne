/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.transpiler;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// `runtime-names.txt` must name every identifier a string literal of the
/// Java runtime spells. Infer types a method call on a value of no one type
/// by the program's methods of that name only when no runtime object can
/// answer it, and a runtime object can answer only a name its source spells;
/// a name missing from the list would type such a call too narrowly.
class RuntimeNamesTest {
    @Test
    void listCoversTheRuntime() throws IOException {
        Path src = Paths.get("..", "python-runtime", "src", "main", "java");
        assertTrue(Files.isDirectory(src), "runtime sources not found at " + src.toAbsolutePath());
        Pattern lit = Pattern.compile("\"([A-Za-z_][A-Za-z0-9_]*)\"");
        Set<String> spelled = new TreeSet<String>();
        List<Path> files;
        try (Stream<Path> s = Files.walk(src)) {
            files = s.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
        for (Path f : files) {
            Matcher m = lit.matcher(new String(Files.readAllBytes(f), StandardCharsets.UTF_8));
            while (m.find()) {
                spelled.add(m.group(1));
            }
        }
        Set<String> listed = new HashSet<String>();
        try (InputStream in = RuntimeNamesTest.class.getResourceAsStream(
                "/com/codename1/python/transpiler/runtime-names.txt")) {
            assertNotNull(in, "runtime-names.txt is missing");
            for (String l : new String(readAll(in), StandardCharsets.UTF_8).split("\n")) {
                listed.add(l.trim());
            }
        }
        List<String> missing = new ArrayList<String>();
        for (String n : spelled) {
            if (!listed.contains(n)) {
                missing.add(n);
            }
        }
        assertTrue(missing.isEmpty(), "add to runtime-names.txt (sorted, one per line): " + missing);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
        byte[] b = new byte[8192];
        for (int n = in.read(b); n > 0; n = in.read(b)) {
            o.write(b, 0, n);
        }
        return o.toByteArray();
    }
}
