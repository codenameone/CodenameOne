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
 * This class is used when requesting to listen to location update.
 * See {@link com.codename1.location.LocationManager#setLocationListener(com.codename1.location.LocationListener l, com.codename1.location.LocationRequest req) setLocationListener}
 *
 * @author Chen
 */
public class LocationRequest {
    
    /**
     * When you need gps location updates
     */
    public static int PRIORITY_HIGH_ACCUARCY = 0;

    /**
     * When accuracy is not highly important and you want to save battery
     */
    public static int PRIORITY_MEDIUM_ACCUARCY = 1;

    /**
     * When accuracy is not important and you want to save battery
     */
    public static int PRIORITY_LOW_ACCUARCY = 2;
    
    private int priority = PRIORITY_MEDIUM_ACCUARCY;
    
    private long interval = 5000;

    /**
     * Empty Constructor
     */ 
    public LocationRequest() {
    }
    
    /**
     * Simple Constructor
     * 
     * @param priority The priority we are interested to listen for location updates.
     * PRIORITY_HIGH_ACCUARCY, PRIORITY_MEDIUM_ACCUARCY, PRIORITY_LOW_ACCUARCY
     * High priority means gps locations which is CPU intensive and consumes more battery.
     * Medium priority is less intensive in terms of battery and might return a 
     * gps or a network location which is less accurate.
     * Low priority won't consume the battery and will return a gps location if 
     * available otherwise the location would be a network location.
     * 
     * @param interval time in milliseconds which determines what are the time 
     * intervals that we would like to get updates from the OS.
     * This is a request only and might not be respected by the underlying OS
     */ 
    public LocationRequest(int priority, long interval) {
        this.priority = priority;
        this.interval = interval;
    }

    /**
     * Gets the request priority
     */ 
    public int getPriority() {
        return priority;
    }

    /**
     * Gest the request time interval
     */ 
    public long getInterval() {
        return interval;
    }
    
    
    
}
