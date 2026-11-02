/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.core;

import dart.collection.ListMixin;

/// A live {@code List} view of a program object that applies dart:collection's
/// {@code ListMixin} (or extends {@code ListBase}), made where the object is passed to a
/// parameter or variable declared {@code List}. The runtime list classes keep private
/// storage, so the object itself cannot be one; this view holds no storage of its own and
/// routes every element read, write and length change through the object's four
/// {@code ListMixin} members, so a write through the view reaches the object and a read
/// sees its current state. {@link dart.runtime.DartRuntime#identical} looks through the view
/// ({@link #source()}), so {@code identical(echo(r), r)} holds as in Dart.
public final class LongListMixinView extends DartLongList implements MixinView {

    private final ListMixin<Long> src;

    public LongListMixinView(ListMixin<Long> src) {
        super(false);
        this.src = src;
    }

    @Override
    public Object source() {
        return src;
    }

    @Override
    public Long get(int index) {
        RangeError.checkValidIndex(index, size());
        return src.$index(index);
    }

    @Override
    protected Object setElement(int index, Object element$in) {
        Long element = dart.core.DartLongList.checkedLong(element$in);
        RangeError.checkValidIndex(index, size());
        Long old = src.$index(index);
        src.$indexSet(index, element);
        return old;
    }

    @Override
    public int size() {
        return (int) src.length();
    }

    @Override
    protected boolean addElement(Object e$in) {
        Long e = dart.core.DartLongList.checkedLong(e$in);
        modCount++;
        src.add(e);
        return true;
    }

    @Override
    protected void insertElement(int index, Object element$in) {
        Long element = dart.core.DartLongList.checkedLong(element$in);
        RangeError.checkValueInInterval(index, 0, size(), "index");
        modCount++;
        int n = size();
        src.add(element);
        for (int i = n; i > index; i--) {
            src.$indexSet(i, src.$index(i - 1));
        }
        src.$indexSet(index, element);
    }

    @Override
    public Long remove(int index) {
        RangeError.checkValidIndex(index, size());
        modCount++;
        Long old = src.$index(index);
        int n = size();
        for (int i = index; i < n - 1; i++) {
            src.$indexSet(i, src.$index(i + 1));
        }
        src.length(n - 1);
        return old;
    }

    @Override
    void setLengthUnchecked(int n) {
        modCount++;
        src.length(n);
    }

    @Override
    public long getLong(long index) {
        RangeError.checkValidIndex(index, size());
        return src.$index(index);
    }

    @Override
    public long setLong(long index, long value) {
        RangeError.checkValidIndex(index, size());
        src.$indexSet(index, value);
        return value;
    }

    @Override
    public boolean addLong(long value) {
        return add((Long) value);
    }

    /** The program class's sublist, which it may override, copied into a long list. */
    @Override
    public DartLongList sublist(long start, long end) {
        return copyOf(src.sublist(start, end));
    }

    private static DartLongList copyOf(DartList<Long> picked) {
        DartLongList out = new DartLongList();
        for (int i = 0; i < picked.size(); i++) {
            out.addLong(picked.get(i));
        }
        return out;
    }

    /** Dart's addAll, run by the program class: it may override it. */
    @Override
    public void addAllIterable(Iterable<? extends Long> elements) {
        modCount++;
        // a view added to its own source is a snapshot: iterating it while appending never ends
        src.addAll(elements == this ? new java.util.ArrayList<>(this) : elements);
    }

    @Override
    public DartLongList sublist(long start) {
        return copyOf(src.sublist(start));
    }
}
