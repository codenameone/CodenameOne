package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import static org.junit.jupiter.api.Assertions.*;

class RGBImageTest extends UITestBase {

    private RGBImage createSampleImage() {
        int[] rgb = new int[]{0xff0000ff, 0xff00ff00, 0xffff0000, 0xffffffff};
        return new RGBImage(rgb, 2, 2);
    }

    @FormTest
    void testScalingAndSubImage() {
        RGBImage image = createSampleImage();
        Image scaled = image.scaled(4, 4);
        assertEquals(4, scaled.getWidth());
        assertEquals(4, scaled.getHeight());

        image.scale(1, 2);
        assertEquals(1, image.getWidth());
        assertEquals(2, image.getHeight());

        Image sub = image.subImage(0, 0, 1, 1, true);
        assertEquals(1, sub.getWidth());
        assertEquals(1, sub.getHeight());
    }

    @FormTest
    void testModifyAlphaAndOpaque() {
        RGBImage image = createSampleImage();
        Image modified = image.modifyAlpha((byte) 0x80);
        int[] rgb = ((RGBImage) modified).getRGB();
        for (int pixel : rgb) {
            assertEquals(0x80000000 & 0xff000000, pixel & 0xff000000);
        }

        assertFalse(image.isOpaque());
        image.setOpaque(true);
        assertTrue(image.isOpaque());
    }

    @FormTest
    void testDrawImageAndGetRGB() {
        RGBImage image = createSampleImage();
        int[] dest = new int[4];
        image.getRGB(dest, 0, 0, 0, 2, 2);
        assertArrayEquals(image.getRGB(), dest);
        assertTrue(image.requiresDrawImage());

        Image canvas = Image.createImage(4, 4);
        Graphics g = canvas.getGraphics();
        g.drawImage(image, 0, 0);
    }
}
