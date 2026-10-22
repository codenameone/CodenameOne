/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.util;

/// A `Comparator` is used to compare two objects to determine their ordering with
/// respect to each other. On a given `Collection`, a `Comparator` can be used to
/// obtain a sorted `Collection` which is *totally ordered*. For a `Comparator`
/// to be *consistent with equals*, its {code #compare(Object, Object)}
/// method has to return zero for each pair of elements (a,b) where a.equals(b)
/// holds true. It is recommended that a `Comparator` implements
/// `java.io.Serializable`.
///
/// #### Since
///
/// 1.2
public interface Comparator<T> {
    /// Compares the two specified objects to determine their relative ordering. The ordering
    /// implied by the return value of this method for all possible pairs of
    /// `(object1, object2)` should form an *equivalence relation*.
    /// This means that
    ///
    /// - `compare(a,a)` returns zero for all `a`
    ///
    /// - the sign of `compare(a,b)` must be the opposite of the sign of `compare(b,a)` for all pairs of (a,b)
    ///
    /// - From `compare(a,b) > 0` and `compare(b,c) > 0` it must
    /// follow `compare(a,c) > 0` for all possible combinations of `(a,b,c)`
    ///
    /// #### Parameters
    ///
    /// - `object1`: an `Object`.
    ///
    /// - `object2`: a second `Object` to compare with `object1`.
    ///
    /// #### Returns
    ///
    /// an integer  0 if `object1` is greater than `object2`.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if objects are not of the correct type.
    public int compare(T object1, T object2);

    /// Compares this `Comparator` with the specified `Object` and indicates whether they
    /// are equal. In order to be equal, `object` must represent the same object
    /// as this instance using a class-specific comparison.
    ///
    /// A `Comparator` never needs to override this method, but may choose so for
    /// performance reasons.
    ///
    /// #### Parameters
    ///
    /// - `object`: the `Object` to compare with this comparator.
    ///
    /// #### Returns
    ///
    /// @return boolean `true` if specified `Object` is the same as this
    /// `Object`, and `false` otherwise.
    ///
    /// #### See also
    ///
    /// - Object#hashCode
    ///
    /// - Object#equals
    public boolean equals(Object object);

    /// This ordering, reversed.
    ///
    /// @return the reversed comparator
    default Comparator<T> reversed() {
        final Comparator<T> self = this;
        return new Comparator<T>() {
            public int compare(T a, T b) {
                return self.compare(b, a);
            }
        };
    }

    /// This ordering, ties broken by `other`.
    ///
    /// @param other the tie breaker
    /// @return the combined comparator
    default Comparator<T> thenComparing(final Comparator<? super T> other) {
        // Arguments are checked here, not when the comparator is first used, as on the JDK.
        if (other == null) {
            throw new NullPointerException();
        }
        final Comparator<T> self = this;
        return new Comparator<T>() {
            public int compare(T a, T b) {
                int r = self.compare(a, b);
                return r != 0 ? r : other.compare(a, b);
            }
        };
    }

    /// This ordering, ties broken by the natural order of a key.
    ///
    /// @param keyExtractor extracts the key
    /// @param <U> the key type
    /// @return the combined comparator
    default <U extends Comparable<? super U>> Comparator<T> thenComparing(
            java.util.function.Function<? super T, ? extends U> keyExtractor) {
        return thenComparing(Comparator.<T, U>comparing(keyExtractor));
    }

    /// Compares by the natural order of a key.
    ///
    /// @param keyExtractor extracts the key
    /// @param <T> the compared type
    /// @param <U> the key type
    /// @return the comparator
    static <T, U extends Comparable<? super U>> Comparator<T> comparing(
            final java.util.function.Function<? super T, ? extends U> keyExtractor) {
        if (keyExtractor == null) {
            throw new NullPointerException();
        }
        return new Comparator<T>() {
            public int compare(T a, T b) {
                return keyExtractor.apply(a).compareTo(keyExtractor.apply(b));
            }
        };
    }

    /// Compares by a key, using the given comparator for the keys.
    ///
    /// @param keyExtractor extracts the key
    /// @param keyComparator orders the keys
    /// @param <T> the compared type
    /// @param <U> the key type
    /// @return the comparator
    static <T, U> Comparator<T> comparing(final java.util.function.Function<? super T, ? extends U> keyExtractor,
            final Comparator<? super U> keyComparator) {
        if (keyExtractor == null || keyComparator == null) {
            throw new NullPointerException();
        }
        return new Comparator<T>() {
            public int compare(T a, T b) {
                return keyComparator.compare(keyExtractor.apply(a), keyExtractor.apply(b));
            }
        };
    }

    /// The natural order of comparable values.
    ///
    /// @param <T> the compared type
    /// @return the comparator
    @SuppressWarnings("unchecked")
    static <T extends Comparable<? super T>> Comparator<T> naturalOrder() {
        return (Comparator<T>) NaturalOrder.INSTANCE;
    }

    /// The reverse of the natural order.
    ///
    /// @param <T> the compared type
    /// @return the comparator
    @SuppressWarnings("unchecked")
    static <T extends Comparable<? super T>> Comparator<T> reverseOrder() {
        // Not Collections.reverseOrder(): naming Collections here makes the translator
        // load it, and with it classes (Collections.SetFromMap) whose presence the native
        // collection traversal keys on, in every application.
        return (Comparator<T>) NaturalOrder.INSTANCE.reversed();
    }

