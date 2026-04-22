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
public class Font {
    private String key;

    private String type;
    
    private String name;

    private Integer face;

    private Integer style;

    private Integer size;

    private String family;

    private Integer sizeSettings;

    private Float actualSize;

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the face
     */
    public Integer getFace() {
        return face;
    }

    /**
     * @return the style
     */
    public Integer getStyle() {
        return style;
    }

    /**
     * @return the size
     */
    public Integer getSize() {
        return size;
    }

    /**
     * @return the family
     */
    public String getFamily() {
        return family;
    }

    /**
     * @return the sizeSettings
     */
    public Integer getSizeSettings() {
        return sizeSettings;
    }

    /**
     * @return the actualSize
     */
    public Float getActualSize() {
        return actualSize;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFace(Integer face) {
        this.face = face;
    }

    public void setStyle(Integer style) {
        this.style = style;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public void setSizeSettings(Integer sizeSettings) {
        this.sizeSettings = sizeSettings;
    }

    public void setActualSize(Float actualSize) {
        this.actualSize = actualSize;
    }
}
