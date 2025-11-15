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
    void xyValueSeriesTransitionInterpolatesValues() throws Exception {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYValueSeries valueSeries = new XYValueSeries("Values");
        valueSeries.add(0, 1, 10);
        valueSeries.add(1, 2, 20);
        dataset.addSeries(valueSeries);

        ChartComponent chartComponent = createChartComponent(dataset);
        Form form = new Form();
        form.add(chartComponent);
        form.show();

        XYValueSeriesTransition transition = new XYValueSeriesTransition(chartComponent, valueSeries);
        XYValueSeries buffer = new XYValueSeries(valueSeries.getTitle());
        buffer.add(0, 3, 30);
        buffer.add(1, 4, 40);
        transition.setBuffer(buffer);

        transition.setDuration(5);
        transition.animateChart();
        while (transition.animate()) {
            Thread.sleep(5);
        }

        assertEquals(2, valueSeries.getItemCount());
        assertEquals(3.0, valueSeries.getY(0));
        assertEquals(4.0, valueSeries.getY(1));
        assertEquals(30.0, valueSeries.getValue(0));
        assertEquals(40.0, valueSeries.getValue(1));
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
