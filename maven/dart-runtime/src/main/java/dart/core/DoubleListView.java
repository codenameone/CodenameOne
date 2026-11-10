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

/// A live List<double> view of a boxed {@code DartList<Double>}. Generic code builds
/// its lists boxed -- `List<E>.of(items)` in a class whose E is bound to double at the use
/// site cannot know to pick the primitive storage class -- while a caller whose static type
/// is List<double> holds a DartDoubleList. The call site therefore views the boxed list
/// through this class rather than casting it: reads and writes go to the same list, so
/// aliasing behaves as in Dart (a copy would lose writes made through either side).
public final class DoubleListView extends DartDoubleList implements MixinView {

    private final DartList<Double> src;

    DoubleListView(DartList<Double> src) {
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
        return src.get(index);
    }

    @Override
    protected Object setElement(int index, Object element$in) {
        Double element = dart.core.DartDoubleList.checkedDouble(element$in);
        RangeError.checkValidIndex(index, size());
        return src.set(index, element);
    }

    @Override
    public int size() {
        return src.size();
    }

    @Override
    protected boolean addElement(Object e$in) {
        Double e = dart.core.DartDoubleList.checkedDouble(e$in);
        modCount++;
        return src.add(e);
    }

    @Override
    protected void insertElement(int index, Object element$in) {
        Double element = dart.core.DartDoubleList.checkedDouble(element$in);
        RangeError.checkValueInInterval(index, 0, size(), "index");
        modCount++;
        src.add(index, element);
    }

    @Override
    public Double remove(int index) {
        RangeError.checkValidIndex(index, size());
        modCount++;
        return src.remove(index);
    }

    @Override
    void setLengthUnchecked(int n) {
        modCount++;
        src.length(n);
    }

    @Override
    public double getDouble(long index) {
        RangeError.checkValidIndex(index, size());
        return src.get((int) index);
    }

    @Override
    public double setDouble(long index, double value) {
        RangeError.checkValidIndex(index, size());
        src.set((int) index, value);
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
            out.addDouble(src.get((int) i));
        }
        return out;
    }

    @Override
    public DartDoubleList sublist(long start) {
        return sublist(start, size());
    }
}
