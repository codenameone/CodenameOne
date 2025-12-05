package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.EncodedImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.RGBImage;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class DrawImage extends AbstractGraphicsScreenshotTest {
    private Image mutable;
    private Image mutableWithAlpha;
    private EncodedImage encoded;
    private RGBImage rgbImage;
    private Image fromRgba;
    private Image fromBytes;

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int size = bounds.getWidth() / 3;
        if(mutable == null) {
            mutable = Image.createImage(size, size);
            Graphics mg = mutable.getGraphics();
            mg.fillRadialGradient(0xff0000, 0xff, 0, 0, size, size);
            mutableWithAlpha = Image.createImage(size, size, 0x2000ff00);
            mg = mutableWithAlpha.getGraphics();
            mg.setColor(0xff0000);
            mg.fillRect(30, 30, size - 60, size - 60);
            encoded = EncodedImage.createFromImage(mutable, false);
            rgbImage = new RGBImage(mutable);
            fromRgba = Image.createImage(rgbImage.getRGB(), size, size);
            fromBytes = Image.createImage(encoded.getImageData(), 0, encoded.getImageData().length);
        }
        int yBound = bounds.getY();
        g.drawImage(mutable, bounds.getX(), yBound);

        g.setColor(0xff);
        g.drawArc(bounds.getX() + size, yBound, size, size, 0, 360);
        g.drawImage(mutableWithAlpha, bounds.getX() + size, yBound);

        g.drawImage(encoded, bounds.getX() + size * 2, yBound);
        yBound = bounds.getY() + size;
        g.drawImage(rgbImage, bounds.getX(), yBound);
        g.drawImage(fromRgba, bounds.getX() + size, yBound);
        g.drawImage(fromBytes, bounds.getX() + size * 2, yBound);

        int smallSize = size / 2;
        yBound = bounds.getY() + size * 2;
        g.drawImage(mutable, bounds.getX(), yBound, smallSize, smallSize);

        g.drawArc(bounds.getX() + smallSize, yBound, smallSize, smallSize, 0, 360);
        g.drawImage(mutableWithAlpha, bounds.getX() + smallSize, yBound, smallSize, smallSize);

        g.drawImage(encoded, bounds.getX() + smallSize * 2, yBound, smallSize, smallSize);
        g.drawImage(rgbImage, bounds.getX() + smallSize * 3, yBound, smallSize, smallSize);
        g.drawImage(fromRgba, bounds.getX() + smallSize * 4, yBound, smallSize, smallSize);
        g.drawImage(fromBytes, bounds.getX() + smallSize * 5, yBound, smallSize, smallSize);
        yBound += smallSize;

        int larger = bounds.getWidth() / 2;
        g.drawImage(mutable, bounds.getX(), yBound, larger, larger);

        g.drawArc(bounds.getX() + larger, yBound, larger, larger, 0, 360);
        g.drawImage(mutableWithAlpha, bounds.getX() + larger, yBound, larger, larger);

        yBound += larger;
        g.drawImage(encoded, bounds.getX(), yBound, larger, larger);
        g.drawImage(rgbImage, bounds.getX() + larger, yBound, larger, larger);

        yBound += larger;
        g.drawImage(fromRgba, bounds.getX(), yBound, larger, larger);
        g.drawImage(fromBytes, bounds.getX() + larger, yBound, larger, larger);
    }

    @Override
    protected String screenshotName() {
        return "graphics-draw-image-rect";
    }
}
