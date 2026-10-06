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
/** Most specific variable-arity overload, compared through the expanded formals (JLS 15.12.2.5). */
public class VarargsSpecific {
    static String m(Object a, Object... rest) { return "Object,Object... " + rest.length; }
    static String m(String... all) { return "String... " + all.length; }
    static String n(Number a, Integer... rest) { return "Number,Integer..."; }
    static String n(Integer... all) { return "Integer..."; }
    static String p(int a, long... rest) { return "int,long..."; }
    static String p(long... all) { return "long..."; }
    public static void main(String[] args) {
        System.out.println(m("a", "b"));
        System.out.println(m("a"));
        System.out.println(m(1, "b"));
        System.out.println(n(1, 2, 3));
        System.out.println(p(1, 2));
        System.out.println(p(1L, 2));
    }
}
