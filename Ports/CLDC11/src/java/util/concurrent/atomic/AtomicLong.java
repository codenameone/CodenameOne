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
 * Codename One subset implementation of {@code AtomicLong}. Backed by a
 * monitor on the receiver rather than the JDK's CAS hardware intrinsics --
 * the visible contract (happens-before, CAS semantics) is preserved.
 */
public class AtomicLong extends Number implements java.io.Serializable {
    private volatile long value;

    public AtomicLong(long initialValue) {
        value = initialValue;
    }

    public AtomicLong() {
    }

    public final long get() {
        return value;
    }

    public final void set(long newValue) {
        value = newValue;
    }

    public final void lazySet(long newValue) {
        value = newValue;
    }

    public final long getAndSet(long newValue) {
        synchronized (this) {
            long prev = value;
            value = newValue;
            return prev;
        }
    }

    public final boolean compareAndSet(long expect, long update) {
        synchronized (this) {
            if (value == expect) {
                value = update;
                return true;
            }
            return false;
        }
    }

    public final boolean weakCompareAndSet(long expect, long update) {
        return compareAndSet(expect, update);
    }

    public final long getAndIncrement() {
        synchronized (this) {
            return value++;
        }
    }

    public final long getAndDecrement() {
        synchronized (this) {
            return value--;
        }
    }

    public final long getAndAdd(long delta) {
        synchronized (this) {
            long prev = value;
            value += delta;
            return prev;
        }
    }

    public final long incrementAndGet() {
        synchronized (this) {
            return ++value;
        }
    }

    public final long decrementAndGet() {
        synchronized (this) {
            return --value;
        }
    }

    public final long addAndGet(long delta) {
        synchronized (this) {
            return value += delta;
        }
    }

    @Override
    public String toString() {
        return Long.toString(get());
    }

    @Override
    public int intValue() {
        return (int) get();
    }

    @Override
    public long longValue() {
        return get();
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
