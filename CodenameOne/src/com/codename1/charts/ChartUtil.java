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
package com.codename1.charts;

import com.codename1.charts.views.AbstractChart;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;

/**
 * A utility class for painting a chart onto a Graphics context.  This is a low level
 * API.  You should use the {@link ChartComponent} class instead.
 * @author shannah
 */
public class ChartUtil {
    private Canvas c = new Canvas();
    public void paintChart(Graphics g, AbstractChart chart, Rectangle bounds, int absX, int absY){
        c.g = g;
        c.bounds = bounds;
        c.absoluteX = absX;
        c.absoluteY = absY;
        chart.draw(c, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), new Paint());
    }
    
    
}
