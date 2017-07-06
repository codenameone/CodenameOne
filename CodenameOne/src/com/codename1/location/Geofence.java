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
 * <p>Metadata for geofencing support that allows tracking user location in the background while the app
 * is inactive.<br>
 * The sample below tracks location and posts a notification or shows a dialog based on the state of the app:</p>
 * 
 * <script src="https://gist.github.com/codenameone/3de90e0ff4886ec145e8.js"></script>
 * 
 * <p><strong>NOTE:</strong> For iOS you must include the <code>ios.background_modes</code> build hint with a value that includes "location" for geofencing to work.</p>
 * 
 * <p>Geofencing is not supported on all platforms, use {@link LocationManager#isGeofenceSupported() } to find out if the current
 * platform supports it at runtime.</p>
 *
 * @author Chen
 * @see LocationManager#isGeofenceSupported() 
 * @see LocationManager#addGeoFencing(java.lang.Class, com.codename1.location.Geofence) 
 * @see GeofenceListener
 */
public class Geofence {
    
    private String id;
    
    private Location loc;
    
    private int radius;
    
    private long expiration;

    /**
     * Constructor
     * 
     * @param id unique identifier
     * @param loc the center location of this Geofence
     * @param radius the radius in meters
     * @param expiration the expiration time in milliseconds
     */ 
    public Geofence(String id, Location loc, int radius, long expiration) {
        this.id = id;
        this.loc = loc;
        this.radius = radius;
        this.expiration = expiration;
    }

    /**
     * Simple Getter
     * 
     * @return the id
     */ 
    public String getId() {
        return id;
    }

    /**
     * Simple Getter
     * 
     * @return the center Location
     */ 
    public Location getLoc() {
        return loc;
    }

    /**
     * Simple Getter
     * 
     * @return the Geofence expiration
     */ 
    public long getExpiration() {
        return expiration;
    }

    /**
     * Simple Getter
     * 
     * @return Geofence radius
     */ 
    public int getRadius() {
        return radius;
    }
    
    
}
