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
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Build-time tool: {@code BuildStubLibrary out.cn1stubs [--exclude prefix]... input...}
 * where each input is a jar or a directory of class files. Writes a
 * {@link StubLibrary}; the first input that has a class wins, so list the runtime
 * library (JavaAPI) before anything that might shadow it. JVM only (it reads jars),
 * which is why it lives outside the translatable compiler sources.
 */
public final class BuildStubLibrary {
    private BuildStubLibrary() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("usage: BuildStubLibrary out.cn1stubs [--exclude prefix]... input...");
            System.exit(2);
        }
        Map<String, byte[]> stubs = new TreeMap<String, byte[]>();
        java.util.List<String> excludes = new java.util.ArrayList<String>();
        for (int i = 1; i < args.length; i++) {
            if ("--exclude".equals(args[i]) && i + 1 < args.length) {
                excludes.add(args[++i]);
                continue;
            }
            File in = new File(args[i]);
            if (in.isDirectory()) {
                walk(in, in, stubs, excludes);
            } else if (in.isFile()) {
                readJar(in, stubs, excludes);
            } else {
                System.err.println("missing input " + in);
                System.exit(1);
            }
        }
        byte[] packed = StubLibrary.pack(stubs);
        OutputStream out = new FileOutputStream(args[0]);
        try {
            out.write(packed);
        } finally {
            out.close();
        }
        System.out.println("stub library: " + stubs.size() + " classes, " + packed.length + " bytes -> " + args[0]);
    }

    private static boolean excluded(String name, java.util.List<String> excludes) {
        if (name.equals("module-info") || name.endsWith("/package-info") || name.startsWith("META-INF/")) {
            return true;
        }
        for (String e : excludes) {
            if (name.startsWith(e)) {
                return true;
            }
        }
        return false;
    }

    private static void add(String name, byte[] bytes, Map<String, byte[]> stubs, java.util.List<String> excludes) {
        if (excluded(name, excludes) || stubs.containsKey(name)) {
            return;
        }
        stubs.put(name, StubLibrary.stub(bytes));
    }

    private static void walk(File root, File dir, Map<String, byte[]> stubs, java.util.List<String> excludes) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        java.util.Arrays.sort(files);
        for (File f : files) {
            if (f.isDirectory()) {
                walk(root, f, stubs, excludes);
            } else if (f.getName().endsWith(".class")) {
                String rel = root.toURI().relativize(f.toURI()).getPath();
                add(rel.substring(0, rel.length() - 6), readAll(new FileInputStream(f)), stubs, excludes);
            }
        }
    }

    private static void readJar(File jar, Map<String, byte[]> stubs, java.util.List<String> excludes) throws IOException {
        ZipInputStream zin = new ZipInputStream(new FileInputStream(jar));
        try {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                String n = e.getName();
                if (!e.isDirectory() && n.endsWith(".class") && !n.startsWith("META-INF/versions/")) {
                    add(n.substring(0, n.length() - 6), readAllNoClose(zin), stubs, excludes);
                }
            }
        } finally {
            zin.close();
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        try {
            return readAllNoClose(in);
        } finally {
            in.close();
        }
    }

    private static byte[] readAllNoClose(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
