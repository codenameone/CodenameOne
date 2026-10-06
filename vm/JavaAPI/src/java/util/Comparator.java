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

/**
 * A {@code Comparator} is used to compare two objects to determine their ordering with
 * respect to each other. On a given {@code Collection}, a {@code Comparator} can be used to
 * obtain a sorted {@code Collection} which is <i>totally ordered</i>. For a {@code Comparator}
 * to be <i>consistent with equals</i>, its {code #compare(Object, Object)}
 * method has to return zero for each pair of elements (a,b) where a.equals(b)
 * holds true. It is recommended that a {@code Comparator} implements
 * {@link java.io.Serializable}.
 *
 * @since 1.2
 */
public interface Comparator<T> {
    /**
     * Compares the two specified objects to determine their relative ordering. The ordering
     * implied by the return value of this method for all possible pairs of
     * {@code (object1, object2)} should form an <i>equivalence relation</i>.
     * This means that
     * <ul>
     * <li>{@code compare(a,a)} returns zero for all {@code a}</li>
     * <li>the sign of {@code compare(a,b)} must be the opposite of the sign of {@code
     * compare(b,a)} for all pairs of (a,b)</li>
     * <li>From {@code compare(a,b) > 0} and {@code compare(b,c) > 0} it must
     * follow {@code compare(a,c) > 0} for all possible combinations of {@code
     * (a,b,c)}</li>
     * </ul>
     * 
     * @param object1
     *            an {@code Object}.
     * @param object2
     *            a second {@code Object} to compare with {@code object1}.
     * @return an integer < 0 if {@code object1} is less than {@code object2}, 0 if they are
     *         equal, and > 0 if {@code object1} is greater than {@code object2}.
     * @throws ClassCastException
     *                if objects are not of the correct type.
     */
    public int compare(T object1, T object2);

    /**
     * Compares this {@code Comparator} with the specified {@code Object} and indicates whether they
     * are equal. In order to be equal, {@code object} must represent the same object
     * as this instance using a class-specific comparison.
     * <p>
     * A {@code Comparator} never needs to override this method, but may choose so for
     * performance reasons.
     * 
     * @param object
     *            the {@code Object} to compare with this comparator.
     * @return boolean {@code true} if specified {@code Object} is the same as this
     *         {@code Object}, and {@code false} otherwise.
     * @see Object#hashCode
     * @see Object#equals
     */
    public boolean equals(Object object);

    /**
     * This ordering, reversed.
     *
     * @return the reversed comparator
     */
    default Comparator<T> reversed() {
        final Comparator<T> self = this;
        return new Comparator<T>() {
            public int compare(T a, T b) {
                return self.compare(b, a);
            }
        };
    }

    /**
     * This ordering, ties broken by {@code other}.
     *
     * @param other the tie breaker
     * @return the combined comparator
     */
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

    /**
     * This ordering, ties broken by the natural order of a key.
     *
     * @param keyExtractor extracts the key
     * @param <U> the key type
     * @return the combined comparator
     */
    default <U extends Comparable<? super U>> Comparator<T> thenComparing(
            java.util.function.Function<? super T, ? extends U> keyExtractor) {
        return thenComparing(Comparator.<T, U>comparing(keyExtractor));
    }

    /**
     * Compares by the natural order of a key.
     *
     * @param keyExtractor extracts the key
     * @param <T> the compared type
     * @param <U> the key type
     * @return the comparator
     */
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

    /**
     * Compares by a key, using the given comparator for the keys.
     *
     * @param keyExtractor extracts the key
     * @param keyComparator orders the keys
     * @param <T> the compared type
     * @param <U> the key type
     * @return the comparator
     */
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

    /**
     * The natural order of comparable values.
     *
     * @param <T> the compared type
     * @return the comparator
     */
    @SuppressWarnings("unchecked")
    static <T extends Comparable<? super T>> Comparator<T> naturalOrder() {
        return (Comparator<T>) NaturalOrder.INSTANCE;
    }

    /**
     * The reverse of the natural order.
     *
     * @param <T> the compared type
     * @return the comparator
     */
    @SuppressWarnings("unchecked")
    static <T extends Comparable<? super T>> Comparator<T> reverseOrder() {
        // Not Collections.reverseOrder(): naming Collections here makes the translator
        // load it, and with it classes (Collections.SetFromMap) whose presence the native
        // collection traversal keys on, in every application.
        return (Comparator<T>) NaturalOrder.INSTANCE.reversed();
    }
}

/** The natural ordering {@link Comparator#naturalOrder()} hands out. */
final class NaturalOrder implements Comparator<Comparable<Object>> {
    static final NaturalOrder INSTANCE = new NaturalOrder();

    public int compare(Comparable<Object> a, Comparable<Object> b) {
        return a.compareTo(b);
    }
}
