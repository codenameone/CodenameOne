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
public class AtomicInteger extends Number implements java.io.Serializable {

    public AtomicInteger(int initialValue) {
    }

    public AtomicInteger() {
    }

    public final int get() {
        return 0;
    }

    public final void set(int newValue) {
    }

    public final void lazySet(int newValue) {
    }

    public final int getAndSet(int newValue) {
        return 0;
    }

    public final boolean compareAndSet(int expect, int update) {
        return false;
    }

    public final boolean weakCompareAndSet(int expect, int update) {
        return false;
    }

    public final int getAndIncrement() {
        return 0;
    }

    public final int getAndDecrement() {
        return 0;
    }

    public final int getAndAdd(int delta) {
        return 0;
    }

    public final int incrementAndGet() {
        return 0;
    }

    public final int decrementAndGet() {
        return 0;
    }

    public final int addAndGet(int delta) {
        return 0;
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
