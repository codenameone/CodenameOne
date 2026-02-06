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

/// Encapsulates the stroke used for drawing paths.
///
/// @author Steve Hannah
///
/// #### See also
///
/// - Graphics#setStroke
///
/// - Graphics#getStroke
public class Stroke {

    // Constants for the type of join to use for the stroke

    /// Join style constant to join strokes MITER (i.e. pointy)
    /// Examples can be seen at [here](http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/LineJoinStyleJOINBEVELJOINMITERJOINROUND.htm).
    ///
    /// #### See also
    ///
    /// - #setJoinStyle
    ///
    /// - #getJoinStyle
    public static final int JOIN_MITER = 0;

    /// Join style constant to join strokes rounded.
    /// Examples can be seen [here](http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/LineJoinStyleJOINBEVELJOINMITERJOINROUND.htm).
    ///
    /// #### See also
    ///
    /// - #setJoinStyle
    ///
    /// - #getJoinStyle
    public static final int JOIN_ROUND = 1;

    /// Join style constant to join strokes bevel.
    /// Examples can be seen [here](http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/LineJoinStyleJOINBEVELJOINMITERJOINROUND.htm).
    ///
    /// #### See also
    ///
    /// - #setJoinStyle
    ///
    /// - #getJoinStyle
    public static final int JOIN_BEVEL = 2;

    /// Cap style constant to cap strokes with a butt (or flat).
    /// Examples can be seen [here](http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/SettingLinecaps.htm).
    ///
    /// #### See also
    ///
    /// - #setCapStyle
    ///
    /// - #getCapStyle
    public static final int CAP_BUTT = 0;

    /// Cap style constant to cap strokes with a round end.
    /// Examples can be seen [here](http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/SettingLinecaps.htm)
    ///
    /// #### See also
    ///
    /// - #setCapStyle
    ///
    /// - #getCapStyle
    public static final int CAP_ROUND = 1;

    /// Cap style constant to cap strokes with a square end.
    /// Examples can be seen [here](http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/SettingLinecaps.htm)
    ///
    /// #### See also
    ///
    /// - #setCapStyle
    ///
    /// - #getCapStyle
    public static final int CAP_SQUARE = 2;


    private int joinStyle = 0;
    private int capStyle = 0;
    private float lineWidth = 1f;
    private float miterLimit = 4f;

    /// Creates a stroke with the specified characteristics.
    ///
    /// #### Parameters
    ///
    /// - `lineWidth`: The width of the stroke pixels.
    ///
    /// - `capStyle`: The cap style of the stroke.  Should be one of `#CAP_BUTT`, `#CAP_ROUND`, or `#CAP_SQUARE`.
    ///
    /// - `joinStyle`: The join style of the strokes.  Should be one of `#JOIN_MITER`, `#JOIN_ROUND`, or `#JOIN_BEVEL`.
    ///
    /// - `miterLimit`: The Miter limit controls the point at which a Miter join automatically is converted to a Bevel join. If the distance from the inner intersection point to the tip of the triangle measured in stroke widths is more than the Miter limit, the join will be drawn in the Bevel style.
    public Stroke(float lineWidth, int capStyle, int joinStyle, float miterLimit) {
        this.lineWidth = lineWidth;
        this.capStyle = capStyle;
        this.joinStyle = joinStyle;
        this.miterLimit = miterLimit;
    }

    /// Creates a stroke with default settings.  Default settings are:
    ///
    ///  Join style`#JOIN_MITER`
    ///  Cap style`#CAP_BUTT`
    ///  Line Width1.0
    ///  Miter Limit4.0
    public Stroke() {

    }

    /// Copies the properties of `stroke` into this stroke.
    ///
    /// #### Parameters
    ///
    /// - `stroke`: The stroke whose properties we wish to copy into the current stroke.
    public void setStroke(Stroke stroke) {
        this.lineWidth = stroke.lineWidth;
        this.capStyle = stroke.capStyle;
        this.joinStyle = stroke.joinStyle;
        this.miterLimit = stroke.miterLimit;
    }

