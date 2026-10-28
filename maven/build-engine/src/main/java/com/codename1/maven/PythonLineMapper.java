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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/// Points the classes translated from Python at the Python source.
///
/// The transpiler writes, into each module's class, a constant naming the
/// `.py` file and pairing Java lines with the Python lines they came from.
/// This rewrites every line number of the module's classes (nested ones
/// included) to the Python line, sets the source file to the `.py` file,
/// adds a JSR-45 source map carrying its path, and removes the constant.
/// A Java debugger then steps through, and sets breakpoints in, the Python
/// source, and a stack trace names `main.py:12` -- on every target, since the
/// device translators read the same line tables.
///
/// Runs over the compiled classes, after javac; a class without the
/// constant, or already rewritten, is left alone.
public final class PythonLineMapper {
    /// The package the transpiler writes into.
    static final String PACKAGE_DIR = "com/codename1/generated/python";
    /// Must match `PythonTranspiler.LINE_MAP_FIELD`.
    static final String FIELD = "$cn1PyLines";

    private PythonLineMapper() {
    }

    /// `PythonLineMapper <classes dir>`, for a build that compiles the
    /// generated Java itself.
    public static void main(String[] args) throws IOException {
        System.out.println("Mapped " + apply(new File(args[0])) + " Python classes to their sources");
    }

    /// Rewrites the classes under `classesDir`; returns how many.
    public static int apply(File classesDir) throws IOException {
        File dir = new File(classesDir, PACKAGE_DIR);
        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }
        Map<String, Table> tables = new HashMap<String, Table>();
        for (File f : files) {
            String n = f.getName();
            // Module class names may hold a '$' themselves (`py_app$2eutil`),
            // so every class is read for the constant, not only top-level
            // looking ones.
            if (n.endsWith(".class")) {
                Table t = read(Files.readAllBytes(f.toPath()));
                if (t != null) {
                    tables.put(n.substring(0, n.length() - ".class".length()), t);
                }
            }
        }
        int count = 0;
        for (File f : files) {
            String n = f.getName();
            if (!n.endsWith(".class")) {
                continue;
            }
            String stem = n.substring(0, n.length() - ".class".length());
            // The class's own table, else its nearest enclosing class's.
            String outer = stem;
            Table t = tables.get(outer);
            for (int cut = outer.lastIndexOf('$'); t == null && cut >= 0; cut = outer.lastIndexOf('$')) {
                outer = outer.substring(0, cut);
                t = tables.get(outer);
            }
            if (t != null) {
                Files.write(f.toPath(), rewrite(Files.readAllBytes(f.toPath()), t));
                count++;
            }
        }
        return count;
    }

    /// A module's line table.
    static final class Table {
        final String path;
        final TreeMap<Integer, Integer> lines = new TreeMap<Integer, Integer>();
        int max;

        Table(String value) {
            int nl = value.indexOf('\n');
            path = value.substring(0, nl);
            for (String pair : value.substring(nl + 1).split(",")) {
                int colon = pair.indexOf(':');
                int py = Integer.parseInt(pair.substring(colon + 1));
                lines.put(Integer.valueOf(pair.substring(0, colon)), py);
                max = Math.max(max, py);
            }
        }

        int map(int javaLine) {
            Map.Entry<Integer, Integer> e = lines.floorEntry(javaLine);
            return e != null ? e.getValue() : lines.firstEntry().getValue();
        }

        String fileName() {
            return path.substring(path.lastIndexOf('/') + 1);
        }

        /// One file, Python lines mapped to themselves: the line table
        /// already holds Python lines, and the map adds the file's path,
        /// which the source file attribute cannot carry.
        String smap(String javaFile) {
            return "SMAP\n" + javaFile + "\nPython\n*S Python\n*F\n+ 1 " + fileName() + "\n" + path
                    + "\n*L\n1#1," + max + ":1\n*E\n";
        }
    }

    static Table read(byte[] bytes) {
        final String[] value = new String[1];
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object v) {
                if (FIELD.equals(name) && v instanceof String) {
                    value[0] = (String) v;
                }
                return null;
            }
        }, ClassReader.SKIP_CODE);
        return value[0] == null ? null : new Table(value[0]);
    }

    static byte[] rewrite(byte[] bytes, final Table t) {
        ClassReader r = new ClassReader(bytes);
        ClassWriter w = new ClassWriter(r, 0);
        r.accept(new ClassVisitor(Opcodes.ASM9, w) {
            @Override
            public void visitSource(String source, String debug) {
                super.visitSource(t.fileName(), t.smap(source == null ? "" : source));
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object v) {
                return FIELD.equals(name) ? null : super.visitField(access, name, desc, signature, v);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                    String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, desc, signature, exceptions)) {
                    @Override
                    public void visitLineNumber(int line, Label start) {
                        super.visitLineNumber(t.map(line), start);
                    }
                };
            }
        }, 0);
        return w.toByteArray();
    }
}
