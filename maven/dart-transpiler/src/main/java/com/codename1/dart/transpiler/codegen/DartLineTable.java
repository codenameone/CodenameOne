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
package com.codename1.dart.transpiler.codegen;

import java.util.ArrayList;
import java.util.List;

/// The Dart line table of a generated Java file.
///
/// The emitter writes a [#MARK] comment line in front of every Dart
/// statement it lowers. Once the file is final (after the dynamic-case
/// pruning, which drops lines), [#apply] reads the marks into
/// `javaLine:dartLine` pairs and stores them in a constant, [#FIELD], added
/// just before the type's closing brace so that no line above it moves.
/// `DartLineMapper` in build-engine reads the constant from the compiled
/// class, rewrites the class's line numbers and source file to the Dart
/// ones, adds a JSR-45 source map and removes the constant -- so a Java
/// debugger steps through the Dart source and a stack trace names
/// `main.dart:12`.
///
/// The constant's value: the Dart files, one per line, the class's own
/// first; then one line of comma separated pairs, `javaLine:dartLine` for
/// the first file and `javaLine:dartLine:fileIndex` for any other (a
/// `part` file's function lands in its owning library's class).
public final class DartLineTable {
    /// Must match `DartLineMapper.FIELD` in build-engine. A long table is
    /// split over `FIELD`, `FIELD1`, `FIELD2`...: a class file constant
    /// cannot exceed 65535 bytes, and javac rejects a longer literal.
    public static final String FIELD = "$cn1DartLines";
    /// Opens a mark line: `//@dart:<line> <file>`.
    static final String MARK = "//@dart:";
    /// Characters per constant, well under the class file limit even if
    /// every character took three bytes.
    private static final int CHUNK = 20000;

    private DartLineTable() {
    }

    /// The mark line for a statement at `line` of `file`, or null when the
    /// node carries no position.
    static String mark(String file, int line) {
        if (file == null || line <= 0) {
            return null;
        }
        return MARK + line + " " + file.replace('\\', '/');
    }

    /// True for a line (indented or not) that is a mark.
    static boolean isMark(String line) {
        return line.trim().startsWith(MARK);
    }

    /// `java` with its line table added, `ownFile` first among the files;
    /// unchanged when it holds no mark.
    static String apply(String java, String ownFile) {
        List<String> files = new ArrayList<String>();
        files.add(ownFile.replace('\\', '/'));
        StringBuilder pairs = new StringBuilder();
        int line = 0;
        int dart = 0;
        int fileIndex = 0;
        String last = null;
        int start = 0;
        while (start < java.length()) {
            int end = java.indexOf('\n', start);
            if (end < 0) {
                end = java.length();
            }
            line++;
            int i = start;
            while (i < end && java.charAt(i) == ' ') {
                i++;
            }
            if (java.startsWith(MARK, i)) {
                int space = java.indexOf(' ', i + MARK.length());
                dart = Integer.parseInt(java.substring(i + MARK.length(), space));
                String f = java.substring(space + 1, end);
                fileIndex = files.indexOf(f);
                if (fileIndex < 0) {
                    fileIndex = files.size();
                    files.add(f);
                }
            } else if (dart > 0 && i < end) {
                String pair = fileIndex == 0 ? dart + "" : dart + ":" + fileIndex;
                if (!pair.equals(last)) {
                    pairs.append(pairs.length() == 0 ? "" : ",").append(line).append(':').append(pair);
                    last = pair;
                }
            }
            start = end + 1;
        }
        int close = java.lastIndexOf('}');
        if (pairs.length() == 0 || close < 0) {
            return java;
        }
        StringBuilder value = new StringBuilder();
        for (String f : files) {
            value.append(f).append('\n');
        }
        value.append(pairs);
        StringBuilder fields = new StringBuilder();
        for (int at = 0, n = 0; at < value.length(); at += CHUNK, n++) {
            // Interfaces (mixins) take no `private`; a bare static final
            // compiles to the same ConstantValue in every kind of type.
            fields.append("    static final String ").append(FIELD).append(n == 0 ? "" : String.valueOf(n))
                    .append(" = ").append(literal(value.substring(at, Math.min(value.length(), at + CHUNK))))
                    .append(";\n");
        }
        return java.substring(0, close) + fields + java.substring(close);
    }

    private static String literal(String value) {
        StringBuilder lit = new StringBuilder("\"");
        for (int k = 0; k < value.length(); k++) {
            char c = value.charAt(k);
            if (c == '\n') {
                lit.append("\\n");
            } else if (c == '"' || c == '\\') {
                lit.append('\\').append(c);
            } else if (c < 0x20 || c > 0x7e) {
                lit.append(String.format("\\u%04x", (int) c));
            } else {
                lit.append(c);
            }
        }
        return lit.append('"').toString();
    }
}
