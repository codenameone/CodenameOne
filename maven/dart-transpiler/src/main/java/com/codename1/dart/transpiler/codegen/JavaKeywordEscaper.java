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

/// Escapes Dart identifiers that are Java reserved words in emitted Java.
///
/// Dart lets `char`, `native`, `public`, `int`, `long`, `package`... name a
/// field, getter, method, enum value or local; Java does not. Rather than
/// escaping at every one of the emitter's declaration and use sites, the
/// finished Java source is scanned token by token (string and char literals
/// and comments are skipped, so a dynamic `Dyn.get(o, "char")` keeps its
/// Dart name) and a keyword standing where only an identifier can stand is
/// renamed with [JavaEmitter#javaMethodName]'s mapping (`char` to `char_`),
/// which is also what the hand-written runtime and the emitter's own
/// escaped sites use, so the two always agree.
///
/// Dart reserved words (`class`, `new`, `this`...) cannot be Dart
/// identifiers, so only the Java-only keywords are candidates. A candidate
/// is an identifier when it follows a `.`, or is followed by a token that
/// can never follow the keyword use (`;`, `=`, `,`, an operator, `(` except
/// for `synchronized`, `)` except a primitive cast, `.` except
/// `int.class`).
final class JavaKeywordEscaper {
    private static final java.util.Set<String> CANDIDATES = new java.util.HashSet<String>(java.util.Arrays.asList(
            "abstract", "boolean", "byte", "char", "double", "float", "goto", "implements", "import",
            "instanceof", "int", "interface", "long", "native", "package", "private", "protected",
            "public", "short", "static", "strictfp", "synchronized", "throws", "transient", "volatile"));
    private static final java.util.Set<String> PRIMITIVES = new java.util.HashSet<String>(java.util.Arrays.asList(
            "boolean", "byte", "char", "double", "float", "int", "long", "short"));

    private JavaKeywordEscaper() {
    }

    static String apply(String src) {
        boolean any = false;
        for (String k : CANDIDATES) {
            if (src.contains(k)) {
                any = true;
                break;
            }
        }
        if (!any) {
            return src;
        }
        StringBuilder out = null;
        int n = src.length();
        int copied = 0;
        int i = 0;
        char prevSig = 0;
        String prevWord = null;
        while (i < n) {
            char c = src.charAt(i);
            if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') {
                int e = src.indexOf('\n', i);
                i = e < 0 ? n : e;
                continue;
            }
            if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') {
                int e = src.indexOf("*/", i + 2);
                i = e < 0 ? n : e + 2;
                continue;
            }
            if (c == '"' && src.startsWith("\"\"\"", i)) {
                int e = src.indexOf("\"\"\"", i + 3);
                while (e > 0 && src.charAt(e - 1) == '\\') {
                    e = src.indexOf("\"\"\"", e + 1);
                }
                i = e < 0 ? n : e + 3;
                prevSig = '"';
                prevWord = null;
                continue;
            }
            if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < n && src.charAt(j) != c && src.charAt(j) != '\n') {
                    if (src.charAt(j) == '\\') {
                        j++;
                    }
                    j++;
                }
                i = j + 1;
                prevSig = c;
                prevWord = null;
                continue;
            }
            if (Character.isJavaIdentifierStart(c)) {
                int j = i + 1;
                while (j < n && Character.isJavaIdentifierPart(src.charAt(j))) {
                    j++;
                }
                String w = src.substring(i, j);
                if (CANDIDATES.contains(w) && isIdentifierUse(src, j, w, prevSig, prevWord)) {
                    if (out == null) {
                        out = new StringBuilder(n + 16);
                    }
                    out.append(src, copied, j).append('_');
                    copied = j;
                }
                prevSig = 'a';
                prevWord = w;
                i = j;
                continue;
            }
            if (Character.isDigit(c)) {
                int j = i + 1;
                while (j < n && (Character.isLetterOrDigit(src.charAt(j)) || src.charAt(j) == '.'
                        || src.charAt(j) == '_')) {
                    j++;
                }
                prevSig = '0';
                prevWord = null;
                i = j;
                continue;
            }
            if (!Character.isWhitespace(c)) {
                prevSig = c;
                prevWord = null;
            }
            i++;
        }
        if (out == null) {
            return src;
        }
        out.append(src, copied, n);
        return out.toString();
    }

    private static boolean isIdentifierUse(String src, int end, String w, char prevSig, String prevWord) {
        if (prevSig == '.') {
            return true;
        }
        int k = end;
        while (k < src.length() && Character.isWhitespace(src.charAt(k))) {
            k++;
        }
        if (k >= src.length()) {
            return false;
        }
        char next = src.charAt(k);
        switch (next) {
            case ';': case ',': case '=': case '+': case '-': case '*': case '/': case '%':
            case '?': case ':': case '}': case ']': case '!': case '&': case '|': case '^':
                return true;
            case '(':
                return !"synchronized".equals(w);
            case ')':
                return !(PRIMITIVES.contains(w) && prevSig == '(');
            case '.':
                return !(PRIMITIVES.contains(w) && src.startsWith("class", k + 1));
            default:
                return false;
        }
    }
}
