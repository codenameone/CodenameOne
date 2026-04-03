/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
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
