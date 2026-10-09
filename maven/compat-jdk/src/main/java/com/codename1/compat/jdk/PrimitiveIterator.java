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

import java.util.Iterator;
import java.util.function.Consumer;

/// `java.util.PrimitiveIterator` for the Codename One runtime: an iterator
/// that can hand out its elements unboxed.
public interface PrimitiveIterator<T, T_CONS> extends Iterator<T> {

    void forEachRemaining(T_CONS action);

    /// An iterator of `int` values.
    interface OfInt extends PrimitiveIterator<Integer, IntConsumer> {

        int nextInt();

        @Override
        default void forEachRemaining(IntConsumer action) {
            if (action == null) {
                throw new NullPointerException();
            }
            while (hasNext()) {
                action.accept(nextInt());
            }
        }

        @Override
        default Integer next() {
            return Integer.valueOf(nextInt());
        }

        default void forEachRemaining(Consumer<? super Integer> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            while (hasNext()) {
                action.accept(Integer.valueOf(nextInt()));
            }
        }
    }

    /// An iterator of `long` values.
    interface OfLong extends PrimitiveIterator<Long, LongConsumer> {

        long nextLong();

        @Override
        default void forEachRemaining(LongConsumer action) {
            if (action == null) {
                throw new NullPointerException();
            }
            while (hasNext()) {
                action.accept(nextLong());
            }
        }

        @Override
        default Long next() {
            return Long.valueOf(nextLong());
        }

        default void forEachRemaining(Consumer<? super Long> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            while (hasNext()) {
                action.accept(Long.valueOf(nextLong()));
            }
        }
    }

    /// An iterator of `double` values.
    interface OfDouble extends PrimitiveIterator<Double, DoubleConsumer> {

        double nextDouble();

        @Override
        default void forEachRemaining(DoubleConsumer action) {
            if (action == null) {
                throw new NullPointerException();
            }
            while (hasNext()) {
                action.accept(nextDouble());
            }
        }

        @Override
        default Double next() {
            return Double.valueOf(nextDouble());
        }

        default void forEachRemaining(Consumer<? super Double> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            while (hasNext()) {
                action.accept(Double.valueOf(nextDouble()));
            }
        }
    }
}
