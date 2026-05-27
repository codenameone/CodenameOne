/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details.
 */
package java.util.concurrent.atomic;

/**
 * Codename One subset implementation of {@code AtomicInteger}. Backed by a
 * monitor on the receiver rather than the JDK's CAS hardware intrinsics --
 * the visible contract (happens-before, CAS semantics) is preserved.
 */
public class AtomicInteger extends Number implements java.io.Serializable {
    private volatile int value;

    public AtomicInteger(int initialValue) {
        value = initialValue;
    }

    public AtomicInteger() {
    }

    public final int get() {
        return value;
    }

    public final void set(int newValue) {
        value = newValue;
    }

    public final void lazySet(int newValue) {
        value = newValue;
    }

    public final int getAndSet(int newValue) {
        synchronized (this) {
            int prev = value;
            value = newValue;
            return prev;
        }
    }

    public final boolean compareAndSet(int expect, int update) {
        synchronized (this) {
            if (value == expect) {
                value = update;
                return true;
            }
            return false;
        }
    }

    public final boolean weakCompareAndSet(int expect, int update) {
        return compareAndSet(expect, update);
    }

    public final int getAndIncrement() {
        synchronized (this) {
            return value++;
        }
    }

    public final int getAndDecrement() {
        synchronized (this) {
            return value--;
        }
    }

    public final int getAndAdd(int delta) {
        synchronized (this) {
            int prev = value;
            value += delta;
            return prev;
        }
    }

    public final int incrementAndGet() {
        synchronized (this) {
            return ++value;
        }
    }

    public final int decrementAndGet() {
        synchronized (this) {
            return --value;
        }
    }

    public final int addAndGet(int delta) {
        synchronized (this) {
            return value += delta;
        }
    }

    @Override
    public String toString() {
        return Integer.toString(get());
    }

    @Override
    public int intValue() {
        return get();
    }

    @Override
    public long longValue() {
        return (long) get();
    }

    @Override
    public float floatValue() {
        return (float) get();
    }

    @Override
    public double doubleValue() {
        return (double) get();
    }
}
