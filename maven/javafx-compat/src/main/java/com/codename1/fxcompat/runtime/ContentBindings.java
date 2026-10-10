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
package com.codename1.fxcompat.runtime;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.beans.WeakListener;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

/// The bindings that make one collection mirror another.
///
/// A binding is a change listener of the observed collection that replays
/// each change on the mirror, which it holds weakly: a mirror nobody uses is
/// collected, and its binding then removes itself. Bindings of the same
/// mirror are equal, which is how unbinding finds the listener to remove.
public final class ContentBindings {

    private ContentBindings() {
    }

    private static void check(Object first, Object second) {
        if (first == null || second == null) {
            throw new NullPointerException("Both parameters must be specified.");
        }
        if (first == second) {
            throw new IllegalArgumentException("Cannot bind object to itself");
        }
    }

    /// Replays a list change on another list holding the same content.
    static <E> void replay(ListChangeListener.Change<? extends E> change, List<E> target) {
        while (change.next()) {
            int from = change.getFrom();
            int to = change.getTo();
            if (change.wasPermutated()) {
                target.subList(from, to).clear();
                target.addAll(from, new ArrayList<E>(change.getList().subList(from, to)));
            } else {
                if (change.wasRemoved()) {
                    target.subList(from, from + change.getRemovedSize()).clear();
                }
                if (change.wasAdded()) {
                    target.addAll(from, new ArrayList<E>(change.getAddedSubList()));
                }
            }
        }
    }

    /// Makes a list mirror an observable list.
    public static <E> void bind(List<E> list1, ObservableList<? extends E> list2) {
        check(list1, list2);
        ListContent<E> binding = new ListContent<E>(list1);
        if (list1 instanceof ObservableList) {
            ((ObservableList<E>) list1).setAll(list2);
        } else {
            list1.clear();
            list1.addAll(list2);
        }
        list2.removeListener(binding);
        list2.addListener(binding);
    }

    /// Makes a set mirror an observable set.
    public static <E> void bind(Set<E> set1, ObservableSet<? extends E> set2) {
        check(set1, set2);
        SetContent<E> binding = new SetContent<E>(set1);
        set1.clear();
        set1.addAll(set2);
        set2.removeListener(binding);
        set2.addListener(binding);
    }

    /// Makes a map mirror an observable map.
    public static <K, V> void bind(Map<K, V> map1, ObservableMap<? extends K, ? extends V> map2) {
        check(map1, map2);
        MapContent<K, V> binding = new MapContent<K, V>(map1);
        map1.clear();
        map1.putAll(map2);
        map2.removeListener(binding);
        map2.addListener(binding);
    }

