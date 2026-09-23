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
 * A {@link DartList}&lt;Long&gt; backed by a primitive {@code long[]} — the transpiler targets this for
 * Dart {@code List&lt;int&gt;}. The hot index/add paths ({@link #getLong(long)}, {@link #setLong(long, long)},
 * {@link #addLong(long)}) never box; the inherited Dart/Java list surface still works (boxing only at
 * that generic boundary) because {@link DartList} routes through the overridden {@code get/set/size/add}.
 */
public final class DartLongList extends DartList<Long> {

    private long[] a;
    private int len;

    public DartLongList() {
        super(true);
        a = new long[8];
    }

    private DartLongList(long[] a, int len, boolean growable) {
        super(growable);
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
        int n = (int) length;
        long[] backing = new long[Math.max(8, n)];
        Arrays.fill(backing, 0, n, fill);
        return new DartLongList(backing, n, growable);
    }

    public static DartLongList filled(long length, long fill) {
        RangeError.checkNotNegative(length, "length");
        return filled(length, fill, false);
    }

    // Named *Longs (mirroring ofLongs) rather than reusing the DartList generic
    // generate/from names: those would share a JVM erasure with the inherited
    // static methods, which is not a legal hide/override relationship.

    /** Dart's List&lt;int&gt;.generate(length, generator). */
    public static DartLongList generateLongs(long length, Funcs.Func1<Long, Long> generator, boolean growable) {
        RangeError.checkNotNegative(length, "length");
        int n = (int) length;
        long[] backing = new long[Math.max(8, n)];
        for (int i = 0; i < n; i++) {
            backing[i] = generator.call((long) i);
        }
        return new DartLongList(backing, n, growable);
    }

    public static DartLongList generateLongs(long length, Funcs.Func1<Long, Long> generator) {
        RangeError.checkNotNegative(length, "length");
        return generateLongs(length, generator, true);
    }

    /** Dart's List&lt;int&gt;.from(elements). */
    /** {@link #fromLongs(Iterable)} with Dart's {@code growable:} flag. */
    public static DartLongList fromLongs(Iterable<? extends Number> elements, boolean growable) {
        DartLongList grown = fromLongs(elements);
        return new DartLongList(grown.a, grown.len, growable);
    }

    public static DartLongList fromLongs(Iterable<? extends Number> elements) {
        DartLongList l = new DartLongList();
        for (Number e : elements) {
            l.addLong(e.longValue());
        }
        return l;
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

    // --- storage accessors (boxed boundary for the inherited surface) ---

    @Override
    public Long get(int index) {
        RangeError.checkValidIndex(index, len);
        return a[index];
    }

    @Override
    public Long set(int index, Long element) {
        RangeError.checkValidIndex(index, len);
        long old = a[index];
        a[index] = element;
        return old;
    }

    @Override
    public int size() {
        return len;
    }

    @Override
    public boolean add(Long e) {
        return addLong(e);
    }

    @Override
    public void add(int index, Long element) {
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
}
