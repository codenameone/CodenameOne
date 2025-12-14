package com.codename1.charts.transitions;

import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.charts.ChartComponent;
import com.codename1.charts.models.XYValueSeries;
import com.codename1.charts.views.BubbleChart;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import org.junit.jupiter.api.Assertions;

public class XYValueSeriesTransitionTest extends UITestBase {

    @FormTest
    public void testXYValueSeriesTransition() {
        XYValueSeries series = new XYValueSeries("Test Series");
        // Cast first arg to double to ensure add(double, double, double) is called
        // instead of add(int, double, double) from XYSeries
        series.add(1.0, 10.0, 5.0);

        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(series);
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        BubbleChart chart = new BubbleChart(dataset, renderer);
        ChartComponent component = new ChartComponent(chart);

        XYValueSeriesTransition transition = new XYValueSeriesTransition(component, series);

        // Test buffer
        XYValueSeries buffer = transition.getBuffer();
        Assertions.assertNotNull(buffer);
        buffer.add(1.0, 20.0, 10.0);

        // Init transition
        transition.initTransition();

        transition.update(50);

        Assertions.assertEquals(1, series.getItemCount());
        Assertions.assertEquals(15.0, series.getY(0), 0.001);
        Assertions.assertEquals(7.5, series.getValue(0), 0.001);

        transition.update(100);
        Assertions.assertEquals(20.0, series.getY(0), 0.001);
        Assertions.assertEquals(10.0, series.getValue(0), 0.001);

        // Cleanup
        transition.cleanup();
    }
}
