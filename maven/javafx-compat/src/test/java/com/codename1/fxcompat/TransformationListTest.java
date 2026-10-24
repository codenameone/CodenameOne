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
package com.codename1.fxcompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

public class TransformationListTest {

    private static List<Integer> ints(int... values) {
        List<Integer> result = new ArrayList<Integer>();
        for (int value : values) {
            result.add(Integer.valueOf(value));
        }
        return result;
    }

    @Test
    public void filteredListShowsMatchingElements() {
        ObservableList<Integer> source = FXCollections.observableArrayList(ints(1, 2, 3, 4, 5, 6));
        FilteredList<Integer> even = new FilteredList<Integer>(source, n -> n.intValue() % 2 == 0);
        assertEquals("[2, 4, 6]", even.toString());
        assertEquals(3, even.size());
        assertEquals(1, even.getSourceIndex(0));
        assertEquals(5, even.getSourceIndex(2));
        assertEquals(1, even.getViewIndex(3));
        assertTrue(even.getViewIndex(0) < 0);
        assertSame(source, even.getSource());
        assertTrue(even.isInTransformationChain(source));
        assertEquals(3, even.getSourceIndexFor(source, 1));
    }

    @Test
    public void filteredListPropagatesSourceChanges() {
        ObservableList<Integer> source = FXCollections.observableArrayList(ints(1, 2, 3, 4));
        FilteredList<Integer> even = source.filtered(n -> n.intValue() % 2 == 0);
        Probe.ListChanges<Integer> probe = new Probe.ListChanges<Integer>();
        even.addListener(probe);
        source.add(Integer.valueOf(6));
        assertEquals("[add[2,3] +[6]]", probe.take());
        source.add(Integer.valueOf(7));
        assertEquals("an element that does not match changes nothing", "[]", probe.take());
        source.add(0, Integer.valueOf(0));
        assertEquals("[add[0,1] +[0]]", probe.take());
        assertEquals("[0, 2, 4, 6]", even.toString());
        source.remove(Integer.valueOf(2));
        assertEquals("[rem[1] -[2]]", probe.take());
        source.remove(Integer.valueOf(1));
        assertEquals("[]", probe.take());
        source.set(0, Integer.valueOf(10));
        assertEquals("[rep[0,1] -[0] +[10]]", probe.take());
        source.set(0, Integer.valueOf(11));
        assertEquals("[rem[0] -[10]]", probe.take());
        source.set(0, Integer.valueOf(12));
        assertEquals("[add[0,1] +[12]]", probe.take());
        assertEquals("[12, 4, 6]", even.toString());
        source.clear();
        assertEquals("[rem[0] -[12, 4, 6]]", probe.take());
        assertEquals(0, even.size());
    }

    @Test
    public void filteredListFollowsASourceSort() {
        ObservableList<Integer> source = FXCollections.observableArrayList(ints(6, 1, 4, 3, 2));
        FilteredList<Integer> even = new FilteredList<Integer>(source, n -> n.intValue() % 2 == 0);
        assertEquals("[6, 4, 2]", even.toString());
        FXCollections.sort(source);
        assertEquals("[2, 4, 6]", even.toString());
        assertEquals(1, even.getSourceIndex(0));
        source.removeAll(Integer.valueOf(2), Integer.valueOf(6), Integer.valueOf(1));
        assertEquals("[4]", even.toString());
    }

    @Test
    public void filteredListPredicateChange() {
        ObservableList<Integer> source = FXCollections.observableArrayList(ints(1, 2, 3, 4));
        FilteredList<Integer> view = new FilteredList<Integer>(source);
        assertNull(view.getPredicate());
        assertEquals("no predicate shows everything", "[1, 2, 3, 4]", view.toString());
        Probe.ListChanges<Integer> probe = new Probe.ListChanges<Integer>();
        view.addListener(probe);
        view.setPredicate(n -> n.intValue() > 2);
        assertEquals("[3, 4]", view.toString());
        assertEquals(1, probe.events);
        view.predicateProperty().set(n -> n.intValue() < 2);
        assertEquals("[1]", view.toString());
        assertEquals(2, probe.events);
        view.setPredicate(null);
        assertEquals(4, view.size());
    }

