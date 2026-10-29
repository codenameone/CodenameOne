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

/// An array of references updated atomically, as in the JDK.
///
/// Every access goes through this class, so one lock per array makes each operation
/// atomic, the way [AtomicReference] does it.
public class AtomicReferenceArray<E> {
    private final Object lock = new Object();
    private final Object[] array;

    public AtomicReferenceArray(int length) {
        array = new Object[length];
    }

    public AtomicReferenceArray(E[] source) {
        array = new Object[source.length];
        System.arraycopy(source, 0, array, 0, source.length);
    }

    public final int length() {
        return array.length;
    }

    @SuppressWarnings("unchecked")
    public final E get(int i) {
        synchronized (lock) {
            return (E) array[i];
        }
    }

    public final void set(int i, E newValue) {
        synchronized (lock) {
            array[i] = newValue;
        }
    }

    public final void lazySet(int i, E newValue) {
        set(i, newValue);
    }

    @SuppressWarnings("unchecked")
    public final E getAndSet(int i, E newValue) {
        synchronized (lock) {
            Object old = array[i];
            array[i] = newValue;
            return (E) old;
        }
    }

    public final boolean compareAndSet(int i, E expect, E update) {
        synchronized (lock) {
            if (array[i] == expect) {
                array[i] = update;
                return true;
            }
            return false;
        }
    }

    public final boolean weakCompareAndSet(int i, E expect, E update) {
        return compareAndSet(i, expect, update);
    }

    public String toString() {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(get(i));
        }
        return b.append(']').toString();
    }
}
