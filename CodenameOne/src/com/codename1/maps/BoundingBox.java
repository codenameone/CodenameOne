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

import java.util.Vector;

/**
 * This class declares a bounding box of coordinates on the map.
 * 
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public class BoundingBox {

    private final Coord _southWest;
    private final Coord _northEast;

    /**
     * Constructor with 2 coordinates for south west and north east
     * 
     * @param getSouthWest coordinate
     * @param getNorthEast coordinate
     */
    public BoundingBox(Coord southWest, Coord northEast) {
        _southWest = southWest;
        _northEast = northEast;
    }

    /**
     * Gets the /south west coordinate
     * @return 
     */
    public Coord getSouthWest() {
        return _southWest;
    }

    /**
     * Gets the north east coordinate
     * @return 
     */
    public Coord getNorthEast() {
        return _northEast;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "SW: " + _southWest + " NE: " + _northEast;
    }

    /**
     * @return The difference between SE and NW getLongitude in degrees.
     */
    public double latitudeDifference() {
        return _northEast.getLatitude() - _southWest.getLatitude();
    }

    /**
     * @return The difference between SE and NW latitudes in degrees.
     */
    public double longitudeDifference() {
        return _northEast.getLongitude() - _southWest.getLongitude();
    }

    /**
     * indicates if the given coordinate is inside the counding box
     * @param cur coordinate to check
     * @return true if the given coordinate is contained in the bounding box
     */
    public boolean contains(Coord cur) {
        double latitude = cur.getLatitude();
        if (latitude > getNorthEast().getLatitude() || latitude < getSouthWest().getLatitude()) {
            return false;
        }
        double longitude = cur.getLongitude();
        if (longitude < getSouthWest().getLongitude() || longitude > getNorthEast().getLongitude()) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object other) {
        if (!(other instanceof BoundingBox)) {
            return false;
        }
        BoundingBox o = (BoundingBox) other;
        return _southWest.equals(o._southWest) && _northEast.equals(o._northEast);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + this._southWest.hashCode();
        hash = 29 * hash + this._northEast.hashCode();
        return hash;
    }

    /**
     * create a smallest bounding box that contains all of the given coordinates
     * @param coords given coordinates to create a wrapping bounding box.
     * @return a bounding box that contains all of the coordinates
     */
    public static BoundingBox create(Coord[] coords) {
        if (coords.length <= 0) {
            throw new RuntimeException("There must be at least 1 coordinate.");
        }
        
        double north = coords[0].getLatitude();
        double south = coords[0].getLatitude();
        double east = coords[0].getLongitude();
        double west = coords[0].getLongitude();

        boolean projected = true;
        int clen = coords.length;
        for (int i = 0; i < clen; i++) {
            Coord c = coords[i];
            projected = c.isProjected();
            north = Math.max(north, c.getLatitude());
            east = Math.max(east, c.getLongitude());
            south = Math.min(south, c.getLatitude());
            west = Math.min(west, c.getLongitude());
        }
        return new BoundingBox(new Coord(south, west, projected), new Coord(north, east, projected));
    }

    /**
    /**
     * create a smallest bounding box that contains all of the given coordinates
     * @param coords given coordinates to create a wrapping bounding box.
     * @return a bounding box that contains all of the coordinates
     */
    public static BoundingBox create(Vector coords) {
        int length = coords.size();
        if (length <= 0) {
            throw new RuntimeException("There must be at least 1 coordinate.");
        }
        Coord[] coordsArray = new Coord[length];
        coords.copyInto(coordsArray);        
        return create(coordsArray);
    }

    /**
     * create a new bounding box that extends this bounding box with the given 
     * bounding box
     * @param other a bounding box that needs to extends the current bounding box
     * @return a new bounding box that was extended from the current and the other
     */
    public BoundingBox extend(BoundingBox other) {
        double north = Math.max(getNorthEast().getLatitude(), other.getNorthEast().getLatitude());
        double east = Math.max(getNorthEast().getLongitude(), other.getNorthEast().getLongitude());
        double south = Math.min(getSouthWest().getLatitude(), other.getSouthWest().getLatitude());
        double west = Math.min(getSouthWest().getLongitude(), other.getSouthWest().getLongitude());
        return new BoundingBox(new Coord(south, west, projected()), new Coord(north, east, projected()));
    }

    /**
     * Indicates if this bounding box is isProjected
     * @return true if it's a isProjected bounding box
     */
    public boolean projected() {
        return _southWest.isProjected();
    }
}
