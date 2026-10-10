/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.material;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;

/**
 * Render element for {@link Material}: a CN1 Container (UIID "FlutterMaterial")
 * filled with the surface color, covering the element bounds behind the child.
 * Sizes to the child, or fills the bounded incoming axes when childless. The
 * child's components attach after the face, so they paint on top.
 */
public class MaterialRenderElement extends com.codename1.flutter.widgets.EffectRenderElement {

    public MaterialRenderElement(Material widget) {
        super(widget);
    }

    private Material material() {
        return (Material) widget();
    }

    @Override
    protected Widget effectChild() {
        return material().getChild();
    }



    /**
     * A rounded Material paints its own surface (see {@link #paintSurface}), so all this
     * has to do is stop Codename One painting a second, square one underneath.
     *
     * <p>It used to hand the job to a {@code RoundRectBorder}. That draws the fill as the
     * component BACKGROUND, and a background is painted before {@code paint()} runs - so it
     * landed outside the rounded clip this element installs and squared the corners off
     * again from behind. Rounding is not a style the surface happens to have; it is the
     * same shape the subtree is clipped to, and both come from one path here.</p>
     */
    private void applyStyle(Component face) {
        try {
            boolean rounded = maxCornerRadiusLp() > 0;
            if (material().getColor() != null) {
                com.codename1.flutter.material.ThemeDataAdapter.paintColor(
                        face.getAllStyles(), material().getColor());
            }
            if (rounded) {
                face.getAllStyles().setBorder(com.codename1.ui.plaf.Border.createEmpty());
                face.getAllStyles().setBgTransparency(0);
            }
        } catch (Exception err) {
            // best-effort
        }
    }

    /**
     * Fills the surface, and casts its elevation shadow, in the shape the subtree is about
     * to be clipped to — Flutter's {@code Material(color:, shape:, elevation:)}.
     *
     * <p>The shadow is drawn OUTSIDE the surface, as Flutter's is: a few progressively
     * wider, fainter rings under the card. Codename One's own shadow reserves its spread
     * INSIDE the component box and shifts the surface to make room, which is a different
     * thing altogether and moved the card clear of its own bounds.</p>
     */
    private void paintSurface(com.codename1.ui.Graphics g, int[] q, double elevation) {
        com.codename1.flutter.Color c = material().getColor();
        if (c == null || c.alpha() == 0) {
            return;
        }
        int rgb = (int) (c.value() & 0xFFFFFFL);
        boolean oldAA = g.isAntiAliased();
        int oldColor = g.getColor();
        int oldAlpha = g.getAlpha();
        g.setAntiAliased(true);
        try {
            // Material's elevation shadow: roughly a blur of twice the elevation, dropped by
            // half of it. fillShapeShadow does the fill and the blur in one accelerated draw
            // with no retained bitmap, which is what makes it affordable on a card that
            // repaints every frame of a scroll.
            if (elevation > 0 && g.isShapeShadowSupported()) {
                g.fillShapeShadow(clipShape(q[0], q[1], q[2], q[3], q[4], q[5], q[6], q[7]),
                        rgb, c.alpha(), 0x000000, 0.28f,
                        (int) Math.round(com.codename1.flutter.rendering.Dp.px(elevation * 2)),
                        0, (int) Math.round(
                                com.codename1.flutter.rendering.Dp.px(elevation / 2.0)));
                return;
            }
            if (elevation > 0) {
                paintShadowRings(g, q, elevation);
            }
            g.setColor(rgb);
            g.setAlpha(c.alpha());
            // AFTER the rings: they are built through the same reused path, so taking this
            // shape earlier would hand the fill whatever the last ring left behind.
            g.fillShape(clipShape(q[0], q[1], q[2], q[3], q[4], q[5], q[6], q[7]));
        } finally {
            g.setAntiAliased(oldAA);
            g.setAlpha(oldAlpha);
            g.setColor(oldColor);
        }
    }

    /// How many rounded rects approximate the blur where the port has no real one.
    /// Each is a full fill, per card per frame, so this is deliberately small: four reads
    /// as a soft edge, and more is not visible at these opacities.
    private static final int SHADOW_RINGS = 4;

