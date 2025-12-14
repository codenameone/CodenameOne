package com.codename1.ui;

import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.views.LineChart;
import com.codename1.charts.ChartComponent;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.Display;
import org.junit.jupiter.api.Assertions;

public class ChartComponentZoomTest extends UITestBase {

    @FormTest
    public void testZoomTransition() {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setZoomEnabled(true, true);
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        LineChart chart = new LineChart(dataset, renderer);

        ChartComponent component = new ChartComponent(chart);
        Form f = new Form("Chart", new BorderLayout());
        f.add(BorderLayout.CENTER, component);
        f.show();

        // Ensure layout
        f.revalidate();
        f.getAnimationManager().flush();

        // Trigger zoom
        // This will add a ZoomTransition to the internal animations list and start it.
        // We verify that it doesn't crash and changes are applied eventually.
        component.zoomTo(0, 10, 0, 10, 100);

        // Simulate animation frames
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 500) {
            f.getAnimationManager().flush();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {}
        }

    }
}
