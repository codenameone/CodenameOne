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
package com.codename1.androidcompat.runtime;

/// One typed value out of the resource table: Android's (type, data, string)
/// triple. Immutable; copied into a `TypedValue` when handed to callers.
public final class ResValue {
    public final int type;
    public final int data;
    /// The string of a TYPE_STRING value, and the source text of any other
    /// value that came from an XML attribute.
    public final String string;

    public ResValue(int type, int data, String string) {
        this.type = type;
        this.data = data;
        this.string = string;
    }

    @Override
    public String toString() {
        return "ResValue[0x" + Integer.toHexString(type) + ", 0x" + Integer.toHexString(data)
                + (string == null ? "" : ", " + string) + "]";
    }
}
