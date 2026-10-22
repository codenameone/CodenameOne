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

import java.util.Collections;
import java.util.List;

/// Told about the changes made to an [ObservableList].
@FunctionalInterface
public interface ListChangeListener<E> {

    /// Called after the list changed. The change must be walked with
    /// [Change#next()] and is only valid during the call.
    void onChanged(Change<? extends E> c);

    /// A report of what happened to a list, as a sequence of steps.
    ///
    /// The steps are ordered by position and each describes the list as it
    /// is once the steps before it have been applied, so a listener can
    /// replay them in order on a copy. A step is one of: a range of added
    /// elements, possibly replacing removed ones ([#wasAdded()],
    /// [#wasRemoved()], both for [#wasReplaced()]); a reordering of a range
    /// ([#wasPermutated()]); or a range of elements whose own state changed
    /// ([#wasUpdated()]).
    ///
    /// A new change stands before its first step: nothing may be read until
    /// [#next()] returned `true`.
    public abstract static class Change<E> {

        private final ObservableList<E> list;

        /// Creates a change of a list.
        public Change(ObservableList<E> list) {
            this.list = list;
        }

        /// Moves to the next step; returns `false` when there is none.
        public abstract boolean next();

        /// Goes back to before the first step.
        public abstract void reset();

        /// Returns the list that changed.
        public ObservableList<E> getList() {
            return list;
        }

        /// Returns where the step starts.
        public abstract int getFrom();

        /// Returns the end, exclusive, of the range the step added,
        /// reordered or updated; equal to [#getFrom()] for a pure removal.
        public abstract int getTo();

        /// Returns the elements the step removed or replaced, in the order
        /// they had; empty when it removed nothing.
        public abstract List<E> getRemoved();

        /// Returns whether the step reordered its range.
        public boolean wasPermutated() {
            return getPermutation().length != 0;
        }

        /// Returns whether the step added elements.
        public boolean wasAdded() {
            return !wasPermutated() && !wasUpdated() && getFrom() < getTo();
        }

        /// Returns whether the step removed elements.
        public boolean wasRemoved() {
            return !getRemoved().isEmpty();
        }

        /// Returns whether the step removed elements and added others in
        /// their place.
        public boolean wasReplaced() {
            return wasAdded() && wasRemoved();
        }

        /// Returns whether the step reports elements whose state changed.
        public boolean wasUpdated() {
            return false;
        }

        /// Returns the added elements as a view of the list; empty when the
        /// step added nothing.
        public List<E> getAddedSubList() {
            return wasAdded() ? getList().subList(getFrom(), getTo()) : Collections.<E>emptyList();
        }

        /// Returns how many elements the step removed.
        public int getRemovedSize() {
            return getRemoved().size();
        }

        /// Returns how many elements the step added.
        public int getAddedSize() {
            return wasAdded() ? getTo() - getFrom() : 0;
        }

        /// Returns the reordering: entry `i` is the new position of the
        /// element that was at `getFrom() + i`. Empty when the step is not
        /// a permutation.
        protected abstract int[] getPermutation();

        /// Returns the new position of the element that was at `i`.
        ///
        /// #### Throws
        ///
        /// - `IllegalStateException`: when the step is not a permutation
        public int getPermutation(int i) {
            if (!wasPermutated()) {
                throw new IllegalStateException("Not a permutation change");
            }
            return getPermutation()[i - getFrom()];
        }
    }
}
