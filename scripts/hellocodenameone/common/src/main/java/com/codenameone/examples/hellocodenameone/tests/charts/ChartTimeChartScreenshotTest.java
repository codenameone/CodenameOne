package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.models.TimeSeries;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.TimeChart;

import java.util.Calendar;
import java.util.Date;

/// TimeChart with a deterministic series anchored at a fixed local date --
/// avoids using `new Date()` and avoids UTC-midnight labels shifting by the
/// runner's default timezone.
public class ChartTimeChartScreenshotTest extends AbstractChartScreenshotTest {

    @Override
    protected AbstractChart buildChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

        TimeSeries series = new TimeSeries("Visits");
        long day = 24L * 60L * 60L * 1000L;
        long anchor = localMidnight(2024, Calendar.MARCH, 1);
        double[] values = {120, 134, 142, 158, 145, 168, 180, 175, 192, 205};
        for (int i = 0; i < values.length; i++) {
            series.add(new Date(anchor + i * day), values[i]);
        }
        dataset.addSeries(series);

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(20);
        renderer.setMargins(new int[]{36, 80, 24, 24});
        renderer.setShowGrid(true);

        XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
        seriesRenderer.setColor(ColorUtil.rgb(0x14, 0x71, 0xc4));
        seriesRenderer.setLineWidth(3f);
        renderer.addSeriesRenderer(seriesRenderer);

        return new TimeChart(dataset, renderer);
    }

    @Override
    protected String screenshotName() {
        return "chart-time";
    }

    private static long localMidnight(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime().getTime();
    }
}
