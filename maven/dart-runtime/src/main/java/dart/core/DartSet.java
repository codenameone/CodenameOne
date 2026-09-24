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

import java.util.LinkedHashSet;

/**
 * Dart's Set&lt;E&gt;: insertion-ordered (Dart set literals are LinkedHashSet).
 * Extends {@link LinkedHashSet} for direct Java/CN1 interop and adds the Dart
 * API surface the transpiler targets (set algebra plus the shared
 * {@code Iterable} combinators, which delegate to a lazy {@link DartIterable}
 * view so a {@code Set} used as an iterable emits the same call shapes as a
 * {@code List}).
 */
public class DartSet<E> extends LinkedHashSet<E> {

    /// Identity, as Dart's own {@code Set} has: {@code [1] == [1]} is false in Dart
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

    public DartSet() {
    }

    /**
     * The element this set already holds for {@code e} under Dart's {@code ==}, or
     * {@code e} itself -- {@code <num>{1}.contains(1.0)} is true in Dart, and adding
     * 1.0 leaves the set as {1}. See {@link DartMap#numericTwins}.
     */
    private Object storedElement(Object e) {
        if (!(e instanceof Number) || super.contains(e)) {
            return e;
        }
        for (Object twin : DartMap.numericTwins(e)) {
            if (super.contains(twin)) {
                return twin;
            }
        }
        return e;
    }

    @Override
    public boolean contains(Object e) {
        return super.contains(storedElement(e));
    }

    /// Additions and removals, for forEachDart; see DartMap's counter of the same name.
    private int structure;

    protected final void structuralChange() {
        structure++;
    }

    @Override
    public boolean add(E e) {
        boolean added = storedElement(e) == e && super.add(e);
        if (added) {
            structure++;
        }
        return added;
    }

    @Override
    public boolean remove(Object e) {
        boolean removed = super.remove(storedElement(e));
        if (removed) {
            structure++;
        }
        return removed;
    }

    @Override
    public void clear() {
        if (!isEmpty()) {
            structure++;
        }
        super.clear();
    }

    @SafeVarargs
    public static <E> DartSet<E> of(E... elements) {
        DartSet<E> s = new DartSet<>();
        for (E e : elements) {
            s.add(e);
        }
        return s;
    }

    /** Dart's {@code Set.from(iterable)} / {@code Set.of(iterable)} — copy elements. */
    public static <E> DartSet<E> from(Iterable<? extends E> elements) {
        DartSet<E> s = new DartSet<>();
        if (elements != null) {
            for (E e : elements) {
                s.add(e);
            }
        }
        return s;
    }

    /** Dart's {@code Set.identity()}: elements match only when they are the same object. */
    public static <E> DartSet<E> identity() {
        return new DartIdentitySet<E>();
    }

    public long length() {
        return size();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public DartIterable<E> asIterable() {
        return DartIterable.wrap(this);
    }

    // --- set algebra ---------------------------------------------------

    /** Dart's {@code Set.difference(other)} — elements not in {@code other}. */
    public DartSet<E> difference(java.util.Set<?> other) {
        DartSet<E> s = new DartSet<>();
        for (E e : this) {
            if (other == null || !other.contains(e)) {
                s.add(e);
            }
        }
        return s;
    }

    /** Dart's {@code Set.intersection(other)} — elements also in {@code other}. */
    public DartSet<E> intersection(java.util.Set<?> other) {
        DartSet<E> s = new DartSet<>();
        for (E e : this) {
            if (other != null && other.contains(e)) {
                s.add(e);
            }
        }
        return s;
    }

    /** Dart's {@code Set.union(other)} — elements in either set. */
    public DartSet<E> union(java.util.Set<? extends E> other) {
        DartSet<E> s = new DartSet<>();
        s.addAll(this);
        if (other != null) {
            s.addAll(other);
        }
        return s;
    }

    /** Dart's {@code Set.containsAll(other)}. */
    public boolean containsAll(Iterable<?> other) {
        if (other != null) {
            for (Object o : other) {
                if (!contains(o)) {
                    return false;
                }
            }
        }
        return true;
    }

    // --- mutators mirroring the transpiler's Iterable/Set intrinsics ----

    /** Dart's {@code Set.remove(value)} — returns whether it was present. */
    public boolean removeValue(Object value) {
        return remove(value);
    }

    /** Dart's {@code Set.addAll(elements)} — named to avoid the Collection overload. */
    public void addAllIterable(Iterable<? extends E> elements) {
        if (elements != null) {
            for (E e : elements) {
                add(e);
            }
        }
    }

    public void removeAll(Iterable<?> elements) {
        if (elements != null) {
            for (Object o : elements) {
                remove(o);
            }
        }
    }

    public void removeWhere(Funcs.Func1<E, Boolean> test) {
        java.util.Iterator<E> it = iterator();
        while (it.hasNext()) {
            if (Boolean.TRUE.equals(test.call(it.next()))) {
                it.remove();
            }
        }
    }

    // --- Iterable combinators (delegate to the lazy view) ---------------

    public E first() {
        return asIterable().first();
    }

    public E last() {
        return asIterable().last();
    }

    public <R> DartIterable<R> map(Funcs.Func1<E, R> f) {
        return asIterable().map(f);
    }

    public DartIterable<E> where(Funcs.Func1<E, Boolean> test) {
        return asIterable().where(test);
    }

    /** Dart's {@code toList(growable: ...)}: a fixed-length copy when growable is false. */
    public DartList<E> toList(boolean growable) {
        return asIterable().toList(growable);
    }

    public DartList<E> toList() {
        return asIterable().toList();
    }

    public DartSet<E> toSet() {
        return from(this);
    }

    public String join(String separator) {
        return asIterable().join(separator);
    }

    public String join() {
        return join("");
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

    public E firstWhere(Funcs.Func1<E, Boolean> test, Funcs.Func0<E> orElse) {
        return asIterable().firstWhere(test, orElse);
    }

    public E elementAt(long index) {
        return asIterable().elementAt(index);
    }

    public void forEachDart(Funcs.VoidFunc1<E> action) {
        // As DartMap.forEachDart: checked after every callback, not only on the next step.
        int n = size();
        int s = structure;
        for (E e : this) {
            action.call(e);
            if (size() != n || structure != s) {
                throw new ConcurrentModificationError("set changed during forEach");
            }
        }
    }

    // --- lazy Iterable operations, delegated to the iterable view -------

    public E lastWhere(Funcs.Func1<E, Boolean> test, Funcs.Func0<E> orElse) {
        return asIterable().lastWhere(test, orElse);
    }

    public E singleWhere(Funcs.Func1<E, Boolean> test, Funcs.Func0<E> orElse) {
        return asIterable().singleWhere(test, orElse);
    }

    public E reduce(Funcs.Func2<E, E, E> combine) {
        return asIterable().reduce(combine);
    }

    public <R> DartIterable<R> expand(Funcs.Func1<E, Iterable<R>> f) {
        return asIterable().expand(f);
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

    public <T> DartIterable<T> whereType(Class<T> type) {
        return asIterable().whereType(type);
    }

    @Override
    public String toString() {
        if (!DartRuntime.beginFormat(this)) {
            return "{...}";   // a set that contains itself, as Dart prints it
        }
        try {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (E e : this) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(DartRuntime.str(e));
                first = false;
            }
            return sb.append("}").toString();
        } finally {
            DartRuntime.endFormat(this);
        }
    }
}
