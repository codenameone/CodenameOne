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
import dart.runtime.Funcs;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.RandomAccess;

/**
 * Dart's List&lt;E&gt;: growable, insertion-ordered. Extends
 * java.util.AbstractList so it interoperates with any Java/CN1 API for free,
 * and adds the Dart API surface the transpiler targets.
 *
 * <p>Indexes in the Dart API arrive as {@code long} (Dart int); they are
 * range-checked with Dart's RangeError semantics.</p>
 *
 * <p>All the Dart-API methods route element access through the overridable
 * {@link #get(int)}/{@link #set(int, Object)}/{@link #size()}/{@link #add(Object)}
 * accessors, so a subclass backed by a primitive array (see
 * {@link DartLongList}, {@link DartDoubleList}) inherits the whole surface
 * while avoiding boxing on the hot index/add paths.</p>
 */
public class DartList<E> extends AbstractList<E> implements RandomAccess {

    /// Identity, as Dart's own {@code List} has: {@code [1] == [1]} is false in Dart
    /// unless a type overrides {@code operator ==}. The inherited Java equality is
    /// structural, so two separately built collections compared equal and, used as
    /// keys, collapsed into one entry of a map -- different control flow and lost
    /// data in a transpiled application. Element-wise comparison is what Dart's
    /// listEquals, mapEquals and setEquals are for, and they say so explicitly.
    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    /// Consistent with {@link #equals}: identity.
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    private final ArrayList<E> impl;
    private final boolean growable;

    public DartList() {
        this.impl = new ArrayList<>();
        this.growable = true;
    }

    DartList(ArrayList<E> impl, boolean growable) {
        this.impl = impl;
        this.growable = growable;
    }

    /** Subclass hook: primitive-backed lists pass their own storage marker. */
    DartList(boolean growable) {
        this.impl = null;
        this.growable = growable;
    }

    /** Literal helper: DartList.of(a, b, c) for Dart's [a, b, c]. */
    @SafeVarargs
    public static <E> DartList<E> of(E... elements) {
        DartList<E> l = new DartList<>();
        for (E e : elements) {
            l.impl.add(e);
        }
        return l;
    }

    public static <E> DartList<E> from(Iterable<E> elements) {
        DartList<E> l = new DartList<>();
        for (E e : elements) {
            l.impl.add(e);
        }
        return l;
    }

    /** Dart's List.filled(length, fill). */
    public static <E> DartList<E> filled(long length, E fill, boolean growable) {
        ArrayList<E> impl = new ArrayList<>();
        for (long i = 0; i < length; i++) {
            impl.add(fill);
        }
        return new DartList<>(impl, growable);
    }

    public static <E> DartList<E> filled(long length, E fill) {
        return filled(length, fill, false);
    }

    /** Dart's List.generate(length, generator). */
    public static <E> DartList<E> generate(long length, Funcs.Func1<Long, E> generator, boolean growable) {
        ArrayList<E> impl = new ArrayList<>();
        for (long i = 0; i < length; i++) {
            impl.add(generator.call(i));
        }
        return new DartList<>(impl, growable);
    }

    public static <E> DartList<E> generate(long length, Funcs.Func1<Long, E> generator) {
        return generate(length, generator, true);
    }

    final boolean isGrowable() {
        return growable;
    }

    void checkGrowable(String op) {
        if (!growable) {
            throw new UnsupportedError(op + " on a fixed-length list");
        }
    }

    // ------------------------------------------------------------------
    // java.util.List plumbing — the storage accessors (overridden by
    // primitive-backed subclasses). Everything below routes through these.
    // ------------------------------------------------------------------

    @Override
    public E get(int index) {
        RangeError.checkValidIndex(index, impl.size());
        return impl.get(index);
    }

    @Override
    public E set(int index, E element) {
        RangeError.checkValidIndex(index, impl.size());
        return impl.set(index, element);
    }

    @Override
    public int size() {
        return impl.size();
    }

    @Override
    public boolean add(E e) {
        checkGrowable("add");
        return impl.add(e);
    }

    @Override
    public void add(int index, E element) {
        checkGrowable("insert");
        impl.add(index, element);
    }

    @Override
    public E remove(int index) {
        checkGrowable("removeAt");
        RangeError.checkValidIndex(index, size());
        return impl.remove(index);
    }

    // ------------------------------------------------------------------
    // Dart API (long-indexed) — routed through the accessors above
    // ------------------------------------------------------------------

    /** Dart's list[i]. */
    public E idx(long index) {
        RangeError.checkValidIndex(index, size());
        return get((int) index);
    }

    /** Dart's list[i] = v. */
    public E idxSet(long index, E value) {
        RangeError.checkValidIndex(index, size());
        set((int) index, value);
        return value;
    }

