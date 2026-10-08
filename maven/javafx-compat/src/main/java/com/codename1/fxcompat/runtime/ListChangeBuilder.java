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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/// Assembles the change report of an observable list from the single steps
/// its implementation records.
///
/// The steps arrive in the order the list was modified, each in the
/// coordinates the list had at that moment. The builder keeps them ordered
/// by position, moves the earlier ones as later ones shift the content, and
/// folds neighbours together, so that a removal followed by an insertion at
/// the same place is reported as one replacement and a run of single
/// insertions as one range. An element added and removed again inside one
/// change leaves no trace.
///
/// A permutation is kept as one only while it is the whole change; combined
/// with any other step it is reported as a replacement of its range, which
/// says the same less precisely.
public final class ListChangeBuilder<E> {

    private static final int ADD_REMOVE = 0;
    private static final int UPDATE = 1;
    private static final int PERMUTATION = 2;
    private static final int[] NO_PERMUTATION = new int[0];

    private final ObservableList<E> list;
    private int depth;
    private ArrayList<Step<E>> steps = new ArrayList<Step<E>>();

    /// Creates the builder of a list.
    public ListChangeBuilder(ObservableList<E> list) {
        this.list = list;
    }

    /// Opens a change; calls nest.
    public void beginChange() {
        depth++;
    }

    /// Closes a change. Returns the report when this closed the outermost
    /// one and something was recorded, otherwise `null`.
    public ListChangeListener.Change<E> endChange() {
        if (depth <= 0) {
            throw new IllegalStateException("Called endChange before beginChange");
        }
        depth--;
        if (depth == 0 && !steps.isEmpty()) {
            ArrayList<Step<E>> done = steps;
            steps = new ArrayList<Step<E>>();
            return new Built<E>(list, done);
        }
        return null;
    }

    private void check() {
        if (depth == 0) {
            throw new IllegalStateException("beginChange was not called on this builder");
        }
    }

    /// Records that the range from `from` to `to` was added.
    public void nextAdd(int from, int to) {
        check();
        if (from >= to) {
            return;
        }
        convertPermutation();
        splitUpdate(from);
        int count = to - from;
        Step<E> target = null;
        int insertAt = steps.size();
        for (int i = 0; i < steps.size(); i++) {
            Step<E> step = steps.get(i);
            if (step.kind == ADD_REMOVE && step.from <= from && from <= step.to) {
                target = step;
                break;
            }
            if (step.from >= from) {
                insertAt = i;
                break;
            }
        }
        if (target == null) {
            target = new Step<E>(ADD_REMOVE, from, from);
            steps.add(insertAt, target);
        }
        for (int i = 0; i < steps.size(); i++) {
            Step<E> step = steps.get(i);
            if (step != target && step.from >= from) {
                step.from += count;
                step.to += count;
            }
        }
        target.to += count;
        tidy();
    }

    /// Records that an element was removed from a position.
    public void nextRemove(int index, E removed) {
        check();
        convertPermutation();
        splitUpdate(index);
        Step<E> target = null;
        int insertAt = steps.size();
        for (int i = 0; i < steps.size(); i++) {
            Step<E> step = steps.get(i);
            if (step.kind == ADD_REMOVE) {
                if (step.from <= index && index < step.to) {
                    // The element was added by this very change.
                    step.to--;
                    target = step;
                    break;
                }
                if (step.to == index) {
                    step.removed.add(removed);
                    target = step;
                    break;
                }
                if (step.from == index + 1) {
                    step.from--;
                    step.to--;
                    step.removed.add(0, removed);
                    target = step;
                    break;
                }
            }
            if (step.from >= index) {
                insertAt = i;
                break;
            }
        }
        if (target == null) {
            target = new Step<E>(ADD_REMOVE, index, index);
            target.removed.add(removed);
            steps.add(insertAt, target);
        }
        for (int i = 0; i < steps.size(); i++) {
            Step<E> step = steps.get(i);
            if (step == target) {
                continue;
            }
            if (step.from > index) {
                step.from--;
                step.to--;
            } else if (step.kind == UPDATE && step.from <= index && index < step.to) {
                step.to--;
            }
        }
        tidy();
    }

    /// Records that the given elements were removed from a position.
    public void nextRemove(int index, List<? extends E> removed) {
        for (int i = 0; i < removed.size(); i++) {
            nextRemove(index, removed.get(i));
        }
    }

    /// Records that the element at a position was replaced.
    public void nextSet(int index, E old) {
        nextRemove(index, old);
        nextAdd(index, index + 1);
    }

    /// Records that the range now at `from` to `to` replaced the given
    /// elements.
    public void nextReplace(int from, int to, List<? extends E> removed) {
        nextRemove(from, removed);
        nextAdd(from, to);
    }

