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

/// Metadata describing a list, grid, table, or other collection.
public final class AccessibilityCollectionInfo {
    public static final int SELECTION_NONE = 0;
    public static final int SELECTION_SINGLE = 1;
    public static final int SELECTION_MULTIPLE = 2;

    private final int rowCount;
    private final int columnCount;
    private final boolean hierarchical;
    private final int selectionMode;

    public AccessibilityCollectionInfo(int rowCount, int columnCount) {
        this(rowCount, columnCount, false, SELECTION_NONE);
    }

    public AccessibilityCollectionInfo(int rowCount, int columnCount, boolean hierarchical, int selectionMode) {
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.hierarchical = hierarchical;
        this.selectionMode = selectionMode;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public boolean isHierarchical() {
        return hierarchical;
    }

    public int getSelectionMode() {
        return selectionMode;
    }
}