    /// Alpha of each shadow ring, innermost first, out of 255.
    ///
    /// The rings are FILLED shapes drawn outside-in, so each one darkens everything
    /// inside it as well: with a single alpha for all four, the band against the surface
    /// is hit four times and ends up far darker than Material's shadow, and the falloff
    /// outward is linear where Material's is a blur that drops off fast.
    ///
    /// Calibrated against the reference, as darkening of white beside an elevation-6
    /// circle -- the Reply study's compose button, which is the largest single elevated
    /// surface in the gallery:
    ///
    /// ```text
    ///   band   reference   one alpha for all   these alphas
    ///      1       0.071               0.148          0.071
    ///      2       0.035               0.077          0.035
    ///      3       0.020               0.039          0.020
    ///      4       0.008               0.039          0.008
    /// ```
    ///
    /// The three opacities Material composes a shadow from do not change with elevation
    /// -- only the blur and the offset do, and the ring extents already scale with it --
    /// so these hold across elevations rather than fitting the one that was measured.
    private static final int[] RING_ALPHA = {10, 4, 3, 2};

    /**
     * The elevation shadow where {@code fillShapeShadow} is unavailable (the iOS port among
     * them): a few progressively larger, fainter rounded rects under the card, drawn
     * outside-in so their alpha accumulates towards the surface.
     *
     * <p>Deliberately NOT cached to a bitmap. Caching a per-card shadow image is what made
     * these same cards a RAM and jank problem on Android, and the surface has to be redrawn
     * every frame of a scroll anyway.</p>
     */
    private void paintShadowRings(com.codename1.ui.Graphics g, int[] q, double elevation) {
        int spread = Math.max(1, (int) Math.round(
                com.codename1.flutter.rendering.Dp.px(elevation)));
        int drop = Math.max(1, (int) Math.round(
                com.codename1.flutter.rendering.Dp.px(elevation / 2.0)));
        g.setColor(0x000000);
        for (int i = SHADOW_RINGS; i >= 1; i--) {
            int e = Math.max(1, spread * i / SHADOW_RINGS);
            g.setAlpha(RING_ALPHA[i - 1]);
            g.fillShape(clipShape(q[0] - e, q[1] - e + drop, q[2] + e * 2, q[3] + e * 2,
                    grown(q[4], e), grown(q[5], e), grown(q[6], e), grown(q[7], e)));
        }
    }

    /// The matching corner on a shadow ring {@code by} pixels outside a corner of radius
    /// {@code r}. A squared corner stays squared - rounding it would put a curve back on a
    /// corner the clip deliberately cut off at the edge of a viewport.
    private static int grown(int r, int by) {
        return r <= 0 ? 0 : r + by;
    }

