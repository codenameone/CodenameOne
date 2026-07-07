package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Form;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/**
 * Phase 3 milestone test: round-trip through Image.getGraphics().drawXxx(...)
 * → image.getRGB() and verify the pixels read back match what was drawn.
 *
 * The Metal port's mutable-image rendering is asynchronous -- draw calls
 * append ops to a per-image command queue and the GPU only sees them when
 * drawFrame's drain runs. Pixel-reading paths (getRGB, encode-as-PNG/JPEG,
 * toImage) must commit and wait on the queue before reading, otherwise
 * callers see stale or zeroed bytes. This test exercises that contract.
 */
public class MutableImageReadbackTest extends BaseTest {
    private static final int RETRY_INTERVAL_MS = 250;
    private static final int RETRY_TIMEOUT_MS = 5000;
    private long retryDeadline;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        Form form = new Form("Mutable Image Readback", new BorderLayout()) {
            @Override
            protected void onShowCompleted() {
                super.onShowCompleted();
                repaint();
                retryDeadline = System.currentTimeMillis() + RETRY_TIMEOUT_MS;
                UITimer.timer(500, false, this, () -> runReadbackChecks(this));
            }
        };
        form.show();
        return true;
    }

    private void runReadbackChecks(Form form) {
        try {
            String error = readbackError();
            if (error == null) {
                done();
                return;
            }
            if (System.currentTimeMillis() < retryDeadline) {
                form.repaint();
                UITimer.timer(RETRY_INTERVAL_MS, false, form, () -> runReadbackChecks(form));
                return;
            }
            fail(error);
        } catch (Throwable t) {
            fail("Unexpected exception: " + t.getMessage());
        }
    }

    private String readbackError() {
        // Step 1: a fillRect-only mutable. The simplest case verifies that
        // pixel readback sees the most recent solid fill.
        String error = testFillRectReadback();
        if (error != null) {
            return error;
        }

        // Step 2: a fillRect followed by a smaller fillRect inside it.
        // Verifies that later ops override earlier pixels.
        error = testStackedFillsReadback();
        if (error != null) {
            return error;
        }

        // Step 3: a fillShape into the mutable. Verifies the alpha-mask path
        // also flushes through to readback.
        return testFillShapeReadback();
    }

    private String testFillRectReadback() {
        int w = 8, h = 8;
        Image img = Image.createImage(w, h);
        Graphics g = img.getGraphics();
        g.setColor(0xff0000); // red
        g.fillRect(0, 0, w, h);

        int[] pixels = img.getRGB();
        if (pixels == null || pixels.length != w * h) {
            return "FillRect readback: getRGB returned " + (pixels == null ? "null" : "length=" + pixels.length)
                    + ", expected " + (w * h);
        }
        for (int i = 0; i < pixels.length; i++) {
            if ((pixels[i] & 0xffffff) != 0xff0000) {
                return "FillRect readback: pixel " + i + " expected red 0xff0000 in low 24 bits, got 0x"
                        + Integer.toHexString(pixels[i]);
            }
        }
        return null;
    }

    private String testStackedFillsReadback() {
        int w = 16, h = 16;
        Image img = Image.createImage(w, h);
        Graphics g = img.getGraphics();
        g.setColor(0xff0000); // red base
        g.fillRect(0, 0, w, h);
        g.setColor(0x00ff00); // green inset
        g.fillRect(4, 4, 8, 8);

        int[] pixels = img.getRGB();
        // Corner pixel (0, 0) should still be red.
        if ((pixels[0] & 0xffffff) != 0xff0000) {
            return "Stacked fills readback: corner pixel expected red, got 0x"
                    + Integer.toHexString(pixels[0]);
        }
        // Center pixel (8, 8) should be green from the inset.
        int center = pixels[8 * w + 8];
        if ((center & 0xffffff) != 0x00ff00) {
            return "Stacked fills readback: center pixel expected green 0x00ff00, got 0x"
                    + Integer.toHexString(center);
        }
        return null;
    }

    private String testFillShapeReadback() {
        int w = 16, h = 16;
        Image img = Image.createImage(w, h);
        Graphics g = img.getGraphics();
        g.setColor(0xffffff); // white base
        g.fillRect(0, 0, w, h);

        // Triangle covering the centre.
        GeneralPath p = new GeneralPath();
        p.moveTo(0, 0);
        p.lineTo(w, 0);
        p.lineTo(w / 2, h);
        p.closePath();
        g.setColor(0x0000ff); // blue
        g.fillShape(p);

        int[] pixels = img.getRGB();
        // (8, 4) should be inside the triangle -- blue (or close to it
        // after alpha-mask anti-aliasing). Accept any pixel where the
        // blue channel dominates.
        int sample = pixels[4 * w + 8];
        int r = (sample >> 16) & 0xff;
        int gc = (sample >> 8) & 0xff;
        int b = sample & 0xff;
        if (b <= r || b <= gc) {
            return "FillShape readback: center pixel inside triangle expected blue-dominant, got rgb=("
                    + r + "," + gc + "," + b + ")";
        }
        // (0, h-1) is outside the triangle -- white from the base fill.
        int outside = pixels[(h - 1) * w + 0];
        if ((outside & 0xffffff) != 0xffffff) {
            return "FillShape readback: pixel outside triangle expected white, got 0x"
                    + Integer.toHexString(outside);
        }
        return null;
    }
}
