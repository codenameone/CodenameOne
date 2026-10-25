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
package com.codename1.fxcompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.HPos;
import javafx.geometry.HorizontalDirection;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.geometry.VerticalDirection;

public class GeometryTest {

    @Test
    public void insets() {
        Insets insets = new Insets(1, 2, 3, 4);
        assertEquals(1.0, insets.getTop(), 0d);
        assertEquals(2.0, insets.getRight(), 0d);
        assertEquals(3.0, insets.getBottom(), 0d);
        assertEquals(4.0, insets.getLeft(), 0d);
        Insets uniform = new Insets(5);
        assertEquals(5.0, uniform.getTop(), 0d);
        assertEquals(5.0, uniform.getLeft(), 0d);
        assertEquals(insets, new Insets(1, 2, 3, 4));
        assertEquals(insets.hashCode(), new Insets(1, 2, 3, 4).hashCode());
        assertNotEquals(insets, uniform);
        assertNotEquals(insets, "insets");
        assertEquals(Insets.EMPTY, new Insets(0));
        assertEquals("Insets [top=1.0, right=2.0, bottom=3.0, left=4.0]", insets.toString());
    }

    @Test
    public void point() {
        Point2D p = new Point2D(3, 4);
        assertEquals(3.0, p.getX(), 0d);
        assertEquals(4.0, p.getY(), 0d);
        assertEquals(5.0, p.magnitude(), 1e-12);
        assertEquals(5.0, p.distance(0, 0), 1e-12);
        assertEquals(5.0, p.distance(Point2D.ZERO), 1e-12);
        assertEquals(new Point2D(4, 6), p.add(1, 2));
        assertEquals(new Point2D(4, 6), p.add(new Point2D(1, 2)));
        assertEquals(new Point2D(2, 2), p.subtract(1, 2));
        assertEquals(new Point2D(2, 2), p.subtract(new Point2D(1, 2)));
        assertEquals(new Point2D(6, 8), p.multiply(2));
        Point2D unit = p.normalize();
        assertEquals(0.6, unit.getX(), 1e-12);
        assertEquals(0.8, unit.getY(), 1e-12);
        assertEquals(Point2D.ZERO, Point2D.ZERO.normalize());
        assertEquals(new Point2D(2, 3), p.midpoint(1, 2));
        assertEquals(new Point2D(1.5, 2), p.midpoint(Point2D.ZERO));
        assertEquals(11.0, p.dotProduct(1, 2), 0d);
        assertEquals(25.0, p.dotProduct(p), 0d);
        assertEquals(90.0, new Point2D(1, 0).angle(0, 1), 1e-9);
        assertEquals(0.0, new Point2D(1, 0).angle(new Point2D(5, 0)), 1e-9);
        assertEquals(180.0, new Point2D(1, 0).angle(-2, 0), 1e-9);
        assertEquals(45.0, new Point2D(1, 0).angle(1, 1), 1e-9);
        assertEquals(90.0, Point2D.ZERO.angle(new Point2D(1, 0), new Point2D(0, 3)), 1e-9);
        assertEquals(p.hashCode(), new Point2D(3, 4).hashCode());
        assertNotEquals(p, new Point2D(4, 3));
        assertNotEquals(p, "point");
        assertEquals("Point2D [x = 3.0, y = 4.0]", p.toString());
        assertEquals(new Point2D(0.0, 0.0), new Point2D(-0.0, 0.0));
        assertEquals(new Point2D(0.0, 0.0).hashCode(), new Point2D(-0.0, 0.0).hashCode());
        assertNotEquals(new Point2D(Double.NaN, 0.0), new Point2D(Double.NaN, 0.0));
    }

    @Test
    public void dimension() {
        Dimension2D d = new Dimension2D(2, 3);
        assertEquals(2.0, d.getWidth(), 0d);
        assertEquals(3.0, d.getHeight(), 0d);
        assertEquals(d, new Dimension2D(2, 3));
        assertEquals(d.hashCode(), new Dimension2D(2, 3).hashCode());
        assertNotEquals(d, new Dimension2D(3, 2));
        assertEquals("Dimension2D [width = 2.0, height = 3.0]", d.toString());
    }

