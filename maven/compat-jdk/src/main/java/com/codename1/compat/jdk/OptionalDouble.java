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

/// `java.util.OptionalDouble` for the Codename One runtime: a container that
/// holds one `double` or nothing.
public final class OptionalDouble {

    private static final OptionalDouble EMPTY = new OptionalDouble(false, 0d);

    private final boolean present;
    private final double value;

    private OptionalDouble(boolean present, double value) {
        this.present = present;
        this.value = value;
    }

    public static OptionalDouble empty() {
        return EMPTY;
    }

    public static OptionalDouble of(double value) {
        return new OptionalDouble(true, value);
    }

    public double getAsDouble() {
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

    public void ifPresent(DoubleConsumer action) {
        if (present) {
            action.accept(value);
        }
    }

    public void ifPresentOrElse(DoubleConsumer action, Runnable emptyAction) {
        if (present) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

    public DoubleStream stream() {
        return present ? DoubleStream.of(value) : DoubleStream.empty();
    }

    public double orElse(double other) {
        return present ? value : other;
    }

    public double orElseGet(DoubleSupplier supplier) {
        return present ? value : supplier.getAsDouble();
    }

    public double orElseThrow() {
        return getAsDouble();
    }

    public <X extends Throwable> double orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
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
        if (!(obj instanceof OptionalDouble)) {
            return false;
        }
        OptionalDouble other = (OptionalDouble) obj;
        if (present != other.present) {
            return false;
        }
        return !present || Double.compare(value, other.value) == 0;
    }

    @Override
    public int hashCode() {
        return present ? Double.valueOf(value).hashCode() : 0;
    }

    @Override
    public String toString() {
        return present ? "OptionalDouble[" + value + "]" : "OptionalDouble.empty";
    }
}
