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
/** A generic single-element overload beside a variable-arity one: an array argument still binds to the array overload. */
import java.util.stream.*;
public class StreamOverloads {
    static <T> String which(T value) { return "single"; }
    @SafeVarargs
    static <T> String which(T... values) { return "varargs " + values.length; }
    public static void main(String[] args) {
        Integer[] boxed = {1, 2, 3};
        System.out.println(Stream.of(boxed).count() + " " + Stream.of(7).count() + " " + Stream.of(1, 2).count());
        System.out.println(which(boxed) + " | " + which(5) + " | " + which(1, 2) + " | " + which());
        int[] prims = {4, 5};
        System.out.println(Stream.of(prims).count() + " " + which(prims));
        Object o = boxed;
        System.out.println(which(o) + " " + Stream.of(o).count());
    }
}
