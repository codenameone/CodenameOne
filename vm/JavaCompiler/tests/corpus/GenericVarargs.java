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
/** A variable-arity call allocates the inferred array type, as javac does. */
import java.util.*;
public class GenericVarargs {
    @SafeVarargs
    static <T> T[] of(T... values) { return values; }
    @SafeVarargs
    static <T> List<T> list(T... values) { return Arrays.asList(values); }
    static class Box<T> {
        final T[] items;
        @SafeVarargs
        Box(T... items) { this.items = items; }
    }
    public static void main(String[] args) {
        String[] s = of("a", "b");
        System.out.println(s.getClass().getSimpleName() + " " + s.length + " " + s[1]);
        Integer[] i = of(1, 2, 3);
        System.out.println(i.getClass().getSimpleName() + " " + i[2]);
        Number[] n = GenericVarargs.<Number>of(1, 2.5);
        System.out.println(n.getClass().getSimpleName() + " " + n[1]);
        System.out.println(list("x", "y").get(1));
        Box<String> b = new Box<String>("p", "q");
        System.out.println(b.items.getClass().getSimpleName() + " " + b.items[0]);
        Object[] o = of();
        System.out.println(o.getClass().getSimpleName() + " " + o.length);
    }
}
