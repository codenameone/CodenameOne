package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.util.UITimer;

/**
 * Regression test for #5171 ("draw outside the window" on the iOS Metal
 * backend): content drawn onto the screen after a mutable-image detour
 * ignored the active clip and spilled outside its component.
 *
 * Root cause (IOSImplementation.GlobalGraphics.checkControl): after drawing
 * into a mutable image via image.getGraphics(), control returns to the screen
 * graphics but the clip was left marked "already applied" (clipApplied=true).
 * On Metal the mutable-image draw runs on its own render encoder and leaves the
 * shared native scissor at the mutable image's bounds (often much larger than
 * the component). Because clipApplied still read true, applyClip() skipped
 * re-emitting the screen clip, so the next screen draw used that stale,
 * oversized scissor and painted outside the component. The fix invalidates
 * clipApplied when returning to the screen so the clip is always re-applied.
 *
 * This test reproduces the exact shape of ddyer0's report: a component paints
 * by (1) drawing into a screen-sized mutable image (the detour that leaves a
 * full-screen scissor behind) and (2) filling red across an area larger than
 * itself. With a correct clip the red is confined to the component; with the
 * bug it bleeds into the surrounding margin. We read the rendered screen back
 * with Display.screenshot() and assert the margin just outside the component
 * was not painted red. Pixel-readback (not a stored golden), so it is
 * deterministic and self-describing on every pipeline; it only actually
 * exercises the desync on the Metal backend -- elsewhere the clip is honoured
 * and the test simply passes.
 */
public class MutableImageClipReadbackTest extends BaseTest {

    private static final int PAINTER_RED = 0xff2020;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() throws Exception {
        final int dispW = Display.getInstance().getDisplayWidth();
        final int dispH = Display.getInstance().getDisplayHeight();
        // Painter sized to roughly half the screen and centred, leaving a wide
        // white margin on every side that acts as the overpaint sentinel.
        final int painterW = Math.max(40, dispW / 2);
        final int painterH = Math.max(40, dispH / 4);

        final OverflowPainter painter = new OverflowPainter(painterW, painterH, dispW, dispH);
        // Zero padding/margin so the component's bounds equal the area it
        // paints -- the inside/outside sample points below rely on that.
        painter.getAllStyles().setPadding(0, 0, 0, 0);
        painter.getAllStyles().setMargin(0, 0, 0, 0);

        Container holder = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        holder.getAllStyles().setBgColor(0xffffff);
        holder.getAllStyles().setBgTransparency(255);
        holder.getAllStyles().setPadding(0, 0, 0, 0);
        holder.add(painter);

        Form form = new Form("Mutable Image Clip", new BorderLayout());
        form.getAllStyles().setBgColor(0xffffff);
        form.getAllStyles().setBgTransparency(255);
        form.add(BorderLayout.CENTER, holder);
        form.show();

        // Let the form lay out and paint at least once, then read the screen
        // back. 1500ms mirrors BaseTest's settle for the screenshot tests.
        UITimer.timer(1500, false, form, () -> captureAndVerify());
        return true;
    }

    private void captureAndVerify() {
        try {
            Display.getInstance().screenshot(screen -> {
                try {
                    verify(screen);
                } catch (Throwable t) {
                    fail("Unexpected exception during verification: " + t);
                }
            });
        } catch (Throwable t) {
            fail("Display.screenshot threw: " + t);
        }
    }

