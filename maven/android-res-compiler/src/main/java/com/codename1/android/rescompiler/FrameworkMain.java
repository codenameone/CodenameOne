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
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/// Command line entry point used by the compatibility runtime's own build to
/// compile the framework resources (`android.R`, the framework table and the
/// symbols file application builds read).
///
/// ```
/// FrameworkMain <res dir> <views file> <java out> <resources out> <symbols out>
/// ```
///
/// The views file lists `Tag fully.qualified.Class` per line: every view class
/// the runtime implements, so application layouts can be checked against it.
public final class FrameworkMain {

    private FrameworkMain() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 5) {
            // exec:java runs this inside the Maven JVM, so failures throw rather
            // than System.exit, which would end the whole build abruptly.
            throw new IllegalArgumentException("usage: FrameworkMain <res> <views> <javaOut> <resourcesOut> <symbolsOut>");
        }
        ResourceCompiler.Request req = new ResourceCompiler.Request();
        req.framework = true;
        req.res.add(new ResourceCompiler.ResSource(new File(args[0]), "android"));
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(args[1]), Charset.forName("UTF-8")));
        try {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                String[] p = line.split("\\s+");
                req.frameworkViews.put(p[0], p[1]);
            }
        } finally {
            r.close();
        }
        req.javaOut = new File(args[2]);
        req.resourcesOut = new File(args[3]);
        req.symbolsOut = new File(args[4]);
        ResourceCompiler.Result res = new ResourceCompiler().compile(req);
        for (Diagnostic d : res.diagnostics) {
            System.err.println((d.severity == Diagnostic.Severity.ERROR ? "ERROR: " : "WARNING: ") + d);
        }
        if (res.hasErrors()) {
            throw new IllegalStateException("Framework resources failed to compile; see the errors above");
        }
        System.out.println("Compiled " + res.resourceCount + " framework resources");
    }
}
