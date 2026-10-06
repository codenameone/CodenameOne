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
import java.util.*;
/** Generic values unboxed then widened: the value is an Integer, not a Long. */
public class GenericUnboxing {
    static <T> T id(T t) { return t; }
    public static void main(String[] a) {
        Map<Integer, Integer> m = new TreeMap<Integer, Integer>();
        m.put(3, 4);
        for (Map.Entry<Integer, Integer> r : m.entrySet()) {
            long[] x = new long[]{ r.getKey(), r.getValue(), 0, -1 };
            long y = r.getKey();
            double d = r.getValue();
            System.out.println(x[0] + " " + x[1] + " " + y + " " + d);
        }
        List<Short> s = new ArrayList<Short>();
        s.add((short) 5);
        int i = s.get(0);
        long l = id(Integer.valueOf(7));
        System.out.println(i + " " + l);
    }
}
