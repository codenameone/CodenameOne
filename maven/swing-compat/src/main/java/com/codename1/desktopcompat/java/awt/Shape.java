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
package com.codename1.desktopcompat.java.awt;

import com.codename1.desktopcompat.java.awt.geom.AffineTransform;
import com.codename1.desktopcompat.java.awt.geom.PathIterator;
import com.codename1.desktopcompat.java.awt.geom.Point2D;
import com.codename1.desktopcompat.java.awt.geom.Rectangle2D;

/// A geometric outline that can report its bounds, answer hit tests and
/// describe itself as a sequence of path segments.
public interface Shape {

    Rectangle getBounds();

    Rectangle2D getBounds2D();

    boolean contains(double x, double y);

    boolean contains(Point2D p);

    boolean intersects(double x, double y, double w, double h);

    boolean intersects(Rectangle2D r);

    boolean contains(double x, double y, double w, double h);

    boolean contains(Rectangle2D r);

    PathIterator getPathIterator(AffineTransform at);

    PathIterator getPathIterator(AffineTransform at, double flatness);
}
