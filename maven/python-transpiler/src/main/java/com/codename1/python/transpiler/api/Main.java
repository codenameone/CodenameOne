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
package com.codename1.python.transpiler.api;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// Command line: `Main <source root> <output root> [main module]`.
///
/// Transpiles every `.py` under the source root and writes the Java sources
/// under the output root. Exit status is 0 on success, 1 when a source does
/// not transpile (the message names its file and line), 2 for a bad command
/// line.
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 3) {
            System.err.println("usage: Main <source root> <output root> [main module]");
            System.exit(2);
            return;
        }
        Path root = Paths.get(args[0]);
        Map<String, String> sources = new TreeMap<String, String>();
        List<Path> files;
        try (Stream<Path> s = Files.walk(root)) {
            files = s.filter(p -> p.toString().endsWith(".py")).collect(Collectors.toCollection(ArrayList::new));
        }
        for (Path p : files) {
            sources.put(root.relativize(p).toString().replace(File.separatorChar, '/'),
                    new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
        }
        Map<String, String> out;
        try {
            out = PythonTranspiler.transpile(sources, args.length == 3 ? args[2] : null);
        } catch (PythonSyntaxError e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }
        for (Map.Entry<String, String> e : out.entrySet()) {
            Path f = Paths.get(args[1], e.getKey()).toAbsolutePath();
            Path dir = f.getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            Files.write(f, e.getValue().getBytes(StandardCharsets.US_ASCII));
        }
    }
}
