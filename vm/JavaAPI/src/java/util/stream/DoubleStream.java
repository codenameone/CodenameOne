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

import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;

/// A sequence of `double` values to which operations are applied one stage at a
/// time, evaluated lazily, one element at a time, when a terminal operation
/// asks for them.
///
/// Streams here are sequential: `parallel()` answers the same stream, which
/// the JDK allows a parallel stream to be.
public interface DoubleStream extends BaseStream<Double, DoubleStream> {

    /// Declared again with this type, as the JDK does: a call compiled
    /// against the JDK names `DoubleStream.sequential()` answering `DoubleStream`,
    /// and the method inherited from [BaseStream] answers its type
    /// variable, which is another descriptor.
    @Override
    DoubleStream sequential();

    /// Declared again with this type; see [#sequential()].
    @Override
    DoubleStream parallel();

    DoubleStream filter(DoublePredicate predicate);

    DoubleStream map(DoubleUnaryOperator mapper);

    <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper);

    IntStream mapToInt(DoubleToIntFunction mapper);

    LongStream mapToLong(DoubleToLongFunction mapper);

    DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper);

    DoubleStream distinct();

    DoubleStream sorted();

    DoubleStream peek(DoubleConsumer action);

    DoubleStream limit(long maxSize);

    DoubleStream skip(long n);

    DoubleStream takeWhile(DoublePredicate predicate);

    DoubleStream dropWhile(DoublePredicate predicate);

    void forEach(DoubleConsumer action);

    void forEachOrdered(DoubleConsumer action);

    double[] toArray();

    double reduce(double identity, DoubleBinaryOperator op);

    OptionalDouble reduce(DoubleBinaryOperator op);

    <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner);

    double sum();

    OptionalDouble min();

    OptionalDouble max();

    long count();

    OptionalDouble average();

    DoubleSummaryStatistics summaryStatistics();

    boolean anyMatch(DoublePredicate predicate);

    boolean allMatch(DoublePredicate predicate);

    boolean noneMatch(DoublePredicate predicate);

    OptionalDouble findFirst();

    OptionalDouble findAny();

    Stream<Double> boxed();

    @Override
    PrimitiveIterator.OfDouble iterator();

    static DoubleStream empty() {
        return DoublePipeline.ofArray(new double[0], 0, 0);
    }

    static DoubleStream of(double value) {
        return DoublePipeline.ofArray(new double[] {value}, 0, 1);
    }

    static DoubleStream of(double... values) {
        return DoublePipeline.ofArray(values, 0, values.length);
    }

    /// `seed`, then `f` applied to it, then `f` applied to that, without end.
    static DoubleStream iterate(double seed, DoubleUnaryOperator f) {
        return DoublePipeline.iterate(seed, null, f);
    }

    /// As [#iterate(double, DoubleUnaryOperator)], ending before the first value
    /// `hasNext` refuses.
    static DoubleStream iterate(double seed, DoublePredicate hasNext, DoubleUnaryOperator next) {
        if (hasNext == null) {
            throw new NullPointerException();
        }
        return DoublePipeline.iterate(seed, hasNext, next);
    }

    /// The values `s` answers, one call per element, without end.
    static DoubleStream generate(DoubleSupplier s) {
        return DoublePipeline.generate(s);
    }

    /// The elements of `a`, then those of `b`. Closing the result closes both.
    static DoubleStream concat(DoubleStream a, DoubleStream b) {
        return new DoublePipeline(Stream.concat(a.boxed(), b.boxed()));
    }
}
