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

package com.codename1.impl.html5.graphics;

import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

/**
 *
 * @author shannah
 */
class GraphicsUtils {
    static void addBezierArcTo(CanvasRenderingContext2D path, double cx, double cy,
                                          double startX, double startY, double endX, double endY)
    {
        
        if ( startX != endX || startY != endY ){
            final double ax = startX - cx;
            final double ay = startY - cy;
            final double bx = endX - cx;
            final double by = endY- cy;
            final double q1 = ax * ax + ay * ay;
            final double q2 = q1 + ax * bx + ay * by;
            //final double intermed = (ax * by - ay * bx);
            final double k2 = 4.0 / 3.0 * (Math.sqrt(2.0 * q1 * q2) - q2) / (ax * by - ay * bx);
            final double x2 = cx + ax - k2 * ay;
            final double y2 = cy + ay + k2 * ax;
            final double x3 = cx + bx + k2 * by;
            final double y3 = cy + by - k2 * bx;
            
            path.bezierCurveTo(x2, y2, x3, y3, endX, endY);
        } 
        
        
    }
}
