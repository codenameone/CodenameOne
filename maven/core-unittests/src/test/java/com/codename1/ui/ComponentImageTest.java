package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;

import static org.junit.jupiter.api.Assertions.*;

class ComponentImageTest extends UITestBase {

    @FormTest
    void testSizingAndScaling() {
        Label label = new Label("Image");
        label.setPreferredSize(new Dimension(30, 20));
        ComponentImage image = new ComponentImage(label, 30, 20);
        assertEquals(30, image.getWidth());
        assertEquals(20, image.getHeight());

        image.scale(40, 25);
        assertEquals(40, image.getWidth());
        assertEquals(25, image.getHeight());

        Image filled = image.fill(60, 40);
        assertTrue(filled instanceof ComponentImage);
        assertEquals(60, filled.getWidth());
        assertEquals(40, filled.getHeight());

        Image masked = image.applyMask(new Object());
        assertTrue(masked instanceof ComponentImage);
    }

    @FormTest
    void testPulsingAnimationAndDraw() {
        Label label = new Label("Animate");
        label.setPreferredSize(new Dimension(20, 20));
        ComponentImage image = new ComponentImage(label, 20, 20);
        image.enablePulsingAnimation(0, 0.5, 0.2, 1.0);
        assertTrue(image.isPulsingAnimationEnabled());
        assertTrue(image.animate());
        image.disablePulsingAnimation();
        assertFalse(image.isPulsingAnimationEnabled());

        image.setAnimation(true);
        assertTrue(image.isAnimation());

        Image canvas = Image.createImage(40, 40);
        Graphics g = canvas.getGraphics();
        g.drawImage(image, 0, 0);
    }

    @FormTest
    void testToEncodedImageScaling() {
        Label label = new Label("Encoded");
        label.setPreferredSize(new Dimension(25, 25));
        ComponentImage image = new ComponentImage(label, 25, 25);
        EncodedImage encoded = image.toEncodedImage();
        assertEquals(25, encoded.getWidth());
        assertEquals(25, encoded.getHeight());

        EncodedImage scaled = encoded.scaledEncoded(50, 50);
        assertEquals(50, scaled.getWidth());
        assertEquals(50, scaled.getHeight());
    }
}
