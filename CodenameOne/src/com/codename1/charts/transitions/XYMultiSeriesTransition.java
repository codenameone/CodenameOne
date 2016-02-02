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
package com.codename1.charts.transitions;

import com.codename1.charts.ChartComponent;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;

/**
 * A transition to animate the values of a MultipleSeriesDataset (used by BarChart).
 * @author shannah
 */
public class XYMultiSeriesTransition extends SeriesTransition {

    /**
     * The data set whose values are to be animated.
     */
    private final XYMultipleSeriesDataset dataset;
    
    /**
     * A buffer or cache dataset to store values before they are applied to the
     * target dataset.
     */
    private XYMultipleSeriesDataset datasetCache;
    
    /**
     * Transitions for the individual series of the dataset.
     */
    private XYSeriesTransition[] seriesTransitions;
    
    
    /**
     * Creates a new transition for the given chart and dataset.  The dataset
     * must be rendered by the given chart for this to work correctly.
     * @param chart
     * @param dataset 
     */
    public XYMultiSeriesTransition(ChartComponent chart, XYMultipleSeriesDataset dataset) {
        super(chart);
        this.dataset = dataset;
    }

    /**
     * @inherit
     */
    @Override
    protected void initTransition() {
        
        getBuffer(); // initializes the buffer and seriesTranslations
        int len = seriesTransitions.length;
        for (int i=0; i<len; i++){
            seriesTransitions[i].initTransition();
        }
        super.initTransition(); 
        
        
    }

    
    /**
     * @inherit
     * @param progress 
     */
    @Override
    protected void update(int progress) {
        getBuffer(); // initializes the buffer and seriesTranslations
        int len = seriesTransitions.length;
        for (int i=0; i<len; i++){
            seriesTransitions[i].update(progress);
        }
    }

    /**
     * @inherit
     */
    @Override
    protected void cleanup() {
        super.cleanup(); 
        getBuffer(); // initializes the buffer and seriesTranslations
        int len = seriesTransitions.length;
        for (int i=0; i<len; i++){
            seriesTransitions[i].cleanup();
        }
    }
    
    
    /**
     * Gets the buffer/cache for values.  Values set in the buffer will be applied
     * to the target dataset when the transition takes place.
     * @return 
     */
    public XYMultipleSeriesDataset getBuffer(){
        if (datasetCache == null){
            datasetCache = new XYMultipleSeriesDataset();
            for (int i=0; i<dataset.getSeriesCount(); i++){
                datasetCache.addSeries(new XYSeries(dataset.getSeriesAt(i).getTitle()));
            }
            seriesTransitions = new XYSeriesTransition[dataset.getSeries().length];
            int tlen = seriesTransitions.length;
            for (int i=0; i<tlen; i++){
                seriesTransitions[i] = new XYSeriesTransition(getChart(), dataset.getSeriesAt(i));
                seriesTransitions[i].setBuffer(datasetCache.getSeriesAt(i));

            }
        }
        return datasetCache;
    }
    
}
