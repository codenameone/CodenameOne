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

/// Places children in a rectangular grid of rows and columns. Cells are
/// assigned as Android assigns them -- explicit `layout_row` / `layout_column`
/// first, otherwise the next free cell in the orientation's order, wrapping
/// at `columnCount` (or `rowCount`). Each line is as big as its largest
/// single-span child, spanning children widen their last line when needed,
/// and spare space goes to weighted lines, or else to the last line whose
/// children declare a gravity on that axis, as Android's stretching does in
/// the common cases.
public class GridLayout extends ViewGroup {

    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;
    public static final int VERTICAL = LinearLayout.VERTICAL;
    public static final int UNDEFINED = Integer.MIN_VALUE;
    public static final int ALIGN_BOUNDS = 0;
    public static final int ALIGN_MARGINS = 1;

    /// Where a child sits within its cell along one axis.
    public abstract static class Alignment {
        final int code;

        Alignment(int code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return "Alignment(" + code + ")";
        }
    }

    private static final class Fixed extends Alignment {
        Fixed(int code) {
            super(code);
        }
    }

    static final int A_UNDEFINED = 0;
    static final int A_LEADING = 1;
    static final int A_TRAILING = 2;
    static final int A_CENTER = 3;
    static final int A_FILL = 4;
    static final int A_BASELINE = 5;

    static final Alignment UNDEFINED_ALIGNMENT = new Fixed(A_UNDEFINED);
    public static final Alignment TOP = new Fixed(A_LEADING);
    public static final Alignment BOTTOM = new Fixed(A_TRAILING);
    public static final Alignment START = new Fixed(A_LEADING);
    public static final Alignment END = new Fixed(A_TRAILING);
    public static final Alignment LEFT = new Fixed(A_LEADING);
    public static final Alignment RIGHT = new Fixed(A_TRAILING);
    public static final Alignment CENTER = new Fixed(A_CENTER);
    public static final Alignment BASELINE = new Fixed(A_BASELINE);
    public static final Alignment FILL = new Fixed(A_FILL);

    /// A child's cells along one axis: a start line (or [#UNDEFINED]), a span,
    /// an alignment and a weight.
    public static class Spec {
        final int start;
        final int size;
        final Alignment alignment;
        final float weight;

        Spec(int start, int size, Alignment alignment, float weight) {
            this.start = start;
            this.size = Math.max(1, size);
            this.alignment = alignment == null ? UNDEFINED_ALIGNMENT : alignment;
            this.weight = weight;
        }

        Spec copyWithStart(int s) {
            return new Spec(s, size, alignment, weight);
        }

