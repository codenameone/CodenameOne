/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

/// Generated from rive-runtime's `GridItemPlacementBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `GridItemPlacement`.
public abstract class GridItemPlacementBase extends Component {
    public static final int TYPE_KEY = 1068;
    public static final int GRID_COLUMN_PROPERTY_KEY = 1047;
    public static final int GRID_ROW_PROPERTY_KEY = 1048;
    public static final int GRID_COLUMN_SPAN_PROPERTY_KEY = 1049;
    public static final int GRID_ROW_SPAN_PROPERTY_KEY = 1050;

    protected int mGridColumn = 0;
    protected int mGridRow = 0;
    protected int mGridColumnSpan = 1;
    protected int mGridRowSpan = 1;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getGridColumn() {
        return mGridColumn;
    }

    public void setGridColumn(int value) {
        if (mGridColumn == value) {
            return;
        }
        mGridColumn = value;
        gridColumnChanged();
    }

    protected void gridColumnChanged() {
    }

    public int getGridRow() {
        return mGridRow;
    }

    public void setGridRow(int value) {
        if (mGridRow == value) {
            return;
        }
        mGridRow = value;
        gridRowChanged();
    }

    protected void gridRowChanged() {
    }

    public int getGridColumnSpan() {
        return mGridColumnSpan;
    }

    public void setGridColumnSpan(int value) {
        if (mGridColumnSpan == value) {
            return;
        }
        mGridColumnSpan = value;
        gridColumnSpanChanged();
    }

    protected void gridColumnSpanChanged() {
    }

    public int getGridRowSpan() {
        return mGridRowSpan;
    }

    public void setGridRowSpan(int value) {
        if (mGridRowSpan == value) {
            return;
        }
        mGridRowSpan = value;
        gridRowSpanChanged();
    }

    protected void gridRowSpanChanged() {
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case GRID_COLUMN_PROPERTY_KEY:
                mGridColumn = value;
                return true;
            case GRID_ROW_PROPERTY_KEY:
                mGridRow = value;
                return true;
            case GRID_COLUMN_SPAN_PROPERTY_KEY:
                mGridColumnSpan = value;
                return true;
            case GRID_ROW_SPAN_PROPERTY_KEY:
                mGridRowSpan = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case GRID_COLUMN_PROPERTY_KEY:
                setGridColumn(value);
                return true;
            case GRID_ROW_PROPERTY_KEY:
                setGridRow(value);
                return true;
            case GRID_COLUMN_SPAN_PROPERTY_KEY:
                setGridColumnSpan(value);
                return true;
            case GRID_ROW_SPAN_PROPERTY_KEY:
                setGridRowSpan(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case GRID_COLUMN_PROPERTY_KEY:
                return mGridColumn;
            case GRID_ROW_PROPERTY_KEY:
                return mGridRow;
            case GRID_COLUMN_SPAN_PROPERTY_KEY:
                return mGridColumnSpan;
            case GRID_ROW_SPAN_PROPERTY_KEY:
                return mGridRowSpan;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
