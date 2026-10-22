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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import org.junit.Test;

import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ModifiableObservableListBase;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.util.Callback;

public class ObservableListTest {

    private ObservableList<String> list;
    private Probe.ListChanges<String> probe;

    private void start(String... items) {
        list = FXCollections.observableArrayList(items);
        probe = new Probe.ListChanges<String>();
        list.addListener(probe);
    }

    @Test
    public void addReportsTheAddedRange() {
        start();
        assertTrue(list.add("a"));
        assertEquals("[add[0,1] +[a]]", probe.take());
        list.add(0, "b");
        assertEquals("[add[0,1] +[b]]", probe.take());
        list.addAll("c", "d");
        assertEquals("[add[2,4] +[c, d]]", probe.take());
        list.addAll(1, Arrays.asList("x", "y"));
        assertEquals("[add[1,3] +[x, y]]", probe.take());
        assertEquals("[b, x, y, a, c, d]", list.toString());
        assertEquals(4, probe.events);
        assertFalse(list.addAll(new ArrayList<String>()));
        assertEquals("an empty addition is not reported", 4, probe.events);
    }

    @Test
    public void removeReportsTheRemovedElements() {
        start("a", "b", "c", "d");
        assertEquals("b", list.remove(1));
        assertEquals("[rem[1] -[b]]", probe.take());
        assertTrue(list.remove("d"));
        assertEquals("[rem[2] -[d]]", probe.take());
        assertFalse(list.remove("missing"));
        assertEquals("[]", probe.take());
        list.clear();
        assertEquals("[rem[0] -[a, c]]", probe.take());
        list.clear();
        assertEquals(3, probe.events);
    }

    @Test
    public void removeRange() {
        start("a", "b", "c", "d", "e");
        list.remove(1, 3);
        assertEquals("[rem[1] -[b, c]]", probe.take());
        assertEquals("[a, d, e]", list.toString());
        list.remove(1, 1);
        assertEquals("[]", probe.take());
    }

    @Test
    public void setReportsAReplacement() {
        start("a", "b", "c");
        assertEquals("b", list.set(1, "B"));
        assertEquals("[rep[1,2] -[b] +[B]]", probe.take());
    }

    @Test
    public void setAllReplacesEverything() {
        start("a", "b");
        list.setAll("x", "y", "z");
        assertEquals("[rep[0,3] -[a, b] +[x, y, z]]", probe.take());
        list.setAll(Arrays.asList("q"));
        assertEquals("[rep[0,1] -[x, y, z] +[q]]", probe.take());
        list.setAll(new ArrayList<String>());
        assertEquals("[rem[0] -[q]]", probe.take());
        list.setAll("n");
        assertEquals("[add[0,1] +[n]]", probe.take());
    }

    @Test
    public void removeAllReportsEveryRunInOneEvent() {
        start("a", "b", "c", "d", "e");
        assertTrue(list.removeAll("a", "c", "d"));
        assertEquals(1, probe.events);
        assertEquals("[rem[0] -[a], rem[1] -[c, d]]", probe.take());
        assertEquals("[b, e]", list.toString());
        assertFalse(list.removeAll(Arrays.asList("zz")));
        assertEquals(1, probe.events);
    }

    @Test
    public void retainAllReportsEveryRunInOneEvent() {
        start("a", "b", "c", "d", "e");
        assertTrue(list.retainAll("b", "e"));
        assertEquals(1, probe.events);
        assertEquals("[rem[0] -[a], rem[1] -[c, d]]", probe.take());
        assertEquals("[b, e]", list.toString());
        assertFalse(list.retainAll(Arrays.asList("b", "e")));
        assertEquals(1, probe.events);
    }

    @Test
    public void removeIfReportsEachRemoval() {
        start("a", "bb", "c", "dd");
        assertTrue(list.removeIf(s -> s.length() == 2));
        assertEquals("removeIf goes through the iterator, as in JavaFX", 2, probe.events);
        assertEquals("[rem[1] -[bb], rem[2] -[dd]]", probe.take());
    }

