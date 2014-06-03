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
 */package com.codename1.ui.geom;

/**
 * This interface provides a directed path over the boundary of a shape. The path can contain 1st through 3rd order Bezier curves (lines, and quadratic and cubic splines). A shape can have multiple disjoint paths via the {@link #SEG_MOVETO} directive, and can close a circular path back to the previous {@link #SEG_MOVETO} via the {@link #SEG_CLOSE} directive.
 * 
 * <!-- Note:  Some Documentation for this class has been copied from http://developer.classpath.org/doc/java/awt/geom/PathIterator.html.  
 *  See License: http://www.gnu.org/licenses/licenses.html#FDL
 * -->
 * @author shannah
 * @see GeneralPath
 * @see Shape
 * @see com.codename1.ui.Graphics#drawShape
 * @see com.codename1.ui.Graphics#fillShape
 */
public interface PathIterator {

    /**
     * The even-odd winding mode: a point is internal to the shape if a ray from the point to infinity (in any direction) crosses an odd number of segments.
     */
    public static final int WIND_EVEN_ODD = 0;
    
    /**
     * The non-zero winding mode: a point is internal to the shape if a ray from the point to infinity (in any direction) crosses a different number of segments headed clockwise than those headed counterclockwise.
     */
    public static final int WIND_NON_ZERO = 1;

    
    /**
     * Starts a new subpath. There is no segment from the previous vertex.
     */
    public static final int SEG_MOVETO  = 0;
    
    /**
     * The current segment is a line.
     */
    public static final int SEG_LINETO  = 1;
    
    /**
     * The current segment is a quadratic parametric curve. It is interpolated
     * as t varies from 0 to 1 over the current point (CP), first control point
     * (P1), and final interpolated control point (P2):
     * <pre>{@code
     * P(t) = B(2,0)*CP + B(2,1)*P1 + B(2,2)*P2
     * 0 <= t <= 1
     * B(n,m) = mth coefficient of nth degree Bernstein polynomial
     * = C(n,m) * t^(m) * (1 - t)^(n-m)
     * C(n,m) = Combinations of n things, taken m at a time
     * = n! / (m! * (n-m)!) }</pre>
     */
    public static final int SEG_QUADTO  = 2;
    
    /**
     * The current segment is a cubic parametric curve (more commonly known as a
     * Bezier curve). It is interpolated as t varies from 0 to 1 over the
     * current point (CP), first control point (P1), the second control point
     * (P2), and final interpolated control point (P3):
     * <pre>{@code P(t) = B(3,0)*CP + B(3,1)*P1 + B(3,2)*P2 + B(3,3)*P3
     * 0 <= t <= 1
     * B(n,m) = mth coefficient of nth degree Bernstein polynomial
     * = C(n,m) * t^(m) * (1 - t)^(n-m)
     * C(n,m) = Combinations of n things, taken m at a time
     * = n! / (m! * (n-m)!)}</pre>
     */
    public static final int SEG_CUBICTO = 3;
    /**
     * The current segment closes a loop by an implicit line to the previous {@link #SEG_MOVETO} coordinate.
     */
    public static final int SEG_CLOSE   = 4;

    /**
     * Returns the winding rule to determine which points are inside this path.
     * @return the winding rule.  ({@link #WIND_EVEN_ODD} or {@link #WIND_NON_ZERO}).
     */
    public int getWindingRule();

    /**
     * Tests if the iterator is exhausted. If this returns true, currentSegment and next may throw a RuntimeException (although this is not required).
     * @return {@literal true} if the iteration is complete
     */
    public boolean isDone();

    /**
     * Advance to the next segment in the iteration. It is not specified what this does if called when {@link #isDone} returns {@literal true}.
     */
    public void next();

    /**
     * Returns the coordinates of the next point(s), as well as the type of line segment. 
     * The input array must be at least a {@code float[6]}, to accommodate up to three 
     * {@code (x,y)} point pairs (although if you know the iterator is flat, you can probably 
     * get by with a {@code float[2]}). If the returned type is {@link #SEG_MOVETO} or {@link #SEG_LINETO}, 
     * the first point in the array is modified; if the returned type is {@link #SEG_QUADTO}, 
     * the first two points are modified; if the returned type is {@link #SEG_CUBICTO}, all three points are
     * modified; and if the returned type is {@link #SEG_CLOSE}, the array is untouched.
     * @param coords the array to place the point coordinates in
     * @return the segment type. One of ({@link #SEG_MOVETO}, {@link #SEG_LINETO}, {@link #SEG_QUADTO}, {@link #SEG_CUBICTO}, {@link #SEG_CLOSE}).
     */
    public int currentSegment(float[] coords);

    
    /**
     * Returns the coordinates of the next point(s), as well as the type of line segment. 
     * The input array must be at least a {@code double[6]}, to accommodate up to three 
     * {@code (x,y)} point pairs (although if you know the iterator is flat, you can probably 
     * get by with a {@code double[2]}). If the returned type is {@link #SEG_MOVETO} or {@link #SEG_LINETO}, 
     * the first point in the array is modified; if the returned type is {@link #SEG_QUADTO}, 
     * the first two points are modified; if the returned type is {@link #SEG_CUBICTO}, all three points are
     * modified; and if the returned type is {@link #SEG_CLOSE}, the array is untouched.
     * @param coords the array to place the point coordinates in
     * @return the segment type. One of ({@link #SEG_MOVETO}, {@link #SEG_LINETO}, {@link #SEG_QUADTO}, {@link #SEG_CUBICTO}, {@link #SEG_CLOSE}).
     */
    public int currentSegment(double[] coords);

}
