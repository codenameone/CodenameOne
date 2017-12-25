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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Parsed XML data
 *
 * @author Shai Almog
 */
@XmlRootElement(name="border")
@XmlAccessorType(XmlAccessType.FIELD)
public class Border {
    @XmlAttribute
    private String key;

    @XmlAttribute
    private String type;

    @XmlAttribute
    private Float thickness;

    @XmlAttribute
    private boolean millimeters;

    @XmlAttribute
    private Integer color;

    @XmlAttribute
    private Integer colorB;

    @XmlAttribute
    private Integer colorC;

    @XmlAttribute
    private Integer colorD;

    @XmlAttribute
    private Integer arcW;

    @XmlAttribute
    private Integer arcH;

    @XmlAttribute
    private String i1;

    @XmlAttribute
    private String i2;

    @XmlAttribute
    private String i3;

    @XmlAttribute
    private String i4;

    @XmlAttribute
    private String i5;

    @XmlAttribute
    private String i6;

    @XmlAttribute
    private String i7;

    @XmlAttribute
    private String i8;

    @XmlAttribute
    private String i9;

    @XmlAttribute
    private int roundBorderColor;

    @XmlAttribute
    private int opacity = 255;
    
    @XmlAttribute
    private int strokeColor;
    
    @XmlAttribute
    private int strokeOpacity = 255;
    
    @XmlAttribute
    private float strokeThickness;

    @XmlAttribute
    private boolean strokeMM;

    @XmlAttribute
    private float shadowSpread;

    @XmlAttribute
    private int shadowOpacity = 0;

    @XmlAttribute
    private float shadowX = 0.5f;

    @XmlAttribute
    private float shadowY = 0.5f;

    @XmlAttribute
    private float shadowBlur = 10;

    @XmlAttribute
    private boolean shadowMM;

    @XmlAttribute
    private boolean rectangle;

    @XmlAttribute
    private float cornerRadius;
    
    @XmlAttribute
    private boolean bezierCorners;
    
    @XmlAttribute
    private boolean topOnlyMode;
    
    @XmlAttribute
    private boolean bottomOnlyMode;
    
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
     * @return the thickness
     */
    public Float getThickness() {
        return thickness;
    }

    /**
     * @return the color
     */
    public Integer getColor() {
        return color;
    }

    /**
     * @return the colorB
     */
    public Integer getColorB() {
        return colorB;
    }

    /**
     * @return the colorC
     */
    public Integer getColorC() {
        return colorC;
    }

    /**
     * @return the colorD
     */
    public Integer getColorD() {
        return colorD;
    }

    /**
     * @return the arcW
     */
    public Integer getArcW() {
        return arcW;
    }

    /**
     * @return the arcH
     */
    public Integer getArcH() {
        return arcH;
    }

    /**
     * @return the i1
     */
    public String getI1() {
        return i1;
    }

    /**
     * @return the i2
     */
    public String getI2() {
        return i2;
    }

    /**
     * @return the i3
     */
    public String getI3() {
        return i3;
    }

    /**
     * @return the i4
     */
    public String getI4() {
        return i4;
    }

    /**
     * @return the i5
     */
    public String getI5() {
        return i5;
    }

    /**
     * @return the i6
     */
    public String getI6() {
        return i6;
    }

    /**
     * @return the i7
     */
    public String getI7() {
        return i7;
    }

    /**
     * @return the i8
     */
    public String getI8() {
        return i8;
    }

    /**
     * @return the i9
     */
    public String getI9() {
        return i9;
    }

    /**
     * @return the opacity
     */
    public int getOpacity() {
        return opacity;
    }

    /**
     * @return the strokeColor
     */
    public int getStrokeColor() {
        return strokeColor;
    }

    /**
     * @return the strokeOpacity
     */
    public int getStrokeOpacity() {
        return strokeOpacity;
    }

    /**
     * @return the strokeThickness
     */
    public float getStrokeThickness() {
        return strokeThickness;
    }

    /**
     * @return the strokeMM
     */
    public boolean isStrokeMM() {
        return strokeMM;
    }

    /**
     * @return the shadowSpread
     */
    public float getShadowSpread() {
        return shadowSpread;
    }

    /**
     * @return the shadowOpacity
     */
    public int getShadowOpacity() {
        return shadowOpacity;
    }

    /**
     * @return the shadowX
     */
    public float getShadowX() {
        return shadowX;
    }

    /**
     * @return the shadowY
     */
    public float getShadowY() {
        return shadowY;
    }

    /**
     * @return the shadowBlur
     */
    public float getShadowBlur() {
        return shadowBlur;
    }

    /**
     * @return the shadowMM
     */
    public boolean isShadowMM() {
        return shadowMM;
    }

    /**
     * @return the rectangle
     */
    public boolean isRectangle() {
        return rectangle;
    }

    /**
     * @return the roundBorderColor
     */
    public int getRoundBorderColor() {
        return roundBorderColor;
    }

    /**
     * @param roundBorderColor the roundBorderColor to set
     */
    public void setRoundBorderColor(int roundBorderColor) {
        this.roundBorderColor = roundBorderColor;
    }

    /**
     * @return the cornerRadius
     */
    public float getCornerRadius() {
        return cornerRadius;
    }

    /**
     * @return the bezierCorners
     */
    public boolean isBezierCorners() {
        return bezierCorners;
    }

    /**
     * @return the topOnlyMode
     */
    public boolean isTopOnlyMode() {
        return topOnlyMode;
    }

    /**
     * @return the bottomOnlyMode
     */
    public boolean isBottomOnlyMode() {
        return bottomOnlyMode;
    }

    /**
     * @return the millimeters
     */
    public boolean isMillimeters() {
        return millimeters;
    }
}
