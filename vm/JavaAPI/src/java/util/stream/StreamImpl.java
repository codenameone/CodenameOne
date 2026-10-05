/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package java.util.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Lazy fallback for pipelines whose source or callbacks cannot be specialized. */
final class StreamImpl<T> implements Stream<T> {
    private static final int SOURCE = 0, FILTER = 1, MAP = 2, SORTED = 3,
            DISTINCT = 4, LIMIT = 5, SKIP = 6, FLATMAP = 7, PEEK = 8;

    private static final class Source {
        final List<?> values;
        boolean closed;
        /** onClose handlers, shared by every stage of the pipeline, run once by the first close(). */
        List<Runnable> closeHandlers;
        Source(List<?> values) { this.values = values; }
    }

    private final Source source;
    private final StreamImpl<?> upstream;
    private final int operation;
    private final Object callback;
    private final long amount;
    private boolean linkedOrConsumed;

    StreamImpl(List<T> values) {
        source = new Source(values);
        upstream = null;
        operation = SOURCE;
        callback = null;
        amount = 0;
    }

    private StreamImpl(StreamImpl<?> upstream, int operation, Object callback, long amount) {
        upstream.claim();
        this.source = upstream.source;
        this.upstream = upstream;
        this.operation = operation;
        this.callback = callback;
        this.amount = amount;
    }

    private void claim() {
        if (linkedOrConsumed || source.closed) throw new IllegalStateException();
        linkedOrConsumed = true;
    }

    private static void require(Object value) {
        if (value == null) throw new NullPointerException();
    }

