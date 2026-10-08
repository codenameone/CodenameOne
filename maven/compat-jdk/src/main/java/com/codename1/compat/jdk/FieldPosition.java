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

/// `java.text.FieldPosition` for the Codename One runtime: names a field of
/// formatted output and receives the range of characters that field took.
/// `NumberFormat.INTEGER_FIELD` and `NumberFormat.FRACTION_FIELD` are the
/// fields the number formats here report.
public class FieldPosition {

    private final int field;
    private int beginIndex;
    private int endIndex;

    public FieldPosition(int field) {
        this.field = field;
    }

    public int getField() {
        return field;
    }

    public int getBeginIndex() {
        return beginIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setBeginIndex(int bi) {
        beginIndex = bi;
    }

    public void setEndIndex(int ei) {
        endIndex = ei;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FieldPosition)) {
            return false;
        }
        FieldPosition other = (FieldPosition) obj;
        return field == other.field && beginIndex == other.beginIndex && endIndex == other.endIndex;
    }

    @Override
    public int hashCode() {
        return (field << 24) | (beginIndex << 16) | endIndex;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[field=" + field + ",attribute=null,beginIndex=" + beginIndex
                + ",endIndex=" + endIndex + ']';
    }
}
