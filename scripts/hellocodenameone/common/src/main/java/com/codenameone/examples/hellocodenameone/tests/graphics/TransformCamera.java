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

        if (!Transform.isPerspectiveSupported()) {
            g.setColor(0xaa0000);
            g.drawString("No camera", x + 4, y + 4);
            g.setColor(0x884400);
            g.fillRect(x + w / 4, y + h / 4, w / 2, h / 2);
            return;
        }

        // Build Viewport * Perspective * Camera * Translate(model). The
        // earlier test passed the raw clip-space output to fillRect; the
        // first viewport-mapping attempt used g.setTransform(mvp) which
        // depended on the platform's rect rasterizer honouring a 4x4
        // perspective matrix (Android Canvas drops the Z axis on its 3x3
        // Skia matrix and rect rasterization doesn't honour the perspective
        // row reliably; iOS Metal mutable graphics gates the entire branch
        // off via isPerspectiveTransformSupported = false). Project the 4
        // model corners via transformPoint (which does the homogeneous
        // divide on every backend) and draw a 2D polygon, so the rendering
        // is uniform across all 4 panes on every platform.
        float fovy = (float) (Math.PI / 4);
        float aspect = (float) w / (float) h;
        float zNear = 1f;
        float zFar = 1000f;
        float modelZ = -300f;

        Transform mvp = Transform.makeIdentity();
        // Viewport: NDC -> cell pixels.
        mvp.translate(x + w * 0.5f, y + h * 0.5f);
        mvp.scale(w * 0.5f, -h * 0.5f, 1f);
        // Perspective projection.
        Transform persp = Transform.makePerspective(fovy, aspect, zNear, zFar);
        mvp.concatenate(persp);
        // Camera elevated on Y, looking down at the model centre. The
        // ~5.7 deg downward pitch shifts the rendered quad downward in the
        // cell and is visually distinct from TransformPerspective which
        // uses an implicit identity view.
        Transform camera = Transform.makeCamera(
                0f, 30f, 0f,    // eye -- elevated on y
                0f, 0f, modelZ, // looking at the model quad's centre
                0f, 1f, 0f);    // up
        mvp.concatenate(camera);
        // Place the model quad at z=modelZ in world space.
        mvp.translate(0, 0, modelZ);

        // Solid orange quad. The downward camera pitch shifts the quad
        // toward the bottom of the cell.
        g.setColor(0x884400);
        fillProjectedQuad(g, mvp, -50, -50, 100, 100);

        // Same quad rotated 36 deg around Y so the foreshortening is
        // visible against the camera-tilted base.
        Transform rotated = mvp.copy();
        rotated.rotate((float) (Math.PI / 5), 0, 1, 0);
        g.setColor(0x0044aa);
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
        return "graphics-transform-camera";
    }
}