    @Test
    public void rectangle() {
        Rectangle2D r = new Rectangle2D(10, 20, 30, 40);
        assertEquals(10.0, r.getMinX(), 0d);
        assertEquals(20.0, r.getMinY(), 0d);
        assertEquals(30.0, r.getWidth(), 0d);
        assertEquals(40.0, r.getHeight(), 0d);
        assertEquals(40.0, r.getMaxX(), 0d);
        assertEquals(60.0, r.getMaxY(), 0d);
        assertTrue(r.contains(10, 20));
        assertTrue(r.contains(40, 60));
        assertFalse(r.contains(41, 60));
        assertTrue(r.contains(new Point2D(25, 30)));
        assertFalse(r.contains((Point2D) null));
        assertTrue(r.contains(new Rectangle2D(15, 25, 5, 5)));
        assertFalse(r.contains(new Rectangle2D(15, 25, 50, 5)));
        assertFalse(r.contains((Rectangle2D) null));
        assertTrue(r.contains(10, 20, 30, 40));
        assertFalse(r.contains(10, 20, 31, 40));
        assertTrue(r.intersects(new Rectangle2D(35, 55, 100, 100)));
        assertFalse(r.intersects(new Rectangle2D(41, 61, 5, 5)));
        assertFalse(r.intersects((Rectangle2D) null));
        assertTrue(r.intersects(0, 0, 11, 21));
        assertFalse(r.intersects(0, 0, 5, 5));
        assertEquals(r, new Rectangle2D(10, 20, 30, 40));
        assertEquals(r.hashCode(), new Rectangle2D(10, 20, 30, 40).hashCode());
        assertNotEquals(r, Rectangle2D.EMPTY);
        assertTrue(r.toString().startsWith("Rectangle2D [minX = 10.0"));
        try {
            new Rectangle2D(0, 0, -1, 1);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
    }

    @Test
    public void boundingBox() {
        Bounds b = new BoundingBox(1, 2, 10, 20);
        assertEquals(1.0, b.getMinX(), 0d);
        assertEquals(2.0, b.getMinY(), 0d);
        assertEquals(0.0, b.getMinZ(), 0d);
        assertEquals(10.0, b.getWidth(), 0d);
        assertEquals(20.0, b.getHeight(), 0d);
        assertEquals(0.0, b.getDepth(), 0d);
        assertEquals(11.0, b.getMaxX(), 0d);
        assertEquals(22.0, b.getMaxY(), 0d);
        assertEquals(0.0, b.getMaxZ(), 0d);
        assertEquals(6.0, b.getCenterX(), 0d);
        assertEquals(12.0, b.getCenterY(), 0d);
        assertEquals(0.0, b.getCenterZ(), 0d);
        assertFalse(b.isEmpty());
        assertTrue(new BoundingBox(0, 0, -1, 5).isEmpty());
        assertTrue(b.contains(1, 2));
        assertTrue(b.contains(11, 22));
        assertFalse(b.contains(12, 22));
        assertTrue(b.contains(new Point2D(5, 5)));
        assertFalse(b.contains((Point2D) null));
        assertTrue(b.contains(5, 5, 0));
        assertFalse(b.contains(5, 5, 1));
        assertTrue(b.contains(new BoundingBox(2, 3, 1, 1)));
        assertFalse(b.contains(new BoundingBox(2, 3, 100, 1)));
        assertFalse(b.contains((Bounds) null));
        assertTrue(b.contains(2, 3, 4, 5));
        assertTrue(b.contains(2, 3, 0, 4, 5, 0));
        assertTrue(b.intersects(new BoundingBox(10, 20, 50, 50)));
        assertFalse(b.intersects(new BoundingBox(12, 23, 5, 5)));
        assertFalse(b.intersects((Bounds) null));
        assertTrue(b.intersects(0, 0, 1, 2));
        assertFalse(b.intersects(0, 0, 0.5, 0.5));
        assertTrue(b.intersects(0, 0, 0, 5, 5, 0));
        assertFalse(new BoundingBox(0, 0, -1, 5).contains(0, 0));
        Bounds box = new BoundingBox(0, 0, 0, 2, 4, 6);
        assertEquals(6.0, box.getMaxZ(), 0d);
        assertEquals(3.0, box.getCenterZ(), 0d);
        assertTrue(box.contains(1, 1, 5));
        assertEquals(b, new BoundingBox(1, 2, 10, 20));
        assertEquals(b.hashCode(), new BoundingBox(1, 2, 10, 20).hashCode());
        assertNotEquals(b, box);
        assertTrue(b.toString().startsWith("BoundingBox [minX:1.0"));
    }

    @Test
    public void positions() {
        assertSame(VPos.TOP, Pos.TOP_LEFT.getVpos());
        assertSame(HPos.LEFT, Pos.TOP_LEFT.getHpos());
        assertSame(VPos.CENTER, Pos.CENTER.getVpos());
        assertSame(HPos.CENTER, Pos.CENTER.getHpos());
        assertSame(VPos.BASELINE, Pos.BASELINE_RIGHT.getVpos());
        assertSame(HPos.RIGHT, Pos.BASELINE_RIGHT.getHpos());
        assertSame(VPos.BOTTOM, Pos.BOTTOM_CENTER.getVpos());
        assertEquals(12, Pos.values().length);
        assertSame(Pos.CENTER_LEFT, Pos.valueOf("CENTER_LEFT"));
        assertEquals(3, HPos.values().length);
        assertEquals(4, VPos.values().length);
    }

    @Test
    public void directions() {
        assertTrue(Side.LEFT.isVertical());
        assertTrue(Side.RIGHT.isVertical());
        assertTrue(Side.TOP.isHorizontal());
        assertTrue(Side.BOTTOM.isHorizontal());
        assertFalse(Side.TOP.isVertical());
        assertFalse(Side.LEFT.isHorizontal());
        assertEquals(2, Orientation.values().length);
        assertEquals(2, HorizontalDirection.values().length);
        assertEquals(2, VerticalDirection.values().length);
        assertEquals(3, NodeOrientation.values().length);
        assertSame(NodeOrientation.INHERIT, NodeOrientation.valueOf("INHERIT"));
        assertSame(Orientation.VERTICAL, Orientation.valueOf("VERTICAL"));
    }
}
