/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.ui.scene;

/**
 * Encapsulates a point in 3D space.
 * @author Steve Hannah
 * @deprecated For internal use only
 */
public class Point3D {
    private double x, y, z;
    
    /**
     * Creates a new point.
     * @param x The x-coord
     * @param y The y-coord
     * @param z The z-coord
     */
    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Gets x coordinate.
     * @return the x
     */
    public double getX() {
        return x;
    }

    /**
     * Sets the x coordinate
     * @param x the x to set
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Gets the y-coordinate.
     * @return the y
     */
    public double getY() {
        return y;
    }

    /**
     * Sets the y coordinate.
     * @param y the y to set
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Gets the z-coordinate.
     * @return the z
     */
    public double getZ() {
        return z;
    }

    /**
     * Sets the z-coordinate
     * @param z the z to set
     */
    public void setZ(double z) {
        this.z = z;
    }
    
    
}
