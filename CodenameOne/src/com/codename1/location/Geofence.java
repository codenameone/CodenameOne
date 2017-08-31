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

import java.util.Comparator;

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
 * <p>The maximum number of simulataneous Geofences allowed will vary by platform.  iOS currently has a maximum of 20, and Android has a maximum of 100.  If you need to 
 * track more than 20 at a time, consider using the {@link GeofenceManager} class to manage your Geofences, as it will allow you to
 * effectively track an unlimited number of regions.
 * </p>
 *
 * @author Chen
 * @see LocationManager#isGeofenceSupported() 
 * @see LocationManager#addGeoFencing(java.lang.Class, com.codename1.location.Geofence) 
 * @see LocationManager#removeGeoFencing(java.lang.String) 
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
     * @param radius the radius in meters. Note that the actual radius will vary
     * on an actual device depending on the hardware and OS.  Typical android and iOS devices 
     * have a minimum radius of 100m.
     * @param expiration the expiration time in milliseconds.  Note that this is a duration, not a timestamp.  Use {@literal -1} to never expire.
     */ 
    public Geofence(String id, Location loc, int radius, long expiration) {
        this.id = id;
        this.loc = loc;
        this.radius = radius;
        this.expiration = expiration;
    }

    /**
     * Gets the Geofence ID.
     * 
     * @return the id
     */ 
    public String getId() {
        return id;
    }

    /**
     * Gets the location of the Geofence.
     * 
     * @return the center Location
     */ 
    public Location getLoc() {
        return loc;
    }

    /**
     * Gets the expiration duration (from now) of the Geofence in milliseconds.
     * 
     * @return the Geofence expiration
     */ 
    public long getExpiration() {
        return expiration;
    }

    /**
     * Gets the radius of the geofence in metres.  Note that the actual radius will vary
     * on an actual device depending on the hardware and OS.  Typical android and iOS devices 
     * have a minimum radius of 100m.
     * 
     * @return Geofence radius
     */ 
    public int getRadius() {
        return radius;
    }
    
    /**
     * Gets the distance between the current region and the given region.
     * @param gf
     * @return 
     */
    public double getDistanceTo(Geofence gf) {
        return Math.max(0, getLoc().getDistanceTo(gf.getLoc()) - gf.getRadius() - getRadius());
    }
    
    /**
     * Creates a comparator for sorting Geofences from the current Geofence.
     * @return 
     */
    public static Comparator<Geofence> createDistanceComparator(final Geofence refRegion) {
        return new Comparator<Geofence>() {

            public int compare(Geofence o1, Geofence o2) {
                double d1 = refRegion.getDistanceTo(o1);
                double d2 = refRegion.getDistanceTo(o2);
                return d1 < d2 ? -1 : d2 < d1 ? 1 : 0;
            }
            
        };
    }
    
    /**
     * Creates a comparator for sorting Geofences from the given reference point.
     * @return 
     */
    public static Comparator<Geofence> createDistanceComparator(final Location refPoint) {
        return new Comparator<Geofence>() {

            public int compare(Geofence o1, Geofence o2) {
                double d1 = Math.max(0, refPoint.getDistanceTo(o1.getLoc()) - o1.getRadius());
                double d2 = Math.max(0,refPoint.getDistanceTo(o2.getLoc()) - o2.getRadius());
                return d1 < d2 ? -1 : d2 < d1 ? 1 : 0;
            }
            
        };
    }
}
