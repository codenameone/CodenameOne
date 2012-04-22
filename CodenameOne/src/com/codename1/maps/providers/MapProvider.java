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
package com.codename1.maps.providers;

import com.codename1.ui.geom.Dimension;
import com.codename1.maps.BoundingBox;
import com.codename1.maps.Coord;
import com.codename1.maps.Projection;
import com.codename1.maps.Tile;

/**
 * This is a generic map provider.
 * 
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public abstract class MapProvider {

    private Projection _projection;
    private Dimension _tileSize;

    /**
     * Creates a new MapProvider
     * 
     * @param p the projection system
     * @param tileSize the tile size
     */
    protected MapProvider(Projection p, Dimension tileSize) {
        _projection = p;
        _tileSize = tileSize;
    }

    /**
     * Request map to provide tiles of specific sizes. May not be supported by map provider.
     * By default it does nothing;
     *
     * @param size requested tile size
     */
    public void tileSize(Dimension size) {
    }

    /**
     * Returns the bounding box of a position ina given zoom level
     * @param position on the map
     * @param zoomLevel the zoom level
     * @return a bounding box
     */
    public abstract BoundingBox bboxFor(Coord position, int zoomLevel);

    /**
     * Gets a tile for the given bounding box
     * 
     * @param bbox a bounding box
     * @return a Tile for the given bounding box
     */
    public abstract Tile tileFor(BoundingBox bbox);

    /**
     * Maximal zoom level. Zoom levels are counted from zero to maxZoomLevel().
     * 0 is farest view, where the scale is greatest.
     * @return
     */
    public abstract int maxZoomLevel();

    /**
     * Minimal zoom level user is able to see.
     * @return
     */
    public int minZoomLevel() {
        return 0;
    }

    /**
     * Scale is the distance in map units between each pixel in tile at given zoom level.
     * @return Scale at given zoom level.
     */
    public abstract Coord scale(int zoomLevel);

    /**
     * Translates position by [pixelsX, pixelsY] at zoomLevel acordingly to maps scale.
     * @param position in map projection
     * @param zoomLevel 
     * @param pixelsX 
     * @param pixelsY 
     * @return translated position.
     */
    public Coord translate(Coord position, int zoomLevel, int pixelsX, int pixelsY) {
        Coord scale = scale(zoomLevel);
        return position.translate(pixelsY * scale.getLatitude(), pixelsX * scale.getLongitude());
    }

    /**
     * Gets the Provider projection
     * @return the Provider projection
     */
    public Projection projection() {
        return _projection;
    }

    /**
     * Gets the tile size
     * @return the tile size
     */
    public Dimension tileSize() {
        return _tileSize;
    }

    /**
     * The provider attribution.
     * 
     * @return a String of the provider attribution
     */
    public abstract String attribution();

    /**
     * Returns the maximum zoom of a specific Tile.
     * @param tile tile to check the max zoom
     * @return the max zoom of the tile
     */
    public int maxZoomFor(Tile tile) {
        int zoom;
        int height = tile.dimension().getHeight();
        int width = tile.dimension().getWidth();
        double latitude = tile.getBoundingBox().latitudeDifference();
        double longitude = tile.getBoundingBox().longitudeDifference();
        for (zoom = maxZoomLevel(); zoom > 0; zoom--) {
            Coord scale = scale(zoom);
            if ((scale.getLatitude() * height) > latitude && (scale.getLongitude() * width) > longitude) {
                break;
            }
        }
        return zoom;
    }
}
