/*
 * Copyright (c) 2026 Codename One and contributors. All rights reserved.
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
