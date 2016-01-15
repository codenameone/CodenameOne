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
 * Metadata for geofencing support that allows tracking user location in the background while the app
 * is inactive. Usage:
 * 
 * <script src="https://gist.github.com/codenameone/b0fa5280bde905a8f0cd.js"></script>
 * <noscript>Open the javadoc in your browser to see the full sample at https://www.codenameone.com/javadoc/</noscript>
 * Direct link to sample: <a href="https://gist.github.com/codenameone/b0fa5280bde905a8f0cd" target="_blank">https://gist.github.com/codenameone/b0fa5280bde905a8f0cd</a>.
 *
 * @author Chen
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
