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
/// Two dimensional geometry for the desktop compatibility layer: points,
/// rectangles, lines, ellipses, arcs, Bezier curves, general paths and the
/// affine transform that maps between coordinate spaces.
///
/// These classes stand in for the JDK's `java.awt.geom` package once an
/// application has been relocated onto the layer. They are plain data and
/// arithmetic with no dependency on a toolkit, and are written against the
/// small class library a device has: trigonometry beyond sine, cosine and
/// tangent goes through `com.codename1.util.MathUtil`, and every `clone()`
/// builds its copy explicitly.
///
/// Curved outlines are answered through [PathIterator] as cubic Bezier
/// segments. Hit testing of a [Path2D], a [QuadCurve2D], a [CubicCurve2D] and
/// of an [Arc2D] against a rectangle is done over the flattened outline, so
/// it is exact for polygons and accurate to a small fraction of the shape's
/// size for curves.
package com.codename1.desktopcompat.java.awt.geom;
