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
package com.codename1.compat.jdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Holds the [CopyOnWriteArraySet] shim against
/// `java.util.concurrent.CopyOnWriteArraySet`.
public class CopyOnWriteArraySetDifferentialTest {

    private static List<Integer> some(Random random) {
        List<Integer> out = new ArrayList<Integer>();
        for (int i = random.nextInt(5); i > 0; i--) {
            out.add(random.nextInt(8) == 0 ? null : Integer.valueOf(random.nextInt(12)));
        }
        return out;
    }

    @Test
    public void aRandomSequenceOfOperationsMatchesTheJdk() {
        Random random = new Random(20261009L);
        java.util.concurrent.CopyOnWriteArraySet<Integer> theirs =
                new java.util.concurrent.CopyOnWriteArraySet<Integer>();
        CopyOnWriteArraySet<Integer> mine = new CopyOnWriteArraySet<Integer>();
        for (int step = 0; step < 5000; step++) {
            Integer value = random.nextInt(10) == 0 ? null : Integer.valueOf(random.nextInt(12));
            List<Integer> several = some(random);
            switch (random.nextInt(10)) {
                case 0:
                case 1:
                case 2:
                    assertEquals(theirs.add(value), mine.add(value));
                    break;
                case 3:
                    assertEquals(theirs.remove(value), mine.remove(value));
                    break;
                case 4:
                    assertEquals(theirs.contains(value), mine.contains(value));
                    assertEquals(theirs.containsAll(several), mine.containsAll(several));
                    break;
                case 5:
                    assertEquals(theirs.addAll(several), mine.addAll(several));
                    break;
                case 6:
                    assertEquals(theirs.removeAll(several), mine.removeAll(several));
                    break;
                case 7:
                    assertEquals(theirs.retainAll(several), mine.retainAll(several));
                    break;
                case 8:
                    final int limit = random.nextInt(12);
                    assertEquals(theirs.removeIf(x -> x != null && x.intValue() < limit),
                            mine.removeIf(x -> x != null && x.intValue() < limit));
                    break;
                default:
                    if (random.nextInt(20) == 0) {
                        theirs.clear();
                        mine.clear();
                    }
                    break;
            }
            // In insertion order, which is what a copy-on-write set keeps.
            assertEquals(new ArrayList<Integer>(theirs), new ArrayList<Integer>(mine));
            assertEquals(theirs.size(), mine.size());
            assertEquals(theirs.isEmpty(), mine.isEmpty());
            assertEquals(theirs.toString(), mine.toString());
            assertEquals(theirs.hashCode(), mine.hashCode());
            assertEquals(Arrays.asList(theirs.toArray()), Arrays.asList(mine.toArray()));
            assertEquals(Arrays.asList(theirs.toArray(new Integer[0])), Arrays.asList(mine.toArray(new Integer[0])));
            assertTrue(mine.equals(new HashSet<Integer>(theirs)));
            assertTrue(new HashSet<Integer>(theirs).equals(mine));
        }
    }

    @Test
    public void aCopyKeepsTheFirstOfEachElementInOrder() {
        List<String> source = Arrays.asList("b", "a", "b", null, "c", "a", null);
        assertEquals(new ArrayList<String>(new java.util.concurrent.CopyOnWriteArraySet<String>(source)),
                new ArrayList<String>(new CopyOnWriteArraySet<String>(source)));
        assertEquals(Arrays.asList("b", "a", null, "c"),
                new ArrayList<String>(new CopyOnWriteArraySet<String>(source)));
    }

    @Test
    public void anIteratorSeesTheSetAsItWasWhenItWasCreated() {
        java.util.concurrent.CopyOnWriteArraySet<String> theirs =
                new java.util.concurrent.CopyOnWriteArraySet<String>(Arrays.asList("a", "b", "c"));
        CopyOnWriteArraySet<String> mine = new CopyOnWriteArraySet<String>(Arrays.asList("a", "b", "c"));
        List<String> expected = new ArrayList<String>();
        for (String s : theirs) {
            theirs.remove(s);
            theirs.add(s + s);
            expected.add(s);
        }
        List<String> seen = new ArrayList<String>();
        for (String s : mine) {
            mine.remove(s);
            mine.add(s + s);
            seen.add(s);
        }
        assertEquals(expected, seen);
        assertEquals(Arrays.asList("a", "b", "c"), seen);
        assertEquals(new ArrayList<String>(theirs), new ArrayList<String>(mine));

        final List<String> each = new ArrayList<String>();
        mine.forEach(s -> {
            mine.clear();
            each.add(s);
        });
        assertEquals(Arrays.asList("aa", "bb", "cc"), each);
        assertTrue(mine.isEmpty());
    }

    @Test
    public void anIteratorIsReadOnly() {
        CopyOnWriteArraySet<String> mine = new CopyOnWriteArraySet<String>(Arrays.asList("a"));
        Iterator<String> it = mine.iterator();
        it.next();
        try {
            it.remove();
            fail();
        } catch (UnsupportedOperationException expected) {
            // As the JDK's.
        }
        Iterator<String> theirs = new java.util.concurrent.CopyOnWriteArraySet<String>(Arrays.asList("a")).iterator();
        theirs.next();
        try {
            theirs.remove();
            fail();
        } catch (UnsupportedOperationException expected) {
            // The reference.
        }
        assertFalse(mine.isEmpty());
    }
}
