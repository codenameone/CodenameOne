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
package com.codename1.unitycompat.system.collections.generic;

import com.codename1.unitycompat.system.Interop;
import com.codename1.unitycompat.system.Object_;
import com.codename1.unitycompat.system.Struct;

/// ``System.Collections.Generic.KeyValuePair`2``: one entry of a
/// dictionary, which is what a `foreach` over one hands out. A struct, so
/// it follows the protocol the translator expects of one, written out by
/// hand because the type comes from the base library.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class KeyValuePair_2 implements Struct {
    Object key;
    Object value;

    public void $ctor(Object key, Object value) {
        this.key = key;
        this.value = value;
    }

    public static KeyValuePair_2 $new(Object key, Object value, KeyValuePair_2 ret) {
        ret.$ctor(key, value);
        return ret;
    }

    public Object get_Key() {
        return key;
    }

    public Object get_Value() {
        return value;
    }

    public KeyValuePair_2 $copy() {
        KeyValuePair_2 p = new KeyValuePair_2();
        p.$assign(this);
        return p;
    }

    @Override
    public Object $copyValue() {
        return $copy();
    }

    public void $assign(KeyValuePair_2 other) {
        key = other.key;
        value = other.value;
    }

    @Override
    public void $clear() {
        key = null;
        value = null;
    }

    public static void $store(KeyValuePair_2[] array, int index, KeyValuePair_2 value) {
        array[index].$assign(value);
    }

    public static KeyValuePair_2[] $newArray(int length) {
        KeyValuePair_2[] array = new KeyValuePair_2[length];
        for (int i = 0; i < length; i++) {
            array[i] = new KeyValuePair_2();
        }
        return array;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof KeyValuePair_2)) {
            return false;
        }
        KeyValuePair_2 p = (KeyValuePair_2) o;
        return Interop.areEqual(key, p.key) && Interop.areEqual(value, p.value);
    }

    @Override
    public int hashCode() {
        return Interop.hash(key) * 31 + Interop.hash(value);
    }

    private static String text(Object o) {
        return o == null ? "" : Object_.ToString(o);
    }

    @Override
    public String toString() {
        return "[" + text(key) + ", " + text(value) + "]";
    }
}
