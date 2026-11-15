/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.rbx;

import com.codename1.luau.Interp;
import com.codename1.luau.LuaError;
import com.codename1.luau.LuaFunction;
import com.codename1.luau.LuaTable;

/// Argument checks with the engine's error wording.
public final class Args {
    private Args() {
    }

    public static Object at(Object[] a, int i) {
        return i < a.length ? a[i] : null;
    }

    public static double num(Object[] a, int i, String fn) {
        Double d = Interp.toNumber(at(a, i));
        if (d == null) {
            throw bad(a, i, fn, "number");
        }
        return d.doubleValue();
    }

    public static double optNum(Object[] a, int i, double def) {
        Object v = at(a, i);
        if (v == null) {
            return def;
        }
        Double d = Interp.toNumber(v);
        return d == null ? def : d.doubleValue();
    }

    public static int integer(Object[] a, int i, String fn) {
        return (int) Math.floor(num(a, i, fn));
    }

    public static String str(Object[] a, int i, String fn) {
        Object v = at(a, i);
        if (v instanceof String) {
            return (String) v;
        }
        if (v instanceof Double) {
            return Interp.numberToString(((Double) v).doubleValue());
        }
        throw bad(a, i, fn, "string");
    }

    public static String optStr(Object[] a, int i, String def) {
        Object v = at(a, i);
        return v instanceof String ? (String) v : def;
    }

    public static boolean bool(Object[] a, int i) {
        return Interp.truthy(at(a, i));
    }

    public static Vector3 vec3(Object[] a, int i, String fn) {
        Object v = at(a, i);
        if (v instanceof Vector3) {
            return (Vector3) v;
        }
        throw bad(a, i, fn, "Vector3");
    }

    public static CFrame cframe(Object[] a, int i, String fn) {
        Object v = at(a, i);
        if (v instanceof CFrame) {
            return (CFrame) v;
        }
        throw bad(a, i, fn, "CFrame");
    }

    public static Instance inst(Object[] a, int i, String fn) {
        Object v = at(a, i);
        if (v instanceof Instance) {
            return (Instance) v;
        }
        throw bad(a, i, fn, "Instance");
    }

    public static LuaFunction fn(Object[] a, int i, String fn) {
        Object v = at(a, i);
        if (v instanceof LuaFunction) {
            return (LuaFunction) v;
        }
        throw bad(a, i, fn, "function");
    }

    public static LuaTable table(Object[] a, int i, String fn) {
        Object v = at(a, i);
        if (v instanceof LuaTable) {
            return (LuaTable) v;
        }
        throw bad(a, i, fn, "table");
    }

    public static LuaError bad(Object[] a, int i, String fn, String want) {
        return LuaError.rt("invalid argument #" + (i + 1) + " to '" + fn + "' (" + want + " expected, got "
                + Interp.typeName(at(a, i)) + ")");
    }

    public static Double n(double d) {
        return Interp.num(d);
    }
}
