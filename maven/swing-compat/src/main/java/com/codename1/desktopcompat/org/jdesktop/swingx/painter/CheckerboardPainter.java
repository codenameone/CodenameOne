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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Paint;
import com.codename1.desktopcompat.java.awt.geom.Rectangle2D;

/// Covers the area with a checkerboard of two paints. The square at the
/// top left is a light one.
public class CheckerboardPainter extends AbstractPainter<Object> {

    private Paint darkPaint = new Color(204, 204, 204);
    private Paint lightPaint = Color.WHITE;
    private double squareSize = 8;

    public CheckerboardPainter() {
    }

    public CheckerboardPainter(Paint darkPaint, Paint lightPaint) {
        this(darkPaint, lightPaint, 8);
    }

    public CheckerboardPainter(Paint darkPaint, Paint lightPaint, double squareSize) {
        this.darkPaint = darkPaint;
        this.lightPaint = lightPaint;
        this.squareSize = squareSize;
    }

    /// Sets the side of a square, which must be positive.
    public void setSquareSize(double squareSize) {
        if (squareSize <= 0) {
            throw new IllegalArgumentException("Length must be > 0");
        }
        double old = this.squareSize;
        this.squareSize = squareSize;
        setDirty(true);
        firePropertyChange("squareSize", Double.valueOf(old), Double.valueOf(squareSize));
    }

    public double getSquareSize() {
        return squareSize;
    }

    public void setDarkPaint(Paint color) {
        Paint old = darkPaint;
        darkPaint = color;
        setDirty(true);
        firePropertyChange("darkPaint", old, color);
    }

    public Paint getDarkPaint() {
        return darkPaint;
    }

    public void setLightPaint(Paint color) {
        Paint old = lightPaint;
        lightPaint = color;
        setDirty(true);
        firePropertyChange("lightPaint", old, color);
    }

    public Paint getLightPaint() {
        return lightPaint;
    }

    @Override
    protected void doPaint(Graphics2D g, Object component, int width, int height) {
        if (lightPaint != null) {
            g.setPaint(lightPaint);
            g.fillRect(0, 0, width, height);
        }
        double size = squareSize;
        if (darkPaint == null || size < 0.5) {
            return;
        }
        g.clipRect(0, 0, width, height);
        g.setPaint(darkPaint);
        int rows = (int) Math.ceil(height / size);
        int columns = (int) Math.ceil(width / size);
        for (int row = 0; row < rows; row++) {
            for (int column = (row + 1) % 2; column < columns; column += 2) {
                g.fill(new Rectangle2D.Double(column * size, row * size, size, size));
            }
        }
    }
}
