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

import java.util.ArrayList;
import java.util.List;

/**
 * Prints one RESULT line of a record's implicit equals, hashCode and toString,
 * entries separated by ';'. RecordMethodsTest runs it on the JDK and on
 * ParparVM and requires the two lines to match. A hashCode is printed only for
 * a record whose components hash by value: an enum hashes by identity, which
 * differs between any two runs, so the record holding one prints equals and
 * toString only.
 */
public class RecordMethodsApp {
    enum Size { SMALL, LARGE }

    record Prims(boolean z, byte b, short s, char c, int i, long l, float f, double d) { }

    record Point(double lat, double lng) { }

    record Trip(String name, Point from, Point to, List<String> stops) { }

    record Sized(Size size, String label) { }

    record Empty() { }

    private static StringBuilder out = new StringBuilder("RESULT=");
    private static boolean first = true;

    private static void add(String entry) {
        if (!first) {
            out.append(';');
        }
        first = false;
        out.append(entry);
    }

    private static void all(Object a, Object b) {
        add(String.valueOf(a.equals(b)));
        add(String.valueOf(a.hashCode()));
        add(String.valueOf(b.hashCode()));
        add(a.toString());
    }

    public static void main(String[] args) {
        Prims p1 = new Prims(true, (byte) -3, (short) 300, 'q', -7, 1L << 40, 1.5f, -2.25);
        Prims p2 = new Prims(true, (byte) -3, (short) 300, 'q', -7, 1L << 40, 1.5f, -2.25);
        all(p1, p2);
        all(p1, new Prims(false, (byte) -3, (short) 300, 'q', -7, 1L << 40, 1.5f, -2.25));
        all(p1, new Prims(true, (byte) -3, (short) 300, 'q', -7, 1L << 41, 1.5f, -2.25));

        // equals compares float and double components by their bits, as
        // Float.compare does: NaN equals NaN, and 0.0 differs from -0.0.
        all(new Point(Double.NaN, 1), new Point(Double.NaN, 1));
        all(new Point(0.0, 1), new Point(-0.0, 1));
        all(new Prims(false, (byte) 0, (short) 0, 'a', 0, 0, Float.NaN, 0),
                new Prims(false, (byte) 0, (short) 0, 'a', 0, 0, Float.NaN, 0));
        all(new Prims(false, (byte) 0, (short) 0, 'a', 0, 0, 0.0f, 0),
                new Prims(false, (byte) 0, (short) 0, 'a', 0, 0, -0.0f, 0));

        List<String> stops = new ArrayList<String>();
        stops.add("Main St");
        stops.add("Oak Ave");
        List<String> sameStops = new ArrayList<String>(stops);
        Trip t1 = new Trip("work", new Point(32.5, 34.75), new Point(-1, 2), stops);
        Trip t2 = new Trip("work", new Point(32.5, 34.75), new Point(-1, 2), sameStops);
        all(t1, t2);
        all(t1, new Trip("work", new Point(32.5, 34.75), new Point(-1, 3), stops));

        // Null components hash to 0 and print as null.
        Trip blank = new Trip(null, null, null, null);
        all(blank, new Trip(null, null, null, null));
        all(blank, t1);

        Sized s1 = new Sized(Size.LARGE, "x");
        add(String.valueOf(s1.equals(new Sized(Size.LARGE, "x"))));
        add(String.valueOf(s1.equals(new Sized(Size.SMALL, "x"))));
        add(String.valueOf(s1.equals(null)));
        add(String.valueOf(s1.equals("x")));
        add(s1.toString());

        all(new Empty(), new Empty());

        System.out.println(out.toString());
    }
}
