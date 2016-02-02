/*
 * Copyright (c) 2010, 2011 Itiner.pl. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Itiner designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Itiner in the LICENSE.txt file that accompanied this code.
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
 */
package com.codename1.maps.layers;

import com.codename1.util.MathUtil;
import com.codename1.ui.Graphics;
import com.codename1.maps.Coord;
import com.codename1.maps.Mercator;
import com.codename1.maps.Projection;
import com.codename1.ui.geom.Point;
import com.codename1.maps.providers.MapProvider;
import com.codename1.maps.Tile;

/**
 * This class is responsible for painting arrows that indicates direction
 * of walk on map.
 * 
 * @author Michal Koperski
 */
public class ArrowLinesLayer extends LinesLayer {

    private int arrowSegmentLength = 60;
    private final int minArrowSementLength = 20;
    private int arrowWidth = 5;
    private int arrowHeight = 10;

    /**
     * Constructor with default projection Mercator.
     */
    public ArrowLinesLayer() {
        this(new Mercator(), "");
    }

    /**
     * Constructor with default projection Mercator.
     */
    public ArrowLinesLayer(String name) {
        this(new Mercator(), name);
    }

    /**
     * {@inheritDoc}
     */
    public ArrowLinesLayer(Projection p, String name) {
        super(p, name);
    }

    /**
     * Paints arrows on each segment. arrowSegmentLength decides how many
     * arrows will be on each segment.
     * @param g
     * @param segment
     * @param tile
     */
    protected void paintSegment(Graphics g, Coord[] segment, Tile tile) {
        super.paintSegment(g, segment, tile);
        int pointsNo = segment.length;
        for (int i = 1; i < pointsNo; i++) {
            Coord start = segment[i - 1];
            Coord end = segment[i];
            Point s = tile.pointPosition(start);
            Point e = tile.pointPosition(end);
            int noOfSegments = calculateLength(s, e) / arrowSegmentLength;
            if (noOfSegments == 0 && calculateLength(s, e) > minArrowSementLength) {
                noOfSegments = 1;
            }
            for (int j = 1; j <= noOfSegments; j++) {
                if (j == 1) {
                    double div = 1.0 / noOfSegments;
                    drawArrow(g, new Point(s.getX(), s.getY()),
                            new Point((int) (div * e.getX() + s.getX() * (1 - div)),
                            (int) (div * e.getY() + s.getY() * (1 - div))));
                } else if (j == noOfSegments) {
                    double div = (noOfSegments - 1) / (noOfSegments * 1.0);
                    drawArrow(g, new Point((int) (div * e.getX() + s.getX() * (1 - div)),
                            (int) (div * e.getY() + s.getY() * (1 - div))),
                            new Point(e.getX(), e.getY()));
                } else {
                    double div = ((j - 1) * 1.0) / noOfSegments;
                    double div2 = (j * 1.0) / noOfSegments;
                    drawArrow(g, new Point((int) (div * e.getX() + s.getX() * (1 - div)),
                            (int) (div * e.getY() + s.getY() * (1 - div))),
                            new Point((int) (div2 * e.getX() + s.getX() * (1 - div2)),
                            (int) (div2 * e.getY() + s.getY() * (1 - div2))));
                }
            }
        }
    }

    /**
     * This method clones arrowHead object which represents arrow and translate
     * it to position on the map.
     * @param g Graphics Object to paint on
     * @param s starting Point 
     * @param e ending Point
     */
    private void drawArrow(Graphics g, Point s, Point e) {
        double aDir = MathUtil.atan2(e.getY() - s.getY(), e.getX() - s.getX());

        if (aDir < -Math.PI / 2) {
            aDir = MathUtil.atan2(s.getY() - e.getY(), s.getX() - e.getX());
            aDir -= Math.PI;
        }
        aDir -= Math.PI / 2;

        ArrowHead arrowHead = new ArrowHead(arrowHeight, arrowWidth);
        arrowHead.rotate(aDir);
        arrowHead.translate(s.getX() + ((e.getX() - s.getX()) / 2), s.getY() + ((e.getY() - s.getY()) / 2));
        arrowHead.paint(g);
    }

    private int calculateLength(Point s, Point e) {
        return (int) Math.sqrt(sqr(s.getX() - e.getX()) + sqr(s.getY() - e.getY()));
    }

    private double sqr(double a) {
        return a * a;
    }

    /**
     * Returns the arrow height in pixels
     * @return the arrow height
     */
    public int getArrowHeight() {
        return arrowHeight;
    }

    /**
     * Sets the arrow height
     * 
     * @param arrowHeight 
     */
    public void setArrowHeight(int arrowHeight) {
        this.arrowHeight = arrowHeight;
    }

    /**
     * Gets the arrow segment length
     * @return segment length
     */
    public int getArrowSegmentLength() {
        return arrowSegmentLength;
    }

    /**
     * Sets the arrow segment length
     * 
     * @param arrowSegmentLength to set
     */
    public void setArrowSegmentLength(int arrowSegmentLength) {
        this.arrowSegmentLength = arrowSegmentLength;
    }

    /**
     * Gets the arrow width in pixels
     * @return the arrow width
     */
    public int getArrowWidth() {
        return arrowWidth;
    }

    /**
     * Sets the arrow width
     * @param arrowWidth to set
     */
    public void setArrowWidth(int arrowWidth) {
        this.arrowWidth = arrowWidth;
    }

    private class ArrowHead {

        private int _height;
        private int _width;
        private Point[] _nodes;

        ArrowHead(int height, int width) {
            _height = height;
            _width = width;
            _nodes = new Point[3];
            _nodes[0] = new Point(0, 0);
            _nodes[1] = new Point(_width, -_height);
            _nodes[2] = new Point(-_width, -_height);
        }

        public int getHeight() {
            return _height;
        }

        public int getWidth() {
            return _width;
        }

        public Point[] getNodes() {
            return _nodes;
        }

        public void rotate(double aDir) {
            int nlen =  _nodes.length;
            for (int i = 0; i < nlen; i++) {
                _nodes[i] = new Point(rotateX(_nodes[i].getX(), _nodes[i].getY(), aDir),
                        rotateY(_nodes[i].getX(), _nodes[i].getY(), aDir));
            }
        }

        private int rotateX(int x, int y, double aDir) {
            return (int) ((x * Math.cos(aDir)) - (y * Math.sin(aDir)));
        }

        private int rotateY(int x, int y, double aDir) {
            return (int) ((x * Math.sin(aDir)) + (y * Math.cos(aDir)));
        }

        public void translate(int x, int y) {
            int nlen = _nodes.length;
            for (int i = 0; i < nlen; i++) {
                _nodes[i] = new Point(_nodes[i].getX() + x, _nodes[i].getY() + y);
            }
        }

        public void paint(Graphics g) {
            g.fillTriangle(_nodes[0].getX(), _nodes[0].getY(),
                    _nodes[1].getX(), _nodes[1].getY(),
                    _nodes[2].getX(), _nodes[2].getY());
        }
    }
}
