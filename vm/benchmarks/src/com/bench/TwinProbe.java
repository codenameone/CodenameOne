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
package com.bench;

/**
 * A String whose characters live INSIDE it must be indistinguishable from one
 * backed by an array.
 *
 * The VM gives a fused String a twin clazz struct -- same classId, name, vtable
 * and type-test row as java.lang.String, different address -- so the class word
 * can carry the Latin-1/UTF-16 coder for free and the payload needs no header of
 * its own. Everything that decides behaviour keys on the class ID, so the twin
 * is supposed to be a java.lang.String in every way a program can observe.
 *
 * "Supposed to" is what this pins. Each answer below was a real failure mode
 * during the change: an unregistered twin made the collector stop believing such
 * objects were objects at all, and a twin used before its lazy copy ran had a
 * null vtable, so the first virtual call died in vtable[n] off NULL. Both built
 * cleanly and neither is visible by reading the code.
 */
public class TwinProbe {
    private static String describe(Object o) {
        return "val=" + o
            + " String=" + (o instanceof String)
            + " CharSequence=" + (o instanceof CharSequence)
            + " Comparable=" + (o instanceof Comparable)
            + " cls=" + o.getClass().getName()
            + " sameCls=" + (o.getClass() == String.class)
            + " len=" + ((String) o).length()
            + " hash=" + o.hashCode();
    }

    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append("s").append(7);
        String fused = sb.toString();                    // inline via toString
        String concat = "s" + 7;                         // inline via concat
        String sub = "xxs7yy".substring(2, 4);           // inline via substring
        String heap = new String(new char[]{'s', '7'});  // ordinary array-backed
        String wide = new StringBuilder().append("s").append((char) 0x4e2d).toString();

        System.out.println("fused  " + describe(fused));
        System.out.println("concat " + describe(concat));
        System.out.println("sub    " + describe(sub));
        System.out.println("heap   " + describe(heap));
        System.out.println("wide   " + describe(wide));

        // Cross-representation equality and ordering must not notice the difference.
        System.out.println("eq " + fused.equals(concat) + fused.equals(sub) + fused.equals(heap)
            + heap.equals(fused) + concat.equals(heap));
        System.out.println("cmp " + fused.compareTo(heap) + " " + heap.compareTo(concat));
        System.out.println("hashEq " + (fused.hashCode() == heap.hashCode()));

        // Reads that must work without an array to hand back.
        System.out.println("chars " + new String(fused.toCharArray())
            + " " + fused.charAt(1) + fused.indexOf('7') + fused.substring(1)
            + " " + fused.replace('7', '8') + " " + fused.toUpperCase());
        char[] into = new char[4];
        fused.getChars(0, 2, into, 1);
        System.out.println("getChars " + new String(into, 1, 2));
        System.out.println("bytes " + fused.getBytes().length + wide.getBytes().length);

        // Copying constructors take an inline source and must match its coder.
        System.out.println("copy " + new String(fused) + new String(wide));

        // Survive a collection: the twin has to be a registered, markable class.
        java.util.ArrayList<String> keep = new java.util.ArrayList<String>();
        for (int i = 0; i < 20000; i++) {
            keep.add(("k" + i).substring(0, 2));
            if ((i & 1023) == 0) { System.gc(); }
        }
        int acc = 0;
        for (int i = 0; i < keep.size(); i++) { acc += keep.get(i).hashCode(); }
        System.out.println("survived " + keep.size() + " acc=" + acc);
        System.out.println("DONE");
    }
}
