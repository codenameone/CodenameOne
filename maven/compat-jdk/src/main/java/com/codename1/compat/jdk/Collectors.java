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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/// `java.util.stream.Collectors` for the Codename One runtime, whose own
/// class of that name has two methods that answer null.
///
/// The results are the JDK's: `groupingBy` answers a `HashMap` of
/// `ArrayList`s unless told otherwise, `toMap` refuses a second value for a
/// key unless given a function to merge them, and the unmodifiable variants
/// refuse null.
public final class Collectors {

    private static final Set<Collector.Characteristics> NONE = Collections.<Collector.Characteristics>emptySet();

    private Collectors() {
    }

    /// A collector made of its four functions.
    static final class Of<T, A, R> implements Collector<T, A, R> {

        private final Supplier<A> supplier;
        private final BiConsumer<A, T> accumulator;
        private final BinaryOperator<A> combiner;
        private final Function<A, R> finisher;
        private final Set<Characteristics> characteristics;

        Of(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner, Function<A, R> finisher,
           Set<Characteristics> characteristics) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }

        @Override
        public Supplier<A> supplier() {
            return supplier;
        }

        @Override
        public BiConsumer<A, T> accumulator() {
            return accumulator;
        }

        @Override
        public BinaryOperator<A> combiner() {
            return combiner;
        }