    @Override
    protected void paintWithEffect(com.codename1.ui.Graphics g,
            com.codename1.ui.Container pane, Subtree paintChildren) {
        styleOnce(pane);
        double[] lp = cornerRadiiLp();
        int rtl = (int) Math.round(com.codename1.flutter.rendering.Dp.px(lp[0]));
        int rtr = (int) Math.round(com.codename1.flutter.rendering.Dp.px(lp[1]));
        int rbr = (int) Math.round(com.codename1.flutter.rendering.Dp.px(lp[2]));
        int rbl = (int) Math.round(com.codename1.flutter.rendering.Dp.px(lp[3]));
        if (Math.max(Math.max(rtl, rtr), Math.max(rbr, rbl)) <= 0) {
            // Square surface: nothing to paint here that the component's own background
            // does not already do (applyStyle leaves it in place in this case).
            paintChildren.paint(g);
            return;
        }
        boolean clips = !noShapeClip() && g.isShapeClipSupported()
                && material().getClipBehavior() != com.codename1.flutter.Clip.none;
        if (!noShapeClip() && !g.isShapeClipSupported()) {
            com.codename1.flutter.FlutterErrorReport.unimplemented("Material",
                    "this port cannot clip to a shape, so the subtree paints square-cornered");
        }
        int x = pane.getX();
        int y = pane.getY();
        int w = pane.getWidth();
        int h = pane.getHeight();
        // setClip(Shape) REPLACES the clip rather than intersecting it, and the study card
        // lives in a horizontally scrolling carousel that is already clipping us - so
        // replacing outright would let a half-scrolled card paint outside its viewport. The
        // shape we install must therefore be the rounded rect INTERSECTED with the incoming
        // clip.
        //
        // That intersection is computed directly rather than with GeneralPath.intersection().
        // The general polygon clipper allocates heavily, and a half-scrolled card overflows
        // its viewport on EVERY frame of a drag, so it ran per card per frame: a `sample` of
        // a wedged app caught the EDT parked in usleep inside cn1BibopMaybeGc, called from
        // allocArray under ShapeUtil.intersection, called from here. Paint was driving the
        // collector, which is what the stalls and the half-drawn frames were. It was also
        // wrong - the clipper does not survive a path entirely inside the rectangle, which
        // is why this used to need a special case to stop it cutting the icons out of every
        // category row.
        //
        // Intersecting a rounded rect with an axis-aligned rect needs no clipper: the result
        // is the intersected BOUNDS, rounded at exactly those corners the clip left intact.
        // A corner the clip cut through is off-screen anyway, so squaring it is invisible.
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        if (clipGeom == null) {
            clipGeom = new int[8];
        }
        int cap = Math.min(w, h) / 2;
        if (!clipGeometry(clipGeom, x, y, w, h, Math.min(rtl, cap), Math.min(rtr, cap),
                Math.min(rbr, cap), Math.min(rbl, cap), cx, cy, cw, ch)) {
            // Entirely clipped out: painting the subtree could only produce invisible pixels.
            return;
        }
        int[] q = clipGeom;
        if ("true".equals(com.codename1.ui.Display.getInstance()
                .getProperty("cn1.flutter.debugClip", "false"))) {
            // Reported through the error channel so it comes back over bench_errors, which
            // dedupes: a device needs no new tooling to answer "what geometry did this card
            // actually compute", and guessing at that has been expensive.
            com.codename1.flutter.FlutterErrorReport.unimplemented("MaterialClip",
                    "box=" + x + "," + y + "," + w + "," + h
                    + " clip=" + cx + "," + cy + "," + cw + "," + ch
                    + " r=" + rtl + "," + rtr + "," + rbr + "," + rbl + " out=" + q[0] + "," + q[1] + "," + q[2] + "," + q[3]
                    + " corners=" + q[4] + "," + q[5] + "," + q[6] + "," + q[7]
                    + " shapeClip=" + g.isShapeClipSupported()
                    + " shadow=" + g.isShapeShadowSupported());
        }
        // Surface first, then the subtree on top of it, both in the same shape. The surface
        // is painted here rather than as the component's background because a background is
        // painted before paint() runs, i.e. outside the clip below - which is exactly how
        // the corners used to end up square from behind.
        paintSurface(g, q, material().getElevation());
        if (!clips) {
            paintChildren.paint(g);
            return;
        }
        // push/pop, not save-the-four-ints-and-restore: the four ints are a RECTANGLE,
        // so restoring that way degrades whatever shaped clip an ancestor had
        // established to its bounding box. A Material inside a ClipRRect therefore left
        // the outer shape no longer holding, and the subtree looked mis-layered rather
        // than merely unclipped. This is the idiom the shaped-clipping tests in
        // scripts/hellocodenameone use.
        g.pushClip();
        try {
            g.setClip(clipShape(q[0], q[1], q[2], q[3], q[4], q[5], q[6], q[7]));
            paintChildren.paint(g);
        } finally {
            g.popClip();
        }
    }

    /// Scratch for {@link #clipGeometry}, owned per element so the paint path stays
    /// allocation-free.
    private int[] clipGeom;

    /**
     * The rounded rectangle {@code (x,y,w,h)} radius {@code r}, intersected with the clip
     * {@code (cx,cy,cw,ch)}, as bounds plus a radius per corner.
     *
     * <p>Writes {@code x, y, w, h, tlRadius, trRadius, brRadius, blRadius} into {@code out}
     * and returns whether anything is visible at all.</p>
     *
     * <p>Package-private and static so the geometry can be asserted directly: it decides
     * what the user sees at the edge of every scrolling viewport, and it is not something
     * a layout test would catch.</p>
     */
    static boolean clipGeometry(int[] out, int x, int y, int w, int h, int r,
            int cx, int cy, int cw, int ch) {
        return clipGeometry(out, x, y, w, h, r, r, r, r, cx, cy, cw, ch);
    }

    /**
     * As above, with a radius per corner -- Flutter rounds corners independently, and a
     * surface that rounds one of them is not a surface with a single radius.
     */
    static boolean clipGeometry(int[] out, int x, int y, int w, int h,
            int rtl, int rtr, int rbr, int rbl, int cx, int cy, int cw, int ch) {
        int ix = Math.max(x, cx);
        int iy = Math.max(y, cy);
        int ix2 = Math.min(x + w, cx + cw);
        int iy2 = Math.min(y + h, cy + ch);
        if (ix2 <= ix || iy2 <= iy) {
            return false;
        }
        // Keep the arcs inside the visible box. Only reachable once the clip has already cut
        // an edge (r is <= half the full box), i.e. a card reduced to a sliver at the screen
        // edge, where a slightly tighter corner cannot be seen.
        int cap = Math.min(ix2 - ix, iy2 - iy) / 2;

        // An edge the clip did not move is an edge whose two corners are still the card's own.
        boolean l = x >= ix;
        boolean t = y >= iy;
        boolean rt = x + w <= ix2;
        boolean b = y + h <= iy2;
        out[0] = ix;
        out[1] = iy;
        out[2] = ix2 - ix;
        out[3] = iy2 - iy;
        out[4] = l && t ? capped(rtl, cap) : 0;
        out[5] = rt && t ? capped(rtr, cap) : 0;
        out[6] = rt && b ? capped(rbr, cap) : 0;
        out[7] = l && b ? capped(rbl, cap) : 0;
        return true;
    }

