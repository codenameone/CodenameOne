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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Dart's Iterable&lt;E&gt;: lazy combinators over a source iterable.
 * Implements java.lang.Iterable for zero-friction interop.
 */
public class DartIterable<E> implements Iterable<E> {

    private final Iterable<E> source;

    /// Overrides the default element-by-element scan in {@link #contains}, used only
    /// by {@code DartMap.keys()}/{@code valuesIterable()} so a view over a map's own
    /// keys or values agrees with that map's {@code containsKey}/{@code containsValue}
    /// -- see the field's one use for why a scan is not always the same answer.
    private final Funcs.Func1<Object, Boolean> membershipTest;

    /// The reified element type when an operation knew it (a signed `map` callback's
    /// return, the source's own for `where`/`take`/..), else read from a collection
    /// source; null when nothing could record it.
    private dart.runtime.TypeWitness elem;

    private static <T> DartIterable<T> withElem(DartIterable<T> it, dart.runtime.TypeWitness w) {
        it.elem = w;
        return it;
    }

    /// Records the element witness on {@code it} (null leaves it unknown) and answers it.
    public static <T> DartIterable<T> typed(DartIterable<T> it, dart.runtime.TypeWitness w) {
        if (w != null) {
            it.elem = w;
        }
        return it;
    }

    /// The reified element type: recorded, or the collection this iterable reads.
    public dart.runtime.TypeWitness elementWitness() {
        if (elem != null) {
            return elem;
        }
        return witnessOf(source);
    }

    /// The element witness any runtime iterable records, or null.
    public static dart.runtime.TypeWitness witnessOf(Object it) {
        if (it instanceof DartList) {
            return ((DartList<?>) it).elementWitness();
        }
        if (it instanceof DartSet) {
            return ((DartSet<?>) it).elementWitness();
        }
        if (it instanceof DartIterable) {
            return ((DartIterable<?>) it).elementWitness();
        }
        return null;
    }

    protected DartIterable(Iterable<E> source) {
        this(source, null);
    }

    /// Package-private: only a map's own views need to redirect membership to the
    /// map, so this is not part of the public combinator surface.
    DartIterable(Iterable<E> source, Funcs.Func1<Object, Boolean> membershipTest) {
        this.source = source;
        this.membershipTest = membershipTest;
    }

    /// A live Iterable over a program object applying dart:collection's
    /// {@code IterableMixin} (or {@code ListMixin}), made where it is passed to an
    /// {@code Iterable} parameter: each iteration asks the object for a fresh iterator,
    /// so it sees the object's state at that moment. {@code identical} looks through it.
    public static <E> DartIterable<E> ofMixin(dart.collection.IterableMixin<E> source) {
        return new MixinIterable<E>(source);
    }

    private static final class MixinIterable<E> extends DartIterable<E> implements MixinView {
        private final dart.collection.IterableMixin<E> src;

        MixinIterable(dart.collection.IterableMixin<E> src) {
            super(() -> new java.util.Iterator<E>() {
                private final dart.collection.Iterator<E> it = src.iterator();
                private boolean ready;
                private boolean has;

                @Override
                public boolean hasNext() {
                    if (!ready) {
                        has = it.moveNext();
                        ready = true;
                    }
                    return has;
                }

                @Override
                public E next() {
                    if (!hasNext()) {
                        throw new java.util.NoSuchElementException();
                    }
                    ready = false;
                    return it.current();
                }
            });
            this.src = src;
        }

        @Override
        public Object source() {
            return src;
        }
    }

    /// The Dart list or set this iterable is the plain view of ({@link #wrap} of a
    /// {@link DartList} or {@link DartSet}), else the value itself. A List or Set passed
    /// where an Iterable is expected travels as that view, and is still that List or Set
    /// in Dart: `it is List` / `it is Set` look through it.
    ///
    /// This is the runtime's answer to Dart's `List<E> implements Iterable<E>`. Java
    /// cannot say it: DartList must extend AbstractList and DartSet LinkedHashSet to be
    /// the java.util collections Java APIs take, and dart:core's Iterable is this class,
    /// so neither can also extend it. The transpiler therefore emits a program override
    /// that narrows an Iterable return to List or Set (`Set<T> cast()` over
    /// `Iterable<T> cast()`) with the member's Iterable return, returning this view, and
    /// a call through the narrower static type reads the collection back through here.
    public static Object listBehind(Object value) {
        if (value != null && value.getClass() == DartIterable.class
                && (((DartIterable<?>) value).source instanceof DartList
                        || ((DartIterable<?>) value).source instanceof DartSet)) {
            return ((DartIterable<?>) value).source;
        }
        return value;
    }

