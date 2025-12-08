package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class DrawRGB extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int w = 50;
        int h = 50;
        int[] rgbData = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = (x * 255) / w;
                int green = (y * 255) / h;
                int b = 0;
                rgbData[y * w + x] = (0xff << 24) | (r << 16) | (green << 8) | b;
            }
        }

        g.drawRGB(rgbData, 0, bounds.getX(), bounds.getY(), w, h, true);

        // With transparency
        for (int i = 0; i < rgbData.length; i++) {
            if (i % 2 == 0) {
                rgbData[i] = 0; // transparent
            }
        }
        g.drawRGB(rgbData, 0, bounds.getX() + w + 10, bounds.getY(), w, h, true);
    }

    @Override
    protected String screenshotName() {
        return "graphics-draw-rgb";
    }
}
