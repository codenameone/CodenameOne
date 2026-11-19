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

import java.util.ArrayList;

/// A compact flexbox engine: the subset of Yoga that rive-runtime's layouts
/// use (rive-runtime vendors Yoga; this is not a port of it). Supports
/// row/column (and reverse), wrap, justify start/center/end/space-between,
/// align items/self/content, grow/shrink/basis, point/percent/auto sizes
/// with min/max, padding/margin/border/gap, relative and absolute
/// positioning with insets, aspect ratio and display none. A leaf may
/// supply a [Measure] for its intrinsic size.
///
/// Sizes are passed down as definite values or NaN ("size to content").
/// Nothing is allocated while laying out.
public final class FlexNode {
    /// Intrinsic size of a leaf (text, a nested artboard, an image).
    public interface Measure {
        /// Writes width and height into `out`. Each mode is one of
        /// [#MEASURE_UNDEFINED] (the size is NaN), [#MEASURE_EXACTLY] or
        /// [#MEASURE_AT_MOST], as Yoga's measure function receives them.
        void measure(float width, int widthMode, float height, int heightMode, float[] out);
    }

    public static final int MEASURE_UNDEFINED = 0;
    public static final int MEASURE_EXACTLY = 1;
    public static final int MEASURE_AT_MOST = 2;

    public static final int UNIT_UNDEFINED = 0;
    public static final int UNIT_POINT = 1;
    public static final int UNIT_PERCENT = 2;
    public static final int UNIT_AUTO = 3;

    public static final int COLUMN = 0;
    public static final int COLUMN_REVERSE = 1;
    public static final int ROW = 2;
    public static final int ROW_REVERSE = 3;

    public static final int JUSTIFY_START = 0;
    public static final int JUSTIFY_CENTER = 1;
    public static final int JUSTIFY_END = 2;
    public static final int JUSTIFY_SPACE_BETWEEN = 3;

    public static final int ALIGN_AUTO = 0;
    public static final int ALIGN_START = 1;
    public static final int ALIGN_CENTER = 2;
    public static final int ALIGN_END = 3;
    public static final int ALIGN_STRETCH = 4;

    /// Edge indices.
    public static final int LEFT = 0;
    public static final int TOP = 1;
    public static final int RIGHT = 2;
    public static final int BOTTOM = 3;

    // ---- style ----
    public int direction = ROW;
    public boolean wrap;
    public boolean wrapReverse;
    public boolean rtl;
    public int justify = JUSTIFY_START;
    public int alignItems = ALIGN_STRETCH;
    public int alignContent = ALIGN_START;
    public int alignSelf = ALIGN_AUTO;
    public float grow;
    public float shrink;
    public float basis;
    public int basisUnit = UNIT_AUTO;
    public final float[] dim = new float[2];
    public final int[] dimUnit = {UNIT_AUTO, UNIT_AUTO};
    public final float[] min = new float[2];
    public final int[] minUnit = new int[2];
    public final float[] max = new float[2];
    public final int[] maxUnit = new int[2];
    public final float[] padding = new float[4];
    public final int[] paddingUnit = new int[4];
    public final float[] border = new float[4];
    public final float[] margin = new float[4];
    public final int[] marginUnit = new int[4];
    public final float[] inset = new float[4];
    public final int[] insetUnit = new int[4];
    public final float[] gap = new float[2];
    public final int[] gapUnit = new int[2];
    public boolean absolute;
    public boolean hidden;
    public float aspectRatio;
    public Measure measure;
    public final ArrayList<FlexNode> children = new ArrayList<FlexNode>();
    /// Free slot for the owner of the node.
    public Object owner;

    // ---- results (relative to the parent's border box) ----
    public float left;
    public float top;
    public float width;
    public float height;

    // ---- scratch ----
    private float tBasis;
    private float tMain;
    private float tCross;
    private int tLine;
    private boolean tFrozen;
    private final float[] tOut = new float[2];

    private static boolean ok(float v) {
        return !Float.isNaN(v);
    }

    private static float resolve(float v, int unit, float owner) {
        if (unit == UNIT_POINT) {
            return v;
        }
        if (unit == UNIT_PERCENT) {
            return ok(owner) ? v * owner / 100f : Float.NaN;
        }
        return Float.NaN;
    }

    private static float resolveEdge(float v, int unit, float owner) {
        float r = resolve(v, unit, owner);
        return ok(r) ? r : 0f;
    }

    private float pb(int edge, float ownerWidth) {
        return resolveEdge(padding[edge], paddingUnit[edge], ownerWidth) + border[edge];
    }

