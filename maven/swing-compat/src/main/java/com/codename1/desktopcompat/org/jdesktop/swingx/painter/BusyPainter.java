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
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.Shape;
import com.codename1.desktopcompat.java.awt.geom.AffineTransform;
import com.codename1.desktopcompat.java.awt.geom.Ellipse2D;
import com.codename1.desktopcompat.java.awt.geom.PathIterator;
import com.codename1.desktopcompat.java.awt.geom.Rectangle2D;
import com.codename1.desktopcompat.java.awt.geom.RoundRectangle2D;

/// Draws the "busy" indicator: a number of points spread evenly along a
/// trajectory, one of them highlighted and a trail behind it fading back
/// to the base color. Advancing the frame moves the highlight.
///
/// The default is the familiar wheel: bars that point outward along a
/// circle. A frame of -1 highlights nothing.
///
/// ## What differs from SwingX
///
///  - The points are placed by walking the flattened outline of the
///    trajectory at equal distances, and each point shape is turned to
///    stand across the trajectory there. The default wheel comes out the
///    same; an unusual trajectory may place its points slightly
///    differently.
///  - `getXY`, a curve helper of the original, is absent.
public class BusyPainter extends AbstractPainter<Object> {

    /// The way the highlight travels along the trajectory.
    public enum Direction {
        /// In the direction the trajectory was drawn.
        RIGHT,
        /// Against it.
        LEFT
    }

    private int frame = -1;
    private int points = 8;
    private Color baseColor = new Color(200, 200, 200);
    private Color highlightColor = Color.BLACK;
    private int trailLength = 4;
    private Shape pointShape;
    private Shape trajectory;
    private Direction direction = Direction.RIGHT;
    private boolean paintCentered;

    /// The wheel at its default height of 26 pixels.
    public BusyPainter() {
        this(26);
    }

    /// The wheel scaled to the given height.
    public BusyPainter(int height) {
        this(getScaledDefaultPoint(height), getScaledDefaultTrajectory(height));
    }

    public BusyPainter(Shape point, Shape trajectory) {
        init(point, trajectory, new Color(200, 200, 200), Color.BLACK);
    }

    /// The circle the default wheel's bars are centered on, for a wheel of
    /// the given height.
    protected static Shape getScaledDefaultTrajectory(int height) {
        float bar = height * 8 / 26f;
        return new Ellipse2D.Float(bar / 2, bar / 2, height - bar, height - bar);
    }

    /// One bar of the default wheel of the given height.
    protected static Shape getScaledDefaultPoint(int height) {
        float bar = height * 8 / 26f;
        float thick = height * 4 / 26f;
        return new RoundRectangle2D.Float(0, 0, bar, thick, thick, thick);
    }

    /// Sets both shapes and both colors at once.
    protected void init(Shape point, Shape trajectory, Color baseColor, Color highlightColor) {
        this.baseColor = baseColor;
        this.highlightColor = highlightColor;
        this.pointShape = point;
        this.trajectory = trajectory;
    }

    /// The trajectory as a polyline: `{x0, y0, x1, y1, ...}`.
    private static float[] flatten(Shape s) {
        float[] out = new float[64];
        int n = 0;
        float[] c = new float[6];
        float startX = 0;
        float startY = 0;
        for (PathIterator it = s.getPathIterator(null, 0.25); !it.isDone(); it.next()) {
            int type = it.currentSegment(c);
            float x;
            float y;
            if (type == PathIterator.SEG_CLOSE) {
                x = startX;
                y = startY;
            } else {
                x = c[0];
                y = c[1];
                if (type == PathIterator.SEG_MOVETO) {
                    startX = x;
                    startY = y;
                }
            }
            if (n >= 2 && out[n - 2] == x && out[n - 1] == y) {
                continue;
            }
            if (n + 2 > out.length) {
                float[] grown = new float[out.length * 2];
                System.arraycopy(out, 0, grown, 0, n);
                out = grown;
            }
            out[n++] = x;
            out[n++] = y;
        }
        float[] exact = new float[n];
        System.arraycopy(out, 0, exact, 0, n);
        return exact;
    }

    @Override
    protected void doPaint(Graphics2D g, Object t, int width, int height) {
        if (pointShape == null || trajectory == null || points <= 0) {
            return;
        }
        float[] line = flatten(trajectory);
        int vertices = line.length / 2;
        if (vertices < 2) {
            return;
        }
        double total = 0;
        double[] lengths = new double[vertices - 1];
        for (int i = 0; i < vertices - 1; i++) {
            double dx = line[2 * i + 2] - line[2 * i];
            double dy = line[2 * i + 3] - line[2 * i + 1];
            lengths[i] = Math.sqrt(dx * dx + dy * dy);
            total += lengths[i];
        }
        if (total <= 0) {
            return;
        }
        Rectangle2D pb = pointShape.getBounds2D();
        double offsetX = 0;
        double offsetY = 0;
        if (paintCentered) {
            Rectangle tb = trajectory.getBounds();
            offsetX = (width - tb.width) / 2.0 - tb.x;
            offsetY = (height - tb.height) / 2.0 - tb.y;
        }
        boolean closed = line[0] == line[line.length - 2] && line[1] == line[line.length - 1];
        // Along an open trajectory the last point sits on its end.
        double step = closed || points == 1 ? total / points : total / (points - 1);
        int segment = 0;
        double before = 0;
        for (int i = 0; i < points; i++) {
            double at = i * step;
            while (segment < lengths.length - 1 && before + lengths[segment] < at) {
                before += lengths[segment];
                segment++;
            }
            double len = lengths[segment];
            double f = len > 0 ? Math.min(1, (at - before) / len) : 0;
            double x0 = line[2 * segment];
            double y0 = line[2 * segment + 1];
            double dx = line[2 * segment + 2] - x0;
            double dy = line[2 * segment + 3] - y0;
            Graphics copy = g.create();
            try {
                if (copy instanceof Graphics2D) {
                    Graphics2D pg = (Graphics2D) copy;
                    pg.translate(offsetX + x0 + dx * f, offsetY + y0 + dy * f);
                    if (len > 0) {
                        // Across the trajectory: its direction turned a
                        // quarter.
                        pg.transform(AffineTransform.getRotateInstance(-dy, dx));
                    }
                    pg.translate(-pb.getCenterX(), -pb.getCenterY());
                    pg.setColor(colorOf(i));
                    pg.fill(pointShape);
                }
            } finally {
                copy.dispose();
            }
        }
    }

