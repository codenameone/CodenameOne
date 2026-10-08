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
package com.codename1.desktopcompat.org.jdesktop.swingx.painter;

import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.Shape;
import com.codename1.desktopcompat.java.awt.image.BufferedImage;

/// Draws an image: at its own size placed by the layout properties,
/// scaled by a factor, scaled to fit the area, or tiled along either
/// axis.
///
/// The fill paint, if there is one, is painted behind the image over the
/// rectangle the image takes; the border paint outlines that rectangle.
/// A painter made by the constructors has no fill and no border.
public class ImagePainter extends AbstractAreaPainter<Object> {

    /// How an image that is scaled to fit meets the area.
    public enum ScaleType {
        /// The largest size that lies inside the area, in proportion.
        InsideFit,
        /// The smallest size that covers the area, in proportion.
        OutsideFit,
        /// The size of the area, whatever that does to the proportions.
        Distort
    }

    private BufferedImage img;
    private boolean horizontalRepeat;
    private boolean verticalRepeat;
    private boolean scaleToFit;
    private ScaleType scaleType = ScaleType.InsideFit;
    private double imageScale = 1.0;

    public ImagePainter() {
        this((BufferedImage) null);
    }

    public ImagePainter(BufferedImage image) {
        this(image, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
    }

    public ImagePainter(BufferedImage image, HorizontalAlignment horizontal, VerticalAlignment vertical) {
        super(null);
        img = image;
        setHorizontalAlignment(horizontal);
        setVerticalAlignment(vertical);
        setDirty(false);
    }

    public void setImage(BufferedImage image) {
        if (image != img) {
            BufferedImage old = img;
            img = image;
            setDirty(true);
            firePropertyChange("image", old, image);
        }
    }

    public BufferedImage getImage() {
        return img;
    }

    private Rectangle imageBounds(int width, int height) {
        if (img == null) {
            return null;
        }
        int iw = img.getWidth();
        int ih = img.getHeight();
        if (iw <= 0 || ih <= 0) {
            return null;
        }
        if (scaleToFit) {
            if (scaleType == ScaleType.Distort) {
                return calculateLayout(width, height, width, height);
            }
            double sx = width / (double) iw;
            double sy = height / (double) ih;
            double s = scaleType == ScaleType.OutsideFit ? Math.max(sx, sy) : Math.min(sx, sy);
            return calculateLayout(Math.max(1, (int) Math.round(iw * s)), Math.max(1, (int) Math.round(ih * s)),
                    width, height);
        }
        return calculateLayout(Math.max(1, (int) Math.round(iw * imageScale)),
                Math.max(1, (int) Math.round(ih * imageScale)), width, height);
    }

    @Override
    protected void doPaint(Graphics2D g, Object component, int width, int height) {
        Rectangle r = imageBounds(width, height);
        if (r == null || r.width <= 0 || r.height <= 0) {
            return;
        }
        Rectangle area = new Rectangle(r);
        if (horizontalRepeat) {
            area.x = 0;
            area.width = width;
        }
        if (verticalRepeat) {
            area.y = 0;
            area.height = height;
        }
        if (cn1Fills()) {
            g.setPaint(cn1Fitted(getFillPaint(), width, height));
            g.fill(area);
        }
        if (horizontalRepeat || verticalRepeat) {
            Graphics2D tiles = (Graphics2D) g.create();
            try {
                tiles.clipRect(0, 0, width, height);
                // The tiles keep the phase of the placed image: the first
                // one starts at or before the edge.
                int startX = r.x;
                if (horizontalRepeat) {
                    startX = r.x % r.width;
                    if (startX > 0) {
                        startX -= r.width;
                    }
                }
                int startY = r.y;
                if (verticalRepeat) {
                    startY = r.y % r.height;
                    if (startY > 0) {
                        startY -= r.height;
                    }
                }
                int endX = horizontalRepeat ? width : startX + 1;
                int endY = verticalRepeat ? height : startY + 1;
                for (int y = startY; y < endY; y += r.height) {
                    for (int x = startX; x < endX; x += r.width) {
                        tiles.drawImage(img, x, y, r.width, r.height, null);
                    }
                }
            } finally {
                tiles.dispose();
            }
        } else {
            g.drawImage(img, r.x, r.y, r.width, r.height, null);
        }
        if (cn1Outlines()) {
            Graphics2D outline = (Graphics2D) g.create();
            try {
                outline.setPaint(cn1Fitted(getBorderPaint(), width, height));
                outline.setStroke(new com.codename1.desktopcompat.java.awt.BasicStroke(getBorderWidth()));
                outline.draw(area);
            } finally {
                outline.dispose();
            }
        }
    }

    public boolean isScaleToFit() {
        return scaleToFit;
    }

    /// Whether the image is scaled to the painted area, as the scale type
    /// says.
    public void setScaleToFit(boolean scaleToFit) {
        boolean old = this.scaleToFit;
        this.scaleToFit = scaleToFit;
        setDirty(true);
        firePropertyChange("scaleToFit", Boolean.valueOf(old), Boolean.valueOf(scaleToFit));
    }

    public ScaleType getScaleType() {
        return scaleType;
    }

    public void setScaleType(ScaleType scaleType) {
        ScaleType old = this.scaleType;
        this.scaleType = scaleType == null ? ScaleType.InsideFit : scaleType;
        setDirty(true);
        firePropertyChange("scaleType", old, this.scaleType);
    }

    public double getImageScale() {
        return imageScale;
    }

    /// The factor the image is drawn at when it is not scaled to fit.
    public void setImageScale(double imageScale) {
        double old = this.imageScale;
        this.imageScale = imageScale;
        setDirty(true);
        firePropertyChange("imageScale", Double.valueOf(old), Double.valueOf(imageScale));
    }

    public boolean isHorizontalRepeat() {
        return horizontalRepeat;
    }

    public void setHorizontalRepeat(boolean horizontalRepeat) {
        boolean old = this.horizontalRepeat;
        this.horizontalRepeat = horizontalRepeat;
        setDirty(true);
        firePropertyChange("horizontalRepeat", Boolean.valueOf(old), Boolean.valueOf(horizontalRepeat));
    }

    public boolean isVerticalRepeat() {
        return verticalRepeat;
    }

    public void setVerticalRepeat(boolean verticalRepeat) {
        boolean old = this.verticalRepeat;
        this.verticalRepeat = verticalRepeat;
        setDirty(true);
        firePropertyChange("verticalRepeat", Boolean.valueOf(old), Boolean.valueOf(verticalRepeat));
    }

    /// The rectangle the image takes, or `null` without an image.
    @Override
    protected Shape provideShape(Graphics2D g, Object comp, int width, int height) {
        return imageBounds(width, height);
    }

    @Override
    public String toString() {
        return "ImagePainter[image=" + img + "]";
    }
}
