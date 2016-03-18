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

import com.codename1.ui.Transform;

/**
 * <p>An interface that can be implemented by any class that wants to be drawable
 * as a shape. </p>
 * 
 * <script src="https://gist.github.com/codenameone/3f2f8cdaabb7780eae6f.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/graphics-shape-fill.png" alt="Fill a shape general path" />
 * 
 * @author Steve Hannah
 * @see GeneralPath for a concrete implementation of Shape.
 * @see com.codename1.ui.Graphics#drawShape
 * @see com.codename1.ui.Graphics#fillShape
 */
public interface Shape {
    
    /**
     * Gets an iterator to walk all of the path segments of the shape.
     * @return A PathIterator that can iterate over the path segments of the shape.
     * 
     */
    public PathIterator getPathIterator();
    
    /**
     * Gets an iterator where all points are transformed by the provided transform.
     * <p>Note: If {@link com.codename1.ui.Transform#isSupported()} is false, then using this iterator will throw a Runtime Exception.</p>
     * @param transform
     * @return A PathIterator where points are transformed by the provided transform.
     */
    public PathIterator getPathIterator(Transform transform);
    
    /**
     * Returns the bounding rectangle for the shape.  This should be the smallest rectangle
     * such that the all path segments in the shape are contained within it.
     * @return A {@link Rectangle} that comprises the bounds of the shape.
     */
    public Rectangle getBounds();
    
    /**
     * Gets the bounds of the shape as a 4-element array representing the (x,y,width,height)
     * tuple.
     * @return [x, y, width, height] bounds of this shape. 
     */
    public float[] getBounds2D();
    
    /**
     * Checks if this shape is a rectangle.  A Shape is a rectangle if it is a closed quadrilateral
     * composed of two vertical lines and two horizontal lines.  If all points have integer coordinates,
     * and this returns true, then getBounds() should return an equivalent rectangle to the shape itself.
     * @return True if shape is a rectangle.
     */
    public boolean isRectangle();
    
    /**
     * Checks if the shape contains the given point.
     * @param x The x-coordinate of the point to test.
     * @param y The y-coordinate of the point to test.
     * @return True if (x, y) is inside the shape.
     */
    public boolean contains(int x, int y);
    
    /**
     * Returns the shape formed by the intersection of this shape and the provided 
     * rectangle.
     * @param rect A rectangle with which to form an intersection.
     * @return The shape formed by intersecting the current shape with the provided rectangle.
     */
    public Shape intersection(Rectangle rect);
}
