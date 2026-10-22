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

import java.util.LongSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.function.BiConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;

/// A sequence of `long` values to which operations are applied one stage at a
/// time, evaluated lazily, one element at a time, when a terminal operation
/// asks for them.
///
/// Streams here are sequential: `parallel()` answers the same stream, which
/// the JDK allows a parallel stream to be.
public interface LongStream extends BaseStream<Long, LongStream> {

    /// Declared again with this type, as the JDK does: a call compiled
    /// against the JDK names `LongStream.sequential()` answering `LongStream`,
    /// and the method inherited from [BaseStream] answers its type
    /// variable, which is another descriptor.
    @Override
    LongStream sequential();

    /// Declared again with this type; see [#sequential()].
    @Override
    LongStream parallel();

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
        return LongPipeline.ofArray(new long[0], 0, 0);
    }

    static LongStream of(long value) {
        return LongPipeline.ofArray(new long[] {value}, 0, 1);
    }

    static LongStream of(long... values) {
        return LongPipeline.ofArray(values, 0, values.length);
    }

    /// `seed`, then `f` applied to it, then `f` applied to that, without end.
    static LongStream iterate(long seed, LongUnaryOperator f) {
        return LongPipeline.iterate(seed, null, f);
    }

    /// As [#iterate(long, LongUnaryOperator)], ending before the first value
    /// `hasNext` refuses.
    static LongStream iterate(long seed, LongPredicate hasNext, LongUnaryOperator next) {
        if (hasNext == null) {
            throw new NullPointerException();
        }
        return LongPipeline.iterate(seed, hasNext, next);
    }

    /// The values `s` answers, one call per element, without end.
    static LongStream generate(LongSupplier s) {
        return LongPipeline.generate(s);
    }

    /// The values from `startInclusive` up to, not including, `endExclusive`,
    /// in steps of one. Empty when `startInclusive` is not below `endExclusive`.
    static LongStream range(long startInclusive, long endExclusive) {
        return LongPipeline.ofRange(startInclusive, endExclusive, false);
    }

    /// The values from `startInclusive` up to and including `endInclusive`, in
    /// steps of one. Empty when `startInclusive` is above `endInclusive`.
    static LongStream rangeClosed(long startInclusive, long endInclusive) {
        return LongPipeline.ofRange(startInclusive, endInclusive, true);
    }

    /// The elements of `a`, then those of `b`. Closing the result closes both.
    static LongStream concat(LongStream a, LongStream b) {
        // Both checked before boxed() consumes a: a refused call leaves a usable.
        if (a == null || b == null) {
            throw new NullPointerException();
        }
        return new LongPipeline(Stream.concat(a.boxed(), b.boxed()));
    }
}
