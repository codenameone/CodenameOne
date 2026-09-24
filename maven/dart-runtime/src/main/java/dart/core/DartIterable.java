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

    protected DartIterable(Iterable<E> source) {
        this.source = source;
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
     * continuation-passing lowering the blocking async model defers.</p>
     */
    public static <E> DartIterable<E> syncStar(final Funcs.VoidFunc1<DartList<E>> body) {
        return wrap(new Iterable<E>() {
            @Override
            public Iterator<E> iterator() {
                DartList<E> out = new DartList<E>();
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
    public static <E> DartIterable<E> generate(long count, Funcs.Func1<Long, E> generator) {
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
        return new DartIterable<>(() -> new Iterator<R>() {
            private final Iterator<E> it = src.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public R next() {
                return f.call(it.next());
            }
        });
    }

    public DartIterable<E> where(Funcs.Func1<E, Boolean> test) {
        Iterable<E> src = this;
        return new DartIterable<>(() -> new Iterator<E>() {
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
        });
    }

    public DartIterable<E> take(long count) {
        // Dart refuses a negative count; it silently meant "nothing" here, and the
        // mirrored skip(-1) returned everything.
        RangeError.checkNotNegative(count, "count");
        Iterable<E> src = this;
        return new DartIterable<>(() -> new Iterator<E>() {
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
        });
    }

    public DartIterable<E> skip(long count) {
        RangeError.checkNotNegative(count, "count");
        Iterable<E> src = this;
        return new DartIterable<>(() -> {
            Iterator<E> it = src.iterator();
            for (long i = 0; i < count && it.hasNext(); i++) {
                it.next();
            }
            return it;
        });
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
    public <R> DartIterable<R> expand(Funcs.Func1<E, Iterable<R>> f) {
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
        return new DartIterable<>(() -> new Iterator<E>() {
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
        });
    }

    /**
     * Dart's {@code Iterable.whereType&lt;T&gt;()} — the transpiler threads the
     * requested type as a trailing {@code Class} witness.
     */
    @SuppressWarnings("unchecked")
    public <T> DartIterable<T> whereType(Class<T> type) {
        Iterable<E> src = this;
        return new DartIterable<>(() -> new Iterator<T>() {
            private final Iterator<E> it = src.iterator();
            private boolean ready;
            private T next;

            private void advance() {
                while (!ready && it.hasNext()) {
                    E c = it.next();
                    if (type == null ? c != null : type.isInstance(c)) {
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
        });
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
        return new DartIterable<>(() -> new Iterator<E>() {
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
        });
    }

    public DartIterable<E> skipWhile(Funcs.Func1<E, Boolean> test) {
        Iterable<E> src = this;
        return new DartIterable<>(() -> {
            Iterator<E> it = src.iterator();
            java.util.ArrayList<E> buffered = new java.util.ArrayList<>();
            while (it.hasNext()) {
                E c = it.next();
                if (!Boolean.TRUE.equals(test.call(c))) {
                    buffered.add(c);
                    break;
                }
            }
            Iterator<E> tail = it;
            Iterator<E> head = buffered.iterator();
            return new Iterator<E>() {
                @Override
                public boolean hasNext() {
                    return head.hasNext() || tail.hasNext();
                }

                @Override
                public E next() {
                    return head.hasNext() ? head.next() : tail.next();
                }
            };
        });
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
        return growable ? toList() : DartList.from(this, false);
    }

    public DartList<E> toList() {
        DartList<E> l = new DartList<>();
        for (E e : this) {
            l.add(e);
        }
        return l;
    }

    public DartSet<E> toSet() {
        DartSet<E> s = new DartSet<>();
        for (E e : this) {
            s.add(e);
        }
        return s;
    }

    @Override
    public String toString() {
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
