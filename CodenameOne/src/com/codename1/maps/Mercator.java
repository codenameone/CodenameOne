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
package com.codename1.maps;

import com.codename1.util.MathUtil;

/**
 * Represents a Mercator projection http://en.wikipedia.org/wiki/Mercator_projection
 * 
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public class Mercator extends Projection {
    // http://wiki.osgeo.org/wiki/WMS_Tiling_Client_Recommendation

    private final static double SIZE = 20037508.34;

    /**
     * Creates a new Mercator projection
     */
    public Mercator() {
        super(new BoundingBox(new Coord(-SIZE, -SIZE, true), new Coord(SIZE, SIZE, true)));
    }

    /**
     * Create a projected Mercator Coord from the given coordinate
     * @param latitude to project
     * @param longitude to project
     * @return a projected Mercator 
     */
    public static Coord forwardMercator(double latitude, double longitude) {
        double x = longitude * SIZE / 180;
        double y = MathUtil.log(Math.tan((90 + latitude) * Math.PI / 360)) / (Math.PI / 180) * SIZE / 180;
        return new Coord(y, x, true);
    }

    /**
     * Create a unprojected Coord(Latitude, Longitude) from the projected Coord
     * @param latitude projected latitude
     * @param longitude projected longitude
     * @return unprojected Coord
     */
    public static Coord inverseMercator(double latitude, double longitude) {
        double x = (longitude / SIZE) * 180;
        double y = (latitude / SIZE) * 180;
        y = 180.0 / Math.PI * (2 * MathUtil.atan(MathUtil.exp(y * Math.PI / 180)) - Math.PI / 2);
        return new Coord(x, y, false);
    }

    /**
     * Create a projected Mercator Coord from the given coordinate
     * @param wgs84 coordinate to project
     * @return projected Mercator Coord
     */
    public Coord fromWGS84(Coord wgs84) {
        return forwardMercator(wgs84.getLatitude(), wgs84.getLongitude());
    }

    /**
     * Create a Coord(Latitude, Longitude) from the projected Coord
     * @param wgs84 projected Coord
     * @return unprojected Latitude, Longitude
     */
    public Coord toWGS84(Coord projection) {
        return inverseMercator(projection.getLatitude(), projection.getLongitude());
    }
}
