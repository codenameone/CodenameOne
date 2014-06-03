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

import com.codename1.ui.Stroke;

/**
 * An interface that can be implemented by any class that wants to be drawable
 * as a shape. 
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
    public PathIterator getPathIterator(Matrix transform);
    
    /**
     * Returns the bounding rectangle for the shape.  This should be the smallest rectangle
     * such that the all path segments in the shape are contained within it.
     * @return A {@link Rectangle} that comprises the bounds of the shape.
     */
    public Rectangle getBounds();
    public float[] getBounds2D();
    
    public boolean isRectangle();
    public boolean contains(int x, int y);
    public Shape intersection(Rectangle rect);
}
