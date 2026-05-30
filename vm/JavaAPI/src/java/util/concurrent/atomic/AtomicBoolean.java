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
 * Codename One subset implementation of {@code AtomicBoolean}. Backed by a
 * monitor on the receiver rather than the JDK's CAS hardware intrinsics --
 * the visible contract (happens-before, CAS semantics) is preserved.
 */
public class AtomicBoolean implements java.io.Serializable {
    private volatile boolean value;

    public AtomicBoolean(boolean initialValue) {
        value = initialValue;
    }

    public AtomicBoolean() {
    }

    public final boolean get() {
        return value;
    }

    public final void set(boolean newValue) {
        value = newValue;
    }

    public final void lazySet(boolean newValue) {
        value = newValue;
    }

    public final boolean getAndSet(boolean newValue) {
        synchronized (this) {
            boolean prev = value;
            value = newValue;
            return prev;
        }
    }

    public final boolean compareAndSet(boolean expect, boolean update) {
        synchronized (this) {
            if (value == expect) {
                value = update;
                return true;
            }
            return false;
        }
    }

    public final boolean weakCompareAndSet(boolean expect, boolean update) {
        return compareAndSet(expect, update);
    }

    @Override
    public String toString() {
        return String.valueOf(get());
    }
}
