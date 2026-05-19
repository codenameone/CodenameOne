package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

/// Validates the platform's `Graphics.gaussianBlur(Image, float)` primitive
/// (the underlying mechanism that backs CSS `filter: blur(...)`). The harness
/// draws four variants: an unblurred reference, a light blur, a heavy blur, and
/// a blur applied to a gradient-filled source so any artifacting from the
/// blur kernel against a high-frequency gradient is visible.
///
/// Hardware paths under test: CIGaussianBlur on iOS, RenderEffect /
/// ScriptIntrinsicBlur on Android, JHLabs GaussianFilter in the simulator.
public class GaussianBlur extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int cellW = bounds.getWidth() / 2;
        int cellH = bounds.getHeight() / 2;
        int density = Display.getInstance().getDeviceDensity();
        // Use density-aware radii so the blur is visually similar across DPIs.
        float lightRadius = CN.convertToPixels(1.5f);
        float heavyRadius = CN.convertToPixels(4f);

        Image source = buildSource(cellW, cellH);

        // Reference (no blur).
        g.drawImage(source, bounds.getX(), bounds.getY());

        // Light blur of the same source.
        if (g.gaussianBlur(source, lightRadius) != null) {
            g.drawImage(g.gaussianBlur(source, lightRadius),
                    bounds.getX() + cellW, bounds.getY());
        }

        // Heavy blur.
        if (g.gaussianBlur(source, heavyRadius) != null) {
            g.drawImage(g.gaussianBlur(source, heavyRadius),
                    bounds.getX(), bounds.getY() + cellH);
        }

        // Blur of a high-frequency gradient source.
        Image gradient = buildGradient(cellW, cellH);
        if (g.gaussianBlur(gradient, heavyRadius) != null) {
            g.drawImage(g.gaussianBlur(gradient, heavyRadius),
                    bounds.getX() + cellW, bounds.getY() + cellH);
        }
    }

    private Image buildSource(int w, int h) {
        Image img = Image.createImage(w, h, 0xffffffff);
        Graphics g = img.getGraphics();
        g.setAntiAliased(false);
        // Three vertical color bars produce a deterministic three-edge target
        // for the blur kernel.
        int bar = w / 3;
        g.setColor(0xff2255bb);
        g.fillRect(0, 0, bar, h);
        g.setColor(0xff44aa55);
        g.fillRect(bar, 0, bar, h);
        g.setColor(0xffcc3344);
        g.fillRect(bar * 2, 0, w - bar * 2, h);
        return img;
    }

    private Image buildGradient(int w, int h) {
        Image img = Image.createImage(w, h, 0xff000000);
        Graphics g = img.getGraphics();
        g.fillLinearGradient(0xff8800, 0x0044ff, 0, 0, w, h, false);
        return img;
    }

    @Override
    protected String screenshotName() {
        return "graphics-gaussian-blur";
    }
}
