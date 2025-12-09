package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class TransformCamera extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        if (!Transform.isPerspectiveSupported()) {
            g.drawString("Perspective unsupported", bounds.getX(), bounds.getY());
            return;
        }

        float eyeX = 0;
        float eyeY = 0;
        float eyeZ = 500;
        float centerX = 0;
        float centerY = 0;
        float centerZ = 0;
        float upX = 0;
        float upY = 1;
        float upZ = 0;

        Transform t = Transform.makeCamera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);

        // We probably also need a projection matrix for the camera to make sense visually?
        // Or does makeCamera include projection?
        // Typically makeCamera (lookAt) creates a View matrix. We still need Projection.

        float fovy = 45f;
        float aspect = (float)bounds.getWidth() / bounds.getHeight();
        Transform proj = Transform.makePerspective(fovy, aspect, 0.1f, 1000f);

        proj.concatenate(t);

        g.setTransform(proj);

        g.setColor(0x00ff00);
        g.fillRect(-50, -50, 100, 100);

        // Rotate the camera/object slightly to verify 3D
        Transform rot = Transform.makeRotation((float)(Math.PI / 4), 0, 1, 0); // Rotate around Y
        proj.concatenate(rot); // Apply rotation
        g.setTransform(proj);

        g.setColor(0x0000ff);
        g.setAlpha(128);
        g.fillRect(-50, -50, 100, 100);

        g.setTransform(Transform.makeIdentity());
    }

    @Override
    protected String screenshotName() {
        return "graphics-transform-camera";
    }
}
