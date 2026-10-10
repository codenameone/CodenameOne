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

/// The one implementation of [IntStream]: a view of a stream of boxed
/// values, which does the work.
final class IntPipeline implements IntStream {

    private final Stream<Integer> boxed;

    IntPipeline(Stream<Integer> boxed) {
        this.boxed = boxed;
    }

    /// The elements `from` (inclusive) to `to` (exclusive) of `values`.
    static IntStream ofArray(int[] values, int from, int to) {
        if (from < 0 || to > values.length || from > to) {
            throw new ArrayIndexOutOfBoundsException("origin(" + from + ") > fence(" + to + ")");
        }
        return new IntPipeline(ObjPipeline.over(new PullIterator<Integer>() {
            private int at = from;

            @Override
            boolean advance() {
                if (at >= to) {
                    return false;
                }
                emit(Integer.valueOf(values[at++]));
                return true;
            }
        }));
    }

    /// The values from `start` up to `end`, which is part of them when
    /// `closed`. An empty stream when `start` is past `end`.
    static IntStream ofRange(int start, int end, boolean closed) {
        return new IntPipeline(ObjPipeline.over(new PullIterator<Integer>() {
            private int next = start;
            /// Whether `next` is still to be handed out. Counting towards
            /// `end` with a flag, not past it, is what keeps a closed range
            /// that ends at the largest value from wrapping around.
            private boolean more = closed ? start <= end : start < end;

            @Override
            boolean advance() {
                if (!more) {
                    return false;
                }
                emit(Integer.valueOf(next));
                if (closed ? next < end : next < end - 1) {
                    next++;
                } else {
                    more = false;
                }
                return true;
            }
        }));
    }

    @Override
    public IntStream filter(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new IntPipeline(boxed.filter(v -> predicate.test(v)));
    }

    @Override
    public IntStream map(IntUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline(boxed.<Integer>map(v -> mapper.applyAsInt(v)));
    }

    @Override
    public <U> Stream<U> mapToObj(IntFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return boxed.<U>map(v -> mapper.apply(v));
    }

    @Override
    public LongStream mapToLong(IntToLongFunction mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline(boxed.<Long>map(v -> mapper.applyAsLong(v)));
    }

    @Override
    public DoubleStream mapToDouble(IntToDoubleFunction mapper) {
        Objects.requireNonNull(mapper);
        return new DoublePipeline(boxed.<Double>map(v -> mapper.applyAsDouble(v)));
    }

    @Override
    public IntStream flatMap(IntFunction<? extends IntStream> mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline(boxed.<Integer>flatMap(v -> {
            IntStream inner = mapper.apply(v);
            return inner == null ? null : inner.boxed();
        }));
    }

    @Override
    public IntStream distinct() {
        return new IntPipeline(boxed.distinct());
    }

    @Override
    public IntStream sorted() {
        return new IntPipeline(boxed.sorted());
    }

    @Override
    public IntStream peek(IntConsumer action) {
        Objects.requireNonNull(action);
        return new IntPipeline(boxed.peek(v -> action.accept(v)));
    }

    @Override
    public IntStream limit(long maxSize) {
        return new IntPipeline(boxed.limit(maxSize));
    }

    @Override
    public IntStream skip(long n) {
        return new IntPipeline(boxed.skip(n));
    }

    @Override
    public IntStream takeWhile(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new IntPipeline(boxed.takeWhile(v -> predicate.test(v)));
    }

    @Override
    public IntStream dropWhile(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new IntPipeline(boxed.dropWhile(v -> predicate.test(v)));
    }

    @Override
    public void forEach(IntConsumer action) {
        Objects.requireNonNull(action);
        boxed.forEach(v -> action.accept(v));
    }

    @Override
    public void forEachOrdered(IntConsumer action) {
        forEach(action);
    }

