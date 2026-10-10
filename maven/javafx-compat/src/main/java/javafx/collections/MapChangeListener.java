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
package javafx.collections;

/// Told about the changes made to an [ObservableMap].
@FunctionalInterface
public interface MapChangeListener<K, V> {

    /// Called after one key of the map changed.
    void onChanged(Change<? extends K, ? extends V> change);

    /// What happened to one key: a value was added, removed, or -- when
    /// both [#wasAdded()] and [#wasRemoved()] hold -- replaced.
    public abstract static class Change<K, V> {

        private final ObservableMap<K, V> map;

        /// Creates a change of a map.
        public Change(ObservableMap<K, V> map) {
            this.map = map;
        }

        /// Returns the map that changed.
        public ObservableMap<K, V> getMap() {
            return map;
        }

        /// Returns whether a value was put for the key.
        public abstract boolean wasAdded();

        /// Returns whether a value was removed for the key.
        public abstract boolean wasRemoved();

        /// Returns the key that changed.
        public abstract K getKey();

        /// Returns the value now stored, or `null` for a removal.
        public abstract V getValueAdded();

        /// Returns the value that was stored, or `null` for an addition.
        public abstract V getValueRemoved();
    }
}
