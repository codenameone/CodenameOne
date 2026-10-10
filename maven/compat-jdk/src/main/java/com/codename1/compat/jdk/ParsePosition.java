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
package com.codename1.compat.jdk;

/// `java.text.ParsePosition` for the Codename One runtime: where a parse
/// starts, where it stopped, and where it failed.
public class ParsePosition {

    private int index;
    private int errorIndex = -1;

    public ParsePosition(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setErrorIndex(int ei) {
        errorIndex = ei;
    }

    public int getErrorIndex() {
        return errorIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ParsePosition)) {
            return false;
        }
        ParsePosition other = (ParsePosition) obj;
        return index == other.index && errorIndex == other.errorIndex;
    }

    @Override
    public int hashCode() {
        return (errorIndex << 16) | index;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[index=" + index + ",errorIndex=" + errorIndex + ']';
    }
}