    public long length() {
        return size();
    }

    public boolean isNotEmpty() {
        return size() != 0;
    }

    public E first() {
        if (size() == 0) {
            throw new StateError("No element");
        }
        return get(0);
    }

    public E last() {
        if (size() == 0) {
            throw new StateError("No element");
        }
        return get(size() - 1);
    }

    public void insert(long index, E element) {
        checkGrowable("insert");
        RangeError.checkValueInInterval(index, 0, size(), "index");
        add((int) index, element);
    }

    public E removeAt(long index) {
        checkGrowable("removeAt");
        RangeError.checkValidIndex(index, size());
        return remove((int) index);
    }

    public E removeLast() {
        checkGrowable("removeLast");
        if (size() == 0) {
            throw new RangeError("RangeError (index): Invalid value: Valid value range is empty: -1");
        }
        return remove(size() - 1);
    }

    /** Dart's List.remove(Object) — removes first match, returns whether found. */
    public boolean removeValue(Object value) {
        checkGrowable("remove");
        for (int i = 0; i < size(); i++) {
            if (DartRuntime.eq(get(i), value)) {
                remove(i);
                return true;
            }
        }
        return false;
    }

    /** Dart's {@code List + List}: a new list with the elements of {@code a} then {@code b}. */
    public static <E> DartList<E> concat(java.util.List<? extends E> a, java.util.List<? extends E> b) {
        DartList<E> r = new DartList<E>();
        if (a != null) {
            r.addAll(a);
        }
        if (b != null) {
            r.addAll(b);
        }
        return r;
    }

    /** Dart's List.addAll — named distinctly because java.util.List.addAll(Collection) makes the overload ambiguous. */
    public void addAllIterable(Iterable<? extends E> elements) {
        checkGrowable("addAll");
        if (elements == this) {
            // list.addAll(list) duplicates the ORIGINAL elements in Dart. Iterating
            // this list while appending to it never ends here: the storage is the
            // backing ArrayList, so AbstractList's modCount never moves, and the
            // iterator neither snapshots the length nor fails fast -- it chases the
            // end forever. Walking the original length by index is the snapshot,
            // and reads through get() so the primitive-backed subclasses work too.
            int n = size();
            for (int i = 0; i < n; i++) {
                add(get(i));
            }
            return;
        }
        for (E e : elements) {
            add(e);
        }
    }