    @Test
    public void sortReportsAPermutation() {
        start("c", "a", "b");
        FXCollections.sort(list);
        assertEquals("[a, b, c]", list.toString());
        assertEquals("entry i is where the element at i went", "[perm[0,3]{2,0,1}]", probe.take());
        list.sort(Comparator.<String>reverseOrder());
        assertEquals("[c, b, a]", list.toString());
        assertEquals("[perm[0,3]{2,1,0}]", probe.take());
        FXCollections.sort(list, Comparator.<String>reverseOrder());
        assertEquals("an already sorted list reports nothing", "[]", probe.take());
    }

    @Test
    public void reverseShuffleRotateFillReplace() {
        start("a", "b", "c");
        FXCollections.reverse(list);
        assertEquals("[c, b, a]", list.toString());
        assertEquals(1, probe.events);
        FXCollections.rotate(list, 1);
        assertEquals("[a, c, b]", list.toString());
        FXCollections.shuffle(list, new Random(42));
        List<String> sorted = new ArrayList<String>(list);
        Collections.sort(sorted);
        assertEquals("[a, b, c]", sorted.toString());
        assertTrue(FXCollections.replaceAll(list, "a", "A"));
        assertTrue(list.contains("A"));
        assertFalse(FXCollections.replaceAll(list, "nothing", "N"));
        FXCollections.fill(list, "z");
        assertEquals("[z, z, z]", list.toString());
        FXCollections.copy(list, Arrays.asList("1", "2"));
        assertEquals("[1, 2, z]", list.toString());
        assertTrue(probe.events >= 5);
    }

    @Test
    public void subListEditsAreReportedOnTheList() {
        start("a", "b", "c", "d");
        List<String> middle = list.subList(1, 3);
        assertEquals("[b, c]", middle.toString());
        middle.clear();
        assertEquals("[a, d]", list.toString());
        assertEquals("[rem[1] -[b, c]]", probe.take());
        List<String> tail = list.subList(1, 2);
        tail.add("e");
        assertEquals("[a, d, e]", list.toString());
        assertEquals("[add[2,3] +[e]]", probe.take());
        tail.set(0, "D");
        assertEquals("[rep[1,2] -[d] +[D]]", probe.take());
        tail.remove("e");
        assertEquals("[rem[2] -[e]]", probe.take());
    }

    @Test
    public void iteratorEditsAreReported() {
        start("a", "b", "c");
        Iterator<String> it = list.iterator();
        it.next();
        it.remove();
        assertEquals("[rem[0] -[a]]", probe.take());
        ListIterator<String> li = list.listIterator();
        li.next();
        li.set("B");
        assertEquals("[rep[0,1] -[b] +[B]]", probe.take());
        li.add("x");
        assertEquals("[add[1,2] +[x]]", probe.take());
        assertEquals("[B, x, c]", list.toString());
    }

