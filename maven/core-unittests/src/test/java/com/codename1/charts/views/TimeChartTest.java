package com.codename1.charts.views;

import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.TimeSeries;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

import java.util.Date;

public class TimeChartTest extends UITestBase {

    @FormTest
    public void testTimeChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        TimeSeries series = new TimeSeries("Data");
        series.add(new Date(), 100);
        dataset.addSeries(series);

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

        TimeChart chart = new TimeChart(dataset, renderer);

        Assertions.assertEquals("Time", chart.getChartType());
        // getDateFormat returns null by default if not set, and it computes it on the fly during drawing
        Assertions.assertNull(chart.getDateFormat());

        chart.setDateFormat("MM/dd/yyyy");
        Assertions.assertEquals("MM/dd/yyyy", chart.getDateFormat());
    }
}
