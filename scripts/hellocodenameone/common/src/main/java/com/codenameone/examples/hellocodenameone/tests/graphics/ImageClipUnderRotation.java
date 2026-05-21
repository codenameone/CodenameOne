package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.EncodedImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

// Issue #3921: image drawn inside a rotated clip should be clipped to the
// rotated rect, not its axis-aligned bounding box, and not left unclipped.
//
// Per cell: draw a recognisable test image, rotate the Graphics 30deg
// around the image centre, clipRect a slightly inset rectangle (which is
// a rotated rectangle in screen space, i.e. a polygon clip), then draw
// the same image again on top. After popClip, draw a navy outline of the
// same inset rect un-rotated as a reference.
//
// Correct: the over-painted image appears as a 30deg-tilted square,
//          overhanging the navy outline at two diagonal corners.
// Bug:     the over-paint matches the navy outline exactly (rasteriser
//          collapsed the polygon clip to its bbox), or covers the whole
//          underlying image (rasteriser dropped the clip entirely).
//
// Uses pushClip/popClip only -- no getClip/setClip(int[]) -- so the
// rectangular-int[4] limitation that ddyer0 himself called out on the
// issue can't confound the rasterisation signal.
public class ImageClipUnderRotation extends AbstractGraphicsScreenshotTest {
    private static final int TEST_IMAGE_SIZE = 120;

    private EncodedImage testImage;

    // Test image: solid yellow with a thick magenta border and a thick
    // black diagonal X. Yellow + magenta + black are all easy to tell
    // apart from the gray background and the navy reference outline.
    private EncodedImage buildTestImage() {
        Image src = Image.createImage(TEST_IMAGE_SIZE, TEST_IMAGE_SIZE);
        Graphics gc = src.getGraphics();
        gc.setAntiAliased(false);
        gc.setColor(0xffff00);
        gc.fillRect(0, 0, TEST_IMAGE_SIZE, TEST_IMAGE_SIZE);
        gc.setColor(0xff00ff);
        int border = 10;
        gc.fillRect(0, 0, TEST_IMAGE_SIZE, border);
        gc.fillRect(0, TEST_IMAGE_SIZE - border, TEST_IMAGE_SIZE, border);
        gc.fillRect(0, 0, border, TEST_IMAGE_SIZE);
        gc.fillRect(TEST_IMAGE_SIZE - border, 0, border, TEST_IMAGE_SIZE);
        gc.setColor(0x000000);
        gc.drawLine(0, 0, TEST_IMAGE_SIZE - 1, TEST_IMAGE_SIZE - 1);
        gc.drawLine(0, TEST_IMAGE_SIZE - 1, TEST_IMAGE_SIZE - 1, 0);
        return EncodedImage.createFromImage(src, true);
    }

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int x = bounds.getX();
        int y = bounds.getY();
        int w = bounds.getWidth();
        int h = bounds.getHeight();

        g.setColor(0xeeeeee);
        g.fillRect(x, y, w, h);

        if (!Transform.isSupported()) {
            g.setColor(0);
            g.drawString("Affine unsupported", x + 4, y + 4);
            return;
        }
        if (testImage == null) {
            testImage = buildTestImage();
        }

        // Centre a square image region of side = 70% of min(w, h) in the
        // cell, then inset by 12% on every side for the inner clip. This
        // keeps the geometry identical across all four cells regardless
        // of their size or position, and leaves a generous gray margin
        // so a Bug-no-clip render is unmistakable.
        int side = (int)(Math.min(w, h) * 0.7f);
        int imgX = x + (w - side) / 2;
        int imgY = y + (h - side) / 2;
        int inset = side / 8;
        int clipX = imgX + inset;
        int clipY = imgY + inset;
        int clipW = side - inset * 2;
        int clipH = side - inset * 2;
        int pivotX = imgX + side / 2;
        int pivotY = imgY + side / 2;

        // Underlay: a dim greyed-out copy of the image at full size, no
        // clip applied. This gives a baseline so the over-painted clipped
        // copy is visually anchored against the full image footprint.
        g.setAlpha(64);
        g.drawImage(testImage, imgX, imgY, side, side);
        g.setAlpha(255);

        // Clipped rotated over-paint. After popClip the transform is
        // restored to identity so the navy outline below lands where the
        // un-rotated inset rect would.
        g.pushClip();
        float angle = (float)(Math.PI / 6); // 30deg
        g.rotateRadians(angle, pivotX, pivotY);
        g.clipRect(clipX, clipY, clipW, clipH);
        g.drawImage(testImage, imgX, imgY, side, side);
        g.rotateRadians(-angle, pivotX, pivotY);
        g.popClip();

        // Navy reference outline of the un-rotated inset rect: the
        // bbox-bug render would line up with this exactly; the correct
        // render is a tilted square overhanging it at two corners.
        g.setColor(0x000080);
        g.drawRect(clipX, clipY, clipW - 1, clipH - 1);
    }

    @Override
    protected String screenshotName() {
        return "graphics-image-clip-under-rotation";
    }
}