    /// Records that the element at a position changed its own state.
    public void nextUpdate(int index) {
        check();
        convertPermutation();
        int insertAt = steps.size();
        for (int i = 0; i < steps.size(); i++) {
            Step<E> step = steps.get(i);
            if (step.from <= index && index < step.to) {
                // Already reported as added or updated.
                return;
            }
        }
        for (int i = 0; i < steps.size(); i++) {
            Step<E> step = steps.get(i);
            if (step.kind == UPDATE && step.to == index) {
                step.to++;
                tidy();
                return;
            }
            if (step.kind == UPDATE && step.from == index + 1) {
                step.from--;
                tidy();
                return;
            }
            if (step.from > index) {
                insertAt = i;
                break;
            }
        }
        steps.add(insertAt, new Step<E>(UPDATE, index, index + 1));
    }

    /// Records that a range was reordered; entry `i` of the permutation is
    /// the new position of the element that was at `from + i`.
    public void nextPermutation(int from, int to, int[] perm) {
        check();
        if (from >= to) {
            return;
        }
        // The order before the permutation, should it have to be reported
        // as a replacement.
        List<E> before = new ArrayList<E>(to - from);
        for (int i = 0; i < to - from; i++) {
            before.add(list.get(perm[i]));
        }
        if (steps.isEmpty()) {
            Step<E> step = new Step<E>(PERMUTATION, from, to);
            step.removed = before;
            step.perm = perm.clone();
            steps.add(step);
            return;
        }
        Step<E> first = steps.get(0);
        if (steps.size() == 1 && first.kind == PERMUTATION && first.from == from && first.to == to) {
            for (int i = 0; i < first.perm.length; i++) {
                first.perm[i] = perm[first.perm[i] - from];
            }
            return;
        }
        convertPermutation();
        nextReplace(from, to, before);
    }

    private void convertPermutation() {
        if (steps.size() == 1) {
            Step<E> step = steps.get(0);
            if (step.kind == PERMUTATION) {
                step.kind = ADD_REMOVE;
                step.perm = null;
            }
        }
    }

    /// Cuts an update range in two at a position strictly inside it, so a
    /// step recorded at that position can sit between the halves.
    private void splitUpdate(int index) {
        for (int i = 0; i < steps.size(); i++) {
            Step<E> step = steps.get(i);
            if (step.kind == UPDATE && step.from < index && index < step.to) {
                Step<E> tail = new Step<E>(UPDATE, index, step.to);
                step.to = index;
                steps.add(i + 1, tail);
                return;
            }
        }
    }

    /// Drops the steps that ended up describing nothing and joins the
    /// neighbours of the same kind that touch.
    private void tidy() {
        for (int i = steps.size() - 1; i >= 0; i--) {
            Step<E> step = steps.get(i);
            if (step.from == step.to && (step.kind == UPDATE || step.removed.isEmpty())) {
                steps.remove(i);
            }
        }
        for (int i = 0; i + 1 < steps.size(); i++) {
            Step<E> first = steps.get(i);
            Step<E> second = steps.get(i + 1);
            if (first.kind == second.kind && first.kind != PERMUTATION && first.to == second.from) {
                first.to = second.to;
                first.removed.addAll(second.removed);
                steps.remove(i + 1);
                i--;
            }
        }
    }

    /// One step of a change.
    private static final class Step<E> {
        private int kind;
        private int from;
        private int to;
        private List<E> removed = new ArrayList<E>();
        private int[] perm;

        Step(int kind, int from, int to) {
            this.kind = kind;
            this.from = from;
            this.to = to;
        }
    }

    /// The finished report.
    private static final class Built<E> extends ListChangeListener.Change<E> {
        private final List<Step<E>> steps;
        private int cursor = -1;

        Built(ObservableList<E> list, List<Step<E>> steps) {
            super(list);
            this.steps = steps;
        }

        private Step<E> current() {
            if (cursor < 0) {
                throw new IllegalStateException(
                        "Invalid Change state: next() must be called before inspecting the Change.");
            }
            return steps.get(cursor);
        }

        @Override
        public boolean next() {
            if (cursor + 1 < steps.size()) {
                cursor++;
                return true;
            }
            return false;
        }

        @Override
        public void reset() {
            cursor = -1;
        }

        @Override
        public int getFrom() {
            return current().from;
        }

        @Override
        public int getTo() {
            return current().to;
        }

        @Override
        public List<E> getRemoved() {
            Step<E> step = current();
            if (step.kind == ADD_REMOVE) {
                return Collections.unmodifiableList(step.removed);
            }
            return Collections.<E>emptyList();
        }

        @Override
        protected int[] getPermutation() {
            Step<E> step = current();
            return step.kind == PERMUTATION ? step.perm : NO_PERMUTATION;
        }

        @Override
        public boolean wasUpdated() {
            return current().kind == UPDATE;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder("{ ");
            for (int i = 0; i < steps.size(); i++) {
                Step<E> step = steps.get(i);
                if (i > 0) {
                    result.append(", ");
                }
                if (step.kind == PERMUTATION) {
                    result.append("permutated [").append(step.from).append(", ").append(step.to).append(')');
                } else if (step.kind == UPDATE) {
                    result.append("updated [").append(step.from).append(", ").append(step.to).append(')');
                } else {
                    result.append(step.removed).append(" replaced by [").append(step.from).append(", ")
                            .append(step.to).append(')');
                }
            }
            return result.append(" }").toString();
        }
    }
}
