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
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/// The one implementation of [IntStream]: a view of a stream of boxed values,
/// which does the work. Boxing costs little on ParparVM, where a boxed
/// `int` from `valueOf` is usually an immediate rather than an object.
final class IntPipeline implements IntStream {

    private final Stream<Integer> boxed;

    IntPipeline(Stream<Integer> boxed) {
        this.boxed = boxed;
    }

    /// The elements `from` (inclusive) to `to` (exclusive) of `values`.
    static IntStream ofArray(final int[] values, final int from, final int to) {
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
    static IntStream ofRange(final int start, final int end, final boolean closed) {
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

    static IntStream iterate(int seed, IntPredicate hasNext, IntUnaryOperator next) {
        Objects.requireNonNull(next);
        UnaryOperator<Integer> step = Fn.<Integer, Integer>function(Fn.MAP, next);
        if (hasNext == null) {
            return new IntPipeline(Stream.<Integer>iterate(Integer.valueOf(seed), step));
        }
        return new IntPipeline(Stream.<Integer>iterate(Integer.valueOf(seed), Fn.<Integer>predicate(hasNext), step));
    }

    @SuppressWarnings("unchecked")
    static IntStream generate(IntSupplier s) {
        Objects.requireNonNull(s);
        return new IntPipeline(Stream.<Integer>generate((Supplier<Integer>) new Fn(Fn.GET, s)));
    }

    /// Each primitive function this stream is handed, as the object function
    /// the boxed stream underneath takes. One class for every operation keeps
    /// an application that uses this stream from carrying a class per method.
    @SuppressWarnings({"rawtypes", "unchecked"})
    static final class Fn implements Predicate, UnaryOperator, Consumer, Supplier {
        static final int TEST = 0;
        static final int MAP = 1;
        static final int TO_OBJ = 2;
        static final int TO_LONG = 3;
        static final int TO_DOUBLE = 4;
        static final int WIDEN_LONG = 5;
        static final int WIDEN_DOUBLE = 6;
        static final int FLAT = 7;
        static final int BOX = 8;
        static final int ACCEPT = 9;
        static final int GET = 10;

        private final int kind;
        private final Object fn;

        Fn(int kind, Object fn) {
            this.kind = kind;
            this.fn = fn;
        }

        static <T, R> UnaryOperator function(int kind, Object fn) {
            return new Fn(kind, fn);
        }

        static <T> Predicate<T> predicate(IntPredicate fn) {
            return new Fn(TEST, fn);
        }

        static <T> Consumer<T> consumer(IntConsumer fn) {
            return new Fn(ACCEPT, fn);
        }

        @Override
        public boolean test(Object o) {
            return ((IntPredicate) fn).test(((Integer) o).intValue());
        }

        @Override
        public Object apply(Object o) {
            if (kind == BOX) {
                return o;
            }
            int v = ((Integer) o).intValue();
            switch (kind) {
                case MAP:
                    return Integer.valueOf(((IntUnaryOperator) fn).applyAsInt(v));
                case TO_OBJ:
                    return ((IntFunction) fn).apply(v);
                case TO_LONG:
                    return Long.valueOf(((IntToLongFunction) fn).applyAsLong(v));
                case TO_DOUBLE:
                    return Double.valueOf(((IntToDoubleFunction) fn).applyAsDouble(v));
                case WIDEN_LONG:
                    return Long.valueOf(v);
                case WIDEN_DOUBLE:
                    return Double.valueOf(v);
                default: {
                    IntStream inner = (IntStream) ((IntFunction) fn).apply(v);
                    return inner == null ? null : inner.boxed();
                }
            }
        }

        @Override
        public void accept(Object o) {
            ((IntConsumer) fn).accept(((Integer) o).intValue());
        }

        @Override
        public Object get() {
            return Integer.valueOf(((IntSupplier) fn).getAsInt());
        }
    }

    @Override
    public IntStream filter(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new IntPipeline(boxed.filter(Fn.<Integer>predicate(predicate)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public IntStream map(IntUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline(boxed.<Integer>map(Fn.<Integer, Integer>function(Fn.MAP, mapper)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> Stream<U> mapToObj(IntFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return boxed.<U>map(Fn.<Integer, U>function(Fn.TO_OBJ, mapper));
    }

    @Override
    public LongStream mapToLong(IntToLongFunction mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline(boxed.<Long>map(Fn.<Integer, Long>function(Fn.TO_LONG, mapper)));
    }

    @Override
    public DoubleStream mapToDouble(IntToDoubleFunction mapper) {
        Objects.requireNonNull(mapper);
        return new DoublePipeline(boxed.<Double>map(Fn.<Integer, Double>function(Fn.TO_DOUBLE, mapper)));
    }

    @Override
    public LongStream asLongStream() {
        return new LongPipeline(boxed.<Long>map(Fn.<Integer, Long>function(Fn.WIDEN_LONG, null)));
    }

    @Override
    public DoubleStream asDoubleStream() {
        return new DoublePipeline(boxed.<Double>map(Fn.<Integer, Double>function(Fn.WIDEN_DOUBLE, null)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public IntStream flatMap(IntFunction<? extends IntStream> mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline(boxed.<Integer>flatMap(Fn.<Integer, Stream<Integer>>function(Fn.FLAT, mapper)));
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
        return new IntPipeline(boxed.peek(Fn.<Integer>consumer(action)));
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
        return new IntPipeline(boxed.takeWhile(Fn.<Integer>predicate(predicate)));
    }

    @Override
    public IntStream dropWhile(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new IntPipeline(boxed.dropWhile(Fn.<Integer>predicate(predicate)));
    }

    @Override
    public void forEach(IntConsumer action) {
        Objects.requireNonNull(action);
        Iterator<Integer> it = boxed.iterator();
        while (it.hasNext()) {
            action.accept(it.next().intValue());
        }
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
            out[i] = all.get(i).intValue();
        }
        return out;
    }

    @Override
    public int reduce(int identity, IntBinaryOperator op) {
        Objects.requireNonNull(op);
        int result = identity;
        Iterator<Integer> it = boxed.iterator();
        while (it.hasNext()) {
            result = op.applyAsInt(result, it.next().intValue());
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
        int result = it.next().intValue();
        while (it.hasNext()) {
            result = op.applyAsInt(result, it.next().intValue());
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
            accumulator.accept(result, it.next().intValue());
        }
        return result;
    }

    @Override
    public int sum() {
        int total = 0;
        Iterator<Integer> it = boxed.iterator();
        while (it.hasNext()) {
            total += it.next().intValue();
        }
        return total;
    }

    /// The smallest (`max` false) or largest value, as `Math.min` and
    /// `Math.max` choose, which is what the JDK's reduction uses.
    private OptionalInt extreme(boolean max) {
        Iterator<Integer> it = boxed.iterator();
        if (!it.hasNext()) {
            return OptionalInt.empty();
        }
        int result = it.next().intValue();
        while (it.hasNext()) {
            int v = it.next().intValue();
            result = max ? Math.max(result, v) : Math.min(result, v);
        }
        return OptionalInt.of(result);
    }

    @Override
    public OptionalInt min() {
        return extreme(false);
    }

    @Override
    public OptionalInt max() {
        return extreme(true);
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
        return boxed.anyMatch(Fn.<Integer>predicate(predicate));
    }

    @Override
    public boolean allMatch(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.allMatch(Fn.<Integer>predicate(predicate));
    }

    @Override
    public boolean noneMatch(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.noneMatch(Fn.<Integer>predicate(predicate));
    }

    @Override
    public OptionalInt findFirst() {
        // Through the boxed stream's own, which says that the rest is not
        // going to be read: a mapped stream left part read is closed.
        Optional<Integer> first = boxed.findFirst();
        return first.isPresent() ? OptionalInt.of(first.get().intValue()) : OptionalInt.empty();
    }

    @Override
    public OptionalInt findAny() {
        return findFirst();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Stream<Integer> boxed() {
        // A stage of its own, so that this stream counts as used like any
        // other that had an operation applied to it.
        return boxed.map(Fn.<Integer, Integer>function(Fn.BOX, null));
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
            return it.next().intValue();
        }
    }

    @Override
    public boolean isParallel() {
        return boxed.isParallel();
    }

    @Override
    public IntStream sequential() {
        boxed.sequential();
        return this;
    }

    @Override
    public IntStream parallel() {
        boxed.parallel();
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