    private float marginOf(int edge, float ownerWidth) {
        return resolveEdge(margin[edge], marginUnit[edge], ownerWidth);
    }

    float resolveDim(int axis, float owner) {
        float v = resolve(dim[axis], dimUnit[axis], owner);
        return ok(v) && v < 0 ? 0 : v;
    }

    private float clamp(int axis, float value, float owner, float floor) {
        if (!ok(value)) {
            return value;
        }
        float v = value;
        float mx = resolve(max[axis], maxUnit[axis], owner);
        if (ok(mx) && v > mx) {
            v = mx;
        }
        float mn = resolve(min[axis], minUnit[axis], owner);
        if (ok(mn) && v < mn) {
            v = mn;
        }
        return v < floor ? floor : v;
    }

    /// Lays out the tree rooted here into a box of `availWidth` x
    /// `availHeight` (NaN for content sized axes).
    public void calculate(float availWidth, float availHeight) {
        float w = resolveDim(0, availWidth);
        float h = resolveDim(1, availHeight);
        if (!ok(w) && ok(h) && aspectRatio > 0) {
            w = h * aspectRatio;
        } else if (!ok(h) && ok(w) && aspectRatio > 0) {
            h = w / aspectRatio;
        }
        layout(clamp(0, w, availWidth, 0), clamp(1, h, availHeight, 0), availWidth, availHeight);
        left = 0;
        top = 0;
    }

