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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** Lazy fallback for pipelines whose source or callbacks cannot be specialized. */
final class StreamImpl<T> implements Stream<T> {
    private static final int SOURCE = 0, FILTER = 1, MAP = 2, SORTED = 3,
            DISTINCT = 4, LIMIT = 5, SKIP = 6;

    private static final class Source {
        final List<?> values;
        boolean closed;
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
                Collections.sort((List) values);
                input = values.iterator();
                sorted = true;
            }
            if (operation == SKIP) {
                while (remaining > 0 && input.hasNext()) {
                    input.next();
                    remaining--;
                }
            }
            while (input.hasNext()) {
                Object value = input.next();
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
        while (values.hasNext()) if (predicate.test(values.next())) return true;
        return false;
    }

    public boolean allMatch(Predicate<? super T> predicate) {
        require(predicate);
        Iterator<T> values = iterator();
        while (values.hasNext()) if (!predicate.test(values.next())) return false;
        return true;
    }

    public boolean noneMatch(Predicate<? super T> predicate) { return !anyMatch(predicate); }
    public Stream<T> sequential() { return this; }
    public Stream<T> parallel() { return this; }
    public void close() { source.closed = true; linkedOrConsumed = true; }
}
