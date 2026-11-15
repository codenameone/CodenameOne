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

/// An immutable 2D vector.
public final class Vector2 extends RbxType {
    public static final Vector2 ZERO = new Vector2(0, 0);
    public final double x;
    public final double y;

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String typeName() {
        return "Vector2";
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y);
    }

    private static final Method DOT = new Method("Dot", 0);
    private static final Method LERP = new Method("Lerp", 1);
    private static final Method CROSS = new Method("Cross", 2);

    @Override
    public Object get(Interp in, String k) {
        switch (k) {
            case "X":
            case "x":
                return Args.n(x);
            case "Y":
            case "y":
                return Args.n(y);
            case "Magnitude":
                return Args.n(magnitude());
            case "Unit": {
                double m = magnitude();
                return m == 0 ? ZERO : new Vector2(x / m, y / m);
            }
            case "Dot":
                return DOT;
            case "Lerp":
                return LERP;
            case "Cross":
                return CROSS;
            default:
                return super.get(in, k);
        }
    }

    private static Vector2 v2(Object[] a, int i, String fn) {
        Object o = Args.at(a, i);
        if (o instanceof Vector2) {
            return (Vector2) o;
        }
        throw Args.bad(a, i, fn, "Vector2");
    }

    @Override
    protected Object[] invoke(int id, Interp in, Object[] a) {
        switch (id) {
            case 0: {
                Vector2 o = v2(a, 1, "Dot");
                return ret(Args.n(x * o.x + y * o.y));
            }
            case 1: {
                Vector2 o = v2(a, 1, "Lerp");
                double t = Args.num(a, 2, "Lerp");
                return ret(new Vector2(x + (o.x - x) * t, y + (o.y - y) * t));
            }
            default: {
                Vector2 o = v2(a, 1, "Cross");
                return ret(Args.n(x * o.y - y * o.x));
            }
        }
    }

    @Override
    public Object arith(String op, Object o, boolean selfLeft) {
        if (o instanceof Vector2) {
            Vector2 v = (Vector2) o;
            Vector2 l = selfLeft ? this : v;
            Vector2 r = selfLeft ? v : this;
            switch (op) {
                case "add":
                    return new Vector2(l.x + r.x, l.y + r.y);
                case "sub":
                    return new Vector2(l.x - r.x, l.y - r.y);
                case "mul":
                    return new Vector2(l.x * r.x, l.y * r.y);
                case "div":
                    return new Vector2(l.x / r.x, l.y / r.y);
                default:
                    break;
            }
        } else if (o instanceof Double) {
            double s = ((Double) o).doubleValue();
            if ("mul".equals(op)) {
                return new Vector2(x * s, y * s);
            }
            if ("div".equals(op) && selfLeft) {
                return new Vector2(x / s, y / s);
            }
        }
        throw LuaError.rt("attempt to perform arithmetic (" + op + ") on Vector2");
    }

    @Override
    public Object negate() {
        return new Vector2(-x, -y);
    }

    @Override
    public boolean eq(Object o) {
        return o instanceof Vector2 && ((Vector2) o).x == x && ((Vector2) o).y == y;
    }

    @Override
    public boolean equals(Object o) {
        return eq(o);
    }

    @Override
    public int hashCode() {
        return (int) (x * 31 + y);
    }

    @Override
    public String str() {
        return Interp.numberToString(x) + ", " + Interp.numberToString(y);
    }
}
