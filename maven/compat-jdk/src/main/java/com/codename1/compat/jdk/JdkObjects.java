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
package com.codename1.compat.jdk;

import java.util.function.Supplier;

/// The static methods of `java.util.Objects` that the device's `Objects`
/// does not have. The device class stays the application's `Objects`; the
/// build's remap step redirects only the calls to these methods, each to the
/// method of the same name and signature here.
public final class JdkObjects {

    private JdkObjects() {
    }

    public static boolean isNull(Object obj) {
        return obj == null;
    }

    public static <T> T requireNonNull(T obj, Supplier<String> messageSupplier) {
        if (obj == null) {
            throw new NullPointerException(messageSupplier == null ? null : messageSupplier.get());
        }
        return obj;
    }

    public static <T> T requireNonNullElse(T obj, T defaultObj) {
        if (obj != null) {
            return obj;
        }
        if (defaultObj == null) {
            throw new NullPointerException("defaultObj");
        }
        return defaultObj;
    }

    public static <T> T requireNonNullElseGet(T obj, Supplier<? extends T> supplier) {
        if (obj != null) {
            return obj;
        }
        if (supplier == null) {
            throw new NullPointerException("supplier");
        }
        T made = supplier.get();
        if (made == null) {
            throw new NullPointerException("supplier.get()");
        }
        return made;
    }

    public static int checkIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
        }
        return index;
    }

    public static int checkFromToIndex(int fromIndex, int toIndex, int length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + toIndex
                    + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

    public static int checkFromIndexSize(int fromIndex, int size, int length) {
        if (fromIndex < 0 || size < 0 || fromIndex > length - size) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + fromIndex + " + " + size
                    + ") out of bounds for length " + length);
        }
        return fromIndex;
    }
}
