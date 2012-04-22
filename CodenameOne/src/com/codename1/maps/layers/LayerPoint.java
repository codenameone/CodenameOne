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

import com.codename1.maps.Coord;
import com.codename1.ui.geom.Point;
import com.codename1.maps.Tile;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

/**
 * This is a Point Layer.
 * 
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public class LayerPoint extends Coord implements Layer{

    private final String description;
    private Image icon;

    /**
     * Creates a Point Layer.
     * 
     * @param position the position of the Point
     * @param getDescription the getDescription of the Point
     * @param icon icon of the Point
     */
    public LayerPoint(Coord position, String description, Image icon) {
        super(position);
        this.description = description;
        this.icon = icon;
    }

    /**
     * Gets the Point description
     * @return the Point description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the point Icon
     * @return the point Icon
     */
    public Image getIcon() {
        return icon;
    }

    /**
     * @inheritDoc
     */
    public void paint(Graphics g, Tile tile) {
        Point pos = tile.pointPosition(this);
        int width = 6;
        int height = 6;
        if (icon != null) {
            width = icon.getWidth();
            height = icon.getHeight();
        }
        int x = pos.getX() - width / 2;
        int y = pos.getY() - height / 2;
        
        if (icon == null) {
            g.fillRect(x, y, width, height);
        } else {
            g.drawImage(icon, x, y);
        }
        if (description != null) {
            g.drawString(getDescription(), x + width + 1, pos.getY() - g.getFont().getHeight() / 2 - 1);
        }
    }
}
