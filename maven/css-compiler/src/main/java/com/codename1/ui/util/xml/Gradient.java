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
package com.codename1.ui.util.xml;


/**
 * Parsed XML data
 *
 * @author Shai Almog
 */
public class Gradient {
    private String key;
    
    private Integer color1;

    private Integer color2;

    private Float posX;
    
    private Float posY;

    private Float radius;

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the color1
     */
    public Integer getColor1() {
        return color1;
    }

    /**
     * @return the color2
     */
    public Integer getColor2() {
        return color2;
    }

    /**
     * @return the posX
     */
    public Float getPosX() {
        return posX;
    }

    /**
     * @return the posY
     */
    public Float getPosY() {
        return posY;
    }

    /**
     * @return the radius
     */
    public Float getRadius() {
        return radius;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setColor1(Integer color1) {
        this.color1 = color1;
    }

    public void setColor2(Integer color2) {
        this.color2 = color2;
    }

    public void setPosX(Float posX) {
        this.posX = posX;
    }

    public void setPosY(Float posY) {
        this.posY = posY;
    }

    public void setRadius(Float radius) {
        this.radius = radius;
    }
}
