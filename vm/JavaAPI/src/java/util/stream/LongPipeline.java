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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/// The one implementation of [LongStream]: a view of a stream of boxed values,
/// which does the work. Boxing costs little on ParparVM, where a boxed
/// `long` from `valueOf` is usually an immediate rather than an object.
final class LongPipeline implements LongStream {

    private final Stream<Long> boxed;

    LongPipeline(Stream<Long> boxed) {
        this.boxed = boxed;
    }

    /// The elements `from` (inclusive) to `to` (exclusive) of `values`.
    static LongStream ofArray(final long[] values, final int from, final int to) {
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
    static LongStream ofRange(final long start, final long end, final boolean closed) {
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

    static LongStream iterate(long seed, LongPredicate hasNext, LongUnaryOperator next) {
        Objects.requireNonNull(next);
        UnaryOperator<Long> step = Fn.<Long, Long>function(Fn.MAP, next);
        if (hasNext == null) {
            return new LongPipeline(Stream.<Long>iterate(Long.valueOf(seed), step));
        }
        return new LongPipeline(Stream.<Long>iterate(Long.valueOf(seed), Fn.<Long>predicate(hasNext), step));
    }

    @SuppressWarnings("unchecked")
    static LongStream generate(LongSupplier s) {
        Objects.requireNonNull(s);
        return new LongPipeline(Stream.<Long>generate((Supplier<Long>) new Fn(Fn.GET, s)));
    }

    /// Each primitive function this stream is handed, as the object function
    /// the boxed stream underneath takes. One class for every operation keeps
    /// an application that uses this stream from carrying a class per method.
    @SuppressWarnings({"rawtypes", "unchecked"})
    static final class Fn implements Predicate, UnaryOperator, Consumer, Supplier {
        static final int TEST = 0;
        static final int MAP = 1;
        static final int TO_OBJ = 2;
        static final int TO_INT = 3;
        static final int TO_DOUBLE = 4;
        static final int WIDEN_DOUBLE = 5;
        static final int FLAT = 6;
        static final int BOX = 7;
        static final int ACCEPT = 8;
        static final int GET = 9;

        private final int kind;
        private final Object fn;

        Fn(int kind, Object fn) {
            this.kind = kind;
            this.fn = fn;
        }

        static <T, R> UnaryOperator function(int kind, Object fn) {
            return new Fn(kind, fn);
        }

        static <T> Predicate<T> predicate(LongPredicate fn) {
            return new Fn(TEST, fn);
        }

        static <T> Consumer<T> consumer(LongConsumer fn) {
            return new Fn(ACCEPT, fn);
        }

        @Override
        public boolean test(Object o) {
            return ((LongPredicate) fn).test(((Long) o).longValue());
        }

        @Override
        public Object apply(Object o) {
            if (kind == BOX) {
                return o;
            }
            long v = ((Long) o).longValue();
            switch (kind) {
                case MAP:
                    return Long.valueOf(((LongUnaryOperator) fn).applyAsLong(v));
                case TO_OBJ:
                    return ((LongFunction) fn).apply(v);
                case TO_INT:
                    return Integer.valueOf(((LongToIntFunction) fn).applyAsInt(v));
                case TO_DOUBLE:
                    return Double.valueOf(((LongToDoubleFunction) fn).applyAsDouble(v));
                case WIDEN_DOUBLE:
                    return Double.valueOf(v);
                default: {
                    LongStream inner = (LongStream) ((LongFunction) fn).apply(v);
                    return inner == null ? null : inner.boxed();
                }
            }
        }

        @Override
        public void accept(Object o) {
            ((LongConsumer) fn).accept(((Long) o).longValue());
        }

        @Override
        public Object get() {
            return Long.valueOf(((LongSupplier) fn).getAsLong());
        }
    }

    @Override
    public LongStream filter(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new LongPipeline(boxed.filter(Fn.<Long>predicate(predicate)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public LongStream map(LongUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline(boxed.<Long>map(Fn.<Long, Long>function(Fn.MAP, mapper)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> Stream<U> mapToObj(LongFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return boxed.<U>map(Fn.<Long, U>function(Fn.TO_OBJ, mapper));
    }

    @Override
    public IntStream mapToInt(LongToIntFunction mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline(boxed.<Integer>map(Fn.<Long, Integer>function(Fn.TO_INT, mapper)));
    }

    @Override
    public DoubleStream mapToDouble(LongToDoubleFunction mapper) {
        Objects.requireNonNull(mapper);
        return new DoublePipeline(boxed.<Double>map(Fn.<Long, Double>function(Fn.TO_DOUBLE, mapper)));
    }

    @Override
    public DoubleStream asDoubleStream() {
        return new DoublePipeline(boxed.<Double>map(Fn.<Long, Double>function(Fn.WIDEN_DOUBLE, null)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public LongStream flatMap(LongFunction<? extends LongStream> mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline(boxed.<Long>flatMap(Fn.<Long, Stream<Long>>function(Fn.FLAT, mapper)));
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
        return new LongPipeline(boxed.peek(Fn.<Long>consumer(action)));
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
        return new LongPipeline(boxed.takeWhile(Fn.<Long>predicate(predicate)));
    }

    @Override
    public LongStream dropWhile(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new LongPipeline(boxed.dropWhile(Fn.<Long>predicate(predicate)));
    }

    @Override
    public void forEach(LongConsumer action) {
        Objects.requireNonNull(action);
        Iterator<Long> it = boxed.iterator();
        try {
            while (it.hasNext()) {
                action.accept(it.next().longValue());
            }
        } catch (Throwable e) {
            throw ObjPipeline.fail(boxed, e);
        }
    }

    @Override
    public void forEachOrdered(LongConsumer action) {
        forEach(action);
    }

    @Override
    public long[] toArray() {
        List<Long> all = new ArrayList<Long>();
        Iterator<Long> it = boxed.iterator();
        try {
            while (it.hasNext()) {
                all.add(it.next());
            }
            long[] out = new long[all.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = all.get(i).longValue();
            }
            return out;
        } catch (Throwable e) {
            throw ObjPipeline.fail(boxed, e);
        }
    }

    @Override
    public long reduce(long identity, LongBinaryOperator op) {
        Objects.requireNonNull(op);
        long result = identity;
        Iterator<Long> it = boxed.iterator();
        try {
            while (it.hasNext()) {
                result = op.applyAsLong(result, it.next().longValue());
            }
            return result;
        } catch (Throwable e) {
            throw ObjPipeline.fail(boxed, e);
        }
    }

    @Override
    public OptionalLong reduce(LongBinaryOperator op) {
        Objects.requireNonNull(op);
        Iterator<Long> it = boxed.iterator();
        try {
            if (!it.hasNext()) {
                return OptionalLong.empty();
            }
            long result = it.next().longValue();
            while (it.hasNext()) {
                result = op.applyAsLong(result, it.next().longValue());
            }
            return OptionalLong.of(result);
        } catch (Throwable e) {
            throw ObjPipeline.fail(boxed, e);
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        R result = supplier.get();
        Iterator<Long> it = boxed.iterator();
        try {
            while (it.hasNext()) {
                accumulator.accept(result, it.next().longValue());
            }
            return result;
        } catch (Throwable e) {
            throw ObjPipeline.fail(boxed, e);
        }
    }

    @Override
    public long sum() {
        long total = 0;
        Iterator<Long> it = boxed.iterator();
        try {
            while (it.hasNext()) {
                total += it.next().longValue();
            }
            return total;
        } catch (Throwable e) {
            throw ObjPipeline.fail(boxed, e);
        }
    }

    /// The smallest (`max` false) or largest value, as `Math.min` and
    /// `Math.max` choose, which is what the JDK's reduction uses.
    private OptionalLong extreme(boolean max) {
        Iterator<Long> it = boxed.iterator();
        try {
            if (!it.hasNext()) {
                return OptionalLong.empty();
            }
            long result = it.next().longValue();
            while (it.hasNext()) {
                long v = it.next().longValue();
                result = max ? Math.max(result, v) : Math.min(result, v);
            }
            return OptionalLong.of(result);
        } catch (Throwable e) {
            throw ObjPipeline.fail(boxed, e);
        }
    }

    @Override
    public OptionalLong min() {
        return extreme(false);
    }

    @Override
    public OptionalLong max() {
        return extreme(true);
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
        try {
            while (it.hasNext()) {
                all.accept(it.next().longValue());
            }
            return all;
        } catch (Throwable e) {
            throw ObjPipeline.fail(boxed, e);
        }
    }

    @Override
    public boolean anyMatch(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.anyMatch(Fn.<Long>predicate(predicate));
    }

    @Override
    public boolean allMatch(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.allMatch(Fn.<Long>predicate(predicate));
    }

    @Override
    public boolean noneMatch(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.noneMatch(Fn.<Long>predicate(predicate));
    }

    @Override
    public OptionalLong findFirst() {
        // Through the boxed stream's own, which says that the rest is not
        // going to be read: a mapped stream left part read is closed.
        Optional<Long> first = boxed.findFirst();
        return first.isPresent() ? OptionalLong.of(first.get().longValue()) : OptionalLong.empty();
    }

    @Override
    public OptionalLong findAny() {
        return findFirst();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Stream<Long> boxed() {
        // A stage of its own, so that this stream counts as used like any
        // other that had an operation applied to it.
        return boxed.map(Fn.<Long, Long>function(Fn.BOX, null));
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
            return it.next().longValue();
        }
    }

    @Override
    public boolean isParallel() {
        return boxed.isParallel();
    }

    @Override
    public LongStream sequential() {
        boxed.sequential();
        return this;
    }

    @Override
    public LongStream parallel() {
        boxed.parallel();
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