    /// Removes the binding that makes the first object mirror the second.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void unbind(Object obj1, Object obj2) {
        check(obj1, obj2);
        if (obj1 instanceof List && obj2 instanceof ObservableList) {
            ((ObservableList) obj2).removeListener(new ListContent((List) obj1));
        } else if (obj1 instanceof Set && obj2 instanceof ObservableSet) {
            ((ObservableSet) obj2).removeListener(new SetContent((Set) obj1));
        } else if (obj1 instanceof Map && obj2 instanceof ObservableMap) {
            ((ObservableMap) obj2).removeListener(new MapContent((Map) obj1));
        }
    }

    /// Keeps two observable lists equal, starting with the content of the
    /// second.
    public static <E> void bindBidirectional(ObservableList<E> list1, ObservableList<E> list2) {
        check(list1, list2);
        BothLists<E> binding = new BothLists<E>(list1, list2);
        list1.setAll(list2);
        list1.addListener(binding);
        list2.addListener(binding);
    }

    /// Removes the binding that keeps two objects equal.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void unbindBidirectional(Object obj1, Object obj2) {
        check(obj1, obj2);
        if (obj1 instanceof ObservableList && obj2 instanceof ObservableList) {
            BothLists probe = new BothLists((ObservableList) obj1, (ObservableList) obj2);
            ((ObservableList) obj1).removeListener(probe);
            ((ObservableList) obj2).removeListener(probe);
        }
    }

    /// Mirrors an observable list into a list.
    private static final class ListContent<E> implements ListChangeListener<E>, WeakListener {
        private final WeakReference ref;

        ListContent(List<E> target) {
            this.ref = new WeakReference(target);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onChanged(Change<? extends E> change) {
            Object target = ref.get();
            if (target instanceof List) {
                replay(change, (List<E>) target);
            } else {
                change.getList().removeListener(this);
            }
        }

        @Override
        public boolean wasGarbageCollected() {
            return ref.get() == null;
        }

        @Override
        public int hashCode() {
            Object target = ref.get();
            return target == null ? 0 : System.identityHashCode(target);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            Object target = ref.get();
            return target != null && other instanceof ListContent && ((ListContent<?>) other).ref.get() == target;
        }
    }

    /// Mirrors an observable set into a set.
    private static final class SetContent<E> implements SetChangeListener<E>, WeakListener {
        private final WeakReference ref;

        SetContent(Set<E> target) {
            this.ref = new WeakReference(target);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onChanged(Change<? extends E> change) {
            Object target = ref.get();
            if (!(target instanceof Set)) {
                change.getSet().removeListener(this);
            } else if (change.wasRemoved()) {
                ((Set<E>) target).remove(change.getElementRemoved());
            } else {
                ((Set<E>) target).add(change.getElementAdded());
            }
        }

        @Override
        public boolean wasGarbageCollected() {
            return ref.get() == null;
        }

        @Override
        public int hashCode() {
            Object target = ref.get();
            return target == null ? 0 : System.identityHashCode(target);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            Object target = ref.get();
            return target != null && other instanceof SetContent && ((SetContent<?>) other).ref.get() == target;
        }
    }

    /// Mirrors an observable map into a map.
    private static final class MapContent<K, V> implements MapChangeListener<K, V>, WeakListener {
        private final WeakReference ref;

        MapContent(Map<K, V> target) {
            this.ref = new WeakReference(target);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onChanged(Change<? extends K, ? extends V> change) {
            Object target = ref.get();
            if (!(target instanceof Map)) {
                change.getMap().removeListener(this);
            } else if (change.wasAdded()) {
                ((Map<K, V>) target).put(change.getKey(), change.getValueAdded());
            } else {
                ((Map<K, V>) target).remove(change.getKey());
            }
        }

        @Override
        public boolean wasGarbageCollected() {
            return ref.get() == null;
        }

        @Override
        public int hashCode() {
            Object target = ref.get();
            return target == null ? 0 : System.identityHashCode(target);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            Object target = ref.get();
            return target != null && other instanceof MapContent && ((MapContent<?, ?>) other).ref.get() == target;
        }
    }

    /// Keeps two observable lists equal, replaying the changes of each on
    /// the other and ignoring the echo.
    private static final class BothLists<E> implements ListChangeListener<E>, WeakListener {
        private final WeakReference first;
        private final WeakReference second;
        private final int hash;
        private boolean updating;

        BothLists(ObservableList<E> first, ObservableList<E> second) {
            this.first = new WeakReference(first);
            this.second = new WeakReference(second);
            this.hash = System.identityHashCode(first) * System.identityHashCode(second);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onChanged(Change<? extends E> change) {
            if (updating) {
                return;
            }
            Object a = first.get();
            Object b = second.get();
            if (!(a instanceof ObservableList) || !(b instanceof ObservableList)) {
                if (a instanceof ObservableList) {
                    ((ObservableList<E>) a).removeListener(this);
                }
                if (b instanceof ObservableList) {
                    ((ObservableList<E>) b).removeListener(this);
                }
                return;
            }
            updating = true;
            try {
                replay(change, a == change.getList() ? (ObservableList<E>) b : (ObservableList<E>) a);
            } finally {
                updating = false;
            }
        }

        @Override
        public boolean wasGarbageCollected() {
            return first.get() == null || second.get() == null;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof BothLists)) {
                return false;
            }
            Object a = first.get();
            Object b = second.get();
            if (a == null || b == null) {
                return false;
            }
            BothLists<?> that = (BothLists<?>) other;
            Object otherA = that.first.get();
            Object otherB = that.second.get();
            return (a == otherA && b == otherB) || (a == otherB && b == otherA);
        }
    }
}
