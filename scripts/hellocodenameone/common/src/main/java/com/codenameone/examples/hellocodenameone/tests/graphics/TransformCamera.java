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

        g.setColor(0xffffff);
        g.fillRect(x, y, w, h);
        g.setColor(0x000000);
        g.drawRect(x, y, w - 1, h - 1);

        if (!g.isPerspectiveTransformSupported()) {
            g.setColor(0xaa0000);
            g.drawString("No camera", x + 4, y + 4);
            // Always emit a marker so the cell isn't blank on platforms that
            // can't run perspective on this graphics target.
            g.setColor(0x884400);
            g.fillRect(x + w / 4, y + h / 4, w / 2, h / 2);
            return;
        }

        // Build Viewport * Perspective * Camera * ModelTranslate. The earlier
        // test passed the raw clip-space output of makeCamera/makePerspective
        // straight to fillRect, so the result projected to a sub-pixel region
        // around the screen origin and rendered nothing visible. The
        // FlipTransition viewport pattern collapses at cell scale, so the
        // viewport is built directly here.
        float fovy = (float) (Math.PI / 4);
        float aspect = (float) w / (float) h;
        float zNear = 1f;
        float zFar = 1000f;
        float modelZ = -300f; // z position of the centred 100x100 model quad

        Transform mvp = Transform.makeIdentity();
        // Viewport: NDC (-1..1, -1..1) -> cell pixels (cell_x..cell_x+w,
        // cell_y..cell_y+h). Y is flipped because perspective NDC has +y up
        // but screen has +y down.
        mvp.translate(x + w * 0.5f, y + h * 0.5f);
        mvp.scale(w * 0.5f, -h * 0.5f, 1f);
        // Perspective projection.
        Transform persp = Transform.makePerspective(fovy, aspect, zNear, zFar);
        mvp.concatenate(persp);
        // Camera elevated on Y, looking down at the model centre. The
        // tilt-down view shifts the rendered quad toward the bottom of the
        // cell and is visually distinct from TransformPerspective which uses
        // an implicit identity view. eye y=30 looking at z=-300 yields a
        // ~5.7 deg downward pitch.
        Transform camera = Transform.makeCamera(
                0f, 30f, 0f,    // eye -- elevated on y
                0f, 0f, modelZ, // looking at the model quad's centre
                0f, 1f, 0f);    // up
        mvp.concatenate(camera);
        // Model translation: place the quad at z=modelZ in world space.
        mvp.translate(0, 0, modelZ);

        g.setTransform(mvp);

        // Solid orange quad. Camera offset rotates the view, so the quad
        // appears shifted left and slightly tilted.
        g.setColor(0x884400);
        g.fillRect(-50, -50, 100, 100);

        // Same quad rotated 36 deg around Y so the foreshortening is
        // clearly visible against the camera-tilted base.
        Transform rotated = mvp.copy();
        rotated.rotate((float) (Math.PI / 5), 0, 1, 0);
        g.setTransform(rotated);
        g.setColor(0x0044aa);
        g.setAlpha(160);
        g.fillRect(-50, -50, 100, 100);
        g.setAlpha(255);

        g.setTransform(Transform.makeIdentity());
    }

    @Override
    protected String screenshotName() {
        return "graphics-transform-camera";
    }
}
