package com.codename1.flutter.widgets;

import com.codename1.flutter.FlutterErrorReport;
import com.codename1.flutter.Offset;
import com.codename1.flutter.Widget;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;

/**
 * Paints {@link Transform}'s subtree through a scale, rotation and/or translation.
 *
 * <p>Like Flutter's Transform this is a PAINT effect: the child is laid out at its
 * natural size and only the painting is transformed, so a scaled card still occupies the
 * same slot in its parent.</p>
 *
 * <p>The transform is applied about the element's centre, which is Flutter's default
 * (Alignment.center) and what the gallery's carousel expects. An explicit
 * {@code origin}/{@code alignment} is not honoured yet and is reported rather than
 * silently ignored.</p>
 */
public class TransformRenderElement extends EffectRenderElement {

    public TransformRenderElement(Transform widget) {
        super(widget);
    }

    private Transform transform() {
        return (Transform) widget();
    }

    @Override
    protected Widget effectChild() {
        return transform().getChild();
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Runnable paintChildren) {
        double sx = transform().effectiveScaleX();
        double sy = transform().effectiveScaleY();
        Double angle = transform().effectiveAngle();
        Offset offset = transform().effectiveOffset();

        // A/B switch, flipped at runtime with
        // Display.setProperty("cn1.flutter.noTransform","true"). Transform.scale is the
        // main per-frame difference between the carousel (21-29fps on iOS) and a plain
        // list (60fps), and a matrix set per card per frame is only measurable on a
        // device. Same trick as cn1.flutter.noShapeClip.
        boolean suppressed = "true".equals(com.codename1.ui.Display.getInstance()
                .getProperty("cn1.flutter.noTransform", "false"));
        boolean scales = !suppressed && (sx != 1.0 || sy != 1.0);
        boolean rotates = !suppressed && angle != null && angle.doubleValue() != 0.0;
        boolean translates = offset != null && (offset.dx() != 0 || offset.dy() != 0);
        if (!scales && !rotates && !translates) {
            paintChildren.run();
            return;
        }

        // A translation needs no matrix support: shifting the origin is enough, and it
        // works on every port.
        int dx = 0;
        int dy = 0;
        if (translates) {
            dx = (int) Math.round(com.codename1.flutter.rendering.Dp.px(offset.dx()));
            dy = (int) Math.round(com.codename1.flutter.rendering.Dp.px(offset.dy()));
            g.translate(dx, dy);
        }

        if ((scales || rotates) && !g.isTransformSupported()) {
            // Report rather than quietly dropping the visual: a port without matrix
            // support still gets the translation and the untransformed child.
            FlutterErrorReport.unimplemented("Transform",
                    "this platform has no transform support; scale and rotation are ignored");
            try {
                paintChildren.run();
            } finally {
                if (translates) {
                    g.translate(-dx, -dy);
                }
            }
            return;
        }

        com.codename1.ui.Transform saved = null;
        if (scales || rotates) {
            saved = g.getTransform();
            com.codename1.ui.Transform t = saved.copy();
            float cx = pane.getAbsoluteX() + pane.getWidth() / 2f;
            float cy = pane.getAbsoluteY() + pane.getHeight() / 2f;
            // Move the pivot to the centre, apply, move back — otherwise the subtree
            // scales away from the screen origin instead of growing in place.
            t.translate(cx, cy);
            if (rotates) {
                t.rotate((float) angle.doubleValue(), 0, 0);
            }
            if (scales) {
                t.scale((float) sx, (float) sy);
            }
            t.translate(-cx, -cy);
            g.setTransform(t);
        }
        try {
            paintChildren.run();
        } finally {
            if (saved != null) {
                g.setTransform(saved);
            }
            if (translates) {
                g.translate(-dx, -dy);
            }
        }
    }
}
