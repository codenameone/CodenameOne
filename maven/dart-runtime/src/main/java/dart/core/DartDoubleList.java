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

import dart.runtime.DartRuntime;

import java.util.Arrays;

import dart.runtime.Funcs;

/**
 * A {@link DartList}&lt;Double&gt; backed by a primitive {@code double[]} — the transpiler targets this for
 * Dart {@code List&lt;double&gt;}. The hot index/add paths ({@link #getDouble(long)}, {@link #setDouble(long, double)},
 * {@link #addDouble(double)}) never box; the inherited list surface still works (boxing only at that
 * generic boundary) because {@link DartList} routes through the overridden {@code get/set/size/add}.
 */
public class DartDoubleList extends DartList<Double> {

    private double[] a;
    private int len;

    public DartDoubleList() {
        super(true);
        a = new double[8];
    }

    /// For a typed-data view (dart.typed_data) that keeps its own storage: fixed
    /// length, and every storage accessor overridden by the subclass.
    protected DartDoubleList(boolean fixedLength) {
        super(!fixedLength, false);
    }

    private DartDoubleList(double[] a, int len, boolean growable) {
        this(a, len, growable, false);
    }

    private DartDoubleList(double[] a, int len, boolean growable, boolean readOnly) {
        super(growable, readOnly);
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
        RangeError.checkNotNegative(length, "length");
        RangeError.checkAllocatable(length);
        int n = (int) length;
        double[] backing = new double[Math.max(8, n)];
        Arrays.fill(backing, 0, n, fill);
        return new DartDoubleList(backing, n, growable);
    }

    public static DartDoubleList filled(long length, double fill) {
        RangeError.checkNotNegative(length, "length");
        RangeError.checkAllocatable(length);
        return filled(length, fill, false);
    }

    // Named *Doubles (mirroring ofDoubles) to avoid an erasure clash with the
    // inherited DartList generic generate/from statics.

    /** Dart's List&lt;double&gt;.generate(length, generator). */
    public static DartDoubleList generateDoubles(long length, Funcs.Func1<Long, Double> generator, boolean growable) {
        RangeError.checkNotNegative(length, "length");
        RangeError.checkAllocatable(length);
        int n = (int) length;
        double[] backing = new double[Math.max(8, n)];
        for (int i = 0; i < n; i++) {
            backing[i] = generator.call((long) i);
        }
        return new DartDoubleList(backing, n, growable);
    }

    public static DartDoubleList generateDoubles(long length, Funcs.Func1<Long, Double> generator) {
        RangeError.checkNotNegative(length, "length");
        RangeError.checkAllocatable(length);
        return generateDoubles(length, generator, true);
    }

    /** Dart's List&lt;double&gt;.from(elements). */
    /** {@link #fromDoubles(Iterable)} with Dart's {@code growable:} flag. */
    public static DartDoubleList fromDoubles(Iterable<? extends Number> elements, boolean growable) {
        DartDoubleList grown = fromDoubles(elements);
        return new DartDoubleList(grown.a, grown.len, growable);
    }

    /// `value is List<double>`. The primitive-backed list always is one; a boxed DartList is
    /// one when every element is a double. Generic code (`List<E>.of(..)` with E bound to
    /// double at the use site) builds the boxed form, and type arguments are erased here, so
    /// the elements are the only witness the runtime has of the argument. An empty boxed list
    /// answers true, as an erased `is List<String>` does.
    public static boolean isDoubles(Object value) {
        if (value instanceof DartDoubleList) {
            return true;
        }
        if (!(value instanceof DartList)) {
            return false;
        }
        DartList<?> l = (DartList<?>) value;
        if (l.elementWitness() != null) {
            // A boxed list that recorded its type argument answers by it, not its elements.
            return dart.runtime.TypeWitness.isSubtype(l.elementWitness(), ELEMENT);
        }
        int n = l.size();
        for (int i = 0; i < n; i++) {
            Object e = l.get(i);
            if (!(e instanceof Double || e instanceof Float)) {
                return false;
            }
        }
        return true;
    }

    /// A List<double> value held as the primitive-backed list its static type promises:
    /// the value itself when it already is one, otherwise a copy. A generic runtime
    /// method (`Stream<int>.toList()`) builds a boxed DartList, which a local typed
    /// List<double> cannot hold.
    @SuppressWarnings("unchecked")
    /// A value a List<double> is statically typed as, as the DartDoubleList that type is held in:
    /// itself when it already is one, else a live DoubleListView over the boxed DartList that
    /// generic code built (`List<E>.of(..)` with E bound to double at the use site). Unlike
    /// asDoubles this never copies, so writes through either side reach the same list.
    public static DartDoubleList viewDoubles(Object value) {
        if (value == null || value instanceof DartDoubleList) {
            return (DartDoubleList) value;
        }
        if (value instanceof DartList) {
            return new DoubleListView((DartList<Double>) value);
        }
        throw new TypeError("type '" + value.getClass().getSimpleName()
                + "' is not a subtype of type 'List<double>'");
    }

    @SuppressWarnings("unchecked")
    public static DartDoubleList asDoubles(Object value) {
        if (value == null || value instanceof DartDoubleList) {
            return (DartDoubleList) value;
        }
        return fromDoubles((Iterable<? extends Number>) value);
    }

