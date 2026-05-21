package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.EncodedImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

// Targeted repro for issue #3921 ("Clipping region not respected with
// non-90 degree rotations") in the exact shape ddyer0 reported: an image
// drawn inside a rotated rect-clip while the outer Graphics already has a
// scale + translate applied. The companion ClipUnderRotation test covers
// the same polygon-clip rasterisation path with a fillRect; this one
// stresses the drawImage code path on top of it, because the original
// report's screenshots show a misclipped EncodedImage, not a misclipped
// fillRect.
//
// Differences from ddyer0's dtest.java:
//   - pushClip / popClip is used for save / restore. The original repro
//     used `int[] clip = g.getClip(); ...; g.setClip(clip);` which can't
//     by API contract preserve a non-axis-aligned clip shape (a rotated
//     rectangle on screen). ddyer0's own follow-up comment on the issue
//     identified that round-trip as the source of his visible artefact;
//     it is not a port bug, it is the rectangular shape of the int[4]
//     return of getClip(). Using pushClip/popClip isolates the
//     rasterisation half of the bug from that API limitation.
//   - The "translate" vs "translateMatrix" simulator switch from the
//     original repro is dropped; this test goes through the normal
//     Graphics.translate / scale / rotateRadians path on every port.
//
// Sequence inside drawContent (per cell):
//   pushClip
//   clipRect(cell)              outer axis-aligned clip
//   scale(s, s)                 outer scale
//   translate(tx, ty)           outer translate (post-scale)
//   pushClip
//   rotateRadians(30deg, pivot) rotate around image centre
//   clipRect(inner)             intersect; screen-space clip is now a
//                               rotated rect (polygon)
//   drawImage(testImage, ...)   draw the recognisable test pattern
//   rotateRadians(-30deg, ...)  unrotate
//   popClip                     restore axis-aligned outer clip
//   translate(-tx, -ty)
//   scale(1/s, 1/s)
//   popClip                     restore identity-transform clip
//   drawRect(inner outline)     navy reference: where an axis-aligned
//                               bbox-clipped render would land
//
// Possible renders, visually distinguishable against the navy outline:
//
//   - Correct: a 30deg-tilted slice of the test image (yellow / green
//     border / black X), overhanging the navy outline on two corners
//     and falling short on the opposite two. The polygon clip was
//     honoured.
//   - Bug A (clip widened to axis-aligned bbox): a slice of the image
//     that matches the navy outline exactly. The rasteriser collapsed
//     the rotated-rect clip to its bbox.
//   - Bug B (polygon clip dropped entirely): the full test image is
//     drawn at its native rectangle, swamping the navy outline. The
//     rasteriser saw a polygon clip and disabled clipping. Suspected
//     iOS Metal failure mode before the stencil-clip work landed
//     (319c758b6, c56e7aab0, 1a5b132a0).
//
// The 2x2 grid emitted by AbstractGraphicsScreenshotTest separates the
// form-graphics path (top cells) from the mutable-image path (bottom
// cells), and anti-aliasing off/on left vs. right. A bug that only
// shows up on the mutable-image (drawImage) path stays localised.
public class ImageClipUnderRotation extends AbstractGraphicsScreenshotTest {
    // ddyer0's reproduction image: blue square, black X across the
    // diagonal, green 4-pixel border. The border is what makes the
    // clipping shape obvious: a correctly-clipped rotated rect shows
    // diagonal slivers of green that an axis-aligned bbox clip can't
    // produce.
    private static final int TEST_IMAGE_SIZE = 100;

    private EncodedImage testImage;

    private EncodedImage buildTestImage() {
        Image src = Image.createImage(TEST_IMAGE_SIZE, TEST_IMAGE_SIZE);
        Graphics gc = src.getGraphics();
        gc.setColor(0x0000ff);
        gc.fillRect(0, 0, TEST_IMAGE_SIZE, TEST_IMAGE_SIZE);
        gc.setColor(0x000000);
        gc.drawLine(0, 0, TEST_IMAGE_SIZE, TEST_IMAGE_SIZE);
        gc.drawLine(0, TEST_IMAGE_SIZE, TEST_IMAGE_SIZE, 0);
        gc.setColor(0x00ff00);
        gc.fillRect(0, 0, TEST_IMAGE_SIZE, 4);
        gc.fillRect(0, 0, 4, TEST_IMAGE_SIZE);
        gc.fillRect(0, TEST_IMAGE_SIZE - 4, TEST_IMAGE_SIZE, 4);
        gc.fillRect(TEST_IMAGE_SIZE - 4, 0, 4, TEST_IMAGE_SIZE);
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

        // Geometry: target a 60x60 image drawn at a logical (xp, yp) under
        // a 2.5x scale and a small translate offset, so the outer
        // transform stack matches ddyer0's repro shape (scale + translate
        // + per-tile rotate + clipRect). Everything is anchored to the
        // cell bounds so the test renders consistently for both the form
        // and mutable cell sizes.
        float scale = 2.5f;
        int imageDrawSize = 60;
        int clipInset = 2;
        int clipSize = 56;
        int xp = (int)((w / scale) / 2 - imageDrawSize / 2);
        int yp = (int)((h / scale) / 2 - imageDrawSize / 2);
        int pivotX = xp + imageDrawSize / 2;
        int pivotY = yp + imageDrawSize / 2;
        int tx = x;
        int ty = y;

        g.pushClip();
        g.clipRect(x, y, w, h);
        g.scale(scale, scale);
        g.translate((int)(tx / scale), (int)(ty / scale));

        g.pushClip();
        float angle = (float)(Math.PI / 6); // 30deg
        g.rotateRadians(angle, pivotX, pivotY);
        g.clipRect(xp + clipInset, yp + clipInset, clipSize, clipSize);
        g.drawImage(testImage, xp, yp, imageDrawSize, imageDrawSize);
        g.rotateRadians(-angle, pivotX, pivotY);
        g.popClip();

        g.translate(-(int)(tx / scale), -(int)(ty / scale));
        g.scale(1f / scale, 1f / scale);
        g.popClip();

        // Reference outline of the pre-rotation inner clip, in
        // post-scale + post-translate coordinates. Drawn after popClip in
        // identity-transform space so its position is independent of the
        // popped clip state. Apply the same scale/translate transform
        // chain just for this drawRect, otherwise the navy outline lands
        // somewhere unrelated to where the image was rendered.
        g.pushClip();
        g.clipRect(x, y, w, h);
        g.scale(scale, scale);
        g.translate((int)(tx / scale), (int)(ty / scale));
        g.setColor(0x000080);
        g.drawRect(xp + clipInset, yp + clipInset, clipSize - 1, clipSize - 1);
        g.translate(-(int)(tx / scale), -(int)(ty / scale));
        g.scale(1f / scale, 1f / scale);
        g.popClip();

        // Sentinel green dot in the corner; identity-transform space.
        // If popClip / un-rotate / un-scale all restored correctly this
        // pixel lands at the cell's top-left, not somewhere transformed.
        g.setColor(0x008000);
        g.fillRect(x + 2, y + 2, 6, 6);
    }

    @Override
    protected String screenshotName() {
        return "graphics-image-clip-under-rotation";
    }
}
