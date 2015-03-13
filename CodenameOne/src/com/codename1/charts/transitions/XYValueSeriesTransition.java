/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.charts.transitions;

import com.codename1.charts.ChartComponent;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.models.XYValueSeries;

/**
 * A transition for enabling animations between different values in an XYSeries.
 * @author shannah
 */
public class XYValueSeriesTransition extends SeriesTransition {
    
   
    /**
     * The subject series.
     */
    private final XYValueSeries series;
    
    /**
     * The buffer series where values are set before they are finally applied
     * to the series during the animation.
     */
    private XYValueSeries cachedSeries;
    
    /**
     * Start values for the series in the transition.
     */
    private XYValueSeries startVals;
    
    /**
     * End values for the series in the transition.
     */
    private XYValueSeries endVals;
    
    
    /**
     * Creates a new transition on the given chart and associated series.  The 
     * series should be one of the series rendered by the given chart.
     * @param chart The ChartComponent that is being used to render the series.
     * @param series The series whose data you wish to animate.
     */
    public XYValueSeriesTransition(ChartComponent chart, XYValueSeries series){
        super(chart);
        this.series = series;
    }

    /**
     * Initializes the transition.  This can be overridden by subclasses to 
     * provide their own functionality to be executed just before the transition
     * occurs.
     */
    @Override
    public void initTransition() {
        super.initTransition();
        
        
        
        // Now make sure that there are the same number of values in source and
        // target
        startVals = new XYValueSeries("Start");
        copyValues(series, startVals);
        
        endVals = new XYValueSeries("End");
        copyValues(cachedSeries, endVals);
        
        
    }
    
    private void copyValues(XYValueSeries source, XYValueSeries target){
        int len = source.getItemCount();
        
        for (int i=0; i<len; i++){
            int index = target.getIndexForKey(source.getX(i));
            if (index > -1){
                target.remove(index);
            }
            target.add(source.getX(i), source.getY(i), source.getValue(i));
        }
    }

    /**
     * Cleans up after the transition is complete.
     */
    @Override
    protected void cleanup() {
        super.cleanup(); 
        this.cachedSeries.clear();
    }
    
    

    /**
     * Updates the series and renderer at the given progress position (0 to 100).
     * @param progress The progress position in the motion. (0-100).
     */
    protected void update(int progress) {
        double dProgress = (double)progress;
        int len = endVals.getItemCount();
        for (int i=0; i<len; i++){
            double x = endVals.getX(i);
            double y = endVals.getY(i);
            double val = endVals.getValue(i);
            int startIndex = startVals.getIndexForKey(x);
            int endindex = i;
            
            
            double startY = startIndex == -1 ? 0.0 : startVals.getY(startIndex);
            double endY = y;
            double tweenY = startY + (endY-startY)*dProgress/100.0;
            
            double startVal = startIndex == -1 ? 0.0 : startVals.getValue(startIndex);
            double endVal = val;
            double tweenVal = startVal + (endVal-startVal)*dProgress/100.0;
            
            
            int seriesIndex = series.getIndexForKey(x);
            
            if (seriesIndex > -1 ){
                series.remove(seriesIndex);
            }
            series.add(x, tweenY, tweenVal);
            
            
        }
        
        
    }
    
    /**
     * Gets the "buffer" series where values can be set.  Any values set on the 
     * buffer will be applied to the target series during the course of the 
     * transition.
     * @return 
     */
    public XYValueSeries getBuffer(){
        if (cachedSeries == null){
             cachedSeries = new XYValueSeries(series.getTitle());
        }
        return cachedSeries;
    }
    
    /**
     * Sets the buffer/cache series to be used.
     * @param buffer 
     */
    void setBuffer(XYValueSeries buffer){
        this.cachedSeries = buffer;
    }
    
    /**
     * Gets the series whose values are to be animated by this transition.
     * @return 
     */
    public XYValueSeries getSeries(){
        return series;
    }

   
}
