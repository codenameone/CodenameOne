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
     *
     * <p>NaN elements are deliberately not modelled: Java's {@code Double.equals} calls
     * NaN equal to itself, so a set keeps one NaN and {@code contains(double.nan)} is
     * true, where Dart's {@code ==} says NaN equals nothing. Matching Dart would mean a
     * set that holds any number of elements equal to nothing, which a
     * {@link LinkedHashSet} cannot do without wrapping every double it stores -- a cost
     * on every numeric set paid for sets of NaN, which no Flutter application relies on.</p>
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

    /// Set by {@link #constant} for a Dart {@code const} set literal: every write throws.
    private boolean readOnly;

    /**
     * Freezes a set the transpiler just built for a Dart {@code const} literal and
     * returns it. In place: the literal is fresh and nothing else holds it yet.
     */
    public static <S extends DartSet<?>> S constant(S set) {
        ((DartSet<?>) set).readOnly = true;
        return set;
    }

    private void checkWritable() {
        if (readOnly) {
            throw new UnsupportedError("Cannot change an unmodifiable set");
        }
    }

    /// Checks {@link #structure} on every step, hasNext() included, as Dart's moveNext
    /// does: Java's iterator checks only in next(), so a loop body that cleared the set
    /// ended the loop quietly, and when it did notice it threw Java's exception, which
    /// no {@code on ConcurrentModificationError} clause catches. A frozen set's
    /// remove() is refused, which covers removeWhere, retainWhere and the inherited
    /// Java bulk removals.
    @Override
    public java.util.Iterator<E> iterator() {
        return checkedIterator(super.iterator());
    }

    /// The Dart iterator over {@code it}: what {@link #iterator} answers, and what a
    /// subclass with its own storage (DartIdentitySet) wraps its storage's iterator in,
    /// so a change made through the set is reported on the next step there too.
    protected final java.util.Iterator<E> checkedIterator(final java.util.Iterator<E> it) {
        return new java.util.Iterator<E>() {
            int expected = structure;

            @Override
            public boolean hasNext() {
                check();
                return it.hasNext();
            }

            @Override
            public E next() {
                check();
                return it.next();
            }

            @Override
            public void remove() {
                checkWritable();
                // Removal through the iterator is the one sanctioned change.
                it.remove();
                structure++;
                expected = structure;
            }

            private void check() {
                if (structure != expected) {
                    throw new ConcurrentModificationError("set modified during iteration");
                }
            }
        };
    }

    @Override
    public boolean add(E e) {
        checkWritable();
        boolean added = storedElement(e) == e && super.add(e);
        if (added) {
            structure++;
        }
        return added;
    }

    @Override
    public boolean remove(Object e) {
        checkWritable();
        boolean removed = super.remove(storedElement(e));
        if (removed) {
            structure++;
        }
        return removed;
    }

    @Override
    public void clear() {
        checkWritable();
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

    /**
     * An empty set of this set's own kind (plain or identity) -- what {@link #toSet},
     * {@link #union}, {@link #intersection} and {@link #difference} copy into. Dart's own
     * {@code _HashSet} picks this through an identical {@code _newSet()} override in
     * {@code _IdentityHashSet}; without it, every one of those on an identity set
     * silently collapsed distinct-but-equal elements into one, because the copy came
     * back a plain, equality-based set. {@link #from}/{@link #of} are NOT routed through
     * this: Dart's own {@code Set.from}/{@code Set.of} constructors always build a plain
     * set regardless of the iterable's own kind, and that is unaffected by the receiver
     * here.
     */
    DartSet<E> newEmpty() {
        return new DartSet<E>();
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

    /** Dart's {@code Set.difference(other)} -- elements not in {@code other}, same kind as this. */
    public DartSet<E> difference(java.util.Set<?> other) {
        DartSet<E> s = newEmpty();
        for (E e : this) {
            if (other == null || !other.contains(e)) {
                s.add(e);
            }
        }
        return s;
    }

    /** Dart's {@code Set.intersection(other)} -- elements also in {@code other}, same kind as this. */
    public DartSet<E> intersection(java.util.Set<?> other) {
        DartSet<E> s = newEmpty();
        for (E e : this) {
            if (other != null && other.contains(e)) {
                s.add(e);
            }
        }
        return s;
    }

    /** Dart's {@code Set.union(other)} -- elements in either set, same kind as this. */
    public DartSet<E> union(java.util.Set<? extends E> other) {
        DartSet<E> s = newEmpty();
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

    /**
     * Dart's {@code Set.addAll(elements)} -- named to avoid the Collection overload.
     * Checked up front, not just through {@link #add}'s own check: an EMPTY {@code
     * elements} never reaches {@link #add}, so a loop that only checked there left a
     * read-only set's {@code addAll([])} silently a no-op instead of throwing.
     */
    public void addAllIterable(Iterable<? extends E> elements) {
        checkWritable();
        if (elements != null) {
            for (E e : elements) {
                add(e);
            }
        }
    }

    /**
     * Overrides {@code java.util.Set.addAll(Collection)} so a read-only set's {@code
     * addAll([])} throws through the plain {@code java.util.Collection} surface too,
     * for the same reason {@link #addAllIterable} now checks up front: the inherited
     * {@code AbstractCollection.addAll(Collection)} loop only ever reaches {@link
     * #add}, which an empty argument never calls.
     */
    @Override
    public boolean addAll(java.util.Collection<? extends E> c) {
        checkWritable();
        return super.addAll(c);
    }

    /**
     * Dart's {@code Set.removeAll(elements)}: removes every element of {@code elements}
     * that this set has, matched by this set's own equality (identity for an identity
     * set) -- {@link #remove} already gets that right, so this is just a loop over it.
     */
    private boolean removeAllInternal(Iterable<?> elements) {
        boolean modified = false;
        if (elements != null) {
            for (Object o : elements) {
                if (remove(o)) {
                    modified = true;
                }
            }
        }
        return modified;
    }

    /**
     * Checked up front for the same reason {@link #addAllIterable} is: an EMPTY
     * {@code elements} never reaches {@link #remove}, so a read-only set's {@code
     * removeAll([])} was silently a no-op instead of throwing.
     */
    public void removeAll(Iterable<?> elements) {
        checkWritable();
        removeAllInternal(elements);
    }

    /**
     * Overrides {@code java.util.Set.removeAll(Collection)} with the identical semantics
     * as {@link #removeAll(Iterable)} above -- needed because the Java type a transpiled
     * Dart list/set literal gets (DartList, DartSet) is ALSO a {@code java.util.Collection},
     * so the call's static argument type makes the compiler prefer a {@code Collection}
     * parameter over an {@code Iterable} one whenever both are applicable; every ordinary
     * call site lands here, not on the method above. Left unoverridden, the inherited
     * {@code java.util.AbstractSet.removeAll(Collection)} ran instead: its small-collection
     * fast path (iterate the ARGUMENT, call this.remove() per element) happened to stay
     * identity-correct, but only while the argument was smaller than the receiver -- past
     * that size it falls back to the OTHER branch, which asks the ARGUMENT's equality
     * (c.contains(...)) rather than this set's, so an identity set silently went
     * equality-based for exactly the larger argument a quick test would miss.
     */
    @Override
    public boolean removeAll(java.util.Collection<?> c) {
        checkWritable();
        return removeAllInternal(c);
    }

    /**
     * Dart's {@code Set.retainAll(elements)}: keeps only the elements of this set that
     * are also elements of {@code elements}, matched by this set's own equality --
     * identity for an identity set, {@code equals} otherwise (measured against the real
     * SDK: {@code Set.identity()..retainAll([equalButNotIdenticalCopy])} removes the
     * element, where a plain set's {@code retainAll} keeps it).
     *
     * <p>{@code elements} is a loose {@code Iterable}, not necessarily a fast-lookup
     * {@code Set}, and even when it is one its OWN equality notion may differ from this
     * set's -- so it is first copied into a same-kind set ({@link #newEmpty}, the same
     * trick {@link #toSet}/{@link #union}/{@link #intersection}/{@link #difference} use),
     * and that copy's {@code contains} does the matching under the receiver's equality.</p>
     */
    @SuppressWarnings("unchecked")
    private boolean retainAllInternal(Iterable<?> elements) {
        DartSet<E> lookup = newEmpty();
        if (elements != null) {
            for (Object o : elements) {
                lookup.add((E) o);
            }
        }
        boolean modified = false;
        java.util.Iterator<E> it = iterator();
        while (it.hasNext()) {
            if (!lookup.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    public void retainAll(Iterable<?> elements) {
        checkWritable();
        retainAllInternal(elements);
    }

    /**
     * Overrides {@code java.util.Set.retainAll(Collection)} for the same reason as
     * {@link #removeAll(java.util.Collection)} above: a real call site's argument is
     * almost always a {@code Collection} (DartList, DartSet), which the compiler prefers
     * over the {@code Iterable} parameter of {@link #retainAll(Iterable)}. Unlike
     * {@code removeAll}, {@code java.util.AbstractCollection.retainAll(Collection)} has no
     * size-based fast path at all: it unconditionally asks the ARGUMENT's equality
     * (c.contains(...)), so without this override every identity-set {@code retainAll}
     * call was silently equality-based, regardless of argument size.
     */
    @Override
    public boolean retainAll(java.util.Collection<?> c) {
        checkWritable();
        return retainAllInternal(c);
    }

    public void removeWhere(Funcs.Func1<E, Boolean> test) {
        checkWritable();
        java.util.Iterator<E> it = iterator();
        while (it.hasNext()) {
            if (Boolean.TRUE.equals(test.call(it.next()))) {
                it.remove();
            }
        }
    }

    /**
     * Dart's {@code Set.retainWhere(test)}: keeps only the matching elements. The
     * transpiler routes it here for a Set as it does for a List, and only DartList
     * declared it, so every Set call site failed to compile.
     */
    public void retainWhere(Funcs.Func1<E, Boolean> test) {
        checkWritable();
        java.util.Iterator<E> it = iterator();
        while (it.hasNext()) {
            if (!Boolean.TRUE.equals(test.call(it.next()))) {
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

    /** Dart's {@code Set.toSet()} -- a copy of the same kind as this set, per the real SDK. */
    public DartSet<E> toSet() {
        DartSet<E> s = newEmpty();
        s.addAll(this);
        return s;
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

    public DartIterable<E> takeWhile(Funcs.Func1<E, Boolean> test) {
        return asIterable().takeWhile(test);
    }

    public DartIterable<E> skipWhile(Funcs.Func1<E, Boolean> test) {
        return asIterable().skipWhile(test);
    }

    public DartMap<Long, E> asMap() {
        return asIterable().asMap();
    }

    public <T> DartIterable<T> whereType(Class<T> type) {
        return asIterable().whereType(type);
    }

    public <T> DartIterable<T> whereType(Class<T> type, boolean nullable) {
        return asIterable().whereType(type, nullable);
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
