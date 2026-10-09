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

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/// `java.util.stream.IntStream` for the Codename One runtime: a sequential
/// stream of `int` values, evaluated lazily, one element at a time, when a
/// terminal operation asks for them.
///
/// There is no parallel evaluation: `parallel()` answers the same stream.
public interface IntStream extends BaseStream<Integer, IntStream> {

    IntStream filter(IntPredicate predicate);

    IntStream map(IntUnaryOperator mapper);

    <U> Stream<U> mapToObj(IntFunction<? extends U> mapper);

    LongStream mapToLong(IntToLongFunction mapper);

    DoubleStream mapToDouble(IntToDoubleFunction mapper);

    IntStream flatMap(IntFunction<? extends IntStream> mapper);

    IntStream distinct();

    IntStream sorted();

    IntStream peek(IntConsumer action);

    IntStream limit(long maxSize);

    IntStream skip(long n);

    IntStream takeWhile(IntPredicate predicate);

    IntStream dropWhile(IntPredicate predicate);

    void forEach(IntConsumer action);

    void forEachOrdered(IntConsumer action);

    int[] toArray();

    int reduce(int identity, IntBinaryOperator op);

    OptionalInt reduce(IntBinaryOperator op);

    <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner);

    int sum();

    OptionalInt min();

    OptionalInt max();

    long count();

    OptionalDouble average();

    IntSummaryStatistics summaryStatistics();

    boolean anyMatch(IntPredicate predicate);

    boolean allMatch(IntPredicate predicate);

    boolean noneMatch(IntPredicate predicate);

    OptionalInt findFirst();

    OptionalInt findAny();

    LongStream asLongStream();

    DoubleStream asDoubleStream();

    Stream<Integer> boxed();

    @Override
    PrimitiveIterator.OfInt iterator();

    static IntStream empty() {
        return new IntPipeline(Stream.<Integer>empty());
    }

    static IntStream of(int value) {
        return new IntPipeline(Stream.<Integer>of(Integer.valueOf(value)));
    }

    static IntStream of(int... values) {
        return IntPipeline.ofArray(values, 0, values.length);
    }

    static IntStream iterate(int seed, IntUnaryOperator f) {
        if (f == null) {
            throw new NullPointerException();
        }
        return new IntPipeline(Stream.<Integer>iterate(Integer.valueOf(seed), v -> f.applyAsInt(v)));
    }

    static IntStream iterate(int seed, IntPredicate hasNext, IntUnaryOperator next) {
        if (hasNext == null || next == null) {
            throw new NullPointerException();
        }
        return new IntPipeline(Stream.<Integer>iterate(Integer.valueOf(seed), v -> hasNext.test(v),
                v -> next.applyAsInt(v)));
    }

    static IntStream generate(IntSupplier s) {
        if (s == null) {
            throw new NullPointerException();
        }
        return new IntPipeline(Stream.<Integer>generate(() -> s.getAsInt()));
    }

    static IntStream range(int startInclusive, int endExclusive) {
        return IntPipeline.ofRange(startInclusive, endExclusive, false);
    }

    static IntStream rangeClosed(int startInclusive, int endInclusive) {
        return IntPipeline.ofRange(startInclusive, endInclusive, true);
    }

    static IntStream concat(IntStream a, IntStream b) {
        return new IntPipeline(Stream.concat(a.boxed(), b.boxed()));
    }
}
