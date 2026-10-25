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

import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.Shape;

/// An outline built from moves, lines, quadratic and cubic curves, with a
/// winding rule that decides its interior.
///
/// Hit tests run over the flattened outline with every subpath closed. They
/// are exact for polygons; for a rectangle test against a path that overlaps
/// itself under the non-zero rule, `contains` may answer false and
/// `intersects` true where the exact answer is the opposite. `getBounds2D`
/// answers the bounds of every stored point, control points included.
///
/// A path is not safe to change from two threads at once.
public abstract class Path2D implements Shape, Cloneable {

    public static final int WIND_EVEN_ODD = PathIterator.WIND_EVEN_ODD;

    public static final int WIND_NON_ZERO = PathIterator.WIND_NON_ZERO;

    /// A path whose coordinates are held to single precision.
    public static class Float extends Path2D {

        public Float() {
            this(WIND_NON_ZERO, 20);
        }

        public Float(int rule) {
            this(rule, 20);
        }

        public Float(int rule, int initialCapacity) {
            super(rule, initialCapacity, true);
        }

        public Float(Shape s) {
            this(s, null);
        }

        public Float(Shape s, AffineTransform at) {
            super(WIND_NON_ZERO, 20, true);
            copy(s, at);
        }

        public final void moveTo(float x, float y) {
            addMove(x, y);
        }

        public final void lineTo(float x, float y) {
            addLine(x, y);
        }

        public final void quadTo(float x1, float y1, float x2, float y2) {
            addQuad(x1, y1, x2, y2);
        }

        public final void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            addCubic(x1, y1, x2, y2, x3, y3);
        }

