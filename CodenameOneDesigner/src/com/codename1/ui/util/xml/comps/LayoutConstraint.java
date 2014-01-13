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
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Shai Almog
 */
@XmlRootElement(name="layoutConstraint")
@XmlAccessorType(XmlAccessType.FIELD)
public class LayoutConstraint {
    @XmlAttribute
    private String value;

    @XmlAttribute
    private int row;


    @XmlAttribute
    private int column;

    @XmlAttribute
    private int height;

    @XmlAttribute
    private int width;

    @XmlAttribute
    private int align;

    @XmlAttribute
    private int valign;

    @XmlAttribute
    private int spanHorizontal;

    @XmlAttribute
    private int spanVertical;

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
     * @return the row
     */
    public int getRow() {
        return row;
    }

    /**
     * @param row the row to set
     */
    public void setRow(int row) {
        this.row = row;
    }

    /**
     * @return the column
     */
    public int getColumn() {
        return column;
    }

    /**
     * @param column the column to set
     */
    public void setColumn(int column) {
        this.column = column;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * @return the align
     */
    public int getAlign() {
        return align;
    }

    /**
     * @param align the align to set
     */
    public void setAlign(int align) {
        this.align = align;
    }

    /**
     * @return the valign
     */
    public int getValign() {
        return valign;
    }

    /**
     * @param valign the valign to set
     */
    public void setValign(int valign) {
        this.valign = valign;
    }

    /**
     * @return the spanHorizontal
     */
    public int getSpanHorizontal() {
        return spanHorizontal;
    }

    /**
     * @param spanHorizontal the spanHorizontal to set
     */
    public void setSpanHorizontal(int spanHorizontal) {
        this.spanHorizontal = spanHorizontal;
    }

    /**
     * @return the spanVertical
     */
    public int getSpanVertical() {
        return spanVertical;
    }

    /**
     * @param spanVertical the spanVertical to set
     */
    public void setSpanVertical(int spanVertical) {
        this.spanVertical = spanVertical;
    }
}
