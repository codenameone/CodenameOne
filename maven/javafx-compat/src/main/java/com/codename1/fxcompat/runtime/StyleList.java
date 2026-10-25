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
package com.codename1.fxcompat.runtime;

/// What a style sheet's layered value becomes for the node that takes it:
/// one object per fill of a background or stroke of a border, or -- as
/// [#isSides()] says -- the four paints of one stroke's sides, in the
/// order top, right, bottom and left.
///
/// Two lists are equal when their items are, which is what lets the style
/// engine see that a restyle changed nothing.
public final class StyleList {

    private final Object[] items;
    private final boolean sides;

    /// Creates a list that keeps `items` as its own.
    public StyleList(Object[] items, boolean sides) {
        this.items = items;
        this.sides = sides;
    }

    /// Whether the items are the sides of one stroke rather than layers.
    public boolean isSides() {
        return sides;
    }

    /// How many items there are.
    public int size() {
        return items.length;
    }

    /// The item at `index`; past the end, the last one, which is how a
    /// layer without a value of its own is given one. `null` when empty.
    public Object get(int index) {
        if (items.length == 0) {
            return null;
        }
        return items[index < items.length ? Math.max(0, index) : items.length - 1];
    }

    /// The value of layer `index` in `value`: the layer's own when `value`
    /// is a list of layers, else `value` itself for every layer.
    public static Object layer(Object value, int index) {
        if (value instanceof StyleList && !((StyleList) value).sides) {
            return ((StyleList) value).get(index);
        }
        return value;
    }

    /// How many layers `value` is: its size as a list of layers, else one.
    public static int layers(Object value) {
        if (value instanceof StyleList && !((StyleList) value).sides) {
            return ((StyleList) value).items.length;
        }
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StyleList)) {
            return false;
        }
        StyleList other = (StyleList) o;
        if (other.sides != sides || other.items.length != items.length) {
            return false;
        }
        for (int i = 0; i < items.length; i++) {
            Object a = items[i];
            Object b = other.items[i];
            if (a == null ? b != null : !a.equals(b)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = sides ? 31 : 17;
        for (int i = 0; i < items.length; i++) {
            h = h * 31 + (items[i] == null ? 0 : items[i].hashCode());
        }
        return h;
    }
}
