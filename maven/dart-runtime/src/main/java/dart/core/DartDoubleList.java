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

import java.util.Arrays;

import dart.runtime.Funcs;

/**
 * A {@link DartList}&lt;Double&gt; backed by a primitive {@code double[]} — the transpiler targets this for
 * Dart {@code List&lt;double&gt;}. The hot index/add paths ({@link #getDouble(long)}, {@link #setDouble(long, double)},
 * {@link #addDouble(double)}) never box; the inherited list surface still works (boxing only at that
 * generic boundary) because {@link DartList} routes through the overridden {@code get/set/size/add}.
 */
public final class DartDoubleList extends DartList<Double> {

    private double[] a;
    private int len;

    public DartDoubleList() {
        super(true);
        a = new double[8];
    }

    private DartDoubleList(double[] a, int len, boolean growable) {
        super(growable);
        this.a = a;
        this.len = len;
    }

    /**
     * A fixed-length list over {@code backing} itself, not a copy: writes through
     * it land in the array. Dart's Float64List views -- vector_math's
     * {@code Matrix4.storage} -- are exactly that.
     */
    public static DartDoubleList view(double[] backing) {
        return new DartDoubleList(backing, backing.length, false);
    }

    /** Literal helper for Dart's &lt;double&gt;[a, b, c]. */
    public static DartDoubleList ofDoubles(double... elements) {
        double[] backing = elements.length == 0 ? new double[8] : Arrays.copyOf(elements, Math.max(8, elements.length));
        return new DartDoubleList(backing, elements.length, true);
    }

    /** Dart's List&lt;double&gt;.filled(length, fill). */
    public static DartDoubleList filled(long length, double fill, boolean growable) {
        int n = (int) length;
        double[] backing = new double[Math.max(8, n)];
        Arrays.fill(backing, 0, n, fill);
        return new DartDoubleList(backing, n, growable);
    }

    public static DartDoubleList filled(long length, double fill) {
        return filled(length, fill, false);
    }

    // Named *Doubles (mirroring ofDoubles) to avoid an erasure clash with the
    // inherited DartList generic generate/from statics.

    /** Dart's List&lt;double&gt;.generate(length, generator). */
    public static DartDoubleList generateDoubles(long length, Funcs.Func1<Long, Double> generator, boolean growable) {
        int n = (int) length;
        double[] backing = new double[Math.max(8, n)];
        for (int i = 0; i < n; i++) {
            backing[i] = generator.call((long) i);
        }
        return new DartDoubleList(backing, n, growable);
    }

    public static DartDoubleList generateDoubles(long length, Funcs.Func1<Long, Double> generator) {
        return generateDoubles(length, generator, true);
    }

    /** Dart's List&lt;double&gt;.from(elements). */
    /** {@link #fromDoubles(Iterable)} with Dart's {@code growable:} flag. */
    public static DartDoubleList fromDoubles(Iterable<? extends Number> elements, boolean growable) {
        DartDoubleList grown = fromDoubles(elements);
        return new DartDoubleList(grown.a, grown.len, growable);
    }

    public static DartDoubleList fromDoubles(Iterable<? extends Number> elements) {
        DartDoubleList l = new DartDoubleList();
        for (Number e : elements) {
            l.addDouble(e.doubleValue());
        }
        return l;
    }

    private void ensure(int cap) {
        if (cap > a.length) {
            a = Arrays.copyOf(a, Math.max(cap, a.length * 2));
        }
    }

    // --- primitive fast paths (transpiler targets these for List<double>) ---

    public double getDouble(long index) {
        // Inline bounds check + cold throw helper — keeps this frameless (see
        // RangeError.indexError / DartLongList.getLong).
        if (index < 0 || index >= len) {
            RangeError.indexError(index, len);
        }
        return a[(int) index];
    }

    public double setDouble(long index, double value) {
        if (index < 0 || index >= len) {
            RangeError.indexError(index, len);
        }
        a[(int) index] = value;
        return value;
    }

    public boolean addDouble(double value) {
        checkGrowable("add");
        ensure(len + 1);
        a[len++] = value;
        modCount++;
        return true;
    }

    // --- storage accessors (boxed boundary for the inherited surface) ---

    @Override
    public Double get(int index) {
        RangeError.checkValidIndex(index, len);
        return a[index];
    }

    @Override
    public Double set(int index, Double element) {
        RangeError.checkValidIndex(index, len);
        double old = a[index];
        a[index] = element;
        return old;
    }

    @Override
    public int size() {
        return len;
    }

    @Override
    public boolean add(Double e) {
        return addDouble(e);
    }

    @Override
    public void add(int index, Double element) {
        checkGrowable("insert");
        ensure(len + 1);
        System.arraycopy(a, index, a, index + 1, len - index);
        a[index] = element;
        len++;
        modCount++;
    }

    @Override
    public Double remove(int index) {
        checkGrowable("removeAt");
        RangeError.checkValidIndex(index, len);
        double old = a[index];
        System.arraycopy(a, index + 1, a, index, len - index - 1);
        len--;
        modCount++;
        return old;
    }
}