    /**
     * The DartIterable a value that already passed Dart's checked downcast to
     * {@code Iterable} (TypeWitness.checkCast) is handed over as: itself when it is one,
     * a live view of any other runtime collection -- a DartList, a primitive-backed
     * DartLongList, an ElementViews view -- and of a program object applying
     * IterableMixin. Null stays null (the cast already decided whether it may be).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object viewOf(Object value) {
        if (value == null || value instanceof DartIterable) {
            return value;
        }
        if (value instanceof Iterable) {
            return new DartIterable((Iterable) value);
        }
        if (value instanceof dart.collection.IterableMixin) {
            return ofMixin((dart.collection.IterableMixin) value);
        }
        throw new TypeError("type '" + dart.runtime.DartRuntime.typeName(value)
                + "' is not a subtype of type 'Iterable'");
    }

    public static <E> DartIterable<E> wrap(Iterable<E> source) {
        return source instanceof DartIterable<E> di ? di : new DartIterable<>(source);
    }

    /**
     * A {@code sync*} generator's iterable. {@code body} runs the generator's body into
     * the list it is given, and it runs when an iteration begins -- once per iteration,
     * as Dart re-runs a generator for each iterator. It used to run once, when the
     * function was called: side effects happened before anyone iterated, and a second
     * iteration replayed the first one's values instead of running the body again.
     *
     * <p>Still a divergence: an iteration runs the WHOLE body before its first element,
     * where Dart suspends at each {@code yield}. An infinite generator therefore does not
     * terminate here. Suspending needs the body rewritten as a state machine, the same
     * continuation-passing lowering the blocking async model defers. Running the body
     * on a helper thread that hands over one element per {@code moveNext} is not a
     * shortcut: a generator body may read or build widgets, which belong to the EDT,
     * and it would cost a thread per live iterator. The limitation is listed in
     * flutter-runtime/READINESS.md and in the conversion skill.</p>
     */
    /// A `sync*` body whose declared element type is {@code elem}: the generated
    /// iterable, and the list each iteration fills, record it.
    public static <E> DartIterable<E> syncStar(final dart.runtime.TypeWitness elem,
            final Funcs.VoidFunc1<DartList<E>> body) {
        DartIterable<E> it = wrap(new Iterable<E>() {
            @Override
            public Iterator<E> iterator() {
                DartList<E> out = new DartList<E>(elem);
                body.call(out);
                return out.iterator();
            }
        });
        it.elem = elem;
        return it;
    }

