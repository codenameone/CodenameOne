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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/// The common [Collector]s: into collections, maps, strings, groups and
/// numbers.
///
/// The results are the JDK's: `groupingBy` answers a `HashMap` of
/// `ArrayList`s unless told otherwise, `toMap` refuses a second value for a
/// key unless given a function to merge them, and the unmodifiable variants
/// refuse null.
///
/// Every function here is a class of its own, made only by the method that
/// needs it, so an application carries the collectors it uses and no others.
public final class Collectors {

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

    @SuppressWarnings("unchecked")
    private static <T, A, R> Collector<T, A, R> of(Supplier<A> supplier, BiConsumer<A, ? super T> accumulator,
                                                   BinaryOperator<A> combiner, Function<A, R> finisher) {
        return new Of<T, A, R>(supplier, (BiConsumer<A, T>) accumulator, combiner, finisher,
                Collections.<Collector.Characteristics>emptySet());
    }

    @SuppressWarnings("unchecked")
    private static <A, R> Function<A, R> castingIdentity() {
        return (Function<A, R>) Function.identity();
    }

    /// One value being folded, and whether any element reached it yet.
    private static final class Box<T> {
        T value;
        boolean present;

        Box(T value, boolean present) {
            this.value = value;
            this.present = present;
        }
    }

    // ---- into collections ----

    private static <T, C extends Collection<T>> BiConsumer<C, T> adder() {
        return new BiConsumer<C, T>() {
            @Override
            public void accept(C c, T t) {
                c.add(t);
            }
        };
    }

    private static <T, C extends Collection<T>> BinaryOperator<C> addAll() {
        return new BinaryOperator<C>() {
            @Override
            public C apply(C a, C b) {
                a.addAll(b);
                return a;
            }
        };
    }

    private static <T> Supplier<List<T>> newArrayList() {
        return new Supplier<List<T>>() {
            @Override
            public List<T> get() {
                return new ArrayList<T>();
            }
        };
    }

    private static <T> Supplier<Set<T>> newHashSet() {
        return new Supplier<Set<T>>() {
            @Override
            public Set<T> get() {
                return new HashSet<T>();
            }
        };
    }

    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(Supplier<C> collectionFactory) {
        Objects.requireNonNull(collectionFactory);
        return Collectors.<T, C, C>of(collectionFactory, Collectors.<T, C>adder(), Collectors.<T, C>addAll(),
                Collectors.<C, C>castingIdentity());
    }

