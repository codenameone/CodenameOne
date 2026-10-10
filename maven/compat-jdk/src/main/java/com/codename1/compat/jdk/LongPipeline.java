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

/// The one implementation of [LongStream]: a view of a stream of boxed
/// values, which does the work.
final class LongPipeline implements LongStream {

    private final Stream<Long> boxed;

    LongPipeline(Stream<Long> boxed) {
        this.boxed = boxed;
    }

    /// The elements `from` (inclusive) to `to` (exclusive) of `values`.
    static LongStream ofArray(long[] values, int from, int to) {
        if (from < 0 || to > values.length || from > to) {
            throw new ArrayIndexOutOfBoundsException("origin(" + from + ") > fence(" + to + ")");
        }
        return new LongPipeline(ObjPipeline.over(new PullIterator<Long>() {
            private int at = from;

            @Override
            boolean advance() {
                if (at >= to) {
                    return false;
                }
                emit(Long.valueOf(values[at++]));
                return true;
            }
        }));
    }

    /// The values from `start` up to `end`, which is part of them when
    /// `closed`. An empty stream when `start` is past `end`.
    static LongStream ofRange(long start, long end, boolean closed) {
        return new LongPipeline(ObjPipeline.over(new PullIterator<Long>() {
            private long next = start;
            /// Whether `next` is still to be handed out. Counting towards
            /// `end` with a flag, not past it, is what keeps a closed range
            /// that ends at the largest value from wrapping around.
            private boolean more = closed ? start <= end : start < end;

            @Override
            boolean advance() {
                if (!more) {
                    return false;
                }
                emit(Long.valueOf(next));
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
    public LongStream filter(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new LongPipeline(boxed.filter(v -> predicate.test(v)));
    }

    @Override
    public LongStream map(LongUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline(boxed.<Long>map(v -> mapper.applyAsLong(v)));
    }

    @Override
    public <U> Stream<U> mapToObj(LongFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return boxed.<U>map(v -> mapper.apply(v));
    }

    @Override
    public IntStream mapToInt(LongToIntFunction mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline(boxed.<Integer>map(v -> mapper.applyAsInt(v)));
    }

    @Override
    public DoubleStream mapToDouble(LongToDoubleFunction mapper) {
        Objects.requireNonNull(mapper);
        return new DoublePipeline(boxed.<Double>map(v -> mapper.applyAsDouble(v)));
    }

    @Override
    public LongStream flatMap(LongFunction<? extends LongStream> mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline(boxed.<Long>flatMap(v -> {
            LongStream inner = mapper.apply(v);
            return inner == null ? null : inner.boxed();
        }));
    }

    @Override
    public LongStream distinct() {
        return new LongPipeline(boxed.distinct());
    }

    @Override
    public LongStream sorted() {
        return new LongPipeline(boxed.sorted());
    }

    @Override
    public LongStream peek(LongConsumer action) {
        Objects.requireNonNull(action);
        return new LongPipeline(boxed.peek(v -> action.accept(v)));
    }

    @Override
    public LongStream limit(long maxSize) {
        return new LongPipeline(boxed.limit(maxSize));
    }

    @Override
    public LongStream skip(long n) {
        return new LongPipeline(boxed.skip(n));
    }

    @Override
    public LongStream takeWhile(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new LongPipeline(boxed.takeWhile(v -> predicate.test(v)));
    }

    @Override
    public LongStream dropWhile(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new LongPipeline(boxed.dropWhile(v -> predicate.test(v)));
    }

    @Override
    public void forEach(LongConsumer action) {
        Objects.requireNonNull(action);
        boxed.forEach(v -> action.accept(v));
    }

    @Override
    public void forEachOrdered(LongConsumer action) {
        forEach(action);
    }

    @Override
    public long[] toArray() {
        List<Long> all = new ArrayList<Long>();
        Iterator<Long> it = boxed.iterator();
        while (it.hasNext()) {
            all.add(it.next());
        }
        long[] out = new long[all.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = all.get(i);
        }
        return out;
    }

    @Override
    public long reduce(long identity, LongBinaryOperator op) {
        Objects.requireNonNull(op);
        long result = identity;
        Iterator<Long> it = boxed.iterator();
        while (it.hasNext()) {
            result = op.applyAsLong(result, it.next());
        }
        return result;
    }

    @Override
    public OptionalLong reduce(LongBinaryOperator op) {
        Objects.requireNonNull(op);
        Iterator<Long> it = boxed.iterator();
        if (!it.hasNext()) {
            return OptionalLong.empty();
        }
        long result = it.next();
        while (it.hasNext()) {
            result = op.applyAsLong(result, it.next());
        }
        return OptionalLong.of(result);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        R result = supplier.get();
        Iterator<Long> it = boxed.iterator();
        while (it.hasNext()) {
            accumulator.accept(result, it.next());
        }
        return result;
    }

    @Override
    public long sum() {
        long total = 0;
        Iterator<Long> it = boxed.iterator();
        while (it.hasNext()) {
            total += it.next();
        }
        return total;
    }

    @Override
    public OptionalLong min() {
        return reduce((a, b) -> Math.min(a, b));
    }

    @Override
    public OptionalLong max() {
        return reduce((a, b) -> Math.max(a, b));
    }

    @Override
    public long count() {
        return boxed.count();
    }

    @Override
    public OptionalDouble average() {
        LongSummaryStatistics all = summaryStatistics();
        return all.getCount() == 0 ? OptionalDouble.empty() : OptionalDouble.of(all.getAverage());
    }

    @Override
    public LongSummaryStatistics summaryStatistics() {
        LongSummaryStatistics all = new LongSummaryStatistics();
        Iterator<Long> it = boxed.iterator();
        while (it.hasNext()) {
            all.accept(it.next().longValue());
        }
        return all;
    }

    @Override
    public boolean anyMatch(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.anyMatch(v -> predicate.test(v));
    }

    @Override
    public boolean allMatch(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.allMatch(v -> predicate.test(v));
    }

    @Override
    public boolean noneMatch(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.noneMatch(v -> predicate.test(v));
    }

    @Override
    public OptionalLong findFirst() {
        // Through the boxed stream's own, which says that the rest is not
        // going to be read: a mapped stream left part read is closed.
        Optional<Long> first = boxed.findFirst();
        return first.isPresent() ? OptionalLong.of(first.get()) : OptionalLong.empty();
    }

    @Override
    public OptionalLong findAny() {
        return findFirst();
    }

    @Override
    public DoubleStream asDoubleStream() {
        return new DoublePipeline(boxed.<Double>map(v -> Double.valueOf(v.doubleValue())));
    }

    @Override
    public Stream<Long> boxed() {
        // A stage of its own, so that this stream counts as used like any
        // other that had an operation applied to it.
        return boxed.map(v -> v);
    }

    @Override
    public PrimitiveIterator.OfLong iterator() {
        return new Unboxing(boxed.iterator());
    }

    /// The elements of the boxed pipeline, one primitive at a time.
    private static final class Unboxing implements PrimitiveIterator.OfLong {
        private final Iterator<Long> it;

        Unboxing(Iterator<Long> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public long nextLong() {
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
    public LongStream sequential() {
        return this;
    }

    @Override
    public LongStream parallel() {
        return this;
    }

    @Override
    public LongStream unordered() {
        return this;
    }

    @Override
    public LongStream onClose(Runnable closeHandler) {
        boxed.onClose(closeHandler);
        return this;
    }

    @Override
    public void close() {
        boxed.close();
    }
}
