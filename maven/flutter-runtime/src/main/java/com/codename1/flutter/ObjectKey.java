/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

/**
 * A {@link Key} that is equal to another only when they wrap the identical
 * object (reference equality, Dart's {@code identical}) — Flutter's
 * {@code ObjectKey}. The mail preview cards key themselves by their backing
 * email instance so the framework preserves element state as the list reorders.
 */
public class ObjectKey extends Key {

    private final Object value;

    public ObjectKey(Object value) {
        this.value = value;
    }

    public Object value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        // Dart's identical(), not Java's ==: numbers and booleans are values in Dart, so
        // two separately boxed ObjectKey(1000) are the same key and the keyed element
        // keeps its state across a rebuild.
        return dart.runtime.DartRuntime.identical(((ObjectKey) o).value, this.value);
    }

    @Override
    public int hashCode() {
        // Consistent with identical(): a boxed number or bool hashes by value.
        if (value instanceof Long || value instanceof Double || value instanceof Boolean) {
            return value.hashCode();
        }
        return System.identityHashCode(value);
    }

    @Override
    public String toString() {
        return "ObjectKey(" + value + ")";
    }
}