    /// Returns the join style used for this stroke.
    /// See visual examples of join styles [here](http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/LineJoinStyleJOINBEVELJOINMITERJOINROUND.htm).
    ///
    /// #### Returns
    ///
    /// @return the joinStyle This will be one of `#JOIN_MITER`,
    /// `#JOIN_ROUND`, and `#JOIN_BEVEL`.
    ///
    /// #### See also
    ///
    /// - #JOIN_MITER
    ///
    /// - #JOIN_BEVEL
    ///
    /// - #JOIN_ROUND
    public int getJoinStyle() {
        return joinStyle;
    }

    /// Sets the join style of the stroke.
    /// See visual examples of join styles [here](http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/LineJoinStyleJOINBEVELJOINMITERJOINROUND.htm).
    ///
    /// #### Parameters
    ///
    /// - `joinStyle`: @param joinStyle the joinStyle to set.  This should be one of `#JOIN_MITER`,
    ///                  `#JOIN_ROUND`, and `#JOIN_BEVEL`.
    ///
    /// #### See also
    ///
    /// - #JOIN_MITER
    ///
    /// - #JOIN_BEVEL
    ///
    /// - #JOIN_ROUND
    public void setJoinStyle(int joinStyle) {
        this.joinStyle = joinStyle;
    }

    /// Gets the cap style of the stroke.
    /// See visual examples of cap styles [here](http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/SettingLinecaps.htm).
    ///
    /// #### Returns
    ///
    /// the capStyle.  This will be one of `#CAP_BUTT`, `#CAP_ROUND`, and `#CAP_SQUARE`.
    ///
    /// #### See also
    ///
    /// - #CAP_BUTT
    ///
    /// - #CAP_SQUARE
    ///
    /// - #CAP_ROUND
    public int getCapStyle() {
        return capStyle;
    }

    /// Gets the cap style of the stroke.
    /// See visual examples of cap styles [here](http://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/SettingLinecaps.htm).
    ///
    /// #### Parameters
    ///
    /// - `capStyle`: the capStyle to set. This will be one of `#CAP_BUTT`, `#CAP_ROUND`, and `#CAP_SQUARE`.
    ///
    /// #### See also
    ///
    /// - #CAP_BUTT
    ///
    /// - #CAP_SQUARE
    ///
    /// - #CAP_ROUND
    public void setCapStyle(int capStyle) {
        this.capStyle = capStyle;
    }

    /// Returns the line width of the stroke.
    ///
    /// #### Returns
    ///
    /// the lineWidth
    public float getLineWidth() {
        return lineWidth;
    }

    /// Sets the line width of the stroke.
    ///
    /// #### Parameters
    ///
    /// - `lineWidth`: the lineWidth to set
    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    /// Gets the miter limit of the stroke. The Miter limit controls the point at which a Miter join automatically is converted to a Bevel join. If the distance from the inner intersection point to the tip of the triangle measured in stroke widths is more than the Miter limit, the join will be drawn in the Bevel style.
    ///
    /// #### Returns
    ///
    /// the miterLimit
    public float getMiterLimit() {
        return miterLimit;
    }

    /// Sets the miter limit of the stroke. The Miter limit controls the point at which a Miter join automatically is converted to a Bevel join. If the distance from the inner intersection point to the tip of the triangle measured in stroke widths is more than the Miter limit, the join will be drawn in the Bevel style.
    ///
    /// #### Parameters
    ///
    /// - `miterLimit`: the miterLimit to set
    public void setMiterLimit(float miterLimit) {
        this.miterLimit = miterLimit;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Stroke) {
            Stroke s = (Stroke) obj;
            return (com.codename1.util.MathUtil.compare(s.miterLimit, miterLimit) == 0 && s.capStyle == capStyle && s.joinStyle == joinStyle && com.codename1.util.MathUtil.compare(s.lineWidth, lineWidth) == 0);
        }
        return false;
    }

    @Override
    public String toString() {
        return "Stroke{lineWidth=" + lineWidth + ";capStyle=" + capStyle + ";joinStyle=" + joinStyle + ";miterLimit:" + miterLimit + "}";
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
