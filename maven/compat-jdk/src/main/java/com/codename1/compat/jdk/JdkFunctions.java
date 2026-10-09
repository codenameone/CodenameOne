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

import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/// The default and static methods of `java.util.Comparator` and of the
/// `java.util.function` interfaces the device has: there each of those
/// interfaces is its one abstract method and nothing else.
///
/// The build's remap step redirects each such call here; a default method
/// arrives with its receiver as the first argument.
public final class JdkFunctions {

    private JdkFunctions() {
    }

    /// The natural order of elements that are `Comparable`, for the callers
    /// in this package that have no bound to say so with.
    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> Comparator<T> natural() {
        return (a, b) -> ((Comparable) a).compareTo(b);
    }

    // ---- Comparator ----

    public static <T extends Comparable<? super T>> Comparator<T> naturalOrder() {
        return (a, b) -> a.compareTo(b);
    }

    public static <T extends Comparable<? super T>> Comparator<T> reverseOrder() {
        return (a, b) -> b.compareTo(a);
    }

    public static <T> Comparator<T> nullsFirst(Comparator<? super T> comparator) {
        return nulls(true, comparator);
    }

    public static <T> Comparator<T> nullsLast(Comparator<? super T> comparator) {
        return nulls(false, comparator);
    }

    private static <T> Comparator<T> nulls(boolean first, Comparator<? super T> comparator) {
        return (a, b) -> {
            if (a == null) {
                return b == null ? 0 : first ? -1 : 1;
            }
            if (b == null) {
                return first ? 1 : -1;
            }
            // Without a comparator, values that are there are all equal.
            return comparator == null ? 0 : comparator.compare(a, b);
        };
    }

    public static <T, U extends Comparable<? super U>> Comparator<T> comparing(
            Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (a, b) -> keyExtractor.apply(a).compareTo(keyExtractor.apply(b));
    }

    public static <T, U> Comparator<T> comparing(Function<? super T, ? extends U> keyExtractor,
                                                 Comparator<? super U> keyComparator) {
        Objects.requireNonNull(keyExtractor);
        Objects.requireNonNull(keyComparator);
        return (a, b) -> keyComparator.compare(keyExtractor.apply(a), keyExtractor.apply(b));
    }

    public static <T> Comparator<T> comparingInt(ToIntFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (a, b) -> Integer.compare(keyExtractor.applyAsInt(a), keyExtractor.applyAsInt(b));
    }

    public static <T> Comparator<T> comparingLong(ToLongFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (a, b) -> Long.compare(keyExtractor.applyAsLong(a), keyExtractor.applyAsLong(b));
    }

    public static <T> Comparator<T> comparingDouble(ToDoubleFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (a, b) -> Double.compare(keyExtractor.applyAsDouble(a), keyExtractor.applyAsDouble(b));
    }

    public static <T> Comparator<T> reversed(Comparator<T> self) {
        Objects.requireNonNull(self);
        return (a, b) -> self.compare(b, a);
    }

    public static <T> Comparator<T> thenComparing(Comparator<T> self, Comparator<? super T> other) {
        Objects.requireNonNull(self);
        Objects.requireNonNull(other);
        return (a, b) -> {
            int result = self.compare(a, b);
            return result != 0 ? result : other.compare(a, b);
        };
    }

    public static <T, U extends Comparable<? super U>> Comparator<T> thenComparing(
            Comparator<T> self, Function<? super T, ? extends U> keyExtractor) {
        return thenComparing(self, JdkFunctions.<T, U>comparing(keyExtractor));
    }

    public static <T, U> Comparator<T> thenComparing(Comparator<T> self,
                                                     Function<? super T, ? extends U> keyExtractor,
                                                     Comparator<? super U> keyComparator) {
        return thenComparing(self, JdkFunctions.<T, U>comparing(keyExtractor, keyComparator));
    }

    public static <T> Comparator<T> thenComparingInt(Comparator<T> self, ToIntFunction<? super T> keyExtractor) {
        return thenComparing(self, JdkFunctions.<T>comparingInt(keyExtractor));
    }

    public static <T> Comparator<T> thenComparingLong(Comparator<T> self, ToLongFunction<? super T> keyExtractor) {
        return thenComparing(self, JdkFunctions.<T>comparingLong(keyExtractor));
    }

    public static <T> Comparator<T> thenComparingDouble(Comparator<T> self,
                                                        ToDoubleFunction<? super T> keyExtractor) {
        return thenComparing(self, JdkFunctions.<T>comparingDouble(keyExtractor));
    }

    // ---- Predicate ----

    public static <T> Predicate<T> and(Predicate<T> self, Predicate<? super T> other) {
        Objects.requireNonNull(self);
        Objects.requireNonNull(other);
        return t -> self.test(t) && other.test(t);
    }

    public static <T> Predicate<T> or(Predicate<T> self, Predicate<? super T> other) {
        Objects.requireNonNull(self);
        Objects.requireNonNull(other);
        return t -> self.test(t) || other.test(t);
    }

    public static <T> Predicate<T> negate(Predicate<T> self) {
        Objects.requireNonNull(self);
        return t -> !self.test(t);
    }

    public static <T> Predicate<T> not(Predicate<? super T> target) {
        Objects.requireNonNull(target);
        return t -> !target.test(t);
    }

    public static <T> Predicate<T> isEqual(Object targetRef) {
        return t -> targetRef == null ? t == null : targetRef.equals(t);
    }

    // ---- Function, BiFunction, UnaryOperator, BinaryOperator ----

    public static <T> Function<T, T> identity() {
        return t -> t;
    }

    public static <T> UnaryOperator<T> unaryIdentity() {
        return t -> t;
    }

    public static <T, R, V> Function<T, V> andThen(Function<T, R> self, Function<? super R, ? extends V> after) {
        Objects.requireNonNull(self);
        Objects.requireNonNull(after);
        return t -> after.apply(self.apply(t));
    }

    public static <T, R, V> Function<V, R> compose(Function<T, R> self, Function<? super V, ? extends T> before) {
        Objects.requireNonNull(self);
        Objects.requireNonNull(before);
        return v -> self.apply(before.apply(v));
    }

    public static <T, U, R, V> BiFunction<T, U, V> andThen(BiFunction<T, U, R> self,
                                                           Function<? super R, ? extends V> after) {
        Objects.requireNonNull(self);
        Objects.requireNonNull(after);
        return (t, u) -> after.apply(self.apply(t, u));
    }

    public static <T> BinaryOperator<T> minBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return (a, b) -> comparator.compare(a, b) <= 0 ? a : b;
    }

    public static <T> BinaryOperator<T> maxBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return (a, b) -> comparator.compare(a, b) >= 0 ? a : b;
    }

    // ---- Consumer, BiConsumer ----

    public static <T> Consumer<T> andThen(Consumer<T> self, Consumer<? super T> after) {
        Objects.requireNonNull(self);
        Objects.requireNonNull(after);
        return t -> {
            self.accept(t);
            after.accept(t);
        };
    }

    public static <T, U> BiConsumer<T, U> andThen(BiConsumer<T, U> self, BiConsumer<? super T, ? super U> after) {
        Objects.requireNonNull(self);
        Objects.requireNonNull(after);
        return (t, u) -> {
            self.accept(t, u);
            after.accept(t, u);
        };
    }
}
