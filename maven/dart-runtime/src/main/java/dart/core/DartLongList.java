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
 * A {@link DartList}&lt;Long&gt; backed by a primitive {@code long[]} — the transpiler targets this for
 * Dart {@code List&lt;int&gt;}. The hot index/add paths ({@link #getLong(long)}, {@link #setLong(long, long)},
 * {@link #addLong(long)}) never box; the inherited Dart/Java list surface still works (boxing only at
 * that generic boundary) because {@link DartList} routes through the overridden {@code get/set/size/add}.
 */
public class DartLongList extends DartList<Long> {

    private long[] a;
    private int len;

    public DartLongList() {
        super(true);
        a = new long[8];
    }

    /// For a typed-data view (dart.typed_data) that keeps its own storage: fixed
    /// length, and every storage accessor overridden by the subclass.
    protected DartLongList(boolean fixedLength) {
        super(!fixedLength, false);
    }

    private DartLongList(long[] a, int len, boolean growable) {
        this(a, len, growable, false);
    }

    private DartLongList(long[] a, int len, boolean growable, boolean readOnly) {
        super(growable, readOnly);
        this.a = a;
        this.len = len;
    }

    /** Literal helper for Dart's &lt;int&gt;[a, b, c]. */
    public static DartLongList ofLongs(long... elements) {
        long[] backing = elements.length == 0 ? new long[8] : Arrays.copyOf(elements, Math.max(8, elements.length));
        return new DartLongList(backing, elements.length, true);
    }

    /** Dart's List&lt;int&gt;.filled(length, fill). */
    public static DartLongList filled(long length, long fill, boolean growable) {
        RangeError.checkNotNegative(length, "length");
        RangeError.checkAllocatable(length);
        int n = (int) length;
        long[] backing = new long[Math.max(8, n)];
        Arrays.fill(backing, 0, n, fill);
        return new DartLongList(backing, n, growable);
    }

    public static DartLongList filled(long length, long fill) {
        RangeError.checkNotNegative(length, "length");
        RangeError.checkAllocatable(length);
        return filled(length, fill, false);
    }

    // Named *Longs (mirroring ofLongs) rather than reusing the DartList generic
    // generate/from names: those would share a JVM erasure with the inherited
    // static methods, which is not a legal hide/override relationship.

    /** Dart's List&lt;int&gt;.generate(length, generator). */
    public static DartLongList generateLongs(long length, Funcs.Func1<Long, Long> generator, boolean growable) {
        RangeError.checkNotNegative(length, "length");
        RangeError.checkAllocatable(length);
        int n = (int) length;
        long[] backing = new long[Math.max(8, n)];
        for (int i = 0; i < n; i++) {
            backing[i] = generator.call((long) i);
        }
        return new DartLongList(backing, n, growable);
    }

    public static DartLongList generateLongs(long length, Funcs.Func1<Long, Long> generator) {
        RangeError.checkNotNegative(length, "length");
        RangeError.checkAllocatable(length);
        return generateLongs(length, generator, true);
    }

    /** Dart's List&lt;int&gt;.from(elements). */
    /** {@link #fromLongs(Iterable)} with Dart's {@code growable:} flag. */
    public static DartLongList fromLongs(Iterable<? extends Number> elements, boolean growable) {
        DartLongList grown = fromLongs(elements);
        return new DartLongList(grown.a, grown.len, growable);
    }

    /// `value is List<int>`. The primitive-backed list always is one; a boxed DartList is
    /// one when every element is an int. Generic code (`List<E>.of(..)` with E bound to
    /// int at the use site) builds the boxed form, and type arguments are erased here, so
    /// the elements are the only witness the runtime has of the argument. An empty boxed list
    /// answers true, as an erased `is List<String>` does.
    public static boolean isLongs(Object value) {
        if (value instanceof DartLongList) {
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
            if (!(e instanceof Long || e instanceof Integer || e instanceof Short || e instanceof Byte)) {
                return false;
            }
        }
        return true;
    }

    /// A List<int> value held as the primitive-backed list its static type promises:
    /// the value itself when it already is one, otherwise a copy. A generic runtime
    /// method (`Stream<int>.toList()`) builds a boxed DartList, which a local typed
    /// List<int> cannot hold.
    @SuppressWarnings("unchecked")
    /// A value a List<int> is statically typed as, as the DartLongList that type is held in:
    /// itself when it already is one, else a live LongListView over the boxed DartList that
    /// generic code built (`List<E>.of(..)` with E bound to int at the use site). Unlike
    /// asLongs this never copies, so writes through either side reach the same list.
    public static DartLongList viewLongs(Object value) {
        if (value == null || value instanceof DartLongList) {
            return (DartLongList) value;
        }
        if (value instanceof DartList) {
            return new LongListView((DartList<Long>) value);
        }
        throw new TypeError("type '" + value.getClass().getSimpleName()
                + "' is not a subtype of type 'List<int>'");
    }

    @SuppressWarnings("unchecked")
    public static DartLongList asLongs(Object value) {
        if (value == null || value instanceof DartLongList) {
            return (DartLongList) value;
        }
        return fromLongs((Iterable<? extends Number>) value);
    }

    public static DartLongList fromLongs(Iterable<? extends Number> elements) {
        DartLongList l = new DartLongList();
        for (Number e : elements) {
            // Dart checks each element's type: List<int>.from(<num>[1.9]) throws, where
            // longValue() quietly made it 1 -- and a double holding 1.0 is no int either.
            if (!(e instanceof Long || e instanceof Integer || e instanceof Short || e instanceof Byte)) {
                throw new TypeError("type '" + (e == null ? "Null" : "double")
                        + "' is not a subtype of type 'int'");
            }
            l.addLong(e.longValue());
        }
        return l;
    }

    /** Dart's {@code List<int>.unmodifiable(source)}: a copy that refuses every write. */
    public static DartLongList unmodifiableLongs(Iterable<? extends Number> elements) {
        DartLongList copy = fromLongs(elements);
        return new DartLongList(copy.a, copy.len, false, true);
    }

    private void ensure(int cap) {
        if (cap > a.length) {
            a = Arrays.copyOf(a, Math.max(cap, a.length * 2));
        }
    }

    // --- primitive fast paths (transpiler targets these for List<int>) ---

    public long getLong(long index) {
        // Inline bounds check (against the logical length, which can be < backing
        // capacity) + cold throw helper, so this stays a frameless method — see
        // RangeError.indexError. Avoids a full-frame call per index access.
        if (index < 0 || index >= len) {
            RangeError.indexError(index, len);
        }
        return a[(int) index];
    }

    public long setLong(long index, long value) {
        checkWritable();
        if (index < 0 || index >= len) {
            RangeError.indexError(index, len);
        }
        a[(int) index] = value;
        return value;
    }

    public boolean addLong(long value) {
        checkGrowable("add");
        ensure(len + 1);
        a[len++] = value;
        modCount++;
        return true;
    }

    private static final dart.runtime.TypeWitness ELEMENT = dart.runtime.TypeWitness.of(Long.class, false);

    @Override
    public dart.runtime.TypeWitness elementWitness() {
        dart.runtime.TypeWitness w = super.elementWitness();
        return w != null ? w : ELEMENT;
    }

    /// The boxed boundary of a covariant write: a List<int> held as a List<num> or
    /// List<Object> refuses anything that is no int with Dart's TypeError rather than a
    /// ClassCastException.
    protected static Long checkedLong(Object e) {
        if (e instanceof Long) {
            return (Long) e;
        }
        throw new TypeError("type '" + DartRuntime.str(dart.runtime.TypeWitness.runtimeType(e))
                + "' is not a subtype of type 'int' of 'value'");
    }

    // --- storage accessors (boxed boundary for the inherited surface) ---

    @Override
    public Long get(int index) {
        RangeError.checkValidIndex(index, len);
        return a[index];
    }

    @Override
    protected Object setElement(int index, Object element$in) {
        Long element = checkedLong(element$in);
        checkWritable();
        RangeError.checkValidIndex(index, len);
        long old = a[index];
        a[index] = element;
        return old;
    }

    @Override
    public int size() {
        return len;
    }

    /// Growing a List<int> would fill it with null, which Dart rejects as a TypeError;
    /// shrinking truncates.
    @Override
    void setLengthUnchecked(int n) {
        if (n > len) {
            throw new TypeError("type 'Null' is not a subtype of type 'int'");
        }
        len = n;
        modCount++;
    }

    @Override
    protected boolean addElement(Object e$in) {
        Long e = checkedLong(e$in);
        return addLong(e);
    }

    @Override
    protected void insertElement(int index, Object element$in) {
        Long element = checkedLong(element$in);
        checkGrowable("insert");
        ensure(len + 1);
        System.arraycopy(a, index, a, index + 1, len - index);
        a[index] = element;
        len++;
        modCount++;
    }

    @Override
    public Long remove(int index) {
        checkGrowable("removeAt");
        RangeError.checkValidIndex(index, len);
        long old = a[index];
        System.arraycopy(a, index + 1, a, index, len - index - 1);
        len--;
        modCount++;
        return old;
    }

    /**
     * Dart's {@code sublist(start, end)}, typed as this class: a {@code List<int>}
     * is emitted as a {@code DartLongList}, so the inherited {@code DartList} result was
     * not assignable back to the variable that holds it.
     */
    @Override
    public DartLongList sublist(long start, long end) {
        RangeError.checkValueInInterval(start, 0, len, "start");
        RangeError.checkValueInInterval(end, start, len, "end");
        int n = (int) (end - start);
        long[] backing = new long[Math.max(8, n)];
        System.arraycopy(a, (int) start, backing, 0, n);
        return new DartLongList(backing, n, true);
    }

    /** {@link DartList#sublist(long, Long)}, keeping the primitive-backed list type. */
    @Override
    public DartLongList sublist(long start, Long end) {
        return end == null ? sublist(start) : sublist(start, end.longValue());
    }

    /** Dart's {@code sublist(start)}; see {@link #sublist(long, long)}. */
    @Override
    public DartLongList sublist(long start) {
        return sublist(start, len);
    }
}
