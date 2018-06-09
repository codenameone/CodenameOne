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
package com.codename1.ui.scene;

/**
 * Encapsulates bounds in a 3D space.
 * @author Steve Hannah
 * @deprecated For Internal use only
 */
public class Bounds {
    
    /**
     * Min X coordinate
     */
    private double minX;
    
    /**
     * Min Y coordinate
     */
    private double minY;
    
    /**
     * Min Z coordinate
     */
    private double minZ;
    
    /**
     * Width of the bounding cube.  (along x-axis)
     */
    private double width;
    
    /**
     * Height of bounding cube. (along y-axis)
     */
    private double height;
    
    /**
     * Depth of bounding cube (along z-axis)
     */
    private double depth;

    /**
     * 
     * @param minX
     * @param minY
     * @param minZ
     * @param width
     * @param height
     * @param depth 
     */
    public Bounds(double minX, double minY, double minZ, double width, double height, double depth) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.width = width;
        this.height = height;
        this.depth = depth;
    }
    
    /**
     * Gets min X coordinate of bounding cube.
     * @return the minX
     */
    public double getMinX() {
        return minX;
    }

    /**
     * Sets min X coordinate of bounding cube.
     * @param minX the minX to set
     */
    public void setMinX(double minX) {
        this.minX = minX;
    }

    /**
     * Gets the min Y coordinate of bounding cube.
     * @return the minY
     */
    public double getMinY() {
        return minY;
    }

    /**
     * Sets the min Y coordinate of bounding cube.
     * @param minY the minY to set
     */
    public void setMinY(double minY) {
        this.minY = minY;
    }

    /**
     * Sets the min Z coordinate of bounding cube.
     * @return the minZ
     */
    public double getMinZ() {
        return minZ;
    }

    /**
     * Sets the min Z coordinate of the bounding cube.
     * @param minZ the minZ to set
     */
    public void setMinZ(double minZ) {
        this.minZ = minZ;
    }

    /**
     * Gets the width of the bounding cube (along x-axis)
     * @return the width
     */
    public double getWidth() {
        return width;
    }

    /**
     * Sets the width of the bouding cube along x-axis.
     * @param width the width to set
     */
    public void setWidth(double width) {
        this.width = width;
    }

    /**
     * Gets the height of the bounding cube along y-axis).
     * @return the height
     */
    public double getHeight() {
        return height;
    }

    /**
     * Sets the height of the bounding cube along y-axis.
     * @param height the height to set
     */
    public void setHeight(double height) {
        this.height = height;
    }

    /**
     * Gets the depth of the bounding cube along z-axis.
     * @return the depth
     */
    public double getDepth() {
        return depth;
    }

    /**
     * Sets the depth of the bounding cube along z-axis
     * @param depth the depth to set
     */
    public void setDepth(double depth) {
        this.depth = depth;
    }
}
