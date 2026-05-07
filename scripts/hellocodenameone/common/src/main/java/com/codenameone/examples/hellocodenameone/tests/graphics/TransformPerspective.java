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

        // Always paint a known background and frame so the cell is visible
        // regardless of perspective support. Earlier the test left the cell
        // empty whenever the projection produced clip-space coordinates that
        // the renderer mapped to a sub-pixel region.
        g.setColor(0xffffff);
        g.fillRect(x, y, w, h);
        g.setColor(0x000000);
        g.drawRect(x, y, w - 1, h - 1);

        // The mutable-image graphics on the iOS Metal port returns false here
        // even though the static Transform.isPerspectiveSupported() check
        // returns true for the global path. Use the per-graphics check.
        if (!g.isPerspectiveTransformSupported()) {
            g.setColor(0xaa0000);
            g.drawString("No perspective", x + 4, y + 4);
            // Always emit a marker so the cell isn't blank on platforms that
            // can't run perspective on this graphics target.
            g.setColor(0x008800);
            g.fillRect(x + w / 4, y + h / 4, w / 2, h / 2);
            return;
        }

        // Build a Viewport * Perspective * Model matrix that lands inside the
        // cell. Earlier this test passed the raw clip-space output of
        // makePerspective straight to fillRect, so the rect projected to a
        // sub-pixel region around the screen origin and rendered nothing
        // visible. The FlipTransition viewport pattern collapses at cell
        // scale (the small per-cell scale factor multiplied back through the
        // perspective gives nearly identity), so build the viewport directly:
        // Viewport(NDC -> cell pixels) * Perspective * Translate(viewer).
        float fovy = (float) (Math.PI / 4);
        float aspect = (float) w / (float) h;
        float zNear = 1f;
        float zFar = 1000f;
        // Place the model quad at z=zViewer in view space. For portrait cells
        // aspect = w/h < 1, so x is amplified more than y by the perspective.
        // |zViewer| ~= 300 keeps a 100x100 quad inside NDC (±1) on portrait
        // screens with headroom for a 36 deg Y rotation.
        float zViewer = -300f;

        Transform mvp = Transform.makeIdentity();
        // Viewport: NDC (-1..1, -1..1) -> cell pixels (cell_x..cell_x+w,
        // cell_y..cell_y+h). Y is flipped because perspective NDC has +y up
        // but screen has +y down.
        mvp.translate(x + w * 0.5f, y + h * 0.5f);
        mvp.scale(w * 0.5f, -h * 0.5f, 1f);
        // Perspective projection.
        Transform persp = Transform.makePerspective(fovy, aspect, zNear, zFar);
        mvp.concatenate(persp);
        // Model translation: push the quad into the frustum.
        mvp.translate(0, 0, zViewer);

        g.setTransform(mvp);

        // Solid green quad (centred, no rotation) -- foreshortened only by
        // the perspective divide.
        g.setColor(0x008800);
        g.fillRect(-50, -50, 100, 100);

        // Same quad rotated 36 deg around the Y axis so the foreshortening
        // is visible -- left edge moves toward the camera, right edge away.
        Transform rotated = mvp.copy();
        rotated.rotate((float) (Math.PI / 5), 0, 1, 0);
        g.setTransform(rotated);
        g.setColor(0x0000aa);
        g.setAlpha(160);
        g.fillRect(-50, -50, 100, 100);
        g.setAlpha(255);

        g.setTransform(Transform.makeIdentity());
    }

    @Override
    protected String screenshotName() {
        return "graphics-transform-perspective";
    }
}
