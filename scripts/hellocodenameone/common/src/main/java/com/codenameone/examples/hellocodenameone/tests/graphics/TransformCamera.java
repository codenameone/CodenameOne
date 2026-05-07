package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class TransformCamera extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int x = bounds.getX();
        int y = bounds.getY();
        int w = bounds.getWidth();
        int h = bounds.getHeight();

        // Always paint a known background and frame so the cell is visible
        // regardless of perspective/camera support.
        g.setColor(0xffffff);
        g.fillRect(x, y, w, h);
        g.setColor(0x000000);
        g.drawRect(x, y, w - 1, h - 1);

        // Deterministic marker first, so the cell always emits a non-empty
        // comparable image even if the camera branch is a no-op on this
        // graphics target. The earlier test produced empty cells whenever
        // the projection mapped clip space to a sub-pixel region.
        g.setColor(0x884400);
        g.fillRect(x + w / 4, y + h / 4, w / 2, h / 2);

        if (!g.isPerspectiveTransformSupported()) {
            g.setColor(0xaa0000);
            g.drawString("No camera", x + 4, y + 4);
            return;
        }

        // Build a view*projection matrix and a viewport-correcting transform
        // that maps the result back into this cell, following the pattern
        // used by FlipTransition.paint(). Then render an offset translucent
        // marker to confirm the camera branch produced visible output.
        float fovy = (float) (Math.PI / 4);
        float aspect = (float) w / (float) h;
        float zNear = 0.1f;
        float zFar = 1000f;

        Transform perspectiveT = Transform.makePerspective(fovy, aspect, zNear, zFar);
        Transform cameraT = Transform.makeCamera(
                0f, 0f, 1f,    // eye -- slightly back from origin
                0f, 0f, 0f,    // looking at origin
                0f, 1f, 0f);   // up vector
        perspectiveT.concatenate(cameraT);

        float[] br = perspectiveT.transformPoint(new float[]{w, h, zNear});
        if (br[0] == 0f || br[1] == 0f) {
            g.setColor(0xaa0000);
            g.drawString("Camera stub", x + 4, y + 4);
            return;
        }
        float xfactor = -w / br[0];
        float yfactor = -h / br[1];

        Transform t = Transform.makeIdentity();
        t.scale(xfactor, yfactor, 1f);
        t.translate((x + w * 0.5f) / xfactor, (y + h * 0.5f) / yfactor, 0);
        t.concatenate(perspectiveT);
        t.translate(-x - w * 0.5f, -y - h * 0.5f, -zNear - w * 0.5f);

        g.setTransform(t);
        g.setColor(0x0044aa);
        g.fillRect(x + w * 3 / 8, y + h * 3 / 8, w / 4, h / 4);
        g.setTransform(Transform.makeIdentity());
    }

    @Override
    protected String screenshotName() {
        return "graphics-transform-camera";
    }
}