    /** Dart's List.indexOf — long-typed; named to avoid clashing with java.util.List.indexOf(Object). */
    public long indexOfDart(E element) {
        for (int i = 0; i < size(); i++) {
            if (DartRuntime.eq(get(i), element)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Dart's List.contains, with Dart's {@code ==} like removeValue and
     * indexOfDart above. The inherited AbstractList version compares with Java
     * equals, so a List&lt;num&gt; holding the int 1 answered false for 1.0.
     */
    @Override
    public boolean contains(Object element) {
        for (int i = 0; i < size(); i++) {
            if (DartRuntime.eq(get(i), element)) {
                return true;
            }
        }
        return false;
    }

    /** Dart's {@code List.indexWhere(test, [start])}. */
    public long indexWhere(Funcs.Func1<E, Boolean> test, long start) {
        for (int i = (int) Math.max(0, start); i < size(); i++) {
            if (Boolean.TRUE.equals(test.call(get(i)))) {
                return i;
            }
        }
        return -1;
    }

    public long indexWhere(Funcs.Func1<E, Boolean> test) {
        return indexWhere(test, 0);
    }

    /** Dart's {@code List.lastIndexWhere(test, [start])}. */
    public long lastIndexWhere(Funcs.Func1<E, Boolean> test) {
        for (int i = size() - 1; i >= 0; i--) {
            if (Boolean.TRUE.equals(test.call(get(i)))) {
                return i;
            }
        }
        return -1;
    }

    /** Dart's {@code List.removeWhere(test)} — removes every matching element. */
    public void removeWhere(Funcs.Func1<E, Boolean> test) {
        checkGrowable("removeWhere");
        for (int i = size() - 1; i >= 0; i--) {
            if (Boolean.TRUE.equals(test.call(get(i)))) {
                remove(i);
            }
        }
    }

    /** Dart's {@code List.retainWhere(test)} — keeps only matching elements. */
    public void retainWhere(Funcs.Func1<E, Boolean> test) {
        checkGrowable("retainWhere");
        for (int i = size() - 1; i >= 0; i--) {
            if (!Boolean.TRUE.equals(test.call(get(i)))) {
                remove(i);
            }
        }
    }

    public DartList<E> sublist(long start, long end) {
        RangeError.checkValueInInterval(start, 0, size(), "start");
        RangeError.checkValueInInterval(end, start, size(), "end");
        DartList<E> l = new DartList<>();
        for (long i = start; i < end; i++) {
            l.add(get((int) i));
        }
        return l;
    }

    public DartList<E> sublist(long start) {
        return sublist(start, size());
    }

    @SuppressWarnings("unchecked")
    public void sort(Funcs.Func2<E, E, Long> compare) {
        int n = size();
        Object[] arr = new Object[n];
        for (int i = 0; i < n; i++) {
            arr[i] = get(i);
        }
        if (compare == null) {
            java.util.Arrays.sort(arr);
        } else {
            java.util.Arrays.sort(arr, (a, b) -> {
                long r = compare.call((E) a, (E) b);
                return r < 0 ? -1 : (r > 0 ? 1 : 0);
            });
        }
        for (int i = 0; i < n; i++) {
            set(i, (E) arr[i]);
        }
    }

    public void sortDefault() {
        sort((Funcs.Func2<E, E, Long>) null);
    }

    public DartIterable<E> reversed() {
        DartList<E> self = this;
        return DartIterable.wrap(() -> new java.util.Iterator<E>() {
            private int i = self.size() - 1;

            @Override
            public boolean hasNext() {
                return i >= 0;
            }

            @Override
            public E next() {
                return self.get(i--);
            }
        });
    }

    // Dart iterable combinators, delegating to a lazy view.

    public DartIterable<E> asIterable() {
        return DartIterable.wrap(this);
    }

    public <R> DartIterable<R> map(Funcs.Func1<E, R> f) {
        return asIterable().map(f);
    }

    public DartIterable<E> where(Funcs.Func1<E, Boolean> test) {
        return asIterable().where(test);
    }

    public E firstWhere(Funcs.Func1<E, Boolean> test, Funcs.Func0<E> orElse) {
        return asIterable().firstWhere(test, orElse);
    }

    /** Dart's Iterable.elementAt(index) — O(1) for the random-access list. */
    public E elementAt(long index) {
        RangeError.checkValidIndex(index, size());
        return get((int) index);
    }

    public boolean any(Funcs.Func1<E, Boolean> test) {
        return asIterable().any(test);
    }

    public boolean every(Funcs.Func1<E, Boolean> test) {
        return asIterable().every(test);
    }

    public <R> R fold(R initialValue, Funcs.Func2<R, E, R> combine) {
        return asIterable().fold(initialValue, combine);
    }

    public E reduce(Funcs.Func2<E, E, E> combine) {
        return asIterable().reduce(combine);
    }

    public <R> DartIterable<R> expand(Funcs.Func1<E, Iterable<R>> f) {
        return asIterable().expand(f);
    }

    public <T> DartIterable<T> whereType(Class<T> type) {
        return asIterable().whereType(type);
    }

    public DartIterable<E> followedBy(Iterable<E> other) {
        return asIterable().followedBy(other);
    }

    public DartIterable<E> take(long count) {
        return asIterable().take(count);
    }

    public DartIterable<E> skip(long count) {
        return asIterable().skip(count);
    }

    public DartMap<Long, E> asMap() {
        return asIterable().asMap();
    }

    public E lastWhere(Funcs.Func1<E, Boolean> test, Funcs.Func0<E> orElse) {
        return asIterable().lastWhere(test, orElse);
    }

    public E singleWhere(Funcs.Func1<E, Boolean> test, Funcs.Func0<E> orElse) {
        return asIterable().singleWhere(test, orElse);
    }

    /** Dart's {@code List.getRange(start, end)} — a lazy view over a sub-range. */
    public DartIterable<E> getRange(long start, long end) {
        return sublist(start, end).asIterable();
    }

    /** Dart's {@code List.unmodifiable(source)} — a fixed-length copy. */
    public static <E> DartList<E> unmodifiable(Iterable<? extends E> source) {
        ArrayList<E> impl = new ArrayList<>();
        if (source != null) {
            for (E e : source) {
                impl.add(e);
            }
        }
        return new DartList<>(impl, false);
    }

    public String join(String separator) {
        return asIterable().join(separator);
    }

    public String join() {
        return join("");
    }

    public void forEachDart(Funcs.VoidFunc1<E> action) {
        // Named forEachDart because AbstractList inherits Java's forEach(Consumer).
        for (int i = 0, n = size(); i < n; i++) {
            action.call(get(i));
        }
    }

    public DartList<E> toList() {
        return DartList.from(this);
    }

    public DartSet<E> toSet() {
        DartSet<E> s = new DartSet<E>();
        s.addAll(this);
        return s;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(DartRuntime.str(get(i)));
        }
        return sb.append("]").toString();
    }
}
