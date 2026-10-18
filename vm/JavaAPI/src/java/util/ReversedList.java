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
package java.util;

/// The view `List.reversed()` answers: index `i` here is index
/// `size() - 1 - i` of the backing list, and every read and write goes
/// through to it. Structural changes made to the backing list directly are
/// visible here at once, because nothing is copied.
class ReversedList<E> extends AbstractList<E> {

    /// The view of a `RandomAccess` list, which carries the marker too.
    static final class Indexed<E> extends ReversedList<E> implements RandomAccess {
        Indexed(List<E> base) {
            super(base);
        }
    }

    private final List<E> base;

    ReversedList(List<E> base) {
        this.base = base;
    }

    private int flip(int index) {
        return base.size() - 1 - index;
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= base.size()) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        return base.get(flip(index));
    }

    @Override
    public int size() {
        return base.size();
    }

    @Override
    public E set(int index, E element) {
        if (index < 0 || index >= base.size()) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        return base.set(flip(index), element);
    }

    @Override
    public void add(int index, E element) {
        int size = base.size();
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        base.add(size - index, element);
    }

    @Override
    public E remove(int index) {
        if (index < 0 || index >= base.size()) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        return base.remove(flip(index));
    }

    @Override
    public void clear() {
        base.clear();
    }

    @Override
    public List<E> reversed() {
        return base;
    }
}
