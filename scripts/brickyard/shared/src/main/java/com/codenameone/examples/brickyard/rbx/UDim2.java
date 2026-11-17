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
package com.codenameone.examples.brickyard.rbx;

import com.codenameone.examples.brickyard.luau.Interp;
import com.codenameone.examples.brickyard.luau.LuaError;

/// A GUI size or position: a [UDim] per axis.
public final class UDim2 extends RbxType {
    public final UDim x;
    public final UDim y;

    public UDim2(UDim x, UDim y) {
        this.x = x;
        this.y = y;
    }

    public UDim2(double xs, double xo, double ys, double yo) {
        this(new UDim(xs, xo), new UDim(ys, yo));
    }

    @Override
    public String typeName() {
        return "UDim2";
    }

    private static final Method LERP = new Method("Lerp", 0);

    @Override
    public Object get(Interp in, String k) {
        switch (k) {
            case "X":
            case "Width":
                return x;
            case "Y":
            case "Height":
                return y;
            case "Lerp":
                return LERP;
            default:
                return super.get(in, k);
        }
    }

    public UDim2 lerp(UDim2 o, double t) {
        return new UDim2(x.scale + (o.x.scale - x.scale) * t, x.offset + (o.x.offset - x.offset) * t,
                y.scale + (o.y.scale - y.scale) * t, y.offset + (o.y.offset - y.offset) * t);
    }

    @Override
    protected Object[] invoke(int id, Interp in, Object[] a) {
        Object o = Args.at(a, 1);
        if (!(o instanceof UDim2)) {
            throw Args.bad(a, 1, "Lerp", "UDim2");
        }
        return ret(lerp((UDim2) o, Args.num(a, 2, "Lerp")));
    }

    @Override
    public Object arith(String op, Object o, boolean selfLeft) {
        if (o instanceof UDim2) {
            UDim2 u = (UDim2) o;
            UDim2 l = selfLeft ? this : u;
            UDim2 r = selfLeft ? u : this;
            if ("add".equals(op)) {
                return new UDim2(l.x.scale + r.x.scale, l.x.offset + r.x.offset, l.y.scale + r.y.scale, l.y.offset + r.y.offset);
            }
            if ("sub".equals(op)) {
                return new UDim2(l.x.scale - r.x.scale, l.x.offset - r.x.offset, l.y.scale - r.y.scale, l.y.offset - r.y.offset);
            }
        }
        throw LuaError.rt("attempt to perform arithmetic (" + op + ") on UDim2");
    }

    @Override
    public boolean eq(Object o) {
        return o instanceof UDim2 && x.eq(((UDim2) o).x) && y.eq(((UDim2) o).y);
    }

    @Override
    public boolean equals(Object o) {
        return eq(o);
    }

    @Override
    public int hashCode() {
        return x.hashCode() * 31 + y.hashCode();
    }

    @Override
    public String str() {
        return "{" + x.str() + "}, {" + y.str() + "}";
    }
}
