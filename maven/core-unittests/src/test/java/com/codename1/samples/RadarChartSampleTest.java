package com.codename1.samples;

import com.codename1.charts.ChartComponent;
import com.codename1.charts.models.AreaSeries;
import com.codename1.charts.models.CategorySeries;
import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.RadarChart;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class RadarChartSampleTest extends UITestBase {

    @FormTest
    public void testRadarChart() {
        Form f = new Form("RadarChartSample", new BorderLayout());

        // Create dataset
        AreaSeries dataset = new AreaSeries();

        CategorySeries series1 = new CategorySeries("May");
        series1.add("Health", 0.8);
        series1.add("Attack", 0.6);
        series1.add("Defense", 0.4);
        series1.add("Critical", 0.2);
        series1.add("Speed", 1.0);
        dataset.addSeries(series1);

        CategorySeries series2 = new CategorySeries("Chang");
        series2.add("Health", 0.3);
        series2.add("Attack", 0.7);
        series2.add("Defense", 0.5);
        series2.add("Critical", 0.1);
        series2.add("Speed", 0.3);
        dataset.addSeries(series2);

        // Setup renderer
        DefaultRenderer renderer = new DefaultRenderer();
        renderer.setLegendTextSize(32);
        renderer.setLabelsTextSize(24);
        renderer.setLabelsColor(ColorUtil.BLACK);
        renderer.setShowLabels(true);

        SimpleSeriesRenderer r1 = new SimpleSeriesRenderer();
        r1.setColor(ColorUtil.MAGENTA);
        renderer.addSeriesRenderer(r1);

        SimpleSeriesRenderer r2 = new SimpleSeriesRenderer();
        r2.setColor(ColorUtil.CYAN);
        renderer.addSeriesRenderer(r2);

        // Create chart
        RadarChart chart = new RadarChart(dataset, renderer);
        ChartComponent chartComponent = new ChartComponent(chart);

        f.add(BorderLayout.CENTER, chartComponent);
        f.show();

        assertTrue(f.contains(chartComponent));

        // Assertions on the dataset we created
        assertEquals(2, dataset.getSeriesCount());
        assertEquals("May", series1.getTitle());
        assertEquals("Chang", series2.getTitle());
        assertEquals(0.8, series1.getValue(0), 0.001);
    }
}
