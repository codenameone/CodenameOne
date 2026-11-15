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
import com.codename1.util.MathUtil;

/// An immutable 3D vector.
public final class Vector3 extends RbxType {
    public static final Vector3 ZERO = new Vector3(0, 0, 0);
    public static final Vector3 ONE = new Vector3(1, 1, 1);
    public static final Vector3 X_AXIS = new Vector3(1, 0, 0);
    public static final Vector3 Y_AXIS = new Vector3(0, 1, 0);
    public static final Vector3 Z_AXIS = new Vector3(0, 0, 1);

    public final double x;
    public final double y;
    public final double z;

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String typeName() {
        return "Vector3";
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vector3 unit() {
        double m = magnitude();
        return m == 0 ? ZERO : new Vector3(x / m, y / m, z / m);
    }

    public Vector3 add(Vector3 o) {
        return new Vector3(x + o.x, y + o.y, z + o.z);
    }

    public Vector3 sub(Vector3 o) {
        return new Vector3(x - o.x, y - o.y, z - o.z);
    }

    public Vector3 scale(double s) {
        return new Vector3(x * s, y * s, z * s);
    }

    public double dot(Vector3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public Vector3 cross(Vector3 o) {
        return new Vector3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    public Vector3 negateV() {
        return new Vector3(-x, -y, -z);
    }

    public Vector3 lerp(Vector3 o, double t) {
        return new Vector3(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t);
    }

    private static final Method DOT = new Method("Dot", 0);
    private static final Method CROSS = new Method("Cross", 1);
    private static final Method LERP = new Method("Lerp", 2);
    private static final Method FUZZY = new Method("FuzzyEq", 3);
    private static final Method ABS = new Method("Abs", 4);
    private static final Method CEIL = new Method("Ceil", 5);
    private static final Method FLOOR = new Method("Floor", 6);
    private static final Method SIGN = new Method("Sign", 7);
    private static final Method MAX = new Method("Max", 8);
    private static final Method MIN = new Method("Min", 9);
    private static final Method ANGLE = new Method("Angle", 10);

    @Override
    public Object get(Interp in, String k) {
        switch (k) {
            case "X":
            case "x":
                return Args.n(x);
            case "Y":
            case "y":
                return Args.n(y);
            case "Z":
            case "z":
                return Args.n(z);
            case "Magnitude":
            case "magnitude":
                return Args.n(magnitude());
            case "Unit":
            case "unit":
                return unit();
            case "Dot":
                return DOT;
            case "Cross":
                return CROSS;
            case "Lerp":
                return LERP;
            case "FuzzyEq":
                return FUZZY;
            case "Abs":
                return ABS;
            case "Ceil":
                return CEIL;
            case "Floor":
                return FLOOR;
            case "Sign":
                return SIGN;
            case "Max":
                return MAX;
            case "Min":
                return MIN;
            case "Angle":
                return ANGLE;
            default:
                return super.get(in, k);
        }
    }

    @Override
    protected Object[] invoke(int id, Interp in, Object[] a) {
        switch (id) {
            case 0:
                return ret(Args.n(dot(Args.vec3(a, 1, "Dot"))));
            case 1:
                return ret(cross(Args.vec3(a, 1, "Cross")));
            case 2:
                return ret(lerp(Args.vec3(a, 1, "Lerp"), Args.num(a, 2, "Lerp")));
            case 3: {
                Vector3 o = Args.vec3(a, 1, "FuzzyEq");
                double eps = Args.optNum(a, 2, 1e-5);
                return ret(Boolean.valueOf(sub(o).magnitude() <= eps));
            }
            case 4:
                return ret(new Vector3(Math.abs(x), Math.abs(y), Math.abs(z)));
            case 5:
                return ret(new Vector3(Math.ceil(x), Math.ceil(y), Math.ceil(z)));
            case 6:
                return ret(new Vector3(Math.floor(x), Math.floor(y), Math.floor(z)));
            case 7:
                return ret(new Vector3(sign(x), sign(y), sign(z)));
            case 8: {
                Vector3 o = Args.vec3(a, 1, "Max");
                return ret(new Vector3(Math.max(x, o.x), Math.max(y, o.y), Math.max(z, o.z)));
            }
            case 9: {
                Vector3 o = Args.vec3(a, 1, "Min");
                return ret(new Vector3(Math.min(x, o.x), Math.min(y, o.y), Math.min(z, o.z)));
            }
            case 10: {
                Vector3 o = Args.vec3(a, 1, "Angle");
                double d = unit().dot(o.unit());
                return ret(Args.n(MathUtil.acos(Math.max(-1, Math.min(1, d)))));
            }
            default:
                return super.invoke(id, in, a);
        }
    }

    static double sign(double v) {
        return v > 0 ? 1 : v < 0 ? -1 : 0;
    }

    @Override
    public Object arith(String op, Object o, boolean selfLeft) {
        if (o instanceof Vector3) {
            Vector3 v = (Vector3) o;
            Vector3 l = selfLeft ? this : v;
            Vector3 r = selfLeft ? v : this;
            switch (op) {
                case "add":
                    return l.add(r);
                case "sub":
                    return l.sub(r);
                case "mul":
                    return new Vector3(l.x * r.x, l.y * r.y, l.z * r.z);
                case "div":
                    return new Vector3(l.x / r.x, l.y / r.y, l.z / r.z);
                case "idiv":
                    return new Vector3(Math.floor(l.x / r.x), Math.floor(l.y / r.y), Math.floor(l.z / r.z));
                default:
                    break;
            }
        } else if (o instanceof Double) {
            double s = ((Double) o).doubleValue();
            switch (op) {
                case "mul":
                    return scale(s);
                case "div":
                    return selfLeft ? new Vector3(x / s, y / s, z / s) : new Vector3(s / x, s / y, s / z);
                case "idiv":
                    return selfLeft ? new Vector3(Math.floor(x / s), Math.floor(y / s), Math.floor(z / s))
                            : new Vector3(Math.floor(s / x), Math.floor(s / y), Math.floor(s / z));
                default:
                    break;
            }
        }
        throw LuaError.rt("attempt to perform arithmetic (" + op + ") on Vector3 and " + Interp.typeName(o));
    }

    @Override
    public Object negate() {
        return new Vector3(-x, -y, -z);
    }

    @Override
    public boolean eq(Object o) {
        if (!(o instanceof Vector3)) {
            return false;
        }
        Vector3 v = (Vector3) o;
        return v.x == x && v.y == y && v.z == z;
    }

    @Override
    public boolean equals(Object o) {
        return eq(o);
    }

    @Override
    public int hashCode() {
        long b = Double.doubleToLongBits(x) * 31 + Double.doubleToLongBits(y) * 17 + Double.doubleToLongBits(z);
        return (int) (b ^ (b >>> 32));
    }

    @Override
    public String str() {
        return Interp.numberToString(x) + ", " + Interp.numberToString(y) + ", " + Interp.numberToString(z);
    }
}
