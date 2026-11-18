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

/// One GUI axis: a fraction of the parent plus an offset in pixels.
public final class UDim extends RbxType {
    public final double scale;
    public final double offset;

    public UDim(double scale, double offset) {
        this.scale = scale;
        this.offset = offset;
    }

    @Override
    public String typeName() {
        return "UDim";
    }

    /// The pixel length against a parent of `parent` pixels.
    public double resolve(double parent) {
        return scale * parent + offset;
    }

    @Override
    public Object get(Interp in, String k) {
        if ("Scale".equals(k)) {
            return Args.n(scale);
        }
        if ("Offset".equals(k)) {
            return Args.n(offset);
        }
        return super.get(in, k);
    }

    @Override
    public Object arith(String op, Object o, boolean selfLeft) {
        if (o instanceof UDim) {
            UDim u = (UDim) o;
            if ("add".equals(op)) {
                return new UDim(scale + u.scale, offset + u.offset);
            }
            if ("sub".equals(op)) {
                return selfLeft ? new UDim(scale - u.scale, offset - u.offset) : new UDim(u.scale - scale, u.offset - offset);
            }
        }
        throw LuaError.rt("attempt to perform arithmetic (" + op + ") on UDim");
    }

    @Override
    public Object negate() {
        return new UDim(-scale, -offset);
    }

    @Override
    public boolean eq(Object o) {
        return o instanceof UDim && ((UDim) o).scale == scale && ((UDim) o).offset == offset;
    }

    @Override
    public boolean equals(Object o) {
        return eq(o);
    }

    @Override
    public int hashCode() {
        return (int) (scale * 1000 + offset);
    }

    @Override
    public String str() {
        return Interp.numberToString(scale) + ", " + Interp.numberToString(offset);
    }
}
