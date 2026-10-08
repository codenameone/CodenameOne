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

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.codename1.fxcompat.runtime.ListChangeBuilder;
import com.codename1.fxcompat.runtime.ListenerSet;

import javafx.beans.InvalidationListener;

/// Base class for an observable list: it keeps the listeners and assembles
/// the change reports.
///
/// A subclass wraps every modification in [#beginChange()] and
/// [#endChange()] and describes it in between with the `next...` methods,
/// each called right after the step it describes was applied. The steps are
/// merged and ordered, and one change is sent when the outermost
/// `endChange()` returns; the pairs nest, so a bulk operation built from
/// single ones is still reported once.
public abstract class ObservableListBase<E> extends AbstractList<E> implements ObservableList<E> {

    private ListenerSet<ListChangeListener<? super E>> listeners;
    private final ListChangeBuilder<E> changeBuilder = new ListChangeBuilder<E>(this);

    /// Creates the list.
    public ObservableListBase() {
    }

    /// Records that the element at a position changed its own state.
    protected final void nextUpdate(int pos) {
        changeBuilder.nextUpdate(pos);
    }

    /// Records that the element at a position was replaced.
    protected final void nextSet(int idx, E old) {
        changeBuilder.nextSet(idx, old);
    }

    /// Records that the range now at `from` to `to` replaced the given
    /// elements.
    protected final void nextReplace(int from, int to, List<? extends E> removed) {
        changeBuilder.nextReplace(from, to, removed);
    }

    /// Records that the given elements were removed from a position.
    protected final void nextRemove(int idx, List<? extends E> removed) {
        changeBuilder.nextRemove(idx, removed);
    }

    /// Records that an element was removed from a position.
    protected final void nextRemove(int idx, E removed) {
        changeBuilder.nextRemove(idx, removed);
    }

    /// Records that a range was reordered; entry `i` of the permutation is
    /// the new position of the element that was at `from + i`.
    protected final void nextPermutation(int from, int to, int[] perm) {
        changeBuilder.nextPermutation(from, to, perm);
    }

    /// Records that the range from `from` to `to` was added.
    protected final void nextAdd(int from, int to) {
        changeBuilder.nextAdd(from, to);
    }

    /// Opens a change; must be matched by [#endChange()].
    protected final void beginChange() {
        changeBuilder.beginChange();
    }

    /// Closes a change and, when it is the outermost one and anything was
    /// recorded, tells the listeners.
    protected final void endChange() {
        ListChangeListener.Change<E> change = changeBuilder.endChange();
        if (change != null) {
            fireChange(change);
        }
    }

    @Override
    public final void addListener(InvalidationListener listener) {
        listeners = ListenerSet.addInvalidation(listeners, listener);
    }

    @Override
    public final void removeListener(InvalidationListener listener) {
        listeners = ListenerSet.removeInvalidation(listeners, listener);
    }

    @Override
    public final void addListener(ListChangeListener<? super E> listener) {
        listeners = ListenerSet.addChange(listeners, listener);
    }

    @Override
    public final void removeListener(ListChangeListener<? super E> listener) {
        listeners = ListenerSet.removeChange(listeners, listener);
    }

    /// Sends a change to every listener, each starting before its first
    /// step.
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected final void fireChange(ListChangeListener.Change<? extends E> change) {
        ListenerSet<ListChangeListener<? super E>> current = listeners;
        ListenerSet.fireInvalidation(current, this);
        List<ListChangeListener<? super E>> targets = ListenerSet.changeListeners(current);
        for (int i = 0; i < targets.size(); i++) {
            change.reset();
            ((ListChangeListener) targets.get(i)).onChanged(change);
        }
    }

    /// Returns whether anything listens to this list.
    protected final boolean hasListeners() {
        return ListenerSet.hasListeners(listeners);
    }

    @Override
    public boolean addAll(E... elements) {
        return addAll(Arrays.asList(elements));
    }

    @Override
    public boolean setAll(E... elements) {
        return setAll(Arrays.asList(elements));
    }

    @Override
    public boolean setAll(Collection<? extends E> col) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(E... elements) {
        return removeAll(Arrays.asList(elements));
    }

    @Override
    public boolean retainAll(E... elements) {
        return retainAll(Arrays.asList(elements));
    }

    @Override
    public void remove(int from, int to) {
        removeRange(from, to);
    }
}
