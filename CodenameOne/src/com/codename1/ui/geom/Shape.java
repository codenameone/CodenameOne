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

/// An interface that can be implemented by any class that wants to be drawable
/// as a shape.
///
/// ```java
/// Form hi = new Form("Shape");
///
/// // We create a 50 x 100 shape, this is arbitrary since we can scale it easily
/// GeneralPath path = new GeneralPath();
/// path.moveTo(20,0);
/// path.lineTo(30, 0);
/// path.lineTo(30, 100);
/// path.lineTo(20, 100);
/// path.lineTo(20, 15);
/// path.lineTo(5, 40);
/// path.lineTo(5, 25);
/// path.lineTo(20,0);
///
/// hi.getContentPane().getUnselectedStyle().setBgPainter((Graphics g, Rectangle rect) -> {
///     g.setColor(0xff);
///     float widthRatio = ((float)rect.getWidth()) / 50f;
///     float heightRatio = ((float)rect.getHeight()) / 100f;
///     g.scale(widthRatio, heightRatio);
///     g.translate((int)(((float)rect.getX()) / widthRatio), (int)(((float)rect.getY()) / heightRatio));
///     g.fillShape(path);
///     g.resetAffine();
/// });
///
/// hi.show();
/// ```
///
/// Shape can also be used to clip an area e.g.:
///
/// ```java
/// Image duke = null;
/// try {
///     // duke.png is just the default Codename One icon copied into place
///     duke = Image.createImage("/duke.png");
/// } catch(IOException err) {
///     Log.e(err);
/// }
/// final Image finalDuke = duke;
///
/// Form hi = new Form("Shape Clip");
///
/// // We create a 50 x 100 shape, this is arbitrary since we can scale it easily
/// GeneralPath path = new GeneralPath();
/// path.moveTo(20,0);
/// path.lineTo(30, 0);
/// path.lineTo(30, 100);
/// path.lineTo(20, 100);
/// path.lineTo(20, 15);
/// path.lineTo(5, 40);
/// path.lineTo(5, 25);
/// path.lineTo(20,0);
///
/// Stroke stroke = new Stroke(0.5f, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 4);
/// hi.getContentPane().getUnselectedStyle().setBgPainter((Graphics g, Rectangle rect) -> {
///     g.setColor(0xff);
///     float widthRatio = ((float)rect.getWidth()) / 50f;
///     float heightRatio = ((float)rect.getHeight()) / 100f;
///     g.scale(widthRatio, heightRatio);
///     g.translate((int)(((float)rect.getX()) / widthRatio), (int)(((float)rect.getY()) / heightRatio));
///     g.setClip(path);
///     g.setAntiAliased(true);
///     g.drawImage(finalDuke, 0, 0, 50, 100);
///     g.setClip(path.getBounds());
///     g.drawShape(path, stroke);
///     g.translate(-(int)(((float)rect.getX()) / widthRatio), -(int)(((float)rect.getY()) / heightRatio));
///     g.resetAffine();
/// });
///
/// hi.show();
/// ```
///
/// @author Steve Hannah
///
/// #### See also
///
/// - GeneralPath for a concrete implementation of Shape.
///
/// - com.codename1.ui.Graphics#drawShape
///
/// - com.codename1.ui.Graphics#fillShape
public interface Shape {

    /// Gets an iterator to walk all of the path segments of the shape.
    ///
    /// #### Returns
    ///
    /// A PathIterator that can iterate over the path segments of the shape.
    PathIterator getPathIterator();

    /// Gets an iterator where all points are transformed by the provided transform.
    ///
    /// Note: If `com.codename1.ui.Transform#isSupported()` is false, then using this iterator will throw a Runtime Exception.
    ///
    /// #### Parameters
    ///
    /// - `transform`
    ///
    /// #### Returns
    ///
    /// A PathIterator where points are transformed by the provided transform.
    PathIterator getPathIterator(Transform transform);

    /// Returns the bounding rectangle for the shape.  This should be the smallest rectangle
    /// such that the all path segments in the shape are contained within it.
    ///
    /// #### Returns
    ///
    /// A `Rectangle` that comprises the bounds of the shape.
    Rectangle getBounds();

    /// Gets the bounds of the shape as a 4-element array representing the (x,y,width,height)
    /// tuple.
    ///
    /// #### Returns
    ///
    /// [x, y, width, height] bounds of this shape.
    float[] getBounds2D();

    /// Checks if this shape is a rectangle.  A Shape is a rectangle if it is a closed quadrilateral
    /// composed of two vertical lines and two horizontal lines.  If all points have integer coordinates,
    /// and this returns true, then getBounds() should return an equivalent rectangle to the shape itself.
    ///
    /// #### Returns
    ///
    /// True if shape is a rectangle.
    boolean isRectangle();

    /// Checks if the shape contains the given point.
    ///
    /// #### Parameters
    ///
    /// - `x`: The x-coordinate of the point to test.
    ///
    /// - `y`: The y-coordinate of the point to test.
    ///
    /// #### Returns
    ///
    /// True if (x, y) is inside the shape.
    boolean contains(int x, int y);

    /// Returns the shape formed by the intersection of this shape and the provided
    /// rectangle.
    ///
    /// #### Parameters
    ///
    /// - `rect`: A rectangle with which to form an intersection.
    ///
    /// #### Returns
    ///
    /// The shape formed by intersecting the current shape with the provided rectangle.
    Shape intersection(Rectangle rect);
}
