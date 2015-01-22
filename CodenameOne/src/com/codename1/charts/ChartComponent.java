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

import com.codename1.charts.models.Point;
import com.codename1.charts.models.SeriesSelection;
import com.codename1.charts.views.AbstractChart;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.Transform;


/**
 * A component for displaying a chart.
 * @author shannah
 */
public class ChartComponent extends Component {
    
    /**
     * The chart that is to be rendered in this component.
     */
    private AbstractChart chart;
    
    /**
     * Util object for rendering the chart.
     */
    private final ChartUtil util = new ChartUtil();
    
    /**
     * The transform for the chart.  This can be used to scale, translate, and
     * rotate the chart.  This transform assumes its origin at the (absoluteX, absoluteY)
     * of the component at the time it is drawn rather than the screen's origin as is 
     * normally the case with transforms.  This allows the transform to be applied consistently
     * with respect to the chart's coordinates even when the component is moved around the screen.
     */
    private Transform transform = null;
    
    /**
     * The transform that was applied during the last paint() method.
     * This is generally the {@link #transform} concatenated with a
     * translation to the screen origin from the component's origin.  This 
     * is used to convert chart coordinates to screen coordinates and respond
     * properly to events.
     */
    private Transform currentTransform = null;
    
    /**
     * Flag to enable panning the chart.  Default is false.
     */
    private boolean panEnabled = true;
    
    /**
     * During a pan operation, used to store the transform as it was before
     * the start of the pan.
     */
    private Transform dragTransformStart = null;
    
    /**
     * The starting position of a pan operation.
     */
    private Point dragStart = null;
    
    /**
     * Flag to enable pinch zoom.
     */
    private boolean zoomEnabled = false;
    
    /**
     * During a pinch zoom operation, this is the middle point between the
     * two touch points at the start of the zoom operation.  This is in
     * screen coordinates.
     */
    private Point zoomStart = null;
    
    /**
     * During a pinch zoom operation, this is the transform as it was at the start
     * of the zoom.
     */
    private Transform zoomTransformStart = null;
    
    /**
     * During a pinch zoom operation, this is the distance between the two touch
     * points at the start of the zoom operation.
     */
    private double zoomDistStart = 0;
    
    /**
     * Creates a new chart component to display the provided chart.
     * @param chart The chart to be displayed in this component.
     */
    public ChartComponent(AbstractChart chart){
        this.chart = chart;
    }
    
    /**
     * Gets the chart that is being displayed in this component.
     * @return 
     */
    public AbstractChart getChart(){
        return chart;
    }
    
    /**
     * Sets the chart to be displayed in this component.
     * @param chart 
     */
    public void setChart(AbstractChart chart){
        this.chart = chart;
    }
    
    /**
     * Paints the chart.
     * @param g 
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        boolean oldAntialias = g.isAntiAliased();
        g.setAntiAliased(true);
        Transform oldTransform = null;
        if ( getTransform() != null ){
            oldTransform = g.getTransform();
            
            currentTransform = Transform.makeTranslation(getAbsoluteX(), getAbsoluteY());
            currentTransform.concatenate(transform);
            currentTransform.translate(-getAbsoluteX(), -getAbsoluteY());
            
            g.setTransform(currentTransform);
        } else {
            currentTransform = null;
        }
        
        
        util.paintChart(g, chart, getBounds(), getAbsoluteX(), getAbsoluteY());
        
        if ( oldTransform != null ){
            g.setTransform(oldTransform);
        }
        
        g.setAntiAliased(oldAntialias);
    }

    /**
     * Converts screen coordinates to chart coordinates.
     * @param x 
     * @param y
     * @return The chart coordinate corresponding to the given screen coordinate.
     */
    public Point screenToChartCoord(int x, int y){
        if ( currentTransform != null ){
            Transform inverse = currentTransform.getInverse();
            float[] pt = inverse.transformPoint(new float[]{x,y, 0});
            x = (int)pt[0];
            y = (int)pt[1];
            
        }
        return new Point(x-getAbsoluteX(), y-getAbsoluteY());
    }
    
    
    
    @Override
    public void pointerPressed(int x, int y) {
       
        Point chartCoord = screenToChartCoord(x, y);
        SeriesSelection sel = chart.getSeriesAndPointForScreenCoordinate(chartCoord);
        if ( sel == null ){
            return;
        }
        
        seriesPressed(sel);
        super.pointerPressed(x, y); //To change body of generated methods, choose Tools | Templates.
        
    }
    