    /// Sizes this node (a NaN axis sizes to content) and positions its
    /// children.
    void layout(float w, float h, float ownerW, float ownerH) {
        if (hidden) {
            width = 0;
            height = 0;
            return;
        }
        float pbL = pb(LEFT, ownerW);
        float pbR = pb(RIGHT, ownerW);
        float pbT = pb(TOP, ownerW);
        float pbB = pb(BOTTOM, ownerW);
        float pbH = pbL + pbR;
        float pbV = pbT + pbB;
        if (children.isEmpty()) {
            float mw = 0;
            float mh = 0;
            if (measure != null && (!ok(w) || !ok(h))) {
                // Yoga offers the owner's size as an at-most bound for an
                // axis that sizes to content.
                float mW = ok(w) ? w - pbH : ok(ownerW) ? ownerW - pbH : Float.NaN;
                float mH = ok(h) ? h - pbV : ok(ownerH) ? ownerH - pbV : Float.NaN;
                measure.measure(mW, ok(w) ? MEASURE_EXACTLY : ok(mW) ? MEASURE_AT_MOST : MEASURE_UNDEFINED,
                        mH, ok(h) ? MEASURE_EXACTLY : ok(mH) ? MEASURE_AT_MOST : MEASURE_UNDEFINED, tOut);
                mw = tOut[0];
                mh = tOut[1];
            }
            width = ok(w) ? w : clamp(0, mw + pbH, ownerW, pbH);
            height = ok(h) ? h : clamp(1, mh + pbV, ownerH, pbV);
            return;
        }
        boolean isRow = direction >= ROW;
        int m = isRow ? 0 : 1;
        int c = 1 - m;
        int msE = isRow ? LEFT : TOP;
        int meE = isRow ? RIGHT : BOTTOM;
        int csE = isRow ? TOP : LEFT;
        int ceE = isRow ? BOTTOM : RIGHT;
        float pbMain = isRow ? pbH : pbV;
        float pbCross = isRow ? pbV : pbH;
        float pbMainStart = isRow ? pbL : pbT;
        float pbCrossStart = isRow ? pbT : pbL;
        float size0 = isRow ? w : h;
        float size1 = isRow ? h : w;
        float innerMain = ok(size0) ? size0 - pbMain : Float.NaN;
        float innerCross = ok(size1) ? size1 - pbCross : Float.NaN;
        float innerW = ok(w) ? w - pbH : Float.NaN;
        float innerH = ok(h) ? h - pbV : Float.NaN;
        float gapMain = resolveEdge(gap[m], gapUnit[m], innerMain);
        float gapCross = resolveEdge(gap[c], gapUnit[c], innerCross);
        int count = children.size();

        // 1. Flex basis and hypothetical main size of every flow child.
        for (int i = 0; i < count; i++) {
            FlexNode ch = children.get(i);
            if (ch.hidden || ch.absolute) {
                if (ch.hidden) {
                    ch.width = 0;
                    ch.height = 0;
                }
                continue;
            }
            float b = resolve(ch.basis, ch.basisUnit, innerMain);
            if (!ok(b)) {
                b = ch.resolveDim(m, innerMain);
            }
            float crossDef = ch.resolveDim(c, innerCross);
            if (!ok(b) && ok(crossDef) && ch.aspectRatio > 0) {
                b = isRow ? crossDef * ch.aspectRatio : crossDef / ch.aspectRatio;
            }
            if (!ok(b)) {
                float cg = crossDef;
                if (!ok(cg) && !wrap && ok(innerCross) && ch.effectiveAlign(this) == ALIGN_STRETCH) {
                    cg = innerCross - ch.marginOf(csE, innerW) - ch.marginOf(ceE, innerW);
                }
                ch.layoutAxes(isRow, Float.NaN, ch.clamp(c, cg, innerCross, 0), innerW, innerH);
                b = isRow ? ch.width : ch.height;
            }
            ch.tBasis = b;
            ch.tMain = ch.clamp(m, b, innerMain, 0);
            ch.tFrozen = false;
        }

        // 2. Lines.
        int lines = 0;
        float lineUsed = 0;
        int inLine = 0;
        for (int i = 0; i < count; i++) {
            FlexNode ch = children.get(i);
            if (ch.hidden || ch.absolute) {
                continue;
            }
            float outer = ch.tMain + ch.marginOf(msE, innerW) + ch.marginOf(meE, innerW);
            if (wrap && inLine > 0 && ok(innerMain) && lineUsed + gapMain + outer > innerMain + 0.001f) {
                lines++;
                lineUsed = 0;
                inLine = 0;
            }
            lineUsed += (inLine > 0 ? gapMain : 0) + outer;
            inLine++;
            ch.tLine = lines;
        }
        int lineCount = lines + 1;

        // 3. Resolve flexible lengths per line, then hypothetical cross sizes.
        float maxLineMain = 0;
        float totalCross = 0;
        for (int l = 0; l < lineCount; l++) {
            resolveFlexible(l, m, msE, meE, innerMain, innerW, gapMain);
            float used = lineMainUsed(l, msE, meE, innerW, gapMain);
            if (used > maxLineMain) {
                maxLineMain = used;
            }
            float lc = 0;
            for (int i = 0; i < count; i++) {
                FlexNode ch = children.get(i);
                if (ch.hidden || ch.absolute || ch.tLine != l) {
                    continue;
                }
                float cd = ch.resolveDim(c, innerCross);
                if (!ok(cd) && ch.aspectRatio > 0) {
                    cd = isRow ? ch.tMain / ch.aspectRatio : ch.tMain * ch.aspectRatio;
                }
                if (!ok(cd)) {
                    ch.layoutAxes(isRow, ch.tMain, Float.NaN, innerW, innerH);
                    cd = isRow ? ch.height : ch.width;
                }
                ch.tCross = ch.clamp(c, cd, innerCross, 0);
                float outer = ch.tCross + ch.marginOf(csE, innerW) + ch.marginOf(ceE, innerW);
                if (outer > lc) {
                    lc = outer;
                }
            }
            totalCross += lc;
        }
        totalCross += gapCross * (lineCount - 1);

        // 4. Container size.
        if (!ok(size0)) {
            size0 = clamp(m, maxLineMain + pbMain, isRow ? ownerW : ownerH, pbMain);
            innerMain = size0 - pbMain;
            if (maxLineMain > innerMain + 0.001f || maxLineMain < innerMain - 0.001f) {
                for (int l = 0; l < lineCount; l++) {
                    resolveFlexible(l, m, msE, meE, innerMain, innerW, gapMain);
                }
            }
        }
        if (!ok(size1)) {
            size1 = clamp(c, totalCross + pbCross, isRow ? ownerH : ownerW, pbCross);
        }
        innerCross = size1 - pbCross;
        width = isRow ? size0 : size1;
        height = isRow ? size1 : size0;
        innerW = width - pbH;
        innerH = height - pbV;

        // 5. Lines in the cross axis (align-content), stretch, final layout
        // and positions.
        float freeCross = innerCross - totalCross;
        float crossPos = 0;
        if (lineCount == 1) {
            freeCross = 0;
        } else if (alignContent == ALIGN_CENTER) {
            crossPos = freeCross / 2;
        } else if (alignContent == ALIGN_END) {
            crossPos = freeCross;
        }
        boolean mirrorMain = direction == ROW_REVERSE || direction == COLUMN_REVERSE;
        if (isRow && rtl) {
            mirrorMain = !mirrorMain;
        }
        for (int l = 0; l < lineCount; l++) {
            float lineCross = lineCount == 1 ? innerCross : 0;
            if (lineCount > 1) {
                for (int i = 0; i < count; i++) {
                    FlexNode ch = children.get(i);
                    if (ch.hidden || ch.absolute || ch.tLine != l) {
                        continue;
                    }
                    float outer = ch.tCross + ch.marginOf(csE, innerW) + ch.marginOf(ceE, innerW);
                    if (outer > lineCross) {
                        lineCross = outer;
                    }
                }
                if (alignContent == ALIGN_STRETCH && freeCross > 0) {
                    lineCross += freeCross / lineCount;
                }
            }
            int n = 0;
            float used = 0;
            for (int i = 0; i < count; i++) {
                FlexNode ch = children.get(i);
                if (ch.hidden || ch.absolute || ch.tLine != l) {
                    continue;
                }
                float mcs = ch.marginOf(csE, innerW);
                float mce = ch.marginOf(ceE, innerW);
                int al = ch.effectiveAlign(this);
                if (al == ALIGN_STRETCH && !ok(ch.resolveDim(c, innerCross))) {
                    ch.tCross = ch.clamp(c, lineCross - mcs - mce, innerCross, 0);
                }
                ch.layoutAxes(isRow, ch.tMain, ch.tCross, innerW, innerH);
                ch.tMain = isRow ? ch.width : ch.height;
                ch.tCross = isRow ? ch.height : ch.width;
                used += (n > 0 ? gapMain : 0) + ch.tMain + ch.marginOf(msE, innerW) + ch.marginOf(meE, innerW);
                n++;
                float off = 0;
                float slack = lineCross - ch.tCross - mcs - mce;
                if (al == ALIGN_CENTER) {
                    off = slack / 2;
                } else if (al == ALIGN_END) {
                    off = slack;
                }
                float cp = crossPos + off + mcs;
                if (wrapReverse) {
                    cp = innerCross - cp - ch.tCross;
                }
                ch.setCross(isRow, pbCrossStart + cp);
            }
            float free = innerMain - used;
            float pos = 0;
            float between = 0;
            if (justify == JUSTIFY_CENTER) {
                pos = free / 2;
            } else if (justify == JUSTIFY_END) {
                pos = free;
            } else if (justify == JUSTIFY_SPACE_BETWEEN && n > 1 && free > 0) {
                between = free / (n - 1);
            }
            for (int i = 0; i < count; i++) {
                FlexNode ch = children.get(i);
                if (ch.hidden || ch.absolute || ch.tLine != l) {
                    continue;
                }
                pos += ch.marginOf(msE, innerW);
                float p = mirrorMain ? innerMain - pos - ch.tMain : pos;
                ch.setMain(isRow, pbMainStart + p);
                pos += ch.tMain + ch.marginOf(meE, innerW) + gapMain + between;
            }
            crossPos += lineCross + gapCross;
        }

        // 6. Absolutely positioned children, inside the padding box.
        float cbW = width - border[LEFT] - border[RIGHT];
        float cbH = height - border[TOP] - border[BOTTOM];
        for (int i = 0; i < count; i++) {
            FlexNode ch = children.get(i);
            if (ch.hidden || !ch.absolute) {
                continue;
            }
            layoutAbsolute(ch, isRow, cbW, cbH, innerW, innerH, pbL, pbT);
        }
    }

