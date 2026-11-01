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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/// Points the classes transpiled from Dart at the Dart source.
///
/// The transpiler writes, into each class it generates, a constant naming
/// the `.dart` files and pairing Java lines with the Dart lines they came
/// from (`DartLineTable` in dart-transpiler). This rewrites every line
/// number of the class and its nested classes to the Dart line, sets the
/// source file to the `.dart` file, adds a JSR-45 source map carrying the
/// paths, and removes the constant. A Java debugger then steps through, and
/// sets breakpoints in, the Dart source, and a stack trace names
/// `main.dart:12` -- on every target, since the device translators read the
/// same line tables.
///
/// The output package is a build setting (`cn1.flutter.package`), so the
/// whole classes tree is scanned; a class is parsed only when its bytes
/// hold the constant's name. Runs after javac; a class without the
/// constant, or already rewritten, is left alone.
public final class DartLineMapper {
    /// Must match `DartLineTable.FIELD`; a long table continues in
    /// `FIELD1`, `FIELD2`...
    static final String FIELD = "$cn1DartLines";
    private static final byte[] FIELD_BYTES = FIELD.getBytes(StandardCharsets.US_ASCII);

    private DartLineMapper() {
    }

    /// Rewrites the classes under `classesDir`; returns how many.
    public static int apply(File classesDir) throws IOException {
        return applyDir(classesDir);
    }

