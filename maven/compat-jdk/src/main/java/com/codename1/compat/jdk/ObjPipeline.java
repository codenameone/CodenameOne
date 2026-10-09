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
package com.codename1.compat.jdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/// The one implementation of [Stream]: a stage holding the iterator that
/// produces its elements. An intermediate operation wraps that iterator in
/// another and answers a new stage; a terminal one drains it.
///
/// Nothing runs until a terminal operation pulls, and then one element at a
/// time travels the whole chain before the next is asked for.
final class ObjPipeline<T> implements Stream<T> {

    private static final String USED = "stream has already been operated upon or closed";

    private final Iterator<T> source;
    private final StreamState state;
    /// A stage takes one operation, like the JDK's.
    private boolean used;

    private ObjPipeline(Iterator<T> source, StreamState state) {
        this.source = source;
        this.state = state;
    }

    static <T> ObjPipeline<T> over(Iterator<T> source) {
        return new ObjPipeline<T>(source, new StreamState());
    }

    /// A stream of what `source` holds when the terminal operation starts,
    /// not when the stream was made: elements added in between are seen.
    static <T> ObjPipeline<T> over(Iterable<? extends T> source) {
        return over(new PullIterator<T>() {
            private Iterator<? extends T> it;

            @Override
            boolean advance() {
                if (it == null) {
                    it = source.iterator();
                }
                if (!it.hasNext()) {
                    return false;
                }
                emit(it.next());
                return true;
            }
        });
    }

    /// The elements `from` (inclusive) to `to` (exclusive) of `values`.
    static <T> ObjPipeline<T> ofArray(T[] values, int from, int to) {
        if (from < 0 || to > values.length || from > to) {
            throw new ArrayIndexOutOfBoundsException("origin(" + from + ") > fence(" + to + ")");
        }
        return over(new PullIterator<T>() {
            private int at = from;

            @Override
            boolean advance() {
                if (at >= to) {
                    return false;
                }
                emit(values[at++]);
                return true;
            }
        });
    }

    static <T> ObjPipeline<T> concat(Stream<? extends T> a, Stream<? extends T> b) {
        Iterator<? extends T> first = a.iterator();
        Iterator<? extends T> second = b.iterator();
        ObjPipeline<T> out = over(new PullIterator<T>() {
            @Override
            boolean advance() {
                if (first.hasNext()) {
                    emit(first.next());
                    return true;
                }
                if (second.hasNext()) {
                    emit(second.next());
                    return true;
                }
                return false;
            }
        });
        // Closing the concatenation closes both inputs, the second even
        // when the first one's handler throws.
        out.state.onClose(new Runnable() {
            @Override
            public void run() {
                a.close();
            }
        });
        out.state.onClose(new Runnable() {
            @Override
            public void run() {
                b.close();
            }
        });
        return out;
    }

    /// The iterator of this stage, which the caller now owns.
    private Iterator<T> take() {
        if (used) {
            throw new IllegalStateException(USED);
        }
        used = true;
        return source;
    }

    private <R> ObjPipeline<R> stage(Iterator<R> next) {
        return new ObjPipeline<R>(next, state);
    }