    /**
     * Called when a pointer is pressed on a series in the chart.  This can be 
     * overridden by subclasses to respond to this event.
     * @param sel 
     */
    protected void seriesPressed(SeriesSelection sel){
        
    }

    @Override
    public void pointerReleased(int x, int y) {
        dragStart = null;
        dragTransformStart = null;
        zoomStart = null;
        zoomDistStart = 0;
        zoomTransformStart = null;
        
       Point chartCoord = screenToChartCoord(x, y);
        SeriesSelection sel = chart.getSeriesAndPointForScreenCoordinate(chartCoord);
        if ( sel == null ){
            return;
        }
        
        seriesReleased(sel);
        super.pointerReleased(x, y); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Called when a pointer is released from a series in the chart.  This can be 
     * overridden in subclasses to handle these events.
     * @param sel 
     */
    protected void seriesReleased(SeriesSelection sel){
        
    }

    /**
     * 
     * Gets the transform for the chart.  This can be used to scale, translate, and
     * rotate the chart.  This transform assumes its origin at the (absoluteX, absoluteY)
     * of the component at the time it is drawn rather than the screen's origin as is 
     * normally the case with transforms.  This allows the transform to be applied consistently
     * with respect to the chart's coordinates even when the component is moved around the screen.
     * @return The transform for the chart in component coordinates.
     */
    public Transform getTransform() {
        return transform;
    }

    /**
     * Sets the transform for the chart.  Transforms origin assumed to be at (getAbsoluteX, getAbsoluteY).
     * @param transform the transform to set
     */
    public void setTransform(Transform transform) {
        this.transform = transform;
    }

    @Override
    public void pointerDragged(int[] x, int[] y) {
        if ( x.length > 1 ){
            // Pinch zoom
            if ( zoomStart == null ){
                zoomStart = new Point((x[0]+x[1])/2, (y[0]+y[1])/2);
                zoomTransformStart = Transform.makeIdentity();
                if ( transform != null ){
                    zoomTransformStart.concatenate(transform);
                }
                int dx = Math.abs(x[0]-x[1])/2;
                int dy = Math.abs(y[0]-y[1])/2;
                zoomDistStart = Math.sqrt(dx*dx+dy*dy);
                
                
                
            } else {
                int dx = Math.abs(x[0]-x[1])/2;
                int dy = Math.abs(y[0]-y[1])/2;
                double zoomDist = Math.sqrt(dx*dx+dy*dy);
                if ( zoomDist == 0 ){
                    zoomDist = 1;
                }
                transform = Transform.makeIdentity();
                transform.translate(zoomStart.getX(), zoomStart.getY());
                transform.scale((float)(zoomDist/zoomDistStart), (float)(zoomDist/zoomDistStart));
                transform.translate(-zoomStart.getX(), -zoomStart.getY());
                transform.concatenate(zoomTransformStart);
                this.repaint();
            }
            
        } else {
            if ( !panEnabled ){
                return;
            }
            if ( dragStart == null ){
                dragStart = new Point(x[0],y[0]);
                dragTransformStart = Transform.makeIdentity();
                if ( transform != null ){
                    dragTransformStart.concatenate(transform);
                }

            } else {
                transform = Transform.makeIdentity();
                transform.translate(x[0]-dragStart.getX(), y[0]-dragStart.getY());
                transform.concatenate(dragTransformStart);
                this.repaint();
            }
        }
        
        super.pointerDragged(x, y); //To change body of generated methods, choose Tools | Templates.
    }

    
    
    

    /**
     * Checks if panning is enabled.
     * @return the panEnabled
     */
    public boolean isPanEnabled() {
        return panEnabled;
    }

    /**
     * @param panEnabled the panEnabled to set
     */
    public void setPanEnabled(boolean panEnabled) {
        this.panEnabled = panEnabled;
    }

    /**
     * @return the zoomEnabled
     */
    public boolean isZoomEnabled() {
        return zoomEnabled;
    }

    /**
     * @param zoomEnabled the zoomEnabled to set
     */
    public void setZoomEnabled(boolean zoomEnabled) {
        this.zoomEnabled = zoomEnabled;
    }
    
    
    
    
    
    
    
}
