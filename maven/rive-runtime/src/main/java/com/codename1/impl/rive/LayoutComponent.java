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

/// Port of `LayoutComponent`: a flex container sized and placed by the
/// artboard's layout pass ([FlexNode] stands in for the Yoga rive-runtime
/// vendors). Its fills and strokes paint a (rounded) rectangle behind its
/// children and it can clip them.
public class LayoutComponent extends LayoutComponentBase implements FlexNode.Measure {
    private static final float KAPPA = 0.5522848f;
    static final int SCALE_FIXED = 0;
    static final int SCALE_FILL = 1;
    static final int SCALE_HUG = 2;
    private static final int ANIMATION_INHERIT = 1;
    private static final int ANIMATION_CUSTOM = 2;
    private static final int INTERPOLATION_LINEAR = 1;
    private final float[] measured = new float[2];
    private LayoutComponent layoutParent;
    private NestedArtboardLayout hostedBy;
    private KeyFrameInterpolator interpolator;
    private boolean laidOut;
    private boolean animating;
    private float animElapsed;
    private final float[] animFrom = new float[4];
    private final float[] animTo = new float[4];
    protected final FlexNode flex = new FlexNode();
    private LayoutComponentStyle style;
    private final ArrayList<ShapePaint> layoutPaints = new ArrayList<ShapePaint>();
    private final ArrayList<LayoutComponent> layoutRoots = new ArrayList<LayoutComponent>();
    private final RivePath backgroundPath = new RivePath();
    private final RivePath worldBackground = new RivePath();
    private final Mat2D translation = new Mat2D();
    private final float[] pathKey = new float[6];
    private float layoutLeft;
    private float layoutTop;
    private float layoutWidth = Float.NaN;
    private float layoutHeight = Float.NaN;
    private boolean clipSaved;
    private boolean layoutHidden;

    /// Width computed by the last layout pass (the stored width before one).
    public float getLayoutWidth() {
        return Float.isNaN(layoutWidth) ? mWidth : layoutWidth;
    }

    /// Height computed by the last layout pass (the stored height before one).
    public float getLayoutHeight() {
        return Float.isNaN(layoutHeight) ? mHeight : layoutHeight;
    }

    /// X of this layout inside its parent layout's box.
    public float getLayoutX() {
        return layoutLeft;
    }

    /// Y of this layout inside its parent layout's box.
    public float getLayoutY() {
        return layoutTop;
    }

    public LayoutComponentStyle getStyle() {
        return style;
    }

    void addLayoutPaint(ShapePaint p) {
        layoutPaints.add(p);
    }

    @Override
    public int onAddedDirty(Artboard context) {
        int code = super.onAddedDirty(context);
        Core s = context.resolve(mStyleId);
        if (s instanceof LayoutComponentStyle) {
            style = (LayoutComponentStyle) s;
            Core i = context.resolve(style.mInterpolatorId);
            if (i instanceof KeyFrameInterpolator) {
                interpolator = (KeyFrameInterpolator) i;
            }
        }
        flex.owner = this;
        return code;
    }

    /// Wires the flex tree of an artboard (called on the artboard once its
    /// objects are added) and slots a background proxy into the draw list
    /// behind each layout's children.
    protected final void prepareLayout(ArrayList<Core> objects, ArrayList<Drawable> drawables) {
        for (int i = 0; i < objects.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            Core o = objects.get(i);
            if (o instanceof NestedArtboardLayout) {
                // Port of NestedArtboardLayout as a layout node provider: the
                // nested artboard instance is laid out as a child here.
                NestedArtboardLayout host = (NestedArtboardLayout) o;
                Artboard inst = host.getArtboardInstance();
                LayoutComponent p = layoutAncestor(host);
                if (inst != null && p != null) {
                    p.flex.children.add(inst.flex);
                    ((LayoutComponent) inst).hostedBy = host;
                    ((LayoutComponent) inst).layoutParent = p;
                    host.setHostedLayout(inst);
                }
                continue;
            }
            if (!(o instanceof LayoutComponent)) {
                continue;
            }
            LayoutComponent lc = (LayoutComponent) o;
            LayoutComponent p = o instanceof Artboard ? null : layoutAncestor(lc);
            if (p != null) {
                p.flex.children.add(lc.flex);
                lc.layoutParent = p;
            } else {
                layoutRoots.add(lc);
            }
        }
        for (int i = 0; i < drawables.size(); i++) {
            Drawable d = drawables.get(i);
            if (!(d instanceof LayoutComponent) || d instanceof Artboard) {
                continue;
            }
            int last = i;
            for (int j = i + 1; j < drawables.size(); j++) {
                if (isDescendant(drawables.get(j), (LayoutComponent) d)) {
                    last = j;
                }
            }
            drawables.add(last + 1, new BackgroundProxy((LayoutComponent) d));
        }
    }

