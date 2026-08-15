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
            boolean rounded = cornerRadiusLp() > 0;
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
        int rgb = (int) (c.value() & 0xFFFFFF);
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
            g.setAlpha(10);
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
            com.codename1.ui.Container pane, Runnable paintChildren) {
        styleOnce(pane);
        int radius = (int) Math.round(com.codename1.flutter.rendering.Dp.px(cornerRadiusLp()));
        if (radius <= 0) {
            // Square surface: nothing to paint here that the component's own background
            // does not already do (applyStyle leaves it in place in this case).
            paintChildren.run();
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
        if (!clipGeometry(clipGeom, x, y, w, h, Math.min(radius, Math.min(w, h) / 2),
                cx, cy, cw, ch)) {
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
                    + " r=" + radius + " out=" + q[0] + "," + q[1] + "," + q[2] + "," + q[3]
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
            paintChildren.run();
            return;
        }
        try {
            g.setClip(clipShape(q[0], q[1], q[2], q[3], q[4], q[5], q[6], q[7]));
            paintChildren.run();
        } finally {
            g.setClip(cx, cy, cw, ch);
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
        r = Math.max(0, Math.min(r, Math.min(ix2 - ix, iy2 - iy) / 2));

        // An edge the clip did not move is an edge whose two corners are still the card's own.
        boolean l = x >= ix;
        boolean t = y >= iy;
        boolean rt = x + w <= ix2;
        boolean b = y + h <= iy2;
        out[0] = ix;
        out[1] = iy;
        out[2] = ix2 - ix;
        out[3] = iy2 - iy;
        out[4] = l && t ? r : 0;
        out[5] = rt && t ? r : 0;
        out[6] = rt && b ? r : 0;
        out[7] = l && b ? r : 0;
        return true;
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
        String sig = cornerRadiusLp() + "|" + material().getElevation() + "|"
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
        return (int) Math.round(com.codename1.flutter.rendering.Dp.px(cornerRadiusLp()));
    }

    private double cornerRadiusLp() {
        Object r = material().getShape() instanceof com.codename1.flutter.RoundedRectangleBorder
                ? ((com.codename1.flutter.RoundedRectangleBorder) material().getShape()).getBorderRadius()
                : material().getBorderRadius();
        if (r instanceof com.codename1.flutter.BorderRadius) {
            com.codename1.flutter.Radius tl = ((com.codename1.flutter.BorderRadius) r).topLeft();
            return tl == null ? 0 : tl.x();
        }
        return 0;
    }

}
