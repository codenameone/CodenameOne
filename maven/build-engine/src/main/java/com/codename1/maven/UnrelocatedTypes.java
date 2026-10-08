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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Lists the classes that still refer to a TYPE under one of a set of
/// packages: what the compatibility layers' integration tests ask of a
/// classes directory once the remap step has run over it.
///
/// A search of the class file's bytes cannot answer that. A relocated class
/// keeps every string the application wrote, and an application is entitled
/// to the text `"javax/swing/JTable"` -- in a message, a resource path, a
/// key. What must not survive is a reference the device would have to
/// resolve, so this reads each class the way the relocation does and looks
/// only at type names: supertypes, the descriptors and generic signatures
/// of fields and methods, the operands of instructions, the types of
/// annotations, method handles and constant class literals. String
/// constants and annotation values are never looked at.
///
/// #### As a program
///
/// ```
/// java -cp <build-engine and ASM> com.codename1.maven.UnrelocatedTypes \
///      <classes directory or jar> <package> [<package> ...]
/// ```
///
/// A package is slash-separated, without a trailing slash (`javax/swing`).
/// Each class that names a type under one prints as a line -- its path, a
/// tab, and the first few such types -- and the exit status is 1; it is 0
/// when there is none and 2 when the arguments make no sense.
public final class UnrelocatedTypes {

    /// How many type names a line gives before it stops.
    private static final int SHOWN = 3;

    private UnrelocatedTypes() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /// What [#main] does, answering the exit status instead of exiting.
    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length < 2) {
            err.println("usage: UnrelocatedTypes <classes directory or jar> <package> [<package> ...]");
            return 2;
        }
        File root = new File(args[0]);
        if (!root.exists()) {
            err.println(root + " does not exist");
            return 2;
        }
        List<String> packages = new ArrayList<String>();
        for (String p : Arrays.asList(args).subList(1, args.length)) {
            String trimmed = p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
            if (trimmed.length() > 0) {
                packages.add(trimmed + "/");
            }
        }
        try {
            Map<String, Set<String>> found = scan(root, packages);
            for (Map.Entry<String, Set<String>> e : found.entrySet()) {
                StringBuilder line = new StringBuilder(e.getKey()).append('\t');
                int n = 0;
                for (String type : e.getValue()) {
                    if (n == SHOWN) {
                        line.append(", ...");
                        break;
                    }
                    line.append(n == 0 ? "" : ", ").append(type);
                    n++;
                }
                out.println(line);
            }
            return found.isEmpty() ? 0 : 1;
        } catch (IOException e) {
            err.println("Could not read " + root + ": " + e.getMessage());
            return 2;
        }
    }

    /// The classes under `root` -- a directory, or a jar -- that name a type
    /// whose internal name starts with one of `prefixes` (each ending in a
    /// slash), by their path, with the types each one names.
    public static Map<String, Set<String>> scan(File root, List<String> prefixes) throws IOException {
        Map<String, Set<String>> out = new TreeMap<String, Set<String>>();
        if (root.isDirectory()) {
            directory(root, "", prefixes, out);
        } else {
            ZipFile zip = new ZipFile(root);
            try {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    if (!e.isDirectory() && e.getName().endsWith(".class")) {
                        InputStream in = zip.getInputStream(e);
                        try {
                            record(e.getName(), types(readAll(in), prefixes), out);
                        } finally {
                            in.close();
                        }
                    }
                }
            } finally {
                zip.close();
            }
        }
        return out;
    }

    private static void directory(File dir, String rel, List<String> prefixes, Map<String, Set<String>> out)
            throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                directory(f, rel + f.getName() + "/", prefixes, out);
            } else if (f.getName().endsWith(".class")) {
                record(rel + f.getName(), types(Files.readAllBytes(f.toPath()), prefixes), out);
            }
        }
    }

    private static void record(String path, Set<String> types, Map<String, Set<String>> out) {
        if (!types.isEmpty()) {
            out.put(path, types);
        }
    }

    /// The types under `prefixes` the class `bytes` refers to.
    static Set<String> types(byte[] bytes, final List<String> prefixes) {
        final Set<String> found = new TreeSet<String>();
        Remapper collector = new Remapper() {
            @Override
            public String map(String internalName) {
                if (internalName != null) {
                    for (String p : prefixes) {
                        if (internalName.startsWith(p)) {
                            found.add(internalName);
                            break;
                        }
                    }
                }
                return internalName;
            }
        };
        // Debug information is read too: a local variable's type is a name
        // the relocation is expected to have moved with the rest.
        new ClassReader(bytes).accept(new ClassRemapper(new ClassWriter(0), collector), ClassReader.SKIP_FRAMES);
        return found.isEmpty() ? Collections.<String>emptySet() : found;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n = in.read(buf);
        while (n >= 0) {
            out.write(buf, 0, n);
            n = in.read(buf);
        }
        return out.toByteArray();
    }
}
