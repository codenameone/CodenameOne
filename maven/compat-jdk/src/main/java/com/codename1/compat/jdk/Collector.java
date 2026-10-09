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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

/// `java.util.stream.Collector` for the Codename One runtime: how
/// [Stream#collect] folds the elements of a stream into a result.
public interface Collector<T, A, R> {

    Supplier<A> supplier();

    BiConsumer<A, T> accumulator();

    BinaryOperator<A> combiner();

    Function<A, R> finisher();

    Set<Characteristics> characteristics();

    static <T, R> Collector<T, R, R> of(Supplier<R> supplier, BiConsumer<R, T> accumulator,
                                        BinaryOperator<R> combiner, Characteristics... characteristics) {
        if (supplier == null || accumulator == null || combiner == null || characteristics == null) {
            throw new NullPointerException();
        }
        Set<Characteristics> all = new HashSet<Characteristics>(Arrays.asList(characteristics));
        all.add(Characteristics.IDENTITY_FINISH);
        return new Collectors.Of<T, R, R>(supplier, accumulator, combiner, r -> r,
                Collections.unmodifiableSet(all));
    }

    static <T, A, R> Collector<T, A, R> of(Supplier<A> supplier, BiConsumer<A, T> accumulator,
                                           BinaryOperator<A> combiner, Function<A, R> finisher,
                                           Characteristics... characteristics) {
        if (supplier == null || accumulator == null || combiner == null || finisher == null
                || characteristics == null) {
            throw new NullPointerException();
        }
        Set<Characteristics> all = new HashSet<Characteristics>(Arrays.asList(characteristics));
        return new Collectors.Of<T, A, R>(supplier, accumulator, combiner, finisher,
                Collections.unmodifiableSet(all));
    }

    /// What a collector says about itself. Nothing here acts on them: a
    /// sequential stream runs every collector the same way.
    enum Characteristics {
        CONCURRENT,
        UNORDERED,
        IDENTITY_FINISH
    }
}
