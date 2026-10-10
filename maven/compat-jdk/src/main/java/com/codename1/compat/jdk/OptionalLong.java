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
import java.util.function.Supplier;

/// `java.util.OptionalLong` for the Codename One runtime: a container that
/// holds one `long` or nothing.
public final class OptionalLong {

    private static final OptionalLong EMPTY = new OptionalLong(false, 0L);

    private final boolean present;
    private final long value;

    private OptionalLong(boolean present, long value) {
        this.present = present;
        this.value = value;
    }

    public static OptionalLong empty() {
        return EMPTY;
    }

    public static OptionalLong of(long value) {
        return new OptionalLong(true, value);
    }

    public long getAsLong() {
        if (!present) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public boolean isPresent() {
        return present;
    }

    public boolean isEmpty() {
        return !present;
    }

    public void ifPresent(LongConsumer action) {
        if (present) {
            action.accept(value);
        }
    }

    public void ifPresentOrElse(LongConsumer action, Runnable emptyAction) {
        if (present) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

    public LongStream stream() {
        return present ? LongStream.of(value) : LongStream.empty();
    }

    public long orElse(long other) {
        return present ? value : other;
    }

    public long orElseGet(LongSupplier supplier) {
        return present ? value : supplier.getAsLong();
    }

    public long orElseThrow() {
        return getAsLong();
    }

    public <X extends Throwable> long orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (present) {
            return value;
        }
        throw exceptionSupplier.get();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OptionalLong)) {
            return false;
        }
        OptionalLong other = (OptionalLong) obj;
        if (present != other.present) {
            return false;
        }
        return !present || Long.compare(value, other.value) == 0;
    }

    @Override
    public int hashCode() {
        return present ? Long.valueOf(value).hashCode() : 0;
    }

    @Override
    public String toString() {
        return present ? "OptionalLong[" + value + "]" : "OptionalLong.empty";
    }
}
