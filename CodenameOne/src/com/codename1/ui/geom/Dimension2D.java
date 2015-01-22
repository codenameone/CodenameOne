/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.geom;

/**
 * Utility class that holds a width and height that represents a dimension of 
 * a component or element
 * 
 * @author Nir Shabi
 */
public class Dimension2D {
    
    private double width;
    
    private double height;

    /**
     * Creates a new instance of Dimension
     */
    public Dimension2D() {
    }

    /**
     * Creates a new instance of Dimension with a predefine dimension
     * 
     * @param d Dimension to copy
     */
    public Dimension2D(Dimension2D d) {
        this.width = d.width;
        this.height = d.height;
    }

    /**
     * CCreates a new instance of Dimension with width and height
     * 
     * @param width the dimention width
     * @param height the dimention height
     */
    public Dimension2D(double width, double height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Set the width of the dimension
     * 
     * @param width the dimention width
     */
    public void setWidth(double width) {
        this.width = width;
    }

    /**
     * Set the height of the dimension
     * 
     * @param height the dimention height
     */
    public void setHeight(double height) {
        this.height = height;
    }

    /**
     * Returns the width of the dimension
     * 
     * @return width of the dimension
     */
    public double getWidth() {
        return width;
    }

   /**
    * Return the height of the dimension
    * 
    * @return height of the dimension
    */
    public double getHeight() {
        return height;
    }

    /**
     * @inheritDoc
     */
    public String toString() {
        return "width = " + width + " height = " +height;
    }

    
    
    
}
