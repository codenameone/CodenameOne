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

/// A closed numeric interval.
public final class NumberRange extends RbxType {
    public final double min;
    public final double max;

    public NumberRange(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public String typeName() {
        return "NumberRange";
    }

    @Override
    public Object get(Interp in, String k) {
        if ("Min".equals(k)) {
            return Args.n(min);
        }
        if ("Max".equals(k)) {
            return Args.n(max);
        }
        return super.get(in, k);
    }

    @Override
    public boolean eq(Object o) {
        return o instanceof NumberRange && ((NumberRange) o).min == min && ((NumberRange) o).max == max;
    }

    @Override
    public boolean equals(Object o) {
        return eq(o);
    }

    @Override
    public int hashCode() {
        return (int) (min * 31 + max);
    }

    @Override
    public String str() {
        return Interp.numberToString(min) + " " + Interp.numberToString(max);
    }
}
