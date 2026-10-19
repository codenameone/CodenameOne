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

import java.util.IntSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;

/// A sequence of `int` values to which operations are applied one stage at a
/// time, evaluated lazily, one element at a time, when a terminal operation
/// asks for them.
///
/// Streams here are sequential: `parallel()` answers the same stream, which
/// the JDK allows a parallel stream to be.
public interface IntStream extends BaseStream<Integer, IntStream> {

    /// Declared again with this type, as the JDK does: a call compiled
    /// against the JDK names `IntStream.sequential()` answering `IntStream`,
    /// and the method inherited from [BaseStream] answers its type
    /// variable, which is another descriptor.
    @Override
    IntStream sequential();

    /// Declared again with this type; see [#sequential()].
    @Override
    IntStream parallel();

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
        return IntPipeline.ofArray(new int[0], 0, 0);
    }

    static IntStream of(int value) {
        return IntPipeline.ofArray(new int[] {value}, 0, 1);
    }

    static IntStream of(int... values) {
        return IntPipeline.ofArray(values, 0, values.length);
    }

    /// `seed`, then `f` applied to it, then `f` applied to that, without end.
    static IntStream iterate(int seed, IntUnaryOperator f) {
        return IntPipeline.iterate(seed, null, f);
    }

    /// As [#iterate(int, IntUnaryOperator)], ending before the first value
    /// `hasNext` refuses.
    static IntStream iterate(int seed, IntPredicate hasNext, IntUnaryOperator next) {
        if (hasNext == null) {
            throw new NullPointerException();
        }
        return IntPipeline.iterate(seed, hasNext, next);
    }

    /// The values `s` answers, one call per element, without end.
    static IntStream generate(IntSupplier s) {
        return IntPipeline.generate(s);
    }

    /// The values from `startInclusive` up to, not including, `endExclusive`,
    /// in steps of one. Empty when `startInclusive` is not below `endExclusive`.
    static IntStream range(int startInclusive, int endExclusive) {
        return IntPipeline.ofRange(startInclusive, endExclusive, false);
    }

    /// The values from `startInclusive` up to and including `endInclusive`, in
    /// steps of one. Empty when `startInclusive` is above `endInclusive`.
    static IntStream rangeClosed(int startInclusive, int endInclusive) {
        return IntPipeline.ofRange(startInclusive, endInclusive, true);
    }

    /// The elements of `a`, then those of `b`. Closing the result closes both.
    static IntStream concat(IntStream a, IntStream b) {
        // Both checked before boxed() consumes a: a refused call leaves a usable.
        if (a == null || b == null) {
            throw new NullPointerException();
        }
        return new IntPipeline(Stream.concat(a.boxed(), b.boxed()));
    }
}