    private void verify(Image screen) {
        if (screen == null) {
            fail("screenshot returned null");
            return;
        }
        OverflowPainter painter = OverflowPainter.last;
        if (painter == null || painter.getWidth() <= 0 || painter.getHeight() <= 0) {
            fail("painter was not laid out");
            return;
        }
        int imgW = screen.getWidth();
        int imgH = screen.getHeight();
        int[] rgb = screen.getRGB();

        // The screenshot may come back at native pixel resolution while
        // component coordinates are in CN1 display units. Scale sample points
        // by the captured-image / display ratio so we hit the right pixels on
        // every density.
        double sx = imgW / (double) Display.getInstance().getDisplayWidth();
        double sy = imgH / (double) Display.getInstance().getDisplayHeight();

        int pAbsX = painter.getAbsoluteX();
        int pAbsY = painter.getAbsoluteY();
        int pW = painter.getWidth();
        int pH = painter.getHeight();

        // Sanity: the centre of the painter must be red. If it isn't, the
        // detour-then-fill never ran and the outside check below would be
        // meaningless (a false pass).
        int insideX = (int) ((pAbsX + pW / 2) * sx);
        int insideY = (int) ((pAbsY + pH / 2) * sy);
        if (!isRed(sample(rgb, imgW, imgH, insideX, insideY))) {
            fail("painter centre was not red (" + insideX + "," + insideY
                    + ") = 0x" + Integer.toHexString(sample(rgb, imgW, imgH, insideX, insideY))
                    + " -- repro setup did not paint, cannot judge clipping");
            return;
        }

        // The actual regression assertion: a point in the margin just past the
        // painter's right edge must NOT be red. With the #5171 bug the stale
        // full-screen scissor lets the red fill spill out to here. Sample a
        // quarter of the way into the right margin so we are clearly outside
        // the component yet still on-screen.
        int marginGap = Math.max(8, (Display.getInstance().getDisplayWidth() - pW) / 4);
        int outsideX = (int) ((pAbsX + pW + marginGap) * sx);
        int outsideY = (int) ((pAbsY + pH / 2) * sy);
        // Only meaningful if that point is actually on-screen and inside the
        // white margin; if the layout left no room, fall back to a point below
        // the painter.
        if (outsideX >= imgW - 1) {
            outsideX = (int) ((pAbsX + pW / 2) * sx);
            outsideY = (int) ((pAbsY + pH + marginGap) * sy);
        }
        int outside = sample(rgb, imgW, imgH, outsideX, outsideY);
        if (isRed(outside)) {
            fail("content drawn outside its clip (#5171): margin pixel ("
                    + outsideX + "," + outsideY + ") = 0x" + Integer.toHexString(outside)
                    + " is red; the fill spilled past the component clip after the mutable-image detour");
            return;
        }

        done();
    }

    private static int sample(int[] rgb, int w, int h, int x, int y) {
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x >= w) x = w - 1;
        if (y >= h) y = h - 1;
        return rgb[y * w + x];
    }

    private static boolean isRed(int argb) {
        int r = (argb >> 16) & 0xff;
        int g = (argb >> 8) & 0xff;
        int b = argb & 0xff;
        return r > 180 && g < 90 && b < 90;
    }

    /**
     * Paints itself by first drawing into a screen-sized mutable image (the
     * detour) and then filling red across an area larger than its own bounds.
     * A correct clip confines the red to the component; the #5171 bug lets it
     * escape.
     */
    private static final class OverflowPainter extends Component {
        static volatile OverflowPainter last;
        private final int prefW;
        private final int prefH;
        private final int dispW;
        private final int dispH;

        OverflowPainter(int prefW, int prefH, int dispW, int dispH) {
            this.prefW = prefW;
            this.prefH = prefH;
            this.dispW = dispW;
            this.dispH = dispH;
            last = this;
        }

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            return new com.codename1.ui.geom.Dimension(prefW, prefH);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            // (1) The detour: draw into a screen-sized mutable image. On Metal
            // this runs on its own encoder and leaves the shared native scissor
            // at the image's (full-screen) bounds.
            Image scratch = Image.createImage(Math.max(1, dispW), Math.max(1, dispH));
            Graphics sg = scratch.getGraphics();
            sg.setColor(0x0000ff);
            sg.fillRect(0, 0, dispW, dispH);

            // (2) Return to the screen and fill red across an area deliberately
            // larger than this component. g's logical clip is this component's
            // bounds; only a correctly re-applied native clip keeps the red in.
            int over = Math.max(dispW, dispH);
            g.setColor(PAINTER_RED);
            g.fillRect(getX() - over, getY() - over, getWidth() + 2 * over, getHeight() + 2 * over);
        }
    }
}