    @Test
    public void filteredListIsReadOnly() {
        FilteredList<Integer> view = new FilteredList<Integer>(FXCollections.observableArrayList(ints(1)));
        try {
            view.add(Integer.valueOf(2));
            fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertEquals(1, view.size());
        }
    }

    @Test
    public void sortedListOrdersItsSource() {
        ObservableList<Integer> source = FXCollections.observableArrayList(ints(3, 1, 2));
        SortedList<Integer> sorted = new SortedList<Integer>(source, Comparator.<Integer>naturalOrder());
        assertEquals("[1, 2, 3]", sorted.toString());
        assertEquals("[3, 1, 2]", source.toString());
        assertEquals(1, sorted.getSourceIndex(0));
        assertEquals(0, sorted.getSourceIndex(2));
        assertEquals(2, sorted.getViewIndex(0));
        assertEquals(0, sorted.getViewIndex(1));
    }

    @Test
    public void sortedListPropagatesSourceChanges() {
        ObservableList<Integer> source = FXCollections.observableArrayList(ints(30, 10));
        SortedList<Integer> sorted = source.sorted();
        Probe.ListChanges<Integer> probe = new Probe.ListChanges<Integer>();
        sorted.addListener(probe);
        source.add(Integer.valueOf(20));
        assertEquals("[add[1,2] +[20]]", probe.take());
        assertEquals("[10, 20, 30]", sorted.toString());
        source.add(Integer.valueOf(5));
        assertEquals("[add[0,1] +[5]]", probe.take());
        source.remove(Integer.valueOf(20));
        assertEquals("[rem[2] -[20]]", probe.take());
        assertEquals("[5, 10, 30]", sorted.toString());
        source.set(0, Integer.valueOf(7));
        assertEquals("[5, 7, 10]", sorted.toString());
        assertEquals(1, probe.events - 3);
        probe.take();
        source.addAll(ints(1, 100));
        assertEquals("[1, 5, 7, 10, 100]", sorted.toString());
        assertEquals("[add[0,1] +[1], add[4,5] +[100]]", probe.take());
        source.clear();
        assertEquals("[rem[0] -[1, 5, 7, 10, 100]]", probe.take());
    }

    @Test
    public void sortedListIsStableForEqualElements() {
        ObservableList<String> source = FXCollections.observableArrayList("bb", "a", "cc", "d");
        SortedList<String> byLength = source.sorted(new Comparator<String>() {
            @Override
            public int compare(String first, String second) {
                return first.length() - second.length();
            }
        });
        assertEquals("[a, d, bb, cc]", byLength.toString());
        source.add("e");
        assertEquals("[a, d, e, bb, cc]", byLength.toString());
        source.add(0, "ff");
        assertEquals("[a, d, e, ff, bb, cc]", byLength.toString());
    }

    @Test
    public void sortedListComparatorChangeIsAPermutation() {
        ObservableList<Integer> source = FXCollections.observableArrayList(ints(2, 3, 1));
        SortedList<Integer> sorted = new SortedList<Integer>(source, Comparator.<Integer>naturalOrder());
        Probe.ListChanges<Integer> probe = new Probe.ListChanges<Integer>();
        sorted.addListener(probe);
        sorted.setComparator(Comparator.<Integer>reverseOrder());
        assertEquals("[3, 2, 1]", sorted.toString());
        assertEquals("[perm[0,3]{2,1,0}]", probe.take());
        sorted.setComparator(null);
        assertEquals("no comparator shows the source order", "[2, 3, 1]", sorted.toString());
        assertNull(sorted.getComparator());
        sorted.comparatorProperty().set(Comparator.<Integer>naturalOrder());
        assertEquals("[1, 2, 3]", sorted.toString());
    }