    private static int applyDir(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }
        int count = 0;
        Map<String, Table> tables = new HashMap<String, Table>();
        for (File f : files) {
            String n = f.getName();
            if (f.isDirectory()) {
                count += applyDir(f);
            } else if (n.endsWith(".class")) {
                byte[] bytes = Files.readAllBytes(f.toPath());
                if (contains(bytes, FIELD_BYTES)) {
                    Table t = read(bytes);
                    if (t != null) {
                        tables.put(n.substring(0, n.length() - ".class".length()), t);
                    }
                }
            }
        }
        if (tables.isEmpty()) {
            return count;
        }
        for (File f : files) {
            String n = f.getName();
            if (!n.endsWith(".class") || f.isDirectory()) {
                continue;
            }
            // The class's own table, else -- for a class javac split out of
            // another (anonymous, local, nested) -- its host's. The host comes
            // from the class file itself, never from the name: a Dart
            // identifier may hold a '$', and `Foo$Bar` is then a class of
            // its own, maybe of another file, not part of `Foo`.
            String stem = n.substring(0, n.length() - ".class".length());
            Table t = tables.get(stem);
            byte[] bytes = null;
            if (t == null && stem.indexOf('$') > 0) {
                bytes = Files.readAllBytes(f.toPath());
                String host = host(bytes);
                if (host != null) {
                    t = tables.get(host.substring(host.lastIndexOf('/') + 1));
                }
            }
            if (t != null) {
                Files.write(f.toPath(), rewrite(bytes != null ? bytes : Files.readAllBytes(f.toPath()), t));
                count++;
            }
        }
        return count;
    }

    /// The class a javac-split class belongs to: its nest host, else the
    /// class enclosing it (anonymous and local classes), else the outer
    /// class its own InnerClasses entry names; null for a top-level class.
    static String host(byte[] bytes) {
        final String[] found = new String[3];
        final String[] self = new String[1];
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                self[0] = name;
            }

            @Override
            public void visitNestHost(String nestHost) {
                found[0] = nestHost;
            }

            @Override
            public void visitOuterClass(String owner, String name, String descriptor) {
                found[1] = owner;
            }

            @Override
            public void visitInnerClass(String name, String outerName, String innerName, int access) {
                if (name.equals(self[0]) && outerName != null) {
                    found[2] = outerName;
                }
            }
        }, ClassReader.SKIP_CODE);
        for (String h : found) {
            if (h != null) {
                return h;
            }
        }
        return null;
    }

    private static boolean contains(byte[] hay, byte[] needle) {
        outer:
        for (int i = 0, last = hay.length - needle.length; i <= last; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (hay[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    /// A class's line table.
    ///
    /// The class's own Dart file keeps its line numbers. Lines of any other
    /// file (a `part` file's function in its library's class) are moved past
    /// the previous file's last line, because a line table carries no file:
    /// the source map gives each file its own range, so a debugger still
    /// shows the right file and line, while a stack trace names the class's
    /// own file with the shifted number.
    static final class Table {
        final List<String> paths = new ArrayList<String>();
        final TreeMap<Integer, Integer> lines = new TreeMap<Integer, Integer>();
        final int[] max;
        final int[] offset;
        static final int MAX_LINE = 0xffff;

        Table(String value) {
            String[] rows = value.split("\n");
            for (int i = 0; i < rows.length - 1; i++) {
                paths.add(rows[i]);
            }
            max = new int[paths.size()];
            offset = new int[paths.size()];
            List<int[]> parsed = new ArrayList<int[]>();
            for (String pair : rows[rows.length - 1].split(",")) {
                String[] p = pair.split(":");
                int file = p.length > 2 ? Integer.parseInt(p[2]) : 0;
                int line = Integer.parseInt(p[1]);
                parsed.add(new int[] {Integer.parseInt(p[0]), line, file});
                max[file] = Math.max(max[file], line);
            }
            for (int i = 1; i < offset.length; i++) {
                offset[i] = offset[i - 1] + max[i - 1];
                // A line number is a u2. Files that no longer fit keep no
                // range of their own: their statements map to the line before
                // them, rather than wrap into another file's range.
                if (offset[i] + max[i] > MAX_LINE) {
                    max[i] = 0;
                }
            }
            for (int[] p : parsed) {
                if (p[2] == 0 || max[p[2]] > 0) {
                    lines.put(p[0], p[1] + offset[p[2]]);
                }
            }
            if (lines.isEmpty()) {
                lines.put(1, 1);
            }
        }

        /// Java lines before the first statement -- field initializers, a
        /// static initializer, a constructor's header -- take the first
        /// statement's line: a line table must name some line, and the class's
        /// own file is the right one even where the line is approximate.
        int map(int javaLine) {
            Map.Entry<Integer, Integer> e = lines.floorEntry(javaLine);
            return e != null ? e.getValue() : lines.firstEntry().getValue();
        }

        static String fileName(String path) {
            return path.substring(path.lastIndexOf('/') + 1);
        }

        String fileName() {
            return fileName(paths.get(0));
        }

        /// The Dart stratum, the default: every file's lines mapped onto
        /// its range of the rewritten line table.
        String smap(String javaFile) {
            StringBuilder sb = new StringBuilder("SMAP\n").append(javaFile).append("\nDart\n*S Dart\n*F\n");
            for (int i = 0; i < paths.size(); i++) {
                sb.append("+ ").append(i + 1).append(' ').append(fileName(paths.get(i))).append('\n')
                        .append(paths.get(i)).append('\n');
            }
            sb.append("*L\n");
            for (int i = 0; i < paths.size(); i++) {
                if (max[i] > 0) {
                    sb.append("1#").append(i + 1).append(',').append(max[i]).append(':')
                            .append(offset[i] + 1).append('\n');
                }
            }
            return sb.append("*E\n").toString();
        }
    }

    static Table read(byte[] bytes) {
        final TreeMap<Integer, String> parts = new TreeMap<Integer, String>();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object v) {
                int index = index(name);
                if (index >= 0 && v instanceof String) {
                    parts.put(index, (String) v);
                }
                return null;
            }
        }, ClassReader.SKIP_CODE);
        if (parts.isEmpty()) {
            return null;
        }
        StringBuilder value = new StringBuilder();
        for (String p : parts.values()) {
            value.append(p);
        }
        return new Table(value.toString());
    }

    /// The chunk number of a table constant, or -1 for any other field.
    static int index(String name) {
        if (!name.startsWith(FIELD)) {
            return -1;
        }
        String rest = name.substring(FIELD.length());
        if (rest.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < rest.length(); i++) {
            if (rest.charAt(i) < '0' || rest.charAt(i) > '9') {
                return -1;
            }
        }
        return Integer.parseInt(rest);
    }

    static byte[] rewrite(byte[] bytes, final Table t) {
        ClassReader r = new ClassReader(bytes);
        // Not ClassWriter(r, 0): that copies the reader's constant pool whole,
        // so the table's strings would stay in every class after its field
        // is gone.
        ClassWriter w = new ClassWriter(0);
        r.accept(new ClassVisitor(Opcodes.ASM9, w) {
            @Override
            public void visitSource(String source, String debug) {
                super.visitSource(t.fileName(), t.smap(source == null ? "" : source));
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object v) {
                return index(name) >= 0 ? null : super.visitField(access, name, desc, signature, v);
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
