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
package javafx.util;

import java.io.Serializable;

import javafx.beans.NamedArg;

/// An immutable key and value.
public class Pair<K, V> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final K key;
    private final V value;

    /// Creates a pair.
    public Pair(@NamedArg("key") K key, @NamedArg("value") V value) {
        this.key = key;
        this.value = value;
    }

    /// Returns the key.
    public K getKey() {
        return key;
    }

    /// Returns the value.
    public V getValue() {
        return value;
    }

    /// Returns the pair as `key=value`.
    @Override
    public String toString() {
        return key + "=" + value;
    }

    @Override
    public int hashCode() {
        return 31 * (key == null ? 0 : key.hashCode()) + (value == null ? 0 : value.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Pair) {
            Pair<?, ?> other = (Pair<?, ?>) o;
            return (key == null ? other.key == null : key.equals(other.key))
                    && (value == null ? other.value == null : value.equals(other.value));
        }
        return false;
    }
}
