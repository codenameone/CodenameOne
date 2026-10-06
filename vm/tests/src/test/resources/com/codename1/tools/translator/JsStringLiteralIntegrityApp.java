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
public class JsStringLiteralIntegrityApp {
    public static int result;

    public static void main(String[] args) {
        int mask = 0;
        // The translator's text passes rewrite emitted JavaScript with regular
        // expressions, and a string literal used to sit inside that text where they
        // could reach it: a literal shaped like emitted code was rewritten with the
        // code around it. The expected values are hash codes computed on the JVM;
        // an int constant is not something those passes touch.
        String dup = "stack.p(locals[3]); stack.p(stack[stack.length - 1]);";
        String add = "stack.p(x); stack.p(y); { let b = stack.q(); let a = stack.q(); stack.p((a|0) + (b|0)); }";
        String push = "L[3] = foo; let S = []; S.p(a) ; S.p(b)";
        String register = "  s3 = a.b(c);\n  s3 = s3[\"f\"];";
        if (dup.hashCode() == -1253882875 && dup.length() == 53) mask |= 1;
        if (add.hashCode() == 345198824 && add.length() == 89) mask |= 2;
        if (push.hashCode() == -1245267737 && push.length() == 39) mask |= 4;
        if (register.hashCode() == 463976025 && register.length() == 30) mask |= 8;
        result = mask;
        System.exit(mask);
    }
}
