package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class TileImage extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        Image img = Image.createImage(20, 20, 0xff0000ff);
        Graphics ig = img.getGraphics();
        ig.setColor(0xffff00);
        ig.drawLine(0, 0, 20, 20);
        ig.drawLine(20, 0, 0, 20);

        g.tileImage(img, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());

        // Test tiling a sub-area? Graphics.tileImage doesn't seem to support sub-area tiling directly in the API I read.
        // It takes x, y, w, h which is the area to fill with tiles.
    }

    @Override
    protected String screenshotName() {
        return "graphics-tile-image";
    }
}