    /// `List<double>.from(source)` whatever the source's static type (`List<dynamic>`,
    /// `dynamic`, an `Iterable<num>`): every element is tested as an Object before it is
    /// read as a number, so a String in a dynamic list is Dart's TypeError rather than a
    /// cast that ParparVM would not check.
    public static DartDoubleList fromDoublesChecked(Object source, boolean growable) {
        if (!(source instanceof Iterable)) {
            throw new TypeError("type '" + DartRuntime.str(dart.runtime.TypeWitness.runtimeType(source))
                    + "' is not a subtype of type 'Iterable<dynamic>'");
        }
        DartDoubleList l = new DartDoubleList();
        for (Object e : (Iterable<?>) source) {
            if (!(e instanceof Double || e instanceof Float)) {
                throw new TypeError("type '" + DartRuntime.str(dart.runtime.TypeWitness.runtimeType(e))
                        + "' is not a subtype of type 'double'");
            }
            l.addDouble(((Number) e).doubleValue());
        }
        return growable ? l : new DartDoubleList(l.a, l.len, false);
    }

    public static DartDoubleList fromDoubles(Iterable<? extends Number> elements) {
        DartDoubleList l = new DartDoubleList();
        for (Number e : elements) {
            // As List<int>.from: the element must BE a double; an int is not one in Dart.
            if (!(e instanceof Double || e instanceof Float)) {
                throw new TypeError("type '" + (e == null ? "Null" : "int")
                        + "' is not a subtype of type 'double'");
            }
            l.addDouble(e.doubleValue());
        }
        return l;
    }

    /** Dart's {@code List<double>.unmodifiable(source)}: a copy that refuses every write. */
    public static DartDoubleList unmodifiableDoubles(Iterable<? extends Number> elements) {
        DartDoubleList copy = fromDoubles(elements);
        return new DartDoubleList(copy.a, copy.len, false, true);
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
        checkWritable();
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

    private static final dart.runtime.TypeWitness ELEMENT = dart.runtime.TypeWitness.of(Double.class, false);

    @Override
    public dart.runtime.TypeWitness elementWitness() {
        dart.runtime.TypeWitness w = super.elementWitness();
        return w != null ? w : ELEMENT;
    }

    /// The boxed boundary of a covariant write: a List<double> held as a List<num> or
    /// List<Object> refuses anything that is no double with Dart's TypeError rather than a
    /// ClassCastException.
    protected static Double checkedDouble(Object e) {
        if (e instanceof Double) {
            return (Double) e;
        }
        throw new TypeError("type '" + DartRuntime.str(dart.runtime.TypeWitness.runtimeType(e))
                + "' is not a subtype of type 'double' of 'value'");
    }

    // --- storage accessors (boxed boundary for the inherited surface) ---

    @Override
    public Double get(int index) {
        RangeError.checkValidIndex(index, len);
        return a[index];
    }

    @Override
    protected Object setElement(int index, Object element$in) {
        Double element = checkedDouble(element$in);
        checkWritable();
        RangeError.checkValidIndex(index, len);
        double old = a[index];
        a[index] = element;
        return old;
    }

    @Override
    public int size() {
        return len;
    }

    /// Growing a List<double> would fill it with null, which Dart rejects as a TypeError;
    /// shrinking truncates.
    @Override
    void setLengthUnchecked(int n) {
        if (n > len) {
            throw new TypeError("type 'Null' is not a subtype of type 'double'");
        }
        len = n;
        modCount++;
    }

    @Override
    protected boolean addElement(Object e$in) {
        Double e = checkedDouble(e$in);
        return addDouble(e);
    }

    @Override
    protected void insertElement(int index, Object element$in) {
        Double element = checkedDouble(element$in);
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

    /**
     * Dart's {@code sublist(start, end)}, typed as this class: a {@code List<double>}
     * is emitted as a {@code DartDoubleList}, so the inherited {@code DartList} result was
     * not assignable back to the variable that holds it.
     */
    @Override
    public DartDoubleList sublist(long start, long end) {
        RangeError.checkValueInInterval(start, 0, len, "start");
        RangeError.checkValueInInterval(end, start, len, "end");
        int n = (int) (end - start);
        double[] backing = new double[Math.max(8, n)];
        System.arraycopy(a, (int) start, backing, 0, n);
        return new DartDoubleList(backing, n, true);
    }

    /** {@link DartList#sublist(long, Long)}, keeping the primitive-backed list type. */
    @Override
    public DartDoubleList sublist(long start, Long end) {
        return end == null ? sublist(start) : sublist(start, end.longValue());
    }

    /** Dart's {@code sublist(start)}; see {@link #sublist(long, long)}. */
    @Override
    public DartDoubleList sublist(long start) {
        return sublist(start, len);
    }

    /**
     * {@code list[i] = 5} on a {@code List<double>}: Dart converts the int literal to
     * a double in that context, but the emitted Java hands over a {@code long}, which
     * {@code idxSet(long, Double)} does not accept.
     */
    public Double idxSet(long index, long value) {
        return idxSet(index, Double.valueOf((double) value));
    }
}
