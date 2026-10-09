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

/// `java.util.stream.LongStream` for the Codename One runtime: a sequential
/// stream of `long` values, evaluated lazily, one element at a time, when a
/// terminal operation asks for them.
///
/// There is no parallel evaluation: `parallel()` answers the same stream.
public interface LongStream extends BaseStream<Long, LongStream> {

    LongStream filter(LongPredicate predicate);

    LongStream map(LongUnaryOperator mapper);

    <U> Stream<U> mapToObj(LongFunction<? extends U> mapper);

    IntStream mapToInt(LongToIntFunction mapper);

    DoubleStream mapToDouble(LongToDoubleFunction mapper);

    LongStream flatMap(LongFunction<? extends LongStream> mapper);

    LongStream distinct();

    LongStream sorted();

    LongStream peek(LongConsumer action);

    LongStream limit(long maxSize);

    LongStream skip(long n);

    LongStream takeWhile(LongPredicate predicate);

    LongStream dropWhile(LongPredicate predicate);

    void forEach(LongConsumer action);

    void forEachOrdered(LongConsumer action);

    long[] toArray();

    long reduce(long identity, LongBinaryOperator op);

    OptionalLong reduce(LongBinaryOperator op);

    <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner);

    long sum();

    OptionalLong min();

    OptionalLong max();

    long count();

    OptionalDouble average();

    LongSummaryStatistics summaryStatistics();

    boolean anyMatch(LongPredicate predicate);

    boolean allMatch(LongPredicate predicate);

    boolean noneMatch(LongPredicate predicate);

    OptionalLong findFirst();

    OptionalLong findAny();

    DoubleStream asDoubleStream();

    Stream<Long> boxed();

    @Override
    PrimitiveIterator.OfLong iterator();

    static LongStream empty() {
        return new LongPipeline(Stream.<Long>empty());
    }

    static LongStream of(long value) {
        return new LongPipeline(Stream.<Long>of(Long.valueOf(value)));
    }

    static LongStream of(long... values) {
        return LongPipeline.ofArray(values, 0, values.length);
    }

    static LongStream iterate(long seed, LongUnaryOperator f) {
        if (f == null) {
            throw new NullPointerException();
        }
        return new LongPipeline(Stream.<Long>iterate(Long.valueOf(seed), v -> f.applyAsLong(v)));
    }

    static LongStream iterate(long seed, LongPredicate hasNext, LongUnaryOperator next) {
        if (hasNext == null || next == null) {
            throw new NullPointerException();
        }
        return new LongPipeline(Stream.<Long>iterate(Long.valueOf(seed), v -> hasNext.test(v),
                v -> next.applyAsLong(v)));
    }

    static LongStream generate(LongSupplier s) {
        if (s == null) {
            throw new NullPointerException();
        }
        return new LongPipeline(Stream.<Long>generate(() -> s.getAsLong()));
    }

    static LongStream range(long startInclusive, long endExclusive) {
        return LongPipeline.ofRange(startInclusive, endExclusive, false);
    }

    static LongStream rangeClosed(long startInclusive, long endInclusive) {
        return LongPipeline.ofRange(startInclusive, endInclusive, true);
    }

    static LongStream concat(LongStream a, LongStream b) {
        return new LongPipeline(Stream.concat(a.boxed(), b.boxed()));
    }
}
