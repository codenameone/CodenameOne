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
package com.codename1.desktopcompat.java.awt;

import com.codename1.desktopcompat.java.awt.image.ImageObserver;

/// The drawing surface of a component or an image, in logical pixels.
///
/// Every graphics the layer hands out is a [Graphics2D]; see
/// `com.codename1.desktopcompat.rt.G2D` for how it maps onto Codename One.
public abstract class Graphics {

    protected Graphics() {
    }

    public abstract Graphics create();

    public Graphics create(int x, int y, int width, int height) {
        Graphics g = create();
        if (g == null) {
            return null;
        }
        g.translate(x, y);
        g.clipRect(0, 0, width, height);
        return g;
    }

    public abstract void translate(int x, int y);

    public abstract Color getColor();

    public abstract void setColor(Color c);

    public abstract void setPaintMode();

    public abstract void setXORMode(Color c1);

    public abstract Font getFont();

    public abstract void setFont(Font font);

    public FontMetrics getFontMetrics() {
        return getFontMetrics(getFont());
    }

    public abstract FontMetrics getFontMetrics(Font f);

    public abstract Rectangle getClipBounds();

    public abstract void clipRect(int x, int y, int width, int height);

    public abstract void setClip(int x, int y, int width, int height);

    public abstract Shape getClip();

    public abstract void setClip(Shape clip);

    public abstract void copyArea(int x, int y, int width, int height, int dx, int dy);

    public abstract void drawLine(int x1, int y1, int x2, int y2);

    public abstract void fillRect(int x, int y, int width, int height);

    public void drawRect(int x, int y, int width, int height) {
        if (width < 0 || height < 0) {
            return;
        }
        if (height == 0 || width == 0) {
            drawLine(x, y, x + width, y + height);
        } else {
            drawLine(x, y, x + width - 1, y);
            drawLine(x + width, y, x + width, y + height - 1);
            drawLine(x + width, y + height, x + 1, y + height);
            drawLine(x, y + height, x, y + 1);
        }
    }

    public abstract void clearRect(int x, int y, int width, int height);

    public abstract void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight);

    public abstract void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight);

    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        Color c = getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();
        setColor(raised ? brighter : darker);
        drawLine(x, y, x, y + height);
        drawLine(x + 1, y, x + width - 1, y);
        setColor(raised ? darker : brighter);
        drawLine(x + 1, y + height, x + width, y + height);
        drawLine(x + width, y, x + width, y + height - 1);
        setColor(c);
    }

    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        Color c = getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();
        if (!raised) {
            setColor(darker);
        }
        fillRect(x + 1, y + 1, width - 2, height - 2);
        setColor(raised ? brighter : darker);
        drawLine(x, y, x, y + height - 1);
        drawLine(x + 1, y, x + width - 2, y);
        setColor(raised ? darker : brighter);
        drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
        drawLine(x + width - 1, y, x + width - 1, y + height - 2);
        setColor(c);
    }

    public abstract void drawOval(int x, int y, int width, int height);

    public abstract void fillOval(int x, int y, int width, int height);

    public abstract void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle);

    public abstract void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle);

    public abstract void drawPolyline(int[] xPoints, int[] yPoints, int nPoints);

    public abstract void drawPolygon(int[] xPoints, int[] yPoints, int nPoints);

    public void drawPolygon(Polygon p) {
        drawPolygon(p.xpoints, p.ypoints, p.npoints);
    }

    public abstract void fillPolygon(int[] xPoints, int[] yPoints, int nPoints);

    public void fillPolygon(Polygon p) {
        fillPolygon(p.xpoints, p.ypoints, p.npoints);
    }

    public abstract void drawString(String str, int x, int y);

    public void drawChars(char[] data, int offset, int length, int x, int y) {
        drawString(new String(data, offset, length), x, y);
    }

    public abstract boolean drawImage(Image img, int x, int y, ImageObserver observer);

    public abstract boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer);

    public abstract boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer);

    public abstract boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor,
                                      ImageObserver observer);

    public abstract boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2,
                                      int sy2, ImageObserver observer);

    public abstract boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2,
                                      int sy2, Color bgcolor, ImageObserver observer);

    public abstract void dispose();

    @Override
    public String toString() {
        return getClass().getName() + "[font=" + getFont() + ",color=" + getColor() + "]";
    }

    public boolean hitClip(int x, int y, int width, int height) {
        Rectangle clipRect = getClipBounds();
        if (clipRect == null) {
            return true;
        }
        return clipRect.intersects(x, y, width, height);
    }

    public Rectangle getClipBounds(Rectangle r) {
        Rectangle clipRect = getClipBounds();
        if (clipRect != null) {
            r.x = clipRect.x;
            r.y = clipRect.y;
            r.width = clipRect.width;
            r.height = clipRect.height;
        } else if (r == null) {
            throw new NullPointerException("null rectangle parameter");
        }
        return r;
    }
}
