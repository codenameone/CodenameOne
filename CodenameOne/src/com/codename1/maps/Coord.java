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

import com.codename1.ui.geom.Dimension;

/**
 * This class declares a coordinate point on a map.
 * 
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public class Coord {

    private double longitude;
    private double latitude;
    // Projected to local coordinate system. False means WGS84
    private boolean projected;
    private static double DELTA = 0.0000001;

    /**
     * Creates a isProjected Coord
     * @param getLatitude the getLatitude of this Coordinate
     * @param getLongitude the getLongitude of this Coordinate
     */
    public Coord(double latitude, double longitude) {
        this(latitude, longitude, false);
    }

    
    /**
     * a Constructor with getLatitude, getLongitude
     * @param getLatitude the Coordinate getLatitude
     * @param getLongitude the Coordinate getLongitude
     * @param isProjected declares if this is a isProjected Coordinate
     */
    public Coord(double latitude, double longitude, boolean projected) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.projected = projected;
    }

    /**
     * Copy Constructor
     * @param toClone to copy
     */
    public Coord(Coord toClone) {
        longitude = toClone.getLongitude();
        latitude = toClone.getLatitude();
        projected = toClone.isProjected();
    }

    /**
     * Gets the Coord Longitude.
     * @return the Coord Longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Gets the Coord Latitude
     * @return the Coord Latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Sets the Coord Longitude.
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Sets the Coord Latitude.
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    
    /**
     * @inheritDoc
     */
    public String toString() {
        return new StringBuilder().append("{'longitude':").append(getLongitude()).append(", 'latitude':").
                append(getLatitude()).append("}").toString();
    }

    /**
     * Create a new Coord object which is translated with the given coordinates
     * @param latitude translate current latitude with this latitude
     * @param longitude translate current longitude with this longitude
     * @return a new translated Coord object
     */
    public Coord translate(double latitude, double longitude) {
        return new Coord(latitude + getLatitude(), longitude + getLongitude(), isProjected());
    }

    /**
     * Create a new Coord object which is translated with the given coordinates
     * @param coordinates translate current Coord with the given coordinates
     * @return a new translated Coord object
     */
    public Coord translate(Coord coordinates) {
        return translate(coordinates.getLatitude(), coordinates.getLongitude());
    }

    /**
     * @inheritDoc
     */
    public boolean equals(Object other) {
        if (!(other instanceof Coord)) {
            return false;
        }
        Coord o = (Coord) other;
        return (Math.abs(longitude - o.longitude) < DELTA) && (Math.abs(latitude - o.latitude) < DELTA);
    }

    /**
     * @inheritDoc
     */
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + (int) (Double.doubleToLongBits(this.longitude) ^ (Double.doubleToLongBits(this.longitude) >>> 32));
        hash = 17 * hash + (int) (Double.doubleToLongBits(this.latitude) ^ (Double.doubleToLongBits(this.latitude) >>> 32));
        return hash;
    }

    /**
     * Returns true if this is a projected Coordinate
     * @return true if projected
     */
    public final boolean isProjected() {
        return projected;
    }

    /**
     * Sets Coord projected
     * @param projected flag
     */
    public void setProjected(boolean projected) {
        this.projected = projected;
    }

}
