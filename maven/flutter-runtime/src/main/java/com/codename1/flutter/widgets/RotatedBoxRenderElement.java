package com.codename1.flutter.widgets;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;

/**
 * Lays out and paints a {@link RotatedBox}: quarter turns that affect LAYOUT, not just
 * painting.
 *
 * <p>That is the whole difference from {@code Transform.rotate} — an odd number of quarter
 * turns swaps the box's width and height, so the parent reserves the rotated footprint. The
 * child is measured against constraints with the axes swapped, and the painting is rotated
 * about the centre to match.</p>
 */
public class RotatedBoxRenderElement extends EffectRenderElement {

    public RotatedBoxRenderElement(RotatedBox widget) {
        super(widget);
    }

    private RotatedBox box() {
        return (RotatedBox) widget();
    }

    /** Quarter turns normalised to 0..3; only the parity affects the axes. */
    private int turns() {
        long q = box().getQuarterTurns() % 4;
        return (int) (q < 0 ? q + 4 : q);
    }

    private boolean swapsAxes() {
        return (turns() & 1) == 1;
    }

    @Override
    protected Widget effectChild() {
        return box().getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        if (!swapsAxes()) {
            return super.performLayout(constraints);
        }
        // Measure the child in the ROTATED frame, then report its footprint swapped back.
        BoxConstraints swapped = new BoxConstraints(constraints.minHeight(),
                constraints.maxHeight(), constraints.minWidth(), constraints.maxWidth());
        Size child = super.performLayout(swapped);
        return constraints.constrain(new Size(child.height(), child.width()));
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Runnable paintChildren) {
        int t = turns();
        if (t == 0) {
            paintChildren.run();
            return;
        }
        if (!g.isTransformSupported()) {
            com.codename1.flutter.FlutterErrorReport.unimplemented("RotatedBox",
                    "this platform has no transform support; the rotation is not painted");
            paintChildren.run();
            return;
        }
        com.codename1.ui.Transform saved = g.getTransform();
        com.codename1.ui.Transform r = saved.copy();
        float cx = pane.getAbsoluteX() + pane.getWidth() / 2f;
        float cy = pane.getAbsoluteY() + pane.getHeight() / 2f;
        r.translate(cx, cy);
        r.rotate((float) (t * Math.PI / 2), 0, 0);
        r.translate(-cx, -cy);
        g.setTransform(r);
        try {
            paintChildren.run();
        } finally {
            g.setTransform(saved);
        }
    }
}