        @Override
        public Function<A, R> finisher() {
            return finisher;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }
    }

    private static <T, A, R> Collector<T, A, R> of(Supplier<A> supplier, BiConsumer<A, T> accumulator,
                                                   BinaryOperator<A> combiner, Function<A, R> finisher) {
        return new Of<T, A, R>(supplier, accumulator, combiner, finisher, NONE);
    }

    /// One value being folded, and whether any element reached it yet.
    private static final class Box<T> {
        private T value;
        private boolean present;

        Box(T value, boolean present) {
            this.value = value;
            this.present = present;
        }
    }

    /// A sum of doubles and how many there were.
    private static final class Average {
        private final DoubleSum sum = new DoubleSum();
        private long count;
    }

    // ---- into collections ----

    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(Supplier<C> collectionFactory) {
        Objects.requireNonNull(collectionFactory);
        return Collectors.<T, C, C>of(collectionFactory, (c, t) -> c.add(t), (a, b) -> {
            a.addAll(b);
            return a;
        }, c -> c);
    }

    public static <T> Collector<T, ?, List<T>> toList() {
        return Collectors.<T, List<T>, List<T>>of(() -> new ArrayList<T>(), (c, t) -> c.add(t), (a, b) -> {
            a.addAll(b);
            return a;
        }, c -> c);
    }

    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return Collectors.<T, List<T>, List<T>>of(() -> new ArrayList<T>(), (c, t) -> c.add(t), (a, b) -> {
            a.addAll(b);
            return a;
        }, c -> JdkCollections.<T>newList(c.toArray()));
    }

    public static <T> Collector<T, ?, Set<T>> toSet() {
        return Collectors.<T, Set<T>, Set<T>>of(() -> new HashSet<T>(), (c, t) -> c.add(t), (a, b) -> {
            a.addAll(b);
            return a;
        }, c -> c);
    }

    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        return Collectors.<T, Set<T>, Set<T>>of(() -> new HashSet<T>(), (c, t) -> c.add(t), (a, b) -> {
            a.addAll(b);
            return a;
        }, c -> JdkCollections.<T>newSet(c.toArray(), false));
    }

    // ---- into a string ----

    public static Collector<CharSequence, ?, String> joining() {
        return Collectors.<CharSequence, StringBuilder, String>of(() -> new StringBuilder(),
                (sb, s) -> sb.append(String.valueOf(s)), (a, b) -> a.append(b.toString()), sb -> sb.toString());
    }

    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter) {
        return joining(delimiter, "", "");
    }

    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter, CharSequence prefix,
                                                             CharSequence suffix) {
        if (delimiter == null || prefix == null || suffix == null) {
            throw new NullPointerException();
        }
        return Collectors.<CharSequence, StringJoiner, String>of(() -> new StringJoiner(delimiter, prefix, suffix),
                (j, s) -> j.add(s), (a, b) -> a.merge(b), j -> j.toString());
    }

    // ---- adapting another collector ----

    public static <T, U, A, R> Collector<T, ?, R> mapping(Function<? super T, ? extends U> mapper,
                                                          Collector<? super U, A, R> downstream) {
        Objects.requireNonNull(mapper);
        BiConsumer<A, ? super U> inner = downstream.accumulator();
        return Collectors.<T, A, R>of(downstream.supplier(), (a, t) -> inner.accept(a, mapper.apply(t)),
                downstream.combiner(), downstream.finisher());
    }

    public static <T, A, R> Collector<T, ?, R> filtering(Predicate<? super T> predicate,
                                                         Collector<? super T, A, R> downstream) {
        Objects.requireNonNull(predicate);
        BiConsumer<A, ? super T> inner = downstream.accumulator();
        return Collectors.<T, A, R>of(downstream.supplier(), (a, t) -> {
            if (predicate.test(t)) {
                inner.accept(a, t);
            }
        }, downstream.combiner(), downstream.finisher());
    }

    public static <T, U, A, R> Collector<T, ?, R> flatMapping(
            Function<? super T, ? extends Stream<? extends U>> mapper, Collector<? super U, A, R> downstream) {
        Objects.requireNonNull(mapper);
        BiConsumer<A, ? super U> inner = downstream.accumulator();
        return Collectors.<T, A, R>of(downstream.supplier(), (a, t) -> {
            Stream<? extends U> mapped = mapper.apply(t);
            if (mapped != null) {
                try {
                    mapped.forEach(u -> inner.accept(a, u));
                } finally {
                    mapped.close();
                }
            }
        }, downstream.combiner(), downstream.finisher());
    }

    public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(Collector<T, A, R> downstream,
                                                                      Function<R, RR> finisher) {
        Objects.requireNonNull(finisher);
        Function<A, R> first = downstream.finisher();
        return Collectors.<T, A, RR>of(downstream.supplier(), downstream.accumulator(), downstream.combiner(),
                a -> finisher.apply(first.apply(a)));
    }

    // ---- numbers ----

    public static <T> Collector<T, ?, Long> counting() {
        return Collectors.<T, long[], Long>of(() -> new long[1], (a, t) -> a[0]++, (a, b) -> {
            a[0] += b[0];
            return a;
        }, a -> Long.valueOf(a[0]));
    }

    public static <T> Collector<T, ?, Integer> summingInt(ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return Collectors.<T, int[], Integer>of(() -> new int[1], (a, t) -> a[0] += mapper.applyAsInt(t),
                (a, b) -> {
                    a[0] += b[0];
                    return a;
                }, a -> Integer.valueOf(a[0]));
    }

    public static <T> Collector<T, ?, Long> summingLong(ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return Collectors.<T, long[], Long>of(() -> new long[1], (a, t) -> a[0] += mapper.applyAsLong(t),
                (a, b) -> {
                    a[0] += b[0];
                    return a;
                }, a -> Long.valueOf(a[0]));
    }

    public static <T> Collector<T, ?, Double> summingDouble(ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return Collectors.<T, DoubleSum, Double>of(() -> new DoubleSum(), (a, t) -> a.add(mapper.applyAsDouble(t)),
                (a, b) -> {
                    a.add(b);
                    return a;
                }, a -> Double.valueOf(a.value()));
    }

    public static <T> Collector<T, ?, Double> averagingInt(ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return averagingLong(t -> mapper.applyAsInt(t));
    }

    public static <T> Collector<T, ?, Double> averagingLong(ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return Collectors.<T, long[], Double>of(() -> new long[2], (a, t) -> {
            a[0] += mapper.applyAsLong(t);
            a[1]++;
        }, (a, b) -> {
            a[0] += b[0];
            a[1] += b[1];
            return a;
        }, a -> Double.valueOf(a[1] == 0 ? 0.0d : (double) a[0] / a[1]));
    }

    public static <T> Collector<T, ?, Double> averagingDouble(ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return Collectors.<T, Average, Double>of(() -> new Average(), (a, t) -> {
            a.sum.add(mapper.applyAsDouble(t));
            a.count++;
        }, (a, b) -> {
            a.sum.add(b.sum);
            a.count += b.count;
            return a;
        }, a -> Double.valueOf(a.count == 0 ? 0.0d : a.sum.value() / a.count));
    }

    public static <T> Collector<T, ?, IntSummaryStatistics> summarizingInt(ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return Collectors.<T, IntSummaryStatistics, IntSummaryStatistics>of(() -> new IntSummaryStatistics(),
                (a, t) -> a.accept(mapper.applyAsInt(t)), (a, b) -> {
                    a.combine(b);
                    return a;
                }, a -> a);
    }

    public static <T> Collector<T, ?, LongSummaryStatistics> summarizingLong(ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return Collectors.<T, LongSummaryStatistics, LongSummaryStatistics>of(() -> new LongSummaryStatistics(),
                (a, t) -> a.accept(mapper.applyAsLong(t)), (a, b) -> {
                    a.combine(b);
                    return a;
                }, a -> a);
    }

    public static <T> Collector<T, ?, DoubleSummaryStatistics> summarizingDouble(
            ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return Collectors.<T, DoubleSummaryStatistics, DoubleSummaryStatistics>of(
                () -> new DoubleSummaryStatistics(), (a, t) -> a.accept(mapper.applyAsDouble(t)), (a, b) -> {
                    a.combine(b);
                    return a;
                }, a -> a);
    }

    // ---- reductions ----

    public static <T> Collector<T, ?, T> reducing(T identity, BinaryOperator<T> op) {
        Objects.requireNonNull(op);
        return Collectors.<T, Box<T>, T>of(() -> new Box<T>(identity, true), (a, t) -> a.value = op.apply(a.value, t),
                (a, b) -> {
                    a.value = op.apply(a.value, b.value);
                    return a;
                }, a -> a.value);
    }

    public static <T> Collector<T, ?, Optional<T>> reducing(BinaryOperator<T> op) {
        Objects.requireNonNull(op);
        return Collectors.<T, Box<T>, Optional<T>>of(() -> new Box<T>(null, false), (a, t) -> {
            a.value = a.present ? op.apply(a.value, t) : t;
            a.present = true;
        }, (a, b) -> {
            if (b.present) {
                a.value = a.present ? op.apply(a.value, b.value) : b.value;
                a.present = true;
            }
            return a;
        }, a -> Optional.ofNullable(a.value));
    }

    public static <T, U> Collector<T, ?, U> reducing(U identity, Function<? super T, ? extends U> mapper,
                                                     BinaryOperator<U> op) {
        Objects.requireNonNull(mapper);
        Objects.requireNonNull(op);
        return Collectors.<T, Box<U>, U>of(() -> new Box<U>(identity, true),
                (a, t) -> a.value = op.apply(a.value, mapper.apply(t)), (a, b) -> {
                    a.value = op.apply(a.value, b.value);
                    return a;
                }, a -> a.value);
    }

    public static <T> Collector<T, ?, Optional<T>> minBy(Comparator<? super T> comparator) {
        return reducing(JdkFunctions.<T>minBy(comparator));
    }

    public static <T> Collector<T, ?, Optional<T>> maxBy(Comparator<? super T> comparator) {
        return reducing(JdkFunctions.<T>maxBy(comparator));
    }

    // ---- groups ----

    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(Function<? super T, ? extends K> classifier) {
        return groupingBy(classifier, Collectors.<T>toList());
    }

    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(Function<? super T, ? extends K> classifier,
                                                                     Collector<? super T, A, D> downstream) {
        return Collectors.<T, K, D, A, Map<K, D>>groupingBy(classifier, () -> new HashMap<K, D>(), downstream);
    }

    @SuppressWarnings("unchecked")
    public static <T, K, D, A, M extends Map<K, D>> Collector<T, ?, M> groupingBy(
            Function<? super T, ? extends K> classifier, Supplier<M> mapFactory,
            Collector<? super T, A, D> downstream) {
        Objects.requireNonNull(classifier);
        Objects.requireNonNull(mapFactory);
        Supplier<A> innerSupplier = downstream.supplier();
        BiConsumer<A, ? super T> innerAccumulator = downstream.accumulator();
        BinaryOperator<A> innerCombiner = downstream.combiner();
        Function<A, D> innerFinisher = downstream.finisher();
        // While elements arrive the map holds each group's container, and
        // the finisher puts the group's result in its place: one map, of
        // the kind the caller asked for.
        return Collectors.<T, Map<K, A>, M>of(() -> (Map<K, A>) mapFactory.get(), (m, t) -> {
            K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
            A container = m.get(key);
            if (container == null) {
                container = innerSupplier.get();
                m.put(key, container);
            }
            innerAccumulator.accept(container, t);
        }, (a, b) -> {
            for (Map.Entry<K, A> e : b.entrySet()) {
                A mine = a.get(e.getKey());
                a.put(e.getKey(), mine == null ? e.getValue() : innerCombiner.apply(mine, e.getValue()));
            }
            return a;
        }, m -> {
            Map<K, Object> out = (Map<K, Object>) m;
            for (K key : new ArrayList<K>(m.keySet())) {
                out.put(key, innerFinisher.apply(m.get(key)));
            }
            return (M) out;
        });
    }

    public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningBy(Predicate<? super T> predicate) {
        return partitioningBy(predicate, Collectors.<T>toList());
    }

    @SuppressWarnings("unchecked")
    public static <T, D, A> Collector<T, ?, Map<Boolean, D>> partitioningBy(Predicate<? super T> predicate,
                                                                            Collector<? super T, A, D> downstream) {
        Objects.requireNonNull(predicate);
        Supplier<A> innerSupplier = downstream.supplier();
        BiConsumer<A, ? super T> innerAccumulator = downstream.accumulator();
        BinaryOperator<A> innerCombiner = downstream.combiner();
        Function<A, D> innerFinisher = downstream.finisher();
        return Collectors.<T, Object[], Map<Boolean, D>>of(
                () -> new Object[] {innerSupplier.get(), innerSupplier.get()},
                (a, t) -> innerAccumulator.accept((A) a[predicate.test(t) ? 1 : 0], t), (a, b) -> {
                    a[0] = innerCombiner.apply((A) a[0], (A) b[0]);
                    a[1] = innerCombiner.apply((A) a[1], (A) b[1]);
                    return a;
                }, a -> {
                    // Both keys are always there, false first.
                    Map<Boolean, D> out = new LinkedHashMap<Boolean, D>();
                    out.put(Boolean.FALSE, innerFinisher.apply((A) a[0]));
                    out.put(Boolean.TRUE, innerFinisher.apply((A) a[1]));
                    return out;
                });
    }

    // ---- maps ----

    private static <K, U> void putUnique(Map<K, U> map, K key, U value) {
        Objects.requireNonNull(value);
        U old = map.get(key);
        if (old != null) {
            throw new IllegalStateException("Duplicate key " + key + " (attempted merging values " + old + " and "
                    + value + ")");
        }
        map.put(key, value);
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper,
                                                             Function<? super T, ? extends U> valueMapper) {
        Objects.requireNonNull(keyMapper);
        Objects.requireNonNull(valueMapper);
        return Collectors.<T, Map<K, U>, Map<K, U>>of(() -> new HashMap<K, U>(),
                (m, t) -> putUnique(m, keyMapper.apply(t), valueMapper.apply(t)), (a, b) -> {
                    for (Map.Entry<K, U> e : b.entrySet()) {
                        putUnique(a, e.getKey(), e.getValue());
                    }
                    return a;
                }, m -> m);
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper,
                                                             Function<? super T, ? extends U> valueMapper,
                                                             BinaryOperator<U> mergeFunction) {
        return Collectors.<T, K, U, Map<K, U>>toMap(keyMapper, valueMapper, mergeFunction, () -> new HashMap<K, U>());
    }

    private static <K, U> void putMerged(Map<K, U> map, K key, U value, BinaryOperator<U> mergeFunction) {
        Objects.requireNonNull(value);
        U old = map.get(key);
        if (old == null) {
            map.put(key, value);
            return;
        }
        U merged = mergeFunction.apply(old, value);
        if (merged == null) {
            map.remove(key);
        } else {
            map.put(key, merged);
        }
    }

    public static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(
            Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper,
            BinaryOperator<U> mergeFunction, Supplier<M> mapFactory) {
        Objects.requireNonNull(keyMapper);
        Objects.requireNonNull(valueMapper);
        Objects.requireNonNull(mergeFunction);
        Objects.requireNonNull(mapFactory);
        return Collectors.<T, M, M>of(mapFactory,
                (m, t) -> putMerged(m, keyMapper.apply(t), valueMapper.apply(t), mergeFunction), (a, b) -> {
                    for (Map.Entry<K, U> e : b.entrySet()) {
                        putMerged(a, e.getKey(), e.getValue(), mergeFunction);
                    }
                    return a;
                }, m -> m);
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableMap(
            Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        return collectingAndThen(Collectors.<T, K, U>toMap(keyMapper, valueMapper),
                m -> JdkCollections.<K, U>mapCopyOf(m));
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableMap(
            Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper,
            BinaryOperator<U> mergeFunction) {
        return collectingAndThen(Collectors.<T, K, U>toMap(keyMapper, valueMapper, mergeFunction),
                m -> JdkCollections.<K, U>mapCopyOf(m));
    }
}