        @Override
        public Object clone() {
            return new Float(this);
        }
    }

    /// A path whose coordinates are held to double precision.
    public static class Double extends Path2D {

        public Double() {
            this(WIND_NON_ZERO, 20);
        }

        public Double(int rule) {
            this(rule, 20);
        }

        public Double(int rule, int initialCapacity) {
            super(rule, initialCapacity, false);
        }

        public Double(Shape s) {
            this(s, null);
        }

        public Double(Shape s, AffineTransform at) {
            super(WIND_NON_ZERO, 20, false);
            copy(s, at);
        }

        @Override
        public Object clone() {
            return new Double(this);
        }
    }

    private byte[] types;
    private double[] coords;
    private int typeCount;
    private int coordCount;
    private int windingRule;

    /// Whether every stored coordinate is rounded to a float first, which
    /// is all that tells the two concrete paths apart.
    private final boolean single;

    Path2D(int rule, int initialCapacity, boolean single) {
        checkRule(rule);
        this.windingRule = rule;
        this.single = single;
        this.types = new byte[Math.max(initialCapacity, 1)];
        this.coords = new double[Math.max(initialCapacity, 1) * 2];
    }

    private static void checkRule(int rule) {
        if (rule != WIND_EVEN_ODD && rule != WIND_NON_ZERO) {
            throw new IllegalArgumentException("winding rule must be WIND_EVEN_ODD or WIND_NON_ZERO");
        }
    }

    /// Fills a new path from a shape, optionally transformed.
    final void copy(Shape s, AffineTransform at) {
        if (s instanceof Path2D) {
            Path2D p = (Path2D) s;
            windingRule = p.windingRule;
            typeCount = p.typeCount;
            coordCount = p.coordCount;
            types = new byte[Math.max(typeCount, 1)];
            coords = new double[Math.max(coordCount, 2)];
            System.arraycopy(p.types, 0, types, 0, typeCount);
            System.arraycopy(p.coords, 0, coords, 0, coordCount);
            if (at != null) {
                at.transform(coords, 0, coords, 0, coordCount / 2);
            }
            round();
        } else {
            PathIterator pi = s.getPathIterator(at);
            checkRule(pi.getWindingRule());
            windingRule = pi.getWindingRule();
            addAll(pi, false);
        }
    }

    private void round() {
        if (single) {
            for (int i = 0; i < coordCount; i++) {
                coords[i] = (float) coords[i];
            }
        }
    }

    private void room(int type, boolean needsMove) {
        if (needsMove && typeCount == 0) {
            throw new IllegalPathStateException("missing initial moveto in path definition");
        }
        if (typeCount == types.length) {
            byte[] t = new byte[typeCount * 2];
            System.arraycopy(types, 0, t, 0, typeCount);
            types = t;
        }
        if (coordCount + 6 > coords.length) {
            double[] c = new double[coords.length * 2 + 6];
            System.arraycopy(coords, 0, c, 0, coordCount);
            coords = c;
        }
        types[typeCount++] = (byte) type;
    }

    private void put(double v) {
        coords[coordCount++] = single ? (double) (float) v : v;
    }

    final void addMove(double x, double y) {
        if (typeCount > 0 && types[typeCount - 1] == PathIterator.SEG_MOVETO) {
            // a move straight after a move replaces it
            coordCount -= 2;
        } else {
            room(PathIterator.SEG_MOVETO, false);
        }
        put(x);
        put(y);
    }

    final void addLine(double x, double y) {
        room(PathIterator.SEG_LINETO, true);
        put(x);
        put(y);
    }

    final void addQuad(double x1, double y1, double x2, double y2) {
        room(PathIterator.SEG_QUADTO, true);
        put(x1);
        put(y1);
        put(x2);
        put(y2);
    }

    final void addCubic(double x1, double y1, double x2, double y2, double x3, double y3) {
        room(PathIterator.SEG_CUBICTO, true);
        put(x1);
        put(y1);
        put(x2);
        put(y2);
        put(x3);
        put(y3);
    }

    private void addAll(PathIterator pi, boolean connect) {
        double[] c = new double[6];
        while (!pi.isDone()) {
            switch (pi.currentSegment(c)) {
                case PathIterator.SEG_MOVETO:
                    if (!connect || typeCount == 0 || coordCount == 0) {
                        addMove(c[0], c[1]);
                    } else if (types[typeCount - 1] == PathIterator.SEG_CLOSE
                            || coords[coordCount - 2] != stored(c[0]) || coords[coordCount - 1] != stored(c[1])) {
                        // joined to what is already here, unless the open
                        // subpath ends on this very point
                        addLine(c[0], c[1]);
                    }
                    break;
                case PathIterator.SEG_LINETO:
                    addLine(c[0], c[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    addQuad(c[0], c[1], c[2], c[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    addCubic(c[0], c[1], c[2], c[3], c[4], c[5]);
                    break;
                default:
                    closePath();
                    break;
            }
            pi.next();
            connect = false;
        }
    }

    private double stored(double v) {
        return single ? (double) (float) v : v;
    }

    /// Starts a new subpath. A move that directly follows another move
    /// replaces it.
    public void moveTo(double x, double y) {
        addMove(x, y);
    }

    public void lineTo(double x, double y) {
        addLine(x, y);
    }

    public void quadTo(double x1, double y1, double x2, double y2) {
        addQuad(x1, y1, x2, y2);
    }

    public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
        addCubic(x1, y1, x2, y2, x3, y3);
    }

    /// Closes the current subpath; does nothing when it is already closed.
    public final void closePath() {
        if (typeCount == 0 || types[typeCount - 1] != PathIterator.SEG_CLOSE) {
            room(PathIterator.SEG_CLOSE, true);
        }
    }

    public final void append(Shape s, boolean connect) {
        addAll(s.getPathIterator(null), connect);
    }

    /// Adds every segment of an iterator. With `connect`, a leading move
    /// becomes a line from the current point, or is dropped when the open
    /// subpath already ends there.
    public void append(PathIterator pi, boolean connect) {
        addAll(pi, connect);
    }

    public final int getWindingRule() {
        return windingRule;
    }

    public final void setWindingRule(int rule) {
        checkRule(rule);
        windingRule = rule;
    }

    /// The point the next segment starts from, or null for an empty path.
    /// After a close that is the start of the subpath just closed.
    public final Point2D getCurrentPoint() {
        if (typeCount == 0 || coordCount == 0) {
            return null;
        }
        int index = coordCount;
        if (types[typeCount - 1] == PathIterator.SEG_CLOSE) {
            // walk back to the move that opened the closed subpath
            for (int i = typeCount - 2; i > 0; i--) {
                int type = types[i];
                if (type == PathIterator.SEG_MOVETO) {
                    break;
                }
                index -= SegmentIterator.size(type);
            }
        }
        return point(coords[index - 2], coords[index - 1]);
    }

    private Point2D point(double x, double y) {
        if (single) {
            return new Point2D.Float((float) x, (float) y);
        }
        return new Point2D.Double(x, y);
    }

    /// Empties the path, keeping its winding rule.
    public final void reset() {
        typeCount = 0;
        coordCount = 0;
    }

    public void transform(AffineTransform at) {
        at.transform(coords, 0, coords, 0, coordCount / 2);
        round();
    }

    /// Returns a transformed copy of this path, of the same class.
    public final Shape createTransformedShape(AffineTransform at) {
        Object copy = clone();
        if (!(copy instanceof Path2D)) {
            return null;
        }
        Path2D p = (Path2D) copy;
        if (at != null) {
            p.transform(at);
        }
        return p;
    }

    @Override
    public final Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    /// The bounds of every stored point, in the precision of the path.
    @Override
    public Rectangle2D getBounds2D() {
        double x1 = 0.0;
        double y1 = 0.0;
        double x2 = 0.0;
        double y2 = 0.0;
        for (int i = 0; i < coordCount; i += 2) {
            double x = coords[i];
            double y = coords[i + 1];
            if (i == 0) {
                x1 = x;
                x2 = x;
                y1 = y;
                y2 = y;
            } else {
                x1 = Math.min(x1, x);
                x2 = Math.max(x2, x);
                y1 = Math.min(y1, y);
                y2 = Math.max(y2, y);
            }
        }
        if (single) {
            return new Rectangle2D.Float((float) x1, (float) y1, (float) (x2 - x1), (float) (y2 - y1));
        }
        return new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);
    }

    private static Path2D collect(PathIterator pi) {
        Path2D p = new Double(pi.getWindingRule());
        p.append(pi, false);
        return p;
    }

    public static boolean contains(PathIterator pi, double x, double y) {
        return collect(pi).contains(x, y);
    }

    public static boolean contains(PathIterator pi, Point2D p) {
        return collect(pi).contains(p.getX(), p.getY());
    }

    @Override
    public final boolean contains(double x, double y) {
        if (typeCount < 2) {
            return false;
        }
        return Outline.of(this).contains(x, y, windingRule);
    }

    @Override
    public final boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    public static boolean contains(PathIterator pi, double x, double y, double w, double h) {
        return collect(pi).contains(x, y, w, h);
    }

    public static boolean contains(PathIterator pi, Rectangle2D r) {
        return collect(pi).contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public final boolean contains(double x, double y, double w, double h) {
        if (typeCount < 2) {
            return false;
        }
        return Outline.of(this).contains(x, y, w, h, windingRule);
    }

    @Override
    public final boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public static boolean intersects(PathIterator pi, double x, double y, double w, double h) {
        return collect(pi).intersects(x, y, w, h);
    }

    public static boolean intersects(PathIterator pi, Rectangle2D r) {
        return collect(pi).intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public final boolean intersects(double x, double y, double w, double h) {
        if (typeCount < 2) {
            return false;
        }
        return Outline.of(this).intersects(x, y, w, h, windingRule);
    }

    @Override
    public final boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /// The iterator reads this path's own storage: change the path while
    /// iterating and what the iterator returns is undefined.
    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return new SegmentIterator(windingRule, at, types, typeCount, coords);
    }

    @Override
    public final PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new FlatteningPathIterator(getPathIterator(at), flatness);
    }

    @Override
    public abstract Object clone();
}
