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
package com.codename1.ui.geom;

/**
 * Utility class that holds x,y 
 * @author Chen Fishbein
 */
public class Point {

    private int x;
    private int y;

    /**
     * Constructor with x and y
     * @param x
     * @param y 
     */
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Gets the x value of this Point
     * @return 
     */
    public int getX() {
        return x;
    }

    /**
     * Gets the y value of this Point
     * @return 
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the x value of this Point
     * @param x 
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Sets the y value of this Point
     * @param y 
     */
    public void setY(int y) {
        this.y = y;
    }
    
    /**
     * {@inheritDoc} 
     */
    public String toString() {
        return x + ", " + y;
    }
}
