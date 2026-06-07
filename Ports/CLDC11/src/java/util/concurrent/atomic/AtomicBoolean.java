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
public class AtomicBoolean implements java.io.Serializable {

    public AtomicBoolean(boolean initialValue) {
    }

    public AtomicBoolean() {
    }

    public final boolean get() {
        return false;
    }

    public final void set(boolean newValue) {
    }

    public final void lazySet(boolean newValue) {
    }

    public final boolean getAndSet(boolean newValue) {
        return false;
    }

    public final boolean compareAndSet(boolean expect, boolean update) {
        return false;
    }

    public final boolean weakCompareAndSet(boolean expect, boolean update) {
        return false;
    }
}
