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
package com.codenameone.playground;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

/// Classifies documentation snippets the way the inline Playground will treat
/// them when a reader clicks "Try it": compiled by the Playground's own compiler
/// against the Playground's API, exactly as a reader's edit would be.
///
/// Reads every `*.java` file in the directory given as the only argument (one
/// snippet each, written by `scripts/check-doc-snippets.py`) and prints one
/// tab-separated line per file: name, status, line, message. The status is
/// `runnable` (it compiles, so the frame runs it as it stands), `fragment` (it
/// parses, but needs code from the surrounding text -- a variable, a field, an
/// import the Playground does not add) or `not-java` (it does not parse).
///
/// One JVM for the whole set: the stub library loads once, which is what makes
/// classifying a thousand snippets take seconds instead of an hour.
public final class DocSnippetRunnabilityHarness {
    private DocSnippetRunnabilityHarness() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: DocSnippetRunnabilityHarness <snippet-dir>");
            System.exit(2);
        }
        File[] files = new File(args[0]).listFiles((dir, name) -> name.endsWith(".java"));
        if (files == null) {
            System.err.println("Not a directory: " + args[0]);
            System.exit(2);
        }
        Arrays.sort(files);
        StringBuilder out = new StringBuilder();
        for (File f : files) {
            String source = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            String status = "runnable";
            int line = 0;
            String message = "";
            try {
                PlaygroundRunner.compile(source);
            } catch (PlaygroundRunner.CompileFailure failure) {
                status = failure.wellFormed ? "fragment" : "not-java";
                if (!failure.diagnostics.isEmpty()) {
                    PlaygroundRunner.Diagnostic d = failure.diagnostics.get(0);
                    line = d.line;
                    message = d.message;
                }
            } catch (Throwable t) {
                status = "not-java";
                message = String.valueOf(t);
            }
            out.append(f.getName()).append('\t').append(status).append('\t').append(line)
                    .append('\t').append(oneLine(message)).append('\n');
        }
        System.out.print(out);
        System.out.flush();
    }

    private static String oneLine(String s) {
        return s == null ? "" : s.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }
}
