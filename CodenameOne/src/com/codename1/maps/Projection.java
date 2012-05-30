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

/**
 * This class represents a projection type.
 * a Projection has the ability to translate a WGS84 Coordinate to a 
 * projected Coordinate.
 * 
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public abstract class Projection {

    private BoundingBox extent;

    /**
     * Creates a projection to the given bounding box
     * @param extent the bounding box of this projection
     */
    protected Projection(BoundingBox extent) {
        this.extent = extent;
    }

    /**
     * Gets the projection bounding box
     * @return bounding box
     */
    public BoundingBox extent() {
        return extent;
    }

    /**
     * Converts a given WGS84 coordinate to a projection coordinate
     * @param wgs84
     * @return 
     */
    public abstract Coord fromWGS84(Coord wgs84);

    /**
     * Converts a projected coordinate to a WGS84 coordinate
     * @param projection
     * @return 
     */
    public abstract Coord toWGS84(Coord projection);

    /**
     * a utility method that converts an array of WGS84 coordinate to the 
     * projection coordinates system.
     * 
     * @param coords an array to converts
     * @return a converted array
     */
    public final Coord[] fromWGS84(Coord[] coords) {
        Coord[] newCoords = new Coord[coords.length];
        int length = coords.length;
        for (int i = 0; i < length; i++) {
            newCoords[i] = fromWGS84(coords[i]);
        }
        return newCoords;
    }

    /**
     * Converts a WGS84 bounding box to the projection system bounding box
     * 
     * @param bbox bounding box too convert
     * @return a converted bounding box
     */
    public final BoundingBox fromWGS84(BoundingBox bbox) {
        return new BoundingBox(fromWGS84(bbox.getSouthWest()), fromWGS84(bbox.getNorthEast()));
    }
    
    /**
     * Converts a projected bounding box to a WGS84 bounding box
     * 
     * @param bbox bounding box too convert
     * @return a converted bounding box
     */
    public final BoundingBox toWGS84(BoundingBox bbox) {
        return new BoundingBox(toWGS84(bbox.getSouthWest()), toWGS84(bbox.getNorthEast()));
    }
    
}
