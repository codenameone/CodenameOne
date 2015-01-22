/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.charts;

import com.codename1.charts.views.AbstractChart;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;

/**
 *
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
