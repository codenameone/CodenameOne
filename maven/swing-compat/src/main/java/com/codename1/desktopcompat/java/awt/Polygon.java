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

import com.codename1.desktopcompat.java.awt.geom.AffineTransform;
import com.codename1.desktopcompat.java.awt.geom.Path2D;
import com.codename1.desktopcompat.java.awt.geom.PathIterator;
import com.codename1.desktopcompat.java.awt.geom.Point2D;
import com.codename1.desktopcompat.java.awt.geom.Rectangle2D;

/// A closed region bounded by straight edges between integer points,
/// filled by the even-odd rule.
public class Polygon implements Shape {

    public int npoints;
    public int[] xpoints;
    public int[] ypoints;
    protected Rectangle bounds;

    public Polygon() {
        xpoints = new int[4];
        ypoints = new int[4];
    }

    public Polygon(int[] xpoints, int[] ypoints, int npoints) {
        if (npoints > xpoints.length || npoints > ypoints.length) {
            throw new IndexOutOfBoundsException("npoints > xpoints.length || npoints > ypoints.length");
        }
        if (npoints < 0) {
            throw new NegativeArraySizeException("npoints < 0");
        }
        this.npoints = npoints;
        this.xpoints = new int[npoints];
        this.ypoints = new int[npoints];
        System.arraycopy(xpoints, 0, this.xpoints, 0, npoints);
        System.arraycopy(ypoints, 0, this.ypoints, 0, npoints);
    }

    public void reset() {
        npoints = 0;
        bounds = null;
    }

    public void invalidate() {
        bounds = null;
    }

    public void translate(int deltaX, int deltaY) {
        for (int i = 0; i < npoints; i++) {
            xpoints[i] += deltaX;
            ypoints[i] += deltaY;
        }
        if (bounds != null) {
            bounds.translate(deltaX, deltaY);
        }
    }

    public void addPoint(int x, int y) {
        if (npoints >= xpoints.length || npoints >= ypoints.length) {
            int size = Math.max(4, npoints * 2);
            int[] nx = new int[size];
            int[] ny = new int[size];
            System.arraycopy(xpoints, 0, nx, 0, npoints);
            System.arraycopy(ypoints, 0, ny, 0, npoints);
            xpoints = nx;
            ypoints = ny;
        }
        xpoints[npoints] = x;
        ypoints[npoints] = y;
        npoints++;
        bounds = null;
    }

    @Override
    public Rectangle getBounds() {
        if (npoints == 0) {
            return new Rectangle();
        }
        if (bounds == null) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (int i = 0; i < npoints; i++) {
                minX = Math.min(minX, xpoints[i]);
                maxX = Math.max(maxX, xpoints[i]);
                minY = Math.min(minY, ypoints[i]);
                maxY = Math.max(maxY, ypoints[i]);
            }
            bounds = new Rectangle(minX, minY, maxX - minX, maxY - minY);
        }
        return new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public boolean contains(Point p) {
        return contains(p.x, p.y);
    }

    public boolean contains(int x, int y) {
        return contains((double) x, (double) y);
    }

    @Override
    public Rectangle2D getBounds2D() {
        return getBounds();
    }

    private Path2D.Double path() {
        Path2D.Double p = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        for (int i = 0; i < npoints; i++) {
            if (i == 0) {
                p.moveTo(xpoints[i], ypoints[i]);
            } else {
                p.lineTo(xpoints[i], ypoints[i]);
            }
        }
        if (npoints > 0) {
            p.closePath();
        }
        return p;
    }

    @Override
    public boolean contains(double x, double y) {
        return npoints > 2 && path().contains(x, y);
    }

    @Override
    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return npoints > 0 && path().intersects(x, y, w, h);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return npoints > 2 && path().contains(x, y, w, h);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return path().getPathIterator(at);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return path().getPathIterator(at);
    }
}
