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
package com.codename1.android.rescompiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/// Command line entry point used by the compatibility runtime's own build to
/// compile the AndroidX and Material resources it ships (the library table,
/// the symbols file application builds read, and an R class per library).
///
/// ```
/// LibraryMain <library-res dir> <java out> <resources out> <symbols out> <framework symbols>
/// ```
///
/// The library-res directory holds one Android `res` directory per library,
/// named after the library's Java package (`androidx.appcompat/values/...`),
/// and `libraries.txt`, which lists those packages one per line, highest
/// priority first -- the order the Android Gradle plugin would merge them in
/// (Material over AppCompat over core). Every library is compiled into one
/// namespace, so each library's R class has the same ids, as they do in an
/// application.
public final class LibraryMain {

    private LibraryMain() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 5) {
            // exec:java runs this inside the Maven JVM, so failures throw rather
            // than System.exit, which would end the whole build abruptly.
            throw new IllegalArgumentException(
                    "usage: LibraryMain <libraryRes> <javaOut> <resourcesOut> <symbolsOut> <frameworkSymbols>");
        }
        File root = new File(args[0]);
        ResourceCompiler.Request req = new ResourceCompiler.Request();
        req.library = true;
        BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(new File(root, "libraries.txt")), Charset.forName("UTF-8")));
        try {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                File dir = new File(root, line);
                if (!dir.isDirectory()) {
                    throw new IllegalStateException("libraries.txt lists " + line + ", which has no directory");
                }
                req.res.add(new ResourceCompiler.ResSource(dir, line));
            }
        } finally {
            r.close();
        }
        req.javaOut = new File(args[1]);
        req.resourcesOut = new File(args[2]);
        req.symbolsOut = new File(args[3]);
        InputStream fw = new FileInputStream(args[4]);
        ResourceCompiler.Result res;
        try {
            req.frameworkSymbols = fw;
            res = new ResourceCompiler().compile(req);
        } finally {
            fw.close();
        }
        for (Diagnostic d : res.diagnostics) {
            System.err.println((d.severity == Diagnostic.Severity.ERROR ? "ERROR: " : "WARNING: ") + d);
        }
        if (res.hasErrors()) {
            throw new IllegalStateException("Library resources failed to compile; see the errors above");
        }
        System.out.println("Compiled " + res.resourceCount + " library resources");
    }
}
