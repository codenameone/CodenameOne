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
package com.codename1.ui;

/**
 * A base class for Paints that use multiple gradients.
 * @author shannah
 * @since 7.0
 */
public abstract class MultipleGradientPaint implements Paint {

    
    /**
     * Creates a new MultipleGradient paint
     * @param fractions The fractions representing positions where the corresponding color starts.  Values between 0 and 1.
     * @param colors The colors that are part of the gradient.  Should have same number of colors as fractions.
     * @param cycleMethod The cycle method for the gradient.
     * @param colorSpace The color space for the gradient.
     * @param gradientTransform Transform for the gradient.  Not used.
     */
    protected MultipleGradientPaint(float[] fractions, int[] colors, MultipleGradientPaint.CycleMethod cycleMethod, MultipleGradientPaint.ColorSpaceType colorSpace, Transform gradientTransform) {
        this.fractions = fractions;
        this.colors = colors;
        this.cycleMethod = cycleMethod;
        this.colorSpace = colorSpace;
        this.transform = gradientTransform;
        
        
    }
    
    /**
     * Gets the color space for the gradient.
     * @return the colorSpaceType
     */
    public ColorSpaceType getColorSpace() {
        return colorSpace;
    }

    /**
     * Sets the color space for the gradient.
     * @param colorSpaceType the colorSpaceType to set
     */
    public void setColorSpace(ColorSpaceType colorSpaceType) {
        this.colorSpace = colorSpaceType;
    }

    /**
     * Gets the colors used in the gradient.
     * @return the colors
     */
    public int[] getColors() {
        return colors;
    }

    /**
     * Sets the colors used in the gradient.
     * @param colors the colors to set
     */
    public void setColors(int[] colors) {
        this.colors = colors;
    }

    /**
     * Gets the cycle method.
     * @return the cycleMethod
     */
    public CycleMethod getCycleMethod() {
        return cycleMethod;
    }

    /**
     * Sets the cycle method.
     * @param cycleMethod the cycleMethod to set
     */
    public void setCycleMethod(CycleMethod cycleMethod) {
        this.cycleMethod = cycleMethod;
    }

    /**
     * Gets the fractional positions for the color gradients.
     * @return the fractions
     */
    public float[] getFractions() {
        return fractions;
    }

    /**
     * Sets the fractional positions of the color gradients.
     * @param fractions the fractions to set
     */
    public void setFractions(float[] fractions) {
        this.fractions = fractions;
    }

    /**
     * Gets the gradient transform.  Not used currently.
     * @return the transform
     */
    public Transform getTransform() {
        return transform;
    }

    /**
     * Sets the transform for the gradient.  NOt used currently.
     * @param transform the transform to set
     */
    public void setTransform(Transform transform) {
        this.transform = transform;
    }

    /**
     * Gets the transparency for the gradient.
     * @return the transparency
     */
    public int getTransparency() {
        return transparency;
    }

    /**
     * Sets the transparency for the gradient.
     * @param transparency the transparency to set
     */
    public void setTransparency(int transparency) {
        this.transparency = transparency;
    }
    
    /**
     * Cycle methods for gradients.
     */
    public static enum CycleMethod {
        /**
         * The gradient should not cycle at all.  
         */
        NO_CYCLE,
        
        /**
         * The gradient should cycle with reflection.
         */
        REFLECT,
        
        /**
         * The gradient should repeat to fill the space.
         */
        REPEAT
    }
    
    /**
     * Colors spaces for gradients.
     */
    public static enum ColorSpaceType {
        LINEAR_RGB,
        SRGB
    }
    
    private int[] colors;
    private CycleMethod cycleMethod;
    private ColorSpaceType colorSpace;
    private float[] fractions;
    private Transform transform;
    private int transparency=255;
    
    
    
}
