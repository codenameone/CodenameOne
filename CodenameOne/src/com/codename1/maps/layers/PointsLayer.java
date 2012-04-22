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

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import java.util.Vector;
import com.codename1.maps.BoundingBox;
import com.codename1.maps.Coord;
import com.codename1.maps.Projection;
import com.codename1.ui.geom.Point;
import com.codename1.maps.providers.MapProvider;
import com.codename1.maps.Tile;

/**
 * This is a Points Layer
 * 
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public class PointsLayer extends AbstractLayer {

    private Vector points = new Vector();

    private Image icon;
    
    /**
     * @inheritDoc
     */
    public PointsLayer(Projection p, String name) {
        super(p, name);
    }

    /**
     * Sets the Points icon
     * @param icon 
     */
    public void setPointIcon(Image icon) {
        this.icon = icon;
    }

    /**
     * @inheritDoc
     */
    public void paint(Graphics g, Tile tile) {
        int length = points.size();
        g.setColor(0);
        g.setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));
        for (int i = 0; i < length; i++) {
            LayerPoint point = (LayerPoint) points.elementAt(i);
            if (tile.getBoundingBox().contains(point)) {
                point.paint(g, tile);
            }
        }
    }

    /**
     * Adds a point to the Layer
     * 
     * @param position the position of the point
     * @param description the description of the point
     */
    public void addPoint(Coord position, String description) {
        addPoint(position, description, icon);
    }

    /**
     * Adds a point to the Layer
     * 
     * @param position the position of the point
     * @param description the description of the point
     * @param icon the icon of the point if null the Layer icon is taken.
     */
    public void addPoint(Coord position, String description, Image pointIcon) {
        if (pointIcon == null) {
            pointIcon = icon;
        }
        Coord projectedPosition = position.isProjected() ? position : getProjection().fromWGS84(position);
        points.addElement(new LayerPoint(projectedPosition, description, pointIcon));
    }

    /**
     * @inheritDoc
     */
    public BoundingBox boundingBox() {
        return BoundingBox.create(points);
    }
}
