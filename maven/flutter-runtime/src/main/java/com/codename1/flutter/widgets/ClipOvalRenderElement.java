package com.codename1.flutter.widgets;

import com.codename1.flutter.Widget;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.GeneralPath;

/**
 * Clips its subtree to an ellipse inscribed in its box — Flutter's
 * {@code ClipOval}, and what makes an avatar round.
 *
 * <p>It was a pass-through, so every circular portrait in the app rendered as
 * a square photograph sitting on top of a round background: the mail study's
 * sender avatars, the contact rows, the profile chips.</p>
 */
public class ClipOvalRenderElement extends ClipRectRenderElement {

    /// The circle-through-Béziers constant: the control-point offset, as a
    /// fraction of the radius, that makes a cubic segment match a quarter arc.
    private static final double KAPPA = 0.5522847498307933;

    private GeneralPath path;
    private int pathX = Integer.MIN_VALUE;
    private int pathY = Integer.MIN_VALUE;
    private int pathW = -1;
    private int pathH = -1;

    public ClipOvalRenderElement(Widget widget) {
        super(widget);
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Subtree paintChildren) {
        int w = pane.getWidth();
        int h = pane.getHeight();
        boolean shaped;
        try {
            shaped = Display.isInitialized() && g.isShapeClipSupported();
        } catch (Throwable t) {
            shaped = false;
        }
        if (w <= 0 || h <= 0 || !shaped) {
            paintChildren.paint(g);
            return;
        }
        int[] saved = {g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight()};
        // Parent-relative; see ClipRRectRenderElement for why absolute is wrong.
        g.setClip(pathFor(pane.getX(), pane.getY(), w, h));
        try {
            paintChildren.paint(g);
        } finally {
            g.setClip(saved[0], saved[1], saved[2], saved[3]);
        }
    }

    /** The inscribed ellipse, rebuilt only when the box changes. */
    private GeneralPath pathFor(int x, int y, int w, int h) {
        // Reused rather than rebuilt while the box is unchanged; see
        // ClipRRectRenderElement for why that matters. The origin is part of
        // the key because the path is in parent-relative coordinates.
        if (path != null && pathX == x && pathY == y && pathW == w && pathH == h) {
            return path;
        }
        path = new GeneralPath();
        pathX = x;
        pathY = y;
        pathW = w;
        pathH = h;
        float rx = w / 2f;
        float ry = h / 2f;
        float cx = x + rx;
        float cy = y + ry;
        float ox = (float) (rx * KAPPA);
        float oy = (float) (ry * KAPPA);
        path.moveTo(cx - rx, cy);
        path.curveTo(cx - rx, cy - oy, cx - ox, cy - ry, cx, cy - ry);
        path.curveTo(cx + ox, cy - ry, cx + rx, cy - oy, cx + rx, cy);
        path.curveTo(cx + rx, cy + oy, cx + ox, cy + ry, cx, cy + ry);
        path.curveTo(cx - ox, cy + ry, cx - rx, cy + oy, cx - rx, cy);
        path.closePath();
        return path;
    }
}