    /// The layout a component joins: its parent, looking through plain
    /// groups (upstream's transparent layout containers).
    private static LayoutComponent layoutAncestor(Component c) {
        for (ContainerComponent q = c.getParent(); q != null; q = q.getParent()) {
            if (q instanceof LayoutComponent) {
                return (LayoutComponent) q;
            }
            if (q.coreType() != NodeBase.TYPE_KEY) {
                return null;
            }
        }
        return null;
    }

    private static boolean isDescendant(Drawable d, LayoutComponent of) {
        Component c = d instanceof BackgroundProxy ? ((BackgroundProxy) d).host : d;
        for (ContainerComponent p = c.getParent(); p != null; p = p.getParent()) {
            if (p == of) { // NOPMD CompareObjectsWithEquals - identity is the intent
                return true;
            }
        }
        return false;
    }

    /// Runs flex layout over every layout tree of this artboard and moves
    /// the layouts whose box changed; called before the component update.
    protected final void updateLayout() {
        updateLayout(0);
    }

    /// Lays out every root and advances layout animations by `elapsed`
    /// seconds. An artboard hosted by a [NestedArtboardLayout] is laid out by
    /// its host instead.
    protected final void updateLayout(float elapsed) {
        if (hostedBy != null) {
            return;
        }
        for (int i = 0; i < layoutRoots.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            LayoutComponent root = layoutRoots.get(i);
            root.syncTree(false, false, false);
            root.flex.calculate(Float.NaN, Float.NaN);
            if (!(root instanceof Artboard)) {
                root.flex.left = root.getX();
                root.flex.top = root.getY();
            }
            root.applyTree(elapsed);
        }
    }

