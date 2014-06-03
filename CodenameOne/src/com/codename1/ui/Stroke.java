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
 * Encapsulates the stroke used for drawing paths.
 * @author Steve Hannah
 * @see Graphics#setStroke
 * @see Graphics#getStroke
 */
public class Stroke {
    
    // Constants for the type of join to use for the stroke
    
    /**
     * Join style constant to join strokes MITER (i.e. pointy)
     * Examples can be seen at <a target="_blank" href="http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/LineJoinStyleJOINBEVELJOINMITERJOINROUND.htm">here</a>.
     * @see #setJoinStyle
     * @see #getJoinStyle
     */
    public static final int JOIN_MITER = 0;
    
    /**
     * Join style constant to join strokes rounded. 
     * Examples can be seen <a target="_blank" href="http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/LineJoinStyleJOINBEVELJOINMITERJOINROUND.htm">here</a>.
     * @see #setJoinStyle
     * @see #getJoinStyle
     */
    public static final int JOIN_ROUND = 1;
    
    /**
     * Join style constant to join strokes bevel.
     * Examples can be seen <a target="_blank" href="http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/LineJoinStyleJOINBEVELJOINMITERJOINROUND.htm">here</a>.
     * @see #setJoinStyle
     * @see #getJoinStyle
     * 
     */
    public static final int JOIN_BEVEL = 2;
    
    /**
     * Cap style constant to cap strokes with a butt (or flat).
     * Examples can be seen <a target="_blank" href="http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/SettingLinecaps.htm">here</a>.
     * @see #setCapStyle
     * @see #getCapStyle
     */
    public static final int CAP_BUTT = 0;
    
    /**
     * Cap style constant to cap strokes with a round end.
     * Examples can be seen <a target="_blank" href="http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/SettingLinecaps.htm">here</a>
     * @see #setCapStyle
     * @see #getCapStyle
     */
    public static final int CAP_ROUND = 1;
    
    /**
     * Cap style constant to cap strokes with a square end.
     * Examples can be seen <a target="_blank" href="http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/SettingLinecaps.htm">here</a>
     * @see #setCapStyle
     * @see #getCapStyle
     */
    public static final int CAP_SQUARE = 2;
    
    
    
    private int joinStyle=0;
    private int capStyle=0;
    private float lineWidth=1f;
    private float miterLimit=4f;
    
    /**
     * Creates a stroke with the specified characteristics.
     * @param lineWidth The width of the stroke pixels.
     * @param capStyle The cap style of the stroke.  Should be one of {@link #CAP_BUTT}, {@link #CAP_ROUND}, or {@link #CAP_SQUARE}.
     * @param joinStyle The join style of the strokes.  Should be one of {@link #JOIN_MITER}, {@link #JOIN_ROUND}, or {@link #JOIN_BEVEL}.
     * @param miterLimit The Miter limit controls the point at which a Miter join automatically is converted to a Bevel join. If the distance from the inner intersection point to the tip of the triangle measured in stroke widths is more than the Miter limit, the join will be drawn in the Bevel style.
     */
    public Stroke(float lineWidth, int capStyle, int joinStyle, float miterLimit){
        this.lineWidth = lineWidth;
        this.capStyle = capStyle;
        this.joinStyle = joinStyle;
        this.miterLimit = miterLimit;
    }
    
    /**
     * Creates a stroke with default settings.  Default settings are:
     * <table>
     *  <tr><td>Join style</td><td>{@link #JOIN_MITER}</td></tr>
     *  <tr><td>Cap style</td><td>{@link #CAP_BUTT}</td></tr>
     *  <tr><td>Line Width</td><td>1.0</td></tr>
     *  <tr><td>Miter Limit</td><td>4.0</td></tr>
     * </table>
     * 
     */
    public Stroke(){
        
    }

    /**
     * Returns the join style used for this stroke.  
     * See visual examples of join styles <a target="_blank" href="http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/LineJoinStyleJOINBEVELJOINMITERJOINROUND.htm">here</a>.
     * @return the joinStyle This will be one of {@link #JOIN_MITER},
     * {@link #JOIN_ROUND}, and {@link #JOIN_BEVEL}.
     * 
     * @see #JOIN_MITER
     * @see #JOIN_BEVEL
     * @see #JOIN_ROUND
     */
    public int getJoinStyle() {
        return joinStyle;
    }

    /**
     * Sets the join style of the stroke.
     * See visual examples of join styles <a target="_blank" href="http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/LineJoinStyleJOINBEVELJOINMITERJOINROUND.htm">here</a>.
     * @param joinStyle the joinStyle to set.  This should be one of {@link #JOIN_MITER},
     * {@link #JOIN_ROUND}, and {@link #JOIN_BEVEL}.
     * 
     * @see #JOIN_MITER
     * @see #JOIN_BEVEL
     * @see #JOIN_ROUND
     */
    public void setJoinStyle(int joinStyle) {
        this.joinStyle = joinStyle;
    }

    /**
     * Gets the cap style of the stroke.
     * See visual examples of cap styles <a target="_blank" href="http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/SettingLinecaps.htm">here</a>.
     * @return the capStyle.  This will be one of {@link #CAP_BUTT}, {@link #CAP_ROUND}, and {@link #CAP_SQUARE}.
     * @see #CAP_BUTT
     * @see #CAP_SQUARE
     * @see #CAP_ROUND
     */
    public int getCapStyle() {
        return capStyle;
    }

    /**
     * Gets the cap style of the stroke.
     * See visual examples of cap styles <a target="_blank" href="http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/SettingLinecaps.htm">here</a>.
     * @param capStyle the capStyle to set. This will be one of {@link #CAP_BUTT}, {@link #CAP_ROUND}, and {@link #CAP_SQUARE}.
     * @see #CAP_BUTT
     * @see #CAP_SQUARE
     * @see #CAP_ROUND
     */
    public void setCapStyle(int capStyle) {
        this.capStyle = capStyle;
    }

    /**
     * Returns the line width of the stroke.
     * @return the lineWidth
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * Sets the line width of the stroke.
     * @param lineWidth the lineWidth to set
     */
    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * Gets the miter limit of the stroke. The Miter limit controls the point at which a Miter join automatically is converted to a Bevel join. If the distance from the inner intersection point to the tip of the triangle measured in stroke widths is more than the Miter limit, the join will be drawn in the Bevel style.
     * @return the miterLimit
     */
    public float getMiterLimit() {
        return miterLimit;
    }

    /**
     * Sets the miter limit of the stroke. The Miter limit controls the point at which a Miter join automatically is converted to a Bevel join. If the distance from the inner intersection point to the tip of the triangle measured in stroke widths is more than the Miter limit, the join will be drawn in the Bevel style.
     * @param miterLimit the miterLimit to set
     */
    public void setMiterLimit(float miterLimit) {
        this.miterLimit = miterLimit;
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj instanceof Stroke ){
            Stroke s = (Stroke)obj;
            return (s.miterLimit==miterLimit && s.capStyle==capStyle && s.joinStyle==joinStyle && s.lineWidth==lineWidth);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + this.joinStyle;
        hash = 59 * hash + this.capStyle;
        hash = 59 * hash + Float.floatToIntBits(this.lineWidth);
        hash = 59 * hash + Float.floatToIntBits(this.miterLimit);
        return hash;
    }
    
    
    
    
}
