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
 * Codename One subset implementation of {@code AtomicReference}. Backed by a
 * monitor rather than CAS hardware intrinsics -- the JDK contract is
 * preserved (every read sees a happens-before-correct value, all
 * compare-and-set semantics are honored), but throughput under high contention
 * is lower than the JDK's {@code sun.misc.Unsafe}-backed version.
 *
 * <p>Mirror of {@code vm/JavaAPI/src/java/util/concurrent/atomic/AtomicReference.java}.
 * The ParparVM iOS port has this class; CLDC11 (the runtime-subset feed) and
 * the JavaSE simulator now have it too. Android already has the JDK's full
 * implementation. TeaVM (the legacy JavaScript port) does not currently
 * support {@code java.util.concurrent.atomic} -- ParparVM-based JavaScript
 * builds work correctly.
 */
public class AtomicReference<V> {
    private final Object lock = new Object();
    private V ref;

    public AtomicReference() {
    }

    public AtomicReference(V initialValue) {
        ref = initialValue;
    }

    public final boolean compareAndSet(V expect, V update) {
        synchronized (lock) {
            if (expect == ref) {
                ref = update;
                return true;
            }
            return false;
        }
    }

    public V get() {
        synchronized (lock) {
            return ref;
        }
    }

    public final V getAndSet(V newValue) {
        synchronized (lock) {
            V old = ref;
            ref = newValue;
            return old;
        }
    }

    public final void lazySet(V newValue) {
        synchronized (lock) {
            ref = newValue;
        }
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return String.valueOf(ref);
        }
    }

    public final boolean weakCompareAndSet(V expect, V update) {
        return compareAndSet(expect, update);
    }

    public final void set(V newValue) {
        synchronized (lock) {
            ref = newValue;
        }
    }
}