    @Test
    public void sortedOverFilteredFollowsRandomEdits() {
        ObservableList<Integer> source = FXCollections.observableArrayList();
        FilteredList<Integer> filtered = source.filtered(n -> n.intValue() % 3 != 0);
        SortedList<Integer> sorted = filtered.sorted();
        final List<Integer> mirror = new ArrayList<Integer>();
        sorted.addListener(new javafx.collections.ListChangeListener<Integer>() {
            @Override
            public void onChanged(Change<? extends Integer> c) {
                while (c.next()) {
                    if (c.wasPermutated()) {
                        List<Integer> before = new ArrayList<Integer>(mirror.subList(c.getFrom(), c.getTo()));
                        for (int i = c.getFrom(); i < c.getTo(); i++) {
                            mirror.set(c.getPermutation(i), before.get(i - c.getFrom()));
                        }
                    } else {
                        for (int i = 0; i < c.getRemovedSize(); i++) {
                            mirror.remove(c.getFrom());
                        }
                        mirror.addAll(c.getFrom(), c.getAddedSubList());
                    }
                }
            }
        });
        Random random = new Random(7);
        for (int step = 0; step < 400; step++) {
            int action = random.nextInt(6);
            if (action < 2 || source.isEmpty()) {
                source.add(random.nextInt(source.size() + 1), Integer.valueOf(random.nextInt(50)));
            } else if (action == 2) {
                source.remove(random.nextInt(source.size()));
            } else if (action == 3) {
                source.set(random.nextInt(source.size()), Integer.valueOf(random.nextInt(50)));
            } else if (action == 4) {
                source.removeAll(Integer.valueOf(random.nextInt(50)), Integer.valueOf(random.nextInt(50)));
            } else {
                FXCollections.shuffle(source, random);
            }
            List<Integer> expected = new ArrayList<Integer>();
            for (Integer value : source) {
                if (value.intValue() % 3 != 0) {
                    expected.add(value);
                }
            }
            assertEquals("filtered view after step " + step, expected, new ArrayList<Integer>(filtered));
            Collections.sort(expected);
            assertEquals("sorted view after step " + step, expected, new ArrayList<Integer>(sorted));
            assertEquals("replaying the reported changes after step " + step, expected, mirror);
        }
        assertFalse(sorted.isInTransformationChain(FXCollections.observableArrayList()));
        assertTrue(sorted.isInTransformationChain(source));
    }

    @Test
    public void replayingListChangesReproducesTheList() {
        final ObservableList<Integer> source = FXCollections.observableArrayList();
        final List<Integer> mirror = new ArrayList<Integer>();
        source.addListener(new javafx.collections.ListChangeListener<Integer>() {
            @Override
            public void onChanged(Change<? extends Integer> c) {
                while (c.next()) {
                    if (c.wasPermutated()) {
                        List<Integer> before = new ArrayList<Integer>(mirror.subList(c.getFrom(), c.getTo()));
                        for (int i = c.getFrom(); i < c.getTo(); i++) {
                            mirror.set(c.getPermutation(i), before.get(i - c.getFrom()));
                        }
                    } else if (!c.wasUpdated()) {
                        for (int i = 0; i < c.getRemovedSize(); i++) {
                            assertEquals(c.getRemoved().get(i), mirror.remove(c.getFrom()));
                        }
                        mirror.addAll(c.getFrom(), c.getAddedSubList());
                    }
                }
            }
        });
        Random random = new Random(11);
        for (int step = 0; step < 500; step++) {
            int action = random.nextInt(9);
            if (action < 2 || source.size() < 2) {
                source.addAll(random.nextInt(source.size() + 1), ints(random.nextInt(20), random.nextInt(20)));
            } else if (action == 2) {
                source.remove(random.nextInt(source.size()));
            } else if (action == 3) {
                source.set(random.nextInt(source.size()), Integer.valueOf(random.nextInt(20)));
            } else if (action == 4) {
                source.removeAll(ints(random.nextInt(20), random.nextInt(20), random.nextInt(20)));
            } else if (action == 5) {
                source.retainAll(ints(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, random.nextInt(20)));
            } else if (action == 6) {
                FXCollections.sort(source);
            } else if (action == 7) {
                int from = random.nextInt(source.size());
                source.subList(from, from + random.nextInt(source.size() - from)).clear();
            } else {
                final int limit = random.nextInt(20);
                source.removeIf(n -> n.intValue() > limit && n.intValue() % 2 == 0);
            }
            assertEquals("after step " + step, new ArrayList<Integer>(source), mirror);
        }
    }
}
