package com.codename1.charts.views;

import com.codename1.charts.models.AreaSeries;
import com.codename1.charts.models.CategorySeries;
import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.util.ColorUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RadarChartTest {
    private RadarChart createChart(AreaSeries dataset) {
        DefaultRenderer renderer = new DefaultRenderer();
        renderer.setLabelsTextSize(12f);
        renderer.setLegendTextSize(10f);
        renderer.setStartAngle(0f);
        renderer.setApplyBackgroundColor(true);
        renderer.setBackgroundColor(ColorUtil.WHITE);
        renderer.setScale(1f);

        int seriesCount = dataset.getSeriesCount();
        for (int i = 0; i < Math.max(seriesCount, 1); i++) {
            SimpleSeriesRenderer rendererEntry = new SimpleSeriesRenderer();
            rendererEntry.setColor(ColorUtil.argb(255, 50 * (i + 1), 100, 150));
            renderer.addSeriesRenderer(rendererEntry);
        }
        return new RadarChart(dataset, renderer);
    }

    private AreaSeries createDataset(int categories, int seriesCount) {
        AreaSeries area = new AreaSeries();
        for (int s = 0; s < seriesCount; s++) {
            CategorySeries series = new CategorySeries("Series" + s);
            for (int c = 0; c < categories; c++) {
                series.add("C" + c, 0.2 + 0.1 * (s + c));
            }
            area.addSeries(series);
        }
        return area;
    }

    @Test
    public void testLegendShapeWidthConstant() {
        RadarChart chart = createChart(createDataset(3, 1));
        assertEquals(10, chart.getLegendShapeWidth(0));
    }
}
