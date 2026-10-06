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
/** A local assigned once on each path is effectively final (JLS 4.12.4), however many assignments it has. */
import java.util.function.*;
public class EffectivelyFinalPaths {
    static Supplier<String> branches(boolean b) {
        String s;
        if (b) {
            s = "then";
        } else {
            s = "else";
        }
        return () -> s;
    }
    static IntSupplier earlyReturn(int x) {
        int v;
        if (x < 0) {
            v = -1;
            return () -> v;
        }
        v = x * 2;
        return () -> v;
    }
    static IntSupplier switched(int k) {
        int v;
        switch (k) {
            case 1:
                v = 10;
                break;
            case 2:
                v = 20;
                break;
            default:
                v = 0;
        }
        return () -> v;
    }
    static IntSupplier arrows(int k) {
        final int[] box = {0};
        int v;
        switch (k) {
            case 1 -> v = 100;
            case 2 -> { box[0]++; v = 200; }
            default -> v = -100;
        }
        return () -> v + box[0];
    }
    static Supplier<String> conditional(boolean b) {
        String s;
        boolean ok = b ? (s = "yes") != null : (s = "no") != null;
        return () -> s + ok;
    }
    static Runnable inLoop(int n) {
        Runnable last = null;
        for (int i = 0; i < n; i++) {
            int sq;
            sq = i * i;
            last = () -> System.out.println("sq " + sq);
        }
        return last;
    }
    static Supplier<String> tryCatch(String in) {
        String r;
        try {
            Integer.parseInt(in);
        } catch (NumberFormatException e) {
            r = "bad " + in;
            return () -> r;
        }
        r = "good " + in;
        return () -> r;
    }
    public static void main(String[] args) {
        System.out.println(branches(true).get() + " " + branches(false).get());
        System.out.println(earlyReturn(-5).getAsInt() + " " + earlyReturn(5).getAsInt());
        System.out.println(switched(1).getAsInt() + " " + switched(2).getAsInt() + " " + switched(3).getAsInt());
        System.out.println(arrows(1).getAsInt() + " " + arrows(2).getAsInt() + " " + arrows(3).getAsInt());
        System.out.println(conditional(true).get() + " " + conditional(false).get());
        inLoop(4).run();
        System.out.println(tryCatch("12").get() + " " + tryCatch("x").get());
        new Object() {
            void anon() {
                int k;
                if (args.length > 100) {
                    k = 1;
                } else {
                    k = 2;
                }
                Runnable r = new Runnable() {
                    public void run() {
                        System.out.println("inner " + k);
                    }
                };
                r.run();
            }
        }.anon();
    }
}
