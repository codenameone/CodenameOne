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
import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/// The one implementation of [DoubleStream]: a view of a stream of boxed values,
/// which does the work. Boxing costs little on ParparVM, where a boxed
/// `double` from `valueOf` is usually an immediate rather than an object.
final class DoublePipeline implements DoubleStream {

    private final Stream<Double> boxed;

    DoublePipeline(Stream<Double> boxed) {
        this.boxed = boxed;
    }

    /// The elements `from` (inclusive) to `to` (exclusive) of `values`.
    static DoubleStream ofArray(final double[] values, final int from, final int to) {
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

    static DoubleStream iterate(double seed, DoublePredicate hasNext, DoubleUnaryOperator next) {
        Objects.requireNonNull(next);
        UnaryOperator<Double> step = Fn.<Double, Double>function(Fn.MAP, next);
        if (hasNext == null) {
            return new DoublePipeline(Stream.<Double>iterate(Double.valueOf(seed), step));
        }
        return new DoublePipeline(Stream.<Double>iterate(Double.valueOf(seed), Fn.<Double>predicate(hasNext), step));
    }

    @SuppressWarnings("unchecked")
    static DoubleStream generate(DoubleSupplier s) {
        Objects.requireNonNull(s);
        return new DoublePipeline(Stream.<Double>generate((Supplier<Double>) new Fn(Fn.GET, s)));
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
        static final int TO_LONG = 4;
        static final int FLAT = 5;
        static final int BOX = 6;
        static final int ACCEPT = 7;
        static final int GET = 8;

        private final int kind;
        private final Object fn;

        Fn(int kind, Object fn) {
            this.kind = kind;
            this.fn = fn;
        }

        static <T, R> UnaryOperator function(int kind, Object fn) {
            return new Fn(kind, fn);
        }

        static <T> Predicate<T> predicate(DoublePredicate fn) {
            return new Fn(TEST, fn);
        }

        static <T> Consumer<T> consumer(DoubleConsumer fn) {
            return new Fn(ACCEPT, fn);
        }

        @Override
        public boolean test(Object o) {
            return ((DoublePredicate) fn).test(((Double) o).doubleValue());
        }

        @Override
        public Object apply(Object o) {
            if (kind == BOX) {
                return o;
            }
            double v = ((Double) o).doubleValue();
            switch (kind) {
                case MAP:
                    return Double.valueOf(((DoubleUnaryOperator) fn).applyAsDouble(v));
                case TO_OBJ:
                    return ((DoubleFunction) fn).apply(v);
                case TO_INT:
                    return Integer.valueOf(((DoubleToIntFunction) fn).applyAsInt(v));
                case TO_LONG:
                    return Long.valueOf(((DoubleToLongFunction) fn).applyAsLong(v));
                default: {
                    DoubleStream inner = (DoubleStream) ((DoubleFunction) fn).apply(v);
                    return inner == null ? null : inner.boxed();
                }
            }
        }

        @Override
        public void accept(Object o) {
            ((DoubleConsumer) fn).accept(((Double) o).doubleValue());
        }

        @Override
        public Object get() {
            return Double.valueOf(((DoubleSupplier) fn).getAsDouble());
        }
    }

    @Override
    public DoubleStream filter(DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        return new DoublePipeline(boxed.filter(Fn.<Double>predicate(predicate)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public DoubleStream map(DoubleUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        return new DoublePipeline(boxed.<Double>map(Fn.<Double, Double>function(Fn.MAP, mapper)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return boxed.<U>map(Fn.<Double, U>function(Fn.TO_OBJ, mapper));
    }

    @Override
    public IntStream mapToInt(DoubleToIntFunction mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline(boxed.<Integer>map(Fn.<Double, Integer>function(Fn.TO_INT, mapper)));
    }

    @Override
    public LongStream mapToLong(DoubleToLongFunction mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline(boxed.<Long>map(Fn.<Double, Long>function(Fn.TO_LONG, mapper)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
        Objects.requireNonNull(mapper);
        return new DoublePipeline(boxed.<Double>flatMap(Fn.<Double, Stream<Double>>function(Fn.FLAT, mapper)));
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
        return new DoublePipeline(boxed.peek(Fn.<Double>consumer(action)));
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
        return new DoublePipeline(boxed.takeWhile(Fn.<Double>predicate(predicate)));
    }

    @Override
    public DoubleStream dropWhile(DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        return new DoublePipeline(boxed.dropWhile(Fn.<Double>predicate(predicate)));
    }

    @Override
    public void forEach(DoubleConsumer action) {
        Objects.requireNonNull(action);
        Iterator<Double> it = boxed.iterator();
        try {
            while (it.hasNext()) {
                action.accept(it.next().doubleValue());
            }
        } catch (Throwable e) {
            throw ObjPipeline.fail(boxed, e);
        }
    }

    @Override
    public void forEachOrdered(DoubleConsumer action) {
        forEach(action);
    }

    @Override
    public double[] toArray() {
        List<Double> all = new ArrayList<Double>();
        Iterator<Double> it = boxed.iterator();
        try {
            while (it.hasNext()) {
                all.add(it.next());
            }
            double[] out = new double[all.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = all.get(i).doubleValue();
            }
            return out;
        } catch (Throwable e) {
            throw ObjPipeline.fail(boxed, e);
        }
    }

    @Override
    public double reduce(double identity, DoubleBinaryOperator op) {
        Objects.requireNonNull(op);
        double result = identity;
        Iterator<Double> it = boxed.iterator();
        try {
            while (it.hasNext()) {
                result = op.applyAsDouble(result, it.next().doubleValue());
            }
            return result;
        } catch (Throwable e) {
            throw ObjPipeline.fail(boxed, e);
        }
    }

    @Override
    public OptionalDouble reduce(DoubleBinaryOperator op) {
        Objects.requireNonNull(op);
        Iterator<Double> it = boxed.iterator();
        try {
            if (!it.hasNext()) {
                return OptionalDouble.empty();
            }
            double result = it.next().doubleValue();
            while (it.hasNext()) {
                result = op.applyAsDouble(result, it.next().doubleValue());
            }
            return OptionalDouble.of(result);
        } catch (Throwable e) {
            throw ObjPipeline.fail(boxed, e);
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        R result = supplier.get();
        Iterator<Double> it = boxed.iterator();
        try {
            while (it.hasNext()) {
                accumulator.accept(result, it.next().doubleValue());
            }
            return result;
        } catch (Throwable e) {
            throw ObjPipeline.fail(boxed, e);
        }
    }

    @Override
    public double sum() {
        return summaryStatistics().getSum();
    }

    /// The smallest (`max` false) or largest value, as `Math.min` and
    /// `Math.max` choose, which is what the JDK's reduction uses.
    private OptionalDouble extreme(boolean max) {
        Iterator<Double> it = boxed.iterator();
        try {
            if (!it.hasNext()) {
                return OptionalDouble.empty();
            }
            double result = it.next().doubleValue();
            while (it.hasNext()) {
                double v = it.next().doubleValue();
                result = max ? Math.max(result, v) : Math.min(result, v);
            }
            return OptionalDouble.of(result);
        } catch (Throwable e) {
            throw ObjPipeline.fail(boxed, e);
        }
    }

    @Override
    public OptionalDouble min() {
        return extreme(false);
    }

    @Override
    public OptionalDouble max() {
        return extreme(true);
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
        try {
            while (it.hasNext()) {
                all.accept(it.next().doubleValue());
            }
            return all;
        } catch (Throwable e) {
            throw ObjPipeline.fail(boxed, e);
        }
    }

    @Override
    public boolean anyMatch(DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.anyMatch(Fn.<Double>predicate(predicate));
    }

    @Override
    public boolean allMatch(DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.allMatch(Fn.<Double>predicate(predicate));
    }

    @Override
    public boolean noneMatch(DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        return boxed.noneMatch(Fn.<Double>predicate(predicate));
    }

    @Override
    public OptionalDouble findFirst() {
        // Through the boxed stream's own, which says that the rest is not
        // going to be read: a mapped stream left part read is closed.
        Optional<Double> first = boxed.findFirst();
        return first.isPresent() ? OptionalDouble.of(first.get().doubleValue()) : OptionalDouble.empty();
    }

    @Override
    public OptionalDouble findAny() {
        return findFirst();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Stream<Double> boxed() {
        // A stage of its own, so that this stream counts as used like any
        // other that had an operation applied to it.
        return boxed.map(Fn.<Double, Double>function(Fn.BOX, null));
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
            return it.next().doubleValue();
        }
    }

    @Override
    public boolean isParallel() {
        return boxed.isParallel();
    }

    @Override
    public DoubleStream sequential() {
        boxed.sequential();
        return this;
    }

    @Override
    public DoubleStream parallel() {
        boxed.parallel();
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
