package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class TransformPerspective extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int x = bounds.getX();
        int y = bounds.getY();
        int w = bounds.getWidth();
        int h = bounds.getHeight();

        g.setColor(0xffffff);
        g.fillRect(x, y, w, h);
        g.setColor(0x000000);
        g.drawRect(x, y, w - 1, h - 1);

        // The static Transform.isPerspectiveSupported() check (vs the
        // per-graphics g.isPerspectiveTransformSupported() check) returns
        // true on every platform that has a working Matrix.makePerspective
        // implementation. This is the right gate when we project corners
        // ourselves and draw a 2D polygon -- we don't need the per-graphics
        // canvas/encoder to support perspective rasterization.
        if (!Transform.isPerspectiveSupported()) {
            g.setColor(0xaa0000);
            g.drawString("No perspective", x + 4, y + 4);
            g.setColor(0x008800);
            g.fillRect(x + w / 4, y + h / 4, w / 2, h / 2);
            return;
        }

        // Build Viewport * Perspective * Translate(model). Earlier the test
        // passed the raw clip-space output of makePerspective to fillRect,
        // which projected to a sub-pixel region. The first viewport-mapping
        // attempt used g.setTransform(mvp) followed by fillRect, but that
        // depends on the platform's draw path applying a 4x4 perspective
        // matrix to rect rasterization -- Android Canvas converts to a 3x3
        // Skia matrix (drops the Z axis) and rect rasterization on the
        // hardware canvas doesn't honour the perspective row reliably, and
        // the iOS Metal mutable-image graphics flags isPerspectiveTransform
        // Supported = false so the entire perspective branch was skipped.
        // Project the 4 model corners via transformPoint (which does the
        // homogeneous divide on every backend) and draw a 2D polygon, so
        // the rendering is uniform across all 4 panes on every platform.
        float fovy = (float) (Math.PI / 4);
        float aspect = (float) w / (float) h;
        float zNear = 1f;
        float zFar = 1000f;
        float modelZ = -300f; // z position of the centred 100x100 model quad

        Transform mvp = Transform.makeIdentity();
        // Viewport: NDC (-1..1) -> cell pixels. Y is flipped because
        // perspective NDC has +y up and screen has +y down.
        mvp.translate(x + w * 0.5f, y + h * 0.5f);
        mvp.scale(w * 0.5f, -h * 0.5f, 1f);
        // Perspective projection.
        Transform persp = Transform.makePerspective(fovy, aspect, zNear, zFar);
        mvp.concatenate(persp);
        // Push the quad into the frustum.
        mvp.translate(0, 0, modelZ);

        // Solid green quad (centred, no rotation) -- foreshortened only by
        // the perspective divide.
        g.setColor(0x008800);
        fillProjectedQuad(g, mvp, -50, -50, 100, 100);

        // Same quad rotated 36 deg around the Y axis. The left edge moves
        // toward the camera (renders larger) and the right edge away
        // (renders smaller), so the foreshortening is clearly visible vs
        // the unrotated green base.
        Transform rotated = mvp.copy();
        rotated.rotate((float) (Math.PI / 5), 0, 1, 0);
        g.setColor(0x0000aa);
        g.setAlpha(160);
        fillProjectedQuad(g, rotated, -50, -50, 100, 100);
        g.setAlpha(255);
    }

    private static void fillProjectedQuad(Graphics g, Transform t,
                                          int mx, int my, int mw, int mh) {
        float[] tl = t.transformPoint(new float[]{mx, my, 0});
        float[] tr = t.transformPoint(new float[]{mx + mw, my, 0});
        float[] br = t.transformPoint(new float[]{mx + mw, my + mh, 0});
        float[] bl = t.transformPoint(new float[]{mx, my + mh, 0});
        int[] xs = new int[]{(int) tl[0], (int) tr[0], (int) br[0], (int) bl[0]};
        int[] ys = new int[]{(int) tl[1], (int) tr[1], (int) br[1], (int) bl[1]};
        g.fillPolygon(xs, ys, 4);
    }

    @Override
    protected String screenshotName() {
        return "graphics-transform-perspective";
    }
}
