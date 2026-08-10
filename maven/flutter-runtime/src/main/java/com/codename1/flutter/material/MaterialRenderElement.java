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
                    border = border
                            .shadowOpacity(Math.min(255, (int) Math.round(20 + elevation * 15)))
                            .shadowSpread((float) Math.min(3, 0.25f + elevation * 0.25f))
                            .shadowY(1);
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
        // replacing outright would let a half-scrolled card paint outside its viewport.
        //
        // Which of the two forms below applies matters, and was established by trying it:
        // GeneralPath.intersection() does NOT survive the case where the path is entirely
        // inside the rectangle - going through it unconditionally cut the icons out of every
        // category row. So intersect only when we genuinely overflow the clip, and use the
        // plain rounded rect when we do not, which is the common case and the correct one.
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        com.codename1.ui.geom.GeneralPath rounded =
                roundedRect(x, y, w, h, Math.min(radius, Math.min(w, h) / 2));
        boolean insideClip = x >= cx && y >= cy && x + w <= cx + cw && y + h <= cy + ch;
        try {
            g.setClip(insideClip
                    ? (com.codename1.ui.geom.Shape) rounded
                    : rounded.intersection(new com.codename1.ui.geom.Rectangle(cx, cy, cw, ch)));
            paintChildren.run();
        } finally {
            g.setClip(cx, cy, cw, ch);
        }
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

    /// A rounded rectangle in the coordinate space a component paints in - parent-relative,
    /// because the Graphics has already accumulated its ancestors' translation.
    private static com.codename1.ui.geom.GeneralPath roundedRect(int x, int y, int w, int h, int r) {
        com.codename1.ui.geom.GeneralPath p = new com.codename1.ui.geom.GeneralPath();
        p.moveTo(x + r, y);
        p.lineTo(x + w - r, y);
        p.quadTo(x + w, y, x + w, y + r);
        p.lineTo(x + w, y + h - r);
        p.quadTo(x + w, y + h, x + w - r, y + h);
        p.lineTo(x + r, y + h);
        p.quadTo(x, y + h, x, y + h - r);
        p.lineTo(x, y + r);
        p.quadTo(x, y, x + r, y);
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
