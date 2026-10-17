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
package com.codename1.desktopcompat.java.awt.geom;

import com.codename1.util.MathUtil;

/// Part of the ellipse inscribed in a rectangular frame, from a start angle
/// through an extent, both in degrees.
///
/// Angles are measured from the positive x axis and grow towards the
/// negative y axis, which is counter-clockwise on a screen. They are angles
/// of the frame's parameter, so 45 degrees always points at the frame's
/// corner however far the frame is from square. The arc is left open, closed
/// with a chord, or closed through the centre as a pie slice.
///
/// `setArcByTangent` is not provided.
public abstract class Arc2D extends RectangularShape {

    /// No closing segment; hit tests treat the arc as if it had a chord.
    public static final int OPEN = 0;

    /// Closed by a straight line between the ends of the arc.
    public static final int CHORD = 1;

    /// Closed by lines from the ends of the arc to the centre.
    public static final int PIE = 2;

    /// An arc stored in single precision.
    public static class Float extends Arc2D {

        public float x;
        public float y;
        public float width;
        public float height;
        public float start;
        public float extent;

        public Float() {
            super(OPEN);
        }

        public Float(int type) {
            super(type);
        }

        public Float(float x, float y, float w, float h, float start, float extent, int type) {
            super(type);
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.start = start;
            this.extent = extent;
        }

        public Float(Rectangle2D ellipseBounds, float start, float extent, int type) {
            super(type);
            this.x = (float) ellipseBounds.getX();
            this.y = (float) ellipseBounds.getY();
            this.width = (float) ellipseBounds.getWidth();
            this.height = (float) ellipseBounds.getHeight();
            this.start = start;
            this.extent = extent;
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getY() {
            return y;
        }

        @Override
        public double getWidth() {
            return width;
        }

        @Override
        public double getHeight() {
            return height;
        }

        @Override
        public double getAngleStart() {
            return start;
        }

        @Override
        public double getAngleExtent() {
            return extent;
        }

        @Override
        public boolean isEmpty() {
            return width <= 0.0f || height <= 0.0f;
        }

        @Override
        public void setArc(double x, double y, double w, double h, double angSt, double angExt, int closure) {
            setArcType(closure);
            this.x = (float) x;
            this.y = (float) y;
            this.width = (float) w;
            this.height = (float) h;
            this.start = (float) angSt;
            this.extent = (float) angExt;
        }

        @Override
        public void setAngleStart(double angSt) {
            this.start = (float) angSt;
        }

        @Override
        public void setAngleExtent(double angExt) {
            this.extent = (float) angExt;
        }

        @Override
        protected Rectangle2D makeBounds(double x, double y, double w, double h) {
            return new Rectangle2D.Float((float) x, (float) y, (float) w, (float) h);
        }

        @Override
        public Object clone() {
            return new Float(x, y, width, height, start, extent, getArcType());
        }
    }

    /// An arc stored in double precision.
    public static class Double extends Arc2D {

        public double x;
        public double y;
        public double width;
        public double height;
        public double start;
        public double extent;

        public Double() {
            super(OPEN);
        }

        public Double(int type) {
            super(type);
        }

        public Double(double x, double y, double w, double h, double start, double extent, int type) {
            super(type);
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.start = start;
            this.extent = extent;
        }

        public Double(Rectangle2D ellipseBounds, double start, double extent, int type) {
            super(type);
            this.x = ellipseBounds.getX();
            this.y = ellipseBounds.getY();
            this.width = ellipseBounds.getWidth();
            this.height = ellipseBounds.getHeight();
            this.start = start;
            this.extent = extent;
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getY() {
            return y;
        }

        @Override
        public double getWidth() {
            return width;
        }

        @Override
        public double getHeight() {
            return height;
        }

        @Override
        public double getAngleStart() {
            return start;
        }

        @Override
        public double getAngleExtent() {
            return extent;
        }

        @Override
        public boolean isEmpty() {
            return width <= 0.0 || height <= 0.0;
        }

        @Override
        public void setArc(double x, double y, double w, double h, double angSt, double angExt, int closure) {
            setArcType(closure);
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.start = angSt;
            this.extent = angExt;
        }

        @Override
        public void setAngleStart(double angSt) {
            this.start = angSt;
        }

        @Override
        public void setAngleExtent(double angExt) {
            this.extent = angExt;
        }

        @Override
        protected Rectangle2D makeBounds(double x, double y, double w, double h) {
            return new Rectangle2D.Double(x, y, w, h);
        }

        @Override
        public Object clone() {
            return new Double(x, y, width, height, start, extent, getArcType());
        }
    }

