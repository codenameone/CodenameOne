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
public final class DoubleListMixinView extends DartDoubleList implements MixinView {

    private final ListMixin<Double> src;

    public DoubleListMixinView(ListMixin<Double> src) {
        super(false);
        this.src = src;
    }

    @Override
    public Object source() {
        return src;
    }

    @Override
    public Double get(int index) {
        RangeError.checkValidIndex(index, size());
        return src.$index(index);
    }

    @Override
    public Double set(int index, Double element) {
        RangeError.checkValidIndex(index, size());
        Double old = src.$index(index);
        src.$indexSet(index, element);
        return old;
    }

    @Override
    public int size() {
        return (int) src.length();
    }

    @Override
    public boolean add(Double e) {
        modCount++;
        src.add(e);
        return true;
    }

    @Override
    public void add(int index, Double element) {
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
    public Double remove(int index) {
        RangeError.checkValidIndex(index, size());
        modCount++;
        Double old = src.$index(index);
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
    public double getDouble(long index) {
        RangeError.checkValidIndex(index, size());
        return src.$index(index);
    }

    @Override
    public double setDouble(long index, double value) {
        RangeError.checkValidIndex(index, size());
        src.$indexSet(index, value);
        return value;
    }

    @Override
    public boolean addDouble(double value) {
        return add((Double) value);
    }

    @Override
    public DartDoubleList sublist(long start, long end) {
        RangeError.checkValueInInterval(start, 0, size(), "start");
        RangeError.checkValueInInterval(end, start, size(), "end");
        DartDoubleList out = new DartDoubleList();
        for (long i = start; i < end; i++) {
            out.addDouble(src.$index(i));
        }
        return out;
    }

    @Override
    public DartDoubleList sublist(long start) {
        return sublist(start, size());
    }
}