    /// The color of point `i` in the current frame.
    private Color colorOf(int i) {
        if (frame < 0 || baseColor == null || highlightColor == null) {
            return baseColor != null ? baseColor : Color.GRAY;
        }
        int lead = cn1HighlightedPoint();
        // How many points behind the highlighted one this point is.
        int behind = direction == Direction.LEFT ? i - lead : lead - i;
        if (behind < 0) {
            behind += points;
        }
        if (behind == 0) {
            return highlightColor;
        }
        if (behind >= trailLength || trailLength <= 0) {
            return baseColor;
        }
        float terp = 1f - (float) (trailLength - behind) / (float) trailLength;
        return new Color(mix(highlightColor.getRed(), baseColor.getRed(), terp),
                mix(highlightColor.getGreen(), baseColor.getGreen(), terp),
                mix(highlightColor.getBlue(), baseColor.getBlue(), terp),
                mix(highlightColor.getAlpha(), baseColor.getAlpha(), terp));
    }

    private static int mix(int from, int to, float t) {
        int v = Math.round(from + (to - from) * t);
        return v < 0 ? 0 : v > 255 ? 255 : v;
    }

    /// The point the highlight is on in the current frame, or -1.
    int cn1HighlightedPoint() {
        if (frame < 0 || points <= 0) {
            return -1;
        }
        int along = frame % points;
        return direction == Direction.LEFT ? points - 1 - along : along;
    }

    /// The color point `i` is drawn in in the current frame.
    Color cn1PointColor(int i) {
        return colorOf(i);
    }

    public boolean isPaintCentered() {
        return paintCentered;
    }

    /// Whether the trajectory is centered in the painted area rather than
    /// drawn at its own coordinates.
    public void setPaintCentered(boolean paintCentered) {
        boolean old = this.paintCentered;
        this.paintCentered = paintCentered;
        setDirty(true);
        firePropertyChange("paintCentered", Boolean.valueOf(old), Boolean.valueOf(paintCentered));
    }

    public int getFrame() {
        return frame;
    }

    /// Sets the point the highlight is on; -1 highlights none.
    public void setFrame(int frame) {
        int old = this.frame;
        this.frame = frame;
        setDirty(true);
        firePropertyChange("frame", Integer.valueOf(old), Integer.valueOf(frame));
    }

    public Color getBaseColor() {
        return baseColor;
    }

    public void setBaseColor(Color baseColor) {
        Color old = this.baseColor;
        this.baseColor = baseColor;
        setDirty(true);
        firePropertyChange("baseColor", old, baseColor);
    }

    public Color getHighlightColor() {
        return highlightColor;
    }

    public void setHighlightColor(Color highlightColor) {
        Color old = this.highlightColor;
        this.highlightColor = highlightColor;
        setDirty(true);
        firePropertyChange("highlightColor", old, highlightColor);
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        int old = this.points;
        this.points = points;
        setDirty(true);
        firePropertyChange("points", Integer.valueOf(old), Integer.valueOf(points));
    }

    public int getTrailLength() {
        return trailLength;
    }

    /// The number of points behind the highlight that fade back to the
    /// base color.
    public void setTrailLength(int trailLength) {
        int old = this.trailLength;
        this.trailLength = trailLength;
        setDirty(true);
        firePropertyChange("trailLength", Integer.valueOf(old), Integer.valueOf(trailLength));
    }

    public final Shape getPointShape() {
        return pointShape;
    }

    public final void setPointShape(Shape pointShape) {
        Shape old = this.pointShape;
        this.pointShape = pointShape;
        setDirty(true);
        firePropertyChange("pointShape", old, pointShape);
    }

    public final Shape getTrajectory() {
        return trajectory;
    }

    public final void setTrajectory(Shape trajectory) {
        Shape old = this.trajectory;
        this.trajectory = trajectory;
        setDirty(true);
        firePropertyChange("trajectory", old, trajectory);
    }

    public Direction getDirection() {
        return direction;
    }

    /// Sets the way the trail points; `null` is [Direction#RIGHT].
    public void setDirection(Direction dir) {
        Direction old = direction;
        direction = dir == null ? Direction.RIGHT : dir;
        setDirty(true);
        firePropertyChange("direction", old, direction);
    }
}
