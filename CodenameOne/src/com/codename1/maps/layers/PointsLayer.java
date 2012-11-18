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
import com.codename1.maps.Mercator;
import com.codename1.maps.Projection;
import com.codename1.maps.Tile;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;

/**
 * This is a Points Layer
 * 
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public class PointsLayer extends AbstractLayer {

    private Vector points = new Vector();
    private Image icon;
    private EventDispatcher dispatcher = new EventDispatcher();
    private Font f = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
    /**
     * Constructor with default projection Mercator.
     */
    public PointsLayer() {
        super(new Mercator(), "");
    }

    /**
     * Constructor with default projection Mercator.
     */
    public PointsLayer(String name) {
        super(new Mercator(), name);
    }

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
        g.setFont(f);
        for (int i = 0; i < length; i++) {
            PointLayer point = (PointLayer) points.elementAt(i);
            if (tile.getBoundingBox().contains(point)) {
                point.paint(g, tile);
            }
        }
    }

    /**
     * Adds a point to the PointsLayer
     * 
     * @param point a point to add
     */
    public void addPoint(PointLayer point) {
        Image pointIcon = point.getIcon();
        if (pointIcon == null) {
            point.setIcon(icon);
        }
        if(!point.isProjected()){
            Coord c = getProjection().fromWGS84(point);
            point.setLatitude(c.getLatitude());
            point.setLongitude(c.getLongitude());
            point.setProjected(true);
        }
        points.addElement(point);
    }

    /**
     * Removes a point from the PointsLayer
     * 
     * @param point to remove from the PointsLayer
     */
    public void removePoint(PointLayer point) {
        if(!point.isProjected()){
            Coord c = getProjection().fromWGS84(point);
            point.setLatitude(c.getLatitude());
            point.setLongitude(c.getLongitude());
            point.setProjected(true);
        }
        points.removeElement(point);
    }
    
    
    /**
     * @inheritDoc
     */
    public BoundingBox boundingBox() {
        return BoundingBox.create(points);
    }

    /**
     * Adds a listener to the Points Layer which will cause an event to dispatch 
     * on click the ActionEvent will contain the pressed PointLayer unprojected
     * 
     * @param l implementation of the action listener interface
     */
    public void addActionListener(ActionListener l) {
        dispatcher.addListener(l);
    }

    /**
     * Removes the given action listener Points Layer
     * 
     * @param l implementation of the action listener interface
     */
    public void removeActionListener(ActionListener l) {
        dispatcher.removeListener(l);
    }

    /**
     * Trigger an event for the points that in contained in the BoundingBox
     * @param box the BoundingBox to trigger event.
     */
    public void fireActionEvent(BoundingBox box) {
        for (int i = 0; i < points.size(); i++) {
            PointLayer point = (PointLayer) points.elementAt(i);
            if (box.contains(point)) {
                Coord c = projection.toWGS84(point);
                //unprojected point
                PointLayer pl = new PointLayer(c, point.getName(), point.getIcon());
                dispatcher.fireActionEvent(new ActionEvent(pl));
                return;
            }
        }
    }
}