        Spec copyWithAlignment(Alignment a) {
            return new Spec(start, size, a, weight);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Spec)) {
                return false;
            }
            Spec s = (Spec) o;
            return s.start == start && s.size == size && s.alignment == alignment && s.weight == weight;
        }

        @Override
        public int hashCode() {
            return start * 31 + size * 7 + alignment.hashCode();
        }
    }

    public static Spec spec(int start, int size, Alignment alignment, float weight) {
        return new Spec(start, size, alignment, weight);
    }

    public static Spec spec(int start, Alignment alignment, float weight) {
        return spec(start, 1, alignment, weight);
    }

    public static Spec spec(int start, int size, float weight) {
        return spec(start, size, UNDEFINED_ALIGNMENT, weight);
    }

    public static Spec spec(int start, float weight) {
        return spec(start, 1, weight);
    }

    public static Spec spec(int start, int size, Alignment alignment) {
        return spec(start, size, alignment, 0);
    }

    public static Spec spec(int start, Alignment alignment) {
        return spec(start, 1, alignment);
    }

    public static Spec spec(int start, int size) {
        return spec(start, size, UNDEFINED_ALIGNMENT);
    }

    public static Spec spec(int start) {
        return spec(start, 1);
    }

    /// Row and column specs of one child.
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public Spec rowSpec = spec(UNDEFINED);
        public Spec columnSpec = spec(UNDEFINED);

        public LayoutParams(Spec rowSpec, Spec columnSpec) {
            super(WRAP_CONTENT, WRAP_CONTENT);
            this.rowSpec = rowSpec;
            this.columnSpec = columnSpec;
        }

        public LayoutParams() {
            this(spec(UNDEFINED), spec(UNDEFINED));
        }

        public LayoutParams(ViewGroup.LayoutParams params) {
            super(params);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams params) {
            super(params);
        }

        public LayoutParams(LayoutParams source) {
            super((ViewGroup.MarginLayoutParams) source);
            this.rowSpec = source.rowSpec;
            this.columnSpec = source.columnSpec;
        }

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
            TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.GridLayout_Layout);
            int gravity = a.getInt(android.R.styleable.GridLayout_Layout_layout_gravity, Gravity.NO_GRAVITY);
            int column = a.getInt(android.R.styleable.GridLayout_Layout_layout_column, UNDEFINED);
            int colSpan = a.getInt(android.R.styleable.GridLayout_Layout_layout_columnSpan, 1);
            float colWeight = a.getFloat(android.R.styleable.GridLayout_Layout_layout_columnWeight, 0);
            columnSpec = spec(column, colSpan, alignmentFor(gravity, true), colWeight);
            int row = a.getInt(android.R.styleable.GridLayout_Layout_layout_row, UNDEFINED);
            int rowSpan = a.getInt(android.R.styleable.GridLayout_Layout_layout_rowSpan, 1);
            float rowWeight = a.getFloat(android.R.styleable.GridLayout_Layout_layout_rowWeight, 0);
            rowSpec = spec(row, rowSpan, alignmentFor(gravity, false), rowWeight);
            a.recycle();
        }

        @Override
        protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr) {
            width = a.hasValue(widthAttr) ? a.getLayoutDimension(widthAttr, "layout_width") : WRAP_CONTENT;
            height = a.hasValue(heightAttr) ? a.getLayoutDimension(heightAttr, "layout_height") : WRAP_CONTENT;
        }

        public void setGravity(int gravity) {
            rowSpec = rowSpec.copyWithAlignment(alignmentFor(gravity, false));
            columnSpec = columnSpec.copyWithAlignment(alignmentFor(gravity, true));
        }
    }

    static Alignment alignmentFor(int gravity, boolean horizontal) {
        int mask = horizontal ? Gravity.HORIZONTAL_GRAVITY_MASK : Gravity.VERTICAL_GRAVITY_MASK;
        int shift = horizontal ? Gravity.AXIS_X_SHIFT : Gravity.AXIS_Y_SHIFT;
        int flags = (gravity & mask) >> shift;
        switch (flags) {
            case (Gravity.AXIS_SPECIFIED | Gravity.AXIS_PULL_BEFORE):
                return horizontal ? LEFT : TOP;
            case (Gravity.AXIS_SPECIFIED | Gravity.AXIS_PULL_AFTER):
                return horizontal ? RIGHT : BOTTOM;
            case (Gravity.AXIS_SPECIFIED | Gravity.AXIS_PULL_BEFORE | Gravity.AXIS_PULL_AFTER):
                return FILL;
            case Gravity.AXIS_SPECIFIED:
                return CENTER;
            default:
                return UNDEFINED_ALIGNMENT;
        }
    }

    private int mOrientation = HORIZONTAL;
    private int mRowCount = UNDEFINED;
    private int mColumnCount = UNDEFINED;
    private boolean mUseDefaultMargins;
    private int mAlignmentMode = ALIGN_MARGINS;
    private int mDefaultGap;
    private int[] mColumnSizes = new int[0];
    private int[] mRowSizes = new int[0];

    public GridLayout(Context context) {
        this(context, null);
    }

    public GridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GridLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mDefaultGap = Math.round(8 * context.getResources().getDisplayMetrics().density);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.GridLayout, defStyleAttr, defStyleRes);
        mRowCount = a.getInt(android.R.styleable.GridLayout_rowCount, UNDEFINED);
        mColumnCount = a.getInt(android.R.styleable.GridLayout_columnCount, UNDEFINED);
        mOrientation = a.getInt(android.R.styleable.GridLayout_orientation, HORIZONTAL);
        mUseDefaultMargins = a.getBoolean(android.R.styleable.GridLayout_useDefaultMargins, false);
        mAlignmentMode = a.getInt(android.R.styleable.GridLayout_alignmentMode, ALIGN_MARGINS);
        a.recycle();
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
        requestLayout();
    }

    public int getRowCount() {
        return mRowSizes.length > 0 ? mRowSizes.length : mRowCount;
    }

    public void setRowCount(int rowCount) {
        mRowCount = rowCount;
        requestLayout();
    }

    public int getColumnCount() {
        return mColumnSizes.length > 0 ? mColumnSizes.length : mColumnCount;
    }

    public void setColumnCount(int columnCount) {
        mColumnCount = columnCount;
        requestLayout();
    }

    public boolean getUseDefaultMargins() {
        return mUseDefaultMargins;
    }

    public void setUseDefaultMargins(boolean useDefaultMargins) {
        mUseDefaultMargins = useDefaultMargins;
        requestLayout();
    }

    public int getAlignmentMode() {
        return mAlignmentMode;
    }

    public void setAlignmentMode(int alignmentMode) {
        mAlignmentMode = alignmentMode;
        requestLayout();
    }

    public void setRowOrderPreserved(boolean rowOrderPreserved) {
    }

    public void setColumnOrderPreserved(boolean columnOrderPreserved) {
    }

    public boolean isRowOrderPreserved() {
        return true;
    }

    public boolean isColumnOrderPreserved() {
        return true;
    }

    // ------------------------------------------------------------ cell placement

    /// Resolved cell of each child: row, column (and spans in the specs).
    private int[] mRowOf = new int[0];
    private int[] mColOf = new int[0];

    private static boolean fits(int[] a, int value, int start, int end) {
        if (end > a.length) {
            return false;
        }
        for (int i = start; i < end; i++) {
            if (a[i] > value) {
                return false;
            }
        }
        return true;
    }

    private static void procrusteanFill(int[] a, int start, int end, int value) {
        int length = a.length;
        for (int i = Math.min(start, length); i < Math.min(end, length); i++) {
            a[i] = value;
        }
    }

    private void placeChildren() {
        int n = getChildCount();
        mRowOf = new int[n];
        mColOf = new int[n];
        final boolean horizontal = mOrientation == HORIZONTAL;
        final int count = horizontal ? mColumnCount : mRowCount;
        final int limit = count == UNDEFINED ? 0 : count;
        int major = 0;
        int minor = 0;
        int[] maxSizes = new int[limit];
        for (int i = 0; i < n; i++) {
            LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
            Spec majorSpec = horizontal ? lp.rowSpec : lp.columnSpec;
            Spec minorSpec = horizontal ? lp.columnSpec : lp.rowSpec;
            boolean majorWasDefined = majorSpec.start != UNDEFINED;
            boolean minorWasDefined = minorSpec.start != UNDEFINED;
            int majorSpan = majorSpec.size;
            int minorSpan = minorSpec.size;
            if (majorWasDefined) {
                major = majorSpec.start;
            }
            if (minorWasDefined) {
                minor = minorSpec.start;
            }
            if (limit != 0) {
                if (!majorWasDefined || !minorWasDefined) {
                    int guard = 0;
                    while (!fits(maxSizes, major, minor, minor + minorSpan) && guard++ < 100000) {
                        if (minorWasDefined) {
                            major++;
                        } else {
                            if (minor + minorSpan <= limit) {
                                minor++;
                            } else {
                                minor = 0;
                                major++;
                            }
                        }
                    }
                }
                procrusteanFill(maxSizes, minor, minor + minorSpan, major + majorSpan);
            }
            if (horizontal) {
                mRowOf[i] = major;
                mColOf[i] = minor;
            } else {
                mRowOf[i] = minor;
                mColOf[i] = major;
            }
            minor = minor + minorSpan;
        }
    }

    // ------------------------------------------------------------ margins

    private int margin(View c, LayoutParams lp, boolean horizontal, boolean leading, int index, int span, int lines) {
        int m = horizontal ? (leading ? lp.leftMargin : lp.rightMargin) : (leading ? lp.topMargin : lp.bottomMargin);
        if (m != 0 || !mUseDefaultMargins || c instanceof Space) {
            return m;
        }
        boolean atEdge = leading ? index == 0 : index + span >= lines;
        return atEdge ? 0 : mDefaultGap / 2;
    }

    // ------------------------------------------------------------ measure

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        placeChildren();
        int n = getChildCount();
        int columns = mColumnCount == UNDEFINED ? 0 : mColumnCount;
        int rows = mRowCount == UNDEFINED ? 0 : mRowCount;
        for (int i = 0; i < n; i++) {
            LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
            columns = Math.max(columns, mColOf[i] + lp.columnSpec.size);
            rows = Math.max(rows, mRowOf[i] + lp.rowSpec.size);
        }
        int hPad = mPaddingLeft + mPaddingRight;
        int vPad = mPaddingTop + mPaddingBottom;
        // First pass: children at their natural size.
        for (int i = 0; i < n; i++) {
            View c = getChildAt(i);
            if (c.getVisibility() == GONE) {
                continue;
            }
            LayoutParams lp = (LayoutParams) c.getLayoutParams();
            int mh = margin(c, lp, true, true, mColOf[i], lp.columnSpec.size, columns)
                    + margin(c, lp, true, false, mColOf[i], lp.columnSpec.size, columns);
            int mv = margin(c, lp, false, true, mRowOf[i], lp.rowSpec.size, rows)
                    + margin(c, lp, false, false, mRowOf[i], lp.rowSpec.size, rows);
            int cw = getChildMeasureSpec(widthSpec, hPad + mh, lp.width == LayoutParams.MATCH_PARENT ? LayoutParams.WRAP_CONTENT : lp.width);
            int ch = getChildMeasureSpec(heightSpec, vPad + mv, lp.height == LayoutParams.MATCH_PARENT ? LayoutParams.WRAP_CONTENT : lp.height);
            c.measure(cw, ch);
        }
        mColumnSizes = computeSizes(true, columns, widthSpec, hPad);
        mRowSizes = computeSizes(false, rows, heightSpec, vPad);
        // Second pass: children that stretch with a weighted line, or that
        // match_parent, take their cell's size.
        for (int i = 0; i < n; i++) {
            View c = getChildAt(i);
            if (c.getVisibility() == GONE) {
                continue;
            }
            LayoutParams lp = (LayoutParams) c.getLayoutParams();
            boolean fillW = lp.columnSpec.weight > 0 || lp.width == LayoutParams.MATCH_PARENT;
            boolean fillH = lp.rowSpec.weight > 0 || lp.height == LayoutParams.MATCH_PARENT;
            if (fillW || fillH) {
                int cellW = sum(mColumnSizes, mColOf[i], lp.columnSpec.size)
                        - margin(c, lp, true, true, mColOf[i], lp.columnSpec.size, columns)
                        - margin(c, lp, true, false, mColOf[i], lp.columnSpec.size, columns);
                int cellH = sum(mRowSizes, mRowOf[i], lp.rowSpec.size)
                        - margin(c, lp, false, true, mRowOf[i], lp.rowSpec.size, rows)
                        - margin(c, lp, false, false, mRowOf[i], lp.rowSpec.size, rows);
                c.measure(fillW ? MeasureSpec.makeMeasureSpec(Math.max(0, cellW), MeasureSpec.EXACTLY)
                                : MeasureSpec.makeMeasureSpec(c.getMeasuredWidth(), MeasureSpec.EXACTLY),
                        fillH ? MeasureSpec.makeMeasureSpec(Math.max(0, cellH), MeasureSpec.EXACTLY)
                                : MeasureSpec.makeMeasureSpec(c.getMeasuredHeight(), MeasureSpec.EXACTLY));
            }
        }
        int width = sum(mColumnSizes, 0, mColumnSizes.length) + hPad;
        int height = sum(mRowSizes, 0, mRowSizes.length) + vPad;
        width = Math.max(width, getSuggestedMinimumWidth());
        height = Math.max(height, getSuggestedMinimumHeight());
        setMeasuredDimension(resolveSizeAndState(width, widthSpec, 0), resolveSizeAndState(height, heightSpec, 0));
    }

    private static int sum(int[] a, int start, int span) {
        int s = 0;
        for (int i = start; i < Math.min(a.length, start + span); i++) {
            s += a[i];
        }
        return s;
    }

    /// Line sizes along one axis: the largest single-span child per line,
    /// spanning children widening their last line, then spare space to
    /// weighted (or else stretchable) lines when the parent size is exact.
    private int[] computeSizes(boolean horizontal, int lines, int spec, int padding) {
        int[] sizes = new int[lines];
        float[] weights = new float[lines];
        boolean[] flexible = new boolean[lines];
        int n = getChildCount();
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < n; i++) {
                View c = getChildAt(i);
                if (c.getVisibility() == GONE) {
                    continue;
                }
                LayoutParams lp = (LayoutParams) c.getLayoutParams();
                Spec s = horizontal ? lp.columnSpec : lp.rowSpec;
                int start = horizontal ? mColOf[i] : mRowOf[i];
                int size = (horizontal ? c.getMeasuredWidth() : c.getMeasuredHeight())
                        + margin(c, lp, horizontal, true, start, s.size, lines)
                        + margin(c, lp, horizontal, false, start, s.size, lines);
                if (pass == 0 && s.size == 1) {
                    sizes[start] = Math.max(sizes[start], size);
                    weights[start] = Math.max(weights[start], s.weight);
                    if (s.alignment != UNDEFINED_ALIGNMENT) {
                        flexible[start] = true;
                    }
                } else if (pass == 1 && s.size > 1) {
                    int have = sum(sizes, start, s.size);
                    if (have < size) {
                        sizes[Math.min(lines - 1, start + s.size - 1)] += size - have;
                    }
                    if (s.weight > 0) {
                        for (int k = start; k < Math.min(lines, start + s.size); k++) {
                            weights[k] = Math.max(weights[k], s.weight / s.size);
                        }
                    }
                }
            }
        }
        if (MeasureSpec.getMode(spec) == MeasureSpec.EXACTLY) {
            int delta = MeasureSpec.getSize(spec) - padding - sum(sizes, 0, lines);
            if (delta > 0) {
                float totalWeight = 0;
                for (float w : weights) {
                    totalWeight += w;
                }
                if (totalWeight > 0) {
                    int remaining = delta;
                    float remainingWeight = totalWeight;
                    for (int k = 0; k < lines; k++) {
                        if (weights[k] > 0) {
                            int share = (int) (weights[k] * remaining / remainingWeight);
                            sizes[k] += share;
                            remaining -= share;
                            remainingWeight -= weights[k];
                        }
                    }
                } else {
                    for (int k = lines - 1; k >= 0; k--) {
                        if (flexible[k]) {
                            sizes[k] += delta;
                            break;
                        }
                    }
                }
            }
        }
        return sizes;
    }

    // ------------------------------------------------------------ layout

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int n = getChildCount();
        if (mRowOf.length != n) {
            return;
        }
        boolean rtl = isLayoutRtl();
        int width = r - l;
        int columns = mColumnSizes.length;
        int rows = mRowSizes.length;
        for (int i = 0; i < n; i++) {
            View c = getChildAt(i);
            if (c.getVisibility() == GONE) {
                continue;
            }
            LayoutParams lp = (LayoutParams) c.getLayoutParams();
            int col = mColOf[i];
            int row = mRowOf[i];
            int x0 = mPaddingLeft + sum(mColumnSizes, 0, col);
            int cellW = sum(mColumnSizes, col, lp.columnSpec.size);
            int y0 = mPaddingTop + sum(mRowSizes, 0, row);
            int cellH = sum(mRowSizes, row, lp.rowSpec.size);
            int ml = margin(c, lp, true, true, col, lp.columnSpec.size, columns);
            int mr = margin(c, lp, true, false, col, lp.columnSpec.size, columns);
            int mt = margin(c, lp, false, true, row, lp.rowSpec.size, rows);
            int mb = margin(c, lp, false, false, row, lp.rowSpec.size, rows);
            int areaW = cellW - ml - mr;
            int areaH = cellH - mt - mb;
            int cw = c.getMeasuredWidth();
            int ch = c.getMeasuredHeight();
            int dx = offset(lp.columnSpec.alignment, areaW, cw);
            int dy = offset(lp.rowSpec.alignment, areaH, ch);
            if (lp.columnSpec.alignment == FILL) {
                cw = Math.max(0, areaW);
            }
            if (lp.rowSpec.alignment == FILL) {
                ch = Math.max(0, areaH);
            }
            if (cw != c.getMeasuredWidth() || ch != c.getMeasuredHeight()) {
                c.measure(MeasureSpec.makeMeasureSpec(cw, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(ch, MeasureSpec.EXACTLY));
            }
            int cx = x0 + ml + dx;
            int cy = y0 + mt + dy;
            if (rtl) {
                cx = width - cx - cw;
            }
            c.layout(cx, cy, cx + cw, cy + ch);
        }
    }

    private static int offset(Alignment a, int area, int size) {
        switch (a.code) {
            case A_TRAILING:
                return area - size;
            case A_CENTER:
                return (area - size) / 2;
            default:
                return 0;
        }
    }

    // ------------------------------------------------------------ params

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) lp);
        } else if (lp instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) lp);
        }
        return new LayoutParams(lp);
    }

    public CharSequence getAccessibilityClassName() {
        return GridLayout.class.getName();
    }
}
