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

/// `java.util.stream.DoubleStream` for the Codename One runtime: a sequential
/// stream of `double` values, evaluated lazily, one element at a time, when a
/// terminal operation asks for them.
///
/// There is no parallel evaluation: `parallel()` answers the same stream.
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
        return new DoublePipeline(Stream.<Double>empty());
    }

    static DoubleStream of(double value) {
        return new DoublePipeline(Stream.<Double>of(Double.valueOf(value)));
    }

    static DoubleStream of(double... values) {
        return DoublePipeline.ofArray(values, 0, values.length);
    }

    static DoubleStream iterate(double seed, DoubleUnaryOperator f) {
        if (f == null) {
            throw new NullPointerException();
        }
        return new DoublePipeline(Stream.<Double>iterate(Double.valueOf(seed), v -> f.applyAsDouble(v)));
    }

    static DoubleStream iterate(double seed, DoublePredicate hasNext, DoubleUnaryOperator next) {
        if (hasNext == null || next == null) {
            throw new NullPointerException();
        }
        return new DoublePipeline(Stream.<Double>iterate(Double.valueOf(seed), v -> hasNext.test(v),
                v -> next.applyAsDouble(v)));
    }

    static DoubleStream generate(DoubleSupplier s) {
        if (s == null) {
            throw new NullPointerException();
        }
        return new DoublePipeline(Stream.<Double>generate(() -> s.getAsDouble()));
    }

    static DoubleStream concat(DoubleStream a, DoubleStream b) {
        return new DoublePipeline(Stream.concat(a.boxed(), b.boxed()));
    }
}
