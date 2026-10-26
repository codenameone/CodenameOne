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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/// Base class for a modifiable observable list over any storage: the
/// subclass implements `get`, `size` and the three `do...` methods, and this
/// class turns every `List` operation into them and reports the changes.
public abstract class ModifiableObservableListBase<E> extends ObservableListBase<E> {

    /// Creates the list.
    public ModifiableObservableListBase() {
    }

    /// Stores an element at a position, shifting the ones after it.
    protected abstract void doAdd(int index, E element);

    /// Replaces the element at a position and returns the old one.
    protected abstract E doSet(int index, E element);

    /// Removes the element at a position and returns it.
    protected abstract E doRemove(int index);

    @Override
    public boolean setAll(Collection<? extends E> col) {
        List<E> copy = new ArrayList<E>(col);
        beginChange();
        try {
            clear();
            addAll(copy);
        } finally {
            endChange();
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        beginChange();
        try {
            return super.addAll(c);
        } finally {
            endChange();
        }
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        beginChange();
        try {
            return super.addAll(index, c);
        } finally {
            endChange();
        }
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        beginChange();
        try {
            for (int i = fromIndex; i < toIndex; i++) {
                remove(fromIndex);
            }
        } finally {
            endChange();
        }
    }

    @Override
    public void clear() {
        removeRange(0, size());
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        boolean modified = false;
        beginChange();
        try {
            for (int i = size() - 1; i >= 0; i--) {
                if (c.contains(get(i))) {
                    remove(i);
                    modified = true;
                }
            }
        } finally {
            endChange();
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        boolean modified = false;
        beginChange();
        try {
            for (int i = size() - 1; i >= 0; i--) {
                if (!c.contains(get(i))) {
                    remove(i);
                    modified = true;
                }
            }
        } finally {
            endChange();
        }
        return modified;
    }

    @Override
    public void add(int index, E element) {
        doAdd(index, element);
        beginChange();
        nextAdd(index, index + 1);
        ++modCount;
        endChange();
    }

    @Override
    public E set(int index, E element) {
        E old = doSet(index, element);
        beginChange();
        nextSet(index, old);
        endChange();
        return old;
    }

    @Override
    public boolean remove(Object o) {
        int index = indexOf(o);
        if (index != -1) {
            remove(index);
            return true;
        }
        return false;
    }

    @Override
    public E remove(int index) {
        E old = doRemove(index);
        beginChange();
        nextRemove(index, old);
        ++modCount;
        endChange();
        return old;
    }
}
