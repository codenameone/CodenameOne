package com.codename1.charts;

import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.views.ScatterChart;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

public class ChartComponentBBoxTest extends UITestBase {

    @FormTest
    public void testBBox() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setZoomEnabled(true, true);
        renderer.setPanEnabled(true, true);

        ScatterChart chart = new ScatterChart(dataset, renderer);
        ChartComponent c = new ChartComponent(chart);
        c.setZoomEnabled(true);
        c.setPanEnabled(true);

        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();

        // Simulate pinch zoom to trigger BBox usage
        int[] x = new int[] { 100, 200 };
        int[] y = new int[] { 100, 200 };

        c.pointerDragged(x, y);

        // Drag again to trigger zoom logic using BBox
        x[0] += 10;
        x[1] -= 10;
        c.pointerDragged(x, y);
    }
}
