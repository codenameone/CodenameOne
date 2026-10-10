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

/// Told about the changes made to an [ObservableSet].
@FunctionalInterface
public interface SetChangeListener<E> {

    /// Called after one element was added or removed.
    void onChanged(Change<? extends E> change);

    /// What happened to a set: one element was added or removed.
    public abstract static class Change<E> {

        private final ObservableSet<E> set;

        /// Creates a change of a set.
        public Change(ObservableSet<E> set) {
            this.set = set;
        }

        /// Returns the set that changed.
        public ObservableSet<E> getSet() {
            return set;
        }

        /// Returns whether an element was added.
        public abstract boolean wasAdded();

        /// Returns whether an element was removed.
        public abstract boolean wasRemoved();

        /// Returns the added element, or `null` for a removal.
        public abstract E getElementAdded();

        /// Returns the removed element, or `null` for an addition.
        public abstract E getElementRemoved();
    }
}
