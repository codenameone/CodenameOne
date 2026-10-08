/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.desktopcompat.java.awt.geom;

/// Walks the outline of a shape one segment at a time.
///
/// A segment is a move, a line, a quadratic or cubic Bezier curve, or the
/// closing of the current subpath. `currentSegment` stores the points of the
/// segment in the array it is handed and returns one of the `SEG_` constants.
public interface PathIterator {

    /// Interior rule: a point is inside when a ray from it crosses the
    /// outline an odd number of times.
    int WIND_EVEN_ODD = 0;

    /// Interior rule: a point is inside when the signed count of outline
    /// crossings of a ray from it is not zero.
    int WIND_NON_ZERO = 1;

    /// Starts a new subpath at one point.
    int SEG_MOVETO = 0;

    /// A straight line to one point.
    int SEG_LINETO = 1;

    /// A quadratic curve: one control point, then the end point.
    int SEG_QUADTO = 2;

    /// A cubic curve: two control points, then the end point.
    int SEG_CUBICTO = 3;

    /// Closes the current subpath back to its last move; stores no points.
    int SEG_CLOSE = 4;

    int getWindingRule();

    boolean isDone();

    void next();

    int currentSegment(float[] coords);

    int currentSegment(double[] coords);
}
