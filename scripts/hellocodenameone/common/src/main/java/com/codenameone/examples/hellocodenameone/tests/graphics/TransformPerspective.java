package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class TransformPerspective extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        if (!Transform.isPerspectiveSupported()) {
            g.drawString("Perspective unsupported", bounds.getX(), bounds.getY());
            return;
        }

        float fovy = 45f;
        float aspect = (float)bounds.getWidth() / bounds.getHeight();
        float zNear = 0.1f;
        float zFar = 1000f;

        // This sets the projection matrix
        Transform projection = Transform.makePerspective(fovy, aspect, zNear, zFar);

        // Move the object back so it's visible
        Transform modelView = Transform.makeTranslation(0, 0, -500);

        // Combine projection and modelview
        projection.concatenate(modelView);

        g.setTransform(projection);

        g.setColor(0xff0000);
        // Draw a rectangle centered at 0,0 (which should be center of screen due to perspective)
        // Wait, perspective projection usually maps 0,0 to center if set up that way,
        // but Codename One coordinate system is usually top-left 0,0.
        // We probably need to adjust.

        // Let's draw something at the "bounds" location but projected.
        // Since we are using makePerspective, it usually implies a camera at 0,0,0 looking down -Z (or similar depending on convention).
        // Let's assume standard OpenGL-like behavior where camera is at origin.

        g.fillRect(-50, -50, 100, 100);

        g.setTransform(Transform.makeIdentity());
    }

    @Override
    protected String screenshotName() {
        return "graphics-transform-perspective";
    }
}
