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

/// A live List<int> view of a boxed {@code DartList<Long>}. Generic code builds
/// its lists boxed -- `List<E>.of(items)` in a class whose E is bound to int at the use
/// site cannot know to pick the primitive storage class -- while a caller whose static type
/// is List<int> holds a DartLongList. The call site therefore views the boxed list
/// through this class rather than casting it: reads and writes go to the same list, so
/// aliasing behaves as in Dart (a copy would lose writes made through either side).
public final class LongListView extends DartLongList implements MixinView {

    private final DartList<Long> src;

    LongListView(DartList<Long> src) {
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
        return src.get(index);
    }

    @Override
    protected Object setElement(int index, Object element$in) {
        Long element = dart.core.DartLongList.checkedLong(element$in);
        RangeError.checkValidIndex(index, size());
        return src.set(index, element);
    }

    @Override
    public int size() {
        return src.size();
    }

    @Override
    protected boolean addElement(Object e$in) {
        Long e = dart.core.DartLongList.checkedLong(e$in);
        modCount++;
        return src.add(e);
    }

    @Override
    protected void insertElement(int index, Object element$in) {
        Long element = dart.core.DartLongList.checkedLong(element$in);
        RangeError.checkValueInInterval(index, 0, size(), "index");
        modCount++;
        src.add(index, element);
    }

    @Override
    public Long remove(int index) {
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
    public long getLong(long index) {
        RangeError.checkValidIndex(index, size());
        return src.get((int) index);
    }

    @Override
    public long setLong(long index, long value) {
        RangeError.checkValidIndex(index, size());
        src.set((int) index, value);
        return value;
    }

    @Override
    public boolean addLong(long value) {
        return add((Long) value);
    }

    @Override
    public DartLongList sublist(long start, long end) {
        RangeError.checkValueInInterval(start, 0, size(), "start");
        RangeError.checkValueInInterval(end, start, size(), "end");
        DartLongList out = new DartLongList();
        for (long i = start; i < end; i++) {
            out.addLong(src.get((int) i));
        }
        return out;
    }

    @Override
    public DartLongList sublist(long start) {
        return sublist(start, size());
    }
}