    private void layoutAbsolute(FlexNode ch, boolean isRow, float cbW, float cbH,
            float innerW, float innerH, float pbL, float pbT) {
        float il = resolve(ch.inset[LEFT], ch.insetUnit[LEFT], cbW);
        float ir = resolve(ch.inset[RIGHT], ch.insetUnit[RIGHT], cbW);
        float it = resolve(ch.inset[TOP], ch.insetUnit[TOP], cbH);
        float ib = resolve(ch.inset[BOTTOM], ch.insetUnit[BOTTOM], cbH);
        float ml = ch.marginOf(LEFT, cbW);
        float mr = ch.marginOf(RIGHT, cbW);
        float mt = ch.marginOf(TOP, cbW);
        float mb = ch.marginOf(BOTTOM, cbW);
        float cw = ch.resolveDim(0, cbW);
        float chh = ch.resolveDim(1, cbH);
        if (!ok(cw) && ok(il) && ok(ir)) {
            cw = cbW - il - ir - ml - mr;
        }
        if (!ok(chh) && ok(it) && ok(ib)) {
            chh = cbH - it - ib - mt - mb;
        }
        if (!ok(cw) && ok(chh) && ch.aspectRatio > 0) {
            cw = chh * ch.aspectRatio;
        } else if (!ok(chh) && ok(cw) && ch.aspectRatio > 0) {
            chh = cw / ch.aspectRatio;
        }
        ch.layout(ch.clamp(0, cw, cbW, 0), ch.clamp(1, chh, cbH, 0), cbW, cbH);
        float x;
        if (ok(il)) {
            x = border[LEFT] + il + ml;
        } else if (ok(ir)) {
            x = width - border[RIGHT] - ir - mr - ch.width;
        } else {
            x = pbL + ml + alignOffset(isRow ? justifyAsAlign() : ch.effectiveAlign(this),
                    innerW - ch.width - ml - mr);
        }
        float y;
        if (ok(it)) {
            y = border[TOP] + it + mt;
        } else if (ok(ib)) {
            y = height - border[BOTTOM] - ib - mb - ch.height;
        } else {
            y = pbT + mt + alignOffset(isRow ? ch.effectiveAlign(this) : justifyAsAlign(),
                    innerH - ch.height - mt - mb);
        }
        ch.left = x;
        ch.top = y;
    }

