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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/// The one implementation of [DoubleStream]: a view of a stream of boxed
/// values, which does the work.
final class DoublePipeline implements DoubleStream {

    private final Stream<Double> boxed;

    DoublePipeline(Stream<Double> boxed) {
        this.boxed = boxed;
    }

    /// The elements `from` (inclusive) to `to` (exclusive) of `values`.
    static DoubleStream ofArray(double[] values, int from, int to) {
        if (from < 0 || to > values.length || from > to) {
            throw new ArrayIndexOutOfBoundsException("origin(" + from + ") > fence(" + to + ")");
        }
        return new DoublePipeline(ObjPipeline.over(new PullIterator<Double>() {
            private int at = from;

            @Override
            boolean advance() {
                if (at >= to) {
                    return false;
                }
                emit(Double.valueOf(values[at++]));
                return true;
            }
        }));
    }

    @Override
    public DoubleStream filter(DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        return new DoublePipeline(boxed.filter(v -> predicate.test(v)));
    }

    @Override
    public DoubleStream map(DoubleUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        return new DoublePipeline(boxed.<Double>map(v -> mapper.applyAsDouble(v)));
    }

    @Override
    public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return boxed.<U>map(v -> mapper.apply(v));
    }

    @Override
    public IntStream mapToInt(DoubleToIntFunction mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline(boxed.<Integer>map(v -> mapper.applyAsInt(v)));
    }

    @Override
    public LongStream mapToLong(DoubleToLongFunction mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline(boxed.<Long>map(v -> mapper.applyAsLong(v)));
    }

    @Override
    public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
        Objects.requireNonNull(mapper);
        return new DoublePipeline(boxed.<Double>flatMap(v -> {
            DoubleStream inner = mapper.apply(v);
            return inner == null ? null : inner.boxed();
        }));
    }

    @Override
    public DoubleStream distinct() {
        return new DoublePipeline(boxed.distinct());
    }

    @Override
    public DoubleStream sorted() {
        return new DoublePipeline(boxed.sorted());
    }

    @Override
    public DoubleStream peek(DoubleConsumer action) {
        Objects.requireNonNull(action);
        return new DoublePipeline(boxed.peek(v -> action.accept(v)));
    }

    @Override
    public DoubleStream limit(long maxSize) {
        return new DoublePipeline(boxed.limit(maxSize));
    }

    @Override
    public DoubleStream skip(long n) {
        return new DoublePipeline(boxed.skip(n));
    }

    @Override
    public DoubleStream takeWhile(DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        return new DoublePipeline(boxed.takeWhile(v -> predicate.test(v)));
    }

    @Override
    public DoubleStream dropWhile(DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        return new DoublePipeline(boxed.dropWhile(v -> predicate.test(v)));
    }

    @Override
    public void forEach(DoubleConsumer action) {
        Objects.requireNonNull(action);
        boxed.forEach(v -> action.accept(v));
    }

    @Override
    public void forEachOrdered(DoubleConsumer action) {
        forEach(action);
    }

    @Override
    public double[] toArray() {
        List<Double> all = new ArrayList<Double>();
        Iterator<Double> it = boxed.iterator();
        while (it.hasNext()) {
            all.add(it.next());
        }
        double[] out = new double[all.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = all.get(i);
        }
        return out;
    }

    @Override
    public double reduce(double identity, DoubleBinaryOperator op) {
        Objects.requireNonNull(op);
        double result = identity;
        Iterator<Double> it = boxed.iterator();
        while (it.hasNext()) {
            result = op.applyAsDouble(result, it.next());
        }
        return result;
    }

    @Override
    public OptionalDouble reduce(DoubleBinaryOperator op) {
        Objects.requireNonNull(op);
        Iterator<Double> it = boxed.iterator();
        if (!it.hasNext()) {
            return OptionalDouble.empty();
        }
        double result = it.next();
        while (it.hasNext()) {
            result = op.applyAsDouble(result, it.next());
        }
        return OptionalDouble.of(result);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        R result = supplier.get();
        Iterator<Double> it = boxed.iterator();
        while (it.hasNext()) {
            accumulator.accept(result, it.next());
        }
        return result;
    }

    @Override
    public double sum() {
        return summaryStatistics().getSum();
    }

    @Override
    public OptionalDouble min() {
        return reduce((a, b) -> Math.min(a, b));
    }

    @Override
    public OptionalDouble max() {
        return reduce((a, b) -> Math.max(a, b));
    }

    @Override
    public long count() {
        return boxed.count();
    }

    @Override
    public OptionalDouble average() {
        DoubleSummaryStatistics all = summaryStatistics();
        return all.getCount() == 0 ? OptionalDouble.empty() : OptionalDouble.of(all.getAverage());
    }

    @Override
    public DoubleSummaryStatistics summaryStatistics() {
        DoubleSummaryStatistics all = new DoubleSummaryStatistics();
        Iterator<Double> it = boxed.iterator();
        while (it.hasNext()) {
            all.accept(it.next().doubleValue());
        }
        return all;
    }

    @Override
    public boolean anyMatch(DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.anyMatch(v -> predicate.test(v));
    }

    @Override
    public boolean allMatch(DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.allMatch(v -> predicate.test(v));
    }

    @Override
    public boolean noneMatch(DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.noneMatch(v -> predicate.test(v));
    }

    @Override
    public OptionalDouble findFirst() {
        // Through the boxed stream's own, which says that the rest is not
        // going to be read: a mapped stream left part read is closed.
        Optional<Double> first = boxed.findFirst();
        return first.isPresent() ? OptionalDouble.of(first.get()) : OptionalDouble.empty();
    }

    @Override
    public OptionalDouble findAny() {
        return findFirst();
    }

    @Override
    public Stream<Double> boxed() {
        // A stage of its own, so that this stream counts as used like any
        // other that had an operation applied to it.
        return boxed.map(v -> v);
    }

    @Override
    public PrimitiveIterator.OfDouble iterator() {
        return new Unboxing(boxed.iterator());
    }

    /// The elements of the boxed pipeline, one primitive at a time.
    private static final class Unboxing implements PrimitiveIterator.OfDouble {
        private final Iterator<Double> it;

        Unboxing(Iterator<Double> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public double nextDouble() {
            return it.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }

    @Override
    public boolean isParallel() {
        return false;
    }

    @Override
    public DoubleStream sequential() {
        return this;
    }

    @Override
    public DoubleStream parallel() {
        return this;
    }

    @Override
    public DoubleStream unordered() {
        return this;
    }

    @Override
    public DoubleStream onClose(Runnable closeHandler) {
        boxed.onClose(closeHandler);
        return this;
    }

    @Override
    public void close() {
        boxed.close();
    }
}