    public Stream<T> filter(Predicate<? super T> predicate) {
        require(predicate);
        return new StreamImpl<T>(this, FILTER, predicate, 0);
    }

    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        require(mapper);
        return new StreamImpl<R>(this, MAP, mapper, 0);
    }

    public Stream<T> sorted() { return new StreamImpl<T>(this, SORTED, null, 0); }

    public Stream<T> sorted(Comparator<? super T> comparator) {
        // A null comparator is an error, not natural order: SORTED with a null callback means sorted().
        require(comparator);
        return new StreamImpl<T>(this, SORTED, comparator, 0);
    }

    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        require(mapper);
        return new StreamImpl<R>(this, FLATMAP, mapper, 0);
    }

    public Stream<T> peek(Consumer<? super T> action) {
        require(action);
        return new StreamImpl<T>(this, PEEK, action, 0);
    }
    public Stream<T> distinct() { return new StreamImpl<T>(this, DISTINCT, null, 0); }

    public Stream<T> limit(long maxSize) {
        if (maxSize < 0) throw new IllegalArgumentException();
        return new StreamImpl<T>(this, LIMIT, null, maxSize);
    }

    public Stream<T> skip(long n) {
        if (n < 0) throw new IllegalArgumentException();
        return new StreamImpl<T>(this, SKIP, null, n);
    }

    /** One buffered element, never an eagerly materialized intermediate collection. */
    private static final class Cursor implements Iterator<Object> {
        private Iterator<?> input;
        private final int operation;
        private final Object callback;
        private long remaining;
        private Set<Object> seen;
        private boolean sorted;
        private boolean ready;
        private boolean exhausted;
        private Object next;
        private Iterator<?> inner;
        /** The stream flatMap's mapper returned, closed once its elements are used (as the JDK does). */
        private Stream<?> innerStream;

        Cursor(Iterator<?> input, int operation, Object callback, long amount) {
            this.input = input;
            this.operation = operation;
            this.callback = callback;
            remaining = amount;
        }

        @SuppressWarnings("unchecked")
        public boolean hasNext() {
            if (ready) return true;
            if (exhausted) return false;
            if (operation == LIMIT && remaining == 0) {
                exhausted = true;
                return false;
            }
            if (operation == SORTED && !sorted) {
                List<Object> values = new ArrayList<Object>();
                while (input.hasNext()) values.add(input.next());
                if (callback == null) {
                    Collections.sort((List) values);
                } else {
                    Collections.sort(values, (Comparator<Object>) callback);
                }
                input = values.iterator();
                sorted = true;
            }
            if (operation == SKIP) {
                while (remaining > 0 && input.hasNext()) {
                    input.next();
                    remaining--;
                }
            }
            if (operation == FLATMAP) {
                while (true) {
                    if (inner != null && inner.hasNext()) {
                        next = inner.next();
                        ready = true;
                        return true;
                    }
                    closeInner();
                    if (!input.hasNext()) {
                        exhausted = true;
                        return false;
                    }
                    Stream<?> s = (Stream<?>) ((Function<Object, Object>) callback).apply(input.next());
                    innerStream = s;
                    inner = s == null ? null : s.iterator();
                }
            }
            while (input.hasNext()) {
                Object value = input.next();
                if (operation == PEEK) ((Consumer<Object>) callback).accept(value);
                if (operation == FILTER && !((Predicate<Object>) callback).test(value)) continue;
                if (operation == DISTINCT) {
                    if (seen == null) seen = new HashSet<Object>();
                    if (!seen.add(value)) continue;
                }
                if (operation == MAP) value = ((Function<Object, Object>) callback).apply(value);
                if (operation == LIMIT) remaining--;
                next = value;
                ready = true;
                return true;
            }
            exhausted = true;
            return false;
        }

        public Object next() {
            if (!hasNext()) throw new NoSuchElementException();
            Object result = next;
            next = null;
            ready = false;
            return result;
        }

        public void remove() { throw new UnsupportedOperationException(); }

        private void closeInner() {
            if (innerStream != null) {
                Stream<?> s = innerStream;
                innerStream = null;
                inner = null;
                s.close();
            }
        }

        /** A terminal operation stopped early: close the mapped streams still open upstream. */
        void release() {
            closeInner();
            if (input instanceof Cursor) {
                ((Cursor) input).release();
            }
        }
    }

    private static void release(Iterator<?> values) {
        if (values instanceof Cursor) {
            ((Cursor) values).release();
        }
    }

    private Iterator<?> open() {
        if (operation == SOURCE) return new Cursor(source.values.iterator(), SOURCE, null, 0);
        return new Cursor(upstream.open(), operation, callback, amount);
    }

    @SuppressWarnings("unchecked")
    public Iterator<T> iterator() {
        claim();
        return (Iterator<T>) open();
    }

    public void forEach(Consumer<? super T> action) {
        require(action);
        Iterator<T> values = iterator();
        while (values.hasNext()) action.accept(values.next());
    }

    public Object[] toArray() {
        List<T> result = new ArrayList<T>();
        Iterator<T> values = iterator();
        while (values.hasNext()) result.add(values.next());
        return result.toArray();
    }

    public T reduce(T identity, BinaryOperator<T> accumulator) {
        require(accumulator);
        Iterator<T> values = iterator();
        T result = identity;
        while (values.hasNext()) result = accumulator.apply(result, values.next());
        return result;
    }

    public <A, R> R collect(Collector<? super T, A, R> collector) {
        require(collector);
        Iterator<T> values = iterator();
        A container = collector.supplier().get();
        BiConsumer<A, ? super T> accumulator = collector.accumulator();
        while (values.hasNext()) accumulator.accept(container, values.next());
        return collector.finisher().apply(container);
    }

    public long count() {
        Iterator<T> values = iterator();
        long count = 0;
        while (values.hasNext()) { values.next(); count++; }
        return count;
    }

    public boolean anyMatch(Predicate<? super T> predicate) {
        require(predicate);
        Iterator<T> values = iterator();
        boolean found = false;
        while (!found && values.hasNext()) found = predicate.test(values.next());
        release(values);
        return found;
    }

    public boolean allMatch(Predicate<? super T> predicate) {
        require(predicate);
        Iterator<T> values = iterator();
        boolean all = true;
        while (all && values.hasNext()) all = predicate.test(values.next());
        release(values);
        return all;
    }

    public boolean noneMatch(Predicate<? super T> predicate) { return !anyMatch(predicate); }

    public Optional<T> findFirst() {
        Iterator<T> values = iterator();
        Optional<T> first = values.hasNext() ? Optional.of(values.next()) : Optional.<T>empty();
        release(values);
        return first;
    }

    public Optional<T> findAny() { return findFirst(); }

    public Optional<T> min(Comparator<? super T> comparator) {
        require(comparator);
        Iterator<T> values = iterator();
        if (!values.hasNext()) return Optional.<T>empty();
        T best = values.next();
        while (values.hasNext()) {
            T v = values.next();
            if (comparator.compare(v, best) < 0) best = v;
        }
        return Optional.of(best);
    }

    public Optional<T> max(Comparator<? super T> comparator) {
        require(comparator);
        Iterator<T> values = iterator();
        if (!values.hasNext()) return Optional.<T>empty();
        T best = values.next();
        while (values.hasNext()) {
            T v = values.next();
            if (comparator.compare(v, best) > 0) best = v;
        }
        return Optional.of(best);
    }

    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        require(accumulator);
        Iterator<T> values = iterator();
        if (!values.hasNext()) return Optional.<T>empty();
        T result = values.next();
        while (values.hasNext()) result = accumulator.apply(result, values.next());
        return Optional.of(result);
    }

    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        require(supplier);
        require(accumulator);
        // Never called on a sequential stream, but required all the same, as on the JDK.
        require(combiner);
        R container = supplier.get();
        Iterator<T> values = iterator();
        while (values.hasNext()) accumulator.accept(container, values.next());
        return container;
    }

    public List<T> toList() {
        List<T> result = new ArrayList<T>();
        Iterator<T> values = iterator();
        while (values.hasNext()) result.add(values.next());
        return Collections.unmodifiableList(result);
    }
    public Stream<T> sequential() { return this; }
    public Stream<T> parallel() { return this; }
    public Stream<T> onClose(Runnable closeHandler) {
        require(closeHandler);
        if (linkedOrConsumed || source.closed) throw new IllegalStateException();
        if (source.closeHandlers == null) source.closeHandlers = new ArrayList<Runnable>();
        source.closeHandlers.add(closeHandler);
        return this;
    }

    /** Runs every handler even when one throws; the first failure is rethrown with the rest suppressed. */
    public void close() {
        linkedOrConsumed = true;
        if (source.closed) return;
        source.closed = true;
        List<Runnable> handlers = source.closeHandlers;
        source.closeHandlers = null;
        if (handlers == null) return;
        Throwable failure = null;
        for (int i = 0; i < handlers.size(); i++) {
            // Fetched outside the try: the generic get() is a checkcast, which ParparVM does not
            // check, so it must never sit under a handler that would catch its failure.
            Runnable handler = handlers.get(i);
            try {
                handler.run();
            } catch (Throwable t) {
                if (failure == null) {
                    failure = t;
                } else if (failure != t) {
                    failure.addSuppressed(t);
                }
            }
        }
        if (failure != null) {
            // A handler can throw a checked exception (a generic "sneaky throw"); it is rethrown
            // as it is, never dropped, as the JDK's close() does.
            StreamImpl.<RuntimeException>rethrow(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void rethrow(Throwable t) throws E {
        throw (E) t;
    }
}
