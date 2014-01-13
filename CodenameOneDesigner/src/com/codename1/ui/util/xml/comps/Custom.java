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

package com.codename1.ui.util.xml.comps;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Shai Almog
 */
@XmlRootElement(name="custom")
@XmlAccessorType(XmlAccessType.FIELD)
public class Custom {
    @XmlAttribute
    private String name;

    @XmlAttribute
    private String type;

    @XmlAttribute
    private int dimensions;

    @XmlAttribute
    private String value;
    
    @XmlElement
    private StringEntry[] str;
    
    @XmlElement
    private ArrayEntry[] arr;
    
    @XmlAttribute
    private String selectedRenderer;
    
    @XmlAttribute
    private String unselectedRenderer;

    @XmlAttribute
    private String selectedRendererEven;

    @XmlAttribute
    private String unselectedRendererEven;

    @XmlElement
    private MapItems[] mapItems;

    @XmlElement
    private StringEntry[] stringItem;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the dimensions
     */
    public int getDimensions() {
        return dimensions;
    }

    /**
     * @param dimensions the dimensions to set
     */
    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the str
     */
    public StringEntry[] getStr() {
        return str;
    }

    /**
     * @param str the str to set
     */
    public void setStr(StringEntry[] str) {
        this.str = str;
    }

    /**
     * @return the arr
     */
    public ArrayEntry[] getArr() {
        return arr;
    }

    /**
     * @param arr the arr to set
     */
    public void setArr(ArrayEntry[] arr) {
        this.arr = arr;
    }

    /**
     * @return the selectedRenderer
     */
    public String getSelectedRenderer() {
        return selectedRenderer;
    }

    /**
     * @param selectedRenderer the selectedRenderer to set
     */
    public void setSelectedRenderer(String selectedRenderer) {
        this.selectedRenderer = selectedRenderer;
    }

    /**
     * @return the unselectedRenderer
     */
    public String getUnselectedRenderer() {
        return unselectedRenderer;
    }

    /**
     * @param unselectedRenderer the unselectedRenderer to set
     */
    public void setUnselectedRenderer(String unselectedRenderer) {
        this.unselectedRenderer = unselectedRenderer;
    }

    /**
     * @return the selectedRendererEven
     */
    public String getSelectedRendererEven() {
        return selectedRendererEven;
    }

    /**
     * @param selectedRendererEven the selectedRendererEven to set
     */
    public void setSelectedRendererEven(String selectedRendererEven) {
        this.selectedRendererEven = selectedRendererEven;
    }

    /**
     * @return the unselectedRendererEven
     */
    public String getUnselectedRendererEven() {
        return unselectedRendererEven;
    }

    /**
     * @param unselectedRendererEven the unselectedRendererEven to set
     */
    public void setUnselectedRendererEven(String unselectedRendererEven) {
        this.unselectedRendererEven = unselectedRendererEven;
    }

    /**
     * @return the mapItems
     */
    public MapItems[] getMapItems() {
        return mapItems;
    }

    /**
     * @param mapItems the mapItems to set
     */
    public void setMapItems(MapItems[] mapItems) {
        this.mapItems = mapItems;
    }

    /**
     * @return the stringItem
     */
    public StringEntry[] getStringItem() {
        return stringItem;
    }

    /**
     * @param stringItem the stringItem to set
     */
    public void setStringItem(StringEntry[] stringItem) {
        this.stringItem = stringItem;
    }
}
