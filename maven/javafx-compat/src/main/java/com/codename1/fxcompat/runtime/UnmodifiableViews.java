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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.collections.WeakMapChangeListener;
import javafx.collections.WeakSetChangeListener;

/// Read-only views of observable collections. Each refuses modification,
/// shows the content of its source and tells its own listeners about every
/// change of the source, which it observes weakly.
public final class UnmodifiableViews {

    private UnmodifiableViews() {
    }

    /// A read-only view of a list.
    public static final class OfList<E> extends ObservableListBase<E> {
        private final ObservableList<E> source;
        private final ListChangeListener<E> listener;

        /// Creates the view.
        public OfList(ObservableList<E> source) {
            this.source = source;
            this.listener = new ListChangeListener<E>() {
                @Override
                public void onChanged(Change<? extends E> change) {
                    relay(change);
                }
            };
            source.addListener(new WeakListChangeListener<E>(listener));
        }

        private void relay(ListChangeListener.Change<? extends E> change) {
            modCount++;
            fireChange(new Changes.Relayed<E>(this, change));
        }

        @Override
        public E get(int index) {
            return source.get(index);
        }

        @Override
        public int size() {
            return source.size();
        }
    }

    /// A read-only view of a map.
    public static final class OfMap<K, V> extends AbstractMap<K, V> implements ObservableMap<K, V> {
        private final ObservableMap<K, V> source;
        private final Map<K, V> readOnly;
        private final MapChangeListener<K, V> listener;
        private ListenerSet<MapChangeListener<? super K, ? super V>> listeners;

        /// Creates the view.
        public OfMap(ObservableMap<K, V> source) {
            this.source = source;
            this.readOnly = Collections.unmodifiableMap(source);
            this.listener = new MapChangeListener<K, V>() {
                @Override
                public void onChanged(Change<? extends K, ? extends V> change) {
                    relay(change);
                }
            };
            source.addListener(new WeakMapChangeListener<K, V>(listener));
        }

        private void relay(MapChangeListener.Change<? extends K, ? extends V> change) {
            if (ListenerSet.hasListeners(listeners)) {
                Changes.fire(listeners, this, new Changes.OfMap<K, V>(this, change.getKey(),
                        change.getValueRemoved(), change.getValueAdded(), change.wasAdded(), change.wasRemoved()));
            }
        }

        @Override
        public void addListener(InvalidationListener invalidationListener) {
            listeners = ListenerSet.addInvalidation(listeners, invalidationListener);
        }

        @Override
        public void removeListener(InvalidationListener invalidationListener) {
            listeners = ListenerSet.removeInvalidation(listeners, invalidationListener);
        }

        @Override
        public void addListener(MapChangeListener<? super K, ? super V> changeListener) {
            listeners = ListenerSet.addChange(listeners, changeListener);
        }

        @Override
        public void removeListener(MapChangeListener<? super K, ? super V> changeListener) {
            listeners = ListenerSet.removeChange(listeners, changeListener);
        }

        @Override
        public int size() {
            return source.size();
        }

        @Override
        public boolean containsKey(Object key) {
            return source.containsKey(key);
        }

        @Override
        public V get(Object key) {
            return source.get(key);
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return readOnly.entrySet();
        }
    }

    /// A read-only view of a set.
    public static final class OfSet<E> extends AbstractSet<E> implements ObservableSet<E> {
        private final ObservableSet<E> source;
        private final Set<E> readOnly;
        private final SetChangeListener<E> listener;
        private ListenerSet<SetChangeListener<? super E>> listeners;

        /// Creates the view.
        public OfSet(ObservableSet<E> source) {
            this.source = source;
            this.readOnly = Collections.unmodifiableSet(source);
            this.listener = new SetChangeListener<E>() {
                @Override
                public void onChanged(Change<? extends E> change) {
                    relay(change);
                }
            };
            source.addListener(new WeakSetChangeListener<E>(listener));
        }

        private void relay(SetChangeListener.Change<? extends E> change) {
            if (ListenerSet.hasListeners(listeners)) {
                boolean added = change.wasAdded();
                E element = added ? change.getElementAdded() : change.getElementRemoved();
                Changes.fire(listeners, this, new Changes.OfSet<E>(this, element, added));
            }
        }

        @Override
        public void addListener(InvalidationListener invalidationListener) {
            listeners = ListenerSet.addInvalidation(listeners, invalidationListener);
        }

        @Override
        public void removeListener(InvalidationListener invalidationListener) {
            listeners = ListenerSet.removeInvalidation(listeners, invalidationListener);
        }

        @Override
        public void addListener(SetChangeListener<? super E> changeListener) {
            listeners = ListenerSet.addChange(listeners, changeListener);
        }

        @Override
        public void removeListener(SetChangeListener<? super E> changeListener) {
            listeners = ListenerSet.removeChange(listeners, changeListener);
        }

        @Override
        public int size() {
            return source.size();
        }

        @Override
        public boolean contains(Object o) {
            return source.contains(o);
        }

        @Override
        public Iterator<E> iterator() {
            return readOnly.iterator();
        }
    }
}
