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

import dart.runtime.DartRuntime;

/**
 * A key that uses value equality of the wrapped value, mirroring Flutter's
 * {@code ValueKey<T>}. Two ValueKeys are equal when they have the same
 * runtime class and equal values.
 */
public class ValueKey<T> extends Key {
    private final T value;

    public ValueKey(T value) {
        this.value = value;
    }

    public T value() {
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
        ValueKey<?> other = (ValueKey<?>) o;
        // Dart's ==, not Java's equals: ValueKey<num>(1) and ValueKey<num>(1.0) are
        // the same key, and two NaN keys are not. Java's equals said the opposite
        // on both, so a rebuild discarded and recreated a subtree it should keep.
        return DartRuntime.eq(value, other.value);
    }

    @Override
    public int hashCode() {
        return DartRuntime.hashOf(value);   // consistent with DartRuntime.eq above
    }

    @Override
    public String toString() {
        return "ValueKey(" + value + ")";
    }
}
