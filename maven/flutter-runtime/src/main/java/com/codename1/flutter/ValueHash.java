/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter;

/**
 * Hashing for the double-valued value types -- Offset, Size, Rect, EdgeInsets,
 * Alignment, Radius, BoxConstraints.
 *
 * <p>Their equals compares with {@code ==}, under which 0.0 and -0.0 are equal,
 * while Double.doubleToLongBits tells them apart. Equal values then hashed
 * differently, so an Offset(-0.0, y) could not find an entry stored under
 * Offset(0.0, y) in a map or set.</p>
 */
public final class ValueHash {

    private ValueHash() {
    }

    /** The bits to hash for {@code d}, with -0.0 folded onto 0.0 to agree with {@code ==}. */
    public static long bits(double d) {
        return Double.doubleToLongBits(d == 0.0 ? 0.0 : d);
    }
}