    @Override
    public Stream<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        Iterator<T> up = take();
        return stage(new PullIterator<T>() {
            @Override
            boolean advance() {
                while (up.hasNext()) {
                    T value = up.next();
                    if (predicate.test(value)) {
                        emit(value);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        Iterator<T> up = take();
        return stage(new PullIterator<R>() {
            @Override
            boolean advance() {
                if (!up.hasNext()) {
                    return false;
                }
                emit(mapper.apply(up.next()));
                return true;
            }
        });
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline(this.<Integer>map(v -> Integer.valueOf(mapper.applyAsInt(v))));
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline(this.<Long>map(v -> Long.valueOf(mapper.applyAsLong(v))));
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return new DoublePipeline(this.<Double>map(v -> Double.valueOf(mapper.applyAsDouble(v))));
    }

    @Override
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        Objects.requireNonNull(mapper);
        Iterator<T> up = take();
        return stage(new PullIterator<R>() {
            private Stream<? extends R> inner;
            private Iterator<? extends R> it;

            @Override
            boolean advance() {
                while (true) {
                    if (it != null) {
                        if (it.hasNext()) {
                            emit(it.next());
                            return true;
                        }
                        // A mapped stream is closed once its elements have
                        // been taken.
                        Stream<? extends R> finished = inner;
                        inner = null;
                        it = null;
                        finished.close();
                    }
                    if (!up.hasNext()) {
                        return false;
                    }
                    Stream<? extends R> mapped = mapper.apply(up.next());
                    if (mapped != null) {
                        inner = mapped;
                        it = mapped.iterator();
                    }
                }
            }
        });
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline(this.<Integer>flatMap(v -> {
            IntStream mapped = mapper.apply(v);
            return mapped == null ? null : mapped.boxed();
        }));
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline(this.<Long>flatMap(v -> {
            LongStream mapped = mapper.apply(v);
            return mapped == null ? null : mapped.boxed();
        }));
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        Objects.requireNonNull(mapper);
        return new DoublePipeline(this.<Double>flatMap(v -> {
            DoubleStream mapped = mapper.apply(v);
            return mapped == null ? null : mapped.boxed();
        }));
    }

    @Override
    public Stream<T> distinct() {
        Iterator<T> up = take();
        return stage(new PullIterator<T>() {
            private final Set<T> seen = new HashSet<T>();

            @Override
            boolean advance() {
                while (up.hasNext()) {
                    T value = up.next();
                    if (seen.add(value)) {
                        emit(value);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public Stream<T> sorted() {
        return sorted(JdkFunctions.<T>natural());
    }

    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
        Comparator<? super T> order = comparator == null ? JdkFunctions.<T>natural() : comparator;
        Iterator<T> up = take();
        return stage(new PullIterator<T>() {
            private Iterator<T> it;

            @Override
            boolean advance() {
                if (it == null) {
                    // Sorting needs every element, so the first pull takes
                    // the whole of what is upstream.
                    List<T> all = new ArrayList<T>();
                    while (up.hasNext()) {
                        all.add(up.next());
                    }
                    Collections.sort(all, order);
                    it = all.iterator();
                }
                if (!it.hasNext()) {
                    return false;
                }
                emit(it.next());
                return true;
            }
        });
    }

    @Override
    public Stream<T> peek(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        Iterator<T> up = take();
        return stage(new PullIterator<T>() {
            @Override
            boolean advance() {
                if (!up.hasNext()) {
                    return false;
                }
                T value = up.next();
                action.accept(value);
                emit(value);
                return true;
            }
        });
    }

    @Override
    public Stream<T> limit(long maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException(Long.toString(maxSize));
        }
        Iterator<T> up = take();
        return stage(new PullIterator<T>() {
            private long left = maxSize;

            @Override
            boolean advance() {
                // Checked before asking upstream: the element after the
                // last one wanted is never produced.
                if (left <= 0 || !up.hasNext()) {
                    return false;
                }
                left--;
                emit(up.next());
                return true;
            }
        });
    }

    @Override
    public Stream<T> skip(long n) {
        if (n < 0) {
            throw new IllegalArgumentException(Long.toString(n));
        }
        Iterator<T> up = take();
        return stage(new PullIterator<T>() {
            private long left = n;

            @Override
            boolean advance() {
                while (left > 0) {
                    if (!up.hasNext()) {
                        return false;
                    }
                    up.next();
                    left--;
                }
                if (!up.hasNext()) {
                    return false;
                }
                emit(up.next());
                return true;
            }
        });
    }

    @Override
    public Stream<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        Iterator<T> up = take();
        return stage(new PullIterator<T>() {
            @Override
            boolean advance() {
                if (!up.hasNext()) {
                    return false;
                }
                T value = up.next();
                if (!predicate.test(value)) {
                    return false;
                }
                emit(value);
                return true;
            }
        });
    }

    @Override
    public Stream<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        Iterator<T> up = take();
        return stage(new PullIterator<T>() {
            private boolean dropping = true;

            @Override
            boolean advance() {
                while (up.hasNext()) {
                    T value = up.next();
                    if (dropping && predicate.test(value)) {
                        continue;
                    }
                    dropping = false;
                    emit(value);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        Iterator<T> it = take();
        while (it.hasNext()) {
            action.accept(it.next());
        }
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        forEach(action);
    }

    private List<T> drain() {
        List<T> all = new ArrayList<T>();
        Iterator<T> it = take();
        while (it.hasNext()) {
            all.add(it.next());
        }
        return all;
    }

    @Override
    public Object[] toArray() {
        return drain().toArray();
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        Objects.requireNonNull(generator);
        List<T> all = drain();
        A[] out = generator.apply(all.size());
        if (out.length != all.size()) {
            throw new IllegalStateException("The generator answered an array of " + out.length + " for "
                    + all.size() + " elements");
        }
        // Copied through an Object[] view: storing an element of the wrong
        // type fails as an ArrayStoreException, like the JDK's.
        Object[] target = out;
        for (int i = 0; i < target.length; i++) {
            target[i] = all.get(i);
        }
        return out;
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        Objects.requireNonNull(accumulator);
        T result = identity;
        Iterator<T> it = take();
        while (it.hasNext()) {
            result = accumulator.apply(result, it.next());
        }
        return result;
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        Objects.requireNonNull(accumulator);
        Iterator<T> it = take();
        if (!it.hasNext()) {
            return Optional.empty();
        }
        T result = it.next();
        while (it.hasNext()) {
            result = accumulator.apply(result, it.next());
        }
        return Optional.of(result);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        U result = identity;
        Iterator<T> it = take();
        while (it.hasNext()) {
            result = accumulator.apply(result, it.next());
        }
        return result;
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        R result = supplier.get();
        Iterator<T> it = take();
        while (it.hasNext()) {
            accumulator.accept(result, it.next());
        }
        return result;
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        Objects.requireNonNull(collector);
        A container = collector.supplier().get();
        BiConsumer<A, ? super T> accumulator = collector.accumulator();
        Iterator<T> it = take();
        while (it.hasNext()) {
            accumulator.accept(container, it.next());
        }
        return collector.finisher().apply(container);
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return reduce((a, b) -> comparator.compare(a, b) <= 0 ? a : b);
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return reduce((a, b) -> comparator.compare(a, b) >= 0 ? a : b);
    }

    @Override
    public long count() {
        long n = 0;
        Iterator<T> it = take();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        Iterator<T> it = take();
        while (it.hasNext()) {
            if (predicate.test(it.next())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        Iterator<T> it = take();
        while (it.hasNext()) {
            if (!predicate.test(it.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        Iterator<T> it = take();
        while (it.hasNext()) {
            if (predicate.test(it.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Optional<T> findFirst() {
        Iterator<T> it = take();
        return it.hasNext() ? Optional.of(it.next()) : Optional.<T>empty();
    }

    @Override
    public Optional<T> findAny() {
        return findFirst();
    }

    @Override
    public Iterator<T> iterator() {
        return take();
    }

    @Override
    public boolean isParallel() {
        return false;
    }

    @Override
    public Stream<T> sequential() {
        return this;
    }

    @Override
    public Stream<T> parallel() {
        return this;
    }

    @Override
    public Stream<T> unordered() {
        return this;
    }

    @Override
    public Stream<T> onClose(Runnable closeHandler) {
        if (used) {
            throw new IllegalStateException(USED);
        }
        state.onClose(closeHandler);
        return this;
    }

    @Override
    public void close() {
        used = true;
        state.close();
    }

    /// What [Stream#builder] answers.
    static final class ListBuilder<T> implements Stream.Builder<T> {

        private List<T> elements = new ArrayList<T>();

        @Override
        public void accept(T t) {
            if (elements == null) {
                throw new IllegalStateException();
            }
            elements.add(t);
        }

        @Override
        public Stream<T> build() {
            if (elements == null) {
                throw new IllegalStateException();
            }
            List<T> built = elements;
            elements = null;
            return over(built);
        }
    }
}
