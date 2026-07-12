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
package com.codename1.ui.accessibility;

/// Position and span metadata for an item in a semantic collection.
public final class AccessibilityCollectionItemInfo {
    private final int rowIndex;
    private final int rowSpan;
    private final int columnIndex;
    private final int columnSpan;
    private final int positionInSet;
    private final int setSize;
    private final int level;
    private final boolean heading;

    public AccessibilityCollectionItemInfo(int rowIndex, int columnIndex) {
        this(rowIndex, 1, columnIndex, 1, -1, -1, -1, false);
    }

    public AccessibilityCollectionItemInfo(int rowIndex, int rowSpan, int columnIndex, int columnSpan,
                                           int positionInSet, int setSize, int level, boolean heading) {
        this.rowIndex = rowIndex;
        this.rowSpan = rowSpan;
        this.columnIndex = columnIndex;
        this.columnSpan = columnSpan;
        this.positionInSet = positionInSet;
        this.setSize = setSize;
        this.level = level;
        this.heading = heading;
    }

    public int getRowIndex() {
        return rowIndex;
    }
    public int getRowSpan() {
        return rowSpan;
    }
    public int getColumnIndex() {
        return columnIndex;
    }
    public int getColumnSpan() {
        return columnSpan;
    }
    public int getPositionInSet() {
        return positionInSet;
    }
    public int getSetSize() {
        return setSize;
    }
    public int getLevel() {
        return level;
    }
    public boolean isHeading() {
        return heading;
    }
}
