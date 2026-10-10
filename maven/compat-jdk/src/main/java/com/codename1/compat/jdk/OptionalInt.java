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

/// `java.util.OptionalInt` for the Codename One runtime: a container that
/// holds one `int` or nothing.
public final class OptionalInt {

    private static final OptionalInt EMPTY = new OptionalInt(false, 0);

    private final boolean present;
    private final int value;

    private OptionalInt(boolean present, int value) {
        this.present = present;
        this.value = value;
    }

    public static OptionalInt empty() {
        return EMPTY;
    }

    public static OptionalInt of(int value) {
        return new OptionalInt(true, value);
    }

    public int getAsInt() {
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

    public void ifPresent(IntConsumer action) {
        if (present) {
            action.accept(value);
        }
    }

    public void ifPresentOrElse(IntConsumer action, Runnable emptyAction) {
        if (present) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

    public IntStream stream() {
        return present ? IntStream.of(value) : IntStream.empty();
    }

    public int orElse(int other) {
        return present ? value : other;
    }

    public int orElseGet(IntSupplier supplier) {
        return present ? value : supplier.getAsInt();
    }

    public int orElseThrow() {
        return getAsInt();
    }

    public <X extends Throwable> int orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
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
        if (!(obj instanceof OptionalInt)) {
            return false;
        }
        OptionalInt other = (OptionalInt) obj;
        if (present != other.present) {
            return false;
        }
        return !present || Integer.compare(value, other.value) == 0;
    }

    @Override
    public int hashCode() {
        return present ? Integer.valueOf(value).hashCode() : 0;
    }

    @Override
    public String toString() {
        return present ? "OptionalInt[" + value + "]" : "OptionalInt.empty";
    }
}
