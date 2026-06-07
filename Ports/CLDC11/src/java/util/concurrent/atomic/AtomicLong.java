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

/// CLDC11 subset stub. Compile-time visible only; the actual runtime
/// implementation comes from the platform (the Android JDK on Android,
/// `vm/JavaAPI` on ParparVM, the host JDK in the JavaSE simulator).
public class AtomicLong extends Number implements java.io.Serializable {

    public AtomicLong(long initialValue) {
    }

    public AtomicLong() {
    }

    public final long get() {
        return 0L;
    }

    public final void set(long newValue) {
    }

    public final void lazySet(long newValue) {
    }

    public final long getAndSet(long newValue) {
        return 0L;
    }

    public final boolean compareAndSet(long expect, long update) {
        return false;
    }

    public final boolean weakCompareAndSet(long expect, long update) {
        return false;
    }

    public final long getAndIncrement() {
        return 0L;
    }

    public final long getAndDecrement() {
        return 0L;
    }

    public final long getAndAdd(long delta) {
        return 0L;
    }

    public final long incrementAndGet() {
        return 0L;
    }

    public final long decrementAndGet() {
        return 0L;
    }

    public final long addAndGet(long delta) {
        return 0L;
    }

    @Override
    public int intValue() {
        return 0;
    }

    @Override
    public long longValue() {
        return 0L;
    }

    @Override
    public float floatValue() {
        return 0f;
    }

    @Override
    public double doubleValue() {
        return 0.0;
    }
}
