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
/** Overloads that differ only in a primitive or boxed functional return are chosen by what the lambda returns. */
import java.util.function.*;
public class LambdaOverloads {
    interface IntSource { int get(); }
    interface BoxSource { Integer get(); }
    static String pick(IntSource s) { return "int " + s.get(); }
    static String pick(BoxSource s) { return "boxed " + s.get(); }
    static int one() { return 1; }
    static Integer boxedOne() { return Integer.valueOf(1); }
    static Integer nothing() { return null; }
    int field = 5;
    public static void main(String[] args) {
        System.out.println(pick(() -> 3));
        System.out.println(pick(() -> one()));
        System.out.println(pick(() -> boxedOne()));
        System.out.println(pick(() -> { int x = one(); return x + 1; }));
        System.out.println(pick(() -> { Integer x = boxedOne(); return x; }));
        System.out.println(pick(() -> null == args ? 0 : args.length));
        System.out.println(pick(LambdaOverloads::one));
        System.out.println(pick(LambdaOverloads::boxedOne));
        System.out.println(pick(() -> nothing()) .length() > 0 ? "boxed null ok" : "");
        LambdaOverloads o = new LambdaOverloads();
        System.out.println(pick(() -> o.field));
        String s = "abc";
        System.out.println(pick(s::length));
    }
}