    private static int capped(int r, int cap) {
        return Math.max(0, Math.min(r, cap));
    }

    /// A/B switch for the rounded clip, flipped at runtime with
    /// {@code Display.setProperty("cn1.flutter.noShapeClip", "true")}.
    ///
    /// Exists because the clip is a per-card, per-frame native call whose cost is only
    /// measurable on a device, and turning it off is the one experiment that separates
    /// "the clip is expensive" from "something else is". Read per paint deliberately: an
    /// A/B you have to rebuild for is an A/B you run once and mis-attribute.
    private static boolean noShapeClip() {
        return "true".equals(com.codename1.ui.Display.getInstance()
                .getProperty("cn1.flutter.noShapeClip", "false"));
    }

    /// The clip path, rebuilt in place rather than reallocated.
    ///
    /// This is rebuilt on most frames of a drag (the geometry really is changing), so the
    /// point is not to skip the work but to keep it out of the allocator: a scrolling
    /// carousel paints this per card per frame, and paint that allocates is paint that can
    /// be interrupted by a collection - see the note in {@link #paintWithEffect}. Reusing
    /// the instance is safe because every port copies the shape into its own
    /// representation on setClip rather than holding this one.
    private com.codename1.ui.geom.GeneralPath clipPath;
    private int clipKeyA;
    private int clipKeyB;

    /// A rectangle with an independent radius per corner, in the coordinate space a
    /// component paints in - parent-relative, because the Graphics has already accumulated
    /// its ancestors' translation. Radii run clockwise from the top left.
    com.codename1.ui.geom.Shape clipShape(int x, int y, int w, int h,
            int tl, int tr, int br, int bl) {
        // Cheap identity for "same shape as last time": the carousel settles between drags
        // and every static card then re-installs a clip the port can recognise as unchanged.
        int a = (x * 31 + y) * 31 * 31 + w * 31 + h;
        int b = ((tl * 31 + tr) * 31 + br) * 31 + bl;
        if (clipPath != null && a == clipKeyA && b == clipKeyB) {
            return clipPath;
        }
        clipKeyA = a;
        clipKeyB = b;
        if (clipPath == null) {
            clipPath = new com.codename1.ui.geom.GeneralPath();
        } else {
            clipPath.reset();
        }
        com.codename1.ui.geom.GeneralPath p = clipPath;
        p.moveTo(x + tl, y);
        p.lineTo(x + w - tr, y);
        arc(p, x + w - tr, y + tr, tr, -90);
        p.lineTo(x + w, y + h - br);
        arc(p, x + w - br, y + h - br, br, 0);
        p.lineTo(x + bl, y + h);
        arc(p, x + bl, y + h - bl, bl, 90);
        p.lineTo(x, y + tl);
        arc(p, x + tl, y + tl, tl, 180);
        p.closePath();
        return p;
    }

    /**
     * Appends one 90° corner as short line segments, sweeping clockwise from
     * {@code startDeg} about ({@code cx},{@code cy}).
     *
     * <p>Line segments rather than a {@code quadTo} because a clip has to survive being
     * handed to a GPU, and the ports test for a POLYGON to decide how: Codename One's iOS
     * backend renders a polygon clip through a stencil, and anything it cannot reduce to
     * one falls back to the shape's BOUNDING BOX - which is a square-cornered card. The
     * curve buys nothing here anyway: at a 10dp radius these segments are under two pixels
     * each, and the same path also fills the surface, so shape and fill cannot disagree.</p>
     */
    private static void arc(com.codename1.ui.geom.GeneralPath p, int cx, int cy, int r,
            int startDeg) {
        if (r <= 0) {
            return;
        }
        int segs = Math.max(3, Math.min(10, r / 3));
        for (int i = 1; i <= segs; i++) {
            double a = Math.toRadians(startDeg + 90.0 * i / segs);
            p.lineTo((float) (cx + r * Math.cos(a)), (float) (cy + r * Math.sin(a)));
        }
    }

    /// Applies the surface style when it first paints or after its configuration
    /// changes. Re-deriving a RoundRectBorder on every frame would allocate per paint.
    private String styleSignature;