    @Override
    public int[] toArray() {
        List<Integer> all = new ArrayList<Integer>();
        Iterator<Integer> it = boxed.iterator();
        while (it.hasNext()) {
            all.add(it.next());
        }
        int[] out = new int[all.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = all.get(i);
        }
        return out;
    }

    @Override
    public int reduce(int identity, IntBinaryOperator op) {
        Objects.requireNonNull(op);
        int result = identity;
        Iterator<Integer> it = boxed.iterator();
        while (it.hasNext()) {
            result = op.applyAsInt(result, it.next());
        }
        return result;
    }

    @Override
    public OptionalInt reduce(IntBinaryOperator op) {
        Objects.requireNonNull(op);
        Iterator<Integer> it = boxed.iterator();
        if (!it.hasNext()) {
            return OptionalInt.empty();
        }
        int result = it.next();
        while (it.hasNext()) {
            result = op.applyAsInt(result, it.next());
        }
        return OptionalInt.of(result);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        R result = supplier.get();
        Iterator<Integer> it = boxed.iterator();
        while (it.hasNext()) {
            accumulator.accept(result, it.next());
        }
        return result;
    }

    @Override
    public int sum() {
        int total = 0;
        Iterator<Integer> it = boxed.iterator();
        while (it.hasNext()) {
            total += it.next();
        }
        return total;
    }

    @Override
    public OptionalInt min() {
        return reduce((a, b) -> Math.min(a, b));
    }

    @Override
    public OptionalInt max() {
        return reduce((a, b) -> Math.max(a, b));
    }

    @Override
    public long count() {
        return boxed.count();
    }

    @Override
    public OptionalDouble average() {
        IntSummaryStatistics all = summaryStatistics();
        return all.getCount() == 0 ? OptionalDouble.empty() : OptionalDouble.of(all.getAverage());
    }

    @Override
    public IntSummaryStatistics summaryStatistics() {
        IntSummaryStatistics all = new IntSummaryStatistics();
        Iterator<Integer> it = boxed.iterator();
        while (it.hasNext()) {
            all.accept(it.next().intValue());
        }
        return all;
    }

    @Override
    public boolean anyMatch(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.anyMatch(v -> predicate.test(v));
    }

    @Override
    public boolean allMatch(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.allMatch(v -> predicate.test(v));
    }

    @Override
    public boolean noneMatch(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.noneMatch(v -> predicate.test(v));
    }

    @Override
    public OptionalInt findFirst() {
        // Through the boxed stream's own, which says that the rest is not
        // going to be read: a mapped stream left part read is closed.
        Optional<Integer> first = boxed.findFirst();
        return first.isPresent() ? OptionalInt.of(first.get()) : OptionalInt.empty();
    }

    @Override
    public OptionalInt findAny() {
        return findFirst();
    }

    @Override
    public LongStream asLongStream() {
        return new LongPipeline(boxed.<Long>map(v -> Long.valueOf(v.longValue())));
    }

    @Override
    public DoubleStream asDoubleStream() {
        return new DoublePipeline(boxed.<Double>map(v -> Double.valueOf(v.doubleValue())));
    }

    @Override
    public Stream<Integer> boxed() {
        // A stage of its own, so that this stream counts as used like any
        // other that had an operation applied to it.
        return boxed.map(v -> v);
    }

    @Override
    public PrimitiveIterator.OfInt iterator() {
        return new Unboxing(boxed.iterator());
    }

    /// The elements of the boxed pipeline, one primitive at a time.
    private static final class Unboxing implements PrimitiveIterator.OfInt {
        private final Iterator<Integer> it;

        Unboxing(Iterator<Integer> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public int nextInt() {
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
    public IntStream sequential() {
        return this;
    }

    @Override
    public IntStream parallel() {
        return this;
    }

    @Override
    public IntStream unordered() {
        return this;
    }

    @Override
    public IntStream onClose(Runnable closeHandler) {
        boxed.onClose(closeHandler);
        return this;
    }

    @Override
    public void close() {
        boxed.close();
    }
}
