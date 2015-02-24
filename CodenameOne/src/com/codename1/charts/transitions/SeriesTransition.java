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
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.Motion;

/**
 * A base class for series transitions of ChartComponent.  This should be 
 * overridden by concrete classes that implement the update(int) method
 * to update the chart's model and renderer appropriately.  This class can serve
 * as a buffer for changes to the model so that they don't affect the ChartComponent
 * immediately.  Changes can either be eased in using animateChart() or updated
 * in one shot using updateChart().
 * @author shannah
 */
public abstract class SeriesTransition implements Animation {
    
    
    
    public static final int EASING_LINEAR=1;
    public static final int EASING_IN=2;
    public static final int EASING_OUT=3;
    public static final int EASING_IN_OUT=4;
    
    /**
     * The chart to be animated.
     */
    private ChartComponent chart;
    
    /**
     * The duration of the transition (in ms).
     */
    private int duration;
    
    /**
     * Motion that will be used to perform the transition.
     */
    private Motion motion;
    
    /**
     * The type of easing that should be used for the transition.
     */
    private int easing = EASING_LINEAR;
    
    /**
     * Flag to indicate that the animation is finished.
     */
    private boolean finished;
    
    
    public SeriesTransition(ChartComponent chart){
        this(chart, EASING_LINEAR, 200);
    }
    
    public SeriesTransition(ChartComponent chart, int easing){
        this(chart, easing, 200);
    }
    
    public SeriesTransition(ChartComponent chart, int easing, int duration){
        this.chart = chart;
        this.easing = easing;
        this.duration = duration;
        
    }
    
    
    /**
     * Initializes the transition for another iteration.  This can be overridden
     * by subclasses to provide their own initialization.  This method
     * will be called just prior to the transition taking place.
     * IMPORTANT: Subclasses must make sure to call super.initTransition()
     * so that the animation will be initialized properly.
     */
    protected void initTransition(){
        finished = false;
        switch (getEasing()){
            case EASING_LINEAR:
                motion = Motion.createLinearMotion(0, 100, getDuration());
                break;
            case EASING_IN:
                motion = Motion.createEaseInMotion(0, 100, getDuration());
                break;
            case EASING_OUT:
                motion = Motion.createEaseOutMotion(0, 100, getDuration());
                break;
            case EASING_IN_OUT:
                motion = Motion.createEaseInOutMotion(0, 100, getDuration());
                break;
        }
        motion.start();
    }
    
    /**
     * Cleans up any settings in the transition.  Called after a transition 
     * is complete.  This is meant to be overridden by subclasses.
     */
    protected void cleanup(){
        
    }

    /**
     * Updates the renderer and model at the specified progress position of
     * the animation.  Meant to be overridden by subclasses.
     * @param progress The progress of the animation (between 0 and 100). 
     */
    protected abstract void update(int progress);
    
    public boolean animate() {
        if (finished){
            cleanup();
            chart.getComponentForm().deregisterAnimated(this);
            return false;
        } else if (motion.isFinished()){
            finished = true;
        }
        update(motion.getValue());
        return true;
    }

    public void paint(Graphics g) {
        getChart().repaint();
    }

    /**
     * Gets the ChartComponent that is the subject of the transition.
     * @return the chart
     */
    public ChartComponent getChart() {
        return chart;
    }

    /**
     * Sets the ChartComponent that is the subject of the transition.
     * @param chart the chart to set
     */
    public void setChart(ChartComponent chart) {
        this.chart = chart;
    }

    /**
     * Gets the duration of the transition. (in milliseconds)
     * @return the duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Sets the duration of the transition in milliseconds.
     * @param duration the duration to set
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * Gets the type of easing used in the transition.  Should be one of
     * EASING_LINEAR, EASING_IN, EASING_OUT, or EASING_IN_OUT.
     * @return the easing
     */
    public int getEasing() {
        return easing;
    }

    /**
     * Sets the type of easing used in the transition.  Should be one of
     * EASING_LINEAR, EASING_IN, EASING_OUT, or EASING_IN_OUT.
     * @param easing the easing to set
     */
    public void setEasing(int easing) {
        this.easing = easing;
    }
    
    
    /**
     * Applies all pending changes to the chart model and renderer using the
     * current animation settings.
     */
    public void animateChart(){
        initTransition();
        chart.getComponentForm().registerAnimated(this);
        
    }
    
    /**
     * Applies all pending changes to the chart model and renderer and repaints
     * the chart.  This is basically like calling animateChart() with a duration
     * of 0.
     */
    public void updateChart(){
        chart.repaint();
    }
    
}