    private void styleOnce(com.codename1.ui.Container pane) {
        double[] r = cornerRadiiLp();
        String sig = r[0] + "," + r[1] + "," + r[2] + "," + r[3]
                + "|" + material().getElevation() + "|"
                + (material().getColor() == null ? "-" : material().getColor().value());
        if (sig.equals(styleSignature)) {
            return;
        }
        styleSignature = sig;
        applyStyle(pane);
    }


    /// The corner radius in logical pixels from the shape or an explicit borderRadius.
    /**
     * The radius this surface clips its subtree to, in pixels; 0 when it does not clip.
     *
     * <p>Descendants need this because a clip is not reliable on every port — see
     * {@code ImageRenderElement.enclosingCornerRadius}, where an image that fills the
     * surface rounds its own bitmap instead of trusting one.</p>
     */
    public int clipRadiusPx() {
        if (material().getClipBehavior() == com.codename1.flutter.Clip.none) {
            return 0;
        }
        // One radius, so this can only answer for a surface whose corners agree. A
        // surface that rounds some corners and not others has no uniform radius, and
        // answering with one would round corners the surface leaves square; the caller
        // falls back to the clip, which carries all four.
        double[] r = cornerRadiiLp();
        if (r[0] != r[1] || r[1] != r[2] || r[2] != r[3]) {
            return 0;
        }
        return (int) Math.round(com.codename1.flutter.rendering.Dp.px(r[0]));
    }

    /// Scratch for {@link #cornerRadiiLp}, owned per element so the paint path stays
    /// allocation-free.
    private double[] radiiLp;

    /// The four corner radii in logical pixels, in the order {@code tl, tr, br, bl}.
    ///
    /// Flutter's radii are per corner, and reading only the top-left one squared every
    /// surface that rounds some corners and not others. The gallery's settings button is
    /// exactly that -- {@code BorderRadiusDirectional.only(bottomStart: 10)} -- so it
    /// drew as a plain white block where the reference has a rounded bottom-left corner.
    ///
    /// A directional radius is resolved left-to-right, as the rest of the runtime
    /// resolves {@code AlignmentDirectional} and {@code EdgeInsetsDirectional}.
    private double[] cornerRadiiLp() {
        if (radiiLp == null) {
            radiiLp = new double[4];
        }
        double[] out = radiiLp;
        out[0] = 0;
        out[1] = 0;
        out[2] = 0;
        out[3] = 0;
        if (material().getShape() instanceof com.codename1.flutter.CircleBorder) {
            // A CircleBorder is the circle inscribed in the box, which as a
            // rounded rectangle is a corner radius of half the shorter side.
            // The shape was unrecognised and fell through to a radius of zero,
            // so a surface wearing one drew square: Reply's compose button is
            // an OpenContainer with closedShape: CircleBorder(), and it
            // rendered as an orange block sitting on the bottom bar.
            com.codename1.flutter.rendering.Size box = size();
            if (box == null || box.width() <= 0 || box.height() <= 0) {
                return out;
            }
            double scale = com.codename1.flutter.rendering.Dp.scale();
            double shorterPx = Math.min(box.width(), box.height());
            double circle = scale > 0 ? shorterPx / 2 / scale : 0;
            out[0] = circle;
            out[1] = circle;
            out[2] = circle;
            out[3] = circle;
            return out;
        }
        Object r = material().getShape() instanceof com.codename1.flutter.RoundedRectangleBorder
                ? ((com.codename1.flutter.RoundedRectangleBorder) material().getShape()).getBorderRadius()
                : material().getBorderRadius();
        if (r instanceof com.codename1.flutter.BorderRadius) {
            com.codename1.flutter.BorderRadius b = (com.codename1.flutter.BorderRadius) r;
            out[0] = radiusX(b.topLeft());
            out[1] = radiusX(b.topRight());
            out[2] = radiusX(b.bottomRight());
            out[3] = radiusX(b.bottomLeft());
        } else if (r instanceof com.codename1.flutter.BorderRadiusDirectional) {
            com.codename1.flutter.BorderRadiusDirectional b =
                    (com.codename1.flutter.BorderRadiusDirectional) r;
            out[0] = radiusX(b.topStart());
            out[1] = radiusX(b.topEnd());
            out[2] = radiusX(b.bottomEnd());
            out[3] = radiusX(b.bottomStart());
        }
        return out;
    }

    private static double radiusX(com.codename1.flutter.Radius r) {
        return r == null ? 0 : r.x();
    }

    /// The largest of the four corner radii, for the decisions that only need to know
    /// whether this surface is rounded at all.
    private double maxCornerRadiusLp() {
        double[] r = cornerRadiiLp();
        return Math.max(Math.max(r[0], r[1]), Math.max(r[2], r[3]));
    }

}
