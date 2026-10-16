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
 * Base class for widget keys. Keys control how the element reconciler matches
 * widgets across rebuilds: two widgets can only update the same element when
 * their runtime class matches and their keys are equal.
 */
public class Key {

    private final Object value;

    protected Key() {
        this.value = null;
    }

    /**
     * Dart's {@code Key(String value)} is a factory returning a value key; model it
     * as a concrete key carrying the value so {@code new Key(...)} instantiates.
     */
    public Key(Object value) {
        this.value = value;
    }

    public Object keyValue() {
        return value;
    }

    /// A key WITH a value compares by that value; one without compares by
    /// identity. Only the protected no-argument constructor makes a key without a
    /// value, and the keys built through it -- UniqueKey, GlobalKey -- are the
    /// ones Flutter compares by identity. Treating two null values as equal made
    /// every UniqueKey of a class equal to every other (hash 0 for all), so
    /// Widget.canUpdate kept an element and its State that a fresh UniqueKey was
    /// meant to replace, and keyed reconciliation collapsed distinct siblings;
    /// every GlobalKey likewise equalled every other. ValueKey and ObjectKey keep
    /// their own rules.
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Key) || o.getClass() != getClass()) {
            return false;
        }
        if (value == null) {
            return this == o;
        }
        return value.equals(((Key) o).value);
    }

    @Override
    public int hashCode() {
        return value == null ? System.identityHashCode(this) : value.hashCode();
    }
}
