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



    private void applyStyle(Component face) {
        try {
            double radiusLp = cornerRadiusLp();
            double elevation = material().getElevation();
            if (radiusLp > 0 || elevation > 0) {
                // Rounded corners and a shadow are what make a Material surface read as
                // Material; a bare bgColor gives a flat rectangle.
                com.codename1.ui.plaf.RoundRectBorder border =
                        com.codename1.ui.plaf.RoundRectBorder.create()
                                .useCache(false)
                                .cornerRadius(com.codename1.flutter.rendering.Dp.mm(radiusLp));
                if (elevation > 0) {
                    // Flutter draws an elevation shadow OUTSIDE the box, leaving the surface
                    // where layout put it. RoundRectBorder instead reserves the spread INSIDE
                    // the component and draws the surface smaller by that much, displaced
                    // towards whichever edge shadowY favours (0.5 is centred, 1 is hard
                    // against the bottom).
                    //
                    // So the spread is not a free parameter here: it comes straight off the
                    // card's geometry. The previous values - a spread in MILLIMETRES that
                    // worked out to ~23px at this density, with shadowY hard over at 1 -
                    // pushed the study card 23px clear of its own box, which read as a grey
                    // band along its top edge and content spilling past its bottom.
                    //
                    // Keep it in pixels off the elevation, and near-centred so the surface
                    // stays put; Material's shadow is a soft halo cast slightly downwards,
                    // not an offset frame.
                    border = border
                            .shadowOpacity(Math.min(255, (int) Math.round(20 + elevation * 15)))
                            .shadowSpread((int) Math.round(
                                    com.codename1.flutter.rendering.Dp.px(elevation)))
                            .shadowY(0.6f);
                }
                face.getAllStyles().setBorder(border);
            }
            if (material().getColor() != null) {
                com.codename1.flutter.material.ThemeDataAdapter.paintColor(
                        face.getAllStyles(), material().getColor());
                if (radiusLp > 0 || elevation > 0) {
                    // the border paints the fill; keep the flat bg from squaring it off
                    face.getAllStyles().setBgTransparency(
                            material().getColor().alpha() == 0 ? 0 : 255);
                }
            }
        } catch (Exception err) {
            // best-effort
        }
    }

    @Override
    protected void paintWithEffect(com.codename1.ui.Graphics g,
            com.codename1.ui.Container pane, Runnable paintChildren) {
        styleOnce(pane);
        int radius = (int) Math.round(com.codename1.flutter.rendering.Dp.px(cornerRadiusLp()));
        if (radius <= 0 || material().getClipBehavior() == com.codename1.flutter.Clip.none
                || noShapeClip()) {
            paintChildren.run();
            return;
        }
        if (!g.isShapeClipSupported()) {
            com.codename1.flutter.FlutterErrorReport.unimplemented("Material",
                    "this port cannot clip to a shape, so the corners paint square");
            paintChildren.run();
            return;
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
    private com.codename1.ui.geom.Shape clipShape(int x, int y, int w, int h,
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
        if (tr > 0) {
            p.quadTo(x + w, y, x + w, y + tr);
        }
        p.lineTo(x + w, y + h - br);
        if (br > 0) {
            p.quadTo(x + w, y + h, x + w - br, y + h);
        }
        p.lineTo(x + bl, y + h);
        if (bl > 0) {
            p.quadTo(x, y + h, x, y + h - bl);
        }
        p.lineTo(x, y + tl);
        if (tl > 0) {
            p.quadTo(x, y, x + tl, y);
        }
        p.closePath();
        return p;
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
