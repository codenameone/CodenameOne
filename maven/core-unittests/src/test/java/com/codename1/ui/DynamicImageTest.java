package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.Style;

import static org.junit.jupiter.api.Assertions.*;

class DynamicImageTest extends UITestBase {

    private static class SampleDynamicImage extends DynamicImage {
        private boolean drawn;

        SampleDynamicImage() {
            super(20, 20);
        }

        @Override
        protected void drawImageImpl(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
            drawn = true;
            g.fillRect(x, y, w, h);
        }
    }

    @FormTest
    void testStyleCloningAndScaling() {
        SampleDynamicImage image = new SampleDynamicImage();
        Style style = new Style();
        style.setBgColor(0x123456);
        image.setStyle(style);
        assertNotSame(style, image.getStyle());
        assertEquals(0x123456, image.getStyle().getBgColor());
        style.setBgColor(0xffffff);
        assertEquals(0x123456, image.getStyle().getBgColor());

        image.scale(40, 30);
        assertEquals(40, image.getWidth());
        assertEquals(30, image.getHeight());

        Image filled = image.fill(50, 50);
        assertTrue(filled instanceof DynamicImage);
        assertEquals(50, filled.getWidth());
        assertEquals(50, filled.getHeight());

        Image masked = image.applyMask(new Object());
        assertTrue(masked instanceof DynamicImage);
    }

    @FormTest
    void testSetIconBindsStyle() {
        Label label = new Label();
        Style style = label.getStyle();
        style.setBgColor(0xabcdef);
        SampleDynamicImage base = new SampleDynamicImage();
        DynamicImage.setIcon(label, base);
        assertTrue(label.getIcon() instanceof DynamicImage);

        Image canvas = Image.createImage(30, 30);
        Graphics g = canvas.getGraphics();
        g.drawImage(label.getIcon(), 0, 0);
        assertTrue(base.drawn);
    }
}