    /// A `sync*` body lowered to a dart.async.SyncStarFrame: {@code make} builds a fresh
    /// frame for each iteration, which runs the body lazily, one yield per step.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E> DartIterable<E> syncStarFrame(final dart.runtime.TypeWitness elem,
            final Funcs.Func0<? extends Iterator> make) {
        DartIterable<E> it = wrap(new Iterable<E>() {
            @Override
            public Iterator<E> iterator() {
                return (Iterator<E>) make.call();
            }
        });
        it.elem = elem;
        return it;
    }

    /// As {@link #syncStarFrame(dart.runtime.TypeWitness, Funcs.Func0)}, unrecorded.
    @SuppressWarnings("rawtypes")
    public static <E> DartIterable<E> syncStarFrame(final Funcs.Func0<? extends Iterator> make) {
        return syncStarFrame(null, make);
    }

    /// A `sync*` body whose element type is an unwitnessed type variable: unrecorded.
    public static <E> DartIterable<E> syncStar(final Funcs.VoidFunc1<DartList<E>> body) {
        return wrap(new Iterable<E>() {
            @Override
            public Iterator<E> iterator() {
                DartList<E> out = new DartList<E>((dart.runtime.TypeWitness) null);
                body.call(out);
                return out.iterator();
            }
        });
    }

    /**
     * Dart's {@code Iterable.generate(count, [generator])} — a lazy iterable of
     * {@code count} elements produced by {@code generator(index)}. With no
     * generator Dart yields the indices themselves. A negative count is an empty
     * iterable, not an error: measured on the Dart 3.9 VM, {@code Iterable.generate(-1)}
     * neither throws nor yields anything (only {@code List.generate(-1)} throws).
     */
    @SuppressWarnings("unchecked")
    public static <E> DartIterable<E> generate(long count, Funcs.Func1<Long, E> generator) {
        if (generator == null) {
            // Dart's generator is optional, and an explicit null means the same as
            // leaving it out: the indices themselves. It used to throw on first use.
            return generate(count, i -> (E) i);
        }
        return new DartIterable<>(() -> new Iterator<E>() {
            private long i;

            @Override
            public boolean hasNext() {
                return i < count;
            }

            @Override
            public E next() {
                if (i >= count) {
                    throw new NoSuchElementException();
                }
                return generator.call(i++);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <E> DartIterable<E> generate(long count) {
        return generate(count, i -> (E) i);
    }

    @Override
    public Iterator<E> iterator() {
        return source.iterator();
    }

    public <R> DartIterable<R> map(Funcs.Func1<E, R> f) {
        Iterable<E> src = this;
        return withElem(new DartIterable<>(() -> new Iterator<R>() {
            private final Iterator<E> it = src.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public R next() {
                return f.call(it.next());
            }
        }), f instanceof dart.runtime.Signed && !((dart.runtime.Signed) f).$generic() ? ((dart.runtime.Signed) f).$sig()[0] : null);
    }

    public DartIterable<E> where(Funcs.Func1<E, Boolean> test) {
        Iterable<E> src = this;
        return withElem(new DartIterable<>(() -> new Iterator<E>() {
            private final Iterator<E> it = src.iterator();
            private boolean ready;
            private E next;

            private void advance() {
                while (!ready && it.hasNext()) {
                    E candidate = it.next();
                    if (Boolean.TRUE.equals(test.call(candidate))) {
                        next = candidate;
                        ready = true;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                advance();
                return ready;
            }

            @Override
            public E next() {
                advance();
                if (!ready) {
                    throw new NoSuchElementException();
                }
                ready = false;
                E r = next;
                next = null;
                return r;
            }
        }), elementWitness());
    }

    public DartIterable<E> take(long count) {
        // Dart refuses a negative count; it silently meant "nothing" here, and the
        // mirrored skip(-1) returned everything.
        RangeError.checkNotNegative(count, "count");
        Iterable<E> src = this;
        return withElem(new DartIterable<>(() -> new Iterator<E>() {
            private final Iterator<E> it = src.iterator();
            private long remaining = count;

            @Override
            public boolean hasNext() {
                return remaining > 0 && it.hasNext();
            }

            @Override
            public E next() {
                if (remaining <= 0) {
                    throw new NoSuchElementException();
                }
                remaining--;
                return it.next();
            }
        }), elementWitness());
    }

    public DartIterable<E> skip(long count) {
        RangeError.checkNotNegative(count, "count");
        Iterable<E> src = this;
        // Lazy, as Dart's: nothing is skipped (or computed by an upstream map/where)
        // until the iterator is first advanced, not when it is created.
        return withElem(new DartIterable<>(() -> new Iterator<E>() {
            private final Iterator<E> it = src.iterator();
            private boolean skipped;

            private void skipOnce() {
                if (!skipped) {
                    skipped = true;
                    for (long i = 0; i < count && it.hasNext(); i++) {
                        it.next();
                    }
                }
            }

            @Override
            public boolean hasNext() {
                skipOnce();
                return it.hasNext();
            }

            @Override
            public E next() {
                skipOnce();
                return it.next();
            }
        }), elementWitness());
    }

    public long length() {
        long n = 0;
        for (E ignored : this) {
            n++;
        }
        return n;
    }

    public boolean isEmpty() {
        return !iterator().hasNext();
    }

    public boolean isNotEmpty() {
        return iterator().hasNext();
    }

    /** Dart's {@code Iterable.single}. */
    public E single() {
        Iterator<E> it = iterator();
        if (!it.hasNext()) {
            throw new StateError("No element");
        }
        E result = it.next();
        if (it.hasNext()) {
            throw new StateError("Too many elements");
        }
        return result;
    }

    public E first() {
        Iterator<E> it = iterator();
        if (!it.hasNext()) {
            throw new StateError("No element");
        }
        return it.next();
    }

    public E last() {
        Iterator<E> it = iterator();
        if (!it.hasNext()) {
            throw new StateError("No element");
        }
        E e = it.next();
        while (it.hasNext()) {
            e = it.next();
        }
        return e;
    }

    public E firstWhere(Funcs.Func1<E, Boolean> test, Funcs.Func0<E> orElse) {
        for (E e : this) {
            if (Boolean.TRUE.equals(test.call(e))) {
                return e;
            }
        }
        if (orElse != null) {
            return orElse.call();
        }
        throw new StateError("No element");
    }

    /** Dart's Iterable.elementAt(index) — the index-th element (0-based). */
    public E elementAt(long index) {
        if (index < 0) {
            throw new RangeError("index out of range: " + index);
        }
        long i = 0;
        for (E e : this) {
            if (i == index) {
                return e;
            }
            i++;
        }
        throw new RangeError("index out of range: " + index);
    }

    public boolean any(Funcs.Func1<E, Boolean> test) {
        for (E e : this) {
            if (Boolean.TRUE.equals(test.call(e))) {
                return true;
            }
        }
        return false;
    }

    public boolean every(Funcs.Func1<E, Boolean> test) {
        for (E e : this) {
            if (!Boolean.TRUE.equals(test.call(e))) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(Object element) {
        if (membershipTest != null) {
            return Boolean.TRUE.equals(membershipTest.call(element));
        }
        for (E e : this) {
            if (dart.runtime.DartRuntime.eq(e, element)) {
                return true;
            }
        }
        return false;
    }

    public void forEach(Funcs.VoidFunc1<E> action) {
        for (E e : this) {
            action.call(e);
        }
    }

    /**
     * The name the emitter uses for Dart's forEach on every collection receiver --
     * DartList and DartSet need it to stay clear of java.lang.Iterable's own forEach.
     * Without it here, forEach on an Iterable -- the result of map or where --
     * generated a call javac rejected.
     */
    public void forEachDart(Funcs.VoidFunc1<E> action) {
        forEach(action);
    }

    public <R> R fold(R initialValue, Funcs.Func2<R, E, R> combine) {
        R acc = initialValue;
        for (E e : this) {
            acc = combine.call(acc, e);
        }
        return acc;
    }

    /** Dart's {@code Iterable.reduce(combine)} — folds without a seed. */
    public E reduce(Funcs.Func2<E, E, E> combine) {
        Iterator<E> it = iterator();
        if (!it.hasNext()) {
            throw new StateError("No element");
        }
        E acc = it.next();
        while (it.hasNext()) {
            acc = combine.call(acc, it.next());
        }
        return acc;
    }

    /** Dart's {@code Iterable.expand(f)} — flat-maps each element to an iterable. */
    public <R> DartIterable<R> expand(Funcs.Func1<E, ? extends Iterable<R>> f) {
        Iterable<E> src = this;
        return new DartIterable<>(() -> new Iterator<R>() {
            private final Iterator<E> outer = src.iterator();
            private Iterator<R> inner;

            private void advance() {
                while ((inner == null || !inner.hasNext()) && outer.hasNext()) {
                    Iterable<R> next = f.call(outer.next());
                    inner = next == null ? null : next.iterator();
                }
            }

            @Override
            public boolean hasNext() {
                advance();
                return inner != null && inner.hasNext();
            }

            @Override
            public R next() {
                advance();
                if (inner == null || !inner.hasNext()) {
                    throw new NoSuchElementException();
                }
                return inner.next();
            }
        });
    }

    /** Dart's {@code Iterable.followedBy(other)} — lazy concatenation. */
    public DartIterable<E> followedBy(Iterable<E> other) {
        Iterable<E> src = this;
        return withElem(new DartIterable<>(() -> new Iterator<E>() {
            private Iterator<E> it = src.iterator();
            private boolean second;

            @Override
            public boolean hasNext() {
                if (it.hasNext()) {
                    return true;
                }
                if (!second) {
                    second = true;
                    it = other == null ? java.util.Collections.<E>emptyIterator() : other.iterator();
                }
                return it.hasNext();
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return it.next();
            }
        }), elementWitness());
    }

    /**
     * Dart's {@code Iterable.whereType&lt;T&gt;()} for a non-nullable {@code T} -- the
     * transpiler threads the requested type as a trailing {@code Class} witness. Null
     * is not a T, so it is dropped.
     */
    public <T> DartIterable<T> whereType(Class<T> type) {
        return whereType(type, false);
    }

    /**
     * {@code whereType<T?>()}: {@code nullable} keeps null elements, which a nullable
     * type (and {@code dynamic}, the witness {@code null}) admits. {@code Class.isInstance}
     * answers false for null, so the one-argument form silently dropped them.
     */
    @SuppressWarnings("unchecked")
    public <T> DartIterable<T> whereType(Class<T> type, boolean nullable) {
        Iterable<E> src = this;
        return withElem(new DartIterable<>(() -> new Iterator<T>() {
            private final Iterator<E> it = src.iterator();
            private boolean ready;
            private T next;

            private void advance() {
                while (!ready && it.hasNext()) {
                    E c = it.next();
                    if (c == null ? nullable || type == null : type == null || type.isInstance(c)) {
                        next = (T) c;
                        ready = true;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                advance();
                return ready;
            }

            @Override
            public T next() {
                advance();
                if (!ready) {
                    throw new NoSuchElementException();
                }
                ready = false;
                T r = next;
                next = null;
                return r;
            }
        }), dart.runtime.TypeWitness.of(type, nullable));
    }

    /** Dart's {@code Iterable.asMap()} — index-to-element map. */
    public DartMap<Long, E> asMap() {
        DartMap<Long, E> m = new DartMap<>();
        long i = 0;
        for (E e : this) {
            m.put(i++, e);
        }
        return m;
    }

    /** Dart's {@code Iterable.singleWhere(test, {orElse})}. */
    public E singleWhere(Funcs.Func1<E, Boolean> test, Funcs.Func0<E> orElse) {
        E found = null;
        boolean seen = false;
        for (E e : this) {
            if (Boolean.TRUE.equals(test.call(e))) {
                if (seen) {
                    throw new StateError("Too many elements");
                }
                found = e;
                seen = true;
            }
        }
        if (seen) {
            return found;
        }
        if (orElse != null) {
            return orElse.call();
        }
        throw new StateError("No element");
    }

    /** Dart's {@code Iterable.lastWhere(test, {orElse})}. */
    public E lastWhere(Funcs.Func1<E, Boolean> test, Funcs.Func0<E> orElse) {
        E found = null;
        boolean seen = false;
        for (E e : this) {
            if (Boolean.TRUE.equals(test.call(e))) {
                found = e;
                seen = true;
            }
        }
        if (seen) {
            return found;
        }
        if (orElse != null) {
            return orElse.call();
        }
        throw new StateError("No element");
    }

    public DartIterable<E> takeWhile(Funcs.Func1<E, Boolean> test) {
        Iterable<E> src = this;
        return withElem(new DartIterable<>(() -> new Iterator<E>() {
            private final Iterator<E> it = src.iterator();
            private boolean done;
            private boolean ready;
            private E next;

            private void advance() {
                if (!ready && !done && it.hasNext()) {
                    E c = it.next();
                    if (Boolean.TRUE.equals(test.call(c))) {
                        next = c;
                        ready = true;
                    } else {
                        done = true;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                advance();
                return ready;
            }

            @Override
            public E next() {
                advance();
                if (!ready) {
                    throw new NoSuchElementException();
                }
                ready = false;
                return next;
            }
        }), elementWitness());
    }

    public DartIterable<E> skipWhile(Funcs.Func1<E, Boolean> test) {
        Iterable<E> src = this;
        // Lazy, as Dart's: the predicate first runs when the iterator is advanced,
        // not when it is created, so an iterator obtained and never used runs nothing.
        return withElem(new DartIterable<>(() -> new Iterator<E>() {
            private final Iterator<E> it = src.iterator();
            private boolean skipping = true;
            private boolean hasHead;
            private E head;

            private void skipOnce() {
                if (!skipping) {
                    return;
                }
                skipping = false;
                while (it.hasNext()) {
                    E c = it.next();
                    if (!Boolean.TRUE.equals(test.call(c))) {
                        head = c;
                        hasHead = true;
                        return;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                skipOnce();
                return hasHead || it.hasNext();
            }

            @Override
            public E next() {
                skipOnce();
                if (hasHead) {
                    hasHead = false;
                    E h = head;
                    head = null;
                    return h;
                }
                return it.next();
            }
        }), elementWitness());
    }

    public String join(String separator) {
        StringBuilder sb = new StringBuilder();
        boolean firstItem = true;
        for (E e : this) {
            if (!firstItem) {
                sb.append(separator);
            }
            sb.append(dart.runtime.DartRuntime.str(e));
            firstItem = false;
        }
        return sb.toString();
    }

    public String join() {
        return join("");
    }

    /** Dart's {@code toList(growable: ...)}: a fixed-length copy when growable is false. */
    public DartList<E> toList(boolean growable) {
        return growable ? toList() : DartList.typed(DartList.from(this, false), elementWitness());
    }

    public DartList<E> toList() {
        DartList<E> l = new DartList<E>(elementWitness());
        for (E e : this) {
            l.add(e);
        }
        return l;
    }

    public DartSet<E> toSet() {
        DartSet<E> s = DartSet.typed(new DartSet<E>(), elementWitness());
        for (E e : this) {
            s.add(e);
        }
        return s;
    }

    @Override
    public String toString() {
        if (source instanceof DartList || source instanceof DartSet) {
            // A List or Set held as an Iterable (`Future<Iterable<int>> f() async => [3, 4]`)
            // is still that collection: Dart prints it as one, `[3, 4]`, not `(3, 4)`.
            return source.toString();
        }
        if (!DartRuntime.beginFormat(this)) {
            return "(...)";
        }
        try {
            return "(" + join(", ") + ")";
        } finally {
            DartRuntime.endFormat(this);
        }
    }
}