    private int type;

    protected Arc2D() {
        this(OPEN);
    }

    protected Arc2D(int type) {
        this.type = checkType(type);
    }

    private static int checkType(int type) {
        if (type < OPEN || type > PIE) {
            throw new IllegalArgumentException("invalid type for Arc: " + type);
        }
        return type;
    }

    public abstract double getAngleStart();

    public abstract double getAngleExtent();

    public int getArcType() {
        return type;
    }

    public Point2D getStartPoint() {
        return pointAt(getAngleStart());
    }

    public Point2D getEndPoint() {
        return pointAt(getAngleStart() + getAngleExtent());
    }

    private Point2D pointAt(double degrees) {
        double a = Math.toRadians(-degrees);
        return new Point2D.Double(getX() + (Math.cos(a) * 0.5 + 0.5) * getWidth(),
                getY() + (Math.sin(a) * 0.5 + 0.5) * getHeight());
    }

    public abstract void setArc(double x, double y, double w, double h, double angSt, double angExt, int closure);

    public void setArc(Point2D loc, Dimension2D size, double angSt, double angExt, int closure) {
        setArc(loc.getX(), loc.getY(), size.getWidth(), size.getHeight(), angSt, angExt, closure);
    }

    public void setArc(Rectangle2D rect, double angSt, double angExt, int closure) {
        setArc(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), angSt, angExt, closure);
    }

    public void setArc(Arc2D a) {
        setArc(a.getX(), a.getY(), a.getWidth(), a.getHeight(), a.getAngleStart(), a.getAngleExtent(), a.type);
    }

    public void setArcByCenter(double x, double y, double radius, double angSt, double angExt, int closure) {
        setArc(x - radius, y - radius, radius * 2.0, radius * 2.0, angSt, angExt, closure);
    }

    public abstract void setAngleStart(double angSt);

    public abstract void setAngleExtent(double angExt);

    /// Starts the arc where the ray from the centre through the point
    /// leaves the ellipse.
    public void setAngleStart(Point2D p) {
        setAngleStart(Math.toDegrees(angleTo(p.getX(), p.getY())));
    }

    /// Runs the arc counter-clockwise from the direction of the first point
    /// to the direction of the second.
    public void setAngles(double x1, double y1, double x2, double y2) {
        double a1 = angleTo(x1, y1);
        double sweep = angleTo(x2, y2) - a1;
        if (sweep <= 0.0) {
            sweep += Math.PI * 2.0;
        }
        setAngleStart(Math.toDegrees(a1));
        setAngleExtent(Math.toDegrees(sweep));
    }

