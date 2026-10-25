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

/// Atomic access to a volatile reference field of a class, as in the JDK.
///
/// The device has no reflection, so [#newUpdater(Class, Class, String)] cannot find a
/// field by name at run time. The translator resolves the call instead: wherever the
/// class, the value class and the field name are constants -- which is how javac,
/// kotlinc and atomicfu all emit it, in a static initializer -- it replaces the call
/// with an instance of a subclass it generates for that one field. The generated
/// [#compareAndSet] is a hardware compare-and-swap on the field, so it is atomic with
/// respect to plain volatile reads and writes of the field as well as to other
/// updaters. A call whose arguments are not constants reaches the method below and
/// throws.
public abstract class AtomicReferenceFieldUpdater<T, V> {
    /// For the subclasses the translator generates.
    protected AtomicReferenceFieldUpdater() {
    }

    /// Resolved by the translator when every argument is a constant; see the class
    /// description. Reaching this method means it could not be.
    public static <U, W> AtomicReferenceFieldUpdater<U, W> newUpdater(Class<U> tclass, Class<W> vclass, String fieldName) {
        throw new UnsupportedOperationException("Field updaters must name their class and field as constants: " + fieldName);
    }

    public abstract boolean compareAndSet(T obj, V expect, V update);

    public boolean weakCompareAndSet(T obj, V expect, V update) {
        return compareAndSet(obj, expect, update);
    }

    public abstract void set(T obj, V newValue);

    public void lazySet(T obj, V newValue) {
        set(obj, newValue);
    }

    public abstract V get(T obj);

    public V getAndSet(T obj, V newValue) {
        V prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, newValue));
        return prev;
    }

    /// Identity comparison, for the branch-free compare-and-set the translator
    /// generates where every thread shares one host thread (JavaScript).
    protected static boolean cn1Same(Object a, Object b) {
        return a == b;
    }

    /// , for the same generated code.
    protected static Object cn1Select(boolean c, Object a, Object b) {
        return c ? a : b;
    }
}
