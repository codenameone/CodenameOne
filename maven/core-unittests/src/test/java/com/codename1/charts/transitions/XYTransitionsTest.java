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
import com.codename1.charts.models.XYValueSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.views.LineChart;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;

import static org.junit.jupiter.api.Assertions.*;

class XYTransitionsTest extends UITestBase {

    @FormTest
    void xySeriesTransitionAnimatesBufferValues() throws Exception {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries("Series");
        series.add(0, 1);
        series.add(1, 2);
        dataset.addSeries(series);

        ChartComponent chartComponent = createChartComponent(dataset);
        Form form = new Form();
        form.add(chartComponent);
        form.show();

        XYSeriesTransition transition = new XYSeriesTransition(chartComponent, series);
        XYSeries buffer = new XYSeries(series.getTitle(), series.getScaleNumber());
        buffer.add(0, 5);
        buffer.add(1, 7);
        transition.setBuffer(buffer);

        transition.setDuration(5);
        transition.animateChart();
        while (transition.animate()) {
            Thread.sleep(5);
        }

        assertEquals(2, series.getItemCount());
        assertEquals(5.0, series.getY(0));
        assertEquals(7.0, series.getY(1));
        assertEquals(0, buffer.getItemCount());
    }
    
    @FormTest
    void multiSeriesTransitionUpdatesAllSeries() throws Exception {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries first = new XYSeries("First");
        first.add(0, 1);
        first.add(1, 2);
        XYSeries second = new XYSeries("Second");
        second.add(0, 4);
        second.add(1, 5);
        dataset.addSeries(first);
        dataset.addSeries(second);

        ChartComponent chartComponent = createChartComponent(dataset);
        Form form = new Form();
        form.add(chartComponent);
        form.show();

        XYMultiSeriesTransition transition = new XYMultiSeriesTransition(chartComponent, dataset);
        XYMultipleSeriesDataset buffer = transition.getBuffer();
        assertEquals(2, buffer.getSeriesCount());
        buffer.getSeriesAt(0).add(0, 10);
        buffer.getSeriesAt(0).add(1, 12);
        buffer.getSeriesAt(1).add(0, 8);
        buffer.getSeriesAt(1).add(1, 9);

        transition.setDuration(5);
        transition.animateChart();
        while (transition.animate()) {
            Thread.sleep(5);
        }

        assertEquals(10.0, first.getY(0));
        assertEquals(12.0, first.getY(1));
        assertEquals(8.0, second.getY(0));
        assertEquals(9.0, second.getY(1));
        assertEquals(0, buffer.getSeriesAt(0).getItemCount());
        assertEquals(0, buffer.getSeriesAt(1).getItemCount());
    }

    @FormTest
    void updateChartAppliesTheBufferWithoutAnimating() {
        // updateChart() is documented as animateChart() with a duration of 0,
        // and was a bare chart.repaint(): the buffer was never read, so every
        // value written into it was dropped and the series kept the numbers it
        // already had. Nothing failed and nothing was logged -- the chart
        // simply did not change.
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries("Series");
        series.add(0, 1);
        series.add(1, 2);
        dataset.addSeries(series);

        ChartComponent chartComponent = createChartComponent(dataset);
        Form form = new Form();
        form.add(chartComponent);
        form.show();

        XYSeriesTransition transition = new XYSeriesTransition(chartComponent, series);
        XYSeries buffer = transition.getBuffer();
        buffer.add(0, 5);
        buffer.add(1, 7);

        transition.updateChart();

        assertEquals(2, series.getItemCount());
        assertEquals(5.0, series.getY(0), "updateChart() left the series at its old value");
        assertEquals(7.0, series.getY(1), "updateChart() left the series at its old value");
        // Drained, so the transition is in the same state an animation would
        // have left it in and can be reused.
        assertEquals(0, buffer.getItemCount());
    }

    @FormTest
    void updateChartDuringAnAnimationStopsIt() {
        // updateChart() called on a transition animateChart() had already
        // registered left it registered, and its own initTransition() restarted
        // the motion -- so the next frame ran update() at a low progress and
        // walked the series back towards the values this call had just
        // replaced. The immediate update was undone and replayed as an
        // animation.
        //
        // The form is deliberately NOT shown: registration works without it,
        // and showing it puts the EDT's animation loop on the same transition
        // this test drives by hand, so the two race over one XYSeries.
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries("Series");
        series.add(0, 1);
        series.add(1, 2);
        dataset.addSeries(series);

        ChartComponent chartComponent = createChartComponent(dataset);
        Form form = new Form();
        form.add(chartComponent);

        XYSeriesTransition transition = new XYSeriesTransition(chartComponent, series);
        transition.getBuffer().add(0, 5);
        transition.getBuffer().add(1, 7);
        transition.setDuration(10000);
        transition.animateChart();

        // The same transition, finished early. The buffer still holds what the
        // animation was heading for, because only cleanup() drains it.
        transition.updateChart();

        assertEquals(5.0, series.getY(0));
        assertEquals(7.0, series.getY(1));

        // A frame already queued behind the update must not move it again.
        assertFalse(transition.animate(), "the transition is still animating");
        assertEquals(5.0, series.getY(0), "a later frame walked the series back");
        assertEquals(7.0, series.getY(1), "a later frame walked the series back");
    }

    @FormTest
    void aTransitionWithNoBufferIsANoOpRatherThanACrash() {
        // The buffer is created lazily by getBuffer(), so a transition nobody
        // wrote to has a null one. initTransition() handed that straight to
        // copyValues() and threw -- which animateChart() has always done and
        // updateChart() inherited the moment it started running the lifecycle.
        // Applying no pending changes has to stay a repaint.
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries("Series");
        series.add(0, 1);
        series.add(1, 2);
        dataset.addSeries(series);

        ChartComponent chartComponent = createChartComponent(dataset);
        Form form = new Form();
        form.add(chartComponent);

        XYSeriesTransition immediate = new XYSeriesTransition(chartComponent, series);
        immediate.updateChart();
        assertEquals(1.0, series.getY(0), "an empty update changed the series");
        assertEquals(2.0, series.getY(1), "an empty update changed the series");

        XYSeriesTransition animated = new XYSeriesTransition(chartComponent, series);
        animated.animateChart();
        assertEquals(1.0, series.getY(0), "an empty animation changed the series");
    }

    @FormTest
    void aValueSeriesTransitionWithNoBufferIsANoOpToo() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYValueSeries series = new XYValueSeries("Series");
        // Doubles, deliberately: int literals bind to XYSeries.add(int, double,
        // double), which inserts at an index and never records the value.
        series.add(0.0, 1.0, 5.0);
        dataset.addSeries(series);

        ChartComponent chartComponent = createChartComponent(dataset);
        Form form = new Form();
        form.add(chartComponent);

        new XYValueSeriesTransition(chartComponent, series).updateChart();
        assertEquals(1, series.getItemCount());
        assertEquals(1.0, series.getY(0));
    }

    private ChartComponent createChartComponent(XYMultipleSeriesDataset dataset) {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.addSeriesRenderer(new XYSeriesRenderer());
        }
        LineChart chart = new LineChart(dataset, renderer);
        ChartComponent component = new ChartComponent(chart);
        component.setWidth(100);
        component.setHeight(100);
        return component;
    }

}
