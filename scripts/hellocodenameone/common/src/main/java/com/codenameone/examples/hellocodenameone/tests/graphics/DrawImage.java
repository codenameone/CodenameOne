/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
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

    /// Whether a frame has actually been painted with the asynchronous pictures.
    ///
    /// The decode landing and the screen showing it are two different events, and
    /// only the second one is what a screenshot can capture.
    private boolean paintedWithAsyncImages;

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

    /// Holds the capture until a frame has been painted WITH the asynchronously
    /// decoded pictures, not merely until they have decoded.
    ///
    /// The repaint request below can only ask for another paint; it cannot stop
    /// the screenshot being taken before one arrives, and an outstanding decode
    /// leaves the form perfectly idle, so the settle loop used to see nothing to
    /// wait for. That is how this test failed intermittently with its top half
    /// complete and its bottom half all but empty: the two lower variants
    /// painted before the decode and nothing repainted them afterwards.
    ///
    /// Gating on arrival was still a race, because arrival and display are two
    /// events and only the second one is in the screenshot. Between them the
    /// frame on screen is the one painted without the pictures, and what closed
    /// the gap was a fixed settle delay -- fine until a loaded runner spends it
    /// before the repaint lands, which is the same wrong frame again. Waiting for
    /// the paint itself needs no delay to be right.
    @Override
    protected boolean captureBlockedByPendingContent() {
        return mutable != null && !paintedWithAsyncImages;
    }

    /// One forced repaint after the decode lands, before the shot.
    ///
    /// The gate above says the pictures now exist; it does not say anything has
    /// drawn them. Without this the capture can still take the frame painted
    /// while they were missing, which is the same wrong picture arrived at one
    /// step later.
    @Override
    protected long extraSettleBeforeCaptureMillis() {
        return 150;
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
        //
        // Driven by whether a whole frame has been painted with the pictures in
        // hand, not by whether they have arrived. Arrival is not the thing the
        // capture needs: the variants painted before it are still on screen with
        // their pictures missing, and asking only until arrival stops asking on
        // the very paint that first sees them -- leaving the frame that made it
        // to the screenshot one short.
        if (asyncImagesReady(size)) {
            // This pass is drawing them. The variants of one frame are drawn in a
            // single synchronous pass, so by the time anything can capture, every
            // one of them has been through here with the pictures present.
            //
            // Set before the request below, so a port that decodes synchronously
            // answers true on its first paint and never asks for the extra one --
            // leaving every such port's frames exactly as they were.
            paintedWithAsyncImages = true;
        }
        if (!paintedWithAsyncImages && decodeRepaintsLeft > 0) {
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
