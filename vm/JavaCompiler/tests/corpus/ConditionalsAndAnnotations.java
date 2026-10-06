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
/** Boxed numeric conditionals, an inner-class diamond keeping its outer type, and annotations javac accepts. */
import java.util.*;
import java.util.function.*;
public class ConditionalsAndAnnotations {
    static class GN<T> {
        T base;
        class I<U> {
            final T t;
            final U u;
            I(T t, U u) { this.t = t; this.u = u; }
            String show() { return t + "/" + u + "/" + base; }
        }
    }
    @FunctionalInterface
    interface Op { int apply(int a); }
    record Point(int x, int y) {
        @Override
        public int x() { return x * 10; }
    }
    enum Color {
        RED { @Override public String toString() { return "r"; } }, GREEN
    }
    @SuppressWarnings("unchecked")
    @Deprecated
    static <T> T pick(boolean flag, T a, T b) { return flag ? a : b; }
    public static void main(String[] args) {
        boolean flag = args.length == 0;
        Number n = flag ? Integer.valueOf(1) : Double.valueOf(2);
        System.out.println(n + " " + n.getClass().getSimpleName());
        Object o = !flag ? Long.valueOf(3) : Integer.valueOf(4);
        System.out.println(o + " " + o.getClass().getSimpleName());
        Comparable<?> c = flag ? Integer.valueOf(5) : "five";
        System.out.println(c + " " + c.getClass().getSimpleName());
        GN<String> g = new GN<>();
        g.base = "b";
        GN<String>.I<Integer> i = g.new I<>("x", 2);
        System.out.println(i.show());
        Op twice = a -> a * 2;
        System.out.println(twice.apply(21) + " " + new Point(1, 2).x() + " " + Color.RED + Color.GREEN);
        Runnable r = new Runnable() {
            @Override
            public void run() { System.out.println("ran " + pick(flag, "a", "b")); }
        };
        r.run();
        final @SuppressWarnings("unused") int unused = 0;
    }
}
