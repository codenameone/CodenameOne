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

        // Draw a deterministic marker first so the test always produces a
        // non-empty, comparable image even if the perspective branch is a
        // no-op on this graphics target. The earlier test relied entirely on
        // the perspective output being visible, which it isn't on platforms
        // that map clip space directly to pixels (the resulting fill lands
        // within ±1 pixel of the screen origin).
        g.setColor(0x008800);
        g.fillRect(x + w / 4, y + h / 4, w / 2, h / 2);

        // The mutable-image graphics on the iOS Metal port returns false here
        // even though the static Transform.isPerspectiveSupported() check
        // returns true for the global path. Use the per-graphics check.
        if (!g.isPerspectiveTransformSupported()) {
            g.setColor(0xaa0000);
            g.drawString("No perspective", x + 4, y + 4);
            return;
        }

        // Exercise the perspective API: build a perspective matrix, then
        // build a viewport-correcting transform that maps the perspective
        // output back to pixel coordinates inside this cell. Render a small
        // blue square that lands inside the green marker so we can confirm
        // the perspective branch produced visible output. The viewport
        // mapping pattern matches FlipTransition.paint() (line 295-307).
        float fovy = (float) (Math.PI / 4);
        float aspect = (float) w / (float) h;
        float zNear = 0.1f;
        float zFar = 1000f;

        Transform perspectiveT = Transform.makePerspective(fovy, aspect, zNear, zFar);
        float[] br = perspectiveT.transformPoint(new float[]{w, h, zNear});
        if (br[0] == 0f || br[1] == 0f) {
            // Defensive: avoid divide-by-zero if the perspective stub is a
            // no-op on this platform. Fall through with the marker only.
            g.setColor(0xaa0000);
            g.drawString("Perspective stub", x + 4, y + 4);
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
        g.setColor(0x0000aa);
        g.fillRect(x + w * 3 / 8, y + h * 3 / 8, w / 4, h / 4);
        g.setTransform(Transform.makeIdentity());
    }

    @Override
    protected String screenshotName() {
        return "graphics-transform-perspective";
    }
}