    @Test
    public void changeAccessorsFollowTheProtocol() {
        list = FXCollections.observableArrayList("a", "b", "c");
        final int[] checked = new int[1];
        list.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                assertSame(list, c.getList());
                try {
                    c.getFrom();
                    fail("reading before next() must fail");
                } catch (IllegalStateException expected) {
                    checked[0]++;
                }
                assertTrue(c.next());
                assertTrue(c.wasReplaced());
                assertTrue(c.wasAdded());
                assertTrue(c.wasRemoved());
                assertFalse(c.wasPermutated());
                assertFalse(c.wasUpdated());
                assertEquals(1, c.getFrom());
                assertEquals(2, c.getTo());
                assertEquals(1, c.getAddedSize());
                assertEquals(1, c.getRemovedSize());
                assertEquals("[B]", c.getAddedSubList().toString());
                assertEquals("[b]", c.getRemoved().toString());
                assertFalse(c.next());
                c.reset();
                assertTrue(c.next());
                assertEquals(1, c.getFrom());
                checked[0]++;
            }
        });
        list.set(1, "B");
        assertEquals(2, checked[0]);
    }

    @Test
    public void everyListenerSeesTheWholeChange() {
        start("a");
        Probe.ListChanges<String> second = new Probe.ListChanges<String>();
        list.addListener(second);
        list.addAll("b", "c");
        assertEquals("[add[1,3] +[b, c]]", probe.take());
        assertEquals("[add[1,3] +[b, c]]", second.take());
        list.removeListener(second);
        list.add("d");
        assertEquals(1, second.events);
    }

    @Test
    public void invalidationListenerIsToldOfEveryChange() {
        start("a");
        Probe.Invalidations invalidations = new Probe.Invalidations();
        list.addListener(invalidations);
        list.add("b");
        list.add("c");
        assertEquals("a list has no value to read, so it is never lazy", 2, invalidations.count);
        assertSame(list, invalidations.last);
        list.removeListener(invalidations);
        list.add("d");
        assertEquals(2, invalidations.count);
    }

    @Test
    public void extractorReportsUpdates() {
        ObservableList<IntegerProperty> items = FXCollections
                .observableArrayList(new Callback<IntegerProperty, Observable[]>() {
                    @Override
                    public Observable[] call(IntegerProperty item) {
                        return new Observable[] {item};
                    }
                });
        IntegerProperty first = new SimpleIntegerProperty(1);
        IntegerProperty second = new SimpleIntegerProperty(2);
        items.add(first);
        items.add(second);
        Probe.ListChanges<IntegerProperty> updates = new Probe.ListChanges<IntegerProperty>();
        items.addListener(updates);
        second.set(20);
        assertEquals("[upd[1,2]]", updates.take());
        first.set(10);
        assertEquals("[upd[0,1]]", updates.take());
        items.remove(first);
        updates.take();
        first.get();
        first.set(11);
        assertEquals("a removed element is no longer watched", "[]", updates.take());
        second.get();
        second.set(21);
        assertEquals("[upd[0,1]]", updates.take());
    }

    @Test
    public void observableListWrapsTheBackingList() {
        List<String> backing = new ArrayList<String>();
        backing.add("a");
        ObservableList<String> wrapped = FXCollections.observableList(backing);
        wrapped.add("b");
        assertEquals("[a, b]", backing.toString());
        assertEquals(2, wrapped.size());
        ObservableList<String> copy = FXCollections.observableArrayList(backing);
        copy.add("c");
        assertEquals("a copy does not write through", 2, backing.size());
    }

    @Test
    public void unmodifiableViewForwardsChangesAndRejectsEdits() {
        start("a");
        ObservableList<String> view = FXCollections.unmodifiableObservableList(list);
        Probe.ListChanges<String> viewProbe = new Probe.ListChanges<String>();
        view.addListener(viewProbe);
        list.add("b");
        assertEquals("[add[1,2] +[b]]", viewProbe.take());
        assertEquals("[a, b]", view.toString());
        try {
            view.add("c");
            fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertEquals(2, view.size());
        }
        try {
            view.remove(0);
            fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertEquals(2, list.size());
        }
    }

    @Test
    public void emptyAndSingletonLists() {
        ObservableList<String> empty = FXCollections.emptyObservableList();
        assertEquals(0, empty.size());
        try {
            empty.add("a");
            fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertTrue(empty.isEmpty());
        }
        ObservableList<String> one = FXCollections.singletonObservableList("x");
        assertEquals(1, one.size());
        assertEquals("x", one.get(0));
        try {
            one.set(0, "y");
            fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertEquals("x", one.get(0));
        }
        ObservableList<String> joined = FXCollections.concat(one, FXCollections.observableArrayList("y", "z"));
        assertEquals("[x, y, z]", joined.toString());
    }

    @Test
    public void weakListChangeListenerForwards() {
        start();
        Probe.ListChanges<String> target = new Probe.ListChanges<String>();
        WeakListChangeListener<String> weak = new WeakListChangeListener<String>(target);
        list.addListener(weak);
        list.add("a");
        assertEquals("[add[0,1] +[a]]", target.take());
        assertFalse(weak.wasGarbageCollected());
    }

    @Test
    public void modifiableBaseDerivesEverythingFromFourMethods() {
        final List<String> store = new ArrayList<String>();
        ObservableList<String> custom = new ModifiableObservableListBase<String>() {
            @Override
            public String get(int index) {
                return store.get(index);
            }

            @Override
            public int size() {
                return store.size();
            }

            @Override
            protected void doAdd(int index, String element) {
                store.add(index, element);
            }

            @Override
            protected String doSet(int index, String element) {
                return store.set(index, element);
            }

            @Override
            protected String doRemove(int index) {
                return store.remove(index);
            }
        };
        Probe.ListChanges<String> changes = new Probe.ListChanges<String>();
        custom.addListener(changes);
        custom.addAll("a", "b", "c");
        assertEquals("[add[0,3] +[a, b, c]]", changes.take());
        custom.set(0, "A");
        assertEquals("[rep[0,1] -[a] +[A]]", changes.take());
        custom.remove("b");
        assertEquals("[rem[1] -[b]]", changes.take());
        custom.setAll("x");
        assertEquals("[rep[0,1] -[A, c] +[x]]", changes.take());
        custom.retainAll("none");
        assertEquals("[rem[0] -[x]]", changes.take());
        assertEquals("[]", store.toString());
    }

    @Test
    public void listPropertyTracksItsList() {
        javafx.beans.property.ListProperty<String> property = new javafx.beans.property.SimpleListProperty<String>(
                this, "items", FXCollections.<String>observableArrayList("a"));
        Probe.ListChanges<String> content = new Probe.ListChanges<String>();
        Probe.Invalidations invalidations = new Probe.Invalidations();
        property.addListener(content);
        property.addListener(invalidations);
        assertEquals(1, property.getSize());
        assertEquals("items", property.getName());
        javafx.beans.property.ReadOnlyIntegerProperty size = property.sizeProperty();
        javafx.beans.property.ReadOnlyBooleanProperty empty = property.emptyProperty();
        Probe.Values<Number> sizes = new Probe.Values<Number>();
        size.addListener(sizes);
        property.add("b");
        assertEquals("[add[1,2] +[b]]", content.take());
        assertEquals(1, invalidations.count);
        assertEquals("[1->2]", sizes.log.toString());
        assertFalse(empty.get());
        ObservableList<String> other = FXCollections.observableArrayList("x", "y", "z");
        property.set(other);
        assertEquals("[rep[0,3] -[a, b] +[x, y, z]]", content.take());
        assertSame(other, property.get());
        assertEquals(3, size.get());
        other.remove("y");
        assertEquals("[rem[1] -[y]]", content.take());
        assertEquals("[x, z]", property.toString().substring(property.toString().indexOf("value: ") + 7,
                property.toString().length() - 1));
        assertTrue(property.isEqualTo(FXCollections.observableArrayList("x", "z")).get());
        assertEquals("z", property.valueAt(1).get());
        property.set(null);
        assertEquals("[rem[0] -[x, z]]", content.take());
        assertTrue(empty.get());
        assertEquals(0, property.size());
        assertTrue(property.isNull().get());
        javafx.beans.property.ListProperty<String> bound = new javafx.beans.property.SimpleListProperty<String>();
        bound.bind(property);
        property.set(other);
        assertSame(other, bound.get());
        try {
            bound.set(null);
            fail("expected a RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().endsWith("A bound value cannot be set."));
        }
        bound.unbind();
        bound.set(null);
        assertEquals(0, bound.size());
    }
}
