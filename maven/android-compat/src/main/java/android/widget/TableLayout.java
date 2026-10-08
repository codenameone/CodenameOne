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
package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashSet;
import java.util.Set;

/// Rows of cells aligned into columns. Each [TableRow] reports the natural
/// width of its cells; the table takes the widest per column, stretches or
/// shrinks the columns named in `stretchColumns` / `shrinkColumns` to fill
/// the width, and hands the result back to every row to lay out against.
public class TableLayout extends LinearLayout {

    private int[] mMaxWidths;
    private final Set<Integer> mCollapsedColumns = new HashSet<Integer>();
    private final Set<Integer> mStretchableColumns = new HashSet<Integer>();
    private final Set<Integer> mShrinkableColumns = new HashSet<Integer>();
    private boolean mShrinkAllColumns;
    private boolean mStretchAllColumns;

    /// Table rows are always as wide as the table.
    public static class LayoutParams extends LinearLayout.LayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int w, int h) {
            super(MATCH_PARENT, h);
        }

        public LayoutParams(int w, int h, float initWeight) {
            super(MATCH_PARENT, h, initWeight);
        }

        public LayoutParams() {
            super(MATCH_PARENT, WRAP_CONTENT);
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
            width = MATCH_PARENT;
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
            width = MATCH_PARENT;
        }

        @Override
        protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr) {
            this.width = MATCH_PARENT;
            this.height = a.hasValue(heightAttr) ? a.getLayoutDimension(heightAttr, "layout_height") : WRAP_CONTENT;
        }
    }

    public TableLayout(Context context) {
        super(context);
        setOrientation(VERTICAL);
    }

    public TableLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.TableLayout);
        String stretched = a.getString(android.R.styleable.TableLayout_stretchColumns);
        if (stretched != null) {
            if (stretched.length() > 0 && stretched.charAt(0) == '*') {
                mStretchAllColumns = true;
            } else {
                parseColumns(stretched, mStretchableColumns);
            }
        }
        String shrunk = a.getString(android.R.styleable.TableLayout_shrinkColumns);
        if (shrunk != null) {
            if (shrunk.length() > 0 && shrunk.charAt(0) == '*') {
                mShrinkAllColumns = true;
            } else {
                parseColumns(shrunk, mShrinkableColumns);
            }
        }
        String collapsed = a.getString(android.R.styleable.TableLayout_collapseColumns);
        if (collapsed != null) {
            parseColumns(collapsed, mCollapsedColumns);
        }
        a.recycle();
        setOrientation(VERTICAL);
    }

    /// Parses "1, 3,5" into column indexes, skipping anything that is not a
    /// number, as Android does.
    private static void parseColumns(String sequence, Set<Integer> out) {
        int start = 0;
        for (int i = 0; i <= sequence.length(); i++) {
            if (i == sequence.length() || sequence.charAt(i) == ',') {
                String part = sequence.substring(start, i).trim();
                if (part.length() > 0) {
                    try {
                        int column = Integer.parseInt(part);
                        if (column >= 0) {
                            out.add(Integer.valueOf(column));
                        }
                    } catch (NumberFormatException ignored) {
                        // Ignore malformed entries, like Android.
                    }
                }
                start = i + 1;
            }
        }
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        if (child instanceof TableRow) {
            for (Integer c : mCollapsedColumns) {
                ((TableRow) child).setColumnCollapsed(c.intValue(), true);
            }
        }
    }

    public boolean isShrinkAllColumns() {
        return mShrinkAllColumns;
    }

    public void setShrinkAllColumns(boolean shrinkAllColumns) {
        mShrinkAllColumns = shrinkAllColumns;
        requestLayout();
    }

    public boolean isStretchAllColumns() {
        return mStretchAllColumns;
    }

    public void setStretchAllColumns(boolean stretchAllColumns) {
        mStretchAllColumns = stretchAllColumns;
        requestLayout();
    }

    public void setColumnCollapsed(int columnIndex, boolean isCollapsed) {
        if (isCollapsed) {
            mCollapsedColumns.add(Integer.valueOf(columnIndex));
        } else {
            mCollapsedColumns.remove(Integer.valueOf(columnIndex));
        }
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof TableRow) {
                ((TableRow) view).setColumnCollapsed(columnIndex, isCollapsed);
            }
        }
        requestLayout();
    }

    public boolean isColumnCollapsed(int columnIndex) {
        return mCollapsedColumns.contains(Integer.valueOf(columnIndex));
    }

    public void setColumnStretchable(int columnIndex, boolean isStretchable) {
        if (isStretchable) {
            mStretchableColumns.add(Integer.valueOf(columnIndex));
        } else {
            mStretchableColumns.remove(Integer.valueOf(columnIndex));
        }
        requestLayout();
    }

    public boolean isColumnStretchable(int columnIndex) {
        return mStretchAllColumns || mStretchableColumns.contains(Integer.valueOf(columnIndex));
    }

    public void setColumnShrinkable(int columnIndex, boolean isShrinkable) {
        if (isShrinkable) {
            mShrinkableColumns.add(Integer.valueOf(columnIndex));
        } else {
            mShrinkableColumns.remove(Integer.valueOf(columnIndex));
        }
        requestLayout();
    }

    public boolean isColumnShrinkable(int columnIndex) {
        return mShrinkAllColumns || mShrinkableColumns.contains(Integer.valueOf(columnIndex));
    }

    @Override
    public void setOrientation(int orientation) {
        // A table is always vertical; Android ignores other values too.
        super.setOrientation(VERTICAL);
    }

    @Override
    void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        findLargestCells(widthMeasureSpec, heightMeasureSpec);
        shrinkAndStretchColumns(widthMeasureSpec);
        super.measureVertical(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        if (child instanceof TableRow) {
            ((TableRow) child).setColumnsWidthConstraints(mMaxWidths);
        }
        super.measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
    }

    private void findLargestCells(int widthMeasureSpec, int heightMeasureSpec) {
        boolean firstRow = true;
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE || !(child instanceof TableRow)) {
                continue;
            }
            final TableRow row = (TableRow) child;
            row.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
            final int[] widths = row.getColumnsWidths(widthMeasureSpec, heightMeasureSpec);
            final int newLength = widths.length;
            if (firstRow) {
                if (mMaxWidths == null || mMaxWidths.length != newLength) {
                    mMaxWidths = new int[newLength];
                }
                System.arraycopy(widths, 0, mMaxWidths, 0, newLength);
                firstRow = false;
            } else {
                int length = mMaxWidths.length;
                final int difference = newLength - length;
                if (difference > 0) {
                    final int[] oldMaxWidths = mMaxWidths;
                    mMaxWidths = new int[newLength];
                    System.arraycopy(oldMaxWidths, 0, mMaxWidths, 0, oldMaxWidths.length);
                    System.arraycopy(widths, oldMaxWidths.length, mMaxWidths, oldMaxWidths.length, difference);
                }
                final int[] maxWidths = mMaxWidths;
                length = Math.min(length, newLength);
                for (int j = 0; j < length; j++) {
                    maxWidths[j] = Math.max(maxWidths[j], widths[j]);
                }
            }
        }
    }

    private void shrinkAndStretchColumns(int widthMeasureSpec) {
        if (mMaxWidths == null) {
            return;
        }
        int totalWidth = 0;
        for (int width : mMaxWidths) {
            totalWidth += width;
        }
        int size = MeasureSpec.getSize(widthMeasureSpec) - mPaddingLeft - mPaddingRight;
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            return;
        }
        if ((totalWidth > size) && (mShrinkAllColumns || !mShrinkableColumns.isEmpty())) {
            mutateColumnsWidth(mShrinkableColumns, mShrinkAllColumns, size, totalWidth);
        } else if ((totalWidth < size) && (mStretchAllColumns || !mStretchableColumns.isEmpty())) {
            mutateColumnsWidth(mStretchableColumns, mStretchAllColumns, size, totalWidth);
        }
    }

    private void mutateColumnsWidth(Set<Integer> columns, boolean allColumns, int size, int totalWidth) {
        int skipped = 0;
        final int[] maxWidths = mMaxWidths;
        final int length = maxWidths.length;
        final int count = allColumns ? length : columns.size();
        if (count == 0) {
            return;
        }
        final int totalExtraSpace = size - totalWidth;
        int extraSpace = totalExtraSpace / count;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof TableRow) {
                child.forceLayout();
            }
        }
        if (!allColumns) {
            for (Integer c : columns) {
                int column = c.intValue();
                if (column < length) {
                    maxWidths[column] = Math.max(0, maxWidths[column] + extraSpace);
                } else {
                    skipped++;
                }
            }
        } else {
            for (int i = 0; i < count; i++) {
                maxWidths[i] = Math.max(0, maxWidths[i] + extraSpace);
            }
            return;
        }
        if (skipped > 0 && skipped < count) {
            extraSpace = skipped * extraSpace / (count - skipped);
            for (Integer c : columns) {
                int column = c.intValue();
                if (column < length) {
                    if (extraSpace > maxWidths[column]) {
                        maxWidths[column] = 0;
                    } else {
                        maxWidths[column] += extraSpace;
                    }
                }
            }
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new TableLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected LinearLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof TableLayout.LayoutParams;
    }

    @Override
    protected LinearLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) p);
        }
        return new LayoutParams(p);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return TableLayout.class.getName();
    }
}
