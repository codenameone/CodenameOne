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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CollectionsTest {

    @Test
    public void listBasics() {
        DartList<String> l = DartList.of("a", "b", "c");
        assertEquals(3, l.length());
        assertEquals("a", l.idx(0));
        assertEquals("c", l.last());
        assertThrows(RangeError.class, () -> l.idx(3));
        assertThrows(RangeError.class, () -> l.idx(-1));
        assertEquals("[a, b, c]", l.toString());
    }

    @Test
    public void listMapWhereAreLazyButCorrect() {
        DartList<Long> l = DartList.of(1L, 2L, 3L, 4L);
        assertEquals("[2, 4]", l.where(v -> v % 2 == 0).toList().toString());
        assertEquals("2, 4, 6, 8", l.map(v -> v * 2).join(", "));
    }

    @Test
    public void fixedLengthListRejectsGrowth() {
        DartList<Long> l = DartList.filled(2, 0L);
        assertThrows(UnsupportedError.class, () -> l.add(1L));
        l.idxSet(1, 5L);
        assertEquals("[0, 5]", l.toString());
    }

    @Test
    public void listInterOpsWithJavaUtil() {
        DartList<String> l = DartList.of("x", "y");
        java.util.List<String> asJava = l;
        assertEquals(2, asJava.size());
        assertTrue(asJava.contains("y"));
    }

    @Test
    public void mapPreservesInsertionOrder() {
        DartMap<String, Long> m = DartMap.of("z", 1L, "a", 2L, "m", 3L);
        assertEquals("{z: 1, a: 2, m: 3}", m.toString());
        assertEquals("z, a, m", m.keys().join(", "));
    }

    @Test
    public void stringHelpers() {
        assertEquals("ababab", DString.repeat("ab", 3));
        assertEquals("  x", DString.padLeft("x", 3));
        assertEquals("[a, b]", DString.split("a-b", "-").toString());
        assertEquals(42L, DString.parseInt(" 42 "));
        assertThrows(FormatException.class, () -> DString.parseInt("nope"));
    }
}