    private int justifyAsAlign() {
        if (justify == JUSTIFY_CENTER) {
            return ALIGN_CENTER;
        }
        return justify == JUSTIFY_END ? ALIGN_END : ALIGN_START;
    }

    private static float alignOffset(int align, float slack) {
        if (align == ALIGN_CENTER) {
            return slack / 2;
        }
        return align == ALIGN_END ? slack : 0;
    }

    int effectiveAlign(FlexNode parent) {
        return alignSelf == ALIGN_AUTO ? parent.alignItems : alignSelf;
    }

    private void layoutAxes(boolean isRow, float main, float cross, float ownerW, float ownerH) {
        if (isRow) {
            layout(main, cross, ownerW, ownerH);
        } else {
            layout(cross, main, ownerW, ownerH);
        }
    }

    private void setMain(boolean isRow, float v) {
        if (isRow) {
            left = v;
        } else {
            top = v;
        }
    }

    private void setCross(boolean isRow, float v) {
        if (isRow) {
            top = v;
        } else {
            left = v;
        }
    }

    private float lineMainUsed(int l, int msE, int meE, float innerW, float gapMain) {
        float used = 0;
        int n = 0;
        for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            FlexNode ch = children.get(i);
            if (ch.hidden || ch.absolute || ch.tLine != l) {
                continue;
            }
            used += (n > 0 ? gapMain : 0) + ch.tMain + ch.marginOf(msE, innerW) + ch.marginOf(meE, innerW);
            n++;
        }
        return used;
    }

    /// Grows or shrinks the children of line `l` to fill `innerMain`,
    /// freezing children that hit their min or max and redistributing.
    private void resolveFlexible(int l, int m, int msE, int meE, float innerMain, float innerW, float gapMain) {
        if (!ok(innerMain)) {
            return;
        }
        int count = children.size();
        for (int i = 0; i < count; i++) {
            FlexNode ch = children.get(i);
            if (!ch.hidden && !ch.absolute && ch.tLine == l) {
                ch.tFrozen = false;
                ch.tMain = ch.clamp(m, ch.tBasis, innerMain, 0);
            }
        }
        for (int pass = 0; pass < 8; pass++) {
            float used = 0;
            float factors = 0;
            int n = 0;
            for (int i = 0; i < count; i++) {
                FlexNode ch = children.get(i);
                if (ch.hidden || ch.absolute || ch.tLine != l) {
                    continue;
                }
                used += (n > 0 ? gapMain : 0) + ch.marginOf(msE, innerW) + ch.marginOf(meE, innerW)
                        + (ch.tFrozen ? ch.tMain : ch.tBasis);
                n++;
            }
            float free = innerMain - used;
            boolean growing = free > 0;
            for (int i = 0; i < count; i++) {
                FlexNode ch = children.get(i);
                if (ch.hidden || ch.absolute || ch.tLine != l || ch.tFrozen) {
                    continue;
                }
                factors += growing ? ch.grow : ch.shrink * ch.tBasis;
            }
            if (factors <= 0 || free == 0) {
                return;
            }
            boolean violated = false;
            for (int i = 0; i < count; i++) {
                FlexNode ch = children.get(i);
                if (ch.hidden || ch.absolute || ch.tLine != l || ch.tFrozen) {
                    continue;
                }
                float f = growing ? ch.grow : ch.shrink * ch.tBasis;
                float target = ch.tBasis + free * f / factors;
                float cl = ch.clamp(m, target, innerMain, 0);
                if (Float.floatToIntBits(cl) != Float.floatToIntBits(target)) {
                    ch.tFrozen = true;
                    violated = true;
                }
                ch.tMain = cl;
            }
            if (!violated) {
                return;
            }
        }
    }
}
