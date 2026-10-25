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

import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

/// The change reports of maps and sets, the report a view sends on behalf
/// of its source, and the loops that deliver them.
public final class Changes {

    private static final int[] NO_PERMUTATION = new int[0];

    private Changes() {
    }

    /// Tells the listeners of a map about a change.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <K, V> void fire(ListenerSet<MapChangeListener<? super K, ? super V>> set, ObservableMap<K, V> map,
            MapChangeListener.Change<K, V> change) {
        ListenerSet.fireInvalidation(set, map);
        List<MapChangeListener<? super K, ? super V>> targets = ListenerSet.changeListeners(set);
        for (int i = 0; i < targets.size(); i++) {
            ((MapChangeListener) targets.get(i)).onChanged(change);
        }
    }

    /// Tells the listeners of a set about a change.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E> void fire(ListenerSet<SetChangeListener<? super E>> set, ObservableSet<E> observable,
            SetChangeListener.Change<E> change) {
        ListenerSet.fireInvalidation(set, observable);
        List<SetChangeListener<? super E>> targets = ListenerSet.changeListeners(set);
        for (int i = 0; i < targets.size(); i++) {
            ((SetChangeListener) targets.get(i)).onChanged(change);
        }
    }

    /// What happened to one key of a map.
    public static final class OfMap<K, V> extends MapChangeListener.Change<K, V> {
        private final K key;
        private final V removed;
        private final V added;
        private final boolean wasAdded;
        private final boolean wasRemoved;

        /// Creates the report.
        public OfMap(ObservableMap<K, V> map, K key, V removed, V added, boolean wasAdded, boolean wasRemoved) {
            super(map);
            this.key = key;
            this.removed = removed;
            this.added = added;
            this.wasAdded = wasAdded;
            this.wasRemoved = wasRemoved;
        }

        @Override
        public boolean wasAdded() {
            return wasAdded;
        }

        @Override
        public boolean wasRemoved() {
            return wasRemoved;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValueAdded() {
            return added;
        }

        @Override
        public V getValueRemoved() {
            return removed;
        }

        @Override
        public String toString() {
            if (wasAdded && wasRemoved) {
                return removed + " replaced by " + added + " at key " + key;
            }
            return wasAdded ? added + " added at key " + key : removed + " removed at key " + key;
        }
    }

    /// One element added to or removed from a set.
    public static final class OfSet<E> extends SetChangeListener.Change<E> {
        private final E element;
        private final boolean added;

        /// Creates the report.
        public OfSet(ObservableSet<E> set, E element, boolean added) {
            super(set);
            this.element = element;
            this.added = added;
        }

        @Override
        public boolean wasAdded() {
            return added;
        }

        @Override
        public boolean wasRemoved() {
            return !added;
        }

        @Override
        public E getElementAdded() {
            return added ? element : null;
        }

        @Override
        public E getElementRemoved() {
            return added ? null : element;
        }

        @Override
        public String toString() {
            return (added ? "added " : "removed ") + element;
        }
    }

    /// The change of a source list, told again as a change of a view that
    /// shows the same content at the same positions.
    public static final class Relayed<E> extends ListChangeListener.Change<E> {
        private final ListChangeListener.Change<? extends E> change;

        /// Creates the report of a view from the report of its source.
        public Relayed(ObservableList<E> list, ListChangeListener.Change<? extends E> change) {
            super(list);
            this.change = change;
        }

        @Override
        public boolean next() {
            return change.next();
        }

        @Override
        public void reset() {
            change.reset();
        }

        @Override
        public int getFrom() {
            return change.getFrom();
        }

        @Override
        public int getTo() {
            return change.getTo();
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<E> getRemoved() {
            return (List<E>) change.getRemoved();
        }

        @Override
        public boolean wasPermutated() {
            return change.wasPermutated();
        }

        @Override
        public boolean wasUpdated() {
            return change.wasUpdated();
        }

        @Override
        protected int[] getPermutation() {
            if (!change.wasPermutated()) {
                return NO_PERMUTATION;
            }
            int from = change.getFrom();
            int[] result = new int[change.getTo() - from];
            for (int i = 0; i < result.length; i++) {
                result[i] = change.getPermutation(from + i);
            }
            return result;
        }

        @Override
        public String toString() {
            return change.toString();
        }
    }
}
