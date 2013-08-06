/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.location;

/**
 * This class represents a Location Object
 */
public class Location {
    
    private int status;
    
    private double latitude;
    private double longitude;
    private double altitude;
    private float accuracy;
    private float direction;
    private float velocity;
    private long timeStamp;

    public Location() {
    }

    /**
     * Returns the horizontal accuracy of the location in meters
     * @return the accuracy if exists or 0.0 if not.
     */
    public float getAccuracy() {
        return accuracy;
    }

    /**
     * Returns the altitude of this Location
     * @return altitude
     */
    public double getAltitude() {
        return altitude;
    }

    /**
     * Returns the direction of this Location in degress 0-360
     * @return direction in degrees
     */
    public float getDirection() {
        return direction;
    }

    /**
     * Returns the latitude of this Location
     * @return latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Returns the longitude of this Location
     * @return longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Returns the longitude of this Location
     * @return longitude
     * @deprecated use getLongitude
     */
    public double getLongtitude() {
        return longitude;
    }

    /**
     * The status of the location one of: LocationManager.AVAILABLE, 
     * LocationManager.OUT_OF_SERVICE or LocationManager.TEMPORARILY_UNAVAILABLE:
     */
    public int getStatus() {
        return status;
    }

    /**
     * Returns the timestamp of this Location
     * @return timestamp
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * Returns the velocity of this Location in meters per second (m/s)
     * @return velocity
     */
    public float getVelocity() {
        return velocity;
    }

    public void setAccuracy(float accuracy) {
        if(accuracy != Float.NaN){
            this.accuracy = accuracy;
        }
    }

    /**
     * Sets the altitude of this Location
     * @param altitude 
     */
    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    /**
     * Sets the direction of this Location
     * @param direction 
     */
    public void setDirection(float direction) {
        this.direction = direction;
    }

    /**
     * Sets the latitude of this Location
     * @param latitude 
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Sets the longitude of this Location
     * @param longitude 
     */
    public void setLongitude(double longtitude) {
        this.longitude = longtitude;
    }

    /**
     * Sets the longitude of this Location
     * @param longitude 
     * @deprecated use setLongitude
     */
    public void setLongtitude(double longtitude) {
        this.longitude = longtitude;
    }

    /**
     * The status of the location one of: LocationProvider.AVAILABLE, 
     * LocationProvider.OUT_OF_SERVICE or LocationProvider.TEMPORARILY_UNAVAILABLE:
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Sets the timeStamp of this Location
     * @param timeStamp 
     */
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * Sets the velocity of this Location
     * @param velocity 
     */
    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }

    /**
     * @inheritDoc
     */
    public String toString() {
        return "altitude = " + altitude
                + "\nlatitude" + latitude
                + "\nlongtitude" + longitude
                + "\ndirection" + direction
                + "\ntimeStamp" + timeStamp
                + "\nvelocity" + velocity;
                
    }
    
    
}
