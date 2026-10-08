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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

/// One row of a [TableLayout]. Cells occupy consecutive columns unless
/// `layout_column` places them, and `layout_span` makes one cell cover
/// several. Inside a table the row lays its cells out against the column
/// widths the table computed; on its own it behaves as a horizontal
/// LinearLayout.
public class TableRow extends LinearLayout {

    private int mNumColumns;
    private View[] mColumnToChild = new View[0];
    private int[] mColumnWidths;
    private int[] mConstrainedColumnWidths;

    /// A cell's column, span and placement inside its column.
    public static class LayoutParams extends LinearLayout.LayoutParams {
        public int column = -1;
        public int span = 1;
        int mOffset;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, android.R.styleable.TableRow_Cell);
            column = a.getInt(android.R.styleable.TableRow_Cell_layout_column, -1);
            span = a.getInt(android.R.styleable.TableRow_Cell_layout_span, 1);
            if (span <= 1) {
                span = 1;
            }
            a.recycle();
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(int w, int h, float initWeight) {
            super(w, h, initWeight);
        }

        public LayoutParams() {
            super(MATCH_PARENT, WRAP_CONTENT);
        }

        public LayoutParams(int column) {
            this();
            this.column = column;
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        @Override
        protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr) {
            width = a.hasValue(widthAttr) ? a.getLayoutDimension(widthAttr, "layout_width") : MATCH_PARENT;
            height = a.hasValue(heightAttr) ? a.getLayoutDimension(heightAttr, "layout_height") : WRAP_CONTENT;
        }
    }

    public TableRow(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
    }

    public TableRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
    }

    /// Maps columns to children: `layout_column` places a cell, otherwise it
    /// takes the next free column; spans reserve the following columns.
    private void mapIndexAndColumns() {
        int count = getChildCount();
        int next = 0;
        View[] map = new View[Math.max(4, count * 2)];
        int max = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.column >= next) {
                next = lp.column;
            }
            if (next + lp.span > map.length) {
                View[] grown = new View[(next + lp.span) * 2];
                System.arraycopy(map, 0, grown, 0, map.length);
                map = grown;
            }
            map[next] = child;
            next += lp.span;
            max = Math.max(max, next);
        }
        mNumColumns = max;
        mColumnToChild = map;
    }

    @Override
    View getVirtualChildAt(int i) {
        mapIndexAndColumns();
        return i < mNumColumns ? mColumnToChild[i] : null;
    }

    @Override
    int getVirtualChildCount() {
        mapIndexAndColumns();
        return mNumColumns;
    }

    void setColumnCollapsed(int columnIndex, boolean collapsed) {
        View child = getVirtualChildAt(columnIndex);
        if (child != null) {
            child.setVisibility(collapsed ? GONE : VISIBLE);
        }
    }

    /// The natural width of each column's cell (spanning cells count zero),
    /// for the table to combine across rows.
    int[] getColumnsWidths(int widthMeasureSpec, int heightMeasureSpec) {
        mapIndexAndColumns();
        final int numColumns = mNumColumns;
        if (mColumnWidths == null || numColumns != mColumnWidths.length) {
            mColumnWidths = new int[numColumns];
        }
        final int[] columnWidths = mColumnWidths;
        for (int i = 0; i < numColumns; i++) {
            final View child = mColumnToChild[i];
            if (child != null && child.getVisibility() != GONE) {
                final LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                if (layoutParams.span == 1) {
                    int spec;
                    switch (layoutParams.width) {
                        case LayoutParams.WRAP_CONTENT:
                            spec = getChildMeasureSpec(widthMeasureSpec, 0, LayoutParams.WRAP_CONTENT);
                            break;
                        case LayoutParams.MATCH_PARENT:
                            spec = MeasureSpec.makeSafeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec),
                                    MeasureSpec.UNSPECIFIED);
                            break;
                        default:
                            spec = MeasureSpec.makeMeasureSpec(layoutParams.width, MeasureSpec.EXACTLY);
                            break;
                    }
                    child.measure(spec, spec);
                    columnWidths[i] = child.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin;
                } else {
                    columnWidths[i] = 0;
                }
            } else {
                columnWidths[i] = 0;
            }
        }
        return columnWidths;
    }

    void setColumnsWidthConstraints(int[] columnWidths) {
        mConstrainedColumnWidths = columnWidths;
    }

    private int constrained(int column) {
        return mConstrainedColumnWidths != null && column < mConstrainedColumnWidths.length
                ? mConstrainedColumnWidths[column] : 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mConstrainedColumnWidths == null) {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        mapIndexAndColumns();
        int maxHeight = 0;
        int totalWidth = mPaddingLeft + mPaddingRight;
        for (int c = 0; c < mNumColumns; c++) {
            totalWidth += constrained(c);
        }
        for (int c = 0; c < mNumColumns; c++) {
            View child = mColumnToChild[c];
            if (child == null || child.getVisibility() == GONE) {
                continue;
            }
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int columnWidth = 0;
            for (int s = 0; s < lp.span; s++) {
                columnWidth += constrained(c + s);
            }
            int gravity = lp.gravity < 0 ? 0 : lp.gravity;
            boolean isHorizontalGravity = Gravity.isHorizontal(gravity);
            int mode = isHorizontalGravity ? MeasureSpec.AT_MOST : MeasureSpec.EXACTLY;
            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                    Math.max(0, columnWidth - lp.leftMargin - lp.rightMargin), mode);
            int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                    mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin, lp.height);
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            lp.mOffset = 0;
            if (isHorizontalGravity) {
                int extra = columnWidth - lp.leftMargin - lp.rightMargin - child.getMeasuredWidth();
                int abs = Gravity.getAbsoluteGravity(gravity, getLayoutDirection());
                switch (abs & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.RIGHT:
                        lp.mOffset = extra;
                        break;
                    case Gravity.CENTER_HORIZONTAL:
                        lp.mOffset = extra / 2;
                        break;
                    default:
                        break;
                }
            }
            maxHeight = Math.max(maxHeight, child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
        }
        int height = Math.max(maxHeight + mPaddingTop + mPaddingBottom, getSuggestedMinimumHeight());
        setMeasuredDimension(resolveSizeAndState(Math.max(totalWidth, getSuggestedMinimumWidth()), widthMeasureSpec, 0),
                resolveSizeAndState(height, heightMeasureSpec, 0));
        // Cells that match the row's height take its final height.
        int inner = getMeasuredHeight() - mPaddingTop - mPaddingBottom;
        for (int c = 0; c < mNumColumns; c++) {
            View child = mColumnToChild[c];
            if (child == null || child.getVisibility() == GONE) {
                continue;
            }
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.height == LayoutParams.MATCH_PARENT) {
                child.measure(MeasureSpec.makeMeasureSpec(child.getMeasuredWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(Math.max(0, inner - lp.topMargin - lp.bottomMargin),
                                MeasureSpec.EXACTLY));
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mConstrainedColumnWidths == null) {
            super.onLayout(changed, l, t, r, b);
            return;
        }
        mapIndexAndColumns();
        boolean rtl = isLayoutRtl();
        int height = b - t;
        int childSpace = height - mPaddingTop - mPaddingBottom;
        int x = rtl ? r - l - mPaddingRight : mPaddingLeft;
        for (int c = 0; c < mNumColumns; c++) {
            View child = mColumnToChild[c];
            if (child == null) {
                if (rtl) {
                    x -= constrained(c);
                } else {
                    x += constrained(c);
                }
                continue;
            }
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int columnWidth = 0;
            for (int s = 0; s < lp.span; s++) {
                columnWidth += constrained(c + s);
            }
            if (child.getVisibility() != GONE) {
                int cw = child.getMeasuredWidth();
                int ch = child.getMeasuredHeight();
                int gravity = lp.gravity < 0 ? getGravity() : lp.gravity;
                int top;
                switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
                    case Gravity.CENTER_VERTICAL:
                        top = mPaddingTop + (childSpace - ch) / 2 + lp.topMargin - lp.bottomMargin;
                        break;
                    case Gravity.BOTTOM:
                        top = height - mPaddingBottom - ch - lp.bottomMargin;
                        break;
                    default:
                        top = mPaddingTop + lp.topMargin;
                        break;
                }
                int left = rtl ? x - columnWidth + lp.leftMargin + (columnWidth - lp.leftMargin - lp.rightMargin - cw - lp.mOffset)
                        : x + lp.leftMargin + lp.mOffset;
                child.layout(left, top, left + cw, top + ch);
            }
            if (rtl) {
                x -= columnWidth;
            } else {
                x += columnWidth;
            }
            c += lp.span - 1;
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new TableRow.LayoutParams(getContext(), attrs);
    }

    @Override
    protected LinearLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof TableRow.LayoutParams;
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
        return TableRow.class.getName();
    }
}
