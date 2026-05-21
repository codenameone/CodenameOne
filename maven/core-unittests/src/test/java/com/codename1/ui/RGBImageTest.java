package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Transform;

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
    void testRgbImageModifyAlphaSimdMatchesScalar() {
        RGBImage image = new RGBImage(new int[]{
                0x00FF0000, 0xFFFF0000,
                0x8000FF00, 0xFF0000FF
        }, 2, 2);
        try {
            Image.setSimdOptimizationsEnabled(false);
            RGBImage scalar = (RGBImage) image.modifyAlpha((byte) 0x40);
            Image.setSimdOptimizationsEnabled(true);
            RGBImage simd = (RGBImage) image.modifyAlpha((byte) 0x40);
            assertArrayEquals(scalar.getRGB(), simd.getRGB());
        } finally {
            Image.resetSimdOptimizationsEnabled();
        }
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

    // Regression test for https://github.com/codenameone/CodenameOne/issues/4188:
    // the (w, h) overload of drawImage used to fall through Image#drawImage, which
    // dispatches through the (null) native peer and renders nothing. The new
    // override pushes a translate + scale affine transform onto the graphics
    // context and emits drawRGB at native size, so the platform pipeline applies
    // the scaling.
    @FormTest
    void testScaledDrawImageIntegerScale() {
        int red = 0xffff0000;
        int green = 0xff00ff00;
        int blue = 0xff0000ff;
        int white = 0xffffffff;
        RGBImage source = new RGBImage(new int[]{red, green, blue, white}, 2, 2);

        Image canvas = Image.createImage(4, 4, 0xff000000);
        Graphics g = canvas.getGraphics();
        g.drawImage(source, 0, 0, 4, 4);

        int[] actual = canvas.getRGB();
        int[] expected = new int[]{
                red,   red,   green, green,
                red,   red,   green, green,
                blue,  blue,  white, white,
                blue,  blue,  white, white
        };
        assertArrayEquals(expected, actual,
                "2x integer upscale of RGBImage should replicate each source pixel into a 2x2 block");
    }

    @FormTest
    void testScaledDrawImageAtOffset() {
        int red = 0xffff0000;
        int green = 0xff00ff00;
        int blue = 0xff0000ff;
        int white = 0xffffffff;
        int bg = 0xff000000;
        RGBImage source = new RGBImage(new int[]{red, green, blue, white}, 2, 2);

        Image canvas = Image.createImage(6, 6, bg);
        Graphics g = canvas.getGraphics();
        g.drawImage(source, 1, 1, 4, 4);

        int[] actual = canvas.getRGB();
        int[] expected = new int[]{
                bg,  bg,    bg,    bg,    bg,    bg,
                bg,  red,   red,   green, green, bg,
                bg,  red,   red,   green, green, bg,
                bg,  blue,  blue,  white, white, bg,
                bg,  blue,  blue,  white, white, bg,
                bg,  bg,    bg,    bg,    bg,    bg
        };
        assertArrayEquals(expected, actual,
                "Scaled draw at (1,1) of a 4x4 region should land within the inner 4x4 of a 6x6 canvas");
    }

    @FormTest
    void testScaledDrawImageDownscale() {
        int red = 0xffff0000;
        int green = 0xff00ff00;
        int blue = 0xff0000ff;
        int white = 0xffffffff;
        int[] src = new int[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int color;
                if (row < 2 && col < 2) color = red;
                else if (row < 2) color = green;
                else if (col < 2) color = blue;
                else color = white;
                src[row * 4 + col] = color;
            }
        }
        RGBImage source = new RGBImage(src, 4, 4);

        Image canvas = Image.createImage(2, 2, 0xff000000);
        Graphics g = canvas.getGraphics();
        g.drawImage(source, 0, 0, 2, 2);

        int[] actual = canvas.getRGB();
        int[] expected = new int[]{red, green, blue, white};
        assertArrayEquals(expected, actual,
                "2x integer downscale should pick one representative pixel per source quadrant");
    }

    // Regression test for the screen-rendering case: on ports where
    // impl.isTranslationSupported() is false (iOS), g.translate(...) calls
    // accumulate xTranslate/yTranslate on the Graphics object and are baked
    // into drawRGB coordinates BEFORE the impl matrix is applied. A naked
    // translateMatrix + scale composition would multiply that accumulator
    // by the scale factor, shifting the image off-target. The fix uses
    // setTransform, which conjugates the matrix with T(xT, yT) so the
    // image lands at (xT + x, yT + y) regardless of the scale factor.
    @FormTest
    void testScaledDrawImageRespectsPriorGraphicsTranslate() {
        int red = 0xffff0000;
        int green = 0xff00ff00;
        int blue = 0xff0000ff;
        int white = 0xffffffff;
        int bg = 0xff000000;
        RGBImage source = new RGBImage(new int[]{red, green, blue, white}, 2, 2);

        Image canvas = Image.createImage(8, 8, bg);
        Graphics g = canvas.getGraphics();
        g.translate(2, 2);
        g.drawImage(source, 0, 0, 4, 4);

        int[] actual = canvas.getRGB();
        int[] expected = new int[]{
                bg, bg, bg,    bg,    bg,    bg,    bg, bg,
                bg, bg, bg,    bg,    bg,    bg,    bg, bg,
                bg, bg, red,   red,   green, green, bg, bg,
                bg, bg, red,   red,   green, green, bg, bg,
                bg, bg, blue,  blue,  white, white, bg, bg,
                bg, bg, blue,  blue,  white, white, bg, bg,
                bg, bg, bg,    bg,    bg,    bg,    bg, bg,
                bg, bg, bg,    bg,    bg,    bg,    bg, bg
        };
        assertArrayEquals(expected, actual,
                "Scaled draw should land at (xTranslate + x, yTranslate + y) -- the prior g.translate " +
                "must not be multiplied by the scale factor");
    }

    @FormTest
    void testScaledDrawImagePreservesPriorTransform() {
        RGBImage source = createSampleImage();

        Image canvas = Image.createImage(4, 4, 0xff000000);
        Graphics g = canvas.getGraphics();
        Transform before = Transform.makeIdentity();
        g.getTransform(before);

        g.drawImage(source, 0, 0, 4, 4);

        Transform after = Transform.makeIdentity();
        g.getTransform(after);
        assertTrue(before.equals(after),
                "drawImage(w,h) must restore the graphics transform it modified");
    }
}