    private void syncTree(boolean parentIsRow, boolean hasLayoutParent, boolean parentRtl) {
        syncStyle(parentIsRow, hasLayoutParent, parentRtl);
        boolean isRow = flex.direction >= FlexNode.ROW;
        ArrayList<FlexNode> kids = flex.children;
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            ((LayoutComponent) kids.get(i).owner).syncTree(isRow, true, flex.rtl);
        }
    }

    private void applyTree(float elapsed) {
        float[] to = animTo;
        if (Float.floatToIntBits(flex.left) != Float.floatToIntBits(to[0])
                || Float.floatToIntBits(flex.top) != Float.floatToIntBits(to[1])
                || Float.floatToIntBits(flex.width) != Float.floatToIntBits(to[2])
                || Float.floatToIntBits(flex.height) != Float.floatToIntBits(to[3])) {
            // Port of LayoutComponent::updateLayoutBounds: a new box animates
            // from wherever the shown one is, even mid animation.
            animating = laidOut && animates();
            animFrom[0] = layoutLeft;
            animFrom[1] = layoutTop;
            animFrom[2] = getLayoutWidth();
            animFrom[3] = getLayoutHeight();
            animElapsed = 0;
            to[0] = flex.left;
            to[1] = flex.top;
            to[2] = flex.width;
            to[3] = flex.height;
        } else if (animating) {
            animElapsed += elapsed;
        }
        laidOut = true;
        float l = to[0];
        float t = to[1];
        float w = to[2];
        float h = to[3];
        if (animating) {
            float time = interpolationTime();
            float f = time > 0 ? animElapsed / time : 1;
            if (f >= 1) {
                animating = false;
            } else {
                if (interpolation() != INTERPOLATION_LINEAR && interpolator != null) {
                    f = interpolator.transform(f);
                }
                l = animFrom[0] + (l - animFrom[0]) * f;
                t = animFrom[1] + (t - animFrom[1]) * f;
                w = animFrom[2] + (w - animFrom[2]) * f;
                h = animFrom[3] + (h - animFrom[3]) * f;
            }
        }
        if (Float.floatToIntBits(l) != Float.floatToIntBits(layoutLeft)
                || Float.floatToIntBits(t) != Float.floatToIntBits(layoutTop)
                || Float.floatToIntBits(w) != Float.floatToIntBits(layoutWidth)
                || Float.floatToIntBits(h) != Float.floatToIntBits(layoutHeight)) {
            layoutLeft = l;
            layoutTop = t;
            layoutWidth = w;
            layoutHeight = h;
            markWorldTransformDirty();
            addDirt(DIRT_PATH);
            if (hostedBy != null) {
                hostedBy.markWorldTransformDirty();
            }
        }
        if (flex.hidden != layoutHidden) {
            layoutHidden = flex.hidden;
            if (!(this instanceof Artboard)) {
                collapse(layoutHidden);
                collapseTree(this, layoutHidden);
            }
        }
        ArrayList<FlexNode> kids = flex.children;
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            ((LayoutComponent) kids.get(i).owner).applyTree(elapsed);
        }
        // Artboard overrides propagateSize upstream (artboard.cpp) and never
        // control-sizes its direct children: a text straight under the
        // artboard keeps its own sizing, width and height.
        if (!layoutHidden && style != null && !(this instanceof Artboard)) {
            propagateSize(this);
        }
    }

    /// Port of `LayoutComponent::propagateSizeToChildren`: hands the resolved
    /// box to the content-sized children (text, images, nested artboards),
    /// through containers but not into groups or nested layouts.
    private void propagateSize(ContainerComponent c) {
        ArrayList<Component> kids = c.getChildren();
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            Component k = kids.get(i);
            if (k instanceof LayoutComponent || k instanceof NestedArtboardLayout || k.coreType() == NodeBase.TYPE_KEY) {
                continue;
            }
            int ws = style.mLayoutWidthScaleType;
            int hs = style.mLayoutHeightScaleType;
            if (k instanceof Text) {
                ((Text) k).controlSize(getLayoutWidth(), getLayoutHeight(), ws, hs);
                continue;
            }
            if (k instanceof Image) {
                ((Image) k).controlSize(getLayoutWidth(), getLayoutHeight());
                continue;
            }
            if (k instanceof NestedArtboard) {
                continue;
            }
            if (k instanceof ContainerComponent) {
                propagateSize((ContainerComponent) k);
            }
        }
    }

    /// Port of `LayoutComponent::measureLayout`: the largest intrinsic size
    /// among the direct content children.
    @Override
    public void measure(float width, int widthMode, float height, int heightMode, float[] out) {
        float mw = 0;
        float mh = 0;
        float w = widthMode == FlexNode.MEASURE_UNDEFINED ? Float.NaN : width;
        float h = heightMode == FlexNode.MEASURE_UNDEFINED ? Float.NaN : height;
        ArrayList<Component> kids = getChildren();
        float[] m = measured;
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            Component k = kids.get(i);
            if (k instanceof Text) {
                ((Text) k).measureLayout(w, h, m);
            } else if (k instanceof Image) {
                ((Image) k).measureLayout(width, widthMode, height, heightMode, m);
            } else if (k instanceof NestedArtboard && !(k instanceof NestedArtboardLayout)) {
                Artboard a = ((NestedArtboard) k).getArtboardInstance();
                m[0] = Math.min(Float.isNaN(w) ? Float.MAX_VALUE : w, a == null ? 0 : a.getWidth());
                m[1] = Math.min(Float.isNaN(h) ? Float.MAX_VALUE : h, a == null ? 0 : a.getHeight());
            } else {
                continue;
            }
            mw = Math.max(mw, m[0]);
            mh = Math.max(mh, m[1]);
        }
        out[0] = mw;
        out[1] = mh;
    }

    private boolean animates() {
        return style != null && style.mAnimationStyleType != 0 && interpolation() != 0 && interpolationTime() > 0;
    }

    /// Port of `LayoutComponent::interpolation`; an inheriting style takes
    /// its nearest custom ancestor's.
    private int interpolation() {
        LayoutComponent c = customAnimationSource();
        return c == null ? 0 : c.style.mInterpolationType;
    }

    private float interpolationTime() {
        LayoutComponent c = customAnimationSource();
        return c == null ? 0 : c.style.mInterpolationTime;
    }

    private LayoutComponent customAnimationSource() {
        LayoutComponent c = this;
        while (c != null && c.style != null && c.style.mAnimationStyleType == ANIMATION_INHERIT) {
            c = c.layoutParent;
        }
        return c != null && c.style != null && c.style.mAnimationStyleType == ANIMATION_CUSTOM ? c : null;
    }

    private static void collapseTree(ContainerComponent c, boolean value) {
        ArrayList<Component> kids = c.getChildren();
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            Component k = kids.get(i);
            k.collapse(value);
            if (k instanceof ContainerComponent) {
                collapseTree((ContainerComponent) k, value);
            }
        }
    }

    private static int effectiveUnits(int scaleType, int stored, boolean absolute) {
        if (absolute && scaleType != 2) {
            return stored;
        }
        if (scaleType != 0) {
            return FlexNode.UNIT_AUTO;
        }
        if (stored == FlexNode.UNIT_POINT || stored == FlexNode.UNIT_PERCENT) {
            return stored;
        }
        return FlexNode.UNIT_POINT;
    }

    /// Port of `LayoutComponent::syncStyle` and the style's item/container
    /// appliers, onto a [FlexNode].
    private void syncStyle(boolean parentIsRow, boolean hasLayoutParent, boolean parentRtl) {
        LayoutComponentStyle s = style;
        if (s == null) {
            s = new LayoutComponentStyle();
            style = s;
        }
        FlexNode n = flex;
        n.direction = s.mFlexDirectionValue & 3;
        n.wrap = s.mFlexWrapValue != 0;
        n.wrapReverse = s.mFlexWrapValue == 2;
        n.rtl = s.mDirectionValue == 2 || s.mDirectionValue == 0 && parentRtl;
        n.hidden = s.mDisplayValue == 1;
        n.absolute = s.mPositionTypeValue == 2;
        boolean isRow = n.direction >= FlexNode.ROW;

        int t = s.mLayoutAlignmentType;
        int main;
        int cross;
        if (t >= 9) {
            main = FlexNode.JUSTIFY_SPACE_BETWEEN;
            cross = t - 9 > 2 ? 2 : t - 9;
        } else {
            int hz = t % 3;
            int vt = t / 3;
            main = isRow ? hz : vt;
            cross = isRow ? vt : hz;
        }
        n.justify = main;
        n.alignItems = cross + FlexNode.ALIGN_START;
        n.alignContent = n.alignItems;

        int wScale = s.mLayoutWidthScaleType;
        int hScale = s.mLayoutHeightScaleType;
        n.dim[0] = mWidth < 0 ? 0 : mWidth;
        n.dim[1] = mHeight < 0 ? 0 : mHeight;
        n.dimUnit[0] = effectiveUnits(wScale, s.mWidthUnitsValue, n.absolute);
        n.dimUnit[1] = effectiveUnits(hScale, s.mHeightUnitsValue, n.absolute);
        n.measure = s.mIntrinsicallySizedValue && n.children.isEmpty() ? this : null;
        if (hostedBy != null) {
            // Port of StyleOverrider: the hosting NestedArtboardLayout's
            // instance size and scale types replace the artboard's own.
            wScale = hostedBy.getInstanceWidthScaleType();
            hScale = hostedBy.getInstanceHeightScaleType();
            float iw = hostedBy.getInstanceWidth();
            float ih = hostedBy.getInstanceHeight();
            n.dim[0] = iw < 0 ? mWidth : iw;
            n.dim[1] = ih < 0 ? mHeight : ih;
            n.dimUnit[0] = wScale == SCALE_FIXED ? hostedBy.getInstanceWidthUnitsValue() : FlexNode.UNIT_AUTO;
            n.dimUnit[1] = hScale == SCALE_FIXED ? hostedBy.getInstanceHeightUnitsValue() : FlexNode.UNIT_AUTO;
        }
        n.grow = 0;
        n.shrink = 0;
        n.basis = s.mFlexBasis;
        n.basisUnit = FlexNode.UNIT_AUTO;
        n.alignSelf = FlexNode.ALIGN_AUTO;
        int mainScale = parentIsRow ? wScale : hScale;
        int crossScale = parentIsRow ? hScale : wScale;
        if (mainScale == 1) {
            n.grow = parentIsRow ? mFractionalWidth : mFractionalHeight;
            n.shrink = n.grow;
            n.basisUnit = s.mFlexBasisUnitsValue;
        }
        if (crossScale == 1) {
            n.alignSelf = FlexNode.ALIGN_STRETCH;
        }
        n.min[0] = s.mMinWidth;
        n.minUnit[0] = s.mMinWidthUnitsValue;
        n.min[1] = s.mMinHeight;
        n.minUnit[1] = s.mMinHeightUnitsValue;
        n.max[0] = s.mMaxWidth;
        n.maxUnit[0] = s.mMaxWidthUnitsValue;
        n.max[1] = s.mMaxHeight;
        n.maxUnit[1] = s.mMaxHeightUnitsValue;
        n.aspectRatio = s.mAspectRatio > 0 ? s.mAspectRatio : 0;

        int start = n.rtl ? FlexNode.RIGHT : FlexNode.LEFT;
        int end = n.rtl ? FlexNode.LEFT : FlexNode.RIGHT;
        n.margin[start] = s.mMarginLeft;
        n.margin[end] = s.mMarginRight;
        n.margin[FlexNode.TOP] = s.mMarginTop;
        n.margin[FlexNode.BOTTOM] = s.mMarginBottom;
        n.marginUnit[start] = hasLayoutParent ? s.mMarginLeftUnitsValue : FlexNode.UNIT_POINT;
        n.marginUnit[end] = hasLayoutParent ? s.mMarginRightUnitsValue : FlexNode.UNIT_POINT;
        n.marginUnit[FlexNode.TOP] = hasLayoutParent ? s.mMarginTopUnitsValue : FlexNode.UNIT_POINT;
        n.marginUnit[FlexNode.BOTTOM] = hasLayoutParent ? s.mMarginBottomUnitsValue : FlexNode.UNIT_POINT;
        n.inset[start] = s.mPositionLeft;
        n.inset[end] = s.mPositionRight;
        n.inset[FlexNode.TOP] = s.mPositionTop;
        n.inset[FlexNode.BOTTOM] = s.mPositionBottom;
        n.insetUnit[start] = s.mPositionLeftUnitsValue;
        n.insetUnit[end] = s.mPositionRightUnitsValue;
        n.insetUnit[FlexNode.TOP] = s.mPositionTopUnitsValue;
        n.insetUnit[FlexNode.BOTTOM] = s.mPositionBottomUnitsValue;
        n.padding[start] = s.mPaddingLeft;
        n.padding[end] = s.mPaddingRight;
        n.padding[FlexNode.TOP] = s.mPaddingTop;
        n.padding[FlexNode.BOTTOM] = s.mPaddingBottom;
        n.paddingUnit[start] = s.mPaddingLeftUnitsValue;
        n.paddingUnit[end] = s.mPaddingRightUnitsValue;
        n.paddingUnit[FlexNode.TOP] = s.mPaddingTopUnitsValue;
        n.paddingUnit[FlexNode.BOTTOM] = s.mPaddingBottomUnitsValue;
        n.border[start] = s.mBorderLeftUnitsValue == FlexNode.UNIT_POINT ? s.mBorderLeft : 0;
        n.border[end] = s.mBorderRightUnitsValue == FlexNode.UNIT_POINT ? s.mBorderRight : 0;
        n.border[FlexNode.TOP] = s.mBorderTopUnitsValue == FlexNode.UNIT_POINT ? s.mBorderTop : 0;
        n.border[FlexNode.BOTTOM] = s.mBorderBottomUnitsValue == FlexNode.UNIT_POINT ? s.mBorderBottom : 0;
        n.gap[0] = s.mGapHorizontal;
        n.gapUnit[0] = s.mGapHorizontalUnitsValue;
        n.gap[1] = s.mGapVertical;
        n.gapUnit[1] = s.mGapVerticalUnitsValue;
    }

    @Override
    public void updateTransform() {
        transform.identity();
    }

    /// Port of `LayoutComponent::composeWorldTransform`: the parent's world
    /// moved to this layout's box (relative to the artboard's origin for a
    /// layout directly under the artboard).
    @Override
    public void composeWorldTransform() {
        float tx = layoutLeft;
        float ty = layoutTop;
        if (parent instanceof Artboard) {
            Artboard art = (Artboard) parent;
            tx -= art.getLayoutWidth() * art.getOriginX();
            ty -= art.getLayoutHeight() * art.getOriginY();
        }
        translation.set(1, 0, 0, 1, tx, ty);
        if (parentTransform != null) {
            worldTransform.multiply(parentTransform.getWorldTransform(), translation);
        } else {
            worldTransform.set(translation);
        }
    }

    /// Draws the background (fills and strokes over the rounded box) and
    /// pushes the clip; [#draw(RiveRenderer)] pops it after the children.
    void drawBackground(RiveRenderer renderer) {
        if (isHidden() || layoutPaints.isEmpty() && !mClip) {
            return;
        }
        updateBackgroundPath();
        if (!layoutPaints.isEmpty()) {
            renderer.save();
            renderer.transform(worldTransform);
            for (int i = 0; i < layoutPaints.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                ShapePaint p = layoutPaints.get(i);
                if (!p.isVisible() || p.isCollapsed()) {
                    continue;
                }
                p.updatePaint(renderOpacity);
                backgroundPath.fillRule = p instanceof Fill ? ((Fill) p).getFillRule() : RivePath.FILL_NON_ZERO;
                renderer.drawPath(p.effectPath(backgroundPath), p.getRenderPaint());
            }
            renderer.restore();
        }
        if (mClip) {
            worldBackground.rewind();
            worldBackground.addPath(backgroundPath, worldTransform);
            renderer.save();
            renderer.clipPath(worldBackground);
            clipSaved = true;
        }
    }

    private void updateBackgroundPath() {
        float w = getLayoutWidth();
        float h = getLayoutHeight();
        float tl = 0;
        float tr = 0;
        float br = 0;
        float bl = 0;
        if (style != null) {
            if (style.mLinkCornerRadius) {
                tl = style.mCornerRadiusTL;
                tr = tl;
                br = tl;
                bl = tl;
            } else {
                boolean ltr = !flex.rtl;
                tl = ltr ? style.mCornerRadiusTL : style.mCornerRadiusTR;
                tr = ltr ? style.mCornerRadiusTR : style.mCornerRadiusTL;
                bl = ltr ? style.mCornerRadiusBL : style.mCornerRadiusBR;
                br = ltr ? style.mCornerRadiusBR : style.mCornerRadiusBL;
            }
        }
        float[] k = pathKey;
        if (!backgroundPath.isEmpty() && k[0] == w && k[1] == h && k[2] == tl && k[3] == tr && k[4] == br && k[5] == bl) { // NOPMD - exact cache key compare
            return;
        }
        k[0] = w;
        k[1] = h;
        k[2] = tl;
        k[3] = tr;
        k[4] = br;
        k[5] = bl;
        addRoundedRect(backgroundPath, w, h, tl, tr, br, bl);
    }

    /// Port of `Path::addRoundedRect` (radii clamped to half the shorter side).
    static void addRoundedRect(RivePath p, float w, float h, float rtl, float rtr, float rbr, float rbl) {
        p.rewind();
        float lim = Math.min(w, h) / 2f;
        float tl = Math.max(0, Math.min(rtl, lim));
        float tr = Math.max(0, Math.min(rtr, lim));
        float br = Math.max(0, Math.min(rbr, lim));
        float bl = Math.max(0, Math.min(rbl, lim));
        float k = 1 - KAPPA;
        p.moveTo(tl, 0);
        p.lineTo(w - tr, 0);
        if (tr > 0) {
            p.cubicTo(w - tr * k, 0, w, tr * k, w, tr);
        }
        p.lineTo(w, h - br);
        if (br > 0) {
            p.cubicTo(w, h - br * k, w - br * k, h, w - br, h);
        }
        p.lineTo(bl, h);
        if (bl > 0) {
            p.cubicTo(bl * k, h, 0, h - bl * k, 0, h - bl);
        }
        p.lineTo(0, tl);
        if (tl > 0) {
            p.cubicTo(0, tl * k, tl * k, 0, tl, 0);
        }
        p.close();
    }

    @Override
    public void draw(RiveRenderer renderer) {
        if (clipSaved) {
            clipSaved = false;
            renderer.restore();
        }
    }

    /// Stands in the draw list behind a layout's children to paint its
    /// background first (rive-runtime's `drawProxy`).
    static final class BackgroundProxy extends Drawable {
        final LayoutComponent host;

        BackgroundProxy(LayoutComponent host) {
            this.host = host;
        }

        @Override
        public int coreType() {
            return -1;
        }

        @Override
        public void draw(RiveRenderer renderer) {
            host.drawBackground(renderer);
        }
    }
}