    public void setAngles(Point2D p1, Point2D p2) {
        setAngles(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /// The arc angle, in radians, at which the ray from the centre through
    /// a point crosses the ellipse.
    private double angleTo(double px, double py) {
        return MathUtil.atan2(getWidth() * (getCenterY() - py), getHeight() * (px - getCenterX()));
    }

    public void setArcType(int type) {
        this.type = checkType(type);
    }

    /// Moves and resizes the frame, keeping the angles and the closure.
    @Override
    public void setFrame(double x, double y, double w, double h) {
        setArc(x, y, w, h, getAngleStart(), getAngleExtent(), type);
    }

    /// The tight bounds of the arc and its closing lines, not the frame of
    /// the whole ellipse.
    @Override
    public Rectangle2D getBounds2D() {
        if (isEmpty()) {
            return makeBounds(getX(), getY(), getWidth(), getHeight());
        }
        // in the frame where the ellipse is the unit circle about the origin
        double a0 = Math.toRadians(-getAngleStart());
        double a1 = Math.toRadians(-getAngleStart() - getAngleExtent());
        double[] xs = {Math.cos(a0), Math.cos(a1), 0.0, 1.0, 0.0, -1.0, 0.0};
        double[] ys = {Math.sin(a0), Math.sin(a1), 0.0, 0.0, -1.0, 0.0, 1.0};
        double x1 = Math.min(xs[0], xs[1]);
        double x2 = Math.max(xs[0], xs[1]);
        double y1 = Math.min(ys[0], ys[1]);
        double y2 = Math.max(ys[0], ys[1]);
        for (int i = 2; i < 7; i++) {
            // the centre counts for a pie, an axis end when the arc passes it
            if (i == 2 ? type == PIE : containsAngle((i - 3) * 90.0)) {
                x1 = Math.min(x1, xs[i]);
                x2 = Math.max(x2, xs[i]);
                y1 = Math.min(y1, ys[i]);
                y2 = Math.max(y2, ys[i]);
            }
        }
        double w = getWidth() / 2.0;
        double h = getHeight() / 2.0;
        return makeBounds(getX() + w + x1 * w, getY() + h + y1 * h, (x2 - x1) * w, (y2 - y1) * h);
    }

    protected abstract Rectangle2D makeBounds(double x, double y, double w, double h);

    /// Brings an angle into the range above -180 up to and including 180.
    private static double normalize(double degrees) {
        if (degrees > 180.0) {
            if (degrees <= 540.0) {
                return degrees - 360.0;
            }
            degrees = degrees - Math.floor(degrees / 360.0) * 360.0;
            return degrees > 180.0 ? degrees - 360.0 : degrees;
        }
        if (degrees <= -180.0) {
            if (degrees > -540.0) {
                return degrees + 360.0;
            }
            degrees = degrees - Math.floor(degrees / 360.0) * 360.0;
            return degrees > 180.0 ? degrees - 360.0 : degrees;
        }
        return degrees;
    }

    /// Whether an angle in degrees falls within the arc: at or after the
    /// start in the arc's direction and before its end.
    public boolean containsAngle(double angle) {
        double extent = getAngleExtent();
        boolean backwards = extent < 0.0;
        if (backwards) {
            extent = -extent;
        }
        if (extent >= 360.0) {
            return true;
        }
        angle = normalize(angle) - normalize(getAngleStart());
        if (backwards) {
            angle = -angle;
        }
        if (angle < 0.0) {
            angle += 360.0;
        }
        return angle >= 0.0 && angle < extent;
    }

    @Override
    public boolean contains(double x, double y) {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0.0 || h <= 0.0) {
            return false;
        }
        // in the frame where the ellipse is the circle of diameter one
        // about the origin
        double nx = (x - getX()) / w - 0.5;
        double ny = (y - getY()) / h - 0.5;
        if (nx * nx + ny * ny >= 0.25) {
            return false;
        }
        double extent = Math.abs(getAngleExtent());
        if (extent >= 360.0) {
            return true;
        }
        boolean inSector = containsAngle(-Math.toDegrees(MathUtil.atan2(ny, nx)));
        if (type == PIE) {
            return inSector;
        }
        // closed by the chord: the sector with the centre triangle removed
        // when the arc is under a half turn, added when it is over
        if (inSector) {
            if (extent >= 180.0) {
                return true;
            }
        } else if (extent <= 180.0) {
            return false;
        }
        double a0 = Math.toRadians(-getAngleStart());
        double a1 = Math.toRadians(-getAngleStart() - getAngleExtent());
        double sx = Math.cos(a0);
        double sy = Math.sin(a0);
        double ex = Math.cos(a1);
        double ey = Math.sin(a1);
        boolean centreSide = Line2D.relativeCCW(sx, sy, ex, ey, nx * 2.0, ny * 2.0)
                * Line2D.relativeCCW(sx, sy, ex, ey, 0.0, 0.0) >= 0;
        return inSector ? !centreSide : centreSide;
    }

    /// Tested over the flattened outline unless the arc is a whole ellipse.
    @Override
    public boolean intersects(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0.0 || h <= 0.0) {
            return false;
        }
        if (Math.abs(getAngleExtent()) >= 360.0) {
            return new Ellipse2D.Double(getX(), getY(), getWidth(), getHeight()).intersects(x, y, w, h);
        }
        return Outline.of(this).intersects(x, y, w, h, PathIterator.WIND_NON_ZERO);
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        if (!(contains(x, y) && contains(x + w, y) && contains(x, y + h) && contains(x + w, y + h))) {
            return false;
        }
        // every closure is convex except a pie of more than a half turn,
        // whose notch a rectangle can straddle with all four corners inside
        if (type != PIE || Math.abs(getAngleExtent()) <= 180.0) {
            return true;
        }
        Rectangle2D r = new Rectangle2D.Double(x, y, w, h);
        double cx = getCenterX();
        double cy = getCenterY();
        Point2D s = getStartPoint();
        Point2D e = getEndPoint();
        return !r.intersectsLine(cx, cy, s.getX(), s.getY()) && !r.intersectsLine(cx, cy, e.getX(), e.getY());
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /// The arc as at most four cubic Bezier curves, each spanning no more
    /// than a quarter turn, followed by the closing lines of its type. A
    /// frame with a negative size has no outline at all.
    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        SegmentIterator it = new SegmentIterator(PathIterator.WIND_NON_ZERO, at, 7);
        double w = getWidth() / 2.0;
        double h = getHeight() / 2.0;
        if (w < 0.0 || h < 0.0) {
            return it;
        }
        double cx = getX() + w;
        double cy = getY() + h;
        // device angles: y grows downwards, so the sign flips
        double start = -Math.toRadians(getAngleStart());
        double sweep = -getAngleExtent();
        int pieces;
        double step;
        if (sweep >= 360.0 || sweep <= -360.0) {
            pieces = 4;
            step = sweep < 0.0 ? -Math.PI / 2.0 : Math.PI / 2.0;
        } else {
            pieces = (int) Math.ceil(Math.abs(sweep) / 90.0);
            step = pieces == 0 ? 0.0 : Math.toRadians(sweep / pieces);
        }
        // control points sit on the tangents at each end, this far along
        double k = 4.0 / 3.0 * Math.tan(step / 4.0);
        double cos = Math.cos(start);
        double sin = Math.sin(start);
        it.move(cx + cos * w, cy + sin * h);
        for (int i = 1; i <= pieces; i++) {
            double a = start + step * i;
            double cos2 = Math.cos(a);
            double sin2 = Math.sin(a);
            it.cubic(cx + (cos - k * sin) * w, cy + (sin + k * cos) * h,
                    cx + (cos2 + k * sin2) * w, cy + (sin2 - k * cos2) * h,
                    cx + cos2 * w, cy + sin2 * h);
            cos = cos2;
            sin = sin2;
        }
        if (type == PIE) {
            it.line(cx, cy);
        }
        if (type != OPEN) {
            it.close();
        }
        return it;
    }

    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits = bits * 31 + java.lang.Double.doubleToLongBits(getY());
        bits = bits * 31 + java.lang.Double.doubleToLongBits(getWidth());
        bits = bits * 31 + java.lang.Double.doubleToLongBits(getHeight());
        bits = bits * 31 + java.lang.Double.doubleToLongBits(getAngleStart());
        bits = bits * 31 + java.lang.Double.doubleToLongBits(getAngleExtent());
        bits = bits * 31 + type;
        return (int) bits ^ (int) (bits >> 32);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Arc2D) {
            Arc2D a = (Arc2D) obj;
            return getX() == a.getX() && getY() == a.getY()
                    && getWidth() == a.getWidth() && getHeight() == a.getHeight()
                    && getAngleStart() == a.getAngleStart() && getAngleExtent() == a.getAngleExtent()
                    && type == a.type;
        }
        return false;
    }

    /// Returns a copy of this arc. A subclass from elsewhere that does not
    /// override it gets a double precision arc back.
    @Override
    public Object clone() {
        return new Double(getX(), getY(), getWidth(), getHeight(), getAngleStart(), getAngleExtent(), type);
    }
}
