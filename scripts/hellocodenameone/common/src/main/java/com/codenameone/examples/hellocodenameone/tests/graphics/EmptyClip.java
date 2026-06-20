package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.CN;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

/**
 * Regression guard for issue #5263.
 *
 * <p>When {@code clipRect} intersects two rectangles that do not overlap the
 * result is an <em>empty</em> clip, so every subsequent draw must be culled --
 * nothing should appear. The simulator, Android and the iOS GL backend all
 * honor this. The iOS Metal backend (now the default) did not: an empty clip
 * collapsed to bounds {@code (0,0,0,0)} reached {@code CN1MetalSetScissor},
 * which mistook a zero-size rect for "disable clipping" and opened the whole
 * framebuffer. A fully clipped-out {@code fillRect}/{@code drawImage} then
 * painted over the entire screen.
 *
 * <p>The correct output is therefore a solid green cell. If the bug returns the
 * cell is flooded by the white fill and the red marker image drawn below the
 * empty clip.
 */
public class EmptyClip extends AbstractGraphicsScreenshotTest {

    private Image marker;

    // The watchOS Core Graphics backend has its own still-open empty-clip bug
    // (a follow-up to issue #5263): fixing it there regresses watch text fields
    // that rely on the old "empty clip == no clip" behavior. Until that is
    // resolved the watch render is knowingly wrong, so skip the capture on watch
    // rather than baseline a buggy image or fail the watch suite on a new
    // screenshot with no golden.
    @Override
    public boolean shouldTakeScreenshot() {
        return !CN.isWatch();
    }

    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            done();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int x = bounds.getX();
        int y = bounds.getY();
        int w = bounds.getWidth();
        int h = bounds.getHeight();

        // Baseline that must remain fully visible -- nothing below may overwrite it.
        g.setColor(0x00aa00);
        g.fillRect(x, y, w, h);

        g.pushClip();

        // Clip to a centered rectangle...
        g.clipRect(x + w / 4, y + h / 4, w / 2, h / 2);
        // ...then intersect with a rectangle in the far bottom-right corner that
        // lies completely outside it. The intersection is empty.
        g.clipRect(x + w - 12, y + h - 12, 8, 8);

        // Both draws are entirely outside the (empty) clip and must be culled.
        g.setColor(0xffffff);
        g.fillRect(x, y, w, h);
        g.drawImage(marker(), x, y, w, h);

        g.popClip();
    }

    private Image marker() {
        if (marker == null) {
            marker = Image.createImage(8, 8, 0xffff0000);
        }
        return marker;
    }

    @Override
    protected String screenshotName() {
        return "graphics-empty-clip";
    }
}
