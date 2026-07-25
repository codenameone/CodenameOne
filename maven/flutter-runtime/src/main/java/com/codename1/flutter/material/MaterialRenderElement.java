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
        // Clip.antiAlias would cut the subtree to the rounded shape; not done yet, so a
        // child that fills the surface still paints square corners over the rounded
        // background. Reported rather than left as a silent visual difference.
        if (cornerRadiusLp() > 0
                && material().getClipBehavior() != com.codename1.flutter.Clip.none) {
            com.codename1.flutter.FlutterErrorReport.unimplemented("Material",
                    "clipBehavior is ignored; a child filling the surface paints over its rounded corners");
        }
        paintChildren.run();
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
