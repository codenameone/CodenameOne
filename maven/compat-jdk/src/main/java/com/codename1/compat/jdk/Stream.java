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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/// `java.util.stream.Stream` for the Codename One runtime, whose own class
/// of that name has a handful of methods and factories that answer null.
///
/// A stream here is sequential and lazy: each stage pulls one element at a
/// time from the one before it, and only when a terminal operation asks. So
/// an infinite `iterate` or `generate` behind a `limit` ends, `findFirst`
/// and the `anyMatch` family stop at the element that decides them, and the
/// side effects of `peek` happen element by element, in encounter order.
///
/// What is not here: spliterators, and with them parallel evaluation.
/// `parallel()` answers the stream it was called on.
public interface Stream<T> extends BaseStream<T, Stream<T>> {

    Stream<T> filter(Predicate<? super T> predicate);

    <R> Stream<R> map(Function<? super T, ? extends R> mapper);

    IntStream mapToInt(ToIntFunction<? super T> mapper);

    LongStream mapToLong(ToLongFunction<? super T> mapper);

    DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper);

    <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

    IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper);

    LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper);

    DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper);

    Stream<T> distinct();

    Stream<T> sorted();

    Stream<T> sorted(Comparator<? super T> comparator);

    Stream<T> peek(Consumer<? super T> action);

    Stream<T> limit(long maxSize);

    Stream<T> skip(long n);

    Stream<T> takeWhile(Predicate<? super T> predicate);

    Stream<T> dropWhile(Predicate<? super T> predicate);

    void forEach(Consumer<? super T> action);

    void forEachOrdered(Consumer<? super T> action);

    Object[] toArray();

    <A> A[] toArray(IntFunction<A[]> generator);

    T reduce(T identity, BinaryOperator<T> accumulator);

    Optional<T> reduce(BinaryOperator<T> accumulator);

    <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner);

    <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner);

    <R, A> R collect(Collector<? super T, A, R> collector);

    /// An unmodifiable list of the elements, which may be null.
    @SuppressWarnings("unchecked")
    default List<T> toList() {
        return (List<T>) Collections.unmodifiableList(new ArrayList<Object>(Arrays.asList(toArray())));
    }

    Optional<T> min(Comparator<? super T> comparator);

    Optional<T> max(Comparator<? super T> comparator);

    long count();

    boolean anyMatch(Predicate<? super T> predicate);

    boolean allMatch(Predicate<? super T> predicate);

    boolean noneMatch(Predicate<? super T> predicate);

    Optional<T> findFirst();

    Optional<T> findAny();

    static <T> Builder<T> builder() {
        return new ObjPipeline.ListBuilder<T>();
    }

    static <T> Stream<T> empty() {
        return ObjPipeline.over(Collections.<T>emptyList());
    }

    static <T> Stream<T> of(T t) {
        return ObjPipeline.over(Collections.singletonList(t));
    }

    static <T> Stream<T> ofNullable(T t) {
        return t == null ? Stream.<T>empty() : Stream.of(t);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    static <T> Stream<T> of(T... values) {
        return ObjPipeline.ofArray(values, 0, values.length);
    }

    static <T> Stream<T> iterate(T seed, UnaryOperator<T> f) {
        if (f == null) {
            throw new NullPointerException();
        }
        return ObjPipeline.over(new PullIterator<T>() {
            private boolean started;
            private T current;

            @Override
            boolean advance() {
                current = started ? f.apply(current) : seed;
                started = true;
                emit(current);
                return true;
            }
        });
    }

    static <T> Stream<T> iterate(T seed, Predicate<? super T> hasNext, UnaryOperator<T> next) {
        if (hasNext == null || next == null) {
            throw new NullPointerException();
        }
        return ObjPipeline.over(new PullIterator<T>() {
            private boolean started;
            private T current;

            @Override
            boolean advance() {
                current = started ? next.apply(current) : seed;
                started = true;
                if (!hasNext.test(current)) {
                    return false;
                }
                emit(current);
                return true;
            }
        });
    }

    static <T> Stream<T> generate(Supplier<? extends T> s) {
        if (s == null) {
            throw new NullPointerException();
        }
        return ObjPipeline.over(new PullIterator<T>() {
            @Override
            boolean advance() {
                emit(s.get());
                return true;
            }
        });
    }

    static <T> Stream<T> concat(Stream<? extends T> a, Stream<? extends T> b) {
        if (a == null || b == null) {
            throw new NullPointerException();
        }
        return ObjPipeline.concat(a, b);
    }

    /// `java.util.stream.Stream.Builder`: collects elements, then answers
    /// them as a stream, once.
    interface Builder<T> extends Consumer<T> {

        @Override
        void accept(T t);

        default Builder<T> add(T t) {
            accept(t);
            return this;
        }

        Stream<T> build();
    }
}
