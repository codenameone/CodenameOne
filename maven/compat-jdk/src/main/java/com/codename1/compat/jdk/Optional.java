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

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/// `java.util.Optional` for the Codename One runtime: a container that holds
/// a non-null value or nothing.
public final class Optional<T> {

    private static final Optional<?> EMPTY = new Optional<Object>(null);

    private final T value;

    private Optional(T value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> empty() {
        return (Optional<T>) EMPTY;
    }

    public static <T> Optional<T> of(T value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return new Optional<T>(value);
    }

    public static <T> Optional<T> ofNullable(T value) {
        if (value == null) {
            return empty();
        }
        return new Optional<T>(value);
    }

    public T get() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public boolean isPresent() {
        return value != null;
    }

    public boolean isEmpty() {
        return value == null;
    }

    public void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) {
        if (value != null) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

    @SuppressWarnings("unchecked")
    public Optional<T> or(Supplier<? extends Optional<? extends T>> supplier) {
        if (supplier == null) {
            throw new NullPointerException();
        }
        if (value != null) {
            return this;
        }
        Optional<T> result = (Optional<T>) supplier.get();
        if (result == null) {
            throw new NullPointerException();
        }
        return result;
    }

    /// A stream of the value, or an empty one.
    public Stream<T> stream() {
        return value == null ? Stream.<T>empty() : Stream.of(value);
    }

    public T orElseThrow() {
        return get();
    }

    public void ifPresent(Consumer<? super T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    public Optional<T> filter(Predicate<? super T> predicate) {
        if (predicate == null) {
            throw new NullPointerException();
        }
        if (value == null) {
            return this;
        }
        return predicate.test(value) ? this : Optional.<T>empty();
    }

    public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        if (value == null) {
            return empty();
        }
        return Optional.<U>ofNullable(mapper.apply(value));
    }

    public <U> Optional<U> flatMap(Function<? super T, Optional<U>> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        if (value == null) {
            return empty();
        }
        Optional<U> result = mapper.apply(value);
        if (result == null) {
            throw new NullPointerException();
        }
        return result;
    }

    public T orElse(T other) {
        return value != null ? value : other;
    }

    public T orElseGet(Supplier<? extends T> other) {
        return value != null ? value : other.get();
    }

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (value != null) {
            return value;
        }
        throw exceptionSupplier.get();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Optional)) {
            return false;
        }
        Object other = ((Optional<?>) obj).value;
        return value == null ? other == null : value.equals(other);
    }

    @Override
    public int hashCode() {
        return value == null ? 0 : value.hashCode();
    }

    @Override
    public String toString() {
        return value != null ? "Optional[" + value + "]" : "Optional.empty";
    }
}
