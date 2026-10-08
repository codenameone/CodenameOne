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
package com.codename1.tools.javac;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Compiles sources with the in-tree compiler against the class library of the JVM
 * it runs on, read through the class loader (rt.jar on 8, the runtime image on 9
 * and later), and writes the class files. The conformance test runs it under the
 * same JDK as the reference javac so both compile against one library.
 *
 * <pre>java RuntimeLibraryCompile &lt;out-dir&gt; &lt;source&gt;...</pre>
 *
 * Prints each diagnostic as javac does and exits 1 when any is an error.
 */
public final class RuntimeLibraryCompile {
    private RuntimeLibraryCompile() {
    }

    /** The running JVM's classes, by internal name; null for a class it does not have. */
    static final ClassLibrary RUNTIME = new ClassLibrary() {
        @Override
        public byte[] classBytes(String internalName) {
            InputStream in = ClassLoader.getSystemResourceAsStream(internalName + ".class");
            if (in == null) {
                return null;
            }
            try {
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                    }
                    return out.toByteArray();
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                throw new IllegalStateException("reading " + internalName, e);
            }
        }
    };

    public static void main(String[] args) throws IOException {
        Path out = Paths.get(args[0]);
        JavaCompiler jc = new JavaCompiler(RUNTIME);
        for (int i = 1; i < args.length; i++) {
            Path p = Paths.get(args[i]);
            jc.addSource(p.getFileName().toString(), new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
        }
        JavaCompiler.Result r = jc.compile();
        for (Diagnostic d : r.getDiagnostics()) {
            System.out.println(d);
        }
        for (Map.Entry<String, byte[]> e : r.getClasses().entrySet()) {
            Path f = out.resolve(e.getKey() + ".class");
            Files.createDirectories(f.getParent());
            Files.write(f, e.getValue());
        }
        System.exit(r.isSuccess() ? 0 : 1);
    }
}
