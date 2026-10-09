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
package com.codename1.unitycompat.system;

/// ``System.ValueTuple`3``: what a C# tuple `(a, b)` is. A struct, so it
/// follows the protocol the translator expects of one -- a no-argument
/// constructor, `$new`, `$copy`, `$assign`, `$clear` and the array helpers
/// -- written out by hand because this type comes from the base library and
/// not from translated C#. The items are erased, like every type argument.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class ValueTuple_3 implements Struct {
    public Object Item1;
    public Object Item2;
    public Object Item3;

    public void $ctor(Object item1, Object item2, Object item3) {
        Item1 = item1;
        Item2 = item2;
        Item3 = item3;
    }

    public static ValueTuple_3 $new(Object item1, Object item2, Object item3, ValueTuple_3 ret) {
        ret.$ctor(item1, item2, item3);
        return ret;
    }

    public ValueTuple_3 $copy() {
        ValueTuple_3 t = new ValueTuple_3();
        t.$assign(this);
        return t;
    }

    @Override
    public Object $copyValue() {
        return $copy();
    }

    public void $assign(ValueTuple_3 other) {
        Item1 = other.Item1;
        Item2 = other.Item2;
        Item3 = other.Item3;
    }

    @Override
    public void $clear() {
        Item1 = null;
        Item2 = null;
        Item3 = null;
    }

    public static void $store(ValueTuple_3[] array, int index, ValueTuple_3 value) {
        array[index].$assign(value);
    }

    public static ValueTuple_3[] $newArray(int length) {
        ValueTuple_3[] array = new ValueTuple_3[length];
        for (int i = 0; i < length; i++) {
            array[i] = new ValueTuple_3();
        }
        return array;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ValueTuple_3)) {
            return false;
        }
        ValueTuple_3 t = (ValueTuple_3) o;
        return Interop.areEqual(Item1, t.Item1) && Interop.areEqual(Item2, t.Item2) && Interop.areEqual(Item3, t.Item3);
    }

    @Override
    public int hashCode() {
        int h = 0;
        h = h * 31 + Interop.hash(Item1);
        h = h * 31 + Interop.hash(Item2);
        h = h * 31 + Interop.hash(Item3);
        return h;
    }

    private static String text(Object o) {
        return o == null ? "" : Object_.ToString(o);
    }

    @Override
    public String toString() {
        return "(" + text(Item1) + ", " + text(Item2) + ", " + text(Item3) + ")";
    }
}
