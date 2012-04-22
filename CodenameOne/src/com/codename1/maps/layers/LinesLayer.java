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

import com.codename1.ui.Graphics;
import java.util.Vector;
import com.codename1.maps.BoundingBox;
import com.codename1.maps.Coord;
import com.codename1.maps.Projection;
import com.codename1.ui.geom.Point;
import com.codename1.maps.providers.MapProvider;
import com.codename1.maps.Tile;

/**
 * This is a Lines Layer
 * 
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public class LinesLayer extends AbstractLayer {

    private Vector _lineSegments;
    protected int _lineColor;

    /**
     * @inheritDoc
     */
    public LinesLayer(Projection p, String name) {
        super(p, name);
        _lineSegments = new Vector();
        _lineColor = 0x000000;
    }

    /**
     * @inheritDoc
     */
    public void paint(Graphics g, Tile screenTile) {
        g.setColor(_lineColor);
        g.setAntiAliased(true);
        int segmentsNo = _lineSegments.size();
        for (int i = 0; i < segmentsNo; i++) {
            paintSegment(g, (Coord[]) _lineSegments.elementAt(i), screenTile);
        }
    }

    /**
     * Paint a segment.
     * 
     * @param g a Graphics Object to paint on
     * @param segment array of Coord to draw a Line.
     * @param tile 
     */
    protected void paintSegment(Graphics g, Coord[] segment, Tile tile) {
        int pointsNo = segment.length;
        for (int i = 1; i < pointsNo; i++) {
            Coord start = (Coord) segment[i - 1];
            Coord end = (Coord) segment[i];
            Point s = tile.pointPosition(start);
            Point e = tile.pointPosition(end);
            g.drawLine(s.getX(), s.getY(), e.getX(), e.getY());
            // lame & simple way to make line thicker
            g.drawLine(s.getX() - 1, s.getY(), e.getX() - 1, e.getY());
            g.drawLine(s.getX() + 1, s.getY(), e.getX() + 1, e.getY());
            g.drawLine(s.getX(), s.getY() - 1, e.getX(), e.getY() - 1);
            g.drawLine(s.getX(), s.getY() + 1, e.getX(), e.getY() + 1);
        }
    }

    /**
     * Adds a Line segment to the Layer
     * @param coords 
     */
    public void addLineSegment(Coord[] coords) {
        if (coords == null || coords.length <= 1) {
            return;
        }
        if (!coords[0].isProjected()) {
            coords = getProjection().fromWGS84(coords);
        }
        _lineSegments.addElement(coords);
    }

    /**
     * Sets the color of the Lines
     * @param rgb 
     */
    public void lineColor(int rgb) {
        _lineColor = rgb;
    }

    /**
     * @inheritDoc
     */
    public BoundingBox boundingBox() {
        BoundingBox bbox = null;
        for (int i = 0; i < _lineSegments.size(); i++) {
            Coord[] coords = (Coord[]) _lineSegments.elementAt(i);
            BoundingBox cBbox = BoundingBox.create(coords);
            if (bbox == null) {
                bbox = cBbox;
            } else {
                bbox = bbox.extend(cBbox);
            }
        }
        return bbox;
    }
}