    public static <T> Collector<T, ?, List<T>> toList() {
        return Collectors.<T, List<T>, List<T>>of(Collectors.<T>newArrayList(), Collectors.<T, List<T>>adder(),
                Collectors.<T, List<T>>addAll(), Collectors.<List<T>, List<T>>castingIdentity());
    }

    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return Collectors.<T, List<T>, List<T>>of(Collectors.<T>newArrayList(), Collectors.<T, List<T>>adder(),
                Collectors.<T, List<T>>addAll(), new Function<List<T>, List<T>>() {
                    @Override
                    public List<T> apply(List<T> c) {
                        return List.copyOf(c);
                    }
                });
    }

    public static <T> Collector<T, ?, Set<T>> toSet() {
        return Collectors.<T, Set<T>, Set<T>>of(Collectors.<T>newHashSet(), Collectors.<T, Set<T>>adder(),
                Collectors.<T, Set<T>>addAll(), Collectors.<Set<T>, Set<T>>castingIdentity());
    }

    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        return Collectors.<T, Set<T>, Set<T>>of(Collectors.<T>newHashSet(), Collectors.<T, Set<T>>adder(),
                Collectors.<T, Set<T>>addAll(), new Function<Set<T>, Set<T>>() {
                    @Override
                    public Set<T> apply(Set<T> c) {
                        return Set.copyOf(c);
                    }
                });
    }

    // ---- into a string ----

    /// What `java.util.StringJoiner` does for [#joining(CharSequence,
    /// CharSequence, CharSequence)].
    private static final class Joiner {
        private final String delimiter;
        private final String prefix;
        private final String suffix;
        private StringBuilder value;

        Joiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
            this.delimiter = delimiter.toString();
            this.prefix = prefix.toString();
            this.suffix = suffix.toString();
        }

        private StringBuilder next() {
            if (value == null) {
                value = new StringBuilder().append(prefix);
            } else {
                value.append(delimiter);
            }
            return value;
        }

        void add(CharSequence s) {
            next().append(String.valueOf(s));
        }

        /// Adds what `other` holds, without its prefix and suffix, as one element.
        void merge(Joiner other) {
            if (other.value != null) {
                next().append(other.value, other.prefix.length(), other.value.length());
            }
        }

        @Override
        public String toString() {
            return value == null ? prefix + suffix : value.toString() + suffix;
        }
    }

    public static Collector<CharSequence, ?, String> joining() {
        return Collectors.<CharSequence, StringBuilder, String>of(new Supplier<StringBuilder>() {
            @Override
            public StringBuilder get() {
                return new StringBuilder();
            }
        }, new BiConsumer<StringBuilder, CharSequence>() {
            @Override
            public void accept(StringBuilder sb, CharSequence s) {
                sb.append(String.valueOf(s));
            }
        }, new BinaryOperator<StringBuilder>() {
            @Override
            public StringBuilder apply(StringBuilder a, StringBuilder b) {
                return a.append(b.toString());
            }
        }, new Function<StringBuilder, String>() {
            @Override
            public String apply(StringBuilder sb) {
                return sb.toString();
            }
        });
    }

    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter) {
        return joining(delimiter, "", "");
    }

    public static Collector<CharSequence, ?, String> joining(final CharSequence delimiter,
                                                             final CharSequence prefix, final CharSequence suffix) {
        if (delimiter == null || prefix == null || suffix == null) {
            throw new NullPointerException();
        }
        return Collectors.<CharSequence, Joiner, String>of(new Supplier<Joiner>() {
            @Override
            public Joiner get() {
                return new Joiner(delimiter, prefix, suffix);
            }
        }, new BiConsumer<Joiner, CharSequence>() {
            @Override
            public void accept(Joiner j, CharSequence s) {
                j.add(s);
            }
        }, new BinaryOperator<Joiner>() {
            @Override
            public Joiner apply(Joiner a, Joiner b) {
                a.merge(b);
                return a;
            }
        }, new Function<Joiner, String>() {
            @Override
            public String apply(Joiner j) {
                return j.toString();
            }
        });
    }

    // ---- adapting another collector ----

    public static <T, U, A, R> Collector<T, ?, R> mapping(final Function<? super T, ? extends U> mapper,
                                                          Collector<? super U, A, R> downstream) {
        Objects.requireNonNull(mapper);
        final BiConsumer<A, ? super U> inner = downstream.accumulator();
        return Collectors.<T, A, R>of(downstream.supplier(), new BiConsumer<A, T>() {
            @Override
            public void accept(A a, T t) {
                inner.accept(a, mapper.apply(t));
            }
        }, downstream.combiner(), downstream.finisher());
    }

    public static <T, A, R> Collector<T, ?, R> filtering(final Predicate<? super T> predicate,
                                                         Collector<? super T, A, R> downstream) {
        Objects.requireNonNull(predicate);
        final BiConsumer<A, ? super T> inner = downstream.accumulator();
        return Collectors.<T, A, R>of(downstream.supplier(), new BiConsumer<A, T>() {
            @Override
            public void accept(A a, T t) {
                if (predicate.test(t)) {
                    inner.accept(a, t);
                }
            }
        }, downstream.combiner(), downstream.finisher());
    }

    public static <T, U, A, R> Collector<T, ?, R> flatMapping(
            final Function<? super T, ? extends Stream<? extends U>> mapper, Collector<? super U, A, R> downstream) {
        Objects.requireNonNull(mapper);
        final BiConsumer<A, ? super U> inner = downstream.accumulator();
        return Collectors.<T, A, R>of(downstream.supplier(), new BiConsumer<A, T>() {
            @Override
            public void accept(A a, T t) {
                Stream<? extends U> mapped = mapper.apply(t);
                if (mapped != null) {
                    try {
                        Iterator<? extends U> it = mapped.iterator();
                        while (it.hasNext()) {
                            inner.accept(a, it.next());
                        }
                    } finally {
                        mapped.close();
                    }
                }
            }
        }, downstream.combiner(), downstream.finisher());
    }

    public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(Collector<T, A, R> downstream,
                                                                      final Function<R, RR> finisher) {
        Objects.requireNonNull(finisher);
        final Function<A, R> first = downstream.finisher();
        return Collectors.<T, A, RR>of(downstream.supplier(), downstream.accumulator(), downstream.combiner(),
                new Function<A, RR>() {
                    @Override
                    public RR apply(A a) {
                        return finisher.apply(first.apply(a));
                    }
                });
    }

    // ---- numbers ----

    /// Folds the `long` values `mapper` answers into a [LongSummaryStatistics];
    /// `mapper` null counts the elements instead.
    private static <T> Collector<T, LongSummaryStatistics, LongSummaryStatistics> longs(
            final ToLongFunction<? super T> mapper) {
        return Collectors.<T, LongSummaryStatistics, LongSummaryStatistics>of(
                new Supplier<LongSummaryStatistics>() {
                    @Override
                    public LongSummaryStatistics get() {
                        return new LongSummaryStatistics();
                    }
                }, new BiConsumer<LongSummaryStatistics, T>() {
                    @Override
                    public void accept(LongSummaryStatistics a, T t) {
                        a.accept(mapper == null ? 1L : mapper.applyAsLong(t));
                    }
                }, new BinaryOperator<LongSummaryStatistics>() {
                    @Override
                    public LongSummaryStatistics apply(LongSummaryStatistics a, LongSummaryStatistics b) {
                        a.combine(b);
                        return a;
                    }
                }, Collectors.<LongSummaryStatistics, LongSummaryStatistics>castingIdentity());
    }

    private static <T> Collector<T, IntSummaryStatistics, IntSummaryStatistics> ints(
            final ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return Collectors.<T, IntSummaryStatistics, IntSummaryStatistics>of(
                new Supplier<IntSummaryStatistics>() {
                    @Override
                    public IntSummaryStatistics get() {
                        return new IntSummaryStatistics();
                    }
                }, new BiConsumer<IntSummaryStatistics, T>() {
                    @Override
                    public void accept(IntSummaryStatistics a, T t) {
                        a.accept(mapper.applyAsInt(t));
                    }
                }, new BinaryOperator<IntSummaryStatistics>() {
                    @Override
                    public IntSummaryStatistics apply(IntSummaryStatistics a, IntSummaryStatistics b) {
                        a.combine(b);
                        return a;
                    }
                }, Collectors.<IntSummaryStatistics, IntSummaryStatistics>castingIdentity());
    }

    private static <T> Collector<T, DoubleSummaryStatistics, DoubleSummaryStatistics> doubles(
            final ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return Collectors.<T, DoubleSummaryStatistics, DoubleSummaryStatistics>of(
                new Supplier<DoubleSummaryStatistics>() {
                    @Override
                    public DoubleSummaryStatistics get() {
                        return new DoubleSummaryStatistics();
                    }
                }, new BiConsumer<DoubleSummaryStatistics, T>() {
                    @Override
                    public void accept(DoubleSummaryStatistics a, T t) {
                        a.accept(mapper.applyAsDouble(t));
                    }
                }, new BinaryOperator<DoubleSummaryStatistics>() {
                    @Override
                    public DoubleSummaryStatistics apply(DoubleSummaryStatistics a, DoubleSummaryStatistics b) {
                        a.combine(b);
                        return a;
                    }
                }, Collectors.<DoubleSummaryStatistics, DoubleSummaryStatistics>castingIdentity());
    }

    /// Statistics a collector folds into, answering one of their figures.
    private static final int SUM = 0;
    private static final int AVERAGE = 1;
    private static final int COUNT = 2;

    private static <S, R> Function<S, R> figure(final int which) {
        return new Function<S, R>() {
            @Override
            @SuppressWarnings("unchecked")
            public R apply(S s) {
                Object out;
                if (s instanceof IntSummaryStatistics) {
                    IntSummaryStatistics i = (IntSummaryStatistics) s;
                    out = which == SUM ? (Object) Integer.valueOf((int) i.getSum()) : Double.valueOf(i.getAverage());
                } else if (s instanceof LongSummaryStatistics) {
                    LongSummaryStatistics l = (LongSummaryStatistics) s;
                    out = which == AVERAGE ? (Object) Double.valueOf(l.getAverage())
                            : Long.valueOf(which == SUM ? l.getSum() : l.getCount());
                } else {
                    DoubleSummaryStatistics d = (DoubleSummaryStatistics) s;
                    out = Double.valueOf(which == SUM ? d.getSum() : d.getAverage());
                }
                return (R) out;
            }
        };
    }

    private static <T, S, R> Collector<T, S, R> finishing(Collector<T, S, S> folded, int which) {
        return Collectors.<T, S, R>of(folded.supplier(), folded.accumulator(), folded.combiner(),
                Collectors.<S, R>figure(which));
    }

    public static <T> Collector<T, ?, Long> counting() {
        return Collectors.<T, LongSummaryStatistics, Long>finishing(Collectors.<T>longs(null), COUNT);
    }

    public static <T> Collector<T, ?, Integer> summingInt(ToIntFunction<? super T> mapper) {
        return Collectors.<T, IntSummaryStatistics, Integer>finishing(ints(mapper), SUM);
    }

    public static <T> Collector<T, ?, Long> summingLong(ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return Collectors.<T, LongSummaryStatistics, Long>finishing(longs(mapper), SUM);
    }

    public static <T> Collector<T, ?, Double> summingDouble(ToDoubleFunction<? super T> mapper) {
        return Collectors.<T, DoubleSummaryStatistics, Double>finishing(doubles(mapper), SUM);
    }

    public static <T> Collector<T, ?, Double> averagingInt(ToIntFunction<? super T> mapper) {
        return Collectors.<T, IntSummaryStatistics, Double>finishing(ints(mapper), AVERAGE);
    }

    public static <T> Collector<T, ?, Double> averagingLong(ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return Collectors.<T, LongSummaryStatistics, Double>finishing(longs(mapper), AVERAGE);
    }

    public static <T> Collector<T, ?, Double> averagingDouble(ToDoubleFunction<? super T> mapper) {
        return Collectors.<T, DoubleSummaryStatistics, Double>finishing(doubles(mapper), AVERAGE);
    }

    public static <T> Collector<T, ?, IntSummaryStatistics> summarizingInt(ToIntFunction<? super T> mapper) {
        return ints(mapper);
    }

    public static <T> Collector<T, ?, LongSummaryStatistics> summarizingLong(ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return longs(mapper);
    }

    public static <T> Collector<T, ?, DoubleSummaryStatistics> summarizingDouble(
            ToDoubleFunction<? super T> mapper) {
        return doubles(mapper);
    }

    // ---- reductions ----

    public static <T> Collector<T, ?, T> reducing(T identity, BinaryOperator<T> op) {
        return Collectors.<T, T>reducing(identity, Function.<T>identity(), op);
    }

    public static <T> Collector<T, ?, Optional<T>> reducing(final BinaryOperator<T> op) {
        Objects.requireNonNull(op);
        return Collectors.<T, Box<T>, Optional<T>>of(new Supplier<Box<T>>() {
            @Override
            public Box<T> get() {
                return new Box<T>(null, false);
            }
        }, new BiConsumer<Box<T>, T>() {
            @Override
            public void accept(Box<T> a, T t) {
                a.value = a.present ? op.apply(a.value, t) : t;
                a.present = true;
            }
        }, new BinaryOperator<Box<T>>() {
            @Override
            public Box<T> apply(Box<T> a, Box<T> b) {
                if (b.present) {
                    a.value = a.present ? op.apply(a.value, b.value) : b.value;
                    a.present = true;
                }
                return a;
            }
        }, new Function<Box<T>, Optional<T>>() {
            @Override
            public Optional<T> apply(Box<T> a) {
                return Optional.ofNullable(a.value);
            }
        });
    }

    public static <T, U> Collector<T, ?, U> reducing(final U identity, final Function<? super T, ? extends U> mapper,
                                                     final BinaryOperator<U> op) {
        Objects.requireNonNull(mapper);
        Objects.requireNonNull(op);
        return Collectors.<T, Box<U>, U>of(new Supplier<Box<U>>() {
            @Override
            public Box<U> get() {
                return new Box<U>(identity, true);
            }
        }, new BiConsumer<Box<U>, T>() {
            @Override
            public void accept(Box<U> a, T t) {
                a.value = op.apply(a.value, mapper.apply(t));
            }
        }, new BinaryOperator<Box<U>>() {
            @Override
            public Box<U> apply(Box<U> a, Box<U> b) {
                a.value = op.apply(a.value, b.value);
                return a;
            }
        }, new Function<Box<U>, U>() {
            @Override
            public U apply(Box<U> a) {
                return a.value;
            }
        });
    }

    public static <T> Collector<T, ?, Optional<T>> minBy(Comparator<? super T> comparator) {
        return reducing(BinaryOperator.<T>minBy(comparator));
    }

    public static <T> Collector<T, ?, Optional<T>> maxBy(Comparator<? super T> comparator) {
        return reducing(BinaryOperator.<T>maxBy(comparator));
    }

    // ---- groups ----

    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(Function<? super T, ? extends K> classifier) {
        return groupingBy(classifier, Collectors.<T>toList());
    }

    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(Function<? super T, ? extends K> classifier,
                                                                     Collector<? super T, A, D> downstream) {
        return Collectors.<T, K, D, A, Map<K, D>>groupingBy(classifier, new Supplier<Map<K, D>>() {
            @Override
            public Map<K, D> get() {
                return new HashMap<K, D>();
            }
        }, downstream);
    }

    @SuppressWarnings("unchecked")
    public static <T, K, D, A, M extends Map<K, D>> Collector<T, ?, M> groupingBy(
            final Function<? super T, ? extends K> classifier, final Supplier<M> mapFactory,
            Collector<? super T, A, D> downstream) {
        Objects.requireNonNull(classifier);
        Objects.requireNonNull(mapFactory);
        final Supplier<A> innerSupplier = downstream.supplier();
        final BiConsumer<A, ? super T> innerAccumulator = downstream.accumulator();
        final BinaryOperator<A> innerCombiner = downstream.combiner();
        final Function<A, D> innerFinisher = downstream.finisher();
        // While elements arrive the map holds each group's container, and
        // the finisher puts the group's result in its place: one map, of
        // the kind the caller asked for.
        return Collectors.<T, Map<K, A>, M>of(new Supplier<Map<K, A>>() {
            @Override
            public Map<K, A> get() {
                return (Map<K, A>) mapFactory.get();
            }
        }, new BiConsumer<Map<K, A>, T>() {
            @Override
            public void accept(Map<K, A> m, T t) {
                K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
                A container = m.get(key);
                if (container == null) {
                    container = innerSupplier.get();
                    m.put(key, container);
                }
                innerAccumulator.accept(container, t);
            }
        }, new BinaryOperator<Map<K, A>>() {
            @Override
            public Map<K, A> apply(Map<K, A> a, Map<K, A> b) {
                for (Map.Entry<K, A> e : b.entrySet()) {
                    A mine = a.get(e.getKey());
                    a.put(e.getKey(), mine == null ? e.getValue() : innerCombiner.apply(mine, e.getValue()));
                }
                return a;
            }
        }, new Function<Map<K, A>, M>() {
            @Override
            public M apply(Map<K, A> m) {
                Map<K, Object> out = (Map<K, Object>) m;
                for (K key : new ArrayList<K>(m.keySet())) {
                    out.put(key, innerFinisher.apply(m.get(key)));
                }
                return (M) out;
            }
        });
    }

    public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningBy(Predicate<? super T> predicate) {
        return partitioningBy(predicate, Collectors.<T>toList());
    }

    @SuppressWarnings("unchecked")
    public static <T, D, A> Collector<T, ?, Map<Boolean, D>> partitioningBy(final Predicate<? super T> predicate,
                                                                            Collector<? super T, A, D> downstream) {
        Objects.requireNonNull(predicate);
        final Supplier<A> innerSupplier = downstream.supplier();
        final BiConsumer<A, ? super T> innerAccumulator = downstream.accumulator();
        final BinaryOperator<A> innerCombiner = downstream.combiner();
        final Function<A, D> innerFinisher = downstream.finisher();
        return Collectors.<T, Object[], Map<Boolean, D>>of(new Supplier<Object[]>() {
            @Override
            public Object[] get() {
                return new Object[] {innerSupplier.get(), innerSupplier.get()};
            }
        }, new BiConsumer<Object[], T>() {
            @Override
            public void accept(Object[] a, T t) {
                innerAccumulator.accept((A) a[predicate.test(t) ? 1 : 0], t);
            }
        }, new BinaryOperator<Object[]>() {
            @Override
            public Object[] apply(Object[] a, Object[] b) {
                a[0] = innerCombiner.apply((A) a[0], (A) b[0]);
                a[1] = innerCombiner.apply((A) a[1], (A) b[1]);
                return a;
            }
        }, new Function<Object[], Map<Boolean, D>>() {
            @Override
            public Map<Boolean, D> apply(Object[] a) {
                // Both keys are always there, false first.
                Map<Boolean, D> out = new LinkedHashMap<Boolean, D>();
                out.put(Boolean.FALSE, innerFinisher.apply((A) a[0]));
                out.put(Boolean.TRUE, innerFinisher.apply((A) a[1]));
                return out;
            }
        });
    }

    // ---- maps ----

    /// Puts `value` under `key`; a second value for a key is an error when
    /// `mergeFunction` is null and merged with the first otherwise, a merged
    /// null removing the key.
    static <K, U> void put(Map<K, U> map, K key, U value, BinaryOperator<U> mergeFunction) {
        Objects.requireNonNull(value);
        U old = map.get(key);
        if (old == null) {
            map.put(key, value);
            return;
        }
        if (mergeFunction == null) {
            throw new IllegalStateException("Duplicate key " + key + " (attempted merging values " + old + " and "
                    + value + ")");
        }
        U merged = mergeFunction.apply(old, value);
        if (merged == null) {
            map.remove(key);
        } else {
            map.put(key, merged);
        }
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper,
                                                             Function<? super T, ? extends U> valueMapper) {
        return Collectors.<T, K, U, Map<K, U>>toMap(keyMapper, valueMapper, null, Collectors.<K, U>newHashMap());
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper,
                                                             Function<? super T, ? extends U> valueMapper,
                                                             BinaryOperator<U> mergeFunction) {
        Objects.requireNonNull(mergeFunction);
        return Collectors.<T, K, U, Map<K, U>>toMap(keyMapper, valueMapper, mergeFunction,
                Collectors.<K, U>newHashMap());
    }

    private static <K, U> Supplier<Map<K, U>> newHashMap() {
        return new Supplier<Map<K, U>>() {
            @Override
            public Map<K, U> get() {
                return new HashMap<K, U>();
            }
        };
    }

    public static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(
            final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends U> valueMapper,
            final BinaryOperator<U> mergeFunction, Supplier<M> mapFactory) {
        Objects.requireNonNull(keyMapper);
        Objects.requireNonNull(valueMapper);
        Objects.requireNonNull(mapFactory);
        return Collectors.<T, M, M>of(mapFactory, new BiConsumer<M, T>() {
            @Override
            public void accept(M m, T t) {
                put(m, keyMapper.apply(t), valueMapper.apply(t), mergeFunction);
            }
        }, new BinaryOperator<M>() {
            @Override
            public M apply(M a, M b) {
                for (Map.Entry<K, U> e : b.entrySet()) {
                    put(a, e.getKey(), e.getValue(), mergeFunction);
                }
                return a;
            }
        }, Collectors.<M, M>castingIdentity());
    }

    private static <K, U> Function<Map<K, U>, Map<K, U>> unmodifiableMap() {
        return new Function<Map<K, U>, Map<K, U>>() {
            @Override
            public Map<K, U> apply(Map<K, U> m) {
                return Map.copyOf(m);
            }
        };
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableMap(
            Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        return collectingAndThen(Collectors.<T, K, U>toMap(keyMapper, valueMapper),
                Collectors.<K, U>unmodifiableMap());
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableMap(
            Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper,
            BinaryOperator<U> mergeFunction) {
        return collectingAndThen(Collectors.<T, K, U>toMap(keyMapper, valueMapper, mergeFunction),
                Collectors.<K, U>unmodifiableMap());
    }
}
