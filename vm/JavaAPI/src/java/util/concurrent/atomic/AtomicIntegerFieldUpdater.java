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
package java.util.concurrent.atomic;

/// Atomic access to a volatile `int` field of a class, as in the JDK.
///
/// Resolved by the translator exactly as [AtomicReferenceFieldUpdater] is: see that
/// class for how, and for what a call with non-constant arguments does. Only
/// [#get], [#set] and [#compareAndSet] are generated per field; every other operation
/// is a compare-and-set loop over them.
public abstract class AtomicIntegerFieldUpdater<T> {
    /// For the subclasses the translator generates.
    protected AtomicIntegerFieldUpdater() {
    }

    /// Resolved by the translator when every argument is a constant; reaching this
    /// method means it could not be.
    public static <U> AtomicIntegerFieldUpdater<U> newUpdater(Class<U> tclass, String fieldName) {
        throw new UnsupportedOperationException("Field updaters must name their class and field as constants: " + fieldName);
    }

    public abstract boolean compareAndSet(T obj, int expect, int update);

    public boolean weakCompareAndSet(T obj, int expect, int update) {
        return compareAndSet(obj, expect, update);
    }

    public abstract void set(T obj, int newValue);

    public void lazySet(T obj, int newValue) {
        set(obj, newValue);
    }

    public abstract int get(T obj);

    public int getAndSet(T obj, int newValue) {
        int prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, newValue));
        return prev;
    }

    public int getAndAdd(T obj, int delta) {
        int prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, prev + delta));
        return prev;
    }

    public int addAndGet(T obj, int delta) {
        return getAndAdd(obj, delta) + delta;
    }

    public int getAndIncrement(T obj) {
        return getAndAdd(obj, 1);
    }

    public int getAndDecrement(T obj) {
        return getAndAdd(obj, -1);
    }

    public int incrementAndGet(T obj) {
        return getAndAdd(obj, 1) + 1;
    }

    public int decrementAndGet(T obj) {
        return getAndAdd(obj, -1) - 1;
    }

    /// Equality, for the branch-free compare-and-set the translator generates where
    /// every thread shares one host thread (JavaScript).
    protected static boolean cn1Same(int a, int b) {
        return a == b;
    }

    /// `c ? a : b`, for the same generated code.
    protected static int cn1Select(boolean c, int a, int b) {
        return c ? a : b;
    }
}
