package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.EncodedImage;
import com.codename1.ui.FontImage;
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
    private FontImage fontImage;
    private Image scaled;

    /// How many repaints are still allowed while waiting for an asynchronous
    /// decode. Bounded so a picture that never decodes fails the comparison
    /// instead of repainting for ever: a wedged suite reports nothing, a wrong
    /// frame at least says what is wrong.
    private int decodeRepaintsLeft = 60;

    /// Whether the pictures whose decode is asynchronous are ready to draw.
    ///
    /// Image.createImage(byte[]) hands the browser encoded bytes and the
    /// JavaScript port decodes them on an HTMLImageElement "load" event, so one
    /// created during a paint cannot be drawn in that same paint. Until it is
    /// ready the port answers a placeholder size, which is what this reads.
    /// Every other port decodes synchronously and answers true on the first
    /// call.
    private boolean asyncImagesReady(int size) {
        return fromBytes != null && encoded != null
                && fromBytes.getWidth() == size && encoded.getWidth() == size;
    }

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int size = bounds.getWidth() / 4;
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
            fontImage = FontImage.createFixed("" + FontImage.MATERIAL_ALARM_ON, FontImage.getMaterialDesignFont(), 0xff0000, size, size, 2);
            scaled = mutable.scaled(size * 2, size * 2).scaled(size, size);
        }
        // The mutable-image variants render into a FRESH image on every paint so
        // a first-paint transient can heal (see AbstractGraphicsScreenshotTest).
        // That only helps if something repaints AFTER the decode finishes, and
        // nothing else will: the form is settled and the capture is next. So ask
        // for another paint while the asynchronous pictures are still not ready.
        // This is what made graphics-draw-image-rect differ between runs -- the
        // bottom half, which is the two mutable-image variants, captured
        // whatever had decoded by the first paint.
        if (!asyncImagesReady(size) && decodeRepaintsLeft > 0) {
            decodeRepaintsLeft--;
            com.codename1.ui.Form current = com.codename1.ui.Display.getInstance().getCurrent();
            if (current != null) {
                current.repaint();
            }
        }
        int yBound = bounds.getY();
        g.drawImage(mutable, bounds.getX(), yBound);

        g.setColor(0xff);
        g.drawArc(bounds.getX() + size, yBound, size, size, 0, 360);
        g.drawImage(mutableWithAlpha, bounds.getX() + size, yBound);

        g.drawImage(encoded, bounds.getX() + size * 2, yBound);
        g.drawImage(fontImage, bounds.getX() + size * 3, yBound);

        yBound = bounds.getY() + size;
        g.drawImage(rgbImage, bounds.getX(), yBound);
        g.drawImage(fromRgba, bounds.getX() + size, yBound);
        g.drawImage(fromBytes, bounds.getX() + size * 2, yBound);
        g.drawImage(scaled, bounds.getX() + size * 3, yBound);

        int smallSize = size / 2;
        yBound = bounds.getY() + size * 2;
        g.drawImage(mutable, bounds.getX(), yBound, smallSize, smallSize);

        g.drawArc(bounds.getX() + smallSize, yBound, smallSize, smallSize, 0, 360);
        g.drawImage(mutableWithAlpha, bounds.getX() + smallSize, yBound, smallSize, smallSize);

        g.drawImage(encoded, bounds.getX() + smallSize * 2, yBound, smallSize, smallSize);
        g.drawImage(rgbImage, bounds.getX() + smallSize * 3, yBound, smallSize, smallSize);
        g.drawImage(fromRgba, bounds.getX() + smallSize * 4, yBound, smallSize, smallSize);
        g.drawImage(fromBytes, bounds.getX() + smallSize * 5, yBound, smallSize, smallSize);
        g.drawImage(fontImage, bounds.getX() + smallSize * 6, yBound, smallSize, smallSize);
        g.drawImage(scaled, bounds.getX() + smallSize * 7, yBound, smallSize, smallSize);
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

        yBound += larger;
        g.drawImage(fontImage, bounds.getX(), yBound, larger, larger);
        g.drawImage(scaled, bounds.getX() + larger, yBound, larger, larger);
    }

    @Override
    protected String screenshotName() {
        return "graphics-draw-image-rect";
    }
}
