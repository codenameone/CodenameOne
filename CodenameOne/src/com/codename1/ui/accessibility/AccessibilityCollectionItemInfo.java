/*
 * Copyright (c) 2026 Codename One and contributors. All rights reserved.
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