    /// This ordering, ties broken by a key in the order `keyComparator` gives.
    ///
    /// @param keyExtractor extracts the key
    /// @param keyComparator orders the keys
    /// @param <U> the key type
    /// @return the combined comparator
    default <U> Comparator<T> thenComparing(java.util.function.Function<? super T, ? extends U> keyExtractor,
            Comparator<? super U> keyComparator) {
        return thenComparing(Comparator.<T, U>comparing(keyExtractor, keyComparator));
    }

    /// This ordering, ties broken by an `int` key.
    ///
    /// @param keyExtractor extracts the key
    /// @return the combined comparator
    default Comparator<T> thenComparingInt(java.util.function.ToIntFunction<? super T> keyExtractor) {
        return thenComparing(Comparator.<T>comparingInt(keyExtractor));
    }

    /// This ordering, ties broken by a `long` key.
    ///
    /// @param keyExtractor extracts the key
    /// @return the combined comparator
    default Comparator<T> thenComparingLong(java.util.function.ToLongFunction<? super T> keyExtractor) {
        return thenComparing(Comparator.<T>comparingLong(keyExtractor));
    }

    /// This ordering, ties broken by a `double` key, compared as [Double#compare] does.
    ///
    /// @param keyExtractor extracts the key
    /// @return the combined comparator
    default Comparator<T> thenComparingDouble(java.util.function.ToDoubleFunction<? super T> keyExtractor) {
        return thenComparing(Comparator.<T>comparingDouble(keyExtractor));
    }

    /// Compares by an `int` key.
    ///
    /// @param keyExtractor extracts the key
    /// @param <T> the compared type
    /// @return the comparator
    static <T> Comparator<T> comparingInt(final java.util.function.ToIntFunction<? super T> keyExtractor) {
        if (keyExtractor == null) {
            throw new NullPointerException();
        }
        return new Comparator<T>() {
            public int compare(T a, T b) {
                int x = keyExtractor.applyAsInt(a);
                int y = keyExtractor.applyAsInt(b);
                return x < y ? -1 : x == y ? 0 : 1;
            }
        };
    }

    /// Compares by a `long` key.
    ///
    /// @param keyExtractor extracts the key
    /// @param <T> the compared type
    /// @return the comparator
    static <T> Comparator<T> comparingLong(final java.util.function.ToLongFunction<? super T> keyExtractor) {
        if (keyExtractor == null) {
            throw new NullPointerException();
        }
        return new Comparator<T>() {
            public int compare(T a, T b) {
                long x = keyExtractor.applyAsLong(a);
                long y = keyExtractor.applyAsLong(b);
                return x < y ? -1 : x == y ? 0 : 1;
            }
        };
    }

    /// Compares by a `double` key, as [Double#compare] does: NaN above
    /// everything and `-0.0` below `0.0`.
    ///
    /// @param keyExtractor extracts the key
    /// @param <T> the compared type
    /// @return the comparator
    static <T> Comparator<T> comparingDouble(final java.util.function.ToDoubleFunction<? super T> keyExtractor) {
        if (keyExtractor == null) {
            throw new NullPointerException();
        }
        return new Comparator<T>() {
            public int compare(T a, T b) {
                return Double.compare(keyExtractor.applyAsDouble(a), keyExtractor.applyAsDouble(b));
            }
        };
    }

    /// Orders null before every other value, and the rest by `comparator`;
    /// with a null `comparator` the others are all equal.
    ///
    /// @param comparator orders the values that are not null, or null
    /// @param <T> the compared type
    /// @return the comparator
    static <T> Comparator<T> nullsFirst(Comparator<? super T> comparator) {
        return new NullOrder<T>(true, comparator);
    }

    /// Orders null after every other value, and the rest by `comparator`;
    /// with a null `comparator` the others are all equal.
    ///
    /// @param comparator orders the values that are not null, or null
    /// @param <T> the compared type
    /// @return the comparator
    static <T> Comparator<T> nullsLast(Comparator<? super T> comparator) {
        return new NullOrder<T>(false, comparator);
    }
}

/// The order [Comparator#nullsFirst] and [Comparator#nullsLast] hand out. Its
/// [#reversed] keeps null on the same side and reverses the rest, as the JDK's does.
final class NullOrder<T> implements Comparator<T> {
    private final boolean nullFirst;
    private final Comparator<T> real;

    @SuppressWarnings("unchecked")
    NullOrder(boolean nullFirst, Comparator<? super T> real) {
        this.nullFirst = nullFirst;
        this.real = (Comparator<T>) real;
    }

    public int compare(T a, T b) {
        if (a == null) {
            return b == null ? 0 : nullFirst ? -1 : 1;
        }
        if (b == null) {
            return nullFirst ? 1 : -1;
        }
        return real == null ? 0 : real.compare(a, b);
    }

    public Comparator<T> thenComparing(Comparator<? super T> other) {
        if (other == null) {
            throw new NullPointerException();
        }
        return new NullOrder<T>(nullFirst, real == null ? other : real.thenComparing(other));
    }

    public Comparator<T> reversed() {
        return new NullOrder<T>(!nullFirst, real == null ? null : real.reversed());
    }
}

/// The natural ordering [Comparator#naturalOrder] hands out.
final class NaturalOrder implements Comparator<Comparable<Object>> {
    static final NaturalOrder INSTANCE = new NaturalOrder();

    public int compare(Comparable<Object> a, Comparable<Object> b) {
        return a.compareTo(b);
    }
}
