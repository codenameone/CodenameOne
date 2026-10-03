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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Command-line entry point:
 * {@code Main [-cp dir1;dir2] -d outDir File1.java File2.java ...}.
 * Class-path entries are directories of class files. Uses plain java.io only, so
 * the same program runs on the JVM and translated by ParparVM (JavaScript or C).
 * Exit status is 0 on success, 1 when there were compile errors.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {
        List<String> roots = new ArrayList<String>();
        String out = ".";
        List<String> sources = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (("-cp".equals(a) || "-classpath".equals(a)) && i + 1 < args.length) {
                String cp = args[++i];
                int start = 0;
                for (int k = 0; k <= cp.length(); k++) {
                    if (k == cp.length() || cp.charAt(k) == ';' || cp.charAt(k) == File.pathSeparatorChar) {
                        if (k > start) {
                            roots.add(cp.substring(start, k));
                        }
                        start = k + 1;
                    }
                }
            } else if ("-d".equals(a) && i + 1 < args.length) {
                out = args[++i];
            } else {
                sources.add(a);
            }
        }
        if (sources.isEmpty()) {
            System.out.println("usage: Main [-cp dir;dir] -d outDir Source.java...");
            System.exit(2);
            return;
        }
        final List<String> libRoots = roots;
        JavaCompiler compiler = new JavaCompiler(new ClassLibrary() {
            @Override
            public byte[] classBytes(String internalName) {
                for (String r : libRoots) {
                    File f = new File(r, internalName + ".class");
                    if (f.exists()) {
                        try {
                            return readAll(f);
                        } catch (IOException e) {
                            return null;
                        }
                    }
                }
                return null;
            }
        });
        for (String s : sources) {
            File f = new File(s);
            compiler.addSource(f.getName(), new String(readAll(f), "UTF-8"));
        }
        JavaCompiler.Result result = compiler.compile();
        for (Diagnostic d : result.getDiagnostics()) {
            System.out.println(d.toString());
        }
        for (Map.Entry<String, byte[]> e : result.getClasses().entrySet()) {
            File f = new File(out, e.getKey() + ".class");
            File dir = f.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs()) {
                throw new IOException("cannot create " + dir);
            }
            OutputStream os = new FileOutputStream(f);
            try {
                os.write(e.getValue());
            } finally {
                os.close();
            }
        }
        System.exit(result.isSuccess() ? 0 : 1);
    }

    static byte[] readAll(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } finally {
            in.close();
        }
    }
}
