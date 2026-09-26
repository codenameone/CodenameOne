/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Correctness coverage for the primitive long->long map (edge cases the benchmark never hits). */
public class DartLongMapTest {

    @Test
    public void putGetOverwrite() {
        DartLongMap m = new DartLongMap();
        m.putLong(1, 10);
        m.putLong(2, 20);
        assertEquals(10, m.getLongOr(1, -1));
        assertEquals(20, m.getLongOr(2, -1));
        assertEquals(-1, m.getLongOr(99, -1));
        assertEquals(2, m.length());
        m.putLong(1, 111); // overwrite must not grow size
        assertEquals(111, m.getLongOr(1, -1));
        assertEquals(2, m.length());
    }

    @Test
    public void bareIndexReturnsNullWhenAbsent() {
        DartLongMap m = new DartLongMap();
        m.putLong(5, 50);
        assertEquals(Long.valueOf(50), m.idxLong(5));
        assertNull(m.idxLong(6));
    }

    @Test
    public void containsAndRemove() {
        DartLongMap m = new DartLongMap();
        m.putLong(7, 70);
        assertTrue(m.containsKeyLong(7));
        assertFalse(m.containsKeyLong(8));
        assertEquals(70, m.removeLong(7));
        assertFalse(m.containsKeyLong(7));
        assertEquals(0, m.length());
        // re-add after remove (tombstone slot must be reusable)
        m.putLong(7, 700);
        assertEquals(700, m.getLongOr(7, -1));
        assertEquals(1, m.length());
    }

    @Test
    public void insertionOrderPreserved() {
        DartLongMap m = new DartLongMap();
        long[] order = {50, 3, 9, 1, 42, 7, 100, 2};
        for (long k : order) {
            m.putLong(k, k * 2);
        }
        List<Long> keys = new ArrayList<Long>();
        for (Long k : m.keySet()) {
            keys.add(k);
        }
        assertEquals(order.length, keys.size());
        for (int i = 0; i < order.length; i++) {
            assertEquals(Long.valueOf(order[i]), keys.get(i), "key order at " + i);
        }
        // removing the middle key keeps the rest in order
        m.removeLong(9);
        keys.clear();
        for (Long k : m.keySet()) {
            keys.add(k);
        }
        assertEquals(order.length - 1, keys.size());
        assertFalse(keys.contains(9L));
        assertEquals(Long.valueOf(50), keys.get(0));
        assertEquals(Long.valueOf(2), keys.get(keys.size() - 1));
    }

    @Test
    public void growthAndReadbackManyEntries() {
        DartLongMap m = new DartLongMap();
        int n = 5000;
        for (int i = 0; i < n; i++) {
            m.putLong(i, (long) i * 3 + 1);
        }
        assertEquals(n, m.length());
        long sum = 0;
        for (int i = 0; i < n; i++) {
            sum += m.getLongOr(i, 0);
        }
        long expect = 0;
        for (int i = 0; i < n; i++) {
            expect += (long) i * 3 + 1;
        }
        assertEquals(expect, sum);
    }

    @Test
    public void negativeAndZeroKeys() {
        DartLongMap m = new DartLongMap();
        m.putLong(0, 100);
        m.putLong(-1, 200);
        m.putLong(Long.MIN_VALUE, 300);
        m.putLong(Long.MAX_VALUE, 400);
        assertEquals(100, m.getLongOr(0, -999));
        assertEquals(200, m.getLongOr(-1, -999));
        assertEquals(300, m.getLongOr(Long.MIN_VALUE, -999));
        assertEquals(400, m.getLongOr(Long.MAX_VALUE, -999));
    }

    @Test
    public void mapInteropAndClear() {
        DartLongMap m = new DartLongMap();
        m.putLong(1, 10);
        m.putLong(2, 20);
        assertEquals(Long.valueOf(10), m.get(1L));
        assertEquals(2, m.size());
        int entries = 0;
        for (java.util.Map.Entry<Long, Long> e : m.entrySet()) {
            entries++;
            assertEquals(Long.valueOf(m.getLongOr(e.getKey(), -1)), e.getValue());
        }
        assertEquals(2, entries);
        m.clear();
        assertEquals(0, m.length());
        assertFalse(m.containsKeyLong(1));
        m.putLong(3, 30);
        assertEquals(30, m.getLongOr(3, -1));
    }
}
